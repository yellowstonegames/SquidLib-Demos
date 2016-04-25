package com.jtransc.examples.tictactoe

import jtransc.game.JTranscGame

object TicTacToeGdx {
	@JvmStatic fun main(args: Array<String>) {
		JTranscLibgdx.init()
		TicTacToe.main(args)
	}
}

object TicTacToeTransc {
	@JvmStatic fun main(args: Array<String>) {
		JTranscLime.init()
		TicTacToe.main(args)
	}
}

object TicTacToe {
	@JvmStatic fun main(args: Array<String>) {
		JTranscGame.init(512, 512) { game ->
			val ingameScene = IngameController(Views.Ingame(GameAssets(game)))
			ingameScene.start()
			game.root.addChild(ingameScene.ingameView)
		}
	}
}