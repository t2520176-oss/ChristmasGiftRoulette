package com.lifeyourchoice.core.motto

import com.lifeyourchoice.core.engine.Endings
import com.lifeyourchoice.core.engine.SummaryGenerator
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.Stat

/** Assembles the final LIFE REPORT: story, important choices, three lessons and the life's motto. */
object LifeReportBuilder {
    fun build(s: GameState, number: Int, recentMottoIds: List<String>, nowMillis: Long): LifeRecord {
        val ending = Endings.select(s)
        val motto = MottoEngine.select(s, recentMottoIds)
        return LifeRecord(
            number = number,
            playerName = s.playerName,
            gender = s.gender,
            appearance = s.appearance,
            ageReached = s.ageYears,
            careerTitle = when {
                s.employed -> s.jobTitle
                s.lastCareer != com.lifeyourchoice.core.model.CareerTrack.NONE -> s.lastCareer.titleAt(s.lastCareerLevel)
                else -> "No career"
            },
            endingId = ending.id,
            endingTitle = ending.title,
            endingTagline = ending.tagline,
            wealth = s.stat(Stat.MONEY),
            family = s.stat(Stat.FAMILY),
            health = s.stat(Stat.HEALTH),
            reputation = s.stat(Stat.REPUTATION),
            friendship = s.stat(Stat.FRIENDSHIP),
            happiness = s.stat(Stat.HAPPINESS),
            summary = SummaryGenerator.summary(s, ending),
            importantChoices = s.milestones.sortedBy { it.ageYears }.take(12),
            lessons = LessonEngine.select(s).map { it.text },
            mottoId = motto.id,
            mottoText = motto.text,
            mottoCategory = motto.category.label,
            achievements = s.achievementsThisLife.toList(),
            timestamp = nowMillis
        )
    }
}
