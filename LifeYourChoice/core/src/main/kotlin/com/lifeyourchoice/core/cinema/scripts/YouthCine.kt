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

/** Youth years: school extras, student life, first steps into work, and old school choices coming back. */
val YouthCine = cinePack("youth") {

    // ------------------------------------------------------------------ school

    scene("sch_club_choice", HALLWAY, MORNING) {
        place(PLAYER, 0.26f, RIGHT)
        place(TEACHER, 0.90f, LEFT)
        hold(TEACHER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        cue("bell")
        say(TEACHER, "Sign-up day! One club per student, this year.", HAPPY, EXPLAIN, MEDIUM)
        enter(FRIEND, Edge.RIGHT, 0.50f)
        say(FRIEND, "{name}! Which one are you picking?", EXCITED, WAVE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1000)
        say(PLAYER, "Robots, paint, debates, sawdust… I want them all.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(FRIEND, "You can’t. Pick the one that feels like you.", HAPPY, GENTLE_SMILE, TWO_SHOT)

        option(0, "Robotics and coding. I’m in.", EXCITED, FIST_PUMP, MEDIUM) {
            move(PLAYER, 0.72f)
            hold(PLAYER, Prop.LAPTOP)
            say(TEACHER, "Robotics! First rule: keep it away from walls.", HAPPY, NOD, TWO_SHOT)
            say(FRIEND, "Build me a robot butler, {name}!", LAUGHING, POINT, REACTION)
            act(PLAYER, TYPE, EXCITED, 1100)
        }
        option(1, "Art and design. I want to make things.", HAPPY, GENTLE_SMILE, MEDIUM) {
            move(PLAYER, 0.72f)
            hold(PLAYER, Prop.BOOK)
            say(TEACHER, "Art and design. Everyone’s a critic, you know.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(PLAYER, "Then I’ll grow a thick skin.", CONFIDENT, NONE, CLOSE_UP)
            say(FRIEND, "And I’ll be your first fan.", HAPPY, CLAP, REACTION)
        }
        option(2, "Peer tutoring and the debate team.", CONFIDENT, EXPLAIN, MEDIUM) {
            move(PLAYER, 0.72f)
            say(TEACHER, "Tutoring and debate. A brave combination.", HAPPY, NOD, TWO_SHOT)
            say(FRIEND, "So you’ll win every argument we have?", LAUGHING, POINT, REACTION)
            say(PLAYER, "Politely. Always politely.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(3, "The woodwork and auto-shop workshop.", CONFIDENT, FIST_PUMP, MEDIUM) {
            move(PLAYER, 0.72f)
            hold(PLAYER, Prop.BOX)
            say(TEACHER, "The workshop. Measure twice, cut once.", HAPPY, NOD, TWO_SHOT)
            say(FRIEND, "You’ll come home covered in sawdust.", LAUGHING, SHRUG, REACTION)
            act(PLAYER, FIST_PUMP, PROUD, 1000)
        }
    }

    scene("sch_exam_pressure", CLASSROOM, EVENING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(FRIEND, 0.54f, LEFT, seated = true)
        place(TEACHER, 0.88f, LEFT)
        hold(PLAYER, Prop.BOOK)
        cam(WIDE)
        say(TEACHER, "Last reminder: the big exam is tomorrow morning.", NEUTRAL, EXPLAIN, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 900)
        say(PLAYER, "Tomorrow?! I haven’t even opened chapter nine.", AFRAID, SCRATCH_HEAD, CLOSE_UP)
        exit(TEACHER, Edge.RIGHT)
        cue("phone")
        say(FRIEND, "Forget it. Everyone’s going to the park tonight!", EXCITED, WAVE, TWO_SHOT)
        act(PLAYER, THINK, WORRIED, 1000)
        say(PLAYER, "It’s late, and the night keeps getting shorter.", TIRED, NONE, CLOSE_UP)

        option(0, "I’m pulling an all-nighter.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(FRIEND, "You’re insane. Good luck, {name}.", SURPRISED, SHAKE_HEAD, REACTION)
            exit(FRIEND, Edge.RIGHT)
            card("3 A.M.", ms = 1600)
            act(PLAYER, HEAD_DOWN, TIRED, 1400)
            say(PLAYER, "Mitochondria… powerhouse… still awake…", TIRED, THINK, CLOSE_UP)
            say(NARRATOR, "You walk into the exam exhausted, but you know the material.")
        }
        option(1, "Two hours of study. Then I’m sleeping.", THOUGHTFUL, NOD, MEDIUM) {
            say(FRIEND, "Boring. But smart. Sleep well!", HAPPY, WAVE, REACTION)
            exit(FRIEND, Edge.RIGHT)
            card("TWO HOURS LATER", ms = 1600)
            stand(PLAYER)
            hold(PLAYER, Prop.NONE)
            say(PLAYER, "Done. Lights out. Chapter nine, I’ve got you.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(2, "I’ll wing it. Wait, I’m coming too!", EXCITED, FIST_PUMP, MEDIUM) {
            stand(PLAYER)
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "Yes! Park, ten minutes!", EXCITED, FIST_PUMP, REACTION)
            cue("whoosh")
            exit(FRIEND, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
            card("EXAM MORNING", ms = 1600)
            enter(PLAYER, Edge.LEFT, 0.34f)
            sit(PLAYER)
            outcome(0) {
                act(PLAYER, NONE, SURPRISED, 900)
                say(PLAYER, "Wait… I actually know this one.", SURPRISED, NONE, CLOSE_UP)
                say(NARRATOR, "To your surprise, you scrape through on what you already knew.")
            }
            outcome(1) {
                act(PLAYER, HEAD_DOWN, AFRAID, 1000)
                say(PLAYER, "I don’t know a single one of these…", AFRAID, FACEPALM, CLOSE_UP)
                say(NARRATOR, "The paper is full of questions you can’t answer. It stings.")
            }
        }
        option(3, "How about studying together instead?", HAPPY, EXPLAIN, TWO_SHOT) {
            act(FRIEND, FACEPALM, TIRED, 1000)
            say(FRIEND, "Ugh. Studying? On a night like this?", TIRED, SHRUG, REACTION)
            say(PLAYER, "I’ll bring snacks.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(FRIEND, "…Fine. Snacks make it a deal.", HAPPY, NOD, MEDIUM)
            card("MIDNIGHT", ms = 1500)
            say(PLAYER, "Question five. Go!", EXCITED, POINT, MEDIUM)
            say(FRIEND, "Ha! I actually know this one!", LAUGHING, FIST_PUMP, REACTION)
        }
    }

    scene("sch_found_wallet", STREET, EVENING) {
        place(PLAYER, 0.14f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        narrate("On the way home from school, something lies on the pavement.")
        move(PLAYER, 0.42f)
        act(PLAYER, HEAD_DOWN, SURPRISED, 1000)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(CLOSE_UP, PLAYER)
        say(PLAYER, "A wallet! And it’s full of cash.", SURPRISED, NONE, CLOSE_UP)
        say(PLAYER, "There’s an ID. Two streets from here.", THOUGHTFUL, THINK, MEDIUM)
        look(PLAYER, null)
        pause(500)
        say(PLAYER, "Nobody saw me. Nobody would ever know.", WORRIED, NONE, CLOSE_UP)

        option(0, "I’ll take it back to the owner.", CONFIDENT, NOD, MEDIUM) {
            cam(FOLLOW, PLAYER)
            move(PLAYER, 0.60f)
            enter(STRANGER, Edge.RIGHT, 0.84f)
            act(STRANGER, HANDS_UP, WORRIED, 900)
            say(PLAYER, "Excuse me. Is this yours?", NEUTRAL, EXPLAIN, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            hold(STRANGER, Prop.DOCUMENTS)
            say(STRANGER, "My wallet! I’ve searched everywhere!", SURPRISED, HANDS_UP, MEDIUM)
            say(STRANGER, "Thank you. Truly. You’re one in a million.", HAPPY, HANDSHAKE, CLOSE_UP)
            act(PLAYER, NONE, PROUD, 1100)
        }
        option(1, "Keep the cash. Nobody will know.", NEUTRAL, SHRUG, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, EMBARRASSED, 1000)
            narrate("The cash disappears in a day: snacks, a new game.")
            act(PLAYER, HEAD_DOWN, SAD, 1300)
            say(PLAYER, "Why doesn’t this feel better?", SAD, NONE, CLOSE_UP)
        }
        option(2, "I’ll hand it in at the police station.", CONFIDENT, NOD, MEDIUM) {
            card("AT THE POLICE STATION", ms = 1500)
            move(PLAYER, 0.62f)
            enter(STRANGER, Edge.RIGHT, 0.86f)
            say(STRANGER, "Officer on duty. Found something?", NEUTRAL, NONE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            say(PLAYER, "A wallet. The ID says two streets away.", NEUTRAL, EXPLAIN, MEDIUM)
            say(STRANGER, "Good citizen. May I take your name?", HAPPY, GENTLE_SMILE, REACTION)
        }
        option(3, "Not my problem. I’m leaving it.", NEUTRAL, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, SHRUG, NEUTRAL, 800)
            exit(PLAYER, Edge.LEFT)
            narrate("Somewhere, someone is still looking for it.")
        }
    }

    scene("sch_group_project", CLASSROOM) {
        place(PLAYER, 0.28f, RIGHT, seated = true)
        place(FRIEND, 0.52f, LEFT, seated = true)
        place(RIVAL, 0.74f, LEFT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        hold(FRIEND, Prop.PHONE)
        cam(WIDE)
        say(PLAYER, "The presentation is Friday. Who has the slides?", WORRIED, EXPLAIN, MEDIUM)
        say(FRIEND, "I was going to start. Tonight. Probably.", EMBARRASSED, SCRATCH_HEAD, TWO_SHOT)
        say(RIVAL, "Relax. It always works out somehow.", CONFIDENT, SHRUG, REACTION)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, HANDS_UP, ANGRY, 1100)
        say(PLAYER, "Three weeks. We had three whole weeks.", ANGRY, HANDS_UP, MEDIUM)

        option(0, "Fine. I’ll do most of it myself.", TIRED, HEAD_DOWN, MEDIUM) {
            say(FRIEND, "You’re the best, {name}!", HAPPY, CLAP, REACTION)
            say(RIVAL, "Knew you’d handle it.", CONFIDENT, SHRUG, MEDIUM)
            exit(RIVAL, Edge.RIGHT, concurrent = true)
            exit(FRIEND, Edge.RIGHT)
            card("THE NIGHT BEFORE", ms = 1600)
            act(PLAYER, TYPE, TIRED, 1600)
            say(PLAYER, "Slide twelve of twenty. Wonderful.", TIRED, TYPE, CLOSE_UP)
        }
        option(1, "That’s it. I’m telling the teacher.", ANGRY, POINT, MEDIUM) {
            say(FRIEND, "Wait, what? Come on, {name}!", SURPRISED, HANDS_UP, REACTION)
            stand(PLAYER)
            enter(TEACHER, Edge.RIGHT, 0.90f)
            say(TEACHER, "Two members haven’t contributed? We’ll sort it out.", NEUTRAL, EXPLAIN, MEDIUM)
            act(FRIEND, HEAD_DOWN, EMBARRASSED, 1000)
            act(RIVAL, CROSS_ARMS, ANGRY, 900)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1000)
        }
        option(2, "Okay. Let’s split it up. Real deadlines.", CONFIDENT, EXPLAIN, MEDIUM) {
            stand(PLAYER)
            say(PLAYER, "{friend}, research by Tuesday. {rival}, the poster.", CONFIDENT, POINT, MEDIUM)
            say(FRIEND, "Wow. Okay. Tuesday. I can do that.", SURPRISED, NOD, REACTION)
            say(RIVAL, "Fine. I’ll make it look amazing.", CONFIDENT, FIST_PUMP, MEDIUM)
            say(PLAYER, "Friday, we nail it. Together.", HAPPY, FIST_PUMP, TWO_SHOT)
        }
        option(3, "I’m done. They can learn the hard way.", DISAPPOINTED, CROSS_ARMS, MEDIUM) {
            say(FRIEND, "Wait… are we actually doing something?", WORRIED, SCRATCH_HEAD, REACTION)
            card("FRIDAY", ms = 1500)
            enter(TEACHER, Edge.RIGHT, 0.90f)
            say(TEACHER, "Is this everything? It looks unfinished.", DISAPPOINTED, SHAKE_HEAD, MEDIUM)
            act(PLAYER, HEAD_DOWN, SAD, 1200)
        }
    }

    scene("sch_help_classmate", CLASSROOM, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        hold(PLAYER, Prop.BOOK)
        cam(ESTABLISHING)
        pause(500)
        enter(UNDERDOG, Edge.RIGHT, 0.64f)
        hold(UNDERDOG, Prop.DOCUMENTS)
        say(UNDERDOG, "{name}? Do you have a minute?", WORRIED, WAVE, MEDIUM)
        say(UNDERDOG, "I got a thirty-eight. Fail the next test, fail the year.", SAD, HEAD_DOWN, CLOSE_UP)
        say(UNDERDOG, "Could you help me catch up? Please?", SAD, HANDS_UP, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, WORRIED, 1000)
        say(PLAYER, "It would eat into my own study time…", WORRIED, SCRATCH_HEAD, CLOSE_UP)

        option(0, "Sure. Let’s study every afternoon.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(UNDERDOG, "Really? You’d do that for me?", SURPRISED, CLAP, REACTION)
            move(UNDERDOG, 0.48f)
            sit(UNDERDOG)
            card("TWO WEEKS LATER", ms = 1600)
            act(UNDERDOG, FIST_PUMP, EXCITED, 1100)
            say(UNDERDOG, "I passed! I actually passed!", EXCITED, FIST_PUMP, CLOSE_UP)
            say(PLAYER, "And I finally understand it, too.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "Here. Take my notes. Good luck.", NEUTRAL, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            hold(UNDERDOG, Prop.BOOK)
            say(UNDERDOG, "Your notes? Thank you. Really.", HAPPY, NOD, CLOSE_UP)
            say(PLAYER, "Small favour. Good luck on the test.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(2, "Sorry, I’m too busy right now.", NEUTRAL, SHRUG, MEDIUM) {
            act(UNDERDOG, NONE, SAD, 900)
            say(UNDERDOG, "Oh. No, it’s okay. I understand.", SAD, HEAD_DOWN, CLOSE_UP)
            exit(UNDERDOG, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1200)
        }
        option(3, "I can help. For a small fee.", CONFIDENT, SHRUG, MEDIUM) {
            act(UNDERDOG, NONE, SURPRISED, 900)
            say(UNDERDOG, "You want me to pay you?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Just a little. My time is worth something.", NEUTRAL, SHRUG, MEDIUM)
            say(UNDERDOG, "…Okay. Fine. How much?", DISAPPOINTED, SCRATCH_HEAD, CLOSE_UP)
            act(PLAYER, NONE, THOUGHTFUL, 1100)
        }
    }

    scene("sch_late_party", STREET, EVENING) {
        place(PLAYER, 0.30f, RIGHT)
        place(FRIEND, 0.52f, LEFT)
        place(STRANGER, 0.78f, LEFT)
        cam(ESTABLISHING)
        say(STRANGER, "You two coming tonight? Party across town.", CONFIDENT, EXPLAIN, MEDIUM)
        say(FRIEND, "Everyone says it’s the night of the year!", EXCITED, WAVE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, SCRATCH_HEAD, WORRIED, 900)
        say(PLAYER, "My parents think I’m studying at your place.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(STRANGER, "So? They’ll never know. Come on.", CONFIDENT, SHRUG, OVER_SHOULDER)

        option(0, "Okay. I’m sneaking out. Let’s go!", EXCITED, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Yes! Let’s go before it gets late!", EXCITED, FIST_PUMP, REACTION)
            cue("whoosh")
            exit(STRANGER, Edge.RIGHT, run = true, concurrent = true)
            exit(FRIEND, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
            card("2 A.M.", ms = 1600)
            enter(PLAYER, Edge.LEFT, 0.34f)
            outcome(0) {
                act(PLAYER, NONE, HAPPY, 800)
                say(PLAYER, "Shh. Nobody’s awake. Best night ever.", HAPPY, GENTLE_SMILE, MEDIUM)
                narrate("Loud, exciting, and you’re home unnoticed.")
            }
            outcome(1) {
                cue("door")
                enter(MOTHER, Edge.RIGHT, 0.70f)
                say(MOTHER, "Do you know what time it is?", ANGRY, CROSS_ARMS, MEDIUM)
                say(PLAYER, "Mom… I can explain.", AFRAID, HANDS_UP, REACTION)
                say(MOTHER, "Your father and I have been waiting for hours.", DISAPPOINTED, POINT, CLOSE_UP)
                act(PLAYER, HEAD_DOWN, SAD, 1200)
            }
        }
        option(1, "I’d better ask my parents first.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            say(PLAYER, "Mom? There’s a party across town. May I go?", NEUTRAL, PHONE, CLOSE_UP)
            voice(MOTHER, "Thank you for asking me, sweetheart. Let me talk to your dad.", NEUTRAL)
            outcome(0) {
                voice(MOTHER, "You can go. But we’ll pick you up at midnight.", HAPPY)
                hold(PLAYER, Prop.NONE)
                act(PLAYER, FIST_PUMP, HAPPY, 1000)
                say(FRIEND, "Midnight? Cinderella rules. Still counts!", LAUGHING, CLAP, REACTION)
            }
            outcome(1) {
                voice(MOTHER, "Not tonight, honey. But thank you for asking.", NEUTRAL)
                hold(PLAYER, Prop.NONE)
                act(PLAYER, SHRUG, SAD, 1000)
                say(FRIEND, "Aw. Next time, {name}.", SAD, SHRUG, REACTION)
            }
        }
        option(2, "No thanks. I’m staying home.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "Your loss. We’re off.", CONFIDENT, SHRUG, REACTION)
            exit(STRANGER, Edge.RIGHT)
            say(FRIEND, "You’re no fun, you know. But I respect it.", HAPPY, SHRUG, TWO_SHOT)
            exit(FRIEND, Edge.RIGHT)
            act(PLAYER, NONE, THOUGHTFUL, 1200)
            narrate("On Monday, you hear the party was a mess.")
        }
        option(3, "Come to my place instead!", HAPPY, EXPLAIN, MEDIUM) {
            say(STRANGER, "Your place? Why would we do that?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Pizza. Board games. Terrible jokes.", HAPPY, EXPLAIN, MEDIUM)
            say(FRIEND, "Pizza? Count me in!", EXCITED, FIST_PUMP, TWO_SHOT)
            act(STRANGER, NONE, THOUGHTFUL, 900)
            say(STRANGER, "…Is it pepperoni?", EMBARRASSED, SHRUG, CLOSE_UP)
            cue("cheer")
        }
    }

    scene("sch_new_teen_move", STREET) {
        place(PLAYER, 0.46f, LEFT)
        place(FRIEND, 0.72f, LEFT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        say(FRIEND, "Don’t look now. Your fan club is back.", LAUGHING, POINT, MEDIUM)
        cue("steps")
        enter(UNDERDOG, Edge.LEFT, 0.20f, run = true)
        say(UNDERDOG, "Hi! What do you do after school?", EXCITED, WAVE, OVER_SHOULDER)
        say(UNDERDOG, "Do you play games? Draw? Anything fun?", EXCITED, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, SCRATCH_HEAD, EMBARRASSED, 1000)
        say(FRIEND, "Go on, {name}. Say something.", LAUGHING, SHRUG, TWO_SHOT)

        option(0, "Come on. I’ll show you around.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(UNDERDOG, NONE, SURPRISED, 800)
            say(UNDERDOG, "Really? You’d show me around?", EXCITED, CLAP, REACTION)
            move(UNDERDOG, 0.34f)
            say(FRIEND, "You just adopted a kid, {name}.", LAUGHING, SHRUG, MEDIUM)
            say(PLAYER, "Everyone needs a first friend.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "Hi. Nice to meet you. See you around.", NEUTRAL, WAVE, MEDIUM) {
            say(UNDERDOG, "Oh. Okay. Bye then!", EMBARRASSED, WAVE, REACTION)
            exit(UNDERDOG, Edge.LEFT)
            act(PLAYER, NONE, THOUGHTFUL, 1000)
            say(FRIEND, "That was… a little awkward.", NEUTRAL, SHRUG, TWO_SHOT)
        }
        option(2, "Ha! Look, I’ve got a fan club!", LAUGHING, POINT, MEDIUM) {
            act(FRIEND, NONE, LAUGHING, 900)
            cam(CLOSE_UP, UNDERDOG)
            act(UNDERDOG, HEAD_DOWN, SAD, 1100)
            cue("steps")
            enter(TEACHER, Edge.RIGHT, 0.92f)
            act(TEACHER, CROSS_ARMS, DISAPPOINTED, 1100)
            say(TEACHER, "That wasn’t kind, {name}.", DISAPPOINTED, SHAKE_HEAD, MEDIUM)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1200)
        }
        option(3, "I know people who like that stuff. Come on.", HAPPY, WAVE, MEDIUM) {
            say(UNDERDOG, "Wait. There are others like me?", SURPRISED, NONE, REACTION)
            exit(UNDERDOG, Edge.LEFT, concurrent = true)
            exit(PLAYER, Edge.LEFT)
            say(FRIEND, "…Okay. That was actually kind of nice.", SURPRISED, SHRUG, CLOSE_UP)
        }
    }

    scene("sch_rival_contest", CLASSROOM) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(RIVAL, 0.64f, LEFT)
        place(TEACHER, 0.90f, LEFT)
        hold(RIVAL, Prop.BOX)
        cam(WIDE)
        say(TEACHER, "Science fair is Friday. The winner goes to regionals.", NEUTRAL, EXPLAIN, MEDIUM)
        say(RIVAL, "Regionals? I’ve basically won already.", CONFIDENT, CROSS_ARMS, TWO_SHOT)
        say(RIVAL, "Wait until you see my project, {name}.", CONFIDENT, SHRUG, MEDIUM)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, ANGRY, 1000)
        say(PLAYER, "Always bragging. Always.", ANGRY, CROSS_ARMS, CLOSE_UP)

        option(0, "I’ll work harder than ever on my project.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(RIVAL, "We’ll see about that.", CONFIDENT, SHRUG, REACTION)
            stand(PLAYER)
            hold(PLAYER, Prop.BOX)
            card("FAIR DAY", ms = 1500)
            cue("cheer")
            outcome(0) {
                say(TEACHER, "First place goes to {name}!", HAPPY, CLAP, MEDIUM)
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                say(RIVAL, "…Congratulations. Really.", EMBARRASSED, HANDSHAKE, TWO_SHOT)
            }
            outcome(1) {
                say(TEACHER, "Second place: {name}. First place: {rival}.", NEUTRAL, EXPLAIN, MEDIUM)
                act(RIVAL, FIST_PUMP, CONFIDENT, 900)
                say(PLAYER, "Next year, it’s mine.", CONFIDENT, GENTLE_SMILE, CLOSE_UP)
            }
        }
        option(1, "What if we team up instead?", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(RIVAL, NONE, SURPRISED, 900)
            say(RIVAL, "You and me? Together?", SURPRISED, NONE, REACTION)
            stand(PLAYER)
            move(PLAYER, 0.46f)
            say(RIVAL, "…Fine. But I get the final word.", CONFIDENT, SHRUG, TWO_SHOT)
            say(PLAYER, "Deal.", HAPPY, HANDSHAKE, MEDIUM)
            act(RIVAL, HANDSHAKE, HAPPY, 1000)
        }
        action(2) {
            say(RIVAL, "Lunch time. Nobody touch my project.", CONFIDENT, POINT, MEDIUM)
            hold(RIVAL, Prop.NONE)
            exit(RIVAL, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            look(PLAYER, null)
            act(PLAYER, NONE, AFRAID, 900)
            stand(PLAYER)
            move(PLAYER, 0.62f)
            act(PLAYER, NONE, WORRIED, 1200)
            card("AN HOUR BEFORE JUDGING", ms = 1600)
            enter(RIVAL, Edge.RIGHT, 0.80f)
            say(RIVAL, "No, no, no! It was working this morning!", AFRAID, HANDS_UP, MEDIUM)
            cam(REACTION, PLAYER)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1400)
            narrate("Nobody knows it was you. But you do.")
        }
        option(3, "Skip the fair. It’s not worth the stress.", TIRED, SHRUG, MEDIUM) {
            say(RIVAL, "Smart. Less competition for me.", CONFIDENT, SHRUG, REACTION)
            card("THE WEEKEND", ms = 1500)
            act(PLAYER, NONE, HAPPY, 1300)
            say(PLAYER, "A whole weekend off. Peaceful.", HAPPY, NONE, MEDIUM)
            act(PLAYER, THINK, THOUGHTFUL, 1300)
            say(PLAYER, "…I wonder what I might have built.", THOUGHTFUL, NONE, CLOSE_UP)
        }
    }

    scene("sch_study_vs_fun", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(PLAYER, TYPE, TIRED, 900)
        say(PLAYER, "Page one. Of fifteen. Great start.", TIRED, TYPE, MEDIUM)
        cue("door")
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "{name}! Your favourite band is playing downtown!", EXCITED, WAVE, TWO_SHOT)
        say(FRIEND, "Free concert. Tonight. We have to go!", EXCITED, FIST_PUMP, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 800)
        say(PLAYER, "The project is due tomorrow. I’ve barely started.", WORRIED, SCRATCH_HEAD, CLOSE_UP)

        option(0, "Let’s go. I’ll finish it in the morning!", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            cue("cheer")
            exit(FRIEND, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
            card("THE NEXT MORNING", ms = 1600)
            enter(PLAYER, Edge.LEFT, 0.34f)
            hold(PLAYER, Prop.LAPTOP)
            sit(PLAYER)
            act(PLAYER, FACEPALM, TIRED, 1300)
            say(PLAYER, "Best night ever. Worst morning ever.", TIRED, HEAD_DOWN, CLOSE_UP)
        }
        option(1, "Not tonight. I have to finish this.", THOUGHTFUL, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Seriously? Your loss.", DISAPPOINTED, SHRUG, REACTION)
            exit(FRIEND, Edge.RIGHT)
            cue("cheer")
            act(PLAYER, TYPE, THOUGHTFUL, 1400)
            narrate("Through the window, you can hear the music.")
            act(PLAYER, NONE, SAD, 1000)
        }
        option(2, "One hour of work. Then I’ll join you.", THOUGHTFUL, NOD, MEDIUM) {
            say(FRIEND, "One hour! I’ll wait outside.", HAPPY, POINT, REACTION)
            exit(FRIEND, Edge.RIGHT)
            act(PLAYER, TYPE, THOUGHTFUL, 1200)
            card("ONE HOUR LATER", ms = 1500)
            stand(PLAYER)
            hold(PLAYER, Prop.NONE)
            say(PLAYER, "Done! Wait for me, we’ll catch the second half!", HAPPY, FIST_PUMP, MEDIUM)
            cue("whoosh")
            exit(PLAYER, Edge.RIGHT, run = true)
        }
        option(3, "Let me ask my teacher for an extension.", THOUGHTFUL, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            say(PLAYER, "Hello? It’s {name}. About the project deadline…", NEUTRAL, PHONE, CLOSE_UP)
            say(FRIEND, "Fingers crossed!", WORRIED, NONE, REACTION)
            outcome(0) {
                whenever(has("respected_by_teacher"), otherwise = {
                    voice(TEACHER, "Okay. Just this once. Enjoy the concert.", NEUTRAL)
                }) {
                    voice(TEACHER, "Given your record, yes. Enjoy the concert.", HAPPY)
                }
                hold(PLAYER, Prop.NONE)
                act(PLAYER, FIST_PUMP, EXCITED, 1000)
                say(FRIEND, "Yes! Come on, we’re going!", EXCITED, FIST_PUMP, MEDIUM)
            }
            outcome(1) {
                voice(TEACHER, "I’m sorry. The deadline stays.", NEUTRAL)
                hold(PLAYER, Prop.NONE)
                act(PLAYER, HEAD_DOWN, SAD, 1100)
                say(FRIEND, "Ouch. Sorry, {name}.", SAD, SHRUG, REACTION)
                say(PLAYER, "Looks like it’s a late night anyway.", TIRED, NONE, CLOSE_UP)
            }
        }
    }

    scene("sch_teacher_conflict", CLASSROOM, MORNING) {
        place(PLAYER, 0.28f, RIGHT, seated = true)
        place(FRIEND, 0.46f, RIGHT, seated = true)
        place(RIVAL, 0.70f, LEFT, seated = true)
        place(TEACHER, 0.90f, LEFT)
        cam(WIDE)
        say(TEACHER, "Who keeps making that noise back there?", ANGRY, POINT, MEDIUM)
        say(TEACHER, "{name}! That’s the third time today.", ANGRY, POINT, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        say(PLAYER, "What? That wasn’t me!", SURPRISED, HANDS_UP, CLOSE_UP)
        say(TEACHER, "Don’t argue. Everyone saw it.", ANGRY, CROSS_ARMS, MEDIUM)
        cam(REACTION, RIVAL)
        act(RIVAL, NONE, CONFIDENT, 1000)
        act(FRIEND, NONE, WORRIED, 900)
        narrate("No one says a word.")

        option(0, "That’s not fair! It wasn’t me!", ANGRY, POINT, MEDIUM) {
            stand(PLAYER)
            say(TEACHER, "That’s enough! To the principal’s office. Now!", ANGRY, POINT, MEDIUM)
            act(RIVAL, NONE, LAUGHING, 800)
            exit(PLAYER, Edge.LEFT)
            cue("door")
            act(FRIEND, NONE, SAD, 1000)
        }
        option(1, "May I explain after class, please?", CONFIDENT, NOD, MEDIUM) {
            say(TEACHER, "…Fine. After class.", NEUTRAL, NONE, MEDIUM)
            cue("bell")
            card("AFTER CLASS", ms = 1500)
            exit(FRIEND, Edge.LEFT, concurrent = true)
            exit(RIVAL, Edge.RIGHT)
            stand(PLAYER)
            move(PLAYER, 0.62f)
            say(PLAYER, "It wasn’t me. I wasn’t even making a sound.", NEUTRAL, EXPLAIN, TWO_SHOT)
            act(TEACHER, NONE, THOUGHTFUL, 1000)
            say(TEACHER, "…I owe you an apology, {name}.", NEUTRAL, NOD, CLOSE_UP)
            say(TEACHER, "I’ll remember how calmly you handled this.", PROUD, HANDSHAKE, MEDIUM)
        }
        option(2, "{friend}, you saw it. Please tell them.", WORRIED, POINT, MEDIUM) {
            stand(FRIEND)
            say(FRIEND, "It wasn’t {name}. I saw who did it.", CONFIDENT, POINT, MEDIUM)
            look(TEACHER, RIVAL)
            say(TEACHER, "…Is that so?", SURPRISED, NONE, REACTION)
            act(RIVAL, NONE, EMBARRASSED, 1100)
            say(TEACHER, "Then I owe you an apology, {name}.", NEUTRAL, NOD, CLOSE_UP)
            act(PLAYER, NONE, HAPPY, 900)
        }
        option(3, "Fine. It was me. I’m sorry.", SAD, HEAD_DOWN, CLOSE_UP) {
            say(TEACHER, "Good. Let’s move on.", NEUTRAL, NOD, MEDIUM)
            act(RIVAL, NONE, CONFIDENT, 900)
            act(FRIEND, NONE, WORRIED, 900)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1400)
            narrate("It blows over. But your name has a mark next to it.")
        }
    }

    scene("sch_tryouts", BASKETBALL_COURT) {
        place(PLAYER, 0.28f, RIGHT)
        place(FRIEND, 0.50f, LEFT)
        place(RIVAL, 0.84f, LEFT)
        hold(RIVAL, Prop.BALL)
        cam(ESTABLISHING)
        say(FRIEND, "Tryouts are on Friday. Are you in?", EXCITED, EXPLAIN, MEDIUM)
        cam(WIDE)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "Look at them. So tall. So fast.", WORRIED, POINT, OVER_SHOULDER)
        say(RIVAL, "Thinking of trying out? Brave.", CONFIDENT, SHRUG, TWO_SHOT)
        say(FRIEND, "Ignore that. It’s just basketball.", HAPPY, NOD, REACTION)

        option(0, "I’ll practise every morning until Friday.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BALL)
            card("EVERY MORNING, 6 A.M.", ms = 1600)
            act(PLAYER, NONE, TIRED, 1100)
            outcome(0) {
                cue("cheer")
                say(FRIEND, "Your name’s on the list! You made it!", EXCITED, FIST_PUMP, REACTION)
                act(PLAYER, FIST_PUMP, EXCITED, 1200)
            }
            outcome(1) {
                say(FRIEND, "Not this year. Next time, {name}.", SAD, SHRUG, REACTION)
                say(PLAYER, "I’m fitter than I’ve ever been.", PROUD, GENTLE_SMILE, CLOSE_UP)
            }
        }
        option(1, "Let’s try out together. Just for fun.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(FRIEND, FIST_PUMP, EXCITED, 900)
            say(FRIEND, "Race you to the baseline!", LAUGHING, POINT, MEDIUM)
            cue("whoosh")
            move(PLAYER, 0.62f, run = true, concurrent = true)
            move(FRIEND, 0.74f, run = true)
            act(PLAYER, NONE, LAUGHING, 1000)
            say(PLAYER, "Last one there buys lunch!", LAUGHING, POINT, TWO_SHOT)
        }
        option(2, "Skip it. I’d only embarrass myself.", SAD, SHRUG, MEDIUM) {
            say(FRIEND, "Come on, {name}. Just try!", WORRIED, HANDS_UP, REACTION)
            say(PLAYER, "Maybe next year.", SAD, SHRUG, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
            sit(PLAYER)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1500)
            narrate("From the bleachers, you’re not so sure it was no big deal.")
        }
        option(3, "I’ll try the track team instead.", CONFIDENT, NOD, MEDIUM) {
            say(FRIEND, "Track? Since when do you run?", SURPRISED, SCRATCH_HEAD, REACTION)
            say(PLAYER, "Nobody’s competing for my lane.", CONFIDENT, SHRUG, MEDIUM)
            cue("whoosh")
            exit(PLAYER, Edge.LEFT, run = true)
            say(FRIEND, "…Huh. Good luck!", HAPPY, WAVE, MEDIUM)
        }
    }

    scene("sch_weekend_job", STREET) {
        place(PLAYER, 0.28f, RIGHT)
        place(BOSS, 0.74f, LEFT)
        hold(BOSS, Prop.BOX)
        cam(ESTABLISHING)
        say(BOSS, "Hey, you’re always walking past my shop.", HAPPY, WAVE, MEDIUM)
        say(BOSS, "Want a weekend job? Small pay, but it’s real.", NEUTRAL, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1000)
        say(PLAYER, "Weekends… that’s all my free time.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(BOSS, "Think it over. But I need an answer today.", NEUTRAL, SHRUG, TWO_SHOT)

        option(0, "Yes. I’ll take the job.", HAPPY, HANDSHAKE, MEDIUM) {
            move(PLAYER, 0.56f)
            act(BOSS, HANDSHAKE, HAPPY, 1000)
            say(BOSS, "Great. Saturday, eight sharp. Don’t be late.", HAPPY, POINT, MEDIUM)
            hold(BOSS, Prop.NONE)
            card("SATURDAY, 8 A.M.", ms = 1500)
            enter(CUSTOMER, Edge.LEFT, 0.28f)
            say(CUSTOMER, "How much for all of this?", NEUTRAL, EXPLAIN, TWO_SHOT)
            say(PLAYER, "That’s… three twenty. No, three fifty? Sorry!", EMBARRASSED, SCRATCH_HEAD, CLOSE_UP)
            say(BOSS, "You’ll get the hang of it.", HAPPY, GENTLE_SMILE, REACTION)
        }
        option(1, "Thanks, but I’ll focus on school.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(BOSS, "Fair enough. The offer stays open.", NEUTRAL, NOD, REACTION)
            act(PLAYER, NOD, THOUGHTFUL, 900)
            say(PLAYER, "I’ll hit the books instead.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(2, "Could I volunteer at the community centre?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(BOSS, NONE, SURPRISED, 800)
            say(BOSS, "Hm. They’re always short of hands. Go for it.", HAPPY, NOD, REACTION)
            say(PLAYER, "Then that’s what I’ll do.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("The community centre is delighted to have you.")
        }
        option(3, "I’ll start my own thing. Washing cars.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(BOSS, "Your own business? Ha! Ambitious.", SURPRISED, NONE, REACTION)
            say(PLAYER, "Watch me.", CONFIDENT, POINT, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            card("SATURDAY", ms = 1400)
            hold(PLAYER, Prop.BOX)
            enter(CUSTOMER, Edge.RIGHT, 0.70f)
            say(CUSTOMER, "My neighbour said you wash cars?", NEUTRAL, EXPLAIN, TWO_SHOT)
            say(PLAYER, "Sparkling clean, or it’s free!", HAPPY, FIST_PUMP, MEDIUM)
        }
    }

    // ------------------------------------------------------------- milestones

    scene("ms_uni_years", CAMPUS, MORNING) {
        place(PLAYER, 0.32f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        card("FIRST DAY OF UNIVERSITY", ms = 1800)
        enter(FRIEND, Edge.RIGHT, 0.64f)
        say(FRIEND, "Is this really happening? University!", EXCITED, WAVE, MEDIUM)
        say(PLAYER, "Nice campus. Terrifying syllabus.", WORRIED, SCRATCH_HEAD, TWO_SHOT)
        say(FRIEND, "Late-night pizza. Big ideas. That’s the plan.", HAPPY, EXPLAIN, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1200)
        say(FRIEND, "So… what kind of student are you?", HAPPY, POINT, TWO_SHOT)

        option(0, "I’ll study hard and aim high.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(FRIEND, "Of course you will. See you in the library?", LAUGHING, SHRUG, REACTION)
            card("THREE YEARS LATER", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, NONE, PROUD, 1000)
            say(FRIEND, "Top of the class! Look at you!", PROUD, CLAP, MEDIUM)
            say(PLAYER, "All those late nights. Worth it.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "Classes by day, a part-time job after.", THOUGHTFUL, NOD, MEDIUM) {
            say(FRIEND, "Another shift? You never stop!", SURPRISED, SHRUG, REACTION)
            say(PLAYER, "Rent doesn’t pay itself.", NEUTRAL, SHRUG, MEDIUM)
            card("THREE YEARS LATER", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "How did you manage all of it?", SURPRISED, HANDS_UP, TWO_SHOT)
            say(PLAYER, "Time management. And lots of coffee.", HAPPY, GENTLE_SMILE, MEDIUM)
        }
        option(2, "I want to enjoy campus life and friends.", EXCITED, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Now you’re talking! Pizza tonight?", EXCITED, FIST_PUMP, REACTION)
            cue("cheer")
            card("THREE YEARS LATER", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "I’m going to miss this place.", SAD, NONE, CLOSE_UP)
            act(FRIEND, HUG, HAPPY, 1400)
            say(PLAYER, "We collected a lot of stories.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(3, "I’ll start a club or a project on the side.", EXCITED, EXPLAIN, MEDIUM) {
            say(FRIEND, "A project? Count me in!", EXCITED, FIST_PUMP, REACTION)
            hold(PLAYER, Prop.LAPTOP)
            card("THREE YEARS LATER", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "Remember our first meeting? Four people.", LAUGHING, SHRUG, MEDIUM)
            say(PLAYER, "It didn’t change the world. It changed us.", PROUD, GENTLE_SMILE, TWO_SHOT)
        }
    }

    scene("ms_job_search", COFFEE_SHOP) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(PLAYER, HEAD_DOWN, TIRED, 1000)
        say(PLAYER, "Another week. Another odd job.", TIRED, NONE, CLOSE_UP)
        enter(FRIEND, Edge.RIGHT, 0.62f)
        hold(FRIEND, Prop.CUP)
        say(FRIEND, "Hey. I brought you a coffee.", HAPPY, GENTLE_SMILE, MEDIUM)
        sit(FRIEND)
        say(FRIEND, "You’re talented. But it’s time to find something with a future.", WORRIED, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SAD, 1000)
        say(PLAYER, "I know. I just don’t know where to start.", SAD, SHRUG, CLOSE_UP)

        option(0, "I’ll apply for an office job.", NEUTRAL, NOD, MEDIUM) {
            say(FRIEND, "Good! I’ll help with your résumé.", HAPPY, CLAP, REACTION)
            card("TWO WEEKS LATER", ms = 1500)
            hold(PLAYER, Prop.PHONE)
            cue("phone")
            voice(INTERVIEWER, "We’d like to offer you the position.", HAPPY)
            act(PLAYER, FIST_PUMP, EXCITED, 1100)
            say(PLAYER, "Yes! Thank you! I’ll be there Monday.", EXCITED, PHONE, CLOSE_UP)
            say(FRIEND, "Told you. You’re hired!", HAPPY, CLAP, TWO_SHOT)
        }
        option(1, "I’ll learn a trade.", CONFIDENT, NOD, MEDIUM) {
            say(FRIEND, "A trade? Really?", SURPRISED, NONE, REACTION)
            say(PLAYER, "I miss making things with my hands.", THOUGHTFUL, EXPLAIN, MEDIUM)
            say(FRIEND, "My neighbour runs a workshop. I’ll introduce you.", HAPPY, POINT, TWO_SHOT)
            act(PLAYER, HANDSHAKE, HAPPY, 1100)
        }
        option(2, "I’ll retrain in technology.", THOUGHTFUL, TYPE, MEDIUM) {
            say(FRIEND, "Tech? That means months of study.", SURPRISED, SHRUG, REACTION)
            say(PLAYER, "Evenings and weekends. I’m ready.", CONFIDENT, TYPE, MEDIUM)
            card("MONTHS OF EVENING STUDY", ms = 1700)
            act(PLAYER, TYPE, TIRED, 1300)
            say(PLAYER, "Done. My first interview is tomorrow!", EXCITED, FIST_PUMP, CLOSE_UP)
        }
        option(3, "I’ll build a freelance creative career.", EXCITED, EXPLAIN, MEDIUM) {
            say(FRIEND, "Freelance? That’s unpredictable.", WORRIED, SHRUG, REACTION)
            say(PLAYER, "I know. But it’s mine.", CONFIDENT, GENTLE_SMILE, CLOSE_UP)
            say(FRIEND, "Then I’ll be your first client.", HAPPY, HANDSHAKE, TWO_SHOT)
        }
    }

    scene("ms_work_start", OFFICE, MORNING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(COWORKER, 0.58f, LEFT, seated = true)
        place(BOSS, 0.86f, LEFT)
        hold(PLAYER, Prop.LAPTOP)
        cam(WIDE)
        card("YOUR FIRST JOB", ms = 1600)
        say(BOSS, "{name}, the client report. By five o’clock.", NEUTRAL, POINT, MEDIUM)
        say(PLAYER, "By five? Today? …Of course!", AFRAID, SCRATCH_HEAD, CLOSE_UP)
        exit(BOSS, Edge.RIGHT)
        say(COWORKER, "Welcome to real life. No syllabus.", LAUGHING, SHRUG, TWO_SHOT)
        say(PLAYER, "At school there were rules. Here… nothing.", WORRIED, HANDS_UP, MEDIUM)
        say(COWORKER, "You’ll figure it out. We all did.", HAPPY, GENTLE_SMILE, REACTION)

        option(0, "I want to learn everything I can.", EXCITED, FIST_PUMP, MEDIUM) {
            say(COWORKER, "Ambitious! Stick with me. I’ll show you.", HAPPY, NOD, REACTION)
            card("A FEW MONTHS LATER", ms = 1600)
            enter(BOSS, Edge.RIGHT, 0.86f)
            say(BOSS, "{name}, you’ve become really useful.", PROUD, GENTLE_SMILE, MEDIUM)
            act(PLAYER, NONE, PROUD, 1000)
        }
        option(1, "I’ll save from every paycheque.", NEUTRAL, NOD, MEDIUM) {
            say(COWORKER, "Saving already? Respect.", SURPRISED, NOD, REACTION)
            card("ONE YEAR LATER", ms = 1600)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, NONE, SURPRISED, 1000)
            say(PLAYER, "Is that really my balance?", SURPRISED, NONE, CLOSE_UP)
            say(COWORKER, "Told you. Small steps add up.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(2, "My own money! Let’s celebrate tonight!", HAPPY, FIST_PUMP, MEDIUM) {
            say(COWORKER, "Finally! Someone fun around here.", LAUGHING, CLAP, REACTION)
            cue("cheer")
            hold(PLAYER, Prop.CUP)
            card("A MONTH LATER", ms = 1500)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, FACEPALM, WORRIED, 1000)
            say(PLAYER, "My budget is crying.", WORRIED, NONE, CLOSE_UP)
            say(COWORKER, "Worth it. Mostly.", LAUGHING, SHRUG, TWO_SHOT)
        }
        option(3, "I’ll sign up for evening courses.", THOUGHTFUL, NOD, MEDIUM) {
            say(COWORKER, "Work all day, class all night? Wow.", SURPRISED, HANDS_UP, REACTION)
            card("A YEAR LATER", ms = 1600)
            act(PLAYER, NONE, TIRED, 900)
            enter(BOSS, Edge.RIGHT, 0.86f)
            say(BOSS, "I heard about your courses. I have a bigger role for you.", PROUD, EXPLAIN, MEDIUM)
            act(PLAYER, FIST_PUMP, EXCITED, 1000)
        }
    }

    scene("ms_gap_year", AIRPORT, MORNING) {
        place(PLAYER, 0.36f, RIGHT)
        place(MOTHER, 0.66f, LEFT)
        place(FATHER, 0.82f, LEFT)
        hold(PLAYER, Prop.SUITCASE)
        cam(ESTABLISHING)
        narrate("A whole year, and nobody telling you what to do.")
        say(MOTHER, "A whole year. Are you sure about this?", WORRIED, NONE, MEDIUM)
        say(FATHER, "Everyone has an opinion. What’s yours?", NEUTRAL, SHRUG, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1000)
        say(PLAYER, "I only know I won’t waste it.", CONFIDENT, FIST_PUMP, CLOSE_UP)
        say(MOTHER, "So what will you do with the year?", HAPPY, EXPLAIN, TWO_SHOT)

        option(0, "I’m going to travel the world on a budget.", EXCITED, FIST_PUMP, MEDIUM) {
            say(FATHER, "On a shoestring? Call every Sunday.", NEUTRAL, POINT, REACTION)
            act(MOTHER, HUG, HAPPY, 1400)
            cue("bell")
            exit(PLAYER, Edge.LEFT)
            card("ONE YEAR LATER", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.36f)
            say(MOTHER, "You look taller. No… older.", SURPRISED, NONE, MEDIUM)
            say(PLAYER, "I’ve seen so much, Mom.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "I’m going to volunteer abroad.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(MOTHER, "Helping others. I’m so proud of you.", PROUD, HUG, REACTION)
            say(FATHER, "Be safe. Be kind. Both.", NEUTRAL, NOD, MEDIUM)
            exit(PLAYER, Edge.LEFT)
            card("ONE YEAR LATER", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.36f)
            say(PLAYER, "I’ll never look at a map the same way.", THOUGHTFUL, NONE, CLOSE_UP)
            say(MOTHER, "Welcome home, sweetheart.", HAPPY, HUG, TWO_SHOT)
        }
        option(2, "I’m not flying. I’ll work and save.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(FATHER, "Staying? That’s… actually smart.", SURPRISED, NOD, REACTION)
            act(MOTHER, NONE, HAPPY, 900)
            card("ONE YEAR LATER", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            say(PLAYER, "A boring, brilliant year. Look at my savings.", PROUD, EXPLAIN, MEDIUM)
            say(FATHER, "Now that’s a plan.", PROUD, CLAP, TWO_SHOT)
        }
        option(3, "I’ll teach myself new skills online.", THOUGHTFUL, THINK, MEDIUM) {
            hold(PLAYER, Prop.LAPTOP)
            say(MOTHER, "Online? At home, all year?", SURPRISED, NONE, REACTION)
            say(PLAYER, "A year of learning. On my own terms.", CONFIDENT, EXPLAIN, MEDIUM)
            card("ONE YEAR LATER", ms = 1800)
            act(PLAYER, TYPE, PROUD, 1200)
            say(PLAYER, "I can do things I couldn’t imagine.", PROUD, GENTLE_SMILE, CLOSE_UP)
        }
    }

    // ----------------------------------------------------------------- payoffs

    scene("pay_underdog_mocked", MEETING) {
        place(COWORKER, 0.14f, RIGHT, seated = true)
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(BOSS, 0.90f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(WIDE)
        say(BOSS, "Everyone, meet the head of our new partner company.", NEUTRAL, EXPLAIN, MEDIUM)
        enter(UNDERDOG, Edge.RIGHT, 0.66f)
        hold(UNDERDOG, Prop.DOCUMENTS)
        say(BOSS, "This is {underdog}.", HAPPY, NONE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        say(PLAYER, "{underdog}? It can’t be…", AFRAID, NONE, CLOSE_UP)
        whenever(trustAtMost(NpcRole.UNDERDOG, 25), otherwise = {
            say(UNDERDOG, "{name}. It’s been a long time.", NEUTRAL, NONE, TWO_SHOT)
        }) {
            say(UNDERDOG, "{name}. I still remember that lunch table.", DISAPPOINTED, NONE, TWO_SHOT)
        }
        narrate("The room feels colder.")

        option(0, "Can we talk privately? I owe you an apology.", SAD, NONE, CLOSE_UP) {
            exit(BOSS, Edge.RIGHT, concurrent = true)
            exit(COWORKER, Edge.LEFT)
            stand(PLAYER)
            move(PLAYER, 0.48f)
            say(PLAYER, "I’m sorry. For laughing at you back then.", SAD, HEAD_DOWN, CLOSE_UP)
            act(UNDERDOG, NONE, THOUGHTFUL, 1200)
            say(UNDERDOG, "…Thank you. It means more than you’d think.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            act(PLAYER, HANDSHAKE, HAPPY, 1100)
        }
        option(1, "Nice to meet you. I don’t think we’ve met.", NEUTRAL, HANDSHAKE, MEDIUM) {
            act(UNDERDOG, NONE, DISAPPOINTED, 1200)
            say(UNDERDOG, "Of course. Pleased to meet you, too.", DISAPPOINTED, NONE, CLOSE_UP)
            act(PLAYER, NONE, EMBARRASSED, 1100)
            narrate("{underdog} lets you pretend. Somehow that’s worse.")
        }
        option(2, "Let’s get started. We have a lot to cover.", NEUTRAL, NOD, MEDIUM) {
            say(UNDERDOG, "Of course. Let’s keep it professional.", NEUTRAL, NOD, REACTION)
            act(PLAYER, NONE, THOUGHTFUL, 1000)
            narrate("Some conversations are never had.")
        }
        option(3, "Sorry, I have to step out. Can you cover?", WORRIED, HANDS_UP, MEDIUM) {
            stand(PLAYER)
            say(COWORKER, "{name}? …Fine. I’ll cover for you.", SURPRISED, SHRUG, REACTION)
            exit(PLAYER, Edge.LEFT)
            look(UNDERDOG, null)
            act(UNDERDOG, NONE, SAD, 1300)
        }
    }

    scene("pay_cheat_echo", RESTAURANT, EVENING) {
        place(PLAYER, 0.28f, RIGHT, seated = true)
        place(BOSS, 0.50f, LEFT, seated = true)
        place(COWORKER, 0.74f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(COWORKER, Prop.CUP)
        cam(ESTABLISHING)
        say(COWORKER, "Wait. {name}! We went to the same school!", EXCITED, POINT, MEDIUM)
        say(PLAYER, "Ha. Small world.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(COWORKER, "Didn’t you copy the answers in that famous exam?", LAUGHING, POINT, MEDIUM)
        cam(WIDE)
        act(BOSS, NONE, SURPRISED, 1000)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        narrate("The table goes quiet. Your boss is listening.")
        whenever(trustAtLeast(NpcRole.BOSS, 60), otherwise = {
            say(BOSS, "Is that true, {name}?", NEUTRAL, NONE, MEDIUM)
        }) {
            say(BOSS, "{name}? Surely that’s a joke.", SURPRISED, NONE, MEDIUM)
        }

        option(0, "I did. It’s one of my biggest regrets.", SAD, HEAD_DOWN, CLOSE_UP) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(BOSS, "That took guts to say, {name}.", PROUD, NOD, REACTION)
            say(COWORKER, "Wow. Sorry I brought it up.", EMBARRASSED, SCRATCH_HEAD, MEDIUM)
            act(PLAYER, NONE, HAPPY, 1100)
        }
        option(1, "That’s not true. I never did that.", ANGRY, SHAKE_HEAD, MEDIUM) {
            say(COWORKER, "Oh. I… must be mistaken.", EMBARRASSED, HANDS_UP, REACTION)
            act(BOSS, CROSS_ARMS, DISAPPOINTED, 1300)
            exit(BOSS, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, WORRIED, 1200)
            narrate("Nobody can prove it. Nobody completely believes you.")
        }
        option(2, "Ha! Doesn’t everyone have a school story?", LAUGHING, SHRUG, MEDIUM) {
            say(COWORKER, "Ha… right. Everyone does.", EMBARRASSED, SHRUG, REACTION)
            act(BOSS, NONE, THOUGHTFUL, 1200)
            narrate("The laughter is thinner than you’d like.")
        }
        option(3, "Can we talk outside later? Just us two.", NEUTRAL, NOD, MEDIUM) {
            say(COWORKER, "Uh… sure. Sorry.", EMBARRASSED, SCRATCH_HEAD, REACTION)
            exit(BOSS, Edge.RIGHT)
            card("AFTER DINNER", ms = 1500)
            stand(PLAYER)
            stand(COWORKER)
            move(PLAYER, 0.42f, concurrent = true)
            move(COWORKER, 0.58f)
            say(COWORKER, "I was only teasing. I was nervous, too.", EMBARRASSED, SHRUG, TWO_SHOT)
            say(PLAYER, "It’s fine. It was a real mistake, though.", NEUTRAL, GENTLE_SMILE, MEDIUM)
        }
    }

    scene("pay_bullied_friend", PARK, EVENING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        cam(ESTABLISHING)
        pause(500)
        enter(FRIEND, Edge.RIGHT, 0.64f)
        say(FRIEND, "I wanted you to be the first to know.", EXCITED, WAVE, MEDIUM)
        say(FRIEND, "We’re having a baby.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        say(PLAYER, "What?! That’s… that’s amazing!", SURPRISED, HANDS_UP, CLOSE_UP)
        say(FRIEND, "And I want you to be the godparent.", PROUD, GENTLE_SMILE, TWO_SHOT)
        whenever(has("housed_friend")) {
            say(FRIEND, "You even gave me a couch when I had nothing.", SAD, NONE, CLOSE_UP)
        }
        say(FRIEND, "You were there when I couldn’t be brave myself.", SAD, NONE, CLOSE_UP)

        option(0, "Yes! Of course! I’d be honoured.", HAPPY, HUG, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.48f)
            mood(FRIEND, HAPPY, HUG)
            act(PLAYER, HUG, HAPPY, 1500)
            cue("cheer")
            mood(FRIEND, HAPPY, NONE)
            say(PLAYER, "I’m crying. Don’t look at me.", LAUGHING, NONE, CLOSE_UP)
            say(FRIEND, "Too late. I’m crying too.", LAUGHING, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "Yes, of course. But how are you, really?", WORRIED, GENTLE_SMILE, CLOSE_UP) {
            act(FRIEND, NONE, SURPRISED, 800)
            sit(FRIEND)
            say(FRIEND, "I never told you how bad it really was.", SAD, HEAD_DOWN, CLOSE_UP)
            say(FRIEND, "Having you there changed everything.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(PLAYER, "I’m here. I always will be.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        }
        option(2, "I’d be honoured. I just worry I’ll be too busy.", HAPPY, SHRUG, MEDIUM) {
            say(FRIEND, "You only have to show up.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(PLAYER, "Then I’ll show up. Always.", HAPPY, NOD, TWO_SHOT)
        }
        option(3, "Thank you… but I have to say no.", SAD, SHAKE_HEAD, CLOSE_UP) {
            act(FRIEND, NONE, SURPRISED, 1000)
            say(FRIEND, "…Oh. I… understand.", SAD, HEAD_DOWN, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1500)
            narrate("You’re not sure why you said it. Neither is {friend}.")
        }
    }

    scene("pay_rival_returns", MEETING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(WIDE)
        pause(500)
        enter(RIVAL, Edge.RIGHT, 0.68f)
        hold(RIVAL, Prop.DOCUMENTS)
        say(PLAYER, "{rival}? You’re on the other side of this deal?", SURPRISED, NONE, MEDIUM)
        say(RIVAL, "Looks like we meet again.", CONFIDENT, EXPLAIN, TWO_SHOT)
        say(RIVAL, "Remember the science fair? We built something great.", LAUGHING, POINT, MEDIUM)
        whenever(trustAtLeast(NpcRole.RIVAL, 70)) {
            say(RIVAL, "You changed how I see competition, you know.", PROUD, GENTLE_SMILE, CLOSE_UP)
        }
        say(RIVAL, "This time, I’d rather be on your side.", PROUD, GENTLE_SMILE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, HAPPY, 1000)

        option(0, "Let’s partner up. We’d be unstoppable.", EXCITED, HANDSHAKE, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.50f)
            act(RIVAL, HANDSHAKE, HAPPY, 1200)
            cue("cheer")
            say(RIVAL, "Then let’s build something big.", EXCITED, FIST_PUMP, TWO_SHOT)
        }
        option(1, "Let’s compete fairly. May the best win.", CONFIDENT, NOD, MEDIUM) {
            say(RIVAL, "Fair and square. I like it.", CONFIDENT, HANDSHAKE, REACTION)
            say(PLAYER, "And no hard feelings after.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        }
        option(2, "Lunch? I’ll share what I’ve learned.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(RIVAL, "Only if we talk about the science fair.", LAUGHING, POINT, REACTION)
            say(PLAYER, "We took it way too seriously.", LAUGHING, SHRUG, TWO_SHOT)
        }
        option(3, "Let’s keep this professional.", NEUTRAL, NOD, MEDIUM) {
            act(RIVAL, NONE, DISAPPOINTED, 900)
            say(RIVAL, "Of course. Professional it is.", NEUTRAL, SHRUG, CLOSE_UP)
            act(PLAYER, NONE, THOUGHTFUL, 1000)
        }
    }
}
