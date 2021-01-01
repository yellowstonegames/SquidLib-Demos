package com.squidpony.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.squidpony.DawnlikeDemo;

import static com.squidpony.DawnlikeDemo.*;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new DawnlikeDemo(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("DawnlikeDemo");
        configuration.setResizable(true);
        configuration.useVsync(true);
        configuration.setForegroundFPS(120);
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void iconified(boolean isIconified) {
                if(isIconified)
                    Gdx.app.getApplicationListener().pause();
                else {
                    Gdx.app.getApplicationListener().resume();
                }
            }

            @Override
            public void maximized(boolean isMaximized) {
                if (isMaximized) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    Gdx.graphics.setVSync(true);
                } else {
                    Gdx.graphics.setWindowedMode(gridWidth * cellWidth, gridHeight * cellHeight);
                    Gdx.graphics.setVSync(false);
                }
            }

            @Override
            public void focusLost() {
                Gdx.app.postRunnable(() -> maximized(false));
            }

            @Override
            public void focusGained() {
                Gdx.app.postRunnable(() -> maximized(true));
            }

            @Override
            public boolean closeRequested() {
                Gdx.app.exit();
                return true;
            }
        });
//        configuration.setDecorated(false);
//        configuration.setAutoIconify(true);
        configuration.enableGLDebugOutput(true, System.out);
        ShaderProgram.prependVertexCode = "#version 110\n";
        ShaderProgram.prependFragmentCode = "#version 110\n";
        configuration.setWindowedMode(gridWidth * cellWidth, gridHeight * cellHeight);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
