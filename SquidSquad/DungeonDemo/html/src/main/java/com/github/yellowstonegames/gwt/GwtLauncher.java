package com.github.yellowstonegames.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.yellowstonegames.DungeonDemo;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {
            // Resizable application, uses available space in browser with no padding:
//            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
//            cfg.padVertical = 0;
//            cfg.padHorizontal = 0;
//            return cfg;
            // If you want a fixed size application, comment out the above resizable section,
            // and uncomment below:
            return new GwtApplicationConfiguration(
                DungeonDemo.SHOWN_WIDTH * DungeonDemo.CELL_WIDTH,
                DungeonDemo.SHOWN_HEIGHT * DungeonDemo.CELL_HEIGHT);
        }

        @Override
        public ApplicationListener createApplicationListener () {
            return new DungeonDemo();
        }
}
