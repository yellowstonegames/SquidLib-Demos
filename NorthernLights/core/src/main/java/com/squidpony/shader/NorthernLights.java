package com.squidpony.shader;

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
	private int seed;
	private int width, height;

	@Override public void create () {
		batch = new SpriteBatch();

		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.drawPixel(0, 0, 0xFFFFFFFF);
		pixel = new Texture(pixmap);

		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(Gdx.files.internal("northern_vertex.glsl"), Gdx.files.internal("northern_fragment.glsl"));
		if (!shader.isCompiled()) {
			System.out.printf("error compiling shader:\n%s", shader.getLog());
			Gdx.app.exit();
			return;
		}
		batch.setShader(shader);

		startTime = TimeUtils.millis();

		long state = TimeUtils.nanoTime() - startTime;
		// Sarong's DiverRNG.determine(), may be used in SquidLib later.
		seed = (int) (((state = ((state << ((state & 31) + 5)) ^ state ^ 0xDB4F0B9175AE2165L) * 0xD1B54A32D192ED03L)
			^ (state >>> ((state >>> 60) + 16))) * 0x369DEA0F31A53F85L >>> 32);

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	@Override public void resize (int width, int height) {
		this.width = width;
		this.height = height;
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
	}

	@Override public void render () {
		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

		shader.begin();
		shader.setUniformi("seed", seed);
		shader.setUniformi("tm", (int) TimeUtils.timeSinceMillis(startTime));
		shader.end();

		batch.begin();
		batch.draw(pixel, 0, 0, width, height);
		batch.end();
	}
}