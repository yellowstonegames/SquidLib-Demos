package com.github.tommyettinger.bench.gwt;


import squidpony.StringKit;
import squidpony.squidmath.RandomnessSource;

import java.io.Serializable;

/**
 * An unusual generator built around a type of subcycle generator that Mark Overton discovered and mostly discarded,
 * MoverCounter32RNG uses two rca generators and mixes them together with some bitwise ops and one multiplication. It has
 * a period of just under 2 to the 64, 0xFFFE870A6BECE609, which is roughly 2 to the 63.999968, and allows 2 to the 32
 * initial seeds.
 * <br>
 * <br>
 * Its period is 0xFFFE870A6BECE609 for the largest cycle, which it always initializes into if {@link #setState(int)} is
 * used. setState() only allows 2 to the 32 starting states, but less than 2 to the 64 states are in the largest cycle,
 * so using a long or two ints to set the state seems ill-advised. The generator has two similar parts, each updated
 * without needing to read from the other part. Each is a 32-bit rca generator, which rotates a state by a constant,
 * adds anf09503069eother constant, and stores that as the next state. The particular constants used here were found by randomly
 * picking 32-bit probable primes numbers as addends, checking the period for every non-zero rotation, and reporting
 * the addend and rotation amount when a period was found that was greater than 0xFFF00000. Better multipliers are
 * almost guaranteed to exist, but finding them would be a challenge. The ones used here are (rotate left by 25, add
 * 0xC4DE9951) and (rotate left by 1, add 0xAA78EDD7).
 * <br>
 * This is a RandomnessSource but not a StatefulRandomness because it needs to take care and avoid seeds that would put
 * it in a short-period subcycle. It uses two generators with different cycle lengths, and skips at most 65536 times
 * into each generator's cycle independently when seeding. It uses constants to store 128 known midpoints for each
 * generator, which ensures it calculates an advance for each generator at most 511 times. 
 * <br>
 * The name comes from M. Overton, who discovered this category of subcycle generators, and also how this generator can
 * really move when it comes to speed.
 * <br>
 * Created by Tommy Ettinger on 8/6/2018.
 * @author Mark Overton
 * @author Tommy Ettinger
 */
public final class MoverCounter32RNG implements RandomnessSource, Serializable {
    private static final long serialVersionUID = 1L;
    private int stateA, stateB;

    public MoverCounter32RNG() {
        this((int) ((Math.random() * 2.0 - 1.0) * 0x80000000),
                (int) ((Math.random() * 2.0 - 1.0) * 0x80000000));
    }

    public MoverCounter32RNG(final int state) {
        setState(state);
    }

    public MoverCounter32RNG(final int stateA, final int stateB) {
        this.stateA = stateA == 0 ? 1 : stateA;
        this.stateB = stateB == 0 ? 1 : stateB;
    }

    public final void setState(final int s) {
        stateB = s;
        stateA = ~s;
        stateA ^= stateA >>> 13;
        stateA = (stateA << 19) - stateA;
        stateA ^= stateA >>> 12;
        stateA = (stateA << 17) - stateA;
        stateA ^= stateA >>> 14;
        stateA = (stateA << 13) - stateA;
        stateA ^= stateA >>> 15;

    }

    public final int nextInt() {
//        final int result = (stateB = (stateB << 25 | stateB >>> 7) + 0xC4DE9951) * (stateA >>> 11 | 1);
//        return result ^ (result >> 16) + (stateA = (stateA << 1 | stateA >>> 31) + 0xAA78EDD7);

        return ((stateB += 0x9E3779BD) ^ (stateA = (stateA << 21 | stateA >>> 11) * (stateB | 0xFFE00001)) * 0xA5295);

    }

    @Override
    public final int next(final int bits) {
        return ((stateB += 0x9E3779BD) ^ (stateA = (stateA << 21 | stateA >>> 11) * (stateB | 0xFFE00001)) * 0xA5295) >>> (32 - bits);
    }

    @Override
    public final long nextLong() {
        final long t = ((stateB += 0x9E3779BD) ^ (stateA = (stateA << 21 | stateA >>> 11) * (stateB | 0xFFE00001)) * 0xA5295) & 0xFFFFFFFFL;
        return t << 32 | (((stateB += 0x9E3779BD) ^ (stateA = (stateA << 21 | stateA >>> 11) * (stateB | 0xFFE00001)) * 0xA5295) & 0xFFFFFFFFL);
    }

    /**
     * Produces a copy of this MoverCounter32RNG that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this MoverCounter32RNG
     */
    @Override
    public MoverCounter32RNG copy() {
        return new MoverCounter32RNG(stateA, stateB);
    }

    /**
     * Gets the "A" part of the state; if this generator was set with {@link #MoverCounter32RNG()}, {@link #MoverCounter32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be.
     *
     * @return the "A" part of the state, an int
     */
    public int getStateA() {
        return stateA;
    }

    /**
     * Gets the "B" part of the state; if this generator was set with {@link #MoverCounter32RNG()}, {@link #MoverCounter32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be.
     *
     * @return the "B" part of the state, an int
     */
    public int getStateB() {
        return stateB;
    }

    /**
     * Sets the "A" part of the state to any int, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     *
     * @param stateA any int except 0, which this changes to 1
     */
    public void setStateA(final int stateA) {
        this.stateA = stateA;
    }

    /**
     * Sets the "B" part of the state to any int, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     *
     * @param stateB any int except 0, which this changes to 1
     */
    public void setStateB(final int stateB) {
        this.stateB = stateB;
    }

    @Override
    public String toString() {
        return "MoverCounter32RNG with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoverCounter32RNG moverCounter32RNG = (MoverCounter32RNG) o;

        return stateA == moverCounter32RNG.stateA && stateB == moverCounter32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }

}