package com.squidpony.demo.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.squidpony.shader.NorthernLights;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new NorthernLights(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("NorthernLights");
//        configuration.setWindowedMode(300, 300);
//        configuration.setWindowedMode(1920, 1080);
//        configuration.setDecorated(false);
        configuration.setWindowedMode(480, 480);
        configuration.useVsync(true);
        configuration.setForegroundFPS(240);
        configuration.setIdleFPS(30);
        configuration.disableAudio(true);
        ShaderProgram.prependFragmentCode = "#version 120\n";
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
