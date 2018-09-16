package me.mrpingu.hltvapi.extension

import me.mrpingu.hltvapi.util.IntListMatrix

fun IntListMatrix.transpose() =
		if (isEmpty()) this
		else {
			val columns = get(0).size
			
			val matrix = List(columns) { mutableListOf<Int>() }
			
			forEachIndexed { _, intArray ->
				if (intArray.size != columns) throw IllegalArgumentException("This list is not a matrix.")
				
				intArray.forEachIndexed { j, int ->
					matrix[j] += int
				}
			}
			
			matrix
		}
