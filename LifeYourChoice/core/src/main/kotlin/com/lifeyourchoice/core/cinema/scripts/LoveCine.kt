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

/** Love: wholesome, honest, and never forced. */
val LoveCine = cinePack("love") {

    scene("lov_first_crush", HALLWAY) {
        place(PLAYER, 0.32f, RIGHT)
        cam(WIDE)
        enter(PARTNER, Edge.RIGHT, 0.70f)
        act(PARTNER, NONE, HAPPY, 700)
        look(PARTNER, PLAYER)
        cam(CLOSE_UP, PARTNER)
        act(PARTNER, GENTLE_SMILE, HAPPY, 900)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, EMBARRASSED, 1100)
        say(NARRATOR, "{partner} is smiling at you from across the hall.")
        say(PARTNER, "Hi.", HAPPY, WAVE, MEDIUM)
        say(PLAYER, "Uh… hi.", EMBARRASSED, SCRATCH_HEAD, REACTION)
        cam(TWO_SHOT, PLAYER, PARTNER)

        option(0, "Hi. I’m {name}.", HAPPY, WAVE, MEDIUM) {
            move(PLAYER, 0.56f)
            say(PARTNER, "I know. I’m {partner}.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(NARRATOR, "By the end of the week, you’re walking home together.")
        }
        option(1, "Hey {friend}, help me out here…", EMBARRASSED, SCRATCH_HEAD) {
            say(NARRATOR, "{friend} makes a gentle introduction. You’re friends before anything else.")
        }
        option(2, "Not now. Exams.", NEUTRAL, SHRUG) {
            exit(PARTNER, Edge.RIGHT)
            say(NARRATOR, "The crush fades into a quiet memory. Your grades benefit.")
        }
        option(3, "I wrote you a note.", EMBARRASSED, SCRATCH_HEAD, MEDIUM) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "You wrote… me a note?", HAPPY, NONE, CLOSE_UP)
        }
    }

    scene("lov_long_distance", AIRPORT, EVENING) {
        place(PLAYER, 0.32f, RIGHT)
        place(PARTNER, 0.66f, LEFT)
        hold(PARTNER, Prop.SUITCASE)
        cam(ESTABLISHING)
        say(PARTNER, "I got the job. The one I always dreamed about.", HAPPY, EXPLAIN, MEDIUM)
        act(PARTNER, NONE, WORRIED, 800)
        say(PARTNER, "It’s in another city.", WORRIED, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SAD, 1000)
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "Would you support me? Even from far away?", SAD, NONE, OVER_SHOULDER)

        option(0, "Go. I’ll support you all the way.", CONFIDENT, GENTLE_SMILE, MEDIUM) {
            act(PARTNER, NONE, HAPPY, 800)
            say(PARTNER, "I love you.", HAPPY, HUG, CLOSE_UP)
            outcome(0) { say(NARRATOR, "The distance is hard, but love holds. Calls, visits, letters.") }
            outcome(1) { say(NARRATOR, "The distance is harder than you imagined. You make it work, barely.") }
        }
        option(1, "Please… stay.", SAD, NONE, CLOSE_UP) {
            act(PARTNER, HEAD_DOWN, SAD, 1000)
            outcome(0) { say(PARTNER, "…Okay.", SAD, NONE, REACTION) }
            outcome(1) {
                say(PARTNER, "I can’t. I’m sorry.", SAD, NONE, REACTION)
                exit(PARTNER, Edge.RIGHT)
                say(NARRATOR, "The relationship doesn’t survive the argument.")
            }
        }
        option(2, "I’m not sure.", WORRIED, SHRUG) {
            say(PARTNER, "At least that’s honest.", SAD, NONE, CLOSE_UP)
        }
        option(3, "Then I’ll come with you.", EXCITED, NONE, MEDIUM) {
            act(PARTNER, NONE, SURPRISED, 1000)
            say(PARTNER, "You’d really do that?", EXCITED, HANDS_UP, CLOSE_UP)
            move(PLAYER, 0.54f)
            act(PLAYER, HUG, HAPPY, 1500)
        }
    }

    scene("lov_proposal", NIGHT_CITY, NIGHT) {
        place(PLAYER, 0.40f, RIGHT)
        place(PARTNER, 0.60f, LEFT)
        cam(ESTABLISHING)
        say(PARTNER, "Beautiful night.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(PLAYER, "It is.", HAPPY, NONE)
        say(PARTNER, "Everyone keeps asking me when it’s coming.", EMBARRASSED, SHRUG, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "…Do you know what I mean?", EMBARRASSED, NONE, CLOSE_UP)

        option(0, "{partner}. Will you marry me?", HAPPY, NONE, CLOSE_UP) {
            act(PARTNER, HANDS_UP, SURPRISED, 1000)
            say(PARTNER, "Yes! Yes, of course yes!", EXCITED, HUG, CLOSE_UP)
            cue("cheer")
        }
        option(1, "Soon. When I’m more secure.", NEUTRAL, EXPLAIN) {
            say(PARTNER, "Soon. Okay.", SAD, GENTLE_SMILE, CLOSE_UP)
        }
        option(2, "I’m not ready yet.", SAD, SHRUG, CLOSE_UP) {
            say(PARTNER, "I’d rather know.", DISAPPOINTED, NOD, REACTION)
        }
        option(3, "Marriage isn’t necessary for us.", THOUGHTFUL, NONE) {
            say(PARTNER, "Then we’ll do it our way.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    scene("lov_communication", APARTMENT, NIGHT) {
        place(PLAYER, 0.30f, RIGHT)
        place(PARTNER, 0.70f, LEFT)
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "We always do this.", ANGRY, EXPLAIN, MEDIUM)
        say(PLAYER, "Do what?", ANGRY, HANDS_UP)
        say(PARTNER, "Argue about the same thing. Chores. Money. Time.", ANGRY, POINT, OVER_SHOULDER)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, ANGRY, 900)

        option(0, "…You’re right. Tell me what you need.", SAD, GENTLE_SMILE, CLOSE_UP) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Really? You’ll listen?", SURPRISED, NONE, REACTION)
            say(PARTNER, "Okay. Here’s what I need.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "I need some air.", ANGRY, SHRUG) {
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "A pause helps a little. The conversation still needs to happen.")
        }
        option(2, "Well, I’m right.", ANGRY, POINT, MEDIUM) {
            say(PARTNER, "Wow. Okay.", ANGRY, CROSS_ARMS, CLOSE_UP)
            say(NARRATOR, "You’re technically right. The house is silent for two days.")
        }
        option(3, "Maybe we should talk to a counsellor.", THOUGHTFUL, NOD) {
            say(PARTNER, "…Yes. I think we should.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }
}
