package com.github.tommyettinger.bench.gwt;

import squidpony.squidmath.RandomnessSource;

public class XoshiroPlusPlus32RNG implements RandomnessSource {
	private int stateA, stateB, stateC, stateD;

	public XoshiroPlusPlus32RNG(long seed) {
		setSeed(seed);
	}

	public void setSeed(long seed) {
		long x = seed;
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateA = (int)(x ^= x >>> 27);
		stateB = (int)(x >>> 32);
		x = (seed + 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateC = (int)(x ^= x >>> 27);
		stateD = (int)(x >>> 32);
	}

	public XoshiroPlusPlus32RNG(int stateA, int stateB, int stateC, int stateD) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
		this.stateD = (stateA|stateB|stateC|stateD) == 0 ? 1 : stateD;
	}

	public long nextLong() {
		int h = (stateA + stateD);
		h = (h << 7 | h >>> 25) + stateA;
		int l = stateC - stateB;
		l = (l << 13 | l >>> 19) + stateC;
		int t = stateB << 9;
		stateC ^= stateA;
		stateD ^= stateB;
		stateB ^= stateC;
		stateA ^= stateD;
		stateC ^= t;
		stateD = (stateD << 11 | stateD >>> 21);
		return (long) h << 32 ^ l;
	}

	public int next(int bits) {
		int result = (stateA + stateD);
		result = (result << 7 | result >>> 25) + stateA;
		int t = stateB << 9;
		stateC ^= stateA;
		stateD ^= stateB;
		stateB ^= stateC;
		stateA ^= stateD;
		stateC ^= t;
		stateD = (stateD << 11 | stateD >>> 21);
		return result >>> (32 - bits);
	}

	public int nextInt() {
		int result = (stateA + stateD);
		//noinspection PointlessBitwiseExpression
		result = (result << 7 | result >>> 25) + stateA | 0; // this isn't pointless on GWT!
		int t = stateB << 9;
		stateC ^= stateA;
		stateD ^= stateB;
		stateB ^= stateC;
		stateA ^= stateD;
		stateC ^= t;
		stateD = (stateD << 11 | stateD >>> 21);
		return result;
	}

	public XoshiroPlusPlus32RNG copy() {
		return new XoshiroPlusPlus32RNG(stateA, stateB, stateC, stateD);
	}
}