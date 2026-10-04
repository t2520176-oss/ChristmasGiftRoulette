package com.lifeyourchoice.core.story.packs

import com.lifeyourchoice.core.model.CareerTrack.*
import com.lifeyourchoice.core.model.Category.*
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.NpcRole.*
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.SceneArt.*
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait.*
import com.lifeyourchoice.core.story.*

/** Job interview and safe, exciting action situations. Each one has a cinematic staging. */
val ActionStories = storyPack("action", "Moments of Action") {

    scenario("car_interview", CAREER, INTERVIEW_ROOM, 24..50, weight = 10, title = "Job Interview") {
        requires(employed, careerLevel(max = 4))
        boost(counterAtLeast("career_changes", 1), 6)
        text("You are interviewing for a better position at another company. The interviewer taps your résumé: “You’ve changed jobs a few times. Why should we trust you to stay?”")
        choice("I’m looking for long-term growth.") {
            confidence(+2); trait(AMBITIOUS, 1)
            outcome(50) {
                bonus(atLeast(Stat.CAREER, 45), 15); bonus(atLeast(Stat.REPUTATION, 55), 12); bonus(atLeast(Stat.CONFIDENCE, 55), 8)
                promote(); money(+7); career(+6); milestone("Won a better position after a tough interview")
                result("They like the ambition behind the answer. A week later, the offer arrives.")
            }
            outcome(50) { career(+1); result("They thank you politely. Another candidate gets the role.") }
        }
        choice("I’ve learned a lot from my past jobs.") {
            knowledge(+2); trait(RESPONSIBLE, 1)
            outcome(55) {
                bonus(atLeast(Stat.KNOWLEDGE, 55), 18); bonus(atLeast(Stat.DISCIPLINE, 55), 8)
                promote(); money(+6); career(+5); milestone("Won a better position after a tough interview")
                result("Turning job-hopping into a story of growth wins them over. You get the job.")
            }
            outcome(45) { career(+1); result("A solid answer, but not a memorable one. They go with someone else.") }
        }
        choice("I understand your concern.") {
            trait(RESPONSIBLE, 2); reputation(+2); confidence(+1)
            outcome(50) {
                bonus(atLeast(Stat.REPUTATION, 60), 15)
                promote(); money(+6); career(+5); milestone("Won a better position after a tough interview")
                result("Honesty about your history impresses them more than a polished answer would. You’re hired.")
            }
            outcome(50) { career(+1); result("They appreciate the candour, but the concern stays. No offer this time.") }
        }
        choice("I may not be the right fit, then.") {
            confidence(-4); happiness(-2); count("risk_avoided"); trait(RISK_TAKER, -1)
            result("The words are out before you can take them back. The interviewer nods and thanks you for your time.")
        }
    }

    scenario("act_run_for_train", CAREER, STREET, 22..55, weight = 8, title = "Running Late") {
        requires(employed)
        text("Today is the most important meeting of the year, and your alarm didn’t go off. The station is four blocks away. You can hear the train arriving.")
        choice("Run for it!") {
            energy(-10); health(-1); trait(IMPULSIVE, 1)
            outcome(50) { bonus(atLeast(Stat.HEALTH, 65), 22); bonus(atLeast(Stat.ENERGY, 60), 10); career(+3); confidence(+3); result("You slide through the doors as they close. Out of breath, but on time.") }
            outcome(50) { career(-2); confidence(-2); result("You watch the train pull away from the platform, gasping for air.") }
        }
        choice("Take the shortcut through the park.") {
            trait(RISK_TAKER, 2); count("risk_taken"); energy(-6)
            outcome(55) { bonus(atLeast(Stat.KNOWLEDGE, 50), 8); career(+2); confidence(+4); result("The shortcut saves two minutes. You make it, just.") }
            outcome(45) { career(-3); energy(-4); result("A locked gate ruins your plan. You arrive at the office long after it started.") }
        }
        choice("Call ahead and explain.") {
            trait(RESPONSIBLE, 2); reputation(+2); confidence(+1)
            result("You phone your team, apologise and ask them to start without you. It isn’t perfect, but it’s honest.")
        }
        choice("Hail a taxi.") {
            money(-4); career(+2); energy(-2)
            result("Expensive, but you arrive calm and only a few minutes late.")
        }
    }

    scenario("act_basketball_final", SCHOOL, BASKETBALL_COURT, 15..18, weight = 16, title = "The Final") {
        requires(has("on_the_team"))
        text("Final seconds of the championship game. One point down. The ball is in your hands, the crowd is loud, and {friend} is waving for a pass.")
        choice("Take the shot yourself!") {
            trait(RISK_TAKER, 2); count("risk_taken")
            outcome(45) { bonus(atLeast(Stat.CONFIDENCE, 55), 20); bonus(atLeast(Stat.DISCIPLINE, 60), 15); reputation(+9); confidence(+8); happiness(+7); milestone("Scored the winning shot"); result("Swish. The gym erupts. You are carried off the court.") }
            outcome(55) { confidence(-3); happiness(-3); result("The ball rattles off the rim. The buzzer sounds. Silence.") }
        }
        choice("Pass to {friend}.") {
            trust(BEST_FRIEND, +10); trait(LOYAL, 2); friendship(+4)
            outcome(55) { bonus(trustAtLeast(BEST_FRIEND, 60), 20); reputation(+6); happiness(+7); friendship(+4); result("{friend} catches it, jumps, shoots, and scores. You win together.") }
            outcome(45) { happiness(-2); result("{friend} is blocked at the last second. You lose, but nobody blames you.") }
        }
        choice("Drive to the basket.") {
            health(-2); energy(-6); trait(RISK_TAKER, 1)
            outcome(50) { bonus(atLeast(Stat.HEALTH, 70), 18); reputation(+7); confidence(+6); happiness(+6); result("You weave through two defenders and lay it in. The crowd goes wild.") }
            outcome(50) { reputation(+1); result("A defender stops you short. The game ends, but you gave everything.") }
        }
        choice("Call a time-out.") {
            discipline(+3); trait(RESPONSIBLE, 2)
            outcome(60) { bonus(atLeast(Stat.KNOWLEDGE, 50), 10); reputation(+5); happiness(+5); result("The coach draws up a perfect play. It works. You win.") }
            outcome(40) { happiness(-1); result("The play is read, and blocked. The game ends one point short.") }
        }
    }

    scenario("act_emergency_help", MAJOR, STREET, 20..72, weight = 6, title = "Emergency") {
        text("On the pavement, an elderly man collapses. People gather and stare. Someone says, “Should we do something?” Nobody moves.")
        choice("Call emergency services and stay with him.") {
            trait(RESPONSIBLE, 3); trait(COMPASSIONATE, 2); reputation(+5); confidence(+4); count("kindness"); flag("helped_stranger")
            milestone("Helped a stranger in an emergency")
            result("You give the address, stay on the line and hold his hand until the ambulance arrives. He is going to be fine.")
        }
        choice("Use the first aid you know.") {
            trait(RISK_TAKER, 1); trait(COMPASSIONATE, 2); count("kindness"); flag("helped_stranger"); energy(-6)
            outcome(65) { bonus(atLeast(Stat.KNOWLEDGE, 55), 15); reputation(+7); confidence(+6); result("Your calm, careful help keeps him stable until the paramedics arrive.") }
            outcome(35) { reputation(+3); confidence(+1); result("You do what you can. The paramedics take over and say you did well.") }
        }
        choice("Tell people what to do and organise help.") {
            trait(RESPONSIBLE, 2); confidence(+5); reputation(+4); flag("helped_stranger")
            result("“You, call 911. You, bring a blanket.” Everyone has a job, and suddenly the street is a team.")
        }
        choice("Walk on. Somebody else will help.") {
            happiness(-3); trait(COMPASSIONATE, -2); count("mistakes")
            result("You keep walking. For the rest of the day, you can’t stop thinking about it.")
        }
    }

    scenario("act_big_presentation", CAREER, MEETING, 24..55, weight = 9, title = "The Presentation") {
        requires(employed)
        text("One minute before the biggest presentation of your career, your laptop dies. The room is full. {boss} glances at the clock.")
        choice("Present without slides.") {
            confidence(+3); trait(RISK_TAKER, 2); count("risk_taken")
            outcome(55) { bonus(atLeast(Stat.CONFIDENCE, 55), 18); bonus(atLeast(Stat.KNOWLEDGE, 60), 15); career(+8); reputation(+6); result("Without slides, you speak from the heart. The room is captivated, and the deal is signed.") }
            outcome(45) { career(-2); confidence(-3); result("You lose your thread halfway. It’s a rough forty minutes.") }
        }
        choice("Ask for five minutes to fix it.") {
            trait(RESPONSIBLE, 1); energy(-4)
            outcome(60) { bonus(atLeast(Stat.DISCIPLINE, 50), 12); career(+4); result("You borrow a charger, restart, and recover just in time. Your calm earns respect.") }
            outcome(40) { career(-1); reputation(-1); result("Five minutes becomes fifteen. The room’s patience wears thin.") }
        }
        choice("Use {coworker}’s laptop.") {
            showIf(trustAtLeast(COWORKER, 55))
            badge("Because {coworker} trusts you")
            trust(COWORKER, +8); career(+6); reputation(+4); confidence(+3)
            result("“Take mine,” says {coworker}, already unplugging it. You present flawlessly, and make sure everyone knows who saved the day.")
        }
        choice("Admit the problem and reschedule.") {
            reputation(-2); career(-2); trait(RESPONSIBLE, 1)
            result("Honest, if embarrassing. {boss} reschedules for tomorrow with a tight smile.")
        }
    }
}
