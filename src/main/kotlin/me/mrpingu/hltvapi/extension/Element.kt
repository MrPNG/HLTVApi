package me.mrpingu.hltvapi.extension

import org.jsoup.nodes.Element

fun Element.selectLast(cssQuery: String) = select(cssQuery).last()

fun Element.href() = attr("href")

fun Element.title() = attr("title")

fun Element.timestamp() = attr("data-unix").toLong()
