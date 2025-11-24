package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.recording.ReplayScene;
import java.util.Collections;
import java.util.List;

public class GameReplay {
    public static void main(String[] args) {
        try {
            RecordingStorage storage = new FileRecordingStorage();
            List<String> list = storage.listRecordings();
            if (list == null || list.isEmpty()) return;
            Collections.sort(list);
            String latest = list.get(list.size() - 1);
            GameEngine engine = new GameEngine(1600, 900, "回放");
            ReplayScene scene = new ReplayScene(latest, storage);
            scene.setRenderer(engine.getRenderer());
            engine.setScene(scene);
            engine.run();
        } catch (Exception e) {
        }
    }
}