package me.mrpingu.hltvapi.extension

fun <T> Array<T>.dropFirst() = drop(1)

fun <T> Array<T>.dropLast() = dropLast(1)

fun <T> Array<T>.second() = get(1)

fun <T> Array<T>.third() = get(2)

fun <T> Array<T>.secondOrNull() = getOrNull(1)

fun <T> Array<T>.thirdOrNull() = getOrNull(2)

fun Array<Int>.toIntRange() =
		if (isEmpty()) throw IllegalArgumentException("List is empty.")
		else first() .. (secondOrNull() ?: first())

fun IntArray.second() = get(1)

fun IntArray.third() = get(2)

fun IntArray.secondOrNull() = getOrNull(1)

fun IntArray.thirdOrNull() = getOrNull(2)

fun IntArray.toIntRange() =
		if (isEmpty()) throw IllegalArgumentException("List is empty.")
		else first() .. (secondOrNull() ?: first())
