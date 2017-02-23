package com.github.SquidPony;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import squidpony.FakeLanguageGen;
import squidpony.panel.IColoredString;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.MixedGenerator;
import squidpony.squidmath.*;

import java.util.ArrayList;
import java.util.List;

public class TsarGame extends ApplicationAdapter {
    private enum Phase {WAIT, PLAYER_ANIM, MONSTER_ANIM}
    private static class Monster {
        public AnimatedEntity entity;
        public int state;

        public Monster(AnimatedEntity ae, int state)
        {
            entity = ae;
            this.state = state;
        }
        public Monster change(int state)
        {
            this.state = state;
            return this;
        }

    }
    SpriteBatch batch;

    private Phase phase = Phase.WAIT;
    private StatefulRNG rng;
    private SquidLayers display;
    private SquidPanel subCell;
    private SquidMessageBox messages;
    /**
     * Non-{@code null} iff '?' was pressed before
     */
    private /*Nullable*/ Actor help;
    private DungeonGenerator dungeonGen;
    private char[][] decoDungeon, bareDungeon, lineDungeon;
    private double[][] res;
    private int[][] lights;
    private Color[][] colors, bgColors;
    private double[][] fovmap;
    private AnimatedEntity player;
    private FOV fov;
    /**
     * In number of cells
     */
    private int width;
    /**
     * In number of cells
     */
    private int height;
    /**
     * The pixel width of a cell
     */
    private int cellWidth;
    /**
     * The pixel height of a cell
     */
    private int cellHeight;
    private VisualInput input;
    private double counter;
    private boolean[][] seen;
    private int health = 7;
    private Color bgColor;
    private OrderedMap<Coord, Monster> monsters;
    private DijkstraMap getToPlayer, playerToCursor;
    private Stage stage;
    private int framesWithoutAnimation = 0;
    private Coord cursor;
    private ArrayList<Coord> toCursor;
    private ArrayList<Coord> awaitedMoves;
    private String lang;
    private TextCellFactory textFactory;
    public static final int INTERNAL_ZOOM = 1;
    private Viewport viewport;
    private float currentZoomX = INTERNAL_ZOOM, currentZoomY = INTERNAL_ZOOM;
    private SquidColorCenter scc;
    @Override
    public void create () {
        // gotta have a random number generator. We seed a LightRNG with any long we want, then pass that to an RNG.
        rng = new StatefulRNG(0xBADBEEFB0BBL);

        batch = new SpriteBatch();
        width = 80;
        height = 28;
        //NOTE: cellWidth and cellHeight are assigned values that are significantly larger than the corresponding sizes
        //in the EverythingDemoLauncher's main method. Because they are scaled up by an integer here, they can be scaled
        //down when rendered, allowing certain small details to appear sharper. This _only_ works with distance field,
        //a.k.a. stretchable, fonts! INTERNAL_ZOOM is a tradeoff between rendering more pixels to increase quality (when
        // values are high) or rendering fewer pixels for speed (when values are low). Using 2 seems to work well.
        cellWidth = 13 * INTERNAL_ZOOM;
        cellHeight = 26 * INTERNAL_ZOOM;

        // Sets our color center, which can be used to filter colors with various effects, though we don't really use it here.
        scc = DefaultResources.getSCC();
        // getStretchableFont loads an embedded font, Inconsolata-LGC-Custom, that is a distance field font as mentioned
        // earlier. We set the smoothing multiplier on it only because we are using internal zoom to increase sharpness
        // on small details, but if the smoothing is incorrect some sizes look blurry or over-sharpened. This can be set
        // manually if you use a constant internal zoom; here we use 1f for internal zoom 1, about 2/3f for zoom 2, and
        // about 1/2f for zoom 3. If you have more zooms as options for some reason, this formula should hold for many
        // cases but probably not all.
        textFactory = DefaultResources.getStretchableFont().setSmoothingMultiplier(2f / (INTERNAL_ZOOM + 1f))
                .width(cellWidth).height(cellHeight).initBySize();
        // Creates a layered series of text grids in a SquidLayers object, using the previously set-up textFactory and
        // SquidColorCenters.
        display = new SquidLayers(width, height, cellWidth, cellHeight,
                textFactory.copy());
        //subCell is a SquidPanel, the same class that SquidLayers has for each of its layers, but we want to render
        //certain effects on top of all other panels, which can't be done in the all-in-one-pass rendering of the grids
        //in SquidLayers, though it could be done with a slight hassle if the effects are made into AnimatedEntity
        //objects or Actors, then rendered separately like the monsters are (see render() below). It is called subCell
        //because its text will be made smaller than a full cell, and appears in the upper left corner for things like
        //the current health of the player and an '!' for alerted monsters.
        subCell = new SquidPanel(width, height, textFactory.copy());

        display.setAnimationDuration(0.1f);
        messages = new SquidMessageBox(width, 4,
                textFactory.copy());
        // a bit of a hack to increase the text height slightly without changing the size of the cells they're in.
        // this causes a tiny bit of overlap between cells, which gets rid of an annoying gap between vertical lines.
        // if you use '#' for walls instead of box drawing chars, you don't need this.
        messages.setTextSize(cellWidth, cellHeight + INTERNAL_ZOOM * 2);
        display.setTextSize(cellWidth, cellHeight + INTERNAL_ZOOM * 2);
        //The subCell SquidPanel uses a smaller size here; the numbers 8 and 16 should change if cellWidth or cellHeight
        //change, and the INTERNAL_ZOOM multiplier keeps things sharp, the same as it does all over here.
        subCell.setTextSize(8 * INTERNAL_ZOOM, 16 * INTERNAL_ZOOM);
        viewport = new StretchViewport(width * cellWidth, (height + 4) * cellHeight);
        stage = new Stage(viewport, batch);

        //These need to have their positions set before adding any entities if there is an offset involved.
        messages.setBounds(0, 0, cellWidth * width, cellHeight * 4);
        display.setPosition(0, messages.getHeight());
        subCell.setPosition(0, messages.getHeight());
        messages.appendWrappingMessage("Use numpad or vi-keys (hjklyubn) to move. Use ? for help, f to change colors, q to quit." +
                " Click the top or bottom border of this box to scroll.");
        counter = 0;


        dungeonGen = new DungeonGenerator(width, height, rng);
        dungeonGen.addWater(30, 6);
        dungeonGen.addGrass(5);
        dungeonGen.addBoulders(10);
        dungeonGen.addDoors(18, false);
        MixedGenerator mix = new MixedGenerator(width, height, rng);
        mix.putCaveCarvers(1);
        mix.putBoxRoomCarvers(1);
        mix.putRoundRoomCarvers(2);
        char[][] mg = mix.generate();
        decoDungeon = dungeonGen.generate(mg);

        // change the TilesetType to lots of different choices to see what dungeon works best.
        //bareDungeon = dungeonGen.generate(TilesetType.DEFAULT_DUNGEON);
        bareDungeon = dungeonGen.getBareDungeon();
        lineDungeon = DungeonUtility.hashesToLines(dungeonGen.getDungeon(), true);
        // it's more efficient to get random floors from a packed set containing only (compressed) floor positions.
        GreasedRegion placement = new GreasedRegion(bareDungeon, '.');
        Coord pl = placement.singleRandom(rng);
        placement.remove(pl);
        int numMonsters = 25;
        monsters = new OrderedMap<Coord, Monster>(numMonsters);
        for (int i = 0; i < numMonsters; i++) {
            Coord monPos = placement.singleRandom(rng);
            placement.remove(monPos);
            monsters.put(monPos, new Monster(display.animateActor(monPos.x, monPos.y, 'Я',
                    SColor.CRIMSON), 0));
        }
        // your choice of FOV matters here.
        fov = new FOV(FOV.RIPPLE_TIGHT);
        res = DungeonUtility.generateResistances(decoDungeon);
        fovmap = fov.calculateFOV(res, pl.x, pl.y, 8, Radius.SQUARE);
        getToPlayer = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.CHEBYSHEV);
        getToPlayer.rng = rng;
        // just showing off a little here; we can use smoothly changing colors for the special AnimatedEntity values we
        // use for the player and monsters
        player = display.animateActor(pl.x, pl.y, '@',
                scc.loopingGradient(SColor.CAPE_JASMINE, SColor.HAN_PURPLE, 45), 1.5f, false);
        cursor = Coord.get(-1, -1);
        toCursor = new ArrayList<Coord>(10);
        awaitedMoves = new ArrayList<Coord>(10);
        playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.EUCLIDEAN);
        final int[][] initialColors = DungeonUtility.generatePaletteIndices(decoDungeon),
                initialBGColors = DungeonUtility.generateBGPaletteIndices(decoDungeon);
        colors = new Color[width][height];
        bgColors = new Color[width][height];
        ArrayList<Color> palette = display.getPalette();
        bgColor = SColor.DARK_SLATE_GRAY;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                colors[i][j] = palette.get(initialColors[i][j]);
                bgColors[i][j] = palette.get(initialBGColors[i][j]);
            }
        }
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, counter);
        seen = new boolean[width][height];
        lang = FakeLanguageGen.RUSSIAN_AUTHENTIC.sentence(rng, 4, 6, new String[]{",", ",", ",", " -"},
                new String[]{"..."}, 0.25);
        // this is a big one.
        // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
        // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
        // keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
        // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
        // implementation here handles hjklyubn keys for 8-way movement, numpad for 8-way movement, arrow keys for
        // 4-way movement, and wasd for 4-way movement. Shifted letter keys produce capitalized chars when passed to
        // KeyHandler.handle(), but we don't care about that so we just use two case statements with the same body,
        // one for the lower case letter and one for the upper case letter.
        // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
        // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
        // the event when someone clicks.
        input = new VisualInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key) {
                    case SquidInput.UP_ARROW:
                    case 'k':
                    case 'w':
                    case 'K':
                    case 'W': {
                        move(0, -1);
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 'j':
                    case 's':
                    case 'J':
                    case 'S': {
                        move(0, 1);
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'h':
                    case 'a':
                    case 'H':
                    case 'A': {
                        move(-1, 0);
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'l':
                    case 'd':
                    case 'L':
                    case 'D': {
                        move(1, 0);
                        break;
                    }

                    case SquidInput.UP_LEFT_ARROW:
                    case 'y':
                    case 'Y': {
                        move(-1, -1);
                        break;
                    }
                    case SquidInput.UP_RIGHT_ARROW:
                    case 'u':
                    case 'U': {
                        move(1, -1);
                        break;
                    }
                    case SquidInput.DOWN_RIGHT_ARROW:
                    case 'n':
                    case 'N': {
                        move(1, 1);
                        break;
                    }
                    case SquidInput.DOWN_LEFT_ARROW:
                    case 'b':
                    case 'B': {
                        move(-1, 1);
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE: {
                        Gdx.app.exit();
                        break;
                    }
                    case '?':
                    default: {
                        toggleHelp();
                        break;
                    }
                }
            }
        }, new SquidMouse(cellWidth, cellHeight, width, height, 0, 0, new InputAdapter() {

            // if the user clicks within FOV range and there are no awaitedMoves queued up, generate toCursor if it
            // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(fovmap[screenX][screenY] > 0.0 && awaitedMoves.isEmpty()) {
                    if (toCursor.isEmpty()) {
                        cursor = Coord.get(screenX, screenY);
                        //Uses DijkstraMap to get a path. from the player's position to the cursor
                        toCursor = playerToCursor.findPath(30, null, null, Coord.get(player.gridX, player.gridY), cursor);
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
                if(fovmap[screenX][screenY] > 0.0) {
                    cursor = Coord.get(screenX, screenY);
                    //Uses DijkstraMap to get a path. from the player's position to the cursor
                    toCursor = playerToCursor.findPath(30, null, null, Coord.get(player.gridX, player.gridY), cursor);
                }
                return false;
            }
        }));
        //set this to true to test visual input on desktop
        input.forceButtons = false;
        //actions to give names to in the visual input menu
        input.init("filter", "??? help?", "quit");
        // ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        subCell.setOffsetY(messages.getGridHeight() * cellHeight);
        // and then add display and messages, our two visual components, to the list of things that act in Stage.
        stage.addActor(display);
        // stage.addActor(subCell); // this is not added since it is manually drawn after other steps
        stage.addActor(messages);
        viewport = input.resizeInnerStage(stage);
    }
    /**
     * Move the player or open closed doors, remove any monsters the player bumped, then update the DijkstraMap and
     * have the monsters that can see the player try to approach.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {
        clearHelp();

        if (health <= 0) return;
        int newX = player.gridX + xmod, newY = player.gridY + ymod;
        if (newX >= 0 && newY >= 0 && newX < width && newY < height
                && bareDungeon[newX][newY] != '#') {
            // '+' is a door.
            if (lineDungeon[newX][newY] == '+') {
                decoDungeon[newX][newY] = '/';
                lineDungeon[newX][newY] = '/';
                // changes to the map mean the resistances for FOV need to be regenerated.
                res = DungeonUtility.generateResistances(decoDungeon);
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov.calculateFOV(res, player.gridX, player.gridY, 8, Radius.SQUARE);

            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                fovmap = fov.calculateFOV(res, newX, newY, 8, Radius.SQUARE);
                display.slide(player, newX, newY);
                // if a monster was at the position we moved into, and so was successfully removed...
                if(monsters.remove(Coord.get(newX, newY)) != null)
                {
                    // ...then we make a little blood burst effect.
                    display.getForegroundLayer().burst(
                            // the position
                            newX, newY,
                            //what it should look like (eight ' marks)
                            true, '\'',
                            // the color starts as "blood-colored" and fades to transparent
                            SColor.BLOOD,
                            // you can alter this to change the duration of the effect
                            1f);
                }
            }
            phase = Phase.PLAYER_ANIM;
        }
    }

    private void postMove()
    {
        phase = Phase.MONSTER_ANIM;
        Coord[] playerArray = {Coord.get(player.gridX, player.gridY)};
        // in some cases you can use keySet() to get a Set of keys, but that makes a read-only view, and we want
        // a copy of the key set that we can edit (so monsters don't move into each others' spaces)
        OrderedSet<Coord> monplaces = monsters.keysAsOrderedSet();
        int monCount = monplaces.size();

        // recalculate FOV, store it in fovmap for the render to use.
        fovmap = fov.calculateFOV(res, player.gridX, player.gridY, 8, Radius.SQUARE);
        // handle monster turns
        ArrayList<Coord> nextMovePositions;
        for(int ci = 0; ci < monCount; ci++)
        {
            Coord pos = monplaces.removeFirst();
            Monster mon = monsters.get(pos);
            // monster values are used to store their aggression, 1 for actively stalking the player, 0 for not.
            if (mon.state > 0 || fovmap[pos.x][pos.y] > 0.1) {
                if (mon.state == 0) {
                    messages.appendMessage("The AЯMED GUAЯD shouts at you, \"" +
                            FakeLanguageGen.RUSSIAN_AUTHENTIC.sentence(rng, 1, 3,
                                    new String[]{",", ",", ",", " -"}, new String[]{"!"}, 0.25) + "\"");
                }
                getToPlayer.clearGoals();
                nextMovePositions = getToPlayer.findPath(1, monplaces, null, pos, playerArray);
                if (nextMovePositions != null && !nextMovePositions.isEmpty()) {
                    Coord tmp = nextMovePositions.get(0);
                    // if we would move into the player, instead damage the player and give newMons the current
                    // position of this monster.
                    if (tmp.x == player.gridX && tmp.y == player.gridY) {
                        display.tint(player.gridX, player.gridY, SColor.PURE_CRIMSON, 0, 0.415f);
                        health--;
                        mon.change(1);
                        monplaces.add(pos);
                    }
                    // otherwise store the new position in newMons.
                    else {
                        mon.change(1);
                        // alter is a method on OrderedMap and OrderedSet that changes a key in-place
                        monsters.alter(pos, tmp);
                        display.slide(mon.entity, tmp.x, tmp.y);
                        monplaces.add(tmp);
                    }
                } else {
                    mon.change(1);
                    monplaces.add(pos);
                }
            }
            else
            {
                monplaces.add(pos);
            }
        }

    }

    private void toggleHelp() {
        if(help != null)
        {
            clearHelp();
            return;
        }
        final int nbMonsters = monsters.size();

		/* Prepare the String to display */
        final IColoredString<Color> cs = new IColoredString.Impl<Color>();
        cs.append("Still ", null);
        final Color nbColor;
        if (nbMonsters <= 1)
            /* Green */
            nbColor = Color.GREEN;
        else if (nbMonsters <= 5)
            /* Orange */
            nbColor = Color.ORANGE;
        else
            /* Red */
            nbColor = Color.RED;
        cs.appendInt(nbMonsters, nbColor);
        cs.append(" guard"+(nbMonsters == 1 ? "" : "s")+" to shank", null);

        IColoredString<Color> helping1 = new IColoredString.Impl<Color>("Use numpad or vi-keys (hjklyubn) to move.", Color.WHITE);
        IColoredString<Color> helping2 = new IColoredString.Impl<Color>("Use ? for help, f to change colors, q to quit.", Color.WHITE);
        IColoredString<Color> helping3 = new IColoredString.Impl<Color>("Click the top or bottom border of the lower message box to scroll.", Color.WHITE);
        IColoredString<Color> helping4 = new IColoredString.Impl<Color>("Each Я is an AЯMED GUAЯD; bump into them to kill them.", Color.WHITE);
        IColoredString<Color> helping5 = new IColoredString.Impl<Color>("If an Я starts its turn next to where you just moved, you take damage.", Color.WHITE);


        /* Some grey color */
        final Color bgColor = new Color(0.3f, 0.3f, 0.3f, 0.9f);

        final Actor a;

			/*
			 * Use TextPanel. With this class, we can use a more legible variable-width font.
			 */
            final TextPanel<Color> tp = new TextPanel<Color>(new GDXMarkup(), DefaultResources.getStretchablePrintFont());
            tp.backgroundColor = SColor.DARK_SLATE_GRAY;

            final List<IColoredString<Color>> text = new ArrayList<IColoredString<Color>>();
            text.add(cs);
			/* No need to call IColoredString::wrap, TextPanel does it on its own */
            text.add(helping1);
            text.add(helping2);
            text.add(helping3);
            text.add(helping4);
            text.add(helping5);

            final float w = width * cellWidth, aw = helping3.length() * cellWidth * 0.85f * INTERNAL_ZOOM;
            final float h = height * cellHeight, ah = cellHeight * 13 * INTERNAL_ZOOM;
            tp.init(aw, ah, text);
            a = tp.getScrollPane();
            final float x = (w - aw) / 2f;
            final float y = (h - ah) / 2f;
            a.setPosition(x, y);
            stage.setScrollFocus(a);

        help = a;

        stage.addActor(a);
    }

    private void clearHelp() {
        if (help == null)
			/* Nothing to do */
            return;
        help.clear();
        stage.getActors().removeValue(help, true);
        help = null;
    }

    public void putMap()
    {
        boolean overlapping;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                overlapping = monsters.containsKey(Coord.get(i,j)) || (player.gridX == i && player.gridY == j);
                // if we see it now, we remember the cell and show a lit cell based on the fovmap value (between 0.0
                // and 1.0), with 1.0 being brighter at +75 lightness and 0.0 being rather dark at -105.
                if (fovmap[i][j] > 0.0) {
                    seen[i][j] = true;
                    display.put(i, j, (overlapping) ? ' ' : lineDungeon[i][j], colors[i][j], bgColors[i][j],
                            lights[i][j] + (int) (-105 + 180 * fovmap[i][j]));
                    // if we don't see it now, but did earlier, use a very dark background, but lighter than black.
                } else if (seen[i][j]) {
                    display.put(i, j, lineDungeon[i][j], colors[i][j], bgColors[i][j], -140);
                }
            }
        }
        for (Coord pt : toCursor)
        {
            // use a brighter light to trace the path to the cursor, from 170 max lightness to 0 min.
            display.highlight(pt.x, pt.y, lights[pt.x][pt.y] + (int) (170 * fovmap[pt.x][pt.y]));
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // not sure if this is always needed...
        //Gdx.gl.glEnable(GL20.GL_BLEND);

        // used as the z-axis when generating Simplex noise to make water seem to "move"
        counter += Gdx.graphics.getDeltaTime() * 15;
        // this does the standard lighting for walls, floors, etc. but also uses counter to do the Simplex noise thing.
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, counter);
        //textFactory.configureShader(batch);

        // you done bad. you done real bad.
        if (health <= 0) {
            // still need to display the map, then write over it with a message.
            putMap();
            display.putBoxedString(width / 2 - 18, height / 2 - 10, "   THE TSAR WILL HAVE YOUR HEAD!    ");
            display.putBoxedString(width / 2 - 18, height / 2 - 5,  "      AS THE OLD SAYING GOES,       ");
            display.putBoxedString(width / 2 - lang.length() / 2, height / 2, lang);
            display.putBoxedString(width / 2 - 18, height / 2 + 5,  "             q to quit.             ");

            // because we return early, we still need to draw.
            stage.draw();
            // q still needs to quit.
            if(input.hasNext())
                input.next();
            return;
        }
        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {

            // extremely similar to the block below that also checks if animations are done
            // this doesn't check for input, but instead processes and removes Points from awaitedMoves.
            if(!display.hasActiveAnimations()) {
                ++framesWithoutAnimation;
                if (framesWithoutAnimation >= 3) {
                    framesWithoutAnimation = 0;
                    switch (phase) {
                        case WAIT:
                        case MONSTER_ANIM:
                            Coord m = awaitedMoves.remove(0);
                            toCursor.remove(0);
                            move(m.x - player.gridX, m.y - player.gridY);
                            break;
                        case PLAYER_ANIM:
                            postMove();
                            break;
                    }
                }
            }
        }
        // if we are waiting for the player's input and get input, process it.
        else if(input.hasNext() && !display.hasActiveAnimations() && phase == Phase.WAIT) {
            input.next();
        }
        // if the previous blocks didn't happen, and there are no active animations, then either change the phase
        // (because with no animations running the last phase must have ended), or start a new animation soon.
        else if(!display.hasActiveAnimations()) {
            ++framesWithoutAnimation;
            if (framesWithoutAnimation >= 3) {
                framesWithoutAnimation = 0;
                switch (phase) {
                    case WAIT:
                        break;
                    case MONSTER_ANIM: {
                        phase = Phase.WAIT;
                    }
                    break;
                    case PLAYER_ANIM: {
                        postMove();

                    }
                }
            }
        }
        // if we do have an animation running, then how many frames have passed with no animation needs resetting
        else
        {
            framesWithoutAnimation = 0;
        }

        input.show();
        // stage has its own batch and must be explicitly told to draw(). this also causes it to act().
        stage.getViewport().apply(true);
        stage.draw();
        stage.act();

        subCell.erase();
        if(help == null) {
            // display does not draw all AnimatedEntities by default, since FOV often changes how they need to be drawn.
            batch.begin();
            // the player needs to get drawn every frame, of course.
            display.drawActor(batch, 1.0f, player);
            subCell.put(player.gridX, player.gridY, Character.forDigit(health, 10), SColor.PURE_CRIMSON);

            Monster mon;
            for (int i = 0; i < monsters.size(); i++) {
                mon = monsters.getAt(i);
                // monsters are only drawn if within FOV.
                if (fovmap[mon.entity.gridX][mon.entity.gridY] > 0.0) {
                    display.drawActor(batch, 1.0f, mon.entity);
                    if(mon.state > 0)
                        subCell.put(mon.entity.gridX, mon.entity.gridY, '!', SColor.DARK_RED);
                }
            }
            subCell.draw(batch, 1.0F);
            // batch must end if it began.
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // message box won't respond to clicks on the far right if the stage hasn't been updated with a larger size
        currentZoomX = width * 1f / this.width;
        // total new screen height in pixels divided by total number of rows on the screen
        currentZoomY = height * 1f / (this.height + messages.getGridHeight());
        // message box should be given updated bounds since I don't think it will do this automatically
        messages.setBounds(0, 0, width, currentZoomY * messages.getGridHeight());
        // SquidMouse turns screen positions to cell positions, and needs to be told that cell sizes have changed
        input.reinitialize(currentZoomX, currentZoomY, this.width, this.height, 0, 0, width, height);
        currentZoomX = cellWidth / currentZoomX;
        currentZoomY = cellHeight / currentZoomY;
        input.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }
}
