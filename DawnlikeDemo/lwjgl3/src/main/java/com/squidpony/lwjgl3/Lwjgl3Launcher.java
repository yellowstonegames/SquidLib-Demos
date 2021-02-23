package com.squidpony.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
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
                if(!UIUtils.isMac) // focus is handled differently by MacOS
                    Gdx.app.postRunnable(() -> maximized(false));
            }

            @Override
            public void focusGained() {
                if(!UIUtils.isMac) // focus is handled differently by MacOS
                    Gdx.app.postRunnable(() -> maximized(true));
            }

            @Override
            public void iconified(boolean isIconified) {
                if(isIconified)
                    Gdx.app.getApplicationListener().pause();
                else {
                    Gdx.app.getApplicationListener().resume();
                }
            }

            @Override
            public boolean closeRequested() {
                Gdx.app.exit();
                return true;
            }
        });

        //// these are a different way of handling alt-tab better.
//        configuration.setDecorated(false);
//        configuration.setAutoIconify(true);

        //// useful to know if something's wrong in a shader.
        //// you should remove the next line for a release.
        configuration.enableGLDebugOutput(true, System.out);
        ShaderProgram.prependVertexCode = "#version 110\n";
        ShaderProgram.prependFragmentCode = "#version 110\n";
        // these are constants in the main game class; they should match your
        // initial viewport size in pixels before it gets resized to fullscreen.
        configuration.setWindowedMode(gridWidth * cellWidth, gridHeight * cellHeight);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
