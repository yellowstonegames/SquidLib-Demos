package com.github.yellowstonegames.teavm;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

import java.io.File;
import java.io.IOException;

import com.github.yellowstonegames.DungeonDemo;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;

/** Builds the TeaVM/HTML application. */
public class TeaVMBuilder {
    public static void main(String[] args) throws IOException {
        // typically set by the Gradle task, but can also be set here or with the command-line arg "debug"
        boolean debug = false;
        // typically set by the Gradle task, but can also be set here or with the command-line arg "run"
        boolean startJetty = false;
        for (int i = 0; i < args.length; i++) {
            if("debug".equals(args[i])) debug = true;
            else if("run".equals(args[i])) startJetty = true;
        }
        new TeaCompiler(
            new WebBackend()
                .setHtmlWidth(DungeonDemo.SHOWN_WIDTH * DungeonDemo.CELL_WIDTH)
                .setHtmlHeight(DungeonDemo.SHOWN_HEIGHT * DungeonDemo.CELL_HEIGHT)
                .setHtmlTitle("Dungeon Demo!")
//                .setWebAssembly(true)
                .setStartJettyAfterBuild(startJetty)
        )
            .addAssets(new AssetFileHandle("../assets"))
            .setOptimizationLevel(debug ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED)
            .setMainClass(TeaVMLauncher.class.getName())
            .setObfuscated(!debug)
            .setDebugInformationGenerated(debug)
            .setSourceMapsFileGenerated(debug)
            .setSourceFilePolicy(TeaVMSourceFilePolicy.COPY)
            .addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")))
//            .addReflectionClass(Styles.class)
            .build(new File("build/dist"));
    }
}
