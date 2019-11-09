package com.squidpony.samples.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.squidpony.samples.*;

/**
 * Created by Tommy Ettinger on 9/11/2018.
 */
public class Sampler extends ApplicationAdapter {
    public VisSelectBox<CustomConfig> choices;
    public VisTextButton button;
    public Stage stage;
    @Override
    public void create() {
        super.create();
        stage = new Stage();
        VisUI.setSkipGdxVersionCheck(true);
        VisUI.load();
        choices = new VisSelectBox<>();
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
        button = new VisTextButton("Launch!", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final CustomConfig sel = choices.getSelected();
                System.out.println(sel.name);
                ((Lwjgl3Application)Gdx.app).newWindow(sel.instantiate(), sel);
            }
        });
        Table table = new Table(VisUI.getSkin());
        table.setFillParent(true);
        table.add(new Separator()).height(32f);
        table.row();
        table.add(choices);
        table.add(new Separator());
        table.add(button);
        table.row();
        table.add(new Separator()).expandY();
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
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
