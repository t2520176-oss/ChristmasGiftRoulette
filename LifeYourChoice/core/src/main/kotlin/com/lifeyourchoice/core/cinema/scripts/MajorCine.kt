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

/** Major life events and the moments where old decisions come back. */
val MajorCine = cinePack("major") {

    scene("maj_parent_ill", HOSPITAL, NIGHT) {
        place(PLAYER, 0.30f, RIGHT)
        place(DOCTOR, 0.52f, LEFT)
        place(FATHER, 0.80f, LEFT, seated = true)
        cam(ESTABLISHING)
        act(FATHER, NONE, TIRED, 300)
        say(DOCTOR, "{name}. Your father is seriously ill.", WORRIED, NONE, MEDIUM)
        say(DOCTOR, "The treatment will work. But it’s expensive.", NEUTRAL, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        say(NARRATOR, "It could put you in debt. You have to decide.")
        cam(TWO_SHOT, PLAYER, DOCTOR)

        option(0, "Use my savings. Whatever it takes.", CONFIDENT, NOD, CLOSE_UP) {
            say(DOCTOR, "I’ll start the paperwork.", NEUTRAL, NOD, MEDIUM)
            cam(CLOSE_UP, FATHER)
            say(FATHER, "You didn’t have to.", SAD, GENTLE_SMILE)
            say(PLAYER, "Yes, I did.", CONFIDENT, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "I’ll find a loan.", WORRIED, NOD) {
            say(DOCTOR, "Good. We’ll begin tomorrow.", NEUTRAL, NOD, MEDIUM)
        }
        option(2, "Is there a cheaper treatment?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(DOCTOR, "There is. It’s slower.", THOUGHTFUL, SHRUG)
            outcome(0) { say(NARRATOR, "With careful research, you find a programme that works. He recovers.") }
            outcome(1) { say(NARRATOR, "The cheaper option works less well. You wonder if you chose right.") }
        }
        option(3, "I can’t. Work needs me.", SAD, SHRUG, MEDIUM) {
            cam(CLOSE_UP, FATHER)
            act(FATHER, HEAD_DOWN, DISAPPOINTED, 1400)
            say(NARRATOR, "Work is a refuge. A quiet disappointment lingers in the room.")
        }
    }

    scene("maj_job_loss", OFFICE) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        place(BOSS, 0.70f, LEFT)
        cam(ESTABLISHING)
        say(BOSS, "{name}. Please, sit.", NEUTRAL, NONE, MEDIUM)
        say(BOSS, "The company is restructuring.", SAD, EXPLAIN, TWO_SHOT)
        say(BOSS, "Your position is gone. I’m sorry.", SAD, HEAD_DOWN, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1200)
        say(NARRATOR, "A box, a handshake, an hour to clear your desk.")
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "I’ll start job hunting today.", CONFIDENT, NOD, MEDIUM) {
            outcome(0) { say(NARRATOR, "Within weeks, a solid track record lands you a new position.") }
            outcome(1) { say(NARRATOR, "The search drags on. You take a step down, and rebuild.") }
        }
        option(1, "Maybe it’s time to retrain.", THOUGHTFUL, EXPLAIN) {
            outcome(0) { say(NARRATOR, "The new skills pay off. You return stronger.") }
            outcome(1) { say(NARRATOR, "It takes a long time. But you come back with more tools.") }
        }
        option(2, "I’ll start my own business.", EXCITED, FIST_PUMP, MEDIUM) {
            say(BOSS, "That’s brave.", HAPPY, NOD, CLOSE_UP)
        }
        option(3, "I’ll call everyone I know.", WORRIED, PHONE) {
            outcome(0) { say(NARRATOR, "An old colleague offers an interview. You’re back before the savings run low.") }
            outcome(1) { say(NARRATOR, "Few doors open. You find work eventually, but not what you hoped for.") }
        }
        option(4, "{coworker}? I need a favour.", WORRIED, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(COWORKER, "Come work with us.", HAPPY)
            voice(COWORKER, "You were there when it mattered to me.", PROUD)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1700)
        }
    }

    scene("maj_family_emergency", OFFICE, NIGHT) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        place(BOSS, 0.80f, LEFT, seated = true)
        cam(WIDE)
        cue("phone")
        say(PLAYER, "Sorry. I have to take this.", WORRIED, PHONE, MEDIUM)
        hold(PLAYER, Prop.PHONE)
        voice(SIBLING, "{name}, it’s {sibling}. It’s bad.", AFRAID)
        voice(SIBLING, "They took Dad to the hospital.", SAD)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, AFRAID, 1400)
        card("THE CALL THAT CHANGED EVERYTHING", null, 2000)
        hold(PLAYER, Prop.NONE)
        cam(TWO_SHOT, PLAYER, BOSS)
        say(BOSS, "We’re in the middle of the deal.", ANGRY, POINT, MEDIUM)

        option(0, "I’m leaving. Now.", AFRAID, NONE, CLOSE_UP) {
            say(BOSS, "{name}…", ANGRY, NONE, REACTION)
            exit(PLAYER, Edge.LEFT, run = true)
        }
        option(1, "Give me an hour. Then I’ll go.", WORRIED, EXPLAIN) {
            say(BOSS, "Good. One hour.", NEUTRAL, NOD, MEDIUM)
        }
        option(2, "Update me by phone. I’ll stay.", SAD, SHRUG) {
            say(NARRATOR, "By the time you get there, visiting hours are over.")
        }
        option(3, "Can someone cover for me?", WORRIED, HANDS_UP) {
            say(BOSS, "…Go. I’ll handle it.", NEUTRAL, NOD, MEDIUM)
            exit(PLAYER, Edge.LEFT, run = true)
        }
    }

    scene("maj_overseas_offer", AIRPORT, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(BOSS, 0.70f, LEFT)
        hold(PLAYER, Prop.SUITCASE)
        cam(ESTABLISHING)
        say(BOSS, "There’s a role for you overseas.", CONFIDENT, EXPLAIN, MEDIUM)
        say(BOSS, "Two years. A big step up.", CONFIDENT, NONE, TWO_SHOT)
        say(BOSS, "The flight leaves tonight. What do you say?", CONFIDENT, POINT, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1200)
        say(NARRATOR, "Far from family and friends, in a place you’ve never been.")
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "I’ll take it. I’m going.", EXCITED, FIST_PUMP, MEDIUM) {
            say(BOSS, "Good. Don’t miss the plane.", PROUD, HANDSHAKE, CLOSE_UP)
            cue("whoosh")
            exit(PLAYER, Edge.RIGHT)
        }
        option(1, "Thank you. But home matters more.", SAD, SHAKE_HEAD) {
            say(BOSS, "Understood.", NEUTRAL, NOD, CLOSE_UP)
        }
        option(2, "Could it be six months instead?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            outcome(0) { say(BOSS, "Six months. Done.", HAPPY, HANDSHAKE) }
            outcome(1) { say(BOSS, "It’s all or nothing.", NEUTRAL, SHRUG) }
        }
        option(3, "I need to talk to my family first.", WORRIED, PHONE) {
            say(BOSS, "One hour. Then I need an answer.", NEUTRAL, NOD, MEDIUM)
        }
    }
}
