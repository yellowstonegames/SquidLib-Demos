package com.github.tommyettinger.demos;

import com.badlogic.gdx.math.MathUtils;
import squidpony.squidmath.NumberTools;

/**
 * Created by Tommy Ettinger on 9/25/2018.
 */
public class ColorTools {
    private ColorTools(){}
    /**
     * Gets a packed float representation of a color given as 4 RGBA float components. LibGDX expects ABGR format
     * in some places, but not all, and it can be confusing to track when it wants RGBA, ABGR, or ARGB. Generally,
     * packed floats like what this returns are ABGR format, the kind that can be passed directly to
     * {@link com.badlogic.gdx.graphics.g2d.Batch#setPackedColor(float)} without constructing intermediate objects.
     * SquidPanel also uses floats internally instead of LibGDX Color objects in its internal 2D array that
     * associates colors to cells; this has changed from earlier releases and should be much more efficient.
     *
     * @param r a float from 0.0 to 1.0 for red
     * @param g a float from 0.0 to 1.0 for green
     * @param b a float from 0.0 to 1.0 for blue
     * @param a a float from 0.0 to 1.0 for alpha/opacity
     * @return a packed float that can be given to the setColor method in LibGDX's Batch classes
     */
    public static float floatGet(float r, float g, float b, float a) {
        return NumberTools.intBitsToFloat(((int) (a * 255) << 24 & 0xFE000000) | ((int) (b * 255) << 16)
                | ((int) (g * 255) << 8) | (int) (r * 255));
    }

    /**
     * Gets a color as a packed float given floats representing hue, saturation, value, and opacity.
     * All parameters should normally be between 0 and 1 inclusive, though hue is tolerated if it is negative down to
     * -6f at the lowest or positive up to any finite value, though precision loss may affect the color if the hue is
     * too large. A hue of 0 is red, progressively higher hue values go to orange, yellow, green, blue, and purple
     * before wrapping around to red as it approaches 1. A saturation of 0 is grayscale, a saturation of 1 is brightly
     * colored, and values close to 1 will usually appear more distinct than values close to 0, especially if the
     * hue is different (saturation below 0.0039f is treated specially here, and does less work to simply get a color
     * between black and white with the given opacity). The value is similar to lightness; a value of 0.0039 or less is
     * always black (also using a shortcut if this is the case, respecting opacity), while a value of 1 is as bright as
     * the color gets with the given saturation and value. To get a value of white, you would need both a value of 1 and
     * a saturation of 0.
     *
     * @param hue        0f to 1f, color wheel position
     * @param saturation 0f to 1f, 0f is grayscale and 1f is brightly colored
     * @param value      0f to 1f, 0f is black and 1f is bright or light
     * @param opacity    0f to 1f, 0f is fully transparent and 1f is opaque
     * @return a float encoding a color with the given properties
     */
    public static float floatGetHSV(float hue, float saturation, float value, float opacity) {
        if (saturation <= 0.0039f) {
            return floatGet(value, value, value, opacity);
        } else if (value <= 0.0039f) {
            return NumberTools.intBitsToFloat((int) (opacity * 255f) << 24 & 0xFE000000);
        } else {
            final float h = ((hue + 6f) % 1f) * 6f;
            final int i = (int) h;
            value = MathUtils.clamp(value, 0f, 1f);
            saturation = MathUtils.clamp(saturation, 0f, 1f);
            final float a = value * (1 - saturation);
            final float b = value * (1 - saturation * (h - i));
            final float c = value * (1 - saturation * (1 - (h - i)));

            switch (i) {
                case 0:
                    return floatGet(value, c, a, opacity);
                case 1:
                    return floatGet(b, value, a, opacity);
                case 2:
                    return floatGet(a, value, c, opacity);
                case 3:
                    return floatGet(a, b, value, opacity);
                case 4:
                    return floatGet(c, a, value, opacity);
                default:
                    return floatGet(value, a, b, opacity);
            }
        }
    }
    /**
     * Interpolates from the packed float color start towards end by change, but keeps the alpha of start and uses the
     * alpha of end as an extra factor that can affect how much to change. Both start and end should be packed colors,
     * as from {@link com.badlogic.gdx.graphics.Color#toFloatBits()} or {@link #floatGet(float, float, float, float)},
     * and change can be between 0f (keep start) and 1f (only use end). This is a good way to reduce allocations of
     * temporary Colors.
     * @param start the starting color as a packed float; alpha will be preserved
     * @param end the target color as a packed float; alpha will not be used directly, and will instead be multiplied with change
     * @param change how much to go from start toward end, as a float between 0 and 1; higher means closer to end
     * @return a packed float that represents a color between start and end
     */
    public static float lerpFloatColors(final float start, final float end, float change) {
        final int s = NumberTools.floatToIntBits(start), e = NumberTools.floatToIntBits(end),
                rs = (s & 0xFF), gs = (s >>> 8) & 0xFF, bs = (s >>> 16) & 0xFF, as = s & 0xFE000000,
                re = (e & 0xFF), ge = (e >>> 8) & 0xFF, be = (e >>> 16) & 0xFF, ae = (e >>> 25);
        change *= ae * 0.007874016f;
        return NumberTools.intBitsToFloat(((int) (rs + change * (re - rs)) & 0xFF)
                | (((int) (gs + change * (ge - gs)) & 0xFF) << 8)
                | (((int) (bs + change * (be - bs)) & 0xFF) << 16)
                | as);
    }

}
