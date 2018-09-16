package me.mrpingu.hltvapi

import com.google.gson.annotations.SerializedName
import me.mrpingu.hltvapi.extension.*
import me.mrpingu.hltvapi.util.IntListMatrix
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

object HLTVApi {
	
	private const val baseUrl = "https://www.hltv.org/"
	private const val userAgent =
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36"
	
	private val alphabetRegex = "[A-Za-z]".toRegex()
	
	private fun document(url: String) = Jsoup.connect(url).userAgent(userAgent).get()
	
	fun ongoingEvents() =
			parseOngoingEvents(document("${baseUrl}events").select(
					"div.events-holder a[href].big-event"))
	
	fun upcomingBigEvents() =
			parseUpcomingBigEvents(document("${baseUrl}events").select(
					"div.events-holder a[href].big-event"))
	
	fun upcomingSmallEvents() =
			parseUpcomingSmallEvents(document("${baseUrl}events").select(
					"div.events-holder a[href].big-event"))
	
	private fun Element.extractUrl() = baseUrl + href().dropFirst()
	
	private fun String.extractId() = split("/").let { split -> split[split.lastIndex - 1] }.toInt()
	
	private fun parseOngoingEvents(elements: Elements) = elements.map { element ->
		val url = element.extractUrl()
		val id = url.extractId()
		val name = element.select("div.event-name-small div.text-ellipsis").html()
		
		val locationType = element
				.select("div.event-name-small lan-marker")
				.let { elements ->
					if (elements.isEmpty()) Location.Companion.Type.ONLINE
					else Location.Companion.Type.UNKNOWN_LAN
				}
		
		val timestamps = element
				.select("tr.eventDetails span[data-unix]")
				.map(Element::timestamp)
		val startTimestamp = timestamps.first()
		val endTimestamp = timestamps.secondOrNull() ?: startTimestamp
		
		SimpleEvent(
				id,
				name,
				url,
				startTimestamp,
				endTimestamp,
				null,
				null,
				Location(locationType))
	}
	
	private fun parseUpcomingBigEvents(elements: Elements) = elements.map { element ->
		val url = element.extractUrl()
		val id = url.extractId()
		val name = element.select("div.info div.big-event-name").html()
		
		val location = element.select("div.info span.big-event-location").html()
		
		val timestamps = element
				.select("div.additional-info td.col-value.col-date span[data-unix]")
				.map(Element::timestamp)
		val startTimestamp = timestamps.first()
		val endTimestamp = timestamps.secondOrNull() ?: startTimestamp
		
		var money: Int? = null
		var special: String? = null
		
		element
				.select("div.additional-info td[title].col-value")
				.first()
				.title()
				.let { title ->
					money = title.dropFirst().remove(",").toIntOrNull()
					
					if (money == null && title != "TBA") special = title
				}
		
		val teams = element.selectLast("div.additional-info td.col-value").html().toIntOrNull() ?: -1
		
		SimpleEvent(
				id,
				name,
				url,
				startTimestamp,
				endTimestamp,
				SimplePrize(money != null, money, special),
				teams,
				Location(Location.Companion.Type.INTERNATIONAL_LAN, location))
	}
	
	private fun parseUpcomingSmallEvents(elements: Elements) = elements.map { element ->
		val url = element.extractUrl()
		val id = url.extractId()
		val name = element.select("table.table td.col-value.event-col div.text-ellipsis").html()
		
		val location = element
				.select("tr.eventDetails span.smallCountry span.col-desc")
				.html()
				.remove(" |")
				.remove(" (Online)")
		val locationType =
				Location.Companion.Type[element.select("table.table td.col-value.small-col.gtSmartphone-only").html()]
		
		val timestamps = element
				.select("tr.eventDetails span.col-desc span[data-unix]")
				.map(Element::timestamp)
		val startTimestamp = timestamps.first()
		val endTimestamp = timestamps.secondOrNull() ?: startTimestamp
		
		var money: Int? = null
		var special: String? = null
		
		element
				.select("table.table td.col-value.small-col.prizePoolEllipsis")
				.first()
				.title()
				.let { title ->
					money = title.dropFirst().remove(",").toIntOrNull()
					
					if (money == null && title != "TBA") special = title
				}
		
		val teams = element.selectLast("table.table td.col-value.small-col").html().toIntOrNull() ?: -1
		
		SimpleEvent(
				id,
				name,
				url,
				startTimestamp,
				endTimestamp,
				SimplePrize(money != null, money, special),
				teams,
				Location(locationType, location))
	}
	
	fun events() = document("${baseUrl}events").let { document ->
		arrayOf(
				parseOngoingEvents(document.select("#ALL a[href].ongoing-event")),
				parseUpcomingBigEvents(document.select("div.events-holder a[href].big-event")),
				parseUpcomingSmallEvents(document.select("div.events-holder a[href].small-event")))
				.flatMap { it }
	}
	
	fun event(url: String) = document(url).let { document ->
		val id = url.extractId()
		val name = document.select("div.eventname").html()
		
		val location = document.select("table.info td.location.gtSmartphone-only span.text-ellipsis").html()
		val locationType =
				if (location.contains("Online, true")) Location.Companion.Type.ONLINE
				else Location.Companion.Type.UNKNOWN_LAN
		
		val timestamps = document.select("table.info td.eventdate span[data-unix]").map(Element::timestamp)
		val startTimestamp = timestamps.first()
		val endTimestamp = timestamps.secondOrNull() ?: startTimestamp
		
		var money: Int? = null
		var special: String? = null
		
		document
				.select("table.info td.prizepool.text-ellipsis")
				.html()
				.let { prize ->
					money = prize.dropFirst().remove(",").toIntOrNull()
					
					if (money == null && prize != "TBA") special = prize
				}
		
		val prizeDistribution = document
				.select("div.placements div.placement")
				.map { element ->
					val teamElement = element.select("div.team").firstOrNull()
					var team: PrizeTeam? = null
					
					if (teamElement != null) {
						val url = teamElement.selectFirst("a[href]").extractUrl()
						val id = url.extractId()
						val name = element.select("a[href]").html()
						
						val country = element.selectFirst("img[title].flag-spacing.flag").title()
						
						team = PrizeTeam(id, name, url, country)
					}
					
					val placement = element
							.select("div:not([class])")
							.first()
							.html()
							.remove(alphabetRegex)
							.split("-")
							.map(String::toInt)
							.toIntRange()
							.toList()
					
					var money: Int? = null
					var special: String? = null
					
					element
							.select("div.prizeMoney")
							.let { prize ->
								money = prize.first().html().dropFirst().remove(",").toIntOrNull()
								
								if (prize.secondOrNull()?.html() != "TBA") special = prize.secondOrNull()?.html()
							}
					
					PrizeDistribution(placement, team, money, special)
				}
		
		val teams = document.select("table.info td.teamsNumber").html().toIntOrNull()
		val eventTeams = document
				.select("div.teams-attending.grid div.standard-box.team-box")
				.map { element ->
					val url = element.selectFirst("div.team-name a[href]").extractUrl()
					val id = url.extractId()
					val name = element.select("div.team-name a[href] div.text").html()
					
					val players = element.select("div.flag-align.player").map { element ->
						val url = element.selectFirst("a[href]").extractUrl()
						val id = url.extractId()
						val name = element.select("a[href]").html()
						
						val country = element.selectFirst("img[title].flag-spacing.flag").title()
						
						EventPlayer(id, name, url, country)
					}
					
					val classifier = element.selectFirst("div.sub-text.text-ellipsis").html()
					
					EventTeam(id, name, url, players, classifier)
				}
				.sortedBy { eventTeam -> eventTeam.name.toLowerCase() }
		
		val mapPool = document
				.select("div.map-pool div.map-pool-map-holder div.map-pool-map-name")
				.map { element -> Map[element.html()] }
				.sorted()
		
		Event(
				id,
				name,
				url,
				startTimestamp,
				endTimestamp,
				Prize(money != null, money, special, prizeDistribution),
				EventTeams(teams, eventTeams),
				Location(locationType, location),
				mapPool)
	}
	
	fun event(id: Int, name: String) =
			event("$baseUrl/events/$id/${name.toLowerCase().replace(
					" ",
					"-")}")
	
	fun liveMatches() = document("$baseUrl/matches")
			.select("div.live-matches div:not(.no-height).live-match")
			.map { element ->
				val url = element.selectFirst("a[href]").extractUrl()
				val id = url.extractId()
				val event = element.select("div.live-match-header div.event-name").html()
				
				val stars = element.select("div.live-match-header div.stars i.star").size
				
				val bestOf = element.selectFirst("div.scores table.table td.bestof").html().split(" ").last().toInt()
				val mapElements = element.select("div.scores table.table td.map")
				val maps =
						if (bestOf == 1) listOf(Map.valueOf(mapElements.first().html().toUpperCase()))
						else mapElements.map { Map[it.html()] }.toList()
				
				val teams = element.select("div.scores table.table td.teams span.team-name").map(Element::html)
				
				val scores = element
						.select("div.scores table.table tr:not([class])")
						.map { tr -> tr.select("td.livescore").mapNotNull { it.html().toIntOrNull() } }
						.transpose()
				
				SimpleMatch(id, url, event, null, stars, bestOf, maps, teams, scores)
			}
	
	fun upcomingMatches() = document("$baseUrl/matches")
			.select("div.upcoming-matches a[href]")
			.filter { it.select("td.placeholder-text-cell").isEmpty() }
			.map { element ->
				val url = element.extractUrl()
				val id = url.extractId()
				val event = element.select("td.event span.event-name").html()
				
				val timestamp = element.selectFirst("td.time div.time").timestamp()
				
				val stars = element.select("td.star-cell div.stars i.star").size
				
				val mapName = element.selectFirst("td.star-cell div.map-text").html()
				val map = Map[mapName]
				val maps = if (map == Map.UNKNOWN) null else listOf(map)
				
				val bestOf = if (map == Map.UNKNOWN) mapName.takeLast().toInt() else 1
				
				val teams = element.select("td.team-cell div.team").map(Element::html)
				
				SimpleMatch(id, url, event, timestamp, stars, bestOf, maps, teams, null)
			}
	
	fun matches() = document("$baseUrl/matches").let { }
}

data class SimplePrize(val standard: Boolean, val money: Int?, val special: String?)

data class Location(val type: Type, val name: String? = null) {
	
	companion object {
		
		enum class Type(private val string: String) {
			
			ONLINE("Online"),
			REGIONAL_LAN("Reg. LAN"),
			LOCAL_LAN("Local LAN"),
			INTERNATIONAL_LAN("Intl. LAN"),
			UNKNOWN_LAN("LAN"),
			UNKNOWN("Other");
			
			override fun toString() = string
			
			companion object {
				
				private val typeByName by lazy { values().map { it.string to it }.toMap() }
				
				operator fun get(string: String) = typeByName[string] ?: UNKNOWN
			}
		}
	}
}

data class SimpleEvent(
		val id: Int,
		val name: String,
		val url: String,
		val startTimestamp: Long,
		val endTimestamp: Long,
		val prize: SimplePrize?,
		val teams: Int?,
		val location: Location)

data class PrizeTeam(val id: Int, val name: String, val url: String, val country: String)

data class PrizeDistribution(val placement: List<Int>, val team: PrizeTeam?, val money: Int?, val special: String?)

data class Prize(
		val standard: Boolean,
		val money: Int?,
		val special: String?,
		val distribution: List<PrizeDistribution>)

data class EventPlayer(val id: Int, val name: String, val url: String, val country: String)

data class EventTeam(
		val id: Int,
		val name: String,
		val url: String,
		val players: List<EventPlayer>,
		val qualifier: String)

data class EventTeams(val amount: Int?, val teams: List<EventTeam>)

data class Event(
		val id: Int,
		val name: String,
		val url: String,
		val startTimestamp: Long,
		val endTimestamp: Long,
		val prize: Prize?,
		val teams: EventTeams?,
		val location: Location,
		val mapPool: List<Map>?)

enum class Map(private val string: String, private val abbreviation: String) {
	
	@SerializedName("de_cache")
	CACHE("Cache", "cch"),
	@SerializedName("de_cbbl")
	COBBLESTONE("Cobblestone", "cbl"),
	@SerializedName("de_dust2")
	DUST2("Dust2", "d2"),
	@SerializedName("de_inferno")
	INFERNO("Inferno", "inf"),
	@SerializedName("de_mirage")
	MIRAGE("Mirage", "mrg"),
	@SerializedName("de_nuke")
	NUKE("Nuke", "nuke"),
	@SerializedName("de_overpass")
	OVERPASS("Overpass", "ovp"),
	@SerializedName("de_train")
	TRAIN("Train", "trn"),
	@SerializedName("")
	UNKNOWN("", "");
	
	override fun toString() = string
	
	companion object {
		
		private val typeByName by lazy { values().map { it.string to it }.toMap() }
		private val typeByAbbreviation by lazy { values().map { it.abbreviation to it }.toMap() }
		
		operator fun get(string: String) =
				(if (string.length < 5) typeByAbbreviation[string.toLowerCase()] else typeByName[string]) ?: UNKNOWN
	}
}

data class SimpleMatch(
		val id: Int,
		val url: String,
		val event: String,
		val timestamp: Long?,
		val stars: Int,
		val bestOf: Int,
		val maps: List<Map>?,
		val teams: List<String>,
		val scores: IntListMatrix?)
