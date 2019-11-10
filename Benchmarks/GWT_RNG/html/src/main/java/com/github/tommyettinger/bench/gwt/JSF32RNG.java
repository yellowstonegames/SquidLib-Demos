package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.LightRNG;
import squidpony.squidmath.RandomnessSource;

import java.io.Serializable;

/**
 * A high-quality 32-bit generator using one of
 * <a href="http://www.pcg-random.org/posts/bob-jenkins-small-prng-passes-practrand.html">Bob Jenkins' designs</a>; JSF
 * is short for Jenkins Small Fast and was coined by Chris Doty-Humphrey when he reviewed it favorably in PractRand.
 * This is a "chaotic" generator, meaning its state transition is very challenging to predict; it is essentially
 * impossible to invert the generator and get the state from even quite a lot of outputs, unlike generators like
 * {@link LightRNG}. It uses no multiplication internally and primarily depends on bitwise rotation combined with fast
 * operations like addition and XOR to achieve quality results. Its period is unknown beyond that for all seeds
 * possible, the period must be at minimum 2 to the 20, and is at most a little less than 2 to the 128.
 * <br>
 * This is a RandomnessSource but not a StatefulRandomness because it needs to take care and avoid seeds that would put
 * it in a short-period subcycle. It always takes an int when being seeded.
 * <br>
 * Special thanks to everyone who's reviewed this type of RNG, especially M.E. O'Neill, whose blog showed me how useful
 * JSF is. Of course, thanks go to Bob Jenkins for writing yet another remarkable piece of code.
 * <br>
 * Created by Tommy Ettinger on 11/3/2019.
 * @author Bob Jenkins
 * @author Tommy Ettinger
 */
public final class JSF32RNG implements RandomnessSource, Serializable {
    private static final long serialVersionUID = 2L;
    private int stateA, stateB, stateC, stateD;

    /**
     * Calls {@link #seed(int)} with a random int value (obtained using {@link Math#random()}).
     */
    public JSF32RNG()
    {
        seed((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }

    /**
     * The recommended constructor, this guarantees the generator will have a period of at least 2 to the 20, and makes
     * it likely that the period is actually much larger. All ints are permissible values for {@code state}. Uses
     * {@link #seed(int)} to handle the actual spread of bits into the states.
     * @param state any int; will be used to get the actual state used in the generator (which is four ints internally)
     */
    public JSF32RNG(final int state)
    {
        seed(state);
    }

    /**
     * Not recommended for general use, only for remaking existing generators from their states. This can put the
     * generator in a low-period subcycle (which would be bad), but not if the states were taken from a generator
     * in the longest-period subcycle (which is the case for generators that have used {@link #seed(int)})
     * @param stateA state a; can technically be any int but should probably not be the same as all other states
     * @param stateB state b; can technically be any int
     * @param stateC state c; can technically be any int
     * @param stateD state d; can technically be any int
     */
    public JSF32RNG(final int stateA, final int stateB, final int stateC, final int stateD)
    {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }
    /**
     * Seeds the state using all bits of the given int {@code s}. This is guaranteed to put the generator on its
     * longest subcycle, and 2 to the 32 states are possible.
     * @param s all bits are used to affect 3 of 4 states verbatim (0 is tolerated, and one state is unaffected by seed)
     */
    public final void seed(final int s) {
        stateA = 0xf1ea5eed;
        stateB = s;
        stateC = s;
        stateD = s;
        for (int i = 0; i < 20; i++) {
            final int e = stateA - (stateB << 27 | stateB >>> 5);
            stateA = stateB ^ (stateC << 17 | stateC >>> 15);
            stateB = stateC + stateD | 0;
            stateC = stateD + e | 0;
            stateD = e + stateA | 0;
        }
    }

    public final int nextInt()
    {
        final int e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        return (stateD = e + stateA | 0);
    }
    @Override
    public final int next(final int bits)
    {
        final int e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        return (stateD = e + stateA | 0) >>> 32 - bits;
    }

    @Override
    public final long nextLong() {
        int e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        final long high = (stateD = e + stateA | 0);
        e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        return high << 32 | ((stateD = e + stateA | 0) & 0xFFFFFFFFL);
    }

    /**
     * Gets a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive).
     * @return a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public final double nextDouble() {
        int e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        final long high = (stateD = e + stateA | 0);
        e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        return ((high << 32 | ((stateD = e + stateA | 0) & 0xFFFFFFFFL)) & 0x1fffffffffffffL) * 0x1p-53;
    }

    /**
     * Gets a pseudo-random float between 0.0f (inclusive) and 1.0f (exclusive).
     * @return a pseudo-random float between 0.0f (inclusive) and 1.0f (exclusive)
     */
    public final float nextFloat() {
        final int e = stateA - (stateB << 27 | stateB >>> 5);
        stateA = stateB ^ (stateC << 17 | stateC >>> 15);
        stateB = stateC + stateD | 0;
        stateC = stateD + e | 0;
        return ((stateD = e + stateA | 0) & 0xffffff) * 0x1p-24f;
    }

    /**
     * Produces a copy of this MegaMover32RNG that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this MegaMover32RNG
     */
    @Override
    public JSF32RNG copy() {
        return new JSF32RNG(stateA, stateB, stateC, stateD);
    }

    public int getStateA()
    {
        return stateA;
    }
    /**
     * Sets the first part of the state to the given int; this can put the generator on a bad subcycle.
     * @param stateA any int
     */
    public void setStateA(int stateA)
    {
        this.stateA = stateA;
    }
    public int getStateB()
    {
        return stateB;
    }

    /**
     * Sets the second part of the state to the given int; this can put the generator on a bad subcycle.
     * @param stateB any int
     */
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
    }
    public int getStateC()
    {
        return stateC;
    }

    /**
     * Sets the third part of the state to the given int; this can put the generator on a bad subcycle.
     * @param stateC any int
     */
    public void setStateC(int stateC)
    {
        this.stateC = stateC;
    }

    public int getStateD()
    {
        return stateD;
    }

    /**
     * Sets the fourth part of the state to the given int; this can put the generator on a bad subcycle.
     * @param stateD any int
     */
    public void setStateD(int stateD)
    {
        this.stateD = stateD;
    }

    /**
     * Sets the current internal state of this MegaMover32RNG with four ints, where each can be any int; this can put
     * the generator on a bad subcycle.
     * @param stateA any int
     * @param stateB any int
     * @param stateC any int
     * @param stateD any int
     */
    public void setState(final int stateA, final int stateB, final int stateC, final int stateD)
    {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    @Override
    public String toString() {
        return "JSF32RNG with stateA 0x" + StringKit.hex(stateA) +
                ", stateB 0x" + StringKit.hex(stateB) + ", stateC 0x" + StringKit.hex(stateC)
                + ", stateD 0x" + StringKit.hex(stateD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSF32RNG jsf32RNG = (JSF32RNG) o;

        return stateA == jsf32RNG.stateA && stateB == jsf32RNG.stateB &&
                stateC == jsf32RNG.stateC && stateD == jsf32RNG.stateD;
    }

    @Override
    public int hashCode() {
        return 31 * 31 * 31 * stateA + 31 * 31 * stateB + 31 * stateC + stateD | 0;
    }
}
