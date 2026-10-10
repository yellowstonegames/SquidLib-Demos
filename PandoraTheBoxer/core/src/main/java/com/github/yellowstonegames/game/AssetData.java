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
 * This has constants to provide names for the numbered tiles and entities in the {@code isometric-trpg.atlas}, a map
 * {@link #TILES} that allows looking up tile constants with a String name, and a map {@link #ENTITIES} that allows
 * looking up entity constants (for use in the four Animation Arrays). Importantly, this also stores data for how to
 * render some tiles when they rotate, to prevent paths from becoming disconnected when the map rotates.
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
 * <br>
 * If you use a different set of assets, you will not need the values in this file, but you could use it as an example
 * for how to make your own. You could also just use int indices throughout your project, or name the regions in your
 * atlas according to your own rules. Other free assets may have names you can apply to the atlas instead of numbers,
 * but this tends to be rather rare in practice. It isn't isometric normally, but
 * <a href="https://github.com/tommyettinger/DawnLikeAtlas">DawnLikeAtlas</a> is a large, free set of assets that has
 * names attached to each file in a libGDX atlas. Its entities are about the same size as the ones used here, though
 * they don't have facing and have just two animation frames. The DawnLike creatures could be used instead of the assets
 * here, though, since there are many more of them. Ideally, you would repack any extra assets into an atlas like the
 * one this uses (with a font and any terrain you want). You can either fetch the raw assets from the independent repo
 * used for in-progress parts of this project,
 * <a href="https://github.com/tommyettinger/IsometricVoxelDemo">which is here</a>, or unpack the atlas used here (the
 * GUI linked above has an "Unpack atlas" option in the top bar), then get the parts you want from any atlases you want
 * to mix in, and pack those all together into one central atlas. Using just one folder works well.
 * <br>
 * There are several terms used in the tile names for grouping logically.
 * <ul>
 * <li>Tiles that are meant to be approximately half as tall as a unit voxel contain "HALF" in the constant name.</li>
 * <li>Tiles that are meant to just barely cover the top of a unit voxel contain "BASE" in their constant name.</li>
 * <li>Tiles that are meant to flow over the top of a surface of their height contain "COVER" in their constant name.</li>
 * <li>Cover tiles have a suffix containing "F", "G", and/or "H" depending on whether they cover the f or g side edges,
 * or the h face on top.</li>
 * <li>Tiles that have a path on them that changes depending on view angle have "PATH" in their constant name. Path tiles
 * have a suffix containing "F", "G", "T", and/or "R" depending on which faces are connected to the path, where fgtr are
 * the recommended keys on a QWERTY keyboard to move in those directions, or on a map of Europe, the locations of
 * France, Germany, Tallinn (in Estonia), and Reykjavík (in Iceland) relative to Amsterdam in the center.</li>
 * <li>Tiles that are not meant to be stackable have "DECO" in their constant name.</li>
 * <li>Bed tiles have "BED" in the name and a suffix indicating the faces that touch the footboard and headboard.</li>
 * <li>Some tiles have "CAP" in their name to indicate that only the top face is a given type, and the rest of the unit
 * voxel is simply dirt.</li>
 * </ul>
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

    /**
     * No need to instantiate.
     */
    private AssetData() {}

    public static final int DIRT                 =   0 +  0;
    public static final int GRASS                =   1 +  0;
    public static final int BASALT               =   2 +  0;
    public static final int SAND                 =   3 +  0;
    public static final int HALF_COVER_WATER_FGH =   4 +  0;
    public static final int HALF_COVER_WATER_FH  =   5 +  0;
    public static final int HALF_COVER_WATER_GH  =   6 +  0;
    public static final int HALF_COVER_WATER_H   =   7 +  0;
    public static final int DECO_ROCKS           =   8 +  0;
    public static final int DECO_BOULDER         =   9 +  0;
    public static final int DECO_CATTAIL         =  10 +  0;
    public static final int HALF_DIRT            =   0 + 11;
    public static final int HALF_GRASS           =   1 + 11;
    public static final int HALF_BASALT          =   2 + 11;
    public static final int HALF_SAND            =   3 + 11;
    public static final int BASE_COVER_WATER_FGH =   4 + 11;
    public static final int BASE_COVER_WATER_FH  =   5 + 11;
    public static final int BASE_COVER_WATER_GH  =   6 + 11;
    public static final int BASE_COVER_WATER_H   =   7 + 11;
    public static final int DECO_BUSH            =   8 + 11;
    public static final int DECO_STUMP           =   9 + 11;
    public static final int DECO_FLAME           =  10 + 11;
    public static final int SNOW                 =   0 + 22;
    public static final int ICE                  =   1 + 22;
    public static final int LAVA                 =   2 + 22;
    public static final int DRY                  =   3 + 22;
    public static final int HALF_COVER_SWAMP_FGH =   4 + 22;
    public static final int HALF_COVER_SWAMP_FH  =   5 + 22;
    public static final int HALF_COVER_SWAMP_GH  =   6 + 22;
    public static final int HALF_COVER_SWAMP_H   =   7 + 22;
    public static final int DECO_BUNDLE          =   8 + 22;
    public static final int DECO_HEDGE           =   9 + 22;
    public static final int DECO_CRYSTALS        =  10 + 22;
    public static final int HALF_SNOW            =   0 + 33;
    public static final int HALF_ICE             =   1 + 33;
    public static final int HALF_LAVA            =   2 + 33;
    public static final int HALF_DRY             =   3 + 33;
    public static final int BASE_COVER_SWAMP_FGH =   4 + 33;
    public static final int BASE_COVER_SWAMP_FH  =   5 + 33;
    public static final int BASE_COVER_SWAMP_GH  =   6 + 33;
    public static final int BASE_COVER_SWAMP_H   =   7 + 33;
    public static final int DECO_LOG             =   8 + 33;
    public static final int DECO_CACTUS          =   9 + 33;
    public static final int DECO_SPIKE           =  10 + 33;
    public static final int PATH_GRASS_GR        =   0 + 44;
    public static final int PATH_GRASS_FT        =   1 + 44;
    public static final int PATH_GRASS_FTR       =   2 + 44;
    public static final int PATH_GRASS_FGT       =   3 + 44;
    public static final int PATH_GRASS_FGTR      =   4 + 44;
    public static final int PATH_GRASS_GTR       =   5 + 44;
    public static final int PATH_GRASS_FGR       =   6 + 44;
    public static final int PATH_GRASS_FG        =   7 + 44;
    public static final int PATH_GRASS_GT        =   8 + 44;
    public static final int PATH_GRASS_TR        =   9 + 44;
    public static final int PATH_GRASS_FR        =  10 + 44;
    public static final int HALF_PATH_GRASS_GR   =   0 + 55;
    public static final int HALF_PATH_GRASS_FT   =   1 + 55;
    public static final int HALF_PATH_GRASS_FTR  =   2 + 55;
    public static final int HALF_PATH_GRASS_FGT  =   3 + 55;
    public static final int HALF_PATH_GRASS_FGTR =   4 + 55;
    public static final int HALF_PATH_GRASS_GTR  =   5 + 55;
    public static final int HALF_PATH_GRASS_FGR  =   6 + 55;
    public static final int HALF_PATH_GRASS_FG   =   7 + 55;
    public static final int HALF_PATH_GRASS_GT   =   8 + 55;
    public static final int HALF_PATH_GRASS_TR   =   9 + 55;
    public static final int HALF_PATH_GRASS_FR   =  10 + 55;
    public static final int PATH_DRY_GR          =   0 + 66;
    public static final int PATH_DRY_FT          =   1 + 66;
    public static final int PATH_DRY_FTR         =   2 + 66;
    public static final int PATH_DRY_FGT         =   3 + 66;
    public static final int PATH_DRY_FGTR        =   4 + 66;
    public static final int PATH_DRY_GTR         =   5 + 66;
    public static final int PATH_DRY_FGR         =   6 + 66;
    public static final int PATH_DRY_FG          =   7 + 66;
    public static final int PATH_DRY_GT          =   8 + 66;
    public static final int PATH_DRY_TR          =   9 + 66;
    public static final int PATH_DRY_FR          =  10 + 66;
    public static final int HALF_PATH_DRY_GR     =   0 + 77;
    public static final int HALF_PATH_DRY_FT     =   1 + 77;
    public static final int HALF_PATH_DRY_FTR    =   2 + 77;
    public static final int HALF_PATH_DRY_FGT    =   3 + 77;
    public static final int HALF_PATH_DRY_FGTR   =   4 + 77;
    public static final int HALF_PATH_DRY_GTR    =   5 + 77;
    public static final int HALF_PATH_DRY_FGR    =   6 + 77;
    public static final int HALF_PATH_DRY_FG     =   7 + 77;
    public static final int HALF_PATH_DRY_GT     =   8 + 77;
    public static final int HALF_PATH_DRY_TR     =   9 + 77;
    public static final int HALF_PATH_DRY_FR     =  10 + 77;
    public static final int SANDSTONE            =   0 + 88;
    public static final int SLATE                =   1 + 88;
    public static final int BRICK                =   2 + 88;
    public static final int WOOD                 =   3 + 88;
    public static final int CAP_SANDSTONE        =   4 + 88;
    public static final int CAP_SLATE            =   5 + 88;
    public static final int CAP_WOOD             =   6 + 88;
    public static final int CAP_THATCHED         =   7 + 88;
    public static final int DECO_BED_GR          =   8 + 88;
    public static final int DECO_BED_FT          =   9 + 88;
    public static final int DECO_FENCE_GR        =  10 + 88;
    public static final int HALF_SANDSTONE       =   0 + 99;
    public static final int HALF_SLATE           =   1 + 99;
    public static final int HALF_BRICK           =   2 + 99;
    public static final int HALF_WOOD            =   3 + 99;
    public static final int BASE_SANDSTONE       =   4 + 99;
    public static final int BASE_SLATE           =   5 + 99;
    public static final int BASE_WOOD            =   6 + 99;
    public static final int BASE_THATCHED        =   7 + 99;
    public static final int HALF_DECO_BED_GR     =   8 + 99;
    public static final int HALF_DECO_BED_FT     =   9 + 99;
    public static final int DECO_FENCE_FT        =  10 + 99;

    public static final int KNIGHT      = 0;
    public static final int LANCER      = 1;
    public static final int ARCHER      = 2;
    public static final int ROGUE       = 3;
    public static final int ORC_BRUTE   = 4;
    public static final int ORC_GUARD   = 5;
    public static final int ORC_BUTCHER = 6;
    public static final int ORC_SLINGER = 7;
    public static final int SHADOW      = 8;
    public static final int WIZARD      = 9;
    public static final int DEMON       = 10;
    public static final int CROSSBOWMAN = 11;
    public static final int MITE        = 12;
    public static final int FISH        = 13;
    public static final int BAT         = 14;
    public static final int JELLY       = 15;

    /** Special index; not an animation. */
    public static final int TOMBSTONE   = 128;
    /** Special index; not an animation. */
    public static final int UNKNOWN     = 129;

    public static final ObjectIntMap<String> TILES = new ObjectIntMap<>(128);

    public static final String[] entityNames = {
        "Pandora", "Pandora_Attack", "Eye_Tyrant", "Eye_Tyrant_Attack", "Box_Closed", "Box_Open"
    };
    public static final NumberedSet<String> ENTITIES = new NumberedSet<>(entityNames);
    public static final IntSet UNIT_VOXELS;
    public static final IntSet HALF_VOXELS;
    public static final IntSet BASE_VOXELS;
    public static final IntSet HALF_COVERS;
    public static final IntSet BASE_COVERS;
    public static final IntSet DECORATIONS;
    public static final IntSet HALF_DECORATIONS;
    public static final IntSet UNIT_ANY;
    public static final IntSet HALF_ANY;
    public static final IntSet BASE_ANY;

    /**
     * This maps indices of rotation-dependent tiles to their appropriate rotated versions when rotated 0 degrees, 90
     * degrees counterclockwise, 180 degrees, and 270 degrees counterclockwise. The rotated versions are also in the
     * form of indices, each an array of exactly 4 int indices (for the listed rotations, in that order).
     */
    public static final IntMap<int[]> ROTATIONS = new IntMap<>(58);

    static {
        TILES.put("dirt"                , DIRT                );
        TILES.put("grass"               , GRASS               );
        TILES.put("basalt"              , BASALT              );
        TILES.put("sand"                , SAND                );
        TILES.put("half cover water fgh", HALF_COVER_WATER_FGH);
        TILES.put("half cover water fh" , HALF_COVER_WATER_FH );
        TILES.put("half cover water gh" , HALF_COVER_WATER_GH );
        TILES.put("half cover water h"  , HALF_COVER_WATER_H  );
        TILES.put("deco rocks"          , DECO_ROCKS          );
        TILES.put("deco boulder"        , DECO_BOULDER        );
        TILES.put("deco cattail"        , DECO_CATTAIL        );
        TILES.put("half dirt"           , HALF_DIRT           );
        TILES.put("half grass"          , HALF_GRASS          );
        TILES.put("half basalt"         , HALF_BASALT         );
        TILES.put("half sand"           , HALF_SAND           );
        TILES.put("base cover water fgh", BASE_COVER_WATER_FGH);
        TILES.put("base cover water fh" , BASE_COVER_WATER_FH );
        TILES.put("base cover water gh" , BASE_COVER_WATER_GH );
        TILES.put("base cover water h"  , BASE_COVER_WATER_H  );
        TILES.put("deco bush"           , DECO_BUSH           );
        TILES.put("deco stump"          , DECO_STUMP          );
        TILES.put("deco flame"          , DECO_FLAME          );
        TILES.put("snow"                , SNOW                );
        TILES.put("ice"                 , ICE                 );
        TILES.put("lava"                , LAVA                );
        TILES.put("dry"                 , DRY                 );
        TILES.put("half cover swamp fgh", HALF_COVER_SWAMP_FGH);
        TILES.put("half cover swamp fh" , HALF_COVER_SWAMP_FH );
        TILES.put("half cover swamp gh" , HALF_COVER_SWAMP_GH );
        TILES.put("half cover swamp h"  , HALF_COVER_SWAMP_H  );
        TILES.put("deco bundle"         , DECO_BUNDLE         );
        TILES.put("deco hedge"          , DECO_HEDGE          );
        TILES.put("deco crystals"       , DECO_CRYSTALS       );
        TILES.put("half snow"           , HALF_SNOW           );
        TILES.put("half ice"            , HALF_ICE            );
        TILES.put("half lava"           , HALF_LAVA           );
        TILES.put("half dry"            , HALF_DRY            );
        TILES.put("base cover swamp fgh", BASE_COVER_SWAMP_FGH);
        TILES.put("base cover swamp fh" , BASE_COVER_SWAMP_FH );
        TILES.put("base cover swamp gh" , BASE_COVER_SWAMP_GH );
        TILES.put("base cover swamp h"  , BASE_COVER_SWAMP_H  );
        TILES.put("deco log"            , DECO_LOG            );
        TILES.put("deco cactus"         , DECO_CACTUS         );
        TILES.put("deco spike"          , DECO_SPIKE          );
        TILES.put("path grass gr"       , PATH_GRASS_GR       );
        TILES.put("path grass ft"       , PATH_GRASS_FT       );
        TILES.put("path grass ftr"      , PATH_GRASS_FTR      );
        TILES.put("path grass fgt"      , PATH_GRASS_FGT      );
        TILES.put("path grass fgtr"     , PATH_GRASS_FGTR     );
        TILES.put("path grass gtr"      , PATH_GRASS_GTR      );
        TILES.put("path grass fgr"      , PATH_GRASS_FGR      );
        TILES.put("path grass fg"       , PATH_GRASS_FG       );
        TILES.put("path grass gt"       , PATH_GRASS_GT       );
        TILES.put("path grass tr"       , PATH_GRASS_TR       );
        TILES.put("path grass fr"       , PATH_GRASS_FR       );
        TILES.put("half path grass gr"  , HALF_PATH_GRASS_GR  );
        TILES.put("half path grass ft"  , HALF_PATH_GRASS_FT  );
        TILES.put("half path grass ftr" , HALF_PATH_GRASS_FTR );
        TILES.put("half path grass fgt" , HALF_PATH_GRASS_FGT );
        TILES.put("half path grass fgtr", HALF_PATH_GRASS_FGTR);
        TILES.put("half path grass gtr" , HALF_PATH_GRASS_GTR );
        TILES.put("half path grass fgr" , HALF_PATH_GRASS_FGR );
        TILES.put("half path grass fg"  , HALF_PATH_GRASS_FG  );
        TILES.put("half path grass gt"  , HALF_PATH_GRASS_GT  );
        TILES.put("half path grass tr"  , HALF_PATH_GRASS_TR  );
        TILES.put("half path grass fr"  , HALF_PATH_GRASS_FR  );
        TILES.put("path dry gr"         , PATH_DRY_GR         );
        TILES.put("path dry ft"         , PATH_DRY_FT         );
        TILES.put("path dry ftr"        , PATH_DRY_FTR        );
        TILES.put("path dry fgt"        , PATH_DRY_FGT        );
        TILES.put("path dry fgtr"       , PATH_DRY_FGTR       );
        TILES.put("path dry gtr"        , PATH_DRY_GTR        );
        TILES.put("path dry fgr"        , PATH_DRY_FGR        );
        TILES.put("path dry fg"         , PATH_DRY_FG         );
        TILES.put("path dry gt"         , PATH_DRY_GT         );
        TILES.put("path dry tr"         , PATH_DRY_TR         );
        TILES.put("path dry fr"         , PATH_DRY_FR         );
        TILES.put("half path dry gr"    , HALF_PATH_DRY_GR    );
        TILES.put("half path dry ft"    , HALF_PATH_DRY_FT    );
        TILES.put("half path dry ftr"   , HALF_PATH_DRY_FTR   );
        TILES.put("half path dry fgt"   , HALF_PATH_DRY_FGT   );
        TILES.put("half path dry fgtr"  , HALF_PATH_DRY_FGTR  );
        TILES.put("half path dry gtr"   , HALF_PATH_DRY_GTR   );
        TILES.put("half path dry fgr"   , HALF_PATH_DRY_FGR   );
        TILES.put("half path dry fg"    , HALF_PATH_DRY_FG    );
        TILES.put("half path dry gt"    , HALF_PATH_DRY_GT    );
        TILES.put("half path dry tr"    , HALF_PATH_DRY_TR    );
        TILES.put("half path dry fr"    , HALF_PATH_DRY_FR    );
        TILES.put("sandstone"           , SANDSTONE           );
        TILES.put("slate"               , SLATE               );
        TILES.put("brick"               , BRICK               );
        TILES.put("wood"                , WOOD                );
        TILES.put("cap sandstone"       , CAP_SANDSTONE       );
        TILES.put("cap slate"           , CAP_SLATE           );
        TILES.put("cap wood"            , CAP_WOOD            );
        TILES.put("cap thatched"        , CAP_THATCHED        );
        TILES.put("deco bed gr"         , DECO_BED_GR         );
        TILES.put("deco bed ft"         , DECO_BED_FT         );
        TILES.put("deco fence gr"       , DECO_FENCE_GR       );
        TILES.put("half sandstone"      , HALF_SANDSTONE      );
        TILES.put("half slate"          , HALF_SLATE          );
        TILES.put("half brick"          , HALF_BRICK          );
        TILES.put("half wood"           , HALF_WOOD           );
        TILES.put("base sandstone"      , BASE_SANDSTONE      );
        TILES.put("base slate"          , BASE_SLATE          );
        TILES.put("base wood"           , BASE_WOOD           );
        TILES.put("base thatched"       , BASE_THATCHED       );
        TILES.put("half deco bed gr"    , HALF_DECO_BED_GR    );
        TILES.put("half deco bed ft"    , HALF_DECO_BED_FT    );
        TILES.put("deco fence ft"       , DECO_FENCE_FT       );

        ROTATIONS.put(HALF_COVER_WATER_FGH, new int[]{HALF_COVER_WATER_FGH, HALF_COVER_WATER_GH, HALF_COVER_WATER_H, HALF_COVER_WATER_FH, });
        ROTATIONS.put(HALF_COVER_WATER_FH, new int[]{HALF_COVER_WATER_FH, HALF_COVER_WATER_GH, HALF_COVER_WATER_H, HALF_COVER_WATER_H, });
        ROTATIONS.put(HALF_COVER_WATER_GH, new int[]{HALF_COVER_WATER_GH, HALF_COVER_WATER_H, HALF_COVER_WATER_H, HALF_COVER_WATER_FH, });
        ROTATIONS.put(BASE_COVER_WATER_FGH, new int[]{BASE_COVER_WATER_FGH, BASE_COVER_WATER_GH, BASE_COVER_WATER_H, BASE_COVER_WATER_FH, });
        ROTATIONS.put(BASE_COVER_WATER_FH, new int[]{BASE_COVER_WATER_FH, BASE_COVER_WATER_GH, BASE_COVER_WATER_H, BASE_COVER_WATER_H, });
        ROTATIONS.put(BASE_COVER_WATER_GH, new int[]{BASE_COVER_WATER_GH, BASE_COVER_WATER_H, BASE_COVER_WATER_H, BASE_COVER_WATER_FH, });
        ROTATIONS.put(HALF_COVER_SWAMP_FGH, new int[]{HALF_COVER_SWAMP_FGH, HALF_COVER_SWAMP_GH, HALF_COVER_SWAMP_H, HALF_COVER_SWAMP_FH, });
        ROTATIONS.put(HALF_COVER_SWAMP_FH, new int[]{HALF_COVER_SWAMP_FH, HALF_COVER_SWAMP_GH, HALF_COVER_SWAMP_H, HALF_COVER_SWAMP_H, });
        ROTATIONS.put(HALF_COVER_SWAMP_GH, new int[]{HALF_COVER_SWAMP_GH, HALF_COVER_SWAMP_H, HALF_COVER_SWAMP_H, HALF_COVER_SWAMP_FH, });
        ROTATIONS.put(BASE_COVER_SWAMP_FGH, new int[]{BASE_COVER_SWAMP_FGH, BASE_COVER_SWAMP_GH, BASE_COVER_SWAMP_H, BASE_COVER_SWAMP_FH, });
        ROTATIONS.put(BASE_COVER_SWAMP_FH, new int[]{BASE_COVER_SWAMP_FH, BASE_COVER_SWAMP_GH, BASE_COVER_SWAMP_H, BASE_COVER_SWAMP_H, });
        ROTATIONS.put(BASE_COVER_SWAMP_GH, new int[]{BASE_COVER_SWAMP_GH, BASE_COVER_SWAMP_H, BASE_COVER_SWAMP_H, BASE_COVER_SWAMP_FH, });
        ROTATIONS.put(PATH_GRASS_GR, new int[]{PATH_GRASS_GR, PATH_GRASS_FT, PATH_GRASS_GR, PATH_GRASS_FT, });
        ROTATIONS.put(PATH_GRASS_FT, new int[]{PATH_GRASS_FT, PATH_GRASS_GR, PATH_GRASS_FT, PATH_GRASS_GR, });
        ROTATIONS.put(PATH_GRASS_FTR, new int[]{PATH_GRASS_FTR, PATH_GRASS_FGR, PATH_GRASS_FGT, PATH_GRASS_GTR, });
        ROTATIONS.put(PATH_GRASS_FGR, new int[]{PATH_GRASS_FGR, PATH_GRASS_FGT, PATH_GRASS_GTR, PATH_GRASS_FTR, });
        ROTATIONS.put(PATH_GRASS_FGT, new int[]{PATH_GRASS_FGT, PATH_GRASS_GTR, PATH_GRASS_FTR, PATH_GRASS_FGR, });
        ROTATIONS.put(PATH_GRASS_GTR, new int[]{PATH_GRASS_GTR, PATH_GRASS_FTR, PATH_GRASS_FGR, PATH_GRASS_FGT, });
        ROTATIONS.put(PATH_GRASS_FG, new int[]{PATH_GRASS_FG, PATH_GRASS_GT, PATH_GRASS_TR, PATH_GRASS_FR, });
        ROTATIONS.put(PATH_GRASS_GT, new int[]{PATH_GRASS_GT, PATH_GRASS_TR, PATH_GRASS_FR, PATH_GRASS_FG, });
        ROTATIONS.put(PATH_GRASS_TR, new int[]{PATH_GRASS_TR, PATH_GRASS_FR, PATH_GRASS_FG, PATH_GRASS_GT, });
        ROTATIONS.put(PATH_GRASS_FR, new int[]{PATH_GRASS_FR, PATH_GRASS_FG, PATH_GRASS_GT, PATH_GRASS_TR, });
        ROTATIONS.put(HALF_PATH_GRASS_GR, new int[]{HALF_PATH_GRASS_GR, HALF_PATH_GRASS_FT, HALF_PATH_GRASS_GR, HALF_PATH_GRASS_FT, });
        ROTATIONS.put(HALF_PATH_GRASS_FT, new int[]{HALF_PATH_GRASS_FT, HALF_PATH_GRASS_GR, HALF_PATH_GRASS_FT, HALF_PATH_GRASS_GR, });
        ROTATIONS.put(HALF_PATH_GRASS_FTR, new int[]{HALF_PATH_GRASS_FTR, HALF_PATH_GRASS_FGR, HALF_PATH_GRASS_FGT, HALF_PATH_GRASS_GTR, });
        ROTATIONS.put(HALF_PATH_GRASS_FGR, new int[]{HALF_PATH_GRASS_FGR, HALF_PATH_GRASS_FGT, HALF_PATH_GRASS_GTR, HALF_PATH_GRASS_FTR, });
        ROTATIONS.put(HALF_PATH_GRASS_FGT, new int[]{HALF_PATH_GRASS_FGT, HALF_PATH_GRASS_GTR, HALF_PATH_GRASS_FTR, HALF_PATH_GRASS_FGR, });
        ROTATIONS.put(HALF_PATH_GRASS_GTR, new int[]{HALF_PATH_GRASS_GTR, HALF_PATH_GRASS_FTR, HALF_PATH_GRASS_FGR, HALF_PATH_GRASS_FGT, });
        ROTATIONS.put(HALF_PATH_GRASS_FG, new int[]{HALF_PATH_GRASS_FG, HALF_PATH_GRASS_GT, HALF_PATH_GRASS_TR, HALF_PATH_GRASS_FR, });
        ROTATIONS.put(HALF_PATH_GRASS_GT, new int[]{HALF_PATH_GRASS_GT, HALF_PATH_GRASS_TR, HALF_PATH_GRASS_FR, HALF_PATH_GRASS_FG, });
        ROTATIONS.put(HALF_PATH_GRASS_TR, new int[]{HALF_PATH_GRASS_TR, HALF_PATH_GRASS_FR, HALF_PATH_GRASS_FG, HALF_PATH_GRASS_GT, });
        ROTATIONS.put(HALF_PATH_GRASS_FR, new int[]{HALF_PATH_GRASS_FR, HALF_PATH_GRASS_FG, HALF_PATH_GRASS_GT, HALF_PATH_GRASS_TR, });
        ROTATIONS.put(PATH_DRY_GR, new int[]{PATH_DRY_GR, PATH_DRY_FT, PATH_DRY_GR, PATH_DRY_FT, });
        ROTATIONS.put(PATH_DRY_FT, new int[]{PATH_DRY_FT, PATH_DRY_GR, PATH_DRY_FT, PATH_DRY_GR, });
        ROTATIONS.put(PATH_DRY_FTR, new int[]{PATH_DRY_FTR, PATH_DRY_FGR, PATH_DRY_FGT, PATH_DRY_GTR, });
        ROTATIONS.put(PATH_DRY_FGR, new int[]{PATH_DRY_FGR, PATH_DRY_FGT, PATH_DRY_GTR, PATH_DRY_FTR, });
        ROTATIONS.put(PATH_DRY_FGT, new int[]{PATH_DRY_FGT, PATH_DRY_GTR, PATH_DRY_FTR, PATH_DRY_FGR, });
        ROTATIONS.put(PATH_DRY_GTR, new int[]{PATH_DRY_GTR, PATH_DRY_FTR, PATH_DRY_FGR, PATH_DRY_FGT, });
        ROTATIONS.put(PATH_DRY_FG, new int[]{PATH_DRY_FG, PATH_DRY_GT, PATH_DRY_TR, PATH_DRY_FR, });
        ROTATIONS.put(PATH_DRY_GT, new int[]{PATH_DRY_GT, PATH_DRY_TR, PATH_DRY_FR, PATH_DRY_FG, });
        ROTATIONS.put(PATH_DRY_TR, new int[]{PATH_DRY_TR, PATH_DRY_FR, PATH_DRY_FG, PATH_DRY_GT, });
        ROTATIONS.put(PATH_DRY_FR, new int[]{PATH_DRY_FR, PATH_DRY_FG, PATH_DRY_GT, PATH_DRY_TR, });
        ROTATIONS.put(HALF_PATH_DRY_GR, new int[]{HALF_PATH_DRY_GR, HALF_PATH_DRY_FT, HALF_PATH_DRY_GR, HALF_PATH_DRY_FT, });
        ROTATIONS.put(HALF_PATH_DRY_FT, new int[]{HALF_PATH_DRY_FT, HALF_PATH_DRY_GR, HALF_PATH_DRY_FT, HALF_PATH_DRY_GR, });
        ROTATIONS.put(HALF_PATH_DRY_FTR, new int[]{HALF_PATH_DRY_FTR, HALF_PATH_DRY_FGR, HALF_PATH_DRY_FGT, HALF_PATH_DRY_GTR, });
        ROTATIONS.put(HALF_PATH_DRY_FGR, new int[]{HALF_PATH_DRY_FGR, HALF_PATH_DRY_FGT, HALF_PATH_DRY_GTR, HALF_PATH_DRY_FTR, });
        ROTATIONS.put(HALF_PATH_DRY_FGT, new int[]{HALF_PATH_DRY_FGT, HALF_PATH_DRY_GTR, HALF_PATH_DRY_FTR, HALF_PATH_DRY_FGR, });
        ROTATIONS.put(HALF_PATH_DRY_GTR, new int[]{HALF_PATH_DRY_GTR, HALF_PATH_DRY_FTR, HALF_PATH_DRY_FGR, HALF_PATH_DRY_FGT, });
        ROTATIONS.put(HALF_PATH_DRY_FG, new int[]{HALF_PATH_DRY_FG, HALF_PATH_DRY_GT, HALF_PATH_DRY_TR, HALF_PATH_DRY_FR, });
        ROTATIONS.put(HALF_PATH_DRY_GT, new int[]{HALF_PATH_DRY_GT, HALF_PATH_DRY_TR, HALF_PATH_DRY_FR, HALF_PATH_DRY_FG, });
        ROTATIONS.put(HALF_PATH_DRY_TR, new int[]{HALF_PATH_DRY_TR, HALF_PATH_DRY_FR, HALF_PATH_DRY_FG, HALF_PATH_DRY_GT, });
        ROTATIONS.put(HALF_PATH_DRY_FR, new int[]{HALF_PATH_DRY_FR, HALF_PATH_DRY_FG, HALF_PATH_DRY_GT, HALF_PATH_DRY_TR, });
        ROTATIONS.put(DECO_BED_GR, new int[]{DECO_BED_GR, DECO_BED_FT, DECO_BED_GR, DECO_BED_FT, });
        ROTATIONS.put(DECO_BED_FT, new int[]{DECO_BED_FT, DECO_BED_GR, DECO_BED_FT, DECO_BED_GR, });
        ROTATIONS.put(HALF_DECO_BED_GR, new int[]{HALF_DECO_BED_GR, HALF_DECO_BED_FT, HALF_DECO_BED_GR, HALF_DECO_BED_FT, });
        ROTATIONS.put(HALF_DECO_BED_FT, new int[]{HALF_DECO_BED_FT, HALF_DECO_BED_GR, HALF_DECO_BED_FT, HALF_DECO_BED_GR, });
        ROTATIONS.put(DECO_FENCE_GR, new int[]{DECO_FENCE_GR, DECO_FENCE_FT, DECO_FENCE_GR, DECO_FENCE_FT, });
        ROTATIONS.put(DECO_FENCE_FT, new int[]{DECO_FENCE_FT, DECO_FENCE_GR, DECO_FENCE_FT, DECO_FENCE_GR, });

        UNIT_VOXELS = new IntSet(128);
        HALF_VOXELS = new IntSet(64);
        BASE_VOXELS = new IntSet(64);
        HALF_COVERS = new IntSet(16);
        BASE_COVERS = new IntSet(16);
        DECORATIONS = new IntSet(32);
        HALF_DECORATIONS = new IntSet(4);
        UNIT_ANY = new IntSet(64);
        HALF_ANY = new IntSet(64);
        BASE_ANY = new IntSet(64);

        UNIT_VOXELS.addAll(TILES.values().toArray());

        for(ObjectIntMap.Entry<String> e : TILES.entries()){
            if(e.key.contains("half")){
                if(e.key.contains("cover")) HALF_COVERS.add(e.value);
                else if(!e.key.contains("deco")) HALF_VOXELS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
            if(e.key.contains("base")){
                if(e.key.contains("cover")) BASE_COVERS.add(e.value);
                else BASE_VOXELS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
            if(e.key.contains("deco")){
                if(e.key.contains("half")) HALF_DECORATIONS.add(e.value);
                else DECORATIONS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
        }
        UNIT_ANY.addAll(UNIT_VOXELS);
        UNIT_ANY.addAll(DECORATIONS);
        HALF_ANY.addAll(HALF_VOXELS);
        HALF_ANY.addAll(HALF_COVERS);
        HALF_ANY.addAll(HALF_DECORATIONS);
        BASE_ANY.addAll(BASE_VOXELS);
        BASE_ANY.addAll(BASE_COVERS);
    }
}
