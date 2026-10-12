package com.github.yellowstonegames;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/**
 * A variant on {@link IsoSprite} that changes its visual Sprite based on an animation, typically a looping one.
 */
public class AnimatedIsoSprite extends IsoSprite {
    /**
     * The Animation of Sprite that determines which Sprite will be currently shown.
     */
    public Animation<? extends TextureAtlas.AtlasSprite> animation;

    /**
     * Creates an AnimatedIsoSprite with the given Animation of Sprite, setting the initial visual to its first frame.
     * @param animation an Animation of Sprite or subclasses of Sprite
     */
    public AnimatedIsoSprite(Animation<? extends TextureAtlas.AtlasSprite> animation) {
        super(animation.getKeyFrame(0, true));
        this.animation = animation;
    }

    /**
     * Creates an AnimatedIsoSprite with the given Animation of Sprite, setting the initial visual to its first frame.
     * Places it at the given isometric tile position.
     * @param animation an Animation of Sprite or subclasses of Sprite
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    public AnimatedIsoSprite(Animation<? extends TextureAtlas.AtlasSprite> animation, float f, float g, float h) {
        super(animation.getKeyFrame(0, true));
        this.animation = animation;
        setPosition(f, g, h);
    }


    /**
     * Acts just like the superclass implementation, {@link IsoSprite#setPosition(float, float, float)}, but also sets
     * each Sprite's position in the animation.
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    @Override
    public void setPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        for (Sprite s : animation.getKeyFrames()) {
            s.setPosition(worldX, worldY);
        }
    }

    /**
     * Acts just like the superclass implementation, {@link IsoSprite#setOriginBasedPosition(float, float, float)} , but
     * also sets each Sprite's origin-based position in the animation.
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    @Override
    public void setOriginBasedPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        for (Sprite s : animation.getKeyFrames()) {
            s.setOriginBasedPosition(worldX, worldY);
        }
    }

    /**
     * Acts just like the superclass implementation, {@link IsoSprite#setOrigin(float, float)}, but also sets each
     * Sprite's origin in the animation.
     * @param originX x relative to the Sprite's position, in world coordinates
     * @param originY y relative to the Sprite's position, in world coordinates
     */
    @Override
    public void setOrigin(float originX, float originY) {
        for (Sprite s : animation.getKeyFrames()) {
            s.setOrigin(originX, originY);
        }
    }

    /**
     * Acts just like the superclass implementation, {@link IsoSprite#setOriginCenter()}, but also sets the origin to
     * center for each Sprite in the animation.
     */
    @Override
    public void setOriginCenter() {
        for (Sprite s : animation.getKeyFrames()) {
            s.setOriginCenter();
        }
    }

    /**
     * Changes the currently drawn Sprite based on the current key frame in {@link #animation} given {@code stateTime}.
     * This delegates to {@link #update(float, boolean)} with {@code looping} set to true.
     *
     * @param stateTime time, in seconds; typically since this was constructed or since the last time the game un-paused
     */
    @Override
    public AnimatedIsoSprite update(float stateTime) {
        return update(stateTime, true);
    }

    /**
     * Changes the currently drawn Sprite based on the current key frame in {@link #animation} given {@code stateTime}.
     * If {@code looping} is true, then sequential {@link Animation.PlayMode} settings will always loop, or if looping
     * is false, then if stateTime is greater than {@link Animation#getAnimationDuration()}, the last Sprite will be
     * used instead of wrapping around.
     *
     * @param stateTime time, in seconds; typically since this was constructed or since the last time the game un-paused
     * @param looping if true, the animation will always loop to the beginning; if false, it will end on the last Sprite
     *               in the animation if stateTime is too high
     */
    public AnimatedIsoSprite update(float stateTime, boolean looping) {
        super.setSprite(animation.getKeyFrame(stateTime, looping));
        return this;
    }
}
