package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait

class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    /** Evaluated only when a life ends (needs the finished picture). */
    val atEnd: Boolean = false,
    val test: (GameState) -> Boolean
)

object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_paycheck", "FIRST PAYCHECK", "Earn your first real job.") { it.has(Flags.FIRST_JOB) },
        AchievementDef("true_friend", "TRUE FRIEND", "Earn a best friend's deepest trust.") {
            it.trust(NpcRole.BEST_FRIEND) >= 88
        },
        AchievementDef("comeback_kid", "COMEBACK KID", "Rise again after a business failure.") {
            it.has(Flags.BUSINESS_FAILED) && it.has(Flags.COMEBACK)
        },
        AchievementDef("entrepreneur", "ENTREPRENEUR", "Start your own business.") { it.has(Flags.STARTED_BUSINESS) },
        AchievementDef("family_first", "FAMILY FIRST", "Choose your family when it counted.") {
            it.has(Flags.FAMILY_FIRST) || (it.stat(Stat.FAMILY) >= 90 && it.ageYears >= 30)
        },
        AchievementDef("millionaire", "MILLIONAIRE", "Reach true financial abundance.") { it.stat(Stat.MONEY) >= 92 },
        AchievementDef("survivor", "SURVIVOR", "Come through two major crises.") { it.counter(Flags.C_CRISES) >= 2 },
        AchievementDef("mentor", "MENTOR", "Lift someone up the way someone once lifted you.") { it.has(Flags.MENTORED) },
        AchievementDef("lifelong_friend", "LIFELONG FRIEND", "Keep your best friend for life.", atEnd = true) {
            it.trust(NpcRole.BEST_FRIEND) >= 70 && it.ageYears >= 60 && !it.has(Flags.BETRAYED_FRIEND)
        },
        AchievementDef("second_chance", "SECOND CHANCE", "Turn a hard moment into a new beginning.") {
            it.has(Flags.SECOND_CHANCE)
        },
        AchievementDef("clean_conscience", "CLEAN CONSCIENCE", "Live a full life without cutting corners.", atEnd = true) {
            it.trait(Trait.DISHONEST) <= 3 && it.ageYears >= 60
        },
        AchievementDef("world_traveler", "WORLD TRAVELER", "Build part of your life far from home.") { it.has(Flags.WENT_ABROAD) },
        AchievementDef("scholar", "SCHOLAR", "Master the art of learning.") { it.stat(Stat.KNOWLEDGE) >= 92 },
        AchievementDef("beloved", "WELL RESPECTED", "Earn a name people trust.") { it.stat(Stat.REPUTATION) >= 92 },
        AchievementDef("healthy_old", "STRONG TO THE END", "Stay healthy into later life.", atEnd = true) {
            it.stat(Stat.HEALTH) >= 75 && it.ageYears >= 65
        },
        AchievementDef("whole_life", "A WHOLE LIFE", "Complete a life.", atEnd = true) { it.finished }
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): AchievementDef? = byId[id]
}
