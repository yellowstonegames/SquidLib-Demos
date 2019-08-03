package com.github.tommyettinger;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;

import java.util.Arrays;

/**
 * Created by Tommy Ettinger on 8/2/2019.
 */
public class ColorEqualizer {
    private final int[] lumas = new int[2041];
    public ColorEqualizer()
    {
    }
    public Pixmap process(Pixmap pm)
    {
        Arrays.fill(lumas, 0);
        final int w = pm.getWidth();
        final int h = pm.getHeight();
        final float invArea = 255f / (w * h - 1f);
        int c;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                c = pm.getPixel(x, y);
                lumas[(c >>> 23 & 0x1FE) + (c >>> 24) + (c >>> 14 & 0x3FC) + (c >>> 8 & 0xFF)]++;
            }
        }
        c = 0;
        for (int i = 0; i < 2041; i++) {
            if(c != (c += lumas[i])) // hoo boy. if this luma showed up at least once, add its frequency to c and run.
            {
                lumas[i] = c;
            }
        }
        float luma, warm, mild;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                c = pm.getPixel(x, y);
                luma = lumas[(c >>> 23 & 0x1FE) + (c >>> 24) + (c >>> 14 & 0x3FC) + (c >>> 8 & 0xFF)] * invArea;
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
