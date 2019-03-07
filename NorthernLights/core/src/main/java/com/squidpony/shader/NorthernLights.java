package com.squidpony.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.TimeUtils;

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
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		batch = new SpriteBatch();
		
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.drawPixel(0, 0, 0xFFFFFFFF);
		pixel = new Texture(pixmap);

		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("northern_fragment.glsl"));
		if (!shader.isCompiled()) {
			Gdx.app.error("Shader", "error compiling shader:\n" + shader.getLog());
			Gdx.app.exit();
			return;
		}
		batch.setShader(shader);

		startTime = TimeUtils.millis();

		long state = TimeUtils.nanoTime() - startTime;
		// Sarong's DiverRNG.randomize()
		seed = ((((state = (state ^ (state << 41 | state >>> 23) ^ (state << 17 | state >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L) ^ state >>> 43 ^ state >>> 31 ^ state >>> 23) * 0xDB4F0B9175AE2165L) >>> 36) * 0x1.5bf0a8p-16f;

//		// Sarong's DiverRNG.determine(), may be used in SquidLib later.
//		seed = (((state = ((state << ((state & 31) + 5)) ^ state ^ 0xDB4F0B9175AE2165L) * 0xD1B54A32D192ED03L)
//			^ (state >>> ((state >>> 60) + 16))) * 0x369DEA0F31A53F85L >>> 32) * 0x1.5bf0a8p-20f;

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	@Override public void resize (int width, int height) {
		this.width = width;
		this.height = height;
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
	}
	private static float swayRandomized(int seed, float value)
	{
		final int floor = value >= 0f ? (int) value : (int) value - 1;
		final float start = (((seed += floor * 0x6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 9) * 0x0.9ffffffp-20f,
				end = (((seed += 0x6C8D) ^ (seed << 11 | seed >>> 21)) * (seed >>> 13 | 0xA529) >>> 9) * 0x0.9ffffffp-20f;
		value -= floor;
		value *= value * (3f - 2f * value);
		return (1f - value) * start + value * end;
	}

	@Override public void render () {
		//Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		//Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
		Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " FPS");
		final float ftm = TimeUtils.timeSinceMillis(startTime) * 0x3p-14f;
		batch.begin();
		shader.setUniformf("seed", seed);
		shader.setUniformf("tm", ftm);
		shader.setUniformf("s",
				swayRandomized(0x9E3779B9, ftm - 1.11f),
				swayRandomized(0xD1B54A32, ftm + 1.41f),
				swayRandomized(0xE19B01AA, ftm + 2.61f));
		shader.setUniformf("c",
				swayRandomized(0xC13FA9A9, ftm - 1.11f),
				swayRandomized(0xDB4F0B91, ftm + 1.41f),
				swayRandomized(0xE60E2B72, ftm + 2.61f));
		batch.draw(pixel, 0, 0, width, height);
		batch.end();
	}
}