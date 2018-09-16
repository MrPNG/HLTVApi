package me.mrpingu.hltvapi.extension

fun <T> List<T>.dropLast() = dropLast(1)

fun <T> List<T>.takeLast() = takeLast(1)

fun <T> List<T>.second() = get(1)

fun <T> List<T>.third() = get(2)

fun <T> List<T>.secondOrNull() = getOrNull(1)

fun <T> List<T>.thirdOrNull() = getOrNull(2)

fun List<Int>.toIntRange() =
		if (isEmpty()) throw IllegalArgumentException("List is empty.")
		else first() .. (secondOrNull() ?: first())
