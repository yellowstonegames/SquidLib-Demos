package com.squidpony.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.AnimatedGif;
import com.github.tommyettinger.anim8.AnimatedPNG;
import com.github.tommyettinger.anim8.PNG8;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class NorthernLights extends ApplicationAdapter {
    private static final float RATE = 1.5f;
    private int seed;
    private SpriteBatch batch;
    private Texture tiny;
    private long startTime;
    private int width, height;
    private float iw, ih;
    private final transient float[] con = new float[3];
    private Array<Pixmap> frames;
    private AnimatedPNG animatedPNG;
    private PNG8 iapng;
    private AnimatedGif animatedGif;
    @Override
    public void create() {
        super.create();
        startTime = TimeUtils.millis();
        long state = TimeUtils.nanoTime() - startTime;
        // Sarong's DiverRNG.randomize()
        seed = (int)
                ((state = ((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L) ^ state >>> 43 ^ state >>> 31 ^ state >>> 23) * 0xDB4F0B9175AE2165L) ^ state >>> 28);
        startTime -= seed >>> 16;
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch = new SpriteBatch();
        batch.disableBlending();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pm.drawPixel(0, 0, -1); // white pixel
        tiny = new Texture(pm);
        width = 256;
        height = 256;
        animatedPNG = new AnimatedPNG(width * height * 3 >>> 1);
        animatedGif = new AnimatedGif();
//        animatedGif.palette = new PaletteReducer(new int[]{0, 255, -1});
        iapng = new PNG8(width * height * 3 >>> 1);
//        iapng.palette = animatedGif.palette;
//        iapng.palette = new PaletteReducer(new int[]{
//                0x00000000, 0x19092DFF, 0x213118FF, 0x314A29FF, 0x8C847BFF, 0x6E868EFF, 0x9CA59CFF, 0xAFC7CFFF,
//                0xD6F7D6FF, 0xFBD7EBFF, 0xFDFBE3FF, 0xE73129FF, 0x7B2921FF, 0xE79C94FF, 0xBF4529FF, 0xE35A00FF,
//                0xAD6329FF, 0xE78431FF, 0x4A2D11FF, 0xD39A5EFF, 0xFFAA4DFF, 0xF7CF9EFF, 0xA58C29FF, 0xFBE76AFF,
//                0xBDB573FF, 0x6B7321FF, 0x8CAD29FF, 0xC7FF2DFF, 0x96DF1DFF, 0xBFEF94FF, 0x296318FF, 0x62FF39FF,
//                0x39C621FF, 0x319421FF, 0x4AEF31FF, 0x39AD5AFF, 0x49FF8AFF, 0x319E7AFF, 0x296B5AFF, 0x49B39AFF,
//                0x52F7DEFF, 0xA5DEDEFF, 0x39BDC6FF, 0x52CEEFFF, 0x42A5C6FF, 0x396B9CFF, 0x29426BFF, 0x394ABDFF,
//                0x2910DEFF, 0x29189CFF, 0x21105AFF, 0x6329E7FF, 0x9C84CEFF, 0x8A49DBFF, 0xCEADE7FF, 0x9C29B5FF,
//                0x6B1873FF, 0xD631DEFF, 0xE773D6FF, 0xA52973FF, 0xE7298CFF, 0xCF1562FF, 0x845A6BFF, 0xD66B7BFF
//        });
        frames = new Array<>(true, 120, Pixmap.class);
        iw = 1f / width;
        ih = 1f / height;
//        width = Gdx.graphics.getWidth();
//        height = Gdx.graphics.getHeight();

        for (int i = 0; i < 50; i++) {
            Pixmap frame = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            final int tm = i << 4;
            final float rt = tm * RATE,
                    ftm = rt * 0x5p-13f;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    final float ax = x * iw, ay = y * ih; // adjusted for starting dimensions
                    con[0] = ftm + ay;
                    con[1] = ftm + ax;
                    con[2] = ax + ay;
                    cosmic(seed ^ 0xC13FA9A9, con, 1, 2, 0);
                    cosmic(seed ^ 0xDB4F0B91, con, 2, 0, 1);
                    cosmic(seed ^ 0x19F1D48E, con, 0, 1, 2);
                    frame.setColor(swayTight(con[0]), swayTight(con[1]), swayTight(con[2]), 1f);
                    frame.drawPixel(x, y
//                            255 - Math.min(255, (int)(260 * Math.sqrt(((x - 128) * (x - 128) + (y - 128) * (y - 128)) * 0x1p-14)))
////use as the alpha to get a circle that fades at the edges
//                            1f - Math.min(1f, (int)(1.02f * Math.sqrt(((x - 128) * (x - 128) + (y - 128) * (y - 128)) * 0x1p-14)))
                    );
                }
            }
            frames.add(frame);
        }
        animatedPNG.write(Gdx.files.local("animated" + TimeUtils.millis() + ".png"), frames, 20);
        iapng.write(Gdx.files.local("animatedIndexed" + TimeUtils.millis() + ".png"), frames, 20);
        animatedGif.write(Gdx.files.local("animatedIndexed" + TimeUtils.millis() + ".gif"), frames, 20);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        this.width = width;
        this.height = height;
		iw = 1f / width;
		ih = 1f / height;
    }
//    public static float swayRandomized(int seed, float value)
//    {
//        final int floor = value >= 0f ? (int) value : (int) value - 1;
//        final float start = (((seed += floor * 0xD6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 10) * 0x4.ffffffp-24f,
//                end = (((seed += 0xD6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 10) * 0x4.ffffffp-24f;
//        value -= floor;
//        value *= value * (3f - 2f * value);
//        return ((1f - value) * start + value * end);
//    }

//    public static float swayRandomized(int seed, float value) {
//        final int floor = value >= 0f ? (int) value : (int) value - 1;
//        final float start = (((seed += floor * 0x9E377) ^ 0xD1B54A35) * 0x1D2473 & 0x3FFFFF) * 0x3.FFFFFp-23f - 1f,
//                end = ((seed + 0x9E377 ^ 0xD1B54A35) * 0x1D2473 & 0x3FFFFF) * 0x3.FFFFFp-23f - 1f;
//        value -= floor;
//        value *= value * (3f - 2f * value);
//        return (1f - value) * start + value * end;
//    }
    public static float swayRandomized(int seed, float value) {
        final int floor = value >= 0f ? (int) value : (int) value - 1;
        final float start = ((((seed += floor) ^ 0xD1B54A35) * 0x1D2473 & 0xFFFFF)) * 0x1p-20f,
                end = (((seed + 1 ^ 0xD1B54A35) * 0x1D2473 & 0xFFFFF)) * 0x1p-20f;
        value -= floor;
        value *= value * (3f - 2f * value);
        return (1f - value) * start + value * end;
    }


    public static float swayTight(float value)
    {
        int floor = (value >= 0f ? (int) value : (int) value - 1);
        value -= floor;
        floor &= 1;
//        return (int)(value * value * (765f - 510f * value) * (-floor | 1) + (-floor & 255));
        return value * value * (3f - 2f * value) * (-floor | 1) + floor;
//        return value * value * value * (value * (value * 6f - 15f) + 10f) * (-floor | 1) + floor;
    }

    /**
     * Based on CosmicNumbering in SquidLib.
     * @param c0 connection 0
     * @param c1 connection 1
     * @param c2 connection 2
     * @return a continuous noise-like value between -0.25f and 4.25f
     */
    private float cosmic(float c0, float c1, float c2)
    {
        final float sum = swayRandomized(seed, c0 + c1 + c2) * 1.5f;
//        float sum = swayRandomized(seed, c2 + c0);
//        sum += swayRandomized(~seed, sum + c0 + c1);
//        sum += swayRandomized(seed ^ 0x9E3779B9, sum + c1 + c2);
//        return sum + 0.5f + 2.5f * swayRandomized(seed ^ seed >>> 16, sum + c0 + c1 + c2);
        return sum + swayRandomized(-seed, sum * 0.5698402909980532f + 0.7548776662466927f * (c0 - c1 - c2));
    }

    private void cosmic(int seed, float[] con, float x, float y, float z)
    {
        con[0] += (x = swayRandomized(seed, x + z));
        con[1] += (y = swayRandomized(seed ^ 0x7F4A7C15, y + x));
        con[2] += (swayRandomized(seed ^ 0x9E3779B9, z + y));
    }

    private void cosmic(int seed, float[] con, int x, int y, int z)
    {
        con[0] += swayRandomized(seed, con[x] + con[z]);
        con[1] += swayRandomized(seed ^ 0x7F4A7C15, con[y] + con[x]);
        con[2] += swayRandomized(seed ^ 0x9E3779B9, con[z] + con[y]);
    }

    private void cosmic(int seed, float[] con, int x, int y)
    {
        con[0] += swayRandomized(seed, con[x] - con[y]) * MathUtils.sin(con[1]);
        con[1] += swayRandomized(seed ^ 0x7F4A7C15, con[x] + con[y]) * MathUtils.cos(con[0]);
        con[0] += swayRandomized(seed ^ 0x9E3779B9, con[y] - con[x]) * MathUtils.cos(con[1]);
        con[1] += swayRandomized(seed ^ 0xDB4F0B91, con[x] + con[y]) * MathUtils.sin(con[0]);
    }

    private void cosmic(int seed, float[] con)
    {
//        con[0] += swayRandomized(seed, con[0] - con[1]) * MathUtils.sin(con[1]);
//        con[1] += swayRandomized(seed ^ 0x7F4A7C15, con[0] + con[1]) * MathUtils.cos(con[0]);
        con[0] += swayRandomized(seed, con[0] - con[1]) + MathUtils.sin(con[1]);
        con[1] -= swayRandomized(seed ^ 0x7F4A7C15, con[0] + con[1]) + MathUtils.cos(con[0]);
        con[0] -= swayRandomized(seed ^ 0x9E3779B9, con[1] - con[0]) + MathUtils.cos(con[1]);
        con[1] += swayRandomized(seed ^ 0xDB4F0B91, con[0] + con[1]) + MathUtils.sin(con[0]);
    }

	@Override
    public void render() {
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        final int tm = (int) TimeUtils.timeSinceMillis(startTime);
        final float rt = tm * RATE,
                ftm = rt * 0x5p-13f;
//                s0 = swayRandomized(0x9E3779B9, ftm - 1.11f) * 0x1p-6f,
//                c0 = swayRandomized(0xC13FA9A9, ftm - 1.11f) * 0x1p-6f, 
//                s1 = swayRandomized(0xD1B54A32, ftm + 1.41f) * 0x1p-6f,
//                c1 = swayRandomized(0xDB4F0B91, ftm + 1.41f) * 0x1p-6f, 
//                s2 = swayRandomized(0xE19B01AA, ftm + 2.61f) * 0x1p-6f,
//                c2 = swayRandomized(0xE60E2B72, ftm + 2.61f) * 0x1p-6f;

        
//        final float r0 = rt * 0x3.cac1p-13f;//swayRandomized(0x12345678, rt * 0x3.cac1p-13f);
//        final float r1 = rt * 0x4.e6e9p-13f;//swayRandomized(0x81234567, rt * 0x4.e6e9p-13f);
//        final float r2 = rt * 0x5.09fcp-13f;//swayRandomized(0x78123456, rt * 0x5.09fcp-13f);

        batch.begin();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
				float ax = x * 0.0075f, ay = y * 0.005f; // adjusted for starting dimensions
				con[0] = ftm + ay;
				con[1] = ftm + ax;
				con[2] = ax + ay;
				
                //conn0 = swayRandomized(-1052792407, yt - 1.11f) * ax + swayRandomized(-1640531527, xt - 3.11f) * ay + swayRandomized(924071052, -2.4375f - xy) * ftm;
				//conn1 = swayRandomized(-615576687, yt + 2.41f) * ax + swayRandomized(776648142, 1.41f - xt) * ay + swayRandomized(-566875093, xy + 1.5625f) * ftm;
				//conn2 = swayRandomized(435278990, 3.61f - yt) * ax + swayRandomized(-509935190, xt + 2.61f) * ay + swayRandomized(-284277664, xy + -3.8125f) * ftm;
                //conn0 = cosmic(conn0, conn1, conn2);
                //conn1 = cosmic(conn0, conn1, conn2);
                //conn2 = cosmic(conn0, conn1, conn2);
                
                //2D
//                cosmic(seed ^ 0xDB4F0B91, con, 0, 1);
//                cosmic(~seed, con, 1, 0);
//                cosmic(seed, con);
                
                //cosmic(seed ^ 0x19F1D48E, con, 0, 1, 2);

//                //3D
                cosmic(seed ^ 0xC13FA9A9, con, 1, 2, 0);
                cosmic(seed ^ 0xDB4F0B91, con, 2, 0, 1);
                cosmic(seed ^ 0x19F1D48E, con, 0, 1, 2);

//                cosmic(seed ^ 0xC13FA9A9, con, con[1], con[2], con[0]);
//                cosmic(seed ^ 0xDB4F0B91, con, con[2], con[0], con[1]);
//                cosmic(seed ^ 0x19F1D48E, con, con[0], con[1], con[2]);
                
//                zone  = cosmic(conn0, conn1, conn2);
//                conn0 = /*r0*/ + x * c0 - y * s0;
//                conn1 = /*r1*/ - x * c1 + y * s1;
//                conn2 = /*r2*/ + x * c2 + y * s2;
//
//                zone = 0f;//cosmic(x * 0x1p-7f, y * 0x1p-7f, ftm);
//                conn0 = cosmic(conn0, conn1, conn2) + zone;
//                conn1 = cosmic(conn0, conn1, conn2) + zone;
//                conn2 = cosmic(conn0, conn1, conn2) + zone;
//                int bright = (int)((swayRandomized(seed ^ 0xDB4F0B91, (con[0]) * 2) * 85.25f
//                        + swayRandomized(seed ^ 0xBBE05633, (con[1]) * 3) * 85.25f
//                        + swayRandomized(seed ^ 0xA0F2EC75, (con[2]) * 5) * 85.25f));
//                batch.setColor(bright, bright, bright);
                //0xDB4F0B9175AE2165L, 0xBBE0563303A4615FL, 0xA0F2EC75A1FE1575L
                //batch.setColor(swayTight((((con[0]) * 0.2f)
                //                + ((con[1]) * 0.3f) 
                //                + ((con[2]) * 0.5f) )),
                //        swayTight((((con[1]) * 0.2f)
                //                + ((con[2]) * 0.3f) 
                //                + ((con[0]) * 0.5f) )),
                //        swayTight((((con[2]) * 0.2f)
                //                + ((con[0]) * 0.3f) 
                //                + ((con[1]) * 0.5f) )));
                
//                batch.setColor((int)((swayRandomized(seed ^ 0xDB4F0B91, (con[0]) * 2) * 85.25f
//                                + swayRandomized(seed ^ 0xBBE05633, (con[1]) * 3) * 85.25f
//                                + swayRandomized(seed ^ 0xA0F2EC75, (con[2]) * 5) * 85.25f)),
//                        (int)((swayRandomized(seed ^ 0x0B9175AE, (con[1]) * 2) * 85.25f
//                                + swayRandomized(seed ^ 0x563303A4, (con[2]) * 3) * 85.25f
//                                + swayRandomized(seed ^ 0xEC75A1FE, (con[0]) * 5) * 85.25f)),
//                        (int)((swayRandomized(seed ^ 0x75AE2165, (con[2]) * 2) * 85.25f
//                                + swayRandomized(seed ^ 0x03A4615F, (con[0]) * 3) * 85.25f
//                                + swayRandomized(seed ^ 0xA1FE1575, (con[1]) * 5) * 85.25f)));
//                final int bright = swayTight(con[0] + con[1] + con[2]);
//                batch.setColor(bright, bright, bright);
                batch.setColor(swayTight(con[0]), swayTight(con[1]), swayTight(con[2]), 1f);
//                batch.setColor((int)(con[0] * 127.99 + 128), (int)(con[1] * 127.99 + 128), (int)(con[2] * 127.99 + 128));
//                batch.setColor(lerpFloatColors(
//                        floatGet(swayTight(conn0), swayTight(conn1), swayTight(conn2))
//                        , floatGetHSV(swayTight(conn2), 1f, 1f), swayTight(0.5f - conn1))
//            );

//                conn0 = swayTight(conn0 + conn1 + conn2);
//                conn0 = swayTight(cosmic(conn0, conn1, conn2));
//                conn0 = swayTight(conn2 + zone);
//                batch.setColor(conn0, conn0, conn0, 1f);
                batch.draw(tiny, x, y);
            }
        }
        batch.end();
    }


    public static float floatGetHSV(float hue, float saturation, float value) {
        if (saturation <= 0.0039f) {
            return floatGet(value, value, value);
        } else if (value <= 0.0039f) {
            return NumberUtils.intBitsToFloat(0xFE000000);
        } else {
            final float h = ((hue + 6f) % 1f) * 6f;
            final int i = (int) h;
            value = MathUtils.clamp(value, 0f, 1f);
            saturation = MathUtils.clamp(saturation, 0f, 1f);
            final float a = value * (1 - saturation);
            final float b = value * (1 - saturation * (h - i));
            final float c = value * (1 - saturation * (1 - (h - i)));

            switch (i) {
                case 0:
                    return floatGet(value, c, a);
                case 1:
                    return floatGet(b, value, a);
                case 2:
                    return floatGet(a, value, c);
                case 3:
                    return floatGet(a, b, value);
                case 4:
                    return floatGet(c, a, value);
                default:
                    return floatGet(value, a, b);
            }
        }
    }
    public static float floatGet(float r, float g, float b) {
        return NumberUtils.intBitsToFloat(0xFE000000 | ((int) (b * 255) << 16)
                | ((int) (g * 255) << 8) | (int) (r * 255));
    }
    public static float lerpFloatColors(final float start, final float end, final float change) {
        final int s = NumberUtils.floatToIntBits(start), e = NumberUtils.floatToIntBits(end),
                rs = (s & 0xFF), gs = (s >>> 8) & 0xFF, bs = (s >>> 16) & 0xFF,
                re = (e & 0xFF), ge = (e >>> 8) & 0xFF, be = (e >>> 16) & 0xFF;
        return NumberUtils.intBitsToFloat(((int) (rs + change * (re - rs)) & 0xFF)
                | (((int) (gs + change * (ge - gs)) & 0xFF) << 8)
                | (((int) (bs + change * (be - bs)) & 0xFF) << 16)
                | 0xFE000000);
    }

}
