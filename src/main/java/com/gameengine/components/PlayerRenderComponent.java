package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class PlayerRenderComponent extends Component<PlayerRenderComponent> {
    @Override
    public void initialize() {}

    @Override
    public void update(float dt) {}

    @Override
    public void render() {
        Scene s = owner.getScene();
        if (s == null) return;
        Renderer renderer = s.getRenderer();
        TransformComponent transformComponent = owner.getComponent(TransformComponent.class);
        if (transformComponent == null) return;
        Vector2 basePosition = transformComponent.getPosition();
        renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
    }
}