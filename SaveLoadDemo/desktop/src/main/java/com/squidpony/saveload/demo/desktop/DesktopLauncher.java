package com.squidpony.saveload.demo.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.squidpony.saveload.demo.MainApplication;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new MainApplication(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "SaveLoadDemo";
        config.vSyncEnabled = false;
        config.foregroundFPS = 0;
        config.width = MainApplication.gridWidth * MainApplication.cellWidth;
        config.height = (MainApplication.gridHeight + MainApplication.bonusHeight) * MainApplication.cellHeight;
        for (int size : new int[] { 128, 64, 32, 16 }) {
            config.addIcon("Tentacle-" + size + ".png", FileType.Internal);
        }
        return config;
    }
}