package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait

class EndingDef(
    val id: String,
    val title: String,
    val tagline: String,
    /** Closing sentence of the personal summary. */
    val closing: (GameState) -> String,
    /** 0 = does not apply; the highest score wins. */
    val score: (GameState) -> Int
)

object Endings {
    private fun avg(vararg v: Int) = v.sum() / v.size

    val all: List<EndingDef> = listOf(
        EndingDef("successful_happy", "SUCCESSFUL & HAPPY", "You built a life that worked on every level.",
            { "You built something real, and you had people to share it with." }) { s ->
            val h = s.stat(Stat.HAPPINESS); val m = s.stat(Stat.MONEY); val f = s.stat(Stat.FAMILY); val r = s.stat(Stat.REPUTATION)
            if (h >= 72 && m >= 62 && f >= 62 && r >= 60 && s.stat(Stat.HEALTH) >= 50) 85 + (avg(h, m, f, r) - 70).coerceIn(0, 10) else 0
        },
        EndingDef("wealthy_lonely", "WEALTHY BUT LONELY", "The money came. Many of the people did not stay.",
            { "Your accounts were full, but the house was quiet." }) { s ->
            val m = s.stat(Stat.MONEY)
            if (m >= 72 && (s.stat(Stat.FAMILY) <= 45 || s.stat(Stat.FRIENDSHIP) <= 40) && s.stat(Stat.HAPPINESS) <= 62) 82 + (m - 72) / 3 else 0
        },
        EndingDef("family_person", "THE FAMILY PERSON", "Your greatest work was the people around your table.",
            { "Your home was never perfect, but it was always full of love." }) { s ->
            val f = s.stat(Stat.FAMILY)
            if (f >= 72 && (s.children > 0 || s.relationship == RelationshipStatus.MARRIED) && s.stat(Stat.HAPPINESS) >= 58) 78 + (f - 72) / 2 else 0
        },
        EndingDef("respected_professional", "RESPECTED PROFESSIONAL", "People trusted your work, and your word.",
            { "Colleagues and strangers alike knew what your name stood for." }) { s ->
            if (s.careerLevel >= 4 && s.stat(Stat.REPUTATION) >= 64 && s.businessStage < 2) 76 + (s.stat(Stat.REPUTATION) - 64) / 3 else 0
        },
        EndingDef("self_made", "SELF-MADE ENTREPRENEUR", "You turned an idea into something that employed other people.",
            { "You built a company with your own hands, and it carried your values." }) { s ->
            if ((s.businessStage >= 3 || s.has(Flags.BUSINESS_SUCCESS)) && s.stat(Stat.MONEY) >= 50) 80 + (s.stat(Stat.MONEY) - 50) / 4 else 0
        },
        EndingDef("quiet_life", "THE QUIET LIFE", "You chose peace over noise, and found it.",
            { "You never chased the spotlight, and you never needed it." }) { s ->
            if (s.stat(Stat.HAPPINESS) >= 55 && s.counter(Flags.C_RISK_TAKEN) <= 2 && s.trait(Trait.AMBITIOUS) < 35 &&
                s.stat(Stat.MONEY) in 28..70) 70 else 0
        },
        EndingDef("comeback", "THE COMEBACK", "You fell. You got back up. Then you kept going.",
            { "You lost something important, and rebuilt something better." }) { s ->
            if (s.has(Flags.COMEBACK) && s.stat(Stat.HAPPINESS) >= 55) 84 else 0
        },
        EndingDef("success_at_a_cost", "SUCCESS AT A COST", "You won. You also paid for it.",
            { "You reached the top, and only then counted what the climb had cost." }) { s ->
            val ach = s.stat(Stat.MONEY) >= 68 || s.careerLevel >= 4
            if (ach && (s.stat(Stat.HEALTH) <= 45 || s.stat(Stat.FAMILY) <= 40 || s.stat(Stat.FRIENDSHIP) <= 32)) 81 else 0
        },
        EndingDef("missed_opportunities", "MISSED OPPORTUNITIES", "You played it safe. Safe has a price too.",
            { "You often wondered what might have happened if you had said yes." }) { s ->
            if (s.counter(Flags.C_RISK_AVOIDED) >= 3 && s.counter(Flags.C_RISK_TAKEN) <= 1 &&
                s.trait(Trait.AMBITIOUS) < 25 && s.stat(Stat.HAPPINESS) < 66) 72 else 0
        },
        EndingDef("adventure", "A LIFE OF ADVENTURE", "You never stayed in one place long enough to get bored.",
            { "You collected stories instead of regrets." }) { s ->
            if ((s.has(Flags.WENT_ABROAD) || s.has(Flags.ADVENTURE)) && s.trait(Trait.RISK_TAKER) >= 10 &&
                s.stat(Stat.HAPPINESS) >= 50) 75 else 0
        },
        EndingDef("financial_struggle", "FINANCIAL STRUGGLE", "Money was always the hardest part.",
            { "Money was never easy, but you kept going and kept your dignity." }) { s ->
            val m = s.stat(Stat.MONEY)
            if (m <= 24) 78 + (24 - m).coerceAtMost(8) else 0
        },
        EndingDef("broken_relationships", "BROKEN RELATIONSHIPS", "The people who mattered slowly drifted away.",
            { "You had plenty of chances to repair what was broken, and not enough of them were taken." }) { s ->
            if (s.stat(Stat.FAMILY) <= 35 && s.stat(Stat.FRIENDSHIP) <= 35) 80 else 0
        },
        EndingDef("second_chance", "SECOND CHANCE", "Life gave you a second beginning, and you took it.",
            { "You got a second chance, and you made it count." }) { s ->
            if (s.has(Flags.SECOND_CHANCE) && s.stat(Stat.HAPPINESS) >= 50) 83 else 0
        },
        EndingDef("meaningful_life", "A MEANINGFUL LIFE", "Not perfect. Not easy. But yours.",
            { "You were never the richest person in the room, but you lived a meaningful life." }) { s ->
            46 + (s.stat(Stat.HAPPINESS) + s.stat(Stat.FAMILY) + s.stat(Stat.FRIENDSHIP) + s.stat(Stat.REPUTATION)) / 18 +
                (if (s.trait(Trait.COMPASSIONATE) >= 25) 3 else 0)
        }
    )

    fun select(s: GameState): EndingDef = all.maxByOrNull { it.score(s) } ?: all.last()
}

/** Builds the short personalised paragraph from what the player actually did. */
object SummaryGenerator {
    private class Clause(val priority: Int, val text: String)

    fun summary(s: GameState, ending: EndingDef): String {
        val c = mutableListOf<Clause>()
        if (s.has(Flags.BUSINESS_FAILED)) c += Clause(100, "faced the failure of your own business")
        if (s.has(Flags.LOST_JOB)) c += Clause(95, "lost a job and had to start over")
        when (val n = s.counter(Flags.C_CAREER_CHANGES)) {
            0 -> {}
            1 -> c += Clause(90, "changed careers once")
            else -> c += Clause(90, "changed careers $n times")
        }
        if (s.counter(Flags.C_CRISES) >= 2) c += Clause(88, "weathered more than one crisis")
        else if (s.counter(Flags.C_CRISES) == 1) c += Clause(80, "came through a crisis that tested everything")
        if (s.has(Flags.COMEBACK)) c += Clause(97, "rebuilt what you had lost")
        if (s.has(Flags.FAMILY_PROTECTED)) c += Clause(85, "protected your family during difficult years")
        if (s.has(Flags.HELPED_BULLIED_FRIEND)) c += Clause(70, "stood up for a friend when it mattered")
        if (s.has(Flags.MENTORED)) c += Clause(72, "mentored people who came after you")
        if (s.businessStage >= 3) c += Clause(98, "built a thriving company")
        else if (s.businessStage == 2) c += Clause(96, "built a successful small business")
        if (s.has(Flags.WENT_ABROAD)) c += Clause(75, "built part of your life far from home")
        if (s.counter(Flags.C_KINDNESS) >= 3) c += Clause(65, "helped more people than you probably realize")
        if (s.stat(Stat.KNOWLEDGE) >= 80) c += Clause(60, "never stopped learning")
        if (s.has(Flags.TOOK_SHORTCUT)) c += Clause(78, "cut corners you later wished you hadn't")
        if (s.has(Flags.REFUSED_UNETHICAL)) c += Clause(74, "turned down work that went against your conscience")
        if (s.has(Flags.WORKED_TOO_MUCH)) c += Clause(55, "gave too many years to work")
        if (s.children > 0) c += Clause(50, if (s.children == 1) "raised a child" else "raised ${s.children} children")
        if (s.careerLevel >= 4) c += Clause(58, "worked your way up to ${s.jobTitle.lowercase()}")

        val chosen = c.sortedByDescending { it.priority }.take(4)
        val body = when (chosen.size) {
            0 -> "You lived a quiet life, making your choices one day at a time"
            1 -> "You ${chosen[0].text}"
            else -> "You " + chosen.dropLast(1).joinToString(", ") { it.text } + ", and " + chosen.last().text
        }
        return "$body. ${ending.closing(s)}"
    }
}
