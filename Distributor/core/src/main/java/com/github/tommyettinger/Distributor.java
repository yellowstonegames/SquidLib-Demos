package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.ArrayTools;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidmath.*;
import text.formic.Stringf;

import java.util.Arrays;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Distributor extends ApplicationAdapter {

    private FilterBatch batch;
    private SparseLayers layers;
    private Stage stage;
    private int[] amounts = new int[512];
    private MoonwalkRNG rng;
    private KumaraswamyDistribution kd;
    private long seed = 1L;
    private long startTime;
    private TextCellFactory font;


    @Override
    public void create() {
        startTime = TimeUtils.millis();
        Coord.expandPoolTo(512, 512);
        font = new TextCellFactory().font(DefaultResources.getCozyFont());
        rng = new MoonwalkRNG(1234567890);
        kd = new KumaraswamyDistribution(2.0, 5.0);
        batch = new FilterBatch();
        stage = new Stage(new StretchViewport(512, 540), batch);
        layers = new SparseLayers(512, 520, 1, 1, font);
        layers.setDefaultForeground(SColor.WHITE);
        stage.addActor(layers);
    }

    public void update() {
        Arrays.fill(amounts, 0);
        ArrayTools.fill(layers.backgrounds, 0f);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) kd.setA(kd.getA() + 0.5 * Gdx.graphics.getDeltaTime());
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) kd.setA(kd.getA() - 0.5 * Gdx.graphics.getDeltaTime());
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) kd.setB(kd.getB() + 0.5 * Gdx.graphics.getDeltaTime());
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) kd.setB(kd.getB() - 0.5 * Gdx.graphics.getDeltaTime());
        font.bmpFont.setColor(SColor.BLACK);
        font.bmpFont.draw(batch, Stringf.format("Kumaraswamy with a=%1.3f, b=%1.3f", kd.getA(), kd.getB()), 100, 522);
        for (int i = 0; i < 0x40000; i++) {
            amounts[Noise.fastFloor(kd.nextDouble(rng) * 512)]++;
        }
        for (int i = 0; i < 512; i++) {
            float color = (i & 63) == 0
                    ? -0x1.c98066p126F // CW Azure
                    : -0x1.d08864p126F; // CW Sapphire
            for (int j = Math.max(0, 519 - (amounts[i] >> 2)); j < 520; j++) {
                layers.backgrounds[i][j] = color;
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 8; j < 520; j += 32) {
                layers.backgrounds[i][j] = -0x1.7677e8p125F;
            }
        }
    }

    @Override
    public void render() {
        // standard clear the background routine for libGDX
        ScreenUtils.clear(1f, 1f, 1f, 1f);
        Camera camera = stage.getViewport().getCamera();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        update();
        stage.getRoot().draw(batch, 1);
        batch.end();
    }

}