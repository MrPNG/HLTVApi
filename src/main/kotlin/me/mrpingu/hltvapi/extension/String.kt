package me.mrpingu.hltvapi.extension

fun String.dropFirst() = drop(1)

fun String.dropLast() = dropLast(1)

fun String.takeFirst() = take(1)

fun String.takeLast() = takeLast(1)

fun String.remove(string: String) = replace(string, "")

fun String.remove(regex: Regex) = replace(regex, "")
