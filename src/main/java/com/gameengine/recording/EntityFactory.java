package com.gameengine.recording;

import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.components.TransformComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.RenderComponent.RenderType;
import com.gameengine.components.RenderComponent.Color;
import com.gameengine.math.Vector2;

public class EntityFactory {
    public static GameObject create(Renderer renderer, String name, String rt, float w, float h, float[] color, float x, float y, String imagePath) {
        GameObject obj = new GameObject(name);
        obj.addComponent(new TransformComponent(new Vector2(x, y)));
        RenderType type = rt != null ? RenderType.valueOf(rt) : RenderType.RECTANGLE;
        float ww = w > 0 ? w : 20f;
        float hh = h > 0 ? h : 20f;
        Color c = color != null && color.length == 4 ? new Color(color[0], color[1], color[2], color[3]) : new Color(1f, 1f, 1f, 1f);
        RenderComponent rc = obj.addComponent(new RenderComponent(type, new Vector2(ww, hh), c));
        rc.setRenderer(renderer);
        // 如果有图片路径，加载图片
        if (imagePath != null && !imagePath.isEmpty()) {
            rc.setImageFromResource(imagePath);
        }
        return obj;
    }
    
    // 保留旧的方法以保持兼容性
    public static GameObject create(Renderer renderer, String name, String rt, float w, float h, float[] color, float x, float y) {
        return create(renderer, name, rt, w, h, color, x, y, null);
    }
}