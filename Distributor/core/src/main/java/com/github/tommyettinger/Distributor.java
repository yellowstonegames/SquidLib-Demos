package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
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
    private final int[] amounts = new int[512];
    private GWTRNG rng;
    private double a, b;
    private TextCellFactory font;


    @Override
    public void create() {
        Coord.expandPoolTo(512, 512);
        font = new TextCellFactory().font(DefaultResources.getCozyFont());
        rng = new GWTRNG(1234567890);
        a = 2.0;
        b = 5.0;
        batch = new FilterBatch();
        stage = new Stage(new StretchViewport(512, 540), batch);
        layers = new SparseLayers(512, 520, 1, 1, font);
        layers.setDefaultForeground(SColor.WHITE);
        stage.addActor(layers);
    }
    public double nextExclusiveDouble (){
        final long bits = rng.nextLong();
        return NumberTools.longBitsToDouble(1022L - Long.numberOfTrailingZeros(bits) << 52 | bits >>> 12);
    }

    public void update() {
        Arrays.fill(amounts, 0);
        ArrayTools.fill(layers.backgrounds, 0f);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) a += 0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) a += -0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) b += 0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) b += -0.5 * Gdx.graphics.getDeltaTime();
        if (a <= 0) a = 1e-9;
        if (b <= 0) b = 1e-9;
        font.bmpFont.setColor(SColor.BLACK);
        double aa = 1.0 / a;
        double bb = 1.0 / b;
        font.bmpFont.draw(batch, Stringf.format("Kumaraswamy with a=%1.3f, b=%1.3f; mean=%1.3f", a, b,
                (MathExtras.factorial(aa) * MathExtras.gamma(b) * b) / MathExtras.factorial(aa + b)), 100, 522);

        for (int i = 0; i < 0x40000; i++) {
            amounts[Math.min(Noise.fastFloor((Math.pow(1.0 - Math.pow(nextExclusiveDouble(), bb), aa)) * 512), 511)]++;
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