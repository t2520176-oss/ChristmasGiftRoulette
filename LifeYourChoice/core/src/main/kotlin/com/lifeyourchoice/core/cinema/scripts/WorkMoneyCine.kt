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
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.SceneArt.*
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.story.*

/** Career, business and money scenes. */
val WorkMoneyCine = cinePack("workmoney") {

    // =====================================================================================
    //  CAREER
    // =====================================================================================

    scene("car_first_day", OFFICE, MORNING) {
        place(PLAYER, 0.40f, LEFT)
        place(COWORKER, 0.90f, LEFT, seated = true)
        hold(COWORKER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(COWORKER, TYPE, NEUTRAL, 800)
        enter(BOSS, Edge.LEFT, 0.16f)
        hold(BOSS, Prop.DOCUMENTS)
        say(BOSS, "Welcome, {name}. I’m {boss}.", HAPPY, HANDSHAKE, MEDIUM)
        say(BOSS, "Here’s your first week of tasks.", NEUTRAL, EXPLAIN, TWO_SHOT)
        hold(BOSS, Prop.NONE)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(BOSS, "I’m in meetings all day. Good luck.", NEUTRAL, SHRUG, MEDIUM)
        exit(BOSS, Edge.LEFT)
        cam(WIDE)
        say(NARRATOR, "Everyone else seems to know exactly what they’re doing.")
        say(PLAYER, "Where do I even start?", WORRIED, SCRATCH_HEAD, MEDIUM)
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Sorry, can I ask a really basic question?", WORRIED, EXPLAIN, MEDIUM) {
            move(PLAYER, 0.66f)
            act(COWORKER, NONE, SURPRISED, 700)
            say(COWORKER, "Ask away. Nobody laughs here.", HAPPY, GENTLE_SMILE, MEDIUM)
            say(COWORKER, "I asked where the printer was for a week.", LAUGHING, SHRUG, TWO_SHOT)
            act(PLAYER, NONE, HAPPY, 1000)
        }
        option(1, "I’ll figure it out on my own.", NEUTRAL, NOD, MEDIUM) {
            move(PLAYER, 0.64f)
            sit(PLAYER)
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 2000)
            act(PLAYER, SCRATCH_HEAD, TIRED, 1000)
            say(NARRATOR, "It takes longer. A mistake or two slips through. But you learn deeply.")
        }
        option(2, "I’ll stay late and prove myself.", CONFIDENT, FIST_PUMP, MEDIUM) {
            move(PLAYER, 0.64f)
            sit(PLAYER)
            hold(PLAYER, Prop.LAPTOP)
            act(PLAYER, TYPE, CONFIDENT, 1200)
            card("HOURS LATER")
            say(COWORKER, "Don’t burn out on day one!", HAPPY, WAVE, MEDIUM)
            stand(COWORKER)
            exit(COWORKER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, TYPE, TIRED, 1800)
            enter(BOSS, Edge.LEFT, 0.30f)
            hold(BOSS, Prop.CUP)
            say(BOSS, "Still here? I noticed.", PROUD, NOD, TWO_SHOT)
            say(NARRATOR, "{boss} notices the extra hours. So does your back.")
        }
        option(3, "{coworker}, could you show me around?", HAPPY, WAVE, MEDIUM) {
            move(PLAYER, 0.66f)
            stand(COWORKER)
            say(COWORKER, "Sure! Best coffee machine first.", HAPPY, POINT, TWO_SHOT)
            cam(FOLLOW, COWORKER)
            move(COWORKER, 0.30f, concurrent = true)
            move(PLAYER, 0.46f)
            say(COWORKER, "Shortcuts, snacks, and who to avoid.", LAUGHING, EXPLAIN, TWO_SHOT)
            act(PLAYER, NONE, HAPPY, 1000)
        }
    }

    scene("car_promotion", MEETING) {
        place(PLAYER, 0.22f, RIGHT, seated = true)
        place(COWORKER, 0.46f, RIGHT, seated = true)
        place(BOSS, 0.82f, LEFT)
        hold(BOSS, Prop.DOCUMENTS)
        cam(WIDE)
        say(BOSS, "A promotion has opened up.", NEUTRAL, EXPLAIN, MEDIUM)
        say(BOSS, "Two names on my shortlist: {name} and {coworker}.", NEUTRAL, NONE, TWO_SHOT)
        cam(REACTION, COWORKER)
        act(COWORKER, NONE, WORRIED, 900)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 900)
        say(BOSS, "I’ll decide within two weeks.", NEUTRAL, NOD, MEDIUM)
        say(COWORKER, "May the best one win, {name}.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        cam(CLOSE_UP, PLAYER)
        say(NARRATOR, "Two weeks. One role. How do you play it?")

        option(0, "{boss}, I’d like to be considered. Seriously.", CONFIDENT, EXPLAIN, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.60f)
            say(BOSS, "I like a direct answer. Noted.", PROUD, NOD, TWO_SHOT)
            card("TWO WEEKS LATER")
            outcome(0) {
                say(BOSS, "{name}. The promotion is yours.", HAPPY, HANDSHAKE, MEDIUM)
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                say(COWORKER, "Congratulations. You earned it.", HAPPY, CLAP, MEDIUM)
            }
            outcome(1) {
                say(BOSS, "Not this time. But you’re next in line.", NEUTRAL, SHRUG, CLOSE_UP)
                act(PLAYER, NOD, THOUGHTFUL, 1200)
            }
        }
        option(1, "I’ll let my work speak for itself.", CONFIDENT, NOD, MEDIUM) {
            say(BOSS, "Hm. I’ll be watching.", THOUGHTFUL, THINK, CLOSE_UP)
            say(NARRATOR, "You put your head down. You deliver, week after week.")
            card("TWO WEEKS LATER")
            outcome(0) {
                say(BOSS, "Your results spoke loudest. It’s you, {name}.", PROUD, HANDSHAKE, MEDIUM)
                cue("cheer")
                act(PLAYER, FIST_PUMP, HAPPY, 1000)
            }
            outcome(1) {
                say(BOSS, "I’m sorry, {name}. It’s {coworker}.", NEUTRAL, SHRUG, MEDIUM)
                act(COWORKER, NONE, SURPRISED, 900)
                act(PLAYER, HEAD_DOWN, DISAPPOINTED, 1300)
                say(NARRATOR, "Quiet work doesn’t always get noticed.")
            }
        }
        option(2, "{coworker} deserves it. I’m withdrawing.", NEUTRAL, GENTLE_SMILE, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "What? Why would you do that?", SURPRISED, HANDS_UP, REACTION)
            say(PLAYER, "Because you’d do the same for me.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            stand(PLAYER)
            stand(COWORKER)
            move(PLAYER, 0.36f)
            act(COWORKER, HUG, HAPPY, 1500)
            act(BOSS, NOD, PROUD, 1200)
        }
        option(3, "{boss}, you should know about {coworker}’s mistakes.", CONFIDENT, POINT, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "Excuse me? Where is this coming from?", ANGRY, POINT, REACTION)
            say(BOSS, "Let me look into it.", THOUGHTFUL, CROSS_ARMS, CLOSE_UP)
            card("TWO WEEKS LATER")
            outcome(0) {
                say(BOSS, "The role is yours. Don’t make me regret it.", NEUTRAL, NONE, MEDIUM)
                act(COWORKER, HEAD_DOWN, SAD, 1200)
                say(NARRATOR, "You got it. The route was nasty, and some people suspect it.")
            }
            outcome(1) {
                say(BOSS, "I checked. Your claims don’t hold up, {name}.", ANGRY, POINT, CLOSE_UP)
                act(PLAYER, HEAD_DOWN, EMBARRASSED, 1300)
                say(NARRATOR, "Your tactics come to light. The role goes to someone else.")
            }
        }
    }

    scene("car_bad_boss", OFFICE) {
        place(COWORKER, 0.12f, RIGHT, seated = true)
        place(PLAYER, 0.38f, RIGHT, seated = true)
        place(BOSS, 0.78f, LEFT)
        hold(BOSS, Prop.DOCUMENTS)
        cam(WIDE)
        say(BOSS, "…and that’s how I solved it. My idea, my plan.", CONFIDENT, EXPLAIN, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, ANGRY, 1100)
        say(COWORKER, "That was your project, {name}…", WORRIED, SHRUG, TWO_SHOT)
        say(NARRATOR, "It was your idea. Your work. Your name wasn’t mentioned once.")
        cam(TWO_SHOT, PLAYER, BOSS)
        say(BOSS, "Good meeting, wasn’t it, {name}?", HAPPY, GENTLE_SMILE, OVER_SHOULDER)
        act(PLAYER, NONE, ANGRY, 800)

        option(0, "{boss}, a word in private. That was my idea.", ANGRY, POINT, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.56f)
            act(BOSS, NONE, SURPRISED, 800)
            say(BOSS, "…Fine. Let’s step aside.", NEUTRAL, NOD, CLOSE_UP)
            outcome(0) {
                say(BOSS, "You’re right. I’m sorry. You’ll be credited.", SAD, HANDSHAKE, MEDIUM)
                act(PLAYER, NOD, CONFIDENT, 1100)
            }
            outcome(1) {
                say(BOSS, "I presented the team’s work. Don’t take it personally.", ANGRY, CROSS_ARMS, CLOSE_UP)
                exit(BOSS, Edge.RIGHT)
                say(NARRATOR, "Things between you become frosty.")
            }
        }
        option(1, "I’ll put it all in writing. For HR.", THOUGHTFUL, NOD, MEDIUM) {
            act(BOSS, NONE, WORRIED, 900)
            say(BOSS, "HR? There’s really no need for that.", WORRIED, HANDS_UP, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 2000)
            say(NARRATOR, "It’s slow and awkward. But now there’s a paper trail, and a quiet policy change.")
        }
        option(2, "Yes. Great meeting.", NEUTRAL, GENTLE_SMILE, CLOSE_UP) {
            say(BOSS, "Knew you’d understand.", HAPPY, NOD, MEDIUM)
            exit(BOSS, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1600)
            say(NARRATOR, "You swallow your frustration. It sits in your stomach for months.")
        }
        option(3, "…I think it’s time to look elsewhere.", SAD, SHAKE_HEAD, CLOSE_UP) {
            act(BOSS, NONE, SURPRISED, 800)
            say(BOSS, "Hm? Did you say something?", NEUTRAL, NONE, TWO_SHOT)
            say(PLAYER, "Nothing. Just thinking aloud.", NEUTRAL, SHRUG, MEDIUM)
            exit(BOSS, Edge.RIGHT)
            hold(PLAYER, Prop.LAPTOP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 1800)
            say(NARRATOR, "Updating your résumé feels strangely good. Options exist, and you know it.")
        }
    }

    scene("car_coworker_conflict", OFFICE) {
        place(PLAYER, 0.32f, RIGHT)
        place(COWORKER, 0.68f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        hold(COWORKER, Prop.LAPTOP)
        cam(TWO_SHOT, PLAYER, COWORKER)
        say(COWORKER, "Your plan is too slow. We’ll miss the deadline.", ANGRY, POINT, MEDIUM)
        say(PLAYER, "Yours skips the testing. That’s worse.", ANGRY, EXPLAIN, OVER_SHOULDER)
        say(COWORKER, "I’ve done this a dozen times, {name}.", CONFIDENT, CROSS_ARMS, CLOSE_UP)
        cam(WIDE)
        say(NARRATOR, "Around you, the team is quietly picking sides.")
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, ANGRY, 1000)
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Let’s hear each other out and combine them.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "…Really? You’d meet me halfway?", SURPRISED, NONE, CLOSE_UP)
            move(PLAYER, 0.46f)
            say(PLAYER, "Your speed. My testing.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            act(COWORKER, NOD, HAPPY, 1000)
            say(NARRATOR, "The combined plan beats both originals. The team notices.")
        }
        option(1, "We’re going with my plan.", ANGRY, POINT, MEDIUM) {
            act(COWORKER, NONE, DISAPPOINTED, 900)
            say(COWORKER, "Fine. You win.", ANGRY, SHRUG, CLOSE_UP)
            exit(COWORKER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1300)
            say(NARRATOR, "You win the vote. The victory feels hollow when the room goes quiet.")
        }
        option(2, "Let’s ask {boss} to decide.", NEUTRAL, SHRUG, MEDIUM) {
            cue("knock")
            enter(BOSS, Edge.RIGHT, 0.90f)
            say(BOSS, "Why is this on my desk?", ANGRY, CROSS_ARMS, MEDIUM)
            say(BOSS, "Fine. We do it my way.", NEUTRAL, POINT, TWO_SHOT)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 900)
            act(COWORKER, HEAD_DOWN, DISAPPOINTED, 900)
            say(NARRATOR, "Neither of you is happy. {boss} resents being pulled in.")
        }
        option(3, "Fine. Do it your way. It’s not worth it.", TIRED, SHRUG, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "…Just like that?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Just like that.", TIRED, SHRUG, CLOSE_UP)
            say(NARRATOR, "Peace is restored. You’re not sure it was the right call.")
        }
    }

    scene("car_unethical_order", OFFICE, EVENING) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        act(PLAYER, TYPE, TIRED, 900)
        cam(ESTABLISHING)
        cue("door")
        enter(BOSS, Edge.RIGHT, 0.70f)
        hold(BOSS, Prop.DOCUMENTS)
        say(BOSS, "{name}. Do you have a minute?", NEUTRAL, NONE, MEDIUM)
        say(BOSS, "The client report. Those numbers are… unfortunate.", WORRIED, EXPLAIN, TWO_SHOT)
        say(BOSS, "Adjust them a little. Nobody will ever notice.", CONFIDENT, SHRUG, CLOSE_UP)
        say(BOSS, "Everyone does it.", CONFIDENT, NONE)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1200)
        say(NARRATOR, "Your stomach tightens. This is not a small favour.")
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "…Okay. If you say so.", WORRIED, NOD, CLOSE_UP) {
            say(BOSS, "Good. I won’t forget this.", PROUD, HANDSHAKE, MEDIUM)
            exit(BOSS, Edge.RIGHT)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1700)
            say(NARRATOR, "The report goes out. Nothing happens. The knot in your stomach stays for weeks.")
        }
        option(1, "No, {boss}. I can’t do that.", CONFIDENT, SHAKE_HEAD, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            act(BOSS, NONE, SURPRISED, 900)
            outcome(0) {
                say(BOSS, "…Fine. Forget I asked.", EMBARRASSED, SHRUG, CLOSE_UP)
                exit(BOSS, Edge.RIGHT)
                say(NARRATOR, "{boss} backs off, a bit embarrassed. Things stay civil.")
            }
            outcome(1) {
                say(BOSS, "Your next review will reflect this.", ANGRY, POINT, CLOSE_UP)
                exit(BOSS, Edge.RIGHT)
                act(PLAYER, NONE, CONFIDENT, 1200)
                say(NARRATOR, "It costs you. But you hold your head high.")
            }
        }
        action(2) {
            act(PLAYER, NONE, THOUGHTFUL, 900)
            say(BOSS, "Think it over. Quickly.", NEUTRAL, NONE, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 2200)
            say(NARRATOR, "An anonymous message to compliance. An inquiry follows. Messy, but the numbers get corrected.")
        }
        option(3, "Give me until morning. I’ll find another way.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(BOSS, "Fine. Don’t take long.", NEUTRAL, NOD, CLOSE_UP)
            exit(BOSS, Edge.RIGHT)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 2000)
            card("NEXT MORNING")
            enter(BOSS, Edge.RIGHT, 0.70f)
            say(BOSS, "…This version actually solves the problem.", THOUGHTFUL, THINK, MEDIUM)
            say(BOSS, "Fine. We’ll use yours.", NEUTRAL, NOD, CLOSE_UP)
        }
    }

    scene("car_burnout", APARTMENT, MORNING) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        cam(ESTABLISHING)
        act(PLAYER, HEAD_DOWN, TIRED, 1400)
        cue("phone")
        say(NARRATOR, "The alarm rings. You don’t move.")
        voice(BOSS, "{name}? The team is waiting for you.", WORRIED)
        say(PLAYER, "I can’t. I just can’t get up.", TIRED, HEAD_DOWN, CLOSE_UP)
        say(NARRATOR, "Work looks like a mountain. Sleep isn’t fixing this kind of tired.")
        cam(PUSH_IN, PLAYER)
        act(PLAYER, FACEPALM, SAD, 1200)
        say(PLAYER, "What is wrong with me?", SAD, NONE, CLOSE_UP)

        option(0, "I need a doctor. And a real break.", SAD, NOD, CLOSE_UP) {
            hold(PLAYER, Prop.PHONE)
            voice(DOCTOR, "Come in this week. And then rest. Truly rest.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            card("A WEEK OF REST")
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, GENTLE_SMILE, HAPPY, 1700)
            say(NARRATOR, "The doctor’s advice is simple: stop. You do, and slowly you come back to life.")
        }
        option(1, "Everyone’s tired. I’ll push through.", TIRED, SHRUG, MEDIUM) {
            stand(PLAYER)
            hold(PLAYER, Prop.BAG)
            cam(FOLLOW, PLAYER)
            exit(PLAYER, Edge.RIGHT)
            say(NARRATOR, "You keep going. Your body keeps score, and it will send the bill later.")
        }
        option(2, "{boss}? I need to talk about my workload.", WORRIED, PHONE, CLOSE_UP) {
            hold(PLAYER, Prop.PHONE)
            voice(BOSS, "Thank you for telling me. Let’s lighten your plate.", NEUTRAL)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, HAPPY, 1500)
            hold(PLAYER, Prop.NONE)
            say(NARRATOR, "{boss} is kinder than you expected. Some tasks come off your plate.")
        }
        option(3, "I need two days away. Come with me?", TIRED, PHONE, CLOSE_UP) {
            hold(PLAYER, Prop.PHONE)
            whenever(status(RelationshipStatus.DATING, RelationshipStatus.MARRIED), otherwise = {
                voice(FRIEND, "Pack a bag. I’ll drive.", HAPPY)
            }) {
                voice(PARTNER, "Pack a bag. I’ll drive.", HAPPY)
            }
            hold(PLAYER, Prop.SUITCASE)
            stand(PLAYER)
            cam(FOLLOW, PLAYER)
            exit(PLAYER, Edge.RIGHT)
            say(NARRATOR, "Two days of doing very little. You return slower, but clearer.")
        }
    }

    scene("car_award", MEETING, EVENING) {
        place(COWORKER, 0.12f, RIGHT)
        place(PLAYER, 0.30f, RIGHT)
        place(BOSS, 0.76f, LEFT)
        hold(BOSS, Prop.BOX)
        cam(WIDE)
        say(BOSS, "Employee of the Year goes to… {name}!", EXCITED, HANDS_UP, MEDIUM)
        cue("cheer")
        act(PLAYER, NONE, SURPRISED, 1000)
        cam(REACTION, COWORKER)
        act(COWORKER, CLAP, HAPPY, 900)
        move(PLAYER, 0.56f)
        hold(BOSS, Prop.NONE)
        hold(PLAYER, Prop.BOX)
        say(BOSS, "Say a few words, {name}.", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(PLAYER, "Oh. I… wow. Thank you.", SURPRISED, SCRATCH_HEAD, CLOSE_UP)
        say(NARRATOR, "You know the truth: much of this was your team’s quiet effort.")
        cam(TWO_SHOT, PLAYER, BOSS)

        option(0, "This belongs to my whole team. Especially {coworker}.", HAPPY, POINT, MEDIUM) {
            cam(REACTION, COWORKER)
            act(COWORKER, NONE, SURPRISED, 800)
            cue("cheer")
            act(COWORKER, CLAP, HAPPY, 1300)
            say(COWORKER, "{name}… thank you.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            say(NARRATOR, "Your team beams. People remember a leader like that.")
        }
        option(1, "Thank you. I’m honoured.", HAPPY, GENTLE_SMILE, MEDIUM) {
            say(BOSS, "Well deserved.", PROUD, HANDSHAKE, MEDIUM)
            act(COWORKER, CLAP, NEUTRAL, 900)
            say(NARRATOR, "A warm moment. A few polite, noncommittal claps.")
        }
        option(2, "Oh, it’s nothing. Really.", EMBARRASSED, SHRUG, MEDIUM) {
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1000)
            cam(REACTION, COWORKER)
            act(COWORKER, NONE, DISAPPOINTED, 1200)
            say(NARRATOR, "The attention makes you uncomfortable. Your team wishes you’d let them cheer.")
        }
        option(3, "Thank you. And about that raise…", CONFIDENT, EXPLAIN, MEDIUM) {
            act(BOSS, NONE, SURPRISED, 800)
            say(BOSS, "…Now? Let’s talk on Monday.", NEUTRAL, SHRUG, CLOSE_UP)
            cam(REACTION, COWORKER)
            act(COWORKER, NONE, EMBARRASSED, 1000)
            say(NARRATOR, "They agree, quietly. A few people mutter about the timing.")
        }
    }

    scene("car_leadership", MEETING) {
        place(BOSS, 0.82f, LEFT)
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(COWORKER, 0.56f, LEFT, seated = true)
        cam(WIDE)
        say(BOSS, "{name}, you’ll lead the new team. Five people.", NEUTRAL, EXPLAIN, MEDIUM)
        say(BOSS, "The deadline is tight. I’m counting on you.", CONFIDENT, POINT, TWO_SHOT)
        cam(REACTION, COWORKER)
        act(COWORKER, CROSS_ARMS, DISAPPOINTED, 900)
        say(COWORKER, "No offence, {name}. Are we sure about this approach?", NEUTRAL, SHRUG, MEDIUM)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1000)
        say(NARRATOR, "Two of them have more experience than you. One already doubts you.")
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Clear goals first. And I’ll hear from each of you.", CONFIDENT, EXPLAIN, MEDIUM) {
            stand(PLAYER)
            move(PLAYER, 0.42f)
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "…You’re actually asking for our input?", SURPRISED, NONE, REACTION)
            say(PLAYER, "Always.", CONFIDENT, GENTLE_SMILE, TWO_SHOT)
            outcome(0) {
                cue("cheer")
                act(COWORKER, NOD, HAPPY, 1100)
                say(NARRATOR, "The team delivers on time, and proud of it. People ask to join your next project.")
            }
            outcome(1) {
                act(COWORKER, NOD, NEUTRAL, 1100)
                say(NARRATOR, "It’s tough, but the team holds together and does well enough.")
            }
        }
        option(1, "I’ll make the decisions. Follow the plan.", ANGRY, POINT, MEDIUM) {
            stand(PLAYER)
            act(COWORKER, CROSS_ARMS, ANGRY, 1100)
            say(COWORKER, "So much for teamwork.", ANGRY, SHAKE_HEAD, CLOSE_UP)
            act(PLAYER, NONE, TIRED, 1100)
            say(NARRATOR, "The deadline is met, but people grumble. You’re drained.")
        }
        option(2, "You’re the expert here. Take the lead on your part.", HAPPY, NOD, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "You’d trust me with that?", SURPRISED, NONE, REACTION)
            say(PLAYER, "I would.", CONFIDENT, GENTLE_SMILE, TWO_SHOT)
            act(COWORKER, NOD, PROUD, 1200)
            say(NARRATOR, "Trust brings out their best. You learn to lead without hovering.")
        }
        option(3, "I’ll do it all myself, just to be safe.", WORRIED, SHRUG, MEDIUM) {
            hold(PLAYER, Prop.LAPTOP)
            act(PLAYER, TYPE, TIRED, 1400)
            say(COWORKER, "Just… tell us if you change your mind.", SAD, SHRUG, CLOSE_UP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 1600)
            say(NARRATOR, "It gets done, at a cost. The team feels sidelined.")
        }
    }

    scene("car_mentor_meets", COFFEE_SHOP) {
        place(PLAYER, 0.30f, RIGHT, seated = true)
        place(MENTOR, 0.68f, LEFT, seated = true)
        hold(PLAYER, Prop.CUP)
        hold(MENTOR, Prop.CUP)
        cam(ESTABLISHING)
        say(MENTOR, "Thanks for coming, {name}. Coffee’s on me.", HAPPY, GENTLE_SMILE, MEDIUM)
        say(MENTOR, "I’ve been watching how you work.", NEUTRAL, EXPLAIN, TWO_SHOT)
        say(MENTOR, "I see myself in you.", PROUD, NONE, CLOSE_UP)
        say(MENTOR, "If you want, I’ll show you how this place really works.", HAPPY, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1100)

        option(0, "Yes, please. I’ll take notes.", EXCITED, NOD, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            say(MENTOR, "Rule one: nobody has it all figured out.", HAPPY, EXPLAIN, TWO_SHOT)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, HEAD_DOWN, HAPPY, 1500)
            say(NARRATOR, "{mentor} shares hard-earned lessons you won’t find in any handbook.")
        }
        option(1, "Thank you, but I’d like to find my own way.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            act(MENTOR, NONE, SURPRISED, 700)
            say(MENTOR, "Fair enough. The door stays open.", NEUTRAL, GENTLE_SMILE, CLOSE_UP)
            act(PLAYER, NOD, CONFIDENT, 1100)
        }
        option(2, "Yes. And be honest with me, even when it hurts.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(MENTOR, NONE, SURPRISED, 900)
            say(MENTOR, "That’s rare. I promise I will.", PROUD, HANDSHAKE, CLOSE_UP)
            act(PLAYER, NOD, HAPPY, 1100)
        }
        option(3, "What’s in it for you?", WORRIED, POINT, CLOSE_UP) {
            act(MENTOR, NONE, DISAPPOINTED, 1000)
            say(MENTOR, "Not everyone has an angle, {name}.", SAD, SHRUG, CLOSE_UP)
            act(PLAYER, CROSS_ARMS, THOUGHTFUL, 1200)
            say(NARRATOR, "You keep your distance. It’s hard to say whether that was wise.")
        }
    }

    scene("car_help_coworker", OFFICE, EVENING) {
        place(COWORKER, 0.68f, LEFT, seated = true)
        hold(COWORKER, Prop.LAPTOP)
        act(COWORKER, HEAD_DOWN, WORRIED, 600)
        place(PLAYER, 0.10f, RIGHT)
        hold(PLAYER, Prop.BAG)
        cam(ESTABLISHING)
        say(NARRATOR, "Everyone else has gone home. {coworker} is still at the screen.")
        move(PLAYER, 0.34f)
        act(PLAYER, NONE, WORRIED, 700)
        say(COWORKER, "I’m going to miss the deadline.", SAD, HEAD_DOWN, CLOSE_UP)
        say(COWORKER, "There’s just too much. I can’t…", AFRAID, FACEPALM)
        cam(TWO_SHOT, PLAYER, COWORKER)
        say(PLAYER, "Hey. {coworker}. Breathe.", WORRIED, NONE, MEDIUM)
        say(NARRATOR, "Your own workload isn’t light either.")

        option(0, "Move over. I’m staying to help.", CONFIDENT, GENTLE_SMILE, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            move(PLAYER, 0.56f)
            sit(PLAYER)
            act(PLAYER, TYPE, TIRED, 1500)
            card("HOURS LATER")
            say(COWORKER, "We did it. Minutes to spare.", HAPPY, FIST_PUMP, CLOSE_UP)
            say(COWORKER, "I won’t forget this, {name}.", PROUD, GENTLE_SMILE, TWO_SHOT)
        }
        option(1, "Try this shortcut. You’ve got this.", NEUTRAL, EXPLAIN, MEDIUM) {
            act(COWORKER, NOD, THOUGHTFUL, 900)
            say(COWORKER, "Oh. That helps. Thank you.", HAPPY, NOD, CLOSE_UP)
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "It’s a small thing, but it takes the edge off.")
        }
        option(2, "Let me tell {boss}. The work should be shared.", NEUTRAL, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(BOSS, "Understood. I’ll rebalance the workload.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            act(COWORKER, NONE, EMBARRASSED, 900)
            say(COWORKER, "Thanks… I think?", EMBARRASSED, SHRUG, CLOSE_UP)
        }
        option(3, "Sorry. I have my own deadlines.", NEUTRAL, SHRUG, MEDIUM) {
            act(COWORKER, HEAD_DOWN, SAD, 1000)
            say(COWORKER, "No, of course. Sorry.", SAD, NONE, CLOSE_UP)
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "You protect your own time. The office feels a little cooler afterwards.")
        }
    }

    scene("car_change_jobs", OFFICE) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(PLAYER, TYPE, NEUTRAL, 700)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "Hello? Yes, speaking.", NEUTRAL, PHONE, MEDIUM)
        voice(INTERVIEWER, "I have an offer: a better title and a bigger salary.", EXCITED)
        voice(INTERVIEWER, "A competitor wants you. New team, unknown culture.", NEUTRAL)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1200)
        say(PLAYER, "Can I… take a moment to think?", THOUGHTFUL, SCRATCH_HEAD)
        voice(INTERVIEWER, "Of course. I’ll hold.", NEUTRAL)

        option(0, "Yes. I’ll take the job.", EXCITED, FIST_PUMP, CLOSE_UP) {
            voice(INTERVIEWER, "Wonderful! Welcome aboard.", HAPPY)
            hold(PLAYER, Prop.NONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1500)
            say(NARRATOR, "The first month is rough. But the bigger challenge is exactly what you needed.")
        }
        option(1, "Thanks. Let me talk to {boss} first.", THOUGHTFUL, NOD, MEDIUM) {
            voice(INTERVIEWER, "Don’t wait too long.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            enter(BOSS, Edge.RIGHT, 0.70f)
            say(PLAYER, "{boss}, I have an offer. Can we talk salary?", CONFIDENT, EXPLAIN, TWO_SHOT)
            act(BOSS, NONE, SURPRISED, 900)
            outcome(0) {
                say(BOSS, "Hm. We’ll match most of it. Stay.", NEUTRAL, NOD, CLOSE_UP)
                act(PLAYER, NOD, CONFIDENT, 1000)
            }
            outcome(1) {
                say(BOSS, "No. And I’m disappointed you asked.", ANGRY, CROSS_ARMS, CLOSE_UP)
                act(PLAYER, NONE, EMBARRASSED, 1000)
            }
        }
        option(2, "Thank you, but I’m staying. Loyalty matters.", NEUTRAL, SHAKE_HEAD, CLOSE_UP) {
            voice(INTERVIEWER, "A pity. The offer stands for a week.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, THOUGHTFUL, 1400)
            say(NARRATOR, "You stay, with a faint question mark that doesn’t quite go away.")
        }
        option(3, "I’ll call you back. I need advice first.", THOUGHTFUL, NOD, MEDIUM) {
            voice(INTERVIEWER, "Of course. Talk soon.", NEUTRAL)
            voice(MENTOR, "Where do you want to be in ten years?", THOUGHTFUL)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, THOUGHTFUL, 1600)
            say(PLAYER, "…Honestly? I think I know now.", CONFIDENT, GENTLE_SMILE)
            hold(PLAYER, Prop.NONE)
        }
    }

    scene("car_career_vs_family", OFFICE, EVENING) {
        place(PLAYER, 0.34f, RIGHT)
        place(COWORKER, 0.70f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(COWORKER, "The launch starts at seven. Everyone’s waiting.", EXCITED, EXPLAIN, MEDIUM)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        whenever(hasChildren, otherwise = {
            voice(PARTNER, "You promised you’d be there tonight.", SAD)
        }) {
            voice(CHILD, "You promised you’d come tonight!", SAD)
        }
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, SCRATCH_HEAD, WORRIED, 1100)
        say(PLAYER, "I know. I promised. I know.", WORRIED, NONE, CLOSE_UP)
        hold(PLAYER, Prop.NONE)
        cam(TWO_SHOT, PLAYER, COWORKER)
        say(COWORKER, "You can’t do both, {name}.", WORRIED, SHRUG, OVER_SHOULDER)

        option(0, "The launch. The career won’t wait.", CONFIDENT, NOD, MEDIUM) {
            act(COWORKER, FIST_PUMP, EXCITED, 900)
            say(COWORKER, "Yes! Let’s go.", EXCITED, WAVE, MEDIUM)
            cue("cheer")
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, NONE, TIRED, 1300)
            say(NARRATOR, "The launch is a triumph. The apology at home is awkward.")
        }
        option(1, "I’m going home. Family comes first.", CONFIDENT, GENTLE_SMILE, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "Really? Tonight?", SURPRISED, HANDS_UP, REACTION)
            say(PLAYER, "Really. Cover for me?", HAPPY, NOD, TWO_SHOT)
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "You miss a big night. But their faces are something you’ll remember much longer.")
        }
        option(2, "I’ll attend the first hour of each.", WORRIED, EXPLAIN, MEDIUM) {
            cue("whoosh")
            exit(PLAYER, Edge.RIGHT, run = true)
            say(COWORKER, "In your best clothes? Good luck!", LAUGHING, WAVE, MEDIUM)
            say(NARRATOR, "You run between venues. It’s exhausting and a little ridiculous, but you make it.")
        }
        option(3, "Could your team cover the launch for me?", WORRIED, HANDS_UP, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 800)
            say(COWORKER, "Cover the launch? Without you?", SURPRISED, NONE, CLOSE_UP)
            say(PLAYER, "Please. I trust you.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            outcome(0) {
                say(COWORKER, "We’ve got this. Go.", PROUD, NOD, MEDIUM)
                exit(PLAYER, Edge.LEFT)
                say(NARRATOR, "They step up beautifully. The launch goes well without you.")
            }
            outcome(1) {
                say(COWORKER, "…We’ll try. No promises.", WORRIED, SHRUG, MEDIUM)
                exit(PLAYER, Edge.LEFT)
                say(NARRATOR, "The launch is messy without you. You hear about it for weeks.")
            }
        }
    }

    // =====================================================================================
    //  BUSINESS
    // =====================================================================================

    scene("biz_first_year", SMALL_BUSINESS, MORNING) {
        place(PLAYER, 0.34f, RIGHT)
        cam(ESTABLISHING)
        card("ONE YEAR LATER")
        enter(CUSTOMER, Edge.RIGHT, 0.70f)
        say(CUSTOMER, "The usual, {name}. A year already!", HAPPY, WAVE, MEDIUM)
        say(PLAYER, "Our first regular. Coming right up!", HAPPY, GENTLE_SMILE, TWO_SHOT)
        say(CUSTOMER, "Congratulations. Keep going!", PROUD, NOD)
        exit(CUSTOMER, Edge.RIGHT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, WORRIED, 1200)
        say(NARRATOR, "A year. A few regulars. And bills that are very real.")
        whenever(has("side_hustle"), otherwise = {
            say(PLAYER, "Where do I put what little I have?", THOUGHTFUL, SHRUG, CLOSE_UP)
        }) {
            say(PLAYER, "Day job by day, this place by night. Where does it all go?", TIRED, SHRUG, CLOSE_UP)
        }

        option(0, "I’m putting my savings into marketing.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(PLAYER, Prop.BOX)
            cue("whoosh")
            outcome(0) {
                cue("bell")
                enter(CUSTOMER, Edge.RIGHT, 0.64f)
                enter(STRANGER, Edge.RIGHT, 0.84f)
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                say(NARRATOR, "The gamble pays off. Orders double, and you hire your first employee.")
            }
            outcome(1) {
                act(PLAYER, HEAD_DOWN, SAD, 1500)
                say(NARRATOR, "The customers don’t come fast enough. The money runs out first.")
            }
        }
        option(1, "Keep costs tiny. Slow and steady.", THOUGHTFUL, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NOD, THOUGHTFUL, 1500)
            outcome(0) {
                act(PLAYER, NONE, PROUD, 1200)
                say(NARRATOR, "After a long, anxious year, the numbers turn positive.")
            }
            outcome(1) {
                act(PLAYER, HEAD_DOWN, SAD, 1400)
                say(NARRATOR, "You’re careful, but it never quite takes off. Eventually, the doors close.")
            }
        }
        option(2, "{friend}, will you partner with me?", HAPPY, EXPLAIN, MEDIUM) {
            enter(FRIEND, Edge.RIGHT, 0.68f)
            say(FRIEND, "I thought you’d never ask.", HAPPY, GENTLE_SMILE, TWO_SHOT)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HANDSHAKE, HAPPY, 1000)
            outcome(0) {
                say(NARRATOR, "Two heads, twice the energy. The business thrives, and so does the friendship.")
                act(FRIEND, FIST_PUMP, EXCITED, 1000)
            }
            outcome(1) {
                say(FRIEND, "We want different things, {name}.", SAD, SHRUG, CLOSE_UP)
                exit(FRIEND, Edge.RIGHT)
                say(NARRATOR, "Disagreements pile up, then the money. It ends in a quiet, awkward talk.")
            }
        }
        option(3, "I’ll keep my day job and grow this at night.", TIRED, NOD, MEDIUM) {
            hold(PLAYER, Prop.LAPTOP)
            card("MONTHS OF LATE NIGHTS")
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, TIRED, 2000)
            outcome(0) {
                act(PLAYER, FIST_PUMP, HAPPY, 1100)
                say(NARRATOR, "Sleepless, but it works. You finally leave your day job.")
            }
            outcome(1) {
                act(PLAYER, FACEPALM, TIRED, 1300)
                say(NARRATOR, "You’re too stretched for either to work well. It’s a hard, quiet end.")
            }
        }
    }

    scene("biz_customer_problem", SMALL_BUSINESS) {
        place(STRANGER, 0.12f, RIGHT)
        place(PLAYER, 0.38f, RIGHT)
        cam(ESTABLISHING)
        cue("bell")
        enter(CUSTOMER, Edge.RIGHT, 0.72f)
        hold(CUSTOMER, Prop.BOX)
        say(CUSTOMER, "Your product broke on the first day!", ANGRY, POINT, MEDIUM)
        say(CUSTOMER, "You ruined my entire weekend!", ANGRY, HANDS_UP, CLOSE_UP)
        say(PLAYER, "Sir, please, let me—", WORRIED, HANDS_UP, OVER_SHOULDER)
        say(CUSTOMER, "Everyone should know what you sell!", ANGRY, POINT, MEDIUM)
        cam(REACTION, STRANGER)
        act(STRANGER, NONE, WORRIED, 800)
        say(NARRATOR, "The other customers are watching to see what you do.")
        cam(TWO_SHOT, PLAYER, CUSTOMER)

        option(0, "I’m so sorry. Here’s a full refund.", WORRIED, NOD, MEDIUM) {
            hold(CUSTOMER, Prop.NONE)
            hold(PLAYER, Prop.BOX)
            act(CUSTOMER, NONE, SURPRISED, 900)
            say(CUSTOMER, "…Thank you. That’s fair.", NEUTRAL, NOD, CLOSE_UP)
            exit(CUSTOMER, Edge.RIGHT)
            act(STRANGER, NOD, HAPPY, 1000)
            say(NARRATOR, "Later, a glowing review appears. People remember how you handled it.")
        }
        option(1, "Our product is fine. You used it wrong.", ANGRY, CROSS_ARMS, MEDIUM) {
            say(CUSTOMER, "Unbelievable. I’m never coming back!", ANGRY, POINT, CLOSE_UP)
            cue("door")
            exit(CUSTOMER, Edge.RIGHT)
            act(STRANGER, SHAKE_HEAD, DISAPPOINTED, 1000)
            exit(STRANGER, Edge.LEFT)
            say(NARRATOR, "You win the argument and lose the customer, and a few who overheard.")
        }
        option(2, "I’ll replace it, and give you a discount.", NEUTRAL, NOD, MEDIUM) {
            hold(CUSTOMER, Prop.NONE)
            hold(PLAYER, Prop.BOX)
            say(CUSTOMER, "…Fine. I suppose that’s reasonable.", NEUTRAL, SHRUG, CLOSE_UP)
            act(CUSTOMER, NOD, NEUTRAL, 900)
            say(NARRATOR, "A decent compromise. They grumble, but accept.")
        }
        option(3, "Show me what broke. I’ll fix the root cause.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            move(PLAYER, 0.56f)
            hold(CUSTOMER, Prop.NONE)
            hold(PLAYER, Prop.BOX)
            act(PLAYER, THINK, THOUGHTFUL, 1400)
            say(PLAYER, "There it is. A weak seam. We’ll fix it.", CONFIDENT, POINT, CLOSE_UP)
            act(CUSTOMER, NONE, SURPRISED, 800)
            hold(PLAYER, Prop.DOCUMENTS)
            say(NARRATOR, "You find the flaw, fix it, and write a clear policy. Next time it won’t happen.")
        }
    }

    scene("biz_investor", MEETING) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        place(STRANGER, 0.68f, LEFT, seated = true)
        hold(STRANGER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(STRANGER, "I love what you’ve built, {name}.", HAPPY, GENTLE_SMILE, MEDIUM)
        say(STRANGER, "I’ll invest a large sum. For a big share.", CONFIDENT, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(PLAYER, "A big share? Of everything?", WORRIED, NONE, CLOSE_UP)
        say(NARRATOR, "Room to grow. And someone else with a say in everything.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Deal. I accept the full offer.", EXCITED, HANDSHAKE, MEDIUM) {
            act(STRANGER, HANDSHAKE, HAPPY, 1000)
            outcome(0) {
                say(NARRATOR, "Their money and connections open doors you couldn’t reach alone.")
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
            }
            outcome(1) {
                say(NARRATOR, "Their priorities differ from yours. You win growth, but lose some freedom.")
                act(PLAYER, NONE, WORRIED, 1200)
            }
        }
        option(1, "How about a smaller stake?", CONFIDENT, EXPLAIN, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 800)
            say(STRANGER, "Bold. Let’s negotiate.", THOUGHTFUL, SHRUG, CLOSE_UP)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NOD, CONFIDENT, 1600)
            say(NARRATOR, "It takes patience, and a lawyer. But you keep funding and control.")
        }
        option(2, "Thank you, but I’ll grow on my own terms.", CONFIDENT, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "A pity. But I respect it.", NEUTRAL, NOD, CLOSE_UP)
            hold(STRANGER, Prop.NONE)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            say(NARRATOR, "It’s slower, but it stays yours.")
        }
        option(3, "Let me ask {mentor} first.", THOUGHTFUL, PHONE, MEDIUM) {
            hold(PLAYER, Prop.PHONE)
            voice(MENTOR, "Send me the contract. I’ll read it twice.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            card("THE NEXT DAY")
            voice(MENTOR, "Clause nine is a trap. Here’s what to ask for.", THOUGHTFUL)
            say(NARRATOR, "You walk back in prepared.")
        }
    }

    scene("biz_partner_choice", MEETING) {
        place(PLAYER, 0.26f, RIGHT, seated = true)
        place(FRIEND, 0.54f, LEFT, seated = true)
        place(STRANGER, 0.82f, LEFT, seated = true)
        cam(WIDE)
        say(PLAYER, "The business needs more hands. And more money.", TIRED, EXPLAIN, MEDIUM)
        whenever(trustAtLeast(NpcRole.BEST_FRIEND, 65), otherwise = {
            say(FRIEND, "I’d love to help. If you’re sure.", WORRIED, SHRUG, MEDIUM)
        }) {
            say(FRIEND, "I’m in. Always. Just say the word.", EXCITED, FIST_PUMP, MEDIUM)
        }
        say(STRANGER, "Fifteen years in this industry. I can start Monday.", CONFIDENT, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        say(NARRATOR, "Whoever you choose will shape everything.")
        cam(WIDE)

        option(0, "{friend}. I trust you completely.", HAPPY, GENTLE_SMILE, MEDIUM) {
            stand(PLAYER)
            stand(FRIEND)
            move(FRIEND, 0.42f)
            act(FRIEND, HUG, HAPPY, 1200)
            act(STRANGER, NOD, NEUTRAL, 900)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            outcome(0) { say(NARRATOR, "Your friend’s loyalty is exactly what the business needed.") }
            outcome(1) {
                act(FRIEND, NONE, WORRIED, 1000)
                say(NARRATOR, "Skills didn’t match friendship. Painful conversations, but you work it out.")
            }
        }
        option(1, "I’d like to work with you.", CONFIDENT, HANDSHAKE, MEDIUM) {
            stand(STRANGER)
            move(STRANGER, 0.44f)
            act(STRANGER, HANDSHAKE, HAPPY, 1100)
            act(FRIEND, HEAD_DOWN, SAD, 1000)
            say(FRIEND, "I understand. Good luck.", SAD, GENTLE_SMILE, CLOSE_UP)
            outcome(0) { say(NARRATOR, "The stranger’s experience saves you years. Trust builds slowly, but it builds.") }
            outcome(1) { say(NARRATOR, "Different values, different goals. The partnership ends within the year.") }
        }
        option(2, "I’ll keep going alone. Thank you both.", TIRED, SHAKE_HEAD, MEDIUM) {
            stand(FRIEND)
            stand(STRANGER)
            exit(FRIEND, Edge.RIGHT, concurrent = true)
            exit(STRANGER, Edge.RIGHT)
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, HEAD_DOWN, TIRED, 1500)
            say(NARRATOR, "Full control, and full responsibility. Some nights, the loneliness is heavy.")
        }
        option(3, "I’ll take an investor’s money, with conditions.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            stand(FRIEND)
            stand(STRANGER)
            exit(FRIEND, Edge.RIGHT, concurrent = true)
            exit(STRANGER, Edge.RIGHT)
            hold(PLAYER, Prop.DOCUMENTS)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NOD, CONFIDENT, 1500)
            say(NARRATOR, "The cash helps, and so do the investor’s contacts. Their opinions come with it.")
        }
    }

    scene("biz_expansion", MEETING) {
        place(PLAYER, 0.32f, RIGHT)
        place(COWORKER, 0.68f, LEFT)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(COWORKER, "The second location is still available.", NEUTRAL, EXPLAIN, MEDIUM)
        say(COWORKER, "It could double our reach.", HAPPY, NONE, TWO_SHOT)
        say(COWORKER, "Or double our problems.", WORRIED, SHRUG, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        say(PLAYER, "Business has never been better.", THOUGHTFUL, EXPLAIN, MEDIUM)
        say(NARRATOR, "A real chance, and a real risk. The lease must be signed this week.")
        cam(TWO_SHOT, PLAYER, COWORKER)

        option(0, "Let’s expand. Sign the lease.", EXCITED, FIST_PUMP, MEDIUM) {
            act(COWORKER, NONE, WORRIED, 900)
            say(COWORKER, "Then we’d better be ready.", WORRIED, NOD, CLOSE_UP)
            card("MONTHS LATER")
            outcome(0) {
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                say(NARRATOR, "The new location is busy within weeks. You’re running a real company now.")
            }
            outcome(1) {
                act(PLAYER, HEAD_DOWN, TIRED, 1400)
                say(NARRATOR, "Overstretched, you close the second location. You keep the first, and the lesson.")
            }
        }
        option(1, "Let’s grow in smaller steps.", THOUGHTFUL, NOD, MEDIUM) {
            act(COWORKER, NONE, HAPPY, 900)
            say(COWORKER, "One hire at a time. I like that.", HAPPY, GENTLE_SMILE, CLOSE_UP)
            card("A YEAR LATER")
            outcome(0) {
                act(PLAYER, NONE, PROUD, 1200)
                say(NARRATOR, "Careful growth takes longer, and holds up better.")
            }
            outcome(1) {
                act(PLAYER, NOD, NEUTRAL, 1200)
                say(NARRATOR, "Steady, if unspectacular. The business holds its ground.")
            }
        }
        option(2, "We’ll stay the size we are.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(COWORKER, NONE, HAPPY, 800)
            say(COWORKER, "Honestly? That’s a relief.", HAPPY, NOD, CLOSE_UP)
            say(NARRATOR, "You love what you’ve built. There’s dignity in that.")
        }
        option(3, "Maybe it’s time to sell and cash out.", THOUGHTFUL, SHRUG, MEDIUM) {
            act(COWORKER, NONE, SURPRISED, 1000)
            say(COWORKER, "Sell? After everything?", SURPRISED, HANDS_UP, REACTION)
            say(NARRATOR, "A big cheque, a quiet Monday morning, and the strange feeling of having time.")
            cam(CLOSE_UP, PLAYER)
            act(PLAYER, GENTLE_SMILE, THOUGHTFUL, 1500)
        }
    }

    scene("biz_sell_or_keep", MEETING) {
        place(COWORKER, 0.14f, RIGHT)
        place(PLAYER, 0.44f, RIGHT, seated = true)
        place(STRANGER, 0.80f, LEFT, seated = true)
        hold(STRANGER, Prop.DOCUMENTS)
        cam(WIDE)
        say(STRANGER, "We’d like to acquire your company.", CONFIDENT, EXPLAIN, MEDIUM)
        say(STRANGER, "The figure is more than generous.", HAPPY, NONE, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, HEAD_DOWN, THOUGHTFUL, 1400)
        say(NARRATOR, "Your hands are tired. But this is so much more than a number on a page.")
        say(COWORKER, "Whatever you decide, we’re with you.", SAD, GENTLE_SMILE, MEDIUM)
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Alright. I’ll sell.", THOUGHTFUL, NOD, MEDIUM) {
            act(STRANGER, HANDSHAKE, HAPPY, 1100)
            say(STRANGER, "Wise. You’ve earned it.", PROUD, NOD, CLOSE_UP)
            act(COWORKER, HEAD_DOWN, SAD, 1200)
            card("THE NEXT MORNING")
            act(PLAYER, GENTLE_SMILE, HAPPY, 1500)
            say(NARRATOR, "You wake without an alarm for the first time in decades.")
        }
        option(1, "I’d rather pass it on to the people who built it.", PROUD, GENTLE_SMILE, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 800)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            cam(REACTION, COWORKER)
            act(COWORKER, NONE, SURPRISED, 900)
            say(COWORKER, "You… really mean that?", HAPPY, HANDS_UP, CLOSE_UP)
            stand(PLAYER)
            move(COWORKER, 0.32f)
            act(COWORKER, HUG, HAPPY, 1500)
            say(NARRATOR, "It’s a hard handover, but the right one. They keep the name, and the spirit.")
        }
        option(2, "No. I’m not done yet.", CONFIDENT, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "The offer won’t last forever.", NEUTRAL, SHRUG, CLOSE_UP)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            act(COWORKER, NONE, HAPPY, 1000)
            say(NARRATOR, "You’re still in the shop at dawn. For you, it’s not work. It’s a way of life.")
        }
        option(3, "I’ll sell, but I’d like to stay on as an adviser.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(STRANGER, "We’d welcome that.", HAPPY, HANDSHAKE, CLOSE_UP)
            act(PLAYER, HANDSHAKE, HAPPY, 1000)
            act(COWORKER, NOD, HAPPY, 1000)
            say(NARRATOR, "A quieter life with the best bits left in. You watch your creation grow without you.")
        }
    }

    // =====================================================================================
    //  MONEY
    // =====================================================================================

    scene("mon_first_savings", LIVING_ROOM) {
        place(PLAYER, 0.36f, RIGHT)
        place(MOTHER, 0.68f, LEFT)
        place(FATHER, 0.88f, LEFT)
        hold(MOTHER, Prop.BOX)
        cam(ESTABLISHING)
        say(MOTHER, "Happy birthday, {name}! This is from all your relatives.", HAPPY, EXPLAIN, MEDIUM)
        hold(MOTHER, Prop.NONE)
        hold(PLAYER, Prop.BOX)
        act(PLAYER, NONE, SURPRISED, 1000)
        say(PLAYER, "Whoa. That’s… a lot of money.", SURPRISED, HANDS_UP, CLOSE_UP)
        say(FATHER, "It’s yours. You decide what to do with it.", PROUD, NOD, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        say(NARRATOR, "You’ve never had this much money to yourself before.")

        option(0, "I’m going to save most of it.", CONFIDENT, NOD, MEDIUM) {
            act(FATHER, NOD, PROUD, 1000)
            say(FATHER, "That’s wise.", PROUD, GENTLE_SMILE, CLOSE_UP)
            say(NARRATOR, "The number in your account grows quietly. It feels good in a way spending doesn’t.")
        }
        option(1, "I’m buying something I’ve always wanted!", EXCITED, FIST_PUMP, MEDIUM) {
            say(MOTHER, "Already?", SURPRISED, HANDS_UP, REACTION)
            cue("whoosh")
            hold(PLAYER, Prop.BAG)
            exit(PLAYER, Edge.LEFT, run = true)
            say(NARRATOR, "You have a great week. By the end of the month, you barely remember where the money went.")
        }
        option(2, "I’ll put it toward a course for my hobby.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.BOOK)
            act(FATHER, NONE, SURPRISED, 800)
            say(FATHER, "A course? Now that’s an investment.", HAPPY, NOD, CLOSE_UP)
            say(NARRATOR, "It’s not glamorous. But it opens a door that was shut before.")
        }
        option(3, "Here. Take some. It’s for the family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            move(PLAYER, 0.54f)
            hold(PLAYER, Prop.NONE)
            hold(MOTHER, Prop.BOX)
            say(MOTHER, "No, sweetheart, we couldn’t…", SAD, SHAKE_HEAD, CLOSE_UP)
            say(PLAYER, "Please. I want to.", HAPPY, NOD, TWO_SHOT)
            act(MOTHER, HUG, SAD, 1800)
            say(NARRATOR, "Your parents hug you for a very long time.")
        }
    }

    scene("mon_emergency", APARTMENT, EVENING) {
        place(PLAYER, 0.34f, RIGHT, seated = true)
        cam(ESTABLISHING)
        cue("knock")
        enter(STRANGER, Edge.RIGHT, 0.70f)
        hold(STRANGER, Prop.DOCUMENTS)
        say(STRANGER, "Burst pipe. I can fix it today.", NEUTRAL, EXPLAIN, MEDIUM)
        say(STRANGER, "I’ll be honest. It’s not cheap.", WORRIED, SHRUG, TWO_SHOT)
        say(PLAYER, "How much?", WORRIED, NONE, OVER_SHOULDER)
        hold(STRANGER, Prop.NONE)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, FACEPALM, WORRIED, 1200)
        say(NARRATOR, "A large, unavoidable expense. It has to be paid this month.")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "Fine. I’ll pay it from my savings.", NEUTRAL, NOD, MEDIUM) {
            whenever(has("saved_money"), otherwise = {
                act(PLAYER, HEAD_DOWN, WORRIED, 1300)
                say(NARRATOR, "You have no cushion, and the stress is heavy.")
            }) {
                act(PLAYER, NOD, CONFIDENT, 1100)
                say(NARRATOR, "Having savings softens the blow. This is exactly what they were for.")
            }
            say(STRANGER, "I’ll get started.", NEUTRAL, NOD, MEDIUM)
        }
        option(1, "Put it on the credit card.", WORRIED, SHRUG, MEDIUM) {
            say(STRANGER, "Card works. Sign here.", NEUTRAL, POINT, CLOSE_UP)
            act(PLAYER, HEAD_DOWN, WORRIED, 1200)
            say(NARRATOR, "It solves today’s problem, and creates next year’s.")
        }
        option(2, "Can I ask my family for help?", WORRIED, PHONE, CLOSE_UP) {
            hold(PLAYER, Prop.PHONE)
            voice(MOTHER, "Of course. How much do you need?", HAPPY)
            whenever(has("supported_family")) {
                voice(MOTHER, "After everything you’ve done for us? Never ask twice.", PROUD)
            }
            hold(PLAYER, Prop.NONE)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1300)
            say(NARRATOR, "It’s a humbling call to make.")
        }
        option(3, "Is there a cheaper fix? I’ll learn to do it.", THOUGHTFUL, EXPLAIN, MEDIUM) {
            say(STRANGER, "There is. I’ll show you. Hold this.", HAPPY, POINT, TWO_SHOT)
            stand(PLAYER)
            move(PLAYER, 0.54f)
            act(PLAYER, SCRATCH_HEAD, TIRED, 1400)
            say(NARRATOR, "It’s messy, but you save a lot. And now you know a thing or two.")
        }
    }

    scene("mon_scam_call", APARTMENT, EVENING) {
        place(PLAYER, 0.40f, RIGHT, seated = true)
        cam(ESTABLISHING)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "Hello?", NEUTRAL, PHONE, MEDIUM)
        voice(STRANGER, "This is your bank’s security team. Your account is at risk.", WORRIED)
        voice(STRANGER, "I’m very worried for you. Please, we have to hurry.", AFRAID)
        voice(STRANGER, "I just need your password to protect it.", NEUTRAL)
        cam(PUSH_IN, PLAYER)
        act(PLAYER, NONE, WORRIED, 1300)
        say(PLAYER, "My… password?", WORRIED, SCRATCH_HEAD, CLOSE_UP)

        option(0, "I’ll hang up and call the bank directly.", CONFIDENT, SHAKE_HEAD, CLOSE_UP) {
            voice(STRANGER, "Sir, wait—", AFRAID)
            hold(PLAYER, Prop.NONE)
            card("A CALL TO THE BANK")
            act(PLAYER, NONE, PROUD, 1500)
            say(NARRATOR, "The bank confirms it was a scam. Relief, and a trace of pride.")
        }
        option(1, "Okay. Let me give you the details.", WORRIED, NOD, CLOSE_UP) {
            voice(STRANGER, "Thank you. That’s very helpful.", HAPPY)
            hold(PLAYER, Prop.NONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, HEAD_DOWN, SAD, 1700)
            say(NARRATOR, "Within an hour the account is drained. The bank recovers some, but it’s a painful lesson.")
        }
        option(2, "Can I have your name first? I’ll verify you.", THOUGHTFUL, EXPLAIN, CLOSE_UP) {
            voice(STRANGER, "…Hello? The line is… I have to go.", AFRAID)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NOD, CONFIDENT, 1300)
            say(NARRATOR, "They hang up fast. That tells you everything you need to know.")
        }
        option(3, "Scammers! I must warn my family.", ANGRY, POINT, CLOSE_UP) {
            voice(MOTHER, "Oh! I had a call like that today. I hung up.", PROUD)
            voice(MOTHER, "Thank you for warning me, {name}.", HAPPY)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, HAPPY, 1200)
            say(NARRATOR, "Thanks to you, your family knew exactly what to do.")
        }
    }

    scene("mon_buy_home", NEW_HOME) {
        place(PLAYER, 0.34f, RIGHT)
        place(STRANGER, 0.68f, LEFT)
        hold(STRANGER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(STRANGER, "Two bedrooms. Good light. A quiet street.", HAPPY, EXPLAIN, MEDIUM)
        say(PLAYER, "It’s… really nice.", HAPPY, NONE, CLOSE_UP)
        say(STRANGER, "The numbers just about work for you.", NEUTRAL, POINT, TWO_SHOT)
        say(STRANGER, "It will stretch you. But it would be yours.", NEUTRAL, SHRUG, OVER_SHOULDER)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1200)
        say(STRANGER, "Others are interested. I need an answer.", NEUTRAL, NONE, MEDIUM)
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’ll take it. Let’s sign.", EXCITED, FIST_PUMP, MEDIUM) {
            act(STRANGER, HANDSHAKE, HAPPY, 1000)
            card("THE DAY YOU GET THE KEYS")
            hold(STRANGER, Prop.NONE)
            exit(STRANGER, Edge.RIGHT)
            sit(PLAYER)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, GENTLE_SMILE, HAPPY, 2000)
            say(NARRATOR, "You sit on the floor of the empty living room and grin.")
        }
        option(1, "I’ll keep renting and save a bigger down payment.", THOUGHTFUL, NOD, MEDIUM) {
            say(STRANGER, "Sensible. Think of me when you’re ready.", NEUTRAL, NOD, CLOSE_UP)
            say(NARRATOR, "You’re patient. Next year’s home may be better, and the savings keep growing.")
        }
        option(2, "Do you have something smaller and cheaper?", THOUGHTFUL, EXPLAIN, MEDIUM) {
            act(STRANGER, NONE, THOUGHTFUL, 800)
            say(STRANGER, "Actually, yes. Let me show you.", HAPPY, POINT, CLOSE_UP)
            say(NARRATOR, "It isn’t your dream house, but it’s mostly paid for, and it’s yours.")
        }
        option(3, "I’ll wait for the market to improve.", NEUTRAL, SHRUG, MEDIUM) {
            say(STRANGER, "The market doesn’t wait.", NEUTRAL, SHRUG, CLOSE_UP)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NONE, THOUGHTFUL, 1300)
            say(NARRATOR, "The market doesn’t seem to care about your timing.")
        }
    }

    scene("mon_investment_scheme", COFFEE_SHOP) {
        place(PLAYER, 0.32f, RIGHT, seated = true)
        place(STRANGER, 0.68f, LEFT, seated = true)
        hold(STRANGER, Prop.PHONE)
        cam(ESTABLISHING)
        say(STRANGER, "{name}! Listen. This investment can’t lose.", EXCITED, EXPLAIN, MEDIUM)
        say(STRANGER, "Double your money in six months. Guaranteed.", EXCITED, POINT, TWO_SHOT)
        say(STRANGER, "I’ve already told everyone I know.", HAPPY, WAVE, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, WORRIED, 1100)
        say(PLAYER, "Guaranteed? Really?", THOUGHTFUL, SCRATCH_HEAD, CLOSE_UP)
        say(NARRATOR, "It sounds too good to be true. But what if it isn’t?")
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’m in. A big amount.", EXCITED, FIST_PUMP, MEDIUM) {
            act(STRANGER, FIST_PUMP, EXCITED, 900)
            card("SIX MONTHS LATER")
            outcome(0) {
                exit(STRANGER, Edge.RIGHT)
                act(PLAYER, HEAD_DOWN, SAD, 1500)
                say(NARRATOR, "It was a scam. If it sounds too good to be true, it almost always is.")
            }
            outcome(1) {
                act(PLAYER, NONE, EMBARRASSED, 1200)
                say(NARRATOR, "You get out with a small profit, mostly by luck. You know it.")
            }
        }
        option(1, "Let me research it carefully first.", THOUGHTFUL, NOD, MEDIUM) {
            say(STRANGER, "Research? There’s no time!", ANGRY, HANDS_UP, CLOSE_UP)
            hold(PLAYER, Prop.LAPTOP)
            card("THAT EVENING")
            cam(PUSH_IN, PLAYER)
            act(PLAYER, TYPE, THOUGHTFUL, 1600)
            act(PLAYER, NONE, SURPRISED, 1000)
            say(NARRATOR, "The research shows glaring red flags. You tell your friends to stay away.")
        }
        option(2, "I’ll put a small amount in something boring.", NEUTRAL, EXPLAIN, MEDIUM) {
            say(STRANGER, "Boring? Where’s the fun?", DISAPPOINTED, SHRUG, CLOSE_UP)
            say(PLAYER, "Boring grows. Slowly.", CONFIDENT, GENTLE_SMILE, TWO_SHOT)
            say(NARRATOR, "No fireworks, just slow, steady growth. Compound interest begins its quiet work.")
        }
        option(3, "No, thanks. And I’ll warn others, too.", CONFIDENT, SHAKE_HEAD, MEDIUM) {
            say(STRANGER, "Your loss!", ANGRY, POINT, CLOSE_UP)
            hold(STRANGER, Prop.NONE)
            stand(STRANGER)
            exit(STRANGER, Edge.RIGHT)
            act(PLAYER, NOD, CONFIDENT, 1100)
            say(NARRATOR, "Not everyone listens. A few do, and they thank you later.")
        }
    }

    // =====================================================================================
    //  PAYOFFS
    // =====================================================================================

    scene("pay_coworker_returns", MEETING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "{coworker}? It’s been years!", SURPRISED, PHONE, MEDIUM)
        voice(COWORKER, "I’ve moved up. I’m in charge of hiring now.", PROUD)
        voice(COWORKER, "I remember who stayed late to help me.", HAPPY)
        whenever(trustAtLeast(NpcRole.COWORKER, 75), otherwise = {
            voice(COWORKER, "A favour like that is hard to forget.", HAPPY)
        }) {
            voice(COWORKER, "You’re the first person I thought of.", PROUD)
        }
        voice(COWORKER, "I have a senior role for you. Interested?", EXCITED)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1300)

        option(0, "Yes. I’ll take the senior role.", EXCITED, FIST_PUMP, CLOSE_UP) {
            voice(COWORKER, "Wonderful! Welcome aboard.", HAPPY)
            hold(PLAYER, Prop.NONE)
            cue("cheer")
            act(PLAYER, FIST_PUMP, EXCITED, 1200)
            say(NARRATOR, "A better title, a better salary, and a boss who trusts you completely.")
        }
        option(1, "Hire my friend instead. They’re perfect.", HAPPY, EXPLAIN, MEDIUM) {
            voice(COWORKER, "A recommendation from you? Done.", PROUD)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, NONE, PROUD, 1300)
            say(NARRATOR, "Your friend gets the job, and you get a reputation for generosity.")
        }
        option(2, "Thank you, but I’m staying where I am.", HAPPY, SHAKE_HEAD, CLOSE_UP) {
            voice(COWORKER, "I understand. The offer stays open.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, GENTLE_SMILE, HAPPY, 1300)
            say(NARRATOR, "It’s nice to be asked. You’re exactly where you want to be.")
        }
        option(3, "Let me talk terms with my own boss first.", CONFIDENT, EXPLAIN, MEDIUM) {
            voice(COWORKER, "Of course. Take your time.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            enter(BOSS, Edge.RIGHT, 0.72f)
            say(PLAYER, "{boss}, I have an offer. I’d like to talk.", NEUTRAL, EXPLAIN, TWO_SHOT)
            act(BOSS, NONE, SURPRISED, 900)
            say(BOSS, "…Let me see what I can do.", THOUGHTFUL, THINK, CLOSE_UP)
            say(NARRATOR, "Everyone gets a better deal, and a slightly awkward Monday.")
        }
    }

    scene("pay_refused_unethical", OFFICE) {
        place(PLAYER, 0.28f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        place(COWORKER, 0.58f, LEFT)
        card("YEARS LATER")
        cam(ESTABLISHING)
        say(COWORKER, "Have you heard? The audit found everything.", WORRIED, EXPLAIN, MEDIUM)
        say(COWORKER, "People are being let go. It’s chaos.", AFRAID, HANDS_UP, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1000)
        say(PLAYER, "The numbers. I knew they were wrong.", THOUGHTFUL, NONE, CLOSE_UP)
        whenever(has("has_mentor")) {
            say(NARRATOR, "You remember what {mentor} once told you about honesty.")
        }
        cue("steps")
        enter(STRANGER, Edge.RIGHT, 0.86f)
        say(STRANGER, "{name}? A word, please.", NEUTRAL, NONE, MEDIUM)
        say(STRANGER, "Your name came up in the investigation.", NEUTRAL, EXPLAIN, TWO_SHOT)
        say(STRANGER, "You refused to touch those numbers, years ago.", PROUD, NOD, CLOSE_UP)
        say(STRANGER, "We need someone we can trust.", CONFIDENT, POINT, MEDIUM)
        cam(TWO_SHOT, PLAYER, STRANGER)

        option(0, "I’ll lead the clean-up.", CONFIDENT, NOD, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            move(PLAYER, 0.66f)
            act(STRANGER, HANDSHAKE, PROUD, 1100)
            act(COWORKER, CLAP, HAPPY, 900)
            say(NARRATOR, "The board looked back through the records to find you. Your integrity is rewarded.")
        }
        option(1, "I’ll help, but quietly. No spotlight.", NEUTRAL, GENTLE_SMILE, MEDIUM) {
            say(STRANGER, "As you wish. We’re grateful.", NEUTRAL, NOD, CLOSE_UP)
            act(COWORKER, NONE, HAPPY, 1000)
            say(NARRATOR, "You help quietly, and the company sets things right. Your colleagues know who was honest.")
        }
        option(2, "I’ll help. And I’d like a better position.", CONFIDENT, EXPLAIN, MEDIUM) {
            act(STRANGER, NONE, SURPRISED, 800)
            say(STRANGER, "…We can discuss that.", THOUGHTFUL, THINK, CLOSE_UP)
            say(NARRATOR, "It works, mostly, though you lose a little goodwill by asking.")
        }
        option(3, "I’d rather work somewhere that values honesty.", THOUGHTFUL, SHRUG, MEDIUM) {
            say(STRANGER, "I understand.", SAD, NOD, CLOSE_UP)
            hold(PLAYER, Prop.NONE)
            stand(PLAYER)
            exit(PLAYER, Edge.LEFT)
            say(NARRATOR, "A headhunter finds you before you’ve even updated your résumé.")
        }
    }

    scene("pay_hard_work", MEETING) {
        place(PLAYER, 0.36f, RIGHT, seated = true)
        hold(PLAYER, Prop.LAPTOP)
        cam(ESTABLISHING)
        act(PLAYER, TYPE, NEUTRAL, 1500)
        say(NARRATOR, "Years of steady work. Nobody was keeping score. Except someone was.")
        whenever(has("has_mentor")) {
            say(NARRATOR, "You think of everything {mentor} once taught you.")
        }
        cue("phone")
        hold(PLAYER, Prop.PHONE)
        say(PLAYER, "{name} speaking.", NEUTRAL, PHONE, MEDIUM)
        voice(INTERVIEWER, "We’ve followed your work for years.", PROUD)
        voice(INTERVIEWER, "Your steadiness. Your results. Your reputation.", NEUTRAL)
        voice(INTERVIEWER, "We’d like to talk about a leadership role.", EXCITED)
        cam(CLOSE_UP, PLAYER)
        act(PLAYER, NONE, SURPRISED, 1300)

        option(0, "I’m honoured. I’ll take it.", HAPPY, FIST_PUMP, CLOSE_UP) {
            voice(INTERVIEWER, "Excellent. Welcome to the firm.", HAPPY)
            hold(PLAYER, Prop.NONE)
            cam(PUSH_IN, PLAYER)
            act(PLAYER, NONE, CONFIDENT, 1600)
            say(NARRATOR, "Years of small, disciplined choices add up to one big opportunity.")
        }
        option(1, "I’ll use this to negotiate with my boss.", CONFIDENT, EXPLAIN, MEDIUM) {
            voice(INTERVIEWER, "Of course. We understand.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            enter(BOSS, Edge.RIGHT, 0.72f)
            say(PLAYER, "{boss}, I’ve had an offer elsewhere.", NEUTRAL, EXPLAIN, TWO_SHOT)
            act(BOSS, NONE, SURPRISED, 900)
            say(BOSS, "Then let’s make sure you stay.", NEUTRAL, NOD, CLOSE_UP)
            say(NARRATOR, "Your boss is impressed. You stay, with a raise and a new project.")
        }
        option(2, "Thank you, but I’m happy where I am.", HAPPY, SHAKE_HEAD, CLOSE_UP) {
            voice(INTERVIEWER, "If you ever change your mind, we’ll be here.", NEUTRAL)
            hold(PLAYER, Prop.NONE)
            act(PLAYER, GENTLE_SMILE, HAPPY, 1400)
        }
        option(3, "Could I start with a trial project?", THOUGHTFUL, NOD, MEDIUM) {
            voice(INTERVIEWER, "Smart. We’ll arrange one.", HAPPY)
            hold(PLAYER, Prop.NONE)
            card("A TRIAL PROJECT LATER")
            act(PLAYER, NOD, PROUD, 1300)
            say(NARRATOR, "The trial goes well, and the offer becomes a standing invitation.")
        }
    }

    scene("pay_sabotage_echo", MEETING, EVENING) {
        place(PLAYER, 0.30f, RIGHT)
        hold(PLAYER, Prop.CUP)
        card("AN INDUSTRY DINNER")
        cam(ESTABLISHING)
        enter(RIVAL, Edge.RIGHT, 0.68f)
        say(RIVAL, "{name}. It’s been a long time.", NEUTRAL, NONE, MEDIUM)
        say(RIVAL, "I always wondered what happened to my science project.", THOUGHTFUL, EXPLAIN, TWO_SHOT)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, AFRAID, 1100)
        say(RIVAL, "I figured it out, years later.", NEUTRAL, NONE, CLOSE_UP)
        say(RIVAL, "I just wanted you to know I knew.", SAD, NONE)
        cam(TWO_SHOT, PLAYER, RIVAL)

        option(0, "It was me. I’m sorry. Truly.", SAD, HEAD_DOWN, CLOSE_UP) {
            hold(PLAYER, Prop.NONE)
            act(RIVAL, NONE, SURPRISED, 1100)
            say(RIVAL, "Thank you. That took courage.", THOUGHTFUL, NOD, CLOSE_UP)
            say(NARRATOR, "It isn’t forgiveness, quite. But it’s a start.")
        }
        option(1, "I have no idea what you mean.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(RIVAL, "Of course you don’t.", ANGRY, CROSS_ARMS, CLOSE_UP)
            exit(RIVAL, Edge.RIGHT)
            act(PLAYER, NONE, EMBARRASSED, 1300)
            say(NARRATOR, "{rival} tells a few people. You can feel the temperature drop.")
        }
        option(2, "That was a long time ago.", NEUTRAL, SHRUG, MEDIUM) {
            say(RIVAL, "It was. And I’ve still carried it.", SAD, HEAD_DOWN, CLOSE_UP)
            act(PLAYER, HEAD_DOWN, EMBARRASSED, 1200)
        }
        option(3, "Let me make it up to you. In practical ways.", CONFIDENT, EXPLAIN, MEDIUM) {
            hold(PLAYER, Prop.NONE)
            act(RIVAL, NONE, SURPRISED, 1000)
            say(RIVAL, "…How?", THOUGHTFUL, NONE, CLOSE_UP)
            say(PLAYER, "A referral. An introduction. Whatever you need.", CONFIDENT, NOD, TWO_SHOT)
            act(RIVAL, NOD, THOUGHTFUL, 1200)
            say(NARRATOR, "Actions speak louder than apologies, and {rival} notices.")
        }
    }

    scene("pay_savings_opportunity", NEW_HOME) {
        place(PLAYER, 0.34f, RIGHT)
        place(FRIEND, 0.68f, LEFT)
        hold(FRIEND, Prop.DOCUMENTS)
        cam(ESTABLISHING)
        say(FRIEND, "{name}, I found something you need to see.", EXCITED, WAVE, MEDIUM)
        say(FRIEND, "A small property. Well below market value.", EXCITED, EXPLAIN, TWO_SHOT)
        say(FRIEND, "But whoever buys it needs ready money. Now.", WORRIED, POINT, CLOSE_UP)
        cam(REACTION, PLAYER)
        act(PLAYER, NONE, THOUGHTFUL, 1100)
        whenever(has("owns_home"), otherwise = {
            say(PLAYER, "Years of saving, for a moment like this.", HAPPY, NOD, CLOSE_UP)
        }) {
            say(PLAYER, "Another property? I do have my savings.", THOUGHTFUL, NOD, CLOSE_UP)
        }
        cam(TWO_SHOT, PLAYER, FRIEND)

        option(0, "I’ll take the chance, with part of my savings.", CONFIDENT, FIST_PUMP, MEDIUM) {
            hold(FRIEND, Prop.NONE)
            hold(PLAYER, Prop.DOCUMENTS)
            act(FRIEND, HANDSHAKE, HAPPY, 1000)
            card("MONTHS LATER")
            outcome(0) {
                cue("cheer")
                act(PLAYER, FIST_PUMP, EXCITED, 1100)
                say(NARRATOR, "It pays off nicely. Being prepared turned a risk into a measured bet.")
            }
            outcome(1) {
                act(PLAYER, NONE, WORRIED, 1000)
                say(NARRATOR, "It doesn’t pan out, but you can absorb the loss. That’s what savings are for.")
            }
        }
        option(1, "I’ll put the money into something safe.", NEUTRAL, NOD, MEDIUM) {
            say(FRIEND, "Safe. Sensible.", NEUTRAL, SHRUG, CLOSE_UP)
            say(NARRATOR, "Slow and steady. You’ll never regret being careful.")
        }
        option(2, "Let’s share this with friends and family.", HAPPY, GENTLE_SMILE, MEDIUM) {
            act(FRIEND, NONE, SURPRISED, 800)
            say(FRIEND, "Everyone benefits. I love it.", HAPPY, HANDSHAKE, CLOSE_UP)
            say(NARRATOR, "A community effort, with everyone benefitting. It’s a good feeling.")
        }
        option(3, "Thanks. I’ll keep the money for the future.", NEUTRAL, SHAKE_HEAD, MEDIUM) {
            say(FRIEND, "Fair enough. Your call.", NEUTRAL, SHRUG, CLOSE_UP)
            say(NARRATOR, "Nothing to regret. The money is there if you ever need it.")
        }
    }
}
