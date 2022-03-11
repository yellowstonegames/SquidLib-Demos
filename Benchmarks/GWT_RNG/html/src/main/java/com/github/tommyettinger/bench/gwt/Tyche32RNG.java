package com.github.tommyettinger.bench.gwt;

import squidpony.squidmath.RandomnessSource;

public class Tyche32RNG implements RandomnessSource {
	private int stateA, stateB, stateC, stateD;

	public Tyche32RNG(long seed) {
		setSeed(seed);
	}

	public void setSeed(long seed) {
		long x = (seed += 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateA = (int) (x ^ x >>> 27);
		x = (seed += 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateB = (int) (x ^ x >>> 27);
		x = (seed += 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateC = (int) (x ^ x >>> 27);
		x = (seed + 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateD = (int) (x ^ x >>> 27);
	}

	public Tyche32RNG(int stateA, int stateB, int stateC, int stateD) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
		this.stateD = stateD;
	}

	public long nextLong() {
		stateB = (stateB << 7 | stateB >>> 25) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 8 | stateD >>> 24) ^ stateA;
		stateA = stateA - stateB | 0;
		stateB = (stateD << 12 | stateD >>> 20) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 16 | stateD >>> 16) ^ stateA;
		stateA = stateA - stateB | 0;
		return (long) stateA << 32 ^ stateB;
	}

	public int next(int bits) {
		stateB = (stateB << 7 | stateB >>> 25) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 8 | stateD >>> 24) ^ stateA;
		stateA = stateA - stateB | 0;
		stateB = (stateD << 12 | stateD >>> 20) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 16 | stateD >>> 16) ^ stateA;
		stateA = stateA - stateB | 0;
		return stateA >>> (32 - bits);
	}

	public int nextInt() {
		stateB = (stateB << 7 | stateB >>> 25) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 8 | stateD >>> 24) ^ stateA;
		stateA = stateA - stateB | 0;
		stateB = (stateD << 12 | stateD >>> 20) ^ stateC;
		stateC = stateC - stateD | 0;
		stateD = (stateD << 16 | stateD >>> 16) ^ stateA;
		stateA = stateA - stateB | 0;
		return stateA;
	}

	public Tyche32RNG copy() {
		return new Tyche32RNG(stateA, stateB, stateC, stateD);
	}
}