package com.github.tommyettinger;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;

import java.util.Arrays;

/**
 * Created by Tommy Ettinger on 8/2/2019.
 */
public class ColorEqualizer {
    private final float[] lumas = new float[2041];
    public ColorEqualizer()
    {
    }
    public Pixmap process(Pixmap pm)
    {
        final int w = pm.getWidth();
        final int h = pm.getHeight();
        float area = (w * h - 1f);
        if((w == 1 && h == 1) || w == 0 || h == 0)
            return pm;
        Arrays.fill(lumas, 0);
        int c;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                c = pm.getPixel(x, y);
                if((c & 0x80) != 0) 
                    lumas[(c >>> 23 & 0x1FE) + (c >>> 24) + (c >>> 14 & 0x3FC) + (c >>> 8 & 0xFF)]++;
                else
                    area--;
            }
        }
        final float invArea = 1f / area;

        c = 0;
//        int minLuma = 0, maxLuma = 2040;
//        for (int i = 0; i < 2041; i++) {
//            if(lumas[i] != 0)
//            {
//                minLuma = i + 8 >>> 4;
//                break;
//            }
//        }
        for (int i = 0; i < 2041; i++) {
            if(c != (c += lumas[i])) // hoo boy. if this luma showed up at least once, add its frequency to c and run.
            {
                lumas[i] = (float)Math.expm1(c * invArea) * 148.40406025167826f;
//                maxLuma = i;
            }
        }
//        maxLuma = 2048 + maxLuma >>> 4;
        float luma, warm, mild;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                c = pm.getPixel(x, y);
                luma = lumas[(c >>> 23 & 0x1FE) + (c >>> 24) + (c >>> 14 & 0x3FC) + (c >>> 8 & 0xFF)];
                warm = (c >>> 24) - (c >>> 8 & 0xFF);
                mild = ((c >>> 16 & 0xFF) - (c >>> 8 & 0xFF)) * 0.5f;
                pm.drawPixel(x, y, 
                        MathUtils.clamp((int) (luma + 0.625f * warm - mild), 0, 255)<<24|
                        MathUtils.clamp((int) (luma - 0.375f * warm + mild), 0, 255)<<16|
                        MathUtils.clamp((int) (luma - 0.375f * warm - mild), 0, 255)<<8|
                        (c & 0xFF));

            }
        }
        return pm;
    }
}
