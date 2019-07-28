package com.squidpony.demo.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.squidpony.demo.NorthernLights;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(960, 640);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new NorthernLights();
    }
}