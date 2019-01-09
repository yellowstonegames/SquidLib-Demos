package com.squidpony.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.TimeUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class NorthernLights extends ApplicationAdapter {
    private int seed;
    private SpriteBatch batch;
    private Texture tiny;
    private long startTime;
    private int width, height;
    @Override
    public void create() {
        super.create();
        startTime = TimeUtils.millis();
        long state = TimeUtils.nanoTime() - startTime;
        // Sarong's DiverRNG.determine(), may be used in SquidLib later.
        seed = (int) (((state = ((state << ((state & 31) + 5)) ^ state ^ 0xDB4F0B9175AE2165L) * 0xD1B54A32D192ED03L)
                ^ (state >>> ((state >>> 60) + 16))) * 0x369DEA0F31A53F85L >>> 32);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch = new SpriteBatch();
        batch.disableBlending();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pm.drawPixel(0, 0, -1);
        tiny = new Texture(pm);
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.width = width;
        this.height = height;
    }
    public static float swayRandomized(int seed, float value)
    {
        final int floor = value >= 0f ? (int) value : (int) value - 1;
        final float start = (((seed += floor * 0x6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529)) * 0x0.ffffffp-31f,
                end = (((seed += 0x6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529)) * 0x0.ffffffp-31f;
        value -= floor;
        value *= value * (3f - 2f * value);
        return (1f - value) * start + value * end;
    }
    // cubic, not quintic like in SquidLib.
    public static float swayTight(float value)
    {
        int floor = (value >= 0f ? (int) value : (int) value - 1);
        value -= floor;
        floor &= 1;
        return value * value * (3f - 2f * value) * (-floor | 1) + floor;
//        return value * value * value * (value * (value * 6f - 15f) + 10f) * (-floor | 1) + floor;
    }

    /**
     * Based on CosmicNumbering in SquidLib.
     * @param c0 connection 0
     * @param c1 connection 1
     * @param c2 connection 2
     * @return a continuous noise-like value between -0.5 and 1.5
     */
    private float cosmic(float c0, float c1, float c2)
    {
        float sum = swayRandomized(seed, c2 + c0);
        sum += swayRandomized(seed, sum + c0 + c1);
        sum += swayRandomized(seed, sum + c1 + c2);
        return sum * 0.33333333333f + 0.5f;
    }
    
    @Override
    public void render() {
        super.render();
        final int tm = (int) TimeUtils.timeSinceMillis(startTime);
        final float ftm = tm * 0x3p-14f;
        final float s0 = swayRandomized(0x9E3779B9, ftm - 1.11f) * 0.025f;
        final float c0 = swayRandomized(0xC13FA9A9, ftm - 1.11f) * 0.025f;
        final float s1 = swayRandomized(0xD1B54A32, ftm + 1.41f) * 0.025f;
        final float c1 = swayRandomized(0xDB4F0B91, ftm + 1.41f) * 0.025f;
        final float s2 = swayRandomized(0xE19B01AA, ftm + 2.61f) * 0.025f;
        final float c2 = swayRandomized(0xE60E2B72, ftm + 2.61f) * 0.025f;
        float conn0, conn1, conn2;
        batch.begin();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                conn0 = tm * (0x1.cac084p-12f) + x * c0 - y * s0;
                conn1 = tm * (0x1.26e978p-11f) - x * c1 + y * s1;
                conn2 = tm * (0x1.a9fbe8p-11f) + x * c2 + y * s2;

                conn0 = cosmic(conn0, conn1, conn2);
                conn1 = cosmic(conn0, conn1, conn2);
                conn2 = cosmic(conn0, conn1, conn2);
                batch.setColor(swayTight(conn0), swayTight(conn1), swayTight(conn2), 1f);
                batch.draw(tiny, x, y);
            }
        }
        batch.end();
    }
}