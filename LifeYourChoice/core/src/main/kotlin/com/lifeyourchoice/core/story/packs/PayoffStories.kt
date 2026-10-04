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
 * Delayed consequences: decisions from years ago come back. They are shown with a
 * "from your past" tag so the player recognises that an old choice mattered.
 */
val PayoffStories = storyPack("payoffs", "Echoes of Your Choices") {

    scenario("pay_underdog_returns", MAJOR, MEETING, 24..70, priority = 60, fromPast = true, title = "A Familiar Face") {
        requires(has("helped_underdog"))
        text("At a meeting, a familiar voice calls your name. It’s {underdog}, the quiet classmate you sat with at lunch all those years ago. {underdog.He} is now running a company of {underdog.his} own, and {underdog.he} hasn’t forgotten.")
        extra(careerIs(DOCTOR, ENGINEER, TECH), "“You were the only person who talked to me that year,” {underdog.he} says. “It changed how I saw myself.”")
        extra(businessAtLeast(1), "{underdog.He} has heard about your business, and is smiling.")
        choice("Hear out {underdog}’s offer to work together.") {
            trust(UNDERDOG, +20); money(+14); career(+8); reputation(+6); confidence(+6); happiness(+6)
            flag("kindness_returned"); milestone("A classmate you helped years ago opened a door for you")
            result("“I’ve been looking for a partner I could trust,” {underdog.he} says. “I remembered you.” The project that follows becomes one of the biggest of your life.")
        }
        choice("Catch up over dinner.") {
            friendship(+6); happiness(+7); trust(UNDERDOG, +20); reputation(+3); flag("kindness_returned")
            result("You talk for hours. {underdog} tells you how much your small gesture meant, and you realise it was never small.")
        }
        choice("Introduce {underdog.him} to your network.") {
            reputation(+7); trait(COMPASSIONATE, 2); trust(UNDERDOG, +15); career(+3); flag("kindness_returned"); count("kindness")
            result("Doors open for {underdog.him}, and, in the process, for you. Good people tend to find each other.")
        }
        choice("Say you were glad to help, and leave it there.") {
            happiness(+3); trait(COMPASSIONATE, 1); reputation(+2); flag("kindness_returned")
            result("A handshake and a smile. Sometimes the best reward is knowing you did something good.")
        }
    }

    scenario("pay_underdog_mocked", MAJOR, MEETING, 26..65, priority = 55, fromPast = true, title = "Old Echoes") {
        requires(has("mocked_underdog"), lacks("apologised_underdog"))
        text("In a meeting, you’re introduced to the new head of a partner company: {underdog}. Your old classmate. The one you laughed at. {underdog.He} recognises you instantly, and the room feels colder.")
        choice("Apologise sincerely, in private.") {
            trait(COMPASSIONATE, 3); trait(DISHONEST, -2); reputation(+3); confidence(+3); trust(UNDERDOG, +20)
            flag("apologised_underdog"); count("redemptions"); milestone("Apologised to someone you had hurt in school"); hint()
            result("{underdog} is quiet for a moment, then nods. “Thank you. It means more than you’d think.” The meeting goes well.")
        }
        choice("Pretend not to remember.") {
            trait(DISHONEST, 2); confidence(-3); trust(UNDERDOG, -5); career(-2)
            result("{underdog} lets you pretend. It’s worse than if {underdog.he} hadn’t.")
        }
        choice("Handle the meeting professionally and say nothing.") {
            career(+1); trait(RESPONSIBLE, 1); trust(UNDERDOG, 0)
            result("The partnership goes ahead, a bit stiffly. Some conversations are never had.")
        }
        choice("Excuse yourself and let a colleague handle it.") {
            career(-3); confidence(-2); happiness(-2)
            result("You avoid it. The deal is done, and the discomfort stays.")
        }
    }

    scenario("pay_friend_helps_crisis", MAJOR, LIVING_ROOM, 25..75, priority = 50, fromPast = true, title = "A True Friend") {
        requires(trustAtLeast(BEST_FRIEND, 65), anyOf(has("lost_job"), has("business_failed"), counterAtLeast("crises", 1)), lacks("friend_helped"))
        text("There’s a knock at the door. It’s {friend}, with a bag of groceries, a laptop and a determined expression. “I heard what’s going on,” {friend.he} says. “You’re not doing this alone.”")
        extra(has("helped_bullied_friend"), "You think about the day you stood beside {friend.him} in a school hallway, a very long time ago.")
        extra(has("housed_friend"), "{friend.He} hasn’t forgotten the weeks on your couch.")
        choice("Accept the help gratefully.") {
            money(+8); happiness(+8); confidence(+8); trust(BEST_FRIEND, +10); friendship(+6)
            flag("friend_helped", "kindness_returned"); milestone("A loyal friend stood by you in your darkest year")
            result("It’s the first time in weeks you’ve eaten a proper meal. {friend} spends the weekend helping you get your life back in order.")
        }
        choice("Accept, but insist on paying it back.") {
            money(+5); happiness(+5); confidence(+5); trait(RESPONSIBLE, 3); trust(BEST_FRIEND, +8)
            flag("friend_helped"); count("redemptions")
            result("{friend} smiles, “Pay me in coffee.” You do, for years.")
        }
        choice("Say you’re fine. Too proud to accept.") {
            happiness(-3); trust(BEST_FRIEND, -6); confidence(-2)
            flag("friend_helped")
            result("{friend} doesn’t push, and leaves the bag on the table. You eat from it that night anyway.")
        }
        choice("Ask {friend.him} for advice, not money.") {
            knowledge(+3); confidence(+6); trust(BEST_FRIEND, +10)
            flag("friend_helped"); count("redemptions")
            result("{friend} listens, asks the right questions and helps you see a path you hadn’t noticed.")
        }
    }

    scenario("pay_cheat_echo", MAJOR, OFFICE, 27..52, priority = 50, fromPast = true, title = "Skeletons") {
        requires(has("cheated_exam"), employed)
        text("At a team dinner, a colleague turns out to have been in your class years ago. Over dessert, your colleague laughs: “Didn’t you copy the answers in that famous exam?” The table goes quiet. Your boss is listening.")
        choice("Own up honestly.") {
            reputation(-1); confidence(+5); trait(DISHONEST, -4); trait(RESPONSIBLE, 3); count("redemptions")
            result("“I did,” you say. “It’s one of my biggest regrets.” The silence breaks, and people respect you more than before.")
        }
        choice("Deny it.") {
            trait(DISHONEST, 5); reputation(-5); confidence(-3); flag("trust_broken")
            result("Nobody can prove it, and nobody completely believes you. The dinner ends early.")
        }
        choice("Laugh it off as a joke.") {
            reputation(-2); trait(DISHONEST, 2); confidence(-1)
            result("You joke your way out of it. The laughter is thinner than you’d like.")
        }
        choice("Pull your colleague aside and clear the air afterwards.") {
            reputation(+1); trait(RESPONSIBLE, 2); confidence(+2)
            result("You talk it through in the corridor. It turns out they were just teasing, and a bit nervous themselves.")
        }
    }

    scenario("pay_coworker_returns", CAREER, MEETING, 28..62, priority = 50, fromPast = true, title = "A Favour Returned") {
        requires(has("helped_coworker"), trustAtLeast(COWORKER, 55), lacks("coworker_returned"))
        text("You get a call from {coworker}, who has moved up in another company. “I’ve just been put in charge of hiring,” {coworker.he} says. “I remember who stayed late to help me when it counted.”")
        choice("Accept the offer for a senior role.") {
            promote(); money(+9); career(+8); reputation(+5); confidence(+5); trust(COWORKER, +10)
            flag("coworker_returned", "kindness_returned"); milestone("A coworker you once helped offered you a better job")
            result("A better title, a better salary and a boss who trusts you completely.")
        }
        choice("Recommend a friend of yours instead.") {
            trust(BEST_FRIEND, +8); reputation(+5); trait(COMPASSIONATE, 2); flag("coworker_returned", "kindness_returned"); count("kindness")
            result("{coworker} agrees. Your friend gets the job, and you get a reputation for generosity.")
        }
        choice("Thank {coworker.him}, but stay where you are.") {
            happiness(+3); reputation(+2); trust(COWORKER, +5); trait(LOYAL, 1); flag("coworker_returned", "kindness_returned")
            result("It’s nice to be asked. You decide you’re exactly where you want to be.")
        }
        choice("Use the offer to negotiate with your current boss.") {
            money(+6); career(+3); trust(BOSS, -4); trait(AMBITIOUS, 1); flag("coworker_returned", "kindness_returned")
            result("It works. Everyone gets a better deal, and a slightly awkward Monday.")
        }
    }

    scenario("pay_refused_unethical", CAREER, OFFICE, 32..62, priority = 50, fromPast = true, title = "Vindicated") {
        requires(has("refused_unethical"), employed)
        text("Years after you refused to fudge the numbers, an audit uncovers exactly the sort of problem you warned about. People are being let go. Your name comes up in the investigation, in a good way.")
        choice("Accept the offer to lead the clean-up.") {
            career(+8); reputation(+9); money(+7); promote(); energy(-10); trait(RESPONSIBLE, 3)
            flag("kindness_returned"); milestone("Your integrity was rewarded years later")
            result("The company needs someone it can trust, and the board looked back through the records to find you.")
        }
        choice("Offer your help, but stay out of the spotlight.") {
            reputation(+5); trait(RESPONSIBLE, 2); confidence(+4)
            result("You help quietly, and the company sets things right. Your colleagues know who was honest.")
        }
        choice("Use the situation to negotiate a better position.") {
            money(+5); career(+4); reputation(-1); trait(AMBITIOUS, 2)
            result("It works, mostly, though you lose a little goodwill by asking.")
        }
        choice("Leave for a company that values honesty from the start.") {
            career(+3); happiness(+4); reputation(+3); money(+2)
            result("A headhunter finds you before you’ve even updated your résumé.")
        }
    }

    scenario("pay_family_returns", FAMILY, LIVING_ROOM, 32..70, priority = 45, fromPast = true, title = "Family Returns the Favour") {
        requires(has("supported_family"), anyOf(has("lost_job"), has("business_failed"), counterAtLeast("crises", 1), atMost(Stat.MONEY, 35)), lacks("family_returned"))
        text("You’re having a hard time, and you’d tried to keep it to yourself. But your family always knows. Your mother pushes an envelope across the table. “We didn’t forget everything you did for us. Please.”")
        choice("Accept it, with tears.") {
            money(+10); family(+8); happiness(+7); confidence(+4); flag("family_returned", "kindness_returned")
            milestone("Your family supported you when you needed it")
            result("The envelope is smaller than the weight of the gesture. You’ve never felt so loved.")
        }
        choice("Accept it as a loan, and repay it.") {
            money(+7); family(+5); trait(RESPONSIBLE, 3); flag("family_returned")
            result("Your family insists it’s a gift. You repay it anyway, with interest, in flowers and dinners.")
        }
        choice("Refuse, and thank them.") {
            family(+4); confidence(+1); happiness(+1); flag("family_returned")
            result("They put it in your coat pocket as you leave. You find it on the bus.")
        }
        choice("Ask them for advice instead.") {
            family(+6); knowledge(+2); happiness(+3); flag("family_returned")
            result("Your father’s advice is simple, and surprisingly good. You’ve never really asked before.")
        }
    }

    scenario("pay_become_mentor", CAREER, OFFICE, 36..62, priority = 45, fromPast = true, title = "Your Turn") {
        requires(has("has_mentor"), employed, lacks("mentored"))
        text("A young colleague catches you in the corridor, a bit nervous. “I’ve been watching how you handle things,” they say. “Could you teach me? Just a little?” You remember someone once said exactly the same words to {mentor}.")
        choice("Say yes, and make it a real commitment.") {
            reputation(+6); happiness(+7); trait(COMPASSIONATE, 3); energy(-5); trust(MENTOR, +10)
            flag("mentored", "kindness_returned"); count("kindness"); milestone("Became the mentor you once needed")
            result("Weekly coffees, hard truths and kind encouragement. In a few years, they’ll be mentoring someone else.")
        }
        choice("Offer a few tips whenever you can.") {
            reputation(+3); happiness(+3); trait(COMPASSIONATE, 1)
            result("It’s a small contribution that may mean more than you think.")
        }
        choice("Say you’re too busy.") {
            happiness(-2); reputation(-1)
            result("They nod, thank you politely, and walk away.")
        }
        choice("Start a mentoring programme in the company.") {
            reputation(+9); career(+6); happiness(+6); energy(-8); trait(RESPONSIBLE, 3); flag("mentored", "kindness_returned"); count("kindness")
            milestone("Founded a mentoring programme")
            result("What starts as a chat becomes a company-wide programme. Dozens of people benefit.")
        }
    }

    scenario("pay_bullied_friend", FRIENDSHIP, WEDDING, 28..60, priority = 45, fromPast = true, title = "Where It Began") {
        requires(has("helped_bullied_friend"), trustAtLeast(BEST_FRIEND, 55), lacks("godparent"))
        text("{friend} calls with news. “I wanted you to be the first to know,” {friend.he} says. “I’m having a baby. And I want you to be {friend.his} godparent. You were there when I wasn’t brave enough to be there for myself.”")
        choice("Accept with joy.") {
            friendship(+8); happiness(+9); family(+4); trust(BEST_FRIEND, +10); flag("godparent", "kindness_returned")
            milestone("Became godparent to your best friend’s child")
            result("You cry. You laugh. You promise to be there, always. It’s a promise you won’t break.")
        }
        choice("Accept, and ask {friend.him} how {friend.he} is.") {
            friendship(+6); happiness(+6); trust(BEST_FRIEND, +10); trait(COMPASSIONATE, 1); flag("godparent", "kindness_returned")
            result("{friend} tells you things {friend.he} has never said out loud, about that year, and what your friendship has meant.")
        }
        choice("Say you’d be honoured, but that you’re worried you’ll be too busy.") {
            friendship(+3); happiness(+2); trust(BEST_FRIEND, +2); flag("godparent")
            result("{friend} understands, and says you only have to show up. You will.")
        }
        choice("Decline politely.") {
            friendship(-6); trust(BEST_FRIEND, -8); happiness(-3); flag("godparent")
            result("It’s an awkward silence. You aren’t sure why you said it, and neither is {friend}.")
        }
    }

    scenario("pay_rival_returns", CAREER, MEETING, 28..60, priority = 45, fromPast = true, title = "Old Rivals") {
        requires(has("allied_rival"), trustAtLeast(RIVAL, 45), lacks("rival_returned"))
        text("In a new deal, the person across the table is {rival}, your old rival from school, the one you ended up teaming up with. {rival.He} grins. “Looks like we meet again. This time, I’d rather be on your side.”")
        choice("Propose a partnership.") {
            money(+10); career(+6); reputation(+6); trust(RIVAL, +15); flag("rival_returned", "kindness_returned")
            milestone("An old rival became a lasting ally")
            result("A rivalry that turned into a partnership. Together, you’re much better than either of you apart.")
        }
        choice("Compete fairly, with mutual respect.") {
            career(+4); reputation(+4); trait(RESPONSIBLE, 2); trust(RIVAL, +8); flag("rival_returned")
            result("You each push the other to be better, and you both win in different ways.")
        }
        choice("Share advice over lunch.") {
            happiness(+5); friendship(+4); trust(RIVAL, +10); flag("rival_returned")
            result("You laugh about how seriously you took that science fair.")
        }
        choice("Keep the relationship formal.") {
            career(+1); trust(RIVAL, -2); flag("rival_returned")
            result("Polite, and a little distant. You’ve no regrets, but you also don’t get anywhere new.")
        }
    }

    scenario("pay_sabotage_echo", MAJOR, MEETING, 28..60, priority = 45, fromPast = true, title = "The Past Calls") {
        requires(has("sabotaged_rival"), lacks("apologised_rival"))
        text("At an industry dinner, {rival} walks up to you with a polite, tight smile. “I always wondered what happened to my science project,” {rival.he} says. “I figured it out, years later. I just wanted you to know I knew.”")
        choice("Admit it and apologise.") {
            reputation(+2); confidence(+4); trait(DISHONEST, -3); trait(COMPASSIONATE, 2); trust(RIVAL, +25)
            flag("apologised_rival"); count("redemptions"); milestone("Owned up to a mistake from your school days")
            result("{rival} is silent for a long moment, then nods. “Thank you. That took courage.” It isn’t forgiveness, quite, but it’s a start.")
        }
        choice("Deny it.") {
            trait(DISHONEST, 4); reputation(-4); trust(RIVAL, -15); flag("trust_broken")
            result("{rival} doesn’t argue. {rival.He} tells a few people. You can feel the temperature drop.")
        }
        choice("Say it was a long time ago.") {
            trait(DISHONEST, 1); reputation(-2); trust(RIVAL, -5)
            result("“It was,” {rival} says, “and I’ve still carried it.”")
        }
        choice("Offer to make it up to {rival.him}, in practical ways.") {
            reputation(+3); trait(RESPONSIBLE, 3); trust(RIVAL, +15); money(-3); flag("apologised_rival"); count("redemptions")
            result("A referral, an introduction. Actions speak, and {rival} notices.")
        }
    }

    scenario("pay_hard_work", CAREER, MEETING, 30..56, priority = 40, fromPast = true, title = "Years of Discipline") {
        requires(atLeast(Stat.DISCIPLINE, 70), atLeast(Stat.KNOWLEDGE, 62), employed, lacks("headhunted"))
        text("A call comes from a respected firm. They’ve been following your work for years: your steadiness, your results and your reputation for quietly getting things done. They’d like to talk about a leadership role.")
        choice("Take it.") {
            promote(); money(+9); career(+8); reputation(+5); confidence(+5); flag("headhunted"); milestone("Headhunted for a leadership role")
            result("Years of small, disciplined choices add up to one big opportunity. You step into the role with calm confidence.")
        }
        choice("Use the offer to negotiate better terms at your current job.") {
            money(+6); career(+4); trust(BOSS, -3); flag("headhunted")
            result("Your boss is surprised and impressed. You stay, with a raise and a new project.")
        }
        choice("Decline. You’re happy where you are.") {
            happiness(+4); trait(LOYAL, 1); flag("headhunted")
            result("They’re gracious. “If you ever change your mind,” they say, “we’ll be here.”")
        }
        choice("Ask for a trial project first.") {
            knowledge(+3); career(+3); trait(RESPONSIBLE, 2); flag("headhunted")
            result("A trial project goes well, and the offer becomes a standing invitation.")
        }
    }

    scenario("pay_savings_opportunity", MONEY, NEW_HOME, 30..58, priority = 40, fromPast = true, title = "Ready When It Counts") {
        requires(has("saved_money"), atLeast(Stat.MONEY, 45), lacks("used_savings_chance"))
        text("A chance appears that most people can’t take: a small property for sale well below market value, or a share in a friend’s promising venture. It needs ready money, and you’ve spent years building exactly that.")
        choice("Take the chance with a portion of your savings.") {
            money(+8); confidence(+5); trait(RISK_TAKER, 1); flag("used_savings_chance"); count("risk_taken")
            outcome(65) { bonus(atLeast(Stat.KNOWLEDGE, 55), 10); money(+8); reputation(+3); result("It pays off nicely. Being prepared turned a risk into a measured bet.") }
            outcome(35) { money(-5); result("It doesn’t pan out, but you can absorb the loss and keep going. That’s what savings are for.") }
        }
        choice("Put the money in something safe.") {
            money(+4); discipline(+2); flag("used_savings_chance"); count("risk_avoided")
            result("Slow and steady. You’ll never regret being careful.")
        }
        choice("Share the opportunity with friends and family.") {
            friendship(+4); family(+4); reputation(+4); trait(COMPASSIONATE, 2); flag("used_savings_chance"); count("kindness")
            result("A community effort, with everyone benefitting. It’s a good feeling.")
        }
        choice("Keep the money for the future.") {
            money(+2); happiness(+1); flag("used_savings_chance")
            result("Nothing to regret. The money is there if you need it.")
        }
    }
}
