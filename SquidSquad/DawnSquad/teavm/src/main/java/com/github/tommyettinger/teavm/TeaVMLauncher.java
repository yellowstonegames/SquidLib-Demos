package com.github.tommyettinger.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.tommyettinger.DawnSquad;

import static com.github.tommyettinger.DawnSquad.*;

/**
 * Launches the TeaVM/HTML application.
 */
public class TeaVMLauncher {
    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        //// If width and height are each greater than 0, then the app will use a fixed size.
        config.width = shownWidth * cellWidth;
        config.height = shownHeight * cellHeight;
        new WebApplication(new DawnSquad(), config);
    }
}
