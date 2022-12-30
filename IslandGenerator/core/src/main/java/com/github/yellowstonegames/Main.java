package com.github.yellowstonegames;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import make.some.noise.Noise;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private Noise noise = new Noise(12345, 1f/300f, Noise.FOAM_FRACTAL, 5);

    private static final int width = 512, height = 512;
    private static final float halfWidth = width * 0.5f, halfHeight = height * 0.5f;
    private static final float distModifier = 1.25f / (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);

    private static final Color[] colors = {new Color(0x002266FF), new Color(0x2299F0FF),
        new Color(0xFFE090FF), new Color(0xC0D03FFF), new Color(0x389028FF),
        new Color(0xD0D0D0FF), new Color(0xE0FFFFFF),
        new Color(0xE0FFFFFF), new Color(0xF0FFFFFF), };
    private static final float[] minLevels = {-3f, -0.2f, -0.1f, 0.0f, 0.2f, 0.4f, 0.55f, 0.8f, 1.001f};

    private Pixmap pm;
    private Texture texture;
    private Viewport view;
    private SpriteBatch batch;
    private transient final Color tmp = new Color();

    @Override
    public void create() {
        noise.setSeed((int)System.currentTimeMillis());
        batch = new SpriteBatch();
        view = new StretchViewport(width, height);
        pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        texture = new Texture(pm);
        regenerateIsland();
    }

    public boolean regenerateIsland() {
        boolean landHo = false; // Yarr, me mateys, we be coming aground!
        for (int r = 0; r < 100 && !landHo; r++) {
            noise.setSeed(noise.getSeed() + 1);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    float falloff = -(float) Math.pow(Vector2.len(x - halfWidth, y - halfHeight) * distModifier + 0.125f, 1.8f);
                    float n = noise.getConfiguredNoise(x, y) + falloff;
                    int index = 1;
                    for (int i = 0; i < minLevels.length; i++) {
                        if (n < minLevels[index]) break;
                        index++;
                    }
                    landHo |= index >= 3;
                    float nrm = Interpolation.smoother.apply(MathUtils.norm(minLevels[index - 1], minLevels[index], n));
                    tmp.set(colors[index - 1]).lerp(colors[index], nrm);
                    pm.setColor(tmp);
                    pm.drawPixel(x, y);
                }
            }
        }
        if(!landHo){
            pm.setColor(Color.RED);
            pm.fill();
        }
        texture.draw(pm, 0, 0);
        return landHo;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
        view.apply(true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.BLACK);
        Camera camera = view.getCamera();
        camera.update();

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            regenerateIsland();
        else if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
            noise.setSeed(noise.getSeed() - 2);
            regenerateIsland();
        }
        else if(Gdx.input.isKeyJustPressed(Input.Keys.SLASH)){
            noise.setSeed(MathUtils.random.nextInt());
            regenerateIsland();
        }

        Batch batch = this.batch;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(texture, 0, 0);
        batch.end();

    }
}
