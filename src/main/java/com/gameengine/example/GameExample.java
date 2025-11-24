package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.input.InputManager;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.recording.RecordingService;
import com.gameengine.recording.RecordingConfig;

import java.util.List;
import java.util.Random;

/**
 * 游戏示例
 */
public class GameExample {
    private enum BulletColor {
        RED,
        ORANGE,
        YELLOW,
        GREEN,
        BLUE,
        INDIGO,
        PURPLE
    }

    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");

        try {
            // 创建游戏引擎
            GameEngine engine = new GameEngine(1600, 900, "葫芦娃大战妖精");

            // 创建游戏场景
            Scene gameScene = new Scene("BattleScene") {
                private Renderer renderer;
                private Random random;
                private float time;
                private GameLogic gameLogic;
                private GameObject player;
                private int maxBullets;
                private BulletColor currentColor;

                @Override
                public void initialize() {
                    super.initialize();
                    this.renderer = engine.getRenderer();
                    this.setRenderer(this.renderer);
                    this.random = new Random();
                    this.gameLogic = new GameLogic(this);
                    this.time = 0;
                    this.maxBullets = 5;
                    this.currentColor = BulletColor.RED;

                    createPlayer();
                    createEnemies();
                    createDecorations();
                    createHUD();
                }

                @Override
                public void resetGameObjects() {
                    clear();
                    this.gameLogic = new GameLogic(this);
                    this.time = 0;
                    createPlayer();
                    createEnemies();
                    createDecorations();
                    createHUD();
                    this.gameLogic.setRunning();
                }

                @Override
                public void update(float deltaTime) {
                    super.update(deltaTime);

                    gameLogic.togglePause();
                    if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.PAUSE) {
                        return;
                    }
                    if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.GAME_OVER) {
                        if (gameLogic.isRestartKeyPressed()) {
                            resetGameObjects();
                        }
                        return;
                    }

                    time += deltaTime;

                    int colorIdx = gameLogic.getColorSwitchIndexJustPressed();
                    if (colorIdx != 0) {
                        BulletColor oldColor = currentColor;
                        currentColor = colorFromIndex(colorIdx);
                        gameLogic.setCurrentColorIndex(colorIdx);
                        System.out.println("子弹颜色切换: " + oldColor.name() + " -> " + currentColor.name());
                    }

                    gameLogic.setEnemySpeed(50f);
                    gameLogic.handlePlayerInput();

                    if (time > 5.0f) {
                        createEnemy();
                        time = 0;
                    }

                    for (GameObject obj : this.getGameObjects()) {
                        if (obj.getName().equals("Enemy")) {
                            gameLogic.updateEnemyAI(obj, deltaTime);
                        }
                    }

                    gameLogic.handleEnemyAvoidance(deltaTime);

                    if (player != null && player.isActive()) {
                        boolean firePurple = (currentColor == BulletColor.PURPLE) && gameLogic.isShootPressedOnce();
                        boolean fireOthers = (currentColor != BulletColor.PURPLE) && gameLogic.isShootHeld();
                        if (firePurple || fireOthers) {
                            tryShoot();
                        }
                    }
                    else {
                        gameLogic.setGameOver();
                    }

                    gameLogic.updatePhysics();
                    gameLogic.checkCollisions();
                    gameLogic.cleanupDeadObjects();
                }

                private BulletColor colorFromIndex(int idx) {
                    switch (idx) {
                        case 1: return BulletColor.RED;
                        case 2: return BulletColor.ORANGE;
                        case 3: return BulletColor.YELLOW;
                        case 4: return BulletColor.GREEN;
                        case 5: return BulletColor.BLUE;
                        case 6: return BulletColor.INDIGO;
                        case 7: return BulletColor.PURPLE;
                        default: return currentColor;
                    }
                }

                @Override
                public void render() {
                    // 绘制背景
                    renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);
                    // 渲染所有对象
                    super.render();
                    if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.GAME_OVER) {
                        renderer.drawTextCentered("Game Over", 32, 1, 0, 0, 1);
                        renderer.drawTextCenteredOffset("Press Space to Restart", 28, 48f, 1, 1, 1, 1);
                    }

                }

                private void createPlayer() {
                    this.player = new GameObject("Player");

                    // 添加变换组件
                    TransformComponent transform = player.addComponent(new TransformComponent(new Vector2(400, 300)));

                    // 添加物理组件
                    PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
                    physics.setFriction(0.95f);

                    // 添加生命和攻击组件
                    player.addComponent(new HealthComponent(10000));
                    player.addComponent(new AttackComponent(50, 100));

                    player.addComponent(new PlayerRenderComponent());
                    addGameObject(player);
                }

                private void createEnemies() {
                    for (int i = 0; i < 20; i++) {
                        createEnemy();
                    }
                }

                private void createEnemy() {
                    GameObject enemy = new GameObject("Enemy") {
                        @Override
                        public void update(float deltaTime) {
                            super.update(deltaTime);
                            updateComponents(deltaTime);
                        }

                        @Override
                        public void render() {
                            renderComponents();
                        }
                    };

                    // 随机位置
                    Vector2 position = new Vector2(
                            random.nextFloat() * renderer.getWidth(),
                            random.nextFloat() * renderer.getHeight());

                    // 添加变换组件
                    TransformComponent transform = enemy.addComponent(new TransformComponent(position));

                    RenderComponent render = enemy.addComponent(new RenderComponent(
                            RenderComponent.RenderType.RECTANGLE,
                            new Vector2(20, 20),
                            new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)));
                    render.setSize(new Vector2(40, 40));
                    render.setImageFromResource(
                            "E:\\java\\classroom\\j03-ksmlie\\src\\main\\java\\com\\gameengine\\resources\\snake.png");
                    render.setRenderer(renderer);

                    // 添加物理组件
                    PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
                    physics.setVelocity(new Vector2(
                            (random.nextFloat() - 0.5f) * 100,
                            (random.nextFloat() - 0.5f) * 100));
                    physics.setFriction(0.98f);

                    // 添加生命和攻击组件
                    enemy.addComponent(new HealthComponent(8000));
                    enemy.addComponent(new AttackComponent(10, 50));

                    addGameObject(enemy);
                }

                private void tryShoot() {
                    java.util.List<GameObject> targets = gameLogic.chooseTargetsForShooting(maxBullets, currentColor == BulletColor.PURPLE);
                    for (GameObject t : targets) {
                        createBullet(player, t);
                    }
                }

                private void createBullet(GameObject shooter, GameObject target) {
                    final BulletColor mode = currentColor;
                    TransformComponent shooterTransform = shooter.getComponent(TransformComponent.class);
                    Vector2 start = shooterTransform != null ? shooterTransform.getPosition() : new Vector2(0, 0);
                    createBulletFrom(start, target, mode, true);
                }

                private void createBulletFrom(Vector2 start, GameObject target, BulletColor mode, boolean allowChain) {
                    final float baseSpeed = 480f;
                    final float speedMul = (mode == BulletColor.ORANGE ? 1.5f : 1.0f);
                    final int finalDmg = (mode == BulletColor.RED ? (int) (1000 * 1.8f) : 1000);
                    final boolean invisible = (mode == BulletColor.INDIGO);
                    final boolean chain = mode == BulletColor.YELLOW;
                    GameObject bullet = new GameObject("Bullet");
                    bullet.addComponent(new TransformComponent(start));
                    RenderComponent renderComponent = bullet.addComponent(new RenderComponent(RenderComponent.RenderType.CIRCLE,
                            new Vector2(6, 6), new RenderComponent.Color(0.6f, 0.8f, 1.0f, 1.0f)));
                    renderComponent.setRenderer(renderer);
                    if (mode != BulletColor.INDIGO) {
                        switch (mode) {
                            case RED:
                                renderComponent.setColor(1.0f, 0.0f, 0.0f, 1.0f);
                                break;
                            case ORANGE:
                                renderComponent.setColor(1.0f, 0.5f, 0.0f, 1.0f);
                                break;
                            case YELLOW:
                                renderComponent.setColor(1.0f, 1.0f, 0.0f, 1.0f);
                                break;
                            case GREEN:
                                renderComponent.setColor(0.0f, 1.0f, 0.0f, 1.0f);
                                break;
                            case BLUE:
                                renderComponent.setColor(0.0f, 0.6f, 1.0f, 1.0f);
                                break;
                            case PURPLE:
                                renderComponent.setColor(0.6f, 0.0f, 1.0f, 1.0f);
                                break;
                            case INDIGO:
                            default:
                                break;
                        }
                    }
                    if (mode == BulletColor.INDIGO)
                        renderComponent.setVisible(false);
                    PhysicsComponent physicsComponent = bullet.addComponent(new PhysicsComponent(0.1f));
                    physicsComponent.setFriction(1.0f);
                    TransformComponent targetTransform = target.getComponent(TransformComponent.class);
                    Vector2 targetPos = targetTransform != null ? targetTransform.getPosition() : new Vector2(0, 0);
                    RenderComponent targetRender = target.getComponent(RenderComponent.class);
                    Vector2 targetSize = targetRender != null ? targetRender.getSize() : new Vector2(20, 20);
                    Vector2 targetCenter = targetPos.add(new Vector2(targetSize.x * 0.5f, targetSize.y * 0.5f));
                    Vector2 dir = targetCenter.subtract(start).normalize();
                    physicsComponent.setVelocity(dir.multiply(baseSpeed * speedMul));
                    bullet.addComponent(new BulletComponent(
                            target,
                            baseSpeed * speedMul,
                            finalDmg,
                            allowChain && chain,
                            invisible,
                            mode == BulletColor.GREEN,
                            5f,
                            200f,
                            mode == BulletColor.BLUE,
                            3f,
                            0.3f,
                            mode == BulletColor.PURPLE,
                            3f,
                            true));
                    addGameObject(bullet);
                }

                private void createDecorations() {
                    for (int i = 0; i < 5; i++) {
                        createDecoration();
                    }
                }

                private void createDecoration() {
                    GameObject decoration = new GameObject("Decoration") {
                        @Override
                        public void update(float deltaTime) {
                            super.update(deltaTime);
                            updateComponents(deltaTime);
                        }

                        @Override
                        public void render() {
                            renderComponents();
                        }
                    };

                    // 随机位置
                    Vector2 position = new Vector2(
                            random.nextFloat() * renderer.getWidth(),
                            random.nextFloat() * renderer.getHeight());

                    // 添加变换组件
                    TransformComponent transform = decoration.addComponent(new TransformComponent(position));

                    // 添加渲染组件
                    RenderComponent render = decoration.addComponent(new RenderComponent(
                            RenderComponent.RenderType.CIRCLE,
                            new Vector2(5, 5),
                            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)));
                    render.setRenderer(renderer);

                    addGameObject(decoration);
                }
                private void createHUD() {
                    GameObject hud = new GameObject("HUD") {
                        @Override
                        public void update(float deltaTime) { super.update(deltaTime); updateComponents(deltaTime); }
                        @Override
                        public void render() { renderComponents(); }
                    };
                    hud.addComponent(new HudComponent(gameLogic));
                    addGameObject(hud);
                }
            };
            Scene menu = new MenuScene(engine);
            menu.setRenderer(engine.getRenderer());
            engine.setScene(menu);

            // 运行游戏
            engine.run();

        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("游戏结束");
    }

    public static Scene createBattleScene(GameEngine engine) {
        return new Scene("BattleScene") {
                private Renderer renderer;
                private Random random;
                private float time;
                private GameLogic gameLogic;
                private GameObject player;
                private int maxBullets;
                private BulletColor currentColor;
                private RecordingService recordingService;
                private boolean recordingStopped;

            @Override
                public void initialize() {
                    super.initialize();
                    this.renderer = engine.getRenderer();
                    this.setRenderer(this.renderer);
                    this.random = new Random();
                    this.gameLogic = new GameLogic(this);
                    this.time = 0;
                    this.maxBullets = 5;
                    this.currentColor = BulletColor.RED;
                    this.recordingStopped = false;

                    createPlayer();
                    createEnemies();
                    createDecorations();
                    createHUD();

                    RecordingStorage storage = new FileRecordingStorage();
                    RecordingConfig cfg = new RecordingConfig();
                    this.recordingService = new RecordingService(storage, cfg);
                    String name = "battle_" + System.currentTimeMillis();
                    this.recordingService.start(this, this.renderer, InputManager.getInstance(), name, "v1");
                }

            @Override
                public void resetGameObjects() {
                    clear();
                    this.gameLogic = new GameLogic(this);
                    this.time = 0;
                    createPlayer();
                    createEnemies();
                    createDecorations();
                    createHUD();
                    this.gameLogic.setRunning();

                    if (this.recordingService != null) {
                        this.recordingService.stop();
                    }
                    RecordingStorage storage = new FileRecordingStorage();
                    RecordingConfig cfg = new RecordingConfig();
                    this.recordingService = new RecordingService(storage, cfg);
                    String name = "battle_" + System.currentTimeMillis();
                    this.recordingService.start(this, this.renderer, InputManager.getInstance(), name, "v1");
                }

            @Override
                public void update(float deltaTime) {
                    super.update(deltaTime);

                    if (this.recordingService != null) {
                        this.recordingService.onFrame(deltaTime);
                    }

                    gameLogic.togglePause();
                    if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.PAUSE) {
                        return;
                    }
                    if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.GAME_OVER) {
                        if (gameLogic.isRestartKeyPressed()) {
                            resetGameObjects();
                        }
                        return;
                    }

                time += deltaTime;

                int colorIdx = gameLogic.getColorSwitchIndexJustPressed();
                if (colorIdx != 0) {
                    BulletColor oldColor = currentColor;
                    currentColor = colorFromIndex(colorIdx);
                    gameLogic.setCurrentColorIndex(colorIdx);
                    System.out.println("子弹颜色切换: " + oldColor.name() + " -> " + currentColor.name());
                }

                gameLogic.setEnemySpeed(50f);
                gameLogic.handlePlayerInput();

                if (time > 5.0f) {
                    createEnemy();
                    time = 0;
                }

                for (GameObject obj : this.getGameObjects()) {
                    if (obj.getName().equals("Enemy")) {
                        gameLogic.updateEnemyAI(obj, deltaTime);
                    }
                }

                gameLogic.handleEnemyAvoidance(deltaTime);

                    if (player != null && player.isActive()) {
                        boolean firePurple = (currentColor == BulletColor.PURPLE) && gameLogic.isShootPressedOnce();
                        boolean fireOthers = (currentColor != BulletColor.PURPLE) && gameLogic.isShootHeld();
                        if (firePurple || fireOthers) {
                            tryShoot();
                        }
                    }
                    else {
                        gameLogic.setGameOver();
                        if (!recordingStopped && this.recordingService != null) {
                            this.recordingService.stop();
                            recordingStopped = true;
                        }
                    }

                    gameLogic.updatePhysics();
                    gameLogic.checkCollisions();
                    gameLogic.cleanupDeadObjects();
                }

            private BulletColor colorFromIndex(int idx) {
                switch (idx) {
                    case 1: return BulletColor.RED;
                    case 2: return BulletColor.ORANGE;
                    case 3: return BulletColor.YELLOW;
                    case 4: return BulletColor.GREEN;
                    case 5: return BulletColor.BLUE;
                    case 6: return BulletColor.INDIGO;
                    case 7: return BulletColor.PURPLE;
                    default: return currentColor;
                }
            }

            @Override
            public void render() {
                renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);
                super.render();
                if (gameLogic.getGameState() == com.gameengine.core.GameLogic.GameState.GAME_OVER) {
                    renderer.drawTextCentered("Game Over", 32, 1, 0, 0, 1);
                    renderer.drawTextCenteredOffset("Press Space to Restart", 28, 48f, 1, 1, 1, 1);
                }

            }

            private void createPlayer() {
                this.player = new GameObject("Player");

                TransformComponent transform = player.addComponent(new TransformComponent(new Vector2(400, 300)));

                PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
                physics.setFriction(0.95f);

                player.addComponent(new HealthComponent(10000));
                player.addComponent(new AttackComponent(50, 100));

                player.addComponent(new PlayerRenderComponent());
                addGameObject(player);
            }

            private void createEnemies() {
                for (int i = 0; i < 20; i++) {
                    createEnemy();
                }
            }

            private void createEnemy() {
                GameObject enemy = new GameObject("Enemy") {
                    @Override
                    public void update(float deltaTime) {
                        super.update(deltaTime);
                        updateComponents(deltaTime);
                    }

                    @Override
                    public void render() {
                        renderComponents();
                    }
                };

                Vector2 position = new Vector2(
                        random.nextFloat() * renderer.getWidth(),
                        random.nextFloat() * renderer.getHeight());

                TransformComponent transform = enemy.addComponent(new TransformComponent(position));

                RenderComponent render = enemy.addComponent(new RenderComponent(
                        RenderComponent.RenderType.RECTANGLE,
                        new Vector2(20, 20),
                        new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)));
                render.setSize(new Vector2(40, 40));
                render.setImageFromResource(
                        "E:\\java\\classroom\\j03-ksmlie\\src\\main\\java\\com\\gameengine\\resources\\snake.png");
                render.setRenderer(renderer);

                PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
                physics.setVelocity(new Vector2(
                        (random.nextFloat() - 0.5f) * 100,
                        (random.nextFloat() - 0.5f) * 100));
                physics.setFriction(0.98f);

                enemy.addComponent(new HealthComponent(8000));
                enemy.addComponent(new AttackComponent(10, 50));

                addGameObject(enemy);
            }

            private void tryShoot() {
                java.util.List<GameObject> targets = gameLogic.chooseTargetsForShooting(maxBullets, currentColor == BulletColor.PURPLE);
                for (GameObject t : targets) {
                    createBullet(player, t);
                }
            }

            private void createBullet(GameObject shooter, GameObject target) {
                final BulletColor mode = currentColor;
                TransformComponent shooterTransform = shooter.getComponent(TransformComponent.class);
                Vector2 start = shooterTransform != null ? shooterTransform.getPosition() : new Vector2(0, 0);
                createBulletFrom(start, target, mode, true);
            }

            private void createBulletFrom(Vector2 start, GameObject target, BulletColor mode, boolean allowChain) {
                final float baseSpeed = 480f;
                final float speedMul = (mode == BulletColor.ORANGE ? 1.5f : 1.0f);
                final int finalDmg = (mode == BulletColor.RED ? (int) (1000 * 1.8f) : 1000);
                final boolean invisible = (mode == BulletColor.INDIGO);
                final boolean chain = mode == BulletColor.YELLOW;
                GameObject bullet = new GameObject("Bullet");
                bullet.addComponent(new TransformComponent(start));
                RenderComponent renderComponent = bullet.addComponent(new RenderComponent(RenderComponent.RenderType.CIRCLE,
                        new Vector2(6, 6), new RenderComponent.Color(0.6f, 0.8f, 1.0f, 1.0f)));
                renderComponent.setRenderer(renderer);
                if (mode != BulletColor.INDIGO) {
                    switch (mode) {
                        case RED:
                            renderComponent.setColor(1.0f, 0.0f, 0.0f, 1.0f);
                            break;
                        case ORANGE:
                            renderComponent.setColor(1.0f, 0.5f, 0.0f, 1.0f);
                            break;
                        case YELLOW:
                            renderComponent.setColor(1.0f, 1.0f, 0.0f, 1.0f);
                            break;
                        case GREEN:
                            renderComponent.setColor(0.0f, 1.0f, 0.0f, 1.0f);
                            break;
                        case BLUE:
                            renderComponent.setColor(0.0f, 0.6f, 1.0f, 1.0f);
                            break;
                        case PURPLE:
                            renderComponent.setColor(0.6f, 0.0f, 1.0f, 1.0f);
                            break;
                        case INDIGO:
                        default:
                            break;
                    }
                }
                if (mode == BulletColor.INDIGO)
                    renderComponent.setVisible(false);
                PhysicsComponent physicsComponent = bullet.addComponent(new PhysicsComponent(0.1f));
                physicsComponent.setFriction(1.0f);
                TransformComponent targetTransform = target.getComponent(TransformComponent.class);
                Vector2 targetPos = targetTransform != null ? targetTransform.getPosition() : new Vector2(0, 0);
                RenderComponent targetRender = target.getComponent(RenderComponent.class);
                Vector2 targetSize = targetRender != null ? targetRender.getSize() : new Vector2(20, 20);
                Vector2 targetCenter = targetPos.add(new Vector2(targetSize.x * 0.5f, targetSize.y * 0.5f));
                Vector2 dir = targetCenter.subtract(start).normalize();
                physicsComponent.setVelocity(dir.multiply(baseSpeed * speedMul));
                bullet.addComponent(new BulletComponent(
                        target,
                        baseSpeed * speedMul,
                        finalDmg,
                        allowChain && chain,
                        invisible,
                        mode == BulletColor.GREEN,
                        5f,
                        200f,
                        mode == BulletColor.BLUE,
                        3f,
                        0.3f,
                        mode == BulletColor.PURPLE,
                        3f,
                        true));
                addGameObject(bullet);
            }

            private void createDecorations() {
                for (int i = 0; i < 5; i++) {
                    createDecoration();
                }
            }

            private void createDecoration() {
                GameObject decoration = new GameObject("Decoration") {
                    @Override
                    public void update(float deltaTime) {
                        super.update(deltaTime);
                        updateComponents(deltaTime);
                    }

                    @Override
                    public void render() {
                        renderComponents();
                    }
                };

                Vector2 position = new Vector2(
                        random.nextFloat() * renderer.getWidth(),
                        random.nextFloat() * renderer.getHeight());

                TransformComponent transform = decoration.addComponent(new TransformComponent(position));

                RenderComponent render = decoration.addComponent(new RenderComponent(
                        RenderComponent.RenderType.CIRCLE,
                        new Vector2(5, 5),
                        new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)));
                render.setRenderer(renderer);

                addGameObject(decoration);
            }
            private void createHUD() {
                GameObject hud = new GameObject("HUD") {
                    @Override
                    public void update(float deltaTime) { super.update(deltaTime); updateComponents(deltaTime); }
                    @Override
                    public void render() { renderComponents(); }
                };
                hud.addComponent(new HudComponent(gameLogic));
                addGameObject(hud);
            }

            @Override
            public void clear() {
                if (this.recordingService != null) {
                    this.recordingService.stop();
                }
                super.clear();
            }
        };
    }
}
