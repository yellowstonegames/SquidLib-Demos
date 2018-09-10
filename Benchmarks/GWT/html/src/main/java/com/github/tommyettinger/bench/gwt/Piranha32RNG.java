/*  Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.StatefulRandomness;

import java.io.Serializable;

/**
 * A work-in-progress modification of Blackman and Vigna's xoroshiro64** generator; very close to {@link Lobster32RNG},
 * but uses a large addition (by 0x41C64E6D, used in PractRand) instead of a multiplication (by 31), and changes some
 * of the hard-to-reverse operation's shift/rotation amounts. It's doing pretty well in testing, but benchmarks in the
 * browser give bafflingly different results -- extremely fast in Firefox, but slower than Lobster in Chrome.
 * <br>
 * The name comes from the sea creature theme I'm using for this family of generators and the fast, somewhat reckless
 * movement of piranha fish.
 * <br>
 * <a href="http://xoshiro.di.unimi.it/xoroshiro64starstar.c">Original version here for xoroshiro64**</a>.
 * <br>
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 * Ported and modified in 2018 by Tommy Ettinger
 * @author Sebastiano Vigna
 * @author David Blackman
 * @author Tommy Ettinger (if there's a flaw, use SquidLib's or Sarong's issues and don't bother Vigna or Blackman, it's probably a mistake in SquidLib's implementation)
 */
public final class Piranha32RNG implements StatefulRandomness, Serializable {

    private static final long serialVersionUID = 1L;

    private int stateA, stateB;

    /**
     * Creates a new generator seeded using two calls to Math.random().
     */
    public Piranha32RNG() {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    /**
     * Constructs this Lathe32RNG by dispersing the bits of seed using {@link #setSeed(int)} across the two parts of state
     * this has.
     * @param seed an int that won't be used exactly, but will affect both components of state
     */
    public Piranha32RNG(final int seed) {
        setSeed(seed);
    }
    /**
     * Constructs this Lathe32RNG by splitting the given seed across the two parts of state this has with
     * {@link #setState(long)}.
     * @param seed a long that will be split across both components of state
     */
    public Piranha32RNG(final long seed) {
        setState(seed);
    }
    /**
     * Constructs this Lathe32RNG by calling {@link #setState(int, int)} on stateA and stateB as given; see that method
     * for the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public Piranha32RNG(final int stateA, final int stateB) {
        setState(stateA, stateB);
    }
    
    @Override
    public final int next(int bits) {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 + 0x41C64E6D;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 6) - (result << 4 | result >>> 28) >>> (32 - bits);
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public final int nextInt() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 + 0x41C64E6D;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 6) - (result << 4 | result >>> 28) | 0;
    }

    @Override
    public final long nextLong() {
        int s0 = stateA;
        int s1 = stateB ^ s0;
        final int high = s0 + 0x41C64E6D;
        s0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        s1 = (s1 << 13 | s1 >>> 19) ^ s0;
        final int low = s0 + 0x41C64E6D;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        final long result = (high << 6) - (high << 4 | high >>> 28);
        return result << 32 ^ ((low << 6) - (low << 4 | low >>> 28));
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public Piranha32RNG copy() {
        return new Piranha32RNG(stateA, stateB);
    }

    /**
     * Sets the state of this generator using one int, running it through Zog32RNG's algorithm two times to get 
     * two ints. If the states would both be 0, state A is assigned 1 instead.
     * @param seed the int to use to produce this generator's state
     */
    public void setSeed(final int seed) {
        int z = seed + 0xC74EAD55 | 0, a = seed ^ z;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3 | 0;
        a ^= a >>> 15;
        stateA = (z ^ z >>> 20) + (a ^= a << 13) | 0;
        z = seed + 0x8E9D5AAA | 0;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3 | 0;
        a ^= a >>> 15;
        stateB = (z ^ z >>> 20) + (a ^ a << 13) | 0;
        if((stateA | stateB) == 0)
            stateA = 1;
    }

    public int getStateA()
    {
        return stateA;
    }
    /**
     * Sets the first part of the state to the given int. As a special case, if the parameter is 0 and stateB is
     * already 0, this will set stateA to 1 instead, since both states cannot be 0 at the same time. Usually, you
     * should use {@link #setState(int, int)} to set both states at once, but the result will be the same if you call
     * setStateA() and then setStateB() or if you call setStateB() and then setStateA().
     * @param stateA any int
     */

    public void setStateA(int stateA)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
    }
    public int getStateB()
    {
        return stateB;
    }

    /**
     * Sets the second part of the state to the given int. As a special case, if the parameter is 0 and stateA is
     * already 0, this will set stateA to 1 and stateB to 0, since both cannot be 0 at the same time. Usually, you
     * should use {@link #setState(int, int)} to set both states at once, but the result will be the same if you call
     * setStateA() and then setStateB() or if you call setStateB() and then setStateA().
     * @param stateB any int
     */
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
        if((stateB | stateA) == 0) stateA = 1;
    }

    /**
     * Sets the current internal state of this Lathe32RNG with three ints, where stateA and stateB can each be any int
     * unless they are both 0 (which will be treated as if stateA is 1 and stateB is 0).
     * @param stateA any int (if stateA and stateB are both 0, this will be treated as 1)
     * @param stateB any int
     */
    public void setState(int stateA, int stateB)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
        this.stateB = stateB;
    }

    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return (stateA & 0xFFFFFFFFL) | ((long)stateB) << 32;
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     *
     * @param state a 64-bit long. You should avoid passing 0; this implementation will treat it as 1.
     */
    @Override
    public void setState(long state) {
        stateA = state == 0 ? 1 : (int)(state & 0xFFFFFFFFL);
        stateB = (int)(state >>> 32);
    }

    @Override
    public String toString() {
        return "Piranha32RNG with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Piranha32RNG piranha32RNG = (Piranha32RNG) o;

        if (stateA != piranha32RNG.stateA) return false;
        return stateB == piranha32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }
}
