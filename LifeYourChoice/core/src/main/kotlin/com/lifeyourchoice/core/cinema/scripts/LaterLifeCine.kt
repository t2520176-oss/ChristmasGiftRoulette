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

/** Friends, major events, later life and everyday moments. */
val LaterLifeCine = cinePack("laterlife") {

    // ================================================================== milestones & later life

    scene("ms_retirement", PARK, MORNING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("A lifetime of alarm clocks. And now, one last decision.")
        pause(300)
        enter(COWORKER, Edge.RIGHT, 0.68f)
        say(COWORKER, "There you are. Counting down the days?", HAPPY, WAVE, MEDIUM)
        say(COWORKER, "Everyone has advice. Travel. Garden. Sleep till noon.", LAUGHING, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, SHRUG, THOUGHTFUL, 1000)
        say(PLAYER, "A thousand opinions. One decision.", THOUGHTFUL, NONE, CLOSE_UP)
        say(COWORKER, "So. What will it be?", HAPPY, NONE, TWO_SHOT)

        option(0, "Retire fully. I’ve earned the rest.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(COWORKER, "Every single day of it.", PROUD, HANDSHAKE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1200)
            narrate("The first morning, you wake early out of habit, then realise you don’t have to, and smile.")
        }
        option(1, "Part-time, for a while. The best of both.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "Two days a week? Sneaky.", LAUGHING, POINT, TWO_SHOT)
            say(PLAYER, "Only the work I actually like.", HAPPY, GENTLE_SMILE)
        }
        option(2, "I love the work. I’m not done yet.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(COWORKER, "Of course you aren’t.", LAUGHING, SHAKE_HEAD, TWO_SHOT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, PROUD, 1100)
            narrate("Retirement is a word you’ll consider another day.")
        }
        option(3, "I’ll retire, and give my time to others.", HAPPY, NOD, MEDIUM) {
            say(COWORKER, "Tutoring? Gardening for the whole street?", SURPRISED, EXPLAIN, TWO_SHOT)
            say(PLAYER, "Wherever I’m needed.", PROUD, GENTLE_SMILE, CLOSE_UP)
            narrate("You’re busier than ever, and happier.")
        }
    }

    scene("lat_grandchildren", LIVING_ROOM) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        cam(ESTABLISHING)
        narrate("The holidays. The house is full of noise, crayons and questions.")
        cue("door")
        enter(CHILD, Edge.RIGHT, 0.64f, run = true, concurrent = true)
        hold(CHILD, Prop.DOCUMENTS)
        say(CHILD, "Look! I drew you!", EXCITED, EXPLAIN, MEDIUM)
        say(PLAYER, "Is that my hat? It’s wonderful.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(CHILD, "Were you ever little like me?", EXCITED, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, HAPPY, 900)
        say(CHILD, "I want to know everything about you!", EXCITED, HANDS_UP, TWO_SHOT)

        option(0, "Let’s spend every day together.", HAPPY, GENTLE_SMILE, TWO_SHOT) {
            act(CHILD, FIST_PUMP, EXCITED, 900)
            say(CHILD, "Fishing? And baking?", EXCITED, CLAP, MEDIUM)
            say(PLAYER, "Fishing, baking, and every story I know.", LAUGHING, EXPLAIN)
            narrate("Fishing, stories and baking disasters. They’ll remember these weeks all their lives.")
        }
        option(1, "I’ll help with your lessons, and with advice.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(CHILD, "What kind of advice?", SURPRISED, NONE, REACTION)
            say(PLAYER, "My mistakes. Then you can make better ones.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(CHILD, NONE, HAPPY, 900)
            narrate("You pay for lessons, share your mistakes, and let them make their own.")
        }
        option(2, "Sit down. I’ll tell you my whole story.", HAPPY, EXPLAIN, MEDIUM) {
            sit(CHILD)
            say(PLAYER, "It began a very long time ago…", THOUGHTFUL, EXPLAIN, TWO_SHOT)
            cam(CLOSE_UP, CHILD)
            act(CHILD, NONE, SURPRISED, 1400)
            narrate("They listen wide-eyed to every word.")
            say(CHILD, "Can you tell it again?", EXCITED, HANDS_UP, TWO_SHOT)
        }
        option(3, "Go and play. I’m here if you need me.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(CHILD, "Okay! Come and find me later!", EXCITED, WAVE)
            exit(CHILD, Edge.LEFT, run = true)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("There when they need you, and out of the way when they don’t.")
        }
    }

    scene("lat_loss", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(SIBLING, 0.68f, LEFT)
        cam(ESTABLISHING)
        narrate("The service is over. The house is full of photographs and quiet voices.")
        whenever(trustAtLeast(NpcRole.BEST_FRIEND, 75)) {
            narrate("A friend from the old days. You were there from the very start.")
        }
        say(SIBLING, "So many people came. They were loved.", SAD, GENTLE_SMILE, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, HEAD_DOWN, SAD, 1100)
        say(PLAYER, "Every story today made me laugh and cry.", SAD, NONE, CLOSE_UP)
        say(SIBLING, "What do you need, {name}? Whatever it is, do it.", SAD, NOD, TWO_SHOT)

        option(0, "I’d like to say a few words.", SAD, NOD, MEDIUM) {
            say(SIBLING, "Go on. They’d want to hear it from you.", PROUD, GENTLE_SMILE, CLOSE_UP)
            cam(CLOSE_UP, PLAYER)
            say(PLAYER, "I’m not sure my voice will hold. But I’ll try.", SAD, NONE)
            narrate("Your voice shakes, but the words come. People tell you afterwards what it meant to them.")
        }
        option(1, "I’ll stay close to their family, these next weeks.", THOUGHTFUL, NOD, MEDIUM) {
            say(SIBLING, "They’ll need that. So will you.", SAD, HUG, TWO_SHOT)
            narrate("Your friend’s family leans on you. You lean on them too, and everyone is a little less alone.")
        }
        option(2, "I need some time on my own.", SAD, HEAD_DOWN, CLOSE_UP) {
            say(SIBLING, "Take all the time you need.", SAD, NOD)
            exit(PLAYER, Edge.LEFT)
            cam(CLOSE_UP, SIBLING)
            act(SIBLING, NONE, WORRIED, 1200)
            narrate("Grief needs time. You take it, and it takes you.")
        }
        option(3, "Let’s celebrate their life. Food, photos, songs.", HAPPY, EXPLAIN, MEDIUM) {
            act(SIBLING, NONE, SURPRISED, 800)
            say(SIBLING, "They’d have wanted music. Loud music.", LAUGHING, CLAP, TWO_SHOT)
            narrate("Food, photographs, songs. It’s a wake that feels like a party.")
        }
    }

    scene("lat_memoir", LIVING_ROOM) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FRIEND, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("A quiet afternoon. Tea, and an old friend with an idea.")
        say(FRIEND, "You should write it all down, you know.", HAPPY, EXPLAIN, MEDIUM)
        say(PLAYER, "Write what down?", SURPRISED, NONE, REACTION)
        say(FRIEND, "Your story. For your family, and for anyone who might learn from it.", PROUD, GENTLE_SMILE, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, SCRATCH_HEAD, THOUGHTFUL, 1000)
        say(PLAYER, "That’s a bigger task than it sounds.", WORRIED, NONE)

        option(0, "I’ll write it, chapter by chapter.", CONFIDENT, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(FRIEND, "I’ll keep the tea coming.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 1500)
            narrate("Forgotten memories surface. When you finish, you have a stack of pages, and a new understanding of your life.")
        }
        option(1, "I’ll record my stories for the family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            enter(CHILD, Edge.RIGHT, 0.52f)
            sit(CHILD)
            say(CHILD, "Is it recording? Is it?", EXCITED, POINT, MEDIUM)
            say(PLAYER, "It is. Now, where shall I begin?", HAPPY, EXPLAIN, TWO_SHOT)
            narrate("A microphone, a pot of tea and a patient audience. It’s an irreplaceable gift.")
        }
        option(2, "I’ll teach what I know in a local class.", PROUD, EXPLAIN, MEDIUM) {
            say(FRIEND, "A class? You’d be wonderful.", EXCITED, CLAP, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            act(PLAYER, EXPLAIN, CONFIDENT, 1400)
            narrate("The classroom is full. You realise you never stopped being a teacher.")
        }
        option(3, "No. Some stories are best left untold.", TIRED, SHAKE_HEAD, CLOSE_UP) {
            say(FRIEND, "Hm. As you wish.", SAD, SHRUG, REACTION)
            act(PLAYER, HEAD_DOWN, SAD, 1200)
            narrate("A quiet decision. It stays with you.")
        }
    }

    // ================================================================== friendship

    scene("fri_old_friend_returns", COFFEE_SHOP) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        pause(500)
        cue("phone")
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 900)
        narrate("A message, from someone you haven’t spoken to in years.")
        voice(FRIEND, "Hey, it’s me. I was thinking about you. Can we catch up?", HAPPY)
        whenever(trustAtLeast(NpcRole.BEST_FRIEND, 70), otherwise = {
            say(PLAYER, "Is that… really them?", SURPRISED, NONE, MEDIUM)
        }) {
            act(PLAYER, NONE, HAPPY, 800)
            say(PLAYER, "{friend}! After all this time.", HAPPY, NONE, MEDIUM)
        }
        say(PLAYER, "So many years. What do I say?", WORRIED, NONE, CLOSE_UP)

        option(0, "Let’s meet. I want to hear everything.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            card("A FEW HOURS LATER", null, 1800)
            place(FRIEND, 0.66f, LEFT, seated = true)
            cam(TWO_SHOT, PLAYER, FRIEND)
            say(FRIEND, "Has it really been five hours?", LAUGHING, SHRUG, TWO_SHOT)
            say(PLAYER, "Who’s counting?", HAPPY, GENTLE_SMILE)
            narrate("Two hours become five. You leave feeling lighter.")
        }
        option(1, "Sorry, I’m swamped. Let’s talk soon.", WORRIED, SHRUG, CLOSE_UP) {
            act(PLAYER, TYPE, TIRED, 1100)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, SAD, 1200)
            narrate("The thread fades. You’re a little relieved, and a little sad.")
        }
        option(2, "Come over for dinner. I’ll cook.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            card("THAT EVENING", null, 1800)
            cue("door")
            place(FRIEND, 0.66f, LEFT)
            hold(FRIEND, Prop.FLOWERS)
            cam(TWO_SHOT, PLAYER, FRIEND)
            say(FRIEND, "It smells wonderful in here.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(PLAYER, "Sit down. We have years to catch up on.", HAPPY, EXPLAIN)
            narrate("The kitchen fills with old stories and new ones.")
        }
        option(3, "So, what are you up to these days?", CONFIDENT, NONE, CLOSE_UP) {
            act(PLAYER, TYPE, CONFIDENT, 1000)
            voice(FRIEND, "I run a small design firm now. Why do you ask?", HAPPY)
            say(PLAYER, "A firm? We should talk business.", CONFIDENT, EXPLAIN, MEDIUM)
            voice(FRIEND, "…Oh. Right. Business.", DISAPPOINTED)
            narrate("You make a contact, but you can feel the shift. It isn’t quite friendship.")
        }
    }

    scene("fri_wedding_invite", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        pause(400)
        enter(FRIEND, Edge.RIGHT, 0.68f, run = true)
        say(FRIEND, "{name}! I’m getting married! Two weeks from now!", EXCITED, HANDS_UP, MEDIUM)
        say(FRIEND, "It’s abroad, I know. The flights are awful.", EMBARRASSED, SHRUG, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 900)
        say(PLAYER, "Two weeks? That’s when my big project wraps up.", WORRIED, NONE, CLOSE_UP)
        cam(CLOSE_UP, FRIEND)
        say(FRIEND, "It’s okay if you can’t make it.", SAD, HEAD_DOWN)
        narrate("The words say one thing. The look in {friend.his} eyes says another.")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’m booking the flight. I wouldn’t miss it.", HAPPY, FIST_PUMP, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "You mean it? You’re really coming?", EXCITED, HANDS_UP, CLOSE_UP)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            move(FRIEND, 0.50f)
            say(PLAYER, "Try and stop me.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(FRIEND, HUG, HAPPY, 1600)
            narrate("You give the toast of your life, and {friend} cries through half of it.")
        }
        option(1, "I’ll join by video, and send a gift.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            say(FRIEND, "A video call. It’s not the same…", SAD, NONE, CLOSE_UP)
            say(FRIEND, "But thank you. Truly.", HAPPY, GENTLE_SMILE)
            narrate("It isn’t the same, but {friend} sends you a long thank-you message.")
        }
        option(2, "I’m sorry. Work has to come first.", SAD, SHRUG, MEDIUM) {
            say(FRIEND, "Sure. I understand.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, SAD, 1300)
            narrate("The project goes well. The wedding photos show an empty seat where you were meant to be.")
        }
        option(3, "Let me ask my boss for the time off.", WORRIED, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            act(FRIEND, NONE, SURPRISED, 800)
            say(PLAYER, "It’s my best friend’s wedding. Let me ask.", CONFIDENT, NOD, CLOSE_UP)
            outcome(0) {
                voice(BOSS, "Two weeks? …Go. Don’t miss it.", HAPPY)
                act(PLAYER, FIST_PUMP, EXCITED, 1000)
                act(FRIEND, NONE, HAPPY, 900)
                narrate("The boss agrees. You make it just in time.")
            }
            outcome(1) {
                voice(BOSS, "I’m sorry. I can’t spare you that week.", SAD)
                act(PLAYER, HEAD_DOWN, SAD, 1100)
                say(FRIEND, "A recorded speech will be perfect.", SAD, GENTLE_SMILE, CLOSE_UP)
                narrate("The answer is no. You send a recorded speech instead.")
            }
        }
    }

    scene("fri_moving_apart", CAMPUS, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.68f, LEFT)
        hold(FRIEND, Prop.SUITCASE)
        cam(ESTABLISHING)
        narrate("After school, everyone is scattering. Different cities, different jobs.")
        say(FRIEND, "This is it. My train leaves in an hour.", SAD, NONE, MEDIUM)
        say(FRIEND, "The group chat has already gone quiet.", SAD, SHRUG, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, HEAD_DOWN, SAD, 1000)
        say(PLAYER, "It won’t be the same, will it?", WORRIED, NONE, CLOSE_UP)
        say(FRIEND, "Will we really keep in touch?", WORRIED, NONE, TWO_SHOT)

        option(0, "Every Sunday, a call. I mean it.", CONFIDENT, NOD, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "Every Sunday. Deal.", HAPPY, HANDSHAKE, TWO_SHOT)
            narrate("It takes effort, but that effort is the friendship. Distance turns out to be just a number.")
        }
        option(1, "I’ll make new friends where I am.", NEUTRAL, SHRUG, MEDIUM) {
            say(FRIEND, "You should. Of course you should.", DISAPPOINTED, HEAD_DOWN, CLOSE_UP)
            narrate("You find a new group. The old one fades, and that hurts more than you expected.")
        }
        option(2, "That’s life. People drift apart.", TIRED, SHRUG, MEDIUM) {
            say(FRIEND, "…Yeah. I guess they do.", SAD, NONE, CLOSE_UP)
            exit(FRIEND, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, SAD, 1300)
            narrate("The messages stop. You miss the people more than you thought you would.")
        }
        option(3, "Let’s plan a reunion weekend for everyone.", EXCITED, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Everyone? Really? I’m in!", EXCITED, CLAP, TWO_SHOT)
            say(PLAYER, "Leave the planning to me.", CONFIDENT, GENTLE_SMILE)
            narrate("You become the person who keeps everyone together. People call it the best part of their year.")
        }
    }

    scene("fri_neighborhood", STREET, MORNING) {
        place(PLAYER, 0.32f, RIGHT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("Saturday morning. Your street is organising a clean-up, with food afterwards.")
        enter(STRANGER, Edge.RIGHT, 0.68f, concurrent = true)
        hold(STRANGER, Prop.BAG)
        say(STRANGER, "Morning! We’re cleaning up the street today.", HAPPY, WAVE, MEDIUM)
        say(STRANGER, "Everyone’s bringing something to eat afterwards.", HAPPY, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, TIRED, 1000)
        say(PLAYER, "It’s my only free weekend…", TIRED, SCRATCH_HEAD, CLOSE_UP)
        say(STRANGER, "I know. No pressure at all.", HAPPY, GENTLE_SMILE, TWO_SHOT)

        option(0, "Count me in. I’ll bring something to share.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            say(STRANGER, "Wonderful! Bring a chair too.", EXCITED, CLAP, TWO_SHOT)
            narrate("You meet half the street. Two of them become real friends.")
        }
        option(1, "Sorry, I really need to rest this weekend.", TIRED, SHRUG, MEDIUM) {
            say(STRANGER, "Of course. Rest well!", HAPPY, WAVE)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NONE, TIRED, 1200)
            narrate("You’re rested. The neighbours barely notice your absence.")
        }
        option(2, "I’ll drop in for an hour.", NEUTRAL, NOD, MEDIUM) {
            say(STRANGER, "An hour is perfect. Thank you!", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("A cup of coffee, a few introductions, and you’re off.")
        }
        option(3, "Let me organise the next one myself.", CONFIDENT, FIST_PUMP, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 900)
            say(STRANGER, "You? Seriously? That would be fantastic.", EXCITED, HANDS_UP, TWO_SHOT)
            narrate("Within a few months you’ve built something that outlasts the street’s grumbles.")
        }
    }

    scene("fri_business_ask", COFFEE_SHOP) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FRIEND, 0.68f, LEFT, seated = true)
        hold(FRIEND, Prop.LAPTOP)
        cam(TWO_SHOT, PLAYER, FRIEND)
        say(FRIEND, "{name}, I’ve got it. The idea. The real one.", EXCITED, EXPLAIN, MEDIUM)
        say(FRIEND, "I want you in. Your money, your time. Everything.", EXCITED, POINT, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "Money and friendship. That’s a risky mix.", WORRIED, NONE, CLOSE_UP)
        say(FRIEND, "I know. That’s why I’m asking you.", HAPPY, GENTLE_SMILE, TWO_SHOT)

        option(0, "I’m in. I’ll invest and join you.", CONFIDENT, HANDSHAKE, MEDIUM) {
            say(FRIEND, "Partners, then!", EXCITED, HANDSHAKE, TWO_SHOT)
            card("A YEAR LATER", null, 1800)
            outcome(0) {
                cue("cheer")
                say(FRIEND, "We did it, partner! Look at these numbers!", EXCITED, FIST_PUMP, MEDIUM)
                narrate("The idea takes off. Your share pays back handsomely.")
            }
            outcome(1) {
                say(FRIEND, "I’m so sorry. It’s all gone.", SAD, HEAD_DOWN, CLOSE_UP)
                say(PLAYER, "We’re still friends. That’s what counts.", SAD, GENTLE_SMILE, TWO_SHOT)
                narrate("It stumbles and folds. You lose most of your stake, but not the friendship.")
            }
        }
        option(1, "I’ll give you advice instead of money.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(FRIEND, "Honestly? Your advice is worth plenty.", HAPPY, NOD, TWO_SHOT)
            narrate("Your sensible advice saves {friend} from a few classic mistakes.")
        }
        option(2, "I can’t mix money and friendship.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Fair enough. I get it.", DISAPPOINTED, SHRUG, CLOSE_UP)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1200)
            narrate("It’s a clean decision, though you wonder what might have been.")
        }
        option(3, "Put it in writing, and I’ll invest a little.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "A contract? …Fair. It protects us both.", THOUGHTFUL, NOD, TWO_SHOT)
            narrate("Clear terms protect both of you. The business moves slowly but steadily.")
        }
    }

    // ================================================================== major life events

    scene("maj_health_scare", HOSPITAL) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        place(DOCTOR, 0.68f, LEFT)
        hold(DOCTOR, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(DOCTOR, "{name}. Your test results are in.", NEUTRAL, NONE, MEDIUM)
        say(DOCTOR, "Blood pressure, sleep, stress. Your body has been sending signals for years.", WORRIED, EXPLAIN, TWO_SHOT)
        whenever(has("worked_too_much")) {
            narrate("The late nights. The skipped meals. The years of “later”.")
        }
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        say(PLAYER, "Is it serious?", AFRAID, NONE, CLOSE_UP)
        say(DOCTOR, "It’s a warning. And it’s not too late.", WORRIED, GENTLE_SMILE, TWO_SHOT)

        option(0, "I’ll change my whole lifestyle. Starting today.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(DOCTOR, "Good. Walks, real food, proper sleep.", PROUD, NOD, TWO_SHOT)
            card("SIX MONTHS LATER", null, 1800)
            say(DOCTOR, "Look at these numbers. Remarkable.", HAPPY, CLAP, MEDIUM)
            act(PLAYER, NONE, HAPPY, 1200)
            narrate("It’s tedious at first, then oddly wonderful. You feel years younger.")
        }
        option(1, "I’m too busy for this right now.", TIRED, SHRUG, MEDIUM) {
            say(DOCTOR, "Your body won’t wait, {name}.", DISAPPOINTED, POINT, CLOSE_UP)
            stand(PLAYER)
            exit(PLAYER, Edge.LEFT)
            narrate("The warning is easy to dismiss. Your body is less forgiving.")
        }
        option(2, "I’d like a second opinion.", THOUGHTFUL, NOD, MEDIUM) {
            say(DOCTOR, "Of course. It’s your health.", NEUTRAL, NOD, TWO_SHOT)
            card("A SECOND OPINION", null, 1800)
            outcome(0) {
                say(DOCTOR, "Good news. It’s manageable, with a clear plan.", HAPPY, GENTLE_SMILE, MEDIUM)
                act(PLAYER, NONE, HAPPY, 1000)
                narrate("The second doctor is more optimistic, with a clear, manageable plan.")
            }
            outcome(1) {
                say(DOCTOR, "I agree with my colleague. Here are the details.", NEUTRAL, EXPLAIN, MEDIUM)
                act(PLAYER, NOD, THOUGHTFUL, 1000)
                narrate("The second opinion agrees with the first, with more details this time.")
            }
        }
        option(3, "I’ll lighten my workload, but keep going.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(DOCTOR, "That’s a start. Don’t wait too long.", NEUTRAL, NOD, TWO_SHOT)
            narrate("A compromise that helps your health, and surprises your boss.")
        }
    }

    scene("maj_move_city", AIRPORT, MORNING) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.68f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("An offer in another city. Better pay, a fresh start, and a ticket on the table.")
        say(FRIEND, "So it’s real. You could actually go.", SAD, NONE, MEDIUM)
        say(PLAYER, "A new city. A new job. A whole new start.", EXCITED, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "And everyone I love is right here.", SAD, NONE, CLOSE_UP)
        say(FRIEND, "That’s the problem, isn’t it?", SAD, SHRUG, TWO_SHOT)

        option(0, "I’m going. It’s now or never.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.SUITCASE)
            say(FRIEND, "Call me the moment you land.", SAD, HUG, TWO_SHOT)
            cue("whoosh")
            exit(PLAYER, Edge.LEFT)
            narrate("You pack your whole life into boxes. The city is loud, bright and strange, and you love it.")
        }
        option(1, "I’m staying. This is home.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(FRIEND, NONE, HAPPY, 900)
            say(FRIEND, "I was hoping you’d say that.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            narrate("You stay close to the people you know. It’s comfortable, though you wonder.")
        }
        option(2, "What if I work remotely, with regular trips?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(FRIEND, "Would they even agree to that?", WORRIED, SHRUG, REACTION)
            outcome(0) {
                voice(INTERVIEWER, "Mostly remote, with regular trips? We can do that.", HAPPY)
                act(FRIEND, CLAP, EXCITED, 1000)
                narrate("Mostly remote, with regular trips. You get the best of both.")
            }
            outcome(1) {
                voice(INTERVIEWER, "Hybrid? …We’ll try it. No promises.", NEUTRAL)
                card("A FEW MONTHS LATER", null, 1800)
                act(PLAYER, NONE, TIRED, 1200)
                narrate("The arrangement works poorly. Commuting is tiring.")
            }
        }
        option(3, "What do you think? Tell me honestly.", WORRIED, NONE, CLOSE_UP) {
            say(FRIEND, "Honestly? I’d miss you terribly.", SAD, NONE, MEDIUM)
            say(FRIEND, "But I’d never forgive you for staying because of me.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            hold(PLAYER, Prop.PHONE)
            voice(MOTHER, "Whatever you decide, we’re proud of you.", HAPPY)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1000)
            narrate("Everyone has an opinion. In the end, you already knew your answer.")
        }
    }

    scene("maj_friend_in_trouble", APARTMENT, NIGHT) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        narrate("Past midnight. The phone is ringing.")
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 700)
        voice(FRIEND, "{name}, I’m sorry to call so late.", SAD)
        voice(FRIEND, "I need a huge favour. Co-sign a loan for me.", SAD)
        say(PLAYER, "A loan? What’s happened?", WORRIED, NONE, CLOSE_UP)
        voice(FRIEND, "I’ll lose everything without it. I swear I’ll pay it back.", AFRAID)
        cam(MEDIUM, PLAYER)
        act(PLAYER, NONE, WORRIED, 1200)

        option(0, "I’ll co-sign. You’d do the same for me.", CONFIDENT, NOD, CLOSE_UP) {
            voice(FRIEND, "Thank you. Thank you, {name}.", HAPPY)
            card("A YEAR LATER", null, 1800)
            outcome(0) {
                place(FRIEND, 0.70f, LEFT)
                cue("cheer")
                say(FRIEND, "Paid off. Early. I told you.", PROUD, FIST_PUMP, MEDIUM)
                narrate("{friend} keeps every promise and pays the loan off early.")
            }
            outcome(1) {
                place(FRIEND, 0.70f, LEFT)
                say(FRIEND, "I couldn’t keep up with the payments…", SAD, HEAD_DOWN, CLOSE_UP)
                say(PLAYER, "We’ll sort it out together.", SAD, GENTLE_SMILE, TWO_SHOT)
                narrate("You end up paying part of the loan. The friendship survives, changed.")
            }
        }
        option(1, "I can’t co-sign, but I’ll give what I can.", THOUGHTFUL, NOD, CLOSE_UP) {
            voice(FRIEND, "You don’t have to do that…", EMBARRASSED)
            say(PLAYER, "I know. No strings attached.", HAPPY, GENTLE_SMILE, MEDIUM)
            narrate("You give what you can afford to lose. It’s a generous, sensible compromise.")
        }
        option(2, "Let’s find you a good financial adviser.", CONFIDENT, EXPLAIN, MEDIUM) {
            voice(FRIEND, "An adviser? …Okay. Okay, that might help.", WORRIED)
            narrate("An hour with a professional opens options neither of you had considered.")
        }
        option(3, "I’m sorry. I can’t risk it.", SAD, SHAKE_HEAD, CLOSE_UP) {
            voice(FRIEND, "…I understand. Goodnight, {name}.", SAD)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, SAD, 1500)
            narrate("It was a rational choice, and it’s a hard one to live with.")
        }
    }

    scene("ms_midlife_review", SKYLINE, EVENING) {
        place(PLAYER, 0.36f, RIGHT)
        cam(ESTABLISHING)
        narrate("You’ve passed the middle of your life. The view from here is different.")
        cam(PUSH_IN, PLAYER)
        say(PLAYER, "I can see further back than I can see ahead.", THOUGHTFUL, NONE, MEDIUM)
        whenever(has("ignored_parents")) {
            act(PLAYER, HEAD_DOWN, SAD, 900)
            say(PLAYER, "There are conversations I should have had sooner.", SAD, NONE, CLOSE_UP)
        }
        whenever(has("business_failed")) {
            say(PLAYER, "Even the business that failed. Oddly, I’m grateful for it.", THOUGHTFUL, GENTLE_SMILE, MEDIUM)
        }
        whenever(atLeast(Stat.FAMILY, 75)) { narrate("The people you love are close, and it shows.") }
        whenever(has("supported_family")) { narrate("Your family’s gratitude is something you’ve never needed to ask for.") }
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "Look at you, philosophising again.", LAUGHING, POINT, TWO_SHOT)
        say(FRIEND, "So, what happens next?", HAPPY, NONE, MEDIUM)

        option(0, "I’ll mend what’s broken. Starting with some calls.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            say(FRIEND, "That’s brave. And long overdue.", PROUD, GENTLE_SMILE, TWO_SHOT)
            narrate("Letters, calls, knocks on doors. Not every conversation is easy. Every one is worth it.")
        }
        option(1, "I want to mentor someone.", HAPPY, EXPLAIN, MEDIUM) {
            say(FRIEND, "You’d be so good at that.", PROUD, NOD, TWO_SHOT)
            narrate("You find a young person who needs what you know. Both of you learn from it.")
        }
        option(2, "It’s time to chase that old dream.", EXCITED, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Finally! Painting? Travelling? Writing?", EXCITED, CLAP, TWO_SHOT)
            say(PLAYER, "All of it. No more postponing.", HAPPY, NONE, CLOSE_UP)
            narrate("Whether it’s painting, travelling, building or writing, you stop putting it off.")
        }
        option(3, "Keep the course. It’s working.", NEUTRAL, SHRUG, MEDIUM) {
            say(FRIEND, "Comfortable, then?", THOUGHTFUL, NONE, TWO_SHOT)
            say(PLAYER, "Comfortable. And a little restless.", TIRED, GENTLE_SMILE, CLOSE_UP)
            narrate("A few adjustments, mostly more of the same.")
        }
    }

    scene("ms_decade_review", SKYLINE, NIGHT) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.66f, LEFT)
        cam(ESTABLISHING)
        narrate("Your thirtieth birthday. A modest party, a few cards, and a long walk home.")
        say(FRIEND, "Thirty! How does it feel?", HAPPY, CLAP, MEDIUM)
        say(PLAYER, "Strange. Like I should have a plan by now.", THOUGHTFUL, SHRUG, TWO_SHOT)
        whenever(has("worked_too_much")) { narrate("You’ve worked hard, perhaps harder than was good for you.") }
        whenever(has("helped_bullied_friend")) { narrate("You think of the friend you stood up for years ago, and the person it made you.") }
        whenever(atMost(Stat.FRIENDSHIP, 40)) { narrate("Some old friends are further away than you’d like.") }
        whenever(atLeast(Stat.MONEY, 65)) { narrate("Financially, you’re doing better than you expected.") }
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1000)
        say(FRIEND, "So what is the next ten years about?", THOUGHTFUL, NONE, TWO_SHOT)

        option(0, "My career comes first for the next ten years.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Then make it count.", PROUD, NOD, TWO_SHOT)
            narrate("You set goals and a deadline. The next decade will be about building something.")
        }
        option(1, "Family and relationships come first.", HAPPY, NOD, MEDIUM) {
            say(FRIEND, "That’s never a wrong call.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("You promise yourself that the people in your life will always come before the calendar.")
        }
        option(2, "I’ll rebuild my health and my friendships.", HAPPY, EXPLAIN, MEDIUM) {
            say(FRIEND, "Hiking? Long dinners? I’m in!", LAUGHING, FIST_PUMP, TWO_SHOT)
            narrate("You catch up with the old crowd, and feel like yourself again.")
        }
        option(3, "Time for a bold risk. A fresh start.", EXCITED, POINT, MEDIUM) {
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "Ooh. What are you going to try?", SURPRISED, NONE, REACTION)
            say(PLAYER, "I’ll write it down first. Then I’ll be too proud to quit.", HAPPY, EXPLAIN, TWO_SHOT)
            narrate("You write down the thing you’ve been afraid to try, and put it on the fridge.")
        }
    }

    scene("lat_new_hobby", PARK) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.BOOK)
        cam(ESTABLISHING)
        narrate("A sunny morning in the park. There’s time now, and nobody to stop you.")
        say(PLAYER, "Painting. Gardening. A language. An instrument.", THOUGHTFUL, EXPLAIN, MEDIUM)
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "Still dreaming up projects?", LAUGHING, POINT, TWO_SHOT)
        say(PLAYER, "I’ve wanted to try something new for years.", HAPPY, SHRUG, CLOSE_UP)
        say(FRIEND, "So? Which one will it be?", HAPPY, NONE, TWO_SHOT)

        option(0, "I’m starting today. No more waiting.", EXCITED, FIST_PUMP, MEDIUM) {
            stand(PLAYER)
            say(FRIEND, "Now that’s what I like to hear!", EXCITED, CLAP, TWO_SHOT)
            card("A FEW WEEKS LATER", null, 1800)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("You’re terrible at first, then merely bad, then, one afternoon, quite good. It’s the best feeling.")
        }
        option(1, "Come with me. Let’s try it together.", HAPPY, WAVE, MEDIUM) {
            say(FRIEND, "Me? I can’t even paint a fence!", LAUGHING, HANDS_UP, TWO_SHOT)
            say(PLAYER, "That’s what makes it fun.", HAPPY, GENTLE_SMILE)
            stand(PLAYER)
            exit(FRIEND, Edge.RIGHT, concurrent = true)
            exit(PLAYER, Edge.RIGHT)
            narrate("The company matters more than the hobby, though the hobby is a good excuse.")
        }
        option(2, "Let me think about it a little longer.", THOUGHTFUL, SHRUG, MEDIUM) {
            say(FRIEND, "Take your time. It’s not going anywhere.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1300)
            narrate("The weeks drift, and the idea stays on the shelf.")
        }
        option(3, "Let me teach someone what I already know.", HAPPY, EXPLAIN, MEDIUM) {
            enter(CHILD, Edge.LEFT, 0.12f)
            say(CHILD, "Excuse me. Could you show me how?", EXCITED, WAVE, MEDIUM)
            say(PLAYER, "Gladly. Come and sit beside me.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            move(CHILD, 0.50f)
            sit(CHILD)
            narrate("You find a young learner who is delighted, and, to your surprise, so are you.")
        }
    }

    scene("lat_health_routine", PARK, MORNING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        cam(ESTABLISHING)
        pause(600)
        enter(DOCTOR, Edge.RIGHT, 0.68f)
        say(DOCTOR, "Good morning, {name}. Out for a walk, I hope?", HAPPY, WAVE, MEDIUM)
        say(PLAYER, "Just resting my knees.", TIRED, SHRUG, TWO_SHOT)
        say(DOCTOR, "Movement. Good food. Company. Sleep. They matter enormously.", HAPPY, EXPLAIN, CLOSE_UP)
        say(PLAYER, "You said that at the last check-up.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(DOCTOR, "And I’ll keep saying it. So, what’s the plan?", NEUTRAL, POINT, MEDIUM)

        option(0, "I’ll follow your routine. Every single day.", CONFIDENT, NOD, MEDIUM) {
            stand(PLAYER)
            say(DOCTOR, "A daily walk, simple meals, a regular bedtime. Wonderful!", EXCITED, CLAP, TWO_SHOT)
            narrate("Your doctor is thrilled.")
        }
        option(1, "I’ll join a group exercise class.", HAPPY, EXPLAIN, MEDIUM) {
            say(DOCTOR, "Tai chi? Swimming? Chair yoga?", HAPPY, EXPLAIN, TWO_SHOT)
            say(PLAYER, "Whatever has the best company.", LAUGHING, SHRUG)
            narrate("You make friends faster than you gain flexibility.")
        }
        option(2, "Just the minimum. I promise.", NEUTRAL, SHRUG, MEDIUM) {
            say(DOCTOR, "The minimum is a start.", NEUTRAL, NOD, TWO_SHOT)
            narrate("A little here and a little there. You’re doing okay.")
        }
        option(3, "I’ve survived this long without it.", TIRED, SHAKE_HEAD, MEDIUM) {
            say(DOCTOR, "Hm. We’ll see at the next check-up.", WORRIED, NONE, CLOSE_UP)
            narrate("You aren’t surprised when the next check-up isn’t great.")
        }
    }

    scene("maj_financial_crisis", KITCHEN, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("Prices rise, hours are cut, and every household is tightening its belt.")
        act(PLAYER, HEAD_DOWN, TIRED, 800)
        cue("knock")
        enter(SIBLING, Edge.RIGHT, 0.68f)
        say(SIBLING, "They cut my hours too. How about yours?", WORRIED, SHRUG, MEDIUM)
        say(PLAYER, "Nearly in half. I don’t know how I’ll manage.", WORRIED, NONE, CLOSE_UP)
        say(SIBLING, "Half the street is the same.", SAD, NONE, TWO_SHOT)
        say(SIBLING, "So, what will you do?", WORRIED, EXPLAIN)

        option(0, "We cut spending and ride it out.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            whenever(has("saved_money")) { narrate("Your savings keep you steady while others struggle.") }
            narrate("You live more simply for a while. It’s hard, but it’s not unbearable.")
        }
        option(1, "I’ll take every extra shift I can.", TIRED, FIST_PUMP, MEDIUM) {
            say(SIBLING, "Don’t burn yourself out.", WORRIED, NONE, TWO_SHOT)
            act(PLAYER, HEAD_DOWN, TIRED, 1200)
            narrate("The money helps, and your body complains.")
        }
        option(2, "I’ll sell something valuable to stay afloat.", SAD, SHRUG, CLOSE_UP) {
            hold(PLAYER, Prop.BOX)
            say(SIBLING, "Are you sure? That meant a lot to you.", SAD, NONE, TWO_SHOT)
            narrate("It hurts to let go, but the cushion gets you through.")
        }
        option(3, "Let’s organise mutual support on the street.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(SIBLING, "A shared pantry? Shared tools?", SURPRISED, NONE, TWO_SHOT)
            say(PLAYER, "And shared dinners.", HAPPY, GENTLE_SMILE)
            narrate("A community pantry, shared tools and shared dinners turn a hard year into a warm memory.")
        }
    }

    scene("maj_midlife_crossroads", SKYLINE, MORNING) {
        place(PLAYER, 0.36f, RIGHT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("An ordinary Wednesday morning. The same commute, the same coffee.")
        enter(COWORKER, Edge.RIGHT, 0.68f)
        say(COWORKER, "{name}? You’ll be late for the meeting.", NEUTRAL, WAVE, MEDIUM)
        act(PLAYER, NONE, THOUGHTFUL, 900)
        say(PLAYER, "Tell me something. Is this the life I wanted?", THOUGHTFUL, NONE, CLOSE_UP)
        act(COWORKER, NONE, SURPRISED, 900)
        say(COWORKER, "…At nine in the morning? That’s a big question.", SURPRISED, SCRATCH_HEAD, TWO_SHOT)

        option(0, "I’m changing direction. Completely.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(COWORKER, "You’re serious.", SURPRISED, NONE, REACTION)
            say(PLAYER, "For the first time in years, I’m excited.", EXCITED, GENTLE_SMILE, CLOSE_UP)
            narrate("You trade the corner office for something that feels like yours. It’s scary, and exciting.")
        }
        option(1, "I’m doubling down on what I have.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(COWORKER, "That’s the spirit. I think.", NEUTRAL, SHRUG, TWO_SHOT)
            narrate("You work harder than ever. It pays off, mostly.")
        }
        option(2, "I need a long sabbatical.", TIRED, NONE, MEDIUM) {
            hold(PLAYER, Prop.SUITCASE)
            say(COWORKER, "A sabbatical? Can I come?", LAUGHING, HANDS_UP, TWO_SHOT)
            narrate("Months of travel and tea, and slow mornings. You come back lighter.")
        }
        option(3, "I’ll start a passion project on the side.", HAPPY, EXPLAIN, MEDIUM) {
            say(COWORKER, "On top of this job?", SURPRISED, NONE, TWO_SHOT)
            say(PLAYER, "An hour or two a day. That’s all.", HAPPY, GENTLE_SMILE)
            narrate("An hour or two a day becomes the best part of your day.")
        }
    }

    scene("maj_unexpected_opportunity", MEETING) {
        place(PLAYER, 0.34f, RIGHT)
        place(STRANGER, 0.68f, LEFT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("A conference coffee break. A casual chat turns into something bigger.")
        say(STRANGER, "I’ve been watching how you work. I’m impressed.", HAPPY, NOD, MEDIUM)
        say(STRANGER, "I’d like you to lead a high-profile project.", CONFIDENT, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)
        say(PLAYER, "Me? Lead it?", SURPRISED, NONE, CLOSE_UP)
        say(STRANGER, "You have one day to decide.", NEUTRAL, POINT, TWO_SHOT)

        option(0, "Yes. I’ll lead it.", CONFIDENT, HANDSHAKE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(STRANGER, "Excellent. Welcome aboard.", PROUD, HANDSHAKE, TWO_SHOT)
            outcome(0) {
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1200)
                narrate("It’s a stretch, and you make it. Doors that were closed for years begin to open.")
            }
            outcome(1) {
                act(PLAYER, NONE, TIRED, 1200)
                narrate("It’s harder than expected, and only a partial success. Still, you’re noticed for trying.")
            }
        }
        option(1, "Give me a week to prepare, then I’ll decide.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(STRANGER, "A week. Let me think about that.", NEUTRAL, NONE, TWO_SHOT)
            outcome(0) {
                say(STRANGER, "I’m happy to wait. Prepare well.", HAPPY, NOD)
                say(PLAYER, "Then I’ll say yes, with a plan.", CONFIDENT, GENTLE_SMILE, CLOSE_UP)
                narrate("They’re happy to wait. Prepared, you say yes with a plan.")
            }
            outcome(1) {
                say(STRANGER, "I can’t wait that long. I’ve asked someone else.", SAD, SHRUG, CLOSE_UP)
                narrate("The opportunity goes to someone who said yes immediately.")
            }
        }
        option(2, "Thank you, but it’s too much right now.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "I understand. The door stays open.", NEUTRAL, NOD, TWO_SHOT)
            narrate("Calm, safe, and a little wistful.")
        }
        option(3, "Let me recommend someone from my team.", HAPPY, EXPLAIN, MEDIUM) {
            enter(COWORKER, Edge.LEFT, 0.12f)
            say(STRANGER, "Hm. Tell me about them.", THOUGHTFUL, NONE, TWO_SHOT)
            say(COWORKER, "Me? You’d really do that?", SURPRISED, HANDS_UP, MEDIUM)
            say(PLAYER, "You’ve earned it.", PROUD, GENTLE_SMILE, TWO_SHOT)
            narrate("Your colleague takes the role and flourishes, and doesn’t forget who gave them the chance.")
        }
    }

    scene("maj_community_crisis", STREET, NIGHT) {
        place(PLAYER, 0.36f, RIGHT)
        cam(ESTABLISHING)
        narrate("A severe storm has flooded half the town overnight. The power is out.")
        cue("door")
        enter(FRIEND, Edge.RIGHT, 0.68f, run = true)
        say(FRIEND, "{name}! The radio’s asking for help!", AFRAID, HANDS_UP, MEDIUM)
        say(FRIEND, "The roads are blocked. People are stranded.", WORRIED, POINT, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1000)
        say(PLAYER, "We have to do something.", WORRIED, NONE, CLOSE_UP)
        say(FRIEND, "Anyone who can help. Right now.", WORRIED, NONE, TWO_SHOT)

        option(0, "Let’s go. I’ll help get people out.", CONFIDENT, FIST_PUMP, MEDIUM) {
            cue("whoosh")
            exit(FRIEND, Edge.RIGHT, run = true, concurrent = true)
            exit(PLAYER, Edge.RIGHT, run = true)
            narrate("Wet, tired and filthy, you help dozens of families to safety. You’re remembered for it.")
        }
        option(1, "I’ll donate supplies and money.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            say(FRIEND, "It helps more than you know.", HAPPY, NOD, TWO_SHOT)
            narrate("It’s not heroic, but it helps, and you sleep a little better.")
        }
        option(2, "Bring people to my home. There’s room.", CONFIDENT, WAVE, MEDIUM) {
            say(FRIEND, "Are you sure? It could be dozens.", SURPRISED, NONE, REACTION)
            say(PLAYER, "Quickly. Before the water rises.", WORRIED, POINT, MEDIUM)
            narrate("Your living room becomes a camp. The nights are crowded, and kind.")
        }
        option(3, "I have to keep my family safe at home.", WORRIED, SHRUG, CLOSE_UP) {
            say(FRIEND, "Of course. Stay safe.", SAD, NOD, TWO_SHOT)
            exit(FRIEND, Edge.RIGHT, run = true)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1300)
            narrate("You keep your family safe, which matters most. Afterwards, you feel useless.")
        }
    }

    // ================================================================== a turn to give back

    scene("pay_become_mentor", OFFICE) {
        place(PLAYER, 0.34f, RIGHT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("A busy corridor. A nervous colleague stops you.")
        enter(COWORKER, Edge.RIGHT, 0.66f)
        say(COWORKER, "{name}? Do you have a minute?", WORRIED, WAVE, MEDIUM)
        say(COWORKER, "I’ve been watching how you handle things.", EMBARRASSED, SCRATCH_HEAD, TWO_SHOT)
        say(COWORKER, "Could you teach me? Just a little?", WORRIED, NONE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        narrate("You once said exactly those words to {mentor}.")
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Yes. Let’s make it a real commitment.", HAPPY, HANDSHAKE, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "Really? Thank you. Thank you so much.", HAPPY, HANDSHAKE, CLOSE_UP)
            narrate("Weekly coffees, hard truths and kind encouragement. One day, they’ll mentor someone else.")
        }
        option(1, "I’ll share a few tips whenever I can.", NEUTRAL, NOD, MEDIUM) {
            say(COWORKER, "Anything helps. Thank you.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("It’s a small contribution that may mean more than you think.")
        }
        option(2, "I’m sorry, I’m too busy right now.", WORRIED, SHRUG, MEDIUM) {
            say(COWORKER, "Of course. Thanks anyway.", SAD, NOD, CLOSE_UP)
            exit(COWORKER, Edge.RIGHT)
            cam(REACTION, PLAYER)
            act(PLAYER, NONE, EMBARRASSED, 1200)
            narrate("They nod, thank you politely, and walk away.")
        }
        option(3, "Why not a mentoring programme for everyone?", EXCITED, EXPLAIN, MEDIUM) {
            say(COWORKER, "A whole programme? Could we?", SURPRISED, NONE, TWO_SHOT)
            card("A YEAR LATER", null, 1800)
            act(PLAYER, NONE, PROUD, 1200)
            narrate("What starts as a chat becomes a company-wide programme. Dozens of people benefit.")
        }
    }

    // ================================================================== the shape of a career

    scene("ms_next_step", CAMPUS) {
        place(PLAYER, 0.36f, RIGHT)
        place(MOTHER, 0.70f, LEFT)
        place(FATHER, 0.84f, LEFT)
        cam(ESTABLISHING)
        narrate("The doors are open in every direction. Not all of them stay open.")
        say(MOTHER, "So, my graduate. What comes next?", HAPPY, NONE, MEDIUM)
        say(FATHER, "University. A trade. A job. There’s no wrong answer.", NEUTRAL, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "That’s what scares me. No wrong answer.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(MOTHER, "Then choose what feels like you.", HAPPY, GENTLE_SMILE, TWO_SHOT)

        option(0, "I’m going to university.", CONFIDENT, NOD, MEDIUM) {
            say(FATHER, "A big campus. And a lot of coffee.", LAUGHING, GENTLE_SMILE, TWO_SHOT)
            narrate("A big decision, a bigger campus, and a lot of coffee ahead of you.")
        }
        option(1, "I want to learn a trade.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(FATHER, "Good hands. I like that.", PROUD, NOD, TWO_SHOT)
            narrate("You pick tools over textbooks, or at least alongside them. You know where you’re headed.")
        }
        option(2, "I’m going to start working right away.", NEUTRAL, NOD, MEDIUM) {
            say(MOTHER, "A real job. And a real alarm clock.", LAUGHING, POINT, TWO_SHOT)
            narrate("A real job, a real paycheque, and a real alarm clock.")
        }
        option(3, "I’m taking a gap year to find out who I am.", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.SUITCASE)
            say(MOTHER, "A whole year?", SURPRISED, HANDS_UP, REACTION)
            say(FATHER, "Let them go. They’ll be back.", LAUGHING, NOD, TWO_SHOT)
            narrate("Everyone has an opinion about it. You nod, and book the first ticket.")
        }
    }

    scene("ms_trade_years", WORKSHOP) {
        place(PLAYER, 0.34f, RIGHT)
        place(MENTOR, 0.68f, LEFT)
        cam(ESTABLISHING)
        narrate("Long days. Busy hands, and a head full of new things.")
        say(MENTOR, "Again. Slower this time. Feel the material.", NEUTRAL, EXPLAIN, MEDIUM)
        act(PLAYER, SCRATCH_HEAD, WORRIED, 1000)
        say(PLAYER, "Like this?", WORRIED, NONE, CLOSE_UP)
        say(MENTOR, "Better. Much better.", PROUD, NOD, TWO_SHOT)
        say(MENTOR, "Soon you’ll be on your own. What kind of tradesperson will you be?", THOUGHTFUL, POINT, MEDIUM)

        option(0, "I’ll master the craft, step by step.", CONFIDENT, NOD, MEDIUM) {
            say(MENTOR, "Patience. That’s the whole secret.", PROUD, GENTLE_SMILE, TWO_SHOT)
            narrate("You’re the one instructors point to when new students ask what “good” looks like.")
        }
        option(1, "I want to learn the business side, too.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(MENTOR, "Most people skip that. Smart.", HAPPY, NOD, TWO_SHOT)
            narrate("You ask about invoices and customers while others ask about tools. It sets you apart.")
        }
        option(2, "Let me help the students who are struggling.", HAPPY, NOD, MEDIUM) {
            enter(UNDERDOG, Edge.LEFT, 0.12f)
            say(UNDERDOG, "I just can’t get this joint right…", SAD, HEAD_DOWN, MEDIUM)
            move(PLAYER, 0.24f)
            say(PLAYER, "Here. Hold it like this.", HAPPY, EXPLAIN, TWO_SHOT)
            narrate("You become the person people come to for help. It makes you better, too.")
        }
        option(3, "I’ll take extra shifts. I need the money.", TIRED, SHRUG, MEDIUM) {
            say(MENTOR, "Mind your hands. They’re your living.", WORRIED, POINT, TWO_SHOT)
            narrate("You finish with experience, a little savings and tired hands.")
        }
    }

    scene("ms_career_trade", WORKSHOP) {
        place(PLAYER, 0.34f, RIGHT)
        place(MENTOR, 0.68f, LEFT)
        hold(MENTOR, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("The apprenticeship is over. You know your craft.")
        say(MENTOR, "That’s the last tool put away. You’re done with us.", PROUD, GENTLE_SMILE, MEDIUM)
        say(MENTOR, "Your certificate. You earned every line of it.", PROUD, EXPLAIN, TWO_SHOT)
        hold(MENTOR, Prop.NONE)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, PROUD, 1000)
        say(PLAYER, "I couldn’t have done it without you.", HAPPY, NONE, CLOSE_UP)
        say(MENTOR, "Now. Where will you take it?", HAPPY, POINT, TWO_SHOT)

        option(0, "I’ll work as a skilled tradesperson.", CONFIDENT, NOD, MEDIUM) {
            say(MENTOR, "Good hands deserve good work.", PROUD, GENTLE_SMILE, TWO_SHOT)
            narrate("You’re good at what you do, and people can see it.")
        }
        option(1, "I’m moving into technology.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(MENTOR, "Hands-on and technical. That’s a rare mix.", SURPRISED, NOD, TWO_SHOT)
            narrate("Your practical mindset stands out in a world of theory.")
        }
        option(2, "I’m opening my own workshop.", EXCITED, FIST_PUMP, MEDIUM) {
            say(MENTOR, "Your name on the sign. I’ll send you your first customer.", LAUGHING, HANDSHAKE, TWO_SHOT)
            narrate("Your name on the sign. Your tools on the wall. And an empty order book.")
        }
        option(3, "I’ll take a steady job at a large company.", NEUTRAL, NOD, MEDIUM) {
            say(MENTOR, "Nothing wrong with solid ground.", NEUTRAL, NOD, TWO_SHOT)
            narrate("A steady paycheque and a safe path. It may not be glamorous, but it’s a solid start.")
        }
    }

    scene("ms_career_univ", OFFICE) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(TEACHER, 0.68f, LEFT)
        cam(ESTABLISHING)
        narrate("Your studies are behind you. And the world keeps asking: so, what do you do?")
        say(TEACHER, "Your results are strong. Plenty of doors will open.", PROUD, EXPLAIN, MEDIUM)
        say(TEACHER, "Engineering, technology, teaching, medicine… Which feels like yours?", HAPPY, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, SCRATCH_HEAD, WORRIED, 1000)
        say(PLAYER, "That’s exactly what I can’t decide.", WORRIED, SHRUG, CLOSE_UP)
        say(TEACHER, "Then listen to what excites you.", HAPPY, GENTLE_SMILE, TWO_SHOT)

        option(0, "Engineering. I want to build real things.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(TEACHER, "The problems are real. So are the deadlines.", LAUGHING, POINT, TWO_SHOT)
            narrate("You join a small engineering team. The work is demanding, and exactly what you hoped for.")
        }
        option(1, "Technology. Everything is changing, and I want in.", EXCITED, NOD, MEDIUM) {
            say(TEACHER, "Start as a junior. You’ll grow fast.", HAPPY, NOD, TWO_SHOT)
            narrate("You start as a junior developer. Everything is evolving, and so are you.")
        }
        option(2, "Teaching. I want to help people learn.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(TEACHER, "I may be biased, but I approve.", LAUGHING, GENTLE_SMILE, TWO_SHOT)
            narrate("The pay isn’t spectacular, but when a student finally understands, you’d do it for free.")
        }
        option(3, "Medicine. I want to look after people.", CONFIDENT, NOD, MEDIUM) {
            say(TEACHER, "Long hours. Enormous responsibility.", WORRIED, EXPLAIN, TWO_SHOT)
            say(PLAYER, "And a purpose that never goes away.", PROUD, GENTLE_SMILE, CLOSE_UP)
            narrate("The training is relentless, and the sense of purpose is real.")
        }
        option(4, "Design. I want to make beautiful things.", HAPPY, EXPLAIN, MEDIUM) {
            say(TEACHER, "Then do that. Every project is a gift.", PROUD, GENTLE_SMILE, TWO_SHOT)
            narrate("You turn what you’ve always loved into a career.")
        }
    }

    scene("ms_career_other", OFFICE) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(INTERVIEWER, 0.68f, LEFT, seated = true)
        hold(INTERVIEWER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("No degree on the wall. But plenty of energy, and plenty of ideas.")
        say(INTERVIEWER, "I’ve read your file. No degree. Lots of drive.", NEUTRAL, EXPLAIN, MEDIUM)
        say(INTERVIEWER, "Plenty of people made it the unconventional way.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1000)
        say(PLAYER, "So where do I start?", WORRIED, SHRUG, CLOSE_UP)
        say(INTERVIEWER, "That’s up to you. What will you build?", HAPPY, POINT, TWO_SHOT)

        option(0, "I’ll join a company as an employee.", NEUTRAL, NOD, MEDIUM) {
            say(INTERVIEWER, "Start at the bottom. Don’t stay there.", HAPPY, NOD, TWO_SHOT)
            narrate("You start at the bottom, and promise yourself you won’t stay there.")
        }
        option(1, "I want to become a skilled tradesperson.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(INTERVIEWER, "A local workshop is hiring apprentices.", HAPPY, NOD, TWO_SHOT)
            narrate("You get paid to learn, and you’re learning fast.")
        }
        option(2, "I’ll build a creative career.", EXCITED, FIST_PUMP, MEDIUM) {
            say(INTERVIEWER, "Risky. But some of the best paths are.", THOUGHTFUL, SHRUG, TWO_SHOT)
            narrate("Videos, designs, music, words. You post everything, and learn from every comment.")
        }
        option(3, "I’m going into technology.", CONFIDENT, NOD, MEDIUM) {
            say(INTERVIEWER, "Self-taught, determined, stubborn. I like it.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("You land a junior role, and prove yourself in weeks.")
        }
        option(4, "I’ll start something of my own.", EXCITED, POINT, MEDIUM) {
            act(INTERVIEWER, NONE, SURPRISED, 900)
            say(INTERVIEWER, "There’s no degree for that. Good luck.", PROUD, HANDSHAKE, TWO_SHOT)
            narrate("You figure it out as you go.")
        }
    }

    scene("ms_management_path", MEETING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(BOSS, 0.68f, LEFT, seated = true)
        place(COWORKER, 0.90f, LEFT, seated = true)
        hold(BOSS, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(BOSS, "{name}, I’ll be direct. We want you in management.", CONFIDENT, EXPLAIN, MEDIUM)
        say(BOSS, "Fewer hands-on tasks. More meetings. More pay.", NEUTRAL, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(COWORKER, "Some say you’d be great. A few say you’d hate it.", LAUGHING, SHRUG, MEDIUM)
        say(PLAYER, "Both sound about right.", THOUGHTFUL, GENTLE_SMILE, CLOSE_UP)

        option(0, "I’ll take the management role.", CONFIDENT, HANDSHAKE, MEDIUM) {
            say(BOSS, "Welcome to the other side of the table.", PROUD, HANDSHAKE, TWO_SHOT)
            narrate("Your calendar fills up with other people’s problems. You’re good at it, and it’s tiring.")
        }
        option(1, "I’d rather stay hands-on, as an expert.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(BOSS, "Understood. We’ll always need the best hands.", NEUTRAL, NOD, TWO_SHOT)
            narrate("You become the person everyone asks when things go wrong. It’s the work you love.")
        }
        option(2, "Could I try it for a trial period?", THOUGHTFUL, NOD, MEDIUM) {
            say(BOSS, "A trial. Fair enough.", NEUTRAL, NOD, TWO_SHOT)
            card("A FEW MONTHS LATER", null, 1800)
            outcome(0) {
                say(BOSS, "You’re a natural. Let’s make it permanent.", PROUD, HANDSHAKE, MEDIUM)
                act(PLAYER, NONE, PROUD, 1000)
                narrate("The trial goes well, and they make it permanent.")
            }
            outcome(1) {
                say(PLAYER, "I miss the real work. Management isn’t for me.", THOUGHTFUL, SHAKE_HEAD, CLOSE_UP)
                say(BOSS, "Better to know. Thank you for trying.", NEUTRAL, NOD, TWO_SHOT)
                narrate("You find out management isn’t for you. It’s better to know.")
            }
        }
        option(3, "Thank you, but I’ll decline.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(BOSS, "A pity. But I understand.", NEUTRAL, SHRUG, TWO_SHOT)
            narrate("You stay where you’re comfortable. Someone else takes the role.")
        }
    }

    // ================================================================== everyday moments

    scene("fill_old_photograph", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        hold(PLAYER, Prop.BOX)
        cam(ESTABLISHING)
        narrate("While tidying up, you find something you haven’t seen in years.")
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(PLAYER, "Look at this. An old photograph.", SURPRISED, NONE, MEDIUM)
        say(PLAYER, "Faces I haven’t seen in years…", THOUGHTFUL, NONE, CLOSE_UP)
        say(PLAYER, "I used to love that place.", HAPPY, GENTLE_SMILE)
        narrate("For a moment, the room goes quiet.")

        option(0, "I’ll call someone from the picture.", HAPPY, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "…{name}? Is that really you?", SURPRISED)
            say(PLAYER, "It is. I found an old photograph of us.", HAPPY, NONE, CLOSE_UP)
            voice(FRIEND, "Don’t hang up. I have so much to tell you!", EXCITED)
            narrate("A surprised voice on the other end, and a long, warm conversation.")
        }
        option(1, "I’ll put it on the wall.", HAPPY, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("Now it greets you every morning, and reminds you of who you are.")
        }
        option(2, "I’ll tuck it away again.", TIRED, SHRUG, CLOSE_UP) {
            act(PLAYER, HEAD_DOWN, SAD, 1300)
            narrate("Some memories are better kept in a drawer.")
        }
        option(3, "I’ll show my family and tell the story.", HAPPY, EXPLAIN, MEDIUM) {
            enter(SIBLING, Edge.RIGHT, 0.68f)
            say(SIBLING, "What have you got there? Wait. Is that…?", SURPRISED, POINT, TWO_SHOT)
            say(PLAYER, "Sit down. It’s a long story.", LAUGHING, GENTLE_SMILE)
            narrate("They ask a hundred questions, and you’re delighted to answer every one.")
        }
    }

    scene("fill_new_habit", APARTMENT, MORNING) {
        place(PLAYER, 0.36f, RIGHT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        narrate("Reading, stretching, writing, calling family. A small daily habit.")
        say(PLAYER, "I keep saying I’ll start.", TIRED, SHRUG, MEDIUM)
        cue("knock")
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "Morning! Still planning that habit of yours?", HAPPY, WAVE, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1000)
        say(PLAYER, "Maybe today is the day.", HAPPY, GENTLE_SMILE, CLOSE_UP)

        option(0, "I’m starting right now. And I’m committing.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(FRIEND, "Look at you. Day one!", EXCITED, CLAP, TWO_SHOT)
            card("THIRTY DAYS LATER", null, 1800)
            act(PLAYER, NONE, PROUD, 1400)
            narrate("Day one is easy. Day thirty is a quiet victory.")
        }
        option(1, "Just five minutes a day. That’s all.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(FRIEND, "Five minutes? Sneaky. I like it.", LAUGHING, POINT, TWO_SHOT)
            narrate("Five minutes is nothing, and it turns out to be everything.")
        }
        option(2, "Maybe when things calm down.", TIRED, SHRUG, CLOSE_UP) {
            say(FRIEND, "“Later” is a very busy place.", NEUTRAL, SHRUG, REACTION)
            act(PLAYER, HEAD_DOWN, TIRED, 1200)
            narrate("The better time somehow never arrives.")
        }
        option(3, "Want to start this together with me?", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(FRIEND, "Count me in. Same time every day?", EXCITED, FIST_PUMP, TWO_SHOT)
            say(PLAYER, "Same time. No excuses.", HAPPY, HANDSHAKE)
            narrate("Doing it together makes it easy, and fun.")
        }
    }

    scene("fill_quiet_stretch", LIVING_ROOM) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FRIEND, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(FRIEND, Prop.CUP)
        cam(ESTABLISHING)
        narrate("Nothing dramatic happens for a while. The days find a comfortable rhythm.")
        say(FRIEND, "Funny. Nothing on the calendar at all.", HAPPY, SHRUG, MEDIUM)
        say(PLAYER, "A quiet week. I could get used to it.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        cam(PUSH_IN, PLAYER)
        say(FRIEND, "So, what will you do with all this time?", THOUGHTFUL, NONE, TWO_SHOT)

        option(0, "Spend it with the people I care about.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(FRIEND, "Starting with this cup of tea?", LAUGHING, GENTLE_SMILE, TWO_SHOT)
            narrate("Nothing happens, and that’s exactly the point. It’s a good few weeks.")
        }
        option(1, "I’ll learn something new.", EXCITED, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(FRIEND, "Teach me when you’re done.", LAUGHING, POINT, TWO_SHOT)
            narrate("A book, a video, a workshop. You come out knowing something you didn’t before.")
        }
        option(2, "I’ll look after my health.", CONFIDENT, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            stand(FRIEND)
            say(FRIEND, "A walk? I’ll come along.", HAPPY, WAVE, TWO_SHOT)
            exit(FRIEND, Edge.RIGHT, concurrent = true)
            exit(PLAYER, Edge.RIGHT)
            narrate("Walks, early nights and proper meals. You feel sharper.")
        }
        option(3, "I’ll save a little, and plan a little.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.DOCUMENTS)
            say(FRIEND, "Boring. And very wise.", LAUGHING, SHRUG, TWO_SHOT)
            narrate("A small plan today makes a big difference later.")
        }
    }

    scene("rnd_reunion", COFFEE_SHOP) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(FRIEND, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(TWO_SHOT, PLAYER, FRIEND)
        narrate("An invitation has arrived: the school reunion.")
        say(FRIEND, "Did you see? The reunion invitation.", EXCITED, POINT, MEDIUM)
        say(FRIEND, "Everyone will be comparing careers, of course.", LAUGHING, SHRUG, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(PLAYER, "That’s exactly what worries me.", WORRIED, NONE, CLOSE_UP)
        whenever(has("mocked_underdog")) {
            narrate("You also remember some things you did back then that you’re not proud of.")
        }
        say(FRIEND, "So? Are you going?", HAPPY, NONE, TWO_SHOT)

        option(0, "I’ll go, and just enjoy it.", HAPPY, NOD, MEDIUM) {
            say(FRIEND, "Good. Let’s go together.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("It’s warmer and kinder than you feared. Everyone is just a person who got older.")
        }
        option(1, "I’ll go, and show them how well I’ve done.", CONFIDENT, POINT, MEDIUM) {
            say(FRIEND, "…Is that really the point?", THOUGHTFUL, NONE, CLOSE_UP)
            narrate("People nod politely. You leave with a faint feeling of having missed the point.")
        }
        option(2, "I’m skipping it.", TIRED, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Suit yourself. I’ll send you photos.", NEUTRAL, SHRUG, TWO_SHOT)
            narrate("You spend a quiet evening at home. The photos arrive the next day.")
        }
        option(3, "I’ll go, and find the people I lost touch with.", HAPPY, EXPLAIN, MEDIUM) {
            say(FRIEND, "Then I’m coming too. Let’s find them.", EXCITED, FIST_PUMP, TWO_SHOT)
            narrate("Two hours with an old friend are worth the whole trip.")
        }
    }

    scene("rnd_old_friend_call", COFFEE_SHOP, EVENING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        narrate("Your phone buzzes. A message, out of the blue.")
        voice(FRIEND, "Remember me? I found some old photos, and thought of you.", HAPPY)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 900)
        say(PLAYER, "Wow. That takes me back.", HAPPY, GENTLE_SMILE, CLOSE_UP)
        say(PLAYER, "I’ve been meaning to reach out for ages.", EMBARRASSED, SCRATCH_HEAD, MEDIUM)

        option(0, "I’m calling you right now.", EXCITED, PHONE, CLOSE_UP) {
            voice(FRIEND, "You called! I can’t believe it!", EXCITED)
            act(PLAYER, NONE, LAUGHING, 1200)
            narrate("A two-hour call, a lot of laughter and a promise to meet. This time you keep it.")
        }
        option(1, "I’ll reply with a quick message.", NEUTRAL, NOD, MEDIUM) {
            act(PLAYER, TYPE, NEUTRAL, 1200)
            narrate("A thumbs-up, a heart. It’s the minimum, but it’s something.")
        }
        option(2, "Come and stay for the weekend!", EXCITED, WAVE, MEDIUM) {
            voice(FRIEND, "Are you serious? I’ll bring the photos!", EXCITED)
            narrate("The weekend is chaotic and full of memories. You vow not to lose touch again.")
        }
        option(3, "I’m too busy right now. Later.", TIRED, SHRUG, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, TIRED, 1300)
            narrate("The message sits unanswered, and you feel it every time you open your phone.")
        }
    }

    scene("rnd_stranger_kindness", STREET, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        narrate("It’s been a hard week. You’re soaked, your phone is dead, and the bus has gone.")
        act(PLAYER, HEAD_DOWN, TIRED, 1200)
        say(PLAYER, "Dead phone. Missed bus. Of course.", TIRED, SHRUG, MEDIUM)
        enter(STRANGER, Edge.RIGHT, 0.66f)
        hold(STRANGER, Prop.BAG)
        say(STRANGER, "Here. Take this umbrella, and a charger.", HAPPY, EXPLAIN, TWO_SHOT)
        act(PLAYER, NONE, SURPRISED, 900)
        say(STRANGER, "No strings. Just pay it forward.", HAPPY, GENTLE_SMILE, CLOSE_UP)

        option(0, "Thank you. I promise I’ll pay it forward.", HAPPY, NOD, MEDIUM) {
            say(STRANGER, "That’s all I ask.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("A small moment becomes a memory. You do pay it forward, later that month.")
        }
        option(1, "Thank you. But let me pay you back.", NEUTRAL, NOD, MEDIUM) {
            say(STRANGER, "Not necessary. Really.", HAPPY, SHAKE_HEAD, TWO_SHOT)
            narrate("They decline, and smile. You walk home dry, and thoughtful.")
        }
        option(2, "That’s kind, but I can manage.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "As you like. Take care.", NEUTRAL, WAVE)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NONE, TIRED, 1200)
            narrate("You walk home soaked, and a little proud. Possibly foolish.")
        }
        option(3, "What do you want in return?", WORRIED, EXPLAIN, CLOSE_UP) {
            act(STRANGER, NONE, SURPRISED, 800)
            say(STRANGER, "Nothing. Honestly.", LAUGHING, SHRUG, TWO_SHOT)
            narrate("You realise how guarded the world has made you.")
        }
    }

    scene("rnd_stray_pet", STREET, EVENING) {
        place(PLAYER, 0.62f, LEFT)
        cam(ESTABLISHING)
        narrate("A scruffy stray dog has followed you all the way home.")
        act(PLAYER, NONE, SURPRISED, 900)
        say(PLAYER, "Hey. You’ve followed me the whole way.", SURPRISED, NONE, MEDIUM)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 900)
        say(PLAYER, "No collar. No tag. Where do you belong?", WORRIED, NONE)
        narrate("It sits on your doorstep, looking up with enormous eyes.")

        option(0, "Come on, then. You’re staying with me.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("Within a week, you can’t imagine life without the furry shadow at your heels.")
        }
        option(1, "I’ll take you to a shelter.", NEUTRAL, NOD, MEDIUM) {
            say(PLAYER, "They’ll check for a chip. I’ll leave my number.", THOUGHTFUL, EXPLAIN)
            exit(PLAYER, Edge.LEFT)
            narrate("A tidy, kind solution.")
        }
        option(2, "I’ll post photos and look for your owner.", CONFIDENT, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            act(PLAYER, TYPE, THOUGHTFUL, 1200)
            card("A FEW DAYS LATER", null, 1800)
            hold(PLAYER, Prop.NONE)
            enter(CHILD, Edge.LEFT, 0.30f, run = true)
            say(CHILD, "That’s him! That’s our dog!", EXCITED, HANDS_UP, MEDIUM)
            act(CHILD, HUG, HAPPY, 1500)
            narrate("A very emotional family arrives to collect their dog.")
        }
        option(3, "Go on. Shoo. I can’t take you.", WORRIED, HANDS_UP, MEDIUM) {
            act(PLAYER, HEAD_DOWN, SAD, 1300)
            narrate("It looks back at you from the corner of the street. You don’t sleep well.")
        }
    }

}
