package com.github.tommyettinger.demos;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
    /**
     * An alternate shader for libGDX applications that use the standard SpriteBatch and RGBA color setup, but want to
     * have more contrast and saturation using the tools colorful has for YCwCmA colors. This can take an image like
     * <a href="https://i.imgur.com/yinuYzF.png">this, taken of the great game Sigma Finite Dungeon</a>, to
     * <a href="https://i.imgur.com/MUKe3St.png">this, with higher saturation and contrast</a>. There's issues with this
     * approach (gray often gets saturated too much and becomes somewhat cyan, and colors like brown may change their
     * appearance significantly with higher saturation), but it does tend to make drab or muted areas "pop" more.
     * @return a ShaderProgram that will increase saturation and contrast, and slightly decrease overall lightness
     */
    public static ShaderProgram createContrastShader () {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
           + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
           + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
           + "uniform mat4 u_projTrans;\n" //
           + "varying vec4 v_color;\n" //
           + "varying vec2 v_texCoords;\n" //
           + "\n" //
           + "void main()\n" //
           + "{\n" //
           + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
           + "   v_color.a = v_color.a * (255.0/254.0);\n" //
           + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
           + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
           + "}\n";
        String fragmentShader =
           "#ifdef GL_ES\n" +
              "#define LOWP lowp\n" +
              "precision mediump float;\n" +
              "#else\n" +
              "#define LOWP \n" +
              "#endif\n" +
              "varying vec2 v_texCoords;\n" +
              "varying LOWP vec4 v_color;\n" +
              "uniform sampler2D u_texture;\n" +
              "const vec3 bright = vec3(0.375, 0.5, 0.125);\n" +
              "void main()\n" +
              "{\n" +
              "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
              "   vec3 ycc = vec3(\n" +
// the constants 0.43 (lightness multiplier, 0.5 is middle), 1.4642634172891231 (higher means sharper contrast), and 2.714829459393612 are adjustable for luma
              "     (dot(v_color.rgb, bright) * 0.75 - 0.75 + 0.43 * pow(dot(tgt.rgb, bright), 1.4642634172891231) * 2.714829459393612),\n" + // luma
// the constant 1.5 is adjustable for both of the next two lines; 1.0 is neutral saturation, 1.5 is high saturation, 0.5 is low saturation
              "     ((v_color.r - v_color.b) + (tgt.r - tgt.b) * 1.5),\n" + // warmth
              "     ((v_color.g - v_color.b) + (tgt.g - tgt.b) * 1.5));\n" + // mildness
              "   gl_FragColor = clamp(vec4(\n" +
              "     dot(ycc, vec3(1.0, 0.625, -0.5)),\n" + // back to red
              "     dot(ycc, vec3(1.0, -0.375, 0.5)),\n" + // back to green
              "     dot(ycc, vec3(1.0, -0.375, -0.5)),\n" + // back to blue
              "     v_color.a * tgt.a), 0.0, 1.0);\n" + // back to alpha and clamp
              "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

}
