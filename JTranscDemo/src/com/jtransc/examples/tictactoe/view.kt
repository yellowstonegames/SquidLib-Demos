package com.jtransc.examples.tictactoe

import jtransc.game.stage.Image
import jtransc.game.stage.Sprite

object Views {
	class Board(val assets: GameAssets) : Sprite() {
		init {
			addChild(Image(assets.board))
		}
	}

	class Cell(val assets: GameAssets) : Sprite() {
		val hoverImage = image(assets.EMPTY)
		val chipImage = image(assets.EMPTY)

		fun hover() {
			hoverImage.image = assets.hover;
		}

		fun out() {
			hoverImage.image = assets.EMPTY;
		}

		fun put(type: ChipType) {
			chipImage.image = when (type) {
				ChipType.X -> assets.X
				ChipType.O -> assets.O
				ChipType.EMPTY -> assets.EMPTY
			}
		}
	}

	class Ingame(val assets: GameAssets) : Sprite() {
		val board = addChild(Board(assets))
		val cells = (0 until 3).map { y ->
			(0 until 3).map { x ->
				addChild(addChild(Cell(assets).apply {
					this.x = (x * 128).toDouble()
					this.y = (y * 128).toDouble()
				}))
			}
		}
		fun cell(x:Int, y:Int) = cells[y][x]
	}
}
