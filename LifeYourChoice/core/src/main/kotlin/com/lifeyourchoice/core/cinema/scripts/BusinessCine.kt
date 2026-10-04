package com.lifeyourchoice.core.cinema.scripts

import com.lifeyourchoice.core.cinema.ActorId.*
import com.lifeyourchoice.core.cinema.Edge
import com.lifeyourchoice.core.cinema.Emotion.*
import com.lifeyourchoice.core.cinema.Facing.*
import com.lifeyourchoice.core.cinema.Gesture.*
import com.lifeyourchoice.core.cinema.Prop
import com.lifeyourchoice.core.cinema.Shot.*
import com.lifeyourchoice.core.cinema.TimeOfDay.*
import com.lifeyourchoice.core.cinema.TitleKind
import com.lifeyourchoice.core.cinema.cinePack
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.SceneArt.*
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.story.*

/** Business and money: starting, struggling, failing, rising again. */
val BusinessCine = cinePack("business") {

    scene("biz_idea", SMALL_BUSINESS, MORNING) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.68f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(FRIEND, "So this is the idea you keep talking about?", HAPPY, EXPLAIN, TWO_SHOT)
        say(PLAYER, "It’s been following me around for months.", EXCITED, EXPLAIN, MEDIUM)
        say(FRIEND, "Then it’s time. Start it. Or let it go.", CONFIDENT, POINT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’m doing it. I’m quitting and starting now.", EXCITED, FIST_PUMP, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "Now? Really? That’s brave.", EXCITED, HANDS_UP, CLOSE_UP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1600)
        }
        option(1, "I’ll start it on the side. Keep my job.", THOUGHTFUL, NOD) {
            say(FRIEND, "Smart. And exhausting.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(2, "I’ll research for a year first.", THOUGHTFUL, EXPLAIN) {
            say(FRIEND, "A year. Okay. Do it right.", NEUTRAL, NOD, MEDIUM)
        }
        option(3, "Maybe it’s too risky.", SAD, SHRUG) {
            say(FRIEND, "Maybe. Or maybe it’s just scary.", THOUGHTFUL, NONE, CLOSE_UP)
        }
    }

    scene("biz_slow_season", SMALL_BUSINESS, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(COWORKER, 0.70f, LEFT)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        say(COWORKER, "Rent is due Friday.", WORRIED, NONE, MEDIUM)
        say(COWORKER, "Sales are down again. Third month.", WORRIED, SHRUG, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(NARRATOR, "The line on the screen is heading straight down.")
        cam(TWO_SHOT, PLAYER, COWORKER)
        say(COWORKER, "What do we do?", WORRIED, NONE, OVER_SHOULDER)

        option(0, "We cut costs. Starting with my salary.", SAD, NOD, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "You don’t have to…", WORRIED, NONE, REACTION)
            say(PLAYER, "I do.", CONFIDENT, NONE)
        }
        option(1, "I’ll take out a loan to bridge it.", WORRIED, EXPLAIN) {
            say(COWORKER, "That’s a lot of risk.", WORRIED, SHRUG, CLOSE_UP)
        }
        option(2, "We try something new. Today.", EXCITED, POINT, MEDIUM) {
            say(COWORKER, "A new product?", SURPRISED, NONE, REACTION)
            outcome(0) { say(NARRATOR, "The new offering sells. A crisis showed you what customers really want."); act(PLAYER, FIST_PUMP, HAPPY, 1100) }
            outcome(1) { say(NARRATOR, "It doesn’t land. You spent your reserves, but learned a lot."); act(PLAYER, NONE, TIRED, 1100) }
        }
        option(3, "Let’s ask {friend} and the regulars for help.", THOUGHTFUL, NOD, MEDIUM) {
            say(COWORKER, "People do like us.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    scene("biz_failed", SMALL_BUSINESS, NIGHT) {
        place(PLAYER, 0.50f, RIGHT, seated = true)
        card("BUSINESS FAILED", null, 2400, kind = TitleKind.CARD)
        cam(ESTABLISHING)
        say(NARRATOR, "The shutters are down. Debts, unsold stock, and a hollow quiet.")
        cam(PUSH_IN, PLAYER)
        act(PLAYER, HEAD_DOWN, SAD, 1600)
        say(PLAYER, "Three years. All of it, gone.", SAD, NONE, CLOSE_UP)
        say(NARRATOR, "What matters now is what you do next.")

        option(0, "I’m done. Entrepreneurship isn’t for me.", SAD, SHRUG, CLOSE_UP) {
            say(NARRATOR, "You take a steady job. It’s safe, and slightly grey.")
        }
        option(1, "Back to work. I’ll rebuild my savings.", CONFIDENT, NOD, CLOSE_UP) {
            say(NARRATOR, "Every spare coin goes into savings. The pain softens as the balance grows.")
        }
        option(2, "Smaller. Smarter. I’m trying again.", CONFIDENT, FIST_PUMP, CLOSE_UP) {
            say(NARRATOR, "You start again, smaller, wiser, with the old mistakes pinned to the wall.")
        }
        option(3, "I need advice from an old friend.", THOUGHTFUL, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "I’m listening.", NEUTRAL)
            outcome(0) { voice(FRIEND, "You’re not finished. Come over, we’ll make a plan.", PROUD); say(NARRATOR, "An hour later, you have work, and a plan.") }
            outcome(1) { voice(FRIEND, "Sorry, I’m swamped. Coffee next week?", TIRED); say(NARRATOR, "A kind but short conversation.") }
            hold(PLAYER, Prop.NONE)
        }
        action(4) {
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "You were there for me when no one else was.", HAPPY)
            voice(FRIEND, "Let’s do this properly. I’ll fund the first six months.", PROUD)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1800)
        }
    }

    scene("biz_comeback", SMALL_BUSINESS, MORNING) {
        place(PLAYER, 0.36f, RIGHT)
        place(FRIEND, 0.70f, LEFT)
        cam(ESTABLISHING)
        card("YEARS LATER", null, 1600)
        say(FRIEND, "You’ve got that look again.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(PLAYER, "I have savings. Scars. And a better idea.", CONFIDENT, EXPLAIN, MEDIUM)
        say(FRIEND, "So? Are you doing it?", EXCITED, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Yes. Wiser, this time.", CONFIDENT, FIST_PUMP, MEDIUM) {
            outcome(0) { cam(PUSH_IN, PLAYER); say(NARRATOR, "This time, it works. Customers, staff, a name people trust."); act(PLAYER, NONE, PROUD, 1500) }
            outcome(1) { say(NARRATOR, "It doesn’t work. But this time you know what to do next.") }
        }
        option(1, "No. I’ll stay where I’m stable.", NEUTRAL, SHRUG) {
            say(FRIEND, "That’s a good life too.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(2, "I’ll join a young startup instead.", THOUGHTFUL, NOD) {
            say(FRIEND, "They’ll treat your advice like gold.", HAPPY, NOD, MEDIUM)
        }
        option(3, "I want to mentor new founders.", HAPPY, GENTLE_SMILE) {
            say(FRIEND, "Your worst year becomes your best lesson.", PROUD, NONE, CLOSE_UP)
        }
    }
}
