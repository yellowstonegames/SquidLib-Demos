package com.squidpony.saveload.demo.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.squidpony.saveload.demo.MainApplication;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(
                MainApplication.gridWidth * MainApplication.cellWidth, 
                MainApplication.gridHeight * MainApplication.cellHeight);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new MainApplication();
    }
}