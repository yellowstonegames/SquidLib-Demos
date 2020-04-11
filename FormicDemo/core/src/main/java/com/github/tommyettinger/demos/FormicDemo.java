package com.github.tommyettinger.demos;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.FakeLanguageGen;
import squidpony.squidgrid.gui.gdx.DefaultResources;
import squidpony.squidgrid.gui.gdx.FilterBatch;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.SparseLayers;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidmath.GWTRNG;
import text.formic.Stringf;

/**
 * This is a small, not-overly-simple demo that presents some important features of SquidLib and shows a faster,
 * cleaner, and more recently-introduced way of displaying the map and other text. Features include dungeon map
 * generation, field of view, pathfinding (to the mouse position), continuous noise (used for a wavering torch effect),
 * language generation/ciphering, a colorful glow effect, and ever-present random number generation (with a seed).
 * You can increase the size of the map on most target platforms (but GWT struggles with large... anything) by
 * changing gridHeight and gridWidth to affect the visible area or bigWidth and bigHeight to adjust the size of the
 * dungeon you can move through, with the camera following your '@' symbol.
 * <br>
 * The assets folder of this project, if it was created with SquidSetup, will contain the necessary font files (just one
 * .fnt file and one .png are needed, but many more are included by default). You should move any font files you don't
 * use out of the assets directory when you produce a release JAR, APK, or GWT build.
 */
public class FormicDemo extends ApplicationAdapter {
    // FilterBatch is almost the same as SpriteBatch, but is a bit faster with SquidLib and allows color filtering
    private FilterBatch batch;
    // a type of random number generator, see below
    private GWTRNG rng;
    // rendering classes that show only the chars and backgrounds that they've been told to render, unlike some earlier
    // classes in SquidLib.
    private SparseLayers languageDisplay;

    //Here, gridHeight refers to the total number of rows to be displayed on the screen.
    //We're displaying 25 rows of dungeon, then 7 more rows of text generation to show some tricks with language.
    //gridHeight is 25 because that variable will be used for generating the dungeon (the actual size of the dungeon
    //will be triple gridWidth and triple gridHeight), and determines how much off the dungeon is visible at any time.
    //The bonusHeight is the number of additional rows that aren't handled like the dungeon rows and are shown in a
    //separate area; here we use them for translations. The gridWidth is 90, which means we show 90 grid spaces
    //across the whole screen, but the actual dungeon is larger. The cellWidth and cellHeight are 10 and 20, which will
    //match the starting dimensions of a cell in pixels, but won't be stuck at that value because we use a "Stretchable"
    //font, and so the cells can change size (they don't need to be scaled by equal amounts, either). While gridWidth
    //and gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the initial pixel dimensions of
    //one cell; resizing the window can make the units cellWidth and cellHeight use smaller or larger than a pixel.

    /** In number of cells */
    public static final int gridWidth = 120;
    /** In number of cells */
    public static final int gridHeight = 25;

    /** The pixel width of a cell */
    public static final int cellWidth = 8;
    /** The pixel height of a cell */
    public static final int cellHeight = 20;
    private SquidInput input;
    private Color bgColor;
    private Stage languageStage;

    private static final float FLOAT_LIGHTING = -0x1.cff1fep126F, // same result as SColor.COSMIC_LATTE.toFloatBits()
            GRAY_FLOAT = -0x1.7e7e7ep125F; // same result as SColor.CW_GRAY_BLACK.toFloatBits()
    @Override
    public void create () {
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        // if the seed is identical between two runs, any random factors will also be identical (until user input may
        // cause the usage of an RNG to change). You can randomize the dungeon and several other initial settings by
        // just removing the String seed, making the line "rng = new GWTRNG();" . Keeping the seed as a default allows
        // changes to be more easily reproducible, and using a fixed seed is strongly recommended for tests. 

        // SquidLib has many methods that expect an IRNG instance, and there's several classes to choose from.
        // In this program we'll use GWTRNG, which will behave better on the HTML target than other generators.
        rng = new GWTRNG(123456789);

        // FilterBatch is exactly like libGDX' SpriteBatch, except it is a fair bit faster when the Batch color is set
        // often (which is always true for SquidLib's text-based display), and it allows a FloatFilter to be optionally
        // set that can adjust colors in various ways. The FloatFilter here, a YCwCmFilter, can have its adjustments to
        // brightness (Y, also called luma), warmth (blue/green vs. red/yellow) and mildness (blue/red vs. green/yellow)
        // changed at runtime, and the putMap() method does this. This can be very powerful; you might increase the
        // warmth of all colors (additively) if the player is on fire, for instance.
        batch = new FilterBatch();
        StretchViewport languageViewport = new StretchViewport(gridWidth * cellWidth, gridHeight * cellHeight);
        languageViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        languageStage = new Stage(languageViewport, batch);

        languageDisplay = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, DefaultResources.getCrispSlabFont());
        languageDisplay.defaultPackedBackground = SColor.FLOAT_WHITE;
        languageDisplay.setPosition(0f, 0f);
        
        bgColor = SColor.WHITE;
        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                default:
                    {
                        putMap();
                        break;
                    }
                }
            }
        });
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(languageStage, input));
        languageStage.addActor(languageDisplay);
        putMap();
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        languageDisplay.clear(0);
        languageDisplay.fillBackground(languageDisplay.defaultPackedBackground);
        for (int i = 0; i < gridHeight; i++) {
            String s = Stringf.format("%11d %<08X %12s %10.10f %<10.10g %<10.10E %<10.10a",
                rng.nextInt(), 
                FakeLanguageGen.CELESTIAL.word(rng, true, 3),
                rng.nextDouble() / (1.0 - rng.nextDouble()));
            languageDisplay.put(1, i, s,
                rng.getRandomElement(SColor.COLOR_WHEEL_PALETTE));
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(input.hasNext()) {
            input.next();
        }
        // we need to do some work with viewports here so the language display (or game info messages in a real game)
        // will display in the same place even though the map view will move around. We have the language stuff set up
        // its viewport so it is in place and won't be altered by the map. Then we just tell the Stage for the language
        // texts to draw.
        languageStage.getViewport().apply(false);
        languageStage.draw();
        Gdx.graphics.setTitle("Formic Demo running at FPS: " + Gdx.graphics.getFramesPerSecond());
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // message box won't respond to clicks on the far right if the stage hasn't been updated with a larger size
        float currentZoomX = (float)width / gridWidth;
        // total new screen height in pixels divided by total number of rows on the screen
        float currentZoomY = (float)height / (gridHeight);
        // message box should be given updated bounds since I don't think it will do this automatically
        languageDisplay.setBounds(0, 0, width, currentZoomY * gridHeight);
        // the viewports are updated separately so each doesn't interfere with the other's drawn area.
        languageStage.getViewport().update(width, height, false);
        // we also set the bounds of that drawn area here for each viewport.
        languageStage.getViewport().setScreenBounds(0, 0, width, height);
    }
}
