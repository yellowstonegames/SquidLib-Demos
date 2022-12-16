package com.github.tommyettinger;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.ds.IntObjectMap;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.random.ChopRandom;
import com.github.yellowstonegames.grid.*;
import com.github.yellowstonegames.path.DijkstraMap;
import com.github.yellowstonegames.place.DungeonProcessor;
import com.github.yellowstonegames.smooth.AnimatedGlidingSprite;
import com.github.yellowstonegames.smooth.CoordGlider;
import com.github.yellowstonegames.smooth.Director;
import com.github.yellowstonegames.smooth.VectorSequenceGlider;
import com.github.yellowstonegames.text.Language;

import static com.badlogic.gdx.Gdx.input;
import static com.badlogic.gdx.Input.Keys.*;
import static com.github.yellowstonegames.core.DescriptiveColor.*;

public class DawnSquad extends ApplicationAdapter {
    private static final float DURATION = 0.375f;
    private long startTime;
    private enum Phase {WAIT, PLAYER_ANIM, MONSTER_ANIM}
    private SpriteBatch batch;
    private Phase phase;

    // random number generator
    private ChopRandom rng;

    // This maps chars, such as '#', as keys to specific images, such as a pillar.
    private IntObjectMap<TextureAtlas.AtlasRegion> charMapping;

    private char[][] bareDungeon, lineDungeon, prunedDungeon;
    // these use packed RGBA8888 int colors, which avoid the overhead of creating new Color objects
    private int[][] bgColors;
    private Coord player;
    private Coord[] playerArray = {player};
    private final int fovRange = 8;
    private final Vector2 pos = new Vector2();

    //Here, gridHeight refers to the total number of rows to be displayed on the screen.
    //We're displaying 32 rows of dungeon, then 1 more row of player stats, like current health.
    //gridHeight is 32 because that variable will be used for generating the dungeon (the actual size of the dungeon
    //will be double gridWidth and double gridHeight), and determines how much off the dungeon is visible at any time.
    //The bonusHeight is the number of additional rows that aren't handled like the dungeon rows and are shown in a
    //separate area; here we use one row for player stats. The gridWidth is 48, which means we show 48 grid spaces
    //across the whole screen, but the actual dungeon is larger. The cellWidth and cellHeight are each 16, which will
    //match the starting dimensions of a cell in pixels, but won't be stuck at that value because a PixelPerfectViewport
    //is used and will increase the cell size in multiples of 16 when the window is resized. While gridWidth and
    //gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the initial pixel dimensions of one
    //cell; resizing the window can make the units cellWidth and cellHeight use smaller or larger than a pixel.

    /** In number of cells */
    public static final int gridWidth = 32;
    /** In number of cells */
    public static final int gridHeight = 24;

    /** In number of cells */
    public static final int bigWidth = gridWidth * 2;
    /** In number of cells */
    public static final int bigHeight = gridHeight * 2;

//    /** In number of cells */
//    public static final int bonusHeight = 0;
    /** The pixel width of a cell */
    public static final int cellWidth = 32;
    /** The pixel height of a cell */
    public static final int cellHeight = 32;

    private Color bgColor;
    private BitmapFont font;
    private TextureAtlas atlas;
    private Viewport mainViewport;
    private Camera camera;

    private CoordObjectOrderedMap<AnimatedGlidingSprite> monsters;
    private AnimatedGlidingSprite playerSprite;
    private Director<AnimatedGlidingSprite> playerDirector;
    private Director<Coord> monsterDirector, directorSmall;
    private DijkstraMap getToPlayer, playerToCursor;
    private Coord cursor;
    private final ObjectList<Coord> toCursor = new ObjectList<>(100);
    private final ObjectList<Coord> awaitedMoves = new ObjectList<>(200);
    private final ObjectList<Coord> nextMovePositions = new ObjectList<>(200);
    private String lang;
    private float[][] resistance;
    private float[][] visible;
    private TextureAtlas.AtlasRegion solid;
    private int health;

    private Region blockage;
    private Region seen;

    private static final int
            INT_WHITE = -1,
            INT_BLACK = 255,
            INT_BLOOD = describe("dark dull red"),
            INT_LIGHTING = describe("lighter dullest white yellow"),
            INT_GRAY = describe("darker gray");

    private boolean onGrid(int screenX, int screenY)
    {
        return screenX >= 0 && screenX < bigWidth && screenY >= 0 && screenY < bigHeight;
    }

    /**
     * Just the parts of create() that can be called again if the game is reloaded.
     */
    public void restart() {
        restart(TimeUtils.millis() ^ System.identityHashCode(this));
    }
    /**
     * Just the parts of create() that can be called again if the game is reloaded.
     */
    public void restart(long seed) {
        health = 9;
        phase = Phase.WAIT;
        toCursor.clear();
        awaitedMoves.clear();
        nextMovePositions.clear();
        // Starting time for the game; other times are measured relative to this so that they aren't huge numbers.
        startTime = TimeUtils.millis();
        // We just need to have a random number generator.
        // This is seeded the same every time.
        rng = new ChopRandom(seed);
        // Using this would give a different dungeon every time.
//        rng = new ChopRandom(startTime);

        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good winding dungeons
        //with rooms by default, but in the later call to dungeonGen.generate(), you can use a TilesetType such as
        //TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS or TilesetType.CAVES_LIMIT_CONNECTIVITY to change the sections that
        //this will use, or just pass in a full 2D char array produced from some other generator, such as
        //SerpentMapGenerator, OrganicMapGenerator, or DenseRoomMapGenerator.
        DungeonProcessor dungeonGen = new DungeonProcessor(bigWidth, bigHeight, rng);
        //this next line randomly adds water to the dungeon in pools.
        dungeonGen.addWater(DungeonProcessor.ALL, 12);
        //this next line makes 10% of valid door positions into complete doors.
        dungeonGen.addDoors(10, true);
        //this next line randomly adds water to the cave parts of the dungeon in patches.
        dungeonGen.addGrass(DungeonProcessor.ALL, 10);
        //some boulders make the map a little more tactically interesting, and show how the FOV works.
        dungeonGen.addBoulders(DungeonProcessor.ALL, 5);
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
        lineDungeon = LineTools.hashesToLines(dungeonGen.generate(), true);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBarePlaceGrid();

        resistance = FOV.generateSimpleResistances(lineDungeon);
        visible = new float[bigWidth][bigHeight];
        prunedDungeon = ArrayTools.copy(lineDungeon);
        // here, we need to get a random floor cell to place the player upon, without the possibility of putting him
        // inside a wall. There are a few ways to do this in SquidSquad. The most straightforward way is to randomly
        // choose x and y positions until a floor is found, but particularly on dungeons with few floor cells, this can
        // have serious problems -- if it takes too long to find a floor cell, either it needs to be able to figure out
        // that random choice isn't working and instead choose the first it finds in simple iteration, or potentially
        // keep trying forever on an all-wall map. There are better ways! These involve using a kind of specific storage
        // for points or regions, getting that to store only floors, and finding a random cell from that collection of
        // floors. SquidSquad provides the Region class to handle on-or-off regions of positions in a larger grid. It's
        // relatively efficient to get a random point from a Region, especially on maps with few valid points to choose;
        // there are lots of other features Region has that make it a good choice for lots of location-related code.

        // Here we fill a Region; it stores the cells that contain a floor, the '.' char, as "on."
        // Region is a hard-to-explain class, but it's an incredibly useful one for map generation and many other tasks;
        // it stores a region of "on" cells where everything not in that region is considered "off," and can be used as
        // a Collection of Coord points. However, it's more than that! Because of how it is implemented, it can perform
        // bulk operations on as many as 64 points at a time, and can efficiently do things like expanding the "on" area
        // to cover adjacent cells that were "off", retracting the "on" area away from "off" cells to shrink it, getting
        // the surface ("on" cells that are adjacent to "off" cells) or fringe ("off" cells that are adjacent to "on"
        // cells), and generally useful things like picking a random point from all "on" cells. Here, we use a Region to
        // store all floors that the player can walk on, a small rim of cells just beyond the player's vision that
        // blocks pathfinding to areas we can't see a path to, and we also store all cells that we have seen in the past
        // in a Region (in most roguelikes, there would be one of these per dungeon floor).
        Region floors = new Region(bareDungeon, '.');
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player. The singleRandom() method on Region finds one Coord
        //in that region that is "on," or -1,-1 if there are no such cells. It takes an RNG object as a parameter, and
        //if you gave a seed to the RNG constructor, then the cell this chooses will be reliable for testing. If you
        //don't seed the RNG, any valid cell should be possible.
        player = floors.singleRandom(rng);
        playerSprite = new AnimatedGlidingSprite(new Animation<>(DURATION,
                atlas.findRegions(rng.randomElement(Data.possibleCharacters)), Animation.PlayMode.LOOP), player);
        playerSprite.setSize(1f, 1f);
        playerDirector = new Director<>(AnimatedGlidingSprite::getLocation, ObjectList.with(playerSprite), 150);
        // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
        FOV.reuseFOV(resistance, visible, player.x, player.y, 9f, Radius.CIRCLE);
        // 0.0 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.0 will _not_ be in
        // the blockage Collection, but anything 0.0 or less will be in it. This lets us use blockage to prevent access
        // to cells we can't see from the start of the move.
        blockage = new Region(visible, 0f);
        // Here we mark the initially seen cells as anything that wasn't included in the unseen "blocked" region.
        // We invert the copy's contents to prepare for a later step, which makes blockage contain only the cells that
        // are above 0.0, then copy it to save this step as the seen cells. We will modify seen later independently of
        // the blocked cells, so a copy is correct here. Most methods on Region objects will modify the
        // Region they are called on, which can greatly help efficiency on long chains of operations.
        seen = blockage.not().copy();
        // Here is one of those methods on a Region; fringe8way takes a Region (here, the set of cells
        // that are visible to the player), and modifies it to contain only cells that were not in the last step, but
        // were adjacent to a cell that was present in the last step. This can be visualized as taking the area just
        // beyond the border of a region, using 8-way adjacency here because we specified fringe8way instead of fringe.
        // We do this because it means pathfinding will only have to work with a small number of cells (the area just
        // out of sight, and no further) instead of all invisible cells when figuring out if something is currently
        // impossible to enter.
        blockage.fringe8way();
        LineTools.pruneLines(lineDungeon, seen, prunedDungeon);
        floors.remove(player);
        int numMonsters = 100;
        monsters = new CoordObjectOrderedMap<>(numMonsters);
        for (int i = 0; i < numMonsters; i++) {
            Coord monPos = floors.singleRandom(rng);
            floors.remove(monPos);
            String enemy = rng.randomElement(Data.possibleEnemies);
            AnimatedGlidingSprite monster =
                    new AnimatedGlidingSprite(new Animation<>(DURATION,
                            atlas.findRegions(enemy), Animation.PlayMode.LOOP), monPos);
            monster.setSize(1f, 1f);
//            monster.setPackedColor(ColorTools.floatGetHSV(rng.nextFloat(), 0.75f, 0.8f, 0f));
            monsters.put(monPos, monster);
        }
//        monsterDirector = new Director<>((e) -> e.getValue().getLocation(), monsters, 125);
        monsterDirector = new Director<>(c -> monsters.get(c).getLocation(), monsters.order(), 150);
        directorSmall = new Director<>(c -> monsters.get(c).getSmallMotion(), monsters.order(), 300L);
        //This is used to allow clicks or taps to take the player to the desired area.
        //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
        //Measurement used is EUCLIDEAN, which allows 8 directions, but will prefer orthogonal moves unless diagonal
        //ones are clearly closer "as the crow flies." Alternatives are MANHATTAN, which means 4-way movement only, no
        //diagonals possible, and CHEBYSHEV, which allows 8 directions of movement at the same cost for all directions.
        playerToCursor = new DijkstraMap(bareDungeon, Measurement.EUCLIDEAN);
        getToPlayer = new DijkstraMap(bareDungeon, Measurement.EUCLIDEAN);
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor.setGoal(player);
        // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
        // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
        // Region that contains the cells just past the edge of the player's FOV area.
        playerToCursor.partialScan(13, blockage);

        lang = '"' + Language.DEMONIC.sentence(rng, 4, 7,
                new String[]{",", ",", ",", " -"}, new String[]{"...\"", ", heh...\"", ", nyehehe...\"",  "!\"", "!\"", "!\"", "!\" *PTOOEY!*",}, 0.2);

    }

    @Override
    public void create () {

        Gdx.app.setLogLevel(Application.LOG_ERROR);

        // We need access to a batch to render most things.
        batch = new SpriteBatch();

        mainViewport = new ScalingViewport(Scaling.fill, gridWidth, gridHeight);
        mainViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        camera = mainViewport.getCamera();
        camera.update();

        // Stores all images we use here efficiently, as well as the font image
        atlas = new TextureAtlas(Gdx.files.internal("dawnlike/Dawnlike2.atlas"), Gdx.files.internal("dawnlike"));
        font = new BitmapFont(Gdx.files.internal("dawnlike/font2.fnt"), atlas.findRegion("font"));
//        font = new BitmapFont(Gdx.files.internal("dawnlike/PlainAndSimplePlus.fnt"), atlas.findRegion("PlainAndSimplePlus"));
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f/cellWidth, 1f/cellHeight);
        font.getData().markupEnabled = true;
        bgColors = ArrayTools.fill(0x808080FF, bigWidth, bigHeight);
        solid = atlas.findRegion("pixel");
        charMapping = new IntObjectMap<>(64);

        charMapping.put('.', atlas.findRegion("day tile floor c"));
        charMapping.put(',', atlas.findRegion("brick clear pool center"      ));
        charMapping.put('~', atlas.findRegion("brick murky pool center"      ));
        charMapping.put('"', atlas.findRegion("dusk grass floor c"      ));
        charMapping.put('#', atlas.findRegion("lit brick wall center"     ));
        charMapping.put('+', atlas.findRegion("closed wooden door front")); //front
        charMapping.put('/', atlas.findRegion("open wooden door side"  )); //side
        charMapping.put('┌', atlas.findRegion("lit brick wall right down"            ));
        charMapping.put('└', atlas.findRegion("lit brick wall right up"            ));
        charMapping.put('┴', atlas.findRegion("lit brick wall left right up"           ));
        charMapping.put('┬', atlas.findRegion("lit brick wall left right down"           ));
        charMapping.put('─', atlas.findRegion("lit brick wall left right"            ));
        charMapping.put('│', atlas.findRegion("lit brick wall up down"            ));
        charMapping.put('├', atlas.findRegion("lit brick wall right up down"           ));
        charMapping.put('┼', atlas.findRegion("lit brick wall left right up down"          ));
        charMapping.put('┤', atlas.findRegion("lit brick wall left up down"           ));
        charMapping.put('┘', atlas.findRegion("lit brick wall left up"            ));
        charMapping.put('┐', atlas.findRegion("lit brick wall left down"            ));

        charMapping.put(' ', atlas.findRegion("lit brick wall up down"            ));

        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be incredibly huge, Coord performs best for x and y values less than 256, but
        // by default it can also handle some negative x and y values (-3 is the lowest it can efficiently store). You
        // can call Coord.expandPool() or Coord.expandPoolTo() if you need larger maps to be just as fast.
        cursor = Coord.get(-1, -1);

        bgColor = Color.BLACK;

        restart(0);

        //+1 is up on the screen
        //-1 is down on the screen
        // if the user clicks and mouseMoved hasn't already assigned a path to toCursor, then we call mouseMoved
        // ourselves and copy toCursor over to awaitedMoves.
        // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
        // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
        // we also need to check if screenX or screenY is the same cell.
        // This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
        // player position to the position the user clicked on. The "PreScanned" part is an optimization
        // that's special to DijkstraMap; because the part of the map that is viable to move into has
        // already been fully analyzed by the DijkstraMap.partialScan() method at the start of the
        // program, and re-calculated whenever the player moves, we only need to do a fraction of the
        // work to find the best path with that info.
        // findPathPreScanned includes the current cell (goal) by default, which is helpful when
        // you're finding a path to a monster or loot, and want to bump into it, but here can be
        // confusing because you would "move into yourself" as your first move without this.
        InputProcessor input = new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                switch (keycode) {
                    case P:
                        debugPrintVisible();
                        break;
                    case ESCAPE:
                        Gdx.app.exit();
                        break;
                }
                return true;
            }

            // if the user clicks and mouseMoved hasn't already assigned a path to toCursor, then we call mouseMoved
            // ourselves and copy toCursor over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                pos.set(screenX, screenY);
                mainViewport.unproject(pos);
                if (onGrid(MathUtils.floor(pos.x), MathUtils.floor(pos.y))) {
                    mouseMoved(screenX, screenY);
                    awaitedMoves.addAll(toCursor);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return mouseMoved(screenX, screenY);
            }

            // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
            // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (!awaitedMoves.isEmpty())
                    return false;
                pos.set(screenX, screenY);
                mainViewport.unproject(pos);
                if (onGrid(screenX = MathUtils.floor(pos.x), screenY = MathUtils.floor(pos.y))) {
                    // we also need to check if screenX or screenY is the same cell.
                    if (cursor.x == screenX && cursor.y == screenY) {
                        return false;
                    }
                    cursor = Coord.get(screenX, screenY);
                    // This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                    // player position to the position the user clicked on. The "PreScanned" part is an optimization
                    // that's special to DijkstraMap; because the part of the map that is viable to move into has
                    // already been fully analyzed by the DijkstraMap.partialScan() method at the start of the
                    // program, and re-calculated whenever the player moves, we only need to do a fraction of the
                    // work to find the best path with that info.
                    toCursor.clear();
                    playerToCursor.findPathPreScanned(toCursor, cursor);
                    // findPathPreScanned includes the current cell (goal) by default, which is helpful when
                    // you're finding a path to a monster or loot, and want to bump into it, but here can be
                    // confusing because you would "move into yourself" as your first move without this.
                    if (!toCursor.isEmpty()) {
                        toCursor.remove(0);
                    }
                }
                return false;
            }
        };
        Gdx.input.setInputProcessor(input);
    }

    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param next
     */
    private void move(Coord next) {
        CoordGlider cg = playerSprite.location;
        // this prevents movements from restarting while a slide is already in progress.
        if(cg.getChange() != 0f && cg.getChange() != 1f) return;

        int newX = next.x, newY = next.y;
        if (health <= 0) return;
        playerSprite.setPackedColor(Color.WHITE_FLOAT_BITS);
        if (newX >= 0 && newY >= 0 && newX < bigWidth && newY < bigHeight
                && bareDungeon[newX][newY] != '#') {
            // '+' is a door.
            if (prunedDungeon[newX][newY] == '+') {
                prunedDungeon[newX][newY] = '/';
                lineDungeon[newX][newY] = '/';
                // changes to the map mean the resistances for FOV need to be regenerated.
                resistance = FOV.generateSimpleResistances(prunedDungeon);
                // recalculate FOV, store it in fovmap for the render to use.
                FOV.reuseFOV(resistance, visible, player.x, player.y, fovRange, Radius.CIRCLE);
                blockage.refill(visible, 0f);
                seen.or(blockage.not());
                blockage.fringe8way();
                LineTools.pruneLines(lineDungeon, seen, prunedDungeon);
            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                FOV.reuseFOV(resistance, visible, newX, newY, fovRange, Radius.CIRCLE);
                blockage.refill(visible, 0f);
                seen.or(blockage.not());
                blockage.fringe8way();
                LineTools.pruneLines(lineDungeon, seen, prunedDungeon);
                playerSprite.location.setStart(player);
                playerSprite.location.setEnd(player = next);
                phase = Phase.PLAYER_ANIM;
                playerDirector.play();

                // if a monster was at the position we moved into, and so was successfully removed...
                if(monsters.containsKey(player))
                {
                    monsters.remove(player);
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            if(rng.nextBoolean())
                                bgColors[newX+x][newY+y] = INT_BLOOD;
                        }
                    }
                }
            }
            phase = Phase.PLAYER_ANIM;
        }
    }

    private void postMove()
    {
        phase = Phase.MONSTER_ANIM;
        // updates our mutable player array in-place, because a Coord like player is immutable.
        playerArray[0] = player;
        int monCount = monsters.size();
        // recalculate FOV, store it in fovmap for the render to use.
        FOV.reuseFOV(resistance, visible, player.x, player.y, fovRange, Radius.CIRCLE);
        blockage.refill(visible, 0f);
        seen.or(blockage.not());
        blockage.fringe8way();
        // handle monster turns
        for(int ci = 0; ci < monCount; ci++)
        {
            Coord pos = monsters.keyAt(ci);
            AnimatedGlidingSprite mon = monsters.getAt(ci);
            if(mon == null) continue;
            // monster values are used to store their aggression, 1 for actively stalking the player, 0 for not.
            if (visible[pos.x][pos.y] > 0.1) {
                // the player's position is set as a goal by findPath(), later.
                getToPlayer.clearGoals();
                // clear the buffer, we fill it next
                nextMovePositions.clear();
                // this gets the path from pos (the monster's starting position) to the player, and stores it in
                // nextMovePositions. it only stores one cell of movement, but it looks ahead up to 7 cells.
                // The keySet() from monsters is interesting here. it contains the current monster, but DijkstraMap
                // ignores the starting cell's blocking-or-not status, so that isn't an issue. the keyset is cached in
                // the CoordObjectOrderedMap, so it doesn't constantly allocate new sets (don't do this with a HashMap).
                // again to reduce allocations, the target position (and there could be more than one in many games) is
                // stored in a one-element array that gets modified, instead of using a new varargs every time (which
                // silently creates an array each time it is called).
                getToPlayer.findPath(nextMovePositions, 1, 7, monsters.keySet(), null, pos, playerArray);
                if (nextMovePositions.notEmpty()) {
                    Coord tmp = nextMovePositions.get(0);
                    // if we would move into the player, instead damage the player and animate a bump motion.
                    if (tmp.x == player.x && tmp.y == player.y) {
                        playerSprite.setPackedColor(rgbaIntToFloat(INT_BLOOD));
                        health--;
                        VectorSequenceGlider small = VectorSequenceGlider.BUMPS.getOrDefault(pos.toGoTo(player), null);
                        if(small != null) {
                            small = small.copy();
                            small.setCompleteRunner(() -> mon.setSmallMotion(null));
                        }
                        mon.setSmallMotion(small);
                        directorSmall.play();

                    }
                    // otherwise, make the monster start moving from its current position to its next one.
                    else {
                        mon.location.setStart(pos);
                        mon.location.setEnd(tmp);
                        // this changes the key from pos to tmp without affecting its value.
                        monsters.alter(pos, tmp);
                    }
                }
            }
        }
        monsterDirector.play();
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
        final float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        int rainbow = //hsl2rgb(time - (int)time, 0.9f, 0.6f, 1f);
                toRGBA8888(oklabByHCL(time - (int)time, 0.1f, 0.7f, 1f));
        for (int i = 0; i < bigWidth; i++) {
            for (int j = 0; j < bigHeight; j++) {
                if(visible[i][j] > 0.0) {
                    batch.setPackedColor(toCursor.contains(Coord.get(i, j))
                            ? rgbaIntToFloat(lerpColors(bgColors[i][j], rainbow, 0.9f))
                            : oklabIntToFloat(edit(fromRGBA8888(bgColors[i][j]), visible[i][j] * 0.7f + 0.25f, 0f, 0.018f, 0f, 0.4f, 1f, 1f, 1f)));
                    if(prunedDungeon[i][j] == '/' || prunedDungeon[i][j] == '+') // doors expect a floor drawn beneath them
                        batch.draw(charMapping.getOrDefault('.', solid), i, j, 1f, 1f);
                    batch.draw(charMapping.getOrDefault(prunedDungeon[i][j], solid), i, j, 1f, 1f);
                } else if(seen.contains(i, j)) {
                    batch.setPackedColor(rgbaIntToFloat(lerpColors(bgColors[i][j], INT_GRAY, 0.6f)));
                    if(prunedDungeon[i][j] == '/' || prunedDungeon[i][j] == '+') // doors expect a floor drawn beneath them
                        batch.draw(charMapping.getOrDefault('.', solid), i, j, 1f, 1f);
                    batch.draw(charMapping.getOrDefault(prunedDungeon[i][j], solid), i, j, 1f, 1f);
                }
            }
        }
        batch.setPackedColor(Color.WHITE_FLOAT_BITS);
        // I tried some other approaches here, but this is the fastest by quite a lot.

        AnimatedGlidingSprite monster;
        for (int i = 0; i < bigWidth; i++) {
            for (int j = 0; j < bigHeight; j++) {
                if (visible[i][j] > 0.0) {
                    if ((monster = monsters.get(Coord.get(i, j))) != null) {
                        monster.animate(time).draw(batch);
                    }
                }
            }
        }

        // both of the approaches below don't seem as fast as the approach above.

//        ObjectList<Coord> monPositions = monsters.order();
//        for (int i = 0, len = monPositions.size(); i < len; i++) {
//            Coord c = monPositions.get(i);
//            if(visible[c.x][c.y] > 0.0)
//                monsters.get(c).animate(time).draw(batch);
//        }

//        for(Map.Entry<Coord, AnimatedGlidingSprite> entry : monsters.entrySet()){
//            final Coord c = entry.getKey();
//            if(visible[c.x][c.y] > 0.0)
//                entry.getValue().animate(time).draw(batch);
//        }

        playerSprite.animate(time).draw(batch);

        // for some reason, this takes an unusually high amount of time.
        // when framerate is uncapped, commenting this out can provide more than a 25% boost to FPS.
//        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
    }
    /**
     * Supports WASD, vi-keys (hjklyubn), arrow keys, and numpad for movement, plus '.' or numpad 5 to stay still.
     */
    public void handleHeldKeys() {
        float c = playerSprite.location.getChange();
        if(c != 0f && c != 1f) return;
        if(input.isKeyPressed(A)  || input.isKeyPressed(H) || input.isKeyPressed(LEFT) || input.isKeyPressed(NUMPAD_4))
            move(Direction.LEFT);
        else if(input.isKeyPressed(S)  || input.isKeyPressed(J) || input.isKeyPressed(DOWN) || input.isKeyPressed(NUMPAD_2))
            move(Direction.DOWN);
        else if(input.isKeyPressed(W)  || input.isKeyPressed(K) || input.isKeyPressed(UP) || input.isKeyPressed(NUMPAD_8))
            move(Direction.UP);
        else if(input.isKeyPressed(D)  || input.isKeyPressed(L) || input.isKeyPressed(RIGHT) || input.isKeyPressed(NUMPAD_6))
            move(Direction.RIGHT);
        else if(input.isKeyPressed(Y) || input.isKeyPressed(NUMPAD_7))
            move(Direction.UP_LEFT);
        else if(input.isKeyPressed(U) || input.isKeyPressed(NUMPAD_9))
            move(Direction.UP_RIGHT);
        else if(input.isKeyPressed(B) || input.isKeyPressed(NUMPAD_1))
            move(Direction.DOWN_LEFT);
        else if(input.isKeyPressed(N) || input.isKeyPressed(NUMPAD_3))
            move(Direction.DOWN_RIGHT);
        else if(input.isKeyPressed(PERIOD) || input.isKeyPressed(NUMPAD_5) || input.isKeyPressed(NUMPAD_DOT))
            move(Direction.NONE);
    }

    private void move(Direction dir) {
        toCursor.clear();
        awaitedMoves.clear();
        awaitedMoves.add(playerSprite.getLocation().getStart().translate(dir));
    }


    @Override
    public void render () {
        // standard clear the background routine for libGDX
        ScreenUtils.clear(bgColor);

        // center the camera on the player's position
        camera.position.x = playerSprite.getX();
        camera.position.y =  playerSprite.getY();
        camera.update();
        mainViewport.apply(false);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // you done bad. you done real bad.
        if (health <= 0) {
            // still need to display the map, then write over it with a message.
            putMap();
            float wide = mainViewport.getWorldWidth(),
                    x = playerSprite.getX() - mainViewport.getWorldWidth() * 0.5f,
                    y = playerSprite.getY();
            font.draw(batch, "[RED]YOUR CRAWL IS OVER!", x, y + 2, wide, Align.center, true);
            font.draw(batch, "[GRAY]A monster sniffs your corpse and says,", x, y + 1, wide, Align.center, true);
            font.draw(batch, "[FOREST]" + lang, x, y, wide, Align.center, true);
            font.draw(batch, "[GRAY]q to quit.", x, y - 2, wide, Align.center, true);
            font.draw(batch, "[YELLOW]r to restart.", x, y - 4, wide, Align.center, true);
            batch.end();
            if(Gdx.input.isKeyJustPressed(Q))
                Gdx.app.exit();
            else if(input.isKeyJustPressed(R))
                restart(lang.hashCode());
            return;
        }
        playerDirector.step();
        handleHeldKeys();
        monsterDirector.step();
        directorSmall.step();

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        if(phase == Phase.MONSTER_ANIM) {
            if (!monsterDirector.isPlaying() && !directorSmall.isPlaying()) {
                phase = Phase.WAIT;
                if (!awaitedMoves.isEmpty()) {
                    Coord m = awaitedMoves.remove(0);
                    if (!toCursor.isEmpty())
                        toCursor.remove(0);
                    move(m);
                }
            }
        }
        else if(phase == Phase.WAIT && !awaitedMoves.isEmpty())
        {
            Coord m = awaitedMoves.remove(0);
            if (!toCursor.isEmpty())
                toCursor.remove(0);
            move(m);
        }
        else if(phase == Phase.PLAYER_ANIM) {
            if (!playerDirector.isPlaying() && !monsterDirector.isPlaying() && !directorSmall.isPlaying()) {
                phase = Phase.MONSTER_ANIM;
                postMove();
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
                    // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell, so the
                    // player's position, and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
                    // currently-moused-over cell, which we only need to set where the mouse is being handled.
                    playerToCursor.setGoal(player);
                    // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
                    // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
                    // Region that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(13, blockage);
                }
            }
        }
        pos.set(10, Gdx.graphics.getHeight() - cellHeight - cellHeight);
        mainViewport.unproject(pos);
        font.draw(batch, "[GRAY]Current Health: [RED]" + health + "[WHITE] at "
                + Gdx.graphics.getFramesPerSecond() + " FPS", pos.x, pos.y);
        batch.end();
    }
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mainViewport.update(width, height, false);
    }

    private void debugPrintVisible(){
        for (int y = prunedDungeon[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < prunedDungeon.length; x++) {
                System.out.print(prunedDungeon[x][y]);
            }
            System.out.print(' ');
            for (int x = 0; x < prunedDungeon.length; x++) {
                if(player.x == x && player.y == y)
                    System.out.print('@');
                else
                    System.out.print(visible[x][y] > 0f ? '+' : '_');
            }
            System.out.println();
        }

    }

}
