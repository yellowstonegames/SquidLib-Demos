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
//        TeaBuildConfiguration teaBuildConfiguration = new TeaBuildConfiguration();
//        teaBuildConfiguration.assetsPath.add(new AssetFileHandle("../assets"));
//        teaBuildConfiguration.webappPath = new File("build/dist").getCanonicalPath();
//
//        // Register any extra classpath assets here:
//        // teaBuildConfiguration.additionalAssetsClasspathFiles.add("com/github/yellowstonegames/asset.extension");
//
//        // If you need to match specific classes based on the package and class name,
//        // you can use the reflectionListener to do fine-grained matching on the String fullClassName.
////        teaBuildConfiguration.reflectionListener = fullClassName -> {
////            if(fullClassName.startsWith("where.your.reflective.code.is") && fullClassName.endsWith("YourSuffix"))
////                return true;
////            return false;
////        };
//
//        // You can also register any classes or packages that require reflection here:
//        // TeaReflectionSupplier.addReflectionClass("com.github.yellowstonegames.reflect");
//
//        // JavaScript is the default target type for TeaVM, and it works better during debugging.
////        teaBuildConfiguration.targetType = TeaVMTargetType.JAVASCRIPT;
//        // You can choose to use the WebAssembly (WASM) GC target instead, which tends to perform better, but isn't
//        // as easy to debug. It might be a good idea to alternate target types during development if you plan on using
//        // WASM at release time.
//        teaBuildConfiguration.targetType = TeaVMTargetType.WEBASSEMBLY_GC;
//
//        // Used by older TeaVM versions.
////        TeaVMTool tool = TeaBuilder.config(teaBuildConfiguration);
//
//        // The next two lines are used by gdx-teavm 1.3.1 and newer (and libGDX 1.14.0 and newer).
//        TeaBuilder.config(teaBuildConfiguration);
//        TeaVMTool tool = new TeaVMTool();
//
//        tool.setMainClass(TeaVMLauncher.class.getName());
//        // For many (or most) applications, using a high optimization won't add much to build time.
//        // If your builds take too long, and runtime performance doesn't matter, you can change ADVANCED to SIMPLE .
//        // Using SIMPLE makes debugging easier, also, so it is used when DEBUG is enabled.
//        tool.setOptimizationLevel(DEBUG ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED);
//        // The line below will make the generated code hard to read (and smaller) in releases and easier to follow
//        // when DEBUG is true. Setting DEBUG to false should always be done before a release, anyway.
//        tool.setObfuscated(!DEBUG);
//
//        // If DEBUG is set to true, these lines allow step-debugging JVM languages from the browser,
//        // setting breakpoints in Java code and stopping in the appropriate place in generated browser code.
//        // This may work reasonably well when targeting WEBASSEMBLY_GC, but it usually works better with JAVASCRIPT .
//        if(DEBUG) {
//            tool.setDebugInformationGenerated(true);
//            tool.setSourceMapsFileGenerated(true);
//            tool.setWasmDebugInfoLevel(WasmDebugInfoLevel.FULL);
//            tool.setSourceFilePolicy(TeaVMSourceFilePolicy.COPY);
//            tool.addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")));
//        }
//
//        TeaBuilder.build(tool);
//    }
}
