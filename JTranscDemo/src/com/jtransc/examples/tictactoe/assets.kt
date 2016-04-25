package com.jtransc.examples.tictactoe

import jtransc.game.JTranscGame

class GameAssets(game: JTranscGame) {
	val beep = game.sound("assets/beep.mp3")
	
	private val atlas = game.image("assets/spritesheet.png", 512, 512)
	val board = atlas.slice(128, 0, 128 * 3, 128 * 3)
	val O = atlas.slice(0, 128 * 0, 128, 128)
	val X = atlas.slice(0, 128 * 1, 128, 128)
	val hover = atlas.slice(0, 128 * 2, 128, 128)
	val EMPTY = atlas.slice(0, 128 * 3, 128, 128)
}
