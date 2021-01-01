package com.squidpony.samples;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.squidpony.samples.desktop.CustomConfig;
import squidpony.StringKit;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidgrid.gui.gdx.SquidMouse;
import squidpony.squidgrid.mapping.FantasyPoliticalMapper;
import squidpony.squidgrid.mapping.WorldMapGenerator;
import squidpony.squidmath.FastNoise;
import squidpony.squidmath.LinnormRNG;
import squidpony.squidmath.NumberTools;
import squidpony.squidmath.StatefulRNG;

/**
 * Port of Zachary Carter's world generation technique, https://github.com/zacharycarter/mapgen
 * It seems to mostly work now, though it only generates one view of the map that it renders (but biome, moisture, heat,
 * and height maps can all be requested from it).
 * Currently, clouds are in progress, and look like <a href="http://i.imgur.com/Uq7Whzp.gifv">this preview</a>.
 */
public class DetailedWorldMapDemo extends ApplicationAdapter {
    public DetailedWorldMapDemo(){}
    public static final int
            Desert                 = 0 ,
            Savanna                = 1 ,
            TropicalRainforest     = 2 ,
            Grassland              = 3 ,
            Woodland               = 4 ,
            SeasonalForest         = 5 ,
            TemperateRainforest    = 6 ,
            BorealForest           = 7 ,
            Tundra                 = 8 ,
            Ice                    = 9 ,
            Beach                  = 10,
            Rocky                  = 11,
            River                  = 12,
            Ocean                  = 13,
            Empty                  = 14;

    //private static final int width = 314 * 3, height = 300;
    private static final int width = 1024, height = 512;
//    private static final int width = 512, height = 256;
//    private static final int width = 400, height = 400;
//    private static final int width = 300, height = 300;
    //private static final int width = 1600, height = 800;
//    private static final int width = 1024, height = 1024;
//    private static final int width = 700, height = 700;
//    private static final int width = 512, height = 512;
    
    private SpriteBatch batch;
    private static final int cellWidth = 1, cellHeight = 1;
    private SquidInput input;
    private Viewport view;
    private StatefulRNG rng;
    private long seed;
    private int mode = 1, maxModes = 4;

    private WorldMapGenerator world;

    private Pixmap pm;
    private Texture pt;
    //private int counter = 0;
    private Color tempColor = Color.WHITE.cpy();
    
    private float nation = 0f;
    private long ttg = 0; // time to generate
    private WorldMapGenerator.DetailedBiomeMapper dbm;
    private FantasyPoliticalMapper fpm;
    private char[][] political;
    
    // Biome map colors
    private static float baseIce = SColor.ALICE_BLUE.toFloatBits();
    private static float ice = baseIce;
    private static float lightIce = SColor.FLOAT_WHITE;
    private static float desert = SColor.floatGetI(248, 229, 180);
    private static float savanna = SColor.floatGetI(181, 200, 100);
    private static float tropicalRainforest = SColor.floatGetI(66, 123, 25);
    private static float tundra = SColor.floatGetI(151, 175, 159);
    private static float temperateRainforest = SColor.floatGetI(54, 113, 60);
    private static float grassland = SColor.floatGetI(169, 185, 105);
    private static float seasonalForest = SColor.floatGetI(100, 158, 75);
    private static float borealForest = SColor.floatGetI(75, 105, 45);
    private static float woodland = SColor.floatGetI(122, 170, 90);
    private static float rocky = SColor.floatGetI(171, 175, 145);
    private static float beach = SColor.floatGetI(255, 235, 180);
    private static float emptyColor = SColor.DB_INK.toFloatBits();

    // water colors
    private static float baseDeepColor = SColor.floatGetI(0, 42, 88);
    private static float baseShallowColor = SColor.floatGetI(0, 73, 137);
    private static float baseCoastalColor = SColor.lightenFloat(baseShallowColor, 0.3f);
    private static float baseFoamColor = SColor.floatGetI(61,  162, 215);

    private static float deepColor = baseDeepColor;
    private static float shallowColor = baseShallowColor;
    private static float coastalColor = baseCoastalColor;
    private static float foamColor = baseFoamColor;
    
    private static float desertAlt = SColor.floatGetI(253, 226, 160);

    private static float[] biomeColors = {
            desert,
            savanna,
            tropicalRainforest,
            grassland,
            woodland,
            seasonalForest,
            temperateRainforest,
            borealForest,
            tundra,
            ice,
            beach,
            rocky,
            foamColor,
            deepColor,
            emptyColor
    };

    protected final static float[] BIOME_TABLE = {
            //COLDEST   //COLDER      //COLD               //HOT                     //HOTTER                 //HOTTEST
            Ice+0.7f,   Ice+0.65f,    Grassland+0.9f,      Desert+0.75f,             Desert+0.8f,             Desert+0.85f,            //DRYEST
            Ice+0.6f,   Tundra+0.9f,  Grassland+0.6f,      Grassland+0.3f,           Desert+0.65f,            Desert+0.7f,             //DRYER
            Ice+0.5f,   Tundra+0.7f,  Woodland+0.4f,       Woodland+0.6f,            Savanna+0.8f,           Desert+0.6f,              //DRY
            Ice+0.4f,   Tundra+0.5f,  SeasonalForest+0.3f, SeasonalForest+0.5f,      Savanna+0.6f,            Savanna+0.4f,            //WET
            Ice+0.2f,   Tundra+0.3f,  BorealForest+0.35f,  TemperateRainforest+0.4f, TropicalRainforest+0.6f, Savanna+0.2f,            //WETTER
            Ice+0.0f,   BorealForest, BorealForest+0.15f,  TemperateRainforest+0.2f, TropicalRainforest+0.4f, TropicalRainforest+0.2f, //WETTEST
            Rocky+0.9f, Rocky+0.6f,   Beach+0.4f,          Beach+0.55f,              Beach+0.75f,             Beach+0.9f,              //COASTS
            Ice+0.3f,   River+0.8f,   River+0.7f,          River+0.6f,               River+0.5f,              River+0.4f,              //RIVERS
            Ice+0.2f,   River+0.7f,   River+0.6f,          River+0.5f,               River+0.4f,              River+0.3f,              //LAKES
            Ocean+0.9f, Ocean+0.75f,  Ocean+0.6f,          Ocean+0.45f,              Ocean+0.3f,              Ocean+0.15f,             //OCEANS
            Empty                                                                                                                      //SPACE
    }, BIOME_COLOR_TABLE = new float[61], BIOME_DARK_COLOR_TABLE = new float[61];
    private static final float[] NATION_COLORS = new float[144];
    private static void randomizeColors(long seed)
    {
        float b, diff, alt, hue = NumberTools.randomSignedFloat(seed);
        int bCode;
        for (int i = 0; i < 60; i++) {
            b = BIOME_TABLE[i];
            bCode = (int)b;
            alt = SColor.toEditedFloat(biomeColors[bCode],
                    hue,
                    NumberTools.randomSignedFloat(seed * 3L + bCode) * 0.45f - 0.1f,
                    NumberTools.randomSignedFloat(seed * 5L + bCode) * 0.5f,
                    0f);
            diff = ((b % 1.0f) - 0.48f) * 0.27f;
            BIOME_COLOR_TABLE[i] = (b = (diff >= 0)
                    ? SColor.lightenFloat(alt, diff)
                    : SColor.darkenFloat(alt, -diff));
            BIOME_DARK_COLOR_TABLE[i] = SColor.darkenFloat(b, 0.08f);
        }
        float sat = NumberTools.randomSignedFloat(seed * 3L - 1L) * 0.4f, 
                value = NumberTools.randomSignedFloat(seed * 5L - 1L) * 0.3f;
        
        deepColor = SColor.toEditedFloat(baseDeepColor, hue, sat, value, 0f);
        shallowColor = SColor.toEditedFloat(baseShallowColor, hue, sat, value, 0f);
        coastalColor = SColor.toEditedFloat(baseCoastalColor, hue, sat, value, 0f);
        foamColor = SColor.toEditedFloat(baseFoamColor, hue, sat, value, 0f);
        ice = SColor.toEditedFloat(baseIce, hue, sat * 0.3f, value * 0.2f, 0f);
    }
    static {
        float b, diff;
        for (int i = 0; i < 60; i++) {
            b = BIOME_TABLE[i];
            diff = ((b % 1.0f) - 0.48f) * 0.27f;
            BIOME_COLOR_TABLE[i] = (b = (diff >= 0)
                    ? SColor.lightenFloat(biomeColors[(int)b], diff)
                    : SColor.darkenFloat(biomeColors[(int)b], -diff));
            BIOME_DARK_COLOR_TABLE[i] = SColor.darkenFloat(b, 0.08f);
        }
        BIOME_COLOR_TABLE[60] = BIOME_DARK_COLOR_TABLE[60] = emptyColor;
        for (int i = 0; i < 144; i++) {
            NATION_COLORS[i] =  SColor.COLOR_WHEEL_PALETTE_REDUCED[((i + 1234567) * 13 & 0x7FFFFFFF) % 144].toFloatBits();
        }
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
//        display = new SquidPanel(width, height, cellWidth, cellHeight);
        //display.getTextCellFactory().font().getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        view = new StretchViewport(width*cellWidth, height*cellHeight);
        pm = new Pixmap(width * cellWidth, height * cellHeight, Pixmap.Format.RGB888);
        pm.setBlending(Pixmap.Blending.None);
        pt = new Texture(pm);
        pt.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        
//        stage = new Stage(view, batch);
        seed = 0x0c415cf07774ab2eL;//0x9987a26d1e4d187dL;//0xDEBACL;
        rng = new StatefulRNG(seed);
//        world = new WorldMapGenerator.TilingMap(seed, width, height, FastNoise.instance, 1.25);
        //world = new WorldMapGenerator.SphereMapAlt(seed, width, height, FastNoise.instance, 0.8);
        //world = new WorldMapGenerator.EllipticalMap(seed, width, height, FastNoise.instance, 0.8);
        //world = new WorldMapGenerator.EllipticalHammerMap(seed, width, height, FastNoise.instance, 0.75);
        //world = new WorldMapGenerator.MimicMap(seed, WhirlingNoise.instance, 0.8);
        //world = new WorldMapGenerator.SpaceViewMap(seed, width, height, FastNoise.instance, 0.7);
        //world = new WorldMapGenerator.RotatingSpaceMap(seed, width, height, FastNoise.instance, 0.75);
        //world = new WorldMapGenerator.RoundSideMap(seed, width, height, FastNoise.instance, 0.8);
        world = new WorldMapGenerator.HyperellipticalMap(seed, width, height, new FastNoise(1337, 2.25f, FastNoise.FOAM_FRACTAL, 2, 2.5f, 0.4f), 0.7);//, 0.1, 3.25);
        //cloudNoise = new Noise.Turbulent4D(WhirlingNoise.instance, new Noise.Ridged4D(SeededNoise.instance, 2, 3.7), 3, 5.9);
        //cloudNoise = new Noise.Layered4D(WhirlingNoise.instance, 2, 3.2);
        //cloudNoise2 = new Noise.Ridged4D(SeededNoise.instance, 3, 6.5);
        //world = new WorldMapGenerator.TilingMap(seed, width, height, WhirlingNoise.instance, 0.9);
        dbm = new WorldMapGenerator.DetailedBiomeMapper();
        fpm = new FantasyPoliticalMapper();
        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key) {
                    case SquidInput.ENTER:
                        seed = rng.nextLong();
                        generate(seed);
                        rng.setState(seed);
                        break;
                    case '=':
                    case '+':
                        zoomIn();
                        break;
                    case '-':
                    case '_':
                        zoomOut();
                        break;
                    case 'M':
                    case 'm':
                        mode = (mode + 2) % maxModes;
                        break;
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE: {
                        ((Lwjgl3Graphics)Gdx.graphics).getWindow().closeWindow();
                    }
                }
                Gdx.graphics.requestRendering();
            }
        }, new SquidMouse(1, 1, width, height, 0, 0, new InputAdapter()
        {
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(button == Input.Buttons.RIGHT)
                {
                    zoomOut(screenX, screenY);
//                    Gdx.graphics.requestRendering();
                }
                else
                {
                    zoomIn(screenX, screenY);
//                    Gdx.graphics.requestRendering();
                }
                return true;
            }
        }));
        input.setRepeatGap(Long.MAX_VALUE);
        generate(seed);
        rng.setState(seed);
        Gdx.input.setInputProcessor(input);
//        display.setPosition(0, 0);
//        stage.addActor(display);
//        Gdx.graphics.setContinuousRendering(true);
//        Gdx.graphics.requestRendering();
    }

    public void zoomIn() {
        zoomIn(width>>1, height>>1);
    }
    public void zoomIn(int zoomX, int zoomY)
    {
        long startTime = System.currentTimeMillis();
        world.zoomIn(1, zoomX, zoomY);
        dbm.makeBiomes(world);
        //political = fpm.adjustZoom();//.generate(seed + 1000L, world, dbm, null, 50, 1.0);
//        System.out.println(StringKit.hex(CrossHash.hash64(world.heightCodeData)) + " " + StringKit.hex(CrossHash.hash64(dbm.biomeCodeData)));
        ttg = System.currentTimeMillis() - startTime;
    }
    public void zoomOut()
    {
        zoomOut(width>>1, height>>1);
    }
    public void zoomOut(int zoomX, int zoomY)
    {
        long startTime = System.currentTimeMillis();
        world.zoomOut(1, zoomX, zoomY);
        dbm.makeBiomes(world);
        //political = fpm.adjustZoom();//.generate(seed + 1000L, world, dbm, null, 50, 1.0);
//        System.out.println(StringKit.hex(CrossHash.hash64(world.heightCodeData)) + " " + StringKit.hex(CrossHash.hash64(dbm.biomeCodeData)));
        ttg = System.currentTimeMillis() - startTime;
    }
    public void generate(final long seed)
    {
        long startTime = System.currentTimeMillis();
        System.out.println("Seed used: 0x" + StringKit.hex(seed) + "L");
        //world.setCenterLongitude((System.currentTimeMillis() & 0xFFFFFFF) * 0.0002);
        world.generate(1.0 + NumberTools.formCurvedDouble((seed ^ 0x123456789ABCDL) * 0x12345689ABL) * 0.3,
                LinnormRNG.determineDouble(seed * 0x12345L + 0x54321L) * 0.2 + 0.9, seed);
        dbm.makeBiomes(world);
        //randomizeColors(seed);
        //political = fpm.generate(seed + 1000L, world, dbm, null, 50, 1.0);
//        System.out.println(StringKit.hex(CrossHash.hash64(world.heightCodeData)) + " " + StringKit.hex(CrossHash.hash64(dbm.biomeCodeData)));
        //counter = 0L;
        ttg = System.currentTimeMillis() - startTime;
    }
    public void rotate()
    {
        long startTime = System.currentTimeMillis();
        world.setCenterLongitude((System.currentTimeMillis() & 0xFFFFFFF) * 0.0002);
        world.generate(world.landModifier, world.heatModifier, seed);
        dbm.makeBiomes(world);
        //political = fpm.generate(seed + 1000L, world, dbm, null, 50, 1.0);
//        System.out.println(StringKit.hex(CrossHash.hash64(world.heightCodeData)) + " " + StringKit.hex(CrossHash.hash64(dbm.biomeCodeData)));
        ttg = System.currentTimeMillis() - startTime;
    }

    public void putMap() {
        int hc, tc, bc;
        int[][] heightCodeData = world.heightCodeData;
        double[][] heightData = world.heightData;
        int[][] heatCodeData = dbm.heatCodeData;
        int[][] biomeCodeData = dbm.biomeCodeData;
        pm.setColor(quantize(SColor.DB_INK));
        pm.fill();
        for (int y = 0; y < height; y++) {
            PER_CELL:
            for (int x = 0; x < width; x++) {
                hc = heightCodeData[x][y];
                if (hc == 1000)
                    continue;
                tc = heatCodeData[x][y];
                bc = biomeCodeData[x][y];
                if (tc == 0) {
                    switch (hc) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(shallowColor, ice,
                                    (float) ((heightData[x][y] - -1.0) / (WorldMapGenerator.sandLower - -1.0))));
//                        pm.setColor(tempColor);
//                        pm.drawRectangle(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                            pm.drawPixel(x, y, quantize(tempColor));//Color.rgba8888(tempColor));
                            //display.put(x, y, SColor.lerpFloatColors(shallowColor, ice,
                            //        (float) ((heightData[x][y] - -1.0) / (0.1 - -1.0))));
                            continue PER_CELL;
                        case 4:
                            Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(lightIce, ice,
                                    (float) ((heightData[x][y] - WorldMapGenerator.sandLower) / (WorldMapGenerator.sandUpper - WorldMapGenerator.sandLower))));
//                        pm.setColor(tempColor);
//                        pm.drawRectangle(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                            pm.drawPixel(x, y, quantize(tempColor));//Color.rgba8888(tempColor));
                            //display.put(x, y, SColor.lerpFloatColors(lightIce, ice,
                            //        (float) ((heightData[x][y] - 0.1) / (0.18 - 0.1))));
                            continue PER_CELL;
                    }
                }
                switch (hc) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(
                                BIOME_COLOR_TABLE[56], coastalColor,
                                (MathUtils.clamp((float) (((heightData[x][y] + 0.06) * 8.0) / (WorldMapGenerator.sandLower + 1.0)), 0f, 1f))));
//                        Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(deepColor, coastalColor,
//                                (float) ((heightData[x][y] - -1.0) / (WorldMapGenerator.sandLower - -1.0))));
//                    pm.setColor(tempColor);
//                    pm.drawRectangle(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                        pm.drawPixel(x, y, quantize(tempColor));//Color.rgba8888(tempColor));
                        //display.put(x, y, SColor.lerpFloatColors(deepColor, coastalColor,
                        //        (float) ((heightData[x][y] - -1.0) / (0.1 - -1.0))));
                        break;
                    default:
                        /*
                        if(partialLakeData.contains(x, y))
                            System.out.println("LAKE  x=" + x + ",y=" + y + ':' + (((heightData[x][y] - lowers[hc]) / (differences[hc])) * 19
                                    + shadingData[x][y] * 13) * 0.03125f);
                        else if(partialRiverData.contains(x, y))
                            System.out.println("RIVER x=" + x + ",y=" + y + ':' + (((heightData[x][y] - lowers[hc]) / (differences[hc])) * 19
                                    + shadingData[x][y] * 13) * 0.03125f);
                        */

                        Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(BIOME_COLOR_TABLE[dbm.extractPartB(bc)],
                                BIOME_DARK_COLOR_TABLE[dbm.extractPartA(bc)], dbm.extractMixAmount(bc)));
//                    pm.setColor(tempColor);
//                    pm.drawRectangle(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                        pm.drawPixel(x, y, quantize(tempColor));//Color.rgba8888(tempColor));
                        //display.put(x, y, SColor.lerpFloatColors(BIOME_COLOR_TABLE[biomeLowerCodeData[x][y]],
                        //        BIOME_DARK_COLOR_TABLE[biomeUpperCodeData[x][y]],
                        //        (float) //(((heightData[x][y] - lowers[hc]) / (differences[hc])) * 11 +
                        //                shadingData[x][y]// * 21) * 0.03125f
                        //        ));

                        //display.put(x, y, SColor.lerpFloatColors(darkTropicalRainforest, desert, (float) (heightData[x][y])));
                }
            }
        }
        batch.begin();
        pt.draw(pm, 0, 0);
        batch.draw(pt, 0, 0, width, height);
        batch.end();
    }
    public void putHeatMap() {
        int hc;
        int[][] heightCodeData = world.heightCodeData;
        double[][] heatData = world.heatData;
        double heat;
        pm.setColor(quantize(SColor.DB_INK));
        pm.fill();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                hc = heightCodeData[x][y];
                if (hc == 1000)
                    continue;
                heat = heatData[x][y];
                if(hc < 4)
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(ice, deepColor,
                            (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat + 0.001))));
                else
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(-0x1.5bbf5ap126F, // SColor.MOSS_GREEN
                             -0x1.8081fep125F, // SColor.CORAL_RED
                        (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat + 0.001))));
                pm.drawPixel(x, y, quantize(tempColor));
            }
        }
        batch.begin();
        pt.draw(pm, 0, 0);
        batch.draw(pt, 0, 0, width >> 1, height >> 1);
        batch.end();
    }
    public void putMoistureMap() {
        int hc;
        int[][] heightCodeData = world.heightCodeData;
        double[][] moistureData = world.moistureData;
        double moisture;
        pm.setColor(quantize(SColor.DB_INK));
        pm.fill();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                hc = heightCodeData[x][y];
                if (hc == 1000)
                    continue;
                moisture = moistureData[x][y];
                if(hc < 4)
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(shallowColor, deepColor,
                            (float) ((moisture - world.minWet) / (world.maxWet - world.minWet + 0.001))));
                else
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(desert, tropicalRainforest,
                            (float) ((moisture - world.minWet) / (world.maxWet - world.minWet + 0.001))));
                pm.drawPixel(x, y, quantize(tempColor));
            }
        }
        batch.begin();
        pt.draw(pm, 0, 0);
        batch.draw(pt, 0, 0, width >> 1, height >> 1);
        batch.end();
    }
    private final float emphasize(final float a)
    {
        return a * a * (3f - 2f * a);
    }
    private final float extreme(final float a)
    {
        return a * a * a * (a * (a * 6f - 15f) + 10f);
    }
    public void putExperimentMap() {
        int hc;
        final int[][] heightCodeData = world.heightCodeData;
        final double[][] moistureData = world.moistureData, heatData = world.heatData, heightData = world.heightData;
        double elevation, heat, moisture;
        boolean icy;
        pm.setColor(quantize(SColor.DB_INK));
        pm.fill();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                hc = heightCodeData[x][y];
                if (hc == 1000)
                    continue;
                moisture = moistureData[x][y];
                heat = heatData[x][y];
                elevation = heightData[x][y];
                icy = heat - elevation * 0.25 < 0.16;
                if(hc < 4) {
                    float a = (MathUtils.clamp((float) (((elevation + 0.06) * 16.0) / (WorldMapGenerator.sandLower + 1.0)), 0f, 1f));
                    Color.abgr8888ToColor(tempColor,
                            heat < 0.26 ? SColor.lerpFloatColors(shallowColor, ice,
                                    (float)((elevation + 1.0) / (WorldMapGenerator.sandLower+1.0)))
                                    : SColor.lerpFloatColors(
                            BIOME_COLOR_TABLE[56], coastalColor,
                            a));
                }
                else if(hc == 4)
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(icy ? BIOME_COLOR_TABLE[0] : SColor.lerpFloatColors(BIOME_DARK_COLOR_TABLE[34], BIOME_COLOR_TABLE[41],
                            (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat))),
                            SColor.lerpFloatColors(icy ? ice : SColor.lerpFloatColors(rocky, desertAlt,
                                    (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat))),
                                    icy ? lightIce : SColor.lerpFloatColors(woodland, BIOME_COLOR_TABLE[28],
                                            (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat))),
                                    (extreme((float) (moisture)))),
                            (float) ((elevation - WorldMapGenerator.sandLower) / (WorldMapGenerator.sandUpper - WorldMapGenerator.sandLower))));
                else
                    Color.abgr8888ToColor(tempColor, SColor.lerpFloatColors(icy ? ice : SColor.lerpFloatColors(rocky, desertAlt,
                            (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat))),
                            icy ? lightIce : SColor.lerpFloatColors(woodland, BIOME_COLOR_TABLE[28],
                                    (float) ((heat - world.minHeat) / (world.maxHeat - world.minHeat))),
                            (extreme((float) (moisture)))));
                pm.drawPixel(x, y, quantize(tempColor));
            }
        }
        batch.begin();
        pt.draw(pm, 0, 0);
        batch.draw(pt, 0, 0, width, height);
        batch.end();
    }
    
    public int quantize(Color color)
    {
        // Full 8-bit RGBA channels. No limits on what colors can be displayed.
        //if((mode & 1) == 0) 
            return Color.rgba8888(color);

        // Limits red, green, and blue channels to only use 5 bits (32 values) instead of 8 (256 values).
        //return Color.rgba8888(color) & 0xF8F8F8FF;

        // 253 possible colors, including one all-zero transparent color. 6 possible red values (not bits), 7 possible
        // green values, 6 possible blue values, and the aforementioned fully-transparent black. White is 0xFFFFFFFF and
        // not some off-white value, but other than black (0x000000FF), grayscale values have non-zero saturation.
        // Could be made into a palette, and images that use this can be saved as GIF or in PNG-8 indexed mode.
        //return ((0xFF000000 & (int)(color.r*6) * 0x2AAAAAAA) | (0xFF0000 & (int)(color.g*7) * 0x249249) | (0xFF00 & (int)(color.b*6) * 0x2AAA) | 255) & -(int)(color.a + 0.5f);
        //return SColor.quantize253I(color);
        //return (redLUT[(int)(color.r*31.999f)] | greenLUT[(int)(color.g*31.999f)] | blueLUT[(int)(color.b*31.999f)] | 255) & -(int)(color.a + 0.5f);
    }
    
    @Override
    public void render() {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(SColor.DB_INK.r, SColor.DB_INK.g, SColor.DB_INK.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        // need to display the map every frame, since we clear the screen to avoid artifacts.
        switch (mode)
        {
            /*
            case 3: putHeatMap();
            break;
            case 2: putMoistureMap();
            break;
            */
            case 2:
            case 3: putExperimentMap();
            break;
            default: putMap();
            break;
        }
        //++counter;//nation = NumberTools.swayTight(++counter * 0.0125f);
        Gdx.graphics.setTitle("Took " + ttg + " ms to generate");

        // if we are waiting for the player's input and get input, process it.
        if (input.hasNext()) {
            input.next();
        }
        // stage has its own batch and must be explicitly told to draw().
//        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
        view.apply(true);
    }

    public static CustomConfig config = new CustomConfig("DetailedWorldMapDemo"){
        {
            setTitle("SquidLib Demo: Detailed World Map");
            setWindowedMode(width, height);
            useVsync(false);
            setForegroundFPS(16);
            setIdleFPS(1);
        }
        @Override
        public ApplicationListener instantiate() {
            return new DetailedWorldMapDemo();
        }
    };
}
