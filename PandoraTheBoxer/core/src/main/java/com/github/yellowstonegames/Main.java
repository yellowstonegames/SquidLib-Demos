package com.github.yellowstonegames;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArraySupplier;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.tommyettinger.random.EnhancedRandom;
import com.github.tommyettinger.random.Xoshiro160RoadroxoRandom;
import com.github.yellowstonegames.game.AssetData;
import com.github.yellowstonegames.game.Mover;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.grid.Region;

import java.util.Comparator;

import static com.github.yellowstonegames.util.MathSupport.INVERSE_ROOT_2;

/**
 * This is the primary starting point in the core module, and the only platform-specific code should be in "Launcher"
 * classes in other modules. This is an isometric pixel art demo project where the player (a little person in blue) runs
 * around trying to save goldfish (little orange fish out of water) without bumping into enemies (green-skinned, brawny
 * orcs). Bumping into enemies will take away your health, and reaching 0 health is a game-over condition. Saving all 10
 * goldfish is the win condition. Arrows point to your blue player character on the edges of the map.
 * <br>
 * This uses a special kind of coordinates because isometric coordinates just don't correspond nicely to x, y,
 * and z with any common convention. Here, when referring to isometric tiles, we use "f, g, h" positions.The f and g
 * axes are diagonal, and correspond to movement at a shallow angle on both world x and world y. The h axis is used for
 * elevation, and corresponds to movement on world y. The mnemonics here work as if on a world map, with the origin
 * somewhere in Belgium or the Netherlands:
 * <ul>
 *     <li>The f axis is roughly the diagonal from France to Finland.</li>
 *     <li>The g axis is roughly the diagonal from Germany to Greenland (or Greece to Germany, or Greece to Greenland).</li>
 *     <li>The h axis is the vertical line from your heel to your head (or Hell to Heaven).</li>
 * </ul>
 * The letters "t" and "r" also show up, with the geography mnemonics "Tallinn" (in Estonia, near Finland) and Reykjavík
 * (in Iceland, on the way to Greenland); these refer specifically to the positive f direction and positive g direction,
 * or the back faces of a voxel, while f and g refer to the faces on the front. "fgtr" are the recommended keys on a
 * QWERTY keyboard to move in those directions, or on a map of Europe, the locations of France, Germany, Tallinn (in
 * Estonia), and Reykjavík (in Iceland) relative to Amsterdam in the center. The "fgtr" keys also are close in shape to
 * matching the X-shape of directions you can travel on one axis at a time in isometric coordinates, at least on a
 * QWERTY keyboard.
 * <br>
 * When a Vector3 is used for an isometric position, x,y,z refer to f,g,h. This code also sometimes uses Vector4 when
 * camera distance needs to vary; in that case, the w coordinate refers to camera depth. While movement on the f and g
 * axes moves an object on both screen x and y, movement on the h axis only moves an object on screen y, and movement on
 * depth only changes its sort order (potentially rendering it before or after other objects, and sometimes covering it
 * up entirely). A major goal of this demo is to show how sort order works using only a SpriteBatch to draw things with
 * 3D (or really, 2.5D) positions, using only 2D sprites.
 */
public class Main extends ApplicationAdapter {
    /**
     * Used to draw things from back to front as 2D sprites. Even though our tiles use 3D positions. Well, 4D. Don't run
     * away, I explained all of this in the class comment!
     */
    private SpriteBatch batch;
    /**
     * This is the file name of the atlas of 2D gameplay assets used in the game. It uses
     * <a href="https://gvituri.itch.io/isometric-trpg">these free-to-use assets by Gustavo Vituri</a> and
     * <a href="https://ray3k.wordpress.com/clean-crispy-ui-skin-for-libgdx/">a skin originally by Raymond Buckley</a>.
     * <br>
     * CUSTOM TO YOUR GAME. This is closely related to {@link AssetData}, and if one changes, both should.
     */
    public static final String ATLAS_FILE_NAME = "PandoraTheBoxer.atlas";
    /**
     * This is the actual TextureAtlas of 2D assets used in the game. It uses
     * <a href="https://gvituri.itch.io/isometric-trpg">these free-to-use assets by Gustavo Vituri</a> and
     * <a href="https://ray3k.wordpress.com/clean-crispy-ui-skin-for-libgdx/">a skin by Raymond Buckley</a> with the
     * font changed to <a href="https://github.com/playbeing/dinish">DINish</a>.
     * <br>
     * CUSTOM TO YOUR GAME. This is closely related to {@link AssetData}, and if one changes, both should.
     */
    private TextureAtlas atlas;
    /**
     * Animations taken from {@link #atlas} to be loaded in a more game-runtime-friendly format.
     * Index 0 is front-facing idle animations.
     * Index 1 is rear-facing idle animations.
     * Index 2 is front-facing attack animations.
     * Index 3 is rear-facing attack animations.
     * Inside each of those four Arrays, there is an Array of many Animations of AtlasSprites, with each Animation for a
     * different type of monster or character.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    private Array<Array<Animation<AtlasSprite>>> animations;

    /**
     * This is drawn separately from the gameplay, at 100% pixel-perfect size.
     */
    private Stage stage;
    /**
     * Used so mobile users can actually play the game! This is an on-screen knob that can be dragged in any direction,
     * to move the player around.
     */
    private Touchpad touchpad;
    /**
     * A mapping of keys (the ones a desktop would use to play the game) to TextButtons that can be used on mobile
     * devices. Only a few (or maybe just one) TextButton is actually accessed via this Map.
     * This can be extended in your game to include any buttons that can be held down and act like holding down a key.
     */
    private IntMap<TextButton> keyButtons;

    /**
     * Can be changed to make the game harder with more fish to save, or easier with fewer.
     */
    public static int FISH_COUNT = 10;
    /**
     * Can be changed to make the game harder with more enemies, or easier with fewer.
     */
    public static int ENEMY_COUNT = 10;
    /**
     * Used frequently here, this is the current location map that gameplay takes place in, which also stores the
     * inhabitants of that level.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    private LocalMap map;

    public EnhancedRandom random;
    /**
     * The camera we use to show things with an isometric, or for sticklers, "dimetric" camera projection.
     * We can't use a PerspectiveCamera here, even with sort-of 3D positions, because it wouldn't be pixel-perfect.
     */
    private OrthographicCamera camera;
    /**
     * ScreenViewport is used here for a screen that will be drawn pixel-perfect with 8x scaling, and
     * later scaled down using {@link #growingViewport} with arbitrary scaling. Each pixel drawn with this will be a
     * 8x8 pixel square on the framebuffer before it gets scaled down. If {@link #ZOOM} changes, 8 will change.
     */
    private ScreenViewport viewport;
    /**
     * A FitViewport is used to draw the pixel-perfect zoomed-in screen so it fits on the user's chosen size of window.
     * This usually shinks the zoomed-in framebuffer "canvas" a lot.
     */
    private Viewport growingViewport;
    /**
     * A way to draw a zoomed-in pixel-perfect image off-screen, then later draw it larger with better scaling. This
     * has its texture use linear filtering, which blurs the edges between pixels, but because the image is zoomed-in,
     * only a small part of each 8x8 pixel square actually gets blurred. If the entire square was subject to blur, the
     * image would turn to "mush" at anything but an integer multiple for scale.
     */
    private FrameBuffer buffer;

    /**
     * Mover represents any moving creature or hazard, and can be a player character or non-player character (NPC).
     * This is the player, which has {@code npc = false;} and so won't move on their own.
     */
    private Mover player;

//    /**
//     * A "crosshair-like" indicator of where the player character is on the x-axis, for when they can't be seen.
//     */
//    private Sprite playerAxisX;
//    /**
//     * A "crosshair-like" indicator of where the player character is on the y-axis, for when they can't be seen.
//     */
//    private Sprite playerAxisY;
    /**
     * The enemies are stored in a simple Array. There aren't ever so many of them that the data structure could matter.
     * There's currently no logic for an enemy receiving damage; the player just tries to avoid the orc enemies.
     */
    private Array<Mover> enemies;
    /**
     * Shows current frames per second on the screen; you can remove this in production.
     */
    private Label fpsLabel;
    /**
     * Shows how many goldfish you need to rescue, if you have "won" by saving all goldfish, or if you have "died" by
     * taking 3 separate hits (you are hit by touching an enemy, which is always a green orc here).
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Label goalLabel;
    /**
     * Only shows the current health of the player, using {@code "<3"} for each point of health.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Label healthLabel;
    /**
     * This currently plays a public domain song by Komiku, "Road 4 Fight", the entire time.
     * I hope it isn't too annoying to be playing on loop...
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Music backgroundMusic;

    /**
     * This is the limit the current FPS will max out at unless the player un-limits FPS by pressing 'C'.
     * With simple pixel art graphics, a higher frame rate is likely not noticeable, though input might be affected.
     */
    public static final int FRAME_RATE_LIMIT = 60;

    /**
     * Currently, pressing 'c' will toggle the frame rate cap, so you can see if any physics changes are still
     * frame-rate-independent.
     */
    private int cap = FRAME_RATE_LIMIT;
    /**
     * The base width and height in tiles of a map; this may vary slightly when the map is created, for variety.
     * The variance only goes up by 0 to 3 width and height (by the same amount).
     */
    public static final int MAP_SIZE = 20;
    /**
     * The maximum number of voxels and creatures that can be stacked on top of each other in the map; typically also
     * includes some room to jump higher.
     */
    public static final int MAP_PEAK = 10;
    /**
     * A scale factor applied to the smaller pixel art "canvas" so scaling it doesn't get as blurry. This works well
     * if it's set to about 4 or 8, with 4 being a little more blurry and 8 being a little less. They're very similar.
     */
    public static int ZOOM = 4;
    /**
     * To avoid having to cast ZOOM to a float and divide by it repeatedly, we just store its inverse here.
     */
    public static float INVERSE_ZOOM = 1f / ZOOM;
    /**
     * The computed width in pixels of a 1x zoom complete map at its largest possible {@link #MAP_SIZE}.
     * This is used for the default window size.
     */
    public static final int SCREEN_HORIZONTAL = ((MAP_SIZE+3) * 2 * AssetData.TILE_WIDTH);
    /**
     * The computed height in pixels of a 1x zoom complete map at its largest possible {@link #MAP_SIZE} and
     * {@link #MAP_PEAK}. This is used for the default window size.
     */
    public static final int SCREEN_VERTICAL = ((MAP_SIZE+3) * 2 * AssetData.TILE_HEIGHT + MAP_PEAK * AssetData.TILE_DEPTH);
    /**
     * The computed width in pixels of a zoomed-in complete map at its largest possible {@link #MAP_SIZE}.
     * This is used for the larger-pixel framebuffer "canvas" that gets shrunk down to fit the user's chosen size.
     */
    public static int ZOOMED_HORIZONTAL = SCREEN_HORIZONTAL * (ZOOM);
    /**
     * The computed height in pixels of a zoomed-in complete map at its largest possible {@link #MAP_SIZE} and
     * {@link #MAP_PEAK}.
     * This is used for the larger-pixel framebuffer "canvas" that gets shrunk down to fit the user's chosen size.
     */
    public static int ZOOMED_VERTICAL = SCREEN_VERTICAL * (ZOOM);
    /**
     * Added to the screen y-position of sprites to account for the buttons and touchpad below.
     */
    public static int OFFSET_Y = 64;
    /**
     * The position in fractional tiles of the very center of the map, measured from bottom center.
     */
    public float mapCenter = (MAP_SIZE - 1f) * 0.5f;
    /**
     * In milliseconds, the time since the map was generated or regenerated.
     */
    public long startTime;
    /**
     * In milliseconds, the time at which any multi-frame animation started (usually a map rotation).
     */
    public long animationStart = -1000000L;

    /**
     * For browser-based targets, audio isn't allowed to play until the user has "interacted" with the application.
     * On any other platform, this is set to true in create(), but GWT and TeaVM set it to true once any touch/click
     * input has happened.
     */
    public boolean playMusic = false;

    /**
     * A temporary Vector3 used to store either pixel or world positions being projected or unprojected in 3D.
     */
    private static final Vector3 projectionTempVector = new Vector3();
    /**
     * A temporary Vector3 used to store tile positions (which are world positions on the diagonal grid) in 3D.
     */
    private static final Vector3 isoTempVector = new Vector3();
    /**
     * A temporary Vector2 used to store screen positions in pixels.
     */
    private static final Vector2 screenTempVector = new Vector2();
    /**
     * A temporary Vector4 used to store positions in {@link LocalMap#everything}, which stores every object and Mover
     * so they can be sorted correctly and then displayed in that order. This Vector4 is commonly set to some values
     * that should be checked if they exist in "everything", such as with {@link OrderedMap#containsKey(Object)}.
     * The x, y, and z coordinates correspond directly to a tile's isometric f, g, and h coordinates, while the
     * Vector4's w coordinate corresponds to the depth modifier for any sprite at that f,g,h position. A position with
     * the same x,y,z position (or f,g,h) but a different w (or depth modifier) is treated as different in "everything"
     * and this allows more than one sprite to share a position. This is how the outlines on terrain are handled, and
     * how goldfish can briefly occupy the same area as the player or an enemy.
     */
    private static final Vector4 tempVector4 = new Vector4();

    /**
     * Used to depth-sort isometric points, including if the map is mid-rotation. This gets the center of the LocalMap
     * directly from its size, and permits {@link LocalMap#rotationDegrees} to be any finite value in degrees.
     * It uses the map's pre-calculated {@link LocalMap#cosRotation} and sinRotation instead of needing to repeatedly
     * call cos() and sin(). The isometric points here are Vector4, but for the most part, only the x, y,
     * and z components are used. The fourth component, w, is only used to create another point at the same x, y, z
     * location but with a different depth. The depth change is currently used to draw outlines behind terrain tiles,
     * but have them be overdrawn by other terrain tiles if nearby. The outlines only appear if there is empty space
     * behind a terrain tile.
     */
    public final Comparator<? super Vector4> comparator =
        (a, b) -> Float.compare(
            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w,
            IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + b.w);

    // You can use this block of code instead; it may perform better if framerate is an issue in practice, but it isn't
    // quite as clear to read. Internally, this uses {@link NumberUtils#floatToIntBits(float)} instead of
    // {@link Float#compare(float, float)} because it still returns a completely valid comparison value (it only
    // distinguishes between an int that is positive, negative, or zero), and seems a tiny bit faster. To avoid
    // {@code -0.0f} being treated as a negative comparison value, this adds {@code 0.0f} to the difference of the two
    // compared depths. This is absolutely a magic trick, and the whole thing is probably unnecessary and gratuitous!
//    public final Comparator<? super Vector4> comparator =
//        (a, b) -> NumberUtils.floatToIntBits(
//            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w -
//                IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) - b.w + 0.0f);

    @Override
    public void create() {
        // You can use LOG_INFO in development, but change this to LOG_ERROR or LOG_NONE when releasing anything.
//        Gdx.app.setLogLevel(Application.LOG_INFO);
        Gdx.app.setLogLevel(Application.LOG_ERROR);

        random = new Xoshiro160RoadroxoRandom(1, 2, 3, 4, 5);

        // We can't start playing music immediately on GWT or TeaVM backends.
        if(Gdx.app.getType() != Application.ApplicationType.WebGL)
            playMusic = true;
        // Create and play looping background music.
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Komiku - Road 4 Fight.ogg"));
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.setLooping(true);
        // If we are on a web platform, we have to delay playing any sounds or music until the user interacts.
        // Music can also start playing in handleInput() if targeting a web platform.
        if(playMusic)
            backgroundMusic.play();

        // We draw everything as 2D graphics with a carefully determined sort order.
        batch = new SpriteBatch();

        // Loads the atlas from an internal path, in "assets/".
        atlas = new TextureAtlas(ATLAS_FILE_NAME);

        // Creates a Stage using a ScreenViewport and handling its own Batch.
        stage = new Stage(new ScreenViewport());

        // If the text is too small to see, you can change the units per pixel to 0.5f, 0.25f, or something else small.
//        ((ScreenViewport)stage.getViewport()).setUnitsPerPixel(1f/2f); // 1f/2f will double the size of the Stage.

        // Uses the same assets and a libGDX Skin JSON file to tell scene2d.ui widgets how to draw themselves.
        // See <a href="https://github.com/raeleus/skin-composer/wiki/From-the-Ground-Up:-Scene2D.UI-Tutorials">some scene2d.ui docs</a>
        // for more information on how to use a Skin.
        Skin skin = new Skin(Gdx.files.internal("PandoraTheBoxer.json"), atlas);
        // scene2d.ui uses a Table-based layout system. Here, root is our outermost table, and others are nested.
        Table root = new Table(skin);
        // The touchpad allows movement on mobile devices without a keyboard.
        touchpad = new Touchpad(8f, skin);
        // This stores the TextButtons by the keyboard keys that they are mapped to on desktop.
        keyButtons = new IntMap<>(16);

        // These three Labels show at the top, and provide info about your progress in the game and health.
        goalLabel = new Label("", skin);
        healthLabel = new Label("[SCARLET]<3<3<3", skin);
        fpsLabel = new Label("[#ddddddff]0 FPS", skin);

        root.add(healthLabel);
        root.add(goalLabel);
        root.add(fpsLabel).row();
        // This makes a large gap between the Labels on top and the thicker widget row below.
        root.add(new Table(skin)).growY().row();
        // This starts the widget row with the touchpad on the left.
        root.add(touchpad);
        // This adds as much space as possible between the touchpad and the buttons.
        root.add(new Table(skin)).growX();
        // We use another Table for the buttons, because the touchpad is taller than any one button.
        Table buttonTable = new Table(skin);
        buttonTable.pad(2f);

        // The ClickListeners define what happens when you click each TextButton.

        // Resets the game and generates a new map. The player will have full health and all goldfish will be present.
        TextButton reset = new TextButton("RESET", skin);
        reset.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reset();
            }
        });
        // Closes the game on desktop. Doesn't do anything on the web.
        TextButton exit = new TextButton("EXIT", skin);
        exit.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        // Makes the map and sprites bigger.
        TextButton zoomIn = new TextButton("Zoom In", skin);
        zoomIn.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                camera.zoom *= .5f;
            }
        });
        // Makes the map and sprites smaller.
        TextButton zoomOut = new TextButton("Zoom Out", skin);
        zoomOut.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                camera.zoom *= 2f;
            }
        });
        // Spin the map and its contents counterclockwise.
        TextButton rotateLeft = new TextButton("<-Rotate", skin);
        rotateLeft.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                rotateCamera(1);
            }
        });
        // Spin the map and its contents clockwise.
        TextButton rotateRight = new TextButton("Rotate->", skin);
        rotateRight.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                rotateCamera(-1);
            }
        });
        // The Jump button can be held down to bounce continually, and it's handled like the keys that can be held.
        TextButton jump = new TextButton("Jump", skin);
        keyButtons.put(Input.Keys.Z, reset);
        keyButtons.put(Input.Keys.ESCAPE, exit);
        keyButtons.put(Input.Keys.I, zoomIn);
        keyButtons.put(Input.Keys.O, zoomOut);
        keyButtons.put(Input.Keys.LEFT_BRACKET, rotateLeft);
        keyButtons.put(Input.Keys.RIGHT_BRACKET, rotateRight);
        keyButtons.put(Input.Keys.SPACE, jump);
        buttonTable.add(reset).width(90f);
        buttonTable.add(exit).width(90f).row();
        buttonTable.add(zoomIn).width(90);
        buttonTable.add(zoomOut).width(90).row();
        buttonTable.add(rotateLeft).width(90);
        buttonTable.add(rotateRight).width(90).row();
        // The jump key is bigger, since players need it often.
        // It spans the two columns, making it extra-wide.
        buttonTable.add(jump).width(190).colspan(2);
        // The rightmost part of the thicker widget row holds the buttons.
        root.add(buttonTable).width(200);
        // The root Table should resize as the window does.
        root.setFillParent(true);
        // The root Table should be laid out by doing this.
        root.pack();
        // The stage needs to have the root Table added so it can render it.
        stage.addActor(root);
        // This allows the stage to actually respond to button clicks and drags on the touchpad.
        Gdx.input.setInputProcessor(stage);

        // All regions in the atlas for creatures start with "entity" and have an index.
        Array<TextureAtlas.AtlasRegion> entities = atlas.findRegions("entity");
        // Extract animations from the atlas.
        // This step will be different for every game's assets.
        animations = new Array<>(4);
        // We create a single ArraySupplier with a constructor reference so Android won't create four different ones.
        ArraySupplier<Animation<AtlasSprite>[]> animationSupplier = Animation[]::new;
        ArraySupplier<AtlasSprite[]> spriteSupplier = AtlasSprite[]::new;
        // Apologies for the duplicated lines; these use libGDX 1.13.5 and later's way of initializing an Array.
        animations.add(new Array<>(true, 16, animationSupplier));
        animations.add(new Array<>(true, 16, animationSupplier));
        animations.add(new Array<>(true, 16, animationSupplier));
        animations.add(new Array<>(true, 16, animationSupplier));

        for (String name : AssetData.entityNames) {
            /* Index 0 is F-facing animations. */
            /* Index 1 is G-facing animations. */
            /* Index 2 is T-facing animations. */
            /* Index 3 is R-facing animations. */
            animations.get(0).add(new Animation<>(0.1f, createSprites(name + "_angle0", spriteSupplier), Animation.PlayMode.LOOP_PINGPONG));
            animations.get(1).add(new Animation<>(0.1f, createSprites(name + "_angle1", spriteSupplier), Animation.PlayMode.LOOP_PINGPONG));
            animations.get(2).add(new Animation<>(0.1f, createSprites(name + "_angle2", spriteSupplier), Animation.PlayMode.LOOP_PINGPONG));
            animations.get(3).add(new Animation<>(0.1f, createSprites(name + "_angle3", spriteSupplier), Animation.PlayMode.LOOP_PINGPONG));
        }

        // Initialize a Camera with the width and height of the area to be shown.
        camera = new OrthographicCamera(ZOOMED_HORIZONTAL, ZOOMED_VERTICAL);
        // Center the camera in the middle of the map.
        camera.position.set(AssetData.TILE_WIDTH, SCREEN_VERTICAL * 0.5f, 0f);
        // Updating the camera allows the changes we made to actually take effect.
        camera.update();
        // ScreenViewport is not always a great choice, but here we want only pixel-perfect zooms, and it can do that.
        viewport = new ScreenViewport(camera);
        // This makes the pixel-perfect view potentially much larger, but always scales by an integer multiple.
        viewport.setUnitsPerPixel(INVERSE_ZOOM);
        // This FitViewport scales the world up to fit the screen, adding empty space as needed at the edges.
        growingViewport = new FitViewport(ZOOMED_HORIZONTAL, ZOOMED_VERTICAL);
        // The FrameBuffer allows us to draw off-screen to a pixel-perfect "canvas" and scale it to any size later.
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, ZOOMED_HORIZONTAL, ZOOMED_VERTICAL, false, false);

        // Calling regenerate() does the procedural map generation, and chooses a random player character.
        regenerate(
            /* The seed will change after just over one hour, and will stay the same for over an hour. */
            TimeUtils.millis() >>> 22);

        // The goal label text changes when updateFish() or updateHealth() is called.
        updateFish();
        // The health label shows red hearts (using BitmapFont markup to make them red) for your current health.
        // It shows " :( " if the player reaches 0 health, using darker red.
        updateHealth();

        // These enforce the FPS cap and VSync settings from the first frame rendered.
        // Pressing 'C' will toggle the frame rate cap on or off.
        Gdx.graphics.setForegroundFPS(cap);
        Gdx.graphics.setVSync(cap != 0);
    }

    private Array<AtlasSprite> createSprites(String s, ArraySupplier<AtlasSprite[]> supplier) {
        Array<AtlasSprite> array = new Array<>(true, 4, supplier);
        Array<TextureAtlas.AtlasRegion> rs = atlas.findRegions(s);
        for (int i = 0; i < rs.size; i++) {
            array.add(new AtlasSprite(rs.get(i)));
        }
        return array;
    }

    /**
     * Re-creates the map with the given seed for procedurally-generating the map and its inhabitants.
     * @param seed may be any long
     */
    public void regenerate(long seed) {
        random.setSeed(seed);
        // Needed so the PC Mover always has id 1.
        Mover.ID_COUNTER = 1;
        startTime = TimeUtils.millis();
        map = LocalMap.generateMap(
            random,
            /* Used for both dimensions of the ground plane. */
            MAP_SIZE + ((int)seed & 3),
            /* Used for the number of layers of the map, in elevation. */
            MAP_PEAK,
            /* All terrain tiles in the tileset. */
            atlas);
        map.totalFish = FISH_COUNT;
        map.fishSaved = 0;
        Coord[] boxPositions = map.placeBoxes(random, map.totalFish, animations);
        mapCenter = (map.getFSize() - 1f) * 0.5f;
        // Random initial position for the player.
        Region floorCopy = new Region(map.floors);
        floorCopy.removeSeveral(boxPositions);
        Coord choice = floorCopy.singleRandom(random);
        int rf = choice.x, rg = choice.y;
        floorCopy.remove(choice);
        // Pandora is a boxer! She has big boxing gloves and wears a toga.
        int id = AssetData.ENTITIES.indexOf("Pandora");
        player = new Mover(map, animations, id, rf, rg, 2f);
        player.place(Mover.PLAYER_W);
        map.movers.entities.add(player);

//        // The player axis sprites show a crosshair-like indicator of where the player character is on the x and y axes.
//        playerAxisX = atlas.createSprite("highlight", 4);
//        playerAxisY = atlas.createSprite("highlight", 4);
//        // We try to place the x-axis indicator on the horizontal line just above the buttons.
//        playerAxisX.setY(SCREEN_VERTICAL - 4f);
//        // We try to place the y-axis indicator on the very edge of the left of the screen.
//        playerAxisY.setX(SCREEN_HORIZONTAL * -0.5f);
        // id will always be a floating angry head.
        id = AssetData.ENTITIES.indexOf("Eye_Tyrant");

        enemies = new Array<>(ENEMY_COUNT);

        for (int i = 0; i < ENEMY_COUNT; i++) {
            // enemies can be anywhere except the very edges of the map.
            choice = floorCopy.singleRandom(random);
            rf = choice.x;
            rg = choice.y;
            floorCopy.remove(choice);
            Mover enemy = new Mover(map, animations, id, rf, rg, 2);
            // We track enemies here as well as tracking them as general Movers in the map so that we can handle the
            // semi-random movement of enemies when we update them in Main, without semi-randomly moving the player.
            enemies.add(enemy);
            enemy.place(Mover.NPC_W);
            map.movers.entities.add(enemy);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        // handleInput() is where all keyboard input is handled. Mouse input isn't really handled right now, except to
        // add or remove blocks as a bit of debugging play.
        handleInput(delta);
        // Calling update() on a Mover makes them do all their logic for an unpaused game.
        player.update(delta);
        for(Mover e : enemies) e.update(delta);

        // If the player is dead, show them as a translucent ghost.
        if(player.health <= 0) player.visual.sprite.setAlpha(0.5f);

        // This bit of code gets a little complex to handle rotating the map...
        // But rotating the map is so cool! You can do it by pressing '[' or ']', or
        // by clicking a Rotate button.
        float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        // Rotations stop on a 90-degree angle increment, stored as an int from 0 to 3.
        int prevRotationIndex = (int)((map.rotationDegrees + 45f) * (1f / 90f)) & 3;

        // A rotation completes in half a second, which is quick enough to conceal some of the roughness during parts
        // of the animation.
        map.setRotationDegrees(MathUtils.lerpAngleDeg(map.previousRotation, map.targetRotation,
            Math.min(TimeUtils.timeSinceMillis(animationStart) * 0.002f, 1f)));

        // We sort the "everything" OrderedMap here using our custom comparator.
        final Array<Vector4> order = map.everything.orderedKeys();
        order.sort(comparator);

        // Our current rotation index, in 90-degree increments, so from 0 to 3.
        int rotationIndex = (int)((map.rotationDegrees + 45f) * (1f / 90f)) & 3;
        if(prevRotationIndex != rotationIndex) {
            for (int i = 0, n = order.size; i < n; i++) {
                Vector4 pt = order.get(i);
                if(pt.w != 0f) continue; // 0f is used for terrain, higher values for creatures, lower for outlines.
                setVisual(map.everything.get(pt).sprite, map.tileset.get(AssetData.rotateIndex(map.getTile(pt), rotationIndex)));
            }
        }

        // When the rotation has finished, we set the previous rotation to what we just ended on.
        if(MathUtils.isEqual(map.rotationDegrees, map.targetRotation))
            map.previousRotation = map.targetRotation;

        // When we begin the FrameBuffer, anything we draw won't go to the screen, but an "off-screen canvas".
        buffer.begin();
        viewport.update(ZOOMED_HORIZONTAL, ZOOMED_VERTICAL, false);
        // Very dark blue for the background color.
        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        // Vital to get things to display. I don't actually know what the "combined" matrix is here.
        batch.setProjectionMatrix(camera.combined);
        // We need to apply() the viewport here in case it changed for any reason, such as from key inputs.
        viewport.apply();
        batch.begin();
        for (int i = 0, n = order.size; i < n; i++) {
            Vector4 pos = order.get(i);
            // Updates each voxel in "everything" and then draws it with the parameters needed for rotation.
            map.everything.get(pos).update(time).draw(batch, (map.getFSize() - 1) * 0.5f, (map.getGSize() - 1) * 0.5f, map.cosRotation, map.sinRotation);
        }

        Vector3 pos = player.getPosition();
        // Makes tempVector4 store the position we want to check: the players's location, rounded, at the fish depth.
        // If there is anything at that position, it is a fish the player is touching, and so has rescued.
        map.setToFishPosition(tempVector4, pos.x, pos.y, pos.z);
        IsoSprite fish = map.everything.get(tempVector4);
        if(fish instanceof AnimatedIsoSprite && fish != player.visual){
            ++map.fishSaved;
            map.everything.remove(tempVector4);
            ((Main)Gdx.app.getApplicationListener()).updateFish();
        }
//        playerAxisX.setX(player.visual.getSprite().getX());
//        playerAxisY.setY(player.visual.getSprite().getY());
//        playerAxisX.draw(batch);
//        playerAxisY.draw(batch);

        // When we end the batch, everything scheduled to draw so far actually gets drawn.
        batch.end();
        // When we end the buffer, everything that has been drawn to the pixel-perfect screen is now available.
        buffer.end();
        // Same background color, very dark blue.
        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        // Here we use the different, non-pixel-perfect viewport and its projection.
        batch.setProjectionMatrix(growingViewport.getCamera().combined);
        // We still need to apply the viewport, but here we pass true to center the camera.
        growingViewport.apply(true);
        // This gets the "off-screen canvas" we drew the pixel-perfect texture to, and assigns it to fb.
        Texture fb = buffer.getColorBufferTexture();
        // Causes a small amount of blur, but only at the edges between pixels.
        // Each pixel is already drawn significantly larger, based on ZOOM, so only part of each will blur.
        fb.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        // This batch will only draw one Texture, which is the screen we captured as a framebuffer.
        batch.begin();
        // Because the framebuffer is vertically flipped, we need to draw it with negative height, and offset above.
        batch.draw(fb, 0, fb.getHeight(), fb.getWidth(), -fb.getHeight());
        batch.end();

        fpsLabel.getText().clear();
        fpsLabel.getText().append("[#ddddddff]").append(Gdx.graphics.getFramesPerSecond()).append(" FPS");
        // Allows the FPS label to be drawn with the correct width.
        fpsLabel.invalidate();

        // Without apply(true) on the Stage's Viewport, the Stage won't resize correctly when the window resizes.
        stage.getViewport().apply(true);
        // This is important to allow widgets to respond to input.
        stage.act(delta);
        // This draws the Stage and its contents completely separately from the rest of the gameplay sprites.
        stage.draw();
    }

    private static void setVisual(AtlasSprite sprite, TextureAtlas.AtlasRegion region) {
        TextureAtlas.AtlasRegion r = sprite.getAtlasRegion();
        sprite.originalOffsetX = region.offsetX;
        sprite.originalOffsetY = region.offsetY;
        sprite.setRegion(region);
        sprite.setOrigin(region.originalWidth / 2f, region.originalHeight / 2f);
        int width = region.getRegionWidth();
        int height = region.getRegionHeight();
        if (region.rotate) {
            sprite.rotate90(true);
            sprite.setSuperBounds(region.offsetX, region.offsetY, height, width);
        } else
            sprite.setSuperBounds(region.offsetX, region.offsetY, width, height);
    }

    /**
     * Only handles movement input for the player character. This is called from the main input handling in
     * {@link #handleInput(float)}.
     */
    private void handleInputPlayer() {
        // Our difference (delta) on the f and g isometric axes.
        float df = 0, dg = 0;
        // If the touchpad is being touched, ignore keyboard input and use its knob to get the direction change.
        if(touchpad.isTouched()){
            // We need to rotate the knob percents, which are orthogonal, by -45 degrees to get diagonal directions.
            screenTempVector.set(touchpad.getKnobPercentX(), touchpad.getKnobPercentY()).rotateDeg(-45);
            df = screenTempVector.x;
            dg = screenTempVector.y;
        } else {
            // We allow f,g,t,r to move on one isometric axis, or the numpad to move in all 8 directions.
            // You can also hold a pair of f,g,t,r (adjacent on a QWERTY keyboard) to move north, south, east, or west.
                 if (Gdx.input.isKeyPressed(Input.Keys.F) && Gdx.input.isKeyPressed(Input.Keys.G)) { df = -INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.F) && Gdx.input.isKeyPressed(Input.Keys.R)) { df = -INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.G) && Gdx.input.isKeyPressed(Input.Keys.T)) { df = INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.T) && Gdx.input.isKeyPressed(Input.Keys.R)) { df = INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.F) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_1)) df = -1;
            else if (Gdx.input.isKeyPressed(Input.Keys.G) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_3)) dg = -1;
            else if (Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_9)) df = 1;
            else if (Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_7)) dg = 1;
            else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_2)) { df = -INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_4)) { df = -INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_6)) { df = INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
            else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_8)) { df = INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
        }

        // We account for the map's rotation so the visual rotation of the map (for the player) also affects the
        // direction in tiles for their chosen direction as they perceive it.
        float c = map.cosRotation;
        float s = map.sinRotation;
        float rf = c * df + s * dg;
        float rg = c * dg - s * df;

        player.move(rf, rg, Mover.PC_MOVE_SPEED);

        // Whee! Clicking "Jump" or pressing Space/Numpad 0/Numpad 5 make the player character jump really high.
        if (keyButtons.get(Input.Keys.SPACE).isPressed()
         || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
         || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)
         || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) {
            player.jump();
        }
    }

    /**
     * Input for the game itself, though this also calls {@link #handleInputPlayer()}.
     * @param delta the amount of time in seconds since the last unpaused render
     */
    private void handleInput(float delta) {
        // Get out of here!
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }
        // Zero-state, or a brand-new map and fresh healthy character, also with 10 enemies and 10 goldfish.
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            reset();
            return;
        }
        // cap for frame rate
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            // If cap is at the limit, set it to 0; if cap is 0, set it to the limit. This toggles cap.
            cap ^= FRAME_RATE_LIMIT;
            // If cap is 0, there is no limit on FPS unless the user's drivers force VSync on.
            Gdx.graphics.setForegroundFPS(cap);
            // If the cap is disabled (at 0), then we try to turn VSync off to enable more FPS than can be seen.
            Gdx.graphics.setVSync(cap != 0);
            return;
        }
        handleInputPlayer();

        // The arrow keys move the camera, which isn't really necessary here.
        // You can comment out the next 4 lines if you want.
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) camera.translate(0, 200 * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) camera.translate(0, -200 * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) camera.translate(-200 * delta, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.translate(200 * delta, 0);
        // I and O zoom in and out, respectively.
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) camera.zoom *= .5f; // In
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) camera.zoom *= 2f; // Out

        // The square bracket keys handle rotation, which can be important to spot goldfish behind terrain.
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            rotateCamera(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            rotateCamera(-1);
        }
        // Because the arrow keys, as well as I and O, can change the camera, we update it here.
        camera.update();

        // If playMusic is false, we must be on a browser target (GWT or TeaVM), and we are delaying the music startup
        // until the user has "interacted" with the game. Once they click or touch, we can start playing a song.
        if(!playMusic && Gdx.input.justTouched()) {
            playMusic = true;
            backgroundMusic.play();
        }
    }

    /**
     * Rotates the view as if the camera moved around the map by 90 degrees, over several frames.
     * This actually rotates the map itself, not the camera, which is still fully 2D.
     * Individual voxels don't get moved; here, the map rotation is what determines where voxels are placed and how
     * they get sorted, but the rotation is just one float to change.
     *
     * @param amount Almost always either 1 for a counterclockwise rotation or -1 for a clockwise rotation.
     */
    public void rotateCamera(int amount) {
        map.previousRotation = map.rotationDegrees;
        map.targetRotation = (MathUtils.round(map.rotationDegrees * (1f/90f)) + amount & 3) * 90;
        animationStart = TimeUtils.millis();
    }

    /**
     * Resets the map to a procedurally-generated new map, and updates Labels showing gameplay progress.
     */
    private void reset() {
        regenerate(MathUtils.random.nextLong());
        updateFish();
        updateHealth();
    }

    /**
     * Not actually used here; this doesn't use Screen or Game, so when this object is disposed, the game is over.
     */
    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        backgroundMusic.stop();
        backgroundMusic.dispose();
    }

    /**
     * This gets called when the game window is first created and also whenever the window changes size. It is
     * important to update viewports here if you want them to adapt to the window size.
     *
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    @Override
    public void resize(int width, int height) {
        // In older code, we wanted pixel-perfect zooms only, but now we can make noninteger zooms look decent.

        // The commented block can be used for pixel-perfect zooms without growingViewport.

        // If unitsPerPixel are a fraction like 1f/2 or 1f/3, then that makes each pixel 2x or 3x the size, resp.
        // This will only divide 1f by an integer amount 1 or greater, which makes pixels always the exact right size.
        // This meant to fit an isometric map that is about MAP_SIZE by MAP_PEAK by MAP_SIZE, where MAP_PEAK is how many
        // layers of voxels can be stacked on top of each other.
//        viewport.setUnitsPerPixel(1f / Math.max(1, (int) Math.min(
//            width  / ((MAP_SIZE+1f) * (AssetData.TILE_WIDTH * 2f)),
//            height / ((MAP_SIZE+1f) * (AssetData.TILE_HEIGHT * 2f) + AssetData.TILE_DEPTH * MAP_PEAK))));
//        viewport.update(width, height);

        // Or, we can use growingViewport and not need integer scales at all!
        growingViewport.update(width, height);
        // The Stage is drawn separately, and has its own viewport that needs updating.
        stage.getViewport().update(width, height);
    }

    /**
     * Updates the goal Label to show the appropriate number of goldfish you need to save, or if you have won.
     */
    public void updateFish() {
        if(player.health > 0) {
            if (map.totalFish == map.fishSaved) {
                // We use libGDX color markup to make any text some color other than blindingly-bright white.
                goalLabel.setText("[ORANGE]YOU SAVED THEM ALL! Great job!");
                player.makeInvincible(Float.NaN);
            } else {
                // You can use either named colors or the hex RRGGBBAA color format.
                goalLabel.setText("[#ddddddff]SAVE THE GOLDFISH!!! " + (map.totalFish - map.fishSaved) + " still " +
                    ((map.totalFish - map.fishSaved) == 1 ? "needs" : "need") + " your help!");
            }
        }
        goalLabel.setAlignment(Align.center);
    }

    /**
     * Updates the health and sometimes goal Labels to show your current health and if you have died.
     */
    public void updateHealth() {
        if(player.health <= 0)
        {
            // This text is a slightly darker gray, using RRGGBBAA hex color markup.
            goalLabel.setText("[#bbbbbbff]YOU FAILED... BY DYING...");
            goalLabel.setAlignment(Align.center);
            // This text is a darker red frowning face.
            healthLabel.setText("[FIREBRICK]:(");
        }
        else {
            healthLabel.getText().clear();
            // Shows one red heart per point of health.
            healthLabel.getText().append("[SCARLET]");
            for (int i = 0; i < player.health; i++) {
                healthLabel.getText().append("<3");
            }
            healthLabel.invalidate();
        }
    }
}
