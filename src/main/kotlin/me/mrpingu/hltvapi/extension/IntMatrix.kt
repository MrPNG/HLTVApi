package me.mrpingu.hltvapi.extension

import me.mrpingu.hltvapi.util.IntMatrix

fun IntMatrix.transpose() =
		if (isEmpty()) this
		else {
			val lines = size
			val columns = get(0).size
			
			val matrix = Array(columns) { IntArray(lines) }
			
			forEachIndexed { i, intArray ->
				if (intArray.size != columns) throw IllegalArgumentException("This array is not a matrix.")
				
				intArray.forEachIndexed { j, int ->
					matrix[j][i] = int
				}
			}
			
			matrix
		}
