package com.squidpony.samples.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.squidpony.samples.*;

/**
 * Created by Tommy Ettinger on 9/11/2018.
 */
public class Sampler extends ApplicationAdapter {
    public SelectBox<CustomConfig> choices;
    public TextButton button;
    public Stage stage;
    @Override
    public void create() {
        super.create();
        stage = new Stage();
        Skin skin = new Skin(Gdx.files.internal("skin/neon-ui.json"), new TextureAtlas("skin/neon-ui.atlas"));
        choices = new SelectBox<CustomConfig>(skin);
        choices.setItems(
                TsarDemo.config,
                SparseLightingDemo.config,
                DetailedWorldMapDemo.config,
                RotatingWorldMapDemo.config,
                LocalMapDemo.config,
                WorldMapViewDemo.config,
                WildMapDemo.config,
                WorldMapTextDemo.config,
                WorldWildMapDemo.config
        );
        button = new TextButton("Launch!", skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final CustomConfig sel = choices.getSelected();
                System.out.println(sel.name);
                ((Lwjgl3Application)Gdx.app).newWindow(sel.instantiate(), sel);
            }
        });
        Table table = new Table(skin);
        table.setFillParent(true);
        table.top().row();
        table.row().height(64f);
        table.add(choices).height(48f).spaceRight(32f);
        table.add(button).height(48f);
        table.row();
        table.row().expandY();
        table.bottom();
        table.row().colspan(2);
        table.add("Choose a demo from the dropdown and click Launch.");
        table.row().colspan(2);
        table.add("Thanks to Raymond Buckley for the Neon skin.");
        table.row().colspan(2);
        table.add("https://ray3k.wordpress.com");
//        table.pack();
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
        // use this during testing to auto-launch a demo
//        ((Lwjgl3Application)Gdx.app).newWindow(WorldMapTextDemo.config.instantiate(), WorldMapTextDemo.config);
    }

    @Override
    public void render() {
        super.render();
        Gdx.gl.glClearColor(0.333f, 0.25f, 0.4f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
        stage.act();
    }
}
