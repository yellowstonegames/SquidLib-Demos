package com.github.yellowstonegames.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

import static com.badlogic.gdx.math.MathUtils.floor;
import static com.badlogic.gdx.math.MathUtils.lerp;
import static com.github.yellowstonegames.util.MiniNoise.GradientVectors.*;

/**
 * The noise class, for "continuous noise" in the signal-vs.-noise sense. This doesn't make sounds!
 * If you request similar points in space from this, you will almost always get similar results as floats.
 * If you request very distant points in space from this, you should not notice any patterns between inputs and outputs.
 * This is useful for making organic-looking maps, though you might need to get creative to turn one float into a piece
 * of map terrain!
 * <br>
 * This was adapted from <a href="https://github.com/tommyettinger/cringe">Cringe</a> and its ContinuousNoise class,
 * which can also use the type of noise algorithm here, "Perlue Noise". This whole class was originally more than one
 * file, and it's long because so much has been smashed together here. But, this file is at least standalone, requiring
 * libGDX but nothing else.
 * <br>
 * If you are using this project as a basis for your own game, it probably makes more sense to depend on one of:
 * <ul>
 *     <li><a href="https://github.com/tommyettinger/cringe">Cringe</a></li>
 *     <li><a href="https://github.com/czyzby/noise4j">Noise4J</a></li>
 *     <li><a href="https://github.com/SudoPlayGames/Joise">Joise</a></li>
 *     <li><a href="https://github.com/yellowstonegames/SquidSquad">SquidSquad</a> (maybe)</li>
 * </ul>
 * And to use any of those libraries for their more robust continuous noise code.
 * Each of the suggested libraries is an option in gdx-liftoff's third-party extensions.
 */
@SuppressWarnings("DefaultNotLastCaseInSwitch")
public class MiniNoise {

    /**
     * "Standard" layered octaves of noise, where each octave has a different frequency and weight.
     * Tends to look cloudy with more octaves, and generally like a natural process. This only has
     * an effect with 2 octaves or more.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int FBM = 0;
    /**
     * A less common way to layer octaves of noise, where most results are biased toward higher values,
     * but "valleys" show up filled with much lower values.
     * This probably has some good uses in 3D or higher noise, but it isn't used too frequently.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int BILLOW = 1;
    /**
     * A way to layer octaves of noise so most values are biased toward low values but "ridges" of high
     * values run across the noise. This can be a good way of highlighting the least-natural aspects of
     * some kinds of noise; Perlin Noise has mostly ridges along 45-degree angles,
     * Simplex Noise has many ridges along a triangular grid, and so on. The Perlue Noise used here doesn't
     * look nearly as unnatural as the Perlin and Simplex types that are more common.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int RIDGED = 2;
    /**
     * Layered octaves of noise, where each octave has a different frequency and weight, and the results of
     * earlier octaves affect the inputs to later octave calculations. Tends to look cloudy but with swirling
     * distortions, and generally like a natural process. This only has an effect with 2 octaves or more.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int WARP = 3;

    /**
     * The names that correspond to the numerical mode constants, with the constant value matching the index here.
     */
    public static final String[] MODES = {"FBM", "Billow", "Ridged", "Warp"};

    /**
     * A ContinuousNoise always wraps a noise algorithm, which here is always a PerlueNoise.
     * If you want a more general solution, you can use the very similar API in
     * <a href="https://github.com/tommyettinger/cringe">Cringe</a>, a third-party dependency.
     */
    public PerlueNoise wrapped;
    /**
     * If high, details show up very often, if low, details are stretched out and take longer to change.
     * This works by multiplying the frequency with each coordinate to a noise call, like x, y, or z.
     */
    public float frequency;
    /**
     * One of 0, 1, 2, or 3, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}.
     */
    public int mode;
    /**
     * How many layers of noise with different frequencies to use. Using 2 octaves or more is necessary with
     * {@link #WARP} mode, and other modes tend to look better with more octaves, but the more octaves you request, the
     * slower the noise is to calculate, and there's typically no benefit to using more than 8 or so octaves here.
     */
    protected int octaves;

    /**
     * Creates a MiniNoise with seed 123, frequency (1f/32f), FBM mode, and 1 octave.
     */
    public MiniNoise() {
        this(123, 0.03125f, FBM, 1);

    }

    /**
     * Creates a MiniNoise with the given PerlueNoise to wrap, frequency (1f/32f), FBM mode, and 1 octave.
     */
    public MiniNoise(PerlueNoise toWrap){
        this(toWrap, 0.03125f, FBM, 1);
    }

    /**
     * Creates a MiniNoise wrapping the given PerlueNoise, with the given frequency (which should usually be small, less
     * than 0.5f), mode (which can be 0, 1, 2, or 3, and is usually a constant from this class), and octaves (usually 1
     * to at most 8 or so).
     * @param toWrap the PerlueNoise to wrap; this will use its seed
     * @param frequency the desired frequency, which is always greater than 0.0f but only by a small amount
     * @param mode the mode, which can be {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     * @param octaves how many layers of noise to use with different frequencies; usually between 1 and 8
     */
    public MiniNoise(PerlueNoise toWrap, float frequency, int mode, int octaves){
        wrapped = toWrap;
        this.frequency = frequency;
        this.mode = mode;
        this.octaves = octaves;
    }

    /**
     * Creates a MiniNoise with the given seed, with the given frequency (which should usually be small, less
     * than 0.5f), mode (which can be 0, 1, 2, or 3, and is usually a constant from this class), and octaves (usually 1
     * to at most 8 or so).
     * @param seed a PerlueNoise will be created for this to use with this seed
     * @param frequency the desired frequency, which is always greater than 0.0f but only by a small amount
     * @param mode the mode, which can be {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     * @param octaves how many layers of noise to use with different frequencies; usually between 1 and 8
     */
    public MiniNoise(int seed, float frequency, int mode, int octaves){
        wrapped = new PerlueNoise(seed);
        this.frequency = frequency;
        this.mode = mode;
        this.octaves = octaves;
    }

    /**
     * Copies another MiniNoise, including copying its internal PerlueNoise.
     * @param other another MiniNoise to copy
     */
    public MiniNoise(MiniNoise other) {
        setWrapped(other.getWrapped().copy());
        setSeed(other.getSeed());
        setFrequency(other.getFrequency());
        setFractalType(other.getFractalType());
        setFractalOctaves(other.getFractalOctaves());
    }

    /**
     * Gets the internal PerlueNoise this wraps.
     * @return a PerlueNoise, which is the only algorithm here
     */
    public PerlueNoise getWrapped() {
        return wrapped;
    }

    /**
     * Sets the internal PerlueNoise this wraps.
     * @param wrapped the PerlueNoise to wrap; it's the only algorithm here
     */
    public void setWrapped(PerlueNoise wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * High frequency makes details occur more often; low frequency makes details stretch out over a larger area.
     * Typically, this is a low float, such as {@code 1f/32f}.
     * @return the current frequency
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * High frequency makes details occur more often; low frequency makes details stretch out over a larger area.
     * Typically, this is a low float, such as {@code 1f/32f}.
     * @param frequency the frequency to use
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * Wraps {@link #getFractalType()}.
     * @return an int between 0 and 3, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public int getMode() {
        return getFractalType();
    }

    /**
     * Wraps {@link #setFractalType(int)}
     * @param mode an int between 0 and 3, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public void setMode(int mode) {
        setFractalType(mode);
    }

    /**
     * Gets the current mode, which is {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}.
     * @return an int between 0 and 3, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public int getFractalType() {
        return mode;
    }

    /**
     * Sets the current mode to one of {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}.
     * @param mode an int between 0 and 3, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public void setFractalType(int mode) {
        this.mode = (mode & 3);
    }

    /**
     * Wraps {@link #getFractalOctaves()}.
     * @return how many octaves this uses to increase detail
     */
    public int getOctaves() {
        return getFractalOctaves();
    }

    /**
     * Wraps {@link #setFractalOctaves(int)}.
     * @param octaves how many octaves to use to increase detail; must be at least 1.
     */
    public void setOctaves(int octaves) {
        setFractalOctaves(octaves);
    }

    /**
     * Gets how many octaves this uses to increase detail.
     * @return how many octaves this uses to increase detail
     */
    public int getFractalOctaves() {
        return octaves;
    }

    /**
     * Sets how many octaves this uses to increase detail; always at least 1.
     * @param octaves how many octaves to use to increase detail; must be at least 1.
     */
    public void setFractalOctaves(int octaves) {
        this.octaves = Math.max(1, octaves);
    }

    /**
     *
     * @return PerlueNoise's minimum dimension, which is 1.
     */
    public int getMinDimension() {
        return wrapped.getMinDimension();
    }

    /**
     * @return PerlueNoise's maximum dimension, which is currently 4.
     */
    public int getMaxDimension() {
        return wrapped.getMaxDimension();
    }

    /**
     * Used by Cringe, at least, to serialize its noise concisely. This doesn't really need to be used here.
     * @return a short String describing what noise class this is
     */
    public String getTag() {
        return "MiniNoise";
    }

    /**
     * Saves this MiniNoise to a String and returns it.
     * @return a String that can be fed to {@link #stringDeserialize(String)} to recreate this MiniNoise
     */
    public String stringSerialize() {
        return "`" + wrapped.seed + '~' +
                frequency + '~' +
                mode + '~' +
                octaves + '`';
    }

    /**
     * Reassigns this MiniNoise to use the serialized state produced earlier from {@link #stringSerialize()}.
     * @param data a String produced by {@link #stringSerialize()}
     * @return this MiniNoise, after restoring the state in data
     */
    public MiniNoise stringDeserialize(String data) {
        int pos = data.indexOf('`', data.indexOf('`', 2) + 1)+1;
        setWrapped(new PerlueNoise(MathSupport.intFromDec(data, 1, pos)));
        setFrequency(MathSupport.floatFromDec(data, pos+1, pos = data.indexOf('~', pos+2)));
        setMode(MathSupport.intFromDec(data, pos+1, pos = data.indexOf('~', pos+2)));
        setOctaves(MathSupport.intFromDec(data, pos+1, pos = data.indexOf('`', pos+2)));
        return this;
    }

    /**
     * Creates a MiniNoise from a String produced by {@link #stringSerialize()}.
     * @param data a serialized String, typically produced by {@link #stringSerialize()}
     * @return a new MiniNoise, using the restored state from data
     */
    public static PerlueNoise recreateFromString(String data) {
        return new PerlueNoise(1).stringDeserialize(data);
    }

    /**
     * @return returns a copy of this MiniNoise made with {@link #MiniNoise(MiniNoise)}
     */
    public MiniNoise copy() {
        return new MiniNoise(this);
    }

    public String toString() {
        return "MiniNoise{" +
                "wrapped=" + wrapped +
                ", frequency=" + frequency +
                ", mode=" + mode +
                ", octaves=" + octaves +
                '}';
    }

    /**
     * Gets a more readable version of toString(), with the mode named rather than shown as a number.
     * @return a human-readable summary of this MiniNoise
     */
    public String toHumanReadableString() {
        return getTag() + " wrapping (" + wrapped.toHumanReadableString() + "), with frequency " + frequency +
                ", " + octaves + " octaves, and mode " + MODES[mode];
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MiniNoise that = (MiniNoise) o;

        if (Float.compare(that.frequency, frequency) != 0) return false;
        if (mode != that.mode) return false;
        if (octaves != that.octaves) return false;
        return wrapped.equals(that.wrapped);
    }

    public int hashCode() {
        int result = wrapped.hashCode();
        result = 31 * result + NumberUtils.floatToIntBits(frequency + 0f);
        result = 31 * result + mode;
        result = 31 * result + octaves;
        return result;
    }

    // The big part.

    /**
     * 1D noise using the current seed, frequency, octaves, and mode.
     * @param x 1D coordinate
     * @return a float between -1 and 1
     */
    public float getNoise(float x) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, seed);
            case 1: return billow(x * frequency, seed);
            case 2: return ridged(x * frequency, seed);
            case 3: return warp(x * frequency, seed);
        }
    }

    /**
     * 2D noise using the current seed, frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @return a float between -1 and 1
     */
    public float getNoise(float x, float y) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, seed);
        }
    }
    /**
     * 3D noise using the current seed, frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param z depth coordinate (can also be time)
     * @return a float between -1 and 1
     */
    public float getNoise(float x, float y, float z) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, seed);
        }
    }

    /**
     * 4D noise using the current seed, frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param z depth coordinate
     * @param w higher-dimensional coordinate (time?)
     * @return a float between -1 and 1
     */
    public float getNoise(float x, float y, float z, float w) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, seed);
        }
    }

    public void setSeed(int seed) {
        wrapped.setSeed(seed);
    }

    public int getSeed() {
        return wrapped.getSeed();
    }

    /**
     * 1D noise forcing the given seed and using the current frequency, octaves, and mode.
     * @param x 1D coordinate
     * @param seed any int; must be the same between noise that should connect seamlessly
     * @return a float between -1 and 1
     */
    public float getNoiseWithSeed(float x, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, seed);
            case 1: return billow(x * frequency, seed);
            case 2: return ridged(x * frequency, seed);
            case 3: return warp(x * frequency, seed);
        }
    }

    /**
     * 2D noise forcing the given seed and using the current frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param seed any int; must be the same between noise that should connect seamlessly
     * @return a float between -1 and 1
     */
    public float getNoiseWithSeed(float x, float y, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, seed);
        }
    }

    /**
     * 3D noise forcing the given seed and using the current frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param z depth coordinate (can also be time)
     * @param seed any int; must be the same between noise that should connect seamlessly
     * @return a float between -1 and 1
     */
    public float getNoiseWithSeed(float x, float y, float z, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, seed);
        }
    }

    /**
     * 4D noise forcing the given seed and using the current frequency, octaves, and mode.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param z depth coordinate
     * @param w higher-dimensional coordinate (time?)
     * @param seed any int; must be the same between noise that should connect seamlessly
     * @return a float between -1 and 1
     */
    public float getNoiseWithSeed(float x, float y, float z, float w, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, seed);
        }
    }

    // 1D noise variants.

    protected float fbm(float x, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x = x * 2f;
            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 2D noise variants.

    protected float fbm(float x, float y, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x = x * 2f;
            y = y * 2f;
            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/2f));
            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 3D noise variants.

    protected float fbm(float x, float y, float z, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/3f));
            float c = MathUtils.sinDeg(idx + (180*2/3f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 4D noise variants.

    protected float fbm(float x, float y, float z, float w, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, w, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, w, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, float w, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, float w, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, float w, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, w, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/4f));
            float c = MathUtils.sinDeg(idx + (180*2/4f));
            float d = MathUtils.sinDeg(idx + (180*3/4f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, w + d, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    /**
     * Stores arrays representing vectors on the unit hypersphere in 2D through 4D. This is used by {@link PerlueNoise},
     * as well as indirectly by all classes that use it. Each constant in this class stores 256
     * unit vectors in a 1D array, one after the next, but sometimes with padding.  See the docs for each constant for more
     * information, but {@link #GRADIENTS_2D} and {@link #GRADIENTS_4D} have no padding, and {@link #GRADIENTS_3D} has
     * one ignored float after each vector.
     */
    public static final class GradientVectors {
        /**
         * No need to instantiate.
         */
        private GradientVectors() {}

//<editor-fold defaultstate="collapsed" desc="Huge constant arrays">
        /**
         * 256 equidistant points on the 2D unit circle.
         * Each point is stored in 2 floats, and there no padding. The distance from each
         * point to the origin should be 1.0, subject to rounding error.
         * <br>
         * The points were randomly rotated to try to avoid correlation on any typical axis; all points used the same
         * rotation to keep their distance to each other and shape.
         * <br>
         * This particular set of 256 gradient vectors is either optimal or very close to optimal for this size of a
         * set of vectors.
         */
        public static final float[] GRADIENTS_2D = {
                -0.9995640469f, -0.0295248389f,
                -0.9999875727f, -0.0049854169f,
                -0.9998087434f, +0.0195570081f,
                -0.9990276667f, +0.0440876528f,
                -0.9976448131f, +0.0685917407f,
                -0.9956610156f, +0.0930545115f,
                -0.9930774690f, +0.1174612297f,
                -0.9898957298f, +0.1417971937f,
                -0.9861177144f, +0.1660477444f,
                -0.9817456985f, +0.1901982741f,
                -0.9767823158f, +0.2142342354f,
                -0.9712305559f, +0.2381411501f,
                -0.9650937630f, +0.2619046175f,
                -0.9583756337f, +0.2855103233f,
                -0.9510802148f, +0.3089440483f,
                -0.9432119007f, +0.3321916771f,
                -0.9347754311f, +0.3552392059f,
                -0.9257758877f, +0.3780727520f,
                -0.9162186915f, +0.4006785612f,
                -0.9061095994f, +0.4230430166f,
                -0.8954547008f, +0.4451526467f,
                -0.8842604137f, +0.4669941335f,
                -0.8725334813f, +0.4885543205f,
                -0.8602809673f, +0.5098202206f,
                -0.8475102522f, +0.5307790241f,
                -0.8342290286f, +0.5514181061f,
                -0.8204452967f, +0.5717250345f,
                -0.8061673592f, +0.5916875772f,
                -0.7914038166f, +0.6112937093f,
                -0.7761635620f, +0.6305316210f,
                -0.7604557754f, +0.6493897240f,
                -0.7442899187f, +0.6678566589f,
                -0.7276757296f, +0.6859213020f,
                -0.7106232159f, +0.7035727717f,
                -0.6931426493f, +0.7208004354f,
                -0.6752445595f, +0.7375939160f,
                -0.6569397276f, +0.7539430975f,
                -0.6382391798f, +0.7698381319f,
                -0.6191541805f, +0.7852694447f,
                -0.5996962260f, +0.8002277404f,
                -0.5798770368f, +0.8147040089f,
                -0.5597085515f, +0.8286895302f,
                -0.5392029186f, +0.8421758798f,
                -0.5183724900f, +0.8551549343f,
                -0.4972298132f, +0.8676188753f,
                -0.4757876238f, +0.8795601952f,
                -0.4540588377f, +0.8909717009f,
                -0.4320565436f, +0.9018465186f,
                -0.4097939947f, +0.9121780977f,
                -0.3872846013f, +0.9219602148f,
                -0.3645419221f, +0.9311869775f,
                -0.3415796565f, +0.9398528280f,
                -0.3184116360f, +0.9479525463f,
                -0.2950518163f, +0.9554812534f,
                -0.2715142685f, +0.9624344144f,
                -0.2478131705f, +0.9688078409f,
                -0.2239627992f, +0.9745976937f,
                -0.1999775211f, +0.9798004853f,
                -0.1758717840f, +0.9844130818f,
                -0.1516601083f, +0.9884327046f,
                -0.1273570782f, +0.9918569325f,
                -0.1029773330f, +0.9946837029f,
                -0.0785355581f, +0.9969113131f,
                -0.0540464763f, +0.9985384211f,
                -0.0295248389f, +0.9995640469f,
                -0.0049854169f, +0.9999875727f,
                +0.0195570081f, +0.9998087434f,
                +0.0440876528f, +0.9990276667f,
                +0.0685917407f, +0.9976448131f,
                +0.0930545115f, +0.9956610156f,
                +0.1174612297f, +0.9930774690f,
                +0.1417971937f, +0.9898957298f,
                +0.1660477444f, +0.9861177144f,
                +0.1901982741f, +0.9817456985f,
                +0.2142342354f, +0.9767823158f,
                +0.2381411501f, +0.9712305559f,
                +0.2619046175f, +0.9650937630f,
                +0.2855103233f, +0.9583756337f,
                +0.3089440483f, +0.9510802148f,
                +0.3321916771f, +0.9432119007f,
                +0.3552392059f, +0.9347754311f,
                +0.3780727520f, +0.9257758877f,
                +0.4006785612f, +0.9162186915f,
                +0.4230430166f, +0.9061095994f,
                +0.4451526467f, +0.8954547008f,
                +0.4669941335f, +0.8842604137f,
                +0.4885543205f, +0.8725334813f,
                +0.5098202206f, +0.8602809673f,
                +0.5307790241f, +0.8475102522f,
                +0.5514181061f, +0.8342290286f,
                +0.5717250345f, +0.8204452967f,
                +0.5916875772f, +0.8061673592f,
                +0.6112937093f, +0.7914038166f,
                +0.6305316210f, +0.7761635620f,
                +0.6493897240f, +0.7604557754f,
                +0.6678566589f, +0.7442899187f,
                +0.6859213020f, +0.7276757296f,
                +0.7035727717f, +0.7106232159f,
                +0.7208004354f, +0.6931426493f,
                +0.7375939160f, +0.6752445595f,
                +0.7539430975f, +0.6569397276f,
                +0.7698381319f, +0.6382391798f,
                +0.7852694447f, +0.6191541805f,
                +0.8002277404f, +0.5996962260f,
                +0.8147040089f, +0.5798770368f,
                +0.8286895302f, +0.5597085515f,
                +0.8421758798f, +0.5392029186f,
                +0.8551549343f, +0.5183724900f,
                +0.8676188753f, +0.4972298132f,
                +0.8795601952f, +0.4757876238f,
                +0.8909717009f, +0.4540588377f,
                +0.9018465186f, +0.4320565436f,
                +0.9121780977f, +0.4097939947f,
                +0.9219602148f, +0.3872846013f,
                +0.9311869775f, +0.3645419221f,
                +0.9398528280f, +0.3415796565f,
                +0.9479525463f, +0.3184116360f,
                +0.9554812534f, +0.2950518163f,
                +0.9624344144f, +0.2715142685f,
                +0.9688078409f, +0.2478131705f,
                +0.9745976937f, +0.2239627992f,
                +0.9798004853f, +0.1999775211f,
                +0.9844130818f, +0.1758717840f,
                +0.9884327046f, +0.1516601083f,
                +0.9918569325f, +0.1273570782f,
                +0.9946837029f, +0.1029773330f,
                +0.9969113131f, +0.0785355581f,
                +0.9985384211f, +0.0540464763f,
                +0.9995640469f, +0.0295248389f,
                +0.9999875727f, +0.0049854169f,
                +0.9998087434f, -0.0195570081f,
                +0.9990276667f, -0.0440876528f,
                +0.9976448131f, -0.0685917407f,
                +0.9956610156f, -0.0930545115f,
                +0.9930774690f, -0.1174612297f,
                +0.9898957298f, -0.1417971937f,
                +0.9861177144f, -0.1660477444f,
                +0.9817456985f, -0.1901982741f,
                +0.9767823158f, -0.2142342354f,
                +0.9712305559f, -0.2381411501f,
                +0.9650937630f, -0.2619046175f,
                +0.9583756337f, -0.2855103233f,
                +0.9510802148f, -0.3089440483f,
                +0.9432119007f, -0.3321916771f,
                +0.9347754311f, -0.3552392059f,
                +0.9257758877f, -0.3780727520f,
                +0.9162186915f, -0.4006785612f,
                +0.9061095994f, -0.4230430166f,
                +0.8954547008f, -0.4451526467f,
                +0.8842604137f, -0.4669941335f,
                +0.8725334813f, -0.4885543205f,
                +0.8602809673f, -0.5098202206f,
                +0.8475102522f, -0.5307790241f,
                +0.8342290286f, -0.5514181061f,
                +0.8204452967f, -0.5717250345f,
                +0.8061673592f, -0.5916875772f,
                +0.7914038166f, -0.6112937093f,
                +0.7761635620f, -0.6305316210f,
                +0.7604557754f, -0.6493897240f,
                +0.7442899187f, -0.6678566589f,
                +0.7276757296f, -0.6859213020f,
                +0.7106232159f, -0.7035727717f,
                +0.6931426493f, -0.7208004354f,
                +0.6752445595f, -0.7375939160f,
                +0.6569397276f, -0.7539430975f,
                +0.6382391798f, -0.7698381319f,
                +0.6191541805f, -0.7852694447f,
                +0.5996962260f, -0.8002277404f,
                +0.5798770368f, -0.8147040089f,
                +0.5597085515f, -0.8286895302f,
                +0.5392029186f, -0.8421758798f,
                +0.5183724900f, -0.8551549343f,
                +0.4972298132f, -0.8676188753f,
                +0.4757876238f, -0.8795601952f,
                +0.4540588377f, -0.8909717009f,
                +0.4320565436f, -0.9018465186f,
                +0.4097939947f, -0.9121780977f,
                +0.3872846013f, -0.9219602148f,
                +0.3645419221f, -0.9311869775f,
                +0.3415796565f, -0.9398528280f,
                +0.3184116360f, -0.9479525463f,
                +0.2950518163f, -0.9554812534f,
                +0.2715142685f, -0.9624344144f,
                +0.2478131705f, -0.9688078409f,
                +0.2239627992f, -0.9745976937f,
                +0.1999775211f, -0.9798004853f,
                +0.1758717840f, -0.9844130818f,
                +0.1516601083f, -0.9884327046f,
                +0.1273570782f, -0.9918569325f,
                +0.1029773330f, -0.9946837029f,
                +0.0785355581f, -0.9969113131f,
                +0.0540464763f, -0.9985384211f,
                +0.0295248389f, -0.9995640469f,
                +0.0049854169f, -0.9999875727f,
                -0.0195570081f, -0.9998087434f,
                -0.0440876528f, -0.9990276667f,
                -0.0685917407f, -0.9976448131f,
                -0.0930545115f, -0.9956610156f,
                -0.1174612297f, -0.9930774690f,
                -0.1417971937f, -0.9898957298f,
                -0.1660477444f, -0.9861177144f,
                -0.1901982741f, -0.9817456985f,
                -0.2142342354f, -0.9767823158f,
                -0.2381411501f, -0.9712305559f,
                -0.2619046175f, -0.9650937630f,
                -0.2855103233f, -0.9583756337f,
                -0.3089440483f, -0.9510802148f,
                -0.3321916771f, -0.9432119007f,
                -0.3552392059f, -0.9347754311f,
                -0.3780727520f, -0.9257758877f,
                -0.4006785612f, -0.9162186915f,
                -0.4230430166f, -0.9061095994f,
                -0.4451526467f, -0.8954547008f,
                -0.4669941335f, -0.8842604137f,
                -0.4885543205f, -0.8725334813f,
                -0.5098202206f, -0.8602809673f,
                -0.5307790241f, -0.8475102522f,
                -0.5514181061f, -0.8342290286f,
                -0.5717250345f, -0.8204452967f,
                -0.5916875772f, -0.8061673592f,
                -0.6112937093f, -0.7914038166f,
                -0.6305316210f, -0.7761635620f,
                -0.6493897240f, -0.7604557754f,
                -0.6678566589f, -0.7442899187f,
                -0.6859213020f, -0.7276757296f,
                -0.7035727717f, -0.7106232159f,
                -0.7208004354f, -0.6931426493f,
                -0.7375939160f, -0.6752445595f,
                -0.7539430975f, -0.6569397276f,
                -0.7698381319f, -0.6382391798f,
                -0.7852694447f, -0.6191541805f,
                -0.8002277404f, -0.5996962260f,
                -0.8147040089f, -0.5798770368f,
                -0.8286895302f, -0.5597085515f,
                -0.8421758798f, -0.5392029186f,
                -0.8551549343f, -0.5183724900f,
                -0.8676188753f, -0.4972298132f,
                -0.8795601952f, -0.4757876238f,
                -0.8909717009f, -0.4540588377f,
                -0.9018465186f, -0.4320565436f,
                -0.9121780977f, -0.4097939947f,
                -0.9219602148f, -0.3872846013f,
                -0.9311869775f, -0.3645419221f,
                -0.9398528280f, -0.3415796565f,
                -0.9479525463f, -0.3184116360f,
                -0.9554812534f, -0.2950518163f,
                -0.9624344144f, -0.2715142685f,
                -0.9688078409f, -0.2478131705f,
                -0.9745976937f, -0.2239627992f,
                -0.9798004853f, -0.1999775211f,
                -0.9844130818f, -0.1758717840f,
                -0.9884327046f, -0.1516601083f,
                -0.9918569325f, -0.1273570782f,
                -0.9946837029f, -0.1029773330f,
                -0.9969113131f, -0.0785355581f,
                -0.9985384211f, -0.0540464763f,
        };

        /**
         * The 32 vertices of a <a href="https://en.wikipedia.org/wiki/Rhombic_triacontahedron">rhombic triacontahedron</a>,
         * normalized to lie on the unit sphere in 3D.
         * Each point is stored in 3 floats, and there is 1 float of padding after each point
         * (to allow easier access to points using bitwise operations). The distance from each
         * point to the origin should be 1.0, subject to rounding error.
         * <br>
         * The points were randomly rotated to try to avoid correlation on any typical axis; all points used the same
         * rotation to keep their distance to each other and shape. Each group of 32 vertices is repeated 8 times, each time
         * shuffled differently within that group. That means this holds 256 points, with 8 repeats of each point included
         * in that. If you only look at the first 32 points, each point will be unique.
         * <br>
         * This particular set of 32 gradient vectors is either optimal or very close to optimal for a power-of-two-sized
         * set of vectors (except for very small sets like the vertices of a tetrahedron).
         */
        public static final float[] GRADIENTS_3D = {
                -0.0752651785f, -0.7150730443f, +0.6949861108f, 0.0f,
                +0.3108026609f, +0.8973122849f, +0.3134204354f, 0.0f,
                -0.2868654574f, +0.8018987923f, +0.5240863825f, 0.0f,
                +0.8054770269f, -0.3120841869f, -0.5037958113f, 0.0f,
                +0.1282915969f, -0.1762626708f, +0.9759470975f, 0.0f,
                -0.5488958567f, -0.2924528438f, +0.7830610913f, 0.0f,
                +0.6888417674f, +0.0066053064f, +0.7248816383f, 0.0f,
                -0.1262089260f, +0.9809321878f, -0.1477949592f, 0.0f,
                +0.2902352898f, +0.5000838360f, +0.8158919252f, 0.0f,
                -0.5501830181f, -0.7897659550f, +0.2712349220f, 0.0f,
                -0.9653400724f, +0.1883955081f, -0.1806257930f, 0.0f,
                +0.9255483484f, +0.2703878154f, -0.2650484559f, 0.0f,
                +0.0752651785f, +0.7150730443f, -0.6949861108f, 0.0f,
                -0.8075596979f, -0.4925853301f, -0.3243563271f, 0.0f,
                -0.9255483484f, -0.2703878154f, +0.2650484559f, 0.0f,
                -0.2902352898f, -0.5000838360f, -0.8158919252f, 0.0f,
                +0.8075596979f, +0.4925853301f, +0.3243563271f, 0.0f,
                +0.5488958567f, +0.2924528438f, -0.7830610913f, 0.0f,
                +0.5501830181f, +0.7897659550f, -0.2712349220f, 0.0f,
                -0.8054770269f, +0.3120841869f, +0.5037958113f, 0.0f,
                -0.6869453017f, +0.7261211210f, +0.0292278996f, 0.0f,
                -0.6888417674f, -0.0066053064f, -0.7248816383f, 0.0f,
                -0.3077341151f, +0.2882639790f, +0.9067544281f, 0.0f,
                +0.1262089260f, -0.9809321878f, +0.1477949592f, 0.0f,
                +0.5455292986f, -0.6017663059f, +0.5833310359f, 0.0f,
                -0.5455292986f, +0.6017663059f, -0.5833310359f, 0.0f,
                +0.2868654574f, -0.8018987923f, -0.5240863825f, 0.0f,
                -0.3108026609f, -0.8973122849f, -0.3134204354f, 0.0f,
                +0.9653400724f, -0.1883955081f, +0.1806257930f, 0.0f,
                -0.1282915969f, +0.1762626708f, -0.9759470975f, 0.0f,
                +0.3077341151f, -0.2882639790f, -0.9067544281f, 0.0f,
                +0.6869453017f, -0.7261211210f, -0.0292278996f, 0.0f,
        };
        /**
         * 256 quasi-random 4D unit vectors, generated by taking the "Super-Fibonacci spiral" and gradually adjusting
         * its points until they reached equilibrium. 128 vectors were generated this way, and the remaining
         * 128 are the polar opposite points of each of those vectors (their antipodes).
         * <a href="https://marcalexa.github.io/superfibonacci/">This page by Marc Alexa</a> covers the spiral, though
         * an empirical search found better constants for our specific number of points (128, plus their antipodes).
         * <br>
         * Each point is stored in 4 floats, and there is no padding between points. The distance from each
         * point to the origin should be 1.0, subject to rounding error.
         * Each point is unique, and as long as you sample a point in the expected way (starting on a multiple
         * of four, using four sequential floats as the x, y, z, w components), the points will all be unit vectors.
         * The points were shuffled here to avoid any correlation when sampling in sequence.
         * <br>
         * This particular set of 256 gradient vectors is fairly close to optimal for a power-of-two-sized
         * set of vectors. A set of 256 unit vectors with all-equal distances to their neighbors can't be made.
         * The minimum distance (in 4D Euclidean space) between any two points in this set is 0.369142885554421 .
         * <br>
         * This also doesn't have any easily-noticed visual artifacts, which is much better than the previous
         * set of 64 gradient vectors using a truncated tesseract. Those 64 points were much more likely to
         * lie on straight lines when projected using a certain method, and also had rather clear angles.
         * The "certain method" is to take points on the surface of a sphere in 4D (properly, the "3-sphere"
         * because it has 3 degrees of freedom), and ignore two coordinates while keeping the other two.
         * This should produce 2D points uniformly distributed across the inside of a circle (properly, the
         * "2-ball" because it has 2 degrees of freedom, and apparently a ball is solid, but a sphere is not).
         * Repeating this for each possible pair that can be dropped shows some good views of an otherwise
         * hard-to-contemplate shape. This method does show spirals for certain coordinates (xy and zw show
         * spirals, but xz, zw, and yw do not). This shouldn't be too obvious in practice.
         */
        public static final float[] GRADIENTS_4D = {
                -0.1500775665f, +0.2370168269f, +0.0776990876f, +0.9566935301f,
                -0.2168772668f, +0.5047958493f, +0.1017048284f, -0.8293379545f,
                +0.0763605088f, -0.8615859151f, -0.2528905869f, +0.4334572554f,
                +0.1154861227f, -0.4066920578f, -0.8755832911f, +0.2337058932f,
                -0.1197163314f, -0.6982775331f, -0.1660969406f, -0.6859214902f,
                +0.3497577906f, -0.2025230676f, +0.1025419161f, +0.9089220166f,
                +0.7692472339f, +0.4585693777f, -0.3172645569f, +0.3119552135f,
                +0.9011059999f, +0.1688479185f, -0.1417298466f, -0.3733778596f,
                +0.5451716781f, -0.3883906007f, +0.3582917750f, +0.6508207321f,
                +0.0551428460f, +0.3906531036f, +0.6245298982f, -0.6740266681f,
                -0.0720681995f, +0.3346361816f, -0.8879311085f, -0.3072509468f,
                +0.5837373734f, +0.4879808724f, +0.3614648283f, -0.5389514565f,
                +0.4248800874f, -0.2106733918f, -0.5714059472f, -0.6697677970f,
                +0.2736921608f, +0.1859380007f, -0.3482927382f, -0.8770473003f,
                -0.9203012586f, -0.2347503006f, -0.2557854056f, -0.1803098917f,
                +0.4820273817f, -0.3926214278f, +0.0709615499f, -0.7800400257f,
                -0.4847539961f, -0.1899734437f, -0.3473681211f, +0.7799096107f,
                -0.1541055739f, -0.6454571486f, -0.2610019147f, +0.7010810971f,
                -0.9011059999f, -0.1688479185f, +0.1417298466f, +0.3733778596f,
                +0.3607165217f, +0.3201324046f, -0.8760028481f, -0.0042417473f,
                +0.7660096288f, +0.3920440078f, +0.5009348989f, -0.0927091315f,
                +0.3528687060f, -0.3726457357f, +0.8218995333f, -0.2471843809f,
                +0.2098862231f, +0.4600366652f, -0.7274983525f, -0.4637458026f,
                -0.9244774580f, +0.1559440494f, -0.3474252224f, -0.0178527124f,
                -0.6069507003f, -0.0521510802f, +0.7704458237f, +0.1878946275f,
                -0.7102735639f, +0.5383709669f, +0.4519570172f, +0.0374585539f,
                -0.6069343090f, +0.3758327067f, +0.6232035160f, +0.3193713129f,
                +0.4562335312f, -0.5830481052f, +0.5214208961f, -0.4242948592f,
                -0.0197402649f, +0.8283793926f, -0.5044059157f, +0.2428428233f,
                -0.8456494808f, -0.0475203022f, -0.3905497193f, +0.3606795967f,
                -0.7692472339f, -0.4585693777f, +0.3172645569f, -0.3119552135f,
                -0.3915876150f, -0.5137497783f, +0.1563476324f, +0.7471785545f,
                +0.6474077702f, -0.5600305796f, +0.4030532837f, +0.3236929178f,
                +0.8314927220f, -0.4754710495f, -0.1289689094f, +0.2567377090f,
                -0.5837687254f, +0.5748279691f, +0.1695513278f, +0.5477584600f,
                -0.3913940787f, -0.4500352442f, +0.3034601212f, -0.7430955172f,
                -0.0479935408f, -0.1738562137f, -0.9190009832f, -0.3505822122f,
                +0.1591796726f, -0.1220099106f, -0.8619921803f, +0.4655586779f,
                +0.3481253982f, -0.0606551990f, +0.8087866306f, -0.4700999558f,
                -0.2736921608f, -0.1859380007f, +0.3482927382f, +0.8770473003f,
                +0.3593838513f, -0.7619267702f, +0.5353092551f, +0.0612759069f,
                +0.2731977105f, -0.5373794436f, -0.3627216220f, -0.7106472254f,
                +0.5682965517f, +0.6156543493f, -0.3069647551f, -0.4514216185f,
                -0.3481253982f, +0.0606551990f, -0.8087866306f, +0.4700999558f,
                -0.7229727507f, +0.1000640318f, +0.3482705951f, +0.5882220864f,
                +0.6173728108f, -0.1686241180f, +0.4670835435f, -0.6101226807f,
                -0.0058717611f, -0.5279526114f, +0.3622214794f, +0.7681323290f,
                -0.5682965517f, -0.6156543493f, +0.3069647551f, +0.4514216185f,
                +0.1812587827f, +0.5881740451f, -0.6412660480f, +0.4582296312f,
                -0.1640486717f, +0.4619357288f, -0.5720989704f, +0.6575760841f,
                -0.6290537119f, +0.7484779954f, +0.1338096410f, +0.1617629528f,
                +0.1370694786f, -0.8893449903f, -0.0286894143f, -0.4352636933f,
                -0.6868565679f, -0.1498538405f, +0.4144212902f, -0.5779507160f,
                +0.0627557933f, -0.1148957163f, -0.4865127206f, +0.8638090491f,
                +0.7229727507f, -0.1000640318f, -0.3482705951f, -0.5882220864f,
                -0.5501986146f, -0.3024502695f, +0.5412290096f, +0.5593536496f,
                -0.4975182116f, +0.0216092784f, +0.1241487861f, +0.8582515717f,
                -0.3732183576f, -0.0930780172f, -0.9224595428f, -0.0333623886f,
                -0.3935349584f, +0.5759038925f, +0.7137228251f, -0.0637555048f,
                +0.7688309550f, -0.0562318675f, +0.5756873488f, +0.2726191282f,
                -0.4923638999f, +0.7683649659f, -0.1948917806f, +0.3594581783f,
                +0.7245696187f, -0.3282309175f, -0.4578595459f, +0.3970240057f,
                -0.3815847635f, -0.3854267299f, -0.6985180974f, +0.4668103755f,
                -0.1925185025f, -0.3090348840f, -0.1709938943f, +0.9155299664f,
                -0.0763605088f, +0.8615859151f, +0.2528905869f, -0.4334572554f,
                +0.8098008633f, -0.0068325223f, -0.5799278021f, +0.0886552259f,
                -0.0311528668f, +0.5825442076f, +0.6982141733f, +0.4149323702f,
                +0.2348341793f, -0.7289867997f, +0.1708114296f, +0.6198827624f,
                +0.8799446225f, -0.2076391280f, -0.3413696587f, -0.2570025623f,
                -0.7166067362f, -0.2055816799f, -0.0187202729f, +0.6662286520f,
                -0.2871971726f, +0.3454537392f, +0.8530229926f, +0.2655773759f,
                +0.0479935408f, +0.1738562137f, +0.9190009832f, +0.3505822122f,
                +0.3427777886f, +0.6960333586f, +0.5767480731f, -0.2557395995f,
                +0.0352323204f, +0.4647250772f, -0.1351369023f, +0.8743725419f,
                +0.5837687254f, -0.5748279691f, -0.1695513278f, -0.5477584600f,
                +0.1640486717f, -0.4619357288f, +0.5720989704f, -0.6575760841f,
                -0.0346745104f, +0.2332650721f, +0.5832293630f, +0.7773215175f,
                +0.3666544557f, +0.6371533275f, -0.6673937440f, -0.1191042140f,
                +0.2688287199f, -0.9276711345f, -0.2462313175f, -0.0807942897f,
                +0.2168772668f, -0.5047958493f, -0.1017048284f, +0.8293379545f,
                -0.7688309550f, +0.0562318675f, -0.5756873488f, -0.2726191282f,
                -0.2891888618f, -0.0525769927f, +0.7722691298f, +0.5632103682f,
                +0.5796207190f, +0.5517915487f, +0.0019261374f, +0.5996351242f,
                +0.8315265775f, -0.2538107038f, +0.1608182788f, +0.4672057331f,
                -0.4562335312f, +0.5830481052f, -0.5214208961f, +0.4242948592f,
                +0.0321676619f, +0.7899427414f, +0.6058839560f, +0.0886588320f,
                +0.4847539961f, +0.1899734437f, +0.3473681211f, -0.7799096107f,
                +0.1886299253f, +0.1110786423f, -0.2090769559f, +0.9530829787f,
                +0.9647145867f, -0.0993151814f, -0.2088250220f, +0.1259146184f,
                +0.4292620420f, -0.0465219989f, -0.3925911188f, +0.8120603561f,
                -0.2002530843f, -0.5264436007f, -0.8260012269f, +0.0218623430f,
                -0.6173728108f, +0.1686241180f, -0.4670835435f, +0.6101226807f,
                +0.4251541793f, -0.7684340477f, -0.4242341518f, +0.2208584994f,
                -0.5605144501f, +0.7617027164f, -0.0315162092f, -0.3234800994f,
                -0.3830938637f, -0.5762633681f, -0.5179871321f, -0.5028410554f,
                -0.5804424882f, -0.4247357547f, +0.6436215043f, -0.2616055906f,
                +0.0746164620f, -0.6295408010f, +0.7720863223f, -0.0446486510f,
                -0.2014599144f, +0.9114251137f, -0.2199151367f, -0.2834706008f,
                -0.8888183832f, +0.4347586334f, +0.0185530689f, +0.1436756402f,
                -0.0321676619f, -0.7899427414f, -0.6058839560f, -0.0886588320f,
                -0.3263846636f, -0.2539949715f, -0.7520564198f, -0.5131967068f,
                +0.9244774580f, -0.1559440494f, +0.3474252224f, +0.0178527124f,
                +0.2957792580f, -0.7487596273f, -0.4506414831f, -0.3857408762f,
                +0.7796046734f, -0.2941354215f, +0.1434659064f, -0.5339647532f,
                -0.1591796726f, +0.1220099106f, +0.8619921803f, -0.4655586779f,
                +0.1925185025f, +0.3090348840f, +0.1709938943f, -0.9155299664f,
                +0.5500568151f, +0.6789613962f, +0.4516335726f, +0.1802109927f,
                -0.1282755584f, -0.9451470971f, -0.2913866341f, +0.0730489343f,
                +0.4975182116f, -0.0216092784f, -0.1241487861f, -0.8582515717f,
                -0.1235827729f, +0.6694590449f, +0.5823508501f, -0.4443190992f,
                +0.7102735639f, -0.5383709669f, -0.4519570172f, -0.0374585539f,
                -0.3593838513f, +0.7619267702f, -0.5353092551f, -0.0612759069f,
                -0.7660096288f, -0.3920440078f, -0.5009348989f, +0.0927091315f,
                -0.1886299253f, -0.1110786423f, +0.2090769559f, -0.9530829787f,
                +0.4360558987f, +0.1391784698f, -0.7543152571f, +0.4706306756f,
                -0.0551428460f, -0.3906531036f, -0.6245298982f, +0.6740266681f,
                +0.2002530843f, +0.5264436007f, +0.8260012269f, -0.0218623430f,
                -0.0623782203f, -0.3003311753f, +0.9268308878f, -0.2165521234f,
                -0.4292620420f, +0.0465219989f, +0.3925911188f, -0.8120603561f,
                +0.6668652296f, -0.3556269109f, +0.6501035094f, -0.0786493346f,
                -0.1370694786f, +0.8893449903f, +0.0286894143f, +0.4352636933f,
                +0.3815847635f, +0.3854267299f, +0.6985180974f, -0.4668103755f,
                +0.1565428525f, +0.0181281455f, +0.3355644941f, +0.9287422895f,
                -0.2957792580f, +0.7487596273f, +0.4506414831f, +0.3857408762f,
                -0.5796207190f, -0.5517915487f, -0.0019261374f, -0.5996351242f,
                -0.2652045786f, +0.1773467511f, -0.4390128851f, +0.8399300575f,
                -0.6503618360f, -0.0984814689f, -0.7063113451f, +0.2616396546f,
                -0.2203493118f, +0.3028321564f, -0.5794181228f, -0.7238878608f,
                +0.1772385240f, -0.7025631666f, +0.2201232612f, -0.6530982852f,
                -0.7717817426f, +0.5101420879f, -0.3310671151f, +0.1857492775f,
                +0.9335333705f, +0.3035431206f, -0.1906893849f, -0.0038217760f,
                -0.8314927220f, +0.4754710495f, +0.1289689094f, -0.2567377090f,
                -0.4938579202f, +0.2063272744f, -0.8139879704f, -0.2257363349f,
                -0.4935885072f, -0.8018989563f, -0.2325375825f, +0.2434232235f,
                +0.2652045786f, -0.1773467511f, +0.4390128851f, -0.8399300575f,
                +0.4923638999f, -0.7683649659f, +0.1948917806f, -0.3594581783f,
                +0.3263846636f, +0.2539949715f, +0.7520564198f, +0.5131967068f,
                +0.2014599144f, -0.9114251137f, +0.2199151367f, +0.2834706008f,
                -0.2445772290f, +0.0590920746f, +0.9656262994f, +0.0652375147f,
                -0.5198469758f, +0.5825917125f, +0.1809202135f, -0.5980083942f,
                -0.9647145867f, +0.0993151814f, +0.2088250220f, -0.1259146184f,
                +0.0311528668f, -0.5825442076f, -0.6982141733f, -0.4149323702f,
                -0.2313889712f, -0.8562101722f, -0.2715738714f, -0.3736455441f,
                -0.2083578706f, -0.3882177770f, -0.3572764695f, -0.8235457540f,
                +0.0692037940f, +0.9831219912f, -0.1130947843f, +0.1260616034f,
                +0.2747297585f, +0.8059790730f, -0.0180938281f, -0.5240172148f,
                -0.5578742027f, +0.1918342263f, +0.7655780911f, -0.2566442788f,
                -0.6668652296f, +0.3556269109f, -0.6501035094f, +0.0786493346f,
                +0.1551940888f, +0.2166104019f, -0.6768955588f, +0.6861539483f,
                +0.0623782203f, +0.3003311753f, -0.9268308878f, +0.2165521234f,
                +0.6290537119f, -0.7484779954f, -0.1338096410f, -0.1617629528f,
                +0.6069343090f, -0.3758327067f, -0.6232035160f, -0.3193713129f,
                +0.6724284291f, -0.1377824694f, -0.0431847833f, +0.7259415984f,
                +0.3322996795f, -0.5608487725f, +0.6636862755f, +0.3668054342f,
                -0.1812587827f, -0.5881740451f, +0.6412660480f, -0.4582296312f,
                +0.5804424882f, +0.4247357547f, -0.6436215043f, +0.2616055906f,
                -0.6724284291f, +0.1377824694f, +0.0431847833f, -0.7259415984f,
                +0.7561704516f, +0.3272831738f, -0.5194675922f, -0.2263747603f,
                -0.1154861227f, +0.4066920578f, +0.8755832911f, -0.2337058932f,
                +0.2203493118f, -0.3028321564f, +0.5794181228f, +0.7238878608f,
                +0.1282755584f, +0.9451470971f, +0.2913866341f, -0.0730489343f,
                +0.6868565679f, +0.1498538405f, -0.4144212902f, +0.5779507160f,
                +0.3935349584f, -0.5759038925f, -0.7137228251f, +0.0637555048f,
                +0.3732183576f, +0.0930780172f, +0.9224595428f, +0.0333623886f,
                +0.7658822536f, +0.5263185501f, +0.0733225495f, -0.3619902432f,
                -0.3427777886f, -0.6960333586f, -0.5767480731f, +0.2557395995f,
                -0.8799446225f, +0.2076391280f, +0.3413696587f, +0.2570025623f,
                -0.0627557933f, +0.1148957163f, +0.4865127206f, -0.8638090491f,
                -0.5451716781f, +0.3883906007f, -0.3582917750f, -0.6508207321f,
                -0.0615775883f, -0.0751815885f, -0.9757779241f, +0.1959938258f,
                -0.3120108247f, -0.7524400353f, +0.3078546822f, -0.4916389883f,
                -0.5250310302f, -0.7412517071f, +0.4086282849f, -0.0889459178f,
                -0.4820273817f, +0.3926214278f, -0.0709615499f, +0.7800400257f,
                +0.0197402649f, -0.8283793926f, +0.5044059157f, -0.2428428233f,
                +0.5501986146f, +0.3024502695f, -0.5412290096f, -0.5593536496f,
                -0.8761548996f, -0.1062743291f, +0.1077154949f, -0.4576633871f,
                -0.4559036791f, -0.8604559898f, +0.0381671526f, -0.2243002802f,
                +0.0058717611f, +0.5279526114f, -0.3622214794f, -0.7681323290f,
                -0.9335333705f, -0.3035431206f, +0.1906893849f, +0.0038217760f,
                -0.5222824216f, -0.2446532995f, -0.1422012448f, -0.8044530153f,
                -0.0788757727f, -0.7464979887f, +0.4882335961f, +0.4451375306f,
                -0.5693851709f, -0.3627055287f, -0.7218432426f, -0.1522749811f,
                -0.5837373734f, -0.4879808724f, -0.3614648283f, +0.5389514565f,
                +0.1500775665f, -0.2370168269f, -0.0776990876f, -0.9566935301f,
                +0.4935885072f, +0.8018989563f, +0.2325375825f, -0.2434232235f,
                -0.7796046734f, +0.2941354215f, -0.1434659064f, +0.5339647532f,
                +0.5693851709f, +0.3627055287f, +0.7218432426f, +0.1522749811f,
                +0.5222824216f, +0.2446532995f, +0.1422012448f, +0.8044530153f,
                +0.4178254604f, -0.3877674937f, -0.5196145773f, +0.6364424825f,
                -0.3528687060f, +0.3726457357f, -0.8218995333f, +0.2471843809f,
                +0.3915876150f, +0.5137497783f, -0.1563476324f, -0.7471785545f,
                +0.0788757727f, +0.7464979887f, -0.4882335961f, -0.4451375306f,
                +0.5578742027f, -0.1918342263f, -0.7655780911f, +0.2566442788f,
                +0.5198469758f, -0.5825917125f, -0.1809202135f, +0.5980083942f,
                -0.0746164620f, +0.6295408010f, -0.7720863223f, +0.0446486510f,
                -0.3666544557f, -0.6371533275f, +0.6673937440f, +0.1191042140f,
                +0.7558788657f, +0.5948066711f, +0.1050633341f, +0.2526143789f,
                +0.6503618360f, +0.0984814689f, +0.7063113451f, -0.2616396546f,
                -0.3607165217f, -0.3201324046f, +0.8760028481f, +0.0042417473f,
                +0.7397086620f, +0.2649133801f, +0.3316588402f, +0.5221632123f,
                +0.7166067362f, +0.2055816799f, +0.0187202729f, -0.6662286520f,
                +0.8456494808f, +0.0475203022f, +0.3905497193f, -0.3606795967f,
                +0.4559036791f, +0.8604559898f, -0.0381671526f, +0.2243002802f,
                +0.7394540310f, +0.6632617712f, -0.0962664708f, -0.0634394437f,
                -0.1824457794f, -0.9027240276f, +0.3662159443f, +0.1329995543f,
                +0.3913940787f, +0.4500352442f, -0.3034601212f, +0.7430955172f,
                -0.7245696187f, +0.3282309175f, +0.4578595459f, -0.3970240057f,
                -0.4251541793f, +0.7684340477f, +0.4242341518f, -0.2208584994f,
                +0.9203012586f, +0.2347503006f, +0.2557854056f, +0.1803098917f,
                -0.0692037940f, -0.9831219912f, +0.1130947843f, -0.1260616034f,
                -0.1772385240f, +0.7025631666f, -0.2201232612f, +0.6530982852f,
                +0.0346745104f, -0.2332650721f, -0.5832293630f, -0.7773215175f,
                +0.1197163314f, +0.6982775331f, +0.1660969406f, +0.6859214902f,
                -0.1551940888f, -0.2166104019f, +0.6768955588f, -0.6861539483f,
                -0.4248800874f, +0.2106733918f, +0.5714059472f, +0.6697677970f,
                -0.1565428525f, -0.0181281455f, -0.3355644941f, -0.9287422895f,
                -0.4360558987f, -0.1391784698f, +0.7543152571f, -0.4706306756f,
                -0.5562461615f, -0.0117739718f, -0.5588256121f, -0.6149517298f,
                +0.3120108247f, +0.7524400353f, -0.3078546822f, +0.4916389883f,
                +0.5250310302f, +0.7412517071f, -0.4086282849f, +0.0889459178f,
                -0.3322996795f, +0.5608487725f, -0.6636862755f, -0.3668054342f,
                +0.1235827729f, -0.6694590449f, -0.5823508501f, +0.4443190992f,
                -0.7394540310f, -0.6632617712f, +0.0962664708f, +0.0634394437f,
                +0.2891888618f, +0.0525769927f, -0.7722691298f, -0.5632103682f,
                -0.5500568151f, -0.6789613962f, -0.4516335726f, -0.1802109927f,
                +0.1824457794f, +0.9027240276f, -0.3662159443f, -0.1329995543f,
                -0.2348341793f, +0.7289867997f, -0.1708114296f, -0.6198827624f,
                -0.6474077702f, +0.5600305796f, -0.4030532837f, -0.3236929178f,
                +0.0720681995f, -0.3346361816f, +0.8879311085f, +0.3072509468f,
                -0.2747297585f, -0.8059790730f, +0.0180938281f, +0.5240172148f,
                +0.2871971726f, -0.3454537392f, -0.8530229926f, -0.2655773759f,
                -0.8098008633f, +0.0068325223f, +0.5799278021f, -0.0886552259f,
                -0.2098862231f, -0.4600366652f, +0.7274983525f, +0.4637458026f,
                +0.1541055739f, +0.6454571486f, +0.2610019147f, -0.7010810971f,
                -0.3497577906f, +0.2025230676f, -0.1025419161f, -0.9089220166f,
                -0.2688287199f, +0.9276711345f, +0.2462313175f, +0.0807942897f,
                +0.4938579202f, -0.2063272744f, +0.8139879704f, +0.2257363349f,
                -0.7561704516f, -0.3272831738f, +0.5194675922f, +0.2263747603f,
                -0.2731977105f, +0.5373794436f, +0.3627216220f, +0.7106472254f,
                +0.6069507003f, +0.0521510802f, -0.7704458237f, -0.1878946275f,
                -0.0352323204f, -0.4647250772f, +0.1351369023f, -0.8743725419f,
                -0.7658822536f, -0.5263185501f, -0.0733225495f, +0.3619902432f,
                -0.8315265775f, +0.2538107038f, -0.1608182788f, -0.4672057331f,
                +0.3830938637f, +0.5762633681f, +0.5179871321f, +0.5028410554f,
                +0.5562461615f, +0.0117739718f, +0.5588256121f, +0.6149517298f,
                -0.7397086620f, -0.2649133801f, -0.3316588402f, -0.5221632123f,
                +0.2313889712f, +0.8562101722f, +0.2715738714f, +0.3736455441f,
                +0.2083578706f, +0.3882177770f, +0.3572764695f, +0.8235457540f,
                -0.4178254604f, +0.3877674937f, +0.5196145773f, -0.6364424825f,
                -0.7558788657f, -0.5948066711f, -0.1050633341f, -0.2526143789f,
                +0.7717817426f, -0.5101420879f, +0.3310671151f, -0.1857492775f,
                +0.0615775883f, +0.0751815885f, +0.9757779241f, -0.1959938258f,
                +0.2445772290f, -0.0590920746f, -0.9656262994f, -0.0652375147f,
                +0.8888183832f, -0.4347586334f, -0.0185530689f, -0.1436756402f,
                +0.8761548996f, +0.1062743291f, -0.1077154949f, +0.4576633871f,
                +0.5605144501f, -0.7617027164f, +0.0315162092f, +0.3234800994f,
        };
//</editor-fold>

    }

    /**
     * A mix of "Classic" Perlin noise, written by Ken Perlin before he created Simplex Noise, with value noise calculated
     * at the same time. This uses cubic interpolation throughout (instead of the quintic interpolation used in Simplex
     * Noise, because cubic looks a little smoother and doesn't alternate as badly between sharp change and low change), and
     * has a single {@code int} seed. Perlue Noise can have significant grid-aligned and 45-degree-diagonal artifacts when
     * too few octaves are used, but sometimes this is irrelevant, such as when sampling 3D noise on the surface of a
     * sphere. These artifacts sometimes manifest as "waves" of quickly-changing and then slowly-changing noise, when 3D
     * noise uses time as the z axis.
     * <br>
     * This tends to look fairly different from vanilla PerlinNoise or ValueNoise; it is capable of more chaotic
     * arrangements of high and low values than either of those, but it still tends to have clusters of values of a specific
     * size more often than clusters with very different sizes.
     */
    public static class PerlueNoise {
        public static final PerlueNoise instance = new PerlueNoise();

        private static final float SCALE2 = 1.41421330f; //towardsZero(1f/ (float) Math.sqrt(2f / 4f));
        private static final float SCALE3 = 1.15470030f; //towardsZero(1f/ (float) Math.sqrt(3f / 4f));
        private static final float SCALE4 = 0.99999990f; //towardsZero(1f)                            ;
        private static final float EQ_ADD_2 = 1.0f / 0.85f;
        private static final float EQ_ADD_3 = 0.8f / 0.85f;
        private static final float EQ_ADD_4 = 0.6f / 0.85f;
        private static final float EQ_MUL_2 = 1.2535664f;
        private static final float EQ_MUL_3 = 1.2071217f;
        private static final float EQ_MUL_4 = 1.1588172f;

        public int seed;

        public PerlueNoise() {
            this(0x1337BEEF);
        }

        public PerlueNoise(final int seed) {
            this.seed = seed;
        }

        public PerlueNoise(PerlueNoise other) {
            this.seed = other.seed;
        }

        /**
         * Gets the minimum dimension supported by this generator, which is 1.
         *
         * @return the minimum supported dimension, 1
         */
        public int getMinDimension() {
            return 1;
        }

        /**
         * Gets the maximum dimension supported by this generator, which is 4.
         *
         * @return the maximum supported dimension, 4
         */
        public int getMaxDimension() {
            return 4;
        }

        /**
         * Sets the seed to the given int.
         * @param seed an int seed, with no restrictions
         */
        public void setSeed(int seed) {
            this.seed = seed;
        }

        /**
         * Gets the current seed of the generator, as an int.
         *
         * @return the current seed, as an int
         */
        public int getSeed() {
            return seed;
        }

        /**
         * Returns the constant String {@code "PerlueNoise"} that identifies this in serialized Strings.
         *
         * @return a short String constant that identifies this RawNoise type, {@code "PerlueNoise"}
         */
        public String getTag() {
            return "PerlueNoise";
        }

        /**
         * Creates a copy of this PerlueNoise, which should be a deep copy for any mutable state but can be shallow for immutable
         * types such as functions. This almost always just calls a copy constructor.
         *
         * @return a copy of this PerlueNoise
         */
        public PerlueNoise copy() {
            return new PerlueNoise(this.seed);
        }

        /**
         * Hashes x, y, and the seed, then uses that hash both to look up a gradient vector and as a value noise result.
         * @param seed any int
         * @param x x position of a lattice point
         * @param y y position of a lattice point
         * @param xd how far on x the requested point is from the lattice point
         * @param yd how far on y the requested point is from the lattice point
         * @return a float that's uh... close to 0, and not too much outside -1 to 1...
         */
        protected static float gradCoord2D(int seed, int x, int y,
                                           float xd, float yd) {
            final int h = hashAll(x, y, seed);
            final int hash = h & (255 << 1);
            return (h * 0x1p-32f) + xd * GRADIENTS_2D[hash] + yd * GRADIENTS_2D[hash + 1];
        }

        /**
         * Hashes x, y, z, and the seed, then uses that hash both to look up a gradient vector and as a value noise result.
         * @param seed any int
         * @param x x position of a lattice point
         * @param y y position of a lattice point
         * @param z z position of a lattice point
         * @param xd how far on x the requested point is from the lattice point
         * @param yd how far on y the requested point is from the lattice point
         * @param zd how far on z the requested point is from the lattice point
         * @return a float that's uh... close to 0, and not too much outside -1 to 1...
         */
        protected static float gradCoord3D(int seed, int x, int y, int z, float xd, float yd, float zd) {
            final int h = hashAll(x, y, z, seed);
            final int hash = h & (31 << 2);
            return (h * 0x1p-32f) + xd * GRADIENTS_3D[hash] + yd * GRADIENTS_3D[hash + 1] + zd * GRADIENTS_3D[hash + 2];
        }

        /**
         * Hashes x, y, z, w, and the seed, then uses that hash both to look up a gradient vector and as a value noise result.
         * @param seed any int
         * @param x x position of a lattice point
         * @param y y position of a lattice point
         * @param z z position of a lattice point
         * @param w w position of a lattice point
         * @param xd how far on x the requested point is from the lattice point
         * @param yd how far on y the requested point is from the lattice point
         * @param zd how far on z the requested point is from the lattice point
         * @param wd how far on w the requested point is from the lattice point
         * @return a float that's uh... close to 0, and not too much outside -1 to 1...
         */
        protected static float gradCoord4D(int seed, int x, int y, int z, int w,
                                           float xd, float yd, float zd, float wd) {
            final int h = hashAll(x, y, z, w, seed);
            final int hash = h & (255 << 2);
            return (h * 0x1p-32f) + xd * GRADIENTS_4D[hash] + yd * GRADIENTS_4D[hash + 1] + zd * GRADIENTS_4D[hash + 2] + wd * GRADIENTS_4D[hash + 3];
        }

        /**
         * Given inputs as {@code x} in the range -1.0 to 1.0 that are too biased towards 0.0, this "squashes" the range
         * softly to widen it and spread it away from 0.0 without increasing bias anywhere else.
         * <br>
         * This starts with a common sigmoid function, {@code x / sqrt(x * x + add)}, but instead of approaching -1 and 1
         * but never reaching them, this multiplies the result so the line crosses -1 when x is -1, and crosses 1 when x is
         * 1. It has a smooth derivative, if that matters to you.
         *
         * @param x a float between -1 and 1
         * @param add if greater than 1, this will have nearly no effect; the lower this goes below 1, the more this will
         *           separate results near the center of the range. This must be greater than or equal to 0.0
         * @param mul typically the result of calling {@code (float) Math.sqrt(add + 1f)}
         * @return a float with a slightly different distribution from {@code x}, but still between -1 and 1
         */
        public static float equalize(float x, float add, float mul) {
            return x * mul / (float) Math.sqrt(x * x + add);
        }

        /**
         * Just calls {@link #getNoiseWithSeed(float, int)} using our internal {@link #seed}.
         * @param x a distance traveled; should change by less than 1 between calls, and should be less than about 10000
         * @return a smoothly-interpolated swaying value between -1 and 1, both exclusive
         */
        public float getNoise(final float x) {
            return getNoiseWithSeed(x, seed);
        }

        /**
         * Sway smoothly using bicubic interpolation between 4 points (the two integers before x and the two after).
         * This pretty much never produces steep changes between peaks and valleys; this may make it more useful for
         * things like generating terrain that can be walked across in a side-scrolling game.
         *
         * @param x    a distance traveled; should change by less than 1 between calls, and should be less than about 10000
         * @param seed any long
         * @return a smoothly-interpolated swaying value between -1 and 1, both exclusive
         */
        public float getNoiseWithSeed(float x, int seed)
        {
            final long floor = (long) Math.floor(x);
            // what we add here ensures that at the very least, the upper half will have some non-zero bits.
            long s = ((seed & 0xFFFFFFFFL) ^ (seed >>> 16)) + 0x9E3779B97F4A7C15L;
            // fancy XOR-rotate-rotate is a way to mix bits both up and down without multiplication.
            s = (s ^ (s << 21 | s >>> 43) ^ (s << 50 | s >>> 14)) + floor;
            // we use a different technique here, relative to other wobble methods.
            // to avoid frequent multiplication and replace it with addition by constants, we track 3 variables, each of
            // which updates with a different large, negative long increment. when we want to get a result, we just XOR
            // m, n, and o, and use only the upper bits (by multiplying by a tiny fraction).
            // the multipliers used for m, n, and o are each 2 to the 64 divided by "very irrational numbers" or their
            // powers, with these three numbers all part of a related series.
            final long m = s * 0xD1B54A32D192ED03L;
            final long n = s * 0xABC98388FB8FAC03L;
            final long o = s * 0x8CB92BA72F3D8DD7L;

            // each of the complicated hex constants is a multiple of a "very irrational" multiplier from above.
            // a uses 0 times the m, n, or o multiplier, b uses 1 times that, c uses 2 times that, and d uses 3 times.
            final float a = (m ^ n ^ o);
            final float b = (m + 0xD1B54A32D192ED03L ^ n + 0xABC98388FB8FAC03L ^ o + 0x8CB92BA72F3D8DD7L);
            final float c = (m + 0xA36A9465A325DA06L ^ n + 0x57930711F71F5806L ^ o + 0x1972574E5E7B1BAEL);
            final float d = (m + 0x751FDE9874B8C709L ^ n + 0x035C8A9AF2AF0409L ^ o + 0xA62B82F58DB8A985L);

            // get the fractional part of x.
            x -= floor;
            // this is bicubic interpolation, inlined
            final float p = (d - c) - (a - b);
            // 7.7.228014483236334E-20 , or 0x1.5555555555428p-64 , is just inside {@code -2f/3f/Long.MIN_VALUE} .
            // it gets us about as close as we can go to 1.0 .
            return (x * (x * x * p + x * (a - b - p) + c - a) + b) * 7.228014E-20f;
        }

        /**
         * Uses the {@link #seed} of this PerlueNoise
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @return a float between -1 and 1
         */
        public float getNoise(final float x, final float y) {
            return getNoiseWithSeed(x, y, seed);
        }

        /**
         *
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @param seed may be any int; should be the same between calls to the same area of noise
         * @return a float between -1 and 1
         */
        public float getNoiseWithSeed(float x, float y, final int seed) {
            final int
                    xi = floor(x), x0 = xi * X2,
                    yi = floor(y), y0 = yi * Y2;
            final float xf = x - xi, yf = y - yi;

            final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
            final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
            return
                    equalize(lerp(lerp(gradCoord2D(seed, x0, y0, xf, yf), gradCoord2D(seed, x0+ X2, y0, xf - 1, yf), xa),
                                    lerp(gradCoord2D(seed, x0, y0+ Y2, xf, yf-1), gradCoord2D(seed, x0+ X2, y0+ Y2, xf - 1, yf - 1), xa),
                                    ya) * SCALE2, EQ_ADD_2, EQ_MUL_2);//* 0.875;// * 1.4142;
        }

        /**
         * Uses the {@link #seed} of this PerlueNoise
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @param z z-coordinate; usually different by a small fraction between calls
         * @return a float between -1 and 1
         */
        public float getNoise(final float x, final float y, final float z) {
            return getNoiseWithSeed(x, y, z, seed);
        }

        /**
         *
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @param z z-coordinate; usually different by a small fraction between calls
         * @param seed may be any int; should be the same between calls to the same area of noise
         * @return a float between -1 and 1
         */
        public float getNoiseWithSeed(float x, float y, float z, final int seed) {
            final int
                    xi = floor(x), x0 = xi * X3,
                    yi = floor(y), y0 = yi * Y3,
                    zi = floor(z), z0 = zi * Z3;
            final float xf = x - xi, yf = y - yi, zf = z - zi;

            final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
            final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
            final float za = zf * zf * (1 - zf - zf + 2);//* zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
             return
                     equalize(
                             lerp(
                                     lerp(
                                             lerp(
                                                     gradCoord3D(seed, x0, y0, z0, xf, yf, zf),
                                                     gradCoord3D(seed, x0+ X3, y0, z0, xf - 1, yf, zf),
                                                     xa),
                                             lerp(
                                                     gradCoord3D(seed, x0, y0+ Y3, z0, xf, yf-1, zf),
                                                     gradCoord3D(seed, x0+ X3, y0+ Y3, z0, xf - 1, yf - 1, zf),
                                                     xa),
                                             ya),
                                     lerp(
                                             lerp(
                                                     gradCoord3D(seed, x0, y0, z0+ Z3, xf, yf, zf-1),
                                                     gradCoord3D(seed, x0+ X3, y0, z0+ Z3, xf - 1, yf, zf-1),
                                                     xa),
                                             lerp(
                                                     gradCoord3D(seed, x0, y0+ Y3, z0+ Z3, xf, yf-1, zf-1),
                                                     gradCoord3D(seed, x0+ X3, y0+ Y3, z0+ Z3, xf - 1, yf - 1, zf-1),
                                                     xa),
                                             ya),
                                     za) * SCALE3, EQ_ADD_3, EQ_MUL_3); // 1.0625f
        }

        /**
         * Uses the {@link #seed} of this PerlueNoise
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @param z z-coordinate; usually different by a small fraction between calls
         * @param w w-coordinate; usually different by a small fraction between calls
         * @return a float between -1 and 1
         */
        public float getNoise(final float x, final float y, final float z, final float w) {
            return getNoiseWithSeed(x, y, z, w, seed);
        }

        /**
         *
         * @param x x-coordinate; usually different by a small fraction between calls
         * @param y y-coordinate; usually different by a small fraction between calls
         * @param z z-coordinate; usually different by a small fraction between calls
         * @param w w-coordinate; usually different by a small fraction between calls
         * @param seed may be any int; should be the same between calls to the same area of noise
         * @return a float between -1 and 1
         */
        public float getNoiseWithSeed(float x, float y, float z, float w, final int seed) {
            final int
                    xi = floor(x), x0 = xi * X4,
                    yi = floor(y), y0 = yi * Y4,
                    zi = floor(z), z0 = zi * Z4,
                    wi = floor(w), w0 = wi * W4;
            final float xf = x - xi, yf = y - yi, zf = z - zi, wf = w - wi;

            final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
            final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
            final float za = zf * zf * (1 - zf - zf + 2);//* zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
            final float wa = wf * wf * (1 - wf - wf + 2);//* wf * (wf * (wf * 6.0f - 15.0f) + 9.999998f);
            return
                    equalize(
                            lerp(
                                    lerp(
                                            lerp(
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0, z0, w0, xf, yf, zf, wf),
                                                            gradCoord4D(seed, x0+ X4, y0, z0, w0, xf - 1, yf, zf, wf),
                                                            xa),
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0+ Y4, z0, w0, xf, yf-1, zf, wf),
                                                            gradCoord4D(seed, x0+ X4, y0+ Y4, z0, w0, xf - 1, yf - 1, zf, wf),
                                                            xa),
                                                    ya),
                                            lerp(
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0, z0+ Z4, w0, xf, yf, zf-1, wf),
                                                            gradCoord4D(seed, x0+ X4, y0, z0+ Z4, w0, xf - 1, yf, zf-1, wf),
                                                            xa),
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0+ Y4, z0+ Z4, w0, xf, yf-1, zf-1, wf),
                                                            gradCoord4D(seed, x0+ X4, y0+ Y4, z0+ Z4, w0, xf - 1, yf - 1, zf-1, wf),
                                                            xa),
                                                    ya),
                                            za),
                                    lerp(
                                            lerp(
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0, z0, w0+ W4, xf, yf, zf, wf - 1),
                                                            gradCoord4D(seed, x0+ X4, y0, z0, w0+ W4, xf - 1, yf, zf, wf - 1),
                                                            xa),
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0+ Y4, z0, w0+ W4, xf, yf-1, zf, wf - 1),
                                                            gradCoord4D(seed, x0+ X4, y0+ Y4, z0, w0+ W4, xf - 1, yf - 1, zf, wf - 1),
                                                            xa),
                                                    ya),
                                            lerp(
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0, z0+ Z4, w0+ W4, xf, yf, zf-1, wf - 1),
                                                            gradCoord4D(seed, x0+ X4, y0, z0+ Z4, w0+ W4, xf - 1, yf, zf-1, wf - 1),
                                                            xa),
                                                    lerp(
                                                            gradCoord4D(seed, x0, y0+ Y4, z0+ Z4, w0+ W4, xf, yf-1, zf-1, wf - 1),
                                                            gradCoord4D(seed, x0+ X4, y0+ Y4, z0+ Z4, w0+ W4, xf - 1, yf - 1, zf-1, wf - 1),
                                                            xa),
                                                    ya),
                                            za),
                                    wa) * SCALE4, EQ_ADD_4, EQ_MUL_4);//0.555f);
        }

        /**
         * Produces a String that describes everything needed to recreate this RawNoise in full. This String can be read back
         * in by {@link #stringDeserialize(String)} to reassign the described state to another RawNoise.
         * @return a String that describes this PerlueNoise for serialization
         */
        public String stringSerialize() {
            return "`" + seed + "`";
        }

        /**
         * Given a serialized String produced by {@link #stringSerialize()}, reassigns this PerlueNoise to have the
         * described state from the given String. The serialized String must have been produced by a PerlueNoise.
         *
         * @param data a serialized String, typically produced by {@link #stringSerialize()}
         * @return this PerlueNoise, after being modified (if possible)
         */
        public PerlueNoise stringDeserialize(String data) {
            seed = MathSupport.intFromDec(data, 1, data.indexOf('`', 1));
            return this;
        }

        /**
         * Creates a PerlueNoise from a String produced by {@link #stringSerialize()}.
         * @param data a serialized String, typically produced by {@link #stringSerialize()}
         * @return a new PerlueNoise, using the restored state from data
         */
        public static PerlueNoise recreateFromString(String data) {
            return new PerlueNoise(MathSupport.intFromDec(data, 1, data.indexOf('`', 1)));
        }
        /**
         * Gets a simple human-readable String that describes this noise generator. This should use names instead of coded
         * numbers, and should be enough to differentiate any two generators.
         * @return a String that describes this noise generator for human consumption
         */
        public String toHumanReadableString(){
            return getTag() + " with seed " + getSeed();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PerlueNoise that = (PerlueNoise) o;

            return (seed == that.seed);
        }

        public int hashCode() {
            return seed;
        }

        public String toString() {
            return "PerlueNoise{seed=" + seed + '}';
        }

        /**
         * A 32-bit point hash that needs 2 dimensions pre-multiplied by constants {@link #X2} and {@link #Y2}, as
         * well as an int seed.
         * @param x x position, as an int pre-multiplied by {@link #X2}
         * @param y y position, as an int pre-multiplied by {@link #Y2}
         * @param s any int, a seed to be able to produce many hashes for a given point
         * @return 8-bit hash of the x,y point with the given state s, shifted for {@link GradientVectors#GRADIENTS_2D}
         */
        public static int hashAll(int x, int y, int s) {
            final int h = (s ^ x ^ y) * 0x125493;
            return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
        }
        /**
         * A 32-bit point hash that needs 3 dimensions pre-multiplied by constants {@link #X3} through {@link #Z3}, as
         * well as an int seed.
         * @param x x position, as an int pre-multiplied by {@link #X3}
         * @param y y position, as an int pre-multiplied by {@link #Y3}
         * @param z z position, as an int pre-multiplied by {@link #Z3}
         * @param s any int, a seed to be able to produce many hashes for a given point
         * @return 8-bit hash of the x,y,z point with the given state s, shifted for {@link GradientVectors#GRADIENTS_3D}
         */
        public static int hashAll(int x, int y, int z, int s) {
            final int h = (s ^ x ^ y ^ z) * 0x125493;
            return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
        }

        /**
         * A 32-bit point hash that needs 4 dimensions pre-multiplied by constants {@link #X4} through {@link #W4}, as
         * well as an int seed.
         * @param x x position, as an int pre-multiplied by {@link #X4}
         * @param y y position, as an int pre-multiplied by {@link #Y4}
         * @param z z position, as an int pre-multiplied by {@link #Z4}
         * @param w w position, as an int pre-multiplied by {@link #W4}
         * @param s any int, a seed to be able to produce many hashes for a given point
         * @return 8-bit hash of the x,y,z,w point with the given state s, shifted for {@link GradientVectors#GRADIENTS_4D}
         */
        public static int hashAll(int x, int y, int z, int w, int s) {
            final int h = (s ^ x ^ y ^ z ^ w) * 0x125493;
            return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
        }

        // Predefined constants as 21-bit prime numbers, these are used as multipliers for x, y, z, and w, to make
        // changes to one component different from changes to another when used for hashing. These are 21-bit numbers
        // for GWT reasons; GWT loses precision past 53 bits, and multiplication of an unknown-size int with a constant
        // multiplier fits in 53 bits as long as the multiplier is 21 bits or smaller.
        public static final int
            X2 = 0x1827F5, Y2 = 0x123C3B,
            X3 = 0x1A36BF, Y3 = 0x157931, Z3 = 0x119749,
            X4 = 0x1B69E5, Y4 = 0x177C1F, Z4 = 0x141E75, W4 = 0x113C33;
    }
}
