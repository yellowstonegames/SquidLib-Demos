package com.github.tommyettinger.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.tommyettinger.ShaderDemo;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new ShaderDemo(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("WarmMildShader");
        configuration.setWindowedMode(404, 600);
        configuration.setIdleFPS(10);
        configuration.useVsync(true);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
//        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
//            @Override
//            public void filesDropped(String[] files) {
//                if (files != null && files.length > 0) {
//                    if (files[0].endsWith(".png") || files[0].endsWith(".jpg") || files[0].endsWith(".jpeg"))
//                        app.load(files[0]);
//                }
//            }
//        });

        return configuration;
    }
}