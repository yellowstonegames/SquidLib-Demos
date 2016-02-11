package com.squidpony.basic.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.squidpony.basic.BasicDemo;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
                return new GwtApplicationConfiguration(80 * 8, 40 * 18);
        }

        @Override
        public ApplicationListener createApplicationListener () {
                return new BasicDemo();
        }
}