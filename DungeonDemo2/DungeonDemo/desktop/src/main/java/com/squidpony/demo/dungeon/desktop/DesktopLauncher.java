package com.squidpony.demo.dungeon.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.squidpony.demo.dungeon.DungeonGame;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new DungeonGame(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = "DungeonDemo";
   		configuration.width = 75 * 15;
    	configuration.height = 30 * 27;
		configuration.title = "R is for Rebuild!";
        for (int size : new int[] { 128, 64, 32, 16 }) {
            configuration.addIcon("Tentacle-" + size + ".png", FileType.Internal);
        }
        return configuration;
    }
}