package com.github.tommyettinger.demos.desktop;

import io.anuke.arc.Files.FileType;
import io.anuke.arc.backends.sdl.SdlApplication;
import io.anuke.arc.backends.sdl.SdlConfig;
import com.github.tommyettinger.demos.GraphicalDemo;

import static com.github.tommyettinger.demos.GraphicalDemo.*;

/** Launches the desktop (SDL) application. */
public class DesktopLauncher {
    public static void main(String[] args) {
        createApplication();
    }

    private static SdlApplication createApplication() {
        return new SdlApplication(new GraphicalDemo(), getDefaultConfiguration());
    }

    private static SdlConfig getDefaultConfiguration() {
        SdlConfig configuration = new SdlConfig();
        configuration.title = "GraphicalDemo";
        configuration.width = gridWidth * cellWidth;
        configuration.height = (gridHeight + bonusHeight) * cellHeight;
        configuration.vSyncEnabled = false;
        configuration.setWindowIcon(FileType.Internal, "libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}