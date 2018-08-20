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
            0x00000001, 0xB6E16604, 0x84A91149, 0x67AC52C8, 0xBEF3BB8C, 0x23CE3E07, 0x6EDBB8A1, 0x274276DF,
            0xE66E7F42, 0x782250C3, 0x284F0D35, 0xB3903E44, 0x19A162D2, 0x7DCB5533, 0x31146CDF, 0x3666EE93,
            0x4CFFC6E9, 0xE9AF02E0, 0xC0D678CA, 0x82B174C0, 0xBF6F25BA, 0x6B37ADAB, 0xA8FE16E5, 0xB7B6C86B,
            0x3C6C3DC6, 0xA9CB1833, 0xEAA1A2D7, 0x5B12EB3D, 0xFB6229D1, 0x364EA5EF, 0x582F63D5, 0x0CB374F5,
            0x8B161A5E, 0xE759784A, 0x203788FA, 0xAD6791DB, 0xFE4E70F1, 0xB5E14DA3, 0x849610A5, 0xFFFB00FF,
            0x7D28539A, 0xD0B18B9E, 0x45F2E945, 0x9346659F, 0x02E0C263, 0xFA53EEFE, 0x366BC4B5, 0xBAA06D47,
            0x714EAB99, 0xA77278AE, 0xC7536981, 0xD2D5B2EF, 0x28C69EF7, 0x3B6B15DB, 0xAC81216F, 0xC6D50F66,
            0xBFCE5018, 0x43A36D98, 0x0C111B51, 0x4AE6EAA0, 0x960F00DE, 0x60143353, 0x61FEE9CD, 0x4B0CB168,
            0x91695609, 0x9C10C765, 0x29A4343B, 0xACABAB21, 0x4374163D, 0xC3BAA736, 0x918E3147, 0xC1DAB2FB,
            0x20355E4D, 0xA0D531FF, 0x516DF23D, 0x0F41D121, 0xAF38E8F6, 0xAE866375, 0x764AAA2B, 0xA63AE93A,
            0x35B9C0C5, 0x32DCDB6A, 0xA61561D0, 0x52518525, 0x7115E9B9, 0x27B34AD3, 0x8DBBB84F, 0x0F9AEF15,
            0x199EDEDD, 0xBA5A4993, 0x3CA7D786, 0xDD2C6E48, 0x90BEA6C9, 0xAA34E309, 0xE02FB459, 0x167FCA38,
            0xA8653EE4, 0x9FCF090E, 0xCB2B47F9, 0x9A3909E5, 0x75B0F986, 0x557B897B, 0x0873C70D, 0xCAF3824B,
            0x14F63600, 0xF00EF48B, 0x337CED22, 0xBE2A3E1D, 0x5939AF06, 0x72755544, 0xBEA17CD7, 0x0767E32B,
            0x05D4FB25, 0x560A74AB, 0x58332A3A, 0x309B106B, 0x0DDF9FEF, 0xBF20AB12, 0x4900C5B9, 0xBBF4A294,
            0xB9A4B25F, 0x0E0F882B, 0xB9AB5606, 0xE4DE86BC, 0xDD026D62, 0xEBB4B162, 0xE3CA2222, 0xEA5A01C5,
    }, startingB = {
            0x00000001, 0xA9767029, 0xC36D2FFF, 0x8BC8A46F, 0x3C586BE9, 0x654028F8, 0x3BC36ED8, 0xBCAD2EE5,
            0x12DD2D5C, 0x99D4A55B, 0xBA00D3C0, 0x4F85CF46, 0x2EDE74A5, 0xAD771823, 0x8ECC54D8, 0x2EB0955F,
            0x5C1BBA00, 0x9DB7CE62, 0x2A76D204, 0x6DC1C43E, 0xFF4E0E96, 0xE45728BB, 0x4103F12C, 0xD8E3D609,
            0x58F2587B, 0x4B169E3D, 0x00DDD04F, 0x3721B154, 0xACF777A2, 0x9715D29E, 0x46E45724, 0x299145D6,
            0x6FDD75D4, 0x572A304B, 0x17519541, 0xC59D50A6, 0x9BE5938D, 0x4AA90B25, 0x01626ACC, 0xDA9A024D,
            0x47F60DBD, 0x85BA1183, 0xF553788D, 0x1D642674, 0x50B506A0, 0x9EAC6A04, 0x3F7BFCB9, 0x5C32A24D,
            0x66B3A7AE, 0x1E0D0B7C, 0x3186148B, 0x0461A847, 0xE193E7FA, 0xB5CBB459, 0xFC7D8604, 0xF9F9C493,
            0xE038620A, 0xFFEDAC2F, 0x38FC87CD, 0x8A0E062B, 0x0EAA198F, 0xED6CBC65, 0x66A73D25, 0x5C3D77AD,
            0xCA32D8F4, 0x30D44109, 0x55AC56D7, 0x26784CD2, 0x8E95392F, 0x609DA1AB, 0xC01CBA2D, 0x36A594F5,
            0x65463BB0, 0xEC147FD9, 0xFCB3D73C, 0x13BC191E, 0x36E408C7, 0x550A1050, 0x118BCBCE, 0xF18CEFF0,
            0x781F50BC, 0xEB306A3F, 0x522147AC, 0x43EF7770, 0x48C7FD2B, 0x04965BE3, 0x557720E4, 0xBA355404,
            0x07884E1D, 0x85AB54AA, 0x1197FDFD, 0x635DEDE7, 0xBE49761B, 0xF7FA516C, 0xF1854433, 0x56133FE6,
            0x9F5F8EEF, 0x40B02A4F, 0x4E8B296F, 0xBC197E3B, 0xE896BA7F, 0x6BC0187A, 0x47FCADD1, 0xA594B585,
            0xA6517A0D, 0x45C47256, 0x8877ADD5, 0xAC8C32A7, 0x2376C425, 0x5427F940, 0xC3332A3E, 0xB3358CCA,
            0x8E6B3EDA, 0x33F7BF4F, 0x32A3294C, 0xE18EE95E, 0xAE8908E9, 0x79B012AB, 0xCEDEE0E9, 0xA3EB8638,
            0xA38C2A1A, 0xB60F4A3A, 0xFF670EC7, 0x8E1019AA, 0x9112227D, 0xDBC81559, 0x34CDFF4E, 0x5D357F7C,
    };

    public final void setState(final int s) {
        stateA = startingA[s >>> 9 & 0x7F];
        for (int i = s & 0x1FF; i > 0; i--) {
            stateA *= 0xACED;
            stateA = (stateA << 28 | stateA >>> 4);
        }
        stateB = startingB[s >>> 25];
        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
            stateB *= 0xBA55;
            stateB = (stateB << 19 | stateB >>> 13);
        }
    }

    public final int nextInt()
    {
        int y = stateA * 0xACED;
        y = (stateA = (y << 28 | y >>> 4));
        final int x = stateB * 0xBA55;
        return y ^ (stateB = (x << 19 | x >>> 13));
    }
    @Override
    public final int next(final int bits)
    {
        int y = stateA * 0xACED;
        y = (stateA = (y << 28 | y >>> 4));
        final int x = stateB * 0xBA55;
        return (y ^ (stateB = (x << 19 | x >>> 13))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        int y = stateA * 0xACED;
        y = (y << 28 | y >>> 4);
        int x = stateB * 0xBA55;
        long t = y ^ (x = (x << 19 | x >>> 13));
        y *= 0xACED;
        stateA = (y = (y << 28 | y >>> 4));
        x *= 0xBA55;
        return t << 32 ^ (y ^ (stateB = (x << 19 | x >>> 13)));
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
