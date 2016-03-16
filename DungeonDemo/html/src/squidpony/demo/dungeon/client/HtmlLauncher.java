package squidpony.demo.dungeon.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import squidpony.demo.dungeon.DungeonGame;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
                return new GwtApplicationConfiguration(50 * 22, 32 * 22);
        }

        @Override
        public ApplicationListener createApplicationListener () {
                return new DungeonGame();
        }
}