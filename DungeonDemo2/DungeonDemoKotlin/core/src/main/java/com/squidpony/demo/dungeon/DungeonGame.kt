package com.squidpony.demo.dungeon

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.StretchViewport
import squidpony.ArrayTools
import squidpony.Maker
import squidpony.squidai.DijkstraMap
import squidpony.squidgrid.FOV
import squidpony.squidgrid.Radius
import squidpony.squidgrid.gui.gdx.*
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.FlowingCaveGenerator
import squidpony.squidgrid.mapping.SectionDungeonGenerator
import squidpony.squidgrid.mapping.SerpentMapGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.*
import java.util.*

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
class DungeonGame : ApplicationAdapter() {
    internal var batch: SpriteBatch? = null

    private var rng: RNG? = null
    private var display: SquidLayers? = null
    private var tcf: TextCellFactory? = null
    private var serpent: SerpentMapGenerator? = null
    private var dungeonGen: SectionDungeonGenerator? = null
    private var decoDungeon: Array<CharArray>? = null
    private var bareDungeon: Array<CharArray>? = null
    private var lineDungeon: Array<CharArray>? = null
    private var colors: Array<Array<Color>>? = null
    private var bgColors: Array<Array<Color>>? = null
    private var res: Array<DoubleArray>? = null
    private var fovmap: Array<DoubleArray>? = null
    private var fov: FOV? = null
    /** In number of cells  */
    private var gridWidth: Int = 0
    /** In number of cells  */
    private var gridHeight: Int = 0
    /** The pixel width of a cell  */
    private var cellWidth: Int = 0
    /** The pixel height of a cell  */
    private var cellHeight: Int = 0
    private var input: VisualInput? = null
    private var bgColor: Color? = null
    private var stage: Stage? = null
    private var playerToCursor: DijkstraMap? = null
    private var cursor: Coord? = null
    private var player: Coord? = null
    private var playerAE: AnimatedEntity? = null
    private var toCursor: MutableList<Coord>? = null
    private var awaitedMoves: MutableList<Coord>? = null
    private var placement: GreasedRegion? = null
    private var costs: OrderedMap<Char, Double>? = null

    override fun create() {
        gridWidth = 75
        gridHeight = 30

        cellWidth = 15
        cellHeight = 27
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        rng = RNG("SquidLib!")
        // choice is -499832297 on Desktop
        // choice is -392612924 on GWT
        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = SpriteBatch()
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = Stage(StretchViewport((gridWidth * cellWidth).toFloat(), (gridHeight * cellHeight).toFloat()), batch)
        tcf = DefaultResources.getStretchableSlabFont()

        // display is a SquidLayers object, and that class has a very large number of similar methods for placing text
        // on a grid, with an optional background color and lightness modifier per cell. It also handles animations and
        // other effects, but you don't need to use them at all. SquidLayers also automatically handles the stretchable
        // distance field fonts, which are a big improvement over fixed-size bitmap fonts and should probably be
        // preferred for new games. SquidLayers needs to know what the size of the grid is in columns and rows, how big
        // an individual cell is in pixel width and height, and lastly how to handle text, which can be a BitmapFont or
        // a TextCellFactory. Either way, it will use what is given to make its TextCellFactory, and that handles the
        // layout of text in a cell, among other things. DefaultResources stores pre-configured BitmapFont objects but
        // also some TextCellFactory objects for distance field fonts; either one can be passed to this constructor.
        // the font will try to load Iosevka Slab as a bitmap font with a distance field effect.
        display = SquidLayers(gridWidth, gridHeight, cellWidth, cellHeight, tcf!!)
        // a bit of a hack to increase the text height slightly without changing the size of the cells they're in.
        // this causes a tiny bit of overlap between cells, which gets rid of an annoying gap between vertical lines.
        // if you use '#' for walls instead of box drawing chars, you don't need this.
        display!!.setTextSize(cellWidth * 1.1f, cellHeight * 1.1f)

        // this makes animations very fast, which is good for multi-cell movement but bad for attack animations.
        display!!.animationDuration = 0.09f
        display!!.lightingColor = SColor.PAPAYA_WHIP
        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display!!.setPosition(0f, 0f)

        placement = GreasedRegion(gridWidth, gridHeight)

        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good "ruined" dungeons.
        dungeonGen = SectionDungeonGenerator(gridWidth, gridHeight, rng!!)

        fov = FOV(FOV.SHADOW)

        playerToCursor = DijkstraMap(RNG(CrossHash.Wisp.hash64("Random Path?")))
        costs = Maker.makeOM('£', DijkstraMap.WALL, '¢', 4.0, '"', 2.0)
        rebuild()
        bgColor = SColor.DARK_SLATE_GRAY

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
        input = VisualInput(SquidInput.KeyHandler { key, alt, ctrl, shift ->
            when (key) {
                SquidInput.UP_ARROW, 'k', 'w', 'K', 'W' -> {
                    //-1 is up on the screen
                    move(0, -1)
                }
                SquidInput.DOWN_ARROW, 'j', 's', 'J', 'S' -> {
                    //+1 is down on the screen
                    move(0, 1)
                }
                SquidInput.LEFT_ARROW, 'h', 'a', 'H', 'A' -> {
                    move(-1, 0)
                }
                SquidInput.RIGHT_ARROW, 'l', 'd', 'L', 'D' -> {
                    move(1, 0)
                }
                'Q', 'q', SquidInput.ESCAPE -> {
                    Gdx.app.exit()
                }
                'R', 'r' -> {
                    rebuild()
                }
            }
        },
                SquidMouse(cellWidth.toFloat(), cellHeight.toFloat(), gridWidth.toFloat(), gridHeight.toFloat(), cellWidth shr 1, 0, object : InputAdapter() {

                    // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
                    // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
                    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                        if (awaitedMoves!!.isEmpty()) {
                            if (toCursor!!.isEmpty()) {
                                cursor = Coord.get(screenX, screenY)
                                //This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                                // player position to the position the user clicked on. The "PreScanned" part is an optimization
                                // that's special to DijkstraMap; because the whole map has already been fully analyzed by the
                                // DijkstraMap.scan() method at the start of the program, and re-calculated whenever the player
                                // moves, we only need to do a fraction of the work to find the best path with that info.
                                toCursor = playerToCursor!!.findPathPreScanned(cursor)
                                //findPathPreScanned includes the current cell (goal) by default, which is helpful when
                                // you're finding a path to a monster or loot, and want to bump into it, but here can be
                                // confusing because you would "move into yourself" as your first move without this.
                                // Getting a sublist avoids potential performance issues with removing from the start of an
                                // ArrayList, since it keeps the original list around and only gets a "view" of it.
                                if (!toCursor!!.isEmpty()) {
                                    toCursor = toCursor!!.subList(1, toCursor!!.size)
                                }
                            }
                            awaitedMoves!!.addAll(toCursor!!)
                        }
                        return false
                    }

                    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                        return mouseMoved(screenX, screenY)
                    }

                    // causes the path to the mouse position to become highlighted (toCursor contains a list of points that
                    // receive highlighting). Uses DijkstraMap.findPath() to find the path, which is surprisingly fast.
                    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
                        if (!awaitedMoves!!.isEmpty())
                            return false
                        if (cursor!!.x == screenX && cursor!!.y == screenY) {
                            return false
                        }
                        cursor = Coord.get(screenX, screenY)
                        //This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                        // player position to the position the user clicked on. The "PreScanned" part is an optimization
                        // that's special to DijkstraMap; because the whole map has already been fully analyzed by the
                        // DijkstraMap.scan() method at the start of the program, and re-calculated whenever the player
                        // moves, we only need to do a fraction of the work to find the best path with that info.

                        toCursor = playerToCursor!!.findPathPreScanned(cursor)
                        //findPathPreScanned includes the current cell (goal) by default, which is helpful when
                        // you're finding a path to a monster or loot, and want to bump into it, but here can be
                        // confusing because you would "move into yourself" as your first move without this.
                        // Getting a sublist avoids potential performance issues with removing from the start of an
                        // ArrayList, since it keeps the original list around and only gets a "view" of it.
                        if (!toCursor!!.isEmpty()) {
                            toCursor = toCursor!!.subList(1, toCursor!!.size)
                        }
                        return false
                    }
                }))
        input!!.forceButtons = false
        input!!.eightWay = false
        input!!.init(tcf, "Rebuild", "Quit")

        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.inputProcessor = InputMultiplexer(stage, input)
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        // and then add display, our one visual component, to the list of things that act in Stage.
        stage!!.addActor(display)
        input!!.resizeInnerStage(stage)
    }

    private fun rebuild() {
        serpent = SerpentMapGenerator(gridWidth, gridHeight, rng, rng!!.nextInt(150) * 0.01)

        serpent!!.putWalledBoxRoomCarvers(rng!!.between(5, 10))
        serpent!!.putWalledRoundRoomCarvers(rng!!.between(3, 6))
        serpent!!.putBoxRoomCarvers(rng!!.between(2, 5))
        serpent!!.putRoundRoomCarvers(rng!!.between(2, 5))
        serpent!!.putCaveCarvers(rng!!.between(3, 9))
        dungeonGen!!.clearEffects()
        dungeonGen!!.addWater(SectionDungeonGenerator.CAVE, rng!!.between(10, 30))
        dungeonGen!!.addWater(SectionDungeonGenerator.ROOM, rng!!.between(3, 11))
        dungeonGen!!.addDoors(rng!!.between(10, 25), false)
        dungeonGen!!.addGrass(SectionDungeonGenerator.CAVE, rng!!.between(5, 25))
        dungeonGen!!.addGrass(SectionDungeonGenerator.ROOM, rng!!.between(0, 5))
        dungeonGen!!.addBoulders(SectionDungeonGenerator.ALL, rng!!.between(0, 9))
        if (rng!!.nextInt(3) == 0)
            dungeonGen!!.addLake(rng!!.between(5, 30), '£', '¢')
        else if (rng!!.nextBoolean())
            dungeonGen!!.addLake(rng!!.between(8, 35))
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)

        when (rng!!.next(4)) {
            0, 1, 2, 3 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(serpent!!.generate(), serpent!!.environment))
            4, 5, 6 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(TilesetType.DEFAULT_DUNGEON))
            7, 8 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS))
            9 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(TilesetType.REFERENCE_CAVES))
            10 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(TilesetType.ROOMS_AND_CORRIDORS_A))
            11 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(TilesetType.MAZE_A))
            else -> {
                val flow = FlowingCaveGenerator(gridWidth, gridHeight)
                decoDungeon = DungeonUtility.closeDoors(dungeonGen!!.generate(flow.generate(), flow.getEnvironment()))
            }
        }// 4 bits, so 0 to 15 inclusive

        //There are lots of options for dungeon generation in SquidLib; you can pass a TilesetType enum to generate()
        //as shown on the following lines to change the style of dungeon generated from ruined areas, which are made
        //when no argument is passed to generate or when TilesetType.DEFAULT_DUNGEON is, to caves or other styles.
        //decoDungeon = dungeonGen.generate(TilesetType.REFERENCE_CAVES); // generate caves
        //decoDungeon = dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS); // generate large round rooms

        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen!!.bareDungeon
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing character.
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon!!, true)
        // it's more efficient to get random floors from a packed set containing only (compressed) floor positions.
        // CoordPacker is a deep and involved class, but when other classes request packed data, you usually just need
        // to give them a short array representing a region, as produced by CoordPacker.pack().
        placement!!.refill(decoDungeon, '.')
        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be huge, Coord is optimized for x and y values between -3 and 255, inclusive.
        cursor = Coord.get(-1, -1)
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player.
        player = placement!!.retract8way().singleRandom(rng!!)
        if (!player!!.isWithin(gridWidth, gridHeight))
            rebuild()
        //display.removeAnimatedEntity(playerAE);
        if (playerAE != null)
            display!!.removeActor(playerAE!!.actor)
        playerAE = display!!.animateActor(player!!.x, player!!.y, '@', SColor.CW_APRICOT)
        display!!.addActor(playerAE!!.actor)
        res = DungeonUtility.generateResistances(decoDungeon!!)
        fovmap = fov!!.calculateFOV(res!!, player!!.x, player!!.y, 8.0, Radius.CIRCLE)


        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = ArrayList(100)
        awaitedMoves = ArrayList(100)
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        playerToCursor!!.initialize(decoDungeon!!)
        playerToCursor!!.initializeCost(DungeonUtility.generateCostMap(decoDungeon!!, costs, 1.0))
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor!!.clearGoals()
        playerToCursor!!.resetMap()
        playerToCursor!!.setGoal(player)
        playerToCursor!!.scan(null)

        // DungeonUtility provides various ways to get default colors or other information from a dungeon char 2D array.
        colors = MapUtility.generateDefaultColors(decoDungeon!!, '£', SColor.CW_LIGHT_YELLOW, '¢', SColor.CW_BRIGHT_ORANGE)
        bgColors = MapUtility.generateDefaultBGColors(decoDungeon!!, '£', SColor.CW_ORANGE, '¢', SColor.CW_DARK_ORANGE)
        // the line after this automatically sets the brightness of backgrounds in display to match their contents, so
        // here we simply fill the contents of display with our dungeon (but we don't set the actual colors yet).
        ArrayTools.insert(lineDungeon, display!!.foregroundLayer.contents, 0, 0)
        display!!.autoLight((System.currentTimeMillis() and 0xFFFFFFL) * 0.013, '£', '¢')
    }

    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private fun move(xmod: Int, ymod: Int) {
        val newX = player!!.x + xmod
        val newY = player!!.y + ymod
        if (newX >= 0 && newY >= 0 && newX < gridWidth && newY < gridHeight
                && bareDungeon!![newX][newY] != '#') {
            // '+' is a door.
            if (lineDungeon!![newX][newY] == '+') {
                decoDungeon!![newX][newY] = '/'
                lineDungeon!![newX][newY] = '/'
                // changes to the map mean the resistances for FOV need to be regenerated.
                res = DungeonUtility.generateResistances(decoDungeon!!)
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov!!.calculateFOV(res!!, player!!.x, player!!.y, 8.0, Radius.CIRCLE)

            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov!!.calculateFOV(res!!, newX, newY, 8.0, Radius.CIRCLE)
                display!!.slide(playerAE, newX, newY)
                player = Coord.get(newX, newY)
            }
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    fun putMap() {
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                if (fovmap!![i][j] > 0.0)
                    display!!.put(i, j, lineDungeon!![i][j], colors!![i][j], bgColors!![i][j],
                            (-105 + 180 * (fovmap!![i][j] * (1.0 + 0.2 * SeededNoise.noise(i * 0.2, j * 0.2, (System.currentTimeMillis() and 0xffffff) * 0.001, 10000)))).toInt())
                else
                    display!!.put(i, j, lineDungeon!![i][j], colors!![i][j], bgColors!![i][j],-150)
            }
        }
        for (pt in toCursor!!) {
            // use a brighter light to trace the path to the cursor.
            display!!.highlight(pt.x, pt.y, 130)
        }
    }

    override fun render() {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor!!.r / 255.0f, bgColor!!.g / 255.0f, bgColor!!.b / 255.0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // this does the standard lighting for walls, floors, etc. but also uses the time to do the Simplex noise thing.
        display!!.autoLight((System.currentTimeMillis() and 0xFFFFFFL) * 0.013, '£', '¢')

        putMap()
        // if the user clicked, we have a list of moves to perform.
        if (!awaitedMoves!!.isEmpty()) {
            // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
            if (!display!!.hasActiveAnimations()) {
                val m = awaitedMoves!!.removeAt(0)
                toCursor!!.removeAt(0)
                move(m.x - player!!.x, m.y - player!!.y)
            }
            // this only happens if we just removed the last Coord from awaitedMoves, and it's only then that we need to
            // re-calculate the distances from all cells to the player. We don't need to calculate this information on
            // each part of a many-cell move (just the end), nor do we need to calculate it whenever the mouse moves.
            if (awaitedMoves!!.isEmpty()) {
                // the next two lines remove any lingering data needed for earlier paths
                playerToCursor!!.clearGoals()
                playerToCursor!!.resetMap()
                // the next line marks the player as a "goal" cell, which seems counter-intuitive, but it works because all
                // cells will try to find the distance between themselves and the nearest goal, and once this is found, the
                // distances don't change as long as the goals don't change. Since the mouse will move and new paths will be
                // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell, so the
                // player's position, and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
                // currently-moused-over cell, which we only need to set where the mouse is being handled.
                playerToCursor!!.setGoal(player)
                playerToCursor!!.scan(null)
            }
        } else if (input!!.hasNext()) {
            input!!.next()
            input!!.flush()
        }// if we are waiting for the player's input and get input, process it.
        input!!.show()

        // stage has its own batch and must be explicitly told to draw().
        stage!!.viewport.apply(true)
        // certain classes that use scene2d.ui widgets need to be told to act() to process input.
        stage!!.act()
        stage!!.draw()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
        input!!.reinitialize(width.toFloat() / this.gridWidth, height.toFloat() / this.gridHeight,
                this.gridWidth.toFloat(), this.gridHeight.toFloat(),
                width / (2 * this.gridWidth), 0, width.toFloat(), height.toFloat())
        input!!.update(width, height, true)
        stage!!.viewport.update(width, height, true)

    }
}
