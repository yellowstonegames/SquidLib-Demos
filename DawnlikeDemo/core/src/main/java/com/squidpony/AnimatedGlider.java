package com.squidpony;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import squidpony.squidmath.Coord;

/**
 * Like a {@link com.badlogic.gdx.graphics.g2d.Sprite}, but lighter-weight and customized to the conventions of this
 * demo. Has a start and end position that it is expected to move between as its {@link #change} field changes.
 * Supports a packed float color.
 * <br>
 * Created by Tommy Ettinger on 12/20/2019.
 */
public class AnimatedGlider extends TextureRegion {
    public Animation<TextureRegion> animation;
    public float change;
    public Coord start = Coord.get(0, 0); // you could also use GridPoint2 or Vector2 here
    public Coord end = Coord.get(0, 0);
    public float color;

    private AnimatedGlider()
    {
        super();
        this.color = Color.WHITE_FLOAT_BITS;
    }
    public AnimatedGlider(Animation<TextureRegion> animation) {
        super();
        this.animation = animation;
        setRegion(animation.getKeyFrame(0f));
        this.color = Color.WHITE_FLOAT_BITS;
    }

    public AnimatedGlider(Animation<TextureRegion> animation, Coord coord) {
        this(animation, coord, coord);
    }

    public AnimatedGlider(Animation<TextureRegion> animation, Coord start, Coord end) {
        super();
        this.animation = animation;
        setRegion(animation.getKeyFrame(0f));
        this.color = Color.WHITE_FLOAT_BITS;
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
        if(change >= 1f)
            return (start = end).x;
        return MathUtils.lerp(start.x, end.x, change);
    }

    public float getY()
    {
        if(change >= 1f)
            return (start = end).y;
        return MathUtils.lerp(start.y, end.y, change);
    }
}
