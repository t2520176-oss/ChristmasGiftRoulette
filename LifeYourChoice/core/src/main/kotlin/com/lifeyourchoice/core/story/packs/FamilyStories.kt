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

/** Family life: parents, siblings, money worries, children and ageing relatives. */
val FamilyStories = storyPack("family", "Family Life") {

    scenario("fam_parents_arguing", FAMILY, LIVING_ROOM, 14..17, weight = 10) {
        text("From your room you hear your parents arguing in low, tense voices about money. Dinner tomorrow is going to be quiet.")
        choice("Walk in and ask what’s wrong.") {
            family(+4); confidence(+3); trait(RESPONSIBLE, 2); happiness(-2)
            result("They stop, surprised. Then they admit things have been tight lately. It helps that you asked.")
        }
        choice("Put in headphones and pretend you didn’t hear.") {
            happiness(-2); family(-2); energy(+2)
            result("The music helps for a while. The tension is still there at breakfast.")
        }
        choice("Offer to help with chores and pocket money.") {
            family(+5); money(-2); discipline(+3); trait(RESPONSIBLE, 3); trait(LOYAL, 2); flag("supported_family")
            hint()
            result("Your parents are touched. It doesn’t fix everything, but it makes the house feel like a team.")
        }
        choice("Ask {sibling} what’s going on.") {
            trust(SIBLING, +8); family(+2); knowledge(+1)
            result("{sibling} knows more than you thought. The two of you agree to be extra kind at home.")
        }
    }

    scenario("fam_money_trouble", FAMILY, KITCHEN, 15..19, weight = 9) {
        text("Your parents sit you down. Because of money problems, they can’t afford the school trip, or the extra class you were hoping for. They’re clearly ashamed to say it.")
        choice("Tell them it’s okay and offer to earn your own way.") {
            family(+6); discipline(+4); trait(RESPONSIBLE, 3); money(+2); confidence(+3); flag("supported_family")
            result("It isn’t easy, but you find odd jobs. Your parents don’t say much, but their eyes say plenty.")
        }
        choice("Tell them you’re disappointed.") {
            family(-4); happiness(-3); confidence(-1)
            result("The words are out before you can stop them. You see their faces fall and wish you could take it back.")
        }
        choice("Ask relatives for help.") {
            family(+1); money(+3); reputation(-2)
            result("An uncle quietly covers the cost. It helps, though your parents are embarrassed.")
        }
        choice("Say nothing, but quietly give up the idea.") {
            happiness(-3); family(+3); knowledge(-2); discipline(+1)
            result("You skip the trip. Nobody asks you to, but you know you made it easier for them.")
        }
    }

    scenario("fam_sibling_conflict", FAMILY, LIVING_ROOM, 14..24, weight = 10) {
        text("{sibling} borrowed something important to you without asking, and it came back broken. You’re furious, and so is {sibling}, who insists it was an accident.")
        choice("Yell and demand payment.") {
            family(-4); trust(SIBLING, -10); happiness(-3); trait(IMPULSIVE, 2)
            result("A shouting match follows. Neither of you says sorry.")
        }
        choice("Calmly explain why it matters.") {
            family(+3); trust(SIBLING, +8); trait(RESPONSIBLE, 2); confidence(+2)
            result("{sibling} listens, apologises, and offers to make it up to you. It’s a good talk.")
        }
        choice("Let it go. It’s just an object.") {
            family(+2); happiness(-1); trust(SIBLING, +4); trait(COMPASSIONATE, 1)
            result("You swallow your anger. It stings at first, but the peace is worth it.")
        }
        choice("Ask your parents to settle it.") {
            family(+1); trust(SIBLING, -3); trait(RESPONSIBLE, 1)
            result("Your parents sort it out sensibly. You both feel slightly treated like kids.")
        }
    }

    scenario("fam_help_parents", FAMILY, KITCHEN, 16..26, weight = 9) {
        text("Your parents are overwhelmed at the moment. They’ve asked you to take on more at home: groceries, cooking, picking up {sibling}. Your friends are planning a big weekend away.")
        choice("Stay and help your family.") {
            family(+7); discipline(+3); happiness(-2); friendship(-3); trait(RESPONSIBLE, 3); flag("supported_family"); hint()
            result("The weekend is quiet and tiring. Your parents don’t say thank you, but they look at you differently.")
        }
        choice("Go with your friends. You’ll make it up later.") {
            happiness(+7); friendship(+5); family(-5); trait(IMPULSIVE, 2); flag("ignored_parents")
            result("The trip is wonderful. When you get back, the house is tidy and quiet, and nobody mentions it.")
        }
        choice("Do the chores first, then join the trip late.") {
            family(+3); friendship(+2); energy(-8); discipline(+2)
            result("It’s exhausting, but you pull it off. You arrive tired and proud.")
        }
        choice("Ask {sibling} to share the load.") {
            family(+3); trust(SIBLING, +4); trait(RESPONSIBLE, 1)
            result("{sibling} grumbles, then steps up. It’s the first time you’ve really worked as a team.")
        }
    }

    scenario("fam_expectations", FAMILY, LIVING_ROOM, 16..22, weight = 11) {
        text("Your parents have always dreamed of you following a certain path. Lately, you’ve realised you want something different, and the conversation can’t wait any longer.")
        choice("Do what your parents want.") {
            family(+6); happiness(-4); confidence(-2); trait(RESPONSIBLE, 2); count("risk_avoided")
            result("They’re delighted. You’re not sure whose life you’re living, but it’s safe.")
        }
        choice("Tell them honestly what you want instead.") {
            confidence(+5); family(-3); trait(RISK_TAKER, 2); trait(AMBITIOUS, 2); happiness(+4); count("risk_taken")
            outcome(55) { bonus(atLeast(Stat.FAMILY, 65), 20); family(+4); result("It’s a hard conversation, but they come around. “We just want you to be happy,” your father says.") }
            outcome(45) { family(-4); result("They don’t speak to you for days. It’s a cold house for a while.") }
        }
        choice("Find a compromise.") {
            family(+3); happiness(+1); knowledge(+2); trait(RESPONSIBLE, 1)
            result("You agree on a plan with space for both. Nobody’s fully happy, which, you suspect, is what a compromise is.")
        }
        choice("Pretend to agree, and quietly do your own thing.") {
            family(-2); trait(DISHONEST, 3); happiness(+2); flag("hid_plans")
            result("It works, for now. You know it will have to come out some day.")
        }
    }

    scenario("fam_moving_house", FAMILY, NEW_HOME, 14..17, weight = 7) {
        text("Your family is moving to a new neighbourhood because of your parent’s work. New school, new streets, and your closest friends will be an hour away.")
        choice("Make an effort to meet people at the new school.") {
            confidence(+5); friendship(+3); trait(RISK_TAKER, 1); reputation(+2)
            result("It’s scary, but you join a club in week one. By the end of the term, you have a new group.")
        }
        choice("Stay in touch with your old friends every day.") {
            friendship(+2); trust(BEST_FRIEND, +8); trait(LOYAL, 3); happiness(-1)
            result("Late-night video calls keep the bond alive, though your new neighbourhood stays a bit foreign.")
        }
        choice("Stay in your room and wait it out.") {
            happiness(-5); confidence(-3); friendship(-4); energy(+3)
            result("The months crawl. You feel invisible, and a bit sorry for yourself.")
        }
        choice("Help your parents unpack and set up the new house.") {
            family(+5); discipline(+2); trait(RESPONSIBLE, 2); happiness(+1)
            result("The house starts to feel like home faster than you expected, thanks partly to your work.")
        }
    }

    scenario("fam_grandparent", FAMILY, LIVING_ROOM, 15..45, weight = 7) {
        text("Your grandmother calls and asks you to visit more often. She’s starting to forget little things. “You’re busy,” she says, “I know.”")
        choice("Visit every week.") {
            family(+7); happiness(+3); energy(-6); trait(COMPASSIONATE, 2); trait(LOYAL, 2); flag("close_to_grandparent")
            result("She tells you stories you’ve never heard. You leave each visit with more than you brought.")
        }
        choice("Call regularly, and visit when you can.") {
            family(+3); trait(RESPONSIBLE, 1)
            result("It’s not perfect, but it keeps the thread alive.")
        }
        choice("Promise to visit soon, then get busy.") {
            family(-3); happiness(-2); flag("ignored_parents"); count("mistakes")
            result("“Soon” becomes months. You tell yourself there’s still time.")
        }
        choice("Bring {friend} along and make it a party.") {
            family(+4); friendship(+3); happiness(+4); trust(BEST_FRIEND, +3)
            result("Grandma loves the company, and wins at cards. You all stay for dinner.")
        }
    }

    scenario("fam_dinner_missed", FAMILY, KITCHEN, 22..45, weight = 10) {
        requires(has("first_job"))
        text("It’s the third time this month you’ve missed the Sunday family dinner. Your mother doesn’t complain. She just sets one less place.")
        choice("Make this Sunday count: be there, phone off.") {
            family(+7); happiness(+4); energy(+3); trait(RESPONSIBLE, 2); count("overwork", -1)
            result("The food is simple and the conversation is easy. You realise how much you’d missed it.")
        }
        choice("Send a big gift to make up for it.") {
            family(+1); money(-4); happiness(-1)
            result("It’s appreciated, but not what anyone wanted.")
        }
        choice("Keep working. Things will calm down soon.") {
            career(+4); money(+2); family(-6); happiness(-3); flag("worked_too_much"); count("overwork")
            result("The promotion is a step closer. The empty chair, too.")
        }
        choice("Invite the family to your place instead.") {
            family(+4); money(-3); happiness(+2); discipline(+1)
            result("It’s a bit chaotic, but warm. They’re proud to see where you live.")
        }
    }

    scenario("fam_sibling_loan", FAMILY, LIVING_ROOM, 25..50, weight = 9) {
        requires(has("first_job"))
        text("{sibling} calls late at night, embarrassed. {sibling.He} is behind on rent and asks if you can lend a significant amount of money. “I’ll pay you back, I promise.”")
        choice("Lend the whole amount.") {
            money(-12); family(+6); trust(SIBLING, +15); trait(COMPASSIONATE, 2); trait(LOYAL, 2); flag("supported_family"); hint()
            outcome(60) { bonus(trustAtLeast(SIBLING, 55), 15); money(+8); result("{sibling} pays you back in full, months later, with a handwritten thank-you.") }
            outcome(40) { result("The money is never mentioned again, but the two of you are closer than before.") }
        }
        choice("Lend part of it, with a plan.") {
            money(-5); family(+3); trust(SIBLING, +6); trait(RESPONSIBLE, 2)
            result("A clear plan helps. {sibling} sticks to it, and the debt is cleared in a year.")
        }
        choice("Refuse. You can’t afford the risk.") {
            money(+1); family(-5); trust(SIBLING, -10); happiness(-2)
            result("You explain your reasons, but there’s a coldness in the phone call afterwards.")
        }
        choice("Help {sibling.him} find a better job instead.") {
            family(+5); trust(SIBLING, +10); energy(-5); trait(RESPONSIBLE, 2); reputation(+2)
            result("You spend a few weekends on résumés and mock interviews. {sibling} lands a better position.")
        }
    }

    scenario("fam_parent_ageing", FAMILY, LIVING_ROOM, 36..60, weight = 11) {
        text("Your parents are getting older, and living alone is getting harder. They’d never ask, but you can see the stairs, the shopping and the pills are becoming too much.")
        extra(has("ignored_parents"), "A quiet voice in your head reminds you of all the visits you promised and didn’t make.")
        choice("Move them into your home.") {
            family(+8); money(-5); energy(-8); happiness(+2); trait(RESPONSIBLE, 3); trait(LOYAL, 3); flag("family_protected")
            milestone("Brought your parents into your home")
            result("The house is busier, and louder, and fuller. Your children grow up with their grandparents around.")
        }
        choice("Pay for a good care home nearby and visit often.") {
            family(+3); money(-8); happiness(-1); trait(RESPONSIBLE, 2)
            result("They’re safe and well looked after, and you visit most weekends. Not perfect, but responsible.")
        }
        choice("Arrange regular help at their place.") {
            family(+4); money(-4); trait(RESPONSIBLE, 2); happiness(+1)
            result("A kind carer visits every day. Your parents stay in the house they love.")
        }
        choice("Keep your distance. You have your own life.") {
            family(-8); happiness(-4); trait(LOYAL, -2); flag("ignored_parents"); count("mistakes")
            result("You tell yourself that they’re independent. The phone calls get shorter.")
        }
    }

    scenario("fam_kid_school", FAMILY, LIVING_ROOM, 33..52, weight = 11) {
        requires(hasChildren)
        text("Your child’s teacher calls: they’re falling behind and have become withdrawn. At home, they shrug and say “nothing” whenever you ask.")
        choice("Set aside evenings to sit with them and help.") {
            family(+7); happiness(+3); energy(-8); career(-2); trait(RESPONSIBLE, 3); flag("family_first")
            result("It takes weeks, but they open up. They were struggling to read, and now they have a plan.")
        }
        choice("Hire a tutor.") {
            family(+2); money(-6)
            result("The grades improve, though the real problem takes longer to surface.")
        }
        choice("Tell them to try harder.") {
            family(-5); happiness(-3); trait(DISCIPLINED, 1)
            result("They go quiet. The grades go up a bit, and the smiles go down.")
        }
        choice("Ask the teacher what you can do together.") {
            family(+5); knowledge(+2); trait(RESPONSIBLE, 2); reputation(+1)
            result("The teacher has real ideas, and the two of you start working as a team.")
        }
    }

    scenario("fam_kid_dream", FAMILY, LIVING_ROOM, 38..58, weight = 9) {
        requires(hasChildren)
        text("Your child tells you they want to pursue a creative dream instead of the safe career you’d imagined. Their eyes are bright, and a little afraid.")
        choice("Support them completely.") {
            family(+7); happiness(+5); money(-3); trait(COMPASSIONATE, 2); flag("family_first")
            result("They throw themselves into it. Whatever happens, you’ll have been there at the start.")
        }
        choice("Push them towards the safe option.") {
            family(-6); happiness(-3); money(+1)
            result("They comply. Years later you notice they no longer share their ideas with you.")
        }
        choice("Suggest a plan that includes both.") {
            family(+4); trait(RESPONSIBLE, 2); knowledge(+1)
            result("A trial year, with a fallback and a clear goal. They agree, with a smile.")
        }
        choice("Tell them it’s their decision. You’ll accept either.") {
            family(+3); trait(COMPASSIONATE, 1); confidence(+1)
            result("They pause, surprised, then thank you for trusting them.")
        }
    }

    scenario("fam_empty_nest", FAMILY, NEW_HOME, 48..64, weight = 8) {
        requires(hasChildren)
        text("The last of your children moves out. The house is suddenly quiet, and the days feel longer than they used to.")
        choice("Take up a long-delayed hobby.") {
            happiness(+6); health(+3); knowledge(+2); confidence(+3)
            result("You rediscover someone you had put away: the person who loved painting, or running, or building.")
        }
        choice("Visit your children often.") {
            family(+6); happiness(+3); money(-2); trait(LOYAL, 1)
            result("You’re never quite as wanted as you were, but being there still matters.")
        }
        choice("Work even harder.") {
            career(+4); money(+4); happiness(-3); health(-3); family(-2)
            result("Work fills the silence, but it isn’t the same company.")
        }
        choice("Invite old friends over more often.") {
            friendship(+7); happiness(+4); money(-1)
            result("The kitchen is loud again, with stories older than your children.")
        }
    }

    scenario("fam_reunion", FAMILY, PARK, 28..65, weight = 8) {
        text("A big family reunion is planned. An old conflict between relatives is still unspoken, and several people are quietly hoping someone will break the ice.")
        choice("Go and make the first move to reconcile.") {
            family(+7); happiness(+4); reputation(+2); trait(COMPASSIONATE, 2); trait(RISK_TAKER, 1); flag("reconciled"); count("redemptions")
            hint()
            result("It’s awkward for ten minutes, then easier. Years of tension crack, and something warmer comes through.")
        }
        choice("Attend, but keep things polite.") {
            family(+2); happiness(+1)
            result("Smiles all round, nothing resolved. It’s fine.")
        }
        choice("Skip it. You’re too busy.") {
            family(-4); happiness(-2); energy(+2)
            result("Photos arrive later. You feel like a guest in your own family.")
        }
        choice("Organise a game day to lighten the mood.") {
            family(+5); happiness(+5); reputation(+3); trait(RESPONSIBLE, 1)
            result("A water-balloon fight dissolves a decade of tension. Nobody mentions it, and nobody has to.")
        }
    }

    scenario("fam_estranged_parent", FAMILY, LIVING_ROOM, 36..65, weight = 14, priority = 40, fromPast = true) {
        requires(has("ignored_parents"), lacks("reconciled"), atMost(Stat.FAMILY, 60))
        text("A letter arrives from your parent’s neighbour: “Your mother mentions you often, though she doesn’t say it to you.” The distance between you has grown over the years.")
        choice("Drop everything and visit.") {
            family(+10); happiness(+6); energy(-6); trait(COMPASSIONATE, 3); flag("reconciled"); count("redemptions")
            milestone("Mended things with your parents"); hint()
            result("The front door opens, and the years vanish in a long embrace. There’s so much to catch up on.")
        }
        choice("Write a long letter first.") {
            family(+5); happiness(+2); flag("reconciled"); count("redemptions")
            result("It takes you three drafts. The phone rings two days later. It’s your mother, and she’s crying.")
        }
        choice("Send money and a gift instead.") {
            family(-1); money(-4); happiness(-2)
            result("The gift is thanked politely. Neither of you says what you actually mean.")
        }
        choice("Put it off. There’s always next month.") {
            family(-5); happiness(-4); count("mistakes")
            result("Next month turns into next year. You can’t shake the feeling that you’re running out of time.")
        }
    }

    scenario("fam_new_baby", FAMILY, NEW_HOME, 26..40, weight = 12) {
        requires(status(RelationshipStatus.MARRIED, RelationshipStatus.DATING), noChildren)
        text("You and {partner} have been talking about whether to start a family. It’s a big step, with big costs, in time, money and freedom.")
        choice("Yes. You’re both ready.") {
            children(+1); family(+8); happiness(+7); money(-6); energy(-10); career(-2); flag("family_first")
            milestone("Welcomed your first child")
            trust(PARTNER, +10)
            result("Nights are short and days are long, and every minute is worth it.")
        }
        choice("Wait a few more years to build stability.") {
            money(+3); career(+3); trust(PARTNER, -2); trait(RESPONSIBLE, 2)
            result("You agree on a timeline. It’s a sensible plan, though {partner} looks wistful.")
        }
        choice("Decide that a child isn’t for you.") {
            happiness(+1); career(+2); money(+3); trust(PARTNER, +2); family(-1)
            result("You’re honest with each other. You’ll pour your energy into other things, and each other.")
        }
        choice("Consider adoption or fostering.") {
            children(+1); family(+7); happiness(+6); money(-5); trait(COMPASSIONATE, 4); energy(-8); count("kindness")
            milestone("Opened your home to a child who needed one")
            result("It’s a long, careful process. When the day comes, your home is ready, and so are you.")
        }
    }

    scenario("fam_hospital_vigil", FAMILY, HOSPITAL, 40..70, weight = 5) {
        requires(anyOf(has("close_to_grandparent"), atLeast(Stat.FAMILY, 70)))
        text("A close relative is in the hospital for a long recovery. Visiting means long drives, unpaid days off, and tired evenings. Nobody would blame you for keeping your distance.")
        choice("Be there every day.") {
            family(+8); energy(-12); career(-3); happiness(+2); trait(LOYAL, 3); flag("family_protected")
            result("The nurses know you by name. When your relative recovers, your hands are the first they reach for.")
        }
        choice("Visit on weekends and call daily.") {
            family(+4); energy(-5); trait(RESPONSIBLE, 2)
            result("A balanced plan. You’re present enough to matter.")
        }
        choice("Send flowers and updates.") {
            family(-2); happiness(-1)
            result("A card with your name on it isn’t the same as you.")
        }
        choice("Coordinate the family’s visiting schedule.") {
            family(+5); trait(RESPONSIBLE, 3); reputation(+3); energy(-4)
            result("Without you, it would have been chaos. Everyone is relieved to have someone steady.")
        }
    }
}
