This demo should work with plain java using libgdx, and with jtransc using haxe lime.

## Run with plain JVM

You should run `com.jtransc.examples.tictactoe.TicTacToeGdx` class for example loading `pom.xml` in intelliJ and debug it there.

Using a terminal:
```
mvn exec:java -Dexec.mainClass="com.jtransc.examples.tictactoe.TicTacToeGdx"
```

## Run with JTransc

In order for this to work you must install maven and haxe.

You can install haxe from here: [http://haxe.org/download/](http://haxe.org/download/)

After that you have to install and setup lime (a multiplatform multimedia library).

```
haxelib install lime
haxelib run lime setup
```

In order to build the sample:

```
mvn package
```

Will output:
`/target/jtransc-haxe/out/html5/bin/index.html`
`/target/jtransc-haxe/out/flash/bin/TicTacToeJTranscKotlin.swf`
`/target/jtransc-haxe/out/android/bin/bin/TicTacToeJTranscKotlin-debug.apk`

## HTML5 version
Create an http_server there and open `index.html` at the browser. And you will see the demo. kotlin -> jtransc -> haxe/lime -> webgl
If you don't have a simple web server, you can go to `/target/jtransc-haxe` folder and execute `lime test html5`
