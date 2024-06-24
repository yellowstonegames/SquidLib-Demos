package com.github.yellowstonegames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.Assert;
import org.junit.Test;

public class BasicTest {
    @Test
    public void testNothing() {
        int a = 10;
        int b = 5;
        Assert.assertEquals(a, b + b);
    }

    @Test
    public void testFiles() {
        GdxNativesLoader.load();
        Gdx.files = new HeadlessFiles();
        Assert.assertFalse(Gdx.files.local("foobarbaz.txt").exists());
    }
}
