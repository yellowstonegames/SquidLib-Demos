package com.github.yellowstonegames.game;

import com.badlogic.gdx.utils.*;
import com.github.tommyettinger.ds.NumberedSet;
import com.github.yellowstonegames.LocalMap;

/**
 * Data specific to the art assets used here. If the art assets change, you will need to update this class.
 * This tracks not just the numbers associated with unnamed art assets, but also which tiles are rotations of other
 * tiles for when the map rotates, which path tiles can connect to which other path tiles, and so on.
 * There are also some numbers and names for entities, but the code here mostly doesn't use them because the
 * specific assets were already sorted how we needed them -- entities 0-3 are playable humans, and 4-7 are enemy orcs.
 * <br>
 * This has constants to provide names for the numbered tiles and entities in the {@code PandoraTheBoxer.atlas}, a
 * NumberedSet {@link #LAND} that allows looking up tile indices with a String name and vice versa, and a map
 * {@link #ENTITIES} that allows looking up entity constants (for use in the four Animation Arrays).
 * <br>
 * In the project as it is here, all tiles have already been stored in a single atlas on one page, sharing that page
 * with a bitmap font and UI widgets. Using just one atlas page helps performance quite a bit, because it avoids any
 * "texture swaps" that SpriteBatch would otherwise need to do when a different page or Texture was needed. If you have
 * your own assets, packing them into an atlas is strongly recommended! For this project, the very convenient
 * <a href="https://github.com/crashinvaders/gdx-texture-packer-gui">CrashInvaders' GDX Texture Packer GUI</a> was used,
 * but you can also use the texture packer supplied in libGDX as part of its gdx-tools, or launch that packer as part of
 * your Gradle configuration so your atlas is always up-to-date. Packing an atlas does best with individual tiny images
 * with the appropriate names you want to look up, OR some common prefix followed by an underscore {@code _} and a
 * number to make (usually) an animation frame with that index. The numbers can be finicky; make sure to start at 0 and
 * not include any initial 0 otherwise ("0", "1", "2", is good, but "00", "01", "02" is not).
 */
@SuppressWarnings("PointlessArithmeticExpression")
public final class AssetData {
    /**
     * The horizontal distance in pixels between adjacent tiles. This is equivalent to the distance of one diagonal side
     * of the diamond-shaped top of any solid tile here, measured from left to right for a single side.
     * <br>
     * This depends on your exact terrain assets, and it will potentially change if your art does.
     */
    public static final int TILE_WIDTH = 32;
    /**
     * The vertical distance in pixels between adjacent tiles. This is equivalent to the distance of one diagonal side
     * of the diamond-shaped top of any solid tile here, measured from bottom to top for a single side.
     * <br>
     * This depends on your exact terrain assets, and it will potentially change if your art does.
     */
    public static final int TILE_HEIGHT = 16;
    /**
     * The vertical distance in pixels between stacked tiles. This is equivalent to the distance of a vertical side of
     * any full-sized solid tile (on the left or right side of the block), measured from bottom to top of a solid side.
     * <br>
     * This depends on your exact terrain assets, and it will potentially change if your art does.
     */
    public static final int TILE_DEPTH = 32;

    public static final int NORTH  = 1 << 3;
    public static final int EAST   = 1 << 2;
    public static final int SOUTH  = 1 << 1;
    public static final int WEST   = 1;

    /**
     * Allows getting names in the atlas given indices, and vice versa. All shapes are in groups of 4.
     */
    public static final NumberedSet<String> LAND = NumberedSet.with(
        "Rock_Corner_angle0",
        "Rock_Corner_angle1",
        "Rock_Corner_angle2",
        "Rock_Corner_angle3",
        "Rock_Cube_angle0",
        "Rock_Cube_angle1",
        "Rock_Cube_angle2",
        "Rock_Cube_angle3",
        "Rock_Nick_angle0",
        "Rock_Nick_angle1",
        "Rock_Nick_angle2",
        "Rock_Nick_angle3",
        "Rock_Ridge_angle0",
        "Rock_Ridge_angle1",
        "Rock_Ridge_angle2",
        "Rock_Ridge_angle3",
        "Rock_Slope_angle0",
        "Rock_Slope_angle1",
        "Rock_Slope_angle2",
        "Rock_Slope_angle3",
        "Rock_Valley_angle0",
        "Rock_Valley_angle1",
        "Rock_Valley_angle2",
        "Rock_Valley_angle3",
        "Rock_Lichen_Corner_angle0",
        "Rock_Lichen_Corner_angle1",
        "Rock_Lichen_Corner_angle2",
        "Rock_Lichen_Corner_angle3",
        "Rock_Lichen_Cube_angle0",
        "Rock_Lichen_Cube_angle1",
        "Rock_Lichen_Cube_angle2",
        "Rock_Lichen_Cube_angle3",
        "Rock_Lichen_Nick_angle0",
        "Rock_Lichen_Nick_angle1",
        "Rock_Lichen_Nick_angle2",
        "Rock_Lichen_Nick_angle3",
        "Rock_Lichen_Ridge_angle0",
        "Rock_Lichen_Ridge_angle1",
        "Rock_Lichen_Ridge_angle2",
        "Rock_Lichen_Ridge_angle3",
        "Rock_Lichen_Slope_angle0",
        "Rock_Lichen_Slope_angle1",
        "Rock_Lichen_Slope_angle2",
        "Rock_Lichen_Slope_angle3",
        "Rock_Lichen_Valley_angle0",
        "Rock_Lichen_Valley_angle1",
        "Rock_Lichen_Valley_angle2",
        "Rock_Lichen_Valley_angle3",
        "Rock_Moss_Corner_angle0",
        "Rock_Moss_Corner_angle1",
        "Rock_Moss_Corner_angle2",
        "Rock_Moss_Corner_angle3",
        "Rock_Moss_Cube_angle0",
        "Rock_Moss_Cube_angle1",
        "Rock_Moss_Cube_angle2",
        "Rock_Moss_Cube_angle3",
        "Rock_Moss_Nick_angle0",
        "Rock_Moss_Nick_angle1",
        "Rock_Moss_Nick_angle2",
        "Rock_Moss_Nick_angle3",
        "Rock_Moss_Ridge_angle0",
        "Rock_Moss_Ridge_angle1",
        "Rock_Moss_Ridge_angle2",
        "Rock_Moss_Ridge_angle3",
        "Rock_Moss_Slope_angle0",
        "Rock_Moss_Slope_angle1",
        "Rock_Moss_Slope_angle2",
        "Rock_Moss_Slope_angle3",
        "Rock_Moss_Valley_angle0",
        "Rock_Moss_Valley_angle1",
        "Rock_Moss_Valley_angle2",
        "Rock_Moss_Valley_angle3");

    /**
     * 3 arrays of 16 combinations of corner heights. The index into the outer array should correspond to whether you
     * want Rock, Rock with Lichen, or Rock with Moss. The index into the inner array can be composed by the height of
     * the corners of a tile, as composed by ORing {@link #NORTH}, {@link #SOUTH}, {@link #EAST}, and {@link #WEST}.
     */
    public static final String[][] SLOPES = {
        {
            "Empty_angle0",
            "Rock_Corner_angle1",
            "Rock_Corner_angle0",
            "Rock_Slope_angle1",
            "Rock_Corner_angle3",
            "Rock_Valley_angle0",
            "Rock_Slope_angle0",
            "Rock_Nick_angle0",
            "Rock_Corner_angle2",
            "Rock_Slope_angle2",
            "Rock_Valley_angle1",
            "Rock_Nick_angle1",
            "Rock_Slope_angle3",
            "Rock_Nick_angle2",
            "Rock_Nick_angle3",
            "Rock_Cube_angle0",
        },
        {
            "Empty_angle0",
            "Rock_Lichen_Corner_angle1",
            "Rock_Lichen_Corner_angle0",
            "Rock_Lichen_Slope_angle1",
            "Rock_Lichen_Corner_angle3",
            "Rock_Lichen_Valley_angle0",
            "Rock_Lichen_Slope_angle0",
            "Rock_Lichen_Nick_angle0",
            "Rock_Lichen_Corner_angle2",
            "Rock_Lichen_Slope_angle2",
            "Rock_Lichen_Valley_angle1",
            "Rock_Lichen_Nick_angle1",
            "Rock_Lichen_Slope_angle3",
            "Rock_Lichen_Nick_angle2",
            "Rock_Lichen_Nick_angle3",
            "Rock_Lichen_Cube_angle0",
        },
        {
            "Empty_angle0",
            "Rock_Moss_Corner_angle1",
            "Rock_Moss_Corner_angle0",
            "Rock_Moss_Slope_angle1",
            "Rock_Moss_Corner_angle3",
            "Rock_Moss_Valley_angle0",
            "Rock_Moss_Slope_angle0",
            "Rock_Moss_Nick_angle0",
            "Rock_Moss_Corner_angle2",
            "Rock_Moss_Slope_angle2",
            "Rock_Moss_Valley_angle1",
            "Rock_Moss_Nick_angle1",
            "Rock_Moss_Slope_angle3",
            "Rock_Moss_Nick_angle2",
            "Rock_Moss_Nick_angle3",
            "Rock_Moss_Cube_angle0",
        },
    };

    /**
     * Tile indices that can be looked up in {@link #LAND}; includes -1 when no voxel should be present.
     */
    public static final int[][] SLOPE_INDICES = new int[SLOPES.length][16];
    static {
        LAND.setDefaultValue(-1);
        for (int t = 0; t < SLOPES.length; t++) {
            for (int s = 0; s < 16; s++) {
                SLOPE_INDICES[t][s] = LAND.indexOf(SLOPES[t][s]);
            }
        }
    }

    /**
     * Given an index from {@link #SLOPE_INDICES} and a number of quarter-turns to rotate it, gets the rotated index
     * that can be looked up in {@link #LAND} with {@link NumberedSet#getAt(int)}. If the index is negative, this always
     * returns -1, which importantly can't be looked up with getAt(), but may be a correct tile index.
     * @param index an index from {@link #SLOPE_INDICES}
     * @param relativeRotation how many quarter-turns to rotate the image at the given index; may be negative
     * @return the rotated index
     */
    public static int rotateIndex(int index, int relativeRotation){
        return (index & ~3) | (index + relativeRotation & 3) | (index >> 31);
    }

    /**
     * No need to instantiate.
     */
    private AssetData() {}

    public static final String[] entityNames = {
        "Pandora", "Pandora_Attack", "Eye_Tyrant", "Eye_Tyrant_Attack", "Box_Closed", "Box_Open"
    };
    public static final NumberedSet<String> ENTITIES = new NumberedSet<>(entityNames);
}
