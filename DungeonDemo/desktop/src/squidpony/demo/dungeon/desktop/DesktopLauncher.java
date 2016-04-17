package squidpony.demo.dungeon.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import squidpony.demo.dungeon.DungeonGame;

public class DesktopLauncher {
	public static void main (String[] args) {
    	LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
   		config.width = 50 * 26;
    	config.height = 20 * 40;
    	config.addIcon("Tentacle-16.png", Files.FileType.Internal);
    	config.addIcon("Tentacle-32.png", Files.FileType.Internal);
    	config.addIcon("Tentacle-128.png", Files.FileType.Internal);
		config.title = "R is for Rebuild!";
    	new LwjglApplication(new DungeonGame(), config);
    }
}
