package com.github.tommyettinger.demos.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.github.tommyettinger.demos.ColorfulDemo;

import static com.github.tommyettinger.demos.ColorfulDemo.*;

/** Launches the desktop (LWJGL2) application. */
public class DesktopLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new ColorfulDemo(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = ("ColorfulDemo");
        configuration.vSyncEnabled = (false);
        configuration.foregroundFPS = (0);
        configuration.width = (gridWidth * cellWidth);
        configuration.height = (gridHeight * cellHeight);
        configuration.fullscreen = true;
        configuration.addIcon("libgdx128.png", Files.FileType.Internal);
        configuration.addIcon("libgdx64.png", Files.FileType.Internal);
        configuration.addIcon("libgdx32.png", Files.FileType.Internal);
        configuration.addIcon("libgdx16.png", Files.FileType.Internal);
        configuration.forceExit = false;
        return configuration;
    }
}