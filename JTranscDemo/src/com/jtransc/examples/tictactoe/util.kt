package com.jtransc.examples.tictactoe

import java.util.*

class Array2<T>(val width: Int, val height: Int, generator: (x: Int, y: Int) -> T) {
	val data = (0 until width * height).map { generator(getX(it), getY(it)) }.toMutableList()

	private fun index(x: Int, y: Int) = y * width + x
	private fun getX(n: Int) = n % width
	private fun getY(n: Int) = n / width
	private val side: Int get() {
		if (width != height) throw RuntimeException("Not square array2")
		return width or height
	}

	operator fun get(x: Int, y: Int): T = data[index(x, y)]

	fun getRow(y: Int): List<T> = (0 until width).map { x -> get(x, y) }
	fun getColumn(x: Int): List<T> = (0 until height).map { y -> get(x, y) }
	fun getDiagonal1(): List<T> {
		return (0 until side).map { this[it, it] }
	}

	fun getDiagonal2(): List<T> {
		return (0 until side).map { this[width - it - 1, it] }
	}

	fun getCells() = data.toList()

	fun <T2> map(callback: (x:Int, y:Int, value:T) -> T2): Array2<T2> {
		return Array2(width, height) { x, y -> callback(x, y, this[x, y]) }
	}

	inline fun <reified T2> forEach(callback: (x:Int, y:Int, value:T) -> T2) {
		for (y in 0 until height) {
			for (x in 0 until width) {
				callback(x, y, this[x, y])
			}
		}
	}

	companion object {
		inline fun forEach(width:Int, height:Int, callback: (x:Int, y:Int) -> Unit) {
			for (y in 0 until height) {
				for (x in 0 until width) {
					callback(x, y)
				}
			}
		}
	}
}

fun <T> Random.value(items: List<T>): T = items[this.nextInt(items.size)]
fun <T> List<T>.random(random: Random = Random()): T = this[random.nextInt(this.size)]
