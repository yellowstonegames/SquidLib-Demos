package com.squidpony;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.NumberUtils;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Data that can be used to limit the colors present in a Pixmap or other image, here with the goal of using 256 or less
 * colors in the image (for saving indexed-mode images).
 * <p>
 * Created by Tommy Ettinger on 6/23/2018.
 */
public class PaletteReducer {

    public interface ColorMetric{
        double difference(final int color1, int color2);
        double difference(final int color1, int r2, int g2, int b2);
        double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2);
    }
    public static class BasicColorMetric implements ColorMetric{
        /**
         * Color difference metric; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param color1 an RGBA8888 color as an int
         * @param color2 an RGBA8888 color as an int
         * @return the difference between the given colors, as a positive double
         */
        public double difference(final int color1, final int color2) {
            // if one color is transparent and the other isn't, then this is max-different
            if(((color1 ^ color2) & 0x80) == 0x80) return Double.POSITIVE_INFINITY;
            final int r1 = (color1 >>> 24), g1 = (color1 >>> 16 & 0xFF), b1 = (color1 >>> 8 & 0xFF),
                    r2 = (color2 >>> 24), g2 = (color2 >>> 16 & 0xFF), b2 = (color2 >>> 8 & 0xFF),
                    rmean = r1 + r2,
                    r = r1 - r2,
                    g = g1 - g2,
                    b = b1 - b2,
                    y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
            return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
        }
        /**
         * Color difference metric; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param color1 an RGBA8888 color as an int
         * @param r2     red value from 0 to 255, inclusive
         * @param g2     green value from 0 to 255, inclusive
         * @param b2     blue value from 0 to 255, inclusive
         * @return the difference between the given colors, as a positive double
         */
        public double difference(final int color1, int r2, int g2, int b2) {
            if((color1 & 0x80) == 0) return Double.POSITIVE_INFINITY; // if a transparent color is being compared, it is always different
            final int
                    r1 = (color1 >>> 24),
                    g1 = (color1 >>> 16 & 0xFF),
                    b1 = (color1 >>> 8 & 0xFF),
                    rmean = (r1 + r2),
                    r = r1 - r2,
                    g = g1 - g2,
                    b = b1 - b2,
                    y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
            return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
        }
        /**
         * Color difference metric; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param r1 red value from 0 to 255, inclusive
         * @param g1 green value from 0 to 255, inclusive
         * @param b1 blue value from 0 to 255, inclusive
         * @param r2 red value from 0 to 255, inclusive
         * @param g2 green value from 0 to 255, inclusive
         * @param b2 blue value from 0 to 255, inclusive
         * @return the difference between the given colors, as a positive double
         */
        public double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
            final int rmean = (r1 + r2),
                    r = r1 - r2,
                    g = g1 - g2 << 1,
                    b = b1 - b2,
                    y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
            return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
        }
    }

    public static class LABEuclideanColorMetric implements ColorMetric {
        /**
         * Color difference metric (squared) using L*A*B color space; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param rgba1 an RGBA8888 color as an int
         * @param rgba2 an RGBA8888 color as an int
         * @return the difference between the given colors, as a positive double
         */
        @Override
        public double difference(final int rgba1, final int rgba2)
        {
            if(((rgba1 ^ rgba2) & 0x80) == 0x80) return Double.POSITIVE_INFINITY;
            double x, y, z, r, g, b;

            r = (rgba1 >>> 24) / 255.0;
            g = (rgba1 >>> 16 & 0xFF) / 255.0;
            b = (rgba1 >>> 8 & 0xFF) / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            double L = (116.0 * y) - 16.0;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = (rgba2 >>> 24) / 255.0;
            g = (rgba2 >>> 16 & 0xFF) / 255.0;
            b = (rgba2 >>> 8 & 0xFF) / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            L -= 116.0 * y - 16.0;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            return L * L * 190.0 + A * A * 25.0 + B * B * 10.0;
        }
        @Override
        public double difference(final int rgba1, final int r2, final int g2, final int b2)
        {
            if((rgba1 & 0x80) == 0) return Double.POSITIVE_INFINITY;
            double x, y, z, r, g, b;

            r = (rgba1 >>> 24) / 255.0;
            g = (rgba1 >>> 16 & 0xFF) / 255.0;
            b = (rgba1 >>> 8 & 0xFF) / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            double L = (116.0 * y) - 16.0;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = r2 / 255.0;
            g = g2 / 255.0;
            b = b2 / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            L -= 116.0 * y - 16.0;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            return L * L * 190.0 + A * A * 25.0 + B * B * 10.0;
        }
        @Override
        public double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
            double x, y, z, r, g, b;

            r = r1 / 255.0;
            g = g1 / 255.0;
            b = b1 / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            double L = (116.0 * y) - 16.0;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = r2 / 255.0;
            g = g2 / 255.0;
            b = b2 / 255.0;

            r = ((r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92);
            g = ((g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92);
            b = ((b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.950489; // 0.96422;
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.000000; // 1.00000;
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.088840; // 0.82521;

            x = (x > 0.008856) ? Math.cbrt(x) : (7.787037037037037 * x) + 0.13793103448275862;
            y = (y > 0.008856) ? Math.cbrt(y) : (7.787037037037037 * y) + 0.13793103448275862;
            z = (z > 0.008856) ? Math.cbrt(z) : (7.787037037037037 * z) + 0.13793103448275862;

            L -= 116.0 * y - 16.0;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            //return L * L * 190 + A * A * 25 + B * B * 10;
            return L * L * 190.0 + A * A * 25.0 + B * B * 10.0;
        }

    }

    public static class LABRoughColorMetric implements ColorMetric {
        /**
         * Color difference metric (squared) using L*A*B color space; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param rgba1 an RGBA8888 color as an int
         * @param rgba2 an RGBA8888 color as an int
         * @return the difference between the given colors, as a positive double
         */
        @Override
        public double difference(final int rgba1, final int rgba2)
        {
            if(((rgba1 ^ rgba2) & 0x80) == 0x80) return Double.POSITIVE_INFINITY;
            double x, y, z, r, g, b;

            r = (rgba1 >>> 24) / 255.0;
            g = (rgba1 >>> 16 & 0xFF) / 255.0;
            b = (rgba1 >>> 8 & 0xFF) / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            double L = 100.0 * y;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = (rgba2 >>> 24) / 255.0;
            g = (rgba2 >>> 16 & 0xFF) / 255.0;
            b = (rgba2 >>> 8 & 0xFF) / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            L -= 100.0 * y;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            return L * L * 350.0 + A * A * 25.0 + B * B * 10.0;
        }
        @Override
        public double difference(final int rgba1, final int r2, final int g2, final int b2)
        {
            if((rgba1 & 0x80) == 0) return Double.POSITIVE_INFINITY;
            double x, y, z, r, g, b;

            r = (rgba1 >>> 24) / 255.0;
            g = (rgba1 >>> 16 & 0xFF) / 255.0;
            b = (rgba1 >>> 8 & 0xFF) / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            double L = 100 * y;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = r2 / 255.0;
            g = g2 / 255.0;
            b = b2 / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            L -= 100.0 * y;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            return L * L * 350.0 + A * A * 25.0 + B * B * 10.0;
        }
        @Override
        public double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
            double x, y, z, r, g, b;

            r = r1 / 255.0;
            g = g1 / 255.0;
            b = b1 / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            double L = 100 * y;
            double A = 500.0 * (x - y);
            double B = 200.0 * (y - z);

            r = r2 / 255.0;
            g = g2 / 255.0;
            b = b2 / 255.0;

            r = Math.pow((r + 0.055) / 1.055, 2.4);
            g = Math.pow((g + 0.055) / 1.055, 2.4);
            b = Math.pow((b + 0.055) / 1.055, 2.4);

            x = (r * 0.4124 + g * 0.3576 + b * 0.1805);
            y = (r * 0.2126 + g * 0.7152 + b * 0.0722);
            z = (r * 0.0193 + g * 0.1192 + b * 0.9505);

            x = Math.sqrt(x);
            y = Math.cbrt(y);
            z = Math.sqrt(z);

            L -= 100.0 * y;
            A -= 500.0 * (x - y);
            B -= 200.0 * (y - z);

            return L * L * 350.0 + A * A * 25.0 + B * B * 10.0;
        }

    }


    public static class YCwCmColorMetric implements ColorMetric {
        public static final double[] yLUT = new double[2041], cLUT = new double[511];
        static {
            for (int i = 1; i < 2041; i++) {
                yLUT[i] = Math.cbrt(i / 2041.0) * 1163.73;
            }
            for (int i = 1; i < 256; i++) {
                cLUT[255 - i] = -(cLUT[255 + i] = Math.pow(i / 257.0, 0.625) * 179.293);
            }
        }
        /**
         * Color difference metric (squared) using YCwCm color space with cube-root Y; returns large numbers even for smallish differences.
         * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
         *
         * @param rgba1 an RGBA8888 color as an int
         * @param rgba2 an RGBA8888 color as an int
         * @return the difference between the given colors, as a positive double
         */
        @Override
        public double difference(final int rgba1, final int rgba2)
        {
            if(((rgba1 ^ rgba2) & 0x80) == 0x80) return Double.POSITIVE_INFINITY;
            final int r1 = (rgba1 >>> 24);
            final int g1 = (rgba1 >>> 16 & 0xFF);
            final int b1 = (rgba1 >>> 8 & 0xFF);
            final int r2 = (rgba2 >>> 24);
            final int g2 = (rgba2 >>> 16 & 0xFF);
            final int b2 = (rgba2 >>> 8 & 0xFF);
            
            final double y = yLUT[r1 * 3 + g1 * 4 + b1] - yLUT[r2 * 3 + g2 * 4 + b2];
            final double cw = (cLUT[255 + r1 - b1] - cLUT[255 + r2 - b2]) * 1.5;
            final double cm = cLUT[255 + g1 - b1] - cLUT[255 + g2 - b2];
            return y * y + cw * cw + cm * cm;
            
        }
        @Override
        public double difference(final int rgba1, final int r2, final int g2, final int b2)
        {
            if((rgba1 & 0x80) == 0) return Double.POSITIVE_INFINITY;
            final int r1 = (rgba1 >>> 24);
            final int g1 = (rgba1 >>> 16 & 0xFF);
            final int b1 = (rgba1 >>> 8 & 0xFF);

            final double y = yLUT[r1 * 3 + g1 * 4 + b1] - yLUT[r2 * 3 + g2 * 4 + b2];
            final double cw = (cLUT[255 + r1 - b1] - cLUT[255 + r2 - b2]) * 1.5;
            final double cm = cLUT[255 + g1 - b1] - cLUT[255 + g2 - b2];
            return y * y + cw * cw + cm * cm;
        }
        @Override
        public double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
            final double y = yLUT[r1 * 3 + g1 * 4 + b1] - yLUT[r2 * 3 + g2 * 4 + b2];
            final double cw = (cLUT[255 + r1 - b1] - cLUT[255 + r2 - b2]) * 1.5;
            final double cm = cLUT[255 + g1 - b1] - cLUT[255 + g2 - b2];
            return y * y + cw * cw + cm * cm;
        }

    }

    public static final BasicColorMetric basicMetric = new BasicColorMetric(); // has no state, should be fine static
    public static final LABEuclideanColorMetric labMetric = new LABEuclideanColorMetric();
    public static final LABRoughColorMetric labRoughMetric = new LABRoughColorMetric();
    public static final YCwCmColorMetric ycwcmMetric = new YCwCmColorMetric();
    public byte[] paletteMapping;
    public final int[] paletteArray = new int[256];
    ByteArray curErrorRedBytes, nextErrorRedBytes, curErrorGreenBytes, nextErrorGreenBytes, curErrorBlueBytes, nextErrorBlueBytes;
    float ditherStrength = 0.5f, halfDitherStrength = 0.25f;

    /**
     * DawnBringer's Aurora palette run through Lloyd relaxation to increase the difference between colors.
     * Aurora is available in <a href="http://pixeljoint.com/forum/forum_posts.asp?TID=26080&KW=">this set of tools</a>
     * for a pixel art editor, but it is usable for lots of high-color purposes. This modifies Aurora to include one
     * fully-transparent color and to increase the difference between colors.
     */
    public static final int[] AURORA_RELAXED = new int[]{
            0x00000000, 0x000010ff, 0x101008ff, 0x421839ff, 0x313931ff, 0x4a4a5aff, 0x6b525aff, 0x736b6bff,
            0x9c738cff, 0x94948cff, 0xa5a5a5ff, 0xb5b5b5ff, 0xadcec6ff, 0xced6c6ff, 0xe7f7e7ff, 0xf7f7f7ff,
            0x087b7bff, 0x39c6c6ff, 0x18e7efff, 0xa5f7efff, 0x7b8cefff, 0x0808deff, 0x4242bdff, 0x08084aff,
            0x100863ff, 0x841084ff, 0xde29ceff, 0xe710bdff, 0xe78cefff, 0xf7b5c6ff, 0xf77b8cff, 0xef0829ff,
            0xde314aff, 0x840818ff, 0x5a0818ff, 0x8c3910ff, 0xbd8439ff, 0xf78408ff, 0xe7ce73ff, 0xeff794ff,
            0xf7f710ff, 0xc6b542ff, 0x847b10ff, 0x087b21ff, 0x39ce42ff, 0x10e731ff, 0xb5f7b5ff, 0xbdadc6ff,
            0xc6ad8cff, 0x9cad84ff, 0x7b9494ff, 0x737b8cff, 0x737352ff, 0xa55a4aff, 0xbd736bff, 0xde7b63ff,
            0xe7946bff, 0xe7a58cff, 0xefbd94ff, 0xf7d6adff, 0xefe7d6ff, 0x7b294aff, 0x734239ff, 0xa54a63ff,
            0xbd6373ff, 0xce8c8cff, 0xdeadadff, 0xf7ced6ff, 0xdec6adff, 0xb59c6bff, 0x947342ff, 0x735231ff,
            0x423121ff, 0x315210ff, 0x5a8c21ff, 0x849c4aff, 0x9ca542ff, 0xadbd63ff, 0xb5de7bff, 0xceef94ff,
            0xe7f7c6ff, 0xb5efa5ff, 0xa5ce8cff, 0x6bde39ff, 0x63ad4aff, 0x429429ff, 0x425231ff, 0x182108ff,
            0x185a31ff, 0x217321ff, 0x4a6b4aff, 0x218c31ff, 0x529452ff, 0x6bb56bff, 0x52c67bff, 0x63e763ff,
            0x94e79cff, 0xdef7efff, 0xb5f7ceff, 0x7befa5ff, 0x84bd94ff, 0x4a8c63ff, 0x106b4aff, 0x083918ff,
            0x213952ff, 0x397373ff, 0x52ad9cff, 0x73cebdff, 0x94efd6ff, 0xc6efefff, 0xbdd6efff, 0x9ccedeff,
            0xadb5d6ff, 0x8cadc6ff, 0x399cc6ff, 0x4a7b94ff, 0x315a73ff, 0x101029ff, 0x182139ff, 0x39395aff,
            0x4a3994ff, 0x5a529cff, 0x7373adff, 0x7b73ceff, 0x9484c6ff, 0xada5e7ff, 0xcedef7ff, 0xdedef7ff,
            0xad84c6ff, 0x9c52ceff, 0x7b5294ff, 0x73318cff, 0x4a1852ff, 0x4a314aff, 0x843173ff, 0x9c4a8cff,
            0xad52adff, 0xb573b5ff, 0xf78ce7ff, 0xf7d6efff, 0xdec6deff, 0xe7b5d6ff, 0xde9cbdff, 0xce7bbdff,
            0xce739cff, 0xc6429cff, 0x7b1063ff, 0x420829ff, 0x310818ff, 0x421008ff, 0x631808ff, 0xa51810ff,
            0xd62110ff, 0xd65242ff, 0xef4210ff, 0xef6b39ff, 0xef5263ff, 0xd6ce18ff, 0xefa542ff, 0xde9c18ff,
            0xde6b10ff, 0xbd5a10ff, 0xa54a18ff, 0x6b3110ff, 0x525210ff, 0x636b10ff, 0x8c8463ff, 0xa59410ff,
            0xadb510ff, 0xe7e763ff, 0xe7de10ff, 0xefef42ff, 0xc6ef39ff, 0x9cef4aff, 0x94e718ff, 0x6bd610ff,
            0x63ad10ff, 0x397310ff, 0x293108ff, 0x214a08ff, 0x105a10ff, 0x219410ff, 0x18c610ff, 0x31de18ff,
            0x7bef63ff, 0x42ef5aff, 0x10bd29ff, 0x18b55aff, 0x188c4aff, 0x084231ff, 0x189c84ff, 0x18bd94ff,
            0x18de6bff, 0x29e7adff, 0x4aef94ff, 0x5af7ceff, 0x8ceff7ff, 0x4ae7f7ff, 0x7bd6e7ff, 0x10e7ceff,
            0x189cdeff, 0x105273ff, 0x18295aff, 0x21298cff, 0x1042adff, 0x21739cff, 0x2131e7ff, 0x106bbdff,
            0x217bceff, 0x6ba5c6ff, 0x42adefff, 0x84adefff, 0x52c6efff, 0xadbdf7ff, 0x10bdefff, 0x1073efff,
            0x427bc6ff, 0x6b6befff, 0x426bf7ff, 0x6342e7ff, 0x3142efff, 0x1818deff, 0x0808a5ff, 0x21109cff,
            0x181863ff, 0x4a18adff, 0x5218d6ff, 0x8c29deff, 0x944aefff, 0x7310e7ff, 0xb563efff, 0xb58cf7ff,
            0xd69cf7ff, 0xd6bdefff, 0xefbdf7ff, 0xde6befff, 0xef52e7ff, 0xd631e7ff, 0xbd21efff, 0xb518bdff,
            0x8c18bdff, 0x5a188cff, 0x631063ff, 0x42086bff, 0x310842ff, 0x6b1042ff, 0xad1094ff, 0xc6107bff,
            0xef42bdff, 0xf76bbdff, 0xf794b5ff, 0xef4284ff, 0xe7187bff, 0xc61039ff, 0x9c3152ff, 0x9c1039ff,
    };

    /**
     * This stores a preload code for a PaletteReducer using DB Aurora with {@link #labRoughMetric}. Using
     * a preload code in the constructor {@link #PaletteReducer(int[], String)} eliminates the time needed to fill 32 KB
     * of palette mapping in a somewhat-intricate way that only gets more intricate with better metrics, and replaces it
     * with a straightforward load from a String into a 32KB byte array.
     * <br>
     * Earlier versions of this constant used {@link #basicMetric}, but dithering gets much smoother using
     * {@link #labRoughMetric} instead.
     */
    public static final String ENCODED_AURORA =
            "\001\001\001\001\001\001\001\027\027\027\027\027\030\030\030\030\030ßÞÞÞÞÞÞ\025\025\025\025\025\025\025\025\001\001\001\001uu\027\027\027\027\027\030\030\030\030\030óßßÞÞÞÞÞ\025\025\025\025\025\025\025\025\002\002\002\002uuuu\027\027\027\030\030\030\030óóßßßÞÞÞÞ\025\025\025\025\025ÝÝÝ\002\002\002\002uuuuôôàààààóóßßßßßÞááÝÝÝÝÝÝå"+
            "WWWWWvvvvvvààààóËËËßßááááÝÝÝÝÝÎåWWWWWWvvvÊÊÊÊÊÊËËËËËËááááÝÝÝÎÎÎÎ²²²²²²²²hhÊÊÊÊÊËËËËËËááááÎÎÎÎÎÎÎggggggg\004\004hhhhwwwËËxxÌÌÌÌ\026\026ÎÎÎÎÎÜ"+
            "³½½½½½½½½½hhhwwwwxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³½½½½½ÉÉÉÉÉÉÉÌÌÌÌÌ\026\026\026\026ÜÜÜÜÜ´´IIIIXXXVVÉÉÉÉÉÉÉÉyyyy\026\026\026\026\026ÜÜÜÜ´´´´´XXXXXXXtttttttyyyyyy\026\026\026ÚÚÚÛ"+
            "´´´´´´XXfffffttttttÍÏÏÏÏÏÏÏ××ÚÚÚYYYYYYfffffffiiiiÍÍÍÍÏÏÏÏÏÏ×××ÚÚYYYYYYYYYffff\020iiiÍÍÍÍÍÏÏÏÐÐ××××Ú++++++++++Y\020\020\020\020\020\020\020sÍÍÍÍÐÐÐÐÐ××××"+
            "+++++[[[¼¼¼¼¼\020\020\020\020\020\020ssssØØÐÐÐÐ×××µ[[[[[[[¼¼¼¼¼eeeee\020ssssrrØÈÈÈÈ\024\024µµµµµµµµ[¼¼¼¼e¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024µµµµµµµµUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈÈ"+
            "µµµµµµµµ»»»»»¾¾¾¾¾¾¾jjjrrrrÈÈÒÒÒººººº»»»»»»»»»»»¿¿jjjjjjrrÑÑÒÒÒÒºººººººº»»»»»»»»¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶¶,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÖÖÔÔ¶¶¶¶¶¶,,,,,,,,^^^^^^^¿\021\021\021\021\021\021ÔÔÔÔ·······,,,,,ÀÀÀÀÀ^^^ÁÁÁÇ\021\021\021\021\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022\022Å"+
            "------------ÀÀÀÀÀÂÂÁÁÁÁÁÇÇÇÇ\022\022\022Å----------¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022\022Å-------¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÁÃÃÃÃÃ\022\022Å-¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "\001\001\001\001\001\001\027\027\027\027\027\027\030\030\030\030\030ßÞÞÞÞÞÞ\025\025\025\025\025\025\025\025\002\002\002\002uu\027\027\027\027\027\030\030\030\030\030óßßÞÞÞÞÞ\025\025\025\025\025\025\025Ý\002\002\002\002uuuu\027\027ô\030\030\030\030óóßßßÞÞÞá\025\025\025\025\025ÝÝÝ\002\002\002\002uuuuôôàààààóóßßßßßáááÝÝÝÝÝÝå"+
            "WWWWWvvvvvvààààóËËËßßááááÝÝÝÝÝÎåWWWWWWvvvÊÊÊÊÊÊËËËËËËááááÝÝÝÎÎÎÎ²²²²²²²²hhÊÊÊÊÊËËËËËËááááÎÎÎÎÎÎÎggggggg\004\004hhhhwwwËËxxÌÌÌ\026\026\026ÎÎÎÎÜÜ"+
            "³½½½½½½½½½hhhwwwwxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³½½½½½ÉÉÉÉÉÉÉÌÌÌÌÌ\026\026\026\026ÜÜÜÜÜ´IIIIIXXXVVÉÉÉÉÉÉÉÉyyyy\026\026\026\026\026ÜÜÜÜ´´´´´XXXXXXXtttttttyyyyyy\026\026\026ÚÚÚÛ"+
            "´´´´´´XffffffttttttÍÏÏÏÏÏÏÏ××ÚÚÚYYYYYYfffffffiiiiÍÍÍÍÏÏÏÏÏÏ×××ÚÚYYYYYYYYYffff\020iiiÍÍÍÍÍÏÏÏÐÐ××××Ú++++++++++Y\020\020\020\020\020\020\020sÍÍÍÍÐÐÐÐÐ××××"+
            "+++[[[[[¼¼¼¼¼\020\020\020\020\020\020ssssØØÐÐÐÐ×××µ[[[[[[[¼¼¼¼¼eeeee\020ssssrrØÈÈÈÈ\024\024µµµµµµµ[[¼¼¼¼e¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024µµµµµµµµUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "µµµµµµµµ»»»»»¾¾¾¾¾¾¾jjjrrrrÈÈÒÒÒººººº»»»»»»»»»»»¿¿jjjjjjrrÑÑÒÒÒÒºººººººº»»»»»»»»¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶¶,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÖÖÔÔ¶¶¶¶¶¶,,,,,,,,^^^^^^^¿\021\021\021\021\021\021ÔÔÔÔ·······,,,,,ÀÀÀÀÀ^^^ÁÁÁÇ\021\021\021\021\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022\022Å"+
            "------------ÀÀÀÀÀÂÂÁÁÁÁÁÇÇÇÇ\022\022\022Å----------¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022\022Å-------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÁÃÃÃÃÃ\022\022Å¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "\001\001\001\001\001\001\027\027\027\027\027\030\030\030\030\030óßÞÞÞÞÞÞ\025\025\025\025\025\025\025\025\002\002\002uuuu\027\027\027\027\030\030\030\030\030óßßÞÞÞÞÞ\025\025\025\025\025\025\025Ý\002\002\002\002uuuu\027ôô\030\030\030àóóßßßÞÞÞá\025\025\025\025ÝÝÝå\002\002\002\002uuuuôôàààààóóßßßßßáááÝÝÝÝÝÝå"+
            "WWWWWvvvvvvÊàààóËËËßßááááÝÝÝÝÝÎåWWWWWWvvvÊÊÊÊÊÊËËËËËËááááÝÝÎÎÎÎÎ²²²²²²²²hhÊÊÊÊÊËËËËËËááááÎÎÎÎÎÎÎggggggg\004\004hhhhwwwËxxxÌÌÌ\026\026\026ÎÎÎÎÜÜ"+
            "³³½½½½½½½½hhhwwwwxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³³½½½½ÉÉÉÉÉÉÉÌÌÌÌÌ\026\026\026\026ÜÜÜÜÜIIIIIIXXVVVÉÉÉÉÉÉÉÉyyyy\026\026\026\026\026ÜÜÜÜ´´´´´XXXXXXttttttttyyyyyy\026\026\026ÚÚÚÛ"+
            "´´´´´´XffffffttttttÍÏÏÏÏÏÏÏ××ÚÚÚYYYYYYfffffffiiiiÍÍÍÍÏÏÏÏÏÏ×××ÚÚYYYYYYYYYffffiiiiÍÍÍÍÍÏÏÏÐÐ××××Ú++++++++++Y\020\020\020\020\020\020\020sÍÍÍÍØÐÐÐÐ××××"+
            "[[[[[[[¼¼¼¼¼¼\020\020\020\020\020sssssØØØÐÐÐ×××µ[[[[[[[¼¼¼¼¼eeeee\020ssssrrØÈÈÈÈ\024\024µµµµµµµ[[¼¼¼¼e¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024µµµµµµµµUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "µµµµµµµµ»»»»»¾¾¾¾¾¾¾jjjrrrrÈÈÒÒÒººººº»»»»»»»»»»»¿¿jjjjjjrrÑÑÒÒÒÒºººººººº»»»»»»»¿¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶¶,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÖÖÔÔ¶¶¶¶¶¶,,,,,,,,^^^^^^^¿\021\021\021\021\021\021ÔÔÔÔ·······,,,,,ÀÀÀÀÀ^^^ÁÁÁÇ\021\021\021\021\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022\022Å"+
            "------------ÀÀÀÀÀÂÂÁÁÁÁÁÇÇÇÇ\022\022\022Å---------¹¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022\022Å-------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÁÃÃÃÃÃ\022\022Å¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "\001\001\001\001u\027\027\027\027\027\027\030\030\030\030\030óßÞÞÞÞÞÞ\025\025\025\025\025\025\025\025\002\002\002uuuu\027\027\027\027\030\030\030\030óóßßÞÞÞÞÞ\025\025\025\025\025\025Ýå\002\002\002uuuuuôôô\030\030ààóóßßßßÞÞá\025\025\025ÝÝÝÝåWW\002\002uuvvôôàààààóóßßßßßáááÝÝÝÝÝÝå"+
            "WWWWWvvvvvÊÊàààËËËËßßááááÝÝÝÝÎÎåWWWWWWvvvÊÊÊÊÊÊËËËËËËááááÝÝÎÎÎÎå²²²²²²²hhhhÊÊÊÊËËËËËËááááÎÎÎÎÎÎÎgggggg\004\004\004hhhhwwwËxxxÌÌÌ\026\026\026ÎÎÎÎÜÜ"+
            "³³½½½½½½½½hhwwwwwxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³³½½½ÉÉÉÉÉÉÉÉÌÌÌÌÌ\026\026\026\026ÜÜÜÜÜIIIIIIIXVVVÉÉÉÉÉÉÉÉyyyy\026\026\026\026\026ÜÜÜÜ´´´´´XXXXXXttttttttyyyyyy\026\026\026ÚÚÚÛ"+
            "´´´´´´XffffftttttttÍÏÏÏÏÏÏ×××ÚÚÚYYYYYYfffffffiiiiÍÍÍÍÏÏÏÏÏ××××ÚÚYYYYYYYYYfff\020iiiiÍÍÍÍÍÏÏÐÐÐ××××Ú++++++++++Y\020\020\020\020\020\020\020ssÍÍÍØÐÐÐÐ××××"+
            "[[[[[[[¼¼¼¼¼e\020\020\020\020\020sssssØØØÐÐÐ×××[[[[[[[[¼¼¼¼¼eeeeesssssrrØÈÈÈÈ\024\024µµµµµµµ[[¼¼¼¼e¾¾¾¾¾¾¾rrrrrÈÈÈÈ\024\024µµµµµµµµUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "µµµµµµµ»»»»»»¾¾¾¾¾¾¾jjjrrrrÈÈÒÒÒººººº»»»»»»»»»»»¿¿jjjjjjrÑÑÑÒÒÒÒºººººººº»»»»»»»¿¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶¶,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÖÔÔÔ¶¶¶¶¶¶,,,,,,,,^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ·······,,,,,ÀÀÀÀÀ^^^ÁÁÁÇ\021\021\021k\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022\022Å"+
            "-----------¹ÀÀÀÀÀÂÂÁÁÁÁÁÇÇÇÇ\022\022\022Å---------¹¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022\022Å------¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÁÃÃÃÃÃ\022\022Å¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "\002\002uuuu\027\027\027\027\030\030\030\030\030óóßßÞÞÞÞÞ\025\025\025\025\025\025\025å\002\002\002uuuu\027\027ô\030\030\030\030\030óóßßßÞÞÞá\025\025\025\025\025ÝÝå\002\002uuuôôôôàààóóóßßßßÞáá\025\025ÝÝÝÝÝåWWWvvvvôôààààóóóßßßßßáááÝÝÝÝÝÝå"+
            "WWWWWvvvvvÊÊÊàóËËËËßßááááÝÝÝÝÎÎå²²²WWvvvÊÊÊÊÊÊËËËËËËáááááÝâÎÎÎÎå²²²²²²²hhhhÊÊÊÊËËËËËËááááÎÎÎÎÎÎÎggggg\004\004\004\004hhhwwwwxxxxÌÌÌ\026\026\026ÎÎÎÎÜÜ"+
            "³³½½½½½½½\004hhwwwwwxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³³½½½\005ÉÉÉÉÉÉÉÌÌÌÌÌ\026\026\026\026ÜÜÜÜÜIIIIIIIVVVVÉÉÉÉÉÉÉyyyyy\026\026\026\026\026ÜÜÜÛ´´´´XXXXXXVttttttttyyyyyy\026\026ÚÚÚÚÛ"+
            "´´´´´XfffffftttttttÍÏÏÏÏÏÏ×××ÚÚÚYYYYYYffffffZiiiiÍÍÍÍÏÏÏÏÏ××××ÚÚYYYYYYYYYfffiiiiiÍÍÍÍÍÏÏÐÐÐ××××Ú+++++++++YY\020\020\020\020\020\020\020sssÍØØÐÐÐÐ××××"+
            "[[[[[[[¼¼¼¼¼ee\020\020\020\020sssssØØØÐÐÐ×××[[[[[[[[¼¼¼¼eeeeeessssrrrÈÈÈÈ\024\024\024µµµµµµµUU¼¼¼\\e¾¾¾¾¾¾rrrrrrÈÈÈÈ\024\024µµµµµµµUUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "µµµµµµµ»»»»»»¾¾¾¾¾¾jjjjrrrrÈÈÒÒÒººººº»»»»»»»»»»»¿jjjjjjjrÑÑÑÒÒÒÒºººººººº»»»»»»»¿¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶,,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÖÔÔÔ¶¶¶¶¶,,,,,,,,,^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ·······,,,,ÀÀÀÀÀÀ^^^ÁÁÁÇ\021\021kk\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅ"+
            "-----------¹ÀÀÀÀÀÂÂÁÁÁÁÁÇÇÇ\022\022\022\022Å---------¹¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022\022Å------¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÁÃÃÃÃÃ\022\022Å¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "uu\027\027\027ô\030\030\030\030óóóßßÞÞÞÞá\025\025\025\025\025\025Ýåôôôô\030\030\030óóóßßßÞÞÞá\025\025\025\025ÝÝÝåôôôôôàààóóóßßßßÞáá\025ÝÝÝÝÝÝåWWWvvvvôàààààóóóßßßßááááÝÝÝÝÝåå"+
            "WWWWvvvvvÊÊÊÊóËËËËËáááááÝÝÝâÎÎå²²²²²vvÊÊÊÊÊÊÊËËËËËËáááááââÎÎÎÎå²²²²²²HhhhhhwwËËËËËËxááááÎÎÎÎÎÎågggg\004\004\004\004\004hhhwwwwxxxxÌÌÌ\026\026\026ÎÎÎÜÜÜ"+
            "³³½½½½½½\004\004hwwwwwxxxÌÌÌÌ\026\026\026\026ÜÜÜÜÜ³³³³³³³½½\005\005\005ÉÉÉÉÉÉxÌÌÌÌ\026\026\026\026ÜÜÜÜÜIIIIIIIVVVVÉÉÉÉÉÉÉyyyyy\026\026\026\026ÜÜÜÛÛ´´´´XXXXXXVttttttttyyyyyy\026\026ÚÚÚÚÛ"+
            "´´´´´ffffffftttttttÏÏÏÏÏÏÏ××ÚÚÚÚYYYYYYffffffZiiiÍÍÍÍÍÏÏÏÏÏ××××ÚÚYYYYYYYYYffZiiiiiÍÍÍÍÍÏÏÐÐÐ××××Ú+++++++++Y\020\020\020\020\020\020\020sssssØØÐÐÐÐ××××"+
            "[[[[[[[¼¼¼¼¼ee\020\020\020\020sssssØØØÐÐÐ×××[[[[[[[[¼¼¼¼eeeeeessssrrrÈÈÈÈ\024\024\024µµµµµµUUU¼¼\\\\e¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024\024µµµµµµUUUUU\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "µµµµµµU»»»»»»¾¾¾¾¾¾jjjjrrrrÈÈÒÒÒººººº»»»»»»»»»»¿¿jjjjjjjrÑÑÒÒÒÒÒºººººººº»»»»»»»¿¿¿¿¿jjjj\021\021ÖÖÖÖÒÒºººººººººº»»»»»¿¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶¶,,,,,^^^^^¿¿¿¿\021\021\021\021\021\021ÔÔÔÔ¶¶¶¶,,,,,,,,,,^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ·······,,,,ÀÀÀÀÀÀ^^ÁÁÁÁÇ\021kkk\022\022ÔÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅ"+
            "-----------¹¹ÀÀÀÂÂÂÁÁÁÁÁÇÇÇ\022\022\022ÅÅ--------¹¹¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÇ\022\022ÅÅ-----¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃÃ\022ÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "ôôôôô\030\030óóóßßßßÞÞáá\025\025\025\025ÝÝÝåôôôôôààóóóóßßßÞÞáá\025\025\025ÝÝÝÝåôôôàààóóóóßßßßáááÝÝÝÝÝÝååvv\003\003\003àààóóóóßßßßááááÝÝÝÝÝåå"+
            "WWWWvvv\003\003ÊÊÊËËËËËñáááááÝÝââÎåå²²²²²HHÊÊÊÊÊÊÊËËËËËËáááááââÎÎÎÎå²²²²²HHHhhhwwËËËËËxxááááÎÎÎÎÎÎå²g\004\004\004\004\004\004\004hhwwwwwxxxxÌÌ\026\026\026\026ÎÎÎÜÜÜ"+
            "³³³½½½½\004\004\004wwwwwwxxxxÌÌ\026\026\026\026\026ÜÜÜÜÜ³³³³³³VVV\005\005\005\005\005ÉÉÉxxÌÌÌ\026\026\026\026\026ÜÜÜÜÜIIIIIIVVVVVÉÉÉÉÉÉÉyyyyy\026\026\026\026ÜÛÛÛÛ´´´XXXXXXVVtttttttyyyyyyy\026\026ÚÚÚÚÚ"+
            "´´´´fffffffZttttttÍÏÏÏÏÏÏÏ××ÚÚÚÚ±±YYYYffffZZZiiiÍÍÍÍÍÏÏÏÏÏ×××ÚÚÚ±±±±±YYYffZZiiiiiÍÍÍÍÍÏÏÐÐÐ××××Ú++++++++YY\020\020\020\020\020\020\020sssssØØØÐÐÐ××××"+
            "[[[[[[[¼¼¼¼eee\020\020\020ssssssØØØØÐÐ×××[[[[[[[[¼¼¼¼eeeeeessssrrrÈÈÈÈ\024\024\024µµµµµUUUUU\\\\\\e¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024\024µµµµµUUUUU\\\\\\¾¾¾¾¾¾¾¾rrrrrÈÈÈÈÈ\024"+
            "°°°°°UU»»»»»»¾¾¾¾¾¾jjjrrrrrÈÒÒÒÒ°°°°°»»»»»»»»»»¿¿jjjjjjjÑÑÑÒÒÒÒÒººººººº»»»»»»»»¿¿¿¿jjjjj\021ÖÖÖÖÒÒÒºººººººººº»»»»^¿¿¿¿¿¿¿\021\021\021\021ÖÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶,,,,,,^^^^^¿¿¿\021\021\021\021\021\021\021ÔÔÔÔ¶¶¶,,,,,,,,,,,^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ·······,,,,ÀÀÀÀÀÀ^^ÁÁÁÁÇkkkk\022ÆÆÔ··········ÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅ"+
            "----------¹¹¹ÀÀÀÂÂÂÁÁÁÁÇÇÇÇ\022\022\022ÅÅ--------¹¹¹¹¹¹¹¹ÂÂÂÂÂÁÁÁÇÇÇÃ\022\022ÅÅ----¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃÃ\022ÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃÃÃÃÅ"+
            "ôôôôôàóóóóßßßßÞÞáá\025\025ÝÝÝÝååôôôôàóóóóßßßßßááá\025ÝÝÝÝÝååôààóóóóóßßßßáááÝÝÝÝÝÝåå\003\003\003\003\003óóóóËßßßááááÝÝÝÝââåå"+
            "\003\003\003\003\003\003ËËËËËñáááááââââÎåå²²²HHHHÊÊÊÊÊÊËËËËËñáááááââÎÎÎåå²²HHHHHHwwwËËËxxxááá\026ÎÎÎÎÎÎå\004\004\004\004\004\004\004\004\004hwwwwwwxxxxÌÌ\026\026\026\026ÎÎÜÜÜÜ"+
            "³³³½½½\004\004\004\005wwwwwwxxxxÌÌ\026\026\026\026\026ÜÜÜÜÜ³³³³³VVVV\005\005\005\005\005\005\005ÉyyÌÌÌ\026\026\026\026\026ÜÜÜÜÛIIIIIVVVVVV\005ÉÉÉÉÉyyyyyy\026\026\026\026ÛÛÛÛÛ´IIXXXXVVVVtttttttyyyyyyy\026\026ÚÚÚÚÚ"+
            "´´´´fffffZZZttttttÍÏÏÏÏÏÏÏ××ÚÚÚÚ±±±±±±ffZZZZZiiiÍÍÍÍÏÏÏÏÏÏ×××ÚÚÚ±±±±±±±±ZZZZiiiiiÍÍÍÍÍÏØÐÐÐ×××ÚÚ++++±±±±±±\020\020\020\020\020\020\020sssssØØØÐÐÐ×××Ú"+
            "[[[[[[[¼¼¼¼eeee\020\020ssssssØØØØÐÐ××\024[[[[[[[[¼¼¼eeeeeeessssrrrÈÈÈÈ\024\024\024µµµUUUUUU\\\\\\\\ee¾¾¾¾¾rrrrrÈÈÈÈÈ\024\024µµµUUUUUUU\\\\\\¾¾¾¾¾¾¾rrrrrrÈÈÈÈÒ\024"+
            "°°°°°°°»»»TTT¾¾¾¾¾jjjjrrrrÑÈÒÒÒÒ°°°°°°»»»»»»»»T¿¿jjjjjjjÑÑÑÒÒÒÒÒººººººº»»»»»»»»¿¿¿¿jjjjj\021ÖÖÖÖÒÒÒººººººººº»»»»»^¿¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖÖ"+
            "¶¶¶¶¶¶¶,,,,,^^^^^^^¿¿\021\021\021\021\021\021ÔÔÔÔÔ¶,,,,,,,,,,,,^^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ·······,,,,ÀÀÀÀÀ^^^ÁÁÁÇÇkkkk\022ÆÆÆ·········ÀÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅ"+
            "----------¹¹¹___ÂÂÂÁÁÁÁÇÇÇÇ\022\022\022ÅÅ-------¹¹¹¹¹¹¹¹_ÂÂÂÂÂÁÁÁÇÇÇÃ\022\022ÅÅ---¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂcÃÃÃÃÃÃ\022ÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂcÃÃÃÃÃÃÃÃÅ"+
            "ôôóóóóóóßßßßááááÝÝÝÝÝÝååôóóóóóóßßßßááááÝÝÝÝÝâåå\003\003\003óóóóóßßßßááááÝÝÝÝââåå\003\003\003\003\003óóóËñññááááÝâââââåå"+
            "\003\003\003\003\003\003ËËËËññááááâââââÎååHHHHHHHËËËËËññááááâââÎÎÎååHHHHHHHHwwËxxxxxá\026\026\026ÎÎÎÎÎååHH\004\004\004\004\004\004wwwwwwwxxxxxxÌ\026\026\026\026ÎÜÜÜÜÜ"+
            "³³³³\004\004\004\004\005\005\005\005\005\005wxxxxxÌÌ\026\026\026\026ÜÜÜÜÜÜIIVVVVVV\005\005\005\005\005\005\005\005\005yyyy\026\026\026\026\026\026ÜÜÛÛÛIIIIVVVVVV\005\005\005ttttyyyyyy\026\026\026\026ÛÛÛÛÛ¤¤¤¤¤VVVVVVtttttttyyyyyyy\026ÚÚÚÚÚÚ"+
            "´´fffZZZZZZZtttttÍÍÏÏÏÏÏÏÏ××ÚÚÚÚ±±±±±±ZZZZZZiiiiÍÍÍÍÏÏÏÏÏÏ×××ÚÚÚ±±±±±±±±ZZZZiiiiiÍÍÍÍÏØØÐÐÐ×××ÚÚ±±±±±±±±±±\020\020\020\020\020issssssØØØØÐÐ×××Ù"+
            "[[[[[[[¼¼¼eeeee\020ssssssØØØØØØ{×\024\024UUU[[[[¼¼¼eeeeeeeesssrrrrÈÈÈ\024\024\024\024UUUUUUUUU\\\\\\\\ee¾¾¾¾2rrrrrÈÈÈÈ\024\024\024UUUUUUUUU\\\\\\\\¾¾¾¾¾¾¾rrrrrrÈÈÈÈÒ\024"+
            "°°°°°°°°TTTTT¾¾¾¾jjjjjrrrÑÑÈÒÒÒÒ°°°°°°°»»»»TTTT]jjjjjjjjÑÑÑÒÒÒÒÒººººººº»»»»»»»]¿¿¿¿jjjjj\021ÖÖÖÖÒÒÒººººººººº»»»»^^^¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÖÔ"+
            "¶¶¶¶¶,,,,,,,^^^^^^^¿¿\021\021\021\021\021\021ÔÔÔÔÔ,,,,,,,,,,,,,^^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔ······,,,,,ÀÀÀÀÀ^^^ÁÁÁÇkkkkkÆÆÆÆ·········ÀÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅ"+
            "---------¹¹¹¹___ÂÂÂÁÁÁÁÇÇÇÇ\022\022\022ÅÅ------¹¹¹¹¹¹¹¹¹_ÂÂÂÂÂÁÁÇÇÃÃÃ\022\022ÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂcÃÃÃÃÃÃ\022ÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂcÃÃÃÃÃÃÃÃÄ"+
            "óóóóóóßßßßááááÝÝÝââååå\003\003óóóóóßßßáááááÝÝâââååå\003\003\003\003óóóóñññáááááâââââååå\"\"\003\003\003\003\003òËññññááááâââââååå"+
            "\"\"\003\003\003òòËññññááááââââÎåååHHHHHHHòËËËñññááááâââÎÎåååHHHHHHHxxxxxxx\026\026\026\026ÎÎÎÎÎååHHHHHH\004\004wwwwwwxxxxxx\026\026\026\026\026ÜÜÜÜÜÜ"+
            "³³VV\004\004\005\005\005\005\005\005\005\005\005xxxxxÌ\026\026\026\026\026ÜÜÜÜÛÛ¤¤¤VVVVV\005\005\005\005\005\005\005\005yyyyy\026\026\026\026\026\026ÛÛÛÛÛ¤¤¤¤¤VVVVV\005\005\005tttyyyyyyy\026\026\026\026ÛÛÛÛÛ¤¤¤¤¤¤VVVVtttttttyyyyyyyy\026ÚÚÚÚÚÚ"+
            "¥¥¥¥ZZZZZZZZtttttÍÍÏÏÏÏÏÏ×××ÚÚÚÚ±±±±¥ZZZZZZZiiiiÍÍÍÍÏÏÏÏÏ××××ÚÚÚ±±±±±±±ZZZZZiiiissÍÍÍØØØØÐ××××ÚÚ±±±±±±±±±Z\020\020\020iiisssssØØØØØØ{×××Ù"+
            "JJJJJJJJ¼eeeeeeessssssØØØØØØ{\024\024\024JJJJJJJJJ\\eeeeeeeesssrrrÈÈÈÈ\024\024\024\024UUUUUUUU\\\\\\\\\\ee¾¾¾¾22rrrrÈÈÈÈ\024\024\024UUUUUUUUU\\\\\\\\\\¾¾¾¾¾22rrrrrÈÈÈÈ\024\024"+
            "°°°°°°°TTTTTTT¾¾¾jjjjjrrÑÑÑÒÒÒÒÒ°°°°°°°°TTTTTT]]jjjjjjjÑÑÑÑÒÒÒÒÒºº°°°°°°»»»»T]]]¿¿jjjjj\021\021ÑÖÖÖÒÒÒººººººººº»»»^^^^¿¿¿¿¿\021\021\021\021\021ÖÖÖÖÔÔ"+
            "¶¶¶¶,,,,,,,,^^^^^^^¿\021\021\021\021\021\021\021ÔÔÔÔÔ,,,,,,,,,,,,,^^^^^^^^\021\021\021\021\021kÔÔÔÔÔ······,,,,ÀÀÀÀÀÀ^^^ÁÁÁkkkkkkÆÆÆÆ········SSÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇ\022\022\022ÅÅÅ"+
            "--------¹¹¹¹____ÂÂÂÁÁÁÁÇÇÇÇ\022\022ÅÅÅ-----¹¹¹¹¹¹¹¹¹__ÂÂÂÂÂÁÁÇÃÃÃÃ\022ÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂcÃÃÃÃÃÃÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¸ÂÂÂÂÂccÃÃÃÃÃÃÃlÄ"+
            "\"\"\"\"\"\"\003\003óóóóóñññáááááâââââååå\"\"\"\"\"\"\003\003óóóóñññáááááâââââååå\"\"\"\"\"\"\003\003òòññññáááááâââââååå\"\"\"\"\"\"\003\003òòòñññññááááâââââååå"+
            "òòòòññññááááââââÎåååHHHHHHòòòòññññááááâââÎÎåååHHHHHHxxxxxxx\026\026\026\026ÎÎÎÎÜåå££HHHHHwwwwxxxxxxx\026\026\026\026\026ÜÜÜÜÜÛ"+
            "¤¤¤VV\005\005\005\005\005\005\005\005\005\005xxxxx\026\026\026\026\026\026ÜÜÛÛÛÛ¤¤¤¤¤VVV\005\005\005\005\005\005\005\005yyyyy\026\026\026\026\026ÛÛÛÛÛÛ¤¤¤¤¤¤VVV\005\005\005\005\006\006\006yyyyyyy\026\026\026ÛÛÛÛÛÛ¤¤¤¤¤¤¤VVVtttttttyyyyyyyy×ÚÚÚÚÚÚ"+
            "¥¥¥¥¥ZZZZZZZttttÍÍÏÏÏÏÏÏÏ××ÚÚÚÚÚ¥¥¥¥¥¥ZZZZZZiii\007ÍÍÍÍÏÏÏÏÏ×××ÚÚÚÚ±±±±±¥¥ZZZZiiiiissssÍzØØØØ{××ÙÙÙ±±±±±±±±±e4iiiissssssØØØØØØ{××ÙÙ"+
            "JJJJJJJJeeeeeeeesssss3ØØØØØ{{\024\024\024JJJJJJJJJ\\eeeeeeess22rrrÈÈÈÈ\024\024\024\024UUUUUUUU\\\\\\\\\\ee¾¾¾222rrrrÈÈÈÈ\024\024\024UUUUUUUU\\\\\\\\\\\\¾¾¾¾j22rrrrÈÈÈÈÒ\024\024"+
            "°°°°°°°TTTTTTT¾¾jjjjjjrÑÑÑÑÒÒÒÒÒ°°°°°°°°TTTTTT]]jjjjjjjÑÑÑÑÒÒÒÒÒ°°°°°°°°TTTTT]]]]¿jjjjj\021ÑÑÖÖÖÒÒÓººººººººº»»^^^^^¿¿¿¿j\021\021\021\021\021ÖÖÖÔÔÔ"+
            "¶,,,,,,,,,,^^^^^^^^^\021\021\021\021\021\021\021ÔÔÔÔÔ,,,,,,,,,,,,^^^^^^^^\021\021\021kkkkÔÔÔÔÔ¯¯¯¯¯¯¯¯¯,ÀÀÀÀÀÀ^^ÁÁÁÁkkkkkkÆÆÆÆ······SSSSSÀÀÀÀ_ÂÁÁÁÁÁÇÇÇÇ\022\022\022ÅÅÅ"+
            "-------¹¹¹¹_____ÂÂÂÁÁÁÁÇÇÇÇ\022\022ÅÅÅ---¹¹¹¹¹¹¹¹¹¹___ÂÂÂÂÂccÃÃÃÃÃ\022ÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¸¸ÂÂÂÂÂccÃÃÃÃÃÃÅÅÄ¹¹¹¹¹¹¹¹¹¹¹¹¹¸¸¸¸ÂÂÂÂccÃÃÃÃÃÃÃlÄ"+
            "\"\"\"\"\"\"\"òòòòññññáááááââââåååå\"\"\"\"\"\"\"òòòòññññáááááââââåååå\"\"\"\"\"\"\"òòòòññññáááááââââåååå\"\"\"õõòòòòñññññááááââââåååå"+
            "õõõõòòòòñññññááááââââååååòòòxññññáááââââÎåååå££££££xxxxxxx\026\026\026\026ÎÎÎÜååå££££££££wwwxxxxxx\026\026\026\026\026ÜÜÜÜÛÛÛ"+
            "¤¤££>>>\005\005\005\005\005\005\005\005xxxxx\026\026\026\026\026\026ÛÛÛÛÛÛ¤¤¤¤¤¤V\005\005\005\005\005\005\005\005yyyyyyy\026\026\026\026ÛÛÛÛÛÛ¤¤¤¤¤¤¤VG\006\006\006\006\006\006\006yyyyyyy\026\026\026ÛÛÛÛÛÛ¤¤¤¤¤¤¤¤GG\006\006\006\006\006\006yyyyyyyyÚÚÚÚÚÚä"+
            "¥¥¥¥¥¥ZZZZZZ\007\007\007\007\007ÍÏÏÏÏÏÏÏ××ÚÚÚÚÚ¥¥¥¥¥¥¥ZZZZZi\007\007\007\007ÍÍÍÏÏÏzØ{××ÚÚÙÙ¥¥¥¥¥¥¥¥ZZZ4iiissssszzzØØ{{{×ÙÙÙ±±±±±¥¥¥444444isssss3zØØØØ{{{×ÙÙ"+
            "JJJJJJJJeeeeeeessss333ØØØØØ{\024\024\024\024JJJJJJJJ\\\\eeeeeee2222rrrÈÈÈ|\024\024\024\024UUUUJJJ\\\\\\\\\\\\ee¾¾2222rrrrÈÈÈ\024\024\024\024UUUUUUU\\\\\\\\\\\\\\¾¾¾j222rrrrÈÈÈÈÒ\024\024"+
            "°°°°°°°TTTTTTT¾jjjjjjjÑÑÑÑÑÒÒÒÒÓ°°°°°°°TTTTTT]]]jjjjjjjÑÑÑÑÒÒÒÒÓ°°°°°°°°TTTT]]]]]jjjjjj\021ÑÑÖÖÒÒÒÓºººººº°°°TT^^^^^]¿¿dd\021\021\021\021\021ÖÖÔÔÔÔ"+
            ",,,,,,,,,,,^^^^^^^^dd\021\021\021\021\021ÔÔÔÔÔÔ¯¯¯,,,,,,,,,^^^^^^^^kkkkkkkÔÔÔÔÔ¯¯¯¯¯¯¯¯¯¯SÀÀÀÀ^^^ÁÁÁkkkkkkÆÆÆÆÆ¯¯¯¯SSSSSSSS____ÂÁÁÁÁÁÇÇÇÇ\022\022ÅÅÅÅ"+
            "---SSSSSS¹______ÂÂÂÁÁÁÁÇÇÇ\022\022\022ÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹__ÂÂÂÂÂÂccÃÃÃÃÃ\022ÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¸¸¸ÂÂÂÂcccÃÃÃÃÃÃlÅÄ¹¹¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸ÂÂÂcccÃÃÃÃÃÃllÄ"+
            "\"\"\"\"\"\"\"õõõòòòòòññññááááâââââåååå\"\"\"\"õõõõòòòòòññññááááâââââåååå\"õõõõòòòòòññññááááâââââååååõõõõõòòòòññññááááâââââåååå"+
            "õõõõõòòòòñññññáááââââååååå££££££õõxx\026\026ââââååååå££££££££====xxx\026\026\026ððÜÜÛååå££££££££>>====xxx\026\026\026\026ÛÛÛÛÛÛã"+
            "££>>>>>>>>>\005\005\005y\026\026\026\026\026ÛÛÛÛÛÛÛ¤¤¤¤¤G>>>>\006\006\006\006\006yyyyyyy\026\026\026ÛÛÛÛÛÛÛ¤¤¤¤¤GGGG\006\006\006\006\006\006\006yyyyyyy\026ÛÛÛÛÛÛä¤¤¤¤¤GGGGG\006\006\006\006\006\006yyyyyÚÚÚÚÚÚä"+
            "¥¥¥¥¥¥¥ZZZZ\007\007\007\007\007\007\007ÏÏÏÏ{×ÚÚÚÙÙÙ¥¥¥¥¥¥¥¥ZZ44\007\007\007\007\007\007Ízzzzz{{{ÙÙÙÙÙ¥¥¥¥¥¥¥¥444444\007\007ss33zzzzØ{{{ÙÙÙÙ¥¥¥¥¥¥¥44444444ss3333zzØØ{{{{ÙÙÙ"+
            "JJJJJJJJeeeeeee3333333ØØØØ{{\024\024\024\024JJJJJJJJ\\\\eeeeee222222rrÈÈ|\024\024\024\024\024JJJJJJJ\\\\\\\\\\\\eee222222rrÈÈÈÈ\024\024\024\024UUUUUU\\\\\\\\\\\\\\\\¾¾j22222rrÑÑÈÈÒ\024\024\024"+
            "°°°°°°TTTTTTTTjjjjjjjÑÑÑÑÑÑÒÒÒÓÓ°°°°°°°TTTTTT]]]jjjjjjÑÑÑÑÑÒÒÒÓÓ°°°°°°°°TTT]]]]]]jjjjj\021\021ÑÑÖÖÒÒÓÓ°°°°°°°°TT]]]]]]]]ddd\021\021\021\021\021ÔÔÔÔÔÓ"+
            ",,,,,,,,,,^^^^^^^^^dd\021\021\021\021kÔÔÔÔÔÔ¯¯¯¯¯¯¯¯,,,^^^^^^^^dkkkkkkkÔÔÔÔÔ¯¯¯¯¯¯¯¯¯SSSÀÀÀ^^ÁÁÁÁkkkkkkÆÆÆÆÆ¯¯SSSSSSSSS_____ÂÁÁÁÁÁÇÇÇk\022\022ÅÅÆÆ"+
            "SSSSSSSSS_______ÂÂÂÁÁccÇÇÇ\022\022ÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¹___¸¸ÂÂÂÂcccÃÃÃÃÃÅÅÅÄ¹¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸ÂÂÂÂcccÃÃÃÃÃllÄÄ¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸¸¸ÂÂccccÃÃÃÃÃÃlÄÄ"+
            "õõõõõòòòòòññññááááââââåååååõõõõõòòòòòññññááááââââåååååõõõõõõòòòòññññááááââââåååååõõõõõõòòòññññááááââââååååå"+
            "õõõõõñ\031\031\031\031áðââââååååå£££££££=====\026\026ððððååååå££££££££======\026\026ðððÛÛÛãåå£££££££>>>====\026\026\026ðÛÛÛÛÛãã"+
            ">>>>>>>>>>>>>\026\026\026\026\026ÛÛÛÛÛÛÛGGGGGG>>>>\006\006\006\006\006yyyyyyy\026\026\026ÛÛÛÛÛÛäGGGGGGGGG\006\006\006\006\006\006\006yyy\026ÛÛÛÛÛääGGGGGGGGGG\006\006\006\006\006\006ÚÚÚÚÙää"+
            "¥¥¥¥¥¥¥¥Z\007\007\007\007\007\007\007\007\007z{{{ÙÙÙÙÙÙ¥¥¥¥¥¥¥¥444\007\007\007\007\007\007\007zzzzzz{{{ÙÙÙÙÙ¥¥¥¥¥¥¥444444\007\007\007333zzzzz{{{{ÙÙÙÙ*******4444444433333zzzzØ{{{{ÙÙÙ"+
            "JJJJJJJJeeeee¦¦3333333zØØ||{\024\024\024\024JJJJJJJJ\\\\eeee¦¦222222rrÈ||\024\024\024\024\024JJJJJJJ\\\\\\\\\\\\\\e2222222rrÈÈÈ\024\024\024\024\024°°°°TKKKKKKKKKKj222222ÑÑÑÑÑÒÒ\024\024\024"+
            "°°°°°°TTTTTTKKKjjjjjjÑÑÑÑÑÑÒÒÓÓÓ°°°°°°TTTTTT]]]]jjjjjjÑÑÑÑÑÒÒÓÓÓ°°°°°°°TTTT]]]]]]jjjjj\021ÑqqqqÒÓÓÓ°°°°°°°°T]]]]]]]]ddddd\021\021\021ÔÔÔÔÔÔÓ"+
            "¯¯¯,,,,,,,^^^^^^^^dddkkkkkÔÔÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯^^^^^^^^dkkkkkkkÆÆÆÔÔ¯¯¯¯¯¯¯¯SSSSS__^^ÁÁÁkkkkkkkÆÆÆÆÆSSSSSSSSSSS_____ÂÁÁÁÁÁÇÇkk\022ÆÆÆÆÆ"+
            "SSSSSSSSS_______ÂÂÂccccÇÇÃ\022\022ÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸¸ÂÂÂccccÃÃÃÃllÅÄÄ¹¹¹¹¹¹¹¹¸¸¸¸¸¸¸¸¸ÂÂccccÃÃÃÃÃllÄÄ¹¹¹¹¹¹¹¸¸¸¸¸¸¸¸¸¸¸ÂccccÃÃÃÃÃllÄÄ"+
            "õõõõõõõòñññ\031áááââââååååååõõõõõõññ\031\031\031ááââââååååååõõõõõõ\031\031\031\031ááââââåååååå!!!!õõõõ\031\031\031\031\031áðððâåååååå"+
            "£££!!!=====\031\031\031\031\031ðððððåååååå££££££=======ððððððååååå£££££££======\026ððððÛÛãããã££££>>>>>>===\026\026ððÛÛÛÛããã"+
            ">>>>>>>>>>>>>\026\026\026ÛÛÛÛÛÛÛäGGGGGGG>>\006\006\006\006\006\006y\026ÛÛÛÛÛÛääGGGGGGGGG\006\006\006\006\006\006ÛÛäääGGGGGGGGGG\006\006\006\006\006\006ÙÙÙÙää"+
            "¥¥¥¥¥¥¥44\007\007\007\007\007\007\007\007zzz{{{ÙÙÙÙÙÙ¥¥¥¥¥¥44444\007\007\007\007\007\007\007zzzzz{{{{ÙÙÙÙÙ¥¥¥¥¥¥4444444\007\007\007333zzzzz{{{{ÙÙÙÙ********444444333333zzzz{{{{{ÙÙÙ"+
            "**********¦¦¦¦¦¦33333zzz|||\024\024\024\024\024JJJJJJJJ\\\\ee¦¦¦¦222222r||||\024\024\024\024\024JJJJJJKKKKKKKK22222222rÑÈÈ|\024\024\024\024\024°°KKKKKKKKKKKKK2222222ÑÑÑÑÑÒ\024\024\024\024"+
            "°°°°°TTTTTKKKKKjjjjj2ÑÑÑÑÑÑÒÓÓÓÓ°°°°°°TTTTT]]]]]jjjjjÑÑÑÑqqÒÓÓÓÓ°°°°°°°TTT]]]]]]]ddddddqqqqqÓÓÓÓ°°°°°°°T]]]]]]]]dddddd\021\021\021ÔÔÔÔÔÓÓ"+
            "¯¯¯¯¯¯¯¯,^^^^^^^^ddddkkkkkÔÔÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯^^^^^^^ddkkkkkkÆÆÆÆÆÆ¯¯¯¯¯¯¯SSSSS___^^ÁÁÁkkkkkkÆÆÆÆÆÆSSSSSSSSSSS_____ÂÁÁÁ``ÇkkkÆÆÆÆÆÆ"+
            "SSSSSSSSS_______ÂÂcccccÃÃÃ\022ÅÅÅÅÄ®®®®®®®¸¸¸¸¸¸¸¸¸ÂÂcccccÃÃÃlllÄÄÄ®®®®®¸¸¸¸¸¸¸¸¸¸¸¸ÂcccccÃÃÃÃlllÄÄ®®®¸¸¸¸¸¸¸¸¸¸¸¸¸¸¸cccccÃÃÃÃÃllÄÄ"+
            "!!!!!!õõõ\031\031\031\031\031\031ððððååååååå!!!!!!!õõ\031\031\031\031\031\031ðððððåååååå!!!!!!!õõ\031\031\031\031\031\031ðððððåååååå!!!!!!!===\031\031\031\031\031\031ðððððåååååå"+
            "!!!!!!======\031\031\031\031\031\031ðððððåååååå£££££========ðððððãããããã££££££======ðððððÛããããã###>>>>>>>==\026ðððÛÛÛãããã"+
            "###>>>>>>>>>\026\026ðÛÛÛÛÛÛääGGGGGGGG>\006\006\006\006\006ÛÛÛÛÛäääGGGGGGGGG\006\006\006\006\006ääääGGGGGGGGGG\006\006\006\006\006Ùäää"+
            "¥¥¥¥¥¥44\007\007\007\007\007\007\007\007\007zzzz{{{ÙÙÙÙÙÙ¥¥¥¥¥444444\007\007\007\007\007\007zzzzzz{{{{ÙÙÙÙÙ******4444444\007\0073333zzzz{{{{{ÙÙÙÙ********444444333333zzzz{{{{{\024ÙÙ"+
            "*********¦¦¦¦¦¦¦33333zz||||\024\024\024\024\024JJJJJJJKKK¦¦¦¦¦¦222222|||||\024\024\024\024\024KKKKKKKKKKKKKK22222222ÑÑÑ||\024\024\024\024\024KKKKKKKKKKKKKKK222222ÑÑÑÑÑÑÓÓ\024\024\024"+
            "°°°°TTTTKKKKKKKjjjj2ÑÑÑÑÑÑqÓÓÓÓÓ°°°°°TTTTT]]]]]]jjjj1ÑÑqqqqqÓÓÓÓ°°°°°°TTT]]]]]]]ddddddqqqqqqÓÓÓÓ°°°°°°°]]]]]]]]]dddddddkqqÔÔÔÔÓÓ"+
            "¯¯¯¯¯¯¯¯¯^^^^^^^dddddkkkkkÔÔÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯^^^^^^ddkkkkkkkÆÆÆÆÆo¯¯¯¯¯¯SSSSSS____ÁRRRkkkkkkÆÆÆÆÆÆSSSSSSSSSSS_____Â``````kkkÆÆÆÆÆÆ"+
            "®®®®®®®®__¸¸¸¸¸¸Âcccccc`ÃlllÅÅÄÄ®®®®®®®¸¸¸¸¸¸¸¸¸¸ccccccÃÃÃlllÄÄÄ®®®®®®¸¸¸¸¸¸¸¸¸¸¸ccccccÃÃÃllllÄÄ®®®®­¸¸¸¸¸¸¸¸¸¸¸¸¸cccccÃÃÃÃlll\023\023"+
            "!!!!!!!!\031\031\031\031\031\031ððððððåååååå!!!!!!!!=\031\031\031\031\031\031ððððððåååååå!!!!!!!!=\031\031\031\031\031\031ððððððåååååå!!!!!!!====\031\031\031\031\031\031ððððððãååååå"+
            "!!!!!!======\031\031\031\031\031ððððððãããããã####!=======ððððððãããããã#######====ðððððãããããã#######>>>=ððððÛãããããä"+
            "#######>>>>þððÛÛÛÛääääGGGGGGGG>\006\006\006\006ääääGGGGGGGGG\006\006\006\006\006ääääGGGGGGGGGG\006\007\007\007äää"+
            "¥¥¥¥4444\007\007\007\007\007\007\007\007\007zzzzz{{{{ÙÙÙÙÙä****FF4444\007\007\007\007\007\0073zzzzz{{{{{ÙÙÙÙÙ*******FF444\007\0073333zzzzz{{{{{ÙÙÙÙ********FFF¦¦¦33333\bzzz{{{{{\024\024\024Ù"+
            "*********¦¦¦¦¦¦¦3333\b||||||\024\024\024\024\024*****KKKK¦¦¦¦¦¦¦222222|||||\024\024\024\024\024KKKKKKKKKKKKK¦2222222\tÑÑ|||\024\024\024\024\024KKKKKKKKKKKKKKK2222\t\tÑÑÑÑÑÓÓÓ\024\024\024"+
            "°°°KKKKKKKKKKKK1111\tÑÑÑÑqqqÓÓÓÓÓ°°°°TTTTT]]]]]]111111qqqqqqÓÓÓÓÓ°°°°°TT]]]]]]]]dddddddqqqqqqÓÓÓÓ°°°°°]]]]]]]]]ddddddddkkqqÔÔÔÓÓÓ"+
            "¯¯¯¯¯¯¯¯¯^^^^^^ddddddkkkkkÔÔoooÕ¯¯¯¯¯¯¯¯¯¯S^^^^RRRRRkkkkkÆÆÆÆooo¯¯¯¯¯SSSSSSS___RRRRRkkkkkÆÆÆÆÆÆoSSSSSSSSSS_____````````kkÆÆÆÆÆÆÆ"+
            "®®®®®®®®®¸¸¸¸¸¸¸ccc`````llllÄÄÄÄ®®®®®®®®¸¸¸¸¸¸¸¸cccccccÃÃllllÄÄÄ®®®®®®®­¸¸¸¸¸¸¸¸¸ccccccÃÃllllÄÄÄ­­­­­­­­­­­­¸¸¸¸¸cccccÃÃÃÃlll\023\023\023"+
            "!!!!!!!!ÿ\031\031\031\031\031\031\031ððððððåååååå!!!!!!!!ÿ\031\031\031\031\031\031\031ððððððããåååå!!!!!!!ÿÿ=\031\031\031\031\031\031\031ððððððãããããã!!!!!!!ÿÿ==\031\031\031\031\031\031ððððððãããããã"+
            "!!!!!ÿÿÿ===\031\031\031\031\031ðððððããããããã######ÿÿ===\031\031ðððððããããããã########þþþþðððððããããããä########þþþþþöððððãããããää"+
            "#########þþþþþÛäääää#########\006????äääää¢¢GGGGGGG55????ääää¢GGGGGG555555??äää"+
            "FFFFFFFFF\007\007\007\007\007\007\007\bzzzz{{{{ÙÙÙääFFFFFFFFFFFF\007\007\007\b\b\b\bzzz{{{{{ÙÙÙÙæ*****FFFFFFFFF33\b\b\b\bzz{{{{{{ÙÙÙæ*******FFFF¦¦¦¦33\b\b\b\b\b||||{\024\024\024\024æ"+
            "********¦¦¦¦¦¦¦¦¦3\b\b\b||||||\024\024\024\024\024§§§§KKKKK¦¦¦¦¦¦¦\t\t\t\t\t||||||\024\024\024\024\024KKKKKKKKKKKKK¦\t\t\t\t\t\t\t\tÑ||||\024\024\024\024\024KKKKKKKKKKKKKK\t\t\t\t\t\t\tÑÑÑÑqÓÓÓÓ\024}"+
            "LLLLLLLLLLLLLL111111\n\nqqqqÓÓÓÓÓ}LLLLLLLLLLLLLL1111111\nqqqqqÓÓÓÓÓ¨¨¨¨¨¨¨LL]]]]]dddddddqqqqqqÓÓÓÓÓ¨¨¨¨¨¨¨¨¨]]]]dddddddddkqqqqpppÕÕ"+
            "¯¯¯¯¯¯¯¯¨¨MMMMddddddkkkkkooooooÕ¯¯¯¯¯¯¯¯¯SSSMRRRRRRRkkkkkÆÆooooo¯¯¯SSSSSSSSS__RRRRRRRkkkkÆÆÆÆooo®®®®®®®®®®®¸¸¸¸`````````lÆÆÆÆÆÆÄ"+
            "®®®®®®®®®®¸¸¸¸¸¸````````llllÄÄÄÄ®®®®®®®®­­­¸¸¸¸¸ccccc``ÃlllllÄÄÄ®®®®­­­­­­­­­­¸¸cccccccÃlllllÄÄÄ­­­­­­­­­­­­­­­­­cccccÃÃ.llll\023\023\023"+
            "!!!!ÿÿÿÿÿÿÿÿ\031\031\031\031\031\031\031ððððððããããããî!!!ÿÿÿÿÿÿÿÿÿ\031\031\031\031\031\031\031ððððððããããããîÿÿÿÿÿÿÿÿÿÿ\031\031\031\031\031\031ððððððããããããîÿÿÿÿÿÿÿÿÿ\031\031\031\031\031öðððððããããããî"+
            "ÿÿÿÿÿÿÿþ\031\031ööðððððããããããî#####ÿÿÿÿþþþööööððððããããããî#######þþþþþþööööðððãããããäî#######þþþþþþþööððããããääää"+
            "########þþþþþþäääää¢¢¢¢¢¢¢¢¢??????äääää¢¢¢¢¢¢¢¢555?????ääää¢¢¢¢¢¢5555555???äää"+
            "FFFFFFFF5555555\b\b\b\bzz{{{{ÙæææFFFFFFFFFFFFF\b\b\b\b\b\b\b\b{{{{{{ÙÙÙææ**FFFFFFFFFFFF\b\b\b\b\b\b\b\b{{{{{{ÙÙææ******FFFFF¦¦¦¦\b\b\b\b\b\b||||||\024\024\024\024æ"+
            "*******¦¦¦¦¦¦¦¦¦\t\t\b\b\b||||||\024\024\024\024ç§§§§§§§§¦¦¦¦¦¦¦\t\t\t\t\t\t||||||\024\024\024\024ç§§§§§§§KKKKK¦\t\t\t\t\t\t\t\t\t|||||\024\024\024\024ç§§§§LKKKKKKKKK\t\t\t\t\t\t\t\n\nqqqÓÓ}}}}"+
            "LLLLLLLLLLLLL111111\n\n\nqqqqÓÓÓÓ}}LLLLLLLLLLLLL1111111\n\nqqqqqÓÓÓÓ}¨¨¨¨¨¨¨¨LLLLL11111111\nqqqqqpÓÓÓÕ¨¨¨¨¨¨¨¨¨¨MMMMMddddddd\013\013\013qppppÕÕ"+
            "¨¨¨¨¨¨¨¨¨MMMMMMMRRddkkkk\foooooÕÕ¯¯¯¯¯¯¯¯SSMMMRRRRRRRRkkk\f\foooooÕ®®®®®®®®®®®®RRRRRRRRRRkk\f\fÆoooon®®®®®®®®®®®¸¸¸`````````lllÆÆÆoÄn"+
            "®®®®®®®®®­­­­­¸````````lllllÄÄÄÄ®®®®®®­­­­­­­­­­```````lllllÄÄÄÄ­­­­­­­­­­­­­­­­ccccccQ.llll\023\023\023\023­­­­­­­­­­­­­­­­­cccc.....bb\023\023\023\023"+
            "ÿÿÿÿÿÿÿÿÿÿ\031\031\031\031öööðððððãããããîîÿÿÿÿÿÿÿÿÿ\031\031\031\031öööðððððãããããîîÿÿÿÿÿÿÿÿÿ\031\031ööööðððððãããããîîÿÿÿÿÿÿÿþ\031öööööðððããããããîî"+
            "ÿÿÿÿÿþþþöööööðððããããããîîÿþþþþþþþööööööððããããããîî####þþþþþþþþþööööööïïããããäîî######þþþþþþþþþöööïïïãääääî"+
            "¢¢¢¢¢¢¢¢þþ????ääääää¢¢¢¢¢¢¢¢¢??????äääää¢¢¢¢¢¢¢5555?????ääää¢¢¢¢¢55555555???ääää"+
            "FFFFFF55555555\b\b\b\b\b\bææææFFFFFFFFFFFF5\b\b\b\b\b\b\b\b{{{{{ÙÙææææFFFFFFFFFFFFF\b\b\b\b\b\b\b\b|ææææ***FFFFFFFF¦¦¦\b\b\b\b\b\b\b|||||\024\024\024ææ"+
            "§§§§§§¦¦¦¦¦¦¦¦¦\t\t\t\b\b||||||\024\024çç§§§§§§§§§¦¦¦¦¦\t\t\t\t\t\t\t||||\024\024çç§§§§§§§§§§§K\t\t\t\t\t\t\t\t\t\n|||\024\024çç§§§§§§LLLLLLLE\t\t\t\t\t\n\n\n\nqqÓÓ}}}}ç"+
            "LLLLLLLLLLLLL11111\n\n\n\n\nqqqÓÓ}}}}¨¨¨LLLLLLLLLL1111111\n\n\nqqqpÓÓ}}}¨¨¨¨¨¨¨¨¨¨MMMM111111\n\nqqqpppppÕÕ¨¨¨¨¨¨¨¨¨¨MMMMMMddddd\013\013\013\013\013pppÕÕÕ"+
            "¨¨¨¨¨¨¨¨¨MMMMMMRRRRRRk\f\f\foooooÕÕ®®®®®®¨¨MMMMMRRRRRRRRR\f\f\f\foooooÕ®®®®®®®®®®®NNNRRRRRRRR\f\f\f\foooonn®®®®®®®®®®­­­NNN```````lll\fooÄnn"+
            "®®®®®®®®­­­­­­`````````lllllÄÄÄÄ®®®®­­­­­­­­­­­­`````QQlllllÄÄÄÄ­­­­­­­­­­­­­­­­cc``QQQ..lll\023\023\023\023­­­­­­­­­­­­­­­­­ccQQ.....bb\023\023\023\023"+
            "ÿÿÿÿÿÿÿþöööööööööðïïããããîîîÿÿÿÿÿÿþþööööööööðïïããããîîîÿÿÿÿÿþþþööööööööïïïããããîîîÿÿÿÿþþþþööööööööïïïïãããîîî"+
            "ÿþþþþþþööööööööïïïïãããîîîþþþþþþþþöööööööïïïïïãäîîîþþþþþþþþöööööïïïïïääîîî¢¢¢¢¢¢¢þþþþþþ?öööïïïïääääîî"+
            "¢¢¢¢¢¢¢¢???????äääääî¢¢¢¢¢¢¢¢¢??????äääää¢¢¢¢¢¢¢5555?????ääää¢¢¢¢555555555???ææææ"+
            "¡¡¡55555555555\b\b\b\b\bæææææFFFFFFFFFF555\b\b\b\b\b\b\bæææææFFFFFFFFFFFF6\b\b\b\b\b\b\bææææFFFFFFFFFFF¦¦6\b\b\b\b\b\b|||çççæ"+
            "§§§§§§$$$¦¦¦¦¦\t\t\t\b\b\b|||çççç§§§§§§§§§§$¦\t\t\t\t\t\t\t\t|||çççç§§§§§§§§§§§EEE\t\t\t\t\t\t\n\n\n}ççç§§§§§§§LLLLEEEEEE\t\n\n\n\n\n\nq}}}}}}ç"+
            "LLLLLLLLLLLL111111\n\n\n\n\nqq/}}}}}}¨¨¨¨¨¨LLLLLL1111111\n\n\n\nqqppp}}}}¨¨¨¨¨¨¨¨¨¨MMMMM11111\013\013\013\013\013ppppÕÕÕ¨¨¨¨¨¨¨¨¨MMMMMMMMdd1\013\013\013\013\013ppppÕÕÕ"+
            "¨¨¨¨¨¨¨¨MMMMMMMRRRRRR\f\f\f\fooooÕÕÕ¨¨¨¨¨¨¨¨MMMMMRRRRRRRR\f\f\f\f\foooonÕ®®®®®®®®®NNNNNNRRRRRRR\f\f\f\f\foonnn®®®®®®®®­­­NNNNNNN````ll\f\f\foÄnnn"+
            "®®®®®­­­­­­­­NNNN````QQllllÄÄÄÄÄ­­­­­­­­­­­­­­­````QQQQQllll\023\023\023\023­­­­­­­­­­­­­­­­`QQQQQ...bbb\023\023\023\023­­­­­­­­­­­­­­­­­QQQ.....bbb\023\023\023\023"+
            "ÿÿÿÿþþööööööööïïïïïïãîîîîÿÿÿþþþööööööööïïïïïïãîîîîÿÿÿþþþööööööööïïïïïïîîîîîþþþþþööööööööïïïïïïîîîîî"+
            "þþþþþþöööööööïïïïïïîîîîîþþþþþþþ÷ööööööïïïïïïîîîîîþþþþþþþöööööïïïïïïäîîîî¢¢¢¢¢¢¢¢??????öïïïïäääîîî"+
            "¢¢¢¢¢¢¢¢???????ääääîî¢¢¢¢¢¢¢¢5??????ääääî¢¢¢¢¢¢55555?????äææææ¡¡¡¡¡55555555?@@@@æææææ"+
            "¡¡¡¡¡¡5555555@@@@@@ææææææ¡¡¡¡¡¡FF555666@@\b\b\bææææææFFFFFFFFFF66666\b\b\b\b\bæææææ$$$$$$$$$$$666666\b\b\bçççç"+
            "§$$$$$$$$$$$$66666\b\b|çççç§§§§§§§$$$$$EE\t\t\t\t\tAçççç§§§§§§§§§EEEEEEE\t\t\n\n\n\n}çççç§§§§§§§§LEEEEEEEE\n\n\n\n\n\n\n}}}}}}}ç"+
            "LLLLLLLLLLLEEEEEE\n\n\n\n\n\n///}}}}}}¨¨¨¨¨¨¨¨LLLM111111\n\n\n\n\013\013/ppp}}}}¨¨¨¨¨¨¨¨¨MMMMMM1111\013\013\013\013\013\013ppppÕÕÕ¨¨¨¨¨¨¨¨MMMMMMMMMRR\013\013\013\013\013\013pppÕÕÕÕ"+
            "¨¨¨¨¨¨¨¨MMMMMMMRRRRR\f\f\f\f\f\fooÕÕÕÕ¨¨¨¨¨¨¨MMMMMRRRRRRRRR\f\f\f\f\fooonnn®®®®®®®®NNNNNNNNRRRRR\f\f\f\f\f\fonnnn®®®®®®®­­­NNNNNNNNNNQQl\f\f\f\f\rnnnn"+
            "­­­­­­­­­­­­NNNNNNNQQQQllllÄÄÄmn­­­­­­­­­­­­­­NNNQQQQQQ.bbb\023\023\023\023m­­­­­­­­­­­­­­¬¬QQQQQ...bbbb\023\023\023m­­­­­­­­­­¬¬¬¬¬¬QQQQ.....bbb\023\023\023\023"+
            "ýýýýýý÷÷÷ööööööïïïïïïîîîîîýýýýýý÷÷÷ööööööïïïïïïîîîîîýýýýýý÷÷÷ööööööïïïïïïîîîîîýýýýýý÷÷÷÷öööööïïïïïïîîîîî"+
            "ýýýýýýý÷÷÷÷öööööïïïïïïîîîîîýýýýýýýý÷÷÷÷÷ööööïïïïïïîîîîî¢¢¢¢ýýýýýýý??÷÷÷÷÷öööïïïïïïîîîîî¢¢¢¢¢¢¢ý??????÷÷÷ïïïïääîîîî"+
            "¢¢¢¢¢¢¢¢???????ääîîîî¢¢¢¢¢¢¢¢5??????äææîî¡¡¡¡¡¡¡5555???@@@ææææææ¡¡¡¡¡¡¡¡5555@@@@@@ææææææ"+
            "¡¡¡¡¡¡¡¡¡55@@@@@@@@æææææææ¡¡¡¡¡¡¡¡¡66666@@@@@ææææææ$$$$$$$$$666666666@æææææ$$$$$$$$$$$66666666ççççç"+
            "$$$$$$$$$$$$$66666AAççççç§§§$$$$$$$$$EEEEAAAAAççççç§§§§§§§§EEEEEEEEE\n\nAAA}}çççç§§§§§§§§EEEEEEEEE\n\n\n\n\n///}}}}}çç"+
            "¨¨¨¨¨LLLLEEEEEEEE\n\n\n\n\n////}}}}}}¨¨¨¨¨¨¨¨)))MM1110000\013\013\013////}}}}}¨¨¨¨¨¨¨¨)MMMMMMM0000\013\013\013\013ppppÕÕÕÕ¨¨¨¨¨¨¨¨MMMMMMMMM00\013\013\013\013\013\013pppÕÕÕÕ"+
            "¨¨¨¨¨¨¨MMMMMMMMRRRRR\f\f\f\f\f\fooÕÕÕÕMMMNNRRRRRRR\f\f\f\f\f\foonnnn®®NNNNNNNNNNNRRR\f\f\f\f\f\fnnnnn®®®®­­­­NNNNNNNNNNNNQQ\f\f\f\r\r\rnnnn"+
            "¬¬¬¬¬¬¬¬¬¬¬¬NNNNNNQQQQQllb\023\023mmm~¬¬¬¬¬¬¬¬¬¬¬¬¬¬NNQQQQQQ..bbb\023\023mmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬QQQQQ...bbbb\023\023mm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬QQQ.....bbbb\023\023\023m"+
            "ýýýýýýýýýýýý÷÷÷÷÷÷ööïïïïïïîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷ööïïïïïïîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷ööïïïïïïîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷öïïïïïïîîîîîî"+
            "ýýýýýýýýýýýý÷÷÷÷÷÷÷öïïïïïïîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷ïïïïïîîîîîî¢¢¢ýýýýýýýýý?÷÷÷÷ïïïîîîîîî"+
            "¡¡¡¡¢¢¢ýý??????äîîîîî¡¡¡¡¡¡¡¡5?????@æææííí¡¡¡¡¡¡¡¡¡5@@@@@@@ææææææí¡¡¡¡¡¡¡¡¡¡@@@@@@@@æææææææ"+
            "¡¡¡¡¡¡¡¡¡¡@@@@@@@@@æææææææ¡¡¡¡¡¡¡¡666666@@@@ææææææ$$$$$$$$6666666666æææææ$$$$$$$$$$$6666666ççççç"+
            "$$$$$$$$$$$$$666AAAAççççç$$$$$$$$$$$$EEEAAAAAAAççççç§§§§$$$$$EEEEEEEAAAAAAA}}}çççç§§§§§§EEEEEEEEEEE\n\n\n\n////}}}}}çè"+
            "¨¨¨))))))EEEEEEE0000\013/////}}}}}è¨¨¨¨)))))))))0000000\013\013\013////}}}Õè¨¨¨¨)))))))))MM00000\013\013\013\013//ppÕÕÕÕ¨¨¨¨)))))))MMMMM0000\013\013\013\013\013ppÕÕÕÕÕ"+
            "))))MMMMMRRRRR\f\f\f\f\f\f\fonÕÕÕéNNNNNRRRRR\f\f\f\f\f\f\rnnnnnNNNNNNNNNNR\f\f\f\f\r\r\rnnnnn¬¬¬¬¬¬¬¬¬NNNNNNNNNNQQQ\r\r\r\r\r\rnnn~"+
            "¬¬¬¬¬¬¬¬¬¬¬¬NNNNNQQQQQQbbb\rmmmm~¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬NQQQQQQ.bbbb\023mmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOQQ....bbb\023\023mmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOO.....bbbb\023\023mm"+
            "ýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïîîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïîîîîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïîîîîîîîýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïîîîîîî"+
            "ýýýýýýýýý÷÷÷÷÷÷÷ïïïïïïîîîîîîýýýýýýýý÷÷÷÷÷÷ïïïï\032îîîîîîýýýýýýý÷÷÷÷÷ïïï\032\032îîîîîýýýýýý÷÷÷÷\032\032\032ííííí"+
            "¡¡¡    \032\032\032ííííí¡¡¡¡¡¡@@@æææíííí¡¡¡¡¡¡¡@@@@@ææææææíí¡¡¡¡¡¡¡¡@@@@@@@ææææææææ"+
            "¡¡¡¡¡¡¡¡¡@@@@@@@@ææææææë¡¡¡¡¡¡¡¡666666@@@ææææëë$$$$$$$$666666666ææëëë$$$$$$$$$$6666666çççëë"+
            "$$$$$$$$$$$$$6AAAAAAççççç$$$$$$$$$$$$$AAAAAAAAAççççç$$$$$$$$$EEEEEEAAAAAAAA}}çççèèEEEEEEEE0AAAA///}}}}èèè"+
            ")))))))))))EE0000000//////}}}èèè)))))))))))))0000000\013\013/////}}Õèè))))))))))))))000000\013\013\013////ÕÕééé)))))))))))))))00000\013\013\013\013/ppÕÕééé"+
            ")))))RRRR0\f\f\f\f\f\r\rnnéééNNNNRRR\f\f\f\r\r\r\r\rnnnnéNNNNNNNNN\r\r\r\r\r\r\rnnnn~¬¬¬¬¬¬¬¬¬¬NNNNNNNNQQQ\r\r\r\r\r\r\rn~~~"+
            "¬¬¬¬¬¬¬¬¬¬¬¬¬NNNOOOOQQbbb\rmmmm~~¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOOOO..bbbbmmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOOO..bbbbmmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOO...bbbbbmmaa"+
            "ýýýýýýýý÷÷÷÷÷÷÷÷ïïïï\032\032îîîîîîýýýýýýýý÷÷÷÷÷÷÷÷ïïïï\032\032îîîîîîýýýýýýý÷÷÷÷÷÷÷ïïïï\032\032îîîîîîýýýýýýý÷÷÷÷÷÷÷ïïï\032\032\032îîîîí"+
            "ýýýýýý÷÷÷÷÷÷ïï\032\032\032\032íííííýýýý ÷÷÷÷÷ï\032\032\032\032ííííí     ÷÷÷\032\032\032\032ííííí      ÷\032\032\032\032ííííí"+
            "    \032\032\032\032ííííí¡  \032\032\032\032íííí¡¡¡@@@æææææííí¡¡¡¡@@@@ææææææëë"+
            "      @@@@ææææëëë         66666@æëëëëë         6666666ëëëëë$$$$$$$$$7777777Aççëëë"+
            "$$$$$$$$$$$777AAAAAAçççç\034$$$$$$$$$$$$AAAAAAAAAAçççè\034EEEAAAAAAAAAèèèèèEEE000AAAAèèèè"+
            "))00000000B///èèèè)))))))))))))0000000BBB////éééèè))))))))))))))000000\013BBBB/ééééé)))))))))))))))00000DDDDBééééé"+
            "))&&&&&DDDDDDD\rééé&&&&&&DDD\r\r\r\r\rnnéNNNN&&&&\r\r\r\r\r\r\rnn~~~NNNNNOOOOO\r\r\r\r\r\r\r~~~~"+
            "¬¬¬¬¬¬¬¬¬¬¬¬¬©OOOOOOOObb\r\rmmmm~~¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOOOOOObbbbmmmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOOOOO.bbbPmmmaa¬¬¬¬¬¬¬¬¬¬¬¬¬¬OOOOOOO..bbPPPaaaa"+
            "ýýý  ÷÷÷÷÷÷ü\033\033\033\033\032\032\032íííííýý   ÷÷÷÷÷÷ü\033\033\033\033\032\032\032ííííí    ÷÷÷÷÷üü\033\033\033\033\032\032\032ííííí     ÷÷÷üüü\033\033\033\032\032\032ííííí"+
            "      ÷üüüü\033\033\033\032\032\032ííííí       üüüü\033\033\032\032\032\032ííííí       üüü\033\032\032\032\032ííííí        üü\033\032\032\032\032ííííí"+
            "      \032\032\032\032ííííí   ø\032\032\032\032ííííûøøøø\032\032ííìì   @@øøøøøëëëëì"+
            "       @ëëëëëëë          77777ëëëëëë         7777777ëëëëë        777777777ëëëëë"+
            "$$$$$$$$7777777AAAAAç\034\034\034\0347777AAAAAAAAè\034\034\034\03488888AAAAAAèèèèè888888999èèèèè"+
            "00000999BBBèèèè))))))))))))0000000BBBBBBééèè)))))))))))))000000BBBBBBéééé)))))))))&&00DDDDDDDBéééé"+
            "&&&&&&&DDDDDDDééé&&&&&&&DDDD\r\r\ré&&&&&&&&\r\r\r\r\r\r\r~~~~\177ªªªªªªªªªªª©©©©OOOOO\r\r\r\r\r\r\r~~~~\177"+
            "¬¬¬¬¬¬¬¬¬¬©©©©©OOOOOOO\r\r\rmmmm~\177\177¬¬¬¬¬¬¬¬¬¬¬©©©©OOOOOOOPPPPmmmaaa¬¬¬¬¬¬¬¬¬¬¬¬©©OOOOOOOOPPPPPaaaaa¬¬¬¬¬¬¬¬¬¬¬«««OOOOOO'''PPPPPaaaa"+
            "     üüüüüüüü\033\033\033\033\032\032\032ííííí     üüüüüüüü\033\033\033\033\032\032\032ííííí     üüüüüüüü\033\033\033\033\032\032\032ííííí     üüüüüüüü\033\033\033\033\032\032\032ííííí"+
            "       üüüüüüü\033\033\033\033\032\032\032ííííí       üüüüüüü\033\033\033\033\032\032\032ííííí       üüüüüüü\033\033\033\033\032\032\032ííííí         üüüüüü\033\033\033\032\032\032ííííí"+
            "      ûûûûûûûøøø\032\032\032ííííí  ûûûûûûøøøøø\032\032ììììûûûûûøøøøøøììììì     ûûûûûûøøøøëëëììì"+
            "        ùøëëëëëëë         777777ùùëëëëëë        77777777ùëëëëëë        777777777ùëëëëë"+
            "      7777777777AAAA\034\034\034\034\034\03488888888AAAA\034\034\034\034\034\0348888888AAèèèè\034\034888889999èèèèè"+
            "8999999BBBèèèè99999BBBBBBéèè))))))))))00999BBBBBéééê))&&&&&&:::DDDDDéééê"+
            "&&&&&&&&DDDDDDDééê&&&&&&&&&DDDDD\r\177\177&&&&&&&&&D\r\r\r\r\r~\177\177\177\177ªªªªªªªªªª©©©©©©&&&&\r\r\r\r\r\r<~\177\177\177\177"+
            "ªªªªªªªªª©©©©©©©OOOOOPPPP<<<<\177\177\177ª«««««««««©©©©©©OOOO'PPPPPPaaaaa«««««««««««««©©©O'''''PPPPP\016aaaa((((((((«««««««'''''''PPPPPP\016aaa"+
            "\037\037\037\037\037\037\037\037\037  üüüüüüüüü\033\033\033\033\033\032\032ííííí\037\037\037\037\037\037\037\037    üüüüüüüü\033\033\033\033\033\032\032ííííí\037\037\037\037\037\037\037\037    üüüüüüüü\033\033\033\033\033\032\032ííííí\037\037\037\037\037\037\037\037    üüüüüüüü\033\033\033\033\033\032\032ííííí"+
            "\037\037\037\037\037\037\037\037    üüüüüüüü\033\033\033\033\033\032\032ííííí\037\037\037\037\037\037\037     üüüüüüüü\033\033\033\033\033\032\032ííííí\037\037\037      üüüüüüüø\033\033\033\032\032\032ííííí      ûûûûûûûøøøøø\032\032íííìì"+
            "   ûûûûûûøøøøøøøìììììûûûûûûøøøøøøìììììûûûûûøøøøøìììììì     ûûûûûøøøøøëììììì"+
            "     ûûùùùùùùëëëëìì     77\036\036ùùùùùëëëëëë     777777\036\036\036\036\036ùùùùùëëëëë     7777777\036\036\036\036\036ùùùù\034\034\034ëë"+
            "%%%%%%%%77777777\036\036\036\036\036\036úúù\034\034\034\034\034\034\034%%%%%%%%8888888888\036\036úúú\034\034\034\034\034\03488888888889úè\034\034\034\034\034888889999úèèèè\034"+
            "9999999BBèèèè99999BBBBBêêê::::::BBBBêêê&&&&&&&::::::DDDéêêê"+
            "&&&&&&&&&::DDDDDêêê&&&&&&&&&&DDDDDêªªªªªªªªªª&&&&&&&&&;;;;;;CCC\177\177\177ªªªªªªªªª©©©©©©©&&&;;;;;<<<<\177\177\177\177"+
            "ªªªªªªªª«©©©©©©©©''''PPP<<<<<\177\177\177««««««««««««©©©©'''''PPPPP<<\016aa\017(((((«««««««««©'''''''PPPPP\016\016\016a\017((((((((((««««''''''''PPPPP\016\016\016\016\017"+
            "\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\033\032ííííí\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\033\032ííííí\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\033\032ííííí\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\032\032ííííí"+
            "\037\037\037\037\037\037\037\037\037\037\037 üüüüüüüü\033\033\033\033\033\032\032íííìì\037\037\037\037\037\037\037\037  üüüüüûûûøøø\033\033\032\032íìììì\037\037\037\037\037 ûûûûûûûûøøøøøø\032ììììì\037ûûûûûûûøøøøøøìììììì"+
            "ûûûûûûøøøøøøììììììûûûûûøøøøøøììììììûûûûûûøøøøøììììììûûûûùùùøøøìììììì"+
            "ûûùùùùùùùëëìììì\036\036\036\036\036ùùùùùùùëëëëì77\036\036\036\036\036\036\036ùùùùùùëëëëë%%%%%%777\036\036\036\036\036\036\036\036ùùùùù\034\034\034\034\034"+
            "%%%%%%%%%%88888\036\036\036\036\036\036úúúù\034\034\034\034\034\034\034%%%%%%%%%8888888\036\036\036\036úúúúúú\034\034\034\034%%%%%%8888888899úúúúúúú888899999úúúúúúú"+
            "9999999BBúèèè999999BB\035\035êêêê::::::::B\035\035\035êêêê&&&&&&&::::::::\035\035\035\035\035êêêê"+
            "&&&&&&&&&&:::::DDCCCCCCêêêªªªªªªªª&&&&&&&&&&:;;;;;CCCCCªªªªªªªªªª&&&&&&&&;;;;;;;CCCªªªªªªªªª©©©©©©©&&;;;;;;<<<<\177"+
            "ª«««««««««©©©©©©'''';;P<<<<<<\177\177««««««««««««©©©''''''PPPP<<<\016\017\017\017((((((((««««««''''''''PPPP\016\016\016\016\017\017((((((((((((««''''''''PPPP\016\016\016\016\017\017"+
            "\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\033\032ììììì\037\037\037\037\037\037\037\037\037\037\037üüüüüüüü\033\033\033\033\033\033\033\032ììììì\037\037\037\037\037\037\037\037\037\037\037üüüüüüüûøø\033\033\033\033\033ìììììì\037\037\037\037\037\037\037\037\037\037\037\037üüüüûûûøøøø\033\033\033ìììììì"+
            "\037\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûøøøøøøøìììììì\037\037\037\037\037\037\037ûûûûûûûøøøøøøøìììììì\037\037\037ûûûûûûûøøøøøøøììììììûûûûûûûøøøøøøìììììì"+
            "ûûûûûûøøøøøøììììììûûûûûøøøøøìììììììûûûûûøøøøøìììììììûûûùùùùùùììììììì"+
            "\036\036\036ùùùùùùùùììììì\036\036\036\036\036\036\036ùùùùùùùëëììì%%%\036\036\036\036\036\036\036\036ùùùùùùëëë%%%%%%%%\036\036\036\036\036\036\036\036\036ùùùùù"+
            "%%%%%%%%%%%88\036\036\036\036\036\036\036\036úúúù%%%%%%%%%%88888\036\036\036\036úúúúúú%%%%%%%%88888899úúúúúúúú8899999úúúúúúúú"+
            "9999999úúú\035\035\035ê::::::\035\035\035\035\035\035\035êêêêê::::::::\035\035\035\035\035\035êêêêê&&&&&:::::::::\035\035\035\035\035êêêêê"+
            "ªªªª&&&&&&&&&&&::::::;;CCCCCCêêêªªªªªªª&&&&&&&&&&;;;;;;;CCCCªªªªªªªªª©&&&&&&&;;;;;;;CCCCªªªªªªªª©©©©©©©©&;;;;;;;<<<<"+
            "«««««««««««©©©©''''';;;<<<<<<\017\017(((((««««««««©'''''''PPP<<<<\017\017\017\017((((((((((«««''''''''PPPPP\016\016\017\017\017\017((((((((((((('''''''''PPPP\016\016\017\017\017\017"+
            "\037\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûøøøøøøììììììì\037\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûøøøøøøììììììì\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûøøøøøøììììììì\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûøøøøøøììììììì"+
            "\037\037\037\037\037\037\037\037ûûûûûûûûøøøøøøììììììì\037\037\037\037ûûûûûûûøøøøøøììììììì\037ûûûûûûûøøøøøøìììììììûûûûûûøøøøøøììììììì"+
            "ûûûûûøøøøøøìììììììûûûûûûøøøøøìììììììûûûûùùùùùøìììììììûûùùùùùùùùìììììì"+
            "\036\036\036\036\036ùùùùùùùùììììì\036\036\036\036\036\036\036ùùùùùùùùììììì%%%%%%\036\036\036\036\036\036\036\036ùùùùùùù%%%%%%%%%%\036\036\036\036\036\036\036\036\036ùùùù"+
            "%%%%%%%%%%%%\036\036\036\036\036\036\036\036úúúú%%%%%%%%%%%888\036\036\036\036\036úúúúúú%%%%%%%%%%888888\036úúúúúúúú89999úúúúúúúú"+
            "99999\035\035\035\035\035\035\035\035êê::::::\035\035\035\035\035\035\035\035êêêêê::::::::\035\035\035\035\035\035\035êêêêê&&&&:::::::::\035\035\035\035\035\035êêêêê"+
            "ªªª&&&&&&&&&&&&::::;;;CCCCCCêêêªªªªªªª&&&&&&&&&;;;;;;;CCCCCªªªªªªªª©©&&&&&&;;;;;;;;CCCªªª««««««©©©©©©';;;;;;;;<<<"+
            "««««««««««««©©''''';;;;<<<<<\017\017\017\017((((((((«««««''''''''PPP<<<\017\017\017\017\017((((((((((((«''''''''PPPP\016\016\017\017\017\017\017((((((((((((('''''''''PPP\016\016\017\017\017\017\017"
            ;

//            "\001\001\001\001\001\001\001\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÞÝÝÝ\025\025\025\001\001\001\001\001\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÞÝÝÝ\025\025\025\002\002\002\002\002\002\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\002uuuuu\030\030\030ôóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "WWWWWWvvàààààóóóßßßßßÞÞÞÞÝÝÝÝÝ\025\025ggggg\003\003\003ÊÊÊÊÊÊÊËËñßßßááááÝÝÝÝÝ\025\025²²²ggggggÊÊÊÊÊËËËËËññááááÝÝÝÝÜÜÜ²²²²½½½½½½\004wwËËËËËËËÌ\026\026\026\026\026\026âÜÜÜÜ"+
//            "³³³³³³hhhhhhwwwËËÌÌÌÌÌ\026\026\026\026\026\026ÜÜÜÜ³³³³³³XXXhhhhhxxxÌÌÌÌÌÌ\026\026\026\026\026ÎÎÎÜ´´´XXXXXXXXÉÉttttxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´´YYÉÉÉÉÉttttÍÍyyÏÏÏÏÎÎÎÎÎÎ"+
//            "´´´´´´fffffÉÉÉÉttÍÍÍÍÏÏÏÏÏÏÐÎÎÚÚ±±±±±ffffffffiiiiÍÍÍÍÏÏÏÏÏÐÐÐÐ×Ú±±±±±±±±[[[[[iiiiiisssÏÏÏÐÐÐÐÐ××++++++++[[[[[\020\020\020\020\020\020ssssØØØÐÐÐ×××"+
//            "+++++++¼¼¼¼¼¼\020\020\020\020\020\020\020sssØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020\02033rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾¾3rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾¾jÈÈÈÈÈÈÈÒÒ"+
//            "µµµµµµµ»»»»»»¾¾¾¾¾¾¾¾jjjjÈÈÈÈÈÒÒ»»»»»»»»»»»»»»»»¾¾¾]jjjjjjÑÈÈÈÒÒº»»»»»»»»»»»»»»¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖºººººº,,,,,»»»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ººººººººº,,,,,¿¿¿¿¿¿¿¿\021\021\021\021\021\021ÖÖÖÖ¶¶¶ººººººº,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021\021ÇÇÖÖÔ¶¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀ¿¿¿¿ÇÇÇÇÇÇÇÇÆÆ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅ"+
//            "··········ÀÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÇÅÅ·········¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇ\022\022Å---------¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÁÁÃÃÃ\022\022\022\022-----------¹¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022"+
//            "\001\001\001\001\001\001\001\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÞÝÝÝ\025\025\025\001\001\001\001\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÞÝÝÝ\025\025\025\002\002\002\002\002\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\002uuuuu\030\030ôôóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "WWWWWWvvàààààóóóßßßßßÞÞÞÞÝÝÝÝÝ\025\025ggggg\003\003\003ÊÊÊÊÊÊËËËññßßááááÝÝÝÝÝ\025\025²²²ggggggÊÊÊÊËËËËËËññááááÝÝÝÝÜÜÜ²²²²½½½½½½\004wwËËËËËËËÌ\026\026\026\026\026\026âÜÜÜÜ"+
//            "³³³³³³hhhhhhwwwËËÌÌÌÌÌ\026\026\026\026\026\026ÜÜÜÜ³³³³³³XXXhhhhhxxxÌÌÌÌÌÌ\026\026\026\026ÎÎÎÎÜ´´´XXXXXXXXÉÉttttxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´´YYÉÉÉÉÉttttÍÍyyÏÏÏÏÎÎÎÎÎÚ"+
//            "´´´´´´fffffÉÉÉÉttÍÍÍÍÏÏÏÏÏÏÐÎÎÚÚ±±±±±ffffffffiiiiÍÍÍÍÏÏÏÏÏÐÐÐÐ×Ú±±±±±±±±[[[[[iiiiiisssÏÏÏÐÐÐÐ×××++++++++[[[[[\020\020\020\020\020\020ssssØØØÐÐÐ×××"+
//            "+++++++¼¼¼¼¼¼\020\020\020\020\020\020\020sssØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020\02033rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾¾3rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾¾jÈÈÈÈÈÈÈÒÒ"+
//            "µµµµµµµ»»»»»»¾¾¾¾¾¾¾jjjjjÈÈÈÈÈÒÒ»»»»»»»»»»»»»»»»¾¾]]jjjjjjÑÈÈÈÒÒº»»»»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021\021ÖÖÖÖºººººº,,,,,»»»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ºººººººº,,,,,,¿¿¿¿¿¿¿¿\021\021\021\021\021\021ÖÖÖÖ¶¶¶ººººººº,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021\021ÇÇÖÖÔ¶¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀ¿¿¿¿ÇÇÇÇÇÇÇÇÆÆ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅ"+
//            "··········ÀÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÇÅÅ·········¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇ\022\022Å---------¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÁÁÃÃÃ\022\022\022\022-----------¹¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022"+
//            "\001\001\001\001\001\001\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÞÝÝÝ\025\025\025\001\002\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\002\002\030\030\030\030\030ó\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\002uuuuuôôôôóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "WWWWWWvvàààààóóóßßßßßÞÞááÝÝÝÝÝ\025\025ggggg\003\003\003ÊÊÊÊÊÊËËËñññßááááÝÝÝÝÝ\025\025²²²ggggggÊÊÊÊËËËËËËññááááÝÝÝâÜÜÜ²²²²½½½½½½\004wwËËËËËËËÌ\026\026\026\026\026\026âÜÜÜÜ"+
//            "³³³³³³hhhhhhwwwËËÌÌÌÌÌ\026\026\026\026\026\026ÜÜÜÜ³³³³³³XXXhhhh\005xxxÌÌÌÌÌÌ\026\026\026\026ÎÎÎÎÜ´´´XXXXXXXÉÉÉttttxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´´YYÉÉÉÉÉttttÍÍyyÏÏÏÏÎÎÎÎÎÚ"+
//            "´´´´´´fffffÉÉÉÉttÍÍÍÍÏÏÏÏÏÏÐÎÎÚÚ±±±±±ffffffffiiiiÍÍÍÍÏÏÏÏÏÐÐÐ××Ú±±±±±±±±[[[[[iiiiissssÏÏÏÐÐÐÐ×××++++++++[[[[[\020\020\020\020\020\020ssssØØØÐÐÐ×××"+
//            "+++++++¼¼¼¼¼¼\020\020\020\020\020\020\020sssØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020\02033rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾¾3rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾¾jÈÈÈÈÈÈÈÒÒ"+
//            "µµµµµµµ»»»»»»¾¾¾¾¾¾¾jjjjjÈÈÈÈÈÒÒ»»»»»»»»»»»»»»»»¾]]]jjjjjjÑÈÈÈÒÒ»»»»»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖºººººº,,,,,»»»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ºººººººº,,,,,,¿¿¿¿¿¿¿¿\021\021\021\021\021\021ÖÖÖÖ¶¶¶¶ºººººº,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021\021ÇÇÖÖÔ¶¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀ¿¿¿¿ÇÇÇÇÇÇÇÇÆÆ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅ"+
//            "··········ÀÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÇÅÅ·········¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇ\022\022Å---------¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÁÁÃÃÃ\022\022\022\022-----------¹¹¹¹¹ÂÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022"+
//            "\001\001\001\001\030\030\030\030\030\030\027\027\027\027\027ßßßÞÞÞÞÝÝÝÝ\025\025\025\002\030\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\002\030\030\030\030\030óó\027ßßßßßÞÞÞÞÝÝÝÝ\025\025\025WW\002uuuuvuôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "WWWWW\003vvàààààóóóßßßßßááááÝÝÝÝÝ\025\025ggggg\003\003\003ÊÊÊÊÊÊËËËñññáááááÝÝÝÝâ\025\025²²²gggggÊÊÊÊÊËËËËËËññááá\026ÝÝÝâÜÜÜ²²²²½½½½½\004\004wwËËËËËËËÌ\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "³³³³³³hhhhhhwwwËËÌÌÌÌÌ\026\026\026\026\026\026ÜÜÜÜ³³³³³³XXXhhh\005xxxxÌÌÌÌÌ\026\026\026\026\026ÎÎÎÎÜ´´´XXXXXXXÉÉÉttttxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´´YYÉÉÉÉÉttttÍyyyÏÏÏÏÎÎÎÎÎÚ"+
//            "´´´´´´fffffÉÉÉÉtÍÍÍÍÍÏÏÏÏÏÏÐÎÎÚÚ±±±±±±fffffffiiiiÍÍÍÍÏÏÏÏÏÐÐÐ××Ú±±±±±±±±[[[[[iiiiissssÏÏÏÐÐÐÐ×××++++++++[[[[[\020\020\020\020\020\020ssssØØØÐÐÐ×××"+
//            "+++++++¼¼¼¼¼¼\020\020\020\020\020\020\020sssØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020\02033rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾¾3rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾jjÑÈÈÈÈÈÈÒÒ"+
//            "µµµµµµ»»»»»»»¾¾¾¾¾¾¾jjjjjÈÈÈÈÈÒÒ»»»»»»»»»»»»»»»»]]]jjjjjjjÑÈÈÖÒÒ»»»»»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖºººººº,,,,,,»»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ºººººººº,,,,,,¿¿¿¿¿¿¿¿\021\021\021\021\021\021ÖÖÖÖ¶¶¶¶ºººººº,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021ÇÇÇÖÖÔ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀ¿¿¿¿ÇÇÇÇÇÇÇÇÆÆ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅ"+
//            "··········ÀÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ·········¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇ\022ÅÅ---------¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÁÁÃÃÃ\022\022\022\022-----------¹¹¹¹¹ÂÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022"+
//            "\030\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\030\030\030\030\030\030\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002\002\030\030ôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025WWWuuuuuvôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "WWWW\003\003vvvààààóóóßßßßßááááÝÝÝÝ\025\025\025ggggg\003\003\003ÊÊÊÊÊÊËËËñññáááááÝÝÝÝâÜ\025²²²²ggggÊÊÊÊÊËËËËËËññáá\026\026ÝÝââÜÜÜ²²²²½½½½\004\004wwwËËËËËËÌ\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "³³³³³³hhhhhhwwwËÌÌÌÌÌ\026\026\026\026\026\026\026ÜÜÜÜ³³³³³XXXXhh\005\005xxxxÌÌÌÌÌ\026\026\026\026\026ÎÎÎÎÜ´´XXXXXXXXÉÉÉtttxxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´YYYÉÉÉÉÉttttÍyyyÏÏÏÏÎÎÎÎÎÚ"+
//            "´´´´´ffffffÉÉÉttÍÍÍÍÍÏÏÏÏÏÏÐÎÎÚÚ±±±±±±fffffffiiiiÍÍÍÍÏÏÏÏÏÐÐÐ××Ú±±±±±±±[[[[[[iiiiissssÏÏÏÐÐÐÐ×××+++++++[[[[[[\020\020\020\020\020sssssØØØÐÐÐ×××"+
//            "+++++++¼¼¼¼¼e\020\020\020\020\020\020\020ssØØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020\02033rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾¾3rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾jjÑÈÈÈÈÈÈÒÒ"+
//            "µµµµµµ»»»»»»»¾¾¾¾¾¾¾jjjjÑÑÈÈÈÈÒÒ»»»»»»»»»»»»»»»»]]]jjjjjjjÑÈÈÖÒÒ»»»»»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖººººº,,,,,,,»»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ºººººººº,,,,,,¿¿¿¿¿¿¿¿\021\021\021\021\021\021ÖÖÖÖ¶¶¶¶¶ººººº,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021ÇÇÇÖÔÔ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀ¿¿¿ÁÇÇÇÇÇÇÇÇÆÆ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅ"+
//            "··········¹ÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ········¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇ\022\022ÅÅ---------¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÁÃÃÃ\022\022\022\022-----------¹¹¹¹¹ÂÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022"+
//            "\030\030\030\030\030ó\027\027\027\027ßßßßÞÞÞÞÝÝÝÝ\025\025\025\030\030ôôôóóó\027ßßßßßÞÞÞÞÝÝÝÝ\025\025\025\002\002ôôôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025WWvvôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025"+
//            "W\003\003\003\003\003vvvàààÊóóñßßßßáááááÝÝÝÝ\025\025\025ggggg\003\003\003ÊÊÊÊÊËËËññññáááááÝÝÝâÜÜ\025²²²²ggHH\004ÊÊÊËËËËËËËññá\026\026\026\026ââÜÜÜÜ²²²²½½½\004\004\004wwwËËËËËËÌ\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "³³³³³IhhhhhwwwwxÌÌÌÌÌ\026\026\026\026\026\026\026ÜÜÜÜ³³³³³XXXX\005\005\005\005xxxxÌÌÌÌÌ\026\026\026\026\026ÎÎÎÎÛ´XXXXXXXXXÉÉttttxxÌÌÌÌÌ\026\026\026\026ÎÎÎÎÎ´´´´´´YYYYÉÉÉtttttÍyyyÏÏÏÏÎÎÎÎÎÚ"+
//            "´´´´´ffffffZZZttÍÍÍÍÍÏÏÏÏÏÏÐÎÚÚÚ±±±±±±ffffffZiiiiÍÍÍÍÏÏÏÏÏÐÐÐ××Ú±±±±±±±[[[[[[iiiiissssÏÏÏÐÐÐÐ×××+++++++[[[[[e\020\020\020\020\020ssssØØØØÐÐÐ×××"+
//            "++++++¼¼¼¼¼¼ee\020\020\020\020\020\020ssØØØØØØÐ×××µµµµµ¼¼¼¼¼¼¼¼¼¾¾¾\020\020333rrrrrrr×××µµµµµµµµ¼¼¼¼¼¾¾¾¾¾¾¾33rrrrÈÈÈÈÒÒµµµµµµµµµµ¼¼¾¾¾¾¾¾¾¾¾jjÑÈÈÈÈÈÈÒÒ"+
//            "µµµµµ»»»»»»»»¾¾¾¾¾¾¾jjjjÑÑÈÈÈÈÒÒ»»»»»»»»»»»»»»»]]]]jjjjjjjÑÑÈÖÒÒ,»»»»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖººººº,,,,,,,,»¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ººººººº,,,,,,,¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ¶¶¶¶¶¶ººº,,,ÀÀÀ¿¿¿¿¿¿¿\021\021\021\021ÇÇÇÖÔÔ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀ¿¿¿ÇÇÇÇÇÇÇÇÆÆÆ····¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÅÅÅ"+
//            "··········¹ÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ········¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇ\022\022ÅÅ--------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022-----------¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃÃ\022\022\022"+
//            "ôôôôôóóó\027ßßßßÞÞÞÞÞÝÝÝÝ\025\025\025ôôôôôôóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025ôôôôóóóóóßßßßßÞÞÞÞÝÝÝÝ\025\025\025vvàôóóóóóßßßßááááÝÝÝÝÝ\025\025\025"+
//            "\003\003\003\003\003\003óóññññßáááááÝÝÝâ\025\025\025HHggg\003ÊÊÊÊËËËññññáááááÝÝââÜÜÜ²²²HHH\004\004\004ËËËËËËñññ\026\026\026\026\026ââÜÜÜÜ²²²²\004\004\004\004\004\004wwwwËËËËËÌ\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "³³³IIIIhhhhwwwxxÌÌÌÌÌ\026\026\026\026\026\026ÎÜÜÜÜ³³³³XXXX\005\005\005\005\005xxxxÌÌÌÌÌ\026\026\026\026\026ÎÎÎÎÛXXXXXXXXXVÉÉttttxxÌÌÌÌÌ\026\026\026ÎÎÎÎÎÎ´´´´´YYYYYÉÉÉttttÍyyyyÏÏÏÏÎÎÎÎÚÚ"+
//            "´´´´ffffffZZZZttÍÍÍÍÍÏÏÏÏÏÐÐÎÚÚÚ±±±±±±±ffffZZiiiiÍÍÍÍÏÏÏÏÏÐÐÐ×ÙÚ±±±±±±±[[[[[iiiiiissssÏÏÏÐÐÐÐ×××+++++++[[[[[e\020\020\020\020\020ssssØØØØÐÐÐ×××"+
//            "++++++¼¼¼¼¼¼ee\020\020\020\020\02033sØØØØØØÐ×××µµµµ¼¼¼¼¼¼¼¼¼\\¾¾¾\020\020333rrrrrrr×××µµµµµµµ¼¼¼¼¼\\¾¾¾¾¾¾¾32rrrrÈÈÈÈÒÒµµµµµµµµµµ¼\\¾¾¾¾¾¾¾¾¾jÑÑÈÈÈÈÈÈÒÒ"+
//            "µµµµµ»»»»»»»»¾¾¾¾¾¾jjjjjÑÑÈÈÈÒÒÒ»»»»»»»»»»»»»»»]]]]jjjjjjjÑÑÈÖÒÒ,,,,»»»»»»»»»»»¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖºººº,,,,,,,,,,¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖ"+
//            "ººººººº,,,,,,,¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÔ¶¶¶¶¶¶¶ºº,,ÀÀÀÀ¿¿¿¿¿¿¿\021\021\021\021ÇÇÇÔÔÔ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀ¿¿ÁÇÇÇÇÇÇÇÇÆÆÆ·······¶ÀÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÅÅÅ"+
//            "·········¹¹¹ÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ·······¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇ\022\022ÅÅ--------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022-----------¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃÃ\022\022\022"+
//            "ôôôôôóóóóóßßßßÞÞÞÞÞÝÝÝÝ\025\025\025ôôôôôóóóóóßßßßÞÞÞÞÝÝÝÝÝ\025\025\025ôôôôóóóóßßßßßÞÞááÝÝÝÝÝ\025\025\025vvàóóóóóßßßßßááááÝÝÝÝâ\025\025\025"+
//            "\003óññññññáááááÝÝâââ\025\025HHHHHHÊËËËñññññáááááÝâââÜÜÜ²²HHHHH\004ËËËËËËññ\026\026\026\026\026\026ââÜÜÜÜ²²²²\004\004\004\004\004wwwwwËËËÌÌ\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "IIIIIIIIhhwwwwxÌÌÌÌ\026\026\026\026\026\026ÎÜÜÜÜ³³³XXXXV\005\005\005\005\005xxxxÌÌÌÌÌ\026\026\026\026\026ÎÎÎÎÛ¤¤¤VVVVYVV\005\005ttttxxyyyy\026\026\026\026ÎÎÎÎÎÚ´´´´YYYYYYYÉtttttÍyyyyÏÏÏÏÎÎÎÎÚÚ"+
//            "±±±±fffffZZZZZ\006tÍÍÍÍÍÏÏÏÏÏÐÐÎÚÚÚ±±±±±±±ff[[ZZiiiÍÍÍÍÍÏÏÏÏÐÐÐÐ×ÙÚ±±±±±±±[[[[[iiiiisssssÏÏØÐÐÐÐ×××++++++U[[[[eee\020\020iissssØØØØÐÐÐ×××"+
//            "+++++¼¼¼¼¼¼eeee\020\020\0203333ØØØØØØÐ×××µµµµ¼¼¼¼¼¼¼¼\\\\¾¾¾¾3333rrrrrrr×××µµµµµµµ¼¼¼¼\\\\¾¾¾¾¾¾¾22rrrrrÈÈÈÒÒµµµµµµµµµ\\\\\\¾¾¾¾¾¾¾¾jjÑÑÑÈÈÈÈÒÒÒ"+
//            "µ°°°»»»»»»»»»¾¾¾¾¾¾jjjjjÑÑÈÈÈÒÒÒ»»»»»»»»»»»»»»]]]]]jjjjjjjÑÑÈÒÒÒ,,,,,»»»»»»»»»¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖººº,,,,,,,,,,¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÖÖ"+
//            "ºººººº,,,,,,,,¿¿¿¿¿¿¿\021\021\021\021\021\021\021ÖÖÖÔ¶¶¶¶¶¶¶¶,,,ÀÀÀÀ¿¿¿¿¿¿¿\021\021\021ÇÇÇkÔÔÔ¶¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀ¿¿ÁÁÇÇÇÇÇÇÇÇÆÆÆ········ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇÇÇÇÅÅÅ"+
//            "·········¹¹¹¹ÀÀÀÁÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ·······¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇ\022\022ÅÅ--------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022----------¹¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃÃ\022\022\022"+
//            "ôôôôôóóóóóßßßßßÞÞááÝÝÝÝ\025\025\025\025ôôôôóóóóóßßßßßááááÝÝÝÝ\025\025\025\025ôóóóóóóßßßßßááááÝÝÝÝâ\025\025\025õóóóññññßáááááÝÝÝââ\025\025\025"+
//            "òòññññññáááááÝââââ\025\025HHHHHHËËËñññññáááááââââÜÜÜHHHHHH\004ËËËËËñññ\026\026\026\026\026âââÜÜÜÜIIII\004\004\004\004\004wwwwwÌ\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "IIIIIIII\005\005wwwxxÌÌÌ\026\026\026\026\026\026\026ÎÜÜÜÛ¤¤VVVVV\005\005\005\005\005xxxxxÌÌÌÌ\026\026\026\026\026\026ÎÎÎÛÛ¤¤¤¤VVVVVV\005\005ttttxyyyyy\026\026\026\026ÎÎÎÎÎÚ¤¤YYYYYYYYY\006tttttyyyyyÏÏÏÏÎÎÎÎÚÚ"+
//            "±±±±±ffZZZZZZ\006\006ÍÍÍÍÍÏÏÏÏÏÏÐÐÚÚÚÚ±±±±±±±[[[ZZZiiiÍÍÍÍÏÏÏÏÏÐÐÐÐÙÙÙ±±±±±±[[[[[[iiiiisssssÏØØÐÐÐÐ××Ù++++UUUUUUeeee\020iisssssØØØØÐÐÐ×××"+
//            "++++¼¼¼¼UUUeeee\020\02033333ØØØØØØÐ×××µµµµ¼¼¼¼¼¼\\\\\\\\\\¾¾33333rrrrrrr×××µµµµµµµ¼¼\\\\\\\\\\¾¾¾¾¾322rrrrrÈÈÒÒÒµµµµµµµµµ\\\\\\¾¾¾¾¾¾¾¾22ÑÑÑÈÈÈÈÒÒÒ"+
//            "°°°°°°°°»»»»¾¾¾¾¾¾¾jjjjjÑÑÑÈÈÒÒÒ°°°»»»»»»»»»»»]]]]]jjjjjjjÑÑÖÒÒÒ,,,,,,,»»»»»»»¿¿¿¿^^\021\021\021\021\021\021\021ÖÖÖÖÖ,,,,,,,,,,,,,¿¿¿¿¿¿^\021\021\021\021\021\021\021ÖÖÖÖÖ"+
//            "ººººº,,,,,,,,¿¿¿¿¿¿¿^\021\021\021\021\021\021\021ÖÖÔÔ¶¶¶¶¶¶¶¶,,,ÀÀÀ¿¿¿¿¿¿^\021\021\021\021ÇÇÇkÔÔÔ¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀ¿ÁÁÁÇÇÇÇÇÇÇÇÆÆÆ········ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅÅ"+
//            "········¹¹¹¹¹¹¹ÀÁÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅ······¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇ\022\022ÅÅ-------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022----------¹¹¹¹¹¸ÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022"+
//            "ôôôóóóóóóßßßßáááááÝÝÝâ\025\025\025\025ôôóóóóóóßßßßáááááÝÝÝââ\025\025\025õõõõóóóóññññßáááááÝÝâââ\025\025\025\"\"\"\"õõõõõõòòñññññáááááÝââââ\025\025\025"+
//            "\"\"\"\"õõõõòòñññññáááááâââââÜÜ\025HHHHòòññññññááááââââÜÜÜÜHHHHH\004ËËñññ\026\026\026\026\026ââÜÜÜÜÜIIIII\004===wwww\026\026\026\026\026\026\026\026ÜÜÜÜÜ"+
//            "IIIIIII\005\005\005\005wxxxÌÌ\026\026\026\026\026\026\026ÎÜÜÛÛ¤¤¤VVVV\005\005\005\005\005xxxxxÌÌÌÌ\026\026\026\026\026ÎÎÎÎÛÛ¤¤¤¤¤VVVVV\005ttttxyyyyyy\026\026\026\026ÎÎÎÎÚÚ¤¤¤¤YYYYYY\006\006\006tttyyyyyyÏÏÏÎÎÎÚÚÚ"+
//            "¥¥¥¥¥¥ZZZZZZZ\006\006ÍÍÍÍÍÏÏÏÏÏÏÐÚÚÚÚ±±±±±±[[[ZZZZiiisssÍÏÏÏÏÏÐÐÐÙÙÙÙ±±±±±±[[[[[[iiiissssszzØØÐÐÐ×××ÙUUUUUUUUUUeeeeeiisssssØØØØÐÐ××××"+
//            "++UUUUUUUUUeeeee333333ØØØØØØ××××µµµ¼¼¼¼¼\\\\\\\\\\\\\\¾¾3333rrrrrrrr×××µµµµµµµ\\\\\\\\\\\\\\¾¾¾¾¾222rrrrrÈÈÒÒÒµµµµµµµµ\\\\\\\\\\¾¾¾¾¾¾222ÑÑÑÑÈÈÈÒÒÒ"+
//            "°°°°°°°°°°»»]]]]]]jjjjjjÑÑÑÈÈÒÒÒ°°°°°°»»»»»»»]]]]]]jjjjjjÑÑÑÖÒÒÒ,,,,,,,,,»»»»»^^^^^jj\021\021\021\021\021\021ÖÖÖÖÖ,,,,,,,,,,,,,¿¿^^^^^\021\021\021\021\021\021\021ÖÖÖÖÖ"+
//            "ºººº,,,,,,,,,¿¿¿^^^^^\021\021\021\021\021\021\021ÖÖÔÔ¶¶¶¶¶¶¯¯¯,ÀÀÀÀ¿¿¿^^^^\021\021\021ÇÇÇkkÔÔÔ¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÇÇÇÇÇÇÇÆÆÆÆ········ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÇÇÇÇÇÇÇÅÅÅ"+
//            "·······¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇÅÅÅÅ····¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÃÃÃ\022\022ÅÅ-------¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022----------¹¹¹¹¸¸¸ÂÂÂÂÂÂÂÃÃÃÃ\022\022\022\022"+
//            "\"\"\"\"\"\"õõõóóóóóñññßßáááááÝââââ\025\025\025\"\"\"\"\"\"õõõõóóóóñññññáááááâââââ\025\025\025\"\"\"\"\"\"õõõõõòòòñññññáááááâââââ\025\025\025\"\"\"\"\"õõõõõõòòòñññññáááááâââââ\025\025å"+
//            "\"\"õõõõòòòñññññáááááâââââÜÜåHHHòòòñññññááááââââÜÜÜÜ££££££ñ\026\026\026\026\026\026ââÜÜÜÜÜ£££££=====ww\026\026\026\026\026\026\026ÜÜÜÜÜÛ"+
//            "IIIIII=====xxxÌÌ\026\026\026\026\026\026ÎÎÜÜÛÛ¤¤¤¤VVV\005\005\005\005\005xxxxxyÌÌÌ\026\026\026\026\026ÎÎÎÎÛÛ¤¤¤¤¤VVVV\005\006ttttxyyyyyy\026\026\026ÎÎÎÎÚÚ¤¤¤¤¤YYYZ\006\006\006\006\006ttyyyyyÏÏÏÎÚÚÚÚ"+
//            "¥¥¥¥¥¥ZZZZZZ\006\006\006ÍÍÍÍÍÏÏÏÏÏÐÐÚÚÚÚ±±±±±±[[ZZZZZiisssssÏÏÏÏÐÐÐÐÙÙÙÙ±±±±±[[[[[[eiiiissssszzØØÐÐÐ××ÙÙUUUUUUUUUUeeeeeisssssØØØØØÐÐ××××"+
//            "UUUUUUUUUUUeeeee333333rØØØØ{××××µµ¼¼¼¼\\\\\\\\\\\\\\\\\\\\33333rrrrrrrr×××µµµµµ\\\\\\\\\\\\\\\\\\\\¾¾¾2222rrrrrÈÈÒÒÒ°°°°°°°°\\\\\\\\\\\\¾¾¾¾2222ÑÑÑÑÈÈÈÒÒÒ"+
//            "°°°°°°°°°°°]]]]]]]jjjjjjÑÑÑÑÒÒÒÒ°°°°°°°°°»»»]]]]]]]jjjjjjÑÑÑÖÒÒÒ,,,,,,,,,,»»»^^^^^]jjj\021\021\021\021qÖÖÖÖÖ,,,,,,,,,,,,,^^^^^^^\021\021\021\021\021\021\021ÖÖÖÖÔ"+
//            "¯¯,,,,,,,,,,,^^^^^^^^\021\021\021\021\021\021kÖÔÔÔ¯¯¯¯¯¯¯¯¯¯¯ÀÀÀ^^^^^^^\021\021\021ÇÇÇkkÔÔÔ¶¶¶¶¶¶¶¶ÀÀÀÀÀÀÀÀÀÁÁÁÁÇÇÇÇÇÇÇÆÆÆÆ·······ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇÇÇÇÅÅÅÅ"+
//            "······¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇÅÅÅÅ··¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÃÃÃ\022\022ÅÅÅ------¹¹¹¹¹¹¹¹¹¹ÂÂÂÂÂÂÂÃÃÃÃÃ\022\022\022\022---------¹¹¹¹¸¸¸¸ÂÂÂÂÂÂÃÃÃÃÃ\022\022\022\022"+
//            "\"\"\"\"\"\"õõõõòòòñññññááááááâââââ\025\025å\"\"\"\"\"õõõõõòòòòññññááááááâââââ\025\025å\"\"\"\"\"õõõõõòòòòñññññáááááâââââ\025\025å\"\"\"õõõõõòòòòñññññáááááâââââÜåå"+
//            "õõõòòòòñññññáááááâââââÜÜå£££££òòòòññññ\031á\026\026\026ââââÜÜÜå£££££££=\031\026\026\026\026\026\026ââÜÜÜÜÛ£££££======\026\026\026\026\026\026\026ÜÜÜÜÛÛ"+
//            "IIII=======xxx\026\026\026\026\026\026\026ÎÎÛÛÛÛ¤¤¤¤¤V\005\005\005\005\005xxxxxyyy\026\026\026\026\026\026ÎÎÎÛÛÛ¤¤¤¤¤¤VV\006\006\006\006tttyyyyyy\026\026ÎÎÎÎÚÚÚ¥¥¥¥¥¤ZZ\006\006\006\006\006\006\006yyyyyÏÎÚÚÚÚ"+
//            "¥¥¥¥¥¥¥ZZZZZ\006\006\006ÍÍÍÍÏÏÏÏÏÐÙÚÚÚ¥¥¥¥¥¥¥ZZZZZi\007\007\007sssszzzzÐÐÐÐÙÙÙÙ±±UUUUUUUeeeii\007\007ssssszzzØÐÐ{×ÙÙÙUUUUUUUUUUeeeee3sssssØØØØØ{{×××Ù"+
//            "UUUUUUUUUUUeeee333333rrØØØØ{×××\024\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\T33333rrrrrrr|××\024µµµµ\\\\\\\\\\\\\\\\\\\\\\T¾22222rrrrrrÒÒÒÒ°°°°°°°°\\\\\\\\\\\\¾¾¾22222ÑÑÑÑÑÈÒÒÒÒ"+
//            "°°°°°°°°°°°]]]]]]]jjjjjÑÑÑÑÑÒÒÒÒ°°°°°°°°°°°]]]]]]]]jjjjjjÑÑqÖÒÒÒ,,,,,,,,,,,,^^^^]]]jjjj\021\021\021qÖÖÖÖÖ,,,,,,,,,,,,^^^^^^^^\021\021\021\021\021\021\021ÖÖÖÔÔ"+
//            "¯¯¯¯¯¯¯,,,,,^^^^^^^^^\021\021\021\021\021kkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯¯À^^^^^^^^_\021ÇÇÇkkÆÆÔÔ¶¶¶¶¶¯¯¯ÀÀÀÀÀÀÀÀ^ÁÁÁÁÇÇÇÇÇÇÇÆÆÆÆ·······ÀÀÀÀÀÀÀÀÀÁÁÁÁÁÁÇÇÇÇÇÇÅÅÅÅ"+
//            "·····¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÇÇÇÇÇÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÃÃÃ\022\022ÅÅÅ-----¹¹¹¹¹¹¹¹¹¹¸ÂÂÂÂÂÂÂÃÃÃÃÃ\022\022\022\022---------¹¹¹¸¸¸¸¸ÂÂÂÂÂÂÃÃÃÃÃ\022\022\022\022"+
//            "\"\"\"\"õõõõòòòòòññññáááááââââââ\025åå\"\"õõõõòòòòòññññáááááââââââ\025ååõõõõòòòòòññññáááááââââââ\025ååõõõõòòòòòññññ\031ááááââââââÜåå"+
//            "õõòòòòòòñññ\031\031áááâââââÜÜåå£££££££òòòòòññ\031\031\031\026\026\026\026âââÜÜÜÜå£££££££===\031\026\026\026\026\026\026ðÜÜÜÜÛÛ£££££======\026\026\026\026\026\026ããÜÜÛÛÛ"+
//            "¤¤>>>>>>>>>xx\026\026\026\026\026ãããÛÛÛÛ¤¤¤¤¤¤>>>>>>xxy\026\026\026\026ÎÎÎÎÛÛÛ¤¤¤¤¤¤GGG\006\006\006\006\006yyyyy\026ÎÎÚÚÚÚ¥¥¥¥¥GGGG\006\006\006\006\006\006yyyÚÚÚÚ"+
//            "¥¥¥¥¥¥¥ZZZZ\006\006\006\007\007ÍzzzÐÙÙÚÚ¥¥¥¥¥¥¥JJZZ\007\007\007\007\007ssszzzzz{{{ÙÙÙÙÙUUUUUJJJJJJJ\007\007\007\007sssszzzz{{{{ÙÙÙÙUUUUUUUUUUeeee3333sssØØØØ{{{××ÙÙ"+
//            "*UUUUUUUUUUeee3333333rrrØØ{{{××\024\\\\\\\\\\\\\\\\\\\\\\\\TTT333333rrrrrr||×\024\024°°\\\\\\\\\\\\\\\\\\\\TTTT222222Ñrrrr|ÒÒÒÒ°°°°°°°°\\\\\\TTTTT]22222ÑÑÑÑÑÈÒÒÒÒ"+
//            "°°°°°°°°°°]]]]]]]]jjjjjÑÑÑÑÑÒÒÒÒ°°°°°°°°°°°]]]]]]]jjjjjjjÑqqqÒÒÒ°°°°°°°,,,,^^^]]]]ddjjjjjqqqÖÖÖÔ,,,,,,,,,,,^^^^^^^^^\021\021\021\021\021\021kÖÖÔÔÔ"+
//            "¯¯¯¯¯¯¯¯¯¯¯,^^^^^^^^\021\021\021\021\021kkkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯¯¯^^^^^^^___ÇÇkkkÆÆÔÔ¯¯¯¯¯¯¯¯¯ÀÀÀÀÀÀÀ^ÁÁ__ÇÇÇÇÇÇÆÆÆÆÆ······¹¹ÀÀÀÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÇÅÅÅÅ"+
//            "·¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇÅÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÂÁÁÁÁÁÁÃÃÃÃ\022\022ÅÅÅ----¹¹¹¹¹¹¹¹¹¹¸¸¸ÂÂÂÂÂÃÃÃÃÃÃ\022\022\022\022--------¹¹¸¸¸¸¸¸¸¸ÂÂÂÂÃÃÃÃÃÃ\022\022\022\022"+
//            "õõõòòòòòòñññ\031\031ááááââââââåååõõòòòòòòñññ\031\031ááááââââââåååõõòòòòòòññ\031\031\031ááááââââââåååõòòòòòòññ\031\031\031\031ááââââââÜååå"+
//            "õòòòòòòò\031\031\031\031\031ááââââââÜÜåå££££££££=\031\031\031\031\031\026\026\026ððððÜÜÜåå£££££££===\031\031\026\026\026\026\026ððããÜÛÛÛ£££££>>>>>>\026\026\026\026\026ããããÛÛÛÛ"+
//            "###>>>>>>>>>\026\026\026\026\026ãããÛÛÛÛ¤¤¤¤>>>>>>>>\026\026\026\026ãããÛÛÛÛ¤¤GGGGGGGG\006\006\006yyyyÚÚÚÚÚ¥¥¥¥GGGGGG\006\006\006\006yyÚÚÚÚ"+
//            "¥¥¥¥¥¥¥GGG\006\006\007\007\007\007zzzzÙÙÙÚ¥¥¥¥JJJJJJ\007\007\007\007\007\007ssszzzzz{{{ÙÙÙÙÙJJJJJJJJJJJJ\007\007\007\007ssszzzzz{{{{ÙÙÙÙ***UUUUUJJJee3333333zzØØ{{{{×ÙÙ\024"+
//            "******UUUTTTT33333333rrrr{{{{×\024\024****\\\\\\\\\\TTTTTT333322rrrrr|||\024\024\024°°°\\\\\\\\\\TTTTTTTT222222ÑÑrr|||ÒÒÒ°°°°°°°°TTTTTTTT22222ÑÑÑÑÑÑ|ÒÒÒÒ"+
//            "°°°°°°°°°°]]]]]]]]jjjjjÑÑÑÑÑÒÒÒÒ°°°°°°°°°°]]]]]]]]jjjjjjqqqqqÒÒÒ°°°°°°°°°°]]]]]]]]ddjjjjjqqqÖÖÔÓ¯¯¯¯¯¯¯,,,S^^^^^^^^d\021\021\021\021\021kkÖÔÔÔÔ"+
//            "¯¯¯¯¯¯¯¯¯¯¯S^^^^^^^^_\021\021\021kkkkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯¯S^^^^^_____ÇkkkkÆÆÔÔ¯¯¯¯¯¯¯¯¯¯¯ÀÀÀÀ^^_____ÇÇÇÇkÆÆÆÆÆ···®®®¹¹¹¹¹ÀÀÀÀÁÁÁÁÁÁÁÇÇÇÇÇÆÆÆÆÆ"+
//            "¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁÁÇÇÇÇÅÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹¸ÂÂÁÁÁÁÃÃÃÃÃ\022\022ÅÅÅ--¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸ÂÂÂÂÂÃÃÃÃÃÃ\022\022\022\022-------¹¸¸¸¸¸¸¸¸¸¸ÂÂÂÂÃÃÃÃÃÃ\022\022\022\022"+
//            "!!!!!!!òòòòòòò\031\031\031\031\031\031ááââââââåååå!!!!!!!òòòòòòò\031\031\031\031\031\031ááââââââåååå!!!!!!!!òòòòòò\031\031\031\031\031\031ááââââââåååå!!!!!!!!òòòòòò\031\031\031\031\031\031ááââââââÜååå"+
//            "!!!!!!!!ÿòòòòò\031\031\031\031\031\031\031ððððððãÜååå££££££££ÿÿ\031\031\031\031\031\031\026\026ððððããÛÛåå££££££>>>>\031\031\026\026\026\026ððãããÛÛÛå####>>>>>>>\026\026\026\026\026ããããÛÛÛÛ"+
//            "####>>>>>>>\026\026\026\026ããããÛÛÛÛ#####>>>>>>>\026ãããÛÛÛÛGGGGGGGGGGGG\006yÚÚÚÚ¥¥GGGGGGGGGG\006?ÚÚÚÚ"+
//            "¥¥¥¥¥¥GGGGG\007\007\007\007\007zzzzzÙÙÙÙJJJJJJJJJJ44\007\007\007\007sszzzzzz{{{ÙÙÙÙÙJJJJJJJJJJJ44\007\007\007ssszzzz{{{{{ÙÙÙÙ******JJJJJJ4333333\bzzØ{{{{{ÙÙ\024\024"+
//            "********TTTT¦3333333\brrrr{{{{\024\024\024*****TTTTTTTTTT333222rrrr||||\024\024\024°°TTTTTTTTTTTTTT222222ÑÑÑ||||ÒÒ\024°°°°°°°°TTTTTT]]22222ÑÑÑÑÑÑ|ÒÒÒÒ"+
//            "°°°°°°°°°]]]]]]]]jjjjjjÑÑÑqqÒÒÒÒ°°°°°°°°°°]]]]]]]]jjjjjjqqqqqÒÒÓ°°°°°°°°°SS]]]]]]ddddjjjqqqqqÔÔÓ¯¯¯¯¯¯¯¯SSSS^^^^^^ddd\021\021\021kkkÔÔÔÔÔ"+
//            "¯¯¯¯¯¯¯¯¯¯SSS^^^^^____\021kkkkkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯¯SS^^^______kkkkkÆÆÆÔ¯¯¯¯¯¯¯¯¯¯¯¯ÀÀS_______ÇÇÇkkÆÆÆÆÆ®®®®®®®®®®®®®®ÁÁÁÁÁÁÁ``ÇÇÇÇÆÆÆÆÆ"+
//            "¹¹¹¹¹¹¹¹¹¹¹¹¹¹¹ÁÁÁÁÁÁÁ``ÇÇcÅÅÅÅÅ¹¹¹¹¹¹¹¹¹¹¹¹¹¹¸¸ÂÂÂÁÁÁÃÃÃÃÃ\022ÅÅÅÄ¹¹¹¹¹¹¹¹¹¹¸¸¸¸¸¸¸ÂÂÂÂÃÃÃÃÃÃÃ\022\022\022Ä-----¸¸¸¸¸¸¸¸¸¸¸¸¸¸ÂÂÃÃÃÃÃÃÃ\022\022\022\022"+
//            "!!!!!!!!òòòòò\031\031\031\031\031\031\031áððâââââåååå!!!!!!!!òòòòò\031\031\031\031\031\031\031\031ððððâââåååå!!!!!!!!ÿòòò\031\031\031\031\031\031\031\031\031ðððððððåååå!!!!!!!ÿÿÿòò\031\031\031\031\031\031\031\031\031ððððððãåååå"+
//            "!!!!!!ÿÿÿÿÿÿ\031\031\031\031\031\031\031\031\031ððððððãÛååå!!!!!ÿÿÿÿÿÿ\031\031\031\031\031\031\026ðððððããÛÛåå####>>>>>>\031\031ö\026\026ððããããÛÛÛå#####>>>>>ö\026\026ðãããããÛÛÛÛ"+
//            "#####>>>>>>\026\026\026ãããããÛÛÛÛ######>>>>>>ããÛÛÛäGGGGGGGGGGG???ÚÚÚÚGGGGGGGGGGG???ÚÚÚÚ"+
//            "¥¥¥¥¥GGGG444\007\007\007\007zzzzzÙÙÙÙÙJJJJJJJJJ44444\007\007\007zzzzzz{{{{ÙÙÙÙÙJJJJJJJJJJ44444\007\bszzzzz{{{{ÙÙÙÙÙ*******JJJ¦¦¦\b\b\b\b\b\b\bzz{{{{{{Ù\024\024\024"+
//            "********TTT¦¦¦\b\b\b\b\b\brrrr{{{{\024\024\024\024******TTTTTTTTK222222rrr|||||\024\024\024TTTTTTTTTTTTTTK222222ÑÑÑ|||||ÒÒ\024°°°°°°°TTTTTT]]222222ÑÑÑÑÑ||ÒÒÒÒ"+
//            "°°°°°°°°°]]]]]]]]jjjjjÑÑqqqqÒÒÒÓ°°°°°°°°°]]]]]]]]ddjjjjqqqqqqÓÓÓ°°°°°°°SSSSS]]]]ddddddjqqqqqqÔÓÓ¯¯¯¯¯¯¯SSSSSS^^^^ddddd\021kkkkÔÔÔÔÔ"+
//            "¯¯¯¯¯¯¯¯¯SSSSS^^^_____kkkkkkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯¯SSSS________kkkkkÆÆÆÔ¯¯¯¯¯®®®®®®®®SS_______``kkkÆÆÆÆÆ®®®®®®®®®®®®®®®ÁÁÁÁÁ`````ÇÆÆÆÆÆÆ"+
//            "®®®®®®®®®¹¹¹¹­­ÁÁÁÁÁÁ````ccÅÅÅÅÄ¹¹¹¹¹¹¹¹¹¹­­­¸¸¸¸ÂÂÂÁÃÃÃÃÃÃcÅÅÄÄ­­­­­­­­­­¸¸¸¸¸¸¸¸ÂÂÂÃÃÃÃÃÃÃ\022\022ÄÄ­­­­¸¸¸¸¸¸¸¸¸¸¸¸¸¸¸ÂÂÃÃÃÃÃÃÃ\022\022\022\022"+
//            "!!!!!!ÿÿÿÿÿ\031\031\031\031\031\031\031\031\031ððððððððåååå!!!!!!ÿÿÿÿÿ\031\031\031\031\031\031\031\031\031ðððððððãåååå!!!!!ÿÿÿÿÿÿÿ\031\031\031\031\031\031\031\031ðððððððãåååå!!!!!ÿÿÿÿÿÿÿ\031\031\031\031\031\031\031\031ðððððððãåååå"+
//            "!!!ÿÿÿÿÿÿÿÿÿÿ\031\031\031\031\031\031öððððððããÛååå##ÿÿÿÿÿÿÿÿÿÿ\031\031öööðððððãããÛÛåå#####>>ÿÿþþöööööðððããããÛÛÛå######>>>þþööööðããããããÛÛÛä"+
//            "######>>>>þöãããããÛÛÛä#######>?????ããÛÛääGGGGGGGG??????ÚÚÚäGGGGGGGGG??????ÙÚÚä"+
//            "JJJJJJG4444444??zzzÙÙÙÙÙJJJJJJJJ44444444zzzzzz{{{{ÙÙÙÙÙÙ***JJJJJJFFF444\b\b\bzzzz{{{{{ÙÙÙÙ\024********¦¦¦¦¦\b\b\b\b\b\b\bzz{{{{{{\024\024\024\024"+
//            "********¦¦¦¦¦¦\b\b\b\b\b\brr||||||\024\024\024\024******TTTTKKKKK2222\t\t\t|||||||\024\024\024*TTTTTTTTKKKKKK22222\t\tÑ||||||Ò\024\024°°°°°°°TKKKKKKK22222\tÑÑÑÑ|||ÒÒÒÒ"+
//            "°°°°°°°°]]]]]]]]djjjjjqqqqqqÒÒÓÓ°°°°°°°°]]]]]]]]ddddjjqqqqqqqÓÓÓ°°SSSSSSSSSSSSddddddddqqqqqqpÓÓÓ¯¯¯¯¯SSSSSSSSSS^ddddddkkkkkÔÔÔÔÓ"+
//            "¯¯¯¯¯¯¯¯SSSSSSS_______kkkkkkÔÔÔÔ¯¯¯¯¯¯¯¯¯¯SSSSS_______kkkkkÆÆÆÆÆ®®®®®®®®®®®®®®______````kkkÆÆÆÆÆ®®®®®®®®®®®®®®®ÁÁ````````cÆÆÆÆÆÆ"+
//            "®®®®®®®®®®®­­­­­ÁÁÁ`````ccccÅÅÄÄ­­­­­­­­­­­­­­¸¸¸¸ÂÂÃÃÃÃÃÃcclÄÄÄ­­­­­­­­­­­¸¸¸¸¸¸¸¸ÂÃÃÃÃÃÃÃÃ\022ÄÄÄ­­­­­­¸¸¸¸¸¸¸¸¸¸¸¸¸¸ÃÃÃÃÃÃÃÃ\022\022\022Ä"+
//            "!!!!ÿÿÿÿÿÿÿÿ\031\031\031\031\031\031\031ððððððððãåååå!!!ÿÿÿÿÿÿÿÿÿ\031\031\031\031\031\031\031ððððððððãåååå!!!ÿÿÿÿÿÿÿÿÿ\031\031\031\031\031\031ööððððððããåååå!ÿÿÿÿÿÿÿÿÿÿÿ\031\031\031\031\031öööððððððããåååå"+
//            "ÿÿÿÿÿÿÿÿÿÿÿÿÿ\031ööööööðððððããããååå##ÿÿÿÿÿÿÿÿÿþþþöööööööðððãããããååå#####þþþþþþþþþööööööððãããããÛÛää######þþþþþþþþöööööðããããããÛÛää"+
//            "#######þþþþþþþöããããÛäää¢¢¢###????????ãÛäää¢¢¢¢¢¢?????????ÚÚää¢¢¢¢GG?????????ÙÙää"+
//            "JJJJJ4444444???ÙÙÙÙÙJJJJJJFFFFF444555zzzzz{{{{ÙÙÙÙÙÙ****JJFFFFFFFFF\b\b\bzzz{{{{{{ÙÙÙ\024\024*******¦¦¦¦¦¦\b\b\b\b\b\b\bz{{{{{{\024\024\024\024\024"+
//            "*******¦¦¦¦¦¦¦\b\b\b\b\b\t\t|||||||\024\024\024\024*****KKKKKKKKKK2\t\t\t\t\t|||||||\024\024\024§§§KKKKKKKKKKKK22\t\t\t\t\t||||||\024\024°°°§§LLLKKKKKKK222\t\t\t\tÑÑ||||ÒÒÓÓ"+
//            "°°°°°°LLLLLL]]]ddd11\n\nqqqqqq}ÓÓÓ°°°°°SSSSS]]]]dddddd\n\nqqqqqq}ÓÓÓSSSSSSSSSSSSSSddddddddqqqqqqpÓÓÓ¯¯SSSSSSSSSSSSS__ddddkkkkkkppppÓ"+
//            "¯¯¯¯¯¯SSSSSSSSS_______kkkkkkoooo¯¯¯¯¯¯®®®SSSSS________kkkkkÆÆÆoo®®®®®®®®®®®®®®_____`````kkÆÆÆÆÆÆ®®®®®®®®®®®®®®®`````````cccÆÆÆÆÄ"+
//            "®®®®®®®®®­­­­­­­­```````cccclÄÄÄ­­­­­­­­­­­­­­­¸¸¸ÂÃÃÃÃÃÃccclÄÄÄ­­­­­­­­­­­­¸¸¸¸¸¸¸ÃÃÃÃÃÃÃÃbbÄÄÄ­­­­­­­­¸¸¸¸¸¸¸¸¸¸¸¸ÃÃÃÃÃÃÃÃbbÄÄ"+
//            "ÿÿÿÿÿÿÿÿÿÿÿÿ\031\031ööööööððððððããååååÿÿÿÿÿÿÿÿÿÿÿÿ\031öööööööððððððããååååÿÿÿÿÿÿÿÿÿÿÿööööööööððððððããååååÿÿÿÿÿÿÿÿÿÿööööööööðððððãããåååå"+
//            "ÿÿÿÿÿÿÿþþöööööööððððããããååååÿÿþþþþþþööööööööððããããããäääþþþþþþþþþþöööööööðãããããããäää#####þþþþþþþþþþööööööããããããããäää"+
//            "¢¢¢¢¢¢þþþþþþþþþããããääää¢¢¢¢¢¢¢???????ääää¢¢¢¢¢¢¢???????äää¢¢¢¢¢¢????????ÙÙää"+
//            "¢¢¢¢FFFF55555555ÙÙÙÙæFFFFFFFFFFF555555zzzz{{{{ÙÙÙÙÙÙÙ****FFFFFFFFFF55@@@z{{{ÙÙÙ\024\024\024*****¦¦¦¦¦¦¦¦F\b\b\b@@@{{\024\024\024\024\024"+
//            "******KK¦¦¦¦¦¦\b\b\b\t\t\t|||||||\024\024\024\024\024§§§KKKKKKKKKKKK\t\t\t\t\t\t||||||\024\024\024§§§§§§KKKKKKKKK\t\t\t\t\t\t\t|||||\024ç§§§§§§LLLLLLLLL\t\t\t\t\t\n\n\nq||ÓÓÓ"+
//            "LLLLLLLLLLLLLLLL1111\n\n\nqqqq}}ÓÓÓ¨¨¨¨¨SSSSLLLLLdddddd\n\nqqqqq}}ÓÓÓSSSSSSSSSSSSSSddddddd\nqqqqqppÓÓÓSSSSSSSSSSSSSSS_____dkkkkkpppppÓ"+
//            "¯¯¯¯SSSSSSSSSS_______kkkkkkooooo®®®®®®®®®SSSSS______``kkkkkÆÆooo®®®®®®®®®®®®®®___```````kkÆÆÆÆÆÆ®®®®®®®®®®®®®®®`````````ccclllÄÄ"+
//            "®®®®®®®­­­­­­­­­­``````cccccllÄÄ­­­­­­­­­­­­­­­­¸¸`ÃÃÃÃÃcccblÄÄÄ­­­­­­­­­­­­­¸¸¸¸¸¸..ÃÃÃÃbbbbÄÄÄ­­­­­­­­­¸¸¸¸¸¸¸¸¸¸.......ÃbbbÄÄ"+
//            "ÿÿÿÿÿÿÿÿööööööööðððððããåååååÿÿÿÿÿÿÿÿööööööööðððððãããååååÿÿÿÿÿÿÿööööööööðððððãããååååÿÿÿÿÿÿþööööööööððððããããåååå"+
//            "ÿþþþþþþöööööööðððãããããääääþþþþþþþööööööööðããããããääääþþþþþþþþþöööööööãããããããääää¢¢¢¢¢þþþþþþþþþþööööööããããããääää"+
//            "¢¢¢¢¢¢¢þþþþþþþãããääää¢¢¢¢¢¢¢¢?????\032ääää¢¢¢¢¢¢¢???????ääää¢¢¢¢¢¢¢???????ÙÙää"+
//            "¡¡¡¡¡¡5555555555ÙÙÙÙÙæ¡FFFFFFFF5555555@@ÙÙÙÙÙÙæ*FFFFFFFFFFF55@@@@@ÙÙ\024\024\024\024****¦¦¦¦¦¦¦¦¦@@@@@@\024\024\024\024\024"+
//            "§§§KKKKKK¦¦¦¦¦\t\t\t\t\t\t|||||\024\024\024\024§§§§§§KKKKKKKK\t\t\t\t\t\t\t||||\024\024ç§§§§§§§§KKKKKK\t\t\t\t\t\t\t||||çç§§§§§§LLLLLLLLL11111\n\n\nq|}ÓÓç"+
//            "¨¨¨LLLLLLLLLLLLL1111\n\n\nqqq}}}}ÓÓ¨¨¨¨¨¨¨¨LLLLLLdddd1\n\n\nqqqqq}}}ÓÓ¨¨¨¨¨¨SSSSSSSddddddd\n\n\013qqpppp}ÓÓSSSSSSSSSSSSSS_____RRkkkkppppppÓ"+
//            "SSSSSSSSSSSSS_____RRRkkkkkkooooo®®®®®®®®®SSSS_____RR``kkkkkooooo®®®®®®®®®®®®®__`````````ccÆÆÆÆon®®®®®®®®®®®®®®`````````cccclllÄÄ"+
//            "®®®­­­­­­­­­­­­­``````ccccclllÄÄ­­­­­­­­­­­­­­­­­¸....cccbbblÄÄÄ­­­­­­­­­­­­­­¸¸¸¸......bbbbbÄÄÄ­­­­­­­­­­­¸¸¸¸¸¸¸........bbbb\023\023"+
//            "ÿÿÿÿöööööööööððððãïïïåååîÿÿþöööööööööððððãïïïååäîþþþöööööööööððððãïïïäääîþþþþööööööööðððããïïïääää"+
//            "þþþþþöööööööööðãããïïïääääþþþþþþööööööööããããïïïääääþþþþþþþþöööööööããããïïïääää¢¢¢¢¢¢þþþþþþþþöööööö\032\032ïïïääää"+
//            "¢¢¢¢¢¢¢¢þþþþþþ\032\032\032\032ääää¢¢¢¢¢¢¢¢¢????\032\032\032ääää¢¢¢¢¢¢¢¢?????\032\032ääää¡¡¡¡¡¡¡¡555555\032\032æææ"+
//            "¡¡¡¡¡¡¡555555555@ÙÙÙæææ¡¡¡¡¡¡55555555@@@@ÙÙÙ\024ææ¡¡FFFFFFF555@@@@@@\024\024\024\024\024æ$$$$$¦¦¦¦¦¦@@@@@@@@\024\024\024\024\024\024"+
//            "§§§§§§KKKK¦¦¦@@@@\t\t|||\024\024\024ç§§§§§§§§KKKKK\t\t\t\t\t\t\t|||ççç§§§§§§§§§LLLLK1\t\t\t\t\t\n|ççç§§§§§§§LLLLLLL11111\n\n\n\n}}çç"+
//            "¨¨¨¨¨LLLLLLLLLL1111\n\n\n\nqq}}}}}ÓÓ¨¨¨¨¨¨¨¨¨LLLLLMMM11\n\n\n\nqq/}}}}ÓÓ¨¨¨¨¨¨¨¨¨¨SMMMMMMMM\n\013\013\013\013\013pppp}ÓÓ¨¨¨¨¨¨¨SSSSSSMMMRRRRRk\013\013\013pppppÕÕ"+
//            "¨¨SSSSSSSSSSS__RRRRRRkkkkkoooooÕ®®®®®®®®®))))_RRRRRR``kkkkooooon®®®®®®®®®®®®®``````````ccclllonn®®®®®®®®®®®®­­````````ccccllllÄn"+
//            "­­­­­­­­­­­­­­­­``````ccccclllÄÄ­­­­­­­­­­­­­­­­­.....cbbbbblÄÄÄ­­­­­­­­­­­­­­­¸¸.......bbbbbÄÄm­­­­­­­­­­­­¸¸¸¸¸.........bbb\023\023\023"+
//            "ýýýýöööööööööððïïïïïïääîîýýýýöööööööööððïïïïïïääîîýýýýýööööööööðïïïïïïïääîîýýýýýöööööööööïïïïïïïääîî"+
//            "ýýýýýýöööööööööïïïïïïïäääîýýýýýýþööööööööïïïïïïïäääîýýýýýþþþööööööö\032\032\032ïïïïïäääî¢¢¢¢¢      þþþöööö\032\032\032\032\032ïïääää"+
//            "¢¢¢¢¢¢        \032\032\032\032\032\032ääää¢¢¢¢¢¢¢       \032\032\032\032\032ääää¡¡¡¡¡¡¡¡      \032\032\032\032\032ääæ¡¡¡¡¡¡¡¡¡555555\032\032\032ææææ"+
//            "¡¡¡¡¡¡¡¡555555@@@ÙÙææææ¡¡¡¡¡¡¡55555@@@@@ÙÙ\024æææ$$$$$$$$$5@@@@@@@@\024\024\024\024ææ$$$$$$$$$$$@@@@@@@\024\024\024\024æ"+
//            "§§§§§$$$$$$$$@@6666\024ççç§§§§§§§§§$$$$\t\t\t\t\tAAçççç§§§§§§§§§LLLL111111\n\nççç§§§§§§§LLLLLLL11111\n\n\n\n}}}çç"+
//            "¨¨¨¨¨¨LLLLLLLL11111\n\n\n\n//}}}}}ÓÓ¨¨¨¨¨¨¨¨¨LLMMMMMM1\n\n\n\n\013///}}}}ÓÓ¨¨¨¨¨¨¨¨¨¨MMMMMMMMM\013\013\013\013\013\013ppp}}ÕÕ¨¨¨¨¨¨¨)))))MMMMMRRR\013\013\013\013\013pppppÕÕ"+
//            ")))))))))))))RRRRRRRRkkk\013ooooooÕ®®®®®®)))))))RRRRRRR``kkkooooonn®®®®®®®®®®®))`````````cccclllnnn®®®®®®®®®®­­­`````````ccccllllnn"+
//            "­­­­­­­­­­­­­­­``````cccccllllÄÄ­­­­­­­­­­­­­­­­......bbbbbbllÄm­­­­­­­­­­­­­­­­........bbbbbmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.........bbbb\023\023\023"+
//            "ýýýýýýýýýööööö÷÷÷ïïïïïïïïîîîîýýýýýýýýýöööö÷÷÷÷ïïïïïïïïîîîîýýýýýýýýýý÷÷÷÷÷÷÷ïïïïïïïïäîîîýýýýýýýýýý÷÷÷÷÷÷÷ïïïïïïïïäîîî"+
//            "ýýýýýýýýýýý÷÷÷÷÷÷÷ïïïïïïïïäîîîýýýýýýýýýýýýý÷÷÷÷÷÷÷\032\032ïïïïïïäîîî      ýýýýýýý÷÷÷÷÷÷÷\032\032\032\032ïïïïääîî              ÷÷÷÷÷\032\032\032\032\032\032ïïääîî"+
//            "¢¢             ÷\032\032\032\032\032\032\032äääî¡¡¡¡¡          \032\032\032\032\032\032äääî¡¡¡¡¡¡¡       \032\032\032\032\032ææææ¡¡¡¡¡¡¡¡¡¡5555\032\032\032\032ææææ"+
//            "¡¡¡¡¡¡¡¡¡555@@@@\032\032æææææ¡¡¡¡¡¡¡¡55@@@@@@@\024æææææ$$$$$$$$$$@@@@@@@6\024\024\024æææ$$$$$$$$$$$$6666666\024\024ççç"+
//            "$$$$$$$$$$$$$666666çççç§§§§§§§$$$$$$$6AAAAAAçççç§§§§§§§§§§LEEEE11AAAAAçççç§§§§§§§§LLLLEEEE111\n\n\n}}}ççç"+
//            "¨¨¨¨¨¨¨LLLLLLEEE11\n\n\n\n///}}}}}}ç¨¨¨¨¨¨¨¨¨¨MMMMMMM00\n\013\013\013///}}}}ÕÕ¨¨¨¨¨¨¨¨¨)MMMMMMMMM\013\013\013\013\013//pp}ÕÕÕ¨¨¨)))))))))MMMMMRRR\013\013\013\013\013pppoÕÕÕ"+
//            ")))))))))))))RRRRRRRNN\013\013\fooooonÕ)))))))))))))RRRRRRNNNN\f\f\f\fonnnn®®®®®®®®®))))`````````ccclllnnnn®®®®®®®®­­­­­````````ccccllllnnn"+
//            "­­­­­­­­­­­­­­­```QQQQccbbllllmm­­­­­­­­­­­­­­­......Qbbbbbblmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.......bbbbbmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.........bbb\023\023\023\023"+
//            "ýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïïïîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïïïîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïïïîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷ïïïïïïïïîîîî"+
//            "ýýýýýýýýýýýý÷÷÷÷÷÷÷÷\032ïïïïïïïîîîîýýýýýýýýýýýý÷÷÷÷÷÷÷÷\032\032\032ïïïïïîîîî             ÷÷÷÷÷÷÷\032\032\032\032\032ïïïîîîî              ÷÷÷÷÷\032\032\032\032\032\032\032\032ïäîîî"+
//            "              \032\032\032\032\032\032\032\032äîîî¡¡¡¡          \032\032\032\032\032\032\032æææî¡¡¡¡¡¡¡      \032\032\032\032\032\032ææææ¡¡¡¡¡¡¡¡¡ \032\032\032\032æææææ"+
//            "¡¡¡¡¡¡¡¡¡¡@@@@@\032\032æææææ¡¡¡¡¡¡$$$66666666ææææææ$$$$$$$$$$66666666\024\024ææææ$$$$$$$$$$$$666666ççççç"+
//            "$$$$$$$$$$$$$6666AAççççç§§§§§$$$$$$$$77AAAAAAççççç§§§§§§§§§EEEEEEEAAAAAçççç§§§§§§§§LEEEEEEEEEAAAA}}}ççç"+
//            "¨¨¨¨¨¨¨¨LLMEEEEEE000/////}}}}}Õè¨¨¨¨¨¨¨¨¨MMMMMMM0000\013\013////}}}ÕÕÕ¨¨¨¨¨)))))MMMMMMM00\013\013\013\013///ppÕÕÕÕ))))))))))))MMMMMNNN\013\013\013\013\013pooÕÕÕÕ"+
//            "))))))))))))))RRNNNNNN\f\f\f\f\fonnÕÕ))))))))))))))RRNNNNNN\f\f\f\f\f\fnnnn®®®®)))))))))``````NQQcc\f\f\fnnnnn®®®®­­­­­­­­©`````QQQQQccllllnn~"+
//            "­­­­­­­­­­­­­­`QQQQQQQQbbblllmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.....QQbbbbbbmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.......bbbbbmmm\023¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.........bbb\023\023\023\023"+
//            "ýýýýýýýýýýý÷÷÷÷÷÷÷÷÷ïïïïïïïíîîîîýýýýýýýýýýý÷÷÷÷÷÷÷÷÷ïïïïïïïíîîîîýýýýýýýýýýý÷÷÷÷÷÷÷÷÷ïïïïïïïíîîîîýýýýýýýýýýý÷÷÷÷÷÷÷÷÷\032ïïïïïïíîîîî"+
//            "ýýýýýýýýýý÷÷÷÷÷÷÷÷\032\032ïïïïïíîîîî       ýý÷÷÷÷÷÷÷÷\032\032\032\032ïïïííîîî           ÷÷÷÷÷÷÷\032\032\032\032\032\032\032ïííîîî             ÷÷÷÷÷÷\032\032\032\032\032\032\032\032íííîî"+
//            "              \032\032\032\032\032\032\032íííîî¡¡           \032\032\032\032\032\032\032\032ææææ¡¡¡¡¡\032\032\032\032\032æææææ¡¡¡¡¡¡\032\032\032\032æææææ"+
//            "¡¡¡¡¡¡¡6\032\032ææææææ      $$$66666666æææææææ$$$$$$$$$$6666666ççææææ$$$$$$$$$$$666666çççççç"+
//            "$$$$$$$$$$$$7777AAççççç$$$$$$$$$$$77777AAAAçççççEEEEEE7AAAAAçççççEEEEEEEEAAAAçççç"+
//            "EEEEEE0000////}ÕÕèè¨¨¨¨¨¨)))MMMMM000000\013/////}}ÕÕÕè)))))))))))MMMMM0000\013\013/////ÕÕÕÕÕ)))))))))))))MNNNNNN\013\013\013\f\f\f\fÕÕÕÕ"+
//            ")))))))))))))NNNNNNNNN\f\f\f\f\fnnnéé)))))))))))))NNNNNNNNN\f\f\f\f\fnnnné)))))))))))©©©NNNNNQQQOO\f\f\fnnn~~­­­­­­­©©©©©©©©QQQQQQQQQOll\r\r~~~"+
//            "¬¬¬¬¬¬¬¬¬¬¬¬¬©©QQQQQQQQQbblmmmm~¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬...QQQQQbbbbmmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.......bbbbmmmm\023¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬........bbb\023\023\023\023\023"+
//            "ýýýýýý÷÷÷÷÷÷÷÷÷ïïïïïïíííîîîýýýýý÷÷÷÷÷÷÷÷÷ïïïïïïíííîîîýýýýý÷÷÷÷÷÷÷÷÷\032ïïïïïíííîîîýýýý÷÷÷÷÷÷÷÷÷\032\032ïïïïíííîîî"+
//            "ýýý÷÷÷÷÷÷÷÷\032\032\032\032ïïíííííîî    ÷÷÷÷÷÷÷÷\032\032\032\032\032\032íííííîî      ÷÷÷÷÷÷÷\032\032\032\032\032\032íííííîî       ÷÷÷÷üü\032\032\032\032\032\032\032íííííî"+
//            "    \032\032\032\032\032\032íííííî\032\032\032\032\032\032\032ííæææ\032\032\032\032\032ææææææ  \032\032\032ææææææ"+
//            "       \032æææææææ         6666666æææææææ         $666666çççæææë    $$$$$$$66666ççççëë"+
//            "$$$$$$$$$$7777777Açççççç$$$777777AAAAçççççEEEE777AAAAçççççEEEEEEEAAAAèèèè"+
//            "EEEE000000//ÕèèèMM000000000///ÕÕÕèè)))))))))))MMM000000\013\013///ÕÕÕÕ)))))))))))))NNNNNNN\013\f\f\f\f\fÕéé"+
//            "))))))))))))NNNNNNNNN\f\f\f\f\f\fnnééé)))))))))))©©NNNNNNNNO\f\f\f\f\fnnn~é©©©©©©©©©©©©©©©©NNQQOOOO\f\f\r\rn~~~©©©©©©©©©©©©©©©©QQQQQQOOO\r\r\r\r~~~"+
//            "¬¬¬¬¬¬¬¬¬¬¬©©©©QQQQQQQQQObmmmmm~¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.QQQQQQQbbbmmmmmm¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬......bbbbmmmmma¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬........bP\023\023\023\023\023\023"+
//            "÷÷÷÷÷÷÷÷÷\032\032ïïïííííííîî÷÷÷÷÷÷÷÷÷\032\032\032ïïííííííîî÷÷÷÷÷÷÷÷ü\032\032\032ïïííííííîî÷÷÷÷÷÷üüüü\032\032\032ïíííííííî"+
//            "üüüüüüüüü\032\032\032\032íííííííîüüüüüüüüü\032\032\032\032íííííííîüüüüüüüüü\032\032\032\032ííííííííüüüüüüüü\032\032\032\032\032ííííííí"+
//            "üüüüüü\032\032\032\032\032ííííííí\032\032\032\032\032\032ííííææ\032\032\032\032\032ææææææ    \032\032\032æææææææ"+
//            "       øæææææææ          666ùùææææëë           6666ùççëëëë          777777çççëëë"+
//            "       $777777777Açççççë77777777AAççççççE777778AAAçèèèèEEE888888èèèè"+
//            "E0000000BBBèèèè000000000BBBBèè))))))))))))00000000BBBBÕéé))))))))))))NNNNNNNDDD\f\fééé"+
//            "))))))))))©©NNNNNNNDDDD\f\f\fééé©©©©©©©©©©©©©©©NNNNOODD\f\f\f\réé©©©©©©©©©©©©©©©©©OOOOOOO\r\r\r\r~~~~©©©©©©©©©©©©©©©©QQQQOOOOO\r\r\r\r~~~"+
//            "¬¬¬¬¬¬¬¬©©©©©©©©QQQQQQOOO\r\r\rmm\177\177¬¬¬¬¬¬¬¬¬¬¬¬¬¬©©QQQQQQQPPPmmmmma¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.....QbPPPPmmmaa¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.......PPPP\023\023\023aa"+
//            "üüüüüüüüüü\032\032ííííííííí\033üüüüüüüüüü\032\032ííííííííí\033üüüüüüüüüü\032\032\032íííííííí\033üüüüüüüüüü\032\032\032íííííííí\033"+
//            "üüüüüüüüüüü\032\032íííííííí\033üüüüüüüüüüü\032\032íííííííí\033üüüüüüüüüü\032\032íííííííí\033üüüüüüüüü\032\032\032ííííííí\033"+
//            "üüüüüüüü\032\032\032ííííííí\033üüüüüüû\032\032\032ííííííí\033ûûûøøøææææææ     ûûøøøøæææëë"+
//            "        øøøøøæëëëë           ùùùùëëëëë           7777ùùùùëëëëë          777777ùùùùëëëë"+
//            "        7777777777çççëëë777777778Açççèèë888888888èèèèè888888888èèèè"+
//            "88888889BBBèèè00000999BBBBBèè00099999BBBBéNNNNNDDDDDDéééé"+
//            "©©©©©©©©©©©©©©NNNNDDDDDD\féé©©©©©©©©©©©©©©©©NOOODDDDD\r\ré©©©©©©©©©©©©©©©©©OOOOOOO\r\r\r\r~~~~©©©©©©©©©©©©©©©©©OOOOOOO\r\r\r\r\r~\177\177"+
//            "¬¬¬¬©©©©©©©©©©©©©QQOOOOPPP\r\r\r\016\177\177¬¬¬¬¬¬¬¬¬¬¬¬©©©©QQQQQOPPPPPP\016\016aa¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬««..QQPPPPPPPaaaa¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬.....PPPPPPPaaaa"+
//            "üüüüüüüüüüüüíííííííí\033\033\033üüüüüüüüüüüüíííííííí\033\033\033üüüüüüüüüüüüíííííííí\033\033\033üüüüüüüüüüüüíííííííí\033\033\033"+
//            "üüüüüüüüüüüüûííííííí\033\033\033üüüüüüüüüüüüûííííííí\033\033\033üüüüüüüüüüûûííííííí\033\033\033üüüüüüüüûûûûíííííí\033\033\033"+
//            "üüüüüüûûûûøøøíííí\033\033\033üüüüûûûûûøøøøøíí\033\033\033üûûûûûûøøøøøøëëììûûûûûûøøøøøøëëëë"+
//            "    ûûûûøøøøøëëëëë        ùùùùùùùëëëë          \036ùùùùùùùëëëë          77777\036\036\036\036\036ùùùùùùëëëë"+
//            "%%%%%%%%%7777777\036\036\036\036\036ùùùùëëëë888888888\036\036úèèèè\034\0348888888888úèèèèè888888888BBúèèèè"+
//            "88889999BBBBúèèè99999999BBBBè9999999BBB&&DDDDDDDééé"+
//            "©©©©©©&DDDDDDDD;éê©©©©©©©©©©©©©©©©OODDDDD;;\rê©©©©©©©©©©©©©©©©©OOOOOO\r\r\r\r\rCC\177\177©©©©©©©©©©©©©©©©©OOOOOOP\r\r\r\r\r\177\177\177"+
//            "««««««««©©©©©©©©©OOOOPPPPP<<\016\016\177\177««««««««««««««««««OOPPPPPPPP\016\016\016\016(((((((((««««««««««PPPPPPPPPaaaa(((((((((((((««««««'''''''PPaaaa"+
//            "\037\037\037\037\037\037\037\037\037üüüüüüüüüüüûûíííííí\033\033\033\033\037\037\037\037\037\037\037\037\037üüüüüüüüüüûûûíííííí\033\033\033\033\037\037\037\037\037\037\037\037\037üüüüüüüüüüûûûíííííí\033\033\033\033\037\037\037\037\037\037\037\037\037üüüüüüüüüüûûûíííííí\033\033\033\033"+
//            "\037\037\037\037\037\037\037\037\037üüüüüüüüüûûûûøííííí\033\033\033\033\037\037\037\037\037\037\037\037\037üüüüüüüüüûûûûøííííí\033\033\033\033\037\037\037\037\037\037\037\037\037üüüüüüüüûûûûûøøøííí\033\033\033\033üüüüüüûûûûûûøøøøíí\033\033\033\033"+
//            "üüüûûûûûûûøøøøøø\033\033\033\033ûûûûûûûûøøøøøøììììûûûûûûûøøøøøøëìììûûûûûøøøøøøøëëìì"+
//            "ûûûùùùùùùøëëëë\036ùùùùùùùùëëëë%%%%%%\036\036\036\036\036ùùùùùùùëëëë%%%%%%%%%%%\036\036\036\036\036\036\036\036\036\036ùùùùùùëëëë"+
//            "%%%%%%%%%%%8\036\036\036\036\036\036\036\036\036\036úùùùùù\034\034\034\034%%%%%%%%%%888888\036\036\036\036\036úúúúúùèè\034\034\034888888888\036\036úúúúúúèèè\03488888999BBúúúúúèèè"+
//            "9999999BBBúúúèè9999999BBBúúè&999::::::ê&&&&::::::êêê"+
//            "©&&&&DDD;;;;êêêªªªªª©©©©©©©©©©©&&DD;;;;;CCCCCêêªªªªªª©©©©©©©©©©OOOOO;;;<<CCCC\177ªªªªªªª©©©©©©©©©©OOOOOPP<<<<C\177\177\177"+
//            "«««««««««««««««««OOOPPPPPP<<\016\016\177\177««««««««««««««««««PPPPPPPPP\016\016\016\016\016(((((((((««««««««««'PPPPPPP\016\016aaa(((((((((((((«««««''''''''''aaa\017"+
//            "\037\037\037\037\037\037\037\037\037\037üüüüüüûûûûûûøííí\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037üüüüüüûûûûûûøííí\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037üüüüüüûûûûûûøøíí\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037üüüüüûûûûûûûøøíí\033\033\033\033\033\033"+
//            "\037\037\037\037\037\037\037\037\037\037üüüüûûûûûûûûøøøí\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037üüüüûûûûûûûûøøøø\033\033\033\033\033\033üüûûûûûûûûûøøøøø\033\033\033\033\033ûûûûûûûûûûøøøøøø\033\033\033\033\033"+
//            "ûûûûûûûûøøøøøøøììììûûûûûûûøøøøøøøììììûûûûûûøøøøøøøììììûûûûûøøøøøøøëììì"+
//            "ûûùùùùùùùùëëìì\036\036ùùùùùùùùëëëë%%%%%%%%\036\036\036\036\036ùùùùùùùùëëë\034%%%%%%%%%%%\036\036\036\036\036\036\036\036\036\036ùùùùùùù\034\034\034\034"+
//            "%%%%%%%%%%%\036\036\036\036\036\036\036\036\036\036\036úùùùùù\034\034\034\034%%%%%%%%%%%%\036\036\036\036\036\036\036\036\036úúúúúùù\034\034\034\034%888\036\036\036\036úúúúúúúè\034\034\034999999úúúúúúúèè\034"+
//            "9999999úúúúúúèè99999:::úúúúê&&&&:::::::\035\035\035êêê&&&&&&::::;\035\035\035\035\035êêêê"+
//            "&&&&&&;;;;;\035\035\035\035êêêêªªªªªªªªª©©©©©&&&&&;;;;;CCCCCêêêªªªªªªªªªª©©©©©©&&;;;;;<<CCCCªªªªªªªªª«««««©©©OOOPPP<<<<<C"+
//            "««««««««««««««««««PPPPPPP<<<\016\016««««««««««««««««««PPPPPPPP<\016\016\016\016\016(((((((((«««««««««''''''''P\016\016\016\017\017(((((((((((((«««««''''''''''a\017\017\017"+
//            "\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøø\033\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøø\033\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøø\033\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøø\033\033\033\033\033\033\033"+
//            "\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøøø\033\033\033\033\033\033\037\037\037\037\037\037\037\037\037\037\037ûûûûûûûûûûøøøøø\033\033\033\033\033\033ûûûûûûûûûûøøøøøø\033\033\033\033ìûûûûûûûûûøøøøøøììììì"+
//            "ûûûûûûûûøøøøøøìììììûûûûûûøøøøøøøøììììûûûûûøøøøøøøøììììûûûøøøøøøøøìììì"+
//            "ûùùùùùùùùùëììì%%%%%\036\036ùùùùùùùùù\034\034\034ì%%%%%%%%%\036\036\036\036\036\036ùùùùùùùù\034\034\034\034%%%%%%%%%%%\036\036\036\036\036\036\036\036\036\036ùùùùùùù\034\034\034\034"+
//            "%%%%%%%%%%%\036\036\036\036\036\036\036\036\036\036\036úùùùùù\034\034\034\034%%%%%%%%%%%\036\036\036\036\036\036\036\036\036úúúúúúù\034\034\034\034\034\036\036\036\036\036\036úúúúúúú\034\034\034\0349999úúúúúúúú\034\034"+
//            "9999:úúúúúúú\034&&:::::úúúúêê&&&&&:::::\035\035\035\035\035\035êêêê&&&&&&::;;\035\035\035\035\035\035êêêê"+
//            "&&&&&&&;;;;\035\035\035\035\035êêêêªªªªªªªªªªªª&&&&&&&;;;;;CCCCCêêêªªªªªªªªªªªªªª&&&&;;;;;<<CCCªªªªªªªª«««««««««&PPPP<<<<<C"+
//            "««««««««««««««««««PPPPPP<<<\016\016««««««««««««««««««''''PPPP\016\016\016\016\017\017(((((((((««««««««''''''''''\016\017\017\017\017(((((((((((((««««'''''''''''\017\017\017\017"
//            ;

    /**
     * Constructs a default PaletteReducer that uses the DawnBringer Aurora palette.
     */
    public PaletteReducer() {
        //this(Coloring.AURORA);
        exact(AURORA_RELAXED, ENCODED_AURORA);
    }

    /**
     * Constructs a PaletteReducer that uses the given array of RGBA8888 ints as a palette (see {@link #exact(int[])}
     * for more info).
     *
     * @param rgbaPalette an array of RGBA8888 ints to use as a palette
     */
    public PaletteReducer(int[] rgbaPalette) {
        paletteMapping = new byte[0x8000];
        exact(rgbaPalette);
    }

    /**
     * Constructs a PaletteReducer that uses the given array of RGBA8888 ints as a palette (see {@link #exact(int[])}
     * for more info).
     *
     * @param rgbaPalette an array of RGBA8888 ints to use as a palette
     * @param metric      almost always either {@link #basicMetric}, which is faster, or {@link #labMetric}, which may be better
     */
    public PaletteReducer(int[] rgbaPalette, ColorMetric metric) {
        paletteMapping = new byte[0x8000];
        exact(rgbaPalette, metric);
    }

    /**
     * Constructs a PaletteReducer that uses the given array of Color objects as a palette (see {@link #exact(Color[])}
     * for more info).
     *
     * @param colorPalette an array of Color objects to use as a palette
     */
    public PaletteReducer(Color[] colorPalette) {
        paletteMapping = new byte[0x8000];
        exact(colorPalette);
    }

    /**
     * Constructs a PaletteReducer that uses the given Array of Color objects as a palette (see {@link #exact(Color[])}
     * for more info).
     *
     * @param colorPalette an array of Color objects to use as a palette
     */
    public PaletteReducer(Array<Color> colorPalette) {
        paletteMapping = new byte[0x8000];
        if (colorPalette != null)
            exact(colorPalette.items, colorPalette.size);
        else
            exact(AURORA_RELAXED, ENCODED_AURORA);
    }

    /**
     * Constructs a PaletteReducer that analyzes the given Pixmap for color count and frequency to generate a palette
     * (see {@link #analyze(Pixmap)} for more info).
     *
     * @param pixmap a Pixmap to analyze in detail to produce a palette
     */
    public PaletteReducer(Pixmap pixmap) {
        paletteMapping = new byte[0x8000];
        analyze(pixmap);
    }
    /**
     * Constructs a PaletteReducer that uses the given array of RGBA8888 ints as a palette (see {@link #exact(int[])}
     * for more info) and an encoded String to use to look up pre-loaded color data.
     *
     * @param palette an array of RGBA8888 ints to use as a palette
     * @param preload an ISO-8859-1-encoded String containing preload data
     */
    public PaletteReducer(int[] palette, String preload)
    {
        exact(palette, preload);
    }
    /**
     * Constructs a PaletteReducer that analyzes the given Pixmap for color count and frequency to generate a palette
     * (see {@link #analyze(Pixmap, int)} for more info).
     *
     * @param pixmap    a Pixmap to analyze in detail to produce a palette
     * @param threshold the minimum difference between colors required to put them in the palette (default 400)
     */
    public PaletteReducer(Pixmap pixmap, int threshold) {
        paletteMapping = new byte[0x8000];
        analyze(pixmap, threshold);
    }

    /**
     * Color difference metric; returns large numbers even for smallish differences.
     * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
     *
     * @param color1 an RGBA8888 color as an int
     * @param color2 an RGBA8888 color as an int
     * @return the difference between the given colors, as a positive double
     */
    public static double difference(final int color1, final int color2) {
        // if one color is transparent and the other isn't, then this is max-different
        if(((color1 ^ color2) & 0x80) == 0x80) return Double.POSITIVE_INFINITY;
        final int r1 = (color1 >>> 24), g1 = (color1 >>> 16 & 0xFF), b1 = (color1 >>> 8 & 0xFF),
                r2 = (color2 >>> 24), g2 = (color2 >>> 16 & 0xFF), b2 = (color2 >>> 8 & 0xFF),
                rmean = r1 + r2,
                r = r1 - r2,
                g = g1 - g2,
                b = b1 - b2,
                y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
//        return (((512 + rmean) * r * r) >> 8) + g * g + (((767 - rmean) * b * b) >> 8);
        return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
    }

    /**
     * Color difference metric; returns large numbers even for smallish differences.
     * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
     *
     * @param color1 an RGBA8888 color as an int
     * @param r2     red value from 0 to 255, inclusive
     * @param g2     green value from 0 to 255, inclusive
     * @param b2     blue value from 0 to 255, inclusive
     * @return the difference between the given colors, as a positive double
     */
    public static double difference(final int color1, int r2, int g2, int b2) {
        if((color1 & 0x80) == 0) return Double.POSITIVE_INFINITY; // if a transparent color is being compared, it is always different
        final int
                r1 = (color1 >>> 24),
                g1 = (color1 >>> 16 & 0xFF),
                b1 = (color1 >>> 8 & 0xFF),
                rmean = (r1 + r2),
                r = r1 - r2,
                g = g1 - g2,
                b = b1 - b2,
                y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
        return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
    }

    /**
     * Color difference metric; returns large numbers even for smallish differences.
     * If this returns 250 or more, the colors may be perceptibly different; 500 or more almost guarantees it.
     *
     * @param r1 red value from 0 to 255, inclusive
     * @param g1 green value from 0 to 255, inclusive
     * @param b1 blue value from 0 to 255, inclusive
     * @param r2 red value from 0 to 255, inclusive
     * @param g2 green value from 0 to 255, inclusive
     * @param b2 blue value from 0 to 255, inclusive
     * @return the difference between the given colors, as a positive double
     */
    public static double difference(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
        final int rmean = (r1 + r2),
                r = r1 - r2,
                g = g1 - g2 << 1,
                b = b1 - b2,
                y = Math.max(r1, Math.max(g1, b1)) - Math.max(r2, Math.max(g2, b2));
//        return (((512 + rmean) * r * r) >> 8) + g * g + (((767 - rmean) * b * b) >> 8);
//        return (((1024 + rmean) * r * r) >> 9) + g * g + (((1534 - rmean) * b * b) >> 9) + y * y * 5;
        return (((1024 + rmean) * r * r) >> 7) + g * g * 12 + (((1534 - rmean) * b * b) >> 8) + y * y * 14;
    }

    /**
     * Gets a pseudo-random float between -0.65625f and 0.65625f, determined by the upper 23 bits of seed.
     * This currently uses a uniform distribution for its output, but earlier versions intentionally used a non-uniform
     * one; a non-uniform distribution can sometimes work well but is very dependent on how error propagates through a
     * dithered image, and in bad cases can produce bands of bright mistakenly-error-adjusted colors.
     * @param seed any int, but only the most-significant 23 bits will be used
     * @return a float between -0.65625f and 0.65625f, with fairly uniform distribution as long as seed is uniform
     */
    static float randomXi(int seed)
    {
        return ((seed >> 9) * 0x1.5p-23f);
//        return NumberUtils.intBitsToFloat((seed & 0x7FFFFF & ((seed >>> 11 & 0x400000)|0x3FFFFF)) | 0x3f800000) - 1.4f;
//        return NumberUtils.intBitsToFloat((seed & 0x7FFFFF & ((seed >>> 11 & 0x600000)|0x1FFFFF)) | 0x3f800000) - 1.3f;
    }

    /**
     * Builds the palette information this AnimatedPNG8 stores from the RGBA8888 ints in {@code rgbaPalette}, up to 256 colors.
     * Alpha is not preserved except for the first item in rgbaPalette, and only if it is {@code 0} (fully transparent
     * black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, or only has one color, then
     * this defaults to DawnBringer's Aurora palette with 256 hand-chosen colors (including transparent).
     *
     * @param rgbaPalette an array of RGBA8888 ints; all will be used up to 256 items or the length of the array
     */
    public void exact(int[] rgbaPalette) {
        exact(rgbaPalette, basicMetric);
    }
    /**
     * Builds the palette information this AnimatedPNG8 stores from the RGBA8888 ints in {@code rgbaPalette}, up to 256 colors.
     * Alpha is not preserved except for the first item in rgbaPalette, and only if it is {@code 0} (fully transparent
     * black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, or only has one color, then
     * this defaults to DawnBringer's Aurora palette with 256 hand-chosen colors (including transparent).
     *
     * @param rgbaPalette an array of RGBA8888 ints; all will be used up to 256 items or the length of the array
     * @param metric      almost always either {@link #basicMetric}, which is faster, or {@link #labMetric}, which may be better
     */
    public void exact(int[] rgbaPalette, ColorMetric metric) {
        if (rgbaPalette == null || rgbaPalette.length < 2) {
            exact(AURORA_RELAXED, ENCODED_AURORA);
            return;
        }
        Arrays.fill(paletteArray, 0);
        Arrays.fill(paletteMapping, (byte) 0);
        final int plen = Math.min(256, rgbaPalette.length);
        int color, c2;
        double dist;
        for (int i = 0; i < plen; i++) {
            color = rgbaPalette[i];
            if ((color & 0x80) != 0) {
                paletteArray[i] = color;
                paletteMapping[(color >>> 17 & 0x7C00) | (color >>> 14 & 0x3E0) | (color >>> 11 & 0x1F)] = (byte) i;
            }
        }
        int rr, gg, bb;
        for (int r = 0; r < 32; r++) {
            rr = (r << 3 | r >>> 2);
            for (int g = 0; g < 32; g++) {
                gg = (g << 3 | g >>> 2);
                for (int b = 0; b < 32; b++) {
                    c2 = r << 10 | g << 5 | b;
                    if (paletteMapping[c2] == 0) {
                        bb = (b << 3 | b >>> 2);
                        dist = 0x7FFFFFFF;
                        for (int i = 1; i < plen; i++) {
                            if (dist > (dist = Math.min(dist, metric.difference(paletteArray[i], rr, gg, bb))))
                                paletteMapping[c2] = (byte) i;
                        }
                    }
                }
            }
        }
//        generatePreloadCode(paletteMapping);
    }

//    /**
//     * Given a byte array, this writes a file containing a code snippet that can be pasted into Java code as the preload
//     * data used by {@link #exact(int[], String)}; this is almost never needed by external code. When using this for
//     * preload data, the byte array should be {@link #paletteMapping}.
//     * @param data the bytes to use as preload data, usually the {@link #paletteMapping} of a PaletteReducer
//     */
//    @GwtIncompatible
//    public static void generatePreloadCode(final byte[] data){
//        StringBuilder sb = new StringBuilder(data.length);
//        for (int i = 0; i < data.length;) {
//            sb.append('"');
//            for (int j = 0; j < 0x80 && i < data.length; j++) {
//                byte b = data[i++];
//                switch (b)
//                {
//                    case '\t': sb.append("\\t");
//                        break;
//                    case '\b': sb.append("\\b");
//                        break;
//                    case '\n': sb.append("\\n");
//                        break;
//                    case '\r': sb.append("\\r");
//                        break;
//                    case '\f': sb.append("\\f");
//                        break;
//                    case '\"': sb.append("\\\"");
//                        break;
//                    case '\\': sb.append("\\\\");
//                        break;
//                    default:
//                        if(Character.isISOControl(b))
//                            sb.append(String.format("\\%03o", b));
//                        else
//                            sb.append((char)(b&0xFF));
//                        break;
//                }
//            }
//            sb.append('"');
//            if(i != data.length)
//                sb.append('+');
//            sb.append('\n');
//        }
//        String filename = "bytes_" + StringKit.hexHash(data) + ".txt";
//        Gdx.files.local(filename).writeString(sb.toString(), false, "ISO-8859-1");
//        System.out.println("Wrote code snippet to " + filename);
//    }
    /**
     * Builds the palette information this PaletteReducer stores from the given array of RGBA8888 ints as a palette (see
     * {@link #exact(int[])} for more info) and an encoded String to use to look up pre-loaded color data. The encoded
     * string is going to be hard to produce if you intend to do this from outside WarpWriter, but there is a
     * generatePreloadCode() method in this class if you're hacking on WarpWriter. For external code, there's slightly
     * more startup time spent when initially calling {@link #exact(int[])}, but it will produce the same result. 
     *
     * @param palette an array of RGBA8888 ints to use as a palette
     * @param preload an ISO-8859-1-encoded String containing preload data
     */
    public void exact(int[] palette, String preload)
    {
        for (int i = 0; i < 256 & i < palette.length; i++) {
            int color = palette[i];
            if((color & 0x80) != 0)
                paletteArray[i] = color;
        }
        try {
            paletteMapping = preload.getBytes("ISO-8859-1"); // don't use StandardCharsets; not supported on GWT
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            paletteMapping = new byte[0x8000];
        }
    }

    /**
     * Builds the palette information this PaletteReducer stores from the Color objects in {@code colorPalette}, up to
     * 256 colors.
     * Alpha is not preserved except for the first item in colorPalette, and only if its r, g, b, and a values are all
     * 0f (fully transparent black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, or only
     * has one color, then this defaults to DawnBringer's Aurora palette with 256 hand-chosen colors (including
     * transparent).
     *
     * @param colorPalette an array of Color objects; all will be used up to 256 items or the length of the array
     */
    public void exact(Color[] colorPalette) {
        exact(colorPalette, 256, basicMetric);
    }

    /**
     * Builds the palette information this PaletteReducer stores from the Color objects in {@code colorPalette}, up to
     * 256 colors.
     * Alpha is not preserved except for the first item in colorPalette, and only if its r, g, b, and a values are all
     * 0f (fully transparent black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, or only
     * has one color, then this defaults to DawnBringer's Aurora palette with 256 hand-chosen colors (including
     * transparent).
     *
     * @param colorPalette an array of Color objects; all will be used up to 256 items or the length of the array
     * @param metric       almost always either {@link #basicMetric}, which is faster, or {@link #labMetric}, which may be better
     */
    public void exact(Color[] colorPalette, ColorMetric metric) {
        exact(colorPalette, 256, metric);
    }

    /**
     * Builds the palette information this PaletteReducer stores from the Color objects in {@code colorPalette}, up to
     * 256 colors.
     * Alpha is not preserved except for the first item in colorPalette, and only if its r, g, b, and a values are all
     * 0f (fully transparent black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, only has
     * one color, or limit is less than 2, then this defaults to DawnBringer's Aurora palette with 256 hand-chosen
     * colors (including transparent).
     *
     * @param colorPalette an array of Color objects; all will be used up to 256 items, limit, or the length of the array
     * @param limit        a limit on how many Color items to use from colorPalette; useful if colorPalette is from an Array
     */
    public void exact(Color[] colorPalette, int limit) {
        exact(colorPalette, limit, basicMetric);
    }

    /**
     * Builds the palette information this PaletteReducer stores from the Color objects in {@code colorPalette}, up to
     * 256 colors.
     * Alpha is not preserved except for the first item in colorPalette, and only if its r, g, b, and a values are all
     * 0f (fully transparent black); otherwise all items are treated as opaque. If rgbaPalette is null, empty, only has
     * one color, or limit is less than 2, then this defaults to DawnBringer's Aurora palette with 256 hand-chosen
     * colors (including transparent).
     *
     * @param colorPalette an array of Color objects; all will be used up to 256 items, limit, or the length of the array
     * @param limit        a limit on how many Color items to use from colorPalette; useful if colorPalette is from an Array
     * @param metric       almost always either {@link #basicMetric}, which is faster, or {@link #labMetric}, which may be better
     */
    public void exact(Color[] colorPalette, int limit, ColorMetric metric) {
        if (colorPalette == null || colorPalette.length < 2 || limit < 2) {
            exact(AURORA_RELAXED, ENCODED_AURORA);
            return;
        }
        Arrays.fill(paletteArray, 0);
        Arrays.fill(paletteMapping, (byte) 0);
        final int plen = Math.min(Math.min(256, colorPalette.length), limit);
        int color, c2;
        double dist;
        for (int i = 0; i < plen; i++) {
            color = Color.rgba8888(colorPalette[i]);
            paletteArray[i] = color;
            paletteMapping[(color >>> 17 & 0x7C00) | (color >>> 14 & 0x3E0) | (color >>> 11 & 0x1F)] = (byte) i;
        }
        int rr, gg, bb;
        for (int r = 0; r < 32; r++) {
            rr = (r << 3 | r >>> 2);
            for (int g = 0; g < 32; g++) {
                gg = (g << 3 | g >>> 2);
                for (int b = 0; b < 32; b++) {
                    c2 = r << 10 | g << 5 | b;
                    if (paletteMapping[c2] == 0) {
                        bb = (b << 3 | b >>> 2);
                        dist = 0x7FFFFFFF;
                        for (int i = 1; i < plen; i++) {
                            if (dist > (dist = Math.min(dist, metric.difference(paletteArray[i], rr, gg, bb))))
                                paletteMapping[c2] = (byte) i;
                        }
                    }
                }
            }
        }
    }
    /**
     * Analyzes {@code pixmap} for color count and frequency, building a palette with at most 256 colors if there are
     * too many colors to store in a PNG-8 palette. If there are 256 or less colors, this uses the exact colors
     * (although with at most one transparent color, and no alpha for other colors); if there are more than 256 colors
     * or any colors have 50% or less alpha, it will reserve a palette entry for transparent (even if the image has no
     * transparency). Because calling {@link #reduce(Pixmap)} (or any of AnimatedPNG8's write methods) will dither colors that
     * aren't exact, and dithering works better when the palette can choose colors that are sufficiently different, this
     * uses a threshold value to determine whether it should permit a less-common color into the palette, and if the
     * second color is different enough (as measured by {@link #difference(int, int)}) by a value of at least 400, it is
     * allowed in the palette, otherwise it is kept out for being too similar to existing colors. This doesn't return a
     * value but instead stores the palette info in this object; a PaletteReducer can be assigned to the
     * {@link AnimatedPNG8#palette} field or can be used directly to {@link #reduce(Pixmap)} a Pixmap.
     *
     * @param pixmap a Pixmap to analyze, making a palette which can be used by this to {@link #reduce(Pixmap)} or by AnimatedPNG8
     */
    public void analyze(Pixmap pixmap) {
        analyze(pixmap, 400);
    }

    private static final Comparator<IntIntMap.Entry> entryComparator = new Comparator<IntIntMap.Entry>() {
        @Override
        public int compare(IntIntMap.Entry o1, IntIntMap.Entry o2) {
            return o2.value - o1.value;
        }
    };


    /**
     * Analyzes {@code pixmap} for color count and frequency, building a palette with at most 256 colors if there are
     * too many colors to store in a PNG-8 palette. If there are 256 or less colors, this uses the exact colors
     * (although with at most one transparent color, and no alpha for other colors); if there are more than 256 colors
     * or any colors have 50% or less alpha, it will reserve a palette entry for transparent (even if the image has no
     * transparency). Because calling {@link #reduce(Pixmap)} (or any of AnimatedPNG8's write methods) will dither colors that
     * aren't exact, and dithering works better when the palette can choose colors that are sufficiently different, this
     * takes a threshold value to determine whether it should permit a less-common color into the palette, and if the
     * second color is different enough (as measured by {@link #difference(int, int)}) by a value of at least
     * {@code threshold}, it is allowed in the palette, otherwise it is kept out for being too similar to existing
     * colors. The threshold is usually between 250 and 1000, and 400 is a good default. This doesn't return a value but
     * instead stores the palette info in this object; a PaletteReducer can be assigned to the {@link AnimatedPNG8#palette}
     * field or can be used directly to {@link #reduce(Pixmap)} a Pixmap.
     *
     * @param pixmap    a Pixmap to analyze, making a palette which can be used by this to {@link #reduce(Pixmap)} or by AnimatedPNG8
     * @param threshold a minimum color difference as produced by {@link #difference(int, int)}; usually between 250 and 1000, 400 is a good default
     */
    public void analyze(Pixmap pixmap, int threshold) {
        analyze(pixmap, threshold, 256);
    }
    /**
     * Analyzes {@code pixmap} for color count and frequency, building a palette with at most 256 colors if there are
     * too many colors to store in a PNG-8 palette. If there are 256 or less colors, this uses the exact colors
     * (although with at most one transparent color, and no alpha for other colors); if there are more than 256 colors
     * or any colors have 50% or less alpha, it will reserve a palette entry for transparent (even if the image has no
     * transparency). Because calling {@link #reduce(Pixmap)} (or any of AnimatedPNG8's write methods) will dither colors that
     * aren't exact, and dithering works better when the palette can choose colors that are sufficiently different, this
     * takes a threshold value to determine whether it should permit a less-common color into the palette, and if the
     * second color is different enough (as measured by {@link #difference(int, int)}) by a value of at least
     * {@code threshold}, it is allowed in the palette, otherwise it is kept out for being too similar to existing
     * colors. The threshold is usually between 250 and 1000, and 400 is a good default. This doesn't return a value but
     * instead stores the palette info in this object; a PaletteReducer can be assigned to the {@link AnimatedPNG8#palette}
     * field or can be used directly to {@link #reduce(Pixmap)} a Pixmap.
     *
     * @param pixmap    a Pixmap to analyze, making a palette which can be used by this to {@link #reduce(Pixmap)} or by AnimatedPNG8
     * @param threshold a minimum color difference as produced by {@link #difference(int, int)}; usually between 250 and 1000, 400 is a good default
     */
    public void analyze(Pixmap pixmap, int threshold, int limit) {
        Arrays.fill(paletteArray, 0);
        Arrays.fill(paletteMapping, (byte) 0);
        int color;
        final int width = pixmap.getWidth(), height = pixmap.getHeight();
        IntIntMap counts = new IntIntMap(limit);
        int hasTransparent = 0;
        int[] reds = new int[limit], greens = new int[limit], blues = new int[limit];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                color = pixmap.getPixel(x, y);
                if ((color & 0x80) != 0) {
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    counts.getAndIncrement(color, 0, 1);
                } else {
                    hasTransparent = 1;
                }
            }
        }
        final int cs = counts.size;
        ArrayList<IntIntMap.Entry> es = new ArrayList<>(cs);
        for(IntIntMap.Entry e : counts)
        {
            IntIntMap.Entry e2 = new IntIntMap.Entry();
            e2.key = e.key;
            e2.value = e.value;
            es.add(e2);
        }
        Collections.sort(es, entryComparator);
        if (cs + hasTransparent <= limit) {
            int i = hasTransparent;
            for(IntIntMap.Entry e : es) {
                color = e.key;
                paletteArray[i] = color;
                color = (color >>> 17 & 0x7C00) | (color >>> 14 & 0x3E0) | (color >>> 11 & 0x1F);
                paletteMapping[color] = (byte) i;
                reds[i] = color >>> 10;
                greens[i] = color >>> 5 & 31;
                blues[i] = color & 31;
                i++;
            }
        } else // reduce color count
        {
            int i = 1, c = 0;
            PER_BEST:
            for (; i < limit && c < cs;) {
                color = es.get(c++).key;
                for (int j = 1; j < i; j++) {
                    if (difference(color, paletteArray[j]) < threshold)
                        continue PER_BEST;
                }
                paletteArray[i] = color;
                color = (color >>> 17 & 0x7C00) | (color >>> 14 & 0x3E0) | (color >>> 11 & 0x1F);
                paletteMapping[color] = (byte) i;
                reds[i] = color >>> 10;
                greens[i] = color >>> 5 & 31;
                blues[i] = color & 31;
                i++;
            }
        }
        int c2;
        double dist;
        for (int r = 0; r < 32; r++) {
            for (int g = 0; g < 32; g++) {
                for (int b = 0; b < 32; b++) {
                    c2 = r << 10 | g << 5 | b;
                    if (paletteMapping[c2] == 0) {
                        dist = Double.POSITIVE_INFINITY;
                        for (int i = 1; i < limit; i++) {
                            if (dist > (dist = Math.min(dist, difference(reds[i], greens[i], blues[i], r, g, b))))
                                paletteMapping[c2] = (byte) i;
                        }
                    }
                }
            }
        }
    }

    /**
     * Changes the "strength" of the dither effect applied during {@link #reduce(Pixmap)} calls. The default is 1f,
     * and while both values higher than 1f and lower than 1f are valid, they should not be negative. If you want dither
     * to be eliminated, don't set dither strength to 0; use {@link #reduceSolid(Pixmap)} instead of reduce().
     * @param ditherStrength dither strength as a non-negative float that should be close to 1f
     */
    public void setDitherStrength(float ditherStrength) {
        this.ditherStrength = 0.5f * ditherStrength;
        this.halfDitherStrength = 0.25f * ditherStrength;
    }

    /**
     * Modifies the given Pixmap so it only uses colors present in this PaletteReducer, dithering when it can.
     * If you want to reduce the colors in a Pixmap based on what it currently contains, call
     * {@link #analyze(Pixmap)} with {@code pixmap} as its argument, then call this method with the same
     * Pixmap. You may instead want to use a known palette instead of one computed from a Pixmap;
     * {@link #exact(int[])} is the tool for that job.
     * <p>
     * This method is not incredibly fast because of the extra calculations it has to do for dithering, but if you can
     * compute the PaletteReducer once and reuse it, that will save some time.
     * @param pixmap a Pixmap that will be modified in place
     * @return the given Pixmap, for chaining
     */
    public Pixmap reduce (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        byte[] curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue;
        if (curErrorRedBytes == null) {
            curErrorRed = (curErrorRedBytes = new ByteArray(lineLen)).items;
            nextErrorRed = (nextErrorRedBytes = new ByteArray(lineLen)).items;
            curErrorGreen = (curErrorGreenBytes = new ByteArray(lineLen)).items;
            nextErrorGreen = (nextErrorGreenBytes = new ByteArray(lineLen)).items;
            curErrorBlue = (curErrorBlueBytes = new ByteArray(lineLen)).items;
            nextErrorBlue = (nextErrorBlueBytes = new ByteArray(lineLen)).items;
        } else {
            curErrorRed = curErrorRedBytes.ensureCapacity(lineLen);
            nextErrorRed = nextErrorRedBytes.ensureCapacity(lineLen);
            curErrorGreen = curErrorGreenBytes.ensureCapacity(lineLen);
            nextErrorGreen = nextErrorGreenBytes.ensureCapacity(lineLen);
            curErrorBlue = curErrorBlueBytes.ensureCapacity(lineLen);
            nextErrorBlue = nextErrorBlueBytes.ensureCapacity(lineLen);
            for (int i = 0; i < lineLen; i++) {
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }

        }
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used, rdiff, gdiff, bdiff;
        byte er, eg, eb, paletteIndex;
        for (int y = 0; y < h; y++) {
            int ny = y + 1;
            for (int i = 0; i < lineLen; i++) {
                curErrorRed[i] = nextErrorRed[i];
                curErrorGreen[i] = nextErrorGreen[i];
                curErrorBlue[i] = nextErrorBlue[i];
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    er = curErrorRed[px];
                    eg = curErrorGreen[px];
                    eb = curErrorBlue[px];
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = MathUtils.clamp(((color >>> 24)       ) + (er), 0, 0xFF);
                    int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (eg), 0, 0xFF);
                    int bb = MathUtils.clamp(((color >>> 8)  & 0xFF) + (eb), 0, 0xFF);
                    paletteIndex =
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))];
                    used = paletteArray[paletteIndex & 0xFF];
                    pixmap.drawPixel(px, y, used);
                    rdiff = (color>>>24)-    (used>>>24);
                    gdiff = (color>>>16&255)-(used>>>16&255);
                    bdiff = (color>>>8&255)- (used>>>8&255);
                    if(px < lineLen - 1)
                    {
                        curErrorRed[px+1]   += rdiff * ditherStrength;
                        curErrorGreen[px+1] += gdiff * ditherStrength;
                        curErrorBlue[px+1]  += bdiff * ditherStrength;
                    }
                    if(ny < h)
                    {
                        if(px > 0)
                        {
                            nextErrorRed[px-1]   += rdiff * halfDitherStrength;
                            nextErrorGreen[px-1] += gdiff * halfDitherStrength;
                            nextErrorBlue[px-1]  += bdiff * halfDitherStrength;
                        }
                        nextErrorRed[px]   += rdiff * halfDitherStrength;
                        nextErrorGreen[px] += gdiff * halfDitherStrength;
                        nextErrorBlue[px]  += bdiff * halfDitherStrength;
                    }
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    /**
     * Modifies the given Pixmap so it only uses colors present in this PaletteReducer, without dithering. This produces
     * blocky solid sections of color in most images where the palette isn't exact, instead of checkerboard-like
     * dithering patterns. If you want to reduce the colors in a Pixmap based on what it currently contains, call
     * {@link #analyze(Pixmap)} with {@code pixmap} as its argument, then call this method with the same
     * Pixmap. You may instead want to use a known palette instead of one computed from a Pixmap;
     * {@link #exact(int[])} is the tool for that job.
     * @param pixmap a Pixmap that will be modified in place
     * @return the given Pixmap, for chaining
     */
    public Pixmap reduceSolid (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color;
        for (int y = 0; y < h; y++) {
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y);
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    int rr = ((color >>> 24)       );
                    int gg = ((color >>> 16) & 0xFF);
                    int bb = ((color >>> 8)  & 0xFF);
                    pixmap.drawPixel(px, y, paletteArray[
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))] & 0xFF]);
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    /**
     * Modifies the given Pixmap so it only uses colors present in this PaletteReducer, dithering when it can using
     * Burkes dithering instead of the Sierra Lite dithering that {@link #reduce(Pixmap)} uses.
     * If you want to reduce the colors in a Pixmap based on what it currently contains, call
     * {@link #analyze(Pixmap)} with {@code pixmap} as its argument, then call this method with the same
     * Pixmap. You may instead want to use a known palette instead of one computed from a Pixmap;
     * {@link #exact(int[])} is the tool for that job.
     * <p>
     * This method is not incredibly fast because of the extra calculations it has to do for dithering, but if you can
     * compute the PaletteReducer once and reuse it, that will save some time. Burkes dithering causes error to be
     * propagated to more than twice as many pixels as Sierra Lite (7 instead of 3), but both only affect one row ahead
     * of the pixel that is currently being dithered. For small images, the time spent dithering should be negligible.
     * @param pixmap a Pixmap that will be modified in place
     * @return the given Pixmap, for chaining
     */
    public Pixmap reduceBurkes (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        float r4, r2, r1, g4, g2, g1, b4, b2, b1;
        byte[] curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue;
        if (curErrorRedBytes == null) {
            curErrorRed = (curErrorRedBytes = new ByteArray(lineLen)).items;
            nextErrorRed = (nextErrorRedBytes = new ByteArray(lineLen)).items;
            curErrorGreen = (curErrorGreenBytes = new ByteArray(lineLen)).items;
            nextErrorGreen = (nextErrorGreenBytes = new ByteArray(lineLen)).items;
            curErrorBlue = (curErrorBlueBytes = new ByteArray(lineLen)).items;
            nextErrorBlue = (nextErrorBlueBytes = new ByteArray(lineLen)).items;
        } else {
            curErrorRed = curErrorRedBytes.ensureCapacity(lineLen);
            nextErrorRed = nextErrorRedBytes.ensureCapacity(lineLen);
            curErrorGreen = curErrorGreenBytes.ensureCapacity(lineLen);
            nextErrorGreen = nextErrorGreenBytes.ensureCapacity(lineLen);
            curErrorBlue = curErrorBlueBytes.ensureCapacity(lineLen);
            nextErrorBlue = nextErrorBlueBytes.ensureCapacity(lineLen);
            for (int i = 0; i < lineLen; i++) {
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }

        }
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used, rdiff, gdiff, bdiff;
        byte er, eg, eb, paletteIndex;
        for (int y = 0; y < h; y++) {
            int ny = y + 1;
            for (int i = 0; i < lineLen; i++) {
                curErrorRed[i] = nextErrorRed[i];
                curErrorGreen[i] = nextErrorGreen[i];
                curErrorBlue[i] = nextErrorBlue[i];
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    er = curErrorRed[px];
                    eg = curErrorGreen[px];
                    eb = curErrorBlue[px];
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = MathUtils.clamp(((color >>> 24)       ) + (er), 0, 0xFF);
                    int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (eg), 0, 0xFF);
                    int bb = MathUtils.clamp(((color >>> 8)  & 0xFF) + (eb), 0, 0xFF);
                    paletteIndex =
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))];
                    used = paletteArray[paletteIndex & 0xFF];
                    pixmap.drawPixel(px, y, used);
                    rdiff = (color>>>24)-    (used>>>24);
                    gdiff = (color>>>16&255)-(used>>>16&255);
                    bdiff = (color>>>8&255)- (used>>>8&255);
                    r4 = rdiff * halfDitherStrength;
                    g4 = gdiff * halfDitherStrength;
                    b4 = bdiff * halfDitherStrength;
                    r2 = r4 * 0.5f;
                    g2 = g4 * 0.5f;
                    b2 = b4 * 0.5f;
                    r1 = r4 * 0.25f;
                    g1 = g4 * 0.25f;
                    b1 = b4 * 0.25f;
                    if(px < lineLen - 1)
                    {
                        curErrorRed[px+1]   += r4;
                        curErrorGreen[px+1] += g4;
                        curErrorBlue[px+1]  += b4;
                        if(px < lineLen - 2)
                        {

                            curErrorRed[px+2]   += r2;
                            curErrorGreen[px+2] += g2;
                            curErrorBlue[px+2]  += b2;
                        }
                    }
                    if(ny < h)
                    {
                        if(px > 0)
                        {
                            nextErrorRed[px-1]   += r2;
                            nextErrorGreen[px-1] += g2;
                            nextErrorBlue[px-1]  += b2;
                            if(px > 1)
                            {
                                nextErrorRed[px-2]   += r1;
                                nextErrorGreen[px-2] += g1;
                                nextErrorBlue[px-2]  += b1;
                            }
                        }
                        nextErrorRed[px]   += r4;
                        nextErrorGreen[px] += g4;
                        nextErrorBlue[px]  += b4;
                        if(px < lineLen - 1)
                        {
                            nextErrorRed[px+1]   += r2;
                            nextErrorGreen[px+1] += g2;
                            nextErrorBlue[px+1]  += b2;
                            if(px < lineLen - 2)
                            {

                                nextErrorRed[px+2]   += r1;
                                nextErrorGreen[px+2] += g1;
                                nextErrorBlue[px+2]  += b1;
                            }
                        }
                    }
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    /**
     * Modifies the given Pixmap so it only uses colors present in this PaletteReducer, dithering when it can using a
     * modified version of the algorithm presented in "Simple gradient-based error-diffusion method" by Xaingyu Y. Hu in
     * the Journal of Electronic Imaging, 2016. This algorithm uses pseudo-randomly-generated noise to adjust
     * Floyd-Steinberg dithering, with input for the pseudo-random state obtained by the non-transparent color values as
     * they are encountered. Very oddly, this tends to produce less random-seeming dither than
     * {@link #reduceBurkes(Pixmap)}, with this method often returning regular checkerboards where Burkes may produce
     * splotches of color. If you want to reduce the colors in a Pixmap based on what it currently contains, call
     * {@link #analyze(Pixmap)} with {@code pixmap} as its argument, then call this method with the same
     * Pixmap. You may instead want to use a known palette instead of one computed from a Pixmap;
     * {@link #exact(int[])} is the tool for that job.
     * <p>
     * This method is not incredibly fast because of the extra calculations it has to do for dithering, but if you can
     * compute the PaletteReducer once and reuse it, that will save some time. This method is probably slower than
     * {@link #reduceBurkes(Pixmap)} even though Burkes propagates error to more pixels, because this method also has to
     * generate two random values per non-transparent pixel. The random number "algorithm" this uses isn't very good
     * because it doesn't have to be good, it should just be fast and avoid clear artifacts; it's similar to one of
     * <a href="http://www.drdobbs.com/tools/fast-high-quality-parallel-random-number/231000484?pgno=2">Mark Overton's
     * subcycle generators</a> (which are usually paired, but that isn't the case here), but because it's
     * constantly being adjusted by additional colors as input, it may be more comparable to a rolling hash. This uses
     * {@link #randomXi(int)} to get the parameter in Hu's paper that's marked as {@code aξ}, but our randomXi() is
     * adjusted so it has half the range (from -0.5 to 0.5 instead of -1 to 1). That quirk ends up getting rather high
     * quality for this method, though it may have some grainy appearance in certain zones with mid-level intensity (an
     * acknowledged issue with the type of noise-based approach Hu uses, and not a very severe problem).
     * @param pixmap a Pixmap that will be modified in place
     * @return the given Pixmap, for chaining
     */
    public Pixmap reduceWithNoise (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        byte[] curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue;
        if (curErrorRedBytes == null) {
            curErrorRed = (curErrorRedBytes = new ByteArray(lineLen)).items;
            nextErrorRed = (nextErrorRedBytes = new ByteArray(lineLen)).items;
            curErrorGreen = (curErrorGreenBytes = new ByteArray(lineLen)).items;
            nextErrorGreen = (nextErrorGreenBytes = new ByteArray(lineLen)).items;
            curErrorBlue = (curErrorBlueBytes = new ByteArray(lineLen)).items;
            nextErrorBlue = (nextErrorBlueBytes = new ByteArray(lineLen)).items;
        } else {
            curErrorRed = curErrorRedBytes.ensureCapacity(lineLen);
            nextErrorRed = nextErrorRedBytes.ensureCapacity(lineLen);
            curErrorGreen = curErrorGreenBytes.ensureCapacity(lineLen);
            nextErrorGreen = nextErrorGreenBytes.ensureCapacity(lineLen);
            curErrorBlue = curErrorBlueBytes.ensureCapacity(lineLen);
            nextErrorBlue = nextErrorBlueBytes.ensureCapacity(lineLen);
            for (int i = 0; i < lineLen; i++) {
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }

        }
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used, rdiff, gdiff, bdiff, state = 0xFEEDBEEF;
        byte er, eg, eb, paletteIndex;
        //float xir1, xir2, xig1, xig2, xib1, xib2, // would be used if random factors were per-channel
        // used now, where random factors are determined by whole colors as ints
        float xi1, xi2, w1 = ditherStrength * 0.125f, w3 = w1 * 3f, w5 = w1 * 5f, w7 = w1 * 7f;
        for (int y = 0; y < h; y++) {
            int ny = y + 1;
            for (int i = 0; i < lineLen; i++) {
                curErrorRed[i] = nextErrorRed[i];
                curErrorGreen[i] = nextErrorGreen[i];
                curErrorBlue[i] = nextErrorBlue[i];
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    er = curErrorRed[px];
                    eg = curErrorGreen[px];
                    eb = curErrorBlue[px];
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = MathUtils.clamp(((color >>> 24)       ) + (er), 0, 0xFF);
                    int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (eg), 0, 0xFF);
                    int bb = MathUtils.clamp(((color >>> 8)  & 0xFF) + (eb), 0, 0xFF);
                    paletteIndex =
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))];
                    used = paletteArray[paletteIndex & 0xFF];
                    pixmap.drawPixel(px, y, used);
                    rdiff = (color>>>24)-    (used>>>24);
                    gdiff = (color>>>16&255)-(used>>>16&255);
                    bdiff = (color>>>8&255)- (used>>>8&255);
                    state += (color + 0x41C64E6D) ^ color >>> 7;
                    state = (state << 21 | state >>> 11);
                    xi1 = randomXi(state);
                    state ^= (state << 5 | state >>> 27) + 0x9E3779B9;
                    xi2 = randomXi(state);

//                    state += rdiff ^ rdiff << 9;
//                    state = (state << 21 | state >>> 11);
//                    xir1 = randomXi(state);
//                    state = (state << 21 | state >>> 11);
//                    xir2 = randomXi(state);
//                    state += gdiff ^ gdiff << 9;
//                    state = (state << 21 | state >>> 11);
//                    xig1 = randomXi(state);
//                    state = (state << 21 | state >>> 11);
//                    xig2 = randomXi(state);
//                    state += bdiff ^ bdiff << 9;
//                    state = (state << 21 | state >>> 11);
//                    xib1 = randomXi(state);
//                    state = (state << 21 | state >>> 11);
//                    xib2 = randomXi(state);
                    if(px < lineLen - 1)
                    {
                        curErrorRed[px+1]   += rdiff * w7 * (1f + xi1);
                        curErrorGreen[px+1] += gdiff * w7 * (1f + xi1);
                        curErrorBlue[px+1]  += bdiff * w7 * (1f + xi1);
                    }
                    if(ny < h)
                    {
                        if(px > 0)
                        {
                            nextErrorRed[px-1]   += rdiff * w3 * (1f + xi2);
                            nextErrorGreen[px-1] += gdiff * w3 * (1f + xi2);
                            nextErrorBlue[px-1]  += bdiff * w3 * (1f + xi2);
                        }
                        if(px < lineLen - 1)
                        {
                            nextErrorRed[px+1]   += rdiff * w1 * (1f - xi2);
                            nextErrorGreen[px+1] += gdiff * w1 * (1f - xi2);
                            nextErrorBlue[px+1]  += bdiff * w1 * (1f - xi2);
                        }
                        nextErrorRed[px]   += rdiff * w5 * (1f - xi1);
                        nextErrorGreen[px] += gdiff * w5 * (1f - xi1);
                        nextErrorBlue[px]  += bdiff * w5 * (1f - xi1);
                    }
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }
    /**
     * Modifies the given Pixmap so it only uses colors present in this PaletteReducer, dithering when it can using the
     * commonly-used Floyd-Steinberg dithering. If you want to reduce the colors in a Pixmap based on what it currently
     * contains, call {@link #analyze(Pixmap)} with {@code pixmap} as its argument, then call this method with the same
     * Pixmap. You may instead want to use a known palette instead of one computed from a Pixmap;
     * {@link #exact(int[])} is the tool for that job.
     * <p>
     * This method is not incredibly fast because of the extra calculations it has to do for dithering, but if you can
     * compute the PaletteReducer once and reuse it, that will save some time. This method is probably about the same
     * speed as {@link #reduceBurkes(Pixmap)}.
     * @param pixmap a Pixmap that will be modified in place
     * @return the given Pixmap, for chaining
     */
    public Pixmap reduceFloydSteinberg (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        byte[] curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue;
        if (curErrorRedBytes == null) {
            curErrorRed = (curErrorRedBytes = new ByteArray(lineLen)).items;
            nextErrorRed = (nextErrorRedBytes = new ByteArray(lineLen)).items;
            curErrorGreen = (curErrorGreenBytes = new ByteArray(lineLen)).items;
            nextErrorGreen = (nextErrorGreenBytes = new ByteArray(lineLen)).items;
            curErrorBlue = (curErrorBlueBytes = new ByteArray(lineLen)).items;
            nextErrorBlue = (nextErrorBlueBytes = new ByteArray(lineLen)).items;
        } else {
            curErrorRed = curErrorRedBytes.ensureCapacity(lineLen);
            nextErrorRed = nextErrorRedBytes.ensureCapacity(lineLen);
            curErrorGreen = curErrorGreenBytes.ensureCapacity(lineLen);
            nextErrorGreen = nextErrorGreenBytes.ensureCapacity(lineLen);
            curErrorBlue = curErrorBlueBytes.ensureCapacity(lineLen);
            nextErrorBlue = nextErrorBlueBytes.ensureCapacity(lineLen);
            for (int i = 0; i < lineLen; i++) {
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }

        }
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used, rdiff, gdiff, bdiff;
        byte er, eg, eb, paletteIndex;
        float w1 = ditherStrength * 0.125f, w3 = w1 * 3f, w5 = w1 * 5f, w7 = w1 * 7f;
        for (int y = 0; y < h; y++) {
            int ny = y + 1;
            for (int i = 0; i < lineLen; i++) {
                curErrorRed[i] = nextErrorRed[i];
                curErrorGreen[i] = nextErrorGreen[i];
                curErrorBlue[i] = nextErrorBlue[i];
                nextErrorRed[i] = 0;
                nextErrorGreen[i] = 0;
                nextErrorBlue[i] = 0;
            }
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    er = curErrorRed[px];
                    eg = curErrorGreen[px];
                    eb = curErrorBlue[px];
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = MathUtils.clamp(((color >>> 24)       ) + (er), 0, 0xFF);
                    int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (eg), 0, 0xFF);
                    int bb = MathUtils.clamp(((color >>> 8)  & 0xFF) + (eb), 0, 0xFF);
                    paletteIndex =
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))];
                    used = paletteArray[paletteIndex & 0xFF];
                    pixmap.drawPixel(px, y, used);
                    rdiff = (color>>>24)-    (used>>>24);
                    gdiff = (color>>>16&255)-(used>>>16&255);
                    bdiff = (color>>>8&255)- (used>>>8&255);
                    if(px < lineLen - 1)
                    {
                        curErrorRed[px+1]   += rdiff * w7;
                        curErrorGreen[px+1] += gdiff * w7;
                        curErrorBlue[px+1]  += bdiff * w7;
                    }
                    if(ny < h)
                    {
                        if(px > 0)
                        {
                            nextErrorRed[px-1]   += rdiff * w3;
                            nextErrorGreen[px-1] += gdiff * w3;
                            nextErrorBlue[px-1]  += bdiff * w3;
                        }
                        if(px < lineLen - 1)
                        {
                            nextErrorRed[px+1]   += rdiff * w1;
                            nextErrorGreen[px+1] += gdiff * w1;
                            nextErrorBlue[px+1]  += bdiff * w1;
                        }
                        nextErrorRed[px]   += rdiff * w5;
                        nextErrorGreen[px] += gdiff * w5;
                        nextErrorBlue[px]  += bdiff * w5;
                    }
                }
            }
        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    public Pixmap reduceWithRoberts (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used, adj;
        byte paletteIndex;
        for (int y = 0; y < h; y++) {
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    adj = (int)((px * 0xC13FA9A902A6328FL + y * 0x91E10DA5C79E7B1DL >> 57) * ditherStrength);
                    adj ^= adj >> 31;
                    //adj = (-(adj >>> 4 & 1) ^ adj) & 7;
                    adj -= 32 * ditherStrength;
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = MathUtils.clamp(((color >>> 24)       ) + (adj), 0, 0xFF);
                    int gg = MathUtils.clamp(((color >>> 16) & 0xFF) + (adj), 0, 0xFF);
                    int bb = MathUtils.clamp(((color >>> 8)  & 0xFF) + (adj), 0, 0xFF);
                    paletteIndex =
                            paletteMapping[((rr << 7) & 0x7C00)
                                    | ((gg << 2) & 0x3E0)
                                    | ((bb >>> 3))];
                    used = paletteArray[paletteIndex & 0xFF];
                    pixmap.drawPixel(px, y, used);
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    public Pixmap reduceRobertsMul (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used;
        float adj, str = ditherStrength * (256f / paletteArray.length) * 0x2.5p-27f;
        long pos;
        for (int y = 0; y < h; y++) {
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
//                    adj = (((px * 0xC13FA9A902A6328FL + y * 0x91E10DA5C79E7B1DL >> 40) * 0x1.Fp-26f) * ditherStrength) + 1f;
//                    color |= (color >>> 5 & 0x07070700) | 0xFE;
//                    int rr = MathUtils.clamp((int) (((color >>> 24)       ) * adj), 0, 0xFF);
//                    int gg = MathUtils.clamp((int) (((color >>> 16) & 0xFF) * adj), 0, 0xFF);
//                    int bb = MathUtils.clamp((int) (((color >>> 8)  & 0xFF) * adj), 0, 0xFF);
                    //0xD1B54A32D192ED03L, 0xABC98388FB8FAC03L, 0x8CB92BA72F3D8DD7L
//                    adj = (((px * 0xC13FA9A902A6328FL + y * 0x91E10DA5C79E7B1DL) >> 40) * str);
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = ((color >>> 24)       );//MathUtils.clamp((int) (rr * (1f + adj)), 0, 0xFF);
                    int gg = ((color >>> 16) & 0xFF);//MathUtils.clamp((int) (gg * (1f + adj)), 0, 0xFF);
                    int bb = ((color >>> 8)  & 0xFF);//MathUtils.clamp((int) (bb * (1f + adj)), 0, 0xFF);
                    used = paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF];
                    pos = (px * 0xC13FA9A902A6328FL - y * 0x91E10DA5C79E7B1DL);
                    pos ^= pos >>> 1;
                    adj = ((pos >> 40) * str);
                    rr = MathUtils.clamp((int) (rr * (1f + adj * ((used >>> 24) - rr >> 3))), 0, 0xFF);
                    gg = MathUtils.clamp((int) (gg * (1f + adj * ((used >>> 16 & 0xFF) - gg >> 3))), 0, 0xFF);
                    bb = MathUtils.clamp((int) (bb * (1f + adj * ((used >>> 8 & 0xFF) - bb >> 3))), 0, 0xFF);
                    pixmap.drawPixel(px, y, paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF]);
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    public Pixmap reduceRobertsEdit (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used;
        int pos;
        float adj, str = -0x3.Fp-20f * ditherStrength;
        for (int y = 0; y < h; y++) {
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
//                    adj = (((px * 0xC13FA9A902A6328FL + y * 0x91E10DA5C79E7B1DL >> 40) * 0x1.Fp-26f) * ditherStrength) + 1f;
//                    color |= (color >>> 5 & 0x07070700) | 0xFE;
//                    int rr = MathUtils.clamp((int) (((color >>> 24)       ) * adj), 0, 0xFF);
//                    int gg = MathUtils.clamp((int) (((color >>> 16) & 0xFF) * adj), 0, 0xFF);
//                    int bb = MathUtils.clamp((int) (((color >>> 8)  & 0xFF) * adj), 0, 0xFF);
                    //0xD1B54A32D192ED03L, 0xABC98388FB8FAC03L, 0x8CB92BA72F3D8DD7L
//                    adj = (((px * 0xC13FA9A902A6328FL + y * 0x91E10DA5C79E7B1DL) >> 40) * str);
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = ((color >>> 24)       );//MathUtils.clamp((int) (rr * (1f + adj)), 0, 0xFF);
                    int gg = ((color >>> 16) & 0xFF);//MathUtils.clamp((int) (gg * (1f + adj)), 0, 0xFF);
                    int bb = ((color >>> 8)  & 0xFF);//MathUtils.clamp((int) (bb * (1f + adj)), 0, 0xFF);
                    used = paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF];
                    pos = (px * (0xC13FA9A9 + y) + y * (0x91E10DA5 + px));
                    pos += pos >>> 1 ^ pos >>> 3 ^ pos >>> 4;
                    //0xE60E2B722B53AEEBL, 0xCEBD76D9EDB6A8EFL, 0xB9C9AA3A51D00B65L, 0xA6F5777F6F88983FL, 0x9609C71EB7D03F7BL, 
                    //0x86D516E50B04AB1BL
//                    long pr = (px * 0xE60E2B722B53AEEBL - y * 0x86D516E50B04AB1BL),
//                         pg = (px * 0xCEBD76D9EDB6A8EFL + y * 0x9609C71EB7D03F7BL),
//                         pb = (y * 0xB9C9AA3A51D00B65L - px * 0xA6F5777F6F88983FL);
//                    str * ((pr ^ pr >>> 1 ^ pr >>> 3 ^ pr >>> 4) >> 40)
//                    str * ((pg ^ pg >>> 1 ^ pg >>> 3 ^ pg >>> 4) >> 40)
//                    str * ((pb ^ pb >>> 1 ^ pb >>> 3 ^ pb >>> 4) >> 40)
                    //(px + y) * 1.6180339887498949f
                    adj = (pos >> 12) * str;
                    //adj = adj * ditherStrength; //(adj * adj * adj + 0x5p-6f)
                    // + NumberTools.sway(y * 0.7548776662466927f + px * 0.5698402909980532f) * 0.0625f;
                    rr = MathUtils.clamp((int) (rr + (adj * (((used >>> 24) - rr)))), 0, 0xFF); //  * 17 >> 4
                    gg = MathUtils.clamp((int) (gg + (adj * (((used >>> 16 & 0xFF) - gg)))), 0, 0xFF); //  * 23 >> 4
                    bb = MathUtils.clamp((int) (bb + (adj * (((used >>> 8 & 0xFF) - bb)))), 0, 0xFF); // * 5 >> 4
                    pixmap.drawPixel(px, y, paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF]);
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }
    public Pixmap reduceShaderMimic (Pixmap pixmap) {
        boolean hasTransparent = (paletteArray[0] == 0);
        final int lineLen = pixmap.getWidth(), h = pixmap.getHeight();
        Pixmap.Blending blending = pixmap.getBlending();
        pixmap.setBlending(Pixmap.Blending.None);
        int color, used;
        float pos;
        float adj;
        final float strength = 0x1.4p-10f * ditherStrength;
        for (int y = 0; y < h; y++) {
            for (int px = 0; px < lineLen; px++) {
                color = pixmap.getPixel(px, y) & 0xF8F8F880;
                if ((color & 0x80) == 0 && hasTransparent)
                    pixmap.drawPixel(px, y, 0);
                else {
                    color |= (color >>> 5 & 0x07070700) | 0xFE;
                    int rr = ((color >>> 24)       );
                    int gg = ((color >>> 16) & 0xFF);
                    int bb = ((color >>> 8)  & 0xFF);
                    float len = (rr * 5 + gg * 9 + bb * 2) * strength + 1f;
                    //adj = fract(52.9829189 * fract(dot(vec2(0.06711056, 0.00583715), gl_FragCoord.xy))) * len - len * 0.5;
                    used = paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF];
                    pos = (px * 0.06711056f + y * 0.00583715f);
                    pos -= (int)pos;
                    pos *= 52.9829189f;
                    adj = (pos - (int)pos) * len - len * 0.5f;
                    rr = MathUtils.clamp((int) (rr + (adj * ((rr - (used >>> 24))))), 0, 0xFF); //  * 17 >> 4
                    gg = MathUtils.clamp((int) (gg + (adj * ((gg - (used >>> 16 & 0xFF))))), 0, 0xFF); //  * 23 >> 4
                    bb = MathUtils.clamp((int) (bb + (adj * ((bb - (used >>> 8 & 0xFF))))), 0, 0xFF); // * 5 >> 4
                    pixmap.drawPixel(px, y, paletteArray[paletteMapping[((rr << 7) & 0x7C00)
                            | ((gg << 2) & 0x3E0)
                            | ((bb >>> 3))] & 0xFF]);
                }
            }

        }
        pixmap.setBlending(blending);
        return pixmap;
    }

    /**
     * Retrieves a random non-0 color index for the palette this would reduce to, with a higher likelihood for colors
     * that are used more often in reductions (those with few similar colors). The index is returned as a byte that,
     * when masked with 255 as with {@code (palette.randomColorIndex(random) & 255)}, can be used as an index into a
     * palette array with 256 or less elements that should have been used with {@link #exact(int[])} before to set the
     * palette this uses.
     * @param random a Random instance, which may be seeded
     * @return a randomly selected color index from this palette with a non-uniform distribution, can be any byte but 0
     */
    public byte randomColorIndex(Random random)
    {
        return paletteMapping[random.nextInt() >>> 17];
    }

    /**
     * Retrieves a random non-transparent color from the palette this would reduce to, with a higher likelihood for
     * colors that are used more often in reductions (those with few similar colors). The color is returned as an
     * RGBA8888 int; you can assign one of these into a Color with {@link Color#rgba8888ToColor(Color, int)} or
     * {@link Color#set(int)}.
     * @param random a Random instance, which may be seeded
     * @return a randomly selected color from this palette with a non-uniform distribution
     */
    public int randomColor(Random random)
    {
        return paletteArray[paletteMapping[random.nextInt() >>> 17] & 255];
    }

    /**
     * Looks up {@code color} as if it was part of an image being color-reduced and finds the closest color to it in the
     * palette this holds. Both the parameter and the returned color are RGBA8888 ints.
     * @param color an RGBA8888 int that represents a color this should try to find a similar color for in its palette
     * @return an RGBA8888 int representing a color from this palette, or 0 if color is mostly transparent
     * (0 is often but not always in the palette)
     */
    public int reduceSingle(int color)
    {
        if((color & 0x80) == 0) // less visible than half-transparent
            return 0; // transparent
        return paletteArray[paletteMapping[
                (color >>> 17 & 0x7C00)
                        | (color >>> 14 & 0x3E0)
                        | (color >>> 11 & 0x1F)] & 0xFF];
    }

    /**
     * Looks up {@code color} as if it was part of an image being color-reduced and finds the closest color to it in the
     * palette this holds. The parameter is a RGBA8888 int, the returned color is a byte index into the
     * {@link #paletteArray} (mask it like: {@code paletteArray[reduceIndex(color) & 0xFF]}).
     * @param color an RGBA8888 int that represents a color this should try to find a similar color for in its palette
     * @return a byte index that can be used to look up a color from the {@link #paletteArray}
     */
    public byte reduceIndex(int color)
    {
        if((color & 0x80) == 0) // less visible than half-transparent
            return 0; // transparent
        return paletteMapping[
                (color >>> 17 & 0x7C00)
                        | (color >>> 14 & 0x3E0)
                        | (color >>> 11 & 0x1F)];
    }

    /**
     * Looks up {@code color} as if it was part of an image being color-reduced and finds the closest color to it in the
     * palette this holds. Both the parameter and the returned color are packed float colors, as produced by
     * {@link Color#toFloatBits()} or many methods in SColor.
     * @param packedColor a packed float color this should try to find a similar color for in its palette
     * @return a packed float color from this palette, or 0f if color is mostly transparent
     * (0f is often but not always in the palette)
     */
    public float reduceFloat(float packedColor)
    {
        final int color = NumberUtils.floatToIntBits(packedColor);
        if(color >= 0) // if color is non-negative, then alpha is less than half of opaque
            return 0f;
        return NumberUtils.intBitsToFloat(Integer.reverseBytes(paletteArray[paletteMapping[
                (color << 7 & 0x7C00)
                        | (color >>> 6 & 0x3E0)
                        | (color >>> 19)] & 0xFF] & 0xFFFFFFFE));

    }

    /**
     * Modifies {@code color} so its RGB values will match the closest color in this PaletteReducer's palette. If color
     * has {@link Color#a} less than 0.5f, this will simply set color to be fully transparent, with rgba all 0.
     * @param color a libGDX Color that will be modified in-place; do not use a Color constant, use {@link Color#cpy()}
     *              or a temporary Color
     * @return color, after modifications.
     */
    public Color reduceInPlace(Color color)
    {
        if(color.a < 0.5f)
            return color.set(0);
        return color.set(paletteArray[paletteMapping[
                ((int) (color.r * 0x1f.8p+10) & 0x7C00)
                        | ((int) (color.g * 0x1f.8p+5) & 0x3E0)
                        | ((int) (color.r * 0x1f.8p+0))] & 0xFF]);
    }
    public static int hueShift(int rgba)
    {
        final int a = rgba & 0xFF;
        final float r = (rgba >>> 24) / 255f, g = (rgba >>> 16 & 0xFF) / 255f, b = (rgba >>> 8 & 0xFF) / 255f;
        final float luma = (float)Math.pow(r * 0.375f + g * 0.5f + b * 0.125f, 1.1875);
        final float adj = MathUtils.sin((luma - 0.5f) * Math.abs(luma - 0.5f) * 13.5f) * 0.09f;//(1.875f * 6.283185307179586f)
        final float warm = adj + r - b, mild = 0.5f * (adj + g - b);
        return (MathUtils.clamp((int) ((luma + 0.625f * warm - mild) * 256f), 0, 255)<<24|
                MathUtils.clamp((int) ((luma - 0.375f * warm + mild) * 256f), 0, 255)<<16|
                MathUtils.clamp((int) ((luma - 0.375f * warm - mild) * 256f), 0, 255)<<8|
                a);
    }
    public static void hueShiftPalette(int[] palette)
    {
        for (int i = 0; i < palette.length; i++) {
            palette[i] = hueShift(palette[i]);
        }
    }
    public void hueShift()
    {
        hueShiftPalette(paletteArray);
    }

}
