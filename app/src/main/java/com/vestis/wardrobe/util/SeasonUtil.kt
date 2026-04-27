package com.vestis.wardrobe.util

import java.time.LocalDate

enum class Hemisphere { NORTH, SOUTH }

enum class Season(val label: String, val colorHex: String) {
    SUMMER("Summer", "#E07B4F"),
    AUTUMN("Autumn", "#C4773B"),
    WINTER("Winter", "#6B9BBF"),
    SPRING("Spring", "#72B572");

    companion object {
        val all: List<String> = listOf("All") + entries.map { it.label }
    }
}

object SeasonUtil {

    /** Southern-hemisphere month → season map */
    private val southMap: Map<Int, Season> = buildMap {
        listOf(12, 1, 2).forEach { put(it, Season.SUMMER) }
        listOf(3, 4, 5).forEach  { put(it, Season.AUTUMN) }
        listOf(6, 7, 8).forEach  { put(it, Season.WINTER) }
        listOf(9, 10, 11).forEach { put(it, Season.SPRING) }
    }

    /** Northern-hemisphere month → season map */
    private val northMap: Map<Int, Season> = buildMap {
        listOf(3, 4, 5).forEach  { put(it, Season.SPRING) }
        listOf(6, 7, 8).forEach  { put(it, Season.SUMMER) }
        listOf(9, 10, 11).forEach { put(it, Season.AUTUMN) }
        listOf(12, 1, 2).forEach { put(it, Season.WINTER) }
    }

    fun getSeason(date: LocalDate, hemisphere: Hemisphere): Season {
        val map = if (hemisphere == Hemisphere.SOUTH) southMap else northMap
        return map[date.monthValue] ?: Season.SUMMER
    }

    fun getSeason(dateIso: String, hemisphere: Hemisphere): Season =
        getSeason(LocalDate.parse(dateIso), hemisphere)

    fun hemisphereFromLatitude(latitude: Double): Hemisphere =
        if (latitude < 0) Hemisphere.SOUTH else Hemisphere.NORTH
}
