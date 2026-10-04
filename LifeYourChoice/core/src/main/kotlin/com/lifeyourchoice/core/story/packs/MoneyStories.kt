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

/** Money: saving, spending, debt, emergencies and investing. Educational, never gambling. */
val MoneyStories = storyPack("money", "Money") {

    scenario("mon_first_savings", MONEY, LIVING_ROOM, 14..19, weight = 11) {
        text("For your birthday you receive a surprisingly generous amount of money from relatives. You’ve never had this much to yourself before.")
        choice("Save most of it.") {
            money(+7); discipline(+4); trait(RESPONSIBLE, 3); trait(DISCIPLINED, 2); happiness(-1); flag("saved_money"); hint()
            result("The number in your account grows quietly. It feels good in a way spending doesn’t.")
        }
        choice("Spend it on something you’ve always wanted.") {
            money(-2); happiness(+8); trait(IMPULSIVE, 2)
            result("You have a great week. By the end of the month, you barely remember where the money went.")
        }
        choice("Invest in an online course or a tool for your hobby.") {
            money(-2); knowledge(+5); confidence(+3); trait(AMBITIOUS, 2)
            result("It’s not glamorous, but it opens a door that was shut before.")
        }
        choice("Give part of it to your family.") {
            family(+6); money(+2); trait(COMPASSIONATE, 2); trait(LOYAL, 1); flag("supported_family"); count("kindness")
            result("Your parents try to refuse, then hug you for a very long time.")
        }
    }

    scenario("mon_expensive_buy", MONEY, STREET, 16..32, weight = 10) {
        text("A brand-new gadget everyone is talking about has just launched. It’s a big chunk of what you have, and you could buy it today on an instalment plan.")
        choice("Buy it now with the instalment plan.") {
            happiness(+6); money(-9); trait(IMPULSIVE, 3); discipline(-2)
            result("It’s fantastic for about a month. The monthly payments continue for a lot longer.")
        }
        choice("Save up and buy it outright later.") {
            discipline(+4); money(-4); trait(DISCIPLINED, 3); happiness(+2); flag("saved_money")
            result("By the time you can afford it, a newer model is out, and it’s cheaper. You smile.")
        }
        choice("Buy a good second-hand one.") {
            money(-3); happiness(+4); knowledge(+1); trait(RESPONSIBLE, 2)
            result("It does 95% of what the new one does for half the price. You feel very clever.")
        }
        choice("Skip it. You really don’t need it.") {
            money(+2); discipline(+2); happiness(-1)
            result("A little pang, then it passes. Nobody remembers who owned what.")
        }
    }

    scenario("mon_credit_card", MONEY, APARTMENT, 20..34, weight = 10) {
        text("A bank offers you your first credit card with a generous limit and a 0% introductory rate. A pile of things you “need” suddenly looks very reachable.")
        choice("Use it for everyday needs and pay it off every month.") {
            money(+2); discipline(+3); knowledge(+3); trait(DISCIPLINED, 2); trait(RESPONSIBLE, 2)
            result("You build a credit history, and a good habit. Easy, if you stay honest with yourself.")
        }
        choice("Go on a shopping spree.") {
            happiness(+6); money(-12); discipline(-4); trait(IMPULSIVE, 4); count("mistakes")
            flag("in_debt")
            result("It’s thrilling until the statement arrives. Interest, it turns out, is a quiet, patient thing.")
        }
        choice("Decline. You don’t trust yourself with it yet.") {
            discipline(+2); trait(RESPONSIBLE, 1); count("risk_avoided")
            result("A cautious choice. You’ll have to build credit another way someday.")
        }
        choice("Read the terms and ask a knowledgeable friend before deciding.") {
            knowledge(+4); confidence(+2); trait(RESPONSIBLE, 2)
            result("The fine print had a few traps. You take a safer card with clear terms.")
        }
    }

    scenario("mon_emergency", MONEY, APARTMENT, 22..50, weight = 9, title = "Emergency Expense") {
        text("Without warning, a large, unavoidable expense lands in your lap: a broken car, a burst pipe, a dental bill. It has to be paid this month.")
        choice("Pay it from your savings.") {
            money(-8); trait(RESPONSIBLE, 2)
            onlyIf(has("saved_money")) { money(+5); note("Having savings put aside softens the blow. This is exactly what they were for."); happiness(+2) }
            onlyIf(lacks("saved_money")) { happiness(-4); note("You have no cushion, and the stress is heavy.") }
            result("The money is gone, but the problem is fixed.")
        }
        choice("Put it on a credit card.") {
            money(-4); happiness(-2); trait(IMPULSIVE, 1); flag("in_debt")
            result("It solves today’s problem and creates next year’s.")
        }
        choice("Ask family for help.") {
            money(-3); family(+2); reputation(-1); trait(RESPONSIBLE, 1)
            onlyIf(has("supported_family")) { family(+4); note("Because you’ve always helped them, your family helps you without hesitation.") }
            result("It’s a humbling call to make.")
        }
        choice("Find a cheaper fix, and learn how to do it yourself.") {
            money(-3); knowledge(+3); energy(-8); discipline(+2); trait(DISCIPLINED, 1)
            result("It’s messy, but you save a lot, and now you know a thing or two.")
        }
    }

    scenario("mon_investment_scheme", MONEY, COFFEE_SHOP, 23..55, weight = 9, title = "Investment Decision") {
        text("An acquaintance excitedly tells you about an investment opportunity that “can’t lose” and promises to double your money in six months. They’re already telling their friends.")
        choice("Put in a large amount. You don’t want to miss out.") {
            money(-14); trait(RISK_TAKER, 3); trait(IMPULSIVE, 3); count("risk_taken"); count("mistakes")
            outcome(80) { money(-6); confidence(-4); happiness(-3); result("It was a scam. If something sounds too good to be true, it almost always is. You lose most of the money.") }
            outcome(20) { bonus(atLeast(Stat.KNOWLEDGE, 70), 25); money(+10); result("You get out with a small profit, mostly by luck. You know you were very lucky.") }
        }
        choice("Research it carefully before deciding.") {
            knowledge(+4); discipline(+3); trait(RESPONSIBLE, 3); count("risk_avoided")
            result("The research shows glaring red flags. You tell your friends to stay away, and you keep your money.")
        }
        choice("Invest a small amount in something diversified and boring.") {
            money(+3); knowledge(+3); trait(RESPONSIBLE, 2); trait(DISCIPLINED, 2); flag("saved_money")
            result("No fireworks, just slow, steady growth. Compound interest begins its quiet work.")
        }
        choice("Politely refuse, and warn others.") {
            reputation(+2); trait(RESPONSIBLE, 2); trait(COMPASSIONATE, 1); count("risk_avoided")
            result("Not everyone listens. A few do, and they thank you later.")
        }
    }

    scenario("mon_raise_lifestyle", MONEY, APARTMENT, 24..40, weight = 9) {
        requires(employed)
        text("You get a raise. Suddenly a nicer apartment, a car and better meals seem within reach. Your friends joke about “finally living a bit.”")
        choice("Upgrade everything. You’ve earned it.") {
            happiness(+6); money(-7); trait(IMPULSIVE, 2)
            result("Life is more comfortable, and the new baseline gets expensive quickly.")
        }
        choice("Keep your lifestyle and save the difference.") {
            money(+8); discipline(+4); happiness(+1); trait(DISCIPLINED, 3); flag("saved_money"); hint()
            result("Your friends tease you a little. Your savings quietly don’t.")
        }
        choice("Pay off debts first.") {
            money(+5); discipline(+3); happiness(+3); trait(RESPONSIBLE, 3); unflag("in_debt")
            result("It feels as if you’ve put down a backpack you’d been carrying for years.")
        }
        choice("Share some of it with your family.") {
            family(+6); money(+1); happiness(+3); trait(COMPASSIONATE, 2); flag("supported_family"); count("kindness")
            result("Your parents are overwhelmed. “We’ll never forget this,” your mother says.")
        }
    }

    scenario("mon_buy_home", MONEY, NEW_HOME, 27..45, weight = 10, title = "A Place of Your Own") {
        requires(employed)
        text("You’ve been renting for years. A modest home comes on the market, and the numbers just about work. It would stretch you, but it would also be yours.")
        choice("Buy it now.") {
            money(-10); happiness(+7); family(+3); confidence(+3); trait(RISK_TAKER, 1); flag("owns_home"); milestone("Bought your first home")
            result("The day you get the keys, you sit on the floor of the empty living room and grin.")
        }
        choice("Keep renting and save for a bigger down payment.") {
            money(+6); discipline(+3); trait(DISCIPLINED, 2); flag("saved_money")
            result("You’re patient. Next year’s home may be better, and the savings keep growing.")
        }
        choice("Buy something smaller and cheaper.") {
            money(-5); happiness(+4); trait(RESPONSIBLE, 3); flag("owns_home")
            result("It isn’t your dream house, but it’s paid for, mostly, and it’s yours.")
        }
        choice("Wait for the market to “improve.”") {
            money(+2); count("risk_avoided"); happiness(-2)
            result("The market doesn’t seem to care about your timing.")
        }
    }

    scenario("mon_inheritance", MONEY, LIVING_ROOM, 35..60, weight = 6) {
        text("A distant relative passes away and leaves you a modest inheritance. {sibling} was also named, and they’re waiting to see what you’ll do.")
        choice("Invest it for the long term.") {
            money(+10); discipline(+3); trait(RESPONSIBLE, 3); flag("saved_money")
            result("You split it carefully. Years from now you’ll be glad you did.")
        }
        choice("Share it equally with {sibling}, and talk about it.") {
            money(+5); family(+6); trust(SIBLING, +15); trait(COMPASSIONATE, 2); trait(LOYAL, 2)
            result("A simple conversation turns a possible argument into a warm memory.")
        }
        choice("Take a long-dreamed-of trip.") {
            happiness(+9); money(+2); health(+2); trait(RISK_TAKER, 1); flag("adventure")
            result("You see places you only read about. It’s expensive, extravagant and unforgettable.")
        }
        choice("Pay off all debts and start fresh.") {
            money(+7); happiness(+6); trait(RESPONSIBLE, 3); unflag("in_debt")
            result("The weight lifts. For the first time in years you’re starting a month with zero owed.")
        }
    }

    scenario("mon_rainy_day", MONEY, APARTMENT, 28..55, weight = 8) {
        requires(employed)
        text("A colleague has just been laid off with no savings. It sets you thinking about how you’d handle a sudden storm of your own.")
        choice("Build a proper emergency fund.") {
            money(+7); discipline(+4); happiness(+2); trait(DISCIPLINED, 2); flag("saved_money")
            result("Three months of expenses, set aside. It feels like having a seat belt on.")
        }
        choice("Keep money working in investments instead.") {
            money(+5); knowledge(+2); trait(RISK_TAKER, 1)
            result("A good return, but not much you can touch quickly. You’ll see how it holds up.")
        }
        choice("Don’t worry. Things will be fine.") {
            happiness(+2); discipline(-2)
            result("The sun comes out and you forget about it. Hopefully it stays out.")
        }
        choice("Talk it over with your partner or a friend to plan.") {
            family(+3); knowledge(+2); money(+3); trait(RESPONSIBLE, 2)
            result("A shared plan feels lighter, and much more likely to happen.")
        }
    }

    scenario("mon_charity", MONEY, STREET, 28..65, weight = 7) {
        text("A local charity is raising funds for a children’s reading programme. They’re short of both money and volunteers. You have a little of one, and not much of the other.")
        choice("Donate a meaningful amount.") {
            money(-5); happiness(+6); reputation(+3); trait(COMPASSIONATE, 3); count("kindness")
            result("You give without fuss. Knowing a child will be reading because of it brings a quiet satisfaction.")
        }
        choice("Volunteer your time on weekends.") {
            energy(-6); happiness(+6); reputation(+5); friendship(+3); trait(COMPASSIONATE, 3); count("kindness")
            result("You meet people you’d never otherwise meet. The kids are brilliant.")
        }
        choice("Politely decline this time.") {
            money(+1)
            result("You keep your weekends. It’s fair, but you feel a flicker of guilt.")
        }
        choice("Help organise a fundraiser with your friends.") {
            friendship(+5); reputation(+6); happiness(+5); energy(-8); trait(RESPONSIBLE, 2); count("kindness")
            result("It raises far more than you expected, and you’re hooked on the feeling.")
        }
    }

    scenario("mon_retirement_plan", MONEY, LIVING_ROOM, 45..62, weight = 11, title = "Planning Ahead") {
        text("A birthday ending in a zero has you thinking about the long term. You imagine slower mornings and travel, and wonder if your savings will be enough.")
        extra(has("saved_money"), "Looking at your accounts, the savings you’ve put away over the years are a comforting sight.")
        choice("Raise your savings and cut unnecessary spending.") {
            money(+8); discipline(+3); happiness(-1); trait(DISCIPLINED, 2); flag("saved_money")
            result("You tighten your belt, and your future self quietly smiles.")
        }
        choice("Keep going as you are. It’ll work out.") {
            happiness(+2); money(-1); count("risk_avoided")
            result("It’s a comfortable denial. Retirement may require some adjustments.")
        }
        choice("Plan to work longer, part-time, into your late sixties.") {
            money(+5); career(+2); energy(-3); trait(RESPONSIBLE, 2)
            result("A smoother glide to retirement, with income and purpose.")
        }
        choice("Meet a financial adviser.") {
            knowledge(+4); money(+4); trait(RESPONSIBLE, 3)
            result("An hour with an expert reveals gaps you hadn’t noticed, and fixes you can start this week.")
        }
    }

    scenario("mon_scam_call", MONEY, APARTMENT, 22..70, weight = 7) {
        text("A caller claims to be from your bank and says your account is “at risk.” They sound urgent, professional, and slightly frightened for you. They want your password to “protect” it.")
        choice("Hang up and call the bank directly.") {
            knowledge(+3); money(+1); trait(RESPONSIBLE, 3); confidence(+2)
            result("The bank confirms it was a scam. You feel a rush of relief and a trace of pride.")
        }
        choice("Give them the details. They sound official.") {
            money(-12); confidence(-5); happiness(-5); trait(IMPULSIVE, 2)
            result("Within an hour, the account is drained. The bank helps recover some, but it’s a painful lesson.")
        }
        choice("Ask for their name and verify them first.") {
            knowledge(+2); trait(RESPONSIBLE, 2); discipline(+1)
            result("They hang up fast. That tells you everything you need to know.")
        }
        choice("Warn friends and family about the scam.") {
            reputation(+2); family(+2); trait(COMPASSIONATE, 2); count("kindness")
            result("Your mother had received a similar call. Thanks to you, she knew what to do.")
        }
    }
}
