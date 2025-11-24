package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class BulletComponent extends Component<BulletComponent> {
    private GameObject targetRef;
    private float life;
    private float trail;
    private float speed;
    private int damage;
    private boolean invisible;
    private boolean chainAvailable;
    private boolean enableBurn;
    private float burnDuration;
    private float burnDps;
    private boolean enableSlow;
    private float slowDuration;
    private float slowFactor;
    private boolean enableRoot;
    private float rootDuration;
    private boolean rootKill;

    public BulletComponent(GameObject target,
                           float speed,
                           int damage,
                           boolean chain,
                           boolean invisible,
                           boolean enableBurn,
                           float burnDuration,
                           float burnDps,
                           boolean enableSlow,
                           float slowDuration,
                           float slowFactor,
                           boolean enableRoot,
                           float rootDuration,
                           boolean rootKill) {
        this.targetRef = target;
        this.speed = speed;
        this.damage = damage;
        this.chainAvailable = chain;
        this.invisible = invisible;
        this.enableBurn = enableBurn;
        this.burnDuration = burnDuration;
        this.burnDps = burnDps;
        this.enableSlow = enableSlow;
        this.slowDuration = slowDuration;
        this.slowFactor = slowFactor;
        this.enableRoot = enableRoot;
        this.rootDuration = rootDuration;
        this.rootKill = rootKill;
        this.life = 0f;
        this.trail = 0f;
    }

    public GameObject getTarget() {
        return targetRef;
    }

    @Override
    public void initialize() {}

    @Override
    public void render() {}

    @Override
    public void update(float dt) {
        life += dt;
        trail += dt;

        TransformComponent bulletTransform = owner.getComponent(TransformComponent.class);
        PhysicsComponent bulletPhysics = owner.getComponent(PhysicsComponent.class);
        if (bulletTransform == null || bulletPhysics == null || targetRef == null || !targetRef.isActive()) {
            owner.destroy();
            Scene s = owner.getScene();
            if (s != null) s.removeGameObject(owner);
            return;
        }

        TransformComponent targetTransform = targetRef.getComponent(TransformComponent.class);
        if (targetTransform == null) {
            owner.destroy();
            Scene s = owner.getScene();
            if (s != null) s.removeGameObject(owner);
            return;
        }

        RenderComponent enemyRenderComponent = targetRef.getComponent(RenderComponent.class);
        Vector2 enemySize = enemyRenderComponent != null ? enemyRenderComponent.getSize() : new Vector2(20, 20);
        Vector2 enemyCenter = targetTransform.getPosition().add(new Vector2(enemySize.x * 0.5f, enemySize.y * 0.5f));

        RenderComponent bulletRenderComponent = owner.getComponent(RenderComponent.class);
        Vector2 bulletSize = bulletRenderComponent != null ? bulletRenderComponent.getSize() : new Vector2(12, 12);
        Vector2 bulletCenter = bulletTransform.getPosition().add(new Vector2(bulletSize.x * 0.5f, bulletSize.y * 0.5f));

        Vector2 dir = enemyCenter.subtract(bulletCenter).normalize();
        bulletPhysics.setVelocity(dir.multiply(speed));

        if (bulletCenter.distance(enemyCenter) < 15f) {
            HealthComponent enemyHealth = targetRef.getComponent(HealthComponent.class);
            if (enemyHealth != null) {
                enemyHealth.damage(damage);
                if (enableBurn) {
                    AttackComponent attackComponent = targetRef.getComponent(AttackComponent.class);
                    if (attackComponent != null) attackComponent.applyBurn(burnDuration, burnDps);
                }
                if (enableSlow) {
                    AttackComponent attackComponent = targetRef.getComponent(AttackComponent.class);
                    if (attackComponent != null) attackComponent.applySlow(slowDuration, slowFactor);
                }
                if (enableRoot) {
                    AttackComponent attackComponent = targetRef.getComponent(AttackComponent.class);
                    if (attackComponent != null) attackComponent.applyRoot(rootDuration, rootKill);
                }
                if (enemyHealth.getHealth() <= 0) {
                    // 留给example的 removeDeadObjects 统一销毁与计数
                }
            }
            if (!enableRoot) {
                createExplosion(bulletCenter);
            }
            if (chainAvailable) {
                GameObject nextTarget = null;
                float best = Float.MAX_VALUE;
                Scene s = owner.getScene();
                if (s != null) {
                    for (GameObject obj : s.getGameObjects()) {
                        if (obj.getName().equals("Enemy") && obj.isActive() && obj != targetRef) {
                            TransformComponent candidateTransform = obj.getComponent(TransformComponent.class);
                            RenderComponent candidateRender = obj.getComponent(RenderComponent.class);
                            if (candidateTransform != null) {
                                Vector2 candidateSize = candidateRender != null ? candidateRender.getSize() : new Vector2(20, 20);
                                Vector2 candidateCenter = candidateTransform.getPosition().add(new Vector2(candidateSize.x * 0.5f, candidateSize.y * 0.5f));
                                float distanceToCandidate = bulletCenter.distance(candidateCenter);
                                if (distanceToCandidate < best) {
                                    best = distanceToCandidate;
                                    nextTarget = obj;
                                }
                            }
                        }
                    }
                }
                if (nextTarget != null) {
                    spawnBulletFrom(bulletCenter, nextTarget, false);
                }
                chainAvailable = false;
            }
            owner.destroy();
            Scene s2 = owner.getScene();
            if (s2 != null) s2.removeGameObject(owner);
            return;
        }

        if (trail > 0.03f) {
            createParticle(bulletTransform.getPosition());
            trail = 0f;
        }

        if (life > 3.0f) {
            owner.destroy();
            Scene s3 = owner.getScene();
            if (s3 != null) s3.removeGameObject(owner);
            return;
        }
    }

    private void spawnBulletFrom(Vector2 start, GameObject target, boolean allowChain) {
        Scene s = owner.getScene();
        if (s == null) return;
        GameObject bullet = new GameObject("Bullet");
        bullet.addComponent(new TransformComponent(start));
        RenderComponent ownerRenderComponent = owner.getComponent(RenderComponent.class);
        RenderComponent bulletRenderComponent2 = bullet.addComponent(new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(6, 6), ownerRenderComponent != null ? ownerRenderComponent.getColor() : new RenderComponent.Color(0.8f, 0.9f, 1.0f, 0.5f)));
        bulletRenderComponent2.setRenderer(s.getRenderer());
        if (invisible) bulletRenderComponent2.setVisible(false);
        PhysicsComponent physicsComponent = bullet.addComponent(new PhysicsComponent(0.1f));
        physicsComponent.setFriction(1.0f);
        TransformComponent targetTransformComponent = target.getComponent(TransformComponent.class);
        Vector2 targetPos = targetTransformComponent != null ? targetTransformComponent.getPosition() : new Vector2(0, 0);
        RenderComponent targetRender = target.getComponent(RenderComponent.class);
        Vector2 targetSize = targetRender != null ? targetRender.getSize() : new Vector2(20, 20);
        Vector2 targetCenter = targetPos.add(new Vector2(targetSize.x * 0.5f, targetSize.y * 0.5f));
        Vector2 dir = targetCenter.subtract(start).normalize();
        physicsComponent.setVelocity(dir.multiply(speed));
        bullet.addComponent(new BulletComponent(target, speed, damage, allowChain && chainAvailable, invisible,
                enableBurn, burnDuration, burnDps,
                enableSlow, slowDuration, slowFactor,
                enableRoot, rootDuration, rootKill));
        s.addGameObject(bullet);
    }

    private void createParticle(Vector2 pos) {
        Scene s = owner.getScene();
        if (s == null) return;
        GameObject particle = new GameObject("Particle") {
            private float life = 0f;
            @Override
            public void update(float dt) {
                super.update(dt);
                life += dt;
                if (life > 0.2f) {
                    this.destroy();
                    s.removeGameObject(this);
                }
            }
            @Override
            public void render() { renderComponents(); }
        };
        particle.addComponent(new TransformComponent(pos));
        RenderComponent ownerRender = owner.getComponent(RenderComponent.class);
        RenderComponent.Color color = ownerRender != null ? ownerRender.getColor() : new RenderComponent.Color(0.8f, 0.9f, 1.0f, 0.5f);
        RenderComponent particleRenderComponent = particle.addComponent(new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(3, 3), color));
        particleRenderComponent.setRenderer(s.getRenderer());
        s.addGameObject(particle);
    }

    private void createExplosion(Vector2 explosionCenter) {
        Scene s = owner.getScene();
        if (s == null) return;
        GameObject explosion = new GameObject("Explosion") {
            private float life = 0f;
            private float duration = 0.4f;
            private Vector2 c = explosionCenter;
            @Override
            public void update(float dt) {
                super.update(dt);
                life += dt;
                float t = Math.min(1f, life / duration);
                RenderComponent renderComponent = this.getComponent(RenderComponent.class);
                float w = 10 + 50 * t;
                float h = 10 + 50 * t;
                if (renderComponent != null) renderComponent.setSize(new Vector2(w, h));
                TransformComponent transformComponent = this.getComponent(TransformComponent.class);
                if (transformComponent != null) transformComponent.setPosition(new Vector2(c.x - w * 0.5f, c.y - h * 0.5f));
                if (life > duration) {
                    this.destroy();
                    s.removeGameObject(this);
                }
            }
            @Override
            public void render() { renderComponents(); }
        };
        TransformComponent explosionTransform = explosion.addComponent(new TransformComponent(explosionCenter));
        RenderComponent explosionRenderComponent = explosion.addComponent(new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(10, 10), new RenderComponent.Color(1.0f, 0.5f, 0.0f, 0.5f)));
        explosionRenderComponent.setRenderer(s.getRenderer());
        Vector2 explosionSizeNow = explosionRenderComponent.getSize();
        explosionTransform.setPosition(new Vector2(explosionCenter.x - explosionSizeNow.x * 0.5f, explosionCenter.y - explosionSizeNow.y * 0.5f));
        s.addGameObject(explosion);
    }
}