package com.squidpony.globe.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.squidpony.globe.GlobeDemo;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(300, 300);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new GlobeDemo();
    }
}
