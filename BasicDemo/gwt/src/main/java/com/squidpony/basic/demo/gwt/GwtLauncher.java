package com.squidpony.basic.demo.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.squidpony.basic.demo.MainApplication;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(180 * 6, 55 * 12);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new MainApplication();
    }
}