package com.github.squidpony;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import squidpony.squidgrid.gui.gdx.DefaultResources;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.TextCellFactory;
import squidpony.squidmath.*;

import static squidpony.squidgrid.gui.gdx.SColor.colorFromFloat;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Bench extends ApplicationAdapter {
    public TextCellFactory tcf;
    public BitmapFont bmpFont;
    public SpriteBatch batch;
    public String[] text;
    public float color;

    /**
     * LFSR code not present in earlier commits.
     * @param state the previous value returned from this function, or any non-zero int
     * @return a non-zero int (as long as state was non-zero)
     */
    public static int determineInt(final int state)
    {
        return state >>> 1 ^ (-(state & 1) & 0xA3000000);
    }
    @Override
    public void create() {
        super.create();
        batch = new SpriteBatch();
        tcf = DefaultResources.getStretchableSlabFont().width(11).height(22).initBySize();
        bmpFont = tcf.font();
        text = new String[5];
        long time, time2;
        
        PintRNG pint = new PintRNG(0x1337BEEF);
        BirdRNG bird = new BirdRNG(0x1337BEEF);
        FlapRNG flap = new FlapRNG(0x1337BEEF);
        LapRNG lap = new LapRNG(0x1337BEEF);
        LightRNG light = new LightRNG(0x1337BEEF);
        int tally;
        tally = 0;
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            tally += pint.next(2);
        }
        time2 = System.currentTimeMillis();
        text[0] = "PintRNG took " + (time2 - time) + " ms.";
        tally = 0;
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            tally += bird.next(2);
        }
        time2 = System.currentTimeMillis();
        text[1] = "BirdRNG took " + (time2 - time) + " ms.";
        tally = 0;
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            tally += flap.next(2);
        }
        time2 = System.currentTimeMillis();
        text[2] = "FlapRNG took " + (time2 - time) + " ms.";
        tally = 0;
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            tally += lap.next(2);
        }
        time2 = System.currentTimeMillis();
        text[3] = "LapRNG took " + (time2 - time) + " ms.";
        tally = 0;
        time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            tally += light.next(2);
        }
        time2 = System.currentTimeMillis();
        text[4] = "LightRNG took " + (time2 - time) + " ms.";

//        final double[] data = new double[16];
//        final float[] data2 = new float[16];
//        int stateA, stateB, tally;
//        tally = 0;
//        time = System.currentTimeMillis();
//        stateA = (int)time;
//        stateB = 0x468ACE;
//        for (int i = 0; i < 1000000; i++) {
//            for (int j = 0; j < 16; j++) {
//                data2[j] = PintRNG.determine(stateA += 0x9E3779B9) / (float) (stateB = determineInt(stateB));
//            }
//            tally ^= CrossHash.Wisp.hashAlt(data2);
//        }
//        time2 = System.currentTimeMillis();
//        text[3] = "Wisp.hashAlt(float[])=" + tally + " \ntook " + (time2 - time) + " ms.";
//        tally = 0;
//        time = System.currentTimeMillis();
//        stateA = (int)time;
//        stateB = 0x468ACE;
//        for (int i = 0; i < 1000000; i++) {
//            for (int j = 0; j < 16; j++) {
//                data2[j] = PintRNG.determine(stateA += 0x9E3779B9) / (float) (stateB = determineInt(stateB));
//            }
//            tally ^= CrossHash.Wisp.hash(data2);
//        }
//        time2 = System.currentTimeMillis();
//        text[2] = "Wisp.hash(float[])=" + tally + " \ntook " + (time2 - time) + " ms.";
//        tally = 0;
//        time = System.currentTimeMillis();
//        stateA = (int)time;
//        stateB = 0x468ACE;
//        for (int i = 0; i < 1000000; i++) {
//            for (int j = 0; j < 16; j++) {
//                data[j] = PintRNG.determine(stateA += 0x9E3779B9) / (double) (stateB = determineInt(stateB));
//            }
//            tally ^= CrossHash.Wisp.hashAlt(data);
//        }
//        time2 = System.currentTimeMillis();
//        text[1] = "Wisp.hashAlt(double[])=" + tally + " \ntook " + (time2 - time) + " ms.";
//        tally = 0;
//        time = System.currentTimeMillis();
//        stateA = (int)time;
//        stateB = 0x468ACE;
//        for (int i = 0; i < 1000000; i++) {
//            for (int j = 0; j < 16; j++) {
//                data[j] = PintRNG.determine(stateA += 0x9E3779B9) / (double) (stateB = determineInt(stateB));
//            }
//            tally ^= CrossHash.Wisp.hash(data);
//        }
//        time2 = System.currentTimeMillis();
//        text[0] = "Wisp.hash(double[])=" + tally + " \ntook " + (time2 - time) + " ms.";

        color = SColor.floatGet(0x8ffffffe);
        colorFromFloat(bmpFont.getColor(), color);
    }

    @Override
    public void render() {
        super.render();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        tcf.configureShader(batch);
        bmpFont.draw(batch, text[0], 20, 400);
        bmpFont.draw(batch, text[1], 20, 350);
        bmpFont.draw(batch, text[2], 20, 300);
        bmpFont.draw(batch, text[3], 20, 250);
        bmpFont.draw(batch, text[4], 20, 200);
        batch.end();

    }
}