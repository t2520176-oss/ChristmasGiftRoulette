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

/** Friendship: loyalty, jealousy, betrayal, old friends and the people who show up. */
val FriendshipStories = storyPack("friendship", "Friendship") {

    scenario("fri_cover_for_me", FRIENDSHIP, HALLWAY, 15..22, weight = 10) {
        text("{friend} is in trouble: late for a deadline and needs you to tell a teacher {friend.he} was with you, helping with a project. It isn’t true.")
        choice("Lie for {friend.him}.") {
            trust(BEST_FRIEND, +12); trait(LOYAL, 3); trait(DISHONEST, 4); reputation(-2); flag("took_shortcut"); count("mistakes")
            result("The teacher buys it. {friend} owes you big. You’d better hope the lie never comes out.")
        }
        choice("Refuse, but offer to help {friend.him} explain honestly.") {
            trust(BEST_FRIEND, +5); trait(RESPONSIBLE, 3); reputation(+2); confidence(+2)
            result("{friend} is annoyed, then relieved. The truth turns out to cost less than the lie would have.")
        }
        choice("Refuse. It isn’t your problem.") {
            trust(BEST_FRIEND, -8); friendship(-3); discipline(+1)
            result("{friend} has to face the consequences alone. Things are cooler for a while.")
        }
        choice("Help {friend.him} finish the work in time.") {
            trust(BEST_FRIEND, +9); energy(-8); trait(LOYAL, 2); trait(COMPASSIONATE, 1); knowledge(+1)
            result("You’re both up until 2 a.m. By morning, it’s done, and it’s true.")
        }
    }

    scenario("fri_jealousy", FRIENDSHIP, COFFEE_SHOP, 16..30, weight = 10) {
        text("{friend} just landed something you wanted badly: a spot, a prize, a chance. Everyone’s congratulating {friend.him}. You smile, but something inside you is twisting.")
        choice("Congratulate {friend.him} sincerely.") {
            trust(BEST_FRIEND, +8); friendship(+4); trait(COMPASSIONATE, 2); confidence(+1); happiness(+1)
            result("Saying it makes it true. You’re genuinely glad, and a little surprised at yourself.")
        }
        choice("Say nothing and withdraw for a while.") {
            trust(BEST_FRIEND, -6); friendship(-3); happiness(-3)
            result("The distance feels like a wall. Eventually {friend} asks what’s wrong.")
        }
        choice("Use the feeling to work harder.") {
            discipline(+4); confidence(+2); trait(AMBITIOUS, 3); energy(-6); trust(BEST_FRIEND, +2)
            result("Jealousy turns into fuel. By next year, you’re the one getting congratulated.")
        }
        choice("Spread a little doubt about how {friend} earned it.") {
            trait(DISHONEST, 6); reputation(-6); trust(BEST_FRIEND, -25); flag("betrayed_friend", "trust_broken"); count("mistakes"); hint()
            result("The rumour spreads faster than you expected. When it gets back to {friend}, the look on {friend.his} face says it all.")
        }
    }

    scenario("fri_secret_trade", FRIENDSHIP, HALLWAY, 17..32, weight = 8) {
        text("Someone offers you a real advantage, a spot or a favour, in exchange for a secret {friend} once confided in you. It would probably never come out.")
        choice("Share the secret.") {
            reputation(+3); career(+3); trust(BEST_FRIEND, -35); friendship(-8); trait(DISHONEST, 8); trait(LOYAL, -5)
            flag("betrayed_friend", "trust_broken"); count("mistakes"); hint()
            result("You get what you wanted. Within a month, {friend} knows. {friend.He} doesn’t shout. {friend.He} just stops calling.")
        }
        choice("Refuse, and tell them what you think of the offer.") {
            trust(BEST_FRIEND, +6); reputation(+4); confidence(+4); trait(LOYAL, 3); flag("protected_reputation"); hint()
            result("Walking away costs you the favour, but you leave with your integrity, and, later, your friend’s trust.")
        }
        choice("Tell {friend} what happened.") {
            trust(BEST_FRIEND, +12); friendship(+4); trait(LOYAL, 3); trait(RESPONSIBLE, 1)
            result("{friend} is quiet for a while, then thanks you. “Not everyone would have told me.”")
        }
        choice("Share something harmless instead.") {
            trait(DISHONEST, 3); reputation(+1); trust(BEST_FRIEND, -2)
            result("It’s a half-measure that satisfies no one, but nobody gets hurt either.")
        }
    }

    scenario("fri_old_friend_returns", FRIENDSHIP, COFFEE_SHOP, 24..55, weight = 10) {
        text("You get a message from someone you haven’t spoken to in years: “Hey, it’s me. I was thinking about you. Can we catch up?”")
        extra(trustAtLeast(BEST_FRIEND, 70), "It’s {friend}. Despite the years, your first reaction is a grin.")
        choice("Meet up and catch up properly.") {
            friendship(+6); happiness(+5); trust(BEST_FRIEND, +6); energy(-3)
            result("Two hours become five. You leave feeling as if you had put down something heavy.")
        }
        choice("Reply politely, but you’re too busy.") {
            friendship(-2); happiness(-1)
            result("The thread fades. You’re a little relieved, and a little sad.")
        }
        choice("Invite them to your home for dinner.") {
            friendship(+7); happiness(+6); family(+2); money(-1); trust(BEST_FRIEND, +8)
            result("The kitchen fills with old stories and new ones. The evening is a small celebration.")
        }
        choice("Ask what they’re up to, with an eye on useful contacts.") {
            career(+2); friendship(-2); trait(AMBITIOUS, 2); trait(DISHONEST, 1)
            result("You make a contact, but you can tell they felt the shift. It isn’t quite friendship any more.")
        }
    }

    scenario("fri_friend_needs_help", FRIENDSHIP, APARTMENT, 22..50, weight = 10) {
        text("{friend} shows up at your door looking worn out. Things have collapsed: a lost job, a broken relationship. {friend.He} needs a place to stay for a few weeks.")
        choice("Of course. Take the couch.") {
            trust(BEST_FRIEND, +15); friendship(+5); energy(-6); money(-3); trait(LOYAL, 4); trait(COMPASSIONATE, 2); count("kindness")
            flag("housed_friend"); milestone("Took in a friend in need"); hint()
            result("It’s cramped, but it’s good. {friend} gets back on {friend.his} feet, and never forgets.")
        }
        choice("Help {friend.him} find a place, and cover the first week.") {
            trust(BEST_FRIEND, +8); money(-5); trait(RESPONSIBLE, 2); trait(COMPASSIONATE, 2); count("kindness")
            result("It’s a practical gift, and {friend} is grateful.")
        }
        choice("Apologise: you simply don’t have the space.") {
            trust(BEST_FRIEND, -8); friendship(-3); money(+1)
            result("{friend} understands, or says so. The conversation feels different afterwards.")
        }
        choice("Say yes, but set clear limits.") {
            trust(BEST_FRIEND, +6); trait(RESPONSIBLE, 3); discipline(+2); energy(-3)
            result("Clear rules keep the peace. It works, with good humour on both sides.")
        }
    }

    scenario("fri_peer_pressure_adult", FRIENDSHIP, NIGHT_CITY, 20..32, weight = 9) {
        text("Your friends are planning an expensive weekend trip. Everyone’s going, and the cost is more than you should be spending right now. “You can’t miss it!”")
        choice("Go. You only live once.") {
            happiness(+7); friendship(+5); money(-8); trait(IMPULSIVE, 3); trait(RISK_TAKER, 1)
            result("The trip is memorable. The credit card statement, less so.")
        }
        choice("Politely decline and explain why.") {
            money(+2); discipline(+3); friendship(-2); trait(RESPONSIBLE, 2); flag("saved_money")
            result("A few raised eyebrows, but most understand. You’re a little ahead for it.")
        }
        choice("Suggest a cheaper alternative.") {
            friendship(+3); happiness(+3); money(-2); trait(RESPONSIBLE, 1); reputation(+1)
            result("A weekend camping trip wins on a vote. It turns out to be the best of the lot.")
        }
        choice("Go, but pick up extra shifts to pay for it.") {
            happiness(+4); friendship(+3); money(-2); energy(-8); health(-2)
            result("It’s a lot of hours for a long weekend. You earn every minute of the sun.")
        }
    }

    scenario("fri_moving_apart", FRIENDSHIP, CAMPUS, 18..25, weight = 9) {
        text("After school, your friends scatter: some to different cities, some to jobs. The group chat is going quiet, and {friend}’s messages are getting rarer.")
        choice("Schedule regular calls and visits.") {
            trust(BEST_FRIEND, +10); friendship(+5); energy(-4); trait(LOYAL, 3); trait(DISCIPLINED, 1)
            result("It takes effort, but that effort is the friendship. Distance turns out to be just a number.")
        }
        choice("Focus on new friends where you are.") {
            friendship(+3); happiness(+3); trust(BEST_FRIEND, -5); confidence(+2)
            result("You find a new group. The old one fades, and that hurts more than you expected.")
        }
        choice("Let it happen. That’s life.") {
            friendship(-6); trust(BEST_FRIEND, -8); happiness(-3)
            result("The messages stop. You miss the people more than you thought you would.")
        }
        choice("Organise a reunion weekend.") {
            friendship(+6); happiness(+5); money(-3); trust(BEST_FRIEND, +6); reputation(+2); trait(RESPONSIBLE, 1)
            result("You become the person who keeps everyone together. People say it’s the best part of their year.")
        }
    }

    scenario("fri_wedding_invite", FRIENDSHIP, WEDDING, 25..40, weight = 8) {
        text("{friend} is getting married, in another country, in two weeks. Flights are expensive and a big project is wrapping up at work. {friend.He} says, “It’s okay if you can’t make it,” but the hurt behind the words is clear.")
        choice("Book the flight. You wouldn’t miss it.") {
            trust(BEST_FRIEND, +15); friendship(+6); money(-8); happiness(+6); career(-2); trait(LOYAL, 4); flag("went_abroad")
            result("You give the toast of your life, and {friend} cries through half of it.")
        }
        choice("Attend by video and send a heartfelt gift.") {
            trust(BEST_FRIEND, +3); friendship(+1); money(-2)
            result("It isn’t the same, but {friend} sends you a long thank-you message.")
        }
        choice("Skip it. Work comes first.") {
            career(+3); money(+2); trust(BEST_FRIEND, -12); friendship(-5); happiness(-3); flag("worked_too_much")
            result("The project goes well. The wedding photos show an empty seat where you were meant to be.")
        }
        choice("Ask your boss for time off and negotiate.") {
            trust(BEST_FRIEND, +10); trait(RESPONSIBLE, 2); confidence(+2)
            outcome(60) { bonus(atLeast(Stat.REPUTATION, 60), 20); friendship(+4); money(-6); result("The boss agrees. You make it just in time.") }
            outcome(40) { friendship(-2); result("The answer is no. You send a recorded speech instead.") }
        }
    }

    scenario("fri_business_ask", FRIENDSHIP, COFFEE_SHOP, 27..48, weight = 8) {
        text("{friend} has a business idea and wants you to be involved. It’s exciting, a little reckless, and would mean putting money, and the friendship, on the line.")
        choice("Invest and join as a partner.") {
            money(-12); trust(BEST_FRIEND, +8); trait(RISK_TAKER, 3); trait(LOYAL, 2); flag("took_business_risk"); count("risk_taken")
            outcome(50) { bonus(atLeast(Stat.KNOWLEDGE, 60), 15); bonus(atLeast(Stat.DISCIPLINE, 60), 10); money(+16); reputation(+4); result("The idea takes off. Your share pays back handsomely.") }
            outcome(50) { money(-5); confidence(-3); result("It stumbles and folds. You lose most of your stake, but not the friendship.") }
        }
        choice("Offer advice instead of money.") {
            trust(BEST_FRIEND, +5); knowledge(+1); trait(RESPONSIBLE, 2)
            result("Your sensible advice saves {friend} from a few classic mistakes.")
        }
        choice("Decline. You can’t mix money and friendship.") {
            money(+1); trust(BEST_FRIEND, -4); count("risk_avoided")
            result("It’s a clean decision, though you wonder what might have been.")
        }
        choice("Ask for a written agreement and invest a small amount.") {
            money(-5); trait(RESPONSIBLE, 3); knowledge(+2); trust(BEST_FRIEND, +3)
            result("Clear terms protect both of you. The business moves slowly but steadily.")
        }
    }

    scenario("fri_apology", FRIENDSHIP, COFFEE_SHOP, 28..60, weight = 14, priority = 45, fromPast = true) {
        requires(has("betrayed_friend"), lacks("apologised"))
        text("You run into {friend} at a coffee shop, years after the falling-out. {friend.He} sees you, hesitates, and starts to turn away.")
        choice("Say you’re sorry, and mean it.") {
            trust(BEST_FRIEND, +30); friendship(+8); confidence(+3); happiness(+6); trait(LOYAL, 3); trait(COMPASSIONATE, 2)
            flag("apologised", "reconciled"); count("redemptions"); milestone("Apologised to a friend you had betrayed"); hint()
            result("{friend} listens to the entire thing. Then, slowly, {friend.he} sits down. “Took you long enough.”")
        }
        choice("Nod politely and let {friend.him} go.") {
            happiness(-3); friendship(-2)
            result("The moment passes. It will probably come back to you later.")
        }
        choice("Explain why it wasn’t entirely your fault.") {
            trust(BEST_FRIEND, -5); happiness(-2); trait(DISHONEST, 1)
            result("The conversation goes in circles. {friend} leaves, politely and finally.")
        }
        choice("Send a message later, after thinking it through.") {
            trust(BEST_FRIEND, +10); friendship(+3); flag("apologised"); trait(RESPONSIBLE, 1)
            result("The message you write is shorter than you planned. The reply is shorter still, but warm.")
        }
    }

    scenario("fri_neighborhood", FRIENDSHIP, STREET, 30..65, weight = 7) {
        text("Your street is organising a community clean-up, with food afterwards. You’re tired, and the weekend is the only free time you have.")
        choice("Join in and bring something to share.") {
            friendship(+6); reputation(+4); happiness(+4); energy(-6); trait(COMPASSIONATE, 1); count("kindness")
            result("You meet half the street. Two of them become real friends.")
        }
        choice("Stay home and rest.") {
            energy(+8); health(+2); friendship(-1)
            result("You’re rested. The neighbours barely notice your absence.")
        }
        choice("Drop in for an hour.") {
            friendship(+2); reputation(+1); energy(-2)
            result("A cup of coffee, a few introductions, and you’re off.")
        }
        choice("Organise the next one yourself.") {
            friendship(+7); reputation(+6); trait(RESPONSIBLE, 2); energy(-8); confidence(+3); flag("community_leader")
            result("Within a few months you’ve built something that outlasts the street’s grumbles.")
        }
    }
}
