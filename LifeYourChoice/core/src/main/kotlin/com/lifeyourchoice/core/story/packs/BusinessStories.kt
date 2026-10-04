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

/**
 * Business: start small, fail, try again, build something. Failure never ends the game.
 * Stages (GameState.businessStage): 1 startup, 2 running, 3 thriving.
 */
val BusinessStories = storyPack("business", "Business") {

    scenario("biz_idea", BUSINESS, SMALL_BUSINESS, 24..45, weight = 14) {
        requires(businessAtMost(0), lacks("started_business"), atLeast(Stat.KNOWLEDGE, 35))
        boost(traitAtLeast(AMBITIOUS, 12), 8)
        boost(traitAtLeast(RISK_TAKER, 12), 6)
        boost(has("early_entrepreneur"), 10)
        text("An idea has been following you around for months: a small business that solves a real problem for people you know. Sooner or later, you’ll have to either start it or let it go.")
        choice("Quit what you’re doing and start it now.") {
            money(-10); confidence(+4); energy(-8); trait(RISK_TAKER, 4); trait(AMBITIOUS, 3)
            flag("started_business", "took_business_risk"); count("risk_taken"); business(1); job(ENTREPRENEUR, 1)
            milestone("Started your first business"); schedule("biz_first_year", 1, 2); hint()
            result("You sign the lease on a tiny space and put your name on the door. Whatever happens next, it’s yours.")
        }
        choice("Start it as a side project while keeping your job.") {
            money(-4); energy(-10); discipline(+3); trait(RESPONSIBLE, 2); trait(RISK_TAKER, 1)
            flag("started_business", "side_hustle"); business(1); count("overwork")
            milestone("Started a business on the side"); schedule("biz_first_year", 1, 2)
            result("Your evenings become the shop’s opening hours. It’s tiring, but your paycheque stays safe.")
        }
        choice("Research the market for a year, then decide.") {
            knowledge(+5); discipline(+3); trait(RESPONSIBLE, 3); money(-1)
            flag("started_business", "well_prepared"); business(1); job(ENTREPRENEUR, 1)
            milestone("Started your first business"); schedule("biz_first_year", 2, 3)
            result("A year of careful research shows you what customers actually want. You launch with a plan.")
        }
        choice("Let it go. It’s too risky.") {
            happiness(-2); discipline(+1); count("risk_avoided"); trait(RISK_TAKER, -2)
            result("You pack the idea away. A year later, you see someone else’s version in a shop window.")
        }
    }

    scenario("biz_first_year", BUSINESS, SMALL_BUSINESS, 24..52, branch = true, months = 0, title = "The First Year") {
        requires(businessAtLeast(1), businessAtMost(1))
        text("Your business is a year old. The first customers have come, a few of them regulars, and the bills are real. You’re at a crossroads about how to spend your limited money and energy.")
        choice("Put your savings into marketing and growth.") {
            money(-8); trait(RISK_TAKER, 2); count("risk_taken")
            outcome(45) {
                bonus(atLeast(Stat.KNOWLEDGE, 60), 12); bonus(atLeast(Stat.CONFIDENCE, 60), 8); bonus(has("well_prepared"), 12)
                business(2); job(BUSINESS_OWNER, 2); flag("business_running"); money(+14); reputation(+6); confidence(+5)
                result("The gamble pays off. Orders double, and you hire your first employee.")
            }
            outcome(45) {
                bonus(atMost(Stat.DISCIPLINE, 40), 12); bonus(atMost(Stat.MONEY, 25), 10)
                business(0); noJob(); flag("business_failed"); money(-22); confidence(-18); happiness(-8); count("crises")
                then("biz_failed")
                result("The customers don’t come fast enough, and the money runs out first.")
            }
        }
        choice("Keep costs tiny and grow slowly.") {
            discipline(+3); trait(RESPONSIBLE, 3); trait(DISCIPLINED, 2)
            outcome(55) {
                bonus(atLeast(Stat.DISCIPLINE, 55), 18); bonus(has("saved_money"), 12)
                business(2); job(BUSINESS_OWNER, 2); flag("business_running"); money(+8); reputation(+5); confidence(+4)
                result("Slow and steady wins here. After a long, anxious year, the numbers turn positive.")
            }
            outcome(35) {
                bonus(atMost(Stat.CONFIDENCE, 40), 10)
                business(0); noJob(); flag("business_failed"); money(-18); confidence(-16); happiness(-7); count("crises")
                then("biz_failed")
                result("You’re careful, but the business never quite takes off. Eventually you have to close the doors.")
            }
        }
        choice("Partner with {friend}.") {
            trust(BEST_FRIEND, +5); trait(LOYAL, 2); trait(RISK_TAKER, 1)
            outcome(55) {
                bonus(trustAtLeast(BEST_FRIEND, 65), 20); bonus(atLeast(Stat.FRIENDSHIP, 60), 8)
                business(2); job(BUSINESS_OWNER, 2); flag("business_running"); money(+11); friendship(+5); trust(BEST_FRIEND, +10); reputation(+4)
                result("Two heads, twice the energy, half the loneliness. The business thrives, and so does the friendship.")
            }
            outcome(45) {
                bonus(trustAtMost(BEST_FRIEND, 45), 25)
                business(0); noJob(); flag("business_failed"); money(-20); confidence(-14); happiness(-8); trust(BEST_FRIEND, -10); count("crises")
                then("biz_failed")
                result("Disagreements pile up, then the money does. It ends in a quiet, awkward conversation.")
            }
        }
        choice("Keep your day job and grow it at night.") {
            energy(-12); health(-3); trait(DISCIPLINED, 2); count("overwork")
            outcome(50) {
                bonus(atLeast(Stat.DISCIPLINE, 60), 15); bonus(atLeast(Stat.HEALTH, 65), 8)
                business(2); job(BUSINESS_OWNER, 2); flag("business_running"); money(+9); reputation(+4); confidence(+4)
                result("Sleepless, but it works. You finally leave your day job, with a business that can stand on its own.")
            }
            outcome(50) {
                business(0); noJob(); flag("business_failed"); money(-15); confidence(-14); happiness(-6); health(-3); count("crises")
                then("biz_failed")
                result("You’re too stretched for either to work well. It’s a hard, quiet end.")
            }
        }
    }

    scenario("biz_failed", BUSINESS, SMALL_BUSINESS, 24..70, branch = true, months = 6, title = "Business Failed") {
        text("BUSINESS FAILED. The shutters are down. Debts, unsold stock and a hollow quiet where the noise used to be. People will ask what happened, and you aren’t sure what to say. What matters now is what you do next.")
        choice("Give up on entrepreneurship.") {
            happiness(-3); confidence(-3); money(+4); trait(RISK_TAKER, -3)
            flag("quit_entrepreneurship"); job(EMPLOYEE, 2); count("risk_avoided")
            result("You take a steady job. It’s safe, and slightly grey, but it pays on time.")
        }
        choice("Find a job and rebuild your savings.") {
            money(+6); discipline(+5); confidence(+3); trait(RESPONSIBLE, 3); trait(DISCIPLINED, 2)
            flag("saved_money"); job(EMPLOYEE, 2); count("redemptions"); schedule("biz_comeback", 3, 6); hint()
            result("You take a job and put every spare coin into savings. The pain softens as the balance grows.")
        }
        choice("Try another, smaller business.") {
            money(-4); confidence(+3); trait(RISK_TAKER, 2); trait(DISCIPLINED, 1)
            business(1); job(ENTREPRENEUR, 1); count("risk_taken"); count("redemptions"); schedule("biz_comeback", 2, 4); hint()
            result("You start again, smaller, wiser, with an old sketch of what went wrong pinned to the wall.")
        }
        choice("Ask an old friend for advice.") {
            confidence(+2); knowledge(+2)
            outcome(60) {
                bonus(trustAtLeast(BEST_FRIEND, 60), 40)
                trust(BEST_FRIEND, +10); knowledge(+4); confidence(+6); money(+3); job(EMPLOYEE, 2); count("redemptions"); schedule("biz_comeback", 3, 6)
                result("{friend} listens for an hour, then helps you find work, and draws up a real comeback plan. “You’re not finished,” {friend.he} says.")
            }
            outcome(40) {
                job(EMPLOYEE, 1); knowledge(+2)
                result("{friend} is kind but busy. You get a coffee and a few useful tips, and not much more.")
            }
        }
        choice("Call {friend}, whom you once stood up for.") {
            showIf(has("helped_bullied_friend"), trustAtLeast(BEST_FRIEND, 70))
            badge("Unlocked: you stood by {friend} years ago")
            trust(BEST_FRIEND, +10); money(+10); confidence(+10); knowledge(+3); count("redemptions"); flag("kindness_returned", "second_chance")
            business(1); job(ENTREPRENEUR, 1); schedule("biz_comeback", 2, 4)
            milestone("Your old friend helped you start again")
            result("“You were there for me when no one else was,” {friend} says, and pays for the new shop’s first six months. “Let’s do this properly.”")
        }
    }

    scenario("biz_comeback", BUSINESS, SMALL_BUSINESS, 26..70, branch = true, months = 12, priority = 55, fromPast = true, title = "Second Try") {
        requires(has("business_failed"), lacks("business_success"))
        text("Years after your business closed, you’re no longer the person who started it. You have savings, scars and lessons, and an idea that might be better than the first.")
        choice("Try again, wiser this time.") {
            money(-6); trait(RISK_TAKER, 2); count("risk_taken")
            outcome(55) {
                bonus(atLeast(Stat.KNOWLEDGE, 55), 10); bonus(atLeast(Stat.DISCIPLINE, 55), 12); bonus(has("saved_money"), 10); bonus(counterAtLeast("redemptions", 1), 8)
                business(3); job(BUSINESS_OWNER, 4); flag("business_success", "comeback", "second_chance"); count("redemptions")
                money(+22); confidence(+14); reputation(+8); happiness(+9)
                milestone("Rebuilt your business and made it thrive")
                result("This time, it works: customers, staff, a name people trust. You open the doors each morning knowing what failure taught you.")
            }
            outcome(45) {
                business(0); noJob(); money(-10); confidence(-6); happiness(-3); job(EMPLOYEE, 2)
                result("It doesn’t work, and this time you know what to do about it: you pick yourself up, take a job and keep going.")
            }
        }
        choice("Stay in your stable career.") {
            happiness(+3); money(+3); trait(RESPONSIBLE, 2)
            onlyIf(atLeast(Stat.MONEY, 50)) { flag("comeback"); note("You never rebuilt the business, but you rebuilt your life, and it’s a good one.") }
            result("You make peace with the past. It stays a story you tell.")
        }
        choice("Join a young startup as an employee and bring your experience.") {
            knowledge(+4); career(+6); reputation(+4); confidence(+4); promote(); flag("comeback")
            result("You’re the one who has seen it all, and been through the fire. The founders treat your advice like gold.")
        }
        choice("Mentor first-time entrepreneurs.") {
            reputation(+7); happiness(+6); trait(COMPASSIONATE, 3); flag("mentored", "comeback"); count("kindness")
            milestone("Mentored people starting their own businesses")
            result("Your worst year becomes the most valuable lesson you ever gave away.")
        }
    }

    scenario("biz_partner_choice", BUSINESS, MEETING, 26..52, weight = 10) {
        requires(businessAtLeast(1))
        text("Your business needs more hands, and more money. You’re deciding who to bring in as a partner, and that choice will shape everything.")
        choice("Bring in {friend}, who you trust completely.") {
            trust(BEST_FRIEND, +8); friendship(+3); trait(LOYAL, 3)
            outcome(55) { bonus(trustAtLeast(BEST_FRIEND, 65), 20); money(+8); reputation(+3); result("Your friend’s loyalty is exactly what the business needed.") }
            outcome(45) { money(-4); trust(BEST_FRIEND, -6); result("Skills didn’t match friendship. Painful conversations, but you work it out.") }
        }
        choice("Hire an experienced stranger.") {
            money(-3); knowledge(+4); career(+3)
            outcome(60) { bonus(atLeast(Stat.KNOWLEDGE, 55), 10); money(+9); result("The stranger’s experience saves you years. Trust builds slowly, but it builds.") }
            outcome(40) { money(-5); confidence(-3); result("Different values, different goals. The partnership ends within the year.") }
        }
        choice("Stay on your own.") {
            energy(-10); discipline(+3); trait(RESPONSIBLE, 2); money(+3)
            result("You keep full control, and full responsibility. Some nights, the loneliness is heavy.")
        }
        choice("Take an investor’s money, with conditions.") {
            money(+10); confidence(+2); reputation(+2); trait(RISK_TAKER, 1)
            result("The cash helps, and so do the investor’s contacts. Their opinions come with it.")
        }
    }

    scenario("biz_customer_problem", BUSINESS, SMALL_BUSINESS, 26..58, weight = 10) {
        requires(businessAtLeast(1))
        text("A loud, furious customer is standing at your counter. Your product had a flaw, and it’s ruined their weekend. Other customers are watching to see what you do.")
        choice("Apologise and refund them fully.") {
            money(-3); reputation(+6); trait(RESPONSIBLE, 2); trait(COMPASSIONATE, 1); confidence(+2)
            result("The customer leaves calmer, and later writes you a glowing review. People remember how you handled it.")
        }
        choice("Defend the product.") {
            reputation(-5); confidence(+1); money(+1); trait(IMPULSIVE, 1)
            result("You win the argument, and lose the customer, and some others who overheard.")
        }
        choice("Replace the product and offer a discount.") {
            money(-2); reputation(+3); trait(RESPONSIBLE, 2)
            result("A decent compromise. They grumble, but accept.")
        }
        choice("Fix the root cause, and write a clear policy.") {
            knowledge(+3); discipline(+3); money(-2); reputation(+4); trait(RESPONSIBLE, 3)
            result("You discover the flaw, fix it, and train your team. Next time it won’t happen.")
        }
    }

    scenario("biz_expansion", BUSINESS, MEETING, 30..58, weight = 12, title = "Time to Grow") {
        requires(businessAtLeast(2), businessAtMost(2))
        text("Your business is doing well, and a second location is available. It could double your reach, or double your problems. Your accountant looks nervous.")
        choice("Expand aggressively.") {
            money(-10); trait(RISK_TAKER, 3); trait(AMBITIOUS, 3); count("risk_taken"); flag("took_business_risk")
            outcome(50) {
                bonus(atLeast(Stat.DISCIPLINE, 55), 12); bonus(atLeast(Stat.KNOWLEDGE, 60), 10); bonus(atLeast(Stat.REPUTATION, 60), 10)
                business(3); job(BUSINESS_OWNER, 4); flag("business_success"); money(+22); reputation(+8); career(+8)
                milestone("Expanded your business successfully")
                result("The new location is busy within weeks. You’re running a real company now.")
            }
            outcome(50) { money(-12); confidence(-8); happiness(-5); energy(-10); result("Overstretched, you have to close the second location within a year. You keep the first, and the lesson.") }
        }
        choice("Grow in smaller steps.") {
            money(-4); discipline(+3); trait(RESPONSIBLE, 3)
            outcome(60) {
                bonus(atLeast(Stat.DISCIPLINE, 50), 15)
                business(3); job(BUSINESS_OWNER, 4); flag("business_success"); money(+14); reputation(+6); career(+5)
                milestone("Expanded your business successfully")
                result("Careful growth, one hire and one customer at a time. It takes longer, and holds up better.")
            }
            outcome(40) { money(+3); result("Steady, if unspectacular. The business holds its ground.") }
        }
        choice("Stay the size you are.") {
            happiness(+3); money(+3); energy(+5); count("risk_avoided")
            result("You love what you’ve built, and you’re not looking for more. There’s dignity in that.")
        }
        choice("Sell and cash out.") {
            money(+20); career(-5); happiness(-1); business(0); noJob(); count("risk_avoided"); flag("sold_business")
            result("A big cheque, a quiet Monday morning, and the strange feeling of having time.")
        }
    }

    scenario("biz_ethics", BUSINESS, MEETING, 28..58, weight = 9) {
        requires(businessAtLeast(1))
        text("A large contract could transform your business. A competitor offers a hint: a quiet bribe to the right official would clinch it. “It’s how things are done around here.”")
        choice("Pay it. The business needs the contract.") {
            money(+10); trait(DISHONEST, 9); reputation(-4); flag("took_shortcut", "trust_broken"); count("mistakes"); hint()
            result("You get the contract, and a new secret to keep.")
        }
        choice("Refuse and win the contract on merit, or not at all.") {
            reputation(+6); trait(RESPONSIBLE, 3); confidence(+4); flag("refused_unethical", "protected_reputation"); hint()
            outcome(45) { bonus(atLeast(Stat.REPUTATION, 60), 15); money(+10); result("The client respects your honesty. You get the contract.") }
            outcome(55) { money(-2); result("You lose the contract, but your name is clean. A few months later, a bigger client hears about it.") ; reputation(+3) }
        }
        choice("Report the request to the authorities.") {
            reputation(+4); trait(RESPONSIBLE, 3); confidence(+3); money(-2); flag("refused_unethical"); count("risk_taken")
            result("It’s a risk, but the officials are investigated. People in your industry quietly notice your integrity.")
        }
        choice("Ask your partner or mentor what they think.") {
            knowledge(+2); trait(RESPONSIBLE, 2); trust(MENTOR, +4)
            result("A calm, honest conversation settles it: you don’t pay, and you find another way to compete.")
        }
    }

    scenario("biz_key_employee", BUSINESS, SMALL_BUSINESS, 30..58, weight = 9) {
        requires(businessAtLeast(2))
        text("Your best employee asks for a meeting. They’ve been offered more money elsewhere. “I like it here,” they say, “but I have a family to support.”")
        choice("Match the offer, within reason.") {
            money(-5); reputation(+3); trait(RESPONSIBLE, 2); friendship(+2)
            result("They stay, and the whole team notices how you treat those who build the business.")
        }
        choice("Offer a profit-share instead.") {
            money(-2); trait(AMBITIOUS, 1); knowledge(+2); reputation(+5)
            result("It aligns everyone: if the business wins, so do they. It’s one of your best decisions.")
        }
        choice("Let them go.") {
            money(+2); reputation(-2); confidence(-2)
            result("The business runs, but the loss is felt in many small ways.")
        }
        choice("Ask what would make them stay.") {
            reputation(+4); knowledge(+2); trait(COMPASSIONATE, 1)
            result("It turns out money wasn’t the only thing. A few schedule changes and a title, and they’re happy.")
        }
    }

    scenario("biz_investor", BUSINESS, MEETING, 30..52, weight = 8) {
        requires(businessAtLeast(1), businessAtMost(2))
        text("An investor loves your business and offers a large sum for a big share of it. It would give you room to grow, and give someone else a say in everything.")
        choice("Accept the full offer.") {
            money(+16); trait(RISK_TAKER, 2); confidence(+3); reputation(+2); flag("took_business_risk"); count("risk_taken")
            outcome(55) { bonus(atLeast(Stat.KNOWLEDGE, 55), 10); money(+8); career(+6); result("Their money and connections open doors you couldn’t reach alone.") }
            outcome(45) { happiness(-3); confidence(-2); career(+2); result("Their priorities differ from yours. You win growth, but you lose some freedom.") }
        }
        choice("Negotiate a smaller stake.") {
            money(+8); knowledge(+3); confidence(+4); trait(RESPONSIBLE, 2)
            result("You walk away with funding and control. It takes patience, and a lawyer.")
        }
        choice("Decline and grow on your own terms.") {
            happiness(+2); discipline(+2); count("risk_avoided")
            result("It’s slower, but it stays yours.")
        }
        choice("Ask {mentor} for advice first.") {
            trust(MENTOR, +6); knowledge(+3); trait(RESPONSIBLE, 2)
            result("{mentor} reads the contract twice and spots the traps. You walk in prepared.")
        }
    }

    scenario("biz_slow_season", BUSINESS, SMALL_BUSINESS, 27..58, weight = 9) {
        requires(businessAtLeast(1))
        text("Sales fall off a cliff for a few months. Rent is due, wages are due, and your cash flow is a straight line heading down.")
        choice("Cut costs, including your own pay.") {
            money(-4); discipline(+4); trait(RESPONSIBLE, 3); reputation(+2); happiness(-3)
            result("It’s an uncomfortable winter. You protect your team and the business pulls through.")
        }
        choice("Borrow money to bridge the gap.") {
            money(+3); trait(RISK_TAKER, 1); confidence(-2); flag("in_debt")
            result("The loan buys you time, and a monthly payment that doesn’t sleep.")
        }
        choice("Pivot: try a new product or service.") {
            knowledge(+4); trait(RISK_TAKER, 2); confidence(+2); count("risk_taken")
            outcome(55) { bonus(atLeast(Stat.KNOWLEDGE, 55), 12); money(+8); result("The new offering sells. Sometimes a crisis shows you what customers really want.") }
            outcome(45) { money(-6); result("The pivot doesn’t land. You’ve spent your reserves, but you learned a lot.") }
        }
        choice("Ask {friend} and your regulars to spread the word.") {
            trust(BEST_FRIEND, +4); friendship(+3); reputation(+3); money(+3)
            result("Your community turns up. People remember who showed up for them, and they show up for you.")
        }
    }

    scenario("biz_sell_or_keep", BUSINESS, MEETING, 52..66, weight = 12, title = "A Life’s Work") {
        requires(businessAtLeast(2))
        text("A larger company offers to buy yours. It’s a very generous figure. You’re getting older, and your hands are tired, but the business is so much more than a number on a page.")
        choice("Sell, and enjoy what you’ve built.") {
            money(+20); happiness(+4); energy(+8); career(-4); business(0); noJob(); flag("sold_business")
            milestone("Sold the business you built")
            result("The signing day is emotional. The next morning, you wake up with no alarm for the first time in decades.")
        }
        choice("Pass it on to the next generation or key employees.") {
            reputation(+8); family(+5); happiness(+5); money(+5); trait(RESPONSIBLE, 3); trait(COMPASSIONATE, 2); business(0); noJob()
            flag("passed_on_business")
            milestone("Passed your business on to the people who helped build it")
            result("It’s a hard handover, but the right one. They keep the name, and the spirit.")
        }
        choice("Keep going, and stay involved.") {
            career(+3); happiness(+2); energy(-6); health(-2)
            result("You’re still in the shop at dawn. For you, it’s not work. It’s a way of life.")
        }
        choice("Step back and take on an advisory role.") {
            money(+8); happiness(+5); energy(+5); reputation(+4); trait(RESPONSIBLE, 1)
            result("A quieter life with the best bits left in. You watch your creation grow without you.")
        }
    }
}
