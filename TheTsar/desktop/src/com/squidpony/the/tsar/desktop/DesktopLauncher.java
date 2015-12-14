package com.squidpony.the.tsar.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.squidpony.the.tsar.TsarGame;

public class DesktopLauncher {
	public static void main (String[] args) {
    	LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
   		config.width = 120 * 8;
    	config.height = 40 * 18;
    	config.addIcon("Tentacle-16.png", Files.FileType.Classpath);
    	config.addIcon("Tentacle-32.png", Files.FileType.Classpath);
    	config.addIcon("Tentacle-128.png", Files.FileType.Classpath);
    	new LwjglApplication(new TsarGame(), config);
    }
}
