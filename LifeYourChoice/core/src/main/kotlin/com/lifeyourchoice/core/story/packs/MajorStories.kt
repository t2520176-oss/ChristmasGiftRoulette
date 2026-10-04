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

/** The big moments: illness, job loss, crises, opportunities, moving and midlife. Crises build resilience. */
val MajorStories = storyPack("major", "Major Life Events") {

    scenario("maj_parent_ill", MAJOR, HOSPITAL, 28..58, weight = 10, title = "Family") {
        text("Your father is seriously ill. The treatment is expensive and could put you in debt. You have to decide what to do.")
        extra(has("ignored_parents"), "You can’t help thinking about the calls you didn’t return.")
        extra(has("supported_family"), "You’ve always been there for your family, and they’re here for you now, standing quietly at your side.")
        choice("Use your savings for his treatment.") {
            enableIf("You don’t have enough savings for this.", atLeast(Stat.MONEY, 28))
            money(-22); family(+10); happiness(+3); trait(LOYAL, 3); trait(RESPONSIBLE, 3); count("crises"); flag("family_protected", "survived_crisis")
            milestone("Spent your savings to save your father"); hint()
            result("The treatment works, and he recovers slowly. The money is gone, but he’s still here to tell stories at dinner.")
        }
        choice("Look for a loan.") {
            money(-8); family(+6); trait(RESPONSIBLE, 2); count("crises"); flag("family_protected", "in_debt", "survived_crisis")
            result("The loan is heavy, but it buys the time you need. Your family steadies itself around the debt.")
        }
        choice("Try a cheaper treatment.") {
            family(+2); money(-5); count("crises"); flag("survived_crisis")
            outcome(55) { bonus(atLeast(Stat.KNOWLEDGE, 60), 15); family(+3); result("With careful research, you find an affordable programme that works. It’s a long road, but he recovers.") }
            outcome(45) { family(-4); happiness(-5); health(-3); result("The cheaper option doesn’t work as well, and the recovery is slow and painful. You wonder whether you should have chosen differently.") }
        }
        choice("Focus on your career and hope for the best.") {
            career(+5); money(+3); family(-10); happiness(-6); trait(LOYAL, -3); flag("ignored_parents"); count("mistakes"); count("crises")
            result("Work is a refuge. Your father pulls through with help from others, and a quiet disappointment lingers in the room.")
        }
    }

    scenario("maj_job_loss", MAJOR, OFFICE, 27..56, weight = 8, title = "Job Loss") {
        requires(employed, careerIs(EMPLOYEE, MANAGER, TECH, ENGINEER, DESIGNER, SKILLED_WORKER))
        boost(atMost(Stat.REPUTATION, 40), 6)
        text("It happens on an ordinary Tuesday. The company is “restructuring,” and your role is gone. You’re given a box, a handshake and an hour to clear your desk.")
        choice("Start job hunting immediately.") {
            loseJob(); money(-6); confidence(-6); happiness(-6); energy(-6); count("crises"); flag("survived_crisis")
            outcome(55) { bonus(atLeast(Stat.REPUTATION, 55), 20); bonus(atLeast(Stat.KNOWLEDGE, 60), 10); rehire(0); confidence(+6); result("Within weeks you land a new position, thanks to a solid track record.") }
            outcome(45) { rehire(-1); money(-6); result("The search drags on. In the end you take a role a step down, and rebuild from there.") }
        }
        choice("Use the time to retrain.") {
            loseJob(); money(-9); knowledge(+9); discipline(+4); confidence(-2); count("crises"); flag("survived_crisis"); count("redemptions")
            trait(DISCIPLINED, 2); trait(AMBITIOUS, 1)
            outcome(60) { bonus(atLeast(Stat.DISCIPLINE, 55), 20); rehire(+1); money(+3); confidence(+6); result("The new skills pay off. You return stronger, and better paid.") }
            outcome(40) { rehire(0); result("It takes a long time. When you re-enter, it’s at the same level, but with more tools in your belt.") }
        }
        choice("Start your own business.") {
            enableIf("You’d need more knowledge and savings to go it alone.", atLeast(Stat.KNOWLEDGE, 40))
            loseJob(); money(-8); confidence(+3); trait(RISK_TAKER, 3); count("crises"); count("risk_taken")
            flag("started_business", "took_business_risk", "survived_crisis"); business(1); job(ENTREPRENEUR, 1)
            milestone("Turned a layoff into your first business"); schedule("biz_first_year", 1, 2)
            result("Losing the job gave you the push you were too afraid to take. Now there’s no safety net, and no limits.")
        }
        choice("Lean on your network.") {
            loseJob(); money(-5); confidence(-3); count("crises"); flag("survived_crisis")
            outcome(55) { bonus(atLeast(Stat.FRIENDSHIP, 60), 20); bonus(atLeast(Stat.REPUTATION, 60), 10); rehire(0); friendship(+3); result("An old colleague has been thinking about you, and offers an interview. You’re back at work before the savings run low.") }
            outcome(45) { rehire(-1); happiness(-3); result("Few doors open. You do find work eventually, but not what you hoped for.") }
        }
        choice("Call {coworker}, whom you once helped.") {
            showIf(has("helped_coworker"), trustAtLeast(COWORKER, 60))
            badge("Unlocked: you helped {coworker} when it mattered")
            loseJob(); rehire(+1); money(+2); confidence(+8); trust(COWORKER, +10); count("crises"); flag("survived_crisis", "kindness_returned")
            milestone("A coworker you once helped got you a new job")
            result("“Come work with us,” says {coworker}. “You were there when it mattered to me.” You start Monday.")
        }
    }

    scenario("maj_overseas_offer", MAJOR, AIRPORT, 25..46, weight = 8, title = "Opportunity Abroad") {
        requires(employed)
        text("An international company offers you a role overseas, a significant step up, in a place you’ve never been. It would take you far from family and friends for at least two years.")
        choice("Take it. You’re going.") {
            career(+8); money(+8); confidence(+8); family(-6); friendship(-5); trait(RISK_TAKER, 3); trait(AMBITIOUS, 2); count("risk_taken")
            flag("went_abroad"); flag("adventure"); promote(); milestone("Moved abroad for a career opportunity")
            result("The first month is hard and the second is better. By the end of the year, you can’t imagine not having gone.")
        }
        choice("Decline. Home is where your people are.") {
            family(+4); friendship(+2); happiness(+1); career(-2); count("risk_avoided")
            result("A good decision in many ways. It leaves a faint question of what if.")
        }
        choice("Negotiate for a shorter assignment.") {
            career(+4); money(+3); trait(RESPONSIBLE, 2); confidence(+3)
            outcome(55) { bonus(atLeast(Stat.REPUTATION, 55), 15); flag("went_abroad"); family(-2); result("They agree to six months. You return with a better title and many stories.") }
            outcome(45) { career(-1); result("They say it’s all or nothing. You politely decline, with respect on both sides.") }
        }
        choice("Talk it through with the people you love.") {
            family(+5); trust(PARTNER, +4); trait(RESPONSIBLE, 2); knowledge(+1)
            result("The conversation turns up fears and hopes you hadn’t known about. Whatever you decide, you decide it together.")
        }
    }

    scenario("maj_family_emergency", MAJOR, HOSPITAL, 28..62, weight = 7, title = "Family Emergency") {
        text("Your phone rings in the middle of an important meeting. It’s {sibling}: a close relative has been rushed to hospital, and the doctors say the next few hours matter.")
        choice("Leave immediately.") {
            family(+9); career(-4); trust(SIBLING, +10); trait(LOYAL, 3); energy(-8); flag("family_first", "family_protected")
            milestone("Dropped everything for family in an emergency")
            result("You’re at the hospital within the hour. It isn’t a catastrophe, but you’re there to hold a hand.")
        }
        choice("Finish the meeting first, then go.") {
            family(+3); career(+2); trust(SIBLING, -4)
            result("You get through the meeting somehow, and hurry in with a heavy heart.")
        }
        choice("Call for updates and stay at work.") {
            family(-5); career(+3); trust(SIBLING, -10); happiness(-4); count("mistakes")
            result("By the time you get there, visiting hours are over. {sibling} doesn’t say anything, and that’s worse.")
        }
        choice("Ask a colleague to cover for you.") {
            family(+7); trust(COWORKER, +6); career(-1); trait(RESPONSIBLE, 2); energy(-6)
            result("A team that trusts you is a team that has your back. Your colleague takes over, and you go.")
        }
    }

    scenario("maj_move_city", MAJOR, AIRPORT, 24..46, weight = 7, title = "A New City") {
        text("A big opportunity opens up in another city, with a better job, better pay and a new start. It also means leaving the place you’ve called home since childhood.")
        choice("Move.") {
            career(+6); money(+5); confidence(+5); friendship(-7); family(-3); trait(RISK_TAKER, 2); count("risk_taken")
            result("You pack your whole life into boxes. The city is loud, bright and strange, and you love it.")
        }
        choice("Stay.") {
            family(+3); friendship(+3); career(-2); happiness(+1); count("risk_avoided")
            result("You stay close to the people you know. It’s comfortable, though you wonder.")
        }
        choice("Negotiate a hybrid arrangement.") {
            career(+3); money(+2); energy(-6); trait(RESPONSIBLE, 1)
            outcome(55) { bonus(atLeast(Stat.REPUTATION, 55), 15); family(+2); result("Mostly remote, with regular trips. You get the best of both.") }
            outcome(45) { energy(-6); career(-1); result("The arrangement works poorly. Commuting is tiring.") }
        }
        choice("Ask {friend} and family to weigh in.") {
            friendship(+3); family(+3); trust(BEST_FRIEND, +3)
            result("Everyone has an opinion. In the end, you realise you already knew your answer.")
        }
    }

    scenario("maj_friend_in_trouble", MAJOR, APARTMENT, 26..58, weight = 8, title = "A Friend in Trouble") {
        text("{friend} calls late at night and asks for a big favour: to co-sign a loan to stop {friend.him} losing everything. “I swear I’ll pay it back. I’d never ask otherwise.”")
        choice("Co-sign. {friend.He} needs you.") {
            trust(BEST_FRIEND, +14); money(-5); trait(LOYAL, 4); trait(RISK_TAKER, 2); count("risk_taken"); flag("supported_friend")
            outcome(55) { bonus(trustAtLeast(BEST_FRIEND, 65), 25); money(+5); friendship(+5); result("{friend} keeps every promise and pays the loan off early.") }
            outcome(45) { money(-12); trust(BEST_FRIEND, -4); result("{friend} struggles, and you end up paying part of the loan. The friendship survives, but it’s changed.") }
        }
        choice("Offer a smaller gift instead.") {
            trust(BEST_FRIEND, +7); money(-4); trait(RESPONSIBLE, 2); trait(COMPASSIONATE, 1)
            result("You give what you can afford to lose, with no strings attached. It’s a generous, sensible compromise.")
        }
        choice("Help {friend.him} find a financial adviser.") {
            trust(BEST_FRIEND, +6); knowledge(+2); energy(-4); trait(RESPONSIBLE, 3)
            result("An hour with a professional opens options neither of you had considered.")
        }
        choice("Refuse. You can’t risk it.") {
            trust(BEST_FRIEND, -15); friendship(-5); money(+1); happiness(-3); flag("refused_friend_help")
            result("It was a rational choice, and it’s a hard one to live with.")
        }
    }

    scenario("maj_financial_crisis", MAJOR, NIGHT_CITY, 32..60, weight = 7, title = "Hard Times") {
        text("The economy sours. Prices rise, hours are cut, and every family you know is quietly tightening their belt. Your own income takes a heavy hit.")
        choice("Cut spending and ride it out.") {
            money(+2); discipline(+4); happiness(-3); trait(DISCIPLINED, 2); count("crises"); flag("survived_crisis")
            onlyIf(has("saved_money")) { money(+5); happiness(+3); note("Your savings keep you steady while others struggle.") }
            result("You live more simply for a while. It’s hard, but it’s not unbearable.")
        }
        choice("Take on extra work.") {
            money(+5); energy(-12); health(-4); family(-3); count("crises"); count("overwork"); flag("survived_crisis")
            result("You take every shift you can. The money helps, and your body complains.")
        }
        choice("Sell something valuable to stay afloat.") {
            money(+7); happiness(-4); count("crises"); flag("survived_crisis")
            result("It hurts to let go, but the cushion gets you through.")
        }
        choice("Help your neighbours organise mutual support.") {
            reputation(+8); friendship(+8); happiness(+4); money(-2); trait(COMPASSIONATE, 3); count("kindness"); count("crises"); flag("survived_crisis")
            result("A community pantry, shared tools and shared dinners turn a hard year into a warm memory.")
        }
    }

    scenario("maj_health_scare", MAJOR, HOSPITAL, 40..64, weight = 8, title = "Health Scare") {
        boost(atMost(Stat.HEALTH, 50), 12)
        boost(has("worked_too_much"), 10)
        text("The doctor’s expression is serious. Blood pressure, sleep, stress: the tests say your body has been sending signals for years. “This is a warning,” she says. “It’s not too late.”")
        choice("Change your lifestyle completely.") {
            health(+14); energy(+8); happiness(+3); career(-2); discipline(+4); trait(DISCIPLINED, 3); count("crises"); count("redemptions"); flag("survived_crisis"); count("overwork", -3)
            milestone("Changed your life after a health scare")
            result("Walks, real food, sleep. It’s tedious at first, then oddly wonderful. Six months later, you feel years younger.")
        }
        choice("Ignore it. You’re too busy.") {
            health(-12); energy(-5); career(+2); happiness(-4); count("mistakes"); flag("worked_too_much")
            result("The doctor’s warning is easy to dismiss. Your body is less forgiving.")
        }
        choice("Get a second opinion.") {
            knowledge(+2); money(-3); health(+4)
            outcome(50) { health(+6); result("The second doctor is more optimistic, with a clear, manageable plan.") }
            outcome(50) { health(+3); result("The second opinion agrees with the first, with more details this time.") }
        }
        choice("Lighten your workload, but keep going.") {
            health(+6); career(-3); money(-2); trait(RESPONSIBLE, 2); count("overwork", -2)
            result("A compromise that helps your health, and surprises your boss.")
        }
    }

    scenario("maj_community_crisis", MAJOR, STREET, 24..66, weight = 6, title = "Storm Night") {
        text("A severe storm floods half of your town overnight. Roads are blocked, power is out, and the local radio is asking for help from anyone who can give it.")
        choice("Volunteer, and help evacuate people.") {
            reputation(+9); energy(-12); happiness(+5); trait(COMPASSIONATE, 4); trait(RISK_TAKER, 1); count("kindness"); flag("community_hero")
            milestone("Helped neighbours through a disaster")
            result("Wet, tired and filthy, you help dozens of families to safety. You’re remembered for it.")
        }
        choice("Donate supplies and money.") {
            money(-4); reputation(+4); trait(COMPASSIONATE, 2); happiness(+3); count("kindness")
            result("It’s not heroic, but it helps, and you sleep a little better.")
        }
        choice("Open your home to neighbours.") {
            family(+3); friendship(+6); reputation(+6); energy(-6); trait(COMPASSIONATE, 3); count("kindness")
            result("Your living room becomes a camp. The nights are crowded, and kind.")
        }
        choice("Stay safe at home.") {
            health(+1); reputation(-1); happiness(-2)
            result("You keep your family safe, which matters most. Afterwards, you feel useless.")
        }
    }

    scenario("maj_midlife_crossroads", MAJOR, SKYLINE, 42..54, weight = 9, title = "Crossroads") {
        text("One morning, in the middle of an ordinary Wednesday, you stop and ask yourself: “Is this the life I wanted?” Your calendar is full, your routines are fixed, and something isn’t quite right.")
        choice("Change direction completely.") {
            happiness(+6); confidence(+6); money(-6); career(-4); trait(RISK_TAKER, 3); count("risk_taken"); count("career_changes_planned")
            flag("midlife_reinvention"); job(CREATOR, 2); milestone("Reinvented your career at midlife")
            result("You trade the corner office for something that feels like yours. It’s scary, and, for the first time in years, exciting.")
        }
        choice("Double down on what you have.") {
            career(+5); money(+4); happiness(-2); energy(-6)
            result("You work harder than ever. It pays off, mostly.")
        }
        choice("Take a long sabbatical.") {
            happiness(+9); energy(+15); health(+5); money(-10); family(+4); flag("adventure")
            result("Months of travel and tea, and slow mornings. You come back lighter.")
        }
        choice("Find a passion project alongside your job.") {
            happiness(+6); energy(-4); knowledge(+3); confidence(+4); trait(AMBITIOUS, 1)
            result("An hour or two a day becomes the best part of your day.")
        }
    }

    scenario("maj_unexpected_opportunity", MAJOR, MEETING, 24..52, weight = 7, title = "Unexpected Opportunity") {
        requires(employed)
        text("A casual conversation at a conference turns into something bigger: a person you barely know offers you a chance to lead a high-profile project. It’s a big leap, and you have a day to decide.")
        choice("Say yes.") {
            career(+7); confidence(+6); energy(-10); trait(RISK_TAKER, 3); trait(AMBITIOUS, 2); count("risk_taken")
            outcome(60) { bonus(atLeast(Stat.KNOWLEDGE, 55), 12); bonus(atLeast(Stat.DISCIPLINE, 55), 12); promote(); money(+8); reputation(+7); milestone("Seized a once-in-a-lifetime opportunity"); result("It’s a stretch, and you make it. Doors that were closed for years begin to open.") }
            outcome(40) { money(-3); confidence(-3); result("It’s harder than you expected, and the project is only a partial success. Still, you’re noticed for trying.") }
        }
        choice("Ask for a week to prepare, then decide.") {
            knowledge(+3); discipline(+2); trait(RESPONSIBLE, 2)
            outcome(50) { career(+4); result("They’re happy to wait. Prepared, you say yes with a plan.") }
            outcome(50) { count("risk_avoided"); result("The opportunity goes to someone who said yes immediately.") }
        }
        choice("Decline politely. It’s too much right now.") {
            happiness(+1); energy(+4); count("risk_avoided"); career(-2)
            result("Calm, safe, and a little wistful.")
        }
        choice("Recommend someone else from your team.") {
            reputation(+5); trust(COWORKER, +12); trait(COMPASSIONATE, 3); count("kindness"); flag("helped_coworker")
            result("Your colleague takes the role and flourishes, and doesn’t forget who gave them the chance.")
        }
    }
}
