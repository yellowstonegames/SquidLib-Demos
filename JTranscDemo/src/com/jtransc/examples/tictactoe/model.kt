package com.jtransc.examples.tictactoe

import jtransc.game.math.IPoint
import jtransc.game.util.Signal

object Model {
	class Cell(val x: Int, val y: Int) {
		var value = ChipType.EMPTY
		val pos = IPoint(x, y)
		override fun toString(): String = "C($x,$y,$value)"
	}

	interface Result {
		object PLAYING : Result
		object DRAW : Result
		data class WIN(val cells: List<Cell>) : Result
	}

	class Game {
		private val data = Array2(3, 3) { x, y -> Cell(x, y) }

		val onCellUpdated = Signal<Cell>()
		val onGameFinished = Signal<Result>()

		fun isEmpty(x: Int, y: Int): Boolean = _isEmpty(x, y)
		fun hasChip(x: Int, y: Int): Boolean = !_isEmpty(x, y)
		private fun _isEmpty(x: Int, y: Int): Boolean = data[x, y].value == ChipType.EMPTY

		operator fun get(x: Int, y: Int): ChipType = data[x, y].value
		operator fun set(x: Int, y: Int, chip: ChipType) {
			if (data[x, y].value == chip) return
			data[x, y].value = chip
			onCellUpdated.dispatch(data[x, y])
			val result = checkResult()
			if (result != Result.PLAYING) onGameFinished.dispatch(result)
		}

		fun checkResult():Result {
			return if (hasWin()) {
				Result.WIN(getWin()!!)
			} else if (isBoardFull()) {
				Result.DRAW
			} else {
				Result.PLAYING
			}
		}

		fun reset() {
			this.data.forEach { x, y, cell ->
				cell.value = ChipType.EMPTY
			}
			this.data.forEach { x, y, cell ->
				onCellUpdated.dispatch(cell)
			}
		}

		fun isBoardFull() = this.data.getCells().all { it.value != ChipType.EMPTY }

		fun hasWin() = getWin() != null

		fun getWin(): List<Cell>? {
			return getWinSets().firstOrNull { cells ->
				val value1 = cells[0].value
				value1 != ChipType.EMPTY && cells.all { it.value == value1 }
			}
		}

		private fun getWinSets(): List<List<Cell>> = (
			(0 until 3).map { data.getRow(it) } +
				(0 until 3).map { data.getColumn(it) } +
				listOf(data.getDiagonal1()) +
				listOf(data.getDiagonal2())
			)

		fun getCells() = data.getCells()
		fun getEmptyCells() = getCells().filter { it.value == ChipType.EMPTY }
	}
}

enum class ChipType(val s: String) {
	EMPTY("."), O("O"), X("X");

	override fun toString() = s
}
