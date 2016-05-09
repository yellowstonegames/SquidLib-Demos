package squidpony.demo.dungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.SectionDungeonGenerator;
import squidpony.squidgrid.mapping.SerpentMapGenerator;
import squidpony.squidgrid.mapping.styled.TilesetType;
import squidpony.squidmath.Coord;
import squidpony.squidmath.CoordPacker;
import squidpony.squidmath.PerlinNoise;
import squidpony.squidmath.RNG;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static squidpony.squidmath.CoordPacker.retract;
import static squidpony.squidmath.CoordPacker.singleRandom;

/**
 * The main class of the game, constructed once in each of the platform-specific Launcher classes. Doesn't use any
 * platform-specific code.
 */
// In SquidSetup, squidlib-util is always a dependency, and squidlib (the display code that automatically includes
// libGDX) is checked by default. If you didn't change those dependencies, this class should run out of the box.
//
// If you didn't select squidib as a dependency in SquidSetup, this class will be full of errors. If you don't depend
// on LibGDX, you'll need to figure out display on your own, and the setup of multiple platform projects is probably
// useless to you. But, if you do depend on LibGDX, you can make some use of this class. You can remove any imports or
// usages of classes in the squidpony.squidgrid.gui.gdx package, remove as much of create() as you  want (some of it
// doesn't use the display classes, so you might want the dungeon generation and such, otherwise just empty out the
// whole method), remove any SquidLib-specific code in render() and resize(), and probably remove putMap entirely.

// A main game class that uses LibGDX to display, which is the default for SquidLib, needs to extend ApplicationAdapter
// or something related, like Game. Game adds features that SquidLib doesn't currently use, so ApplicationAdapter is
// perfectly fine for these uses.
public class DungeonGame extends ApplicationAdapter {
    SpriteBatch batch;

    private RNG rng;
    private SquidLayers display;
    private TextCellFactory tcf;
    private SerpentMapGenerator serpent;
    private SectionDungeonGenerator dungeonGen;
    private char[][] decoDungeon, bareDungeon, lineDungeon;
    private int[][] colorIndices, bgColorIndices, lights;
    private double[][] res, fovmap;
    private FOV fov;
    /** In number of cells */
    private int gridWidth;
    /** In number of cells */
    private int gridHeight;
    /** The pixel width of a cell */
    private int cellWidth;
    /** The pixel height of a cell */
    private int cellHeight;
    private VisualInput input;
    private Color bgColor;
    private Stage stage;
    private DijkstraMap playerToCursor;
    private Coord cursor, player;
    private AnimatedEntity playerAE;
    private ArrayList<Coord> toCursor;
    private ArrayList<Coord> awaitedMoves;
    private float secondsWithoutMoves;
    private String[] lang = new String[12];
    private int langIndex = 0;
    private int INTERNAL_ZOOM = 1;
    private double counter = 0;
    LinkedHashMap<Character, Double> costs;
    @Override
    public void create () {
        //These variables, corresponding to the screen's width and height in cells and a cell's width and height in
        //pixels, must match the size you specified in the launcher for input to behave.
        //This is one of the more common places a mistake can happen.
        //In our desktop launcher, we gave these arguments to the configuration:
        //	config.width = 50 * 22;
        //  config.height = 32 * 22;
        //Here, config.height refers to the total number of rows to be displayed on the screen.
        //We're displaying 24 rows of dungeon, then 8 more rows of text generation to show some tricks with language.
        //That adds up to 32 total rows of height.
        //gridHeight is 24 because that variable will be used for generating the dungeon and handling movement within
        //the upper 24 rows. Anything that refers to the full height, which happens rarely and usually for things like
        //screen resizes, just uses gridHeight + 8. Next to it is gridWidth, which is 50 because we want 50 grid spaces
        //across the whole screen. cellWidth and cellHeight are both 22, and match the multipliers for config.width and
        //config.height, but in this case don't strictly need to because we soon use a "Stretchable" font. While
        //gridWidth and gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the pixel dimensions
        //of an individual cell. The font will look more crisp if the cell dimensions match the config multipliers
        //exactly, and the stretchable fonts (technically, distance field fonts) can resize to non-square sizes and
        //still retain most of that crispness.
        gridWidth = 50;
        gridHeight = 20;

        cellWidth = 26 * INTERNAL_ZOOM;
        cellHeight = 40 * INTERNAL_ZOOM;
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        rng = new RNG("SquidLib!");

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(new StretchViewport(gridWidth * cellWidth, (gridHeight) * cellHeight), batch);
        tcf = DefaultResources.getStretchableFont().setSmoothingMultiplier(2f / (INTERNAL_ZOOM + 1f));

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
        display = new SquidLayers(gridWidth, gridHeight, cellWidth, cellHeight, tcf);
        // a bit of a hack to increase the text height slightly without changing the size of the cells they're in.
        // this causes a tiny bit of overlap between cells, which gets rid of an annoying gap between vertical lines.
        // if you use '#' for walls instead of box drawing chars, you don't need this.
        display.setTextSize(cellWidth + INTERNAL_ZOOM, cellHeight + 2 * INTERNAL_ZOOM);

        // this makes animations very fast, which is good for multi-cell movement but bad for attack animations.
        display.setAnimationDuration(0.125f);
        display.setLightingColor(SColor.PAPAYA_WHIP);
        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display.setPosition(0, 0);

        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good "ruined" dungeons.
        dungeonGen = new SectionDungeonGenerator(gridWidth, gridHeight, rng);

        fov = new FOV(FOV.RIPPLE_TIGHT);
        playerToCursor = new DijkstraMap(DefaultResources.getGuiRandom());
        costs = new LinkedHashMap<>();
        costs.put('£', DijkstraMap.WALL);
        costs.put('¢', 4.0);
        costs.put('"', 2.0);
        rebuild();
        bgColor = SColor.DARK_SLATE_GRAY;


        // these were generated by the FakeLanguageGen class, which is compatible with most platforms SquidLib runs on,
        // but not HTML. So they are simply pre-generated chunks of text to show the glyph support in SquidLib.
        lang = new String[]
                {
                        "Ned jation, quariok sied pebation gnadism erbiss!",
                        "Tezen kisaiba konnouda, bubotan, ne rijonnozouna?",
                        "Mà le roe leth glang içoui?",
                        "Potron oxa kthoi opleipotron ola aisaisp kthou.",
                        "Εοθιαμ οκραυπ ρεοφα τερος ψοσποιζ ριαμ.",
                        "Tuskierovich topliegrachigary khodynamyv, toskiafi!",
                        "Гыпогозуск, глынуск сид фавуриджйглътод!",
                        "Hmaagrai eindian, ase agluxi-ugg?",
                        "Gœu, auna sazeun nonanen kunneûnou ro.",
                        "Esibőnt sěrmü ęãtsed sàpoupot lóâ delyīŉāy goỳ, sneśiec bism ālsi?",
                        "Зaчaire vаτяπλaс щεογκιшι cэнαι гεвов; rαυп, ειрйч бιοκριαρτουggrй nι!",
                        "Gatyriam reta - venőîn dīnøî şonā kazhy ásǻī, tsibiśťinki.",
                };

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
        input = new VisualInput(new SquidInput.KeyHandler() {
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
                    awaitedMoves = new ArrayList<>(toCursor);
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
        input.eightWay = false;
        input.init("Rebuild", "Quit");

        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        // and then add display, our one visual component, to the list of things that act in Stage.
        stage.addActor(display);
        input.resizeInnerStage(stage);
    }

    private void rebuild()
    {
        serpent = new SerpentMapGenerator(gridWidth, gridHeight, rng, rng.nextDouble(0.15));

        serpent.putWalledBoxRoomCarvers(rng.between(5, 10));
        serpent.putWalledRoundRoomCarvers(rng.between(2, 5));
        serpent.putRoundRoomCarvers(rng.between(1, 4));
        serpent.putCaveCarvers(rng.between(8, 15));
        dungeonGen.addWater(SectionDungeonGenerator.CAVE, rng.between(10, 30));
        dungeonGen.addWater(SectionDungeonGenerator.ROOM, rng.between(3, 11));
        dungeonGen.addDoors(rng.between(10, 25), false);
        dungeonGen.addGrass(SectionDungeonGenerator.CAVE, rng.between(5, 25));
        dungeonGen.addGrass(SectionDungeonGenerator.ROOM, rng.between(0, 5));
        dungeonGen.addBoulders(SectionDungeonGenerator.ALL, rng.between(3, 11));
        if(rng.nextInt(3) == 0)
            dungeonGen.addLake(rng.between(5, 30), '£', '¢');
        else if(rng.nextInt(4) <= 1)
            dungeonGen.addLake(rng.between(8, 35));
        else
            dungeonGen.addLake(0);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)

        switch (rng.nextInt(12))
        {
            case 0:
            case 1:
            case 2:
            case 11:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(serpent.generate(), serpent.getEnvironment()));
                break;
            case 3:
            case 4:
            case 5:
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
            default:
                decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.CORNER_CAVES));
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
        short[] placement = CoordPacker.pack(decoDungeon, '.');
        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be huge, Coord is optimized for x and y values between -3 and 255, inclusive.
        cursor = Coord.get(-1, -1);
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player.
        player = singleRandom(retract(placement, 1, gridWidth, gridHeight, true), rng);
        if(!player.isWithin(gridWidth, gridHeight))
            rebuild();
        //display.removeAnimatedEntity(playerAE);
        if(playerAE != null)
            display.removeActor(playerAE.actor);
        playerAE = display.animateActor(player.x, player.y, '@', 10);
        display.addActor(playerAE.actor);
        res = DungeonUtility.generateResistances(decoDungeon);
        fovmap = fov.calculateFOV(res, player.x, player.y, 8, Radius.CIRCLE);


        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<Coord>(100);
        awaitedMoves = new ArrayList<Coord>(100);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        playerToCursor.initialize(decoDungeon);
        playerToCursor.initializeCost(DungeonUtility.generateCostMap(decoDungeon, costs, 1.0));
        // DungeonUtility provides various ways to get default colors or other information from a dungeon char 2D array.
        colorIndices = DungeonUtility.generatePaletteIndices(decoDungeon, '£', 18, '¢', 15);
        bgColorIndices = DungeonUtility.generateBGPaletteIndices(decoDungeon, '£', 6, '¢', 39);

        // this does the standard lighting for walls, floors, etc. but also uses counter to do the Simplex noise thing.
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, counter);

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
                fovmap = fov.calculateFOV(res, player.x, player.y, 8, Radius.CIRCLE);

            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov.calculateFOV(res, newX, newY, 8, Radius.CIRCLE);
                display.slide(playerAE, newX, newY);
            }
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        double alter = 0;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                if(fovmap[i][j] > 0)
                    alter = 220 * fovmap[i][j] + 25 * PerlinNoise.noise(i / 6.0, j / 6.0, counter / 15.0);
                else
                    alter = -50;
                display.put(i, j, lineDungeon[i][j], colorIndices[i][j], bgColorIndices[i][j], lights[i][j] +
                        (int)(-100 + alter));
            }
        }
        for (Coord pt : toCursor)
        {
            // use a brighter light to trace the path to the cursor, from 170 max lightness to 0 min.
            display.highlight(pt.x, pt.y, 100);
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        counter += Gdx.graphics.getDeltaTime() * 15;

        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, counter, '£', '¢');

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
        else if(input.hasNext() && !display.hasActiveAnimations()) {
            input.next();
        }
        // if the previous blocks didn't happen, and there are no active animations, then either change the phase
        // (because with no animations running the last phase must have ended), or start a new animation soon.
        else// if(!display.hasActiveAnimations()) {
            //secondsWithoutMoves += Gdx.graphics.getDeltaTime();
            //if (secondsWithoutMoves >= 0.02) {
                //secondsWithoutMoves = 0;
            //}
        //}
        // if we do have an animation running, then how many frames have passed with no animation needs resetting
        //else
        {
            secondsWithoutMoves = 0;
        }

        input.show();

        // stage has its own batch and must be explicitly told to draw().
        stage.getViewport().apply(true);
        stage.draw();
        stage.act();
    }

    @Override
	public void resize(int width, int height) {
		super.resize(width, height);
        //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
		input.reinitialize((float) width / this.gridWidth, (float)height / (this.gridHeight),
                this.gridWidth, this.gridHeight, 0, 0, width, height);
        input.update(width, height, true);
        stage.getViewport().update(width, height, true);

    }
}
