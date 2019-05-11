/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.*;

/**
 * Shows a basic red diamond and some text until the user increases
 * the size of the window; then it shows a wood texture in the margins
 * that moves around over time.
 * 
 * Based on a libGDX test and uses a texture from 
 * <a href="http://www.mb3d.co.uk/mb3d/maxTextures_Home_-_Free_Seamless_and_Tileable_High_Res_Textures.html">MaxTextures</a>,
 * which is copyright www.maxtextures.com but made freely available under their terms of use.
 */
public class ShaderBar extends ApplicationAdapter {
    FitViewport viewport;
    String name;

    private SpriteBatch batch;
    private Texture texture, wood;
    private BitmapFont font;
    private OrthographicCamera camera;
    private ShaderProgram defaultShader, shader;

    public void create () {
        font = new BitmapFont();
        font.setColor(0, 0, 0, 1);
        defaultShader = SpriteBatch.createDefaultShader();
        shader = new ShaderProgram(defaultShader.getVertexShaderSource(),
        "#ifdef GL_ES\n" +
                "    precision highp float;\n" +
                "#endif\n" +
                "\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform float delta;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_texCoords;\n" +
                "    \n" +
                "    uv.y += 4.0 * sin(delta);\n" +
                "    uv.x += 3.0 * cos(delta);\n" +
                "    \n" +
                "    vec3 color = texture2D(u_texture, uv).rgb;\n" +
                "    gl_FragColor = vec4(color, 1.0);\n" +
                "}");
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        texture = new Texture(pixmap);
        wood = new Texture(Gdx.files.internal("wood.jpg"), Pixmap.Format.RGBA8888, false);
        wood.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        camera.position.set(100, 100, 0);
        camera.update();

        int minWorldWidth = 640;
        int minWorldHeight = 480;
        int maxWorldWidth = 800;
        int maxWorldHeight = 480;
        viewport = new FitViewport(minWorldWidth, minWorldHeight, camera);

        name = "FitViewport";

        Gdx.input.setInputProcessor(new InputAdapter() {
            public boolean keyDown (int keycode) {
                if (keycode == Input.Keys.Q) {
                    Gdx.app.exit();
                }
                return false;
            }
        });
    }

    public void render () {
        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);
        batch.setShader(defaultShader);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        // draw a white background so we are able to see the black bars
        batch.setColor(1, 1, 1, 1);
        batch.draw(texture, -4096, -4096, 4096, 4096, 8192, 8192, 1, 1, 0, 0, 0, 16, 16, false, false);

        batch.setColor(1, 0, 0, 1);
        batch.draw(texture, 150, 100, 16, 16, 32, 32, 1, 1, 45, 0, 0, 16, 16, false, false);

        font.draw(batch, viewport.getClass().getSimpleName(), 150, 100);
        batch.end();

            // This shows how to set the viewport to the whole screen and draw within the black bars.
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();
            HdpiUtils.glViewport(0, 0, screenWidth, screenHeight);
            batch.getProjectionMatrix().idt().setToOrtho2D(0, 0, screenWidth, screenHeight);
            batch.getTransformMatrix().idt();
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.setShader(shader);
//            shader.begin();
            shader.setUniformf("delta", (System.currentTimeMillis() & 0xFFFFFFL) * 0x1p-14f);
            Gdx.gl20.glActiveTexture(0);
            wood.bind();
            shader.setUniformi("u_texture", 0);
            float leftGutterWidth = viewport.getLeftGutterWidth();
            if (leftGutterWidth > 0) {
                batch.draw(wood, 0, 0, leftGutterWidth, screenHeight);
                batch.draw(wood, viewport.getRightGutterX(), 0, viewport.getRightGutterWidth(), screenHeight);
            }
            float bottomGutterHeight = viewport.getBottomGutterHeight();
            if (bottomGutterHeight > 0) {
                batch.draw(texture, 0, 0, screenWidth, bottomGutterHeight);
                batch.draw(texture, 0, viewport.getTopGutterY(), screenWidth, viewport.getTopGutterHeight());
            }
//            shader.end();
            batch.end();
            viewport.update(screenWidth, screenHeight, true); // Restore viewport.
    }

    public void resize (int width, int height) {
        System.out.println(name);
        viewport.update(width, height);
    }

    public void dispose () {
        texture.dispose();
        batch.dispose();
    }
    static public Array<String> getViewportNames () {
        Array<String> names = new Array<String>();
        names.add("StretchViewport");
        names.add("FillViewport");
        names.add("FitViewport");
        names.add("ExtendViewport: no max");
        names.add("ExtendViewport: max");
        names.add("ScreenViewport: 1:1");
        names.add("ScreenViewport: 0.75:1");
        names.add("ScalingViewport: none");
        return names;
    }

    static public Array<Viewport> getViewports (Camera camera) {
        int minWorldWidth = 640;
        int minWorldHeight = 480;
        int maxWorldWidth = 800;
        int maxWorldHeight = 480;

        Array<Viewport> viewports = new Array<Viewport>();
        viewports.add(new StretchViewport(minWorldWidth, minWorldHeight, camera));
        viewports.add(new FillViewport(minWorldWidth, minWorldHeight, camera));
        viewports.add(new ExtendViewport(minWorldWidth, minWorldHeight, camera));
        viewports.add(new ExtendViewport(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, camera));
        viewports.add(new ScreenViewport(camera));

        ScreenViewport screenViewport = new ScreenViewport(camera);
        screenViewport.setUnitsPerPixel(0.75f);
        viewports.add(screenViewport);

        viewports.add(new ScalingViewport(Scaling.none, minWorldWidth, minWorldHeight, camera));
        return viewports;
    }
}