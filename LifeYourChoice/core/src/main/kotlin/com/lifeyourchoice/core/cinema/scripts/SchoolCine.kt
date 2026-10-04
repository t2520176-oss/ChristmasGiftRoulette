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

/** School years: friends, pressure, standing up for someone. */
val SchoolCine = cinePack("school") {

    scene("sch_skip_school", HALLWAY) {
        place(PLAYER, 0.30f, RIGHT)
        cam(ESTABLISHING)
        pause(700)
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "Hey, {name}. Come here.", EXCITED, WAVE, MEDIUM)
        say(FRIEND, "Let’s skip class today.", CONFIDENT, EXPLAIN, TWO_SHOT)
        say(FRIEND, "We’ll go to the mall. Nobody will know.", CONFIDENT, SHRUG, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Alright. Let’s go.", HAPPY) {
            act(FRIEND, FIST_PUMP, EXCITED, 900)
            say(FRIEND, "Yes! Before the bell rings!", EXCITED, WAVE, MEDIUM)
            cue("whoosh")
            exit(FRIEND, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
        }
        option(1, "No. I’m staying.", CONFIDENT, SHAKE_HEAD) {
            say(FRIEND, "Seriously?", SURPRISED, SHRUG)
            say(FRIEND, "Fine. Your loss.", DISAPPOINTED, NONE, REACTION)
            exit(FRIEND, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1300)
        }
        option(2, "I’ll meet you after school.", NEUTRAL, NOD) {
            say(FRIEND, "Deal. Don’t be late.", HAPPY, FIST_PUMP, MEDIUM)
            exit(FRIEND, Edge.RIGHT)
        }
        option(3, "Why are you trying to avoid school?", WORRIED, NONE, CLOSE_UP) {
            cam(REACTION, FRIEND)
            act(FRIEND, HEAD_DOWN, SAD, 1000)
            look(FRIEND, null)
            pause(700)
            say(FRIEND, "There are some guys waiting for me. Again.", SAD, HEAD_DOWN, CLOSE_UP)
            act(PLAYER, NONE, WORRIED, 800)
        }
    }

    scene("sch_bullied_friend", HALLWAY) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.64f, LEFT)
        act(FRIEND, HEAD_DOWN, SAD, 300)
        cam(TWO_SHOT, PLAYER, FRIEND)
        say(FRIEND, "Some older guys wait for me every morning.", SAD, HEAD_DOWN, CLOSE_UP)
        say(FRIEND, "They take my lunch money. Sometimes worse.", AFRAID)
        say(FRIEND, "That’s why I can’t face school.", SAD, NONE, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’ll help you.", CONFIDENT, POINT, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "You… would do that?", SURPRISED, NONE, REACTION)
            cam(WIDE)
            cue("steps")
            enter(STRANGER, Edge.RIGHT, 0.92f)
            say(STRANGER, "Well, look who’s here. Got my money?", ANGRY, EXPLAIN, MEDIUM)
            move(PLAYER, 0.54f)
            say(PLAYER, "Leave him alone.", ANGRY, POINT, OVER_SHOULDER)
            outcome(0) {
                say(STRANGER, "…Whatever. Not worth it.", EMBARRASSED, SHRUG)
                exit(STRANGER, Edge.RIGHT)
                cam(TWO_SHOT, PLAYER, FRIEND)
                act(FRIEND, NONE, HAPPY, 900)
                say(FRIEND, "Nobody ever stood up for me before.", HAPPY, GENTLE_SMILE, CLOSE_UP)
                act(PLAYER, NONE, PROUD, 900)
            }
            outcome(1) {
                say(STRANGER, "Big mistake.", ANGRY, EXPLAIN)
                cue("bell")
                say(NARRATOR, "A teacher arrives, and everyone ends up in the principal’s office.")
                act(FRIEND, NONE, WORRIED, 900)
                say(FRIEND, "Thanks for staying, anyway.", SAD, GENTLE_SMILE, CLOSE_UP)
            }
        }
        option(1, "Let’s tell a teacher.", THOUGHTFUL, NOD) {
            say(FRIEND, "They’ll find out it was me.", AFRAID, HEAD_DOWN)
            say(PLAYER, "I’ll come with you.", CONFIDENT, GENTLE_SMILE, MEDIUM)
            act(FRIEND, NOD, THOUGHTFUL, 900)
            say(FRIEND, "Okay. Okay, let’s do it.", WORRIED, NONE, CLOSE_UP)
            move(PLAYER, 0.76f, concurrent = true)
            move(FRIEND, 0.92f)
            cam(WIDE)
        }
        option(2, "That’s not my problem.", NEUTRAL, SHRUG) {
            act(FRIEND, NONE, SAD, 900)
            say(FRIEND, "Right. Of course.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            exit(FRIEND, Edge.LEFT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1400)
        }
        option(3, "You should talk to your parents.", THOUGHTFUL, EXPLAIN) {
            say(FRIEND, "I don’t know how to start.", WORRIED, SHRUG)
            say(PLAYER, "Start with what you told me.", CONFIDENT, GENTLE_SMILE, MEDIUM)
            say(FRIEND, "…Okay. I’ll try tonight.", THOUGHTFUL, NOD, CLOSE_UP)
        }
    }

    scene("sch_cheating_offer", CLASSROOM) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        place(RIVAL, 0.66f, LEFT, seated = true)
        place(TEACHER, 0.9f, LEFT)
        cam(WIDE)
        say(TEACHER, "Begin. You have forty minutes.", NEUTRAL, NONE)
        look(TEACHER, null)
        cam(TWO_SHOT, PLAYER, RIVAL)
        say(RIVAL, "Psst. {name}.", CONFIDENT, NONE, MEDIUM)
        say(RIVAL, "I have the answer sheet. Want it?", CONFIDENT, SHRUG, TWO_SHOT)
        say(RIVAL, "Everyone smart is using it.", CONFIDENT, EXPLAIN)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1000)

        option(0, "…Give it here.", AFRAID, NONE, CLOSE_UP) {
            outcome(0) { say(NARRATOR, "Nobody notices. You get a great grade, and a knot in your stomach.") }
            outcome(1) {
                cue("bell")
                say(TEACHER, "{name}. See me after class.", ANGRY, POINT, MEDIUM)
                act(PLAYER, HEAD_DOWN, SAD, 1200)
            }
        }
        option(1, "No. I’ll do it myself.", CONFIDENT, SHAKE_HEAD) {
            say(RIVAL, "Your funeral.", DISAPPOINTED, SHRUG)
            act(PLAYER, NONE, PROUD, 1100)
            say(NARRATOR, "It’s harder this way. But it’s yours.")
        }
        option(2, "I’m telling the teacher.", ANGRY, POINT, MEDIUM) {
            say(RIVAL, "You wouldn’t.", AFRAID)
            move(TEACHER, 0.62f)
            say(TEACHER, "What’s going on here?", NEUTRAL, NONE, TWO_SHOT)
            act(PLAYER, NONE, CONFIDENT, 900)
        }
        option(3, "Study with me next time instead.", NEUTRAL, GENTLE_SMILE) {
            act(RIVAL, NONE, SURPRISED, 900)
            say(RIVAL, "…You’d actually do that?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Sure. Thursday, library.", HAPPY, NOD, MEDIUM)
        }
    }

    scene("sch_underdog", SCHOOL_YARD) {
        place(PLAYER, 0.30f, RIGHT)
        place(UNDERDOG, 0.72f, LEFT, seated = true)
        place(RIVAL, 0.88f, LEFT)
        cam(ESTABLISHING)
        act(UNDERDOG, HEAD_DOWN, SAD, 400)
        say(NARRATOR, "{underdog} is eating alone again. A few kids nearby are laughing.")
        cam(TWO_SHOT, PLAYER, UNDERDOG)
        say(RIVAL, "Look at {underdog.him}, talking to a sandwich.", LAUGHING, POINT, MEDIUM)
        cam(CLOSE_UP, UNDERDOG)
        act(UNDERDOG, HEAD_DOWN, SAD, 900)
        say(UNDERDOG, "Just ignore them. Just ignore them.", SAD, HEAD_DOWN)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 900)

        option(0, "Mind if I sit here?", HAPPY, GENTLE_SMILE, MEDIUM) {
            move(PLAYER, 0.56f)
            sit(PLAYER)
            act(UNDERDOG, NONE, SURPRISED, 800)
            say(UNDERDOG, "…Sure. I mean, yes. Please.", EMBARRASSED, NONE, CLOSE_UP)
            say(UNDERDOG, "I’m {underdog}.", HAPPY, GENTLE_SMILE)
            say(PLAYER, "I know. I’m {name}.", HAPPY, NONE, TWO_SHOT)
        }
        option(1, "Ha! Good one.", LAUGHING, POINT) {
            act(RIVAL, NONE, LAUGHING, 800)
            cam(CLOSE_UP, UNDERDOG)
            act(UNDERDOG, HEAD_DOWN, SAD, 1100)
            say(NARRATOR, "{underdog} packs up and leaves without a word.")
            exit(UNDERDOG, Edge.LEFT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1200)
        }
        option(2, "…", NEUTRAL, SHRUG) {
            say(NARRATOR, "You eat with your friends. Nothing changes, and nothing happens.")
        }
        option(3, "Hey. Cut it out.", ANGRY, POINT, MEDIUM) {
            act(RIVAL, NONE, EMBARRASSED, 900)
            say(RIVAL, "Relax. It was a joke.", EMBARRASSED, SHRUG)
            exit(RIVAL, Edge.RIGHT)
            cam(CLOSE_UP, UNDERDOG)
            act(UNDERDOG, NOD, HAPPY, 1000)
        }
    }

    scene("sch_mentor_teacher", CLASSROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(MENTOR, 0.72f, LEFT)
        cam(WIDE)
        say(MENTOR, "{name}. A moment, please.", NEUTRAL, NONE, MEDIUM)
        say(MENTOR, "You have more potential than your grades show.", PROUD, EXPLAIN, TWO_SHOT)
        say(MENTOR, "If you want, I’ll give you extra guidance.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)

        option(0, "Yes. Thank you. I’d like that.", HAPPY, NOD) {
            say(MENTOR, "Good. Tomorrow, after class.", PROUD, GENTLE_SMILE, MEDIUM)
        }
        option(1, "I think I’ve got it covered.", NEUTRAL, SHRUG) {
            say(MENTOR, "The door stays open.", NEUTRAL, NOD, MEDIUM)
        }
        option(2, "Maybe just some advice.", THOUGHTFUL, NONE) {
            say(MENTOR, "Advice, then. Start with this.", HAPPY, EXPLAIN, MEDIUM)
        }
        option(3, "Why are you offering this?", WORRIED, POINT) {
            act(MENTOR, NONE, DISAPPOINTED, 900)
            say(MENTOR, "Because someone once did it for me.", THOUGHTFUL, NONE, CLOSE_UP)
        }
    }
}
