package com.github.yellowstonegames.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.github.tommyettinger.DaybreakDemo;

import static com.github.tommyettinger.DaybreakDemo.*;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
	public static void main(String[] args) {
		// Needed for macOS support, but also Windows with non-ASCII usernames.
		if (StartupHelper.startNewJvmIfRequired()) return;
		// Graal stuff
		org.lwjgl.system.Library.initialize();
		org.lwjgl.system.ThreadLocalUtil.setupEnvData();

		createApplication();
	}

	private static Lwjgl3Application createApplication() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.disableAudio(true);
		configuration.setResizable(true);
		configuration.useVsync(true);
		configuration.setInitialBackgroundColor(new Color(0f, 0f, 0f, 1f)); // Color.BLACK gets edited to use Oklab.
		//// Limits FPS to the refresh rate of the currently active monitor.
		configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
		//// If you remove the above line and set Vsync to false, you may get unlimited FPS, which can be
		//// useful for testing performance, but can also be very stressful to some hardware.
		//// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
		//// That would use the following two settings:
//		configuration.useVsync(false);
//		configuration.setForegroundFPS(0);

		configuration.setTitle("SquidSquad Daybreak Demo");
		//// useful to know if something's wrong in a shader.
		//// you should remove or comment out the next line for a release.
//		configuration.enableGLDebugOutput(true, System.out);
		ShaderProgram.prependVertexCode = "#version 110\n";
		ShaderProgram.prependFragmentCode = "#version 110\n";
		// these are constants in the main game class; they should match your
		// initial viewport size in pixels before it gets resized to fullscreen.
		configuration.setWindowedMode(shownWidth * cellWidth, shownHeight * cellHeight);

		String env = System.getenv("seed");
		long seed;
		if(env == null) seed = System.currentTimeMillis();
		else seed = Long.parseLong(env);
		return new Lwjgl3Application(new DaybreakDemo(seed), configuration);
	}
}