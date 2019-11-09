package com.squidpony.samples.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Created by Tommy Ettinger on 9/11/2018.
 */
public abstract class CustomConfig extends Lwjgl3ApplicationConfiguration {
    public String name;
    public CustomConfig(String name) {
        super();
        setTitle(this.name = name);
        setWindowIcon(Files.FileType.Internal
                , "libgdx128.png"
                , "libgdx64.png"
                , "libgdx32.png"
                , "libgdx16.png");
    }
    
    public abstract ApplicationListener instantiate();
    

    @Override
    public String toString() {
        return name;
    }
}
