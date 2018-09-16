package me.mrpingu.hltvapi.extension

import org.jsoup.select.Elements

fun Elements.selectLast(cssQuery: String) = select(cssQuery).last()
