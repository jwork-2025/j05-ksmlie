package com.gameengine.recording;

import com.gameengine.graphics.Renderer;
import com.gameengine.scene.Scene;
import com.gameengine.core.GameObject;
import com.gameengine.components.TransformComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.input.InputManager;
import com.gameengine.components.RenderComponent.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 录制服务：将运行过程写成 JSONL（header/input/keyframe）。
 *
 * 配置/策略：
 * - warmupNs：暖机期，避免刚启动就写入空关键帧。
 * - intervalNs：关键帧间隔，按固定周期写入状态快照。
 * - known：外观去重集，同名对象的渲染描述只写一次，后续只写位置。
 *
 * 生命周期：
 * - start(scene, renderer, input, name, version)：写 header 并启用录制。
 * - onFrame(deltaTime)：写 just-pressed 输入；暖机后按周期写 keyframe。
 * - stop()：写最后一帧并关闭存储。
 */
public class RecordingService {
    // 存储抽象
    private final RecordingStorage storage;
    private final RecordingConfig config;
    // 运行期引用（只读用于采集）
    private Scene scene;
    private Renderer renderer;
    private InputManager input;
    // 录制状态与时间戳
    private boolean recording;
    private long startNs;
    private long lastKeyframeNs;
    // 暖机时间与关键帧周期来源于config
    // 元信息写入到 header
    private String name;
    private String version;
    // 外观去重,同名对象的外观只写一次
    private final Set<String> known;

    public RecordingService(RecordingStorage storage) {
        this(storage, new RecordingConfig());
    }

    public RecordingService(RecordingStorage storage, RecordingConfig config) {
        this.storage = storage;
        this.config = config;
        this.known = new HashSet<>();
    }

    /**
     * 开始录制：写入 header 并初始化时间轴。
     */
    public void start(Scene scene, Renderer renderer, InputManager input, String name, String version) {
        try {
            this.scene = scene;
            this.renderer = renderer;
            this.input = input;
            this.name = name;
            this.version = version;
            this.storage.openForWrite(name);
            this.recording = true;
            this.startNs = System.nanoTime();
            this.lastKeyframeNs = startNs;
            String header = headerJson(nowMs());
            storage.appendLine(header);
            storage.flush();
        } catch (Exception e) {
            this.recording = false;
        }
    }

    /**
     * 帧回调：
     * - 先写 just-pressed 输入事件；
     * - 暖机结束后，按 intervalNs 周期写关键帧。
     */
    public void onFrame(float deltaTime) {
        if (!recording)
            return;
        long n = System.nanoTime();
        long t = nowMs(n);
        recordInputs(t);
        if (n - startNs < config.getWarmupNs())
            return;
        if (n - lastKeyframeNs >= config.getIntervalNs()) {
            String keyFrame = keyframeJson(t);
            try {
                storage.appendLine(keyFrame);
                storage.flush();
            } catch (Exception e) {
            }
            lastKeyframeNs = n;
        }
    }

    /**
     * 结束录制：强制写最后关键帧并关闭存储。
     */
    public void stop() {
        if (!recording)
            return;
        try {
            String keyFrame = keyframeJson(nowMs());
            storage.appendLine(keyFrame);
            storage.flush();
            storage.closeWrite();
        } catch (Exception e) {
        }
        recording = false;
        known.clear();
    }

    private long nowMs() {
        return nowMs(System.nanoTime());
    }

    private long nowMs(long n) {
        return (n - startNs) / 1_000_000L;
    }

    /**
     * 采集输入：只记录“刚按下”的按键以便回放驱动逻辑。
     */
    private void recordInputs(long t) {
        if (input == null)
            return;
        int[] keys = config.getKeys();
        for (int code : keys) {
            if (input.isKeyJustPressed(code)) {
                String line = inputJson(t, code);
                try {
                    storage.appendLine(line);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * header：窗口尺寸与版本等元信息，用于回放初始化。
     */
    private String headerJson(long t) {
        int w = renderer != null ? renderer.getWidth() : 0;
        int h = renderer != null ? renderer.getHeight() : 0;
        String v = version != null ? version : "";
        return "{" +
                "\"type\":\"header\"," +
                "\"t\":" + t + "," +
                "\"version\":\"" + v + "\"," +
                "\"width\":" + w + "," +
                "\"height\":" + h +
                "}";
    }

    private String inputJson(long t, int code) {
        return "{" +
                "\"type\":\"input\"," +
                "\"t\":" + t + "," +
                "\"key\":" + code +
                "}";
    }

    /**
     * keyframe：对象列表位置快照；首次出现时携带外观（rt/尺寸/颜色/图片路径）。
     */
    private String keyframeJson(long t) {
        List<GameObject> objs = scene != null ? scene.getGameObjects() : new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"keyframe\",");
        sb.append("\"t\":").append(t).append(",");
        sb.append("\"objects\":[");
        boolean first = true;
        for (GameObject obj : objs) {
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc == null)
                continue;
            RenderComponent rc = obj.getComponent(RenderComponent.class);
            float x = tc.getPosition().x;
            float y = tc.getPosition().y;
            String key = obj.getName();
            if (!first)
                sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"name\":\"").append(key).append("\",");
            sb.append("\"x\":").append(x).append(",");
            sb.append("\"y\":").append(y);
            if (!known.contains(key)) {
                if (rc != null) {
                    String rt = rc.getRenderType().name();
                    float w = rc.getSize().x;
                    float h = rc.getSize().y;
                    Color c = rc.getColor();
                    sb.append(",\"rt\":\"").append(rt).append("\"");
                    sb.append(",\"w\":").append(w);
                    sb.append(",\"h\":").append(h);
                    if (c != null) {
                        sb.append(",\"color\":[").append(c.r).append(",").append(c.g).append(",").append(c.b)
                                .append(",").append(c.a).append("]");
                    }
                    // 保存图片路径
                    String imgPath = rc.getImagePath();
                    if (imgPath != null && !imgPath.isEmpty()) {
                        // 转义反斜杠
                        String escapedPath = imgPath.replace("\\", "\\\\");
                        sb.append(",\"img\":\"").append(escapedPath).append("\"");
                    }
                }
            }
            sb.append("}");
        }
        sb.append("]");
        sb.append("}");
        for (GameObject obj : objs) {
            String key = obj.getName();
            known.add(key);
        }
        return sb.toString();
    }
}