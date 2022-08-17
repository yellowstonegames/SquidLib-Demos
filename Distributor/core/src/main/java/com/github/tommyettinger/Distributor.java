package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import squidpony.squidmath.GWTRNG;
import squidpony.squidmath.MathExtras;
import text.formic.Stringf;

import java.util.Arrays;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Distributor extends ApplicationAdapter {

    private SpriteBatch batch;
    private ImmediateModeRenderer20 renderer;
    private final int[] amounts = new int[512];
    private GWTRNG rng;
    private double a, b;
    private BitmapFont font;
    private ScreenViewport viewport;


    @Override
    public void create() {
        font = new BitmapFont(Gdx.files.internal("Cozette.fnt"));
        font.setColor(Color.BLACK);
        rng = new GWTRNG(1234567890);
        a = 2.0;
        b = 5.0;
        batch = new SpriteBatch();
        viewport = new ScreenViewport();
        renderer = new ImmediateModeRenderer20(512 * 3, false, true, 0);
    }
    public final double nextExclusiveDouble (){
        final long bits = rng.nextLong();
        return NumberUtils.longBitsToDouble(1022L - Long.numberOfTrailingZeros(bits) << 52 | bits >>> 12);
    }

    @Override
    public void render() {
        ScreenUtils.clear(1f, 1f, 1f, 1f);
        Camera camera = viewport.getCamera();
        camera.update();
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) a += 0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) a += -0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) b += 0.5 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) b += -0.5 * Gdx.graphics.getDeltaTime();
        if (a <= 0) a = 1e-9;
        if (b <= 0) b = 1e-9;

        double aa = 1.0 / a;
        double bb = 1.0 / b;
        Arrays.fill(amounts, 0);
        for (int i = 0; i < 0x40000; i++) {
            amounts[Math.min((int) ((Math.pow(1.0 - Math.pow(nextExclusiveDouble(), bb), aa)) * 512), 511)]++;
        }
        renderer.begin(camera.combined, GL20.GL_LINES);
        for (int x = 0; x < 512; x++) {
            float color = (x & 63) == 0
                    ? -0x1.c98066p126F // CW Azure
                    : -0x1.d08864p126F; // CW Sapphire
            renderer.color(color);
            renderer.vertex(x, 0, 0);
            renderer.color(color);
            renderer.vertex(x, (amounts[x] >> 2), 0);
        }
        for (int j = 8; j < 520; j += 32) {
            renderer.color(-0x1.7677e8p125F); // CW Bright Red
            renderer.vertex(0, j, 0);
            renderer.color(-0x1.7677e8p125F); // CW Bright Red
            renderer.vertex(10, j, 0);
        }
        renderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, Stringf.format("Kumaraswamy with a=%1.3f, b=%1.3f; mean=%1.3f", a, b,
                (MathExtras.factorial(aa) * MathExtras.gamma(b) * b) / MathExtras.factorial(aa + b)), 100, 522);
        font.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", 100, 500);
        batch.end();
    }
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);
        viewport.apply(true);
    }

}