package com.gameengine.graphics;

import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 渲染器
 */
public class Renderer extends JFrame {
    private int width;
    private int height;
    private String title;
    private GamePanel gamePanel;
    private InputManager inputManager;

    public Renderer(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.inputManager = InputManager.getInstance();

        initialize();
    }

    private void initialize() {
        setTitle(title);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setupInput();

        setVisible(true);
        
        // 确保窗口获得焦点
        requestFocusInWindow();
        gamePanel.requestFocusInWindow();
    }

    private void setupInput() {
        // 键盘输入
        KeyAdapter ka = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                inputManager.onKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                inputManager.onKeyReleased(e.getKeyCode());
            }
        };
        addKeyListener(ka);
        gamePanel.addKeyListener(ka);

        // 鼠标输入
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int button = e.getButton() - 1;
                inputManager.onMousePressed(button);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int button = e.getButton() - 1;
                inputManager.onMouseReleased(button);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                inputManager.onMouseMoved(e.getX(), e.getY());
            }
        });

        setFocusable(true);
        requestFocus();
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }

    /**
     * 开始渲染帧
     */
    public void beginFrame() {
        gamePanel.clear();
    }

    /**
     * 结束渲染帧
     */
    public void endFrame() {
        gamePanel.repaint();
    }

    /**
     * 绘制矩形
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        gamePanel.addDrawable(new RectDrawable(x, y, width, height, r, g, b, a));
    }

    /**
     * 绘制圆形
     */
    public void drawCircle(float x, float y, float radius, int segments, float r, float g, float b, float a) {
        gamePanel.addDrawable(new CircleDrawable(x, y, radius, r, g, b, a));
    }

    /**
     * 绘制线条
     */
    public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        gamePanel.addDrawable(new LineDrawable(x1, y1, x2, y2, r, g, b, a));
    }

    /**
     * 绘制文本
     */
    public void drawText(float x, float y, String text, int fontSize, float r, float g, float b, float a) {
        gamePanel.addDrawable(new TextDrawable(x, y, text, fontSize, r, g, b, a, 1.0f, 5.0f, 5.0f));
    }

    public void drawTextCentered(String text, int fontSize, float r, float g, float b, float a) {
        gamePanel.addDrawable(new TextCenterDrawable(width, height, text, fontSize, r, g, b, a, 0f));
    }

    public void drawTextCenteredOffset(String text, int fontSize, float offsetY, float r, float g, float b, float a) {
        gamePanel.addDrawable(new TextCenterDrawable(width, height, text, fontSize, r, g, b, a, offsetY));
    }

    public void drawImage(float x, float y, BufferedImage image, float width, float height) {
        if (image != null) {
            gamePanel.addDrawable(new ImageDrawable(x, y, image, width, height));
        }
    }

    /**
     * 检查窗口是否应该关闭
     */
    public boolean shouldClose() {
        return !isVisible();
    }

    /**
     * 处理事件
     */
    public void pollEvents() {
        // Swing自动处理事件
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        dispose();
    }

    // Getters
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    /**
     * 游戏面板类
     */
    private class GamePanel extends JPanel {
        private List<Drawable> drawables = new ArrayList<>();

        public GamePanel() {
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.BLACK);
        }

        public void clear() {
            drawables.clear();
        }

        public void addDrawable(Drawable drawable) {
            drawables.add(drawable);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Drawable drawable : drawables) {
                drawable.draw(g2d);
            }
        }
    }

    /**
     * 可绘制对象接口
     */
    private interface Drawable {
        void draw(Graphics2D g);
    }

    /**
     * 矩形绘制类
     */
    private static class RectDrawable implements Drawable {
        private float x, y, width, height;
        private Color color;

        public RectDrawable(float x, float y, float width, float height, float r, float g, float b, float a) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = new Color(r, g, b, a);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect((int) x, (int) y, (int) width, (int) height);
        }
    }

    /**
     * 圆形绘制类
     */
    private static class CircleDrawable implements Drawable {
        private float x, y, radius;
        private Color color;

        public CircleDrawable(float x, float y, float radius, float r, float g, float b, float a) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = new Color(r, g, b, a);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        }
    }

    /**
     * 线条绘制类
     */
    private static class LineDrawable implements Drawable {
        private float x1, y1, x2, y2;
        private Color color;

        public LineDrawable(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = new Color(r, g, b, a);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
    }

    /**
     * 文本绘制类
     */
    private static class TextDrawable implements Drawable {
        private float x, y;
        private String text;
        private int fontSize;
        private Color color;
        private float alpha;// 透明度
        private float liveTime;// 剩余时间
        private float maxLiveTime;

        public TextDrawable(float x, float y, String text, int fontSize, float r, float g, float b, float a,
                float alpha, float liveTime, float maxLiveTime) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.fontSize = fontSize;
            this.color = new Color(r, g, b, a);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.setFont(new Font("Arial", Font.BOLD, fontSize));
            g.drawString(text, x, y);
        }
    }

    private static class TextCenterDrawable implements Drawable {
        private float panelW, panelH, offsetY;
        private String text;
        private int fontSize;
        private Color color;

        public TextCenterDrawable(float panelW, float panelH, String text, int fontSize, float r, float g, float b, float a, float offsetY) {
            this.panelW = panelW;
            this.panelH = panelH;
            this.text = text;
            this.fontSize = fontSize;
            this.color = new Color(r, g, b, a);
            this.offsetY = offsetY;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            Font font = new Font("Arial", Font.BOLD, fontSize);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics(font);
            int textW = fm.stringWidth(text);
            int x = (int) ((panelW - textW) / 2f);
            int y = (int) (((panelH - fm.getHeight()) / 2f) + fm.getAscent() + offsetY);
            g.drawString(text, x, y);
        }
    }

    private static class ImageDrawable implements Drawable {
        private float x, y, width, height;
        private BufferedImage image;

        public ImageDrawable(float x, float y, BufferedImage image, float width, float height) {
            this.x = x;
            this.y = y;
            this.image = image;
            this.width = width;
            this.height = height;
        }

        @Override
        public void draw(Graphics2D g) {
            g.drawImage(image, (int) x, (int) y, (int) width, (int) height, null);
        }
    }
}
