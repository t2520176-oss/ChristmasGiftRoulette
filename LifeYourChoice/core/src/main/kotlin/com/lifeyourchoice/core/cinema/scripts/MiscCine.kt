package com.lifeyourchoice.core.cinema.scripts

import com.lifeyourchoice.core.cinema.ActorId.*
import com.lifeyourchoice.core.cinema.Edge
import com.lifeyourchoice.core.cinema.Emotion.*
import com.lifeyourchoice.core.cinema.Facing.*
import com.lifeyourchoice.core.cinema.Gesture.*
import com.lifeyourchoice.core.cinema.Prop
import com.lifeyourchoice.core.cinema.Shot.*
import com.lifeyourchoice.core.cinema.TimeOfDay.*
import com.lifeyourchoice.core.cinema.cinePack
import com.lifeyourchoice.core.model.SceneArt.*
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.story.*

/** Everyday money decisions, side projects and random life events. */
val MiscCine = cinePack("misc") {

    // =====================================================================================
    //  BUSINESS
    // =====================================================================================

    scene("biz_ethics", MEETING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("The biggest contract your business has ever chased.")
        enter(STRANGER, Edge.RIGHT, 0.72f)
        say(STRANGER, "Congratulations on the shortlist, {name}.", HAPPY, HANDSHAKE, MEDIUM)
        say(PLAYER, "Thank you. We’ve worked hard for this.", HAPPY, NONE, CLOSE_UP)
        say(STRANGER, "Hard work rarely wins these. A friendly tip.", CONFIDENT, SHRUG, TWO_SHOT)
        say(STRANGER, "A quiet gift to the right official clinches it.", NEUTRAL, NONE, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)
        say(STRANGER, "It’s how things are done around here.", CONFIDENT, SHRUG, TWO_SHOT)
        narrate("Your whole team’s hopes sit on this one contract.")

        option(0, "Fine. The business needs this contract.", WORRIED, NOD, MEDIUM) {
            say(STRANGER, "Wise. A pleasure doing business with you.", HAPPY, HANDSHAKE, TWO_SHOT)
            exit(STRANGER, Edge.RIGHT)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, WORRIED, 1500)
            narrate("You get the contract, and a new secret to keep.")
        }
        option(1, "No. We win this on merit, or not at all.", CONFIDENT, POINT, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 800)
            say(STRANGER, "Noble. Let’s see how far that takes you.", NEUTRAL, SHRUG, TWO_SHOT)
            exit(STRANGER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1000)
            card("A WEEK LATER", ms = 1800)
            cue("phone")
            hold(PLAYER, Prop.PHONE)
            outcome(0) {
                voice(CUSTOMER, "We value honesty. The contract is yours.", HAPPY)
                hold(PLAYER, Prop.NONE)
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1200)
                narrate("The client respects your honesty. You get the contract.")
            }
            outcome(1) {
                voice(CUSTOMER, "We’re sorry. We’ve gone with someone else.", NEUTRAL)
                hold(PLAYER, Prop.NONE)
                act(PLAYER, HEAD_DOWN, DISAPPOINTED, 1300)
                narrate("You lose the contract, but your name stays clean.")
                card("MONTHS LATER", ms = 1800)
                cue("phone")
                hold(PLAYER, Prop.PHONE)
                voice(CUSTOMER, "We heard how you handled that. Let’s talk.", HAPPY)
                hold(PLAYER, Prop.NONE)
                act(PLAYER, NONE, PROUD, 1200)
                narrate("A bigger client hears about it, and calls.")
            }
        }
        option(2, "That’s wrong. I’m reporting this.", ANGRY, POINT, MEDIUM) {
            act(STRANGER, NONE, AFRAID, 900)
            say(STRANGER, "You’ll regret this. Think it over!", AFRAID, HANDS_UP, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT, run = true)
            hold(PLAYER, Prop.PHONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, PHONE, THOUGHTFUL, 1500)
            hold(PLAYER, Prop.NONE)
            narrate("It’s a risk. But the officials are investigated.")
            narrate("People in your industry quietly notice your integrity.")
        }
        option(3, "Give me a day. I want my mentor’s advice.", THOUGHTFUL, NOD, MEDIUM) {
            say(STRANGER, "Of course. But don’t wait too long.", NEUTRAL, NOD, TWO_SHOT)
            exit(STRANGER, Edge.RIGHT)
            card("THAT EVENING", ms = 1800)
            enter(MENTOR, Edge.RIGHT, 0.68f)
            say(MENTOR, "You already know the answer, {name}.", THOUGHTFUL, GENTLE_SMILE, TWO_SHOT)
            say(PLAYER, "I know. I just needed to hear it said.", SAD, NONE, CLOSE_UP)
            say(MENTOR, "Then let’s find another way to compete.", CONFIDENT, NOD, MEDIUM)
            narrate("You don’t pay, and you find another way to win.")
        }
    }

    scene("biz_key_employee", SMALL_BUSINESS, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("Closing time. Your best employee asks for a word.")
        enter(COWORKER, Edge.RIGHT, 0.68f)
        say(COWORKER, "{name}, do you have a minute?", WORRIED, NONE, MEDIUM)
        say(PLAYER, "Of course. Is everything alright?", NEUTRAL, GENTLE_SMILE, TWO_SHOT)
        say(COWORKER, "Another company offered me more money.", WORRIED, SHRUG, CLOSE_UP)
        say(COWORKER, "I like it here. But I have a family to support.", SAD, HEAD_DOWN, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        narrate("They’ve helped you build every corner of this place.")
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Let’s match the offer. You’ve earned it.", HAPPY, HANDSHAKE, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "Really? That means everything, {name}.", HAPPY, HANDSHAKE, CLOSE_UP)
            narrate("They stay, and the whole team notices how you treat those who build the business.")
        }
        option(1, "How about a share of the profits instead?", CONFIDENT, EXPLAIN, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "A real stake in the business?", SURPRISED, HANDS_UP, CLOSE_UP)
            say(PLAYER, "If the business wins, we all win.", HAPPY, NOD, TWO_SHOT)
            act(COWORKER, HANDSHAKE, HAPPY, 1200)
            narrate("It aligns everyone. It’s one of your best decisions.")
        }
        option(2, "I understand. I won’t stand in your way.", SAD, SHRUG, MEDIUM) {
            say(COWORKER, "Thank you for understanding, {name}.", SAD, HANDSHAKE, CLOSE_UP)
            exit(COWORKER, Edge.RIGHT)
            card("A FEW WEEKS LATER", ms = 1800)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1400)
            narrate("The business runs, but the loss is felt in many small ways.")
        }
        option(3, "What would make you stay? Tell me honestly.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(COWORKER, THINK, THOUGHTFUL, 1000)
            say(COWORKER, "Honestly? Fewer late shifts. And a proper title.", HAPPY, SHRUG, CLOSE_UP)
            say(PLAYER, "That I can do. Anything else?", HAPPY, NOD, TWO_SHOT)
            say(COWORKER, "Just feeling appreciated. That’s all.", HAPPY, GENTLE_SMILE, MEDIUM)
            narrate("It turns out money wasn’t the only thing. A few changes, and they’re happy.")
        }
    }

    // =====================================================================================
    //  CAREER
    // =====================================================================================

    scene("car_senior_promotion", MEETING) {
        place(PLAYER, 0.30f, RIGHT)
        place(BOSS, 0.72f, LEFT)
        hold(BOSS, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("Years of work. Now, a door is opening.")
        say(BOSS, "{name}, you’re among our most experienced people.", PROUD, GENTLE_SMILE, MEDIUM)
        say(BOSS, "A senior position is opening. Real authority.", NEUTRAL, EXPLAIN, TWO_SHOT)
        say(BOSS, "And real pressure. Are you ready for it?", NEUTRAL, NONE, CLOSE_UP)
        whenever(atLeast(Stat.REPUTATION, 65)) {
            say(BOSS, "Honestly, half the company already asked for you.", HAPPY, NOD, MEDIUM)
        }
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        say(PLAYER, "That’s a lot to take in.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "I’m ready. I’ll take the role.", CONFIDENT, HANDSHAKE, MEDIUM) {
            say(BOSS, "Good. I’ll put your name forward.", PROUD, NOD, TWO_SHOT)
            card("A FEW WEEKS LATER", ms = 1800)
            outcome(0) {
                say(BOSS, "It’s official, {name}. Welcome to the senior team.", HAPPY, HANDSHAKE, MEDIUM)
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                narrate("The authority is real. So is the weight of it.")
            }
            outcome(1) {
                say(BOSS, "We hired externally. I need you to train them.", NEUTRAL, SHRUG, CLOSE_UP)
                act(PLAYER, HEAD_DOWN, DISAPPOINTED, 1300)
                narrate("Frustrating. But your value has been noted.")
            }
        }
        option(1, "Could I shape the role around how I work best?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(BOSS, THINK, SURPRISED, 900)
            say(BOSS, "That’s unusual. What would you change?", THOUGHTFUL, THINK, CLOSE_UP)
            say(PLAYER, "Fewer meetings, more mentoring, clear goals.", CONFIDENT, EXPLAIN, MEDIUM)
            outcome(0) {
                say(BOSS, "Your terms are accepted. A rare compliment.", HAPPY, HANDSHAKE, TWO_SHOT)
                act(PLAYER, NOD, HAPPY, 1100)
                narrate("You grow into the role in your own way.")
            }
            outcome(1) {
                say(BOSS, "I like the ideas. Not the conditions.", NEUTRAL, SHAKE_HEAD, CLOSE_UP)
                act(PLAYER, NOD, THOUGHTFUL, 1200)
                narrate("A respectful stalemate.")
            }
        }
        option(2, "Let me recommend someone from my team.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(BOSS, NONE, SURPRISED, 800)
            say(BOSS, "Someone else? Who do you have in mind?", THOUGHTFUL, NONE, CLOSE_UP)
            enter(COWORKER, Edge.LEFT, 0.12f)
            say(PLAYER, "{coworker}. I’ll mentor them myself.", PROUD, POINT, TWO_SHOT)
            say(COWORKER, "Me? {name}, I don’t know what to say.", SURPRISED, HANDS_UP, REACTION)
            narrate("Your protégé is promoted, and you become the quiet backbone of the department.")
        }
        option(3, "Thank you, but I’ll turn it down.", NEUTRAL, GENTLE_SMILE, MEDIUM) {
            act(BOSS, NONE, SURPRISED, 900)
            say(BOSS, "Really? You’re sure?", SURPRISED, NONE, CLOSE_UP)
            say(PLAYER, "I’d like to keep my evenings. And my sanity.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            say(BOSS, "Fair enough. Enjoy them.", NEUTRAL, NOD, MEDIUM)
            narrate("You keep your evenings, your hobbies and your sanity.")
        }
    }

    scene("car_side_project", APARTMENT, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        narrate("Every evening, after work, the same glow of a screen.")
        act(PLAYER, TYPE, THOUGHTFUL, 1400)
        cue("knock")
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "Still working on your secret project?", LAUGHING, POINT, MEDIUM)
        say(PLAYER, "It’s just a hobby. Nothing special.", EMBARRASSED, SCRATCH_HEAD, TWO_SHOT)
        say(FRIEND, "Nothing special? Everyone I showed it to loved it.", HAPPY, EXPLAIN, CLOSE_UP)
        say(FRIEND, "This could really become something, {name}.", EXCITED, CLAP, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        say(PLAYER, "Or it could stay a hobby. I can’t decide.", THOUGHTFUL, SHRUG, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’m going all in. Launching it publicly.", EXCITED, FIST_PUMP, MEDIUM) {
            act(FRIEND, CLAP, EXCITED, 1000)
            say(FRIEND, "Yes! Press the button!", EXCITED, FIST_PUMP, REACTION)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, AFRAID, 1300)
            cue("whoosh")
            card("LAUNCH NIGHT", ms = 1800)
            outcome(0) {
                card("A YEAR LATER", ms = 1800)
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1200)
                narrate("A small audience grows. Within a year, strangers are paying for your work.")
            }
            outcome(1) {
                act(PLAYER, NOD, THOUGHTFUL, 1300)
                say(FRIEND, "The next one will be even better.", HAPPY, GENTLE_SMILE, TWO_SHOT)
                narrate("A few kind comments, and not much else. You’ve learned a lot.")
            }
        }
        option(1, "I’ll keep it a hobby. It’s my happy place.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(FRIEND, "Fair. Just keep making things.", HAPPY, NOD, TWO_SHOT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, TYPE, HAPPY, 1500)
            narrate("It keeps you creative, and a little less stressed.")
        }
        option(2, "Be honest, {friend}. Is it actually good?", WORRIED, EXPLAIN, MEDIUM) {
            act(FRIEND, THINK, THOUGHTFUL, 1000)
            say(FRIEND, "Honestly? The middle drags. The ending is great.", THOUGHTFUL, EXPLAIN, CLOSE_UP)
            say(PLAYER, "Ouch. Thank you. Keep going.", EMBARRASSED, SCRATCH_HEAD, TWO_SHOT)
            card("A WEEK LATER", ms = 1800)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, CONFIDENT, 1500)
            narrate("{friend}’s notes are brutal and generous. Your next version is much stronger.")
        }
        option(3, "Forget it. I don’t have the time.", SAD, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, SAD, 1000)
            say(FRIEND, "Really? That’s a shame, {name}.", SAD, SHRUG, REACTION)
            exit(FRIEND, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, SAD, 1500)
            narrate("Evenings are quiet. Part of you wonders what that project could have become.")
        }
    }

    scene("car_training", OFFICE, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(PLAYER, TYPE, TIRED, 1000)
        enter(BOSS, Edge.RIGHT, 0.72f)
        hold(BOSS, Prop.DOCUMENTS)
        say(BOSS, "Company training on the new technology. Optional.", NEUTRAL, EXPLAIN, MEDIUM)
        say(BOSS, "After hours. Unpaid. Two evenings a week.", NEUTRAL, SHRUG, TWO_SHOT)
        say(BOSS, "But everyone says it will matter in a few years.", NEUTRAL, NOD, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, TIRED, 1000)
        say(PLAYER, "After a full day? Every single week?", TIRED, FACEPALM, CLOSE_UP)
        say(BOSS, "Think it over. Sign-ups close on Friday.", NEUTRAL, POINT, OVER_SHOULDER)
        narrate("Your evenings, or your future?")

        option(0, "I’ll sign up and attend every session.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(BOSS, "Good. I’ll put your name on the list.", PROUD, NOD, TWO_SHOT)
            exit(BOSS, Edge.RIGHT)
            card("SIX MONTHS LATER", ms = 1800)
            enter(COWORKER, Edge.RIGHT, 0.68f)
            say(COWORKER, "{name}, you’re the expert now. Help me?", WORRIED, EXPLAIN, TWO_SHOT)
            act(PLAYER, NOD, PROUD, 1200)
            narrate("It was hard going. Now you’re the one the others ask for help.")
        }
        option(1, "Skip it. I have a life.", HAPPY, SHRUG, MEDIUM) {
            say(BOSS, "Your call. It’s up to you.", NEUTRAL, SHRUG, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            hold(PLAYER, Prop.BAG)
            cam(FOLLOW, PLAYER)
            exit(PLAYER, Edge.LEFT)
            narrate("You enjoy your evenings. The new technology arrives anyway.")
        }
        option(2, "I’ll learn it myself, at my own pace.", THOUGHTFUL, NOD, MEDIUM) {
            say(BOSS, "Resourceful. Don’t fall behind.", NEUTRAL, NOD, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            card("WEEKS OF TINKERING", ms = 1800)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 1800)
            act(PLAYER, FIST_PUMP, HAPPY, 1000)
            narrate("Slower, but personal. You learn it by building something you care about.")
        }
        option(3, "Could it be part of our working hours?", CONFIDENT, EXPLAIN, MEDIUM) {
            act(BOSS, THINK, SURPRISED, 900)
            say(BOSS, "Hm. I’ll take it upstairs.", THOUGHTFUL, THINK, CLOSE_UP)
            card("A DAY LATER", ms = 1800)
            outcome(0) {
                say(BOSS, "Approved. The training moves into working hours.", HAPPY, HANDSHAKE, MEDIUM)
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                narrate("Half the department thanks you.")
            }
            outcome(1) {
                say(BOSS, "Request denied. But you made your point.", NEUTRAL, SHRUG, CLOSE_UP)
                act(PLAYER, NONE, DISAPPOINTED, 1200)
                narrate("It was worth a try.")
            }
        }
    }

    // =====================================================================================
    //  FRIENDS
    // =====================================================================================

    scene("fri_peer_pressure_adult", NIGHT_CITY, EVENING) {
        place(PLAYER, 0.30f, RIGHT)
        place(FRIEND, 0.60f, LEFT)
        place(COWORKER, 0.84f, LEFT)
        hold(FRIEND, Prop.PHONE)
        cam(ESTABLISHING)
        say(FRIEND, "We’re booking the weekend trip! Beach, hotel, everything!", EXCITED, HANDS_UP, MEDIUM)
        say(COWORKER, "Everyone’s going. You can’t miss it, {name}!", HAPPY, WAVE, TWO_SHOT)
        say(PLAYER, "How much are we talking, roughly?", WORRIED, NONE, CLOSE_UP)
        say(FRIEND, "Not cheap. But memories are priceless!", LAUGHING, SHRUG, MEDIUM)
        hold(PLAYER, Prop.PHONE)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, NONE, WORRIED, 1200)
        narrate("You’ve seen your bank balance. It isn’t a happy number.")
        cam(WIDE)

        option(0, "Count me in. You only live once!", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            cue("cheer")
            act(FRIEND, FIST_PUMP, EXCITED, 900)
            say(COWORKER, "That’s the spirit!", HAPPY, CLAP, MEDIUM)
            card("A WEEKEND BY THE SEA", ms = 1800)
            act(PLAYER, FIST_PUMP, LAUGHING, 1200)
            card("THE FOLLOWING MONTH", ms = 1800)
            hold(PLAYER, Prop.PHONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, FACEPALM, WORRIED, 1400)
            narrate("The trip is memorable. The credit card statement, less so.")
        }
        option(1, "I’ll sit this one out. I need to save.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(COWORKER, NONE, DISAPPOINTED, 800)
            say(FRIEND, "Aw, really? It won’t be the same.", SAD, SHRUG, REACTION)
            say(PLAYER, "I know. It’s just not the right time.", NEUTRAL, GENTLE_SMILE, TWO_SHOT)
            say(COWORKER, "Fair enough. We’ll send you photos.", HAPPY, NOD, MEDIUM)
            narrate("A few raised eyebrows, but most understand. You’re a little ahead for it.")
        }
        option(2, "How about camping instead? Much cheaper.", HAPPY, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "Camping? Hm. Actually, that could be fun.", THOUGHTFUL, THINK, REACTION)
            say(COWORKER, "A campfire! I’m in. Let’s vote!", EXCITED, FIST_PUMP, MEDIUM)
            cue("cheer")
            act(PLAYER, FIST_PUMP, HAPPY, 1000)
            narrate("Camping wins the vote. It turns out to be the best weekend of the lot.")
        }
        option(3, "I’m in. I’ll pick up extra shifts.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "You’re the best, {name}!", HAPPY, CLAP, REACTION)
            card("WEEKS OF EXTRA SHIFTS", ms = 1800)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, TIRED, 1400)
            card("THE LONG WEEKEND", ms = 1800)
            cam(MEDIUM, PLAYER)
            act(PLAYER, GENTLE_SMILE, HAPPY, 1500)
            narrate("It’s a lot of hours for a long weekend. You earn every minute of the sun.")
        }
    }

    scene("fri_secret_trade", HALLWAY) {
        place(PLAYER, 0.34f, RIGHT)
        cam(ESTABLISHING)
        enter(STRANGER, Edge.RIGHT, 0.70f)
        say(STRANGER, "{name}, got a minute? I have an offer.", CONFIDENT, WAVE, MEDIUM)
        say(STRANGER, "A spot on my team. A big favour from me.", CONFIDENT, EXPLAIN, TWO_SHOT)
        say(PLAYER, "What’s the catch?", WORRIED, NONE, CLOSE_UP)
        say(STRANGER, "You’re close to {friend}. Tell me the secret.", NEUTRAL, POINT, OVER_SHOULDER)
        say(STRANGER, "The one you promised to keep. Nobody will know.", CONFIDENT, SHRUG, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1200)
        narrate("A real advantage, for one broken promise.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Okay. Here’s what {friend} told me.", SAD, HEAD_DOWN, MEDIUM) {
            say(STRANGER, "Smart. A pleasure doing business.", HAPPY, HANDSHAKE, TWO_SHOT)
            exit(STRANGER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1400)
            card("A MONTH LATER", ms = 1800)
            enter(FRIEND, Edge.LEFT, 0.12f)
            say(FRIEND, "…I know it was you, {name}.", SAD, HEAD_DOWN, MEDIUM)
            exit(FRIEND, Edge.LEFT)
            act(PLAYER, NONE, SAD, 1400)
            narrate("{friend} doesn’t shout. {friend} just stops calling.")
        }
        option(1, "No. And you should be ashamed to ask.", ANGRY, POINT, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 800)
            say(STRANGER, "Suit yourself. Your loss.", NEUTRAL, SHRUG, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NONE, CONFIDENT, 1100)
            card("LATER", ms = 1500)
            enter(FRIEND, Edge.LEFT, 0.12f)
            say(FRIEND, "I heard what you turned down. Thank you.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("Walking away cost you a favour. It earns you something better.")
        }
        option(2, "{friend} deserves to know about this.", THOUGHTFUL, NOD, MEDIUM) {
            say(STRANGER, "Don’t. It’ll only cause trouble.", WORRIED, HANDS_UP, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            enter(FRIEND, Edge.LEFT, 0.12f)
            say(PLAYER, "{friend}, we need to talk. Someone offered me a deal.", WORRIED, EXPLAIN, TWO_SHOT)
            cam(REACTION, FRIEND)
            act(FRIEND, NONE, SURPRISED, 1300)
            say(FRIEND, "Thank you for telling me. Not everyone would.", SAD, GENTLE_SMILE, TWO_SHOT)
            narrate("{friend} is quiet for a while. Then {friend} thanks you.")
        }
        option(3, "Here’s something harmless. That’s all.", NEUTRAL, SHRUG, MEDIUM) {
            act(STRANGER, NONE, DISAPPOINTED, 900)
            say(STRANGER, "That’s it? Hardly worth the favour.", NEUTRAL, SHRUG, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, WORRIED, 1300)
            narrate("A half-measure that satisfies no one. But nobody gets hurt either.")
        }
    }

    // =====================================================================================
    //  MONEY
    // =====================================================================================

    scene("mon_charity", STREET) {
        place(PLAYER, 0.30f, RIGHT)
        cam(ESTABLISHING)
        enter(STRANGER, Edge.RIGHT, 0.66f)
        hold(STRANGER, Prop.BOX)
        say(STRANGER, "Excuse me! Have you got a moment for kids’ reading?", HAPPY, WAVE, MEDIUM)
        say(STRANGER, "We run a reading programme for local children.", NEUTRAL, EXPLAIN, TWO_SHOT)
        say(STRANGER, "But we’re short of money. And of volunteers.", WORRIED, SHRUG, CLOSE_UP)
        say(PLAYER, "That sounds like a wonderful cause.", HAPPY, GENTLE_SMILE, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        narrate("You have a little money. Time is another matter.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’d like to donate. Here you go.", HAPPY, NOD, MEDIUM) {
            act(PLAYER, NOD, HAPPY, 800)
            say(STRANGER, "Thank you! That’s a lot of new books.", HAPPY, CLAP, CLOSE_UP)
            enter(CHILD, Edge.RIGHT, 0.88f, run = true)
            hold(CHILD, Prop.BOOK)
            say(CHILD, "Look! Is that who bought my book?", EXCITED, POINT, MEDIUM)
            act(PLAYER, NONE, HAPPY, 1200)
            narrate("A quiet satisfaction. A child will be reading because of you.")
        }
        option(1, "I can volunteer on weekends.", HAPPY, WAVE, MEDIUM) {
            say(STRANGER, "Really? You’re a lifesaver!", EXCITED, HANDS_UP, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            card("SATURDAY MORNING", ms = 1800)
            enter(CHILD, Edge.RIGHT, 0.64f, run = true)
            hold(CHILD, Prop.BOOK)
            say(CHILD, "Will you read this one to me?", EXCITED, EXPLAIN, TWO_SHOT)
            sit(PLAYER)
            sit(CHILD)
            say(PLAYER, "Of course. Once upon a time…", HAPPY, GENTLE_SMILE, CLOSE_UP)
            narrate("You meet people you’d never otherwise meet. The kids are brilliant.")
        }
        option(2, "Sorry, not this time.", EMBARRASSED, SHRUG, MEDIUM) {
            say(STRANGER, "No problem. Thanks for listening.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            cam(FOLLOW, PLAYER)
            move(PLAYER, 0.70f)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1200)
            narrate("You keep your weekends. It’s fair, but you feel a flicker of guilt.")
        }
        option(3, "Let’s organise a fundraiser with my friends.", EXCITED, EXPLAIN, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 900)
            say(STRANGER, "A fundraiser? That would be amazing!", EXCITED, HANDS_UP, CLOSE_UP)
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "A fundraiser? Count me in. I’ll bring everyone!", EXCITED)
            hold(PLAYER, Prop.NONE)
            card("FUNDRAISER NIGHT", ms = 1800)
            cue("cheer")
            act(PLAYER, FIST_PUMP, EXCITED, 1200)
            narrate("It raises far more than you expected, and you’re hooked on the feeling.")
        }
    }

    scene("mon_credit_card", APARTMENT) {
        place(PLAYER, 0.38f, RIGHT, seated = true)
        cam(ESTABLISHING)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "Hello? Yes, this is {name}.", NEUTRAL, PHONE, MEDIUM)
        voice(STRANGER, "Congratulations! You qualify for your first credit card.", HAPPY)
        voice(STRANGER, "A generous limit, and zero percent interest at first!", EXCITED)
        say(PLAYER, "Zero percent? Really?", SURPRISED, PHONE, CLOSE_UP)
        voice(STRANGER, "Everything you need could be yours today.", CONFIDENT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1200)
        narrate("A pile of things you “need” suddenly looks very reachable.")
        say(PLAYER, "It does sound tempting…", THOUGHTFUL, SCRATCH_HEAD, CLOSE_UP)

        option(0, "I’ll use it for essentials, and pay it off monthly.", CONFIDENT, NOD, MEDIUM) {
            act(PLAYER, NOD, CONFIDENT, 1000)
            card("EVERY MONTH, ON TIME", ms = 1800)
            hold(PLAYER, Prop.LAPTOP)
            act(PLAYER, TYPE, PROUD, 1500)
            narrate("You build a credit history, and a good habit.")
        }
        option(1, "Shopping spree! I deserve this.", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BAG)
            cue("whoosh")
            exit(PLAYER, Edge.LEFT, run = true)
            card("A MONTH OF SHOPPING", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.38f)
            hold(PLAYER, Prop.DOCUMENTS)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, FACEPALM, AFRAID, 1400)
            narrate("It’s thrilling until the statement arrives. Interest is a quiet, patient thing.")
        }
        option(2, "No, thanks. I don’t trust myself with it yet.", THOUGHTFUL, SHAKE_HEAD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(PLAYER, SHAKE_HEAD, THOUGHTFUL, 1200)
            narrate("A cautious choice. You’ll build credit another way, someday.")
        }
        option(3, "I’ll read the terms first, and call {friend}.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "Read me the fine print. Slowly.", THOUGHTFUL)
            say(PLAYER, "Zero percent for six months. Then… wow.", SURPRISED, PHONE, CLOSE_UP)
            voice(FRIEND, "That’s a trap. Find one with clear terms.", ANGRY)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NOD, CONFIDENT, 1200)
            narrate("The fine print had a few traps. You take a safer card with clear terms.")
        }
    }

    scene("mon_expensive_buy", STREET) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.64f, LEFT)
        cam(ESTABLISHING)
        say(FRIEND, "Look! The new one just launched!", EXCITED, POINT, MEDIUM)
        say(PLAYER, "Wow. It looks incredible.", EXCITED, NONE, CLOSE_UP)
        say(FRIEND, "Everyone’s getting it. They have instalment plans!", EXCITED, EXPLAIN, TWO_SHOT)
        say(PLAYER, "It’s a big chunk of what I have.", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(FRIEND, "But you could take it home today.", CONFIDENT, SHRUG, MEDIUM)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1200)
        narrate("Today, or smarter?")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’m buying it today. Instalments, here I come.", EXCITED, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            act(FRIEND, CLAP, EXCITED, 900)
            cue("cheer")
            card("A MONTH LATER", ms = 1800)
            hold(PLAYER, Prop.PHONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, WORRIED, 1400)
            narrate("It’s fantastic for about a month. The payments last a lot longer.")
        }
        option(1, "I’ll save up and buy it outright later.", CONFIDENT, NOD, MEDIUM) {
            say(FRIEND, "You’re no fun, {name}!", LAUGHING, POINT, REACTION)
            act(PLAYER, NONE, HAPPY, 800)
            card("SIX MONTHS LATER", ms = 1800)
            hold(PLAYER, Prop.BOX)
            act(PLAYER, FIST_PUMP, HAPPY, 1200)
            narrate("A newer model is out, and the old one is cheaper. You smile.")
        }
        option(2, "I’ll find a good second-hand one.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "Second-hand? Really?", SURPRISED, SHRUG, REACTION)
            say(PLAYER, "Ninety-five percent of the phone, half the price.", CONFIDENT, EXPLAIN, TWO_SHOT)
            hold(PLAYER, Prop.BOX)
            act(PLAYER, NONE, PROUD, 1100)
            narrate("You feel very clever.")
        }
        option(3, "Nah. I really don’t need it.", NEUTRAL, SHRUG, MEDIUM) {
            say(FRIEND, "Seriously? You’re passing on that?", SURPRISED, HANDS_UP, REACTION)
            say(PLAYER, "My old one works just fine.", NEUTRAL, GENTLE_SMILE, TWO_SHOT)
            cam(FOLLOW, PLAYER)
            move(PLAYER, 0.90f)
            narrate("A little pang, then it passes. Nobody remembers who owned what.")
        }
    }

    scene("mon_raise_lifestyle", APARTMENT, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("The email says it all. A raise, effective this month.")
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "A raise? Finally living a bit!", LAUGHING, CLAP, MEDIUM)
        say(FRIEND, "A nicer apartment. A car. Proper restaurants!", EXCITED, EXPLAIN, TWO_SHOT)
        say(PLAYER, "It all seems within reach now.", HAPPY, NONE, CLOSE_UP)
        say(FRIEND, "You’ve earned it. Say yes to everything!", EXCITED, POINT, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        narrate("A bigger paycheque. And bigger temptations.")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Upgrade everything. I’ve earned it.", EXCITED, FIST_PUMP, MEDIUM) {
            cue("cheer")
            act(FRIEND, FIST_PUMP, EXCITED, 900)
            card("A FEW MONTHS LATER", ms = 1800)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, FACEPALM, WORRIED, 1300)
            narrate("Life is more comfortable. The new baseline gets expensive quickly.")
        }
        option(1, "Same life as before. I’ll save the difference.", CONFIDENT, NOD, MEDIUM) {
            say(FRIEND, "Boring!", LAUGHING, POINT, REACTION)
            say(PLAYER, "Boring is what future me will thank me for.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("Your friends tease you a little. Your savings quietly don’t.")
        }
        option(2, "First, I’m paying off my debts.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "Debts first? That’s very grown-up.", SURPRISED, NOD, REACTION)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1500)
            narrate("It feels as if you’ve put down a backpack you’d been carrying for years.")
        }
        option(3, "I’m sharing some of it with my family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(MOTHER, "We’ll never forget this, {name}.", SAD)
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "That’s really kind of you.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("Your parents are overwhelmed.")
        }
    }

    scene("mon_inheritance", LIVING_ROOM) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(SIBLING, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        narrate("A letter from a lawyer. A distant relative has passed away.")
        say(SIBLING, "They left us something. Both of us.", SAD, GENTLE_SMILE, MEDIUM)
        say(PLAYER, "I barely knew them. But it was kind.", THOUGHTFUL, NONE, CLOSE_UP)
        say(SIBLING, "A modest inheritance. Not life-changing.", NEUTRAL, SHRUG, TWO_SHOT)
        cam(REACTION, SIBLING)
        act(SIBLING, NONE, THOUGHTFUL, 1000)
        say(SIBLING, "So. What will you do with yours, {name}?", NEUTRAL, NONE, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        say(PLAYER, "I’m not sure yet.", THOUGHTFUL, SCRATCH_HEAD, CLOSE_UP)

        option(0, "I’ll invest it for the long term.", CONFIDENT, NOD, MEDIUM) {
            say(SIBLING, "Sensible. Slow and steady.", HAPPY, NOD, TWO_SHOT)
            act(PLAYER, NOD, CONFIDENT, 1000)
            narrate("You split it carefully. Years from now, you’ll be glad you did.")
        }
        option(1, "Let’s share it equally, {sibling}.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(SIBLING, NONE, SURPRISED, 900)
            say(SIBLING, "Equally? Are you sure?", SURPRISED, HANDS_UP, REACTION)
            say(PLAYER, "Completely. Let’s just talk it through.", HAPPY, NOD, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            stand(SIBLING)
            move(PLAYER, 0.46f)
            act(SIBLING, HUG, HAPPY, 1600)
            narrate("A simple conversation turns a possible argument into a warm memory.")
        }
        option(2, "I’m taking the trip I’ve always dreamed of.", EXCITED, FIST_PUMP, MEDIUM) {
            say(SIBLING, "Go! And send me postcards!", LAUGHING, WAVE, REACTION)
            hold(PLAYER, Prop.SUITCASE)
            stand(PLAYER)
            cue("whoosh")
            exit(PLAYER, Edge.LEFT)
            card("FAR FROM HOME", ms = 2000)
            narrate("You see places you only read about. It’s extravagant, and unforgettable.")
        }
        option(3, "I’ll pay off my debts and start fresh.", THOUGHTFUL, NOD, MEDIUM) {
            say(SIBLING, "A fresh start. I like that.", PROUD, GENTLE_SMILE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1500)
            narrate("The weight lifts. For the first time in years, you start a month owing nothing.")
        }
    }

    scene("mon_rainy_day", APARTMENT, EVENING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        cam(ESTABLISHING)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "Hello? Oh no. Since when?", WORRIED, PHONE, MEDIUM)
        voice(COWORKER, "This morning. No warning. And I have no savings at all.", SAD)
        say(PLAYER, "I’m so sorry. Hang in there.", SAD, PHONE, CLOSE_UP)
        hold(PLAYER, Prop.NONE)
        act(PLAYER, HEAD_DOWN, WORRIED, 1200)
        narrate("It could happen to anyone. Even you.")
        enter(FRIEND, Edge.RIGHT, 0.68f)
        say(FRIEND, "You look like you’ve seen a ghost.", SURPRISED, NONE, MEDIUM)
        say(PLAYER, "A colleague was just laid off. Nothing saved.", WORRIED, EXPLAIN, TWO_SHOT)
        whenever(has("saved_money"), otherwise = {
            say(PLAYER, "And if it were me? I’m not sure I’d cope.", WORRIED, SHRUG, CLOSE_UP)
        }) {
            say(PLAYER, "I have some savings. But is it enough?", THOUGHTFUL, SCRATCH_HEAD, CLOSE_UP)
        }
        say(FRIEND, "Better to think about it now than later.", THOUGHTFUL, NOD, TWO_SHOT)

        option(0, "I’m building a proper emergency fund.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(FRIEND, "How much are you aiming for?", THOUGHTFUL, NONE, REACTION)
            say(PLAYER, "Three months of expenses. At the very least.", CONFIDENT, EXPLAIN, TWO_SHOT)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, NOD, CONFIDENT, 1100)
            narrate("It feels like having a seat belt on.")
        }
        option(1, "I’ll keep my money invested and working.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(FRIEND, "Can you reach it quickly if you need to?", THOUGHTFUL, THINK, REACTION)
            act(PLAYER, SCRATCH_HEAD, WORRIED, 1200)
            say(PLAYER, "Hm. That’s a very good question.", WORRIED, SHRUG, CLOSE_UP)
            narrate("A good return, but not much you can touch quickly. You’ll see how it holds up.")
        }
        option(2, "Don’t worry. It’ll all be fine.", HAPPY, SHRUG, MEDIUM) {
            say(FRIEND, "If you say so…", WORRIED, SHRUG, REACTION)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("The sun comes out, and you forget about it. Hopefully it stays out.")
        }
        option(3, "Let’s work out a plan together, {friend}.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(FRIEND, "Pen and paper? I’ll make the tea.", HAPPY, CLAP, TWO_SHOT)
            hold(PLAYER, Prop.DOCUMENTS)
            hold(FRIEND, Prop.CUP)
            act(PLAYER, TYPE, HAPPY, 1500)
            narrate("A shared plan feels lighter, and much more likely to happen.")
        }
    }

    scene("mon_retirement_plan", LIVING_ROOM, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.66f, LEFT)
        hold(FRIEND, Prop.BOX)
        cam(ESTABLISHING)
        narrate("Another birthday. This one has a very round number.")
        say(FRIEND, "Happy birthday! A big round one this year!", HAPPY, WAVE, MEDIUM)
        hold(FRIEND, Prop.NONE)
        say(FRIEND, "So, what about those slow mornings and travel?", HAPPY, EXPLAIN, TWO_SHOT)
        say(PLAYER, "I daydream about it. Then I wonder about the money.", THOUGHTFUL, SCRATCH_HEAD, CLOSE_UP)
        whenever(has("saved_money"), otherwise = {
            say(PLAYER, "Honestly, I wish I’d started saving sooner.", WORRIED, SHRUG, MEDIUM)
        }) {
            say(PLAYER, "My savings are a comfort. But is it enough?", THOUGHTFUL, NOD, MEDIUM)
        }
        say(FRIEND, "Better to look at it now than later.", THOUGHTFUL, NOD, TWO_SHOT)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)

        option(0, "I’ll save more and cut the waste.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Discipline looks good on you.", PROUD, GENTLE_SMILE, TWO_SHOT)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, NOD, CONFIDENT, 1200)
            narrate("You tighten your belt, and your future self quietly smiles.")
        }
        option(1, "I’ll carry on as I am. It’ll work out.", HAPPY, SHRUG, MEDIUM) {
            say(FRIEND, "Hm. If you say so, {name}.", WORRIED, SHRUG, REACTION)
            act(PLAYER, NONE, HAPPY, 1200)
            narrate("A comfortable denial. Retirement may need some adjustments.")
        }
        option(2, "I’ll work part-time a few more years.", THOUGHTFUL, NOD, MEDIUM) {
            say(FRIEND, "A paycheque and a purpose. Not bad.", HAPPY, NOD, TWO_SHOT)
            narrate("A smoother glide to retirement, with income and purpose.")
        }
        option(3, "I’ll book a meeting with a financial adviser.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(FRIEND, "Smart move. Good luck!", HAPPY, WAVE, REACTION)
            exit(FRIEND, Edge.RIGHT)
            card("ONE HOUR WITH AN ADVISER", ms = 2000)
            enter(STRANGER, Edge.RIGHT, 0.66f)
            hold(STRANGER, Prop.DOCUMENTS)
            say(STRANGER, "There are some gaps. But all fixable.", NEUTRAL, EXPLAIN, TWO_SHOT)
            act(PLAYER, NOD, SURPRISED, 1200)
            narrate("An hour with an expert reveals gaps you hadn’t noticed, and fixes you can start this week.")
        }
    }

    // =====================================================================================
    //  RANDOM EVENTS
    // =====================================================================================

    scene("rnd_found_cash", STREET, EVENING) {
        cam(ESTABLISHING)
        narrate("Walking home after a long day. The street is almost empty.")
        enter(PLAYER, Edge.LEFT, 0.34f)
        cue("steps")
        act(PLAYER, NONE, SURPRISED, 700)
        say(PLAYER, "Wait. Is that a roll of cash?", SURPRISED, POINT, CLOSE_UP)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(PUSH_IN, PLAYER)
        say(PLAYER, "Hello? Did anybody drop this?", WORRIED, NONE, MEDIUM)
        turn(PLAYER, LEFT)
        pause(500)
        turn(PLAYER, RIGHT)
        cam(WIDE)
        narrate("Nobody around. No way of telling whose it is.")
        say(PLAYER, "No name. No wallet. Nothing.", THOUGHTFUL, SHRUG, CLOSE_UP)
        say(PLAYER, "So… what do I do with it?", WORRIED, SCRATCH_HEAD, MEDIUM)

        option(0, "I’ll hand it in at the nearest shop.", HAPPY, NOD, MEDIUM) {
            enter(STRANGER, Edge.RIGHT, 0.68f)
            say(STRANGER, "Very honest of you. Leave your name, just in case.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            card("A FEW DAYS LATER", ms = 1800)
            say(STRANGER, "A woman came for it. Relieved. So grateful.", HAPPY, CLAP, MEDIUM)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("The right thing feels quietly wonderful.")
        }
        option(1, "Finders keepers. Nobody will miss it.", CONFIDENT, SHRUG, MEDIUM) {
            act(PLAYER, NONE, HAPPY, 800)
            cue("steps")
            move(PLAYER, 0.82f)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, SCRATCH_HEAD, WORRIED, 1500)
            narrate("It’s a small windfall, and a small, persistent itch in your conscience.")
        }
        option(2, "I’ll drop it in a charity box.", HAPPY, GENTLE_SMILE, MEDIUM) {
            enter(STRANGER, Edge.RIGHT, 0.68f)
            hold(STRANGER, Prop.BOX)
            say(STRANGER, "Every little bit helps. Thank you!", HAPPY, GENTLE_SMILE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1400)
            narrate("It feels clean and simple. Someone, somewhere, is helped by it.")
        }
        option(3, "I’ll leave a note and wait a while.", THOUGHTFUL, NOD, MEDIUM) {
            sit(PLAYER)
            card("AN HOUR LATER", ms = 1800)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, TIRED, 1400)
            stand(PLAYER)
            narrate("Nobody appears, so you hand it in. It’s the right thing, even if it was inconvenient.")
        }
    }

    scene("rnd_restructuring", OFFICE) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        narrate("An all-staff email lands. “Organisational changes, next month.”")
        enter(COWORKER, Edge.RIGHT, 0.66f)
        say(COWORKER, "Did you read it? “Organisational changes”?", WORRIED, EXPLAIN, MEDIUM)
        say(COWORKER, "People say someone from every team goes.", AFRAID, SHRUG, TWO_SHOT)
        say(PLAYER, "That’s just a rumour. Isn’t it?", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(COWORKER, "Nobody knows who’s safe. Nobody.", SAD, SHRUG, MEDIUM)
        cam(WIDE)
        narrate("Everyone is whispering about who’s safe and who’s not.")
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "I’ll update my résumé. Just in case.", THOUGHTFUL, NOD, MEDIUM) {
            say(COWORKER, "Smart. But keep it quiet.", WORRIED, NOD, REACTION)
            card("THAT EVENING", ms = 1800)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 1600)
            narrate("You don’t panic. You prepare, which is almost as good as being safe.")
        }
        option(1, "I’ll work extra hard to prove my worth.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(COWORKER, "Don’t burn out, {name}.", WORRIED, SHAKE_HEAD, REACTION)
            exit(COWORKER, Edge.RIGHT)
            card("WEEKS OF LATE NIGHTS", ms = 1800)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 1800)
            enter(BOSS, Edge.RIGHT, 0.66f)
            say(BOSS, "Your output is remarkable lately.", PROUD, NOD, TWO_SHOT)
            act(PLAYER, NONE, TIRED, 1000)
            narrate("You become the person nobody dares lay off, though at some cost.")
        }
        option(2, "I’m going to ask {boss} straight out.", CONFIDENT, EXPLAIN, MEDIUM) {
            say(COWORKER, "You’re braver than me. Good luck.", SURPRISED, NOD, REACTION)
            exit(COWORKER, Edge.RIGHT)
            stand(PLAYER)
            hold(PLAYER, Prop.NONE)
            enter(BOSS, Edge.RIGHT, 0.68f)
            say(PLAYER, "{boss}, may I ask about the changes?", NEUTRAL, EXPLAIN, TWO_SHOT)
            say(BOSS, "I can’t say much. But thank you for asking directly.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
            act(PLAYER, NOD, HAPPY, 1100)
            narrate("It helps more than you’d expect.")
        }
        option(3, "I’m ignoring the rumours. It’ll settle.", NEUTRAL, SHRUG, MEDIUM) {
            say(COWORKER, "How can you be so calm?", SURPRISED, HANDS_UP, REACTION)
            say(PLAYER, "Whatever happens, we’ll deal with it then.", NEUTRAL, GENTLE_SMILE, TWO_SHOT)
            act(PLAYER, TYPE, HAPPY, 1200)
            narrate("The rumours fade. Whatever happens, you’ll deal with it then.")
        }
    }

    scene("rnd_freelance_offer", COFFEE_SHOP) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        enter(STRANGER, Edge.RIGHT, 0.68f)
        say(STRANGER, "{name}! We met at the networking event.", HAPPY, WAVE, MEDIUM)
        sit(STRANGER)
        say(STRANGER, "I have a freelance project. Good pay.", HAPPY, EXPLAIN, TWO_SHOT)
        say(PLAYER, "What kind of project?", SURPRISED, NONE, CLOSE_UP)
        say(STRANGER, "Outside your usual work. And a short deadline.", NEUTRAL, SHRUG, MEDIUM)
        say(STRANGER, "I need your answer by tomorrow.", CONFIDENT, POINT, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1200)
        narrate("A good offer. A strange one. And a ticking clock.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’ll take it. I’ll figure it out.", EXCITED, HANDSHAKE, MEDIUM) {
            act(STRANGER, HANDSHAKE, HAPPY, 1000)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            card("A SLEEPLESS WEEK", ms = 1800)
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 1800)
            outcome(0) {
                cue("phone")
                act(PLAYER, FIST_PUMP, EXCITED, 1200)
                narrate("You deliver, narrowly, and the client sends two more referrals.")
            }
            outcome(1) {
                act(PLAYER, FACEPALM, TIRED, 1300)
                narrate("It’s harder than expected, and the client is unimpressed. A tough lesson.")
            }
        }
        option(1, "Can we move the deadline? Then I’m in.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(STRANGER, THINK, THOUGHTFUL, 1000)
            say(STRANGER, "Hm. Fine. A few extra days.", NEUTRAL, NOD, CLOSE_UP)
            say(PLAYER, "Deal. You’ll get my best work.", HAPPY, HANDSHAKE, TWO_SHOT)
            narrate("A little negotiation goes a long way.")
        }
        option(2, "Thank you, but I’ll have to decline.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "No hard feelings. Keep in touch.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NONE, NEUTRAL, 1200)
            narrate("You stay in your lane. Safe, stable, a bit boring.")
        }
        option(3, "I know someone perfect for this: {friend}.", HAPPY, NOD, MEDIUM) {
            say(STRANGER, "Send them my way.", NEUTRAL, NOD, CLOSE_UP)
            hold(PLAYER, Prop.PHONE)
            voice(FRIEND, "You did what?! Thank you, thank you!", EXCITED)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, HAPPY, 1200)
            narrate("{friend} gets the project and the confidence boost. You get a very big thank-you dinner.")
        }
    }

    scene("rnd_car_repair", STREET, MORNING) {
        place(PLAYER, 0.32f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        narrate("Clunk. Rattle. Then silence, halfway to work.")
        act(PLAYER, FACEPALM, WORRIED, 1000)
        enter(STRANGER, Edge.RIGHT, 0.70f)
        hold(STRANGER, Prop.BOX)
        say(STRANGER, "Let me take a look.", NEUTRAL, NONE, MEDIUM)
        cam(PUSH_IN, STRANGER)
        act(STRANGER, THINK, WORRIED, 1200)
        say(STRANGER, "Hm. That’s the engine.", WORRIED, SHAKE_HEAD, CLOSE_UP)
        say(STRANGER, "I’ll be honest. This repair won’t be cheap.", WORRIED, SHRUG, TWO_SHOT)
        say(PLAYER, "How expensive are we talking?", WORRIED, NONE, OVER_SHOULDER)
        say(STRANGER, "Way more than you’d like.", NEUTRAL, SHRUG, CLOSE_UP)
        narrate("A very large bill, and you still have to get to work.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Fine. Do the repair. I’ll pay.", NEUTRAL, NOD, MEDIUM) {
            say(STRANGER, "Good as new by this evening.", HAPPY, NOD, CLOSE_UP)
            whenever(has("saved_money"), otherwise = {
                act(PLAYER, HEAD_DOWN, WORRIED, 1300)
                narrate("You pay, and you drive away with a lighter wallet.")
            }) {
                act(PLAYER, NOD, CONFIDENT, 1100)
                narrate("Your emergency fund quietly takes the hit, so it’s only an annoyance.")
            }
        }
        option(1, "I’ll leave the car and take the bus.", THOUGHTFUL, SHRUG, MEDIUM) {
            say(STRANGER, "It’ll wait here for you.", NEUTRAL, NOD, CLOSE_UP)
            cue("steps")
            cam(FOLLOW, PLAYER)
            exit(PLAYER, Edge.LEFT)
            card("WEEKS ON THE BUS", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.32f)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("The commute is slower, but cheaper, and surprisingly pleasant after a while.")
        }
        option(2, "Is there a cheaper fix? Show me how.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(STRANGER, "There is. A patch-up. Hold this.", HAPPY, POINT, TWO_SHOT)
            move(PLAYER, 0.54f)
            act(PLAYER, SCRATCH_HEAD, TIRED, 1400)
            narrate("A patched-up fix that works fine. You learn some useful basics along the way.")
        }
        option(3, "Forget it. I’ll finance a newer car.", EXCITED, SHRUG, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 900)
            say(STRANGER, "A new car? Well. Good luck.", SURPRISED, SHRUG, CLOSE_UP)
            card("THE DEALERSHIP", ms = 1800)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, FIST_PUMP, HAPPY, 1200)
            act(PLAYER, FACEPALM, WORRIED, 1200)
            narrate("It’s a nice car. The monthly payment is less nice.")
        }
    }

    scene("rnd_viral", APARTMENT, MORNING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        act(PLAYER, NONE, SURPRISED, 800)
        say(PLAYER, "Why is my phone going crazy?", SURPRISED, SCRATCH_HEAD, CLOSE_UP)
        cue("knock")
        enter(FRIEND, Edge.RIGHT, 0.68f, run = true)
        say(FRIEND, "{name}! Your post! It’s everywhere!", EXCITED, HANDS_UP, MEDIUM)
        say(PLAYER, "Everywhere? What do you mean, everywhere?", SURPRISED, NONE, TWO_SHOT)
        say(FRIEND, "Thousands of strangers are talking about it!", EXCITED, POINT, CLOSE_UP)
        say(PLAYER, "My inbox is exploding. What do I do?", AFRAID, FACEPALM, MEDIUM)
        narrate("Overnight, everything has changed. Now what?")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Let’s ride the wave! I’ll post every day.", EXCITED, FIST_PUMP, MEDIUM) {
            say(FRIEND, "Yes! Let’s go!", EXCITED, FIST_PUMP, REACTION)
            card("A MONTH OF POSTING", ms = 1800)
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 1800)
            narrate("It’s exhausting, thrilling, and unreliable.")
        }
        option(1, "Let’s stay low-key. This feels weird.", EMBARRASSED, SHRUG, MEDIUM) {
            say(FRIEND, "Awww. My friend, the former celebrity!", LAUGHING, POINT, REACTION)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, HAPPY, 1300)
            narrate("The attention fades, and you’re relieved.")
        }
        option(2, "Let’s use it to help a cause that matters.", HAPPY, EXPLAIN, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 900)
            say(FRIEND, "That’s… actually amazing, {name}.", PROUD, GENTLE_SMILE, TWO_SHOT)
            cue("cheer")
            act(PLAYER, FIST_PUMP, HAPPY, 1200)
            narrate("You turn the spotlight onto something that deserves it. It raises more than you imagined.")
        }
        option(3, "Time to cash in. Quick deals, fast.", CONFIDENT, POINT, MEDIUM) {
            act(FRIEND, NONE, WORRIED, 900)
            say(FRIEND, "Are you sure about this?", WORRIED, SHRUG, REACTION)
            hold(PLAYER, Prop.DOCUMENTS)
            act(PLAYER, HANDSHAKE, CONFIDENT, 1300)
            narrate("A few quick deals, a few raised eyebrows. The money is real, and so is the whiff of opportunism.")
        }
    }

    scene("rnd_neighbor_move", STREET) {
        place(PLAYER, 0.26f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        narrate("You’re already running late. Then you see her on the stairs.")
        enter(STRANGER, Edge.RIGHT, 0.68f)
        hold(STRANGER, Prop.BOX)
        say(STRANGER, "Oh my. Just… one more step.", TIRED, NONE, MEDIUM)
        say(PLAYER, "That looks heavy. Are you moving in?", WORRIED, POINT, TWO_SHOT)
        say(STRANGER, "Don’t worry about me, dear. You’re busy.", TIRED, WAVE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        say(PLAYER, "I was just about to head out…", EMBARRASSED, SCRATCH_HEAD, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Everything else can wait. Let me help.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(STRANGER, NONE, SURPRISED, 900)
            say(STRANGER, "Oh, bless you!", HAPPY, HANDS_UP, CLOSE_UP)
            hold(STRANGER, Prop.NONE)
            hold(PLAYER, Prop.BOX)
            move(PLAYER, 0.52f)
            exit(PLAYER, Edge.RIGHT)
            card("AN HOUR LATER", ms = 1800)
            enter(PLAYER, Edge.RIGHT, 0.52f)
            hold(PLAYER, Prop.CUP)
            hold(STRANGER, Prop.CUP)
            sit(PLAYER)
            sit(STRANGER)
            say(STRANGER, "Tea, dear? And let me tell you a story.", HAPPY, EXPLAIN, TWO_SHOT)
            narrate("She tells you about her life, and gives you a wonderful recipe.")
        }
        option(1, "I’ll carry the heavy ones, then I must go.", NEUTRAL, NOD, MEDIUM) {
            hold(STRANGER, Prop.NONE)
            hold(PLAYER, Prop.BOX)
            move(PLAYER, 0.52f)
            say(STRANGER, "Thank you! That’s a huge help.", HAPPY, WAVE, MEDIUM)
            exit(PLAYER, Edge.RIGHT)
            enter(PLAYER, Edge.RIGHT, 0.52f)
            hold(PLAYER, Prop.BAG)
            cue("steps")
            exit(PLAYER, Edge.LEFT)
            act(STRANGER, WAVE, HAPPY, 1200)
            narrate("It’s fast and kind. She waves as you go.")
        }
        option(2, "I’ll come back later. I promise.", NEUTRAL, NOD, MEDIUM) {
            say(STRANGER, "Don’t trouble yourself, dear.", NEUTRAL, WAVE, CLOSE_UP)
            exit(PLAYER, Edge.LEFT)
            card("AN HOUR LATER", ms = 1800)
            enter(FRIEND, Edge.LEFT, 0.28f, concurrent = true)
            enter(PLAYER, Edge.LEFT, 0.12f)
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "So where do these boxes go?", HAPPY, WAVE, TWO_SHOT)
            act(STRANGER, CLAP, HAPPY, 1200)
            narrate("You keep your word, with a friend in tow. She’s delighted.")
        }
        option(3, "Sorry, I’m in a hurry.", EMBARRASSED, SHRUG, MEDIUM) {
            say(STRANGER, "Of course, dear. Off you go.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
            cue("steps")
            exit(PLAYER, Edge.LEFT, run = true)
            cam(CLOSE_UP, STRANGER)
            act(STRANGER, NONE, SAD, 1300)
            narrate("You feel a small prick of guilt the whole day.")
        }
    }

    scene("rnd_flu", APARTMENT, MORNING) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        cam(ESTABLISHING)
        narrate("Morning. Your head pounds, your body aches, and your forehead is burning.")
        act(PLAYER, FACEPALM, TIRED, 1200)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "Hello? Yes… I know what day it is.", TIRED, PHONE, MEDIUM)
        voice(BOSS, "Big presentation at nine. We’re all counting on you.", NEUTRAL)
        say(PLAYER, "Of course. I’ll… be there.", TIRED, PHONE, CLOSE_UP)
        hold(PLAYER, Prop.NONE)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, NONE, TIRED, 1300)
        say(PLAYER, "I can barely stand. What do I do?", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        narrate("A crucial day, and everyone expects you to be there.")

        option(0, "I’m staying home. I need to rest.", NEUTRAL, NOD, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(BOSS, "Understood. Get well soon, {name}.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            card("TWO DAYS LATER", ms = 1800)
            act(PLAYER, NOD, HAPPY, 1300)
            narrate("You sleep, drink tea, and recover. Nobody’s world ends.")
        }
        option(1, "I’ll push through. I can’t miss today.", CONFIDENT, FIST_PUMP, MEDIUM) {
            stand(PLAYER)
            hold(PLAYER, Prop.BAG)
            cue("door")
            exit(PLAYER, Edge.LEFT)
            card("TEN HOURS LATER", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.40f)
            hold(PLAYER, Prop.NONE)
            sit(PLAYER)
            outcome(0) {
                cue("phone")
                hold(PLAYER, Prop.PHONE)
                voice(COWORKER, "Half the office is sneezing now, {name}…", ANGRY)
                say(PLAYER, "I’m so sorry. That was a bad idea.", EMBARRASSED, PHONE, CLOSE_UP)
                hold(PLAYER, Prop.NONE)
                narrate("You were useless at work, and you got others sick.")
            }
            outcome(1) {
                cam(PUSH_IN, PLAYER)
                act(PLAYER, HEAD_DOWN, TIRED, 1700)
                narrate("You get through it. You’re wiped out for days afterwards.")
            }
        }
        option(2, "I’ll see a doctor first.", WORRIED, NOD, MEDIUM) {
            cue("knock")
            enter(DOCTOR, Edge.RIGHT, 0.72f)
            hold(DOCTOR, Prop.DOCUMENTS)
            say(DOCTOR, "Just a nasty virus. Rest, fluids, and this.", NEUTRAL, EXPLAIN, TWO_SHOT)
            act(PLAYER, NOD, TIRED, 1100)
            say(DOCTOR, "You’ll feel better in a week.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            narrate("A simple diagnosis and clear instructions.")
        }
        option(3, "I’ll work from bed. Laptop, tea, done.", TIRED, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 1500)
            act(PLAYER, FACEPALM, TIRED, 1000)
            narrate("A compromise, and not a great one. You feel half-ill, half-productive.")
        }
    }

    scene("rnd_power_cut", LIVING_ROOM, NIGHT) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        place(SIBLING, 0.66f, LEFT, seated = true)
        hold(PLAYER, Prop.PHONE)
        hold(SIBLING, Prop.PHONE)
        cam(ESTABLISHING)
        narrate("Friday evening. Everyone is on their own little screen.")
        fadeOut(200)
        pause(500)
        fadeIn(300)
        say(SIBLING, "Hey! The power just went out!", SURPRISED, HANDS_UP, MEDIUM)
        say(PLAYER, "The whole street’s dark. My phone’s at ten percent.", WORRIED, EXPLAIN, CLOSE_UP)
        say(SIBLING, "The Wi-Fi’s dead too. What do we do now?", WORRIED, SHRUG, TWO_SHOT)
        hold(PLAYER, Prop.NONE)
        hold(SIBLING, Prop.NONE)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        narrate("A dark house. A long evening. And no screens.")
        cam(TWO_SHOT, PLAYER, SIBLING)

        option(0, "Candles, snacks, and the old board games!", EXCITED, FIST_PUMP, MEDIUM) {
            stand(PLAYER)
            stand(SIBLING)
            say(SIBLING, "Board games? I’ll bring the snacks!", LAUGHING, CLAP, REACTION)
            sit(PLAYER)
            sit(SIBLING)
            say(SIBLING, "Wait, I never told you this story…", HAPPY, EXPLAIN, TWO_SHOT)
            act(PLAYER, NONE, LAUGHING, 1400)
            narrate("You laugh louder than you have in weeks. Someone tells a story you’ve never heard.")
        }
        option(1, "Let’s go outside and see what’s happening.", CONFIDENT, POINT, MEDIUM) {
            cue("knock")
            enter(STRANGER, Edge.RIGHT, 0.88f)
            say(STRANGER, "Is yours out too? Half the street is outside!", HAPPY, WAVE, MEDIUM)
            stand(PLAYER)
            stand(SIBLING)
            cue("door")
            exit(SIBLING, Edge.RIGHT, concurrent = true)
            exit(PLAYER, Edge.RIGHT)
            narrate("Half the street is outside with torches, and everyone is talking. It feels like a village.")
        }
        option(2, "I’m going to bed. Early night.", TIRED, GENTLE_SMILE, MEDIUM) {
            say(SIBLING, "At this hour? Fine, sleepyhead.", LAUGHING, SHRUG, REACTION)
            stand(PLAYER)
            exit(PLAYER, Edge.LEFT)
            card("SATURDAY MORNING", ms = 1800)
            enter(PLAYER, Edge.LEFT, 0.34f)
            act(PLAYER, FIST_PUMP, HAPPY, 1200)
            narrate("A very early night. You’re ready to conquer Saturday.")
        }
        option(3, "Ugh. This is so annoying!", ANGRY, CROSS_ARMS, MEDIUM) {
            say(SIBLING, "Fuming won’t bring the lights back.", NEUTRAL, SHRUG, REACTION)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, CROSS_ARMS, ANGRY, 1600)
            narrate("Nothing improves by being annoyed in the dark.")
        }
    }

    scene("rnd_conference", MEETING) {
        place(PLAYER, 0.34f, RIGHT)
        cam(ESTABLISHING)
        enter(STRANGER, Edge.RIGHT, 0.66f)
        hold(STRANGER, Prop.DOCUMENTS)
        say(STRANGER, "I can’t make it on Saturday. Take my ticket.", HAPPY, EXPLAIN, MEDIUM)
        say(STRANGER, "A whole industry event. Talks, workshops, everything.", HAPPY, EXPLAIN, TWO_SHOT)
        hold(STRANGER, Prop.NONE)
        hold(PLAYER, Prop.DOCUMENTS)
        say(PLAYER, "My Saturday? And talking to strangers?", WORRIED, SCRATCH_HEAD, CLOSE_UP)
        say(STRANGER, "Everyone’s awkward for ten minutes. Then it’s fun.", LAUGHING, SHRUG, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, THINK, THOUGHTFUL, 1200)
        narrate("A free ticket. Or a free Saturday, if you say no.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’ll go, and meet as many people as I can.", CONFIDENT, FIST_PUMP, MEDIUM) {
            say(STRANGER, "That’s the spirit! Bring business cards.", HAPPY, CLAP, TWO_SHOT)
            exit(STRANGER, Edge.RIGHT)
            card("SATURDAY", ms = 1800)
            act(PLAYER, HANDSHAKE, HAPPY, 1300)
            act(PLAYER, NONE, EXCITED, 1000)
            narrate("Awkward for ten minutes, then genuinely interesting. You come home with a pile of business cards.")
        }
        option(1, "I’ll go, but only to the talks.", THOUGHTFUL, NOD, MEDIUM) {
            say(STRANGER, "A good start, at least.", NEUTRAL, NOD, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            card("SATURDAY", ms = 1800)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NOD, THOUGHTFUL, 1500)
            narrate("You learn a lot and talk to nobody. It’s a start.")
        }
        option(2, "I know someone who needs this more than me.", HAPPY, GENTLE_SMILE, MEDIUM) {
            enter(COWORKER, Edge.LEFT, 0.12f)
            hold(PLAYER, Prop.NONE)
            hold(COWORKER, Prop.DOCUMENTS)
            say(PLAYER, "{coworker}, this is for you.", HAPPY, EXPLAIN, TWO_SHOT)
            say(COWORKER, "For me? Are you serious?", SURPRISED, HANDS_UP, REACTION)
            act(COWORKER, HUG, EXCITED, 1400)
            narrate("They’re stunned, and thrilled. They never forget the favour.")
        }
        option(3, "Thanks, but I’m spending Saturday with family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(STRANGER, "Good choice. Enjoy it!", HAPPY, WAVE, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            hold(PLAYER, Prop.NONE)
            card("SATURDAY MORNING", ms = 1800)
            enter(SIBLING, Edge.RIGHT, 0.64f)
            hold(SIBLING, Prop.CUP)
            say(SIBLING, "Pancakes first. Then a long walk?", HAPPY, GENTLE_SMILE, TWO_SHOT)
            narrate("A slow breakfast and a long walk. You don’t miss the conference at all.")
        }
    }

    scene("rnd_fun_run", PARK, MORNING) {
        place(PLAYER, 0.30f, RIGHT)
        hold(PLAYER, Prop.CUP)
        cam(ESTABLISHING)
        enter(FRIEND, Edge.RIGHT, 0.66f, run = true)
        hold(FRIEND, Prop.DOCUMENTS)
        say(FRIEND, "Great news! I signed you up!", EXCITED, WAVE, MEDIUM)
        say(PLAYER, "Signed me up for what?", SURPRISED, NONE, CLOSE_UP)
        say(FRIEND, "A charity fun run. It’s only five kilometres!", HAPPY, EXPLAIN, TWO_SHOT)
        say(PLAYER, "Only five? I haven’t run in ages.", WORRIED, SCRATCH_HEAD, MEDIUM)
        say(FRIEND, "Trust me. It’ll be fun.", LAUGHING, GENTLE_SMILE, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        narrate("A smile you don’t quite trust.")
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "Fine. I’ll train for it properly.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "Early mornings, together!", EXCITED, FIST_PUMP, REACTION)
            card("WEEKS OF EARLY MORNINGS", ms = 1800)
            cue("cheer")
            move(PLAYER, 0.84f, run = true)
            act(PLAYER, FIST_PUMP, PROUD, 1200)
            narrate("You cross the finish line, wheezing and proud.")
        }
        option(1, "I’ll walk most of it. We can chat on the way.", HAPPY, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            say(FRIEND, "Deal. Last place, together!", LAUGHING, CLAP, REACTION)
            move(FRIEND, 0.84f, concurrent = true)
            move(PLAYER, 0.76f)
            act(PLAYER, NONE, LAUGHING, 1200)
            narrate("You finish last, laughing, with ice cream.")
        }
        option(2, "Cancel it. I’m not a runner.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Aw, come on! Please?", SAD, HANDS_UP, REACTION)
            say(PLAYER, "Sorry. Not this time.", EMBARRASSED, SHRUG, TWO_SHOT)
            exit(FRIEND, Edge.RIGHT)
            hold(PLAYER, Prop.NONE)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, SAD, 1400)
            narrate("You spend the morning on the couch. The photos make it look fun.")
        }
        option(3, "I’ll volunteer at the water station instead.", HAPPY, EXPLAIN, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "Fine! But wave at me when I pass!", LAUGHING, WAVE, REACTION)
            hold(PLAYER, Prop.CUP)
            cue("cheer")
            act(PLAYER, WAVE, HAPPY, 1500)
            narrate("You hand out hundreds of cups of water, and get a surprising number of high-fives.")
        }
    }

    scene("rnd_gift", LIVING_ROOM) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        hold(PLAYER, Prop.PHONE)
        cam(ESTABLISHING)
        cue("phone")
        narrate("A notification. Your bank account just got heavier.")
        say(PLAYER, "A deposit? From… my relative?", SURPRISED, NONE, CLOSE_UP)
        enter(SIBLING, Edge.RIGHT, 0.68f)
        say(SIBLING, "Everything alright? You look stunned.", SURPRISED, NONE, MEDIUM)
        say(PLAYER, "A relative sent money. “Just because.”", HAPPY, EXPLAIN, TWO_SHOT)
        say(SIBLING, "Lucky you! So what’s the plan?", EXCITED, NONE, MEDIUM)
        hold(PLAYER, Prop.NONE)
        say(PLAYER, "It’s not enormous. But it’s unexpected.", THOUGHTFUL, SHRUG, CLOSE_UP)
        cam(TWO_SHOT, PLAYER, SIBLING)

        option(0, "I’m putting it all into savings.", CONFIDENT, NOD, MEDIUM) {
            say(SIBLING, "Look at you. So responsible.", PROUD, NOD, REACTION)
            act(PLAYER, NOD, CONFIDENT, 1100)
            narrate("A little safety cushion grows a bit thicker.")
        }
        option(1, "Dinner out. And you’re coming with me.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(SIBLING, CLAP, EXCITED, 1000)
            say(SIBLING, "Best day ever! I’m starving!", LAUGHING, FIST_PUMP, REACTION)
            card("DINNER THAT EVENING", ms = 1800)
            hold(PLAYER, Prop.PHONE)
            say(PLAYER, "Thank you so much. It really meant a lot.", HAPPY, PHONE, CLOSE_UP)
            hold(PLAYER, Prop.NONE)
            narrate("A good dinner and a long conversation.")
        }
        option(2, "I’ll invest it in a skill I need.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(SIBLING, "Like what?", SURPRISED, NONE, REACTION)
            say(PLAYER, "A course. A good tool. Something that keeps giving.", CONFIDENT, EXPLAIN, TWO_SHOT)
            hold(PLAYER, Prop.BOOK)
            act(PLAYER, NOD, HAPPY, 1200)
            narrate("It’s the kind of gift that keeps on giving.")
        }
        option(3, "Someone needs this more than me. I’ll share it.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(SIBLING, NONE, SURPRISED, 900)
            say(SIBLING, "That is so like you, {name}.", PROUD, GENTLE_SMILE, TWO_SHOT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1400)
            narrate("It’s a joy to pass along. They’ll never know how much it mattered to you, too.")
        }
    }
}
