package com.github.tommyettinger.demos;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.github.tommyettinger.colorful.oklab.ColorTools;
import com.github.tommyettinger.colorful.oklab.ColorfulSprite;
import com.github.tommyettinger.colorful.oklab.Palette;
import squidpony.squidmath.Coord;

/**
 * Like a {@link com.badlogic.gdx.graphics.g2d.Sprite}, but lighter-weight and customized to the conventions of this
 * demo. Has a start and end position that it is expected to move between as its {@link #change} field changes.
 * Supports a packed float color.
 * <br>
 * Created by Tommy Ettinger on 12/20/2019.
 */
public class AnimatedGlider extends ColorfulSprite {
    public Animation<TextureRegion> animation;
    public float change;
    public Coord start = Coord.get(0, 0); // you could also use GridPoint2 or Vector2 here
    public Coord end = Coord.get(0, 0);

    private AnimatedGlider()
    {
        super();
        setTweakedColor(Palette.GRAY, ColorTools.oklab(0.5f, 0.375f, 0.375f, 0.75f));
    }
    public AnimatedGlider(Animation<TextureRegion> animation) {
        super();
        this.animation = animation;
        setRegion(animation.getKeyFrame(0f));
        setTweakedColor(Palette.GRAY, ColorTools.oklab(0.5f, 0.375f, 0.375f, 0.75f));
    }

    public AnimatedGlider(Animation<TextureRegion> animation, Coord coord) {
        this(animation, coord, coord);
    }

    public AnimatedGlider(Animation<TextureRegion> animation, Coord start, Coord end) {
        super();
        this.animation = animation;
        setSize(1, 1);
        setRegion(animation.getKeyFrame(0f));
        setTweakedColor(Palette.GRAY, ColorTools.oklab(0.5f, 0.375f, 0.375f, 0.75f));
        this.start = start;
        this.end = end;
    }

    public AnimatedGlider animate(final float stateTime)
    {
        setRegion(animation.getKeyFrame(stateTime));
        return this;
    }
    
    public float getX()
    {
        return MathUtils.lerp(start.x, end.x, change);
    }

    public float getY()
    {
        return MathUtils.lerp(start.y, end.y, change);
    }

    @Override
    public float[] getVertices() {
        if(change >= 1f)
        {
            start = end;
            super.setPosition(start.x, start.y);
        }
        else {
            super.setPosition(MathUtils.lerp(start.x, end.x, change), MathUtils.lerp(start.y, end.y, change));
        }
        return super.getVertices();
    }
}
