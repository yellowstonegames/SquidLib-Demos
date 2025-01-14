package com.squidpony.demo;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.NumberUtils;
import jagd.MimicWFC;
import jagd.RNG;

/**
 * THE TMX MAP USED HERE HAS SOME SPECIAL QUALITIES!
 * It is surrounded by water, and when there are duplicate tiles in the tileset, it chooses a tile and only uses
 * that one (so if there are 5 identical grass tiles, it will only use the first). There is an exception for the
 * roof of a house; different tiles are used for the roof above the door, to the left of the door, and to the
 * right of the door, and this seems to help increase the placement likelihood for a house.
 */
public class WFCDemo extends ApplicationAdapter {

    private RNG rng;
    /** In number of cells */
    private static final int gridWidth = 64;
    /** In number of cells */
    private static final int gridHeight = 50;

    /** The pixel width of a cell */
    private static final int cellWidth = 16;
    /** The pixel height of a cell */
    private static final int cellHeight = 16;
    private TiledMap originalMap;
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer renderer;

    @Override
    public void create () {
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        rng = new RNG(123456789);
        System.out.println(rng.state);

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        SpriteBatch batch = new SpriteBatch();
        OrthographicCamera camera = new OrthographicCamera(gridWidth * cellWidth, gridHeight * cellHeight);
        //viewport.setScreenBounds(gridWidth * cellWidth / -4, gridHeight * cellHeight / -4, gridWidth * cellWidth, gridHeight * cellHeight);

        TmxMapLoader loader = new TmxMapLoader();
        originalMap = loader.load("testingTerrainIsland.tmx");
        remake();
        renderer = new OrthogonalTiledMapRenderer(tiledMap, batch);
        //camera.translate(gridWidth * cellWidth * -0.5f, gridHeight * cellHeight * -0.5f);
        renderer.setView(camera);

        InputProcessor input = new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                switch (keycode) {
                    case Input.Keys.Q:
                    case Input.Keys.ESCAPE:
                        Gdx.app.exit();
                        break;
                    case Input.Keys.PRINT_SCREEN:
                        System.out.println("Printing screen.");
                        break;
                    case Input.Keys.R:
                        System.out.println("Now using initial state " +
                                (rng.state = NumberUtils.doubleToLongBits(Math.random()) ^ NumberUtils.doubleToLongBits(Math.random()) >>> 32));
                        // FALLTHROUGH
                    default:
                        remake();
                        renderer.setMap(tiledMap);
                        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                }
                return true;
            }
        };
        Gdx.input.setInputProcessor(input);
    }
    public void remake()
    {
        TiledMapTileLayer originalLayer = (TiledMapTileLayer) originalMap.getLayers().get(0);

        int[][] grid = new int[originalLayer.getWidth()][originalLayer.getHeight()];

        for (int y = 0; y < originalLayer.getHeight(); y++) {
            for (int x = 0; x < originalLayer.getWidth(); x++) {
                grid[x][y] = originalLayer.getCell(x, y).getTile().getId();
            }
        }

        MimicWFC wfc = new MimicWFC(grid, 2, gridWidth, gridHeight, false, false, 1, 0); // 0 refers to tile id 0, water
        int i = 0;
        while (!wfc.run(rng, 1000000)) { System.out.println((++i) + " attempts failed."); }
        int[][] grid2 = wfc.result();
        tiledMap = new TiledMap();
        tiledMap.getTileSets().addTileSet(originalMap.getTileSets().getTileSet(0));
        TiledMapTileLayer layer = new TiledMapTileLayer(gridWidth, gridHeight, cellWidth, cellHeight);
        tiledMap.getLayers().add(layer);
        for (int y = 0; y < grid2[0].length; y++) {
            for (int x = 0; x < grid2.length; x++) {
                TiledMapTileLayer.Cell c = new TiledMapTileLayer.Cell();
                c.setTile(tiledMap.getTileSets().getTile(grid2[x][y]));
                layer.setCell(x, y, c);
            }
        }
    }
//    public void remakeSwamp()
//    {
//        tiledMap = loader.load("buch-reduced.tmx");
//        TiledMapTileLayer ground = (TiledMapTileLayer) tiledMap.getLayers().get(0);
//        TiledMapTileLayer fringe = (TiledMapTileLayer) tiledMap.getLayers().get(1);
//
//        int[][] grid = new int[ground.getWidth()][ground.getHeight()];
//
//        for (int x = 0; x < ground.getWidth(); x++) {
//            for (int y = 0; y < ground.getHeight(); y++) {
//                TiledMapTileLayer.Cell g = ground.getCell(x, y), f = fringe.getCell(x, y);
//                grid[x][y] = (g == null ? 0xFFFF : g.getTile().getId()) << 16 | (f == null ? 0xFFFF : f.getTile().getId());
//            }
//        }
//
//        MimicWFC wfc = new MimicWFC(grid, 2, ground.getWidth(), ground.getHeight(), false, false, 1, 0);
//        int i = 0;
//        while (!wfc.run(rng, 1000000)) { System.out.println((i += 1000000) + " attempts failed."); }
//        int[][] grid2 = wfc.result();
//
//        for (int y = 0; y < grid2[0].length; y++) {
//            for (int x = 0; x < grid2.length; x++) {
//                System.out.print(StringKit.hex(grid2[x][y]));
//                System.out.print(" ");
//                if(grid2[x][y] >>> 16 != 0xFFFF)
//                {
//                    if(ground.getCell(x, y) == null)
//                    {
//                        TiledMapTileLayer.Cell c = new TiledMapTileLayer.Cell();
//                        c.setTile(tiledMap.getTileSets().getTile(grid2[x][y] >>> 16));
//                        ground.setCell(x, y, c);
//                    }
//                    else
//                        ground.getCell(x, y).setTile(tiledMap.getTileSets().getTile(grid2[x][y] >>> 16));
//                }
//                else
//                    ground.setCell(x, y, null);
//                if((grid2[x][y] & 0xFFFF) != 0xFFFF)
//                {
//                    if(fringe.getCell(x, y) == null)
//                    {
//                        TiledMapTileLayer.Cell c = new TiledMapTileLayer.Cell();
//                        c.setTile(tiledMap.getTileSets().getTile(grid2[x][y] & 0xFFFF));
//                        fringe.setCell(x, y, c);
//                    }
//                    else
//                        fringe.getCell(x, y).setTile(tiledMap.getTileSets().getTile(grid2[x][y] & 0xFFFF));
//                }
//                else
//                    fringe.setCell(x, y, null);
//            }
//            System.out.println();
//        }
//
//    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(0, 0, 0, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        tiledMap.getLayers().get(0).setOffsetX(width * -0.5f);
        tiledMap.getLayers().get(0).setOffsetY(height * 0.5f);
//        tiledMap.getLayers().get(1).setOffsetX(width * -0.5f);
//        tiledMap.getLayers().get(1).setOffsetY(height * 0.5f);
        tiledMap.getLayers().get(0).invalidateRenderOffset();
//        tiledMap.getLayers().get(1).invalidateRenderOffset();

    }
}
