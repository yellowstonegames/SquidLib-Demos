package com.github.tommyettinger.demos.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.tommyettinger.demos.BasicDemo;

import static com.github.tommyettinger.demos.BasicDemo.*;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(
            gridWidth * cellWidth, gridHeight * cellHeight);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new BasicDemo();
    }
}