package com.squidpony.tools.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.squidpony.tools.KeyRemap;

/** Launches the desktop (LWJGL2) application. */
public class LwjglLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new KeyRemap(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = "KeyRemap";
        configuration.width = 160 * 8;
        configuration.height = 35 * 17;
        configuration.addIcon("libgdx128.png", Files.FileType.Internal);
        configuration.addIcon("libgdx64.png", Files.FileType.Internal);
        configuration.addIcon("libgdx32.png", Files.FileType.Internal);
        configuration.addIcon("libgdx16.png", Files.FileType.Internal);
        return configuration;
    }
}