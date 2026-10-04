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

/** Delayed consequences: something you did long ago walks back through the door. */
val PayoffCine = cinePack("payoffs") {

    scene("pay_friend_helps_crisis", APARTMENT, NIGHT) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        act(PLAYER, HEAD_DOWN, TIRED, 1200)
        say(NARRATOR, "The lights are off. The bills are on the table.")
        cam(CLOSE_UP, PLAYER)
        pause(900)
        cue("door")
        enter(FRIEND, Edge.RIGHT, 0.72f)
        hold(FRIEND, Prop.BAG)
        cam(WIDE)
        act(PLAYER, NONE, SURPRISED, 900)
        say(PLAYER, "{friend}?", SURPRISED, NONE, REACTION)
        act(FRIEND, NONE, HAPPY, 800)
        say(FRIEND, "I heard what’s going on.", HAPPY, GENTLE_SMILE, MEDIUM)
        whenever(has("helped_bullied_friend"), otherwise = {
            whenever(has("housed_friend"), otherwise = {
                say(FRIEND, "You’ve always been there for me.", HAPPY, NONE, CLOSE_UP)
            }) { say(FRIEND, "You took me in when I had nothing.", SAD, NONE, CLOSE_UP) }
        }) {
            say(FRIEND, "You helped me when nobody else would.", SAD, NONE, CLOSE_UP)
        }
        pause(500)
        say(FRIEND, "Now let me help you.", PROUD, GENTLE_SMILE, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Thank you. I won’t forget this.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            stand(PLAYER)
            move(PLAYER, 0.56f)
            act(PLAYER, HUG, HAPPY, 1800)
            say(NARRATOR, "It’s the first proper meal in weeks. {friend} stays the whole weekend.")
        }
        option(1, "Thanks. But I’ll pay you back.", NEUTRAL, HANDSHAKE, MEDIUM) {
            say(FRIEND, "Pay me in coffee.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(2, "I’m fine. Really.", SAD, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "No, you’re not. But okay.", SAD, NONE, REACTION)
            say(NARRATOR, "{friend} leaves the bag on the table. You eat from it that night anyway.")
        }
        option(3, "Can I ask your advice instead?", THOUGHTFUL, NOD) {
            say(FRIEND, "Always. Start from the top.", PROUD, EXPLAIN, MEDIUM)
        }
    }

    scene("pay_underdog_returns", MEETING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(COWORKER, 0.90f, LEFT, seated = true)
        cam(WIDE)
        pause(500)
        enter(UNDERDOG, Edge.RIGHT, 0.62f)
        say(UNDERDOG, "{name}?", SURPRISED, WAVE, MEDIUM)
        act(PLAYER, NONE, SURPRISED, 900)
        say(PLAYER, "{underdog}? From school?", SURPRISED, NONE, REACTION)
        say(UNDERDOG, "You were the only one who sat with me.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        say(UNDERDOG, "I never forgot that.", PROUD, NONE)
        say(UNDERDOG, "I run a company now. I could use someone I trust.", CONFIDENT, EXPLAIN, TWO_SHOT)

        option(0, "Tell me more.", EXCITED, NOD, MEDIUM) {
            say(NARRATOR, "The project that follows becomes one of the biggest of your life.")
        }
        option(1, "Let’s catch up over dinner.", HAPPY, GENTLE_SMILE) {
            say(UNDERDOG, "I’d love that.", HAPPY, HANDSHAKE, CLOSE_UP)
        }
        option(2, "Let me introduce you around.", HAPPY, EXPLAIN) {
            say(NARRATOR, "Doors open for {underdog}, and for you. Good people find each other.")
        }
        option(3, "I was just glad to help.", HAPPY, GENTLE_SMILE) {
            say(UNDERDOG, "It meant more than you know.", HAPPY, NONE, CLOSE_UP)
        }
    }
}
