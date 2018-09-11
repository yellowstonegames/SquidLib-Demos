package com.squidpony.samples.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Created by Tommy Ettinger on 9/11/2018.
 */
public class CustomConfig extends Lwjgl3ApplicationConfiguration {
    public String name;
    public Class<? extends ApplicationListener> launcher;
    public CustomConfig(Class<? extends ApplicationListener> launcherClass) {
        super();
        setTitle(name = launcherClass.getSimpleName());
        launcher = launcherClass;
        setWindowIcon(Files.FileType.Internal
                , "libgdx128.png"
                , "libgdx64.png"
                , "libgdx32.png"
                , "libgdx16.png");

    }
    

    @Override
    public String toString() {
        return name;
    }
}
