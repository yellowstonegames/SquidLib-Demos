package com.squidpony.shader;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.AnimatedGif;
import com.github.tommyettinger.anim8.AnimatedPNG;

import static com.github.tommyettinger.anim8.Dithered.DitherAlgorithm.SCATTER;

/**
 * Credit for the shader adaptation goes to angelickite , a very helpful user on the libGDX Discord.
 * The Discord can be found at <a href="https://discord.gg/crTrDEK">this link</a>.
 */
public class NorthernLights extends ApplicationAdapter {

	private SpriteBatch batch;
	private Texture pixel;
	private ShaderProgram shader;

	private long startTime;
	private float seed;
	private int width, height;

	@Override public void create () {
		//Gdx.app.setLogLevel(Application.LOG_DEBUG);
		batch = new SpriteBatch();
		
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.drawPixel(0, 0, 0xFFFFFFFF);
		pixel = new Texture(pixmap);
		startTime = TimeUtils.millis();
		ShaderProgram.pedantic = false;
//		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("foam_fragment_no_dither.glsl"));
//		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("scrambler_fragment_no_dither.glsl"));
//		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("northern_fragment_no_dither.glsl"));
//		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("cuatro_fragment_no_dither.glsl"));
		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("standoff_fragment_no_dither.glsl"));
		if (!shader.isCompiled()) {
			Gdx.app.error("Shader", "error compiling shader:\n" + shader.getLog());
			Gdx.app.exit();
			return;
		}
		batch.setShader(shader);
		
		long state = -1L;//-987654321234567890L;//TimeUtils.nanoTime() + startTime;//-1234567890L;
		// Sarong's DiverRNG.randomize()
		seed = ((((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L) ^ state >>> 43 ^ state >>> 31 ^ state >>> 23) * 0xDB4F0B9175AE2165L) >>> 36) * 0x1.5bf0a8p-16f;
		startTime -= (state ^ state >>> 11) & 0xFFFFL;
		//startTime -= 0x1000000;
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		
//		renderAPNG();
		renderGif();
	}

	@Override public void resize (int width, int height) {
		this.width = width;
		this.height = height;
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
	}

	@Override public void render () {
		Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
		if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && UIUtils.alt())
		{
			if(Gdx.graphics.isFullscreen())
				Gdx.graphics.setWindowedMode(480, 320);
			else 
			{
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			}
		}
		Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
		final float ftm = TimeUtils.timeSinceMillis(startTime) * (0.02f);
		batch.begin();
		shader.setUniformf("seed", seed);
		shader.setUniformf("tm", ftm);
		batch.draw(pixel, 0, 0, width, height);
		batch.end();
	}

	public void renderAPNG () {
		Array<Pixmap> pixmaps = new Array<>(80);
		for (int i = 1; i <= 80; i++) {
			Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
			Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
			batch.begin();
			shader.setUniformf("seed", seed);
			shader.setUniformf("tm", i * 1.25f);
			batch.draw(pixel, 0, 0, width, height);
			batch.end();
			pixmaps.add(ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		}
		AnimatedPNG apng = new AnimatedPNG();
		apng.setCompression(7);
		apng.write(Gdx.files.local("build/HueWow"+startTime+".png"), pixmaps, 16);
	}
	public void renderGif() {
		Array<Pixmap> pixmaps = new Array<>(80);
		for (int i = 1; i <= 160; i++) {
			Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
			Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
			batch.begin();
			shader.setUniformf("seed", seed);
			shader.setUniformf("tm", i * 0.025f);
			batch.draw(pixel, 0, 0, width, height);
			batch.end();
			pixmaps.add(ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		}
		AnimatedGif gif = new AnimatedGif();
		gif.setDitherAlgorithm(SCATTER);
//		gif.palette = new PaletteReducer(new int[]{0x00000000, 0x2B2821FF, 0x624C3CFF, 0xD9AC8BFF, 0xE3CFB4FF,
//				0x243D5CFF, 0x5D7275FF, 0x5C8B93FF, 0xB1A58DFF, 0xB03A48FF, 0xD4804DFF, 0xE0C872FF, 0x3E6958FF, });
		
		gif.write(Gdx.files.local("build/Wobbly"+startTime+".gif"), pixmaps, 20);
	}
}
