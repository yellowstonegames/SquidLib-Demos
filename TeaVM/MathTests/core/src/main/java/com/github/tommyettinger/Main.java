package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
//import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    SpriteBatch batch;
    BitmapFont font;
    ScreenViewport viewport;
    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new ScreenViewport();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.BLACK);
        float lh = font.getLineHeight(), h = Gdx.graphics.getHeight();
        batch.begin();
        font.draw(batch, String.valueOf(0x100000000L)+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(Long.lowestOneBit(0x100000000L))+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(BitConversion.lowestOneBit(0x100000000L))+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(0x8000000000000000L)+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(-0x8000000000000000L)+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(Math.abs(0x8000000000000000L))+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(Long.lowestOneBit(0x8000000000000000L))+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(BitConversion.lowestOneBit(0x8000000000000000L))+'L', 0, h -= lh);
        font.draw(batch, String.valueOf(0x8000000000000001L)+'L', 0, h -= lh);
        h = Gdx.graphics.getHeight();
        font.draw(batch, "0x"+Base.BASE16.unsigned(0x100000000L)+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(Long.lowestOneBit(0x100000000L))+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(BitConversion.lowestOneBit(0x100000000L))+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(0x8000000000000000L)+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(-0x8000000000000000L)+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(Math.abs(0x8000000000000000L))+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(Long.lowestOneBit(0x8000000000000000L))+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(BitConversion.lowestOneBit(0x8000000000000000L))+'L', 200, h -= lh);
        font.draw(batch, "0x"+Base.BASE16.unsigned(0x8000000000000001L)+'L', 200, h -= lh);
        // to avoid calculating constants at compile-time
        int o = BitConversion.lowestOneBit(1) * BitConversion.lowestOneBit(1);
        float n = h - lh;
        font.draw(batch, "No overflow:      " + ((0xFFFE+o) * 0x7FFF), 0, n -= lh);
        font.draw(batch, "32-bit overflow:  " + ((0xFFFE+o) * 0xFFFF), 0, n -= lh);
        font.draw(batch, "48-bit overflow:  " + ((0xFFFFFE+o) * 0xFFFFFF), 0, n -= lh);
        font.draw(batch, "49-bit overflow:  " + ((0xFFFFFE+o) * 0x1FFFFFF), 0, n -= lh);
        font.draw(batch, "50-bit overflow:  " + ((0x1FFFFFE+o) * 0x1FFFFFF), 0, n -= lh);
        font.draw(batch, "51-bit overflow:  " + ((0x3FFFFFE+o) * 0x1FFFFFF), 0, n -= lh);
        font.draw(batch, "52-bit overflow:  " + ((0x3FFFFFE+o) * 0x3FFFFFF), 0, n -= lh);
        font.draw(batch, "53-bit overflow:  " + ((0x7FFFFFE+o) * 0x3FFFFFF), 0, n -= lh);
        font.draw(batch, "54-bit overflow:  " + ((0x7FFFFFE+o) * 0x7FFFFFF), 0, n -= lh);
        font.draw(batch, "55-bit overflow:  " + ((0xFFFFFFE+o) * 0x7FFFFFF), 0, n -= lh);
        font.draw(batch, "56-bit overflow:  " + ((0xFFFFFFE+o) * 0xFFFFFFF), 0, n -= lh);
        font.draw(batch, "62-bit overflow:  " + ((0x7FFFFFFE+o) * 0x7FFFFFFF), 0, n -= lh);
        font.draw(batch, "63-bit overflow:  " + ((0x7FFFFFFE+o) * 0x80000000), 0, n -= lh);
        font.draw(batch, "64-bit overflow:  " + ((0x7FFFFFFF+o) * 0x80000000), 0, n -= lh);
        n = h - lh;
        font.draw(batch, "No overflow:      0x" + Base.BASE16.unsigned((0xFFFE+o) * 0x7FFF), 300, n -= lh);
        font.draw(batch, "32-bit overflow:  0x" + Base.BASE16.unsigned((0xFFFE+o) * 0xFFFF), 300, n -= lh);
        font.draw(batch, "48-bit overflow:  0x" + Base.BASE16.unsigned((0xFFFFFE+o) * 0xFFFFFF), 300, n -= lh);
        font.draw(batch, "49-bit overflow:  0x" + Base.BASE16.unsigned((0xFFFFFE+o) * 0x1FFFFFF), 300, n -= lh);
        font.draw(batch, "50-bit overflow:  0x" + Base.BASE16.unsigned((0x1FFFFFE+o) * 0x1FFFFFF), 300, n -= lh);
        font.draw(batch, "51-bit overflow:  0x" + Base.BASE16.unsigned((0x3FFFFFE+o) * 0x1FFFFFF), 300, n -= lh);
        font.draw(batch, "52-bit overflow:  0x" + Base.BASE16.unsigned((0x3FFFFFE+o) * 0x3FFFFFF), 300, n -= lh);
        font.draw(batch, "53-bit overflow:  0x" + Base.BASE16.unsigned((0x7FFFFFE+o) * 0x3FFFFFF), 300, n -= lh);
        font.draw(batch, "54-bit overflow:  0x" + Base.BASE16.unsigned((0x7FFFFFE+o) * 0x7FFFFFF), 300, n -= lh);
        font.draw(batch, "55-bit overflow:  0x" + Base.BASE16.unsigned((0xFFFFFFE+o) * 0x7FFFFFF), 300, n -= lh);
        font.draw(batch, "56-bit overflow:  0x" + Base.BASE16.unsigned((0xFFFFFFE+o) * 0xFFFFFFF), 300, n -= lh);
        font.draw(batch, "62-bit overflow:  0x" + Base.BASE16.unsigned((0x7FFFFFFE+o) * 0x7FFFFFFF), 300, n -= lh);
        font.draw(batch, "63-bit overflow:  0x" + Base.BASE16.unsigned((0x7FFFFFFE+o) * 0x80000000), 300, n -= lh);
        font.draw(batch, "64-bit overflow:  0x" + Base.BASE16.unsigned((0x7FFFFFFF+o) * 0x80000000), 300, n -= lh);

        batch.end();
    }
}
