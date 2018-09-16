package me.mrpingu.hltvapi.extension

fun <T> Iterable<T>.dropFirst() = drop(1)

fun <T> Iterable<T>.takeFirst() = take(1)
