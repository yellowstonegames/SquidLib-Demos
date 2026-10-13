package com.github.yellowstonegames;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import com.github.tommyettinger.random.EnhancedRandom;
import com.github.yellowstonegames.game.AssetData;
import com.github.yellowstonegames.game.Mover;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.grid.NoiseWrapper;
import com.github.yellowstonegames.grid.Region;
import com.github.yellowstonegames.grid.VroomNoise;
import com.github.yellowstonegames.place.FlowingCaveGenerator;
import com.github.yellowstonegames.place.PlaceGenerator;
import com.github.yellowstonegames.place.tileset.TilesetType;
import com.github.yellowstonegames.util.HasPosition3D;
import com.github.yellowstonegames.util.VoxelCollider;

import static com.badlogic.gdx.math.MathUtils.round;

/**
 * Stores a current "level" of the game and its contents, including moving beings and immobile terrain.
 * One of the more important and widely-used classes here.
 */
public class LocalMap {

    /**
     * Grid-aligned terrain storage, this uses an int ID per voxel for convenience. In this case, you could also use
     * byte IDs, and some games might require short IDs, but int is used just because Java can directly create ints.
     */
    public int[][][] tiles;

    /**
     * A SquidSquad Region that stores all Coord points where a floor is walkable.
     */
    public Region floors;
    /**
     * The critical, sortable mapping of Vector4 positions to visible IsoSprites. This includes two IsoSprites for each
     * voxel of terrain: one is the terrain cube itself, and one is its outline, which has a substantial depth modifier
     * so it will only render if no other terrain is covering it. Movers' IsoSprite (and AnimatedIsoSprite) instances
     * are also stored in here.
     */
    public OrderedMap<Vector4, IsoSprite> everything;
    /**
     * The Array of voxel types that can be shown here, typically drawn from a TextureAtlas.
     */
    public Array<TextureAtlas.AtlasRegion> tileset;
//    /**
//     * The reused Sprite for all voxel edges, each of which is a translucent outline above the upper edges of the voxel.
//     */
//    public Sprite edge;
    /**
     * The center position, in fractional tiles, of the entire map's f-size.
     */
    public float fCenter;
    /**
     * The center position, in fractional tiles, of the entire map's g-size.
     */
    public float gCenter;
    /**
     * How many fish we started out needing to rescue.
     */
    public int totalFish = 10;
    /**
     * How many fish have been saved so far.
     */
    public int fishSaved = 0;

    /**
     * A collision tracker for Movers so we can tell when the player should be damaged by touching an enemy.
     */
    public VoxelCollider<Mover> movers;

    /**
     * @return the rotation of the map in degrees
     */
    public float getRotationDegrees() {
        return rotationDegrees;
    }

    /**
     * Sets {@link #rotationDegrees}, but also {@link #cosRotation} and {@link #sinRotation}.
     * @param rotationDegrees the desired rotation of the map, in degrees
     */
    public void setRotationDegrees(float rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
        cosRotation = MathUtils.cosDeg(rotationDegrees);
        sinRotation = MathUtils.sinDeg(rotationDegrees);
    }

    /**
     * The map's current rotation, in degrees.
     */
    public float rotationDegrees = 0f;
    /**
     * The cosine of the map's current rotation, used to avoid repeated calls to {@link MathUtils#cosDeg(float)}.
     */
    public float cosRotation = 0f;
    /**
     * The sine of the map's current rotation, used to avoid repeated calls to {@link MathUtils#sinDeg(float)}.
     */
    public float sinRotation = 0f;
    /**
     * During a rotation animation, this is the starting rotation, in degrees.
     */
    public float previousRotation = 0f;
    /**
     * During a rotation animation, this is the target rotation, in degrees.
     */
    public float targetRotation = 0f;

    /**
     * Mutated often in-place and used to check for positions in {@link #everything}.
     */
    private static final Vector4 tempVec4 = new Vector4();

    /**
     * Present for serialization only, this creates a LocalMap but needs many fields initialized.
     * {@link #tileset}, {@link #tiles},  {@link #fCenter}, {@link #gCenter}, {@link #movers}, and of
     * course {@link #everything} need to be initialized if you use this.
     */
    public LocalMap() {

    }
    /**
     * Creates a new LocalMap and initializes all fields.
     * @param width the f-size of the map
     * @param height the g-size of the map
     * @param layers the h-size of the map
     * @param atlas a TextureAtlas this will pull many regions from
     */
    public LocalMap(int width, int height, int layers, TextureAtlas atlas) {
        this.tileset = new Array<>(AssetData.LAND.size());
        for(String land : AssetData.LAND){
            this.tileset.add(atlas.findRegion(land, 0));
        }
//        this.edge = atlas.createSprite("edge");
        tiles = new int[width][height][layers];
        floors = new Region(width, height);
        fCenter = (width - 1) * 0.5f;
        gCenter = (height - 1) * 0.5f;
        for (int f = 0; f < width; f++) {
            for (int g = 0; g < height; g++) {
                for (int h = 0; h < layers; h++) {
                    tiles[f][g][h] = -1;
                }
            }
        }
        everything = new OrderedMap<>(width * height * layers * 3 >>> 2, 0.625f);

        movers = new VoxelCollider<>();
    }

    /**
     * Checks if the given f,g,h isometric tile position is a valid position that can be looked up in {@link #tiles}.
     * @param f f-coordinate; if negative or too high, this returns false
     * @param g g-coordinate; if negative or too high, this returns false
     * @param h h-coordinate; if negative or too high, this returns false
     * @return true if the given coordinates are valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(int f, int g, int h) {
        return f >= 0 && g >= 0 && h >= 0 && f < tiles.length && g < tiles[0].length && h < tiles[0][0].length;
    }

    /**
     * Rounds f, g, and h and passes them to {@link #isValid(int, int, int)}. Note that this permits small negative
     * inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param f f-coordinate; if too low or too high, this returns false
     * @param g g-coordinate; if too low or too high, this returns false
     * @param h h-coordinate; if too low or too high, this returns false
     * @return true if the given coordinates, after rounding, are valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(float f, float g, float h) {
        return isValid(round(f), round(g), round(h));
    }

    /**
     * Delegates to {@link #isValid(float, float, float)} using only the x, y, and z coordinates of {@code point}.
     * @param point a Vector4 of which only x, y, and z will be checked
     * @return true if the given point, after rounding x, y, and z, is valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(Vector4 point) {
        return isValid(point.x, point.y, point.z);
    }

    /**
     * If f, g, and h are valid, this returns the tile at that location; if no tile is present or if the coordinates are
     * invalid, this returns -1.
     * @param f f-coordinate; if negative or too high, this returns false
     * @param g g-coordinate; if negative or too high, this returns false
     * @param h h-coordinate; if negative or too high, this returns false
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(int f, int g, int h) {
        return isValid(f, g, h) ? tiles[f][g][h] : -1;
    }

    /**
     * Rounds the given float coordinates and passes them to {@link #getTile(int, int, int)}. Note that this permits
     * small negative inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param f f-coordinate; if too low or too high, this returns false
     * @param g g-coordinate; if too low or too high, this returns false
     * @param h h-coordinate; if too low or too high, this returns false
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(float f, float g, float h) {
        return getTile(round(f), round(g), round(h));
    }

    /**
     * Rounds the x, y, and z of {@code point} and passes them to {@link #getTile(int, int, int)}. Note that this
     * permits small negative inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param point a Vector4 of which only x, y, and z will be checked
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(Vector4 point) {
        return getTile(round(point.x), round(point.y), round(point.z));
    }

    /**
     * Gets the IsoSprite with the appropriate depth for a terrain voxel at the given isometric position (not rounded).
     * @param f f position of the terrain, almost always an integer stored in a float
     * @param g g position of the terrain, almost always an integer stored in a float
     * @param h h position of the terrain, almost always an integer stored in a float
     * @return the IsoSprite for terrain at the given position or {@code null} if none is present
     */
    public IsoSprite getIsoSpriteTerrain(float f, float g, float h) {
        return everything.get(tempVec4.set(f, g, h, 0));
    }

    /**
     * Modifies the given Vector4 so it holds the given [f,g,h] position with the depth a fish can have.
     * This rounds f, g, and h because a fish is always at an all-integer position. If {@link #everything} does not
     * have anything present at the Vector4 this produces, there is no fish present at that position, but if it does
     * have any IsoSprite present, it will be a fish.
     *
     * @param changing a Vector4 that will be modified in-place
     * @param f the "France to Finland" isometric coordinate; will be rounded and assigned to changing
     * @param g the "Germany to Greenland" isometric coordinate; will be rounded and assigned to changing
     * @param h the "heel to head" isometric coordinate; will be rounded and assigned to changing
     */
    public void setToFishPosition(Vector4 changing, float f, float g, float h) {
        changing.set(round(f), round(g), round(h), Mover.FISH_W);
    }

    /**
     * Adds a {@link Mover} to {@link #everything} and {@link #movers}. If the Mover's position is already occupied by
     * a terrain tile, or if this would collide with another Mover, the Mover's position is randomized until it finds
     * a valid location.
     * @param mover a {@link Mover} that will have its position potentially altered if invalid
     * @param depth the depth modifier to use for the Mover, such as {@link Mover#PLAYER_W}
     * @return the used Vector4 position the Mover was placed into, which may be different from its original position
     */
    public Vector4 addMover(Mover mover, float depth) {
        mover.getPosition().z = getLayers() - 1;
        Vector4 pos = new Vector4(mover.getPosition(), depth);
        while (getTile(pos) != -1 || movers.collisionsWith(mover).notEmpty()) {
            pos.x = MathUtils.random(getWidth() - 1);
            pos.y = MathUtils.random(getHeight() - 1);
            mover.getPosition().set(pos.x, pos.y, pos.z);
        }
        mover.place(depth);
        movers.entities.add(mover);
        return pos;
    }

    /**
     * When point.w is 0, this selects terrain; when it is ENTITY_W, it selects an entity.
     * @param point a Vector4 key in {@link #everything}
     * @return an IsoSprite looked up in {@link #everything}, or null if nothing was found
     */
    public IsoSprite getIsoSprite(Vector4 point) {
        return everything.get(point);
    }

    /**
     * Sets the voxel terrain tile at the given isometric tile position to the tile with the given ID.
     * IDs can be seen in {@link AssetData}.
     * This also places an edge at the same isometric position but a lower depth, so it only shows if there is
     * empty space behind the voxel in the depth sort.
     * @param f f-position as an int
     * @param g g-position as an int
     * @param h h-position as an int
     * @param tileId an ID for a tile, typically from {@link AssetData}; may be -1 to remove a tile
     */
    public void setTile(int f, int g, int h, int tileId) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(tempVec4.set(f, g, h, 0));
                everything.remove(tempVec4.set(f, g, h, -1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(tempVec4.set(f, g, h, 0))) != null) {
                    iso.setSprite(new AtlasSprite(tileset.get(tileId)));
                } else {
                    everything.put(new Vector4(f, g, h, 0), iso = new IsoSprite(new AtlasSprite(tileset.get(tileId)), f, g, h));
//                    // Environment tiles have an outline that may render if there is empty space behind them.
//                    // The position has -1.5 for w, and w is added to the depth for the purpose of sorting.
//                    // Adjacent environment tiles should have a depth that is +1 or -1 from this tile.
//                    // Because the outline is -1.5 behind this tile, adjacent environment tiles will render over it,
//                    // but if there is empty space behind a tile, the outline will be in front of the further tiles.
//                    everything.put(new Vector4(f, g, h, -1.5f), new IsoSprite(edge, f, g, h));
                }
                // Environment tiles (not outlines) get darkened based on their elevation (h axis).
                final float tint = Math.min(1f, h * 0.08f + 0.6f);
                iso.sprite.setColor(tint, tint, tint, 1f);
            }
        }
    }

    /**
     * Sets the voxel terrain tile at the given isometric tile position (as a Vector4, which will be rounded when used
     * in {@link #tiles}) to the tile with the given ID. IDs can be seen in {@link AssetData}.
     * This also places an edge at the same isometric position but a lower depth, so it only shows if there is
     * empty space behind the voxel in the depth sort.
     * @param point a Vector4 of which x, y, and z will be used for a position and w will generally be treated as 0
     * @param tileId an ID for a tile, typically from {@link AssetData}
     */
    public void setTile(Vector4 point, int tileId) {
        int f = round(point.x);
        int g = round(point.y);
        int h = round(point.z);
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(point);
                // remove the outline, too
                everything.remove(point.add(0,0,0,-1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(point)) != null) {
                    iso.setPosition(point.x, point.y, point.z);
                } else {
                    everything.put(point, iso = new IsoSprite(new AtlasSprite(tileset.get(tileId)), point.x, point.y, point.z));
//                    // Environment tiles have an outline that may render if there is empty space behind them.
//                    // The position has -1.5 for w, and w is added to the depth for the purpose of sorting.
//                    // Adjacent environment tiles should have a depth that is +1 or -1 from this tile.
//                    // Because the outline is -1.5 behind this tile, adjacent environment tiles will render over it,
//                    // but if there is empty space behind a tile, the outline will be in front of the further tiles.
//                    everything.put(new Vector4(point.x, point.y, point.z, -1.5f), new IsoSprite(edge, point.x, point.y, point.z));
                }
                // Environment tiles (not outlines) get darkened based on their elevation (h axis).
                final float tint = Math.min(1f, h * 0.08f + 0.6f);
                iso.sprite.setColor(tint, tint, tint, 1f);

            }
        }
    }

    /**
     * Adds an {@link IsoSprite} to {@link #everything} at the requested f, g, h, depth position, if and only if the
     * position (with rounded float coordinates) is valid in the bounds of {@link #tiles}. This removes whatever tile
     * may be present at the position this places the entity into.
     * @param f the "France to Finland" isometric coordinate; will be rounded and assigned to changing
     * @param g the "Germany to Greenland" isometric coordinate; will be rounded and assigned to changing
     * @param h the "heel to head" isometric coordinate; will be rounded and assigned to changing
     * @param depth the depth modifier to use, such as {@link Mover#PLAYER_W}
     * @param sprite the {@link IsoSprite} to place into {@link #everything}
     */
    public void setEntity(float f, float g, float h, float depth, IsoSprite sprite) {
        int rf = round(f), rg = round(g), rh = round(h);
        if (isValid(rf, rg, rh)) {
            tiles[rf][rg][rh] = -1;
            sprite.setPosition(f, g, h);
            everything.put(new Vector4(f, g, h, depth), sprite);
        }
    }

    /**
     * The size of the f-axis (the first dimension of tiles).
     * @return {@code tiles.length}
     */
    public int getFSize() {
        return tiles.length;
    }

    /**
     * The size of the g-axis (the second dimension of tiles).
     * @return {@code tiles[0].length}
     */
    public int getGSize() {
        return tiles[0].length;
    }

    /**
     * The size of the h-axis (the third dimension of tiles).
     * @return {@code tiles[0][0].length}
     */
    public int getHSize() {
        return tiles[0][0].length;
    }

    /**
     * The same as {@link #getFSize()}.
     * @return the f-size of the map.
     */
    public int getWidth() {
        return tiles.length;
    }

    /**
     * The same as {@link #getGSize()}.
     * @return the g-size of the map.
     */
    public int getHeight() {
        return tiles[0].length;
    }

    /**
     * The same as {@link #getHSize()}.
     * @return the h-size of the map.
     */
    public int getLayers() {
        return tiles[0][0].length;
    }

    /**
     * Used to allow paths to meander across the map area, without changing directions completely at random.
     */
    private static final GridPoint2[] DIRECTIONS = {new GridPoint2(1, 0), new GridPoint2(0, 1), new GridPoint2(-1, 0), new GridPoint2(0, -1)};

    /**
     * Generates a simple test map that assumes a specific tileset. Allows setting a specific seed to get the same map
     * every time. This requires a minimum {@code mapSize} of 11 and a minimum {@code mapPeak} of 4.
     * <br>
     * CUSTOM TO YOUR GAME.
     * @param random a juniper EnhancedRandom
     * @param mapSize the width and height of the map, or the dimensions of the ground plane in tiles
     * @param mapPeak the layer count or max elevation of the map
     * @param atlas should probably be the TextureAtlas loaded from {@code PandoraTheBoxer.atlas}
     * @return a new LocalMap
     */
    public static LocalMap generateMap(EnhancedRandom random, int mapSize, int mapPeak, TextureAtlas atlas) {

        // Noise that gradually moves a little
        NoiseWrapper baseNoise = new NoiseWrapper(new VroomNoise(random.nextLong()), 0.17f, NoiseWrapper.FBM, 3);
        // Noise that is usually a low value, but has ridges of high values
        NoiseWrapper ridgeNoise = new NoiseWrapper(new VroomNoise(random.nextLong()), 0.22f, NoiseWrapper.RIDGED_MULTI, 1);

        mapSize = Math.max(11, mapSize);
        mapPeak = Math.max(4, mapPeak);

        LocalMap map = new LocalMap(mapSize, mapSize, mapPeak, atlas);
        PlaceGenerator mapGen = new FlowingCaveGenerator(mapSize, mapSize, TilesetType.DEFAULT_DUNGEON, random);
//        PlaceGenerator mapGen = new OrganicConnectingMapGenerator(mapSize, mapSize, 6, 6, 3, new AceRandom(seed));
        char[][] dungeon = mapGen.generate();
        map.floors.refill(dungeon, '.');
        // Cave map as a base, with slopes determined by noise.
        for (int f = 0; f < mapSize; f++) {
            for (int g = 0; g < mapSize; g++) {
                int height = dungeon[f][g] == '#' ? 2 : 0;
                // tile ID 4 is a solid rock cube.
                map.setTile(f, g, height, 4);
                if(height == 0){
                    int n = baseNoise.getNoise(f+1, g+1) > 0f ? AssetData.NORTH : 0;
                    int w = baseNoise.getNoise(f+1, g  ) > 0f ? AssetData.WEST : 0;
                    int s = baseNoise.getNoise(f  , g  ) > 0f ? AssetData.SOUTH : 0;
                    int e = baseNoise.getNoise(f  , g+1) > 0f ? AssetData.EAST : 0;
                    map.setTile(f, g, 1, AssetData.SLOPE_INDICES[0][n|w|s|e]);
                }
                // Anything below one of these tiles must be solid.
                for (int h = height - 1; h >= 0; h--) {
                    map.setTile(f, g, h, 4);
                }
            }
        }

        // Remove this next line if you intend to allow players to edit the map or destroy tiles during play.
//        removeInvisibleTiles(map);
        return map;
    }

    /**
     * Modifies the given LocalMap so completely-surrounded tiles are removed from the map, "hollowing" out the map.
     * This is meant to reduce the amount of work needed to render large maps with lots of tiles that can't ever be
     * seen because they are surrounded on all sides. Having a tile below another doesn't change its visibility.
     * <br>
     * This should not be called if you want to be able to edit the map at runtime. That would make digging caves
     * impossible, because the ground is just a thin shell above empty space after this is called.
     *
     * @param editing a LocalMap that will be modified in-place
     */
    public static void removeInvisibleTiles(LocalMap editing) {
        int fs = editing.getFSize();
        int gs = editing.getGSize();
        int hs = editing.getHSize();
        // We need two passes to first figure out which tiles to remove,
        // and then to remove them once we know.
        boolean[][][] remove = new boolean[fs][gs][hs];
        // These loops start at 1 and end early so the edges of the map don't get removed.
        for (int f = 1; f < fs - 1; f++) {
            for (int g = 1; g < gs - 1; g++) {
                // This loop has to go through everything.
                for (int h = hs - 1; h >= 0; h--) {
                    // For a tile to be invisible, it:
                    // can't be on the top level of the world,
                    // can't have empty space above it, and
                    // can't have empty space on any side.
                    remove[f][g][h] = h != hs - 1 &&
                        editing.tiles[f][g][h + 1] != -1 &&
                        editing.tiles[f + 1][g][h] != -1 &&
                        editing.tiles[f - 1][g][h] != -1 &&
                        editing.tiles[f][g + 1][h] != -1 &&
                        editing.tiles[f][g - 1][h] != -1;
                }
            }
        }
        // If we decided to remove a tile before, here is where we do that.
        for (int f = 1; f < fs - 1; f++) {
            for (int g = 1; g < gs - 1; g++) {
                for (int h = hs - 1; h >= 0; h--) {
                    if(remove[f][g][h])
                        editing.setTile(f, g, h, -1);
                }
            }
        }
        // This modifies "editing" in-place, so we don't need to return.
    }

    /**
     * Places box {@link AnimatedIsoSprite} instances at various valid locations, chosen sub-randomly.
     * Sub-random here means it is extremely unlikely two boxes will spawn nearby each other, but their position is
     * otherwise random-seeming.
     * <br>
     * This should be customized for your game.
     * @param random if this is seeded identically, the same map will be produced on each call
     * @param boxCount how many boxes to place
     * @param animations used to get the animation for a box so we can make {@link AnimatedIsoSprite}s per box
     * @return this LocalMap for chaining
     */
    public Coord[] placeBoxes(EnhancedRandom random, int boxCount, Array<Array<Animation<AtlasSprite>>> animations) {
        Coord[] found = floors.separatedPoisson(random, 3, boxCount);
        for(Coord point : found)
        {
            int h = tiles[point.x][point.y][1] == -1 ? 1 : 2;
            setEntity(point.x, point.y, h, Mover.NPC_W, new AnimatedIsoSprite(animations.get(0).get(AssetData.ENTITIES.indexOf("Box_Open")), point.x, point.y, h));
        }
        return found;
    }

    /**
     * Simply wraps {@link VoxelCollider#collisionsWith(HasPosition3D)}, using this LocalMap's {@link #movers}.
     * @param mover the Mover to check if any existing Movers collide with
     * @return an Array of colliding Movers, which will be empty if nothing collides with {@code mover}
     */
    public Array<Mover> checkCollision(Mover mover) {
        return movers.collisionsWith(mover);
    }
}
