package com.github.yellowstonegames;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;
import com.github.yellowstonegames.game.AssetData;

import static com.github.yellowstonegames.Main.OFFSET_Y;

/**
 * Wraps a {@link Sprite} so its position can be set using isometric coordinates.
 * There are many possible isometric coordinate systems, so the one used here intentionally avoids calling the isometric
 * axes x, y, or z - here, the axes are f, g, and h, with mnemonics to help remember them. This uses an origin point at
 * the bottom center of the rotated rectangular map, typically the lowest corner of the map. The f and g axes are
 * diagonal, and correspond to movement at a shallow angle on both world x and world y. The h axis is used for
 * elevation, and corresponds to movement on world y. The mnemonics here work as if on a world map, with the origin
 * somewhere in Belgium or the Netherlands:
 * <ul>
 *     <li>The f axis is roughly the diagonal from France to Finland.</li>
 *     <li>The g axis is roughly the diagonal from Germany to Greenland (or Greece to Germany, or Greece to Greenland).</li>
 *     <li>The h axis is the vertical line from your heel to your head (or Hell to Heaven).</li>
 * </ul>
 */
public class IsoSprite {
    /**
     * The "cube side length" for one voxel.
     */
    public static float UNIT = AssetData.TILE_HEIGHT;
    /**
     * The Sprite this wraps and knows how to display.
     */
    public Sprite sprite;
    /**
     * The f-coordinate, on the diagonal axis from "France to Finland".
     */
    public float f;
    /**
     * The g-coordinate, on the diagonal axis from "Germany to Greenland".
     */
    public float g;
    /**
     * The h-coordinate, on the vertical/elevation axis from "heel to head".
     */
    public float h;

    /**
     * Creates an IsoSprite with an empty {@link Sprite#Sprite()} for its visual.
     */
    public IsoSprite() {
        this(new Sprite());
    }

    /**
     * Creates an IsoSprite with the given Sprite for its visual.
     * @param sprite the Sprite to show
     */
    public IsoSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    /**
     * Creates an IsoSprite with the given Sprite for its visual and the given isometric tile position.
     * @param sprite the Sprite to show
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    public IsoSprite(Sprite sprite, float f, float g, float h) {
        this.sprite = sprite;
        setPosition(f, g, h);
    }

    /**
     * Creates an IsoSprite with the given Sprite for its visual and the given isometric tile position.
     * @param sprite the Sprite to show
     * @param position an isometric tile position, storing f,g,h in the Vector3's x,y,z
     */
    public IsoSprite(Sprite sprite, Vector3 position) {
        this.sprite = sprite;
        setPosition(position.x, position.y, position.z);
    }

    /**
     * Creates an IsoSprite with the given Sprite for its visual and the given isometric tile position.
     * @param sprite the Sprite to show
     * @param position an isometric tile position, storing f,g,h in the GridPoint3's x,y,z
     */
    public IsoSprite(Sprite sprite, GridPoint3 position) {
        this.sprite = sprite;
        setPosition(position.x, position.y, position.z);
    }

    /**
     * Sets the position to the given isometric tile coordinates.
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    public void setPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT) + OFFSET_Y;
        sprite.setPosition(worldX, worldY);
    }

    /**
     * Sets the position to the given isometric tile coordinates.
     * @param point an isometric tile position, storing f,g,h in the GridPoint3's x,y,z
     */
    public void setPosition(GridPoint3 point) {
        setPosition(point.x, point.y, point.z);
    }

    /**
     * Sets the position to the given isometric tile coordinates.
     * @param point an isometric tile position, storing f,g,h in the Vector3's x,y,z
     */
    public void setPosition(Vector3 point) {
        setPosition(point.x, point.y, point.z);
    }

    /**
     * Sets the Sprite's origin-based position to the given isometric tile coordinates.
     * @see Sprite#setOriginBasedPosition(float, float)
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     */
    public void setOriginBasedPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT) + OFFSET_Y;
        sprite.setOriginBasedPosition(worldX, worldY);
    }

    /**
     * Sets the Sprite's origin, used for {@link #setOriginBasedPosition(float, float, float)} and so on.
     * @param originX x relative to the Sprite's position, in world coordinates
     * @param originY y relative to the Sprite's position, in world coordinates
     */
    public void setOrigin(float originX, float originY) {
        sprite.setOrigin(originX, originY);
    }

    /**
     * Sets the Sprite's origin to its center, as with {@link Sprite#setOriginCenter()}.
     */
    public void setOriginCenter() {
        sprite.setOriginCenter();
    }

    public float getOriginX() {
        return sprite.getOriginX();
    }

    public float getOriginY() {
        return sprite.getOriginY();
    }

    /**
     * Gets the visual currently used for this IsoSprite.
     * @return the Sprite this uses
     */
    public Sprite getSprite() {
        return sprite;
    }

    /**
     * Sets the visual Sprite to the given parameter, first setting its position, its color, and its origin to match
     * what this IsoSprite uses, then making the {@link #sprite} the same reference as the parameter.
     * @param sprite a Sprite that will be modified to match this IsoSprite's color, position, and origin
     */
    public void setSprite(Sprite sprite) {
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT) + OFFSET_Y;
        sprite.setPosition(worldX, worldY);
        sprite.setPackedColor(this.sprite.getPackedColor());
        sprite.setOrigin(this.sprite.getOriginX(), this.sprite.getOriginY());
        this.sprite = sprite;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    /**
     * Gets a float that roughly determines how close this IsoSprite is to the viewer/camera, for sorting purposes.
     * This won't work for IsoSprites that can have f or g go outside the -1023 to 1023 range.
     * The distance this returns is only useful relative to other results of this method, not in-general position.
     * Higher returned values mean the IsoSprite is closer to the camera, and so should be rendered later.
     * @return an estimate of how close this IsoSprite is to the viewer/camera, in no particular scale
     */
    public float getViewDistance() {
        return (h * 3 - f - g) + (f - g) * (1f/2048);
    }

    /**
     * Calculates the distance from the camera to the given f,g,h position, using the given cos and sin of the rotation
     * of the map around the given origin point. This won't really work for massive maps, larger than 1024x1024 or so
     * in f by g size.
     * @param f isometric tile f-coordinate
     * @param g isometric tile g-coordinate
     * @param h isometric tile h-coordinate
     * @param originF the f-coordinate of the rotational center of the map
     * @param originG the g-coordinate of the rotational center of the map
     * @param cosRotation the pre-calculated cosine of the map's rotation
     * @param sinRotation the pre-calculated sine of the map's rotation
     * @return the view distance to the given position, with the given rotation around the given origin
     */
    public static float viewDistance(float f, float g, float h, float originF, float originG, float cosRotation, float sinRotation) {
        // Get f and g relative to the origin.
        f -= originF;
        g -= originG;
        // Get rotated f and g positions around the origin.
        float rf = cosRotation * f - sinRotation * g + originF;
        float rg = cosRotation * g + sinRotation * f + originG;
        // Return the distance to the rotated position, with height taking priority, then the diagonal positions that
        // affect distance to the camera, then with a tiny fraction of left-to-right position used to break ties.
        return (h * 3f - rf - rg) + ((rf - rg) * (1f/2048f));
    }

    /**
     * Just like {@link #getViewDistance()}, but this returns an int for cases where sorting ints is easier.
     * Higher returned values mean the IsoSprite is closer to the camera, and so should be rendered later.
     * @return an int code that will be greater for IsoSprites that are closer to the camera
     */
    public int getSortCode() {
        int bits = NumberUtils.floatToIntBits((h * 3 - f - g) + (f - g) * (1f/2048) + 0f);
        return bits ^ (bits >> 31 & 0x7FFFFFFF);
    }

    public void draw(Batch batch) {
        sprite.draw(batch);
    }

    public void draw(Batch batch, float alphaModulation) {
        sprite.draw(batch, alphaModulation);
    }

    public void draw(Batch batch, float originF, float originG, float cosRotation, float sinRotation) {
        float af = f - originF;
        float ag = g - originG;
        float rf = cosRotation * af - sinRotation * ag + originF;
        float rg = cosRotation * ag + sinRotation * af + originG;
        float worldX = (rf - rg) * (2 * UNIT);
        float worldY = (rf + rg) * UNIT + h * (2 * UNIT) + OFFSET_Y;
        sprite.setPosition(worldX, worldY);
        sprite.draw(batch);
    }

    public void draw(Batch batch, float alphaModulation, float originF, float originG, float cosRotation, float sinRotation) {
        float af = f - originF;
        float ag = g - originG;
        float rf = cosRotation * af - sinRotation * ag + originF;
        float rg = cosRotation * ag + sinRotation * af + originG;
        float worldX = (rf - rg) * (2 * UNIT);
        float worldY = (rf + rg) * UNIT + h * (2 * UNIT) + OFFSET_Y;
        sprite.setPosition(worldX, worldY);
        sprite.draw(batch, alphaModulation);
    }

    /**
     * Does nothing here, but can be overridden in subclasses to do something with a current time.
     * @param stateTime time, typically in seconds, and typically since some event started (like creating this object)
     * @return this object, for chaining
     */
    public IsoSprite update(float stateTime) {
        return this;
    }

    @Override
    public String toString() {
        return "IsoSprite{" +
            "f=" + f +
            ", g=" + g +
            ", h=" + h +
            '}';
    }
}
