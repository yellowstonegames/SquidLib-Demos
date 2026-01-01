package com.github.yellowstonegames;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.digital.TrigTools;
import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.random.ChopRandom;
import com.github.tommyettinger.random.EnhancedRandom;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.KnownFonts;
import com.github.yellowstonegames.core.DescriptiveColor;
import com.github.yellowstonegames.core.FullPalette;
import com.github.yellowstonegames.glyph.GlyphActor;
import com.github.yellowstonegames.glyph.GlyphGrid;
import com.github.yellowstonegames.glyph.MoreActions;
import com.github.yellowstonegames.grid.*;
import com.github.yellowstonegames.path.DijkstraMap;
import com.github.yellowstonegames.place.DungeonProcessor;
import com.github.yellowstonegames.text.Language;
import regexodus.Pattern;
import regexodus.Replacer;

import static com.badlogic.gdx.Gdx.input;
import static com.badlogic.gdx.Input.Keys.*;
import static com.github.yellowstonegames.core.DescriptiveColor.*;

public class DungeonDemo extends ApplicationAdapter {
    private static final float DURATION = 0.375f;
    private long startTime, lastMove;

    private Stage stage;
    private GlyphGrid gg;
    private DungeonProcessor dungeonProcessor;
    /**
     * The dungeon map using only {@code '#'} for walls and {@code '.'} for floors.
     */
    private char[][] barePlaceMap;

    /**
     * All floors that the player can walk on.
     */
    private Region floors;

    /**
     * Handles field of view calculations as they change when the player moves around; also, lighting with colors.
     */
    private final VisionFramework vision = new VisionFramework();
    /**
     * The 2D position of the player (the moving character who the FOV centers upon).
     */
    private Coord player;

    private final Coord[] playerArray = new Coord[1];

    private final Noise waves = new Noise(123, 0.5f, Noise.FOAM, 1);
    private GlyphActor playerGlyph;
    private DijkstraMap playerToCursor;
    private final ObjectDeque<Coord> toCursor = new ObjectDeque<>(200);
    private final ObjectDeque<Coord> awaitedMoves = new ObjectDeque<>(200);
    private final ObjectDeque<Coord> nextMovePositions = new ObjectDeque<>(200);
    private Coord cursor = Coord.get(-1, -1);
    private final Vector2 pos = new Vector2();

    public static final int SHOWN_WIDTH = 40;
    public static final int SHOWN_HEIGHT = 25;
    public static final int PLACE_WIDTH = SHOWN_WIDTH;
    public static final int PLACE_HEIGHT = SHOWN_HEIGHT;
    public static final int CELL_WIDTH = 32;
    public static final int CELL_HEIGHT = 32;

    private static final int DEEP_OKLAB = describeOklab("dark dull cobalt");
    private static final int SHALLOW_OKLAB = describeOklab("dull denim");
    private static final int GRASS_OKLAB = describeOklab("duller dark green");
    private static final int DRY_OKLAB = describeOklab("dull light apricot sage");
    private static final int STONE_OKLAB = describeOklab("darkmost gray dullest bronze");
    private static final int deepText = toRGBA8888(offsetLightness(DEEP_OKLAB));
    private static final int shallowText = toRGBA8888(offsetLightness(SHALLOW_OKLAB));
    private static final int grassText = toRGBA8888(offsetLightness(GRASS_OKLAB));
    private static final int stoneText = toRGBA8888(describeOklab("gray dullmost butter bronze"));

    /**
     * Used as the color for remembered cells that can't be currently seen. Slightly-yellow-brown,
     * with about 30% lightness; fully opaque.
     */
    private static final int OKLAB_MEMORY = 0xFF848350;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);
        long seed = TimeUtils.millis() >>> 21;
        Gdx.app.log("SEED", "Initial seed is " + seed);
        EnhancedRandom random = new ChopRandom(seed);

        vision.rememberedColor = OKLAB_MEMORY;

        stage = new Stage();
        KnownFonts.setAssetPrefix("fonts/bitmap/Monogram/");
        Font font = KnownFonts.getMonogramFamily();
//        Font font = KnownFonts.getInconsolata(Font.DistanceFieldType.MSDF).multiplyCrispness(0.5f).scaleTo(15f, 25f).adjustLineHeight(1.25f);
//        font = KnownFonts.getCascadiaMono().scale(0.5f, 0.5f);
//        font = KnownFonts.getIosevka().scale(0.75f, 0.75f);
//        Font font = KnownFonts.getCascadiaMono();
//        Font font = KnownFonts.getInconsolata();
//        font = KnownFonts.getDejaVuSansMono().scale(0.75f, 0.75f);
//        Font font = KnownFonts.getCozette();
//        Font font = KnownFonts.getAStarry();
//        Font font = KnownFonts.getIosevkaMSDF().scaleTo(24, 24);
//        Font font = KnownFonts.getAStarry().scaleTo(16, 16);
//        Font font = KnownFonts.getAStarry().fitCell(24, 24, true);
//        Font font = KnownFonts.getInconsolataMSDF().fitCell(24, 24, true);
        gg = new GlyphGrid(font, PLACE_WIDTH, PLACE_HEIGHT, true);
        //use Ă to test glyph height
        String name = Language.ANCIENT_EGYPTIAN.word(random.nextLong(), true);
        Replacer replacer = Pattern.compile(".*?[aeiouAEIOU](.*)").replacer("$1");
        StringBuilder buffer = new StringBuilder(64).append('@');
        replacer.replace(name, buffer, 1);

        Gdx.app.log("NAME", buffer.toString());

        playerGlyph = new GlyphActor(buffer.charAt(buffer.length()-1), "[red orange]", gg.getFont());
        gg.addActor(playerGlyph);

        dungeonProcessor = new DungeonProcessor(PLACE_WIDTH, PLACE_HEIGHT, random);
        dungeonProcessor.addWater(DungeonProcessor.ALL, 30);
        dungeonProcessor.addGrass(DungeonProcessor.ALL, 10);
        waves.setFractalType(Noise.RIDGED_MULTI);
        input.setInputProcessor(new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode){
                    case ESCAPE:
                    case Q:
                        Gdx.app.exit();
                        break;
                    case R:
                        regenerate();
                        break;
                    default: return false;
                }
                return true;
            }
            // if the user clicks and mouseMoved hasn't already assigned a path to toCursor, then we call mouseMoved
            // ourselves and copy toCursor over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                pos.set(screenX, screenY);
                gg.viewport.unproject(pos);
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
                if(!awaitedMoves.isEmpty())
                    return false;
                pos.set(screenX, screenY);
                gg.viewport.unproject(pos);
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
                        toCursor.removeFirst();
                    }
                }
                return false;
            }
        });

        regenerate();
        stage.addActor(gg);
    }

    public void move(Direction way){
        lastMove = TimeUtils.millis();
        // this prevents movements from restarting while a slide is already in progress.
        if(playerGlyph.hasActions()) return;

        final Coord next = Coord.get(Math.round(playerGlyph.getX() + way.deltaX), Math.round(playerGlyph.getY() + way.deltaY));
        if(next.isWithin(PLACE_WIDTH, PLACE_HEIGHT) && barePlaceMap[next.x][next.y] == '.') {
            playerGlyph.addAction(MoreActions.slideTo(next.x, next.y, 0.2f));
            vision.moveViewer(player, next);
            vision.lighting.moveLight(player, next);
            player = next;
        }
        else{
//            if(MathUtils.randomBoolean())
//                playerGlyph.addAction(MoreActions.bump(way, 0.3f).append(MoreActions.wiggle(0.2f, 0.2f))
//                        .append(new GridAction.ExplosionAction(gg, 1.5f, inView, next, 5)).conclude(post));
//            else
//                playerGlyph.addAction(MoreActions.bump(way, 0.3f).append(MoreActions.wiggle(0.2f, 0.2f))
//                    .append(new GridAction.CloudAction(gg, 1.5f, inView, next, 5).useToxicColors()).conclude(post));
//            playerGlyph.addAction(MoreActions.bump(way, 0.3f).append(MoreActions.wiggle(0.125f, 0.2f)));
            playerGlyph.addAction(MoreActions.bump(way, 0.3f));
            gg.burst((playerGlyph.getX() + next.x) * 0.5f, (playerGlyph.getY() + next.y) * 0.5f, 1.5f, 6, '?', 0x992200FF, 0x99220000, -30f, 0f, 1f);
//            gg.summon(next.x, next.y, next.x, next.y + 0.5f, '?', 0xFF22CCAA, 0xFF22CC00, 0f, 0f, 1f);
//            gg.addAction(gg.dyeFG(next.x, next.y, 0x992200FF, 1f, Float.POSITIVE_INFINITY, null));
        }
        vision.finishChanges();
    }

    public void regenerate(){
        char[][] linePlaceMap = LineTools.hashesToLines(dungeonProcessor.generate(), true);
        dungeonProcessor.setPlaceGrid(linePlaceMap);
        barePlaceMap = dungeonProcessor.getBarePlaceGrid();
        floors = floors == null ? new Region(barePlaceMap, '.') : floors.refill(barePlaceMap, '.');
        player = floors.singleRandom(dungeonProcessor.rng);
        playerGlyph.setPosition(player.x, player.y);
        vision.restart(linePlaceMap, player, 8);
        vision.lighting.addLight(player, new Radiance(8, FullPalette.COSMIC_LATTE, 0.3f, 0f));
        floors.remove(player);

        gg.backgrounds = new int[PLACE_WIDTH][PLACE_HEIGHT];
        gg.map.clear();
        if(playerToCursor == null)
            playerToCursor = new DijkstraMap(barePlaceMap, Measurement.EUCLIDEAN);
        else
            playerToCursor.initialize(barePlaceMap);
        playerToCursor.setGoal(player);
        playerToCursor.partialScan(13, vision.blockage);
        toCursor.clear();
        awaitedMoves.clear();
        nextMovePositions.clear();
        // Starting time for the game; other times are measured relative to this so that they aren't huge numbers.
        startTime = TimeUtils.millis();
        lastMove = startTime;

    }

    public void putMap(){
        int playerX = Math.round(playerGlyph.getX());
        int playerY = Math.round(playerGlyph.getY());
        float change = (float) Math.min(Math.max(TimeUtils.timeSinceMillis(lastMove) * 4f, 0.0), 1000.0);
        // Makes everything visible, mainly for FPS tests
//        ArrayTools.fill(vision.lighting.resistances, 0f);
        vision.update(change);
        final float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        int rainbow = DescriptiveColor.maximizeSaturation(160,
            (int) (TrigTools.sinTurns(time * 0.5f) * 30f) + 128, (int) (TrigTools.cosTurns(time * 0.5f) * 30f) + 128, 255);

        for (int i = 0; i < toCursor.size(); i++) {
            Coord curr = toCursor.get(i);
            if (vision.inView.contains(curr))
                vision.backgroundColors[curr.x][curr.y] = rainbow;
        }

//        float[][] light = vision.lighting.fovResult;

        for (int x = 0; x < PLACE_WIDTH; x++) {
            for (int y = 0; y < PLACE_HEIGHT; y++) {
                char glyph = vision.prunedPlaceMap[x][y];
                if (vision.seen.contains(x, y)) {
                    // cells that were seen more than one frame ago, and aren't visible now, appear as a gray memory.
                    gg.backgrounds[x][y] = toRGBA8888(vision.backgroundColors[x][y]);
                    gg.put(x, y, (x == playerX && y == playerY) ? ' ' : glyph, stoneText);
                }
            }
        }

        for (int y = 0; y < PLACE_HEIGHT; y++) {
            for (int x = 0; x < PLACE_WIDTH; x++) {
                if (vision.seen.contains(x, y)) {
                    {
                        switch (vision.prunedPlaceMap[x][y]) {
                            case '~':
                                gg.backgrounds[x][y] = toRGBA8888(lerpColorsBlended(vision.backgroundColors[x][y], DEEP_OKLAB, 0.4f + 0.3f * waves.getConfiguredNoise(x, y, time)));
                                gg.put(x, y, vision.prunedPlaceMap[x][y], deepText);
                                break;
                            case ',':
                                gg.backgrounds[x][y] = toRGBA8888(lerpColorsBlended(vision.backgroundColors[x][y], SHALLOW_OKLAB, 0.4f + 0.3f * waves.getConfiguredNoise(x, y, time)));
                                gg.put(x, y, vision.prunedPlaceMap[x][y], shallowText);
                                break;
                            case '"':
                                gg.backgrounds[x][y] = toRGBA8888(lerpColorsBlended(vision.backgroundColors[x][y], lerpColors(GRASS_OKLAB, DRY_OKLAB, waves.getConfiguredNoise(x, y) * 0.5f + 0.5f), 0.3f + 0.2f * waves.getConfiguredNoise(x, y, time * 0.7f)));
                                gg.put(x, y, vision.prunedPlaceMap[x][y], grassText);
                                break;
                            case ' ':
                                gg.backgrounds[x][y] = 0;
                                break;
                            default:
                                gg.backgrounds[x][y] = toRGBA8888(lerpColorsBlended(vision.backgroundColors[x][y], STONE_OKLAB, 0.5f));
                                gg.put(x, y, vision.prunedPlaceMap[x][y], stoneText);
                        }
                    }
//                } else if (vision.seen.contains(x, y)) {
//                    switch (vision.prunedPlaceMap[x][y]) {
//                        case '~':
//                            gg.backgrounds[x][y] = toRGBA8888(edit(DEEP_OKLAB, 0f, 0f, 0f, 0f, 0.7f, 0f, 0f, 1f));
//                            gg.put(x, y, vision.prunedPlaceMap[x][y], deepText);
//                            break;
//                        case ',':
//                            gg.backgrounds[x][y] = toRGBA8888(edit(SHALLOW_OKLAB, 0f, 0f, 0f, 0f, 0.7f, 0f, 0f, 1f));
//                            gg.put(x, y, vision.prunedPlaceMap[x][y], shallowText);
//                            break;
//                        case ' ':
//                            gg.backgrounds[x][y] = 0;
//                            break;
//                        default:
//                            gg.backgrounds[x][y] = toRGBA8888(edit(STONE_OKLAB, 0f, 0f, 0f, 0f, 0.7f, 0f, 0f, 1f));
//                            gg.put(x, y, vision.prunedPlaceMap[x][y], stoneText);
//                    }
                } else {
                    gg.backgrounds[x][y] = 0;
                }
            }
        }
    }

    /**
     * Supports WASD, vi-keys (hjklyubn), arrow keys, and numpad for movement, plus '.' or numpad 5 to stay still.
     */
    public void handleHeldKeys() {
        if(input.isKeyPressed(A) || input.isKeyPressed(H) || input.isKeyPressed(LEFT) || input.isKeyPressed(NUMPAD_4))
            move(Direction.LEFT);
        else if(input.isKeyPressed(S) || input.isKeyPressed(J) || input.isKeyPressed(DOWN) || input.isKeyPressed(NUMPAD_2))
            move(Direction.DOWN);
        else if(input.isKeyPressed(W) || input.isKeyPressed(K) || input.isKeyPressed(UP) || input.isKeyPressed(NUMPAD_8))
            move(Direction.UP);
        else if(input.isKeyPressed(D) || input.isKeyPressed(L) || input.isKeyPressed(RIGHT) || input.isKeyPressed(NUMPAD_6))
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

    @Override
    public void render() {
        putMap();
        handleHeldKeys();

        if(!gg.areChildrenActing() && !awaitedMoves.isEmpty())
        {
            Coord m = awaitedMoves.removeFirst();
            if (!toCursor.isEmpty())
                toCursor.removeFirst();
            move(playerGlyph.getLocation().toGoTo(m));
        }
        else {
            if (!gg.areChildrenActing()) {
//                postMove();
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
                    playerToCursor.setGoal(playerGlyph.getLocation());
                    // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
                    // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
                    // GreasedRegion that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(13, vision.blockage);
                }
            }
        }

        ScreenUtils.clear(Color.BLACK);
        // if stage.act() is called before the camera is centered on the player,
        // that makes the camera change more smoothly. If act() is called after,
        // there can be short "hiccups" in the movement.
        stage.act();

        Camera camera = gg.viewport.getCamera();
        camera.position.set(playerGlyph.getX(), playerGlyph.getY(), 0f);
        // can be used to center the camera in the same place always.
//        camera.position.set(gg.getGridWidth() * 0.5f, gg.getGridHeight() * 0.5f, 0f);
//        camera.update(); // called already by stage.draw()

        stage.draw();
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        gg.resize(width, height);
    }

    private boolean onGrid(int screenX, int screenY)
    {
        return screenX >= 0 && screenX < PLACE_WIDTH && screenY >= 0 && screenY < PLACE_HEIGHT;
    }
}
