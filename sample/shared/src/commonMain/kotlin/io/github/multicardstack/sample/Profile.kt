package io.github.multicardstack.sample

import androidx.compose.ui.graphics.Color

data class Profile(
    val id: Int,
    val name: String,
    val age: Int,
    val bio: String,
    val colors: List<Color>,
)

fun sampleProfiles(): List<Profile> = listOf(
    Profile(1, "Aoi", 26, "Coffee, film cameras, long walks", listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88))),
    Profile(2, "Haruto", 29, "Weekend climber and ramen hunter", listOf(Color(0xFF8EC5FC), Color(0xFF5B8DEF))),
    Profile(3, "Mio", 24, "Learning Kotlin one bug at a time", listOf(Color(0xFFA1E3B5), Color(0xFF38B98A))),
    Profile(4, "Ren", 31, "Board games, jazz bars, cats", listOf(Color(0xFFFBC2EB), Color(0xFFA18CD1))),
    Profile(5, "Yui", 27, "Always planning the next trip", listOf(Color(0xFFFFE29F), Color(0xFFFFA99F))),
    Profile(6, "Sota", 30, "Home cook, terrible singer", listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4))),
    Profile(7, "Hina", 25, "Bookstores and rainy days", listOf(Color(0xFFCFD9DF), Color(0xFF7F8FA6))),
    Profile(8, "Kaito", 28, "Marathon in progress", listOf(Color(0xFFF6D365), Color(0xFFFDA085))),
    Profile(9, "Sakura", 26, "Plants, pottery, podcasts", listOf(Color(0xFFFCCB90), Color(0xFFD57EEB))),
    Profile(10, "Yuto", 32, "Will debate the best sushi topping", listOf(Color(0xFF96FBC4), Color(0xFF4FACFE))),
)
