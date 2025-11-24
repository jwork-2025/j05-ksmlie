#!/bin/bash

# 简单编译脚本
echo "编译游戏引擎..."

# 创建输出目录
mkdir -p build/classes

# 编译所有Java文件
javac -encoding UTF-8 -d build/classes \
    -cp . \
    src/main/java/com/gameengine/math/Vector2.java \
    src/main/java/com/gameengine/input/InputManager.java \
    src/main/java/com/gameengine/core/Component.java \
    src/main/java/com/gameengine/core/GameObject.java \
    src/main/java/com/gameengine/components/AttackComponent.java \
    src/main/java/com/gameengine/components/HealthComponent.java \
    src/main/java/com/gameengine/components/TransformComponent.java \
    src/main/java/com/gameengine/components/PhysicsComponent.java \
    src/main/java/com/gameengine/components/BulletComponent.java \
    src/main/java/com/gameengine/components/HudComponent.java \
    src/main/java/com/gameengine/components/PlayerRenderComponent.java \
    src/main/java/com/gameengine/components/RenderComponent.java \
    src/main/java/com/gameengine/graphics/Renderer.java \
    src/main/java/com/gameengine/core/GameEngine.java \
    src/main/java/com/gameengine/core/GameLogic.java \
    src/main/java/com/gameengine/scene/Scene.java \
    src/main/java/com/gameengine/recording/RecordingStorage.java \
    src/main/java/com/gameengine/recording/FileRecordingStorage.java \
    src/main/java/com/gameengine/recording/RecordingConfig.java \
    src/main/java/com/gameengine/recording/RecordingService.java \
    src/main/java/com/gameengine/recording/EntityFactory.java \
    src/main/java/com/gameengine/recording/ReplayScene.java \
    src/main/java/com/gameengine/example/MenuScene.java \
    src/main/java/com/gameengine/example/GameExample.java

if [ $? -eq 0 ]; then
    echo "编译成功！"
    echo "运行游戏: java -cp build/classes com.gameengine.example.GameExample"
else
    echo "编译失败！"
    exit 1
fi
