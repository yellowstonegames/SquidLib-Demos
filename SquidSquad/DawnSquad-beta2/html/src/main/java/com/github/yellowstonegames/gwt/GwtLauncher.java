package com.github.yellowstonegames.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.yellowstonegames.SunriseSquad;

import static com.github.yellowstonegames.SunriseSquad.*;

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
        GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(SHOWN_WIDTH * CELL_WIDTH, SHOWN_HEIGHT * CELL_HEIGHT);
        cfg.disableAudio = true;
        return cfg;
    }

    @Override
    public ApplicationListener createApplicationListener() {
		return new SunriseSquad(0L);
//        return new SunriseSquad(System.currentTimeMillis());
    }
}
