package com.squidpony.samples.lwjgl3;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;


/** Launches the desktop (LWJGL) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartOnFirstThreadHelper.startNewJvmIfRequired()) return; // don't execute any code
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Sampler(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Sampler");
//// not sure if this is needed.
//        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 3, 2);
        configuration.setWindowedMode(400, 500);
        configuration.useVsync(true);
        configuration.setForegroundFPS(300);
        configuration.disableAudio(true);
        configuration.setWindowIcon(FileType.Internal
                , "libgdx128.png"
                , "libgdx64.png"
                , "libgdx32.png"
                , "libgdx16.png");
        return configuration;
    }
}