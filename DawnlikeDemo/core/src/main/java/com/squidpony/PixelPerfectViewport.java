package com.squidpony; /*******************************************************************************
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

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * A viewport that scales the world using {@link Scaling}, but limits the scaling to integer multiples or simple halving
 * if the zoom would be 0x. Handy for keeping a pixelated look on high-DPI screens. The {@code conversionX} and {@code conversionY}
 * configurations correspond to the scaling difference from a screen pixel to a world unit.
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

    private final Scaling scaling;
    private final int conversionX, conversionY;
    private float currentScale = 1;

    /**
     * Creates a new viewport using a new {@link OrthographicCamera}.
     * The {@code conversionX} and {@code conversionY} parameters control the size in pixels of a world unit.
     * For a 16x24 pixel world unit, you would use 16 for conversionX and 24 for conversionY.
     * @param scaling which one of the predefined objects in {@link Scaling}; {@link Scaling#fill} is good
     * @param worldWidth the width of the world in world units
     * @param worldHeight the height of the world in world units
     * @param conversionX the width of a world unit in pixels, without any scaling
     * @param conversionY the height of a world unit in pixels, without any scaling
     */
    public PixelPerfectViewport(Scaling scaling, float worldWidth, float worldHeight, int conversionX, int conversionY) {
        this(scaling, worldWidth, worldHeight, conversionX, conversionY, new OrthographicCamera());
    }

    /**
     * Creates a new viewport using a new {@link OrthographicCamera}.
     * The {@code conversion} parameter controls the size in pixels of a world unit, which is always square
     * with this constructor. For a 16x16 pixel world unit, you would use 16 for conversion.
     * @param scaling which one of the predefined objects in {@link Scaling}; {@link Scaling#fill} is good
     * @param worldWidth the width of the world in world units
     * @param worldHeight the height of the world in world units
     * @param conversion the (identical) width and height of a world unit in pixels, without any scaling
     */
    public PixelPerfectViewport(Scaling scaling, float worldWidth, float worldHeight, int conversion) {
        this(scaling, worldWidth, worldHeight, conversion, conversion, new OrthographicCamera());
    }

    /**
     * Creates a new viewport using the given {@code camera}.
     * The {@code conversionX} and {@code conversionY} parameters control the size in pixels of a world unit.
     * For a 16x24 pixel world unit, you would use 16 for conversionX and 24 for conversionY.
     * @param scaling which one of the predefined objects in {@link Scaling}; {@link Scaling#fill} is good
     * @param worldWidth the width of the world in world units
     * @param worldHeight the height of the world in world units
     * @param conversionX the width of a world unit in pixels, without any scaling
     * @param conversionY the height of a world unit in pixels, without any scaling
     * @param camera an existing Camera to reuse here
     */
    public PixelPerfectViewport(Scaling scaling, float worldWidth, float worldHeight, int conversionX, int conversionY, Camera camera) {
        this.scaling = scaling;
        this.conversionX = conversionX;
        this.conversionY = conversionY;
        setWorldSize(worldWidth, worldHeight);
        setCamera(camera);
    }

    /**
     * Scaling isn't an enum anymore, so if code still expects an enum... we use this.
     */
    private static final ObjectMap<Scaling, String> scalingMap = new ObjectMap<>(8);
    static {
        scalingMap.put(Scaling.fit, "fit");
        scalingMap.put(Scaling.fill, "fill");
        scalingMap.put(Scaling.stretch, "stretch");
        scalingMap.put(Scaling.none, "none");
        scalingMap.put(Scaling.fillX, "fillX");
        scalingMap.put(Scaling.fillY, "fillY");
        scalingMap.put(Scaling.stretchX, "stretchX");
        scalingMap.put(Scaling.stretchY, "stretchY");
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        //Vector2 scaled = scaling.apply(getWorldWidth(), getWorldHeight(), screenWidth, screenHeight);
        float worldWidth = getWorldWidth(), worldHeight = getWorldHeight();

        int viewportWidth = 0;
        int viewportHeight = 0;
        switch (scalingMap.get(scaling)) {
            case "fit": {
                float screenRatio = screenHeight / (float)screenWidth;
                float worldRatio = worldHeight / worldWidth;
                float scale = (int) (screenRatio > worldRatio ? screenWidth / (worldWidth * conversionX) : screenHeight / (worldHeight * conversionY));
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                this.currentScale = 1f / scale;
                break;
            }
            case "none": {
                viewportWidth = (int) worldWidth;
                viewportHeight = (int) worldHeight;
                break;
            }
            case "fillX": {
                float scale = (int) Math.ceil(screenWidth / (worldWidth * conversionX));
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                this.currentScale = 1f / scale;
                break;
            }
            case "fillY": {
                float scale = (int) Math.ceil(screenHeight / (worldHeight * conversionY));
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                this.currentScale = 1f / scale;
                break;
            }
            case "stretch":
                viewportWidth = screenWidth;
                viewportHeight = screenHeight;
                break;
            case "stretchX":
                viewportWidth = screenWidth;
                viewportHeight = (int) worldHeight;
                break;
            case "stretchY":
                viewportWidth = (int) worldWidth;
                viewportHeight = screenHeight;
                break;
            //case "fill":
            default: {
                float screenRatio = screenHeight / (float) screenWidth;
                float worldRatio = worldHeight / worldWidth;
                float scale = (int) Math.ceil(screenRatio < worldRatio ? screenWidth / (worldWidth * conversionX) : screenHeight / (worldHeight * conversionY));
                if (scale < 1) scale = 0.5f;
                viewportWidth = Math.round(worldWidth * scale);
                viewportHeight = Math.round(worldHeight * scale);
                this.currentScale = 1f / scale;
                break;
            }
        }
        // Center.
        setScreenBounds((screenWidth - viewportWidth * conversionX) / 2, (screenHeight - viewportHeight * conversionY) / 2, viewportWidth * conversionX, viewportHeight * conversionX);

        apply(centerCamera);
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public static final Scaling contain = new Scaling() {
        @Override
        public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
            float targetRatio = targetHeight / targetWidth;
            float sourceRatio = sourceHeight / sourceWidth;
            float scale = Math.min(targetRatio > sourceRatio
                            ? targetWidth / sourceWidth
                            : targetHeight / sourceHeight,
                    1);
            temp.x = sourceWidth * scale;
            temp.y = sourceHeight * scale;
            return temp;
        }
    };
}
