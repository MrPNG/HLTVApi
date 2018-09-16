package me.mrpingu.hltvapi.extension

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.Reader

inline fun <reified T> Gson.fromJson(json: String) = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: Reader) = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: JsonElement) = fromJson(json, T::class.java)
