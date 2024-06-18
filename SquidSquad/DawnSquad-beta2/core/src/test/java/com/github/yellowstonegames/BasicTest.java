package com.github.yellowstonegames;

import org.junit.Assert;
import org.junit.Test;

public class BasicTest {
    @Test
    public void testNothing() {
        int a = 10;
        int b = 5;
        Assert.assertEquals(a, b + b);
    }
}
