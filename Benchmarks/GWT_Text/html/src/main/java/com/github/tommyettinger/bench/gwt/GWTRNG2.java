package com.github.tommyettinger.bench.gwt;

import com.badlogic.gdx.Gdx;
import squidpony.StringKit;
import squidpony.squidmath.AbstractRNG;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.IStatefulRNG;

import java.io.Serializable;

public final class GWTRNG2 extends AbstractRNG implements IStatefulRNG, Serializable {
    private static final long serialVersionUID = 2L;

    public int stateA, stateB;

    public GWTRNG2()
    {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    public GWTRNG2(final int seed) {
        setSeed(seed);
    }
    public GWTRNG2(final long seed) {
        setState(seed);
    }
    public GWTRNG2(final int stateA, final int stateB) {
        setState(stateA, stateB);
    }
    public GWTRNG2(final String seed) {
        setState(CrossHash.hash(seed), seed == null ? 1 : seed.hashCode());
    }
    @Override
    public final int next(int bits) {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 28 | result >>> 4) + 0x9E3779BD >>> (32 - bits);
    }

    @Override
    public final int nextInt() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (result << 28 | result >>> 4) + 0x9E3779BD | 0;
    }

    @Override
    public final long nextLong() {
        int s0 = stateA;
        int s1 = stateB ^ s0;
        final int high = s0 * 31;
        s0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        s1 = (s1 << 13 | s1 >>> 19) ^ s0;
        final int low = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return ((high << 28 | high >>> 4) + 0x9E3779BDL) << 32
                | ((low << 28 | low >>> 4) + 0x9E3779BD & 0xFFFFFFFFL);
    }

    @Override
    public final boolean nextBoolean() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return (s0 * 31 << 28) < 0;
    }

    @Override
    public final double nextDouble() {
        int s0 = stateA;
        int s1 = stateB ^ s0;
        final int high = s0 * 31;
        s0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        s1 = (s1 << 13 | s1 >>> 19) ^ s0;
        final int low = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return  ((((high << 28 | high >>> 4) + 0x9E3779BDL) << 32
                | ((low << 28 | low >>> 4) + 0x9E3779BD & 0xFFFFFFFFL))
                & 0x1FFFFFFFFFFFFFL) * 0x1p-53;
    }

    @Override
    public final float nextFloat() {
        final int s0 = stateA;
        final int s1 = stateB ^ s0;
        final int result = s0 * 31;
        stateA = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 9);
        stateB = (s1 << 13 | s1 >>> 19);
        return ((result << 28 | result >>> 4) + 0x9E3779BD & 0xFFFFFF) * 0x1p-24f;
    }
    @Override
    public int nextInt(int bound) {
        return (int) ((bound * (nextInt() & 0xFFFFFFFFL)) >>> 32) & ~(bound >> 31);
    }

    @Override
    public GWTRNG2 copy() {
        return new GWTRNG2(stateA, stateB);
    }

    @Override
    public Serializable toSerializable() {
        return this;
    }
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
    public void setStateA(int stateA)
    {
        this.stateA = (stateA | stateB) == 0 ? 1 : stateA;
    }
    public int getStateB()
    {
        return stateB;
    }
    public void setStateB(int stateB)
    {
        this.stateB = stateB;
        if((stateB | stateA) == 0) stateA = 1;
    }
    public void setState(int stateA, int stateB)
    {
        this.stateA = stateA == 0 && stateB == 0 ? 1 : stateA;
        this.stateB = stateB;
    }

    @Override
    public long getState() {
        return stateA & 0xFFFFFFFFL | ((long)stateB) << 32;
    }

    @Override
    public void setState(long state) {
        stateA = state == 0 ? 1 : (int)(state & 0xFFFFFFFFL);
        stateB = (int)(state >>> 32);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GWTRNG2 gwtrng2 = (GWTRNG2) o;

        if (stateA != gwtrng2.stateA) return false;
        return stateB == gwtrng2.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }
    
    @Override
    public String toString() {
        return "GWTRNG2 with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

}
