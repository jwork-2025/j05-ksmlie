# 葫芦娃大战妖精 - Java 游戏引擎

一个基于 Java Swing 的 2D 游戏引擎，实现了葫芦娃与妖精的对战游戏，支持**游戏录制与回放**功能。

## 快速开始

```bash
./run.sh
```

## 📹 录制回放系统

本项目的核心特性，实现了完整的游戏录制与回放功能。

### 系统架构

采用**关键帧（Keyframe）** + **输入事件（Input）**的混合录制方式：

```
录制: 游戏运行 → RecordingService → 采集数据 → JSONL文件
回放: JSONL文件 → ReplayScene → 解析数据 → 重建游戏状态
```

### 录制内容

#### 1. Header（头信息）
```json
{"type": "header", "t": 0, "version": "v1", "width": 1600, "height": 900}
```

#### 2. Input（输入事件）
```json
{"type": "input", "t": 523, "key": 87}
```

#### 3. Keyframe（关键帧）
定期记录所有游戏对象的状态：
```json
{
  "type": "keyframe",
  "t": 1000,
  "objects": [
    {"name": "Player", "x": 400.0, "y": 300.0},
    {
      "name": "Enemy",
      "x": 523.45,
      "y": 167.89,
      "rt": "RECTANGLE",
      "w": 40.0,
      "h": 40.0,
      "color": [1.0, 0.5, 0.0, 1.0],
      "img": "E:\\path\\to\\snake.png"
    }
  ]
}
```

### 核心技术

#### 1. 外观去重机制
同名对象的外观信息（渲染类型、尺寸、颜色、图片）只在首次出现时记录：

```java
private final Set<String> known;  // 已记录外观的对象名称

if (!known.contains(key)) {
    // 首次出现，记录完整外观
    sb.append(",\"rt\":\"").append(rt).append("\"");
    sb.append(",\"w\":").append(w);
    sb.append(",\"color\":[").append(color).append("]");
    sb.append(",\"img\":\"").append(imagePath).append("\"");
}
known.add(key);
```

#### 2. 外观缓存机制
回放时缓存首次出现的完整外观信息，用于创建后续同名实体：

```java
private Map<String, EntityState> appearanceCache;

// 缓存完整外观
if (entity.rt != null && !appearanceCache.containsKey(name)) {
    appearanceCache.put(name, entity);
}

// 创建新实体时使用缓存
EntityState appearance = appearanceCache.get(name);
obj = EntityFactory.create(renderer, name, 
    appearance.rt, appearance.w, appearance.h, 
    appearance.color, x, y, appearance.img);
```

这解决了多个同名实体（如 20 个 Enemy）只有第一个有外观信息的问题。

#### 3. 按名字分组匹配
解决对象顺序不稳定的问题：

```java
// 按名字分组
Map<String, List<EntityState>> entitiesByName = new HashMap<>();
for (EntityState es : frame.objects) {
    entitiesByName.computeIfAbsent(es.name, k -> new ArrayList<>()).add(es);
}

// 对每组分别处理，动态调整数量
for (Map.Entry<String, List<EntityState>> entry : entitiesByName.entrySet()) {
    String name = entry.getKey();
    List<EntityState> frameStates = entry.getValue();
    List<GameObject> currentObjs = currentObjectsByName.get(name);
    
    // 数量不足则创建，过多则移除
    while (currentObjs.size() < frameStates.size()) {
        addGameObject(createEntity(name, appearance));
    }
    while (currentObjs.size() > frameStates.size()) {
        removeGameObject(currentObjs.remove(currentObjs.size() - 1));
    }
    
    // 在组内按索引匹配并插值
    for (int i = 0; i < frameStates.size(); i++) {
        updatePosition(currentObjs.get(i), frameStates.get(i), weight);
    }
}
```

#### 4. 平滑插值
在相邻关键帧之间线性插值：

```java
float weight = (currentTime - frameA.time) / (frameB.time - frameA.time);
float x = entityA.x + (entityB.x - entityA.x) * weight;
float y = entityA.y + (entityB.y - entityA.y) * weight;
```

#### 5. 特殊实体处理

**Player**：使用自定义 `PlayerRenderComponent`，回放时特殊处理：
```java
if ("Player".equalsIgnoreCase(name)) {
    obj = new GameObject("Player");
    obj.addComponent(new PlayerRenderComponent());  // 自定义渲染
}
```

**图片路径序列化**：Enemy 等使用图片，需序列化路径并处理转义：
```java
// 录制
String escapedPath = imgPath.replace("\\", "\\\\");
sb.append(",\"img\":\"").append(escapedPath).append("\"");

// 回放
img = img.replace("\\\\", "\\");
rc.setImageFromResource(img);
```

### 存储格式

- **JSONL**（JSON Lines）：每行一条独立 JSON
- 易于追加写入，逐行解析，内存友好
- 保存路径：`recordings/battle_<timestamp>.jsonl`

### 录制配置

```java
RecordingConfig config = new RecordingConfig();
config.setWarmupNs(500_000_000L);   // 暖机 500ms
config.setIntervalNs(500_000_000L); // 关键帧间隔 500ms
config.setKeys(new int[]{87, 65, 83, 68, ...}); // 监听按键
```

## 游戏特性

### 七种子弹

- 🔴 **红色**：伤害 +80%
- 🟠 **橙色**：速度 +50%
- 🟡 **黄色**：连锁攻击
- 🟢 **绿色**：持续伤害
- 🔵 **蓝色**：冰冻减速
- 🟣 **靛紫**：隐形子弹
- 🟣 **紫色**：斩杀子弹

### 战斗系统

- ECS 组件架构
- 自动瞄准最近敌人
- 敌人 AI：追踪玩家 + 相互避让
- 生命、攻击、碰撞系统