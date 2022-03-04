/*  Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.RandomnessSource;
import squidpony.squidmath.StatefulRandomness;

import java.io.Serializable;

/**
 */
public final class Rumble32RNG implements RandomnessSource, Serializable {

    private static final long serialVersionUID = 1L;

    private int stateA, stateB, stateC, stateD;

    /**
     * Creates a new generator seeded using four calls to Math.random().
     */
    public Rumble32RNG() {
        this((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000),
                (int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    /**
     * Constructs this Rumble32RNG by dispersing the bits of seed using {@link #setSeed(int)} across the two parts of state
     * this has.
     * @param seed a long that won't be used exactly, but will affect both components of state
     */
    public Rumble32RNG(final int seed) {
        setSeed(seed);
    }
    /**
     * Constructs this Rumble32RNG by calling {@link #setState(int, int, int, int)} on the arguments as given; see that method for
     * the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public Rumble32RNG(final int stateA, final int stateB, final int stateC, final int stateD) {
        setState(stateA, stateB, stateC, stateD);
    }
    
    @Override
    public int next(int bits) {
        final int fa = stateA;
        final int fb = stateB;
        final int fc = stateC;
        final int fd = stateD;
        stateA = fb ^ fc;
        stateA = (stateA << 19 | stateA >>> 13);
        stateB = fc ^ fd;
        stateB = (stateB << 24 | stateB >>> 8);
        stateC = fa ^ fb + fc;
        stateD = fd + 0x30ECB8CB | 0;
        return fc >>> (32 - bits);
//        stateD = stateD + 0x30ECB8CB | 0;
//        final int xor = stateC ^ stateD;
//        stateB = (xor << 24 | xor >>> 8);
//        final int sum = stateB + stateC;
//        stateA = (sum << 19 | sum >>> 13);
//        stateC = stateA ^ sum;
//        return stateC >>> (32 - bits);
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public int nextInt() {
        final int fa = stateA;
        final int fb = stateB;
        final int fc = stateC;
        final int fd = stateD;
        final int x = fb ^ fc;
        stateA = (x << 19 | x >>> 13);
        final int y = fc ^ fd;
        stateB = (y << 24 | y >>> 8);
        stateC = fa ^ fb + fc;
        stateD = fd + 0x30ECB8CB | 0;
        return fc;
    }

    @Override
    public long nextLong() {
        final int fa = stateA;
        final int fb = stateB;
        final int fc = stateC;
        final int fd = stateD;
        int ga = fb ^ fc; ga = (ga << 19 | ga >>> 13);
        int gb = fc ^ fd; gb = (gb << 24 | gb >>>  8);
        final int gc = fa ^ fb + fc;
        final int gd = fd + 0x30ECB8CB | 0;
        final long high = fc;
        stateA = gb ^ gc; stateA = (stateA << 19 | stateA >>> 13);
        stateB = gc ^ gd; stateB = (stateB << 24 | stateB >>>  8);
        stateC = ga ^ gb + gc;
        stateD = gd + 0x30ECB8CB | 0;
        return high << 32 ^ gc;
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public Rumble32RNG copy() {
        return new Rumble32RNG(stateA, stateB, stateC, stateD);
    }
    
    /**
     * Sets the state of this generator using one int, running it through Light32RNG's algorithm twice to get two ints.
     * @param seed the int to use to assign this generator's state
     */
    public void setSeed(final int seed) {
        int z = seed + 0xC74EAD55, a = seed ^ z;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3;
        a ^= a >>> 15;
        stateA = (z ^ z >>> 20) + (a ^= a << 13) | 0;
        z = seed + 0xC74EAD55 * 2;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3;
        a ^= a >>> 15;
        stateB = (z ^ z >>> 20) + (a ^= a << 13) | 0;
        z = seed + 0xC74EAD55 * 3;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3;
        a ^= a >>> 15;
        stateC = (z ^ z >>> 20) + (a ^= a << 13) | 0;
        z = seed + 0xC74EAD55 * 4;
        a ^= a >>> 14;
        z = (z ^ z >>> 10) * 0xA5CB3;
        a ^= a >>> 15;
        stateD = (z ^ z >>> 20) + (a ^ a << 13) | 0;
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
     * Sets the current internal state of this Rumble32RNG with two ints, where stateA can be any int except 0, and stateB
     * can be any int.
     * @param stateA any int except 0 (0 will be treated as 1 instead)
     * @param stateB any int
     */
    public void setState(int stateA, int stateB, int stateC, int stateD)
    {
        this.stateA = stateA == 0 && stateB == 0 ? 1 : stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }
    @Override
    public String toString() {
        return "Rumble32RNG with stateA 0x" + StringKit.hex(stateA) +
                "L, stateB 0x" + StringKit.hex(stateB) +
                "L, stateC 0x" + StringKit.hex(stateC) +
                "L and stateD 0x" + StringKit.hex(stateD) + 'L';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rumble32RNG rumble32RNG = (Rumble32RNG) o;

        return stateA == rumble32RNG.stateA && stateB == rumble32RNG.stateB
                && stateC == rumble32RNG.stateC && stateD == rumble32RNG.stateD;
    }

    @Override
    public int hashCode() {
        return 31 * 31 * 31 * stateA +  31 * 31 * stateB + 31 * stateC + stateD | 0;
    }
}
