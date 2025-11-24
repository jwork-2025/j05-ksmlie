package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class HealthComponent extends Component<HealthComponent> {
    private int maxHealth;
    private int currentHealth;

    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public void damage(int num) {
        this.currentHealth -= num;
        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
        }
    }

    public void heal(int num) {// todo:可扩展的回血机制
        this.currentHealth += num;
        if (this.currentHealth > this.maxHealth)
            this.currentHealth = this.maxHealth;
    }

    public int getHealth() {
        return this.currentHealth;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public boolean isDead() {
        return (this.currentHealth <= 0);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaTime) {// todo:生命恢复
    }

    @Override
    public void render() {
        if (owner == null) return;
        Scene s = owner.getScene();
        if (s == null) return;
        Renderer renderer = s.getRenderer();
        if (renderer == null) return;
        TransformComponent transformComponent = owner.getComponent(TransformComponent.class);
        if (transformComponent == null) return;
        Vector2 pos = transformComponent.getPosition();
        float ratio = Math.max(0f, Math.min(1f, currentHealth / (float) Math.max(1, maxHealth)));
        float r = ratio < 0.5f ? 1.0f : 2 - 2 * ratio;
        float g = ratio > 0.5f ? 1.0f : 2 * ratio;
        float b = 0.0f;
        int fontSize = owner.getName().equals("Enemy") ? 10 : 16;
        renderer.drawText(pos.x - 20, pos.y - 30, "HP: " + currentHealth, fontSize, r, g, b, 1f);
    }
}
