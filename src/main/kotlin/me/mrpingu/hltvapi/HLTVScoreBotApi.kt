package me.mrpingu.hltvapi

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import me.mrpingu.hltvapi.extension.*
import okhttp3.*
import java.util.*
import kotlin.concurrent.schedule

object HLTVScoreBotApi: WebSocketListener() {
	
	private val client = OkHttpClient.Builder().build()
	private val gson = GsonBuilder().setPrettyPrinting().create()
	
	private const val baseUrl = "wss://scorebot-secure.hltv.org/socket.io/?EIO=3&transport=websocket"
	
	fun scoreBot(id: Int) {
		client.newWebSocket(Request.Builder().url(baseUrl).build(), HLTVScoreBotWebSocketListener(id))
	}
	
	class HLTVScoreBotWebSocketListener(private val id: Int): WebSocketListener() {
		
		private var message = 0
		private var period = 0L
		
		private val readyForMatch = """42["readyForMatch","{\"token\":\"\",\"listId\":\"%s\"}"]"""
		
		override fun onMessage(webSocket: WebSocket, text: String) {
			
			if (text.startsWith("42")) {
				val json = text.drop(2).remove("\\")
				val type = json.split("\"")[1]
				
				when (type) {
					"log"        -> {
						val log = gson.fromJson<Log>(json.drop(15).dropLast(3))
						
						log.asSequence()
								.filter { it.logEvent() is Kill }
								.map { it.logEvent() as Kill }
								.map { it.weapon }
								.toHashSet()
								.printlnSelf()
					}
					"scoreboard" -> {
						val scoreboard = gson.fromJson<Scoreboard>(json.drop(14).dropLast())
					}
				}
			} else when (++message) {
				1 -> {
					val jsonObject = JsonParser().parse(text.dropFirst()).asJsonObject
					val sid = jsonObject["sid"].asString
					period = jsonObject["pingInterval"].asLong
					
					client.newWebSocket(Request.Builder().url("$baseUrl&sid=$sid").build(), this)
				}
				2 -> webSocket.apply {
					send(readyForMatch.format(id))
					
					Timer().schedule(0, period) { send("2") }
				}
			}
		}
	}
}

enum class Side(private val string: String) {
	
	@SerializedName("CT")
	COUNTER_TERRORISTS("Counter-Terrorists"),
	@SerializedName("TERRORIST")
	TERRORISTS("Terrorists");
	
	override fun toString() = string
}

enum class Result(private val string: String) {
	
	@SerializedName("CTs_Win")
	TERRORISTS_ELIMINATED("Terrorists eliminated"),
	@SerializedName("Terrorists_Win")
	CTS_ELIMINATED("Counter-Terrorists eliminated"),
	@SerializedName("Target_Bombed")
	BOMB_EXPLODED("Bomb exploded"),
	@SerializedName("Bomb_Defused")
	BOMB_DEFUSED("Bomb defused"),
	@SerializedName("lost")
	LOST("Lost");
	
	override fun toString() = string
}

sealed class LogEvent

data class MatchStarted(val map: Map): LogEvent()

object RoundStart: LogEvent()

data class RoundEnd(
		val counterTerroristScore: Int,
		val terroristScore: Int,
		val winner: Side,
		@SerializedName("winType") val result: Result): LogEvent()

data class Kill(
		val killerName: String,
		val killerNick: String,
		val killerSide: Side,
		val victimName: String,
		val victimNick: String,
		val victimSide: Side,
		val weapon: String,
		val headShot: Boolean): LogEvent()

data class BombPlanted(
		val playerName: String,
		val playerNick: String,
		@SerializedName("ctPlayers") val counterTerroristPlayers: Int,
		@SerializedName("tPlayers") val terroristPlayers: Int): LogEvent()

data class BombDefused(val playerName: String, val playerNick: String): LogEvent()

data class PlayerJoin(val playerName: String, val playerNick: String): LogEvent()

data class PlayerQuit(val playerName: String, val playerNick: String, val playerSide: Side): LogEvent()

data class WrappedLogEvent(
		@SerializedName("MatchStarted") private val matchStarted: MatchStarted?,
		@SerializedName("RoundStart") private val roundStart: RoundStart?,
		@SerializedName("RoundEnd") private val roundEnd: RoundEnd?,
		@SerializedName("Kill") private val kill: Kill?,
		@SerializedName("BombPlanted") private val bombPlanted: BombPlanted?,
		@SerializedName("BombDefused") private val bombDefused: BombDefused?,
		@SerializedName("PlayerJoin") private val playerJoin: PlayerJoin?,
		@SerializedName("PlayerQuit") private val playerQuit: PlayerQuit?) {
	
	fun logEvent() =
			(matchStarted ?: roundStart ?: roundEnd ?: kill ?: bombPlanted ?: bombDefused ?: playerJoin ?: playerQuit)!!
}

typealias Log = List<WrappedLogEvent>

data class AdvancedStats(
		val kast: Int,
		val entryKills: Int,
		val entryDeaths: Int,
		val multiKillRounds: Int,
		@SerializedName("oneOnXWins") val clutches: Int,
		val flashAssists: Int)

data class Player(
		val steamId: String,
		val dbId: Int,
		val name: String,
		val nick: String,
		val score: Int,
		val assists: Int,
		val deaths: Int,
		val money: Int,
		@SerializedName("damagePrRound") val adr: Double,
		val alive: Boolean,
		val hp: Int,
		val kevlar: Boolean,
		val helmet: Boolean,
		val hasDefusekit: Boolean,
		val advancedStats: AdvancedStats)

data class Round(
		val roundOrdinal: Int,
		@SerializedName("type") val result: Result,
		@SerializedName("survivingPlayers") val playersAlive: Int)

data class MatchHistory(val firstHalf: List<Round>, val secondHalf: List<Round>)

data class Scoreboard(
		@SerializedName("mapName") val map: Map,
		@SerializedName("ctTeamId") val counterTerroristTeamId: Int,
		@SerializedName("tTeamId") val terroristTeamId: Int,
		@SerializedName("ctTeamName") val counterTerroristTeamName: String,
		val terroristTeamName: String,
		@SerializedName("ctTeamScore") val counterTerroristTeamScore: Int,
		@SerializedName("tTeamScore") val terroristTeamScore: Int,
		@SerializedName("ctMatchHistory") val counterTerroristMatchHistory: MatchHistory,
		val terroristMatchHistory: MatchHistory,
		@SerializedName("CT") val counterTerroristPlayers: List<Player>,
		@SerializedName("TERRORIST") val terroristPlayers: List<Player>,
		@SerializedName("currentRound") val currentRoundOrdinal: Int,
		val counterTerroristScore: Int,
		val terroristScore: Int,
		@SerializedName("live") val isMatchLive: Boolean,
		@SerializedName("frozen") val isMatchFrozen: Boolean,
		@SerializedName("bombPlanted") val isBombPlanted: Boolean)
