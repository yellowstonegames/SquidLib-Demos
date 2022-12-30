package com.github.yellowstonegames;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import make.some.noise.Noise;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private Noise noise = new Noise(12345, 1f/200f, Noise.SIMPLEX_FRACTAL, 5);

    private static final int width = 512, height = 512;
    private static final float halfWidth = width * 0.5f, halfHeight = height * 0.5f;
    private static final float distModifier = 1.4f / (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);

    private static final Color[] colors = {new Color(0x002266FF), new Color(0x2299F0FF),
        new Color(0xFFF08FFF), new Color(0xC0D03FFF), new Color(0x389028FF), new Color(0x777F8FFF),
        new Color(0xE0FFFFFF), new Color(0xF0FFFFFF), };
    private static final float[] minLevels = {-3f, -0.3f, 0f, 0.1f, 0.45f, 0.75f, 0.95f, 1.001f};

    private Pixmap pm;
    private Texture texture;
    private Viewport view;
    private SpriteBatch batch;

    @Override
    public void create() {
        noise.setSeed((int)System.currentTimeMillis());
        batch = new SpriteBatch();
        view = new StretchViewport(width, height);
        pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        texture = new Texture(pm);

        Color tmp = new Color();
        float minFalloff = 100;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
//                float falloff = MathUtils.cos(distModifier * Vector2.len(x - halfWidth, y - halfHeight));
                float falloff = -(float)Math.pow(Vector2.len(x - halfWidth, y - halfHeight) * distModifier + 0.125f, 1.5f);

                minFalloff = Math.min(minFalloff, falloff);
//                float n = Math.min(noise.getConfiguredNoise(x, y), falloff);
                float n = noise.getConfiguredNoise(x, y) + falloff;
                int index = 1;
                for (int i = 0; i < minLevels.length; i++) {
                    if(n < minLevels[index]) break;
                    index++;
                }
                float nrm = MathUtils.norm(minLevels[index-1], minLevels[index], n);
                if(n > 0.15f)
                    System.out.println(nrm + " from " + minLevels[index-1] + ", " + minLevels[index]+ ", " + n);
                tmp.set(colors[index-1]).lerp(colors[index], nrm);
                pm.setColor(tmp);
                pm.drawPixel(x, y);
            }
        }
        System.out.println(minFalloff);
        texture.draw(pm, 0, 0);
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

        Batch batch = this.batch;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(texture, 0, 0);
        batch.end();

    }
}
