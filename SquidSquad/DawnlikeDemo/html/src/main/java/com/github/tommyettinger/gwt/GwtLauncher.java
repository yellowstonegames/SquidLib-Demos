package com.github.tommyettinger.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.tommyettinger.DawnlikeDemo;

import static com.github.tommyettinger.DawnlikeDemo.*;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
		@Override
		public GwtApplicationConfiguration getConfig () {
			// Resizable application, uses available space in browser
//			return new GwtApplicationConfiguration(true);
			// Fixed size application:
			return new GwtApplicationConfiguration(gridWidth * cellWidth * 2, gridHeight * cellHeight * 2);
		}

		@Override
		public ApplicationListener createApplicationListener () { 
			return new DawnlikeDemo();
		}
}
