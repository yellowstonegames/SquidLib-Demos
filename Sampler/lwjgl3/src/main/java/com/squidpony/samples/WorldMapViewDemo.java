package com.squidpony.samples;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.squidpony.samples.lwjgl3.CustomConfig;
import squidpony.StringKit;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.WorldMapGenerator;
import squidpony.squidmath.DiverRNG;
import squidpony.squidmath.FastNoise;
import squidpony.squidmath.NumberTools;
import squidpony.squidmath.StatefulRNG;

public class WorldMapViewDemo extends ApplicationAdapter {

//    private static final int width = 64, height = 64;
//    private static final int width = 1024, height = 512;
//    private static final int width = 512, height = 256;
    private static final int width = 128, height = 128;
//    private static final int width = 400, height = 400;
//    private static final int width = 300, height = 300;
//    private static final int width = 1600, height = 800;
//    private static final int width = 900, height = 900;
//    private static final int width = 700, height = 700;
//    private static final int width = 512, height = 512;
//    private static final int width = 128, height = 128;
    
    private FilterBatch batch;
    private SquidInput input;
    private Viewport view;
    private StatefulRNG rng;
    private long seed;
    private WorldMapGenerator world;
    private WorldMapView wmv;
    private TextureRegion dot;
    
    private boolean spinning = false;

    private long ttg = 0; // time to generate
    
    @Override
    public void create() {
        // in your own code you would probably use your own atlas with a 1x1 white pixel TextureRegion in it
        TextureAtlas atlas = new TextureAtlas("skin/neon-ui.atlas");
        // here the 1x1 white pixel image is called "white"
        dot = atlas.findRegion("white");
        batch = new FilterBatch();
        view = new StretchViewport(width * 4, height * 4);
        seed = 42;
        rng = new StatefulRNG(seed);
        //// NOTE: this FastNoise has a different frequency (1f) than the default (1/32f), and that
        //// makes a huge difference on world map quality. It also uses extra octaves.
        FastNoise noise = new FastNoise(31337, 2.5f, FastNoise.FOAM_FRACTAL, 2, 2.5f, 0.4f);

        world = new WorldMapGenerator.LocalMap(seed, width, height, noise, 1.1);
//        world = new WorldMapGenerator.TilingMap(seed, width, height, new FastNoise(1337, 1f), 1.25);
//        world = new WorldMapGenerator.EllipticalMap(seed, width, height, WhirlingNoise.instance, 0.875);
        //world = new WorldMapGenerator.EllipticalHammerMap(seed, width, height, ClassicNoise.instance, 0.75);
//        world = new WorldMapGenerator.MimicMap(seed, new FastNoise(1337, 1f), 0.7);
//        world = new WorldMapGenerator.SpaceViewMap(seed, width, height, ClassicNoise.instance, 0.7);
//        world = new WorldMapGenerator.RotatingSpaceMap(seed, width, height, noise, 0.7);
        //world = new WorldMapGenerator.RoundSideMap(seed, width, height, ClassicNoise.instance, 0.8);
//        world = new WorldMapGenerator.HyperellipticalMap(seed, width, height, noise, 1.2, 0.0625, 2.5);
//        world = new WorldMapGenerator.SphereMap(seed, width, height, new FastNoise(1337, 1f), 0.6);
//        world = new WorldMapGenerator.LocalMap(seed, width, height, noise, 1.1);
//        world = new WorldMapGenerator.LocalMimicMap(seed, ((WorldMapGenerator.LocalMimicMap) world).earth.not(), new FastNoise(1337, 1f), 0.9);
        
        wmv = new WorldMapView(world);
//        wmv.initialize(SColor.CW_FADED_RED, SColor.AURORA_BRICK, SColor.DEEP_SCARLET, SColor.DARK_CORAL,
//                SColor.LONG_SPRING, SColor.WATER_PERSIMMON, SColor.AURORA_HOT_SAUCE, SColor.PALE_CARMINE,
//                SColor.AURORA_LIGHT_SKIN_3, SColor.AURORA_PINK_SKIN_2,
//                SColor.AURORA_PUTTY, SColor.AURORA_PUTTY, SColor.ORANGUTAN, SColor.SILVERED_RED, null);
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
                    case 'P':
                    case 'p':
                    case 'S':
                    case 's':
                        spinning = !spinning;
                        break;
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE: {
                        ((Lwjgl3Graphics)Gdx.graphics).getWindow().closeWindow();
                    }
                }
            }
        }, new SquidMouse(4, 4, width, height, 0, 0, new InputAdapter()
        {
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(button == Input.Buttons.RIGHT)
                {
                    zoomOut(screenX, height - 1 - screenY);
                }
                else
                {
                    zoomIn(screenX, height - 1 - screenY);
                }
                return true;
            }
        }));
        input.setRepeatGap(Long.MAX_VALUE);
        generate(seed);
        rng.setState(seed);
        Gdx.input.setInputProcessor(input);
    }

    public void zoomIn() {
        long startTime = System.nanoTime();
//        noiseCalls = 0;
        world.zoomIn(7, width / 2, height / 2);
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomIn(int zoomX, int zoomY)
    {
        long startTime = System.nanoTime();
//        noiseCalls = 0;
        world.zoomIn(7, zoomX, zoomY);

        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomOut()
    {
        long startTime = System.nanoTime();
//        noiseCalls = 0;
        world.zoomOut(7, width / 2, height / 2);
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomOut(int zoomX, int zoomY)
    {
        long startTime = System.nanoTime();
//        noiseCalls = 0;
        world.zoomOut(7, zoomX, zoomY);
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void generate(final long seed)
    {
        long startTime = System.nanoTime();
        System.out.println("Seed used: 0x" + StringKit.hex(seed) + "L");
        //// parameters to generate() are seedA, seedB, landModifier, heatModifier.
        //// seeds can be anything (if both 0, they'll be changed so seedA is 1, otherwise used as-is).
        //// higher landModifier means more land, lower means more water; the middle is 1.0.
        //// higher heatModifier means hotter average temperature, lower means colder; the middle is 1.0.
        //// heatModifier defaults to being higher than 1.0 on average here so polar ice caps are smaller.
        wmv.generate((int)(seed & 0xFFFFFFFFL), (int) (seed >>> 32),
                0.9 + NumberTools.formCurvedDouble((seed ^ 0x123456789ABCDL) * 0x12345689ABL) * 0.3,
                DiverRNG.determineDouble(seed * 0x12345L + 0x54321L) * 0.55 + 0.9);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void rotate()
    {
        long startTime = System.nanoTime();
        world.setCenterLongitude((startTime & 0xFFFFFFFFFFFFL) * 0x1.0p-32);
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }


    public void putMap() { 
        float[][] cm = wmv.getColorMap();
        //// everything after this part of putMap() should be customized to your rendering setup
        view.apply(true);
        batch.begin();
        float c;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                c = cm[x][y];
                if(c != WorldMapView.emptyColor) {
                    batch.setColor(c);
                    batch.draw(dot, x * 4, y * 4, 4, 4);
                }
            }
        }
        batch.end();
    }
    
    @Override
    public void render() {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(SColor.DB_INK.r, SColor.DB_INK.g, SColor.DB_INK.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if(spinning) 
            rotate();
        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        Gdx.graphics.setTitle("Took " + ttg + " ms to generate");

        // if we are waiting for the player's input and get input, process it.
        if (input.hasNext()) {
            input.next();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
    }

    public static CustomConfig config = new CustomConfig("WorldMapViewDemo"){
        {
            setTitle("SquidLib Demo: WorldMapView Usage");
            setWindowedMode(width * 4, height * 4);
            useVsync(false);
            setForegroundFPS(16);
            setIdleFPS(1);
        }
        @Override
        public ApplicationListener instantiate() {
            return new WorldMapViewDemo();
        }
    };

}
