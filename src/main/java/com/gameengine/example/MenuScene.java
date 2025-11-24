package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.recording.ReplayScene;

import java.util.ArrayList;
import java.util.List;

public class MenuScene extends Scene {
    private final GameEngine engine;
    private Renderer renderer;
    private InputManager input;
    private RecordingStorage storage;
    private List<String> recordings;
    private int selected;
    private enum State { ROOT, REPLAY }
    private State state;

    public MenuScene(GameEngine engine) {
        super("MenuScene");
        this.engine = engine;
        this.recordings = new ArrayList<>();
        this.selected = 0;
        this.state = State.ROOT;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.setRenderer(this.renderer);
        this.input = InputManager.getInstance();
        this.storage = new FileRecordingStorage();
        refreshRecordings();
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (state == State.ROOT) {
            if (input.isKeyJustPressed(49) || input.isKeyJustPressed(97)) { // 1 / Numpad1
                Scene s = GameExample.createBattleScene(engine);
                s.setRenderer(engine.getRenderer());
                engine.setScene(s);
                return;
            }
            if (input.isKeyJustPressed(50) || input.isKeyJustPressed(98)) { // 2 / Numpad2
                refreshRecordings();
                state = State.REPLAY;
                return;
            }
        } else if (state == State.REPLAY) {
            if (input.isKeyJustPressed(38)) { // Up
                if (!recordings.isEmpty()) selected = (selected - 1 + recordings.size()) % recordings.size();
            }
            if (input.isKeyJustPressed(40)) { // Down
                if (!recordings.isEmpty()) selected = (selected + 1) % recordings.size();
            }
            if (input.isKeyJustPressed(10)) { // Enter
                if (!recordings.isEmpty()) {
                    String name = recordings.get(selected);
                    ReplayScene rs = new ReplayScene(name, storage);
                    rs.setRenderer(engine.getRenderer());
                    engine.setScene(rs);
                    return;
                }
            }
            if (input.isKeyJustPressed(27)) { // Esc
                state = State.ROOT;
                return;
            }
        }
    }

    @Override
    public void render() {
        if (renderer != null) renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.08f, 0.08f, 0.12f, 1f);
        super.render();
        if (renderer == null) return;
        if (state == State.ROOT) {
            renderer.drawTextCentered("KSMLIE - Menu", 36, 1, 1, 1, 1);
            renderer.drawTextCenteredOffset("1: Start Game", 28, 48f, 0.8f, 0.8f, 1f, 1);
            renderer.drawTextCenteredOffset("2: Load Replay", 28, 96f, 0.8f, 1f, 0.8f, 1);
        } else if (state == State.REPLAY) {
            renderer.drawTextCentered("Select Recording", 32, 1, 1, 1, 1);
            float off = 64f;
            for (int i = 0; i < recordings.size(); i++) {
                String name = recordings.get(i);
                boolean sel = (i == selected);
                float r = sel ? 1f : 0.8f;
                float g = sel ? 1f : 0.8f;
                float b = sel ? 0.3f : 0.8f;
                renderer.drawTextCenteredOffset(name, 24, off + i * 32f, r, g, b, 1f);
            }
            if (recordings.isEmpty()) {
                renderer.drawTextCenteredOffset("No recordings found", 24, 96f, 1f, 0.6f, 0.6f, 1f);
            } else {
                renderer.drawTextCenteredOffset("Up/Down to select, Enter to load, Esc to back", 20, off + recordings.size() * 32f + 40f, 0.9f, 0.9f, 0.9f, 1f);
            }
        }
    }

    private void refreshRecordings() {
        try {
            this.recordings = storage.listRecordings();
            if (this.recordings == null) this.recordings = new ArrayList<>();
            this.selected = 0;
        } catch (Exception e) {
            this.recordings = new ArrayList<>();
            this.selected = 0;
        }
    }
}