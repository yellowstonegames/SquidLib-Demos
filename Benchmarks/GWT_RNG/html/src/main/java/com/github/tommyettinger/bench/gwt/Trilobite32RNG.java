/*  Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.RandomnessSource;

import java.io.Serializable;

/**
 * A work-in-progress modification of Blackman and Vigna's xoroshiro64** generator; like {@link Lobster32RNG} but has a
 * third state that increments in a Weyl sequence. Not as fast as hoped.
 * <br>
 * The name comes from the sea creature theme I'm using for this family of generators and the hard shell on a trilobite.
 * <br>
 * <a href="http://xoshiro.di.unimi.it/xoroshiro64starstar.c">Original version here for xoroshiro64**</a>.
 * <br>
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 * Ported and modified in 2018 by Tommy Ettinger
 * @author Sebastiano Vigna
 * @author David Blackman
 * @author Tommy Ettinger (if there's a flaw, use SquidLib's or Sarong's issues and don't bother Vigna or Blackman, it's probably a mistake in SquidLib's implementation)
 */
public final class Trilobite32RNG implements RandomnessSource, Serializable {

    private static final long serialVersionUID = 1L;

    private int stateA, stateB, stateC;

    /**
     * Creates a new generator seeded using two calls to Math.random().
     */
    public Trilobite32RNG() {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    /**
     * Constructs this Lathe32RNG by dispersing the bits of seed using {@link #setSeed(int)} across the two parts of state
     * this has.
     * @param seed an int that won't be used exactly, but will affect both components of state
     */
    public Trilobite32RNG(final int seed) {
        setSeed(seed);
    }
    /**
     * Constructs this Lathe32RNG by splitting the given seed across the two parts of state this has with
     * {@link #setSeed(long)}.
     * @param seed a long that will be split across both components of state
     */
    public Trilobite32RNG(final long seed) {
        setSeed(seed);
    }
    /**
     * Constructs this Lathe32RNG by calling {@link #setState(int, int, int)} on stateA, stateB, and stateC as given.
     * See that method for the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if the first two seeds are 0
     * @param stateB the number to use as the second part of the state
     * @param stateC the number to use as the third part of the state
     */
    public Trilobite32RNG(final int stateA, final int stateB, final int stateC) {
        setState(stateA, stateB, stateC);
    }
    
    @Override
    public final int next(int bits) {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 + (stateC = stateC + 0x9E3779BD | 0);
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 10) - (result << 6 | result >>> 26) >>> (32 - bits);
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public final int nextInt() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 + (stateC = stateC + 0x9E3779BD | 0);
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 10) - (result << 6 | result >>> 26) | 0;
    }

    @Override
    public final long nextLong() {
        int s0 = stateA;
        int s1 = stateB ^ s0;
        final int high = s0 + stateC + 0x9E3779BD;
        s0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        s1 = (s1 << 13 | s1 >>> 19) ^ s0;
        final int low = s0 + (stateC = stateC + 0x3C6EF37A | 0);
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        final long result = (high << 10) - (high << 6 | high >>> 26);
        return result << 32 ^ ((low << 10) - (low << 6 | low >>> 26));
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public Trilobite32RNG copy() {
        return new Trilobite32RNG(stateA, stateB, stateC);
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
        z = seed - 0xC74EAD55 | 0;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3 | 0;
        a ^= a >>> 15;
        stateC = (z ^ z >>> 20) + (a ^ a << 13) | 0;
    }

    public int getStateA()
    {
        return stateA;
    }
    /**
     * Sets the first part of the state to the given int. As a special case, if the parameter is 0 and stateB is
     * already 0, this will set stateA to 1 instead, since both states cannot be 0 at the same time. Usually, you
     * should use {@link #setState(int, int, int)} to set all states at once, but the result will be the same if you
     * call setStateA(), setStateB(), and setStateC() in any order.
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
     * should use {@link #setState(int, int, int)} to set all states at once, but the result will be the same if you
     * call setStateA(), setStateB(), and setStateC() in any order.
     * @param stateB any int
     */
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
        if((stateB | stateA) == 0) stateA = 1;
    }

    public int getStateC() {
        return stateC;
    }

    public void setStateC(int stateC) {
        this.stateC = stateC;
    }

    /**
     * Sets the current internal state of this Lathe32RNG with three ints, where stateA and stateB can each be any int
     * unless they are both 0 (which will be treated as if stateA is 1 and stateB is 0), and stateC can be any int.
     * @param stateA any int (if stateA and stateB are both 0, this will be treated as 1)
     * @param stateB any int
     * @param stateC any int
     */
    public void setState(final int stateA, final int stateB, final int stateC)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
        this.stateB = stateB;
        this.stateC = stateC;
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long, spreading the 64-bit {@code seed} over
     * the 96 bits of state in this generator.
     *
     * @param seed any 64-bit long; 0 is not considered identical to 1.
     */
    public void setSeed(long seed) {
        stateA = seed == 0 ? 1 : (int)(seed & 0xFFFFFFFFL);
        stateB = (int)(seed >>> 32);
        stateC = (int)(seed ^ seed >>> 32);
    }

    @Override
    public String toString() {
        return "Trilobite32RNG with stateA 0x" + StringKit.hex(stateA)
                + ", stateB 0x" + StringKit.hex(stateB)
                + ", and stateC 0x" + StringKit.hex(stateC);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trilobite32RNG trilobite32RNG = (Trilobite32RNG) o;

        if (stateA != trilobite32RNG.stateA) return false;
        return stateB == trilobite32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * stateA + stateB) + stateC | 0;
    }
}
