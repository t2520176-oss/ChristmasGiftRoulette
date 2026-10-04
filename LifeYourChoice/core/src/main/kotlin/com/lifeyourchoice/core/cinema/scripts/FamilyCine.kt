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

/** Family: parents, money worries, expectations. */
val FamilyCine = cinePack("family") {

    scene("fam_money_trouble", KITCHEN, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FATHER, 0.70f, LEFT, seated = true)
        place(MOTHER, 0.90f, LEFT)
        cam(ESTABLISHING)
        pause(500)
        say(FATHER, "Sit down. We need to talk.", NEUTRAL, NONE, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 900)
        cam(TWO_SHOT, PLAYER, FATHER)
        say(PLAYER, "What happened?", WORRIED, NONE, OVER_SHOULDER)
        say(FATHER, "We’re having money problems.", SAD, HEAD_DOWN)
        say(PLAYER, "How bad?", WORRIED, NONE)
        act(FATHER, NONE, SAD, 900)
        look(FATHER, null)
        say(FATHER, "Bad enough that we can’t afford the school trip.", SAD, NONE, CLOSE_UP)
        say(MOTHER, "We’re so sorry.", SAD, HEAD_DOWN, REACTION)
        cam(TWO_SHOT, PLAYER, FATHER)

        option(0, "It’s okay. I’ll earn my own way.", CONFIDENT, NOD, MEDIUM) {
            act(FATHER, NONE, SURPRISED, 800)
            say(FATHER, "You don’t have to do that.", PROUD, GENTLE_SMILE, CLOSE_UP)
            say(MOTHER, "We’re proud of you.", PROUD, GENTLE_SMILE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1400)
        }
        option(1, "That’s not fair!", ANGRY, HANDS_UP, MEDIUM) {
            cam(CLOSE_UP, FATHER)
            act(FATHER, HEAD_DOWN, SAD, 1100)
            say(FATHER, "I know.", SAD, NONE)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1300)
            say(NARRATOR, "The words are out before you can take them back.")
        }
        option(2, "Could our relatives help?", THOUGHTFUL, NONE) {
            act(FATHER, NONE, EMBARRASSED, 900)
            say(FATHER, "…I’d rather not ask. But maybe.", EMBARRASSED, SHRUG, MEDIUM)
            say(NARRATOR, "An uncle quietly covers the cost. Your parents are embarrassed.")
        }
        option(3, "Forget the trip. It’s fine.", SAD, SHRUG) {
            say(MOTHER, "{name}…", SAD, NONE, REACTION)
            say(NARRATOR, "You skip the trip. You made it easier for them, and harder for you.")
        }
    }

    scene("fam_parents_arguing", LIVING_ROOM, NIGHT) {
        place(PLAYER, 0.18f, RIGHT)
        place(FATHER, 0.62f, LEFT)
        place(MOTHER, 0.84f, LEFT)
        cam(ESTABLISHING)
        say(MOTHER, "We can’t keep doing this.", ANGRY, EXPLAIN, TWO_SHOT)
        say(FATHER, "I’m trying. The bills…", ANGRY, HANDS_UP)
        say(MOTHER, "I know about the bills!", ANGRY, POINT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1200)
        cam(WIDE)

        option(0, "Mom? Dad? What’s wrong?", WORRIED, NONE, MEDIUM) {
            act(FATHER, NONE, EMBARRASSED, 800)
            say(FATHER, "Nothing, kiddo. Grown-up stuff.", EMBARRASSED, SHRUG, CLOSE_UP)
            say(MOTHER, "No. {name} should know.", SAD, GENTLE_SMILE)
        }
        option(1, "…", NEUTRAL, NONE) {
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "You put your headphones on. The tension is still there at breakfast.")
        }
        option(2, "I can help. Chores, pocket money…", CONFIDENT, NOD, MEDIUM) {
            act(FATHER, NONE, SURPRISED, 900)
            say(MOTHER, "Oh, sweetheart.", SAD, GENTLE_SMILE, CLOSE_UP)
            move(MOTHER, 0.30f)
            act(MOTHER, HUG, HAPPY, 1500)
        }
        option(3, "What’s going on with {sibling}?", THOUGHTFUL, NONE) {
            say(FATHER, "{sibling} knows. Talk to {sibling.him}.", NEUTRAL, NOD, MEDIUM)
        }
    }

    scene("fam_expectations", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        place(FATHER, 0.70f, LEFT, seated = true)
        cam(TWO_SHOT, PLAYER, FATHER)
        say(FATHER, "So. Have you thought about university?", NEUTRAL, EXPLAIN, MEDIUM)
        say(FATHER, "It’s what your mother and I always planned.", PROUD, GENTLE_SMILE)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(NARRATOR, "You’ve realised you want something different.")

        option(0, "Okay, Dad. I’ll do what you want.", SAD, NOD, MEDIUM) {
            say(FATHER, "Good. You’ll thank me.", PROUD, NONE, CLOSE_UP)
        }
        option(1, "It’s not what I want.", CONFIDENT, NONE, CLOSE_UP) {
            act(FATHER, NONE, SURPRISED, 900)
            say(FATHER, "…What do you want, then?", WORRIED, NONE, REACTION)
            outcome(0) { say(FATHER, "We just want you to be happy.", PROUD, GENTLE_SMILE, CLOSE_UP) }
            outcome(1) { act(FATHER, CROSS_ARMS, ANGRY, 1200); say(FATHER, "We’ll talk later.", ANGRY, NONE) }
        }
        option(2, "What if I try both?", THOUGHTFUL, EXPLAIN) {
            say(FATHER, "Both. Hm.", THOUGHTFUL, THINK, MEDIUM)
            say(FATHER, "Show me a plan.", NEUTRAL, NOD)
        }
        option(3, "Yes, I’ll apply.", NEUTRAL, NOD) {
            say(NARRATOR, "You nod, and keep your own plans to yourself.")
        }
    }

    scene("fam_dinner_missed", KITCHEN, EVENING) {
        place(MOTHER, 0.58f, LEFT)
        place(FATHER, 0.82f, LEFT, seated = true)
        cam(ESTABLISHING)
        say(MOTHER, "That’s the third Sunday this month.", DISAPPOINTED, NONE, MEDIUM)
        enter(PLAYER, Edge.LEFT, 0.30f)
        hold(PLAYER, Prop.BAG)
        say(PLAYER, "Sorry, sorry. Work ran late.", TIRED, SHRUG, TWO_SHOT)
        say(MOTHER, "We set a place for you every week.", SAD, GENTLE_SMILE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, EMBARRASSED, 1000)

        option(0, "I’m here now. Phone off.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            move(PLAYER, 0.52f)
            sit(PLAYER)
            say(MOTHER, "Good. Sit. Eat.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(FATHER, "Tell us everything.", HAPPY, EXPLAIN)
        }
        option(1, "I’ll send something to make up for it.", NEUTRAL, SHRUG) {
            say(MOTHER, "We don’t want gifts.", SAD, SHAKE_HEAD, CLOSE_UP)
        }
        option(2, "Work won’t wait. Next time.", TIRED, SHRUG) {
            say(MOTHER, "Next time.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "The promotion is a step closer. So is the empty chair.")
        }
        option(3, "Come to my place next Sunday.", HAPPY, GENTLE_SMILE) {
            act(MOTHER, NONE, SURPRISED, 800)
            say(MOTHER, "Your place? Truly?", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }
}
