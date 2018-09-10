package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.Lathe32RNG;
import squidpony.squidmath.RandomnessSource;

/**
 * One of Mark Overton's subcycle generators from <a href="http://www.drdobbs.com/tools/229625477">this article</a>,
 * modified to act more like LightRNG and to only use one subcycle generator along with one full-cycle generator;
 * this is the fastest 32-bit generator on desktop JREs that still passes statistical tests, plus it's optimized for GWT
 * (it isn't as fast as GWTRNG/Lathe32RNG when used on GWT, about 3/4 the speed in Firefox and Chrome, but in Opera it's
 * the same speed as Lathe32RNG).
 * It has a period of more than 2 to the 63.99, 0xFFCC893300000000, and allows 2 to the 32 initial seeds. Even if
 * improper initialization gets it into a smaller subcycle, the minimum period is greater than 2 to the 32.
 * <br>
 * This seems to do well in PractRand testing, up to at least 512GB with nothing worse than "unusual" (still testing).
 * "Chaotic" generators like this one tend to score well in
 * PractRand, but it isn't clear if they will fail other tests. As for speed, this is faster than {@link Lathe32RNG}
 * (which is also high-quality) and is also faster than {@link XoRo32RNG} (which is very fast but has quality issues). 
 * Its period is 0xFFCC893300000000, or 18,432,258,227,056,934,912 for the largest cycle, which it always initializes
 * into if {@link #setState(int)} is used. setState() only allows 2 to the 32 starting states, but less than 2 to the 64
 * states are in the largest cycle, so using a long or two ints to set the state seems ill-advised.
 * <br>
 * This is a RandomnessSource but not a StatefulRandomness because it needs to take care and avoid seeds that would put
 * it in a short-period subcycle. It uses two generators with different cycle lengths (one has a period of 0xFFCC8933,
 * and the other a full 0x100000000), and skips at most 65536 times into the shorter-cycle generator when seeding.
 * It uses constants to store 128 known midpoints for the subcycle generator, which ensures it calculates an advance for
 * only one generator, and at most 511 times. 
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
            0x00000001, 0xCB2DA1A7, 0x215A5ADF, 0x2688266B, 0xEA31ECEE, 0x3F02F6A8, 0xB0833422, 0xC791ACA6,
            0x976236C8, 0xF57961C0, 0x16EBE830, 0xCCCC2F10, 0x165D9801, 0x15E02FEA, 0xA302CC65, 0xAF68AE37,
            0x4997CCA3, 0xF331F604, 0xF1DE5DA7, 0x07F21BA7, 0xD1752EC7, 0x308B16F2, 0x1B92D899, 0xF1A38AC8,
            0x58F317B2, 0x1CC8EC79, 0x62588F4B, 0x975BF8FE, 0xE589C2D0, 0xB087C03D, 0x600F5DD0, 0xA32BD629,
            0x3B52D26D, 0x0C7C18FD, 0xEB037A63, 0xE6F8BC93, 0x2CD250CF, 0x84327882, 0xA708FC6E, 0x5873EF12,
            0x72FD78CF, 0xFFE73771, 0x18817285, 0x8EB3BC50, 0xE68597E0, 0xDF719E77, 0x35FE32C8, 0xF60532A1,
            0xE93A1484, 0x697DF36B, 0xDFD41306, 0x37E0FADD, 0x6883EB39, 0xAF9CF955, 0xE11EB329, 0xDA951CC2,
            0x325ECD67, 0x1DD8AC79, 0x7632669F, 0x0949BCB0, 0x965B0557, 0xB72DC0BC, 0x84448A7C, 0x6AC9B9CF,
            0x92B7742A, 0xCFB27744, 0xFF154B26, 0xFD11E5F7, 0x5B6DE8D4, 0x59727211, 0x0A36FF7F, 0x56657899,
            0xF9848758, 0x59415D9E, 0xE70E6901, 0x90858D00, 0x10B73995, 0x324FC7AD, 0xC62F801D, 0x4BBDA0E8,
            0x70C8FDD5, 0xCC4376F1, 0x489AD7B7, 0xF4FB2500, 0x2279E051, 0x7840BF9E, 0x876AABF9, 0xF374F7BD,
            0x6074B429, 0xC2EE6430, 0x238172DA, 0xFE3D050E, 0x5EF2F6B4, 0xF6359946, 0x127AAD89, 0xECA6FA56,
            0x678B27CE, 0xDCD03A3C, 0xA45371BB, 0x5F2F422C, 0xE26B613C, 0x70DD9AF4, 0x1B0787BB, 0x0B8D2553,
            0x3A430C3F, 0xAFF29AE2, 0x9DFAEB51, 0x1DE0F40E, 0x0467D74A, 0x85949411, 0xF8BC0358, 0x558BA744,
            0x41A5B43A, 0x6B7E1C89, 0x9BF095BD, 0x5E2473CC, 0x4DFBF45B, 0xFB3510DC, 0xB7EC5786, 0xA99D6129,
            0x120988F6, 0x796A7DE7, 0x9DEFD945, 0x0D2B25CE, 0xB7C1107E, 0x72E29D75, 0x85E01D79, 0x69AB992F,
    }, startingB = {
            0x00000001, 0xAB7EE445, 0x6FB35C9C, 0x459AB7A2, 0xEA61D065, 0x306F5E5F, 0xCE50A64A, 0x6D76B642,
            0x11F3C6D4, 0xB3FF1D66, 0x657E6790, 0x4C62472D, 0xBEABAB16, 0xFD455176, 0xDCB98EDB, 0x1FC27360,
            0x80C1241C, 0xC0C5BCC0, 0x6A67518B, 0xF2D69A39, 0xFA7D6C16, 0xE906A517, 0x899FFA7B, 0x2E42A99D,
            0xBAF5B6E8, 0x3BBDC45A, 0x2497A707, 0xEA2DB138, 0x7D4ABF97, 0x552F5D4E, 0x15FE4BBD, 0xBC51DF5A,
            0x465BDC95, 0x736A018F, 0x8A72CB63, 0x103119BE, 0x40403117, 0xA295957B, 0xCDDA9C19, 0xF0551CC6,
            0x77CBAB76, 0xA054FD6A, 0x8974C93F, 0x8E314DC1, 0x42BC030E, 0x7F090540, 0x177998EA, 0x20457F09,
            0xC13609D7, 0xA2683753, 0xE9F84638, 0x1BE07B83, 0x5DB36480, 0x39AE5B3A, 0xE044E164, 0x6E6B6191,
            0x6036E5C8, 0x00703FE1, 0x53935ED8, 0x6B4443F5, 0x8FB91605, 0x146478C9, 0x2D0429BB, 0x86E8F88A,
            0xD8DFFDB7, 0x77223F7D, 0x2B065674, 0xD80D2DD6, 0x0DFE5CEB, 0x44A495A5, 0x758EF0A9, 0x5FB55BA5,
            0x8935A9B1, 0x84189069, 0xAA2194BC, 0x5FB95103, 0x6B60B887, 0xC63A769E, 0xA74BE357, 0x9F71B1F8,
            0x3320B09E, 0xD369B3FC, 0xBCDB4B4E, 0xDC4DBCC9, 0x01F67CD2, 0xB3F6AA2B, 0x082CA2B3, 0xA54F168F,
            0x0A9F82C9, 0x77DC3F93, 0x18D32D96, 0x1FC3FCE5, 0x97542B7A, 0x88CA9F81, 0x75370CE4, 0x8C2749C3,
            0x94B63AE4, 0xC55E3BB7, 0x176BA775, 0x8C2BFEFC, 0x8C457557, 0xD8BFFD54, 0xB3D322DC, 0x072D766C,
            0x40912BF4, 0x99CA7F36, 0xBC78BE45, 0x22F95B6B, 0xB37B05C9, 0x23493DB3, 0xCFBDC9C3, 0xB0379084,
            0x2A2BFA20, 0x9A9DA93D, 0xCDE62486, 0x079CF8E6, 0x5B45CF64, 0xA19945A8, 0x196C1AA8, 0x9B19C771,
            0x702CC28B, 0xFF4C5B02, 0x2FDD78D2, 0x71FFBD4E, 0xDF4C60A4, 0x143FAB0B, 0xAD9C8EB0, 0x6F35837D,
    };

    public final void setState(final int s) {
        stateA = startingA[s >>> 9 & 0x7F];
        for (int i = s & 0x1FF; i > 0; i--) {
            stateA *= 0x89A7;
            stateA = (stateA << 13 | stateA >>> 19);
        }
        stateB = startingB[s >>> 25];
        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
            stateB *= 0xBCFD;
            stateB = (stateB << 17 | stateB >>> 15);
        }
    }
    
    public final int nextInt()
    {
        int y = stateA * 0x89A7;
        stateA = (y = (y << 13 | y >>> 19));
        final int x = stateB * 0xBCFD;
        return (y ^ (stateB = (x << 17 | x >>> 15)));
    }
    @Override
    public final int next(final int bits)
    {
        int y = stateA * 0x89A7;
        stateA = (y = (y << 13 | y >>> 19));
        final int x = stateB * 0xBCFD;
        return (y ^ (stateB = (x << 17 | x >>> 15))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        int y = stateA * 0x89A7;
        y = (y << 13 | y >>> 19);
        int x = stateB * 0xBCFD;
        final long t = y ^ (x = (x << 17 | x >>> 15));
        y *= 0x89A7;
        stateA = (y = (y << 13 | y >>> 19));
        x *= 0xBCFD;
        return t << 32 ^ (y ^ (stateB = (x << 17 | x >>> 15)));
    }

//    private static final int[] startingA = {
//            0x00000001, 0xE0F813FB, 0x3853A562, 0x416A00FB, 0x969733D0, 0x68FC5927, 0x99A19AAE, 0xC4C790CC,
//            0x996A09D2, 0x6914B3DE, 0xB1A25EE1, 0xBCF1B09F, 0x442C6EF5, 0x475E34F6, 0xCC900DB2, 0x07E720B0,
//            0x0058393C, 0xA8D71A86, 0xFFA9E99A, 0x2A123AD8, 0x162BA47D, 0x6993B504, 0xAFB932FD, 0xFAD8E139,
//            0x77990180, 0x452C71B3, 0xF265CBCD, 0xC0F7B73D, 0x8A7F9F87, 0x845E4932, 0x8B5D1621, 0xBCAFA92A,
//            0xD6D7A1EC, 0x8E2E18A7, 0x33E41E53, 0xDD326FE3, 0x2F05B4EA, 0xB8F6F3C7, 0x9E3DD946, 0x1F0394CB,
//            0x7F0F299C, 0x61CB8AA1, 0xA9E05E22, 0xBBCD908F, 0xB070DD10, 0x0A2DDBB2, 0xCD0E44D6, 0x0D07CF67,
//            0x6318736A, 0x74851D1A, 0x67403862, 0x7EAF0F09, 0xDCDFDA15, 0xAE41DED5, 0x6C2B2FB6, 0x6C2A7DA7,
//            0x3A469C2E, 0x7B3EA36E, 0x858A16F4, 0x5FE35777, 0x21D4F6AF, 0x8547F196, 0x0D562186, 0x8F88EF25,
//            0x38B8DB7C, 0x5C15CB9D, 0x75F3C219, 0xD0CC6A7B, 0xBBD4DA22, 0x38174193, 0xDEF01557, 0xF8B890C3,
//            0x5CAE3E75, 0xE366F7B7, 0x10BB0CAB, 0xF82D9CB8, 0xB7D312CC, 0x69F60C1C, 0x512E23E0, 0xBFA77DBB,
//            0x8CCC44D0, 0x4A486348, 0x233AC5DD, 0xEFF581CF, 0x51D5EA09, 0x6448830E, 0xFF0B9140, 0xA0F34887,
//            0x92C0F3B8, 0xBBF60BDF, 0xCBE1A1A9, 0x31BF3301, 0xEC6E09A5, 0x87F67486, 0x272E487B, 0x3CEA124F,
//            0x55282704, 0x6157B070, 0x6102419F, 0x58651391, 0x1C305E1A, 0xFE77CF87, 0x6C78B78B, 0xFF22C4C9,
//            0x077E96D6, 0xF37FE662, 0x1B08A31A, 0x9C38F87C, 0x55C56A19, 0xE31AA568, 0x11E35EBE, 0xE7C81D24,
//            0xE6C3D7E0, 0x87A9B50F, 0xB7D3547D, 0x34F0B5C9, 0xC4F93C05, 0xEAC59BB3, 0xDEA348B6, 0x23C56119,
//            0xB76AE1A3, 0x0D8DA124, 0x4927CF73, 0x2580B158, 0x1F731C31, 0x9DEDEC30, 0x12BAFE34, 0x0B139973,
//    }, startingB = {
//            0x00000001, 0xFDE752F6, 0xC84AF5A3, 0xA87499BC, 0xD90270DE, 0xE90ACB23, 0x9257E51C, 0x9EE34DF2,
//            0x6B6145AE, 0xEC75C190, 0xBCC18895, 0x32D10686, 0x317F5535, 0x5A97DDE9, 0x6A49F707, 0x4FD63148,
//            0x31C884C3, 0xEE68C32E, 0xDECE7562, 0x989C6CA0, 0x449BAC70, 0xBFF6415E, 0xEB06F9A5, 0xABB8890F,
//            0x859213D3, 0x7D9C5EB7, 0xCEFB7D21, 0x1054397A, 0x47133437, 0x39EA89DC, 0x57F3FA9B, 0xB7C9825D,
//            0xBF5CF78E, 0x67AD1C2D, 0x841B6434, 0x5E82C45F, 0x97948021, 0xBF76909D, 0xF74020C5, 0x52F504F2,
//            0x6B4466F5, 0x6742D957, 0x9FF19ABC, 0x57104213, 0x39B5F5D1, 0x730E17AB, 0x440F8E1B, 0x3B1D4A42,
//            0x8ED24EBF, 0x7ECE6545, 0x710D59C2, 0x96E768D8, 0x3A33BFAF, 0x775A6C30, 0x5DE6E29C, 0x27B5C59D,
//            0x24630AF2, 0x2F174DBC, 0xBF2D6A4C, 0xD6334C3B, 0xBE53EF43, 0xC1ECD43B, 0x6A60478E, 0x108F9B4A,
//            0xA604530E, 0x4570407F, 0xCC6B423A, 0x47C0C0F9, 0xB09671A7, 0xE9A6BDFD, 0xABBD2751, 0x2524B64F,
//            0x69B20A61, 0x0E696B30, 0x81633930, 0x0006ED93, 0x5C12F794, 0xE82602E0, 0xB1D5EDC2, 0xD31990D7,
//            0xA9F6060E, 0x3C6BFA34, 0xD193C00F, 0x82D7DB5E, 0x82C49F3C, 0x7A771155, 0x8F0D9415, 0x8A17684D,
//            0x2D77E8D6, 0x2913858E, 0xB533A466, 0x8129764F, 0x63162CD2, 0x5F2DD6F6, 0xBF8B497A, 0xB43BE06C,
//            0x98654103, 0x8C28E2AD, 0xDF898920, 0x5D7AA02D, 0x402A4E1F, 0x31CAE12C, 0xA03FB63D, 0x45F0D48A,
//            0xEFB636E4, 0x15BA997E, 0x03BE743D, 0x50C3829B, 0x1995789C, 0x9EB14174, 0x7E0FACE3, 0xEACE464A,
//            0x8FD5E698, 0x921A2C9A, 0xBA254C0A, 0x946AD363, 0x380AAFC3, 0xCEA4C41D, 0x13789C1F, 0xD5F712C3,
//            0x9599AECF, 0x03777BEA, 0xE27AD2AD, 0x17F9E31B, 0xA3AE7641, 0x4A607868, 0x7747EE23, 0x56EB9F40,
//    };
//
//    public final void setState(final int s) {
//        stateA = startingA[s >>> 9 & 0x7F];
//        for (int i = s & 0x1FF; i > 0; i--) {
//            stateA = stateA - 0x9E3779B9 ^ 0xE541440F;
//            stateA = (stateA << 22 | stateA >>> 10);
//        }
//        stateB = startingB[s >>> 25];
//        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
//            stateB += 0xC0EF50EB;
//            stateB = (stateB << 7 | stateB >>> 25);
//        }
//    }
//
//    public final int nextInt()
//    {
////        int y = stateA + 0x9E3779B9;
////        y = (stateA = (y << 2 | y >>> 30));
////        int z = (stateB = stateB + 0xC3564E95 | 0);
////        z = (z ^ (z >>> 15) ^ y) * 0x6C8E9;
////        return z ^ ((z >>> 15) + (y ^ (y >>> 12)));
//        int y = stateA - 0x9E3779B9 ^ 0xE541440F;
//        y = (stateA = (y << 22 | y >>> 10));
//        final int x = stateB + 0xC0EF50EB;
//        y ^= (stateB = (x << 7 | x >>> 25));
//        y ^= y << 9;
//        return y ^ y >>> 13;
////        int x = stateB;
////        x ^= x << 5;
////        y += (stateB = (x << 15 | x >>> 17));
////        return ((y - (y << 3)) ^ (y + (y << 16)) ^ y);
////        int y = stateA + 0x9E3779B9;
////        y = (stateA = (y << 2 | y >>> 30)) ^ 0x632BE5AD;
////        int x = stateB + 0x6C8E9CF7;
////        x = (stateB = (x << 7 | x >>> 25)) ^ 0xC3564E95;
////        return (y - (y << 5)) - (x << 9 | x >>> 23) ^ (x + y >>> 13);
//
//
//                //(y ^ (y << 5)) + (x ^ (x << 8)) ^ (x + y >>> 12);
//                //(0x632BE5AD ^ x + (x << 8)) + (y - (0xC3564E95 ^ y << 9));
//    }
//    @Override
//    public final int next(final int bits)
//    {
////        final int a = stateA * 0x9E37 | 0;
////        stateA = (a << 17 | a >>> 15);
////        final int b = stateB * 0x4E6D | 0;
////        stateB = (b << 14 | b >>> 18);
////        return (stateA ^ stateB) >>> (32 - bits);
//        int y = stateA - 0x9E3779B9 ^ 0xE541440F;
//        y = (stateA = (y << 22 | y >>> 10));
//        final int x = stateB + 0xC0EF50EB;
//        y ^= (stateB = (x << 7 | x >>> 25));
//        y ^= y << 9;
//        return (y ^ y >> 13) >>> (32 - bits);
//    }
//    @Override
//    public final long nextLong()
//    {
////        int y = stateA + 0x9E3779B9;
////        y = (y << 2 | y >>> 30);
////        int z = stateB + 0xC3564E95;
////        z = (z ^ (z >>> 15) ^ y) * 0x6C8E9;
////        long t = z ^ ((z >>> 15) + (y ^ (y >>> 12)));
////        y = y + 0x9E3779B9;
////        stateA = y = (y << 2 | y >>> 30);
////        z = (stateB = stateB + 0x86AC9D2A | 0);
////        z = (z ^ (z >>> 15) ^ y) * 0x6C8E9;
////        return t << 32 ^ (z ^ ((z >>> 15) + (y ^ (y >>> 12))));
//
//        int y = stateA - 0x9E3779B9 ^ 0xE541440F;
//        y = (y << 22 | y >>> 10);
//        int x = stateB + 0xC0EF50EB;
//        int z = y ^ (x = (x << 7 | x >>> 25));
//        z ^= z << 9;
//        long t = z ^ z >>> 13;
//        y = y - 0x9E3779B9 ^ 0xE541440F;
//        stateA = (y = (y << 22 | y >>> 10));
//        x += 0xC0EF50EB;
//        z = y ^ (stateB = (x << 7 | x >>> 25));
//        z ^= z << 9;
//        return t << 32 ^ (z ^ z >>> 13);
////        int a = stateA * 0x9E37 | 0;
////        a = (a << 17 | a >>> 15);
////        int b = stateB * 0x4E6D | 0;
////        b = (b << 14 | b >>> 18);
////        long t = a ^ b;
////        final int aa = a * 0x9E37 | 0;
////        stateA = (aa << 17 | aa >>> 15);
////        final int bb = b * 0x4E6D | 0;
////        stateB = (bb << 14 | bb >>> 18);
////        t = t << 32 ^ (stateA ^ stateB);
////        return t;
//    }

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
