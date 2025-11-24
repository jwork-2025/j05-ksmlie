package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.scene.Scene;

public class GameLauncher {
    public static void main(String[] args) {
        GameEngine engine = new GameEngine(1600, 900, "KSMLIE Menu");
        Scene menu = new MenuScene(engine);
        menu.setRenderer(engine.getRenderer());
        engine.setScene(menu);
        engine.run();
    }
}