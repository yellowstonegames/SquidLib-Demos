package com.squidpony.samples;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.squidpony.samples.lwjgl3.CustomConfig;
import squidpony.StringKit;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidgrid.gui.gdx.SquidMouse;
import squidpony.squidgrid.gui.gdx.WorldMapView;
import squidpony.squidgrid.mapping.WorldMapGenerator;
import squidpony.squidmath.DiverRNG;
import squidpony.squidmath.FastNoise;
import squidpony.squidmath.NumberTools;
import squidpony.squidmath.StatefulRNG;

/**
 * Port of Zachary Carter's world generation technique, https://github.com/zacharycarter/mapgen
 * It seems to mostly work now, though it only generates one view of the map that it renders (but biome, moisture, heat,
 * and height maps can all be requested from it).
 * Currently, clouds are in progress, and look like <a href="http://i.imgur.com/Uq7Whzp.gifv">this preview</a>.
 */
public class RotatingWorldMapDemo extends ApplicationAdapter {
    public RotatingWorldMapDemo(){}

    //private static final int width = 314 * 3, height = 300;
    //private static final int width = 1024, height = 512;
//    private static final int width = 512, height = 256;
//    private static final int width = 400, height = 400;
    private static final int width = 300, height = 300;
    //private static final int width = 1600, height = 800;
    ///private static final int width = 1000, height = 1000;
    //private static final int width = 700, height = 700;
//    private static final int width = 512, height = 512;
    
    private static final int cellWidth = 1, cellHeight = 1;
    private WorldMapGenerator.RotatingSpaceMap world;


    private ImmediateModeRenderer20 batch;
    private SquidInput input;
    private Viewport view;
    private StatefulRNG rng;
    private long seed;
    private WorldMapView wmv;

    private boolean spinning = true;

    private long ttg = 0; // time to generate
//    private long ttd = 0; // time to draw
    

    @Override
    public void create() {

        //// you will probably want to change batch to use whatever rendering system is appropriate
        //// for your game; here it always renders pixels
        batch = new ImmediateModeRenderer20(width * height, false, true, 0);
        view = new StretchViewport(width, height);
        seed = 0x0c415cf07774ab2eL;//0x9987a26d1e4d187dL;//0xDEBACL;
        rng = new StatefulRNG(seed);
        //// NOTE: this FastNoise has a different frequency (1f) than the default (1/32f), and that
        //// makes a huge difference on world map quality. It also uses extra octaves.
        FastNoise noise = new FastNoise(31337, 2.5f, FastNoise.FOAM_FRACTAL, 2, 2.5f, 0.4f);

        //world = new WorldMapGenerator.TilingMap(seed, width, height, WhirlingNoise.instance, 1.25);
//        world = new WorldMapGenerator.SphereMapAlt(seed, width, height, WhirlingNoise.instance, 0.8);
        //world = new WorldMapGenerator.EllipticalMap(seed, width, height, ClassicNoise.instance, 0.8);
        //world = new WorldMapGenerator.EllipticalHammerMap(seed, width, height, ClassicNoise.instance, 0.75);
        //world = new WorldMapGenerator.MimicMap(seed, WhirlingNoise.instance, 0.8);
//        world = new WorldMapGenerator.SpaceViewMap(seed, width, height, ClassicNoise.instance, 0.7);
        world = new WorldMapGenerator.RotatingSpaceMap(seed, width, height, noise, 0.7);
        //world = new WorldMapGenerator.RoundSideMap(seed, width, height, ClassicNoise.instance, 0.8);
        //world = new WorldMapGenerator.HyperellipticalMap(seed, width, height, ClassicNoise.instance, 0.7, 0.1, 3.25);

        wmv = new WorldMapView(world);

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
                    case 'S':
                    case 's':
                    case 'P':
                    case 'p':
                        spinning = !spinning;
                        break;
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE: {
                        ((Lwjgl3Graphics)Gdx.graphics).getWindow().closeWindow();
                    }
                }
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
    }

    public void zoomIn() {
        long startTime = System.nanoTime();
        world.zoomIn();
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomIn(int zoomX, int zoomY)
    {
        long startTime = System.nanoTime();
        world.zoomIn(1, zoomX<<1, zoomY<<1);
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomOut()
    {
        long startTime = System.nanoTime();
        world.zoomOut();
        wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }
    public void zoomOut(int zoomX, int zoomY)
    {
        long startTime = System.nanoTime();
        world.zoomOut(1, zoomX<<1, zoomY<<1);
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
        world.setCenterLongitude((startTime & 0xFFFFFFFFFFFFL) * 0x1p-32);
        //// maybe comment in next line if using something other than RotatingSpaceView
        //wmv.generate(world.seedA, world.seedB, world.landModifier, world.heatModifier);
        //// comment out next line if using something other than RotatingSpaceView
        wmv.getBiomeMapper().makeBiomes(world);
        wmv.show();
        ttg = System.nanoTime() - startTime >> 20;
    }


    public void putMap() {
        long startTime = System.nanoTime();
        float[][] cm = wmv.getColorMap();
        //// everything after this part of putMap() should be customized to your rendering setup
        batch.begin(view.getCamera().combined, GL20.GL_POINTS);
        float c;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                c = cm[x][y];
                if(c != WorldMapView.emptyColor) {
                    batch.color(c);
                    batch.vertex(x, y, 0f);
                }
            }
        }
        batch.end();
        //ttd = System.nanoTime() - startTime >> 20;
    }

    @Override
    public void render() {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(SColor.DB_INK.r, SColor.DB_INK.g, SColor.DB_INK.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        if(spinning)
            rotate();
        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        Gdx.graphics.setTitle("Took " + ttg + " ms to generate");//, took " + ttd + " ms to draw");

        // if we are waiting for the player's input and get input, process it.
        if (input.hasNext()) {
            input.next();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
        view.apply(true);
    }

    public static CustomConfig config = new CustomConfig("RotatingWorldMapDemo"){
        {
            setTitle("SquidLib Demo: Rotating World Map");
            setWindowedMode(width, height);
            useVsync(false);
            setForegroundFPS(0);
            setResizable(false);
            setIdleFPS(1);
        }
        @Override
        public ApplicationListener instantiate() {
            return new RotatingWorldMapDemo();
        }
    };

}
