/*
 * Copyright (c) 2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.tommyettinger.bench.gwt;

import com.github.tommyettinger.random.EnhancedRandom;
import squidpony.squidmath.RandomnessSource;

public final class ChopRandomness implements RandomnessSource {

    /**
     * The first state; can be any int.
     */
    protected int stateA;
    /**
	 * The second state; can be any int.
     */
    protected int stateB;
    /**
     * The third state; can be any int. If this has just been set to some value, then the next call to
     * {@link #nextInt()} will return that value as-is. Later calls will be more random.
     */
    protected int stateC;
    /**
     * The fourth state; can be any int.
     */
    protected int stateD;

    /**
     * Creates a new ChopRandom with a random state.
     */
    public ChopRandomness() {
        this((int)EnhancedRandom.seedFromMath(),
            (int)EnhancedRandom.seedFromMath(),
            (int)EnhancedRandom.seedFromMath(),
            (int)EnhancedRandom.seedFromMath());
    }

    /**
     * Creates a new ChopRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public ChopRandomness(long seed) {
        setSeed(seed);
    }

    /**
     * Creates a new ChopRandom with the given four states; all {@code int} values are permitted.
     * These states will be used verbatim.
     * @param stateA any {@code int} value
     * @param stateB any {@code int} value
     * @param stateC any {@code int} value; will be returned exactly on the first call to {@link #nextInt()}
     * @param stateD any {@code int} value
     */
    public ChopRandomness(int stateA, int stateB, int stateC, int stateD) {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    public void setSeed(long seed) {
        long x = (seed += 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateA = (int)(x ^ x >>> 27);
        x = (seed += 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateB = (int)(x ^ x >>> 27);
        x = (seed += 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateC = (int)(x ^ x >>> 27);
        x = (seed + 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateD = (int)(x ^ x >>> 27);
    }

    public long getStateA() {
        return stateA;
    }

    /**
     * Sets the first part of the state by casting the parameter to an int.
     * @param stateA can be any long, but will be cast to an int before use
     */
    public void setStateA(long stateA) {
        this.stateA = (int)stateA;
    }

    public long getStateB() {
        return stateB;
    }

    /**
     * Sets the second part of the state by casting the parameter to an int.
     * @param stateB can be any long, but will be cast to an int before use
     */
    public void setStateB(long stateB) {
        this.stateB = (int)stateB;
    }

    public long getStateC() {
        return stateC;
    }

    /**
     * Sets the third part of the state by casting the parameter to an int.
     * Note that if you call {@link #nextInt()} immediately after this,
     * it will return the given {@code stateC} (cast to int) as-is, so you
     * may want to call some random generation methods (such as nextInt()) and discard
     * the results after setting the state.
     * @param stateC can be any long, but will be cast to an int before use
     */
    public void setStateC(long stateC) {
        this.stateC = (int)stateC;
    }

    public long getStateD() {
        return stateD;
    }

    /**
     * Sets the fourth part of the state by casting the parameter to an int.
     * @param stateD can be any long, but will be cast to an int before use
     */
    public void setStateD(long stateD) {
        this.stateD = (int)stateD;
    }

    public void setState(long stateA, long stateB, long stateC, long stateD) {
        this.stateA = (int)stateA;
        this.stateB = (int)stateB;
        this.stateC = (int)stateC;
        this.stateD = (int)stateD;
    }

    @Override
    public long nextLong() {
        int fa = stateA;
        int fb = stateB;
        int fc = stateC;
        int fd = stateD;
        int ga = fb ^ fc; ga = (ga << 26 | ga >>>  6);
        int gb = fc ^ fd; gb = (gb << 11 | gb >>> 21);
        final int gc = fa ^ fb + fc;
        final int gd = fd + 0xADB5B165 | 0;
        stateA = gb ^ gc;
        stateA = (stateA << 26 | stateA >>> 6);
        stateB = gc ^ gd;
        stateB = (stateB << 11 | stateB >>> 21);
        stateC = ga ^ gb + gc;
        stateD = gd + 0xADB5B165 | 0;
        return (long)fc << 32 | (gc & 0xFFFFFFFFL);
    }
    @Override
    public int next(int bits) {
        final int fa = stateA;
        final int fb = stateB;
        final int fc = stateC;
        final int fd = stateD;
        stateA = fb ^ fc;
        stateA = (stateA << 26 | stateA >>> 6);
        stateB = fc ^ fd;
        stateB = (stateB << 11 | stateB >>> 21);
        stateC = fa ^ fb + fc;
        stateD = fd + 0xADB5B165 | 0;
        return fc >>> (32 - bits);
    }

    public int nextInt () {
        final int fa = stateA;
        final int fb = stateB;
        final int fc = stateC;
        final int fd = stateD;
        stateA = fb ^ fc;
        stateA = (stateA << 26 | stateA >>> 6);
        stateB = fc ^ fd;
        stateB = (stateB << 11 | stateB >>> 21);
        stateC = fa ^ fb + fc;
        stateD = fd + 0xADB5B165 | 0;
        return fc;
    }

    @Override
    public ChopRandomness copy() {
        return new ChopRandomness(stateA, stateB, stateC, stateD);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ChopRandomness that = (ChopRandomness)o;

        return stateA == that.stateA && stateB == that.stateB && stateC == that.stateC && stateD == that.stateD;
    }

    public String toString() {
        return "ChopRandomness{" + "stateA=" + (stateA) + ", stateB=" + (stateB) + ", stateC=" + (stateC) + ", stateD=" + (stateD) + "}";
    }
}
