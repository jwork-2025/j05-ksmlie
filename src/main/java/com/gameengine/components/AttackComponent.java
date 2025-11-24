package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.core.GameObject;
import java.util.List;

public class AttackComponent extends Component<AttackComponent> {
    private int damage;
    private float attackRange;
    private float burnTime;
    private float burnDps;
    private float slowTime;
    private float slowFactor;
    private float rootTime;
    private boolean rootKill;

    public AttackComponent(int damage, float attackRange) {
        this.damage = damage;
        this.attackRange = attackRange;
        this.burnTime = 0f;
        this.burnDps = 0f;
        this.slowTime = 0f;
        this.slowFactor = 1.0f;
        this.rootTime = 0f;
        this.rootKill = false;
    }

    @Override
    public void render() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaTime) {
        if (burnTime > 0f) {
            burnTime -= deltaTime;
            HealthComponent healthComponent = owner.getComponent(HealthComponent.class);
            if (healthComponent != null && burnDps > 0f) {
                healthComponent.damage((int) (burnDps * deltaTime));
            }
        }
        if (slowTime > 0f) {
            slowTime -= deltaTime;
            if (slowTime <= 0f) {
                slowFactor = 1.0f;
            }
        }
        if (rootTime > 0f) {
            rootTime -= deltaTime;
            PhysicsComponent physicsComponent = owner.getComponent(PhysicsComponent.class);
            if (physicsComponent != null) {
                physicsComponent.setVelocity(0f, 0f);
            }
            if (rootTime <= 0f && rootKill) {
                HealthComponent healthComponent = owner.getComponent(HealthComponent.class);
                if (healthComponent != null) {
                    healthComponent.damage(healthComponent.getHealth());
                }
                rootKill = false;
            }
        }
    }

    public void tryAttack(Scene scene, String targetName) {
        TransformComponent ownerTransform = owner.getComponent(TransformComponent.class);
        if (ownerTransform == null)
            return;
        Vector2 myPos = ownerTransform.getPosition();
        RenderComponent renderComponent = owner.getComponent(RenderComponent.class);
        Vector2 myCenter = renderComponent != null ? myPos.add(new Vector2(renderComponent.getSize().x * 0.5f, renderComponent.getSize().y * 0.5f)) : myPos;

        /*
         * 没有子弹的攻击逻辑
         */
        List<GameObject> targets = scene.findGameObjectsByComponent(TransformComponent.class);
        for (GameObject obj : targets) {
            if (!obj.getName().equals(targetName))
                continue;

            TransformComponent targetTransform = obj.getComponent(TransformComponent.class);
            if (targetTransform == null)
                continue;

            Vector2 targetPos = targetTransform.getPosition();
            RenderComponent targetRenderComponent = obj.getComponent(RenderComponent.class);
            Vector2 targetCenter = targetRenderComponent != null ? targetPos.add(new Vector2(targetRenderComponent.getSize().x * 0.5f, targetRenderComponent.getSize().y * 0.5f)) : targetPos;
            float distance = myCenter.distance(targetCenter);
            if (distance <= this.attackRange) {
                // System.out.println(owner.getName() + " 攻击 " + targetName + "，造成 " + damage +
                        // " 点伤害");

                HealthComponent health = obj.getComponent(HealthComponent.class);
                if (health != null) {
                    health.damage(this.damage);
                }
            }
        }
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    public void applyBurn(float duration, float dps) {
        this.burnTime = Math.max(this.burnTime, duration);
        this.burnDps = dps;
    }

    public void applySlow(float duration, float factor) {
        this.slowTime = Math.max(this.slowTime, duration);
        this.slowFactor = factor;
    }

    public void applyRoot(float duration, boolean killAfter) {
        this.rootTime = Math.max(this.rootTime, duration);
        this.rootKill = killAfter;
    }

    public float getSlowFactor() {
        return slowFactor;
    }

    public float getSlowTime() {
        return slowTime;
    }

    public float getRootTime() {
        return rootTime;
    }
}