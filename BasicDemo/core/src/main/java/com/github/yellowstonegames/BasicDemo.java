package com.github.yellowstonegames;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.ArrayTools;
import squidpony.squidai.DijkstraMap;
import squidpony.squidai.graph.DefaultGraph;
import squidpony.squidai.graph.Heuristic;
import squidpony.squidgrid.Direction;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Measurement;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.DefaultResources;
import squidpony.squidgrid.gui.gdx.FilterBatch;
import squidpony.squidgrid.gui.gdx.FloatFilters;
import squidpony.squidgrid.gui.gdx.MapUtility;
import squidpony.squidgrid.gui.gdx.PanelEffect;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.SparseLayers;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidgrid.gui.gdx.SquidMouse;
import squidpony.squidgrid.gui.gdx.TextCellFactory;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.LineKit;
import squidpony.squidmath.Coord;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.GWTRNG;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.IRNG;
import squidpony.squidmath.NumberTools;

import java.util.ArrayList;
import java.util.List;

/**
 * The main class of the game, constructed once in each of the platform-specific Launcher classes. Doesn't use any
 * platform-specific code.
 */
// If this is an example project in SquidSetup, then squidlib-util (the core of squidlib's logic code, including
// map generation and pathfinding), RegExodus (a dependency of squidlib-util, used for validating generated text and
// some other usages in a way that works cross-platform) and the squidlib (the text-based display module) are always
// dependencies. If you didn't change those dependencies, this class should run out of the box.
//
// This class is useful as a starting point, since it has dungeon generation and some of the trickier parts of input
// handling (using the mouse to get a path for the player) already filled in. You can remove any imports or usages of
// classes that you don't need.

// A main game class that uses LibGDX to display, which is the default for SquidLib, needs to extend ApplicationAdapter
// or something related, like Game. Game adds features that SquidLib doesn't currently use, so ApplicationAdapter is
// perfectly fine for these uses.
public class BasicDemo extends ApplicationAdapter {
    // FilterBatch is almost the same as SpriteBatch.
    private FilterBatch batch;

    // the random number generator to use; here the implementation is GWTRNG, which is more than fast enough on desktop
    // without sacrificing speed on the HTML target, which is traditionally a challenge.
    private IRNG rng;
    // SparseLayers shows any-color chars in a grid, and only spends time drawing chars with a background (this can be used to
    // limit the drawn cells to ones the protagonist can see or has seen). It replaces the older class SquidLayers, which is still
    // available but has a very hard time drawing large maps that are mostly empty or unseen.
    private SparseLayers display;
    // a DungeonGenerator produces dungeon, where a dungeon is just a char[][] with only a few chars used and most signifying some
    // common component of a dungeon or cave map.
    private DungeonGenerator dungeonGen;
    // decoDungeon stores the dungeon map with features like grass and water, if present, as chars like '"' and '~'.
    // bareDungeon stores the dungeon map with just walls as '#' and anything not a wall as '.'.
    // Both of the above maps use '#' for walls, and the next two use box-drawing characters instead.
    // lineDungeon stores the whole map the same as decoDungeon except for walls, which are box-drawing characters here.
    // prunedDungeon takes lineDungeon and adjusts it so unseen segments of wall (represented by box-drawing characters)
    //   are removed from rendering; unlike the others, it is frequently changed.
    private char[][] decoDungeon, bareDungeon, lineDungeon, prunedDungeon;
    // colors stores the foreground color of all dungeon contents, even before they are drawn with a SparseLayers.
    // bgColors is the same, but for background color.
    private float[][] colors, bgColors;

    //Here, gridHeight refers to the total number of rows to be displayed on the screen.
    //We're displaying 25 rows of dungeon.
    //gridHeight is 25 because that variable will be used for generating the dungeon (the actual size of the dungeon
    //will be 5x gridWidth and 5x gridHeight), and determines how much off the dungeon is visible at any time.
    //The bonusHeight is the number of additional rows that aren't handled like the dungeon rows and are shown in a
    //separate area; here this space isn't used. The gridWidth is 90, which means we show 90 grid spaces
    //across the whole screen, but the actual dungeon is larger. The cellWidth and cellHeight are 12 and 20, which will
    //match the starting dimensions of a cell in pixels, but won't be stuck at that value because we use a "Stretchable"
    //font, and so the cells can change size (they don't need to be scaled by equal amounts, either). While gridWidth
    //and gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the initial pixel dimensions of
    //one cell; resizing the window can make the units cellWidth and cellHeight use smaller or larger than a pixel.

    /** In number of cells */
    public static final int gridWidth = 60;
    /** In number of cells */
    public static final int gridHeight = 25;

    /** In number of cells */
    public static final int bigWidth = gridWidth * 5;
    /** In number of cells */
    public static final int bigHeight = gridHeight * 5;

    /** In number of cells */
    public static final int bonusHeight = 0;
    /** The pixel width of a cell */
    public static final int cellWidth = 12;
    /** The pixel height of a cell */
    public static final int cellHeight = 20;

    private static final float DURATION = 0.15f;
    private SquidInput input;
    private Color bgColor;
    private Stage stage;
    private DijkstraMap playerToCursor;
    private DefaultGraph graph;
    private Coord cursor, player;
    private ArrayList<Coord> toCursor;
    private List<Coord> awaitedMoves;

    private Vector2 screenPosition;


    // how much each cell on the grid resists light passing through; 1.0 is a wall, 0.0 is air.
    private double[][] resistance;
    // how well-lit each cell is currently as the player sees it.
    private double[][] visible;
    // GreasedRegion is a hard-to-explain class, but it's an incredibly useful one for map generation and many other
    // tasks; it stores a region of "on" cells where everything not in that region is considered "off," and can be used
    // as a Collection of Coord points. However, it's more than that! Because of how it is implemented, it can perform
    // bulk operations on as many as 64 points at a time, and can efficiently do things like expanding the "on" area to
    // cover adjacent cells that were "off", retracting the "on" area away from "off" cells to shrink it, getting the
    // surface ("on" cells that are adjacent to "off" cells) or fringe ("off" cells that are adjacent to "on" cells),
    // and generally useful things like picking a random point from all "on" cells.
    // Here, we use a GreasedRegion to store all floors that the player can walk on, a small rim of cells just beyond
    // the player's vision that blocks pathfinding to areas we can't see a path to, and we also store all cells that we
    // have seen in the past in a GreasedRegion (in most roguelikes, there would be one of these per dungeon floor).
    private GreasedRegion floors, blockage, seen, currentlySeen;
    // a Glyph is a kind of scene2d Actor that only holds one char in a specific color, but is drawn using the behavior
    // of TextCellFactory (which most text in SquidLib is drawn with) instead of the different and not-very-compatible
    // rules of Label, which older SquidLib code used when it needed text in an Actor. Glyphs are also lighter-weight in
    // memory usage and time taken to draw than Labels.
    private TextCellFactory.Glyph pg;
    // libGDX can use a kind of packed float (yes, the number type) to efficiently store colors, but it also uses a
    // heavier-weight Color object sometimes; SquidLib has a large list of SColor objects that are often used as easy
    // predefined colors since SColor extends Color. SparseLayers makes heavy use of packed float colors internally,
    // but also allows Colors instead for most methods that take a packed float. Some cases, like very briefly-used
    // colors that are some mix of two other colors, are much better to create as packed floats from other packed
    // floats, usually using SColor.lerpFloatColors(), which avoids creating any objects. It's ideal to avoid creating
    // new objects (such as Colors) frequently for only brief usage, because this can cause temporary garbage objects to
    // build up and slow down the program while they get cleaned up (garbage collection, which is slower on Android).
    // Recent versions of SquidLib include the packed float literal in the JavaDocs for any SColor, along with previews
    // of that SColor as a background and foreground when used with other colors, plus more info like the hue,
    // saturation, and value of the color. Here we just use the packed floats directly from the SColor docs, but we also
    // mention what color it is in a line comment, which is a good habit so you can see a preview if needed.
    private static final float FLOAT_LIGHTING = -0x1.cff1fep126F, // same result as SColor.COSMIC_LATTE.toFloatBits()
            GRAY_FLOAT = -0x1.7e7e7ep125F; // same result as SColor.CW_GRAY_BLACK.toFloatBits()
    // This filters colors in a way we adjust over time, producing a sort of hue shift effect.
    // It can also be used to over- or under-saturate colors, change their brightness, or any combination of these.
    private FloatFilters.YCbCrFilter filter;

    //    private FloatFilter sepia;
    @Override
    public void create () {
        Coord.expandPoolTo(bigWidth, bigHeight);
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        // if the seed is identical between two runs, any random factors will also be identical (until user input may
        // cause the usage of an RNG to change). You can randomize the dungeon and several other initial settings by
        // just removing the String seed, making the line "rng = new GWTRNG();" . Keeping the seed as a default allows
        // changes to be more easily reproducible, and using a fixed seed is strongly recommended for tests.
        rng = new GWTRNG(123456789);
        // testing FloatFilter; YCbCrFilter multiplies the brightness (Y) and chroma (Cb, Cr) of a color.
        filter = new FloatFilters.YCbCrFilter(0.875f, 0.6f, 0.6f);
//        sepia = new FloatFilters.ColorizeFilter(SColor.CLOVE_BROWN, 0.6f, 0.0f);

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        // FilterBatch is exactly like the normal libGDX SpriteBatch except that it filters all colors used for text or
        // for tinting images.
        batch = new FilterBatch(filter);
        StretchViewport mainViewport = new StretchViewport(gridWidth * cellWidth, gridHeight * cellHeight);
        mainViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(mainViewport, batch);
        // the font will try to load Inconsolata-LGC as an embedded bitmap font with a MSDF effect (multi scale distance
        // field, a way to allow a bitmap font to stretch while still keeping sharp corners and round curves).
        // the MSDF effect is handled internally by a shader in SquidLib, and will switch to a different shader if a SDF
        // effect is used (SDF is called "Stretchable" in DefaultResources, where MSDF is called "Crisp").
        // this font is covered under the SIL Open Font License (fully free), so there's no reason it can't be used.
        // it also includes 4 text faces (regular, bold, oblique, and bold oblique) so methods in GDXMarkup can make
        // italic or bold text without switching fonts (they can color sections of text too).
        display = new SparseLayers(bigWidth, bigHeight + bonusHeight, cellWidth, cellHeight,
                DefaultResources.getCrispSmoothFont());

        // for some reason, this eliminates ugly jitter in the FPS display text, which
        // only happened when moving and usually when the window was resized.
        // The smoothness of the font thanks to the MSDF (crisp) effect seems to remain clear.
        display.font.font().setUseIntegerPositions(false);

        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good winding dungeons
        //with rooms by default, but in the later call to dungeonGen.generate(), you can use a TilesetType such as
        //TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS or TilesetType.CAVES_LIMIT_CONNECTIVITY to change the sections that
        //this will use, or just pass in a full 2D char array produced from some other generator, such as
        //SerpentMapGenerator, OrganicMapGenerator, or DenseRoomMapGenerator.
        dungeonGen = new DungeonGenerator(bigWidth, bigHeight, rng);
        //uncomment this next line to randomly add water to the dungeon in pools.
        //dungeonGen.addWater(15);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
        decoDungeon = dungeonGen.generate();
        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBareDungeon();
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing char.
        //The end result looks something like this, for a smaller 60x30 map:
        //
        // ┌───┐┌──────┬──────┐┌──┬─────┐   ┌──┐    ┌──────────┬─────┐
        // │...││......│......└┘..│.....│   │..├───┐│..........│.....└┐
        // │...││......│..........├──┐..├───┤..│...└┴────......├┐.....│
        // │...││.................│┌─┘..│...│..│...............││.....│
        // │...││...........┌─────┘│....│...│..│...........┌───┴┴───..│
        // │...│└─┐....┌───┬┘      │........│..│......─────┤..........│
        // │...└─┐│....│...│       │.......................│..........│
        // │.....││........└─┐     │....│..................│.....┌────┘
        // │.....││..........│     │....├─┬───────┬─┐......│.....│
        // └┬──..└┼───┐......│   ┌─┴─..┌┘ │.......│ │.....┌┴──┐..│
        //  │.....│  ┌┴─..───┴───┘.....└┐ │.......│┌┘.....└─┐ │..│
        //  │.....└──┘..................└─┤.......││........│ │..│
        //  │.............................│.......├┘........│ │..│
        //  │.............┌──────┐........│.......│...─┐....│ │..│
        //  │...........┌─┘      └──┐.....│..─────┘....│....│ │..│
        // ┌┴─────......└─┐      ┌──┘..................│..──┴─┘..└─┐
        // │..............└──────┘.....................│...........│
        // │............................┌─┐.......│....│...........│
        // │..│..│..┌┐..................│ │.......├────┤..──┬───┐..│
        // │..│..│..│└┬──..─┬───┐......┌┘ └┐.....┌┘┌───┤....│   │..│
        // │..├──┤..│ │.....│   │......├───┘.....│ │...│....│┌──┘..└──┐
        // │..│┌─┘..└┐└┬─..─┤   │......│.........└─┘...│....││........│
        // │..││.....│ │....│   │......│...............│....││........│
        // │..││.....│ │....│   │......│..┌──┐.........├────┘│..│.....│
        // ├──┴┤...│.└─┴─..┌┘   └┐....┌┤..│  │.....│...└─────┘..│.....│
        // │...│...│.......└─────┴─..─┴┘..├──┘.....│............└─────┤
        // │...│...│......................│........│..................│
        // │.......├───┐..................│.......┌┤.......┌─┐........│
        // │.......│   └──┐..┌────┐..┌────┤..┌────┘│.......│ │..┌──┐..│
        // └───────┘      └──┘    └──┘    └──┘     └───────┘ └──┘  └──┘
        //this is also good to compare against if the map looks incorrect, and you need an example of a correct map when
        //no parameters are given to generate().
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon);

        resistance = DungeonUtility.generateResistances(decoDungeon);
        visible = new double[bigWidth][bigHeight];

        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be incredibly huge, Coord performs best for x and y values less than 256, but
        // by default it can also handle some negative x and y values (-3 is the lowest it can efficiently store). You
        // can call Coord.expandPool() or Coord.expandPoolTo() if you need larger maps to be just as fast.
        cursor = Coord.get(-1, -1);
        // here, we need to get a random floor cell to place the player upon, without the possibility of putting him
        // inside a wall. There are a few ways to do this in SquidLib. The most straightforward way is to randomly
        // choose x and y positions until a floor is found, but particularly on dungeons with few floor cells, this can
        // have serious problems -- if it takes too long to find a floor cell, either it needs to be able to figure out
        // that random choice isn't working and instead choose the first it finds in simple iteration, or potentially
        // keep trying forever on an all-wall map. There are better ways! These involve using a kind of specific storage
        // for points or regions, getting that to store only floors, and finding a random cell from that collection of
        // floors. The two kinds of such storage used commonly in SquidLib are the "packed data" as short[] produced by
        // CoordPacker (which use very little memory, but can be slow, and are treated as unchanging by CoordPacker so
        // any change makes a new array), and GreasedRegion objects (which use slightly more memory, tend to be faster
        // on almost all operations compared to the same operations with CoordPacker, and default to changing the
        // GreasedRegion object when you call a method on it instead of making a new one). Even though CoordPacker
        // sometimes has better documentation, GreasedRegion is generally a better choice; it was added to address
        // shortcomings in CoordPacker, particularly for speed, and the worst-case scenarios for data in CoordPacker are
        // no problem whatsoever for GreasedRegion. CoordPacker is called that because it compresses the information
        // for nearby Coords into a smaller amount of memory. GreasedRegion is called that because it encodes regions,
        // but is "greasy" both in the fatty-food sense of using more space, and in the "greased lightning" sense of
        // being especially fast. Both of them can be seen as storing regions of points in 2D space as "on" and "off."

        // Here we fill a GreasedRegion so it stores the cells that contain a floor, the '.' char, as "on."
        floors = new GreasedRegion(bareDungeon, '.');
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player. The singleRandom() method on GreasedRegion finds one Coord
        // in that region that is "on," or -1,-1 if there are no such cells. It takes an RNG object as a parameter, and
        // if you gave a seed to the RNG constructor, then the cell this chooses will be reliable for testing. If you
        // don't seed the RNG, any valid cell should be possible.
        player = floors.singleRandom(rng);

        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
//        display.setPosition(gridWidth * cellWidth * 0.5f - display.worldX(player.x),
//                gridHeight * cellHeight * 0.5f - display.worldY(player.y));
        display.setPosition(0f, 0f);
        // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
        FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);//, (System.currentTimeMillis() & 0xFFFF) * 0x1p-4, 60.0);

        // 0.01 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.01 will _not_ be in
        // the blockage Collection, but anything 0.01 or less will be in it. This lets us use blockage to prevent access
        // to cells we can't see from the start of the move.
        blockage = new GreasedRegion(visible, 0.0);
        seen = blockage.not().copy();
        currentlySeen = seen.copy();
        blockage.fringe8way();
        // prunedDungeon starts with the full lineDungeon, which includes features like water and grass but also stores
        // all walls as box-drawing characters. The issue with using lineDungeon as-is is that a character like '┬' may
        // be used because there are walls to the east, west, and south of it, even when the player is to the north of
        // that cell and so has never seen the southern connecting wall, and would have no reason to know it is there.
        // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
        // line segments that haven't ever been visible. This is called again whenever seen changes.
        prunedDungeon = ArrayTools.copy(lineDungeon);
        // You can call pruneLines with an optional parameter here, LineKit.lightAlt, which will allow prunedDungeon to
        // use the half-line chars "╴╵╶╷". These chars aren't supported by all fonts, including the one we use here.
        // The default is to use LineKit.light , which will replace '╴' and '╶' with '─' and '╷' and '╵' with '│'.
        LineKit.pruneLines(lineDungeon, seen, prunedDungeon);

        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<>(200);
        //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
        awaitedMoves = new ArrayList<>(200);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
        // MANHATTAN value is used, which means 4-way movement only, no diagonals possible. Alternatives are CHEBYSHEV,
        // which allows 8 directions of movement at the same cost for all directions, and EUCLIDEAN, which allows 8
        // directions, but will prefer orthogonal moves unless diagonal ones are clearly closer "as the crow flies."
        playerToCursor = new DijkstraMap(decoDungeon, Measurement.MANHATTAN);
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor.setGoal(player);
        // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
        // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
        // GreasedRegion that contains the cells just past the edge of the player's FOV area.
        playerToCursor.partialScan(null, 13, blockage, false);
        graph = new DefaultGraph(bareDungeon, true);

        //The next three lines set the background color for anything we don't draw on, but also create 2D arrays of the
        //same size as decoDungeon that store the colors for the foregrounds and backgrounds of each cell as packed
        //floats (a format SparseLayers can use throughout its API), using the colors for the cell with the same x and
        //y. By changing an item in SColor.LIMITED_PALETTE, we also change the color assigned by MapUtility to floors.
        bgColor = SColor.DARK_SLATE_GRAY;
        SColor.LIMITED_PALETTE[3] = SColor.DB_GRAPHITE;
        colors = MapUtility.generateDefaultColorsFloat(decoDungeon);
        bgColors = MapUtility.generateDefaultBGColorsFloat(decoDungeon);
//        for (int x = 0; x < bigWidth; x++) {
//            for (int y = 0; y < bigHeight; y++) {
//                colors[x][y] = f(colors[x][y]);
//                bgColors[x][y] = f(bgColors[x][y]);
//            }
//        }
        //places the player as an '@' at his position in orange.
        pg = display.glyph('@', SColor.SAFETY_ORANGE, player.x, player.y);

        // this is a big one.
        // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
        // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
        // keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
        // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
        // implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
        // Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
        // that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
        // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
        // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
        // the event when someone clicks.
        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case SquidInput.UP_ARROW:
//                    case 'k':
//                    case 'K':
                    case 'w':
                    case 'W':
                    {
                        toCursor.clear();
                        //-1 is up on the screen
                        awaitedMoves.add(player.translate(0, -1));
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
//                    case 'j':
//                    case 'J':
                    case 's':
                    case 'S':
                    {
                        toCursor.clear();
                        //+1 is down on the screen
                        awaitedMoves.add(player.translate(0, 1));
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
//                    case 'h':
//                    case 'H':
                    case 'a':
                    case 'A':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, 0));
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
//                    case 'l':
//                    case 'L':
                    case 'd':
                    case 'D':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, 0));
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                    case 'c':
                    case 'C':
                    {
                        seen.fill(true);
                        break;
                    }
                }
            }
        },
                //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
                //input and converts it to grid coordinates (here, a cell is 10 wide and 20 tall, so clicking at the
                // pixel position 16,51 will pass screenX as 1 (since if you divide 16 by 10 and round down you get 1),
                // and screenY as 2 (since 51 divided by 20 rounded down is 2)).
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

                    // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
                    // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
                    @Override
                    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                        // This is needed because we center the camera on the player as he moves through a dungeon that is three
                        // screens wide and three screens tall, but the mouse still only can receive input on one screen's worth
                        // of cells. (gridWidth >> 1) halves gridWidth, pretty much, and that we use to get the centered
                        // position after adding to the player's position (along with the gridHeight).
                        screenX += player.x - (gridWidth >> 1);
                        screenY += player.y - (gridHeight >> 1);
                        // we also need to check if screenX or screenY is out of bounds.
                        if (screenX < 0 || screenY < 0 || screenX >= bigWidth || screenY >= bigHeight)
                            return false;
                        if (awaitedMoves.isEmpty()) {
                            if (toCursor.isEmpty()) {
                                cursor = Coord.get(screenX, screenY);
                                //This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                                // player position to the position the user clicked on. The "PreScanned" part is an optimization
                                // that's special to DijkstraMap; because the whole map has already been fully analyzed by the
                                // DijkstraMap.scan() method at the start of the program, and re-calculated whenever the player
                                // moves, we only need to do a fraction of the work to find the best path with that info.
//                                toCursor.clear();
//                                playerToCursor.findPathPreScanned(toCursor, cursor);
                                if(graph.contains(cursor) && currentlySeen.contains(cursor))
                                    graph.findShortestPath(cursor, player, toCursor, Heuristic.EUCLIDEAN);
                                else
                                    return false;
                                //findPathPreScanned includes the current cell (goal) by default, which is helpful when
                                // you're finding a path to a monster or loot, and want to bump into it, but here can be
                                // confusing because you would "move into yourself" as your first move without this.
                                if(!toCursor.isEmpty())
                                    toCursor.remove(toCursor.size() - 1);
                            }
                            awaitedMoves.addAll(toCursor);
                        }
                        return true;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        return mouseMoved(screenX, screenY);
                    }

                    // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
                    // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        if(!awaitedMoves.isEmpty())
                            return false;
                        // This is needed because we center the camera on the player as he moves through a dungeon that is three
                        // screens wide and three screens tall, but the mouse still only can receive input on one screen's worth
                        // of cells. (gridWidth >> 1) halves gridWidth, pretty much, and that we use to get the centered
                        // position after adding to the player's position (along with the gridHeight).
                        screenX += player.x - (gridWidth >> 1);
                        screenY += player.y - (gridHeight >> 1);
                        // we also need to check if screenX or screenY is out of bounds.
                        if(screenX < 0 || screenY < 0 || screenX >= bigWidth || screenY >= bigHeight ||
                                (cursor.x == screenX && cursor.y == screenY))
                        {
                            return false;
                        }
                        cursor = Coord.get(screenX, screenY);
                        //This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                        // player position to the position the user clicked on. The "PreScanned" part is an optimization
                        // that's special to DijkstraMap; because the whole map has already been fully analyzed by the
                        // DijkstraMap.scan() method at the start of the program, and re-calculated whenever the player
                        // moves, we only need to do a fraction of the work to find the best path with that info.

//                        toCursor.clear();
//                        playerToCursor.findPathPreScanned(toCursor, cursor);
                        if(graph.contains(cursor) && currentlySeen.contains(cursor))
                            graph.findShortestPath(cursor, player, toCursor, Heuristic.EUCLIDEAN);
                        else
                            return false;
                        //findPathPreScanned includes the current cell (goal) by default, which is helpful when
                        // you're finding a path to a monster or loot, and want to bump into it, but here can be
                        // confusing because you would "move into yourself" as your first move without this.
                        if(!toCursor.isEmpty())
                            toCursor.remove(toCursor.size() - 1);
                        return false;
                    }
                }));
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        // and then add display, our one visual component, to the list of things that act in Stage.
        stage.addActor(display);

        screenPosition = new Vector2(cellWidth, cellHeight);
    }
    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(final int xmod, final int ymod) {
        int newX = player.x + xmod, newY = player.y + ymod;
        if (newX >= 0 && newY >= 0 && newX < bigWidth && newY < bigHeight
                && bareDungeon[newX][newY] != '#')
        {
            display.slide(pg, player.x, player.y, newX, newY, DURATION, null);
            player = player.translate(xmod, ymod);
            FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);//, (System.currentTimeMillis() & 0xFFFF) * 0x1p-4, 60.0);
            // This is just like the constructor used earlier, but affects an existing GreasedRegion without making
            // a new one just for this movement.
            blockage.refill(visible, 0.0);
            seen.or(currentlySeen.remake(blockage.not()));
            blockage.fringe8way();
            // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
            // line segments that haven't ever been visible. This is called again whenever seen changes.
            LineKit.pruneLines(lineDungeon, seen, LineKit.light, prunedDungeon);
        }
        else
        {
            // A SparseLayers knows how to move a Glyph (like the one for the player, pg) out of its normal alignment
            // on the grid, and also how to move it back again. Using bump() will move pg quickly about a third of the
            // way into a wall, then back to its former position at normal speed.
            display.bump(pg, Direction.getRoughDirection(xmod, ymod), DURATION * 2f);
            // PanelEffect is a type of Action (from libGDX) that can run on a SparseLayers or SquidPanel.
            // This particular kind of PanelEffect creates a purple glow around the player when he bumps into a wall.
            // Other kinds can make explosions or projectiles appear.
            display.addAction(new PanelEffect.PulseEffect(display, 1f, currentlySeen, player, 3
                    , new float[]{SColor.CW_FADED_PURPLE.toFloatBits()}
            ));
            // recolor() will change the color of a cell over time from what it is currently to a target color, which is
            // DB_BLOOD here from a DawnBringer palette. We give it a Runnable to run after the effect finishes, which
            // permanently sets the color of the cell you bumped into to the color of your bloody nose. Without such a
            // Runnable, the cell would get drawn over with its normal wall color.
            display.recolor(0f, player.x + xmod, player.y + ymod, 0, SColor.DB_BLOOD.toFloatBits(), 0.4f, new Runnable() {
                int x = player.x + xmod;
                int y = player.y + ymod;
                @Override
                public void run() {
                    colors[x][y] = SColor.DB_BLOOD.toFloatBits();
                }
            });
            //display.addAction(new PanelEffect.ExplosionEffect(display, 1f, floors, player, 6));
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        //In many other situations, you would clear the drawn characters to prevent things that had been drawn in the
        //past from affecting the current frame. This isn't a problem here, but would probably be an issue if we had
        //monsters running in and out of our vision. If artifacts from previous frames show up, uncomment the next line.
        //display.clear();
        filter.crMul = NumberTools.swayRandomized(123456789L, (System.currentTimeMillis() & 0x1FFFFFL) * 0x1.2p-10f) * 1.75f;
        filter.cbMul = NumberTools.swayRandomized(987654321L, (System.currentTimeMillis() & 0x1FFFFFL) * 0x1.1p-10f) * 1.75f;
        // The loop here only will draw tiles if they are potentially in the visible part of the map.
        // It starts at an x,y position equal to the player's position minus half of the shown gridWidth and gridHeight,
        // minus one extra cell to allow the camera some freedom to move. This position won't go lower than 0. The
        // rendering in each direction ends when the edge of the map (bigWidth or bigHeight) is reached, or if
        // gridWidth/gridHeight + 2 cells have been rendered (the + 2 is also for the camera movement).
        for (int x = Math.max(0, player.x - (gridWidth >> 1) - 1), i = 0; x < bigWidth && i < gridWidth + 2; x++, i++) {
            for (int y = Math.max(0, player.y - (gridHeight >> 1) - 1), j = 0; y < bigHeight && j < gridHeight + 2; y++, j++) {
                if (visible[x][y] > 0.0) {
                    // Here we use a convenience method in SparseLayers that puts a char at a specified position (the
                    // first three parameters), with a foreground color for that char (fourth parameter), as well as
                    // placing a background tile made of a one base color (fifth parameter) that is adjusted to bring it
                    // closer to FLOAT_LIGHTING (sixth parameter) based on how visible the cell is (seventh parameter,
                    // comes from the FOV calculations) in a way that fairly-quickly changes over time.
                    // This effect appears to shrink and grow in a circular area around the player, with the lightest
                    // cells around the player and dimmer ones near the edge of vision. This lighting is "consistent"
                    // because all cells at the same distance will have the same amount of lighting applied.
                    // We use prunedDungeon here so segments of walls that the player isn't aware of won't be shown.
                    display.putWithConsistentLight(x, y, prunedDungeon[x][y], colors[x][y], bgColors[x][y], FLOAT_LIGHTING, visible[x][y]);
                } else if (seen.contains(x, y))
                    display.put(x, y, prunedDungeon[x][y], colors[x][y], SColor.lerpFloatColors(bgColors[x][y], GRAY_FLOAT, 0.45f));
            }
        }
        Coord pt;
        for (int i = 0; i < toCursor.size(); i++) {
            pt = toCursor.get(i);
            // use a brighter light to trace the path to the cursor, mixing the background color with mostly white.
            display.put(pt.x, pt.y, SColor.lightenFloat(bgColors[pt.x][pt.y], 0.85f));
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getCamera().position.x = pg.getX();
        stage.getCamera().position.y =  pg.getY();

        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {
            // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
            if (!display.hasActiveAnimations()) {
                Coord m = awaitedMoves.remove(awaitedMoves.size() - 1);
                if(!toCursor.isEmpty())
                    toCursor.remove(toCursor.size() - 1);
                move(m.x - player.x, m.y - player.y);
                // this only happens if we just removed the last Coord from awaitedMoves, and it's only then that we need to
                // re-calculate the distances from all cells to the player. We don't need to calculate this information on
                // each part of a many-cell move (just the end), nor do we need to calculate it whenever the mouse moves.
                if (awaitedMoves.isEmpty()) {
                    // the next two lines remove any lingering data needed for earlier paths
                    playerToCursor.clearGoals();
                    playerToCursor.resetMap();
                    // the next line marks the player as a "goal" cell, which seems counter-intuitive, but it works because all
                    // cells will try to find the distance between themselves and the nearest goal, and once this is found, the
                    // distances don't change as long as the goals don't change. Since the mouse will move and new paths will be
                    // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell (the
                    // player's position), and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
                    // currently-moused-over cell, which we only need to set where the mouse is being handled.
                    playerToCursor.setGoal(player);
                    // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
                    // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
                    // GreasedRegion that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(null, 13, blockage, false);
                }
            }
        }
        // if we are waiting for the player's input and get input, process it.
        else if(input.hasNext()) {
            input.next();
        }
        // certain classes that use scene2d Actors need to be told to act() to process input.
        stage.act();
        // we have the main stage set itself up here.
        stage.getViewport().apply(false);
        // stage has its own batch and must be explicitly told to draw().
        // this is the internal code of Stage.draw() mixed with code to set screenPosition.
        batch.setProjectionMatrix(stage.getCamera().combined);
        screenPosition.set(input.getMouse().getCellWidth() * 6, input.getMouse().getCellHeight());
        stage.screenToStageCoordinates(screenPosition);
        batch.begin();
        stage.getRoot().draw(batch, 1);
        display.font.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", screenPosition.x, screenPosition.y);
        batch.end();
        Gdx.graphics.setTitle("Basic Demo running at FPS: " + Gdx.graphics.getFramesPerSecond());
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // message box won't respond to clicks on the far right if the stage hasn't been updated with a larger size
        float currentZoomX = (float)width / gridWidth;
        // total new screen height in pixels divided by total number of rows on the screen
        float currentZoomY = (float)height / (gridHeight + bonusHeight);
        // SquidMouse turns screen positions to cell positions, and needs to be told that cell sizes have changed
        // a quirk of how the camera works requires the mouse to be offset by half a cell if the width or height is odd
        // (gridWidth & 1) is 1 if gridWidth is odd or 0 if it is even; it's good to know and faster than using % , plus
        // in some other cases it has useful traits (x % 2 can be 0, 1, or -1 depending on whether x is negative, while
        // x & 1 will always be 0 or 1).
        input.getMouse().reinitialize(currentZoomX, currentZoomY, gridWidth, gridHeight,
                (gridWidth & 1) * (int)(currentZoomX * -0.5f), (gridHeight & 1) * (int) (currentZoomY * -0.5f));
        stage.getViewport().update(width, height, false);
    }
}