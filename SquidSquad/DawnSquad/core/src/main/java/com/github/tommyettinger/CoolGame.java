package com.github.tommyettinger;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.lyze.gdxUnBox2d.BodyDefType;
import dev.lyze.gdxUnBox2d.Box2dPhysicsWorld;
import dev.lyze.gdxUnBox2d.GameObject;
import dev.lyze.gdxUnBox2d.UnBox;
import dev.lyze.gdxUnBox2d.behaviours.Box2dSoutBehaviour;
import dev.lyze.gdxUnBox2d.behaviours.box2d.Box2dBehaviour;

public class CoolGame extends Game {
    private Viewport viewport;

    private SpriteBatch batch;

    private UnBox unBox;

    @Override
    public void create() {
        viewport = new FitViewport(30, 10);
        viewport.getCamera().translate(-5, 0, 0);

        batch = new SpriteBatch();

        // Create an instance of the library, with no gravity
        unBox = new UnBox<>(new Box2dPhysicsWorld(new World(new Vector2(0, 0), true))); // Alternative if you don't want to use Box2d: new UnBox<>(new NoPhysicsWorld());

        // Create two game objects, those get automatically added to the libraries instance
        GameObject rightGo = new GameObject(unBox);
        GameObject leftGo = new GameObject(unBox);

        // Attach a Box2D body
        new Box2dBehaviour(BodyDefType.DynamicBody, rightGo);
        new Box2dBehaviour(BodyDefType.DynamicBody, leftGo);

        // Attach a logging behaviour to both of the game objects
        new Box2dSoutBehaviour("Right GO", false, rightGo);
        new Box2dSoutBehaviour("Left GO", false, leftGo);

//        // Attach a movement behaviour to both game objects
//        new MoveBehaviour(true, rightGo);
//        new MoveBehaviour(false, leftGo);
    }

    @Override
    public void render() {
        ScreenUtils.clear(.25f, .25f, .25f, 1);

        // Step through physics and update loops
        unBox.preRender(Gdx.graphics.getDeltaTime());

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Render the state
        batch.begin();
        unBox.render(batch);
        batch.end();

        // Debug render all Box2d bodies (if you are using a physics world)
        //debugRenderer.render(unBox.getPhysicsWorld().getWorld(), viewport.getCamera().combined);

        // Clean up render loop
        unBox.postRender();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}