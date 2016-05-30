package com.github.SquidPony.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.github.SquidPony.SquidShowcaseDemo;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new SquidShowcaseDemo(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "SquidLib Showcase Demo";
        config.width = 120 * 13;
        config.height = 34 * 26;
        for (int size : new int[] { 128, 64, 32, 16 }) {
            config.addIcon("libgdx" + size + ".png", FileType.Internal);
        }
        return config;
    }
}