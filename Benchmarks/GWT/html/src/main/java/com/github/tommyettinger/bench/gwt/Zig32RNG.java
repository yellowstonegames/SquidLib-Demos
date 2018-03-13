package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.StatefulRandomness;

import java.io.Serializable;

/**
 * A 32-bit generator that needs no math on 64-bit values to calculate high-quality pseudo-random numbers, and has been
 * adapted to meet the needs of GWT while maintaining compatibility with other JDK platforms. It is a combination of the
 * 32-bit SplitMix algorithm Light32RNG uses with a 32-bit Marsaglia XorShift generator, adding the XorShift's
 * latest state to an almost-finished SplitMix result before completing the SplitMix steps. It has 2 ints for state,
 * stateA and stateB, where stateA is modified by the XorShift and cannot be given 0, and stateB is modified by SplitMix
 * and can be assigned any int. State A will never be 0, but will be every other int with equal frequency; it repeats
 * every {@code Math.pow(2, 32) - 1} generated numbers. State B goes through every int in a Weyl sequence (adding a
 * large enough odd constant), it is given to SplitMix to alter. The total period is 0xFFFFFFFF00000000
 * (18446744069414584320). Quality is excellent here, and this passes PractRand without failures and only one anomaly
 * (deemed "unusual", not any kind of "suspicious", mid-way through testing) on 32 TB of random values.
 * <br>
 * Although Zig32RNG has a {@link #determine(int, int)} method, calling it is considerably more complex than other
 * RandomnessSources that provide determine() methods. It also doesn't allow skipping through the state, and a moderate
 * amount of the possible values that can be provided with {@link #setState(long)} will be changed before this can use
 * them (there are fewer than 2 to the 64 possible states, but only somewhat).
 * <br>
 * Created by Tommy Ettinger on 7/15/2017.
 */
public final class Zig32RNG implements StatefulRandomness, Serializable {
    private int a, b;
    private static final long serialVersionUID = -374415589203474497L;

    /**
     * Constructs a Zig32RNG with a random state, using two calls to Math.random().
     */
    public Zig32RNG()
    {
        this((int)((Math.random() * 2.0 - 1.0) * 0x80000000), (int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }

    /**
     * Constructs a Zig32RNG with the exact state A given and a similar state B (the least significant bit of state B
     * will always be 1 internally, so even values for state B will be incremented and odd values will be kept as-is).
     * @param stateA any int
     * @param stateB any int, but the last bit will not be used (e.g. 20 and 21 will be treated the same)
     */
    public Zig32RNG(int stateA, int stateB)
    {
        a = stateA == 0 ? 1 : stateA;
        b = stateB;
    }

    /**
     * Takes 32 bits of state and uses it to randomly fill the 63 bits of state this uses.
     * @param statePart any int
     */
    public Zig32RNG(int statePart)
    {
        b = statePart;
        a = ~statePart;
        a ^= a >>> 5;
        a ^= a << 17;
        a ^= a >>> 13;
        a ^= statePart;
        if(a == 0) a = 1; // not sure if this is possible
    }

    /**
     * Constructs a Zig32RNG using a long that combines the two parts of state, as from {@link #getState()}.
     * @param stateCombined a long that combines state A and state B, with state A in the less significant 32 bits
     */
    public Zig32RNG(long stateCombined)
    {
        this((int)stateCombined, (int)(stateCombined >>> 32));
    }

    public final int nextInt() {
        int z = (b = b + 0x632BE5AB | 0);
        a ^= a >>> 14;
        z = (z ^ z >>> 15) * 0xAD7B5 | 0;
        a ^= a << 1;
        z = (z ^ z >>> 12) * 0xCDD1D + (a ^= a >>> 15) | 0;
        return z ^ z >>> 13;
    }

    /**
     * Using this method, any algorithm that might use the built-in Java Random
     * can interface with this randomness source.
     *
     * @param bits the number of bits to be returned
     * @return the integer containing the appropriate number of bits
     */
    @Override
    public final int next(int bits) {
        int z = (b = b + 0x632BE5AB | 0);
        a ^= a >>> 14;
        z = (z ^ z >>> 15) * 0xAD7B5 | 0;
        a ^= a << 1;
        z = (z ^ z >>> 12) * 0xCDD1D + (a ^= a >>> 15) | 0;
        return (z ^ (z >>> 13)) >>> (32 - bits);
    }

    /**
     * Using this method, any algorithm that needs to efficiently generate more
     * than 32 bits of random data can interface with this randomness source.
     * <p>
     * Get a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive).
     *
     * @return a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive)
     */
    @Override
    public final long nextLong() {
        int z = (b + 0x632BE5AB | 0), y = (b = b + 0xC657CB56 | 0);
        a ^= a >>> 14;
        z = (z ^ z >>> 15) * 0xAD7B5 | 0;
        a ^= a << 1;
        z = (z ^ z >>> 12) * 0xCDD1D + (a ^= a >>> 15) | 0;
        a ^= a >>> 14;
        y = (y ^ y >>> 15) * 0xAD7B5 | 0;
        a ^= a << 1;
        y = (y ^ y >>> 12) * 0xCDD1D + (a ^= a >>> 15) | 0;
        return (long)(y ^ (y >>> 13)) << 32 ^ (z ^ (z >>> 13));
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public Zig32RNG copy() {
        return new Zig32RNG(a, b);
    }

    public int getStateA()
    {
        return a;
    }
    public void setStateA(int stateA)
    {
        a = stateA == 0 ? 1 : stateA;
    }
    public int getStateB()
    {
        return b;
    }
    public void setStateB(int stateB)
    {
        b = stateB;
    }

    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return (long) b << 32 | (a & 0xFFFFFFFFL);
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     * This implementation may not use the state verbatim, since only 63 bits are actually used as state here.
     * Specifically, the bit in state that would be masked by 0x0000000100000000L is unused; it is always 1 internally.
     * @param state a 64-bit long. You should avoid passing 0, even though some implementations can handle that.
     */
    @Override
    public void setState(long state) {
        a = (int)(state & 0xFFFFFFFFL);
        if(a == 0) a = 1;
        b = (int) (state >>> 32);
    }

    /**
     * Sets the current internal state of this Zig32RNG with two ints.
     * The least significant bit of stateB is unused; it is always 1 internally.
     * @param stateA any int
     * @param stateB any int, but the last bit will not be used (e.g. 20 and 21 will be treated the same)
     */
    public void setState(int stateA, int stateB)
    {
        a = stateA == 0 ? 1 : stateA;
        b = stateB;
    }

    @Override
    public String toString() {
        return "Zig32RNG with stateA 0x" + StringKit.hex(a) + " and stateB 0x" + StringKit.hex(b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Zig32RNG that = (Zig32RNG) o;

        return a == that.a && b == that.b;
    }

    @Override
    public int hashCode() {
        return 31 * a + b | 0;
    }

    /**
     * Gets a pseudo-random int determined wholly by the given state variables a and b, which should change every call.
     * Call with {@code determine((a = (a = (a ^= a >>> 14) ^ a << 1) ^ a >>> 15), b = b + 0x632BE5AB | 0)}, where a and
     * b are int variables used as state. The complex call to this method allows it to remain static. In the update for
     * b, the bitwise OR with 0 is only needed for GWT in order to force overflow to wrap instead of losing precision (a
     * quirk of the JS numbers GWT uses). If you know you won't target GWT you can use {@code b += 0x632BE5AB}. This
     * should be fairly fast on GWT because most PRNGs (that can pass any decent statistical quality tests) use either
     * 64-bit long values or many int variables for state, while this can get by with just two int variables. Using long
     * on GWT is usually the only reasonable option if you expect arithmetic overflow (because long is emulated by GWT
     * to imitate the behavior of a JDK, and is considerably slower as a result), but by careful attention to this
     * generator, it should have identical results on GWT and on desktop/Android JVMs with only int math.
     * <br>
     * You may find it more convenient to just instantiate a Zig32RNG object rather than using the complex state update;
     * using the methods on a Zig32RNG object may also be more efficient because some operations can be performed in
     * parallel on the same (modern) processor.
     * @param a must be non-zero and updated with each call, using {@code a = (a = (a ^= a >>> 14) ^ a << 1) ^ a >>> 15}
     * @param b must be updated with each call, using {@code b = b + 0x632BE5AB | 0}, with {@code | 0} needed for GWT
     * @return a pseudo-random int, equidistributed over a period of 0xFFFFFFFF00000000; can be any int
     */
    public static int determine(int a, int b) {
        return (b = ((b = (b ^ b >>> 15) * 0xAD7B5) ^ b >>> 12) * 0xCDD1D + a) ^ b >>> 13;
    }
    /**
     * Gets a pseudo-random int between 0 and the given bound, using the given ints a and b as the state; these state
     * variables a and b should change with each call. The exclusive bound can be negative or positive. Call with
     * {@code determine((a = (a = (a ^= a >>> 14) ^ a << 1) ^ a >>> 15), b = b + 0x632BE5AB | 0)}, where a and b are int
     * variables used as state. The complex call to this method allows it to remain static. In the update for b, the
     * bitwise OR with 0 is only needed for GWT in order to force overflow to wrap instead of losing precision (a quirk
     * of the JS numbers GWT uses). If you know you won't target GWT you can use {@code b += 0x632BE5AB}.
     * @param a must be non-zero and updated with each call, using {@code a = (a = (a ^= a >>> 14) ^ a << 1) ^ a >>> 15}
     * @param b must be updated with each call, using {@code b = b + 0x632BE5AB | 0}, with {@code | 0} needed for GWT
     * @param bound the outer exclusive limit on the random number; should be between -32768 and 32767 (both inclusive)
     * @return a pseudo-random int, between 0 (inclusive) and bound (exclusive)
     */
    public static int determineBounded(int a, int b, int bound)
    {
        return ((bound * (((b = ((b = (b ^ b >>> 15) * 0xAD7B5) ^ b >>> 12) * 0xCDD1D + a) ^ b >>> 13) & 0x7FFF)) >> 15);
    }

}
