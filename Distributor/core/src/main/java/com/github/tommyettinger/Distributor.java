package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import squidpony.ArrayTools;
import squidpony.squidgrid.gui.gdx.DefaultResources;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.TextCellFactory;
import squidpony.squidmath.*;
import text.formic.Stringf;

import java.util.Arrays;

import static com.badlogic.gdx.graphics.GL20.GL_POINTS;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Distributor extends ApplicationAdapter {

    private SpriteBatch batch;
    private float[][] backgrounds;
    private ImmediateModeRenderer20 renderer;
    private final int[] amounts = new int[512];
    private GWTRNG rng;
    private double a, b;
    private TextCellFactory font;
    private ScreenViewport viewport;


    @Override
    public void create() {
        Coord.expandPoolTo(512, 512);
        font = new TextCellFactory().font(DefaultResources.getCozyFont());
        rng = new GWTRNG(1234567890);
        a = 2.0;
        b = 5.0;
        batch = new SpriteBatch();
        viewport = new ScreenViewport();
        renderer = new ImmediateModeRenderer20(512 * 520, false, true, 0);
        backgrounds = new float[512][520];
    }
    public double nextExclusiveDouble (){
        final long bits = rng.nextLong();
        return NumberTools.longBitsToDouble(1022L - Long.numberOfTrailingZeros(bits) << 52 | bits >>> 12);
    }

    public void update() {
        Arrays.fill(amounts, 0);
        ArrayTools.fill(backgrounds, SColor.FLOAT_WHITE);
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
        font.bmpFont.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", 100, 500);

        for (int i = 0; i < 0x40000; i++) {
            amounts[Math.min(Noise.fastFloor((Math.pow(1.0 - Math.pow(nextExclusiveDouble(), bb), aa)) * 512), 511)]++;
        }
        for (int i = 0; i < 512; i++) {
            float color = (i & 63) == 0
                    ? -0x1.c98066p126F // CW Azure
                    : -0x1.d08864p126F; // CW Sapphire
            for (int j = Math.max(0, 519 - (amounts[i] >> 2)); j < 520; j++) {
                backgrounds[i][j] = color;
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 8; j < 520; j += 32) {
                backgrounds[i][j] = -0x1.7677e8p125F;
            }
        }
    }

    @Override
    public void render() {
        // standard clear the background routine for libGDX
        ScreenUtils.clear(1f, 1f, 1f, 1f);
        Camera camera = viewport.getCamera();
        camera.update();
        renderer.begin(camera.combined, GL_POINTS);
        for (int x = 0; x < 512; x++) {
            for (int y = 0; y < 520; y++) {
                renderer.color(backgrounds[x][y]);
                renderer.vertex(x, 519 - y, 0);
            }
        }
        renderer.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        update();
        batch.end();

    }
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);
        viewport.apply(true);
    }

}