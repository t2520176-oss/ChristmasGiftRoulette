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

/** Family and love scenes: new babies, dates, weddings, ageing parents, and the people we come home to. */
val HomeLoveCine = cinePack("homelove") {

    // ------------------------------------------------------------------ the first steps of a family

    scene("fam_new_baby", NEW_HOME, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(PARTNER, 0.62f, LEFT, seated = true)
        hold(PARTNER, Prop.CUP)
        cam(ESTABLISHING)
        pause(600)
        say(PARTNER, "I’ve been thinking about something.", THOUGHTFUL, NONE, MEDIUM)
        say(PARTNER, "Do you ever picture a little one running around here?", HAPPY, GENTLE_SMILE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(PLAYER, "Sometimes. Honestly? Quite often.", EMBARRASSED, SCRATCH_HEAD, CLOSE_UP)
        say(PARTNER, "It’s a big step. Money, sleep, freedom…", WORRIED, EXPLAIN, TWO_SHOT)
        say(PARTNER, "I want us to decide this together.", HAPPY, NONE, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, PARTNER)

        option(0, "Yes. I think we’re both ready.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            hold(PARTNER, Prop.NONE)
            act(PARTNER, HANDS_UP, SURPRISED, 800)
            say(PARTNER, "Really? You mean it?", EXCITED, NONE, REACTION)
            stand(PLAYER)
            stand(PARTNER)
            move(PLAYER, 0.50f)
            act(PLAYER, HUG, HAPPY, 1600)
            card("NINE MONTHS LATER", kind = TitleKind.CAPTION)
            cam(WIDE)
            act(PLAYER, NONE, TIRED, 900)
            say(PARTNER, "Your turn. Someone’s awake again.", TIRED, GENTLE_SMILE, MEDIUM)
            say(PLAYER, "On it. Best sleepless nights ever.", TIRED, GENTLE_SMILE, CLOSE_UP)
        }
        option(1, "Let’s wait a few years. Build some stability.", NEUTRAL, EXPLAIN, MEDIUM) {
            act(PARTNER, HEAD_DOWN, SAD, 900)
            say(PARTNER, "That’s sensible. Really.", NEUTRAL, NOD, CLOSE_UP)
            say(PARTNER, "I just felt a little wistful.", SAD, GENTLE_SMILE)
            cam(TWO_SHOT, PLAYER, PARTNER)
            say(PLAYER, "We’ll get there. I promise.", HAPPY, GENTLE_SMILE)
        }
        option(2, "Honestly? I don’t think a child is for me.", SAD, SHRUG, CLOSE_UP) {
            act(PARTNER, NONE, SURPRISED, 900)
            say(PARTNER, "…", THOUGHTFUL, NONE, REACTION)
            say(PARTNER, "Thank you for being honest with me.", THOUGHTFUL, NOD, CLOSE_UP)
            say(PARTNER, "Then we pour everything into each other.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(3, "What about adoption? Or fostering?", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "A child who needs a home…", HAPPY, NONE, REACTION)
            say(PARTNER, "I’d love that. Truly.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            stand(PLAYER)
            stand(PARTNER)
            card("MONTHS OF PAPERWORK LATER", kind = TitleKind.CAPTION)
            cam(WIDE)
            cue("door")
            enter(CHILD, Edge.RIGHT, 0.86f)
            hold(CHILD, Prop.BAG)
            say(PARTNER, "Come in. Don’t be shy.", HAPPY, WAVE, MEDIUM)
            say(CHILD, "Is this… really my room?", WORRIED, NONE, CLOSE_UP)
            say(PLAYER, "Every bit of it. Welcome home.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
    }

    scene("lov_first_date", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(PARTNER, 0.64f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(PARTNER, Prop.CUP)
        cam(ESTABLISHING)
        pause(500)
        say(PARTNER, "So. Saturday. Where are you taking me?", HAPPY, GENTLE_SMILE, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, EMBARRASSED, 900)
        say(PLAYER, "Saturday. Yes. Right. Plans.", EMBARRASSED, SCRATCH_HEAD, CLOSE_UP)
        say(NARRATOR, "Your wallet is nearly empty. Your stomach is doing somersaults.")
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "Surprise me. I trust you.", HAPPY, SHRUG, CLOSE_UP)
        act(PLAYER, NONE, AFRAID, 900)

        option(0, "A walk, and a home-made picnic.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(PARTNER, CLAP, HAPPY, 900)
            say(PARTNER, "A picnic? I love that. Simple and real.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            cam(TWO_SHOT, PLAYER, PARTNER)
            act(PLAYER, NONE, HAPPY, 1000)
        }
        option(1, "I’ll book the fanciest place in town.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Somewhere fancy? Are you sure?", SURPRISED, NONE, REACTION)
            say(PLAYER, "No, no. Totally fine!", WORRIED, SCRATCH_HEAD, MEDIUM)
            cam(CLOSE_UP, PARTNER)
            act(PARTNER, NONE, WORRIED, 1000)
            say(PARTNER, "You’re counting every coin, aren’t you?", WORRIED, GENTLE_SMILE)
        }
        option(2, "Let’s try a class or a sport together.", EXCITED, EXPLAIN, MEDIUM) {
            act(PARTNER, NONE, SURPRISED, 700)
            say(PARTNER, "Ooh. Climbing? Dancing?", EXCITED, HANDS_UP, MEDIUM)
            say(PLAYER, "Whatever we’re both terrible at.", LAUGHING, SHRUG, TWO_SHOT)
            say(PARTNER, "You’re on. Loser buys the drinks.", LAUGHING, FIST_PUMP, CLOSE_UP)
        }
        option(3, "I… can’t. I’m too nervous. Sorry.", SAD, SHRUG, CLOSE_UP) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Oh. Okay.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            stand(PARTNER)
            exit(PARTNER, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, FACEPALM, EMBARRASSED, 1200)
            say(NARRATOR, "You’ll have to find a way to apologise.")
        }
    }

    scene("lov_meet_someone", COFFEE_SHOP, DAY) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(PARTNER, 0.64f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(PARTNER, Prop.CUP)
        cam(ESTABLISHING)
        say(NARRATOR, "A rainy afternoon. A crowded coffee shop. A stranger at the next table.")
        say(PARTNER, "You laughed at the same joke I did. Twice.", HAPPY, GENTLE_SMILE, MEDIUM)
        say(PLAYER, "Great taste in jokes, clearly.", LAUGHING, SHRUG, TWO_SHOT)
        say(PARTNER, "Clearly.", LAUGHING, NONE, CLOSE_UP)
        act(PARTNER, NONE, EMBARRASSED, 900)
        say(PARTNER, "I wrote my number on your cup.", EMBARRASSED, SHRUG, MEDIUM)
        say(PARTNER, "No pressure. Just… in case.", EMBARRASSED, GENTLE_SMILE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)
        cam(TWO_SHOT, PLAYER, PARTNER)

        option(0, "I’ll call you tonight. I promise.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            act(PARTNER, NONE, HAPPY, 800)
            say(PARTNER, "Good. I’ll be waiting.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            stand(PARTNER)
            exit(PARTNER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, EXCITED, 1300)
        }
        option(1, "Let’s be friends first. Same time next week?", HAPPY, NOD, MEDIUM) {
            say(PARTNER, "Friends first. I like that.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(PARTNER, "Same table, same time.", HAPPY, NOD, TWO_SHOT)
        }
        option(2, "That’s kind, but work is my focus right now.", NEUTRAL, SHRUG) {
            act(PARTNER, NONE, SURPRISED, 700)
            say(PARTNER, "Oh. Of course. No worries.", DISAPPOINTED, SHRUG, CLOSE_UP)
            stand(PARTNER)
            exit(PARTNER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1300)
            say(NARRATOR, "You keep the cup. You never make the call.")
        }
        option(3, "Hold on, I’m texting {friend} first.", EMBARRASSED, SCRATCH_HEAD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(PARTNER, "You’re asking your best friend already?", LAUGHING, NONE, TWO_SHOT)
            cue("phone")
            voice(FRIEND, "Why are you even asking me? Call!", LAUGHING)
            hold(PLAYER, Prop.CUP)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 900)
            say(PLAYER, "…Okay. Are you free on Saturday?", HAPPY, SCRATCH_HEAD)
            cam(TWO_SHOT, PLAYER, PARTNER)
            say(PARTNER, "I thought you’d never ask.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    scene("lov_wedding", WEDDING, DAY) {
        place(PLAYER, 0.32f, RIGHT)
        place(PARTNER, 0.54f, LEFT)
        place(MOTHER, 0.86f, LEFT)
        hold(PLAYER, Prop.FLOWERS)
        cam(ESTABLISHING)
        card("THE BIG DAY", kind = TitleKind.CAPTION, ms = 1800)
        cue("bells")
        say(NARRATOR, "The guest list got long. The budget got short. And now, here you are.")
        say(PARTNER, "Deep breath. We’re really doing this.", EXCITED, NONE, TWO_SHOT)
        say(PLAYER, "My heart is going at double speed.", AFRAID, NONE, CLOSE_UP)
        say(PARTNER, "Mine too. Hold my hand?", HAPPY, GENTLE_SMILE, MEDIUM)
        cam(WIDE)
        say(MOTHER, "Everyone’s seated. They’re all waiting.", HAPPY, CLAP, MEDIUM)
        say(PARTNER, "Last chance to change the plan, you know.", LAUGHING, POINT, TWO_SHOT)

        option(0, "Keep it small. Just the people we love.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            say(PARTNER, "Small and meaningful. Perfect.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            cam(WIDE)
            act(MOTHER, CLAP, HAPPY, 1200)
            cue("cheer")
            say(NARRATOR, "Thirty people, one beautiful afternoon. Nobody remembers what the flowers cost.")
        }
        option(1, "Go big! Let’s throw the party of the decade!", EXCITED, FIST_PUMP, MEDIUM) {
            act(PARTNER, NONE, LAUGHING, 800)
            say(PARTNER, "The party of the decade! Bill? What bill?", LAUGHING, FIST_PUMP, MEDIUM)
            cue("cheer")
            act(MOTHER, CLAP, EXCITED, 1200)
            say(NARRATOR, "An unforgettable night, and credit card statements to match.")
        }
        option(2, "Forget the crowd. Let’s elope!", EXCITED, POINT, MEDIUM) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Elope? Right now? Just us?", SURPRISED, HANDS_UP, REACTION)
            say(MOTHER, "Elope?! Oh my goodness.", SURPRISED, HANDS_UP, WIDE)
            say(PLAYER, "A few witnesses. That’s all we need.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(PARTNER, "Then run!", LAUGHING, NONE)
            cue("whoosh")
            exit(PARTNER, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
            cam(CLOSE_UP, MOTHER)
            say(MOTHER, "Well. They’re happy. I’m stunned, but happy.", HAPPY, SHRUG)
        }
        option(3, "A small ceremony at sunset, then off on a trip.", EXCITED, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.SUITCASE)
            act(PARTNER, NONE, SURPRISED, 700)
            say(PARTNER, "A trip? Where?", EXCITED, HANDS_UP, REACTION)
            say(PLAYER, "It’s a surprise. I’ve already packed.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(NARRATOR, "You say your vows at sunset, and spend the next week exploring a new place together.")
        }
    }

    // ------------------------------------------------------------------ brothers, sisters and parents

    scene("fam_sibling_conflict", LIVING_ROOM, DAY) {
        place(PLAYER, 0.30f, RIGHT)
        hold(PLAYER, Prop.BOX)
        cam(ESTABLISHING)
        act(PLAYER, NONE, ANGRY, 700)
        enter(SIBLING, Edge.RIGHT, 0.66f)
        cam(TWO_SHOT, PLAYER, SIBLING)
        say(PLAYER, "{sibling}. Explain this.", ANGRY, POINT, MEDIUM)
        say(SIBLING, "Okay, okay. I was going to tell you.", EMBARRASSED, HANDS_UP)
        say(PLAYER, "It’s broken! You know how much this meant to me!", ANGRY, EXPLAIN, CLOSE_UP)
        say(SIBLING, "It was an accident! I didn’t mean to!", ANGRY, HANDS_UP, REACTION)
        cam(TWO_SHOT, PLAYER, SIBLING)

        option(0, "You’re paying for this. Every cent!", ANGRY, POINT, MEDIUM) {
            say(SIBLING, "Fine! Send me the bill!", ANGRY, HANDS_UP, REACTION)
            cue("door")
            exit(SIBLING, Edge.RIGHT, run = true)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, ANGRY, 900)
            act(PLAYER, HEAD_DOWN, SAD, 1000)
            say(NARRATOR, "A shouting match. Neither of you says sorry.")
        }
        option(1, "I’m upset. Let me explain why it matters.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(SIBLING, NONE, SURPRISED, 800)
            say(SIBLING, "…Okay. I’m listening.", WORRIED, NONE, REACTION)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, EXPLAIN, SAD, 1300)
            act(SIBLING, HEAD_DOWN, SAD, 1000)
            say(SIBLING, "I didn’t know it meant that much. I’m sorry.", SAD, NONE, CLOSE_UP)
            say(SIBLING, "Let me make it up to you.", HAPPY, GENTLE_SMILE)
            cam(TWO_SHOT, PLAYER, SIBLING)
            say(PLAYER, "Thank you. That’s all I wanted.", HAPPY, GENTLE_SMILE)
        }
        option(2, "…It’s just an object. Forget it.", SAD, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, SAD, 900)
            say(SIBLING, "Really? You’re not mad?", SURPRISED, NONE, REACTION)
            say(PLAYER, "I am. A little. But you matter more.", SAD, GENTLE_SMILE, CLOSE_UP)
            say(SIBLING, "Thanks. I owe you one.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(3, "Let’s ask Mom and Dad to sort this out.", NEUTRAL, SHRUG, MEDIUM) {
            say(SIBLING, "Fine. Let’s ask.", ANGRY, CROSS_ARMS, MEDIUM)
            enter(MOTHER, Edge.RIGHT, 0.88f)
            say(MOTHER, "What is all this shouting about?", ANGRY, HANDS_UP, WIDE)
            say(PLAYER, "{sibling} broke my—", ANGRY, POINT)
            say(SIBLING, "It was an accident—", ANGRY, HANDS_UP)
            say(MOTHER, "One at a time! And lower your voices.", ANGRY, POINT, MEDIUM)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 900)
            act(SIBLING, HEAD_DOWN, EMBARRASSED, 900)
            say(NARRATOR, "It’s sorted out sensibly. You both feel slightly treated like kids.")
        }
    }

    scene("fam_help_parents", KITCHEN, DAY) {
        place(MOTHER, 0.68f, LEFT)
        hold(MOTHER, Prop.DOCUMENTS)
        place(PLAYER, 0.32f, RIGHT)
        hold(PLAYER, Prop.SUITCASE)
        cam(ESTABLISHING)
        act(MOTHER, NONE, TIRED, 600)
        say(MOTHER, "Sweetheart, can I ask you something?", TIRED, NONE, MEDIUM)
        say(MOTHER, "Groceries, dinner, picking up {sibling}…", TIRED, EXPLAIN, TWO_SHOT)
        say(MOTHER, "Your father and I can’t manage it all this week.", SAD, HEAD_DOWN, CLOSE_UP)
        cue("phone")
        voice(FRIEND, "Bus leaves at six! You’re coming, right?", EXCITED)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        cam(TWO_SHOT, PLAYER, MOTHER)

        option(0, "I’ll stay. You can count on me.", CONFIDENT, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            move(PLAYER, 0.52f)
            act(MOTHER, NONE, SURPRISED, 700)
            hold(MOTHER, Prop.NONE)
            hold(PLAYER, Prop.DOCUMENTS)
            say(MOTHER, "…Thank you.", SAD, GENTLE_SMILE, CLOSE_UP)
            cam(WIDE)
            say(NARRATOR, "The weekend is quiet and tiring. Your parents look at you differently.")
        }
        option(1, "Sorry, Mom. I’m going. I’ll make it up later.", EMBARRASSED, SHRUG, MEDIUM) {
            act(MOTHER, HEAD_DOWN, SAD, 900)
            say(MOTHER, "…Go. Have fun.", SAD, GENTLE_SMILE, CLOSE_UP)
            cue("door")
            exit(PLAYER, Edge.LEFT, run = true)
            cam(CLOSE_UP, MOTHER)
            act(MOTHER, HEAD_DOWN, TIRED, 1400)
            say(NARRATOR, "When you get back, the house is tidy and quiet, and nobody mentions it.")
        }
        option(2, "Chores first. Then I’ll catch up with them.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(MOTHER, "Oh, would you? Bless you.", HAPPY, CLAP, CLOSE_UP)
            hold(PLAYER, Prop.BAG)
            move(PLAYER, 0.16f, run = true)
            move(PLAYER, 0.46f, run = true)
            card("SIX EXHAUSTING HOURS LATER", kind = TitleKind.CAPTION)
            hold(PLAYER, Prop.SUITCASE)
            cam(MEDIUM, PLAYER)
            act(PLAYER, NONE, TIRED, 1200)
            say(PLAYER, "Done. Now, where’s that bus?", TIRED, FIST_PUMP, CLOSE_UP)
        }
        option(3, "I’ll help, if {sibling} pitches in too.", NEUTRAL, EXPLAIN, MEDIUM) {
            say(MOTHER, "That’s only fair.", NEUTRAL, NOD, MEDIUM)
            enter(SIBLING, Edge.RIGHT, 0.88f)
            say(SIBLING, "Pitch in with what?", WORRIED, SHRUG, WIDE)
            say(PLAYER, "Groceries for me. Pickups for you.", CONFIDENT, POINT, TWO_SHOT)
            say(SIBLING, "…Fine. But you owe me.", EMBARRASSED, SHRUG)
            act(SIBLING, HANDSHAKE, HAPPY, 1000)
            say(NARRATOR, "For the first time, the two of you have really worked as a team.")
        }
    }

    scene("fam_hospital_vigil", HOSPITAL, NIGHT) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(DOCTOR, 0.70f, LEFT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        act(PLAYER, NONE, TIRED, 900)
        say(NARRATOR, "Another long night in the hospital corridor.")
        cam(TWO_SHOT, PLAYER, DOCTOR)
        say(DOCTOR, "The recovery will be long, {name}. Weeks, maybe months.", NEUTRAL, EXPLAIN, MEDIUM)
        say(DOCTOR, "Having family close makes a real difference.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "It’s a long drive. And work won’t wait.", TIRED, SHRUG, MEDIUM)
        say(DOCTOR, "I understand. Nobody would blame you.", NEUTRAL, NOD, TWO_SHOT)

        option(0, "I’ll be here every day.", CONFIDENT, NOD, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            say(DOCTOR, "Then I’ll tell the nurses to expect you.", HAPPY, GENTLE_SMILE, MEDIUM)
            card("WEEKS LATER", kind = TitleKind.CAPTION)
            cam(WIDE)
            cue("steps")
            say(DOCTOR, "Good news, {name}. Your relative woke up asking for you.", HAPPY, GENTLE_SMILE, MEDIUM)
            stand(PLAYER)
            act(PLAYER, NONE, HAPPY, 1200)
            say(PLAYER, "Can I see them?", EXCITED, NONE, CLOSE_UP)
        }
        option(1, "Weekends in person, and a call every day.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(DOCTOR, "A balanced plan. Being present is what matters.", NEUTRAL, NOD, MEDIUM)
            say(PLAYER, "I’ll do everything I can.", WORRIED, NOD, CLOSE_UP)
            hold(PLAYER, Prop.CUP)
        }
        option(2, "I’ll send flowers, and keep up by message.", EMBARRASSED, SHRUG, MEDIUM) {
            stand(PLAYER)
            hold(PLAYER, Prop.FLOWERS)
            say(PLAYER, "Could you give them these, please?", EMBARRASSED, NONE, MEDIUM)
            hold(PLAYER, Prop.NONE)
            hold(DOCTOR, Prop.FLOWERS)
            act(DOCTOR, NONE, DISAPPOINTED, 900)
            say(DOCTOR, "Of course. They’ll appreciate it.", NEUTRAL, NOD, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1300)
            say(NARRATOR, "A card with your name on it isn’t the same as you.")
        }
        option(3, "I’ll organise a visiting rota for the family.", CONFIDENT, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.DOCUMENTS)
            say(DOCTOR, "That would help us enormously.", HAPPY, NOD, MEDIUM)
            say(PLAYER, "A rota. Everyone gets a day, nobody is alone.", CONFIDENT, EXPLAIN, TWO_SHOT)
            say(DOCTOR, "Without you, this would be chaos.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    scene("lov_breakup", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(PARTNER, 0.64f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(PARTNER, Prop.CUP)
        cam(ESTABLISHING)
        pause(600)
        say(NARRATOR, "You used to talk for hours. Now the silences are longer than the sentences.")
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "We used to laugh so much, didn’t we?", SAD, GENTLE_SMILE, MEDIUM)
        say(PLAYER, "We did.", SAD, NONE, REACTION)
        say(PARTNER, "Lately it’s arguments. Or nothing at all.", SAD, HEAD_DOWN, CLOSE_UP)
        say(PARTNER, "Do we still want the same things?", WORRIED, NONE)
        cam(REACTION, PLAYER)
        act(PLAYER, HEAD_DOWN, SAD, 1200)
        cam(TWO_SHOT, PLAYER, PARTNER)

        option(0, "Let’s be honest about where we stand.", THOUGHTFUL, EXPLAIN, CLOSE_UP) {
            say(PARTNER, "Honest. Okay.", WORRIED, NOD, CLOSE_UP)
            outcome(0) {
                say(PARTNER, "I don’t want to lose this. Do you?", SAD, GENTLE_SMILE, CLOSE_UP)
                cam(TWO_SHOT, PLAYER, PARTNER)
                say(PLAYER, "No. Let’s start again. Differently.", HAPPY, GENTLE_SMILE)
                act(PARTNER, GENTLE_SMILE, HAPPY, 1400)
            }
            outcome(1) {
                say(PARTNER, "I think we both know.", SAD, NONE, CLOSE_UP)
                say(PARTNER, "Thank you. For everything.", SAD, GENTLE_SMILE)
                stand(PARTNER)
                exit(PARTNER, Edge.RIGHT)
                cam(CLOSE_UP, PLAYER)
                act(PLAYER, HEAD_DOWN, SAD, 1500)
                say(NARRATOR, "You end it kindly, with tears and thanks. It hurts, but it’s clean.")
            }
        }
        option(1, "I care about you. But I think we should end it.", SAD, NONE, CLOSE_UP) {
            act(PARTNER, HEAD_DOWN, SAD, 1000)
            say(PARTNER, "…Thank you for being honest.", SAD, GENTLE_SMILE, CLOSE_UP)
            stand(PARTNER)
            exit(PARTNER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1500)
        }
        option(2, "It’s nothing. We’re fine.", NEUTRAL, SHRUG, MEDIUM) {
            say(PARTNER, "…Are we?", SAD, NONE, CLOSE_UP)
            cam(REACTION, PLAYER)
            look(PLAYER, null)
            act(PLAYER, NONE, EMBARRASSED, 1200)
            say(NARRATOR, "The silence grows heavier. Eventually, something will have to give.")
        }
        option(3, "Maybe we need a break. Time to think.", THOUGHTFUL, NONE, CLOSE_UP) {
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "A break. …Maybe that’s fair.", THOUGHTFUL, NOD, CLOSE_UP)
            stand(PARTNER)
            say(PARTNER, "Call me when you know.", SAD, GENTLE_SMILE, MEDIUM)
            exit(PARTNER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1400)
        }
    }

    scene("fam_moving_house", NEW_HOME, DAY) {
        place(PLAYER, 0.32f, RIGHT)
        place(FATHER, 0.70f, LEFT)
        place(MOTHER, 0.88f, LEFT)
        hold(PLAYER, Prop.BOX)
        hold(FATHER, Prop.BOX)
        cam(ESTABLISHING)
        cue("door")
        say(FATHER, "That’s the last box. Welcome to our new home!", HAPPY, GENTLE_SMILE, MEDIUM)
        say(MOTHER, "I know everything feels strange right now.", WORRIED, NONE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SAD, 1100)
        say(PLAYER, "New school. New streets. And {friend} an hour away.", SAD, SHRUG, CLOSE_UP)
        say(MOTHER, "It will get easier. I promise.", HAPPY, GENTLE_SMILE, WIDE)

        option(0, "I’ll join a club at my new school this week.", CONFIDENT, NOD, MEDIUM) {
            say(FATHER, "That takes courage.", PROUD, NOD, MEDIUM)
            hold(PLAYER, Prop.BAG)
            card("ONE TERM LATER", kind = TitleKind.CAPTION)
            cam(WIDE)
            cue("cheer")
            act(PLAYER, NONE, HAPPY, 800)
            say(PLAYER, "Mom, I’m late. It’s club night!", EXCITED, WAVE, MEDIUM)
            exit(PLAYER, Edge.LEFT, run = true)
            say(MOTHER, "Look at that. A whole new group of friends.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(1, "I’ll call {friend} every single night.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            voice(FRIEND, "Is that you? Show me your new room!", EXCITED)
            say(PLAYER, "It’s all boxes. Come back in a week.", LAUGHING, SHRUG, CLOSE_UP)
            cam(WIDE)
            say(NARRATOR, "Late-night calls keep the bond alive, though the new neighbourhood stays a bit foreign.")
        }
        option(2, "I’ll be in my room. I don’t want to talk.", SAD, SHRUG) {
            act(MOTHER, NONE, WORRIED, 800)
            say(MOTHER, "{name}…", SAD, NONE, REACTION)
            hold(PLAYER, Prop.NONE)
            exit(PLAYER, Edge.LEFT)
            cam(CLOSE_UP, MOTHER)
            act(MOTHER, HEAD_DOWN, WORRIED, 1000)
            say(NARRATOR, "The months crawl. You feel invisible, and a bit sorry for yourself.")
        }
        option(3, "Let me help. Where does this box go?", HAPPY, NOD, MEDIUM) {
            move(PLAYER, 0.54f)
            say(FATHER, "Kitchen. Careful, it’s the plates!", HAPPY, POINT, MEDIUM)
            say(MOTHER, "You’re a lifesaver, {name}.", HAPPY, CLAP, TWO_SHOT)
            say(NARRATOR, "The house starts to feel like home faster than you expected, thanks partly to you.")
        }
    }

    scene("fam_reunion", PARK, DAY) {
        place(PLAYER, 0.30f, RIGHT)
        place(MOTHER, 0.52f, LEFT)
        place(STRANGER, 0.90f, LEFT, seated = true)
        cam(ESTABLISHING)
        cue("cheer")
        say(MOTHER, "Look at everyone. It’s so good to see them all.", HAPPY, GENTLE_SMILE, MEDIUM)
        look(MOTHER, STRANGER)
        act(MOTHER, NONE, WORRIED, 900)
        say(MOTHER, "Except… you know who.", SAD, NONE, CLOSE_UP)
        cam(WIDE)
        say(NARRATOR, "A relative sits alone on a bench. Years of silence stand between you.")
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        say(MOTHER, "Everyone’s waiting for someone to break the ice.", WORRIED, SHRUG, TWO_SHOT)

        option(0, "I’ll go over there. I’ll make the first move.", CONFIDENT, NOD, MEDIUM) {
            move(PLAYER, 0.74f)
            cam(TWO_SHOT, PLAYER, STRANGER)
            act(STRANGER, NONE, SURPRISED, 900)
            stand(STRANGER)
            say(PLAYER, "Hi. It’s been far too long.", WORRIED, GENTLE_SMILE, MEDIUM)
            act(STRANGER, HEAD_DOWN, EMBARRASSED, 1000)
            say(PLAYER, "I’m sorry it took me this long.", SAD, NONE, CLOSE_UP)
            act(STRANGER, NOD, HAPPY, 1000)
            say(NARRATOR, "It’s awkward for ten minutes. Then it gets easier.")
            cam(TWO_SHOT, PLAYER, STRANGER)
            act(PLAYER, HANDSHAKE, HAPPY, 1400)
            cam(REACTION, MOTHER)
            act(MOTHER, GENTLE_SMILE, HAPPY, 1300)
        }
        option(1, "I’ll say hello to everyone. Keep it polite.", NEUTRAL, NOD, MEDIUM) {
            say(MOTHER, "Polite is fine. It’s a start.", NEUTRAL, NOD, CLOSE_UP)
            cam(WIDE)
            say(NARRATOR, "Smiles all round, nothing resolved. It’s fine.")
        }
        option(2, "Sorry, I can’t stay. Too much on.", TIRED, SHRUG) {
            act(MOTHER, HEAD_DOWN, SAD, 900)
            say(MOTHER, "Oh. Of course. Work.", SAD, GENTLE_SMILE, CLOSE_UP)
            exit(PLAYER, Edge.LEFT)
            cam(CLOSE_UP, MOTHER)
            act(MOTHER, NONE, DISAPPOINTED, 1000)
            say(NARRATOR, "Photos arrive later. You feel like a guest in your own family.")
        }
        option(3, "How about a game day? Water balloons, anyone?", EXCITED, POINT, MEDIUM) {
            act(MOTHER, NONE, SURPRISED, 800)
            say(MOTHER, "Water balloons? Oh, no.", LAUGHING, HANDS_UP, MEDIUM)
            enter(SIBLING, Edge.LEFT, 0.14f)
            say(SIBLING, "Water balloons? I’m in!", EXCITED, FIST_PUMP, WIDE)
            stand(STRANGER)
            move(STRANGER, 0.70f)
            cue("cheer")
            say(NARRATOR, "A water-balloon fight dissolves a decade of tension. Nobody mentions it, and nobody has to.")
        }
    }

    scene("fam_parent_ageing", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.30f, RIGHT)
        place(FATHER, 0.78f, LEFT, seated = true)
        cam(ESTABLISHING)
        enter(MOTHER, Edge.RIGHT, 0.58f)
        hold(MOTHER, Prop.BAG)
        act(MOTHER, NONE, TIRED, 900)
        say(MOTHER, "Don’t fuss, I’m fine. Just a little out of breath.", TIRED, SHRUG, MEDIUM)
        say(FATHER, "The stairs aren’t what they used to be.", TIRED, GENTLE_SMILE, TWO_SHOT)
        say(FATHER, "We manage. We always manage.", NEUTRAL, NOD)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        say(NARRATOR, "They’d never ask. But the stairs, the shopping and the pills are becoming too much.")
        whenever(has("ignored_parents")) {
            say(NARRATOR, "You think of every visit you promised, and didn’t make.")
        }
        cam(TWO_SHOT, PLAYER, MOTHER)

        option(0, "Come and live with us. There’s plenty of room.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(MOTHER, NONE, SURPRISED, 800)
            say(MOTHER, "Oh, we couldn’t. We’d be in the way.", EMBARRASSED, SHRUG, CLOSE_UP)
            say(PLAYER, "You’d never be in the way.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            hold(MOTHER, Prop.NONE)
            move(PLAYER, 0.44f)
            act(PLAYER, HUG, HAPPY, 1700)
            say(NARRATOR, "The house gets busier, and louder, and fuller. And warmer.")
        }
        option(1, "A good care home nearby. I’ll visit every weekend.", NEUTRAL, EXPLAIN, MEDIUM) {
            act(FATHER, NONE, THOUGHTFUL, 900)
            say(FATHER, "A care home. Hm.", THOUGHTFUL, THINK, MEDIUM)
            say(MOTHER, "As long as you visit.", SAD, GENTLE_SMILE, CLOSE_UP)
            say(PLAYER, "Most weekends. I promise.", NEUTRAL, NOD, TWO_SHOT)
        }
        option(2, "Let’s arrange someone to help here, every day.", HAPPY, NOD, MEDIUM) {
            say(MOTHER, "A stranger in my kitchen?", WORRIED, HANDS_UP, CLOSE_UP)
            say(FATHER, "It might be nice to have the company.", HAPPY, GENTLE_SMILE, MEDIUM)
            say(MOTHER, "…Well. Maybe.", HAPPY, SHRUG)
            say(NARRATOR, "A kind carer visits every day. Your parents stay in the house they love.")
        }
        option(3, "You’ve always managed. I’m so busy these days.", TIRED, SHRUG, MEDIUM) {
            say(MOTHER, "…Of course. We manage.", SAD, GENTLE_SMILE, CLOSE_UP)
            act(FATHER, HEAD_DOWN, SAD, 1000)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1300)
            say(NARRATOR, "You tell yourself they’re independent. The phone calls get shorter.")
        }
    }

    // ------------------------------------------------------------------ your own children

    scene("fam_kid_school", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        voice(TEACHER, "I’m worried about your child. They’re falling behind.", WORRIED)
        voice(TEACHER, "And they’ve gone very quiet in class.", WORRIED)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        say(PLAYER, "Thank you for calling. I’ll talk to them.", WORRIED, NOD, MEDIUM)
        hold(PLAYER, Prop.NONE)
        place(CHILD, 0.72f, LEFT, seated = true)
        hold(CHILD, Prop.BOOK)
        act(CHILD, HEAD_DOWN, SAD, 400)
        cam(WIDE)
        move(PLAYER, 0.50f)
        say(PLAYER, "Hey, you. How was school today?", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(CHILD, "Fine.", SAD, HEAD_DOWN, CLOSE_UP)
        say(PLAYER, "Fine? Is something bothering you?", WORRIED, NONE)
        say(CHILD, "…Nothing.", SAD, SHRUG)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        cam(TWO_SHOT, PLAYER, CHILD)

        option(0, "Let’s do homework together, every evening.", HAPPY, GENTLE_SMILE, MEDIUM) {
            sit(PLAYER)
            act(CHILD, NONE, SURPRISED, 800)
            say(CHILD, "Every evening?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Every evening. As long as it takes.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(CHILD, HEAD_DOWN, SAD, 900)
            say(CHILD, "The letters jump around when I read.", SAD, NONE, CLOSE_UP)
            say(PLAYER, "Thank you for telling me. We’ll fix it together.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "How about a tutor? Someone to help you.", NEUTRAL, NOD, MEDIUM) {
            say(CHILD, "A tutor? …Okay.", SAD, SHRUG, CLOSE_UP)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1000)
            say(NARRATOR, "The grades improve, though the real problem takes longer to surface.")
        }
        option(2, "You just need to try harder.", NEUTRAL, SHRUG, MEDIUM) {
            cam(CLOSE_UP, CHILD)
            act(CHILD, HEAD_DOWN, SAD, 1200)
            say(CHILD, "…Okay.", SAD, NONE)
            stand(CHILD)
            exit(CHILD, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1300)
            say(NARRATOR, "The grades go up a bit. The smiles go down.")
        }
        option(3, "I’ll call your teacher. We’ll work on it together.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            act(CHILD, NONE, SURPRISED, 800)
            say(CHILD, "You’re calling my teacher?", WORRIED, NONE, REACTION)
            cue("phone")
            voice(TEACHER, "I have a few ideas. Let’s meet this week.", HAPPY)
            hold(PLAYER, Prop.NONE)
            say(PLAYER, "See? We’re on your team.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(CHILD, GENTLE_SMILE, HAPPY, 1200)
        }
    }

    scene("fam_kid_dream", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        cam(ESTABLISHING)
        pause(500)
        enter(CHILD, Edge.RIGHT, 0.66f, run = true)
        hold(CHILD, Prop.BOOK)
        say(CHILD, "I need to tell you something!", EXCITED, WAVE, MEDIUM)
        say(CHILD, "I want to create things. Art. Music. For real.", EXCITED, EXPLAIN, TWO_SHOT)
        cam(CLOSE_UP, CHILD)
        act(CHILD, NONE, AFRAID, 1000)
        say(CHILD, "Please don’t say it’s not realistic.", AFRAID, HEAD_DOWN)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)
        cam(TWO_SHOT, PLAYER, CHILD)

        option(0, "I’m behind you all the way.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            stand(PLAYER)
            move(PLAYER, 0.52f)
            act(CHILD, NONE, SURPRISED, 800)
            say(CHILD, "Really?", EXCITED, HANDS_UP, REACTION)
            act(PLAYER, HUG, HAPPY, 1600)
            say(NARRATOR, "Whatever happens, you’ll have been there at the start.")
        }
        option(1, "Be realistic. Choose something safe.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            act(CHILD, NONE, SAD, 900)
            say(CHILD, "…Right. Of course.", SAD, HEAD_DOWN, CLOSE_UP)
            exit(CHILD, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, WORRIED, 1200)
            say(NARRATOR, "They comply. Years later, you notice they no longer share their ideas with you.")
        }
        option(2, "How about a trial year, with a backup plan?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(CHILD, "A trial year…", THOUGHTFUL, THINK, MEDIUM)
            say(CHILD, "With a plan B. Okay. Deal!", HAPPY, GENTLE_SMILE, CLOSE_UP)
            stand(PLAYER)
            move(PLAYER, 0.52f)
            act(PLAYER, HANDSHAKE, HAPPY, 1200)
        }
        option(3, "It’s your life. I’ll trust whatever you choose.", HAPPY, NOD, MEDIUM) {
            act(CHILD, NONE, SURPRISED, 900)
            say(CHILD, "You mean that?", SURPRISED, NONE, REACTION)
            say(CHILD, "Thank you for trusting me.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    // ------------------------------------------------------------------ the people you promised to be there for

    scene("lov_anniversary", APARTMENT, EVENING) {
        place(PLAYER, 0.40f, RIGHT)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        voice(PARTNER, "See you tonight! Can’t wait.", HAPPY)
        say(PLAYER, "Me too! See you tonight.", HAPPY, WAVE, MEDIUM)
        hold(PLAYER, Prop.NONE)
        pause(500)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, FACEPALM, AFRAID, 1000)
        say(PLAYER, "Tonight. Why tonight? …Oh no.", SURPRISED, NONE)
        say(PLAYER, "It’s our anniversary. I forgot our anniversary!", AFRAID, HANDS_UP)
        say(NARRATOR, "It’s 4:45. A big meeting starts at 5:30, and you have nothing planned.")
        cam(MEDIUM, PLAYER)

        option(0, "I’m cancelling the meeting. Dinner’s on me!", CONFIDENT, FIST_PUMP, MEDIUM) {
            cue("whoosh")
            card("THAT EVENING", kind = TitleKind.CAPTION)
            hold(PLAYER, Prop.FLOWERS)
            cam(WIDE)
            cue("door")
            enter(PARTNER, Edge.RIGHT, 0.68f)
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Is something burning?", SURPRISED, NONE, MEDIUM)
            say(PLAYER, "Happy anniversary!", EMBARRASSED, HANDS_UP, TWO_SHOT)
            say(PARTNER, "Burnt edges and all. I love it.", LAUGHING, CLAP, CLOSE_UP)
        }
        option(1, "The meeting matters. I’ll apologise afterwards.", TIRED, SHRUG, MEDIUM) {
            act(PLAYER, NONE, TIRED, 800)
            hold(PLAYER, Prop.BAG)
            card("LATE THAT NIGHT", kind = TitleKind.CAPTION)
            cue("door")
            enter(PARTNER, Edge.RIGHT, 0.68f)
            say(PLAYER, "I’m so sorry. I completely forgot.", EMBARRASSED, HANDS_UP, TWO_SHOT)
            say(PARTNER, "It’s fine.", SAD, SHRUG, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1200)
            say(NARRATOR, "It isn’t fine, and you both know it.")
        }
        option(2, "Let me call. I’ll plan something special.", WORRIED, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            say(PLAYER, "Hi, it’s me. I forgot. I’m so sorry.", WORRIED, PHONE, CLOSE_UP)
            voice(PARTNER, "You forgot? …Hm.", NEUTRAL)
            say(PLAYER, "Let me fix it. A whole weekend, just us.", HAPPY, EXPLAIN)
            voice(PARTNER, "A weekend? Apology accepted.", HAPPY)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, HAPPY, 1200)
        }
        option(3, "Flowers to the office, a table for two. Both!", CONFIDENT, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(PLAYER, "A table for two at eight. And flowers, please.", CONFIDENT, PHONE, CLOSE_UP)
            card("THAT EVENING", kind = TitleKind.CAPTION)
            hold(PLAYER, Prop.FLOWERS)
            cam(WIDE)
            cue("door")
            enter(PARTNER, Edge.RIGHT, 0.68f)
            say(PARTNER, "Flowers, and my favourite table?", HAPPY, GENTLE_SMILE, MEDIUM)
            say(PLAYER, "Quick thinking. And I made the meeting.", PROUD, GENTLE_SMILE, TWO_SHOT)
            say(PARTNER, "Happy anniversary.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
    }

    scene("lov_trust", APARTMENT, NIGHT) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        act(PLAYER, HEAD_DOWN, WORRIED, 900)
        enter(PARTNER, Edge.RIGHT, 0.64f)
        say(PARTNER, "You’ve been so quiet lately.", WORRIED, NONE, MEDIUM)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, AFRAID, 900)
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "Is there something you’re not telling me?", WORRIED, NONE, CLOSE_UP)
        say(NARRATOR, "The envelope is in your hands. The truth is right there.")
        cam(REACTION, PLAYER)
        act(PLAYER, HEAD_DOWN, AFRAID, 1200)

        option(0, "Yes, there is. Let me tell you everything.", SAD, NONE, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            hold(PARTNER, Prop.DOCUMENTS)
            cam(CLOSE_UP, PARTNER)
            act(PARTNER, NONE, THOUGHTFUL, 1500)
            hold(PARTNER, Prop.NONE)
            move(PARTNER, 0.50f)
            cam(TWO_SHOT, PLAYER, PARTNER)
            say(PARTNER, "Thank you for telling me.", SAD, GENTLE_SMILE, CLOSE_UP)
            say(PLAYER, "I’m sorry. I was so scared.", SAD, HEAD_DOWN)
        }
        option(1, "It’s just a small money thing. Nothing serious.", NEUTRAL, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(PARTNER, NONE, THOUGHTFUL, 900)
            say(PARTNER, "Okay. If you say so.", NEUTRAL, NOD, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1200)
            say(NARRATOR, "It seems to work. It leaves a shadow you’ll carry for a while.")
        }
        option(2, "No. Nothing’s wrong. I promise.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(PARTNER, "…Okay.", SAD, NONE, CLOSE_UP)
            exit(PARTNER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1400)
            say(NARRATOR, "The lie holds. But trust has a way of dying quietly from the inside.")
        }
        option(3, "Can we talk tomorrow? I need a moment.", WORRIED, NONE, MEDIUM) {
            say(PARTNER, "…Tomorrow, then. Okay.", WORRIED, NOD, CLOSE_UP)
            card("THE NEXT MORNING", kind = TitleKind.CAPTION)
            cam(TWO_SHOT, PLAYER, PARTNER)
            say(PLAYER, "I need to tell you something. All of it.", WORRIED, EXPLAIN, MEDIUM)
            say(PARTNER, "I’m listening.", NEUTRAL, NOD, CLOSE_UP)
        }
    }

    scene("fam_estranged_parent", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(NARRATOR, "A letter from your mother’s neighbour.")
        cam(CLOSE_UP, PLAYER)
        pause(500)
        say(NARRATOR, "“Your mother mentions you often, though she doesn’t say it to you.”")
        act(PLAYER, NONE, SAD, 1200)
        say(PLAYER, "She talks about me…?", SAD, NONE, CLOSE_UP)
        cam(WIDE)
        say(PLAYER, "How long has it been? Years. Too many.", WORRIED, HEAD_DOWN, MEDIUM)
        act(PLAYER, HEAD_DOWN, SAD, 1300)

        option(0, "I’m going. Right now.", CONFIDENT, NOD, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            cue("door")
            exit(PLAYER, Edge.LEFT, run = true)
            card("A LONG DRIVE LATER", kind = TitleKind.CAPTION)
            place(MOTHER, 0.68f, LEFT)
            cue("knock")
            enter(PLAYER, Edge.LEFT, 0.38f)
            cam(TWO_SHOT, PLAYER, MOTHER)
            act(MOTHER, NONE, SURPRISED, 900)
            say(MOTHER, "{name}? Is it really you?", SURPRISED, HANDS_UP, MEDIUM)
            move(PLAYER, 0.54f)
            act(PLAYER, HUG, HAPPY, 1800)
            say(PLAYER, "I’m so sorry it took me this long.", SAD, NONE, CLOSE_UP)
            say(MOTHER, "You’re here. That’s all that matters.", HAPPY, GENTLE_SMILE)
        }
        option(1, "I’ll write to her. A proper letter.", THOUGHTFUL, NOD, MEDIUM) {
            act(PLAYER, TYPE, THOUGHTFUL, 1400)
            card("TWO DAYS LATER", kind = TitleKind.CAPTION)
            cue("phone")
            hold(PLAYER, Prop.PHONE)
            say(PLAYER, "Hello? …Mom?", SURPRISED, PHONE, CLOSE_UP)
            voice(MOTHER, "I got your letter. I read it four times.", SAD)
            voice(MOTHER, "I’m sorry. I’m crying.", SAD)
            act(PLAYER, NONE, SAD, 1000)
            say(PLAYER, "Don’t be sorry. I’m coming to see you.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(2, "I’ll send something. Money, a nice gift.", NEUTRAL, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            pause(500)
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            voice(MOTHER, "Thank you for the gift, dear. It’s lovely.", NEUTRAL)
            say(PLAYER, "You’re welcome. Take care.", NEUTRAL, PHONE, CLOSE_UP)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, SAD, 1300)
            say(NARRATOR, "Neither of you says what you actually mean.")
        }
        option(3, "Next month. There’s always next month.", TIRED, SHRUG, MEDIUM) {
            act(PLAYER, NONE, TIRED, 900)
            card("A YEAR LATER", kind = TitleKind.CAPTION)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, WORRIED, 1400)
            say(NARRATOR, "Next month turned into next year. You can’t shake the feeling that you’re running out of time.")
        }
    }

    scene("fam_empty_nest", NEW_HOME, EVENING) {
        place(PLAYER, 0.60f, RIGHT)
        cam(ESTABLISHING)
        cue("door")
        pause(700)
        say(NARRATOR, "The last of your children has moved out.")
        cam(WIDE)
        move(PLAYER, 0.36f)
        act(PLAYER, NONE, SAD, 1000)
        say(PLAYER, "Well. That’s that.", SAD, SHRUG, MEDIUM)
        move(PLAYER, 0.62f)
        say(PLAYER, "Funny. I used to dream of a quiet house.", THOUGHTFUL, GENTLE_SMILE, CLOSE_UP)
        say(NARRATOR, "The days feel longer than they used to.")
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)

        option(0, "Time to dust off that old hobby.", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            act(PLAYER, NONE, EXCITED, 1000)
            say(PLAYER, "I’d forgotten how much I loved this.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            cam(WIDE)
            say(NARRATOR, "You rediscover someone you had put away.")
        }
        option(1, "I’ll visit the kids more often.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            say(PLAYER, "Hello, dear. Are you free this weekend?", HAPPY, PHONE, MEDIUM)
            say(PLAYER, "I’ll bring dinner. No, it’s no trouble.", HAPPY, GENTLE_SMILE)
            hold(PLAYER, Prop.NONE)
            say(NARRATOR, "You’re never quite as needed as you were. But being there still matters.")
        }
        option(2, "I’ll throw myself into work. It helps.", TIRED, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.LAPTOP)
            move(PLAYER, 0.50f)
            sit(PLAYER)
            act(PLAYER, TYPE, TIRED, 1600)
            say(NARRATOR, "Work fills the silence, but it isn’t the same company.")
        }
        option(3, "Let’s get the old gang over for dinner.", EXCITED, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            say(PLAYER, "Dinner at mine on Friday. Bring everyone!", EXCITED, PHONE, MEDIUM)
            hold(PLAYER, Prop.NONE)
            card("FRIDAY EVENING", kind = TitleKind.CAPTION)
            cue("door")
            enter(FRIEND, Edge.RIGHT, 0.76f)
            hold(FRIEND, Prop.BOX)
            say(FRIEND, "Did someone say dinner?", LAUGHING, WAVE, TWO_SHOT)
            cue("cheer")
            say(NARRATOR, "The house is loud again, with stories older than your children.")
        }
    }

    scene("fam_grandparent", LIVING_ROOM, DAY) {
        place(PLAYER, 0.38f, RIGHT)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        say(PLAYER, "Hi, Grandma. How are you?", HAPPY, PHONE, MEDIUM)
        say(NARRATOR, "Her voice, warm and a little unsure: “You’re busy. I know.”")
        say(NARRATOR, "“Last week I put the kettle on and forgot why. Silly me.”")
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SAD, 1200)
        say(NARRATOR, "“Will you come and see me? When you can?”")
        say(PLAYER, "Grandma… I know. I’m sorry I’ve been away.", SAD, NONE, CLOSE_UP)

        option(0, "Every week, Grandma. Sundays are yours.", HAPPY, GENTLE_SMILE, CLOSE_UP) {
            say(NARRATOR, "“Every week? Oh, I’ll bake.”")
            hold(PLAYER, Prop.NONE)
            card("SUNDAY AFTERNOON", kind = TitleKind.CAPTION)
            cam(MEDIUM, PLAYER)
            hold(PLAYER, Prop.CUP)
            sit(PLAYER)
            act(PLAYER, NONE, LAUGHING, 1200)
            say(PLAYER, "No way! You did what?", LAUGHING, CLAP, CLOSE_UP)
            say(NARRATOR, "She tells you stories you’ve never heard. You leave each visit with more than you brought.")
        }
        option(1, "I’ll call often, and visit when I can.", NEUTRAL, NOD, MEDIUM) {
            say(NARRATOR, "“That would be lovely, dear.”")
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, HAPPY, 900)
            say(NARRATOR, "It isn’t perfect, but it keeps the thread alive.")
        }
        option(2, "Soon, Grandma. I promise. It’s a busy month.", TIRED, SHRUG, MEDIUM) {
            act(PLAYER, NONE, TIRED, 900)
            hold(PLAYER, Prop.NONE)
            card("MONTHS LATER", kind = TitleKind.CAPTION)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1400)
            say(NARRATOR, "“Soon” becomes months. You tell yourself there’s still time.")
        }
        option(3, "I’ll bring {friend}. We’ll make it a party!", EXCITED, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(NARRATOR, "“A party? Oh, how lovely.”")
            cue("door")
            enter(FRIEND, Edge.RIGHT, 0.66f)
            say(FRIEND, "Did I hear the word party?", LAUGHING, WAVE, WIDE)
            say(PLAYER, "We’re visiting Grandma. Bring the cards!", HAPPY, POINT, TWO_SHOT)
            say(FRIEND, "She always wins at cards!", LAUGHING, CLAP, CLOSE_UP)
            cue("cheer")
        }
    }

    scene("fam_sibling_loan", LIVING_ROOM, NIGHT) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        act(PLAYER, NONE, TIRED, 700)
        say(PLAYER, "Hello? …{sibling}? It’s so late.", TIRED, PHONE, MEDIUM)
        voice(SIBLING, "I know. I’m sorry. I hate asking.", EMBARRASSED)
        voice(SIBLING, "I’m behind on rent. Can you lend me a lot?", SAD)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        voice(SIBLING, "I’ll pay you back. I promise.", SAD)

        option(0, "Don’t worry. I’ll lend you the whole amount.", CONFIDENT, NOD, CLOSE_UP) {
            voice(SIBLING, "Really? Thank you. Thank you so much.", SAD)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            card("MONTHS LATER", kind = TitleKind.CAPTION)
            cue("door")
            enter(SIBLING, Edge.RIGHT, 0.68f)
            cam(TWO_SHOT, PLAYER, SIBLING)
            outcome(0) {
                hold(SIBLING, Prop.DOCUMENTS)
                say(SIBLING, "Every cent. And a thank-you note.", HAPPY, GENTLE_SMILE, CLOSE_UP)
                act(PLAYER, NONE, SURPRISED, 800)
                say(PLAYER, "You didn’t have to do this.", HAPPY, NONE, TWO_SHOT)
                say(SIBLING, "Yes. I did.", HAPPY, HANDSHAKE)
            }
            outcome(1) {
                say(SIBLING, "I still haven’t paid you back. I’m sorry.", EMBARRASSED, HEAD_DOWN, CLOSE_UP)
                say(PLAYER, "Forget the money. I’m just glad you’re okay.", HAPPY, GENTLE_SMILE, TWO_SHOT)
                move(PLAYER, 0.54f)
                act(PLAYER, HUG, HAPPY, 1600)
            }
        }
        option(1, "I can lend part of it. Let’s make a plan.", NEUTRAL, EXPLAIN, MEDIUM) {
            voice(SIBLING, "A plan. Yes. Anything.", SAD)
            say(PLAYER, "Half now. The rest when you’re back on track.", NEUTRAL, EXPLAIN, CLOSE_UP)
            voice(SIBLING, "I won’t let you down.", HAPPY)
            hold(PLAYER, Prop.NONE)
            say(NARRATOR, "A clear plan helps. {sibling} sticks to it, and the debt is cleared in a year.")
        }
        option(2, "I’m sorry. I can’t afford the risk.", SAD, SHRUG, CLOSE_UP) {
            voice(SIBLING, "…Right. No. I understand.", SAD)
            hold(PLAYER, Prop.NONE)
            cue("beep")
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1400)
            say(NARRATOR, "You explain your reasons, but there’s a coldness in the call afterwards.")
        }
        option(3, "Let’s find you a better job instead.", HAPPY, NOD, MEDIUM) {
            voice(SIBLING, "A better job? …I’d love that.", SURPRISED)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            card("A FEW WEEKENDS LATER", kind = TitleKind.CAPTION)
            cue("door")
            enter(SIBLING, Edge.RIGHT, 0.68f, run = true)
            hold(SIBLING, Prop.DOCUMENTS)
            say(SIBLING, "I got it! The job! They called!", EXCITED, FIST_PUMP, MEDIUM)
            move(PLAYER, 0.52f)
            act(PLAYER, HUG, HAPPY, 1500)
            say(PLAYER, "All those mock interviews paid off.", PROUD, GENTLE_SMILE, TWO_SHOT)
        }
    }

    // ------------------------------------------------------------------ keeping love alive

    scene("lov_career_vs_love", APARTMENT, NIGHT) {
        place(PARTNER, 0.64f, LEFT, seated = true)
        hold(PARTNER, Prop.CUP)
        cam(ESTABLISHING)
        say(NARRATOR, "The candles have burned low. The dinner is cold.")
        cue("door")
        enter(PLAYER, Edge.LEFT, 0.34f)
        hold(PLAYER, Prop.BAG)
        say(PLAYER, "Sorry, sorry. The meeting ran over again.", TIRED, SHRUG, MEDIUM)
        cam(CLOSE_UP, PARTNER)
        say(PARTNER, "It always runs over.", SAD, NONE)
        act(PARTNER, HEAD_DOWN, SAD, 900)
        cam(TWO_SHOT, PLAYER, PARTNER)
        say(PARTNER, "Where do I fit in your life right now?", SAD, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)

        option(0, "You’re right. I’ll cut back and make time.", SAD, NOD, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            move(PLAYER, 0.50f)
            sit(PLAYER)
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "…Do you mean that?", SAD, GENTLE_SMILE, CLOSE_UP)
            say(PLAYER, "Evenings are yours. I’ll block them out.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "It’s only temporary. It will pass.", TIRED, SHRUG, MEDIUM) {
            say(PARTNER, "That’s what you said last time.", SAD, NONE, CLOSE_UP)
            act(PARTNER, HEAD_DOWN, SAD, 1000)
            say(PARTNER, "…Okay.", SAD, NONE)
            say(NARRATOR, "“Temporary” has a way of turning into permanent. {partner} stops asking.")
        }
        option(2, "Let’s have a weekly date night. No phones.", HAPPY, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "No phones? At all?", SURPRISED, NONE, REACTION)
            hold(PLAYER, Prop.NONE)
            say(PLAYER, "Not even a glance. I promise.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(PARTNER, "Then I’m in. Starting Thursday.", HAPPY, CLAP, CLOSE_UP)
        }
        option(3, "I… need more time to work this out.", WORRIED, SHRUG, MEDIUM) {
            say(PARTNER, "Take your time. But not too long.", SAD, NOD, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1200)
            say(NARRATOR, "It isn’t a good answer. It isn’t a bad one, either. But the question stays on the table.")
        }
    }

    scene("lov_midlife", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        place(PARTNER, 0.64f, LEFT, seated = true)
        hold(PLAYER, Prop.PHONE)
        hold(PARTNER, Prop.BOOK)
        cam(ESTABLISHING)
        say(NARRATOR, "Another quiet evening. The same couch. Different worlds.")
        say(PARTNER, "Did you take the bins out?", NEUTRAL, NONE, MEDIUM)
        say(PLAYER, "Yes. Did you pay the electricity?", NEUTRAL, NONE, TWO_SHOT)
        say(PARTNER, "Yes.", NEUTRAL, NONE)
        cam(REACTION, PLAYER)
        pause(800)
        act(PLAYER, NONE, SAD, 1100)
        cam(REACTION, PARTNER)
        act(PARTNER, NONE, SAD, 1100)
        say(NARRATOR, "Neither of you says it. Both of you know.")
        cam(TWO_SHOT, PLAYER, PARTNER)

        option(0, "Let’s take a trip. Just the two of us.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            hold(PARTNER, Prop.NONE)
            act(PARTNER, NONE, SURPRISED, 800)
            say(PARTNER, "Just us? No dishes, no deadlines?", HAPPY, HANDS_UP, CLOSE_UP)
            say(PLAYER, "Not a single one.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            stand(PARTNER)
            hold(PARTNER, Prop.SUITCASE)
            say(PARTNER, "I’m packing tonight.", LAUGHING, CLAP, MEDIUM)
            say(NARRATOR, "Away from the dishes and the deadlines, you rediscover the person you married.")
        }
        option(1, "Let’s try something new. A cooking class? Pottery?", EXCITED, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            hold(PARTNER, Prop.NONE)
            say(PARTNER, "Pottery? We’d be terrible.", LAUGHING, SHRUG, CLOSE_UP)
            say(PLAYER, "That’s the whole point.", LAUGHING, POINT, TWO_SHOT)
            act(PARTNER, NONE, LAUGHING, 1000)
            say(NARRATOR, "A cooking class and a pottery evening later, you’re laughing again.")
        }
        option(2, "It’s normal. Relationships settle.", NEUTRAL, SHRUG, MEDIUM) {
            say(PARTNER, "…Yes. I suppose they do.", SAD, GENTLE_SMILE, CLOSE_UP)
            cam(TWO_SHOT, PLAYER, PARTNER)
            act(PARTNER, NONE, SAD, 1100)
            say(NARRATOR, "Quiet years, quietly drifting.")
        }
        option(3, "What do you miss? Tell me. I’ll really listen.", HAPPY, NOD, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            hold(PARTNER, Prop.NONE)
            act(PARTNER, NONE, SURPRISED, 900)
            say(PARTNER, "I miss you looking at me when I talk.", SAD, GENTLE_SMILE, CLOSE_UP)
            say(PLAYER, "I’m looking now. Starting tonight.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(PARTNER, GENTLE_SMILE, HAPPY, 1300)
        }
    }

    scene("lov_single_choice", NIGHT_CITY, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.64f, LEFT)
        cam(ESTABLISHING)
        say(FRIEND, "I know someone you’d love. Just one dinner.", EXCITED, EXPLAIN, TWO_SHOT)
        say(PLAYER, "You said that last time.", TIRED, SHRUG, MEDIUM)
        say(FRIEND, "And the time before. I never give up.", LAUGHING, POINT, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1000)
        say(PLAYER, "I’m happy on my own. Most days.", THOUGHTFUL, GENTLE_SMILE, CLOSE_UP)
        say(FRIEND, "Most days?", SURPRISED, NONE, REACTION)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Fine. Set up the blind date.", HAPPY, SHRUG, MEDIUM) {
            say(FRIEND, "Yes! You won’t regret it!", EXCITED, FIST_PUMP, MEDIUM)
            exit(FRIEND, Edge.LEFT)
            card("THAT EVENING", kind = TitleKind.CAPTION)
            outcome(0) {
                enter(PARTNER, Edge.RIGHT, 0.66f)
                say(PARTNER, "…and then the waiter dropped the entire tray!", LAUGHING, HANDS_UP, TWO_SHOT)
                say(PLAYER, "Stop it! I can’t breathe!", LAUGHING, CLAP)
                act(PLAYER, NONE, HAPPY, 1300)
                say(NARRATOR, "To everyone’s surprise, it clicks. You’re still laughing at the end of the night.")
            }
            outcome(1) {
                act(PLAYER, NONE, THOUGHTFUL, 1200)
                say(NARRATOR, "A pleasant evening, though nothing sparks. Your friends are disappointed. You’re fine.")
            }
        }
        option(1, "No, thanks. I’m happily single.", CONFIDENT, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Okay, okay. You do look happy.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(FRIEND, "I’ll stop. …Probably.", LAUGHING, SHRUG)
            say(PLAYER, "Probably?", LAUGHING, SHRUG, TWO_SHOT)
        }
        option(2, "I’d rather meet people through a club or class.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(FRIEND, "A club? Like, pottery? Hiking?", SURPRISED, THINK, MEDIUM)
            say(PLAYER, "Why not both?", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(FRIEND, "Then I’m joining too.", EXCITED, FIST_PUMP, CLOSE_UP)
        }
        option(3, "I’d rather focus on friends and family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "That’s the nicest thing you’ve said all year.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(FRIEND, "Sunday dinner at mine. No setups.", HAPPY, POINT, TWO_SHOT)
        }
    }

    // ------------------------------------------------------------------ the favour comes back

    scene("pay_family_returns", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        cam(ESTABLISHING)
        act(PLAYER, HEAD_DOWN, TIRED, 1200)
        say(NARRATOR, "You tried to keep it to yourself. But your family always knows.")
        cue("knock")
        enter(MOTHER, Edge.RIGHT, 0.56f)
        hold(MOTHER, Prop.DOCUMENTS)
        enter(FATHER, Edge.RIGHT, 0.80f)
        cam(WIDE)
        say(MOTHER, "Don’t say you’re fine. I’m your mother.", SAD, GENTLE_SMILE, MEDIUM)
        say(PLAYER, "Mom, I’m okay, I just—", EMBARRASSED, HANDS_UP, REACTION)
        whenever(has("lost_job"), otherwise = {
            whenever(has("business_failed")) {
                say(MOTHER, "A business can fail. You never did.", PROUD, NONE, CLOSE_UP)
            }
        }) {
            say(MOTHER, "Losing a job doesn’t make you any less.", PROUD, NONE, CLOSE_UP)
        }
        say(MOTHER, "We didn’t forget everything you did for us.", PROUD, NONE, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, MOTHER)
        say(MOTHER, "Please.", SAD, GENTLE_SMILE)

        option(0, "Thank you. I… I don’t know what to say.", SAD, HEAD_DOWN, CLOSE_UP) {
            hold(MOTHER, Prop.NONE)
            hold(PLAYER, Prop.DOCUMENTS)
            stand(PLAYER)
            move(PLAYER, 0.44f)
            act(PLAYER, HEAD_DOWN, SAD, 900)
            act(MOTHER, HUG, HAPPY, 1800)
            say(FATHER, "That’s what family is for.", PROUD, GENTLE_SMILE, MEDIUM)
            say(NARRATOR, "The envelope is smaller than the weight of the gesture. You’ve never felt so loved.")
        }
        option(1, "Thank you. I’ll pay you back, I promise.", HAPPY, NOD, MEDIUM) {
            hold(MOTHER, Prop.NONE)
            hold(PLAYER, Prop.DOCUMENTS)
            say(MOTHER, "It’s a gift, sweetheart. Not a loan.", HAPPY, SHAKE_HEAD, CLOSE_UP)
            say(PLAYER, "I know. I’ll repay it anyway.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(FATHER, "Then pay us in dinners.", LAUGHING, NONE, MEDIUM)
        }
        option(2, "Keep it. I’ll manage. But thank you.", HAPPY, SHAKE_HEAD, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.44f)
            act(PLAYER, HUG, HAPPY, 1500)
            hold(MOTHER, Prop.NONE)
            cue("door")
            exit(PLAYER, Edge.LEFT)
            cam(TWO_SHOT, MOTHER, FATHER)
            say(MOTHER, "Did you slip it in their coat?", WORRIED, NONE, MEDIUM)
            say(FATHER, "Left pocket. Don’t worry.", CONFIDENT, NOD)
            say(MOTHER, "Good. They’ll find it on the bus.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(3, "Could I ask for your advice instead?", THOUGHTFUL, NOD, MEDIUM) {
            say(FATHER, "Advice? You’ve never asked me before.", SURPRISED, NONE, MEDIUM)
            say(FATHER, "One bill at a time. Start with the biggest.", PROUD, EXPLAIN, CLOSE_UP)
            act(PLAYER, NONE, SURPRISED, 800)
            say(PLAYER, "That’s… actually really good advice.", HAPPY, NOD, TWO_SHOT)
            say(MOTHER, "Don’t tell him. He’ll never stop.", LAUGHING, SHRUG, MEDIUM)
        }
    }
}
