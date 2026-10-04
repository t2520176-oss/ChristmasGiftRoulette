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

/** Friendship: loyalty, jealousy, a friend at the door. */
val FriendCine = cinePack("friendship") {

    scene("fri_cover_for_me", HALLWAY) {
        place(PLAYER, 0.30f, RIGHT)
        cam(WIDE)
        enter(FRIEND, Edge.RIGHT, 0.64f, run = true)
        act(FRIEND, NONE, AFRAID, 500)
        say(FRIEND, "{name}! I’m in so much trouble.", AFRAID, HANDS_UP, MEDIUM)
        say(FRIEND, "I missed the deadline. Tell Mr. Hale I was helping you.", WORRIED, EXPLAIN, TWO_SHOT)
        say(FRIEND, "Please. You owe me one.", WORRIED, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Fine. I’ll say you were with me.", WORRIED, NOD, MEDIUM) {
            act(FRIEND, NONE, HAPPY, 900)
            say(FRIEND, "You’re the best. I’ll never forget this.", HAPPY, HUG, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, WORRIED, 1200)
        }
        option(1, "I won’t lie. But I’ll help you explain.", CONFIDENT, NONE, MEDIUM) {
            say(FRIEND, "That’s… actually better.", THOUGHTFUL, NOD)
        }
        option(2, "No. That’s on you.", NEUTRAL, SHAKE_HEAD) {
            say(FRIEND, "Wow. Okay.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
        }
        option(3, "Come on. We’ll finish it together.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "Really? Tonight?", EXCITED, FIST_PUMP)
            say(PLAYER, "Library. Six o’clock.", HAPPY, NOD)
        }
    }

    scene("fri_jealousy", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FRIEND, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        cam(TWO_SHOT, PLAYER, FRIEND)
        say(FRIEND, "I got it! I actually got it!", EXCITED, FIST_PUMP, MEDIUM)
        say(FRIEND, "The one we both wanted. Can you believe it?", HAPPY, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SAD, 900)
        say(NARRATOR, "You smile. Something inside you twists.")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "That’s amazing! You deserve it.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(FRIEND, "Thanks. It means a lot, coming from you.", HAPPY, HUG, CLOSE_UP)
        }
        option(1, "Hm. Good for you.", SAD, NONE) {
            say(FRIEND, "…Are you okay?", WORRIED, NONE, REACTION)
            say(NARRATOR, "The distance between you feels like a wall.")
        }
        option(2, "I’ll get the next one.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(FRIEND, "I bet you will.", HAPPY, NOD)
        }
        option(3, "Wonder how you pulled that off.", ANGRY, SHRUG, CLOSE_UP) {
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "What is that supposed to mean?", ANGRY, NONE, REACTION)
            say(NARRATOR, "You can’t unsay it. Not tonight.")
        }
    }

    scene("fri_friend_needs_help", APARTMENT, NIGHT) {
        place(PLAYER, 0.34f, RIGHT)
        cam(ESTABLISHING)
        cue("knock")
        pause(700)
        enter(FRIEND, Edge.RIGHT, 0.68f)
        hold(FRIEND, Prop.BAG)
        act(FRIEND, HEAD_DOWN, TIRED, 500)
        say(FRIEND, "Sorry. I know it’s late.", TIRED, NONE, MEDIUM)
        say(FRIEND, "I lost my job. And my place.", SAD, HEAD_DOWN, CLOSE_UP)
        say(FRIEND, "Could I stay a few weeks?", SAD, NONE)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Of course. Take the couch.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(FRIEND, NONE, HAPPY, 900)
            say(FRIEND, "I won’t forget this. Ever.", HAPPY, HUG, CLOSE_UP)
        }
        option(1, "Let’s find you a place. I’ll cover week one.", CONFIDENT, NOD) {
            say(FRIEND, "You don’t have to.", EMBARRASSED, NONE)
            say(PLAYER, "I know.", CONFIDENT, GENTLE_SMILE, MEDIUM)
        }
        option(2, "I’m sorry. I really don’t have the space.", SAD, SHRUG) {
            say(FRIEND, "No, no. I understand.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1300)
        }
        option(3, "Yes. But we set some rules.", NEUTRAL, EXPLAIN, MEDIUM) {
            say(FRIEND, "Deal. Rule one: I do dishes.", HAPPY, GENTLE_SMILE)
        }
    }

    scene("fri_apology", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        cam(ESTABLISHING)
        enter(FRIEND, Edge.RIGHT, 0.74f)
        act(FRIEND, NONE, SURPRISED, 600)
        say(FRIEND, "{name}?", SURPRISED, NONE, MEDIUM)
        act(FRIEND, NONE, DISAPPOINTED, 700)
        turn(FRIEND, RIGHT)
        cam(WIDE)
        pause(500)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1000)
        say(PLAYER, "Years. It’s been years.", WORRIED, NONE, CLOSE_UP)
        say(NARRATOR, "Years of silence. And a moment to choose.")

        option(0, "{friend}, wait. I’m sorry. I truly am.", SAD, HANDS_UP, CLOSE_UP) {
            turn(FRIEND, LEFT)
            say(FRIEND, "…", NEUTRAL, NONE, REACTION)
            move(FRIEND, 0.56f)
            sit(FRIEND)
            say(FRIEND, "Took you long enough.", EMBARRASSED, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "…", NEUTRAL, NONE) {
            exit(FRIEND, Edge.RIGHT)
            say(NARRATOR, "The moment passes. It will come back to you later.")
        }
        option(2, "It wasn’t entirely my fault.", ANGRY, EXPLAIN) {
            say(FRIEND, "I should have known.", ANGRY, SHAKE_HEAD, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
        }
        option(3, "I’ll write to you. Properly.", THOUGHTFUL, NOD) {
            say(FRIEND, "…Fine. Write.", NEUTRAL, NOD, MEDIUM)
            exit(FRIEND, Edge.RIGHT)
        }
    }
}
