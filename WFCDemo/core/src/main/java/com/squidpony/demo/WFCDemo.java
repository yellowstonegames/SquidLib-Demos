package com.squidpony.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import squidpony.squidgrid.MimicWFC;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidmath.GWTRNG;

public class WFCDemo extends ApplicationAdapter {
    public SpriteBatch batch;
    
    private GWTRNG rng;
    /** In number of cells */
    private static final int gridWidth = 50;
    /** In number of cells */
    private static final int gridHeight = 50;

    /** The pixel width of a cell */
    private static final int cellWidth = 16;
    /** The pixel height of a cell */
    private static final int cellHeight = 16;
    private SquidInput input;
    private OrthographicCamera camera;
    
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer renderer;
    private TmxMapLoader loader;
    private ScreenViewport viewport;
    @Override
    public void create () {
        // gotta have a random number generator. We can seed a GWTRNG with any long we want, or even a String.
        rng = new GWTRNG();//"Welcome to SquidLib!");
        System.out.println(rng.getState());

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        camera = new OrthographicCamera(gridWidth * cellWidth, gridHeight * cellHeight);
        viewport = new ScreenViewport(camera);
        //viewport.setScreenBounds(gridWidth * cellWidth / -4, gridHeight * cellHeight / -4, gridWidth * cellWidth, gridHeight * cellHeight);
        
        loader = new TmxMapLoader();
        remake();
        renderer = new OrthogonalTiledMapRenderer(tiledMap, batch);
        //camera.translate(gridWidth * cellWidth * -0.5f, gridHeight * cellHeight * -0.5f);
        renderer.setView(camera);

        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                    default:
                    {
                        System.out.println("REMAKING");
                        remake();
                        renderer.setMap(tiledMap);
                        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    }
                }
            }
        });
        input.setRepeatGap(Long.MAX_VALUE);
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(input);
    }
    public void remake()
    {
        tiledMap = loader.load("testingterrain.tmx");
        TiledMapTileLayer ground = (TiledMapTileLayer) tiledMap.getLayers().get(0);

        int[][] grid = new int[ground.getWidth()][ground.getHeight()];

        for (int x = 0; x < ground.getWidth(); x++) {
            for (int y = 0; y < ground.getHeight(); y++) {
                TiledMapTileLayer.Cell g = ground.getCell(x, y);
                grid[x][y] = (g == null ? 0xFFFF : g.getTile().getId());
            }
        }

        MimicWFC wfc = new MimicWFC(grid, 2, ground.getWidth(), ground.getHeight(), false, false, 1, 0);
        int i = 0;
        while (!wfc.run(rng, 1000000)) { System.out.println((i += 1000000) + " attempts failed."); }
        int[][] grid2 = wfc.result();

        for (int y = 0; y < grid2[0].length; y++) {
            for (int x = 0; x < grid2.length; x++) {
//                System.out.print(StringKit.hex(grid2[x][y]));
//                System.out.print(" ");
                if(grid2[x][y] != 0xFFFF)
                {
                    if(ground.getCell(x, y) == null)
                    {
                        TiledMapTileLayer.Cell c = new TiledMapTileLayer.Cell();
                        c.setTile(tiledMap.getTileSets().getTile(grid2[x][y]));
                        ground.setCell(x, y, c);
                    }
                    else
                        ground.getCell(x, y).setTile(tiledMap.getTileSets().getTile(grid2[x][y]));
                }
                else
                    ground.setCell(x, y, null);
            }
//            System.out.println();
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
        if(input.hasNext()) input.next();
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
