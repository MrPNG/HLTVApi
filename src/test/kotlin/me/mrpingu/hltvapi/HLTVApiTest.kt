package me.mrpingu.hltvapi

import com.google.gson.GsonBuilder

val gson = GsonBuilder().setPrettyPrinting().create()

fun main(args: Array<String>) {
	HLTVScoreBotApi.scoreBot(2326677)
	
	/*val liveMatches = me.mrpingu.hltvapi.HLTVApi.liveMatches()
	
	println(gson.toJson(liveMatches))*/
	
	/*val event = me.mrpingu.hltvapi.HLTVApi.event("https://www.hltv.org/events/3885/faceit-major-2018-main-qualifier")
	
	PrintWriter(File("event.json")).use { it.println(gson.toJson(event)) }*/
}
