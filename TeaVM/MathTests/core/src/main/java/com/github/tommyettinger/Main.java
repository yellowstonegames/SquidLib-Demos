package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.tommyettinger.digital.Base;
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

        batch.end();
    }
}
