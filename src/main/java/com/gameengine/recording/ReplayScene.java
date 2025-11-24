package com.gameengine.recording;

import com.gameengine.scene.Scene;
import com.gameengine.graphics.Renderer;
import com.gameengine.core.GameObject;
import com.gameengine.components.TransformComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.PlayerRenderComponent;
import com.gameengine.components.RenderComponent.Color;
import com.gameengine.components.RenderComponent.RenderType;
import com.gameengine.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class ReplayScene extends Scene {
    private final RecordingStorage storage;
    private final String recordingName;
    private Renderer renderer;
    private List<Frame> frames;
    private long durationMs;
    private long elapsedMs;
    private Map<String, GameObject> entities;
    // 缓存已知实体名字的外观信息，用于创建同名新实体
    private Map<String, EntityState> appearanceCache;

    public ReplayScene(String name, RecordingStorage storage) {
        super("ReplayScene");
        this.storage = storage;
        this.recordingName = name;
        this.frames = new ArrayList<>();
        this.entities = new HashMap<>();
        this.appearanceCache = new HashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = getRenderer();
        List<String> lines;
        try {
            lines = storage.readAll(recordingName);
        } catch (Exception e) {
            lines = new ArrayList<>();
        }
        int w = renderer != null ? renderer.getWidth() : 0;
        int h = renderer != null ? renderer.getHeight() : 0;
        for (String line : lines) {
            if (line.contains("\"type\":\"header\"")) {
                int wi = line.indexOf("\"width\":");
                int hi = line.indexOf("\"height\":");
                if (wi >= 0) {
                    int c = nextComma(line, wi);
                    w = parseInt(line.substring(wi + 8, c));
                }
                if (hi >= 0) {
                    int c = nextCommaOrBrace(line, hi);
                    h = parseInt(line.substring(hi + 9, c));
                }
            } else if (line.contains("\"type\":\"keyframe\"")) {
                Frame f = parseFrame(line);
                frames.add(f);
            }
        }
        durationMs = frames.isEmpty() ? 0 : frames.get(frames.size() - 1).t;
        List<EntityState> initObjects = frames.isEmpty() ? Collections.<EntityState>emptyList() : frames.get(0).objects;
        int enemyCount = 0;
        int bulletCount = 0;
        int decorationCount = 0;
        for (EntityState s : initObjects) {
            // 如果这个实体有完整的外观信息，缓存它
            if (s.rt != null && !appearanceCache.containsKey(s.name)) {
                appearanceCache.put(s.name, s);
            }
            
            // 为每个实体生成唯一ID
            String entityId = s.name;
            if ("Enemy".equals(s.name)) {
                entityId = "Enemy_" + (enemyCount++);
            } else if ("Bullet".equals(s.name)) {
                entityId = "Bullet_" + (bulletCount++);
            } else if ("Decoration".equals(s.name)) {
                entityId = "Decoration_" + (decorationCount++);
            }
            
            GameObject obj;
            if ("Player".equalsIgnoreCase(s.name)) {
                obj = new GameObject("Player");
                obj.addComponent(new TransformComponent(new Vector2(s.x, s.y)));
                obj.addComponent(new PlayerRenderComponent());
            } else {
                // 使用缓存的外观信息（如果当前状态没有完整信息）
                EntityState appearance = s.rt != null ? s : appearanceCache.get(s.name);
                if (appearance != null) {
                    obj = EntityFactory.create(getRenderer(), s.name, appearance.rt, appearance.w, appearance.h, appearance.color, s.x, s.y, appearance.img);
                } else {
                    obj = EntityFactory.create(getRenderer(), s.name, s.rt, s.w, s.h, s.color, s.x, s.y, s.img);
                }
            }
            addGameObject(obj);
            entities.put(entityId, obj);
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (frames.isEmpty()) return;
        elapsedMs += (long) (deltaTime * 1000f);
        if (elapsedMs > durationMs) elapsedMs = durationMs;
        
        int i = findFrameIndex(elapsedMs);
        int j = Math.min(i + 1, frames.size() - 1);
        Frame a = frames.get(i);
        Frame b = frames.get(j);
        float w = a.t == b.t ? 0f : Math.min(1f, (elapsedMs - a.t) / (float) (b.t - a.t));
        
        // 获取当前所有游戏对象
        List<GameObject> currentObjects = this.getGameObjects();
        
        // 按名字分组当前帧的实体状态
        Map<String, List<EntityState>> frameEntitiesByName = new HashMap<>();
        for (EntityState es : a.objects) {
            frameEntitiesByName.computeIfAbsent(es.name, k -> new ArrayList<>()).add(es);
        }
        
        // 按名字分组当前游戏对象
        Map<String, List<GameObject>> currentObjectsByName = new HashMap<>();
        for (GameObject obj : currentObjects) {
            currentObjectsByName.computeIfAbsent(obj.getName(), k -> new ArrayList<>()).add(obj);
        }
        
        // 处理每种名字的实体
        for (Map.Entry<String, List<EntityState>> entry : frameEntitiesByName.entrySet()) {
            String name = entry.getKey();
            List<EntityState> frameStates = entry.getValue();
            List<GameObject> currentObjs = currentObjectsByName.getOrDefault(name, new ArrayList<>());
            
            // 缓存外观信息（如果有的话）
            for (EntityState es : frameStates) {
                if (es.rt != null && !appearanceCache.containsKey(name)) {
                    appearanceCache.put(name, es);
                }
            }
            
            // 如果当前对象数量少于帧中需要的数量，创建新对象
            while (currentObjs.size() < frameStates.size()) {
                GameObject newObj;
                if ("Player".equalsIgnoreCase(name)) {
                    newObj = new GameObject("Player");
                    newObj.addComponent(new TransformComponent(new Vector2(0, 0)));
                    newObj.addComponent(new PlayerRenderComponent());
                } else {
                    EntityState appearance = appearanceCache.get(name);
                    if (appearance != null) {
                        newObj = EntityFactory.create(getRenderer(), name, appearance.rt, appearance.w, appearance.h, appearance.color, 0, 0, appearance.img);
                    } else {
                        newObj = EntityFactory.create(getRenderer(), name, null, 20, 20, null, 0, 0, null);
                    }
                }
                addGameObject(newObj);
                currentObjs.add(newObj);
            }
            
            // 如果当前对象数量多于帧中需要的数量，移除多余的对象
            while (currentObjs.size() > frameStates.size()) {
                GameObject toRemove = currentObjs.remove(currentObjs.size() - 1);
                toRemove.setActive(false);
            }
            
            // 更新位置（插值）
            for (int idx = 0; idx < frameStates.size(); idx++) {
                EntityState ea = frameStates.get(idx);
                // 在b帧中查找对应的实体
                EntityState eb = ea;
                List<EntityState> bFrameStates = new ArrayList<>();
                for (EntityState es : b.objects) {
                    if (es.name.equals(name)) {
                        bFrameStates.add(es);
                    }
                }
                if (idx < bFrameStates.size()) {
                    eb = bFrameStates.get(idx);
                }
                
                float x = ea.x + (eb.x - ea.x) * w;
                float y = ea.y + (eb.y - ea.y) * w;
                
                GameObject obj = currentObjs.get(idx);
                TransformComponent tc = obj.getComponent(TransformComponent.class);
                if (tc != null) {
                    tc.setPosition(new Vector2(x, y));
                }
            }
        }
        
        // 移除帧中不存在的对象
        for (GameObject obj : currentObjects) {
            if (!frameEntitiesByName.containsKey(obj.getName())) {
                obj.setActive(false);
            }
        }
    }

    @Override
    public void render() {
        if (renderer != null) renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.05f, 0.05f, 0.08f, 1f);
        super.render();
    }

    private int findFrameIndex(long t) {
        int lo = 0, hi = frames.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (frames.get(mid).t <= t) lo = mid + 1; else hi = mid;
        }
        if (lo > 0 && frames.get(lo).t > t) return lo - 1;
        return lo;
    }

    private Map<String, EntityState> toMap(List<EntityState> list) {
        Map<String, EntityState> m = new HashMap<>();
        for (EntityState s : list) m.put(s.name, s);
        return m;
    }

    

    private Frame parseFrame(String line) {
        Frame f = new Frame();
        int ti = line.indexOf("\"t\":");
        int tc = nextComma(line, ti);
        f.t = parseLong(line.substring(ti + 4, tc));
        int oi = line.indexOf("\"objects\":[");
        int end = line.lastIndexOf("]");
        String arr = oi >= 0 && end > oi ? line.substring(oi + 10, end) : "";
        List<EntityState> list = new ArrayList<>();
        int p = 0;
        while (true) {
            int ni = arr.indexOf("\"name\":\"", p);
            if (ni < 0) break;
            int ns = ni + 9;
            int ne = arr.indexOf("\"", ns);
            String name = ne > ns ? arr.substring(ns, ne) : "";
            int xi = arr.indexOf("\"x\":", ne);
            int xc = nextComma(arr, xi);
            float x = parseFloat(arr.substring(xi + 4, xc));
            int yi = arr.indexOf("\"y\":", xc);
            int yc = nextCommaOrBrace(arr, yi);
            float y = parseFloat(arr.substring(yi + 4, yc));
            String rt = null;
            float w = 0f;
            float h = 0f;
            float[] color = null;
            String img = null;
            int rti = arr.indexOf("\"rt\":\"" , yc);
            if (rti >= 0) {
                int rs = rti + 6;
                int re = arr.indexOf("\"", rs);
                rt = re > rs ? arr.substring(rs, re) : null;
                int wi = arr.indexOf("\"w\":", re);
                int wc = nextComma(arr, wi);
                w = parseFloat(arr.substring(wi + 4, wc));
                int hi = arr.indexOf("\"h\":", wc);
                int hc = nextComma(arr, hi);
                h = parseFloat(arr.substring(hi + 4, hc));
                int ci = arr.indexOf("\"color\":[", hc);
                if (ci >= 0) {
                    int ce = arr.indexOf("]", ci);
                    String colors = arr.substring(ci + 9, ce);
                    String[] parts = colors.split(",");
                    if (parts.length == 4) {
                        color = new float[]{parseFloat(parts[0]), parseFloat(parts[1]), parseFloat(parts[2]), parseFloat(parts[3])};
                    }
                    p = ce;
                } else {
                    p = hc;
                }
                // 解析图片路径
                int imgi = arr.indexOf("\"img\":\"" , p);
                if (imgi >= 0 && imgi < arr.indexOf("}", p)) {
                    int imgs = imgi + 7;
                    int imge = arr.indexOf("\"", imgs);
                    if (imge > imgs) {
                        img = arr.substring(imgs, imge);
                        // 反转义反斜杠
                        img = img.replace("\\\\", "\\");
                        p = imge;
                    }
                }
            } else {
                p = yc;
            }
            EntityState s = new EntityState();
            s.name = name;
            s.x = x;
            s.y = y;
            s.rt = rt;
            s.w = w;
            s.h = h;
            s.color = color;
            s.img = img;
            list.add(s);
        }
        f.objects = list;
        return f;
    }

    private int nextComma(String s, int from) {
        int i = s.indexOf(",", from + 1);
        if (i < 0) i = s.length();
        return i;
    }

    private int nextCommaOrBrace(String s, int from) {
        int i = s.indexOf(",", from + 1);
        int j = s.indexOf("}", from + 1);
        if (i < 0) return j < 0 ? s.length() : j;
        if (j < 0) return i;
        return Math.min(i, j);
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }

    private float parseFloat(String s) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return 0f; }
    }

    private static class Frame {
        long t;
        List<EntityState> objects;
    }

    private static class EntityState {
        String name;
        float x;
        float y;
        String rt;
        float w;
        float h;
        float[] color;
        String img;
    }
}