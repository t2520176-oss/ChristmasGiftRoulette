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

/** Graduation, action moments, and later life. */
val LifeCine = cinePack("life") {

    scene("ms_graduation", CAMPUS) {
        place(PLAYER, 0.36f, RIGHT)
        place(FATHER, 0.70f, LEFT)
        place(MOTHER, 0.82f, LEFT)
        cam(ESTABLISHING)
        cue("cheer")
        say(MOTHER, "We’re so proud of you!", HAPPY, CLAP, MEDIUM)
        say(FATHER, "Look at you. All grown up.", PROUD, GENTLE_SMILE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, PROUD, 1100)
        say(NARRATOR, "A whole chapter closes. Another is about to begin.")
        cam(WIDE)

        option(0, "Thanks, Mom. Thanks, Dad. For everything.", HAPPY, HUG, MEDIUM) {
            say(MOTHER, "Oh, sweetheart.", HAPPY, HUG, CLOSE_UP)
        }
        option(1, "I’m going to find my friends!", EXCITED, WAVE) {
            exit(PLAYER, Edge.LEFT, run = true)
            cue("cheer")
        }
        option(2, "I need a moment to think.", THOUGHTFUL, NONE, CLOSE_UP) {
            say(NARRATOR, "You sit on the empty bleachers as the sun sets, and think about who you want to become.")
        }
        option(3, "Wait. I have to thank {mentor}.", HAPPY, WAVE) {
            enter(MENTOR, Edge.LEFT, 0.20f)
            say(MENTOR, "Go and do something good.", PROUD, GENTLE_SMILE, TWO_SHOT)
        }
    }

    scene("act_basketball_final", BASKETBALL_COURT, EVENING) {
        place(PLAYER, 0.38f, RIGHT)
        place(FRIEND, 0.74f, LEFT)
        hold(PLAYER, Prop.BALL)
        cam(WIDE)
        cue("cheer")
        say(NARRATOR, "Last seconds. One point down. The ball is in your hands.")
        say(PLAYER, "Breathe. Just breathe.", AFRAID, NONE, MEDIUM)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, AFRAID, 1200)
        cam(WIDE)
        say(FRIEND, "{name}! Here! I’m open!", EXCITED, WAVE, MEDIUM)
        cam(PUSH_IN, PLAYER)
        pause(500)
        cam(WIDE)

        option(0, "I’m taking it!", EXCITED, FIST_PUMP, CLOSE_UP) {
            cue("whoosh")
            move(PLAYER, 0.55f, run = true)
            outcome(0) { cue("cheer"); act(PLAYER, FIST_PUMP, EXCITED, 1400); say(NARRATOR, "Swish. The gym erupts.") }
            outcome(1) { act(PLAYER, HEAD_DOWN, SAD, 1300); say(NARRATOR, "The ball rattles off the rim. The buzzer sounds.") }
        }
        option(1, "{friend}, catch!", EXCITED, POINT) {
            hold(PLAYER, Prop.NONE)
            hold(FRIEND, Prop.BALL)
            outcome(0) { cue("cheer"); act(FRIEND, FIST_PUMP, EXCITED, 1200); say(NARRATOR, "{friend} shoots, and scores. You win together.") }
            outcome(1) { act(FRIEND, NONE, SAD, 1000); say(NARRATOR, "{friend} is blocked. You lose, but nobody blames you.") }
        }
        option(2, "Going in!", EXCITED, POINT) {
            cue("whoosh")
            move(PLAYER, 0.60f, run = true)
            outcome(0) { cue("cheer"); say(NARRATOR, "You weave through two defenders and lay it in.") }
            outcome(1) { say(NARRATOR, "A defender stops you short. You gave everything.") }
        }
        option(3, "Time-out! Coach!", WORRIED, HANDS_UP) {
            outcome(0) { say(NARRATOR, "The coach draws up a perfect play. It works. You win.") }
            outcome(1) { say(NARRATOR, "The play is read and blocked. One point short.") }
        }
    }

    scene("act_emergency_help", STREET, EVENING) {
        place(PLAYER, 0.32f, RIGHT)
        place(STRANGER, 0.70f, LEFT)
        cam(ESTABLISHING)
        cue("thud")
        act(STRANGER, HEAD_DOWN, TIRED, 300)
        sit(STRANGER)
        cam(WIDE)
        say(NARRATOR, "On the pavement, an elderly man has collapsed. People gather and stare.")
        say(STRANGER, "Is he breathing? Somebody, do something!", AFRAID, HANDS_UP, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1000)
        say(PLAYER, "Somebody has to do something.", AFRAID, NONE, MEDIUM)
        cam(WIDE)

        option(0, "I’m calling an ambulance. Stay with him!", CONFIDENT, PHONE, MEDIUM) {
            move(PLAYER, 0.56f)
            hold(PLAYER, Prop.PHONE)
            say(PLAYER, "Ambulance, please. There’s a man down.", CONFIDENT, PHONE, CLOSE_UP)
            hold(PLAYER, Prop.NONE)
            say(NARRATOR, "You hold his hand until the ambulance arrives. He is going to be fine.")
        }
        option(1, "I know first aid. Let me help!", CONFIDENT, POINT, MEDIUM) {
            move(PLAYER, 0.56f)
            outcome(0) { say(NARRATOR, "Your calm, careful help keeps him stable until the paramedics arrive.") }
            outcome(1) { say(NARRATOR, "You do what you can. The paramedics say you did well.") }
        }
        option(2, "You, call 911! You, bring a blanket!", EXCITED, POINT, MEDIUM) {
            say(NARRATOR, "Suddenly the street is a team, and everyone has a job.")
        }
        option(3, "Somebody else will help.", SAD, SHRUG) {
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "You keep walking. For the rest of the day, you can’t stop thinking about it.")
        }
    }

    scene("lat_old_friends", PARK, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        cam(ESTABLISHING)
        pause(500)
        enter(FRIEND, Edge.RIGHT, 0.64f)
        say(FRIEND, "Is this seat taken?", HAPPY, GENTLE_SMILE, MEDIUM)
        say(PLAYER, "For you? Never.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        sit(FRIEND)
        say(FRIEND, "The old group is meeting once more.", HAPPY, EXPLAIN)
        whenever(trustAtLeast(NpcRole.BEST_FRIEND, 70)) {
            say(FRIEND, "You and me, we’ve come a long way.", PROUD, GENTLE_SMILE, CLOSE_UP)
        }
        say(FRIEND, "Everyone’s coming. Are you?", EXCITED, NONE, TWO_SHOT)

        option(0, "I wouldn’t miss it. The whole week.", HAPPY, FIST_PUMP, MEDIUM) {
            say(FRIEND, "That’s the spirit!", EXCITED, CLAP)
        }
        option(1, "I’ll come for a day.", NEUTRAL, NOD) {
            say(FRIEND, "A day is plenty.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(2, "I’m comfortable at home.", TIRED, SHRUG) {
            say(FRIEND, "Hm. I’ll send photos.", SAD, NONE, CLOSE_UP)
        }
        option(3, "Come to my house. I’ll host everyone.", EXCITED, EXPLAIN) {
            say(FRIEND, "Now that’s a party.", LAUGHING, CLAP, MEDIUM)
        }
    }

    scene("ms_final_reflection", PARK, EVENING) {
        place(PLAYER, 0.46f, RIGHT, seated = true)
        cam(ESTABLISHING)
        cue("bells")
        say(NARRATOR, "You sit in the late afternoon light, and look back across the years.")
        cam(PUSH_IN, PLAYER)
        say(PLAYER, "So many choices. So many people.", THOUGHTFUL, NONE)
        say(PLAYER, "Funny. It all felt so small, back then.", HAPPY, GENTLE_SMILE)
        whenever(has("helped_bullied_friend")) { say(NARRATOR, "A boy or girl afraid to go to school, and a friend you chose to stand beside.") }
        whenever(has("business_failed")) { say(NARRATOR, "The business that failed, and the person you became afterwards.") }
        whenever(has("family_protected")) { say(NARRATOR, "The hard years when you held your family together.") }
        whenever(atMost(Stat.FAMILY, 40)) { say(NARRATOR, "The distance that grew between you and some of the people you love.") }
        act(PLAYER, NONE, THOUGHTFUL, 1400)

        option(0, "I’d do it all again.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            act(PLAYER, NONE, PROUD, 1800)
        }
        option(1, "I hope the young ones learn from it.", THOUGHTFUL, NOD) {
            say(NARRATOR, "One of them will carry your words for the rest of their life.")
        }
        option(2, "There are things I should say.", SAD, NONE, CLOSE_UP) {
            say(NARRATOR, "A few calls, a few letters. The weight you didn’t know you carried gets lighter.")
        }
        option(3, "Today, I’ll be with the people I love.", HAPPY, GENTLE_SMILE) {
            say(NARRATOR, "Meals, walks, laughter. The days are simple, and they’re enough.")
        }
    }
}
