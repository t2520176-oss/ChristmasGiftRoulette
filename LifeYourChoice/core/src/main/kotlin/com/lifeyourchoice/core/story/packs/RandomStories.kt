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
 * Random events add variety, but they only open situations. The player’s choices (and past
 * decisions) decide what they lead to.
 */
val RandomStories = storyPack("random", "Life Happens") {

    scenario("rnd_found_cash", RANDOM, STREET, 16..70, title = "Lucky Find", weight = 8) {
        text("Walking home, you spot a small roll of cash tucked against the kerb, near a bus stop. There’s nobody around, and no way of telling whose it is.")
        choice("Hand it in at the nearest shop or police desk.") {
            reputation(+3); trait(RESPONSIBLE, 2); happiness(+2); count("kindness")
            result("The shopkeeper takes a note of your name, just in case. Later, a woman comes in, relieved and grateful.")
        }
        choice("Keep it. Finders keepers.") {
            money(+4); trait(DISHONEST, 4); happiness(+1); count("mistakes")
            result("It’s a small windfall, and a small, persistent itch in your conscience.")
        }
        choice("Donate it to a charity box.") {
            happiness(+3); trait(COMPASSIONATE, 2); count("kindness")
            result("It feels clean and simple. Someone, somewhere, is helped by it.")
        }
        choice("Leave a note nearby and wait a while.") {
            trait(RESPONSIBLE, 2); energy(-2); knowledge(+1)
            result("Nobody appears, so you hand it in. It’s the right thing, even if it was inconvenient.")
        }
    }

    scenario("rnd_old_friend_call", RANDOM, COFFEE_SHOP, 22..70, title = "An Old Friend", weight = 8) {
        text("An old friend from years ago sends a message out of the blue: “Remember me? I found some old photos and thought of you.”")
        choice("Call right away.") {
            friendship(+5); happiness(+5); energy(-2)
            result("A two-hour call, a lot of laughter and a promise to meet. This time you keep it.")
        }
        choice("Reply with a short message.") {
            friendship(+1); happiness(+1)
            result("A thumbs-up, a heart. It’s the minimum, but it’s something.")
        }
        choice("Invite them for a weekend.") {
            friendship(+7); happiness(+6); money(-2); energy(-5)
            result("The weekend is chaotic and full of memories. You vow not to lose touch again.")
        }
        choice("Ignore it. You’re busy.") {
            friendship(-3); happiness(-1)
            result("The message sits unanswered, and you feel it every time you open your phone.")
        }
    }

    scenario("rnd_restructuring", RANDOM, OFFICE, 24..58, title = "Rumours at Work", weight = 8) {
        requires(employed)
        text("An email lands: the company is announcing “organisational changes” next month. Everyone is whispering about who’s safe and who’s not.")
        choice("Update your résumé and quietly look around.") {
            confidence(+2); knowledge(+2); trait(RESPONSIBLE, 2); energy(-3)
            result("You don’t panic. You prepare, which is almost as good as being safe.")
        }
        choice("Work extra hard to prove your worth.") {
            career(+4); energy(-10); health(-2); trait(AMBITIOUS, 1); count("overwork")
            result("You become the person nobody dares lay off, though at some cost.")
        }
        choice("Talk to {boss} about the plans.") {
            trust(BOSS, +4); knowledge(+2); confidence(+2)
            result("{boss} can’t say much, but is touched you asked directly. It helps.")
        }
        choice("Ignore the rumours.") {
            happiness(+1); count("risk_avoided")
            result("The rumours fade. Whatever happens, you’ll deal with it then.")
        }
    }

    scenario("rnd_freelance_offer", RANDOM, COFFEE_SHOP, 20..60, title = "Unexpected Offer", weight = 8) {
        text("A stranger you met at a networking event offers you a freelance project: good pay, short deadline, outside your usual work. They need an answer by tomorrow.")
        choice("Accept and figure it out.") {
            money(+7); knowledge(+4); energy(-10); trait(RISK_TAKER, 2); count("risk_taken")
            outcome(65) { bonus(atLeast(Stat.KNOWLEDGE, 55), 15); reputation(+4); confidence(+4); result("You deliver, narrowly, and the client sends two more referrals.") }
            outcome(35) { money(-2); confidence(-3); result("It’s harder than expected, and the client is unimpressed. A tough lesson.") }
        }
        choice("Negotiate a more realistic deadline.") {
            money(+4); knowledge(+3); trait(RESPONSIBLE, 2); confidence(+3)
            result("They agree, and you do your best work. A little negotiation goes a long way.")
        }
        choice("Decline politely.") {
            energy(+3); happiness(+1); count("risk_avoided")
            result("You stay in your lane. Safe, stable, a bit boring.")
        }
        choice("Refer a friend who needs the work.") {
            trust(BEST_FRIEND, +8); friendship(+3); trait(COMPASSIONATE, 2); count("kindness")
            result("{friend} gets the project and the confidence boost. You get a very big thank-you dinner.")
        }
    }

    scenario("rnd_car_repair", RANDOM, STREET, 22..65, title = "Car Trouble", weight = 7) {
        text("Your car gives up with an ominous noise on the way to work. The mechanic whistles when he sees the engine: the repair will be very expensive.")
        choice("Pay for the repair.") {
            money(-8); energy(+2)
            onlyIf(has("saved_money")) { money(+4); note("Your emergency fund quietly takes the hit, so it’s only an annoyance.") }
            result("You pay, and you drive away with a lighter wallet.")
        }
        choice("Take public transport and rethink.") {
            money(+2); health(+2); energy(-4); trait(RESPONSIBLE, 1)
            result("The commute is slower, but cheaper, and surprisingly pleasant after a while.")
        }
        choice("Ask the mechanic for a cheaper fix.") {
            money(-4); knowledge(+2)
            result("A patched-up solution that works fine. You learn some useful basics along the way.")
        }
        choice("Take a loan and buy a newer car.") {
            money(-9); happiness(+3); flag("in_debt"); trait(IMPULSIVE, 1)
            result("It’s a nice car. The monthly payment is less nice.")
        }
    }

    scenario("rnd_viral", RANDOM, APARTMENT, 16..45, title = "Gone Viral", weight = 6) {
        text("Something you posted or made goes viral overnight. Thousands of strangers are talking about it, and your inbox is exploding with messages.")
        choice("Embrace it and build on the momentum.") {
            reputation(+8); confidence(+6); energy(-10); money(+4); trait(AMBITIOUS, 2); trait(RISK_TAKER, 1)
            result("You post every day for a month. It’s exhausting, thrilling, and unreliable.")
        }
        choice("Stay low-key. Fame feels odd.") {
            happiness(+1); confidence(+2)
            result("The attention fades, and you’re relieved. Your friends tease you about being a “former celebrity.”")
        }
        choice("Use the attention to support a good cause.") {
            reputation(+9); happiness(+6); trait(COMPASSIONATE, 3); count("kindness")
            result("You turn the spotlight onto something that deserves it. It raises more than you imagined.")
        }
        choice("Cash in as quickly as possible.") {
            money(+9); reputation(-3); trait(DISHONEST, 2)
            result("A few quick deals, a few raised eyebrows. The money is real; so is the whiff of opportunism.")
        }
    }

    scenario("rnd_neighbor_move", RANDOM, STREET, 18..75, title = "A Neighbour’s Request", weight = 7) {
        text("An elderly neighbour is trying to carry boxes up the stairs alone. You were just about to go out. “Don’t worry about me,” she says breathlessly.")
        choice("Put everything aside and help.") {
            happiness(+4); reputation(+3); energy(-4); trait(COMPASSIONATE, 2); count("kindness")
            result("She makes you tea, tells you about her life, and gives you a wonderful recipe.")
        }
        choice("Help with the heaviest boxes, then go.") {
            reputation(+2); energy(-3); trait(COMPASSIONATE, 1)
            result("It’s fast and kind. She waves as you go.")
        }
        choice("Offer to come back later.") {
            reputation(+1); trait(RESPONSIBLE, 1)
            result("You keep your word, an hour later, with a friend. She’s delighted.")
        }
        choice("Say you’re in a hurry.") {
            happiness(-1)
            result("You feel a small prick of guilt the whole day.")
        }
    }

    scenario("rnd_stranger_kindness", RANDOM, STREET, 16..70, title = "A Stranger’s Kindness", weight = 6) {
        text("It’s been a hard week. Soaked in the rain, with a dead phone and a missed bus, you’re stuck. A stranger holds out an umbrella, then a phone charger, and says, “Pay it forward.”")
        choice("Accept, and promise to pay it forward.") {
            happiness(+6); trait(COMPASSIONATE, 2); flag("paid_forward")
            result("A small moment becomes a memory. You do pay it forward, later that month.")
        }
        choice("Accept, but insist on paying them back.") {
            happiness(+3); money(-1); trait(RESPONSIBLE, 1)
            result("They decline, and smile. You walk home dry, and thoughtful.")
        }
        choice("Refuse politely. You can manage.") {
            happiness(-1); confidence(+1)
            result("You walk home soaked, and a little proud. Possibly foolish.")
        }
        choice("Ask what they need in return.") {
            happiness(-1); trait(DISHONEST, 1)
            result("They look amused. “Nothing,” they say. You realise how guarded the world has made you.")
        }
    }

    scenario("rnd_flu", RANDOM, APARTMENT, 14..70, title = "Under the Weather", weight = 7) {
        text("You wake up feeling terrible: aching, feverish, and miserable. It’s a crucial day, and everyone expects you to be there.")
        choice("Stay home and rest properly.") {
            health(+4); energy(+8); career(-1); trait(RESPONSIBLE, 1); count("overwork", -1)
            result("You sleep, drink tea and recover in two days. Nobody’s world ends.")
        }
        choice("Push through and go anyway.") {
            health(-4); energy(-8); career(+1); count("overwork")
            outcome(60) { health(-2); result("You’re useless at work, and you get others sick. Not your best idea.") }
            outcome(40) { result("You get through it. You’re wiped out for days afterwards.") }
        }
        choice("See a doctor.") {
            health(+3); money(-2); knowledge(+1)
            result("A simple diagnosis and clear instructions. It’s over in a week.")
        }
        choice("Work from home in bed.") {
            health(+1); career(+1); energy(-3)
            result("A compromise, and not a great one. You feel half-ill, half-productive.")
        }
    }

    scenario("rnd_power_cut", RANDOM, LIVING_ROOM, 14..70, title = "Power Cut", weight = 6) {
        text("The power goes out across the neighbourhood on a Friday evening. The phones are at 10%, the Wi-Fi is dead, and the house is dark.")
        choice("Light candles and play board games.") {
            family(+6); happiness(+6); trait(RESPONSIBLE, 1)
            result("You laugh louder than you have in weeks. Someone tells a story you’ve never heard.")
        }
        choice("Go out and see what’s happening.") {
            friendship(+4); happiness(+3); reputation(+2)
            result("Half the street is outside with torches, and everyone is talking. It feels like a village.")
        }
        choice("Go to bed early.") {
            energy(+8); health(+2)
            result("A very early night. You’re ready to conquer Saturday.")
        }
        choice("Sit in the dark, fuming about the outage.") {
            happiness(-2)
            result("Nothing improves by being annoyed in the dark.")
        }
    }

    scenario("rnd_stray_pet", RANDOM, STREET, 18..65, title = "A Visitor", weight = 6) {
        text("A scruffy stray dog follows you home, looks up with enormous eyes, and sits on your doorstep. It has no collar, and clearly nowhere else to go.")
        choice("Adopt it.") {
            happiness(+8); health(+3); money(-3); energy(-4); trait(COMPASSIONATE, 2); flag("has_pet"); count("kindness")
            result("Within a week, you can’t imagine life without the furry shadow at your heels.")
        }
        choice("Take it to a shelter.") {
            trait(RESPONSIBLE, 2); happiness(+2); money(-1)
            result("They scan it for a chip, and you leave your number in case. A tidy, kind solution.")
        }
        choice("Post photos and look for the owner.") {
            reputation(+2); trait(RESPONSIBLE, 2); friendship(+2)
            result("Within days, a very emotional family arrives to collect their dog.")
        }
        choice("Shoo it away.") {
            happiness(-2); trait(COMPASSIONATE, -1)
            result("It looks back at you from the corner of the street. You don’t sleep well.")
        }
    }

    scenario("rnd_reunion", RANDOM, COFFEE_SHOP, 28..55, title = "School Reunion", weight = 6) {
        text("An invitation arrives for your school reunion. Part of you wants to go, and part wants to avoid the comparison game.")
        extra(has("mocked_underdog"), "You also remember some things you did back then that you’re not proud of.")
        choice("Go and just enjoy it.") {
            friendship(+5); happiness(+5); reputation(+2)
            result("It’s warmer and kinder than you feared. Everyone is just a person who got older.")
        }
        choice("Go, and show off your success.") {
            reputation(+1); happiness(-1); friendship(-2); trait(AMBITIOUS, 1)
            result("People nod politely. You leave with a faint feeling of having missed the point.")
        }
        choice("Skip it.") {
            happiness(-1); energy(+2)
            result("You spend a quiet evening at home. The photos arrive the next day.")
        }
        choice("Go, and find the people you lost touch with.") {
            friendship(+7); happiness(+5); trait(LOYAL, 1)
            result("Two hours with an old friend are worth the whole trip.")
        }
    }

    scenario("rnd_conference", RANDOM, MEETING, 24..55, title = "Free Ticket", weight = 6) {
        requires(employed)
        text("A colleague gives you a free ticket to an industry event happening this weekend. You’d have to give up your Saturday, and you’d have to talk to strangers.")
        choice("Go and introduce yourself to everyone.") {
            knowledge(+4); career(+4); reputation(+3); energy(-6); confidence(+3)
            result("Awkward for ten minutes, then genuinely interesting. You come home with a pile of business cards.")
        }
        choice("Go, and attend a few talks only.") {
            knowledge(+4); energy(-3)
            result("You learn a lot and talk to nobody. It’s a start.")
        }
        choice("Give the ticket to a junior colleague.") {
            reputation(+3); trust(COWORKER, +8); trait(COMPASSIONATE, 2); count("kindness")
            result("They’re stunned, and thrilled. They never forget the favour.")
        }
        choice("Spend the Saturday with family instead.") {
            family(+5); happiness(+4); career(-1)
            result("A slow breakfast and a long walk. You don’t miss the conference at all.")
        }
    }

    scenario("rnd_fun_run", RANDOM, PARK, 16..60, title = "Fun Run", weight = 6) {
        text("{friend} signs you up for a charity fun run. “It’s only five kilometres,” {friend.he} says, with a smile you don’t trust.")
        choice("Train for it properly.") {
            health(+7); discipline(+3); energy(-4); confidence(+3); trait(DISCIPLINED, 1)
            result("A few weeks of early mornings, and you cross the finish line, wheezing and proud.")
        }
        choice("Walk most of it, and enjoy the chat.") {
            health(+2); friendship(+4); happiness(+4); trust(BEST_FRIEND, +3)
            result("You finish last, laughing, with ice cream.")
        }
        choice("Cancel. You’re not a runner.") {
            happiness(-1); health(-1)
            result("You spend the morning on the couch. The photos make it look fun.")
        }
        choice("Volunteer at the water station instead.") {
            reputation(+3); happiness(+3); count("kindness")
            result("You hand out hundreds of cups of water, and get a surprising number of high-fives.")
        }
    }

    scenario("rnd_gift", RANDOM, LIVING_ROOM, 16..70, title = "A Surprise Gift", weight = 5) {
        text("A relative sends you a surprise cash gift “just because.” It’s not enormous, but it’s unexpected, and your bank account certainly noticed.")
        choice("Save it.") {
            money(+5); discipline(+2); flag("saved_money")
            result("A little safety cushion grows a bit thicker.")
        }
        choice("Treat yourself and someone you love.") {
            money(+1); happiness(+6); family(+3)
            result("A good dinner and a long conversation. You thank your relative with a heartfelt call.")
        }
        choice("Invest in a skill or a tool you need.") {
            money(+1); knowledge(+4); confidence(+2)
            result("It’s the kind of gift that keeps on giving.")
        }
        choice("Share it with someone who needs it more.") {
            happiness(+5); trait(COMPASSIONATE, 3); reputation(+2); count("kindness")
            result("It’s a joy to pass along. They’ll never know how much it mattered to you, too.")
        }
    }
}
