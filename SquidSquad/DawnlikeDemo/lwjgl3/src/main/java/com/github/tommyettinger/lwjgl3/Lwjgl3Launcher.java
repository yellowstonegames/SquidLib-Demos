package com.github.tommyettinger.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.github.tommyettinger.DawnlikeDemo;

import static com.github.tommyettinger.DawnlikeDemo.*;

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
		configuration.setResizable(true);
		configuration.useVsync(true);
		configuration.setForegroundFPS(120); // upper bound in case vsync fails
		configuration.setTitle("SquidSquad Dawnlike Demo");
		//// useful to know if something's wrong in a shader.
		//// you should remove the next line for a release.
		configuration.enableGLDebugOutput(true, System.out);
		ShaderProgram.prependVertexCode = "#version 110\n";
		ShaderProgram.prependFragmentCode = "#version 110\n";
		// these are constants in the main game class; they should match your
		// initial viewport size in pixels before it gets resized to fullscreen.
		configuration.setWindowedMode(gridWidth * cellWidth, gridHeight * cellHeight);
		return configuration;
	}

}