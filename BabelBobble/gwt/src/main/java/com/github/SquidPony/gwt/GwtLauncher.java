package com.github.SquidPony.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.SquidPony.BabelBobble;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(1000, 700);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new BabelBobble();
    }
}