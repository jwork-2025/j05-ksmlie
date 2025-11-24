package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.Renderer;
import com.gameengine.scene.Scene;

public class HudComponent extends Component<HudComponent> {
    private GameLogic logic;

    public HudComponent(GameLogic logic) {
        this.logic = logic;
    }

    @Override
    public void initialize() {}

    @Override
    public void update(float dt) {}

    @Override
    public void render() {
        Scene s = owner.getScene();
        if (s == null) return;
        Renderer renderer = s.getRenderer();
        int panelW = renderer.getWidth();

        int kills = logic != null ? logic.getKillCount() : 0;
        renderer.drawText(panelW - 270, 50, "Kills: " + kills, 40, 1f, 1f, 0f, 1f);

        GameObject player = null;
        for (GameObject obj : s.getGameObjects()) {
            if (obj.getName().equals("Player") && obj.isActive()) { player = obj; break; }
        }
        if (player != null) {
            HealthComponent healthComponent = player.getComponent(HealthComponent.class);
            if (healthComponent != null) {
                float max = healthComponent.getMaxHealth();
                float current = healthComponent.getHealth();
                float ratio = Math.max(0f, Math.min(1f, current / Math.max(1f, max)));
                float marginRight = 20f;
                float topMargin = 30f;
                int barW = 200;
                int barH = 20;
                int barX = panelW - (int) marginRight - barW - 50;
                int barY = (int) (topMargin + 40);
                float r = ratio < 0.5f ? 1.0f : 2 - 2 * ratio;
                float g = ratio > 0.5f ? 1.0f : 2 * ratio;
                float b = 0.0f;
                renderer.drawRect(barX, barY, barW, barH, 0.2f, 0.2f, 0.2f, 0.9f);
                renderer.drawRect(barX, barY, (int) (barW * ratio), barH, r, g, b, 1.0f);
                String pct = "HP: " + (int) (ratio * 100) + "%";
                renderer.drawText(barX + 15, barY + 70, pct, 40, 1, 1, 1, 1);
            }
        }

        int idx = logic != null ? logic.getCurrentColorIndex() : 1;
        String colorNameText = colorName(idx);
        float[] colorRgb = colorRgb(idx);
        renderer.drawText(0, 50, "Bullet: " + colorNameText, 40, colorRgb[0], colorRgb[1], colorRgb[2], 1);
    }

    private String colorName(int idx) {
        switch (idx) {
            case 1: return "RED";
            case 2: return "ORANGE";
            case 3: return "YELLOW";
            case 4: return "GREEN";
            case 5: return "BLUE";
            case 6: return "INDIGO";
            case 7: return "PURPLE";
            default: return "RED";
        }
    }

    private float[] colorRgb(int idx) {
        switch (idx) {
            case 1: return new float[]{1f, 0f, 0f};
            case 2: return new float[]{1f, 0.5f, 0f};
            case 3: return new float[]{1f, 1f, 0f};
            case 4: return new float[]{0f, 1f, 0f};
            case 5: return new float[]{0f, 0.6f, 1f};
            case 6: return new float[]{0.3f, 0.3f, 1f};
            case 7: return new float[]{0.6f, 0f, 1f};
            default: return new float[]{1f, 1f, 1f};
        }
    }
}