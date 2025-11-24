package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.AttackComponent;
import com.gameengine.components.HealthComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.core.GameObject;
import com.gameengine.components.BulletComponent;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.xml.crypto.dsig.Transform;

/**
 * 游戏逻辑类，处理具体的游戏规则
 */
public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    public enum GameState { RUNNING, PAUSE, GAME_OVER }
    private GameState gameState = GameState.RUNNING;
    private boolean pauseKeyPrev = false;
    private boolean restartKeyPrev = false;
    private boolean shootKeyPrev = false;
    private ExecutorService physicsExecutor;
    private static final boolean PERF_DEBUG = true;
    private long perfLastLogNs;
    private double physicsAccMs;
    private int physicsSamples;
    private double physicsMinMs = Double.MAX_VALUE;
    private double physicsMaxMs = 0.0;
    private double avoidSerialAccMs;
    private int avoidSerialSamples;
    private double avoidSerialMinMs = Double.MAX_VALUE;
    private double avoidSerialMaxMs = 0.0;
    private int avoidSerialEnemyCountAcc;
    private double avoidParallelAccMs;
    private int avoidParallelSamples;
    private double avoidParallelMinMs = Double.MAX_VALUE;
    private double avoidParallelMaxMs = 0.0;
    private int avoidParallelEnemyCountAcc;
    private int physicsLastThreadCount;
    private int physicsLastBatchSize;
    private int avoidanceLastThreadCount;
    private int avoidanceLastBatchSize;
    private int lastEnemyCount;
    private java.util.Map<GameObject, Long> lastShotNs = new java.util.HashMap<>();
    private int killCount;
    private float enemySpeed;
    private int currentColorIdx = 1;

    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        int threadCount = Math.max(4, Runtime.getRuntime().availableProcessors() - 1);
        physicsExecutor = Executors.newFixedThreadPool(threadCount);
        if (PERF_DEBUG)
            perfLastLogNs = System.nanoTime();
    }

    private GameObject getUserPlayer() {
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Player") && obj.hasComponent(PhysicsComponent.class)) {
                return obj;
            }
        }
        return null;
    }

    private List<GameObject> getEnemy() {
        return scene.getGameObjects().stream()
                .filter(obj -> obj.getName().equals("Enemy"))
                .filter(obj -> obj.isActive())
                .collect(Collectors.toList());
    }

    /**
     * 处理玩家输入
     */
    public boolean isRestartKeyPressed() {
        boolean curr = inputManager.isKeyPressed(32);
        boolean justPressed = curr && !restartKeyPrev;
        restartKeyPrev = curr;
        return justPressed;
    }

    public boolean isPauseKeyPressed() {
        boolean curr = inputManager.isKeyPressed(80);
        boolean justPressed = curr && !pauseKeyPrev;
        pauseKeyPrev = curr;
        return justPressed;
    }

    public void togglePause() {
        if (isPauseKeyPressed()) {
            if (gameState == GameState.RUNNING) gameState = GameState.PAUSE; else if (gameState == GameState.PAUSE) gameState = GameState.RUNNING;
        }
    }

    public boolean isShootKeyPressed() {
        boolean curr = inputManager.isKeyPressed(32);
        boolean justPressed = curr && !shootKeyPrev;
        shootKeyPrev = curr;
        return justPressed;
    }

    public boolean isShootHeld() {
        return inputManager.isKeyPressed(32);
    }

    public int getColorSwitchIndexJustPressed() {
        if (inputManager.isKeyJustPressed(49) || inputManager.isKeyJustPressed(97)) return 1; // 1 / Numpad1
        if (inputManager.isKeyJustPressed(50) || inputManager.isKeyJustPressed(98)) return 2; // 2 / Numpad2
        if (inputManager.isKeyJustPressed(51) || inputManager.isKeyJustPressed(99)) return 3; // 3 / Numpad3
        if (inputManager.isKeyJustPressed(52) || inputManager.isKeyJustPressed(100)) return 4; // 4 / Numpad4
        if (inputManager.isKeyJustPressed(53) || inputManager.isKeyJustPressed(101)) return 5; // 5 / Numpad5
        if (inputManager.isKeyJustPressed(54) || inputManager.isKeyJustPressed(102)) return 6; // 6 / Numpad6
        if (inputManager.isKeyJustPressed(55) || inputManager.isKeyJustPressed(103)) return 7; // 7 / Numpad7
        return 0;
    }

    public GameState getGameState() { return gameState; }
    public void setRunning() { gameState = GameState.RUNNING; }
    public void setGameOver() { gameState = GameState.GAME_OVER; }

    public void handlePlayerInput() {
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty())
            return;

        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        if (transform == null || physics == null)
            return;

        Vector2 movement = new Vector2();

        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }

        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);//玩家速度
            physics.setVelocity(movement);
        }
    }

    public void updateEnemyAI(GameObject enemy, float deltaTime) {
        TransformComponent enemyPos = enemy.getComponent(TransformComponent.class);
        PhysicsComponent physics = enemy.getComponent(PhysicsComponent.class);
        AttackComponent attack = enemy.getComponent(AttackComponent.class);
        if (enemyPos == null || physics == null || attack == null) return;
        GameObject player = getUserPlayer();
        if (player == null || !player.isActive()) return;
        if (attack.getRootTime() > 0f) {
            physics.setVelocity(0f, 0f);
        } else {
            TransformComponent playerT = player.getComponent(TransformComponent.class);
            Vector2 playerCenter = playerT.getPosition();
            RenderComponent renderComponent = enemy.getComponent(RenderComponent.class);
            Vector2 size = renderComponent != null ? renderComponent.getSize() : new Vector2(20, 20);
            Vector2 enemyCenter = enemyPos.getPosition().add(new Vector2(size.x * 0.5f, size.y * 0.5f));
            Vector2 direction = playerCenter.subtract(enemyCenter).normalize();
            float sf = attack.getSlowTime() > 0f ? attack.getSlowFactor() : 1.0f;
            physics.setVelocity(direction.multiply(enemySpeed * sf));
        }
        attack.tryAttack(scene, "Player");
    }

    public void cleanupDeadObjects() {
        for (GameObject obj : scene.getGameObjects().toArray(new GameObject[0])) {
            HealthComponent healthComponent = obj.getComponent(HealthComponent.class);
            if (healthComponent != null && healthComponent.getHealth() <= 0) {
                if (obj.getName().equals("Enemy")) {
                    killCount++;
                }
                obj.destroy();
                scene.removeGameObject(obj);
            }
        }
    }

    public List<GameObject> chooseTargetsForShooting(int maxBullets, boolean isPurple) {
        int activeBullets = 0;
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Bullet") && obj.isActive()) activeBullets++;
        }
        int limit = isPurple ? 1 : maxBullets;
        int available = Math.max(0, limit - activeBullets);
        if (available <= 0) return java.util.Collections.emptyList();
        java.util.Set<GameObject> targeted = new java.util.HashSet<>();
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Bullet") && obj.isActive()) {
                BulletComponent bulletComponent = obj.getComponent(BulletComponent.class);
                if (bulletComponent != null) {
                    GameObject t = bulletComponent.getTarget();
                    if (t != null) targeted.add(t);
                }
            }
        }
        GameObject player = getUserPlayer();
        TransformComponent playerTransform = player != null ? player.getComponent(TransformComponent.class) : null;
        Vector2 playerPos = playerTransform != null ? playerTransform.getPosition() : new Vector2(0, 0);
        java.util.List<GameObject> targets = new java.util.ArrayList<>();
        long now = System.nanoTime();
        for (GameObject obj : scene.getGameObjects()) {
            if (!obj.isActive() || !obj.getName().equals("Enemy")) continue;
            if (targeted.contains(obj)) continue;
            Long lt = lastShotNs.get(obj);
            if (lt != null && now - lt < 250_000_000L) continue;
            if (isPurple) {
                AttackComponent attackComponent = obj.getComponent(AttackComponent.class);
                if (attackComponent != null && attackComponent.getRootTime() > 0f) continue;
            }
            targets.add(obj);
        }
        targets.sort((a, b) -> {
            TransformComponent transformA = a.getComponent(TransformComponent.class);
            TransformComponent transformB = b.getComponent(TransformComponent.class);
            RenderComponent renderA = a.getComponent(RenderComponent.class);
            RenderComponent renderB = b.getComponent(RenderComponent.class);
            Vector2 sizeA = renderA != null ? renderA.getSize() : new Vector2(20, 20);
            Vector2 sizeB = renderB != null ? renderB.getSize() : new Vector2(20, 20);
            float distanceA = transformA != null ? playerPos.distance(transformA.getPosition().add(new Vector2(sizeA.x * 0.5f, sizeA.y * 0.5f))) : Float.MAX_VALUE;
            float distanceB = transformB != null ? playerPos.distance(transformB.getPosition().add(new Vector2(sizeB.x * 0.5f, sizeB.y * 0.5f))) : Float.MAX_VALUE;
            return Float.compare(distanceA, distanceB);
        });
        int canFire = Math.min(available, targets.size());
        java.util.List<GameObject> result = new java.util.ArrayList<>();
        for (int i = 0; i < canFire; i++) {
            GameObject t = targets.get(i);
            lastShotNs.put(t, now);
            result.add(t);
        }
        return result;
    }

    public int getKillCount() { return killCount; }
    public void resetKillCount() { killCount = 0; }
    public void setEnemySpeed(float s) { enemySpeed = s; }
    public int getCurrentColorIndex() { return currentColorIdx; }
    public void setCurrentColorIndex(int idx) { if (idx >= 1 && idx <= 7) currentColorIdx = idx; }

    /**
     * 更新物理系统
     */
    public void updatePhysics() {
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        if (physicsComponents.isEmpty())
            return;
        long t0 = 0L;
        if (PERF_DEBUG)
            t0 = System.nanoTime();

        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(4, threadCount);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);
        if (PERF_DEBUG) {
            physicsLastThreadCount = threadCount;
            physicsLastBatchSize = batchSize;
        }

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());

            Future<?> future = physicsExecutor.submit(() -> {
                for (int j = start; j < end; ++j) {
                    PhysicsComponent component = physicsComponents.get(j);
                    updateSinglePhysics(component);
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (PERF_DEBUG) {
            double elapsedMs = (System.nanoTime() - t0) / 1_000_000.0;
            physicsAccMs += elapsedMs;
            physicsSamples += 1;
            physicsMinMs = Math.min(physicsMinMs, elapsedMs);
            physicsMaxMs = Math.max(physicsMaxMs, elapsedMs);
        }
    }

    public void updateSinglePhysics(PhysicsComponent component) {
        TransformComponent transform = component.getOwner().getComponent(TransformComponent.class);
        if (transform != null) {
            Vector2 pos = transform.getPosition();
            Vector2 velocity = component.getVelocity();

            Vector2 size = new Vector2(20, 20);
            RenderComponent render = component.getOwner().getComponent(RenderComponent.class);
            if (render != null) {
                size = render.getSize();
            }
            int screenW = scene.getRenderer() != null ? scene.getRenderer().getWidth() : 1600;
            int screenH = scene.getRenderer() != null ? scene.getRenderer().getHeight() : 900;
            if ((pos.x <= 0 && velocity.x < 0) || (pos.x >= screenW - size.x && velocity.x > 0)) {
                velocity.x = -velocity.x;
                component.setVelocity(velocity);
            }
            if ((pos.y <= 0 && velocity.y < 0) || (pos.y >= screenH - size.y && velocity.y > 0)) {
                velocity.y = -velocity.y;
                component.setVelocity(velocity);
            }
            // 确保在边界内
            if (pos.x < 0)
                pos.x = 0;
            if (pos.y < 0)
                pos.y = 0;
            if (pos.x > screenW - size.x)
                pos.x = screenW - size.x;
            if (pos.y > screenH - size.y)
                pos.y = screenH - size.y;
            transform.setPosition(pos);
        }
    }

    /**
     * 检查碰撞
     */
    public void handleEnemyAvoidance(float deltaTime) {
        List<GameObject> enemy = getEnemy();
        if (enemy.isEmpty())
            return;
        if (PERF_DEBUG)
            lastEnemyCount = enemy.size();

        if (enemy.size() < 300) {
            handleEnemyAvoidanceSerial(enemy, deltaTime);
        } else {
            handleEnemyAvoidanceParallel(enemy, deltaTime);
        }
        if (PERF_DEBUG)
            perfMaybeLog();
    }

    public void handleEnemyAvoidanceSerial(List<GameObject> enemy, float deltaTime) {
        long t0 = 0L;
        if (PERF_DEBUG)
            t0 = System.nanoTime();
        for (int i = 0; i < enemy.size(); ++i) {
            processAvoidanceForPlayer(enemy, i, deltaTime);
        }
        if (PERF_DEBUG) {
            double elapsedMs = (System.nanoTime() - t0) / 1_000_000.0;
            avoidSerialAccMs += elapsedMs;
            avoidSerialSamples += 1;
            avoidSerialMinMs = Math.min(avoidSerialMinMs, elapsedMs);
            avoidSerialMaxMs = Math.max(avoidSerialMaxMs, elapsedMs);
            avoidanceLastThreadCount = 1;
            avoidanceLastBatchSize = enemy.size();
            avoidSerialEnemyCountAcc += enemy.size();
        }
    }

    public void handleEnemyAvoidanceParallel(List<GameObject> enemy, float deltaTime) {
        long t0 = 0L;
        if (PERF_DEBUG)
            t0 = System.nanoTime();
        int threadCount = Math.max(4, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, enemy.size() / threadCount + 1);
        if (PERF_DEBUG) {
            avoidanceLastThreadCount = threadCount;
            avoidanceLastBatchSize = batchSize;
        }

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < enemy.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(enemy.size(), i + batchSize);

            Future<?> future = physicsExecutor.submit(() -> {
                for (int j = start; j < end; ++j) {
                    processAvoidanceForPlayer(enemy, j, deltaTime);
                }
            });

            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (PERF_DEBUG) {
            double elapsedMs = (System.nanoTime() - t0) / 1_000_000.0;
            avoidParallelAccMs += elapsedMs;
            avoidParallelSamples += 1;
            avoidParallelMinMs = Math.min(avoidParallelMinMs, elapsedMs);
            avoidParallelMaxMs = Math.max(avoidParallelMaxMs, elapsedMs);
            avoidParallelEnemyCountAcc += enemy.size();
        }
    }

    private void processAvoidanceForPlayer(List<GameObject> enemy, int index, float deltaTime) {
        GameObject enemy1 = enemy.get(index);
        TransformComponent transform1 = enemy1.getComponent(TransformComponent.class);
        PhysicsComponent physics1 = enemy1.getComponent(PhysicsComponent.class);
        if (transform1 == null || physics1 == null)
            return;

        Vector2 pos1 = transform1.getPosition();
        RenderComponent renderComponent1 = enemy1.getComponent(RenderComponent.class);
        Vector2 size1 = renderComponent1 != null ? renderComponent1.getSize() : new Vector2(40, 40);
        pos1 = new Vector2(pos1.x + size1.x / 2f, pos1.y + size1.y / 2f);
        Vector2 avoidance = new Vector2();

        for (int i = 0; i < enemy.size(); ++i) {
            if (i == index)
                continue;
            GameObject enemy2 = enemy.get(i);
            TransformComponent transform2 = enemy2.getComponent(TransformComponent.class);

            if (transform2 == null)
                continue;

            Vector2 pos2 = transform2.getPosition();
            RenderComponent renderComponent2 = enemy2.getComponent(RenderComponent.class);
            Vector2 size2 = renderComponent2 != null ? renderComponent2.getSize() : new Vector2(40, 40);
            pos2 = new Vector2(pos2.x + size2.x / 2f, pos2.y + size2.y / 2f);
            float distance = pos1.distance(pos2);

            if (distance > 0 && distance < 45) {
                Vector2 direction = pos1.subtract(pos2).normalize();
                float strength = (45 - distance) / 45.0f;
                avoidance = avoidance.add(direction.multiply(strength * 100));
            }
        }

        if (avoidance.magnitude() > 0) {
            Vector2 currentVelocity = physics1.getVelocity();
            Vector2 avoidanceDirection = avoidance.normalize();
            float avoidanceStrength = Math.min(400, avoidance.magnitude());

            Vector2 targetVelocity = currentVelocity
                    .add(avoidanceDirection.multiply(avoidanceStrength * deltaTime * 400));// 消除fps的影响

            float lerpFactor = 0.35f;
            Vector2 newVelocity = new Vector2(
                    currentVelocity.x + (targetVelocity.x - currentVelocity.x) * lerpFactor,
                    currentVelocity.y + (targetVelocity.y - currentVelocity.y) * lerpFactor);

            physics1.setVelocity(newVelocity);
        }
    }

    public void checkCollisions() {
        GameObject player = getUserPlayer();
        if (player == null || !player.isActive())
            return;
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null)
            return;
        Vector2 playerPos = playerTransform.getPosition();
        float playerRadius = 22f;//玩家的判定范围

        List<GameObject> enemies = getEnemy();

        for (GameObject obj : enemies) {
            TransformComponent enemyTransform = obj.getComponent(TransformComponent.class);
            if (enemyTransform != null) {
                Vector2 enemyPos = enemyTransform.getPosition();
                RenderComponent render = obj.getComponent(RenderComponent.class);
                Vector2 size = render != null ? render.getSize() : new Vector2(20, 20);
                Vector2 enemyCenter = new Vector2(enemyPos.x + size.x / 2f, enemyPos.y + size.y / 2f);
                float enemyRadius = Math.max(size.x, size.y) / 2f;

                float distance = playerPos.distance(enemyCenter);
                if (distance < playerRadius + enemyRadius && distance > 0f) {
                    Vector2 direction = playerPos.subtract(enemyCenter).normalize();
                    float push = (playerRadius + enemyRadius) - distance;

                    playerTransform.translate(direction.multiply(push));

                    PhysicsComponent enemyPhysics = obj.getComponent(PhysicsComponent.class);
                    if (enemyPhysics != null) {
                        enemyPhysics.addVelocity(direction.multiply(-100f)); // 敌人后退
                    }

                    PhysicsComponent playerPhysics = player.getComponent(PhysicsComponent.class);
                    if (playerPhysics != null) {
                        playerPhysics.addVelocity(direction.multiply(200f)); // 玩家后退
                    }
                    break;
                }
            }
        }
    }

    private void perfMaybeLog() {
        long now = System.nanoTime();
        if (now - perfLastLogNs >= 1_000_000_000L) {
            double physicsAvg = physicsSamples > 0 ? physicsAccMs / physicsSamples : 0.0;
            double serialAvg = avoidSerialSamples > 0 ? avoidSerialAccMs / avoidSerialSamples : 0.0;
            double parallelAvg = avoidParallelSamples > 0 ? avoidParallelAccMs / avoidParallelSamples : 0.0;
            double serialPerEnemy = avoidSerialEnemyCountAcc > 0 ? avoidSerialAccMs / avoidSerialEnemyCountAcc : 0.0;
            double parallelPerEnemy = avoidParallelEnemyCountAcc > 0 ? avoidParallelAccMs / avoidParallelEnemyCountAcc
                    : 0.0;
            double physicsMin = physicsMinMs == Double.MAX_VALUE ? 0.0 : physicsMinMs;
            double serialMin = avoidSerialMinMs == Double.MAX_VALUE ? 0.0 : avoidSerialMinMs;
            double parallelMin = avoidParallelMinMs == Double.MAX_VALUE ? 0.0 : avoidParallelMinMs;
            String serialAvgDisplay = avoidSerialSamples == 0 ? "N/A"
                    : (serialAvg < 1.0 ? String.format("%.0fus", serialAvg * 1000.0)
                            : String.format("%.2fms", serialAvg));
            String parallelAvgDisplay = avoidParallelSamples == 0 ? "N/A"
                    : (parallelAvg < 1.0 ? String.format("%.0fus", parallelAvg * 1000.0)
                            : String.format("%.2fms", parallelAvg));
            System.out.println(String.format(
                    "Perf(1s) Physics: avg=%.2fms min=%.2fms max=%.2fms (n=%d, threads=%d, batch=%d) | Avoidance(serial): %s (%.0fus/enemy) min=%.2fms max=%.2fms (n=%d) | Avoidance(parallel): %s (%.0fus/enemy) min=%.2fms max=%.2fms (n=%d, threads=%d, batch=%d) | enemies=%d",
                    physicsAvg, physicsMin, physicsMaxMs, physicsSamples, physicsLastThreadCount, physicsLastBatchSize,
                    serialAvgDisplay, serialPerEnemy * 1000.0, serialMin, avoidSerialMaxMs, avoidSerialSamples,
                    parallelAvgDisplay, parallelPerEnemy * 1000.0, parallelMin, avoidParallelMaxMs,
                    avoidParallelSamples, avoidanceLastThreadCount, avoidanceLastBatchSize,
                    lastEnemyCount));
            physicsAccMs = 0.0;
            physicsSamples = 0;
            physicsMinMs = Double.MAX_VALUE;
            physicsMaxMs = 0.0;
            avoidSerialAccMs = 0.0;
            avoidSerialSamples = 0;
            avoidSerialMinMs = Double.MAX_VALUE;
            avoidSerialMaxMs = 0.0;
            avoidSerialEnemyCountAcc = 0;
            avoidParallelAccMs = 0.0;
            avoidParallelSamples = 0;
            avoidParallelMinMs = Double.MAX_VALUE;
            avoidParallelMaxMs = 0.0;
            avoidParallelEnemyCountAcc = 0;
            perfLastLogNs = now;
        }
    }

    public void updateInput() {
        inputManager.update();
    }

    public boolean isShootPressedOnce() {
        boolean pressed = inputManager.isKeyPressed(32);
        boolean just = pressed && !shootKeyPrev;
        shootKeyPrev = pressed;
        return just;
    }

    public boolean isKeyPressed(int code) {
        return inputManager.isKeyPressed(code);
    }

    public boolean isKeyJustPressed(int code) {
        return inputManager.isKeyJustPressed(code);
    }
}