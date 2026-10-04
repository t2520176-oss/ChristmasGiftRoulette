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

/** Work: overtime, interviews, deadlines, presentations. */
val CareerCine = cinePack("career") {

    scene("car_overtime", OFFICE, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        act(PLAYER, TYPE, TIRED, 900)
        cam(ESTABLISHING)
        enter(BOSS, Edge.RIGHT, 0.70f)
        say(BOSS, "{name}, a moment?", NEUTRAL, NONE, MEDIUM)
        say(BOSS, "The big project. I need you to stay late.", CONFIDENT, EXPLAIN, TWO_SHOT)
        say(BOSS, "Every night this week. It’s a chance to impress me.", CONFIDENT, SHRUG)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, TIRED, 1000)
        say(NARRATOR, "You’re exhausted. You haven’t seen your family for days.")
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "Of course. I’ll stay.", TIRED, NOD, MEDIUM) {
            say(BOSS, "Good. I knew I could count on you.", PROUD, GENTLE_SMILE, CLOSE_UP)
            cue("clock")
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 2200)
        }
        option(1, "I’m sorry, I need to rest.", TIRED, SHAKE_HEAD, MEDIUM) {
            act(BOSS, NONE, DISAPPOINTED, 900)
            say(BOSS, "I see. Noted.", DISAPPOINTED, NONE, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1200)
        }
        option(2, "What if I work from home two nights?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(BOSS, "Hm. Let me think.", THOUGHTFUL, THINK, CLOSE_UP)
            outcome(0) { say(BOSS, "Fine. Half-remote. Don’t let me down.", NEUTRAL, NOD) }
            outcome(1) { say(BOSS, "No. I need you here.", ANGRY, CROSS_ARMS) }
        }
        option(3, "Could {coworker} help share the load?", NEUTRAL, EXPLAIN) {
            say(BOSS, "If {coworker} agrees. Ask.", NEUTRAL, NOD, MEDIUM)
        }
    }

    scene("car_interview", INTERVIEW_ROOM) {
        place(INTERVIEWER, 0.70f, LEFT, seated = true)
        place(PLAYER, 0.32f, RIGHT, seated = true)
        hold(INTERVIEWER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        pause(500)
        say(INTERVIEWER, "Thank you for coming in.", NEUTRAL, NOD, MEDIUM)
        say(INTERVIEWER, "I see you’ve changed jobs a few times.", THOUGHTFUL, EXPLAIN, TWO_SHOT)
        say(INTERVIEWER, "Why should we trust you to stay?", WORRIED, NONE, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        cam(TWO_SHOT, PLAYER, INTERVIEWER)

        option(0, "I’m looking for long-term growth.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(INTERVIEWER, NONE, THOUGHTFUL, 900)
            outcome(0) { say(INTERVIEWER, "Ambitious. I like that.", HAPPY, GENTLE_SMILE, CLOSE_UP); say(NARRATOR, "A week later, the offer arrives.") }
            outcome(1) { say(INTERVIEWER, "We’ll be in touch.", NEUTRAL, NOD, CLOSE_UP); say(NARRATOR, "Another candidate gets the role.") }
        }
        option(1, "I’ve learned a lot from my past jobs.", CONFIDENT, NOD, MEDIUM) {
            outcome(0) { say(INTERVIEWER, "Turning that into growth, impressive.", PROUD, NOD, CLOSE_UP); say(NARRATOR, "You get the job.") }
            outcome(1) { say(INTERVIEWER, "Thank you. We’ll let you know.", NEUTRAL, NONE, CLOSE_UP) }
        }
        option(2, "I understand your concern.", NEUTRAL, NONE, MEDIUM) {
            say(INTERVIEWER, "Go on.", THOUGHTFUL, NONE, CLOSE_UP)
            say(PLAYER, "I was searching. Now I know what I want.", CONFIDENT, EXPLAIN, TWO_SHOT)
            outcome(0) { say(INTERVIEWER, "Honesty. Rare. Welcome aboard.", HAPPY, HANDSHAKE, MEDIUM) }
            outcome(1) { say(INTERVIEWER, "I appreciate that. But the concern stays.", NEUTRAL, SHRUG) }
        }
        option(3, "I may not be the right fit, then.", SAD, SHRUG, CLOSE_UP) {
            act(INTERVIEWER, NONE, SURPRISED, 900)
            say(INTERVIEWER, "…Thank you for your time.", NEUTRAL, NONE, REACTION)
            say(NARRATOR, "The words are out before you can take them back.")
        }
    }

    scene("act_big_presentation", MEETING) {
        place(PLAYER, 0.40f, RIGHT)
        place(BOSS, 0.78f, LEFT, seated = true)
        place(COWORKER, 0.90f, LEFT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(WIDE)
        say(BOSS, "Whenever you’re ready, {name}.", NEUTRAL, NONE, MEDIUM)
        cue("beep")
        say(PLAYER, "One moment.", WORRIED, NONE, CLOSE_UP)
        act(PLAYER, NONE, AFRAID, 900)
        say(NARRATOR, "The screen goes black. The laptop is dead.")
        cam(REACTION, BOSS)
        act(BOSS, CROSS_ARMS, ANGRY, 900)
        say(BOSS, "Is there a problem?", ANGRY, NONE, MEDIUM)
        cam(WIDE)

        option(0, "No problem. I’ll just tell you.", CONFIDENT, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            cam(PUSH_IN, PLAYER)
            outcome(0) { say(NARRATOR, "You speak from the heart. The room is captivated.") ; act(BOSS, NONE, PROUD, 1000) }
            outcome(1) { say(NARRATOR, "You lose your thread halfway. It’s a rough forty minutes.") ; act(PLAYER, NONE, EMBARRASSED, 1000) }
        }
        option(1, "Give me five minutes to fix it.", WORRIED, EXPLAIN) {
            say(NARRATOR, "A borrowed charger, a restart and a lot of sweat.")
            outcome(0) { act(PLAYER, NONE, CONFIDENT, 1000); say(BOSS, "Impressive calm.", PROUD, NOD) }
            outcome(1) { act(BOSS, NONE, DISAPPOINTED, 900) }
        }
        action(2) {
            say(COWORKER, "Take mine.", CONFIDENT, NONE, MEDIUM)
            say(PLAYER, "Thank you!", HAPPY, NOD)
            say(NARRATOR, "You present flawlessly, and everyone knows who saved the day.")
        }
        option(3, "I’m sorry. Can we reschedule?", SAD, SHRUG) {
            say(BOSS, "Tomorrow. Nine o’clock.", ANGRY, NONE, CLOSE_UP)
        }
    }

    scene("act_run_for_train", STREET, MORNING) {
        place(PLAYER, 0.20f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(FOLLOW, PLAYER)
        cue("train")
        say(NARRATOR, "Today is the most important meeting of the year. And your alarm didn’t go off.")
        say(PLAYER, "Not today. Not today!", AFRAID, NONE, MEDIUM)
        say(PLAYER, "The 8:15. I can still make the 8:15.", WORRIED, HANDS_UP)
        cam(WIDE)
        pause(300)

        option(0, "Run!", EXCITED, FIST_PUMP, MEDIUM) {
            cue("whoosh")
            cam(FOLLOW, PLAYER)
            move(PLAYER, 0.95f, run = true)
            outcome(0) { cue("train"); act(PLAYER, FIST_PUMP, HAPPY, 1000); say(NARRATOR, "You slide through the doors as they close. On time.") }
            outcome(1) { act(PLAYER, NONE, TIRED, 1200); say(NARRATOR, "You watch the train pull away, gasping for air.") }
        }
        option(1, "The park! It’s shorter.", EXCITED, POINT) {
            cam(FOLLOW, PLAYER)
            move(PLAYER, 0.60f, run = true)
            outcome(0) { move(PLAYER, 0.95f, run = true); say(NARRATOR, "The shortcut saves two minutes. You make it, just.") }
            outcome(1) { act(PLAYER, NONE, ANGRY, 1100); say(NARRATOR, "A locked gate. You arrive long after the meeting began.") }
        }
        option(2, "Better call ahead.", NEUTRAL, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(PLAYER, "Hi, it’s {name}. I’ll be late. Please start without me.", WORRIED, PHONE)
            hold(PLAYER, Prop.NONE)
        }
        option(3, "Taxi!", EXCITED, HANDS_UP, MEDIUM) {
            cue("whoosh")
            exit(PLAYER, Edge.RIGHT, run = true)
            say(NARRATOR, "Expensive, but you arrive calm, and only a few minutes late.")
        }
    }
}
