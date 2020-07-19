package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.FlowingCaveGenerator;
import squidpony.squidgrid.mapping.SectionDungeonGenerator;
import squidpony.squidgrid.mapping.SerpentMapGenerator;
import squidpony.squidgrid.mapping.styled.TilesetType;
import squidpony.squidmath.Coord;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.OrderedMap;
import squidpony.squidmath.RNG;

import java.util.ArrayList;

public class DungeonDemo extends ApplicationAdapter {
    private FilterBatch batch;

    private RNG rng;
    private SparseLayers display;
    private TextCellFactory tcf;
    private char[][] decoDungeon, bareDungeon, lineDungeon;
    private float[][] colorIndices, bgColorIndices;
    private double[][] res, fovmap;
    private int[][] lights;
    private FOV fov;
    /** In number of cells */
    public static final int gridWidth = 75;
    /** In number of cells */
    public static final int gridHeight = 25;
    /** The pixel width of a cell */
    public static final int cellWidth = 13;
    /** The pixel height of a cell */
    public static final int cellHeight = 25;
    private SquidInput input;
    private Color bgColor;
    private Stage stage;
    private DijkstraMap playerToCursor;
    private Coord cursor, player;
    private TextCellFactory.Glyph playerGlyph;
    private ArrayList<Coord> toCursor;
    private ArrayList<Coord> awaitedMoves;
    private float secondsWithoutMoves;
    private long startTime;
    private OrderedMap<Character, Double> costs;
    @Override
    public void create () {
        startTime = System.currentTimeMillis();
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        rng = new RNG("SquidLib!");

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new FilterBatch();
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(new StretchViewport(gridWidth * cellWidth, (gridHeight) * cellHeight), batch);
        tcf = DefaultResources.getCrispSlabFont();

        // display is a SquidLayers object, and that class has a very large number of similar methods for placing text
        // on a grid, with an optional background color and lightness modifier per cell. It also handles animations and
        // other effects, but you don't need to use them at all. SquidLayers also automatically handles the stretchable
        // distance field fonts, which are a big improvement over fixed-size bitmap fonts and should probably be
        // preferred for new games. SquidLayers needs to know what the size of the grid is in columns and rows, how big
        // an individual cell is in pixel width and height, and lastly how to handle text, which can be a BitmapFont or
        // a TextCellFactory. Either way, it will use what is given to make its TextCellFactory, and that handles the
        // layout of text in a cell, among other things. DefaultResources stores pre-configured BitmapFont objects but
        // also some TextCellFactory objects for distance field fonts; either one can be passed to this constructor.
        // the font will try to load Inconsolata-LGC-Custom as a bitmap font with a distance field effect.
        display = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, tcf);
        
        // this makes animations very fast, which is good for multi-cell movement but bad for attack animations.
        //display.setAnimationDuration(0.125f);
        //display.setLightingColor(SColor.PAPAYA_WHIP);
        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display.setPosition(0, 0);

        fov = new FOV(FOV.RIPPLE_TIGHT);
        playerToCursor = new DijkstraMap(DefaultResources.getGuiRandom());
        costs = new OrderedMap<>();
        costs.put('£', DijkstraMap.WALL);
        costs.put('¢', 4.0);
        costs.put('"', 2.0);
        rebuild();
        bgColor = SColor.DB_INK;


        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<Coord>(100);
        awaitedMoves = new ArrayList<Coord>(100);

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
                    case 'k':
                    case 'w':
                    case 'K':
                    case 'W':
                    {
                        //-1 is up on the screen
                        move(0, -1);
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 'j':
                    case 's':
                    case 'J':
                    case 'S':
                    {
                        //+1 is down on the screen
                        move(0, 1);
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'h':
                    case 'a':
                    case 'H':
                    case 'A':
                    {
                        move(-1, 0);
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'l':
                    case 'd':
                    case 'L':
                    case 'D':
                    {
                        move(1, 0);
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                    case 'R':
                    case 'r':
                    {
                        rebuild();
                        break;
                    }
                }
            }
        },
                //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
                //input and converts it to grid coordinates (here, a cell is 12 wide and 24 tall, so clicking at the
                // pixel position 15,51 will pass screenX as 1 (since if you divide 15 by 12 and round down you get 1),
                // and screenY as 2 (since 51 divided by 24 rounded down is 2)).
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

                    // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
                    // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
                    @Override
                    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                        if(awaitedMoves.isEmpty()) {
                            if (toCursor.isEmpty()) {
                                cursor = Coord.get(screenX, screenY);
                                //This uses DijkstraMap.findPath to get a possibly long path from the current player position
                                //to the position the user clicked on.
                                toCursor = playerToCursor.findPath(250, null, null, player, cursor);
                            }
                            awaitedMoves.clear();
                            awaitedMoves.addAll(toCursor);
                        }
                        return false;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        return mouseMoved(screenX, screenY);
                    }

                    // causes the path to the mouse position to become highlighted (toCursor contains a list of points that
                    // receive highlighting). Uses DijkstraMap.findPath() to find the path, which is surprisingly fast.
                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        if(!awaitedMoves.isEmpty())
                            return false;
                        if(cursor.x == screenX && cursor.y == screenY)
                        {
                            return false;
                        }
                        cursor = Coord.get(screenX, screenY);
                        toCursor = playerToCursor.findPath(250, null, null, player, cursor);
                        return false;
                    }
                }));

        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        // and then add display, our one visual component, to the list of things that act in Stage.
        stage.addActor(display);
    }

    private void rebuild()
    {
        SerpentMapGenerator serpent = new SerpentMapGenerator(gridWidth, gridHeight, rng, rng.nextDouble(0.15));

        serpent.putWalledBoxRoomCarvers(rng.between(5, 10));
        serpent.putWalledRoundRoomCarvers(rng.between(2, 5));
        serpent.putRoundRoomCarvers(rng.between(1, 4));
        serpent.putCaveCarvers(rng.between(8, 15));

        FlowingCaveGenerator flowCaves = new FlowingCaveGenerator(gridWidth, gridHeight, TilesetType.DEFAULT_DUNGEON, rng);
        
        SectionDungeonGenerator dungeonGen = new SectionDungeonGenerator(gridWidth, gridHeight, rng);
        dungeonGen.addWater(SectionDungeonGenerator.CAVE, rng.between(10, 30));
        dungeonGen.addWater(SectionDungeonGenerator.ROOM, rng.between(3, 11));
        dungeonGen.addDoors(rng.between(10, 25), false);
        dungeonGen.addGrass(SectionDungeonGenerator.CAVE, rng.between(5, 25));
        dungeonGen.addGrass(SectionDungeonGenerator.ROOM, rng.between(0, 5));
        dungeonGen.addBoulders(SectionDungeonGenerator.ALL, rng.between(3, 11));
        if(rng.nextInt(3) == 0)
            dungeonGen.addLake(rng.between(5, 30), '£', '¢');
        else if(rng.nextInt(5) < 3)
            dungeonGen.addLake(rng.between(8, 35));
        else
            dungeonGen.addLake(0);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)

        switch (rng.nextInt(18))
        {
            case 0:
            case 1:
            case 2:
            case 11:
            case 12:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(serpent.generate(), serpent.getEnvironment()));
                break;
            case 3:
            case 4:
            case 5:
            case 13:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.DEFAULT_DUNGEON));
                break;
            case 6:
            case 7:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS));
                break;
            case 8:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.REFERENCE_CAVES));
                break;
            case 9:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.ROOMS_LIMIT_CONNECTIVITY));
                break;
            case 10:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.CORNER_CAVES));
                break;
            case 14:
            case 15:
            case 16:
            default: 
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(flowCaves.generate(), flowCaves.getEnvironment()));
                break;
        }

        //There are lots of options for dungeon generation in SquidLib; you can pass a TilesetType enum to generate()
        //as shown on the following lines to change the style of dungeon generated from ruined areas, which are made
        //when no argument is passed to generate or when TilesetType.DEFAULT_DUNGEON is, to caves or other styles.
        //decoDungeon = dungeonGen.generate(TilesetType.REFERENCE_CAVES); // generate caves
        //decoDungeon = dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS); // generate large round rooms

        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBareDungeon();
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing character.
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon, true);
        // it's more efficient to get random floors from a packed set containing only (compressed) floor positions.
        // CoordPacker is a deep and involved class, but when other classes request packed data, you usually just need
        // to give them a short array representing a region, as produced by CoordPacker.pack().
        GreasedRegion placement = new GreasedRegion(bareDungeon, '.');
        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be huge, Coord is optimized for x and y values between -3 and 255, inclusive.
        cursor = Coord.get(-1, -1);
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player.
        player = placement.retract8way().singleRandom(rng);
        if(!player.isWithin(gridWidth, gridHeight))
            rebuild();
        if(playerGlyph != null)
            display.removeGlyph(playerGlyph);
        playerGlyph = display.glyph('@', SColor.RED_INCENSE, player.x, player.y);
        res = DungeonUtility.generateResistances(decoDungeon);
        fovmap = new double[gridWidth][gridHeight];
        FOV.reuseFOV(res, fovmap, player.x, player.y, 9.0, Radius.CIRCLE);


        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        playerToCursor.initialize(decoDungeon);
        playerToCursor.initializeCost(DungeonUtility.generateCostMap(decoDungeon, costs, 1.0));
        // DungeonUtility provides various ways to get default colors or other information from a dungeon char 2D array.
        colorIndices = MapUtility.generateDefaultColorsFloat(decoDungeon, '£', SColor.CW_PALE_GOLD.toFloatBits(), '¢', SColor.CW_BRIGHT_APRICOT.toFloatBits());
        bgColorIndices = MapUtility.generateDefaultBGColorsFloat(decoDungeon, '£', SColor.CW_ORANGE.toFloatBits(), '¢', SColor.CW_RICH_APRICOT.toFloatBits());
        
        // this does the standard lighting for walls, floors, etc. but also uses the time to do the Simplex noise thing.
        lights = MapUtility.generateLightnessModifiers(decoDungeon, (System.currentTimeMillis() - startTime) * 0.023, '£', '¢');

    }
    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {
        int newX = player.x + xmod, newY = player.y + ymod;
        if (newX >= 0 && newY >= 0 && newX < gridWidth && newY < gridHeight
                && bareDungeon[newX][newY] != '#') {
            // '+' is a door.
            if (lineDungeon[newX][newY] == '+') {
                decoDungeon[newX][newY] = '/';
                lineDungeon[newX][newY] = '/';
                // changes to the map mean the resistances for FOV need to be regenerated.
                res = DungeonUtility.generateResistances(decoDungeon);
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov.calculateFOV(res, player.x, player.y, 9, Radius.CIRCLE);

            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov.calculateFOV(res, newX, newY, 9, Radius.CIRCLE);
                display.slide(playerGlyph, player.x, player.y, newX, newY, 0.125f, null);
            }
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        float alter = 0;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                if(toCursor.contains(Coord.get(i, j)))
                    alter = 10f;
                else {
                    if (fovmap[i][j] > 0)
                        alter = (float) fovmap[i][j] * 0.75f + lights[i][j] * 0.0325f;
                    else
                        alter = -5f;
                }
                display.putWithConsistentLight(i, j, lineDungeon[i][j],
                        colorIndices[i][j],
                        bgColorIndices[i][j],
                        // -0x1.abdffep126F is SColor.PAPAYA_WHIP.toFloatBits()
                        -0x1.abdffep126F,
                        alter);
            }
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        MapUtility.fillLightnessModifiers(lights, decoDungeon, (System.currentTimeMillis() - startTime) * 0.023, '£', '¢');

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        if(!awaitedMoves.isEmpty())
        {
            // extremely similar to the block below that also checks if animations are done
            // this doesn't check for input, but instead processes and removes Points from awaitedMoves.
            if(!display.hasActiveAnimations()) {
                secondsWithoutMoves += Gdx.graphics.getDeltaTime();
                if (secondsWithoutMoves >= 0.01) {
                    secondsWithoutMoves = 0;
                    Coord m = awaitedMoves.remove(0);
                    toCursor.remove(0);
                    move(m.x - player.x, m.y - player.y);
                    player = m;

                }
            }
        }
        // if we are waiting for the player's input and get input, process it.
        // this step is not needed with libGDX InputProcessors by default; SquidInput uses this
        // to avoid processing key-hold events more often than desired.
        else if(input.hasNext() && !display.hasActiveAnimations()) {
            input.next();
        }
        // if the previous blocks didn't happen, and there are no active animations, then 
        // we reset the timer for how long it has been since a move was made.
        else
        {
            secondsWithoutMoves = 0;
        }

        // stage has its own batch and must be explicitly told to draw().
        stage.getViewport().apply(true);
        stage.draw();
        stage.act();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
        input.getMouse().reinitialize((float) width / this.gridWidth, (float)height / (this.gridHeight),
                this.gridWidth, this.gridHeight, 0, 0);
        stage.getViewport().update(width, height, true);

    }
}
