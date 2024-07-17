package com.github.yellowstonegames.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.yellowstonegames.DungeonDemo;

import static com.github.yellowstonegames.DungeonDemo.*;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
	public static void main(String[] args) {
		if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
		createApplication();
	}

	private static Lwjgl3Application createApplication() {
		return new Lwjgl3Application(new DungeonDemo(), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(PLACE_WIDTH * CELL_WIDTH, PLACE_HEIGHT * CELL_HEIGHT);
		config.disableAudio(true);
		config.setForegroundFPS(0);
		config.useVsync(true);
		return config;
	}
}