package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.Lathe32RNG;
import squidpony.squidmath.RandomnessSource;

/**
 * One of Mark Overton's subcycle generators from <a href="http://www.drdobbs.com/tools/229625477">this article</a>,
 * specifically a cmr^cmr with two 32-bit states; this is the fastest 32-bit generator that still passes statistical
 * tests, plus it's optimized for GWT. It has a period of slightly more than 2 to the 63.86, 0xE89BB7902049CD38, and
 * allows 2 to the 32 initial seeds.
 * <br>
 * This seems to do well in PractRand testing, up to at least 16TB with nothing worse than "unusual", and at least some
 * variants on cmr+cmr pass BigCrush according to Overton. "Chaotic" generators like this one tend to score well in
 * PractRand, but it isn't clear if they will fail other tests. As for speed, this is faster than {@link Lathe32RNG}
 * (which is also high-quality) and is also faster than {@link XoRo32RNG} (which is very fast but has quality issues). Its period is
 * 0xE89BB7902049CD38 or 16,761,192,267,834,314,040 for the largest cycle, which it always initializes into if
 * {@link #setState(int)} is used. setState() only allows 2 to the 32 starting states, but less than 2 to the 64 states
 * are in the largest cycle, so using a long or two ints to set the state seems ill-advised.
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
 */
public final class Mover32RNG implements RandomnessSource {
    private int stateA, stateB;
    public Mover32RNG()
    {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    public Mover32RNG(final int state)
    {
        setState(state);
    }

    /**
     * Not advised for external use; prefer {@link #Mover32RNG(int)} because it guarantees a good subcycle. This
     * constructor allows all subcycles to be produced, including ones with a shorter period.
     * @param stateA
     * @param stateB
     */
    public Mover32RNG(final int stateA, final int stateB)
    {
        this.stateA = stateA == 0 ? 1 : stateA;
        this.stateB = stateB == 0 ? 1 : stateB;
    }

    private static final int[] startingA = {
            0x00000001, 0x9F11DF0B, 0xA9D75498, 0xF4B421DE, 0xB9A82430, 0xED4250C8, 0x15E93F42, 0xDB18983F,
            0x4932B222, 0x312D3B0D, 0x06EE5612, 0xA18E8AF5, 0xC35ED6EC, 0x2296C3BB, 0x897A99B6, 0x113314FA,
            0xE5DC6DF1, 0xA1CB5430, 0x88F7B7A7, 0x26681BF6, 0xA0823E05, 0x37F881AC, 0x6535F6E5, 0x97B3DE1B,
            0xA7EBDF98, 0x8783572B, 0x2C15E0B3, 0x80B297D7, 0x4C4A107F, 0xB27C96F2, 0x304FF465, 0x5C3FB23E,
            0xBB935E48, 0x6E618A42, 0x080A85CE, 0x352770AF, 0xB3E8D627, 0x68B3AA7C, 0x1F11C1F2, 0x7A686342,
            0x517FFE3F, 0x92843302, 0x975D9502, 0xB07AE92C, 0x803D9C7B, 0x60AC7206, 0x1D081127, 0xC143F147,
            0x6A04EB07, 0x2A943751, 0x92C44902, 0xD445ACAF, 0xB85ED97D, 0x08D9E51D, 0x76AFE840, 0xB616A31B,
            0x2CC4B653, 0x7D2D0367, 0x894DBD23, 0xB685E564, 0xE79CD06E, 0xA4A39079, 0x9E668C50, 0x7A6FA73F,
            0x46E9DE96, 0x31603264, 0xFFBEFF3A, 0x2CC114A3, 0xC8595749, 0xA9086FBA, 0x99B297E6, 0xF1A90452,
            0xC9C0AE15, 0x69CDAED1, 0xAB41467E, 0xD1B6D159, 0xD85FA961, 0xF31455D4, 0x104BD460, 0xF8881A1F,
            0x51E9EA5D, 0x53EA0817, 0xAAE7751F, 0x62059A86, 0x3CE2F8D4, 0xC786C03D, 0x4987ED94, 0x5A3003F5,
            0x5B007518, 0x65064757, 0xC235E766, 0x1B9ADC61, 0xAE361B43, 0x6863C452, 0x9503D569, 0x6F465A2C,
            0x6C9372D0, 0x5AF87B87, 0xAD462E2D, 0x4DA7768E, 0x09427461, 0xC70F40EA, 0xF70E648A, 0x85C95AB2,
            0xA33D25B0, 0x5463C91D, 0x503A631E, 0x2774EF9A, 0x0C56BD16, 0xF7065D36, 0xD69F74A2, 0x80246F1F,
            0x4FE448E5, 0x79D2D12C, 0x760B2310, 0x8BB27897, 0x7214F867, 0x3278B28D, 0x3CAAB7AB, 0x20482D22,
            0x48AA7400, 0x8C7865F4, 0x95504AEE, 0x7121E9BC, 0xAB254563, 0x19F96B6B, 0xA04C2CE3, 0x2B6F9021,
    }, startingB = {
            0x00000001, 0xAF5FF586, 0xCB5FA621, 0x147FEB79, 0x0D2B4A87, 0xC09CD63C, 0x493DCA72, 0xD388DD6F,
            0x257C6197, 0xDAFCE14D, 0x2778D97C, 0x9488AF6C, 0xE025E6F9, 0x03F7C4A6, 0x84738348, 0x0F7268E9,
            0xDCD9215E, 0x3FB0AC46, 0x90C7E3DC, 0x4B347FD0, 0x8FB36FBF, 0x525B05B1, 0x311AD316, 0x12375610,
            0xB0E92B50, 0x95707ED7, 0x0D02EFC9, 0x00DA55BD, 0x32FECFC2, 0x8FAB5273, 0xAEBEB303, 0x77B03E4C,
            0x3AA3F9D3, 0x456B44D7, 0x92AFC26E, 0x3E887027, 0x584ED1D3, 0x80DC259B, 0xF3E8D5FA, 0xA7C24C1A,
            0xC415778E, 0xD7BB449A, 0xFECC2032, 0x962501B8, 0x363117E7, 0xEC5E4BCF, 0xE863B55A, 0x1E9BEBF6,
            0x430EDA44, 0x0BBCD30F, 0x81AA0B42, 0x48E8D6DB, 0x4E90224B, 0x5C7C2E57, 0x3D755220, 0xF1F529A5,
            0x64E2BF59, 0x9A80E570, 0xFB851D5E, 0xAAF06CAF, 0xBD18D160, 0x81BAD104, 0xCB7F85A5, 0x05D3FEB2,
            0x3E7F34B6, 0xE2B69058, 0xACCA0C95, 0xF950057D, 0x8EC11DF4, 0xF2CBDB24, 0x863829BF, 0xAEBCA905,
            0xD24BDD76, 0xD3D7E317, 0x54829318, 0x52A10C3F, 0xB9B86F0A, 0xFD746569, 0x7DAC4A01, 0x2B9F88E9,
            0xF3184DB6, 0x50C3A49D, 0x8A22549E, 0x9913C627, 0xEEDCEA6F, 0x9C95D489, 0x7C2A42E7, 0x9E9BD919,
            0x45ACDC0D, 0xECDEEDD9, 0xA24FCCC9, 0xC195D62D, 0x289864BA, 0x89CBC109, 0x4BC5BD74, 0xB4ED95B4,
            0x5E3CF4C9, 0x0B3EF952, 0x99828E82, 0xCFDC5F50, 0xD5307F76, 0x6B905E5D, 0x818DB5FD, 0x747C61A8,
            0x68B48552, 0x32C2352B, 0xC0C1FEAD, 0x63C1C57C, 0x5DE87FA0, 0xA97BE18A, 0x14417618, 0x1C7358F7,
            0x9A7D8D79, 0xDBD3B474, 0xF0594ACC, 0x9D50AF52, 0x0A618F87, 0x23519C0F, 0xFA9E8AB9, 0xA37FDA83,
            0x74B0372D, 0x48590DFF, 0xB8430CA4, 0x3B30E4A4, 0xC8946F5C, 0xE5D7681B, 0x4D9590CC, 0x634A5F38,
    };

    public final void setState(final int s) {
        stateA = startingA[s >>> 9 & 0x7F];
        for (int i = s & 0x1FF; i > 0; i--) {
            stateA = Integer.rotateLeft(stateA * 0x9E37, 17);
        }
        stateB = startingB[s >>> 25];
        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
            stateB = Integer.rotateLeft(stateB * 0x4E6D, 14);
        }
    }

    public final int nextInt()
    {
        return (stateA = Integer.rotateLeft(stateA * 0x9E37, 17)) ^ (stateB = Integer.rotateLeft(stateB * 0x4E6D, 14));
    }
    @Override
    public final int next(final int bits)
    {
        return ((stateA = Integer.rotateLeft(stateA * 0x9E37, 17))
                ^ (stateB = Integer.rotateLeft(stateB * 0x4E6D, 14))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        final long t = (stateA = Integer.rotateLeft(stateA * 0x9E37, 17))
                ^ (stateB = Integer.rotateLeft(stateB * 0x4E6D, 14));
        return t << 32 ^ ((stateA = Integer.rotateLeft(stateA * 0x9E37, 17))
                ^ (stateB = Integer.rotateLeft(stateB * 0x4E6D, 14)));
    }

    /**
     * Produces a copy of this Mover32RNG that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this Mover32RNG
     */
    @Override
    public Mover32RNG copy() {
        return new Mover32RNG(stateA, stateB);
    }

    /**
     * Gets the "A" part of the state; if this generator was set with {@link #Mover32RNG()}, {@link #Mover32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be. 
     * @return the "A" part of the state, an int
     */
    public int getStateA()
    {
        return stateA;
    }

    /**
     * Gets the "B" part of the state; if this generator was set with {@link #Mover32RNG()}, {@link #Mover32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be. 
     * @return the "B" part of the state, an int
     */
    public int getStateB()
    {
        return stateB;
    }
    /**
     * Sets the "A" part of the state to any int, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     * @param stateA any int
     */
    public void setStateA(final int stateA)
    {
        this.stateA = stateA;
    }

    /**
     * Sets the "B" part of the state to any int, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     * @param stateB any int
     */
    public void setStateB(final int stateB)
    {
        this.stateB = stateB;
    }
    @Override
    public String toString() {
        return "Mover32RNG with stateA 0x" + StringKit.hex(stateA) + " and stateB 0x" + StringKit.hex(stateB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mover32RNG mover32RNG = (Mover32RNG) o;

        return stateA == mover32RNG.stateA && stateB == mover32RNG.stateB;
    }

    @Override
    public int hashCode() {
        return 31 * stateA + stateB | 0;
    }

//    public static void main(String[] args)
//    {
//        // A 10 0xC010AEB4
//        // B 22 0x195B9108
//        // all  0x04C194F3485D5A68
//
//        // A 17 0xF7F87D28
//        // B 14 0xF023E25B 
//        // all  0xE89BB7902049CD38
//
//
//        // A11 B14 0xBBDA9763B6CA318D
//        // A8  B14 0xC109F954C76CB09C
//        // A17 B14 0xE89BB7902049CD38
////        BigInteger result = BigInteger.valueOf(0xF7F87D28L);
////        BigInteger tmp = BigInteger.valueOf(0xF023E25BL);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("0x%016X\n", result.longValue());
//        int stateA = 1, i = 0;
//        for (; ; i++) {
//            if((stateA = Integer.rotateLeft(stateA * 0x9E37, 17)) == 1)
//            {
//                System.out.printf("0x%08X\n", i);
//                break;
//            }
//        }
//        BigInteger result = BigInteger.valueOf(i & 0xFFFFFFFFL);
//        i = 0;
//        for (; ; i++) {
//            if((stateA = Integer.rotateLeft(stateA * 0x4E6D, 14)) == 1)
//            {
//                System.out.printf("0x%08X\n", i);
//                break;
//            }
//        }         
//        BigInteger tmp = BigInteger.valueOf(i & 0xFFFFFFFFL);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        System.out.printf("\n0x%016X\n", result.longValue());
//
//    }
    
//    public static void main(String[] args)
//    {
//        Mover32RNG m = new Mover32RNG();
//        System.out.println("int[] startingA = {");
//        for (int i = 0, ctr = 0; ctr < 128; ctr++, i+= 0x00000200) {
//            m.setState(i);
//            System.out.printf("0x%08X, ", m.stateA);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("}, startingB = {");
//        for (int i = 0, ctr = 0; ctr < 128; ctr++, i+= 0x02000000) {
//            m.setState(i);
//            System.out.printf("0x%08X, ", m.stateB);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("};");
//    }
}
