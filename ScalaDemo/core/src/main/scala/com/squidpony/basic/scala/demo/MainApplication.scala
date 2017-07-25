package com.squidpony.basic.scala.demo

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.StretchViewport
import squidpony.ArrayTools
import squidpony.FakeLanguageGen
import squidpony.squidai.DijkstraMap
import squidpony.squidgrid.FOV
import squidpony.squidgrid.Radius
import squidpony.squidgrid.gui.gdx._
import squidpony.squidgrid.mapping.DungeonGenerator
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidmath._
import java.util

/** ApplicationListener implementation shared by all platforms. */
object MainApplication {
  /** In number of cells */
  val gridWidth = 60
  /** In number of cells */
  val gridHeight = 25
  /** In number of cells */
  val bonusHeight = 6
  /** The pixel width of a cell */
  val cellWidth = 16
  /** The pixel height of a cell */
  val cellHeight = 16
}

class MainApplication extends ApplicationAdapter {
  private var batch : SpriteBatch = _
  private var rng = new RNG("SquidLib!")
  private var display : SquidLayers = _
  private var font : TextCellFactory = _
  private var dungeonGen : DungeonGenerator = _
  private var decoDungeon : Array[Array[Char]] = _
  private var bareDungeon : Array[Array[Char]] = _
  private var lineDungeon : Array[Array[Char]] = _
  private var colors : Array[Array[Color]] = _
  private var bgColors : Array[Array[Color]] = _
  private var baseLightness : Array[Array[Int]] = _
  private var input : SquidInput = _
  private var bgColor : Color = _
  private var stage : Stage = _
  private var playerToCursor : DijkstraMap = _
  private var cursor : Coord = _
  private var player : Coord = _
  private var toCursor : util.List[Coord] = _
  private var awaitedMoves : util.List[Coord] = _
  private var secondsWithoutMoves = .0
  private var lang : Array[String] = _
  private var forms : Array[FakeLanguageGen.SentenceForm] = _
  private var langIndex = 0
  private var line : Array[Char] = _
  private var resistance : Array[Array[Double]] = _
  private var visible : Array[Array[Double]] = _
  private var blockage : GreasedRegion = _
  private var seen : GreasedRegion = _

  override def create(): Unit = { // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.

    //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
    batch = new SpriteBatch
    //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
    stage = new Stage(new StretchViewport(MainApplication.gridWidth * MainApplication.cellWidth, (MainApplication.gridHeight + MainApplication.bonusHeight) * MainApplication.cellHeight), batch)
    // the font will try to load Iosevka Slab as an embedded bitmap font with a distance field effect.
    // the distance field effect allows the font to be stretched without getting blurry or grainy too easily.
    // this font is covered under the SIL Open Font License (fully free), so there's no reason it can't be used.
    font = DefaultResources.getStretchableSlabFont.setSmoothingMultiplier(1.8f)
    display = new SquidLayers(MainApplication.gridWidth, MainApplication.gridHeight + MainApplication.bonusHeight, MainApplication.cellWidth, MainApplication.cellHeight, font)
    // a bit of a hack to increase the text height slightly without changing the size of the cells they're in.
    // this causes a tiny bit of overlap between cells, which gets rid of an annoying gap between vertical lines.
    // if you use '#' for walls instead of box drawing chars, you don't need this.
    display.setTextSize(MainApplication.cellWidth * 1.05f, MainApplication.cellHeight * 1.15f)
    // this makes animations very fast, which is good for multi-cell movement but bad for attack animations.
    display.setAnimationDuration(0.03f)
    //These need to have their positions set before adding any entities if there is an offset involved.
    //There is no offset used here, but it's still a good practice here to set positions early on.
    display.setPosition(0, 0)
    //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
    //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good "ruined" dungeons.
    dungeonGen = new DungeonGenerator(MainApplication.gridWidth, MainApplication.gridHeight, rng)
    //uncomment this next line to randomly add water to the dungeon in pools.
    //dungeonGen.addWater(15);
    //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
    //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
    decoDungeon = dungeonGen.generate
    //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
    bareDungeon = dungeonGen.getBareDungeon
    //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
    //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing character.
    lineDungeon = DungeonUtility.hashesToLines(decoDungeon)
    resistance = DungeonUtility.generateResistances(bareDungeon)
    visible = ArrayTools.fill(0.0, MainApplication.gridWidth, MainApplication.gridHeight)
    //Coord is the type we use as a general 2D point, usually in a dungeon.
    //Because we know dungeons won't be incredibly huge, Coord performs best for x and y values less than 256, but
    // by default it can also handle some negative x and y values (-3 is the lowest it can efficiently store). You
    // can call Coord.expandPool() or Coord.expandPoolTo() if you need larger maps to be just as fast.
    cursor = Coord.get(-1, -1)
    // here, we need to get a random floor cell to place the player upon, without the possibility of putting him
    // inside a wall. There are a few ways to do this in SquidLib. The most straightforward way is to randomly
    // choose x and y positions until a floor is found, but particularly on dungeons with few floor cells, this can
    // have serious problems -- if it takes too long to find a floor cell, either it needs to be able to figure out
    // that random choice isn't working and instead choose the first it finds in simple iteration, or potentially
    // keep trying forever on an all-wall map. There are better ways! These involve using a kind of specific storage
    // for points or regions, getting that to store only floors, and finding a random cell from that collection of
    // floors. The two kinds of such storage used commonly in SquidLib are the "packed data" as short[] produced by
    // CoordPacker (which use very little memory, but can be slow, and are treated as unchanging by CoordPacker so
    // any change makes a new array), and GreasedRegion objects (which use slightly more memory, tend to be faster
    // on almost all operations compared to the same operations with CoordPacker, and default to changing the
    // GreasedRegion object when you call a method on it instead of making a new one). Even though CoordPacker
    // sometimes has better documentation, GreasedRegion is generally a better choice; it was added to address
    // shortcomings in CoordPacker, particularly for speed, and the worst-case scenarios for data in CoordPacker are
    // no problem whatsoever for GreasedRegion. CoordPacker is called that because it compresses the information
    // for nearby Coords into a smaller amount of memory. GreasedRegion is called that because it encodes regions,
    // but is "greasy" both in the fatty-food sense of using more space, and in the "greased lightning" sense of
    // being especially fast. Both of them can be seen as storing regions of points in 2D space as "on" and "off."
    // Here we fill a GreasedRegion so it stores the cells that contain a floor, the '.' char, as "on."
    val placement = new GreasedRegion(bareDungeon, '.')
    //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
    //creatures, and possibly a subclass for the player. The singleRandom() method on GreasedRegion finds one Coord
    // in that region that is "on," or -1,-1 if there are no such cells. It takes an RNG object as a parameter, and
    // if you gave a seed to the RNG constructor, then the cell this chooses will be reliable for testing. If you
    // don't seed the RNG, any valid cell should be possible.
    player = placement.singleRandom(rng)
    // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
    FOV.reuseFOV(resistance, visible, player.x, player.y, 7.0, Radius.CIRCLE)
    // 0.1 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.1 will _not_ be in
    // the blockage Collection, but anything 0.1 or less will be in it. This lets us use blockage to prevent access
    // to cells we can't see from the start of the move.
    blockage = new GreasedRegion(visible, 0.1)
    seen = blockage.copy.not
    //This is used to allow clicks or taps to take the player to the desired area.
    toCursor = new util.ArrayList[Coord](200)
    //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
    awaitedMoves = new util.ArrayList[Coord](200)
    //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
    //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
    // MANHATTAN value is used, which means 4-way movement only, no diagonals possible. Alternatives are CHEBYSHEV,
    // which allows 8 directions of movement at the same cost for all directions, and EUCLIDEAN, which allows 8
    // directions, but will prefer orthogonal moves unless diagonal ones are clearly closer "as the crow flies."
    playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.MANHATTAN)
    //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
    // player from all walkable cells in the dungeon.
    playerToCursor.setGoal(player)
    playerToCursor.scan(blockage)
    //The next three lines set the background color for anything we don't draw on, but also create 2D arrays of the
    //same size as decoDungeon that store simple indexes into a common list of colors, using the colors that looks
    // up as the colors for the cell with the same x and y.
    bgColor = SColor.DARK_SLATE_GRAY
    SColor.LIMITED_PALETTE(3) = SColor.DB_GRAPHITE
    colors = MapUtility.generateDefaultColors(decoDungeon)
    bgColors = MapUtility.generateDefaultBGColors(decoDungeon)
    baseLightness = ArrayTools.fill(40, MainApplication.gridWidth, MainApplication.gridHeight)
    // this creates an array of sentence builders, where each imitates one or more languages or linguistic styles.
    // this serves to demonstrate the large amount of glyphs SquidLib supports.
    // there's no need to put much effort into understanding this section yet, and many games won't use the language
    // generation code at all. If you want to know what this does, the parameters are: the language to use,
    // minimum words in a sentence, maximum words in a sentence, "mid" punctuation that can be after a word (like a
    // comma), "end" punctuation that can be at the end of a sentence, frequency of "mid" punctuation (the chance in
    // 1.0 that a word will have something like a comma appended after it), and the limit on how many chars to use.
    forms = Array[FakeLanguageGen.SentenceForm](new FakeLanguageGen.SentenceForm(FakeLanguageGen.ENGLISH, 5, 10, Array[String](",", ",", ",", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.17, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.GREEK_AUTHENTIC, 5, 11, Array[String](",", ",", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.2, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.GREEK_ROMANIZED, 5, 11, Array[String](",", ",", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.2, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.LOVECRAFT, 3, 9, Array[String](",", ",", ";"), Array[String](".", ".", "!", "!", "?", "...", "..."), 0.15, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.FRENCH, 4, 12, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.17, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.RUSSIAN_AUTHENTIC, 6, 13, Array[String](",", ",", ",", ",", ";", " -"), Array[String](".", ".", ".", "!", "?", "..."), 0.25, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.RUSSIAN_ROMANIZED, 6, 13, Array[String](",", ",", ",", ",", ";", " -"), Array[String](".", ".", ".", "!", "?", "..."), 0.25, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.JAPANESE_ROMANIZED, 5, 13, Array[String](",", ",", ",", ",", ";"), Array[String](".", ".", ".", "!", "?", "...", "..."), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.SWAHILI, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.SOMALI, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.HINDI_ROMANIZED, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.NORSE, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.INUKTITUT, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.NAHUATL, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?"), 0.12, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.FANTASY_NAME, 4, 8, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.22, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.FANCY_FANTASY_NAME, 4, 8, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.22, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.ELF, 5, 10, Array[String](",", ",", ",", ";", ";", " -"), Array[String](".", ".", ".", "...", "?"), 0.22, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.GOBLIN, 4, 9, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", ".", "...", "?"), 0.1, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.DEMONIC, 4, 8, Array[String](",", ",", ",", ";", ";"), Array[String](".", ".", "!", "!", "!", "?!"), 0.07, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.INFERNAL, 6, 13, Array[String](",", ",", ",", ";", ";", " -", "*", " ©"), Array[String](".", ".", ".", "...", "?", "...", "?"), 0.25, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.FRENCH.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.65), 5, 9, Array[String](",", ",", ",", ";"), Array[String](".", ".", ".", "!", "?", "?", "..."), 0.14, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.ENGLISH.addAccents(0.5, 0.15), 5, 10, Array[String](",", ",", ",", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.17, MainApplication.gridWidth - 4), new FakeLanguageGen.SentenceForm(FakeLanguageGen.SWAHILI.mix(FakeLanguageGen.JAPANESE_ROMANIZED, 0.5).mix(FakeLanguageGen.FRENCH, 0.35).mix(FakeLanguageGen.RUSSIAN_ROMANIZED, 0.25).mix(FakeLanguageGen.GREEK_ROMANIZED, 0.2).mix(FakeLanguageGen.ENGLISH, 0.15).mix(FakeLanguageGen.FANCY_FANTASY_NAME, 0.12).mix(FakeLanguageGen.LOVECRAFT, 0.1), 5, 10, Array[String](",", ",", ",", ";"), Array[String](".", ".", ".", "!", "?", "..."), 0.2, MainApplication.gridWidth - 4))
    /*
             * Now we generate the initial sentences for each of those many languages. We cycle through the shown sentences
             * by changing langIndex, and change the contents of each sentence once it is cycled out of being visible.
             */ lang = new Array[String](forms.length)
    var i = 0
    while (i < forms.length) {
      lang(i) = forms(i).sentence()
      i += 1
    }
    // this is a big one.
    // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
    // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
    // keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
    // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
    // implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
    // Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
    // that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
    // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
    // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
    // the event when someone clicks.
    input = new SquidInput(new SquidInput.KeyHandler() {
      override def handle(key: Char, alt: Boolean, ctrl: Boolean, shift: Boolean): Unit = key match {
        case SquidInput.UP_ARROW =>
        case 'k' =>
        case 'w' =>
        case 'K' =>
        case 'W' =>
          //-1 is up on the screen
          move(0, -1)
        case SquidInput.DOWN_ARROW =>
        case 'j' =>
        case 's' =>
        case 'J' =>
        case 'S' =>
          //+1 is down on the screen
          move(0, 1)
        case SquidInput.LEFT_ARROW =>
        case 'h' =>
        case 'a' =>
        case 'H' =>
        case 'A' =>
          move(-1, 0)
        case SquidInput.RIGHT_ARROW =>
        case 'l' =>
        case 'd' =>
        case 'L' =>
        case 'D' =>
          move(1, 0)
        case 'Q' =>
        case 'q' =>
        case SquidInput.ESCAPE =>
          Gdx.app.exit()
        case default => ()
      }
    }, //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
      //input and converts it to grid coordinates (here, a cell is 12 wide and 24 tall, so clicking at the
      // pixel position 15,51 will pass screenX as 1 (since if you divide 15 by 12 and round down you get 1),
      // and screenY as 2 (since 51 divided by 24 rounded down is 2)).
      new SquidMouse(MainApplication.cellWidth, MainApplication.cellHeight, MainApplication.gridWidth, MainApplication.gridHeight, 0, 0, new InputAdapter() { // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
        // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
        override def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
          if (awaitedMoves.isEmpty) {
            if (toCursor.isEmpty) {
              cursor = Coord.get(screenX, screenY)
              //This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
              // player position to the position the user clicked on. The "PreScanned" part is an optimization
              // that's special to DijkstraMap; because the whole map has already been fully analyzed by the
              // DijkstraMap.scan() method at the start of the program, and re-calculated whenever the player
              // moves, we only need to do a fraction of the work to find the best path with that info.
              toCursor = playerToCursor.findPathPreScanned(cursor)
              //findPathPreScanned includes the current cell (goal) by default, which is helpful when
              // you're finding a path to a monster or loot, and want to bump into it, but here can be
              // confusing because you would "move into yourself" as your first move without this.
              // Getting a sublist avoids potential performance issues with removing from the start of an
              // ArrayList, since it keeps the original list around and only gets a "view" of it.
              if (!toCursor.isEmpty) {
                line = OrthoLine.lineChars(toCursor)
                toCursor = toCursor.subList(1, toCursor.size)
              }
            }
            awaitedMoves.addAll(toCursor)
          }
          true
        }

        override

        def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = mouseMoved(screenX, screenY) // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
        // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
        override def mouseMoved(screenX: Int, screenY: Int): Boolean = {
          if (!awaitedMoves.isEmpty) return false
          if (cursor.x == screenX && cursor.y == screenY) return false
          cursor = Coord.get(screenX, screenY)
          toCursor = playerToCursor.findPathPreScanned(cursor)
          if (!toCursor.isEmpty) {
            line = OrthoLine.lineChars(toCursor)
            toCursor = toCursor.subList(1, toCursor.size)
          }
          false
        }
      }))
    //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
    Gdx.input.setInputProcessor(new InputMultiplexer(stage, input))
    //You might be able to get by with the next line instead of the above line, but the former is preferred.
    //Gdx.input.setInputProcessor(input);
    // and then add display, our one visual component, to the list of things that act in Stage.
    stage.addActor(display)
  }

  /**
    * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
    * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
    *
    * @param xmod change of x
    * @param ymod change of y
    */
  private def move(xmod: Int, ymod: Int) = {
    val newX = player.x + xmod
    val newY = player.y + ymod
    if (newX >= 0 && newY >= 0 && newX < MainApplication.gridWidth && newY < MainApplication.gridHeight && bareDungeon(newX)(newY) != '#') { // changing the player Coord is all we need to do here, because we re-calculate the distances to the player
      // from all other cells only when we need to, that is, when the movement is finished (see render() ).
      player = player.translate(xmod, ymod)
      FOV.reuseFOV(resistance, visible, player.x, player.y, 7.0, Radius.CIRCLE)
      // This is just like the constructor used earlier, but affects an existing GreasedRegion without making
      // a new one just for this movement.
      blockage.refill(visible, 0.1)
      seen.or(blockage.not)
      blockage.not
    }
    // changes the top displayed sentence to a new one with the same language. the top will be cycled off next.
    lang(langIndex) = forms(langIndex).sentence
    // cycles through the text snippets displayed whenever the player moves
    langIndex = (langIndex + 1) % lang.length
  }

  /**
    * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
    */
  def putMap(): Unit = {
    var i = 0
    while (i < MainApplication.gridWidth) {
      var j = 0
      while (j < MainApplication.gridHeight) {
        if (visible(i)(j) > 0.1) {
          val bright = baseLightness(i)(j) + (-105 + 180 * (visible(i)(j) * (1.0 + 0.2 * SeededNoise.noise(i * 0.2, j * 0.2, (System.currentTimeMillis & 0xffffff).toInt * 0.001, 10000)))).toInt
          display.put(i, j, lineDungeon(i)(j), colors(i)(j), bgColors(i)(j), bright)
        }
        else if (seen.contains(i, j)) display.put(i, j, lineDungeon(i)(j), colors(i)(j), bgColors(i)(j), -80)
        else display.put(i, j, ' ', SColor.BLACK, SColor.BLACK)
        j += 1
      }
      i += 1
    }
    var pt : Coord = Coord.get(-1, -1)
    var h = 0
    while (h < toCursor.size) {
      pt = toCursor.get(h)
      // use a brighter light to trace the path to the cursor, from 170 max lightness to 0 min.
      display.put(pt.x, pt.y, line(h + 1), SColor.CW_RICH_AZURE, bgColors(pt.x)(pt.y), 130)
        h += 1
    }
    //places the player as an '@' at his position in orange.
    display.put(player.x, player.y, '@', SColor.SAFETY_ORANGE)
    //this helps compatibility with the HTML target, which doesn't support String.format()
    val spaceArray = new Array[Char](MainApplication.gridWidth)
    util.Arrays.fill(spaceArray, ' ')
    val spaces = String.valueOf(spaceArray)
    var k = 0
    while (k < 6) {
      display.putString(0, MainApplication.gridHeight + k + 1, spaces, SColor.BLACK, SColor.COSMIC_LATTE)
      display.putString(2, MainApplication.gridHeight + k + 1, lang((langIndex + k) % lang.length), SColor.BLACK, SColor.COSMIC_LATTE)
      k += 1
    }
  }

  override def render(): Unit = { // standard clear the background routine for libGDX
    Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    //font = new TextCellFactory().defaultFont().initByFont()
    // if the user clicked, we have a list of moves to perform.
    if (!awaitedMoves.isEmpty) { // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
      secondsWithoutMoves += Gdx.graphics.getDeltaTime
      if (secondsWithoutMoves >= 0.1) {
        secondsWithoutMoves = 0
        val m = awaitedMoves.remove(0)
        line = OrthoLine.lineChars(toCursor)
        toCursor.remove(0)
        move(m.x - player.x, m.y - player.y)
      }
      // this only happens if we just removed the last Coord from awaitedMoves, and it's only then that we need to
      // re-calculate the distances from all cells to the player. We don't need to calculate this information on
      // each part of a many-cell move (just the end), nor do we need to calculate it whenever the mouse moves.
      if (awaitedMoves.isEmpty) { // the next two lines remove any lingering data needed for earlier paths
        playerToCursor.clearGoals()
        playerToCursor.resetMap()
        // the next line marks the player as a "goal" cell, which seems counter-intuitive, but it works because all
        // cells will try to find the distance between themselves and the nearest goal, and once this is found, the
        // distances don't change as long as the goals don't change. Since the mouse will move and new paths will be
        // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell, so the
        // player's position, and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
        // currently-moused-over cell, which we only need to set where the mouse is being handled.
        playerToCursor.setGoal(player)
        playerToCursor.scan(blockage)
      }
    }
    else { // if we are waiting for the player's input and get input, process it.
      if (input.hasNext) input.next()
    }
    // certain classes that use scene2d.ui widgets need to be told to act() to process input.
    stage.act()
    // need to display the map every frame, since we clear the screen to avoid artifacts.
    putMap()
    // stage has its own batch and must be explicitly told to draw().
    stage.draw()
  }

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
    input.getMouse.reinitialize(width.toFloat / MainApplication.gridWidth, height.toFloat / (MainApplication.gridHeight + MainApplication.bonusHeight), MainApplication.gridWidth, MainApplication.gridHeight, 0, 0)
  }

  override def pause(): Unit = {
    super.pause()
  }

  override def resume(): Unit = {
    super.resume()
  }
}