package com.github.yellowstonegames.util;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Static utility methods and constants for basic math operations, and some not-so-basic ones.
 */
public final class MathSupport {

    /**
     * Cannot be instantiated.
     */
    private MathSupport(){}

    /**
     * The square root of 2, as a float. Useful as the length of a diagonal on a square with length-1 sides.
     */
    public static final float ROOT_2 = (float) Math.sqrt(2f);
    /**
     * 1 divided by the square root of 2, as a float. Scaling by this is useful to take a vector such as
     * {@code [1, 1]} and make it the same length as the vector {@code [1,0]} or {@code [0,-1]}.
     */
    public static final float INVERSE_ROOT_2 = 1f / ROOT_2;

    /**
     * Sets {@code changing} to the point at the given {@code index} into the "R2 Sequence", a sequence of points that
     * remain separated from each other while seeming random for many sequential indices. Scales the point so it fits
     * between 0,0 (inclusive) and the given width and height (exclusive).
     * <br>
     * <a href="https://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/">See this article</a>
     * for more on the R2 sequence, including both pretty pictures and serious math.
     * @param changing the GridPoint2 that will be modified
     * @param index the index into the R2 sequence, often a small positive number but can be any long
     * @param width the width of the area to potentially place {@code changing}
     * @param height the height of the area to potentially place {@code changing}
     * @return {@code changing}, after modifications
     */
    public static GridPoint2 fillR2(GridPoint2 changing, long index, int width, int height) {
        long ix = index * 0xC13FA9A902A6328FL;
        long iy = index * 0x91E10DA5C79E7B1DL;
        double x = (ix >>> 1) * (1.0842021724855043E-19 * width); //1.0842021724855043E-19 is just under pow(2, -63)
        double y = (iy >>> 1) * (1.0842021724855043E-19 * height); //1.0842021724855043E-19 is just under pow(2, -63)
        changing.set((int)x, (int)y);
        return changing;
    }
    /**
     * Reads in a CharSequence containing only decimal digits (only 0-9) with an optional sign at the start
     * and returns the long they represent, reading at most 19 characters (20 if there is a sign) and returning the
     * result if valid, or 0 if nothing could be read. The leading sign can be '+' or '-' if present. This can also
     * represent negative numbers as they are printed as unsigned longs. This means "18446744073709551615" would
     * return the long -1 when passed to this, though you could also simply use "-1" . If you use both '-' at the start
     * and have the number as greater than {@link Long#MAX_VALUE}, such as with "-18446744073709551615", then both
     * indicate a negative number, but the digits will be processed first (producing -1) and then the whole thing will
     * be multiplied by -1 to flip the sign again (returning 1).
     * <br>
     * Should be fairly close to Java 8's Long.parseUnsignedLong method, which is an odd omission from earlier JDKs.
     * This doesn't throw on invalid input, though, instead returning 0 if the first char is not a decimal digit, or
     * stopping the parse process early if a non-0-9 char is read before end is reached. If the parse is stopped
     * early, this behaves as you would expect for a number with fewer digits, and simply doesn't fill the larger places.
     * <br>
     * This code was taken from <a href="https://github.com/tommyettinger/digital">digital</a> and its Base class, with
     * any numbers for that class' variable radix hard-coded to 10 because that's all we need here. Though there is a
     * similar method in Java 8, it isn't available to RoboVM, and even then it's not nearly as tolerant of invalid
     * inputs as this method (this one parses what it can and returns what it can, rather than throwing an exception).
     *
     * @param cs    a CharSequence, such as a String, containing decimal digits with an optional sign
     * @param start the (inclusive) first character position in cs to read
     * @param end   the (exclusive) last character position in cs to read (this stops after 20 characters if end is too large)
     * @return the long that cs represents
     */
    public static long longFromDec(final CharSequence cs, final int start, int end) {
        int sign, h, lim;
        if (cs == null || start < 0 || end <= 0 || (end = Math.min(end, cs.length())) - start <= 0)
            return 0;
        char c = cs.charAt(start);
        if (c == '-') {
            sign = -1;
            h = 0;
            lim = 21;
        } else if (c == '+') {
            sign = 1;
            h = 0;
            lim = 21;
        } else {
            if (!(c >= '0' && c <= '9'))
                return 0;
            else {
                sign = 1;
                lim = 20;
            }
            h = (c - '0');
        }
        long data = h;
        for (int i = start + 1; i < end && i < start + lim; i++) {
            c = cs.charAt(i);
            if (!(c >= '0' && c <= '9'))
                return data * sign;
            data *= 10;
            data += (c - '0');
        }
        return data * sign;
    }


    /**
     * Reads in a CharSequence containing only decimal digits (only 0-9) with an optional sign at the start
     * and returns the int they represent, reading at most 10 characters (11 if there is a sign) and returning the
     * result if valid, or 0 if nothing could be read. The leading sign can be '+' or '-' if present. This can also
     * represent negative numbers as they are printed as unsigned integers. This means "4294967295" would return the int
     * -1 when passed to this, though you could also simply use "-1" . If you use both '-' at the start and have the
     * number as greater than {@link Integer#MAX_VALUE}, such as with "-4294967295", then both indicate a negative
     * number, but the digits will be processed first (producing -1) and then the whole thing will be multiplied by -1
     * to flip the sign again (returning 1).
     * <br>
     * Should be fairly close to Java 8's Integer.parseUnsignedInt method, which is an odd omission from earlier JDKs.
     * This doesn't throw on invalid input, though, instead returning 0 if the first char is not a decimal digit, or
     * stopping the parse process early if a non-0-9 char is read before end is reached. If the parse is stopped
     * early, this behaves as you would expect for a number with fewer digits, and simply doesn't fill the larger places.
     * <br>
     * This code was taken from <a href="https://github.com/tommyettinger/digital">digital</a> and its Base class, with
     * any numbers for that class' variable radix hard-coded to 10 because that's all we need here. Though there is a
     * similar method in Java 8, it isn't available to RoboVM, and even then it's not nearly as tolerant of invalid
     * inputs as this method (this one parses what it can and returns what it can, rather than throwing an exception).
     *
     * @param cs    a CharSequence, such as a String, containing decimal digits with an optional sign
     * @param start the (inclusive) first character position in cs to read
     * @param end   the (exclusive) last character position in cs to read (this stops after 10 characters if end is too large)
     * @return the int that cs represents
     */
    public static int intFromDec(final CharSequence cs, final int start, int end) {
        int sign, h, lim;
        if (cs == null || start < 0 || end <= 0 || (end = Math.min(end, cs.length())) - start <= 0)
            return 0;
        char c = cs.charAt(start);
        if (c == '-') {
            sign = -1;
            h = 0;
            lim = 11;
        } else if (c == '+') {
            sign = 1;
            h = 0;
            lim = 11;
        } else {
            if (!(c >= '0' && c <= '9'))
                return 0;
            else {
                sign = 1;
                lim = 10;
            }
            h = (c - '0');
        }
        int data = h;
        for (int i = start + 1; i < end && i < start + lim; i++) {
            c = cs.charAt(i);
            if (!(c >= '0' && c <= '9'))
                return data * sign;
            data *= 10;
            data += (c - '0');
        }
        return data * sign;
    }

    /**
     * Reads a float in from the String {@code str}, using only the range from {@code start} (inclusive) to {@code end}
     * (exclusive). This effectively returns {@code Float.parseFloat(str.substring(start, Math.min(str.length(), end)))}
     * . Unlike the other number-reading methods here, this doesn't do much to validate its input, so the end of the
     * String must be after the full float number. Because of how parseFloat() works, whitespace will be trimmed out of
     * the substring if present, including space and any ASCII control chars. If the parse fails, this returns 0f.
     * This can handle a wider variety of literals than one might expect; it tolerates a trailing {@code f} or even
     * {@code d} at the end, and in addition to the standard {@code 12.34} decimal notation, it can parse scientific
     * notation, {@code 1.234e+01}, as well as hexadecimal float notation, {@code 0x1.8ae148p3}.
     * @param str a String containing a valid float in the specified range
     * @param start the start index (inclusive) to read from
     * @param end the end index (exclusive) to stop reading before
     * @return the parsed float from the given range, or 0f if the parse failed.
     */
    public static float floatFromDec(final String str, final int start, int end) {
        try {
            return Float.parseFloat(str.substring(start, Math.min(str.length(), end)));
        } catch (NumberFormatException ignored) {
            return 0f;
        }
    }

}
