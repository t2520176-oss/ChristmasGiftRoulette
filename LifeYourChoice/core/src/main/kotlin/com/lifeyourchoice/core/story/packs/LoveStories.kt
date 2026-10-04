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

/** Love: wholesome romance. Dating, trust, distance, marriage, communication and breakups. Never forced. */
val LoveStories = storyPack("love", "Relationships") {

    scenario("lov_first_crush", LOVE, HALLWAY, 15..18, weight = 9) {
        requires(status(RelationshipStatus.SINGLE))
        text("There’s someone in your year you can’t stop noticing: kind, funny, a little shy. {partner} is smiling at you from across the hall.")
        choice("Walk over and say hello.") {
            confidence(+5); happiness(+5); trait(RISK_TAKER, 2); trust(PARTNER, +15); relationship(RelationshipStatus.DATING); count("risk_taken")
            milestone("Had your first sweetheart")
            result("It’s awkward, and wonderful. By the end of the week, you’re walking home together.")
        }
        choice("Ask {friend} to help break the ice.") {
            trust(BEST_FRIEND, +3); trust(PARTNER, +6); confidence(+2); happiness(+3)
            result("{friend} makes a gentle introduction. You’re friends before anything else, which isn’t a bad start.")
        }
        choice("Keep your focus on school.") {
            discipline(+3); knowledge(+2); happiness(-2); count("risk_avoided")
            result("The crush fades into a quiet memory. Your grades benefit.")
        }
        choice("Write {partner.him} a thoughtful note.") {
            confidence(+3); happiness(+4); trust(PARTNER, +10); trait(COMPASSIONATE, 1)
            result("{partner} keeps the note. You’ll hear about it again, years from now.")
        }
    }

    scenario("lov_first_date", LOVE, COFFEE_SHOP, 17..23, weight = 9) {
        requires(status(RelationshipStatus.DATING))
        text("You’re planning a date with {partner}. Money is tight, you want to make it special, and nerves are doing somersaults in your stomach.")
        choice("A simple walk and a home-made picnic.") {
            happiness(+6); trust(PARTNER, +10); money(-1); confidence(+2); trait(RESPONSIBLE, 1)
            result("It’s relaxed and sweet. {partner} says it was the best first date, simply because it was real.")
        }
        choice("Borrow money and plan a fancy dinner.") {
            happiness(+4); trust(PARTNER, +5); money(-8); trait(IMPULSIVE, 2)
            result("It’s lovely, but every moment you’re counting. {partner} notices you’re anxious.")
        }
        choice("Suggest a shared activity like a class or sport.") {
            happiness(+5); trust(PARTNER, +8); health(+2); confidence(+3)
            result("You laugh and get competitive. It’s a great way to get to know someone.")
        }
        choice("Cancel. You’re too nervous.") {
            happiness(-4); confidence(-4); trust(PARTNER, -10)
            result("The excuses sound weaker the more you repeat them. You’ll have to apologise.")
        }
    }

    scenario("lov_meet_someone", LOVE, COFFEE_SHOP, 20..34, weight = 9) {
        requires(status(RelationshipStatus.SINGLE), lacks("closed_to_love"))
        text("In a coffee shop, you start talking to {partner}. It’s easy conversation: shared jokes and plans. {partner.He} writes a number on your cup and gives you a shy look.")
        choice("Call, and ask {partner.him} out.") {
            happiness(+6); confidence(+4); trait(RISK_TAKER, 1); trust(PARTNER, +15); relationship(RelationshipStatus.DATING)
            milestone("Met someone special")
            result("The first date turns into a second, and then into long evenings that feel too short.")
        }
        choice("Become friends first.") {
            friendship(+3); happiness(+3); trust(PARTNER, +8); trait(RESPONSIBLE, 1)
            result("No pressure, just coffee and conversation. It takes time, but it grows.")
        }
        choice("You’re too focused on work right now.") {
            career(+3); happiness(-2); count("risk_avoided"); flag("closed_to_love")
            result("You keep the number, and never use it. Someday you’ll think about that.")
        }
        choice("Ask {friend} for a second opinion first.") {
            trust(BEST_FRIEND, +3); trust(PARTNER, +6); knowledge(+1); relationship(RelationshipStatus.DATING)
            result("{friend} laughs and says: “Why are you even asking me?” You call.")
            then("lov_first_date")
        }
    }

    scenario("lov_trust", LOVE, APARTMENT, 20..40, weight = 8) {
        requires(status(RelationshipStatus.DATING, RelationshipStatus.MARRIED))
        text("You’ve been hiding a mistake from {partner}: a financial slip, a small lie that grew. Tonight {partner} asks you a simple question, and the truth is right there.")
        choice("Tell the full truth.") {
            trust(PARTNER, +10); confidence(+3); trait(RESPONSIBLE, 3); trait(DISHONEST, -2); happiness(+2)
            result("It’s scary. {partner} is quiet, then holds your hand. “Thank you for telling me.”")
        }
        choice("Tell half the truth.") {
            trust(PARTNER, -3); trait(DISHONEST, 3); happiness(-2)
            result("It seems to work, and leaves a shadow that you’ll carry for a while.")
        }
        choice("Lie. It’s easier.") {
            trust(PARTNER, -8); trait(DISHONEST, 6); happiness(-4); flag("trust_broken"); count("mistakes")
            result("The lie holds, but trust has a way of dying quietly from the inside.")
        }
        choice("Ask for a moment and bring it up tomorrow.") {
            trust(PARTNER, +4); trait(RESPONSIBLE, 1)
            result("You collect yourself, and tell the truth the next morning. A bit late, but still honest.")
        }
    }

    scenario("lov_long_distance", LOVE, AIRPORT, 22..36, weight = 11, title = "Long Distance") {
        requires(status(RelationshipStatus.DATING))
        text("{partner} tells you {partner.he} may have to move to another city for a dream career. {partner.He} asks, quietly: “Would you support me, even if it means being in a long-distance relationship?”")
        choice("Encourage {partner.him} to go. You will support {partner.him}.") {
            trust(PARTNER, +15); happiness(-3); confidence(+3); trait(LOYAL, 3); trait(COMPASSIONATE, 3); hint()
            outcome(55) { bonus(trustAtLeast(PARTNER, 65), 20); family(+3); result("The distance is hard, but love holds. Calls, visits and letters keep you close.") }
            outcome(45) { happiness(-3); result("The distance is harder than you imagined. You make it work, barely.") }
        }
        choice("Ask {partner.him} to stay.") {
            trust(PARTNER, -8); happiness(+1); trait(IMPULSIVE, 1)
            outcome(50) { result("{partner} stays, though there’s a flicker of sadness in the way {partner.he} says it’s fine.") }
            outcome(50) { trust(PARTNER, -8); happiness(-5); relationship(RelationshipStatus.SINGLE); result("{partner} goes anyway, and the relationship doesn’t survive the argument.") }
        }
        choice("Say you’re not sure.") {
            trust(PARTNER, -3); confidence(-2); happiness(-2)
            result("Honesty, but not much comfort. {partner} says that’s fair, then goes quiet.")
        }
        choice("Offer to move too.") {
            trust(PARTNER, +20); career(-5); money(-4); friendship(-4); trait(RISK_TAKER, 3); count("risk_taken")
            flag("moved_for_love"); milestone("Moved cities to build a life together")
            result("It’s a leap. A new city, a new job search, and the best company in the world.")
        }
    }

    scenario("lov_career_vs_love", LOVE, APARTMENT, 24..40, weight = 9) {
        requires(employed, status(RelationshipStatus.DATING, RelationshipStatus.MARRIED))
        text("Work has been consuming your evenings. {partner} sits across the table at a dinner you’ve been late to, and quietly asks: “Where do I fit in your life right now?”")
        choice("Cut back on work and make time.") {
            trust(PARTNER, +12); happiness(+5); career(-3); family(+4); trait(RESPONSIBLE, 2); count("overwork", -1)
            result("You block out evenings in your calendar. It takes effort, but you’re happier for it.")
        }
        choice("Explain it’s temporary and keep going.") {
            career(+4); trust(PARTNER, -8); happiness(-3); flag("worked_too_much"); count("overwork")
            result("“Temporary” has a way of turning into permanent. {partner} stops asking.")
        }
        choice("Propose a weekly date night, no phones.") {
            trust(PARTNER, +8); happiness(+4); discipline(+2); family(+2)
            result("It’s a small ritual with big results. Evenings feel like yours again.")
        }
        choice("Say you need more time to figure it out.") {
            trust(PARTNER, -4); happiness(-2); confidence(-2)
            result("It isn’t a good answer. It isn’t a bad one, either, but the question stays on the table.")
        }
    }

    scenario("lov_proposal", LOVE, NIGHT_CITY, 24..38, weight = 13, title = "Big Question") {
        requires(status(RelationshipStatus.DATING), trustAtLeast(PARTNER, 45))
        text("You and {partner} have been together for a long time. Friends have started to ask when the big step is coming, and you can feel the question hanging between you.")
        choice("Propose. You’re sure.") {
            trust(PARTNER, +15); happiness(+10); relationship(RelationshipStatus.MARRIED); money(-5); trait(RISK_TAKER, 1)
            milestone("Married the person you love"); hint()
            then("lov_wedding")
            result("{partner} says yes before you finish the question. Your hands are shaking.")
        }
        choice("Wait until you’re more financially secure.") {
            money(+3); trust(PARTNER, -4); trait(RESPONSIBLE, 2); happiness(-1)
            result("A sensible plan, though you can see {partner} is waiting.")
        }
        choice("Tell {partner.him} you’re not ready.") {
            happiness(-3); trust(PARTNER, -6); confidence(+1)
            result("It’s a hard conversation. Honest, though, and {partner} says: “I’d rather know.”")
        }
        choice("Be happy as you are. Marriage isn’t necessary.") {
            happiness(+3); trust(PARTNER, +3); trait(RISK_TAKER, 1)
            result("You both agree: no rush, no pressure, just commitment on your own terms.")
        }
    }

    scenario("lov_wedding", LOVE, WEDDING, 24..40, branch = true, months = 6, title = "Wedding") {
        text("It’s the big day. The planning is behind you. The guest list got long and the budget got short, and now you’re standing at the entrance with a heart going at double speed.")
        choice("Keep it small and meaningful.") {
            money(+3); happiness(+9); family(+5); trait(RESPONSIBLE, 2)
            result("Thirty people, one beautiful afternoon. Nobody remembers what the flowers cost.")
        }
        choice("Throw the party of the decade.") {
            happiness(+10); friendship(+6); money(-12); reputation(+3); trait(IMPULSIVE, 1)
            result("An unforgettable night, and credit card statements that match.")
        }
        choice("Elope with just a few witnesses.") {
            happiness(+7); money(+5); family(-3); trait(RISK_TAKER, 2)
            result("Spontaneous, romantic and slightly scandalous. Your parents are happy for you, and a bit stunned.")
        }
        choice("Combine a small ceremony with a trip.") {
            happiness(+9); money(-3); health(+2); flag("adventure")
            result("You say your vows at sunset and spend the next week exploring a new place together.")
        }
    }

    scenario("lov_communication", LOVE, APARTMENT, 26..55, weight = 10) {
        requires(status(RelationshipStatus.MARRIED))
        text("You and {partner} have had the same argument three times this month, about chores, money, time. Tonight it starts again, and you can both feel the tension rising.")
        choice("Stop. Listen first, and speak second.") {
            trust(PARTNER, +10); happiness(+4); family(+4); trait(RESPONSIBLE, 2); confidence(+2)
            result("Eventually {partner} stops talking and you realise you hadn’t actually understood the problem.")
        }
        choice("Leave the room to cool down.") {
            trust(PARTNER, -2); energy(+2); happiness(-1)
            result("A pause helps a little. The conversation still needs to happen.")
        }
        choice("Win the argument.") {
            trust(PARTNER, -10); happiness(-5); family(-4); trait(IMPULSIVE, 2)
            result("You’re technically right. The house is silent for two days.")
        }
        choice("Suggest talking with a counsellor.") {
            trust(PARTNER, +6); money(-3); knowledge(+2); family(+3); trait(RESPONSIBLE, 3)
            result("It’s awkward at first, then incredibly useful. You learn to argue less, and listen more.")
        }
    }

    scenario("lov_breakup", LOVE, COFFEE_SHOP, 20..38, weight = 8) {
        requires(status(RelationshipStatus.DATING), trustAtMost(PARTNER, 55))
        text("Lately, you and {partner} talk less and argue more. You still care, but you’re no longer sure you want the same things.")
        choice("Talk honestly about where things stand.") {
            trust(PARTNER, +6); trait(RESPONSIBLE, 3); confidence(+3)
            outcome(50) { bonus(trustAtLeast(PARTNER, 45), 15); happiness(+3); result("You both realise it’s worth fighting for, and you start again, differently.") }
            outcome(50) { relationship(RelationshipStatus.SINGLE); happiness(-4); result("You end it kindly, with tears and thanks. It hurts, but it’s clean.") }
        }
        choice("Break it off respectfully.") {
            relationship(RelationshipStatus.SINGLE); happiness(-5); confidence(+2); trait(RESPONSIBLE, 2); trust(PARTNER, +3)
            result("It’s painful. You tell the truth and say you’re grateful. {partner} says thank you for being honest.")
        }
        choice("Keep drifting. Avoid the conversation.") {
            happiness(-4); trust(PARTNER, -5); energy(-3); count("risk_avoided")
            result("The silence grows heavier. Eventually something will have to give.")
        }
        choice("Suggest a trial break and some time to think.") {
            happiness(-2); trust(PARTNER, 0); confidence(+1)
            result("You both take some space. Time brings clarity, though not necessarily relief.")
        }
    }

    scenario("lov_single_choice", LOVE, NIGHT_CITY, 28..48, weight = 8) {
        requires(status(RelationshipStatus.SINGLE), lacks("closed_to_love"))
        text("Your friends keep trying to set you up. You’re doing well on your own, with your career, your friends and your freedom. But sometimes the apartment gets quiet.")
        choice("Say yes to a blind date.") {
            happiness(+4); confidence(+2); trait(RISK_TAKER, 1)
            outcome(45) { bonus(atLeast(Stat.CONFIDENCE, 55), 15); relationship(RelationshipStatus.DATING); trust(PARTNER, +20); milestone("Found love later in life"); result("To everyone’s surprise, it clicks. You’re still laughing at the end of the night.") }
            outcome(55) { result("A pleasant evening, though nothing sparks. Your friends are disappointed; you’re fine.") }
        }
        choice("Be happily single. Invest in your own life.") {
            happiness(+3); career(+3); money(+3); friendship(+2); flag("closed_to_love"); trait(AMBITIOUS, 1)
            result("You’re content, busy and free. There’s no rule that says the story needs a certain ending.")
        }
        choice("Join a club or class to meet people naturally.") {
            happiness(+4); friendship(+5); knowledge(+2); confidence(+3)
            result("You pick up a new skill, make new friends, and the rest is up to fate.")
        }
        choice("Focus on friends and family.") {
            friendship(+5); family(+4); happiness(+3)
            result("Your life is full of people, just not in the way the movies show it.")
        }
    }

    scenario("lov_anniversary", LOVE, APARTMENT, 28..60, weight = 8) {
        requires(status(RelationshipStatus.MARRIED))
        text("You’ve just remembered, at 4:45 p.m., that today is your anniversary. A big meeting is at 5:30, and you have nothing planned.")
        choice("Cancel the meeting and make a surprise dinner.") {
            trust(PARTNER, +15); happiness(+7); career(-2); family(+4)
            result("The dinner is a bit of a mess. {partner} loves it, burnt edges and all.")
        }
        choice("Do the meeting and apologise afterwards.") {
            career(+3); trust(PARTNER, -10); happiness(-3)
            result("“It’s fine,” says {partner}. It isn’t, and you both know it.")
        }
        choice("Call {partner} and plan a special weekend.") {
            trust(PARTNER, +6); happiness(+4); money(-3)
            result("An honest apology with a plan attached. {partner} accepts, with a smile.")
        }
        choice("Send flowers to the office and book a table.") {
            trust(PARTNER, +10); money(-3); happiness(+4); trait(RESPONSIBLE, 1)
            result("Quick thinking saves the day. You make both the meeting and the dinner.")
        }
    }

    scenario("lov_midlife", LOVE, LIVING_ROOM, 40..58, weight = 8) {
        requires(status(RelationshipStatus.MARRIED), trustAtMost(PARTNER, 80))
        text("Years of routine have made you and {partner} feel more like housemates than partners. Neither of you says it, but both of you know.")
        choice("Plan a trip together, just the two of you.") {
            trust(PARTNER, +12); happiness(+7); money(-4); family(+3)
            result("Away from the dishes and the deadlines, you rediscover the person you married.")
        }
        choice("Try new hobbies, together or apart.") {
            happiness(+4); trust(PARTNER, +6); knowledge(+2); confidence(+3)
            result("A cooking class and a pottery evening later, you’re laughing again.")
        }
        choice("Carry on. It’s normal for relationships to settle.") {
            trust(PARTNER, -4); happiness(-3); family(-2)
            result("Quiet years, quietly drifting.")
        }
        choice("Ask {partner} what they miss, and really listen.") {
            trust(PARTNER, +14); happiness(+5); trait(RESPONSIBLE, 2); confidence(+2); family(+4)
            result("The answer is simple, and about attention. You fix what you can, starting that night.")
        }
    }
}
