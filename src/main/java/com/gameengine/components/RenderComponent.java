package com.gameengine.components;

import javax.sound.sampled.Line;

import com.gameengine.components.RenderComponent.Color;
import com.gameengine.components.RenderComponent.RenderType;
import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.File;

/**
 * 渲染组件，负责对象的渲染
 */
public class RenderComponent extends Component<RenderComponent> {
    private Renderer renderer;
    private RenderType renderType;
    private Vector2 size;
    private Color color;
    private boolean visible;
    private String text;
    private int fontSize;

    public enum RenderType {
        RECTANGLE,
        CIRCLE,
        LINE,
        TEXT,
        IMAGE
    }

    public static class Color {
        public float r, g, b, a;

        public Color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public Color(float r, float g, float b) {
            this(r, g, b, 1.0f);
        }

        public float get_red() {
            return this.r;
        }

        public float get_green() {
            return this.g;
        }

        public float get_blue() {
            return this.b;
        }

        public float get_alpha() {
            return this.a;
        }
    }

    public RenderComponent() {
        this.renderType = RenderType.RECTANGLE;
        this.size = new Vector2(20, 20);
        this.color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        this.visible = true;
        this.text = "";
        this.fontSize = 20;
    }

    public RenderComponent(RenderType renderType, Vector2 size, Color color) {
        this.renderType = renderType;
        this.size = new Vector2(size);
        this.color = color;
        this.visible = true;
        this.text = "";
        this.fontSize = 20;
    }

    @Override
    public void initialize() {
        if (owner != null) {

        }
    }

    @Override
    public void update(float deltaTime) {

    }

    @Override
    public void render() {
        if (!visible || renderer == null) {
            return;
        }

        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }

        Vector2 position = transform.getPosition();

        switch (renderType) {
            case RECTANGLE:
                renderer.drawRect(position.x, position.y, size.x, size.y,
                        color.r, color.g, color.b, color.a);
                break;
            case CIRCLE:
                renderer.drawCircle(position.x + size.x / 2, position.y + size.y / 2,
                        size.x / 2, 16, color.r, color.g, color.b, color.a);
                break;
            case LINE:
                renderer.drawLine(position.x, position.y,
                        position.x + size.x, position.y + size.y,
                        color.r, color.g, color.b, color.a);
                break;
            case TEXT:
                if (text != null && !text.isEmpty()) {
                    renderer.drawText(position.x, position.y, text, fontSize,
                            color.r, color.g, color.b, color.a);
                }
                break;
            case IMAGE:
                if (image != null) {
                    renderer.drawImage(position.x, position.y, image, size.x, size.y);
                } else {
                    renderer.drawRect(position.x, position.y, size.x, size.y,
                            color.r, color.g, color.b, color.a);
                }
                break;
        }
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setColor(float r, float g, float b, float a) {
        this.color = new Color(r, g, b, a);
    }

    public void setSize(Vector2 size) {
        this.size = new Vector2(size);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    // Getters
    public RenderType getRenderType() {
        return renderType;
    }

    public Vector2 getSize() {
        return new Vector2(size);
    }

    public Color getColor() {
        return color;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getText() {
        return text;
    }

    public int getFontSize() {
        return fontSize;
    }


    private BufferedImage image;
    private String imagePath;

    public void setImage(BufferedImage image) {
        this.image = image;
        this.renderType = RenderType.IMAGE;
    }

    public void setImageFromResource(String resourcePath) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                this.image = img;
                this.imagePath = resourcePath;
                this.renderType = RenderType.IMAGE;
                is.close();
            } else {
                InputStream is2 = getClass().getResourceAsStream("/" + resourcePath);
                if (is2 != null) {
                    BufferedImage img = ImageIO.read(is2);
                    this.image = img;
                    this.imagePath = resourcePath;
                    this.renderType = RenderType.IMAGE;
                    is2.close();
                } else {
                    File file = new File(resourcePath);
                    if (file.exists()) {
                        BufferedImage img = ImageIO.read(file);
                        this.image = img;
                        this.imagePath = resourcePath;
                        this.renderType = RenderType.IMAGE;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public String getImagePath() {
        return imagePath;
    }

}
