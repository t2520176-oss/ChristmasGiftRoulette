package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.model.GameState

/** The life is presented as cinematic chapters. Exact ages vary because the timeline is not fixed. */
object ChapterCards {
    private val titles = listOf(
        "THE SCHOOL YEARS", "THE ROAD AHEAD", "THE REAL WORLD", "BUILDING A LIFE",
        "RESPONSIBILITY", "WHAT MATTERS", "THE LIFE YOU BUILT"
    )

    const val COUNT = 7

    fun indexFor(age: Int): Int = when {
        age < 18 -> 1
        age < 23 -> 2
        age < 29 -> 3
        age < 36 -> 4
        age < 45 -> 5
        age < 60 -> 6
        else -> 7
    }

    fun title(index: Int): String = titles[(index - 1).coerceIn(0, titles.size - 1)]

    /** The chapter card to show before the next scene, or null if this chapter was already announced. */
    fun cardFor(s: GameState): Card? {
        val idx = indexFor(s.ageYears)
        if (idx <= s.chapterShown) return null
        return Card(TitleKind.CHAPTER, "CHAPTER $idx", "AGE ${s.ageYears}\n${title(idx)}", 3000)
    }

    /** A short caption when a lot of time has passed since the last scene ("3 YEARS LATER"). */
    fun timeSkipCard(s: GameState): Card? {
        val gap = s.ageYears - s.lastSceneAge
        if (s.lastSceneAge <= 0 || gap < 2) return null
        return Card(TitleKind.CAPTION, if (gap >= 20) "$gap YEARS LATER" else "$gap YEARS LATER", "AGE ${s.ageYears}", 1900)
    }

    /** Marks the chapter/age as shown. Call after the scene's title beats were queued. */
    fun markShown(s: GameState) {
        s.chapterShown = maxOf(s.chapterShown, indexFor(s.ageYears))
        s.lastSceneAge = s.ageYears
    }
}
