package com.github.yellowstonegames.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.tommyettinger.DaybreakDemo;

import static com.github.tommyettinger.DaybreakDemo.*;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
	@Override
	public GwtApplicationConfiguration getConfig() {
		// Resizable application, uses available space in browser with no padding:
//			GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
//			cfg.padVertical = 0;
//			cfg.padHorizontal = 0;
//			return cfg;
		// If you want a fixed size application, comment out the above resizable section,
		// and uncomment below:
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(shownWidth * cellWidth, shownHeight * cellHeight);
		cfg.disableAudio = true;
		return cfg;
	}

	@Override
	public ApplicationListener createApplicationListener() {
		return new DaybreakDemo(System.currentTimeMillis());
	}

//	@Override
//	public void onModuleLoad() {
//		com.badlogic.gdx.graphics.g2d.freetype.gwt.FreetypeInjector.inject(super::onModuleLoad);
//	}
}
