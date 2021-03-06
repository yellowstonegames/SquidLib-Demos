package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.TimeUtils;

import static com.badlogic.gdx.Gdx.input;

public class ShaderDemo extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture screenTexture;

    private long startTime = 0L, lastProcessedTime = 0L;
    private ShaderProgram shader, hueShader;
    private Vector3 add, mul;
    
    private ColorEqualizer eq;
    
    public void load(FileHandle file) {
        if(!file.exists())
            return;
        screenTexture = new Texture(eq.process(new Pixmap(file)), Pixmap.Format.RGBA8888, false);
//        screenTexture = new Texture(file, Pixmap.Format.RGBA8888, false);
        screenTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Gdx.graphics.setWindowedMode(screenTexture.getWidth(), screenTexture.getHeight());
    }

    @Override
    public void create() {
        lastProcessedTime = 0L;
        startTime = TimeUtils.millis();
        eq = new ColorEqualizer();
        add = new Vector3(0f, 0f, 0f);
//        add = new Vector3(0.1f, 0.95f, swayRandomized(12345, TimeUtils.timeSinceMillis(startTime) * 0x1p-9f) * 0.4f + 0.2f);
        mul = new Vector3(1f, 1f, 1f);
        shader = new ShaderProgram(vertexShader, fragmentShaderOnlyWarmMild);
        if (!shader.isCompiled()) throw new GdxRuntimeException("Couldn't compile shader: " + shader.getLog());
        hueShader = new ShaderProgram(vertexShader, fragmentShaderHueLights);
        if (!hueShader.isCompiled()) throw new GdxRuntimeException("Couldn't compile shader: " + hueShader.getLog());
        batch = new SpriteBatch(1000, shader);
        batch.enableBlending();
        load(Gdx.files.internal("Mona_Lisa.jpg"));
    }


    @Override
    public void render() {
        handleInput();
        Gdx.gl.glClearColor(0.4f, 0.4f, 0.4f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setColor(Color.WHITE);
        //you can also use
//        batch.setColor(1f, 1f, 1f, 1f);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenTexture.getWidth(), screenTexture.getHeight());
        batch.begin();
//        shader.setUniformf("u_mul", 0.9f, 0.7f, 0.75f);
//        shader.setUniformf("u_add", 0.05f, 0.14f, 0.16f);
//        shader.setUniformf("u_mul", 1f, 1f, 1f);
//        shader.setUniformf("u_add", 0f, 0f, 0f);
        //// this makes the "mild" parameter move the color from red to orange to yellow over time.
        //// it only moves in that range because the warmth has been pushed very high.
        //add.z = swayRandomized(12345, TimeUtils.timeSinceMillis(startTime) * 0x1p-9f) * 0.4f + 0.2f;
        batch.getShader().setUniformf("u_mul", mul);
        batch.getShader().setUniformf("u_add", add);
        batch.draw(screenTexture, 0, 0);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
    }

    public void handleInput()
    {
        // only process once every 200 ms, or 5 times a second, at most
        if(TimeUtils.timeSinceMillis(lastProcessedTime) < 200)
            return;
        lastProcessedTime = TimeUtils.millis();
        if(input.isKeyPressed(Keys.SPACE))
        {
            if(batch.getShader().equals(shader))
                batch.setShader(hueShader);
            else
                batch.setShader(shader);
            return;
        }
        Vector3 changing;
        // holding shift will change multipliers, otherwise it affects addends
        if(input.isKeyPressed(Keys.SHIFT_LEFT) || input.isKeyPressed(Keys.SHIFT_RIGHT))
            changing = mul;
        else 
            changing = add;
        if(input.isKeyPressed(Keys.UP) || input.isKeyPressed(Keys.L)) //light
            changing.x = MathUtils.clamp(changing.x + 0.02f, -1f, 1f);
        else if(input.isKeyPressed(Keys.DOWN) || input.isKeyPressed(Keys.D)) //dark
            changing.x = MathUtils.clamp(changing.x - 0.02f, -1f, 1f);
        else if(input.isKeyPressed(Keys.W)) //warm
            changing.y = MathUtils.clamp(changing.y + 0.02f, -2f, 2f);
        else if(input.isKeyPressed(Keys.C)) //cool
            changing.y = MathUtils.clamp(changing.y - 0.02f, -2f, 2f);
        else if(input.isKeyPressed(Keys.M)) //mild
            changing.z = MathUtils.clamp(changing.z + 0.02f, -2f, 2f);
        else if(input.isKeyPressed(Keys.B)) // bold
            changing.z = MathUtils.clamp(changing.z - 0.02f, -2f, 2f);
        else if(input.isKeyPressed(Keys.R)) // reset
        {
            mul.set(1f, 1f, 1f);
            add.set(0f, 0f, 0f);
        }
        else if(input.isKeyPressed(Keys.P)) // print
            System.out.println("Mul: Y="+mul.x+",Cw="+mul.y+",Cm="+mul.z+
                    "\nAdd: Y="+add.x+",Cw="+add.y+",Cm="+add.z);
        else if(input.isKeyPressed(Keys.Q) || input.isKeyPressed(Keys.ESCAPE)) //quit
            Gdx.app.exit();
    }
    /**
     * This is the default vertex shader from libGDX.
     */
    public static final String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "   v_color.a = v_color.a * (255.0/254.0);\n"
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "}\n";

    /**
     * This fragment shader allows color space adjustments to be done and does not do any color reduction. The uniforms
     * {@code u_mul} and {@code u_add} are each YCwCm adjustments. The first multiplies the Y (brightness), Cw (Chroma
     * warm, with values greater than 1 making warm colors warmer and cool colors cooler) and Cm (Chroma mild, with
     * values greater than 1 making green/yellow colors closer to those and red/blue colors closer to that) by the
     * image's YCwCm values after palette-substitution. After that, {@code u_add} is added to Y (which can have an
     * internal value between 0 and 1, and all are clamped), Cw (which ranges between -1 for blue/green and 1 for
     * red/yellow), and Cm (which ranges between -1 for red/blue and 1 for yellow/green). You can use this to desaturate
     * colors by setting {@code u_mul} to {@code vec3(1.0, 0.5, 0.5)} or any other small fractions for Cw and Cm. You
     * can make colors warmer by setting {@code u_add} to {@code vec3(0.0, 0.6, 0.0)}; while warmth is added, randomly
     * setting Cm to a value between -0.5 and 0.5 can simulate a fiery color effect over the screen. You can make an icy
     * effect by setting {@code u_add} to {@code vec3(0.3, -0.4, 0.0)}. You can simulate the desaturation and yellowing
     * that happens to old paintings by setting {@code u_mul} to {@code vec3(0.9, 0.7, 0.75)} and {@code u_add} to
     * {@code vec3(0.05, 0.14, 0.16)}.
     */
    public static final String fragmentShaderOnlyWarmMild =
            "varying vec2 v_texCoords;\n" +
                    "varying vec4 v_color;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "uniform vec3 u_add;\n" +
                    "uniform vec3 u_mul;\n" +
                    "void main()\n" +
                    "{\n" +
                    "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
                    "   tgt.rgb = u_add + u_mul * vec3(dot(tgt.rgb, vec3(0.375, 0.5, 0.125)), tgt.r - tgt.b, tgt.g - tgt.b);\n" +
                    "   gl_FragColor.rgb = v_color.rgb * clamp(vec3(dot(tgt.rgb, vec3(1.0, 0.625, -0.5)), dot(tgt.rgb, vec3(1.0, -0.375, 0.5)), dot(tgt.rgb, vec3(1.0, -0.375, -0.5))), 0.0, 1.0);\n" +
                    "   gl_FragColor.a = v_color.a * tgt.a;\n" +
                    "}";
    
    public static final String fragmentShaderHueLights = 
            "varying vec2 v_texCoords;\n" + 
                    "varying vec4 v_color;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "uniform vec3 u_add;\n" + // defaults to all 0.0
                    "uniform vec3 u_mul;\n" + // defaults to all 1.0
                    "void main()\n" +
                    "{\n" +
                    "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
                    "   float luma = dot(tgt.rgb, vec3(0.375, 0.5, 0.125));\n" +
//                    "   luma = log2(luma+1.0);\n" +
//                    "   luma *= sqrt(luma);\n" +
                    "   luma = pow(luma, 1.1875);\n" +
//                    "   luma = 2.0 - exp2(1.0 - luma);\n" +
                    //"   luma = exp2(luma) - 1.0;\n" + // deepens all colors except the brightest
                    //"   luma = clamp(luma * sqrt(luma) * 1.4, 0.0, 1.0);\n" + // not much effect
                    //  other things tried for luma
                    //  clamp(luma * 1.5 - 0.25, 0.0, 1.0) // moves a lot of low values to 0 and high to 1
                    //  (luma * luma * (3.0 - 2.0 * luma)) // cubic interpolation, could use smoothstep.
//                    "   float adj = sin(luma * -6.283185307179586) * 0.15;\n" +
                    // 4pi: 12.566370614359172
                    // 3.75pi: 11.780972450961723
                    "   float adj = sin(( (luma - 0.5) * abs(luma - 0.5)  ) * 13.5) * 0.09;\n" + // does not use an exact multiple of pi
//                    "   float adj = 0.0;\n" + // testing just light adjustment
                    //"   adj *= abs(adj) * 0.2;\n" + // makes weird changes to a sine wave's shape
                    "   tgt.rgb = u_add + u_mul * vec3(luma, adj + tgt.r - tgt.b, adj + tgt.g - tgt.b);\n" +
                    "   gl_FragColor.rgb = v_color.rgb * clamp(vec3(dot(tgt.rgb, vec3(1.0, 0.625, -0.5)), dot(tgt.rgb, vec3(1.0, -0.375, 0.5)), dot(tgt.rgb, vec3(1.0, -0.375, -0.5))), 0.0, 1.0);\n" +
                    "   gl_FragColor.a = v_color.a * tgt.a;\n" +
                    "}";
    
    public static int hueShift(int rgba)
    {
//        luma = Math.log1p(luma) * 1.4426950408889634;
//        luma *= Math.sqrt(luma);
        final int a = rgba & 0xFF;
        final float r = (rgba >>> 24) / 255f, g = (rgba >>> 16 & 0xFF) / 255f, b = (rgba >>> 8 & 0xFF) / 255f;
        final double luma = Math.pow(r * 0.375f + g * 0.5f + b * 0.125f, 1.1875);
//        float adj = (float) Math.sin((luma * luma * (3 - 2 * luma)) * 6.283185307179586f);
        final float adj = (float) Math.sin((luma - 0.5f) * Math.abs(luma - 0.5f) * 13.5) * 0.09f;//(1.875f * 6.283185307179586f)
//        adj *= Math.abs(adj) * -0.2f;
        final float warm = adj + r - b, mild = 0.5f * (adj + g - b);
        return (MathUtils.clamp((int) ((luma + 0.625f * warm - mild) * 256f), 0, 255)<<24|
                MathUtils.clamp((int) ((luma - 0.375f * warm + mild) * 256f), 0, 255)<<16|
                MathUtils.clamp((int) ((luma - 0.375f * warm - mild) * 256f), 0, 255)<<8|
                a);
    }
    
    /**
     * Gets a float between -1.0 and 1.0 that smoothly changes as value goes up or down, with a seed that will determine
     * what the peaks and valleys will be on that smooth change.
     * @param seed an int seed that will determine the pattern of peaks and valleys this will generate as value changes; this should not change between calls
     * @param value a float that typically changes slowly, by less than 2.0, with direction changes at integer inputs
     * @return a pseudo-random float between -1f and 1f (both exclusive), smoothly changing with value
     */
    public static float swayRandomized(final int seed, float value)
    {
        final int floor = value >= 0f ? (int) value : (int) value - 1;
        int z = seed + floor;
        final float start = (((z = (z ^ 0xD1B54A35) * 0x102473) ^ (z << 11 | z >>> 21) ^ (z << 21 | z >>> 11)) * ((z ^ z >>> 15) | 0xFFE00001) + z) * 0x0.ffffffp-31f,
                end = (((z = (seed + floor + 1 ^ 0xD1B54A35) * 0x102473) ^ (z << 11 | z >>> 21) ^ (z << 21 | z >>> 11)) * ((z ^ z >>> 15) | 0xFFE00001) + z) * 0x0.ffffffp-31f;
        value -= floor;
        value *= value * (3 - 2 * value);
        return (1 - value) * start + value * end;
    }

}
