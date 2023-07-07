package com.github.yellowstonegames.gwt;

import static com.github.tommyettinger.DawnSquad.cellHeight;
import static com.github.tommyettinger.DawnSquad.cellWidth;
import static com.github.tommyettinger.DawnSquad.gridHeight;
import static com.github.tommyettinger.DawnSquad.gridWidth;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.tommyettinger.DawnSquad;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
		@Override
		public GwtApplicationConfiguration getConfig () {
			// Resizable application, uses available space in browser with no padding:
//			GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
//			cfg.padVertical = 0;
//			cfg.padHorizontal = 0;
//			return cfg;
			// If you want a fixed size application, comment out the above resizable section,
			// and uncomment below:
			GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(gridWidth * cellWidth, gridHeight * cellHeight);
			cfg.disableAudio = true;
			return cfg;
		}

		@Override
		public ApplicationListener createApplicationListener () {
			return new DawnSquad();
		}
}
