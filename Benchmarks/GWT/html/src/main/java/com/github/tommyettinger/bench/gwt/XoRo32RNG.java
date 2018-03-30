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
 * A port of Blackman and Vigna's xoroshiro128+ generator to use two 32-bit ints of state instead of two 64-bit longs;
 * should be very fast and produce moderate-quality output as-is. In statistical testing, xoroshiro always fails some
 * binary matrix rank tests, but smaller-state versions fail other tests as well. If issues with the quality become
 * apparent, you can safely run the output of {@link #nextInt()} through code like
 * {@code int output = random.nextInt(); output = ((output ^ output >>> 17) * 0xE5CB3 | 0)}, which seems to allow the
 * results of the generator to pass binary matrix rank tests as well as the other tests it normally fails, at least for
 * a longer period of testing than it can normally pass on. Similar code could be used for {@link #nextLong()}, but
 * {@link #next(int)} may need a little extra work. This does slow down the generator somewhat (especially nextLong())
 * and the quality issues may not be noticeable, so by default this quality improvement is a manual step. It's called
 * XoRo32 because it involves Xor and XorShift as well as Rotate operations on the pair of 32-bit pseudo-random states.
 * <br>
 * <a href="http://xoroshiro.di.unimi.it/xoroshiro128plus.c">Original version here for xorshiro128+</a>; this version
 * uses <a href="https://groups.google.com/d/msg/prng/Ll-KDIbpO8k/bfHK4FlUCwAJ">different constants</a> by the same
 * author, Sebastiano Vigna.
 * <br>
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 *
 * @author Sebastiano Vigna
 * @author David Blackman
 * @author Tommy Ettinger (if there's a flaw, use SquidLib's or Sarong's issues and don't bother Vigna or Blackman, it's probably a mistake in SquidLib's implementation)
 */
public final class XoRo32RNG implements StatefulRandomness, Serializable { 
    
    private static final long serialVersionUID = 1L;

    private int stateA, stateB;

    /**
     * Creates a new generator seeded using four calls to Math.random().
     */
    public XoRo32RNG() {
        this((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    /**
     * Constructs this XoRo32RNG by dispersing the bits of seed using {@link #setSeed(int)} across the two parts of state
     * this has.
     * @param seed a long that won't be used exactly, but will affect both components of state
     */
    public XoRo32RNG(final int seed) {
        setSeed(seed);
    }
    /**
     * Constructs this XoRo32RNG by calling {@link #setState(int, int)} on the arguments as given; see that method for 
     * the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public XoRo32RNG(final int stateA, final int stateB) {
        setState(stateA, stateB);
    }
    
    @Override
    public final int next(int bits) {
        final int s0 = stateA;
        int s1 = stateB;
        final int result = (s0 + s1) >>> (32 - bits);
        s1 ^= s0;
        stateA = (s0 << 13 | s0 >>> 19) ^ s1 ^ (s1 << 9); // a, b
        stateB = (s1 << 26 | s1 >>> 6); // c
        return result;
        //return (result ^ result >>> 17) * 0xE5CB3 | 0;
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public final int nextInt() {
        final int s0 = stateA;
        int s1 = stateB;
        final int result = s0 + s1 | 0;
        s1 ^= s0;
        stateA = (s0 << 13 | s0 >>> 19) ^ s1 ^ (s1 << 9); // a, b
        stateB = (s1 << 26 | s1 >>> 6); // c
        return result;
        //return (result ^ result >>> 17) * 0xE5CB3 | 0;
    }

    @Override
    public final long nextLong() {
        int s0 = stateA;
        int s1 = stateB;
        final long high = s0 + s1 | 0;
        s1 ^= s0;
        s0 = (s0 << 13 | s0 >>> 19) ^ s1 ^ (s1 << 9); // a, b
        s1 = (s1 << 26 | s1 >>> 6); // c
        final long result = high << 32 ^ (s0 + s1);
        //final long result = (high ^ high >>> 17) << 32 ^ (s0 + s1);
        s1 ^= s0;
        stateA = (s0 << 13 | s0 >>> 19) ^ s1 ^ (s1 << 9); // a, b
        stateB = (s1 << 26 | s1 >>> 6); // c
        return result;
        //return (result ^ result >>> 17) * 0xE5CB3L;
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public XoRo32RNG copy() {
        return new XoRo32RNG(stateA, stateB);
    }
    
    /**
     * Sets the state of this generator using one int, running it through Light32RNG's algorithm twice to get two ints.
     * @param seed the int to use to assign this generator's state
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
    }

    public int getStateA()
    {
        return stateA;
    }
    public void setStateA(int stateA)
    {
        this.stateA = stateA == 0 && stateB == 0 ? 1 : stateA;
    }
    public int getStateB()
    {
        return stateB;
    }
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
    }

    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return (long) stateB << 32 | (stateA & 0xFFFFFFFFL);
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     * If the bottom 32 bits of the given state are all 0, then this will set stateA to 1, otherwise it sets stateA to
     * those bottom 32 bits. This always sets stateB to the upper 32 bits of the given state.
     * @param state a 64-bit long; the bottom 32 bits should not be all 0, but this is tolerated
     */
    @Override
    public void setState(long state) {
        if (state == 0) {
            stateA = 1;
            stateB = 0;
        } else {
            stateA = (int) (state & 0xFFFFFFFFL);
            stateB = (int) (state >>> 32);
        }
    }

    /**
     * Sets the current internal state of this XoRo32RNG with two ints, where stateA can be any int except 0, and stateB
     * can be any int.
     * @param stateA any int except 0 (0 will be treated as 1 instead)
     * @param stateB any int
     */
    public void setState(int stateA, int stateB)
    {
        this.stateA = stateA == 0 && stateB == 0 ? 1 : stateA;
        this.stateB = stateB;
    }
    @Override
    public String toString() {
        return "XoRo32RNG with stateA 0x" + StringKit.hex(stateA) + "L and stateB 0x" + StringKit.hex(stateB) + 'L';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XoRo32RNG xoRo32RNG = (XoRo32RNG) o;

        if (stateA != xoRo32RNG.stateA) return false;
        return stateB == xoRo32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }
}
