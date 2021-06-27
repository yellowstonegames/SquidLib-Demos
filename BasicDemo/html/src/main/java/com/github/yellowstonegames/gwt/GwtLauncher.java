package com.github.yellowstonegames.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.yellowstonegames.BasicDemo;

import static com.github.yellowstonegames.BasicDemo.*;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(gridWidth * cellWidth, (gridHeight + bonusHeight) * cellHeight);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new BasicDemo();
    }
}