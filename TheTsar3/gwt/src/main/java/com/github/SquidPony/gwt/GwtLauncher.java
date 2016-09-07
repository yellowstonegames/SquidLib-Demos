package com.github.SquidPony.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.SquidPony.TsarGame;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(90 * 12, 30  * 20);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new TsarGame();
    }
}