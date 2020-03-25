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

package com.github.tommyettinger.demos;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * A viewport that scales the world using {@link Scaling}, but limits the scaling to integer multiples or simple halving
 * if the zoom would be 0x. Handy for keeping a pixelated look on high-DPI screens.
 * <p>
 * {@link Scaling#fit} keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the
 * remaining space.
 * <p>
 * {@link Scaling#fill} keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off
 * screen, potentially in both directions regardless of whether fill, fillX, or fillY is used).
 * <p>
 * {@link Scaling#stretch} (NOT RECOMMENDED) does not keep the aspect ratio, the world is scaled to take the whole screen. It is not pixel perfect.
 * <p>
 * {@link Scaling#none} keeps the aspect ratio by using a fixed size world (the world may not fill the screen or some of the world
 * may be off screen).
 *
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class PixelPerfectViewport extends Viewport {

    private Scaling scaling;

    /**
     * Creates a new viewport using a new {@link OrthographicCamera}.
     */
    public PixelPerfectViewport(Scaling scaling, float worldWidth, float worldHeight) {
        this(scaling, worldWidth, worldHeight, new OrthographicCamera());
    }

    public PixelPerfectViewport(Scaling scaling, float worldWidth, float worldHeight, Camera camera) {
        this.scaling = scaling;
        setWorldSize(worldWidth, worldHeight);
        setCamera(camera);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        Vector2 scaled = scaling.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);
        float worldWidth = getWorldWidth(), worldHeight = getWorldHeight();

        int viewportWidth = 0;
        int viewportHeight = 0;

        switch (scaling) {
            case fit: {
                float screenRatio = screenHeight / screenWidth;
                float worldRatio = worldHeight / worldWidth;
                float scale = (int) (screenRatio > worldRatio ? screenWidth / worldWidth : screenHeight / worldHeight);
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                break;
            }
            case fill: {
                float screenRatio = screenHeight / screenWidth;
                float worldRatio = worldHeight / worldWidth;
                float scale = (int) Math.ceil(screenRatio < worldRatio ? screenWidth / worldWidth : screenHeight / worldHeight);
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                break;
            }
            case fillX: {
                float scale = (int) Math.ceil(screenWidth / worldWidth);
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                break;
            }
            case fillY: {
                float scale = (int) Math.ceil(screenHeight / worldHeight);
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                break;
            }
            case stretch:
                viewportWidth = screenWidth;
                viewportHeight = screenHeight;
                break;
            case stretchX:
                viewportWidth = screenWidth;
                viewportHeight = (int) worldHeight;
                break;
            case stretchY:
                viewportWidth = (int) worldWidth;
                viewportHeight = screenHeight;
                break;
            case none:
            default:
                viewportWidth = (int) worldWidth;
                viewportHeight = (int) worldHeight;
                break;
        }
        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);

        apply(centerCamera);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }
}