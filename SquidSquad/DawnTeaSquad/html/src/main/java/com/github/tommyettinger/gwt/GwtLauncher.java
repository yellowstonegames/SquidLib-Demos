package com.github.tommyettinger.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.tommyettinger.DawnSquad;

import static com.github.tommyettinger.DawnSquad.*;
import static com.github.tommyettinger.DawnSquad.cellHeight;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {
            // Resizable application, uses available space in browser with no padding:
            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(gridWidth * cellWidth, gridHeight * cellHeight);
            cfg.disableAudio = true;
            return cfg;
        }

        @Override
        public ApplicationListener createApplicationListener () {
            return new DawnSquad();
        }
}
