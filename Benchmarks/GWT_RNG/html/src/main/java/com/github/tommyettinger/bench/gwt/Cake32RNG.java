package com.github.tommyettinger.bench.gwt;

import squidpony.StringKit;
import squidpony.squidmath.RandomnessSource;

import java.io.Serializable;

/**
 * A variant of Mark Overton's subcycle generators from <a href="http://www.drdobbs.com/tools/229625477">this
 * article</a>, using three 32-bit states but no multiplication (just bitwise operations and addition). It has a known
 * minimum period of just under 2 to the 64, 0xFFFE870A6BECE609, which is roughly 2 to the 63.999968, and an unknown
 * exact period for its largest subcycle (the upper bound is less than 2 to the 96, and probably isn't much higher than
 * 2 to the 64).
 * <br>
 * This generator is still being evaluated, and can be considered "alpha" quality. There are fallbacks that can be used
 * with either more state or a smaller period that are known to pass PractRand in full; this is likely to pass all of
 * PractRand, but it isn't a sure bet that it will.
 * <br>
 * The name comes from how it was first implemented in this library on my birthday (Tommy Ettinger). Coincidentally, it
 * should pass birthday spacing tests, where several others in this library do not.
 * <br>
 * Created by Tommy Ettinger on 4/25/2019.
 * @author Mark Overton
 * @author Tommy Ettinger
 */
public final class Cake32RNG implements RandomnessSource, Serializable {
    private static final long serialVersionUID = 1L;
    private int stateA, stateB, stateC;
    public Cake32RNG()
    {
        setState((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    public Cake32RNG(final int state)
    {
        setState(state);
    }

    /**
     * Not advised for external use; prefer {@link #Cake32RNG(int)} because it guarantees a good subcycle. This
     * constructor allows all subcycles to be produced, including ones with a shorter period.
     * @param stateA an independent subcycle's state; some values will have shorter periods
     * @param stateB an independent subcycle's state; some values will have shorter periods
     * @param stateC the outer state; can have any value
     */
    public Cake32RNG(final int stateA, final int stateB, final int stateC)
    {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
    }

    private static final int[] startingA = {
            0x3F1B4D6F, 0xFE49EB30, 0xE1E9654D, 0x75269C79, 0xF049CFDC, 0x39AA2CA3, 0xAE2B4942, 0x2885ABA7,
            0x1E9F73B5, 0x70BB0038, 0x8520F7A1, 0x46DB32D1, 0x8EEC7226, 0x05E6D109, 0xA9642BB3, 0x1F06E452,
            0x8A797FEC, 0x3A3D2AD6, 0x52C4F8E5, 0xBF7DD4A5, 0x76509953, 0xE0859DC8, 0x45109542, 0x0BCC172D,
            0x5E0AC01B, 0xF95C78E6, 0x620B231B, 0xCBC65C2A, 0x1D0B48E3, 0x92C99C09, 0xCFE015E0, 0xAFA36608,
            0xFEB9C020, 0x330BC555, 0x1B08C181, 0xE82A4715, 0x2BC50EEA, 0xD2633538, 0xD47C6A77, 0x85819B68,
            0xF3CB4DAA, 0x70C0B372, 0x3E8A016C, 0x665AE62E, 0x8C9FAB4E, 0xB5CE5247, 0x48D1D463, 0xF6E69354,
            0x8C054216, 0xDEA535BE, 0x1643E031, 0x863CC48F, 0x42A919A2, 0x828493B0, 0xA6F36FBD, 0x6F0BEA32,
            0x4831522C, 0x41DA5987, 0x0A5CDED9, 0xA93CA28E, 0xEAAEF9F1, 0xB04F1E92, 0x156AC276, 0x6A7F1985,
            0x893D3145, 0x5E9CF016, 0xF63EA367, 0x4BDEA69B, 0xEBA5101F, 0x81852F21, 0x323888D0, 0xFA40CB32,
            0xFE263A04, 0x81284A55, 0x241A5E10, 0x95F0B8C0, 0x73189590, 0x9BDE64F6, 0xEBE4F77A, 0xA3D703AF,
            0x758B84A9, 0x1A11C9DB, 0x9011311B, 0x978464B0, 0x645DF3BA, 0x6E6C6530, 0xB9936F79, 0x370F8DB1,
            0xC25B3CA7, 0x769BE0EE, 0xFB384FCE, 0x24673C95, 0x5049F85E, 0xC8A47ED0, 0x88937012, 0x3C7A253C,
            0x70E6E593, 0x07919EA9, 0x7852B8AA, 0x5A96DBC3, 0xA25F5E9E, 0xB2714357, 0x2E099981, 0x084E5CAB,
            0x8104B545, 0xC4FB91DB, 0xF0D47528, 0xE90F1F60, 0x156CB1FB, 0x8CF95ED6, 0xC9646C64, 0x8F0B8D0C,
            0xEB78E573, 0x71C4403F, 0x1CEC03AA, 0x26728982, 0x38573D6E, 0x541A3469, 0x1ABE6703, 0x5FEC2775,
            0x08DFC2CE, 0x333BF049, 0x203501B2, 0xA2770FB0, 0x58C3C9C6, 0x8CD6D738, 0x4B20A409, 0x78E22799,
    }, startingB = {
            0x7034B30E, 0xB1525E58, 0x28FF626B, 0xA790D90E, 0xF7DB1EE8, 0xD298C3EF, 0x604E56FC, 0x38CC5F96,
            0x87E9A14F, 0x3705913B, 0x3DC5C99A, 0xC5D1F628, 0x5DD29D0C, 0x6099AE4E, 0x05E75D29, 0x36A51877,
            0x2A568BC7, 0x99DAC54C, 0x703CC922, 0x215BA2F3, 0x6888A66F, 0x9B67A5E1, 0x1EE0BBDA, 0xF26B5F4E,
            0x89EC3742, 0x1A41C01C, 0x26651A1E, 0x444AA7DD, 0xC6F9DC15, 0x01740583, 0x4FA0B78E, 0x7C9DBD96,
            0x9C6F33BC, 0x1473D830, 0xBE8663C5, 0x91EC74EF, 0xCD24372A, 0x7F70E336, 0x2B84BDA5, 0x8656B3C9,
            0x8BCDCFE9, 0x0BBCBC39, 0xEF7ED065, 0x4A1901A6, 0x2E28F041, 0xA245F0AE, 0x84A2A30A, 0x2D0F79F6,
            0xECFF4D92, 0xAF391247, 0x4C41D5D7, 0x92F0DA6C, 0x2822C92A, 0xA582E9D1, 0xDBE16511, 0x5D032CE8,
            0x77E9BEE0, 0x1E7DA7B9, 0x0DFC9420, 0x8CA37F71, 0xEAF6C3E2, 0x39FF7169, 0x3069C773, 0xE51D7D45,
            0x826E5417, 0xC6FB882C, 0xDCD86BC5, 0xC74B2E89, 0xBD710C1A, 0xB9BC73B4, 0x801F6CE3, 0xAEFA75F3,
            0x392B6057, 0x01CC6A3C, 0x49DEFACB, 0x4A4A7778, 0x008E49A2, 0x453250E2, 0x1D4F4572, 0x5B24E245,
            0x5198DE18, 0xA2B80723, 0xFB558DE0, 0x5DDAF154, 0xD0C90DF7, 0x1564F1D2, 0x9B75BBDF, 0x40B5F84A,
            0x8A11AB2F, 0x5416579A, 0x85BC24A5, 0x525B11CB, 0xD9D8A920, 0xEC811758, 0x1365AA6E, 0x18C5D873,
            0x60BF7A51, 0xFD67BC3C, 0x728411A3, 0x5F9A2902, 0x517EA59E, 0xECB0EDCF, 0x142871D0, 0x2F2E5DF0,
            0xDEB50840, 0x02C0D2C7, 0x90BD16EF, 0x6D64B3BC, 0x98770E6A, 0x0A5C1D8C, 0xC40955D2, 0xAE7BCAA3,
            0xD3F302D4, 0xC961F091, 0xC7168F58, 0xAEC9872E, 0xC0147D0D, 0x10DF4BCC, 0x5DB46D1A, 0xAE9092B6,
            0x61D502A1, 0xB0CCAE39, 0x6039A428, 0xD331E849, 0xE3852981, 0x1F2097E2, 0xF5DF6653, 0x7F3F242E,
    };

    public final void setState(final int s) {
        stateA = startingA[s >>> 9 & 0x7F];
        for (int i = s & 0x1FF; i > 0; i--) {
            stateA += 0xC4DE9951;
            stateA = (stateA << 7 | stateA >>> 25);
        }
        stateB = startingB[s >>> 25];
        for (int i = s >>> 16 & 0x1FF; i > 0; i--) {
            stateB += 0xAA78EDD7;
            stateB = (stateB << 1 | stateB >>> 31);
        }
        stateC = ~(s + stateA + stateB);
    }

    public final int nextInt()
    {
        int y = stateA + 0xC4DE9951;
        stateA = (y = (y << 7 | y >>> 25));
        final int x = stateB + 0xAA78EDD7;
        return (stateC ^= stateC << 5 ^ stateC >>> 11 ^ (y + (stateB = (x << 1 | x >>> 31))));
    }
    @Override
    public final int next(final int bits)
    {
        int y = stateA + 0xC4DE9951;
        stateA = (y = (y << 7 | y >>> 25));
        final int x = stateB + 0xAA78EDD7;
        return (stateC ^= stateC << 5 ^ stateC >>> 11 ^ (y + (stateB = (x << 1 | x >>> 31)))) >>> (32 - bits);
    }
    @Override
    public final long nextLong()
    {
        int y = stateA + 0xC4DE9951;
        y = (y << 7 | y >>> 25);
        int x = stateB + 0xAA78EDD7;
        final long t = (stateC ^= stateC << 5 ^ stateC >>> 11 ^ (y + (x = (x << 1 | x >>> 31)))) & 0xFFFFFFFFL;
        y += 0xC4DE9951;
        x += 0xAA78EDD7;
        return t << 32 | ((stateC ^= stateC << 5 ^ stateC >>> 11 ^
                ((stateA = (y << 7 | y >>> 25)) + (stateB = (x << 1 | x >>> 31)))) & 0xFFFFFFFFL);
    }

    /**
     * Produces a copy of this Mover32RNG that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this Mover32RNG
     */
    @Override
    public Cake32RNG copy() {
        return new Cake32RNG(stateA, stateB, stateC);
    }

    /**
     * Gets the "A" part of the state; if this generator was set with {@link #Cake32RNG()}, {@link #Cake32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be.
     * @return the "A" part of the state, an int
     */
    public int getStateA()
    {
        return stateA;
    }

    /**
     * Gets the "B" part of the state; if this generator was set with {@link #Cake32RNG()}, {@link #Cake32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be. 
     * @return the "B" part of the state, an int
     */
    public int getStateB()
    {
        return stateB;
    }

    /**
     * Gets the "C" part of the state; if this generator was set with {@link #Cake32RNG()}, {@link #Cake32RNG(int)},
     * or {@link #setState(int)}, then this will be on the optimal subcycle, otherwise it may not be. 
     * @return the "C" part of the state, an int
     */
    public int getStatec()
    {
        return stateC;
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

    /**
     * Sets the "C" part of the state to any int, which may put the generator in a low-period subcycle.
     * Use {@link #setState(int)} to guarantee a good subcycle.
     * @param stateC any int
     */
    public void setStateC(final int stateC)
    {
        this.stateC = stateC;
    }

    @Override
    public String toString() {
        return "Cake32RNG with stateA 0x" + StringKit.hex(stateA) + ", stateB 0x" + StringKit.hex(stateB) + ", and stateC 0x" + StringKit.hex(stateC);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cake32RNG cake32RNG = (Cake32RNG) o;

        return stateA == cake32RNG.stateA && stateB == cake32RNG.stateB && stateC == cake32RNG.stateC;
    }

    @Override
    public int hashCode() {
        return (961 * stateA + 31 * stateB + stateC) | 0;
    }


//    public final int nextIntA()
//    {
//        int y = stateA * 0x89A7;
//        stateA = (y = (y << 13 | y >>> 19));
//        final int x = stateB * 0xBCFD;
//        return (y ^ (stateB = (x << 17 | x >>> 15)));
//    }
//    // valid, but needs different startingA and startingB
//    public final int nextIntB()
//    {
//        int y = stateA;
//        stateA = (y = (y << 13 | y >>> 19) * 0x89A7);
//        final int x = stateB;
//        return (y ^ (stateB = (x << 17 | x >>> 15) * 0xBCFD));
//    }
//    // valid, but needs different startingA and startingB
//    public final int nextIntC()
//    {
//        return ((stateA = (stateA << 13 | stateA >>> 19) * 0x89A7)
//                ^ (stateB = (stateB << 17 | stateB >>> 15) * 0xBCFD));
//    }
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
//        int stateA = 0, stateB = 0;
//        System.out.println("int[] startingA = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            for (int s = 0; s < 0x200; s++) {
//                stateA += 0xC4DE9951;
//                stateA = (stateA << 7 | stateA >>> 25);
//            }
//            System.out.printf("0x%08X, ", stateA);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("}, startingB = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            for (int s = 0; s < 0x200; s++) {
//                stateB += 0xAA78EDD7;
//                stateB = (stateB << 1 | stateB >>> 31);
//            }
//            System.out.printf("0x%08X, ", stateB);
//            if((ctr & 7) == 7)
//                System.out.println();
//        }
//        System.out.println("};");
//    }
    
///////// BEGIN subcycle finder code and period evaluator
//    public static void main(String[] args)
//    {
//        // multiplying
//        // A refers to 0x9E377
//        // A 10 0xC010AEB4
//        // B refers to 0x64E6D
//        // B 22 0x195B9108
//        // all  0x04C194F3485D5A68
//
//        // A=Integer.rotateLeft(A*0x9E377, 17) 0xF7F87D28
//        // B=Integer.rotateLeft(A*0x64E6D, 14) 0xF023E25B 
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
//        // 0x9E37
//        // rotation 27: 0xEE06F34D
//        // 0x9E35
//        // rotation 6 : 0xE1183C3A
//        // rotation 19: 0xC4FCFC55
//        // 0x9E3B
//        // rotation 25: 0xE69313ED
//        // 0xDE4D
//        // rotation 3 : 0xF6C16607
//        // rotation 23: 0xD23AD58D
//        // rotation 29: 0xC56DC41F
//        // 0x1337
//        // rotation 7: 0xF41BD009
//        // rotation 20: 0xF5846878
//        // rotation 25: 0xF38658F9
//        // 0xACED
//        // rotation 28: 0xFC98CC08
//        // rotation 31: 0xFA18CD57
//        // 0xBA55
//        // rotation 19: 0xFB059E43
//        // 0xC6D5
//        // rotation 05: 0xFFD78FD4
//        // 0x5995
//        // rotation 28: 0xFF4AB87D
//        // rotation 02: 0xFF2AA5D5
//        // 0xA3A9
//        // rotation 09: 0xFF6B3AF7
//        // 0xB9EF
//        // rotation 23: 0xFFAEB037
//        // 0x3D29
//        // rotation 04: 0xFF6B92C5
//        // 0x5FAB
//        // rotation 09: 0xFF7E3277 // seems to be very composite
//        // 0xCB7F
//        // rotation 01: 0xFF7F28FE
//        // this range of 4: {
//        // 0x89A7
//        // rotation 13: 0xFFFDBF50 // wow! note that this is a multiple of 16
//        // 0xBCFD
//        // rotation 17: 0xFFF43787 // second-highest yet, also an odd number
//        // 0xA01B
//        // rotation 28: 0xFFEDA0B5
//        // 0xC2B9
//        // rotation 16: 0xFFEA9001
//        // } are all relatively coprime, total period is
//        // 0xFFCA2B600EECB96802194A31711490F0, or 2 to the 127.998814
//        // 
//        // a = rotate32(a, 13) * 0x89A7;
//        // b = rotate32(b, 17) * 0xBCFD;
//        // c = rotate32(c, 28) * 0xA01B;
//        // d = rotate32(d, 16) * 0xC2B9;
//  
//        // e = rotate32(e, 7) + 0xC0EF50EB;
//        
//        // adding
//        // 0x9E3779B9
//        // rotation 2 : 0xFFCC8933
//        // rotation 7 : 0xF715CEDF
//        // rotation 25: 0xF715CEDF
//        // rotation 30: 0xFFCC8933
//        // 0x6C8E9CF5
//        // rotation 6 : 0xF721971A
//        // 0x41C64E6D
//        // rotation 13: 0xFA312DBF
//        // rotation 19: 0xFA312DBF
//        // rotation 1 : 0xF945B8A7
//        // rotation 31: 0xF945B8A7
//        // 0xC3564E95
//        // rotation 1 : 0xFA69E895 also 31
//        // rotation 5 : 0xF2BF5E23 also 27
//        // 0x76BAF5E3
//        // rotation 14: 0xF4DDFC5A also 18
//        // 0xA67943A3 
//        // rotation 11: 0xF1044048 also 21
//        // 0x6C96FEE7
//        // rotation 2 : 0xF4098F0D
//        // 0xA3014337
//        // rotation 15: 0xF3700ABF also 17
//        // 0x9E3759B9
//        // rotation 1 : 0xFB6547A2 also 31
//        // 0x6C8E9CF7
//        // rotation 7 : 0xFF151D74 also 25
//        // rotation 13: 0xFD468E2B also 19
//        // rotation 6 : 0xF145A7EB also 26
//        // 0xB531A935
//        // rotation 13: 0xFF9E2F67 also 19
//        // 0xC0EF50EB
//        // rotation 07: 0xFFF8A98D also 25
//        // 0x518DC14F
//        // rotation 09: 0xFFABD755 also 23 // probably not prime
//        // 0xA5F152BF
//        // rotation 07: 0xFFB234B2 also 25
//        // 0x8092D909
//        // rotation 10: 0xFFA82F7C also 22
//        // 0x73E2CCAB
//        // rotation 09: 0xFF9DE8B1 also 23

//        // stateB = rotate32(stateB + 0xB531A935, 13)
//        // stateC = rotate32(stateC + 0xC0EF50EB, 7)

//        // 0xFFCC8933 (rotate 30, add 0x9E3779B9)
//        // 0xFFB234B2 (rotate 25, add 0xA5F152BF)
//        // 0xFFF8A98D (rotate 7 , add 0xC0EF50EB)

//        // subtracting, rotating, and bitwise NOT:
//        // 0xC68E9CF3
//        // rotation 13: 0xFEF97E17, also 19 
//        // 0xC68E9CB7
//        // rotation 12: 0xFE3D7A2E
//
//        // left xorshift
//        // 5
//        // rotation 15: 0xFFF7E000
//        // 13
//        // rotation 17: 0xFFFD8000
//
//        // minus left shift, then xor
//        // state - (state << 12) ^ 0xC68E9CB7, rotation 21: 0xFFD299CB
//        // add xor
//        // state + 0xC68E9CB7 ^ 0xDFF4ECB9, rotation 30: 0xFFDAEDF7
//        // state + 0xC68E9CB7 ^ 0xB5402ED7, rotation 01: 0xFFE73631
//        // state + 0xC68E9CB7 ^ 0xB2B386E5, rotation 24: 0xFFE29F5D
//        // sub xor
//        // state - 0x9E3779B9 ^ 0xE541440F, rotation 22: 0xFFFC9E3E
//
//
//        // best power of two:
//        // can get 63.999691 with: (period is 0xFFF1F6F18B2A1330)
//        // multiplying A by 0x89A7 and rotating left by 13
//        // multiplying B by 0xBCFD and rotating left by 17
//        // can get 63.998159 with: (period is 0xFFAC703E2B6B1A30)
//        // multiplying A by 0x89A7 and rotating left by 13
//        // multiplying B by 0xB9EF and rotating left by 23
//        // can get 63.998 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // xorshifting B left by 5 (B ^ B << 5) and rotating left by 15
//        // can get 63.99 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // adding 0x6C8E9CF7 for B and rotating left by 7
//        // can get 63.98 with:
//        // adding 0x9E3779B9 for A and rotating left by 2
//        // multiplying by 0xACED, NOTing, and rotating left by 28 for B
//        // 0xFF6B3AF7L 0xFFAEB037L 0xFFD78FD4L
//        
//        // 0xFF42E24AF92DCD8C, 63.995831
//        //BigInteger result = BigInteger.valueOf(0xFF6B3AF7L), tmp = BigInteger.valueOf(0xFFD78FD4L);
//
//        BigInteger result = BigInteger.valueOf(0xFFFDBF50L), tmp = BigInteger.valueOf(0xFFF43787L);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        tmp = BigInteger.valueOf(0xFFEDA0B5L);
//        result = tmp.divide(result.gcd(tmp)).multiply(result);
//        System.out.printf("\n0x%s, %2.6f\n", result.toString(16).toUpperCase(), Math.log(result.doubleValue()) / Math.log(2));
////        tmp = BigInteger.valueOf(0xFFABD755L);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("\n0x%s, %2.6f\n", result.toString(16).toUpperCase(), Math.log(result.doubleValue()) / Math.log(2));
//        int stateA = 1, i;
//        LinnormRNG lin = new LinnormRNG();
//        System.out.println(lin.getState());
//        Random rand = new RNG(lin).asRandom();
//        for (int c = 1; c <= 200; c++) {
//            //final int r = (Light32RNG.determine(20007 + c) & 0xFFFF)|1;
//            final int r = BigInteger.probablePrime(20, rand).intValue();
//            //System.out.printf("(x ^ x << %d) + 0xC68E9CB7\n", c);
//            System.out.printf("%03d/200, testing r = 0x%08X\n", c, r);
//            for (int j = 1; j < 32; j++) {
//                i = 0;
//                for (; ; i++) {
//                    if ((stateA = Integer.rotateLeft(stateA * r, j)) == 1) {
//                        if (i >>> 24 == 0xFF)
//                            System.out.printf("(state * 0x%08X, rotation %02d: 0x%08X\n", r, j, i);
//                        break;
//                    }
//                }
//            }
//        }
//
////        int stateA = 1, i = 0;
////        for (; ; i++) {
////            if((stateA = Integer.rotateLeft(~(stateA * 0x9E37), 7)) == 1)
////            {
////                System.out.printf("0x%08X\n", i);
////                break;
////            }
////        }
////        BigInteger result = BigInteger.valueOf(i & 0xFFFFFFFFL);
////        i = 0;
////        for (; ; i++) {
////            if((stateA = Integer.rotateLeft(~(stateA * 0x4E6D), 17)) == 1)
////            {
////                System.out.printf("0x%08X\n", i);
////                break;
////            }
////        }         
////        BigInteger tmp = BigInteger.valueOf(i & 0xFFFFFFFFL);
////        result = tmp.divide(result.gcd(tmp)).multiply(result);
////        System.out.printf("\n0x%016X\n", result.longValue());
//
//    }
///////// END subcycle finder code and period evaluator
    
    
//    public static void main(String[] args)
//    {
//        int stateA = 1, stateB = 1;
//        System.out.println("int[] startingA = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            System.out.printf("0x%08X, ", stateA);
//            if((ctr & 7) == 7)
//                System.out.println();
//            for (int i = 0; i < 512; i++) {
//                stateA *= 0x89A7;
//                stateA = (stateA << 13 | stateA >>> 19);
//            }
//        }
//        System.out.println("}, startingB = {");
//        for (int ctr = 0; ctr < 128; ctr++) {
//            System.out.printf("0x%08X, ", stateB);
//            if((ctr & 7) == 7)
//                System.out.println();
//            for (int i = 0; i < 512; i++) {
//                stateB *= 0xBCFD;
//                stateB = (stateB << 17 | stateB >>> 15);
//            }
//        }
//        System.out.println("};");
//    }
}
