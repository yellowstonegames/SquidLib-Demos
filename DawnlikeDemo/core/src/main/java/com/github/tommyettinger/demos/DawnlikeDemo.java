package com.github.tommyettinger.demos;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Scaling;
import squidpony.ArrayTools;
import squidpony.FakeLanguageGen;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Measurement;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.*;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.Input.Keys.*;

/**
 */
public class DawnlikeDemo extends ApplicationAdapter {
    private enum Phase {WAIT, PLAYER_ANIM, MONSTER_ANIM}
    private SpriteBatch batch;
    private Phase phase = Phase.WAIT;

    // random number generator, optimized for when you build for the web browser (with GWT)
    private GWTRNG rng;
    
    // Stores all images we use here efficiently, as well as the font image 
    private TextureAtlas atlas;
    // This maps chars, such as '#', to specific images, such as a pillar.
    private IntMap<TextureAtlas.AtlasRegion> charMapping;
    
    private DungeonGenerator dungeonGen;
    private char[][] decoDungeon, bareDungeon, lineDungeon;
    // these use packed float colors, which avoid the overhead of creating new Color objects
    private float[][] colors, bgColors;
    private Coord player;
    private final int fovRange = 8;
    private Vector3 pos = new Vector3();

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
    public static final int gridWidth = 48;
    /** In number of cells */
    public static final int gridHeight = 32;

    /** In number of cells */
    public static final int bigWidth = gridWidth * 2;
    /** In number of cells */
    public static final int bigHeight = gridHeight * 2;

//    /** In number of cells */
//    public static final int bonusHeight = 0;
    /** The pixel width of a cell */
    public static final int cellWidth = 16;
    /** The pixel height of a cell */
    public static final int cellHeight = 16;
    
    private boolean onGrid(int screenX, int screenY)
    {
        return screenX >= 0 && screenX < bigWidth && screenY >= 0 && screenY < bigHeight;
    }


    private InputProcessor input;
    private long lastDrawTime = 0;
    private Color bgColor;
    private BitmapFont font;
    private PixelPerfectViewport mainViewport;
    private Camera camera;
    
    private OrderedMap<Coord, Sprite> monsters;
    private DijkstraMap getToPlayer, playerToCursor;
    private Coord cursor;
    private List<Coord> toCursor;
    private List<Coord> awaitedMoves;
    private String lang;
    private double[][] resistance;
    private double[][] visible;
    private TextureAtlas.AtlasRegion solid;
    private int health = 9;
    
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
    private GreasedRegion floors, blockage, seen;
    // a Glyph is a kind of scene2d Actor that only holds one char in a specific color, but is drawn using the behavior
    // of TextCellFactory (which most text in SquidLib is drawn with) instead of the different and not-very-compatible
    // rules of Label, which older SquidLib code used when it needed text in an Actor. Glyphs are also lighter-weight in
    // memory usage and time taken to draw than Labels.
    private Sprite playerSprite;
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
    // The format used for the floats is a hex literal; these are explained at the bottom of this file, in case you
    // aren't familiar with them (they're a rather obscure feature of Java 5 and newer).
    private static final float
            FLOAT_WHITE = Color.WHITE.toFloatBits(), 
            FLOAT_BLACK = Color.BLACK.toFloatBits(),
            FLOAT_BLOOD = -0x1.564f86p125F,  // same result as SColor.PURE_CRIMSON.toFloatBits()
            FLOAT_LIGHTING = ColorTools.floatGetHSV(0.17f, 0.12f, 0.8f, 1f),//-0x1.cff1fep126F, // same result as SColor.COSMIC_LATTE.toFloatBits()
            FLOAT_GRAY = ColorTools.floatGetHSV(0f, 0f, 0.25f, 1f);
                    //-0x1.7e7e7ep125F; // same result as SColor.CW_GRAY_BLACK.toFloatBits()
    // the player's color as a float
    private float playerColor;
    @Override
    public void create () {
        // Gotta have a random number generator.
        // We can seed a GWTRNG, which is optimized for the HTML target, with any int or long
        // we want. You can also hash a String with CrossHash.hash64("Some seed") to get a
        // random-seeming long to use for a seed. CrossHash is preferred over String.hashCode()
        // because it can produce 64-bit seeds and String.hashCode() will only produce 32-bit
        // seeds; having more possible seeds means more maps and other procedural content
        // become possible. Here we don't seed the GWTRNG, so its seed will be random.
        rng = new GWTRNG();

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        mainViewport = new PixelPerfectViewport(Scaling.fill, gridWidth * cellWidth, gridHeight * cellHeight);
        mainViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        camera = mainViewport.getCamera();
        camera.update();

        atlas = new TextureAtlas("Dawnlike.atlas");
        font = new BitmapFont(Gdx.files.internal("font.fnt"), atlas.findRegion("font"));
        font.setUseIntegerPositions(false);
        font.getData().markupEnabled = true;
        bgColors = ArrayTools.fill(FLOAT_BLACK, bigWidth, bigHeight);
        colors = ArrayTools.fill(FLOAT_WHITE, bigWidth, bigHeight);
        solid = atlas.findRegion("pixel");
        String[] possibleCharacters = {
                "ordinary human",
                "fighter",
                "ranger",
                "bandit",
                "valkyrie",
                "slayer",
                "wizard",
                "monk",
                "priest",
                "aide",
                "samurai",
                "collector",
                "brute",
                "nomad",
                "tourist",
                "convict",
                "man",
                "knight",
                "yeoman",
                "thief",
                "champion",
                "exterminator",
                "archmage",
                "sensei",
                "bishop",
                "healer",
                "ninja",
                "archaeologist",
                "barbarian",
                "caveman",
                "sightseer",
                "desperado",
                "ordinary woman",
                "priestess",
                "cavewoman",
        }, possibleEnemies = {
                "frog",
                "baby plains chickabay",
                "plains chickabay",
                "baby fen chickabay",
                "fen chickabay",
                "baby taiga chickabay",
                "taiga chickabay",
                "baby doom chickabay",
                "doom chickabay",
                "baby nighthawk",
                "nighthawk",
                "baby cave swift",
                "cave swift",
                "baby arctic tern",
                "arctic tern",
                "baby robin",
                "robin",
                "baby osprey",
                "osprey",
                "baby phoenix",
                "phoenix",
                "baby aluminum falcon",
                "aluminum falcon",
                "baby cave penguin",
                "cave penguin",
                "baby penguin",
                "penguin",
                "baby pelican",
                "pelican",
                "baby duck",
                "duck",
                "baby puffin",
                "puffin",
                "baby swan",
                "swan",
                "baby albatross",
                "albatross",
                "baby condor",
                "condor",
                "baby green parrot",
                "green parrot",
                "baby dove",
                "dove",
                "baby parakeet",
                "parakeet",
                "baby thrush",
                "thrush",
                "baby cormorant",
                "cormorant",
                "baby seagull",
                "seagull",
                "baby bald eagle",
                "bald eagle",
                "golden eagle",
                "baby golden eagle",
                "baby ostrich",
                "ostrich",
                "baby emu",
                "emu",
                "baby kiwi",
                "kiwi",
                "chick",
                "hen",
                "rooster",
                "baby peacock",
                "peahen",
                "peacock",
                "baby griffon",
                "griffon",
                "baby snow griffon",
                "snow griffon",
                "baby barn owl",
                "barn owl",
                "baby snowy owl",
                "snowy owl",
                "white nosed bat",
                "giant bat",
                "vampire bat",
                "werebat",
                "baby bat",
                "baby gray parrot",
                "gray parrot",
                "bobcat",
                "leopard",
                "panther",
                "tiger",
                "lion",
                "puma kitten",
                "puma",
                "fierce puma",
                "lynx kitten",
                "lynx",
                "fierce lynx",
                "barn kitten",
                "barn cat",
                "fierce barn cat",
                "weretiger",
                "werecat",
                "barbed devil",
                "marilith",
                "vrock",
                "lava demon",
                "juiblex",
                "yeenoghu",
                "orcus",
                "geryon",
                "sandestin",
                "lesser devil",
                "nalfeshnee",
                "pit fiend",
                "horned devil",
                "minion of huhetotl",
                "water demon",
                "mail daemon",
                "asmodeus",
                "baalzebub",
                "dispater",
                "erinys",
                "demogorgon",
                "cthulhu",
                "balrog",
                "bone devil",
                "ice devil",
                "fire devil",
                "homunculus",
                "manes",
                "lemure",
                "imp",
                "quasit",
                "succubus",
                "incubus",
                "derro slaver",
                "derro brute",
                "nalfeshnee",
                "hezrou",
                "jackal",
                "fox",
                "dingo",
                "coyote",
                "rabid jackal",
                "werejackal",
                "wolf",
                "warg",
                "baby winter wolf",
                "winter wolf",
                "rabid wolf",
                "werewolf",
                "little hound",
                "hound",
                "big hound",
                "little cogdog",
                "cogdog",
                "big cogdog",
                "little terrier",
                "terrier",
                "big terrier",
                "heck puppy",
                "hell hound",
                "cerberus",
                "straw golem",
                "paper golem",
                "wax golem",
                "rope golem",
                "gold golem",
                "leather golem",
                "wood golem",
                "flesh golem",
                "clay golem",
                "stone golem",
                "glass golem",
                "iron golem",
                "snow golem",
                "crystal golem",
                "fog cloud",
                "dust vortex",
                "ice vortex",
                "energy vortex",
                "steam vortex",
                "fire vortex",
                "stalker",
                "air elemental",
                "fire elemental",
                "earth elemental",
                "water elemental",
                "gas spore",
                "freezing sphere",
                "flaming sphere",
                "shocking sphere",
                "floating eye",
                "evil eye",
                "eye tyrant",
                "rock piercer",
                "iron piercer",
                "glass piercer",
                "small mimic",
                "large mimic",
                "giant mimic",
                "strange object",
                "xorn",
                "invisible creature",
                "lurker above",
                "lurker below",
                "yellow light",
                "black light",
                "giant",
                "stone giant",
                "hill giant",
                "fire giant",
                "frost giant",
                "storm giant",
                "ettin",
                "titan",
                "minotaur",
                "cyclops",
                "surtur",
                "keystone kop",
                "kop sergeant",
                "kop lieutenant",
                "kop kaptain",
                "shopkeeper",
                "executioner",
                "ninja",
                "prisoner",
                "forest sprite",
                "forest fairy",
                "forest spirit",
                "mountain sprite",
                "mountain fairy",
                "mountain spirit",
                "green wings",
                "gold wings",
                "orc",
                "hill orc",
                "deep orc",
                "militant orc",
                "orc shaman",
                "orc king",
                "uruk",
                "soldier",
                "sergeant",
                "lieutenant",
                "captain",
                "watchman",
                "watch captain",
                "guard",
                "elf",
                "wood elf",
                "green elf",
                "gray elf",
                "elf lord",
                "elf king",
                "troll",
                "ice troll",
                "rock troll",
                "water troll",
                "olog hai",
                "gnome",
                "gnome lord",
                "gnome wizard",
                "gnome king",
                "leprechaun",
                "peasant man",
                "peasant woman",
                "farmer man",
                "farmer woman",
                "miner",
                "hobbit",
                "dwarf",
                "dwarf lord",
                "dwarf king",
                "wood nymph",
                "water nymph",
                "mountain nymph",
                "fire nymph",
                "quest leader a",
                "quest leader b",
                "quest leader c",
                "quest leader d",
                "satyr",
                "plains centaur",
                "forest centaur",
                "mountain centaur",
                "ogre",
                "ogre lord",
                "ogre king",
                "quantum mechanic",
                "croesus",
                "angel",
                "dark angel",
                "archon",
                "weeping angel",
                "weeping archangel",
                "tengu",
                "djinn",
                "marid",
                "dao",
                "goblin",
                "hobgoblin",
                "goblin king",
                "mind flayer",
                "master mind flayer",
                "bugbear",
                "yeti",
                "oracle",
                "nurse",
                "aligned priest",
                "high priest",
                "ocean spirit",
                "river spirit",
                "snow puff",
                "sand puff",
                "medusa",
                "rodent of unusual size",
                "ratzilla",
                "snowy owlbear",
                "owlbear",
                "werebat",
                "pegasus",
                "kirin",
                "gremlin",
                "gargoyle",
                "winged gargoyle",
                "uranium imp",
                "kobold",
                "large kobold",
                "kobold shaman",
                "kobold lord",
                "monkey",
                "ape",
                "carnivorous ape",
                "jabberwock",
                "vorpal jabberwock",
                "umber hulk",
                "water hulk",
                "big hallucination",
                "small hallucination",
                "baby terror bird",
                "terror bird",
                "zruty",
                "giant tick",
                "giant flea",
                "giant louse",
                "giant beetle",
                "larva",
                "maggot",
                "dung worm",
                "tunnel worm",
                "walking stick",
                "dragonfly",
                "bee grub",
                "queen bee",
                "tsetse fly",
                "killer bee",
                "cave spider",
                "giant spider",
                "shelob",
                "scorpion",
                "giant scorpion",
                "scorpius",
                "baby long worm",
                "long worm",
                "long worm tail",
                "baby purple worm",
                "purple worm",
                "giant ant",
                "soldier ant",
                "fire ant",
                "snow ant",
                "firefly",
                "locust",
                "assassin bug",
                "grid bug",
                "xan",
                "chillbug",
                "baby slug",
                "giant slug",
                "baby snail",
                "giant snail",
                "spark bug",
                "arc bug",
                "phase spider",
                "centipede",
                "spitting beetle",
                "killer beetle",
                "evil shrub",
                "burning evil shrub",
                "demon shrub",
                "burning demon shrub",
                "blizzard shrub",
                "withered blizzard shrub",
                "maelstrom shrub",
                "withered maelstrom shrub",
                "evil tree",
                "burning evil tree",
                "demon tree",
                "burning demon tree",
                "blizzard tree",
                "withered blizzard tree",
                "maelstrom tree",
                "withered maelstrom tree",
                "dungeon fern",
                "dungeon fern sprout",
                "dungeon fern spore",
                "arctic fern",
                "arctic fern sprout",
                "arctic fern spore",
                "blazing fern",
                "blazing fern sprout",
                "blazing fern spore",
                "swamp fern",
                "swamp fern sprout",
                "swamp fern spore",
                "devil snare",
                "mould",
                "ungenomold",
                "shrieker",
                "baby pig",
                "pig",
                "baby hog",
                "hog",
                "feral hog",
                "baby black boar",
                "black boar",
                "vicious black boar",
                "baby dairy cow",
                "dairy cow",
                "baby bull",
                "bull",
                "raging bull",
                "camel",
                "llama",
                "baby white goat",
                "white goat",
                "baby gray goat",
                "gray goat",
                "baby black goat",
                "black goat",
                "baby white sheep",
                "white sheep",
                "baby gray sheep",
                "gray sheep",
                "baby black sheep",
                "black sheep",
                "brown unicorn",
                "white unicorn",
                "gray unicorn",
                "black unicorn",
                "newborn brown horse",
                "baby brown horse",
                "brown horse",
                "strong brown horse",
                "baby gray horse",
                "gray horse",
                "strong gray horse",
                "baby black horse",
                "black horse",
                "strong black horse",
                "baby deer",
                "deer",
                "mastodon",
                "mumak",
                "titanothere",
                "baluchitherium",
                "wumpus",
                "leocrotta",
                "baby sphinx",
                "sphinx",
                "baby wyvern",
                "wyvern",
                "baby dreadwyrm",
                "dreadwyrm",
                "baby glendrake",
                "glendrake",
                "baby firedrake",
                "firedrake",
                "baby icewyrm",
                "icewyrm",
                "baby sanddrake",
                "sanddrake",
                "baby stormwyrm",
                "storrmwyrm",
                "baby darkwyrm",
                "darkwyrm",
                "baby lightwyrm",
                "lightwyrm",
                "baby bogwyrm",
                "bogwyrm",
                "baby sheenwyrm",
                "sheenwyrm",
                "baby kingwyrm",
                "kingwyrm",
                "baby searwyrm",
                "searwyrm",
                "hydra",
                "baby hydra",
                "garter snake",
                "snake",
                "python",
                "water moccasin",
                "pit viper",
                "cobra",
                "asp",
                "serpent",
                "baby red naga",
                "red naga",
                "baby black naga",
                "black naga",
                "baby golden naga",
                "golden naga",
                "baby white naga",
                "white naga",
                "baby guardian naga",
                "guardian naga",
                "baby cockatrice",
                "cockatrice",
                "baby pyrolisk",
                "pyrolisk",
                "lizard",
                "iguana",
                "chameleon",
                "baby crocodile",
                "crocodile",
                "gecko",
                "komodo dragon",
                "gila monster",
                "newt",
                "giant turtle",
                "giant tortoise",
                "salamander",
                "red squirrel",
                "gray squirrel",
                "rabbit",
                "jackrabbit",
                "woodchuck",
                "sewer rat",
                "giant rat",
                "rabid rat",
                "enormous rat",
                "disintegrator",
                "rust monster",
                "rock mole",
                "wererat",
                "kobold zombie",
                "gnome zombie",
                "orc zombie",
                "dwarf zombie",
                "elf zombie",
                "human zombie",
                "ettin zombie",
                "giant zombie",
                "kobold mummy",
                "gnome mummy",
                "orc mummy",
                "dwarf mummy",
                "elf mummy",
                "human mummy",
                "ettin mummy",
                "giant mummy",
                "skeleton",
                "fire skeleton",
                "plague skeleton",
                "shadow skeleton",
                "lich",
                "demilich",
                "master lich",
                "archlich",
                "vampire",
                "vampire lord",
                "vampire mage",
                "vlad the impaler",
                "wisp",
                "spirit",
                "ghost",
                "shade",
                "death",
                "pestilence",
                "famine",
                "wraith",
                "nazgul",
                "barrow wight",
                "charon",
                "wizard of yendor",
                "war",
                "ghoul",
                "dark one",
                "meathead",
                "small meathead",
        };
        charMapping = new IntMap<>(64);

        charMapping.put('.', atlas.findRegion("day tile floor c"));
        charMapping.put(',', atlas.findRegion("brick clear pool center"      ));
        charMapping.put('~', atlas.findRegion("brick murky pool center"      ));
        charMapping.put('"', atlas.findRegion("dusk grass floor c"      ));
        charMapping.put('#', atlas.findRegion("lit brick wall center"     ));
        charMapping.put('+', atlas.findRegion("closed wooden door front")); //front
        charMapping.put('/', atlas.findRegion("open wooden door side"  )); //side
        charMapping.put('└', atlas.findRegion("lit brick wall right down"            ));
        charMapping.put('┌', atlas.findRegion("lit brick wall right up"            ));
        charMapping.put('┬', atlas.findRegion("lit brick wall left right up"           ));
        charMapping.put('┴', atlas.findRegion("lit brick wall left right down"           ));
        charMapping.put('─', atlas.findRegion("lit brick wall left right"            ));
        charMapping.put('│', atlas.findRegion("lit brick wall up down"            ));
        charMapping.put('├', atlas.findRegion("lit brick wall right up down"           ));
        charMapping.put('┼', atlas.findRegion("lit brick wall left right up down"          ));
        charMapping.put('┤', atlas.findRegion("lit brick wall left up down"           ));
        charMapping.put('┐', atlas.findRegion("lit brick wall left up"            ));
        charMapping.put('┘', atlas.findRegion("lit brick wall left down"            ));
        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good winding dungeons
        //with rooms by default, but in the later call to dungeonGen.generate(), you can use a TilesetType such as
        //TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS or TilesetType.CAVES_LIMIT_CONNECTIVITY to change the sections that
        //this will use, or just pass in a full 2D char array produced from some other generator, such as
        //SerpentMapGenerator, OrganicMapGenerator, or DenseRoomMapGenerator.
        dungeonGen = new DungeonGenerator(bigWidth, bigHeight, rng);
        //uncomment this next line to randomly add water to the dungeon in pools.
        dungeonGen.addWater(12);
        dungeonGen.addDoors(10, true);
        dungeonGen.addGrass(10);
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

        resistance = DungeonUtility.generateSimpleResistances(decoDungeon);
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
        //in that region that is "on," or -1,-1 if there are no such cells. It takes an RNG object as a parameter, and
        //if you gave a seed to the RNG constructor, then the cell this chooses will be reliable for testing. If you
        //don't seed the RNG, any valid cell should be possible.
        player = floors.singleRandom(rng);
        playerSprite = new Sprite(atlas.findRegion(rng.getRandomElement(possibleCharacters)));
        playerColor = ColorTools.floatGetHSV(rng.nextFloat(), 1f, 1f, 1f);
        playerSprite.setPackedColor(playerColor);
        playerSprite.setPosition(player.x * cellWidth, player.y * cellHeight);
        // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
        FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);
        // 0.0 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.0 will _not_ be in
        // the blockage Collection, but anything 0.0 or less will be in it. This lets us use blockage to prevent access
        // to cells we can't see from the start of the move.
        blockage = new GreasedRegion(visible, 0.0);
        // Here we mark the initially seen cells as anything that wasn't included in the unseen "blocked" region.
        // We invert the copy's contents to prepare for a later step, which makes blockage contain only the cells that
        // are above 0.0, then copy it to save this step as the seen cells. We will modify seen later independently of
        // the blocked cells, so a copy is correct here. Most methods on GreasedRegion objects will modify the
        // GreasedRegion they are called on, which can greatly help efficiency on long chains of operations.
        seen = blockage.not().copy();
        // Here is one of those methods on a GreasedRegion; fringe8way takes a GreasedRegion (here, the set of cells
        // that are visible to the player), and modifies it to contain only cells that were not in the last step, but
        // were adjacent to a cell that was present in the last step. This can be visualized as taking the area just
        // beyond the border of a region, using 8-way adjacency here because we specified fringe8way instead of fringe.
        // We do this because it means pathfinding will only have to work with a small number of cells (the area just
        // out of sight, and no further) instead of all invisible cells when figuring out if something is currently
        // impossible to enter.
        blockage.fringe8way();
        floors.remove(player);
        int numMonsters = 50;
        monsters = new OrderedMap<>(numMonsters);
        for (int i = 0; i < numMonsters; i++) {
            Coord monPos = floors.singleRandom(rng);
            floors.remove(monPos);
            Sprite monster = new Sprite(atlas.findRegion(rng.getRandomElement(possibleEnemies)));
//            monster.setPackedColor(ColorTools.floatGetHSV(rng.nextFloat(), 0.75f, 0.8f, 0f));
            // new Color().fromHsv(rng.nextFloat(), 0.75f, 0.8f));
            monster.setPosition(monPos.x * cellWidth, monPos.y * cellHeight);
            monsters.put(monPos, monster);
        }
        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<>(200);
        //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
        awaitedMoves = new ArrayList<>(200);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
        //Measurement used is EUCLIDEAN, which allows 8 directions, but will prefer orthogonal moves unless diagonal
        //ones are clearly closer "as the crow flies." Alternatives are MANHATTAN, which means 4-way movement only, no
        //diagonals possible, and CHEBYSHEV, which allows 8 directions of movement at the same cost for all directions.
        playerToCursor = new DijkstraMap(bareDungeon, Measurement.EUCLIDEAN);
        getToPlayer = new DijkstraMap(decoDungeon, Measurement.EUCLIDEAN);
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor.setGoal(player);
        // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
        // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
        // GreasedRegion that contains the cells just past the edge of the player's FOV area.
        playerToCursor.partialScan(13, blockage);


        bgColor = Color.BLACK;


        lang = '"' + FakeLanguageGen.DEMONIC.sentence(rng, 4, 7,
                new String[]{",", ",", ",", " -"}, new String[]{"...\"", ", heh...\"", ", nyehehe...\"",  "!\"", "!\"", "!\"", "!\" *PTOOEY!*",}, 0.2);

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
        input = new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                switch (keycode)
                {
                    case UP:
                    case 'w':
                    case 'W':
                    case NUMPAD_8:
                        toCursor.clear();
                        //+1 is up on the screen
                        awaitedMoves.add(player.translate(0, 1));
                        break;
                    case DOWN:
                    case 's':
                    case 'S':
                    case NUMPAD_2:
                        toCursor.clear();
                        //-1 is down on the screen
                        awaitedMoves.add(player.translate(0, -1));
                        break;
                    case LEFT:
                    case 'a':
                    case 'A':
                    case NUMPAD_4:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, 0));
                        break;
                    case RIGHT:
                    case 'd':
                    case 'D':
                    case NUMPAD_6:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, 0));
                        break;
                    case NUMPAD_1:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, -1));
                        break;
                    case NUMPAD_3:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, -1));
                        break;
                    case NUMPAD_7:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, 1));
                        break;
                    case NUMPAD_9:
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, 1));
                        break;
                    case '.':
                    case NUMPAD_5:
                        toCursor.clear();
                        awaitedMoves.add(player);
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
                pos.set(screenX, screenY, 0f);
                mainViewport.unproject(pos);
                if (onGrid(MathUtils.floor(pos.x) >> 4, MathUtils.floor(pos.y) >> 4)) {
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
                pos.set(screenX, screenY, 0f);
                mainViewport.unproject(pos);
                if (onGrid(screenX = MathUtils.floor(pos.x) >> 4, screenY = MathUtils.floor(pos.y) >> 4)) {
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
                    toCursor = playerToCursor.findPathPreScanned(cursor);
                    // findPathPreScanned includes the current cell (goal) by default, which is helpful when
                    // you're finding a path to a monster or loot, and want to bump into it, but here can be
                    // confusing because you would "move into yourself" as your first move without this.
                    // Getting a sublist avoids potential performance issues with removing from the start of an
                    // ArrayList, since it keeps the original list around and only gets a "view" of it.
                    if (!toCursor.isEmpty()) {
                        toCursor = toCursor.subList(1, toCursor.size());
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
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {
        if (health <= 0) return;
        int newX = player.x + xmod, newY = player.y + ymod;
        if (newX >= 0 && newY >= 0 && newX < bigWidth && newY < bigHeight
                && bareDungeon[newX][newY] != '#') {
            // '+' is a door.
            if (lineDungeon[newX][newY] == '+') {
                decoDungeon[newX][newY] = '/';
                lineDungeon[newX][newY] = '/';
                // changes to the map mean the resistances for FOV need to be regenerated.
                resistance = DungeonUtility.generateSimpleResistances(decoDungeon);
                // recalculate FOV, store it in fovmap for the render to use.
                FOV.reuseFOV(resistance, visible, player.x, player.y, fovRange, Radius.CIRCLE);
                blockage.refill(visible, 0.0);
                seen.or(blockage.not());
                blockage.fringe8way();
            } else {
                // recalculate FOV, store it in fovmap for the render to use.
                FOV.reuseFOV(resistance, visible, newX, newY, fovRange, Radius.CIRCLE);
                blockage.refill(visible, 0.0);
                seen.or(blockage.not());
                blockage.fringe8way();
                playerSprite.setPosition(newX * cellWidth, newY * cellHeight);
                player = Coord.get(newX, newY);
                // if a monster was at the position we moved into, and so was successfully removed...
                if(monsters.containsKey(player))
                {
                    monsters.remove(player);
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            if(rng.nextBoolean()) 
                                bgColors[newX+x][newY+y] = FLOAT_BLOOD;
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
        Coord[] playerArray = {player};
        // in some cases you can use keySet() to get a Set of keys, but that makes a read-only view, and we want
        // a copy of the key set that we can edit (so monsters don't move into each others' spaces)
//        OrderedSet<Coord> monplaces = monsters.keysAsOrderedSet();
        int monCount = monsters.size();

        // recalculate FOV, store it in fovmap for the render to use.
        FOV.reuseFOV(resistance, visible, player.x, player.y, fovRange, Radius.CIRCLE);
        blockage.refill(visible, 0.0);
        seen.or(blockage.not());
        blockage.fringe8way();
        // handle monster turns
        ArrayList<Coord> nextMovePositions;
        for(int ci = 0; ci < monCount; ci++)
        {
            Coord pos = monsters.firstKey();
            Sprite mon = monsters.removeFirst();
            // monster values are used to store their aggression, 1 for actively stalking the player, 0 for not.
            if (visible[pos.x][pos.y] > 0.1) {
                getToPlayer.clearGoals();
                nextMovePositions = getToPlayer.findPath(1, 7, monsters.keySet(), null, pos, playerArray);
                if (nextMovePositions != null && !nextMovePositions.isEmpty()) {
                    Coord tmp = nextMovePositions.get(0);
                    // if we would move into the player, instead damage the player and give newMons the current
                    // position of this monster.
                    if (tmp.x == player.x && tmp.y == player.y) {
                        // not sure if this stays red for very long
                        playerSprite.setPackedColor(FLOAT_BLOOD);
                        health--;
                        // make sure the monster is still actively stalking/chasing the player
                        monsters.put(pos, mon);
                    }
                    // otherwise store the new position in newMons.
                    else {
                        // alter is a method on OrderedMap and OrderedSet that changes a key in-place
                        monsters.alter(pos, tmp);
                        mon.setPosition(tmp.x * cellWidth, tmp.y * cellHeight);
                        //display.slide(mon, pos.x, pos.y, tmp.x, tmp.y, 0.125f, null);
                        monsters.put(tmp, mon);
                    }
                } else {
                    monsters.put(pos, mon);
                }
            }
            else
            {
                monsters.put(pos, mon);
            }
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
        Sprite monster;
        for (int i = 0; i < bigWidth; i++) {
            for (int j = 0; j < bigHeight; j++) {
                if(visible[i][j] > 0.0) {
                    pos.set(i * cellWidth, j * cellHeight, 0f);
                    batch.setPackedColor(toCursor.contains(Coord.get(i, j))
                            ? ColorTools.lerpFloatColors(bgColors[i][j], FLOAT_WHITE, 0.9f)
                            : ColorTools.lerpFloatColors(bgColors[i][j], FLOAT_LIGHTING, (float)visible[i][j] * 0.75f + 0.25f));
                    if(lineDungeon[i][j] == '/' || lineDungeon[i][j] == '+') // doors expect a floor drawn beneath them
                        batch.draw(charMapping.get('.', solid), pos.x, pos.y, cellWidth, cellHeight);
                    batch.draw(charMapping.get(lineDungeon[i][j], solid), pos.x, pos.y, cellWidth, cellHeight);
                    if((monster = monsters.get(Coord.get(i, j))) != null)
                    {
                        monster.setAlpha(1f);
                        //batch.setPackedColor(FLOAT_WHITE);
                        monster.draw(batch);
                    }
                } else if(seen.contains(i, j)) {
                    pos.set(i * cellWidth, j * cellHeight, 0f);
                    batch.setPackedColor(ColorTools.lerpFloatColors(bgColors[i][j], FLOAT_GRAY, 0.7f));
                    if(lineDungeon[i][j] == '/' || lineDungeon[i][j] == '+') // doors expect a floor drawn beneath them
                        batch.draw(charMapping.get('.', solid), pos.x, pos.y, cellWidth, cellHeight);
                    batch.draw(charMapping.get(lineDungeon[i][j], solid), pos.x, pos.y, cellWidth, cellHeight);
                }
            }
        }
//        for (int i = 0; i < monsters.size(); i++) {
//            monsters.getAt(i).draw(batch);
//        }
//        playerSprite.setPackedColor(playerColor);
        playerSprite.setPackedColor(FLOAT_WHITE);
        playerSprite.draw(batch);
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
            
            font.draw(batch, "[RED]YOUR CRAWL IS OVER![WHITE]", x, y + 48, wide, Align.center, true);
            font.draw(batch, "A monster sniffs your corpse and says,", x, y + 16, wide, Align.center, true);
            font.draw(batch, lang, x, y - 16, wide, Align.center, true);
            font.draw(batch, "[LIGHT_GRAY]q[WHITE] to quit.", x, y - 80, wide, Align.center, true);
            batch.end();
            if(Gdx.input.isKeyPressed(Q))
                Gdx.app.exit();
            return;
        }

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {
            // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
            if (System.currentTimeMillis() - lastDrawTime >= 80) {
                lastDrawTime = System.currentTimeMillis();
                switch (phase) {
                    case WAIT:
                    case MONSTER_ANIM:
                        Coord m = awaitedMoves.remove(0);
                        if(!toCursor.isEmpty())
                            toCursor.remove(0);
                        move(m.x - player.x, m.y - player.y);
                        break;
                    case PLAYER_ANIM:
                        postMove();
                        break;
                }
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
                    // GreasedRegion that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(13, blockage);
                }
            }
        }
        // if the previous blocks didn't happen, and there are no active animations, then either change the phase
        // (because with no animations running the last phase must have ended), or start a new animation soon.
        else {
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
        pos.set(10, Gdx.graphics.getHeight() - cellHeight - cellHeight, 0);
        mainViewport.unproject(pos);
        font.draw(batch, "Current Health: [RED]" + health + "[WHITE]", pos.x, pos.y);
        batch.end();
    }     
    @Override
	public void resize(int width, int height) {
		super.resize(width, height);
        mainViewport.update(width, height, false);
	}
}
// An explanation of hexadecimal float/double literals was mentioned earlier, so here it is.
// The literal 0x1p-9f is a good example; it is essentially the same as writing 0.001953125f,
// (float)Math.pow(2.0, -9.0), or (1f / 512f), but is possibly faster than the last two if the
// compiler can't optimize float division effectively, and is a good tool to have because these
// hexadecimal float or double literals always represent numbers accurately. To contrast,
// 0.3 - 0.2 is not equal to 0.1 with doubles, because tenths are inaccurate with floats and
// doubles, and hex literals won't have the option to write an inaccurate float or double.
// There's some slightly confusing syntax used to write these literals; the 0x means the first
// part uses hex digits (0123456789ABCDEF), but the p is not a hex digit and is used to start
// the "p is for power" exponent section. In the example, I used -9 for the power; this is a
// base 10 number, and is used to mean a power of 2 that the hex digits will be multiplied by.
// Because the -9 is a base 10 number, the f at the end is not a hex digit, and actually just
// means the literal is a float, in the same way 1.5f is a float. 2.0 to the -9 is the same as
// 1.0 / Math.pow(2.0, 9.0), but re-calculating Math.pow() is considerably slower if you run it
// for every cell during every frame. Though this is most useful for negative exponents because
// there are a lot of numbers after the decimal point to write out with 0.001953125 or the like,
// it is also sometimes handy when you have an integer or long written in hexadecimal and want
// to make it a float or double. You could use the hex long 0x9E3779B9L, for instance, but to
// write that as a double you would use 0x9E3779B9p0 , not the invalid syntax 0x9E3779B9.0 .
// We use p0 there because 2 to the 0 is 1, so multiplying by 1 gets us the same hex number.
// Very large numbers can also benefit by using a large positive exponent; using p10 and p+10
// as the last part of a hex literal are equivalent. You can see the hex literal for any given
// float with Float.toHexString(float), or for a double with Double.toHexString(double) .
// SColor provides the packed float versions of all color constants as hex literals in the
// documentation for each SColor.
// More information here: https://blogs.oracle.com/darcy/hexadecimal-floating-point-literals
