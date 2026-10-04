package com.lifeyourchoice.core.story.packs

import com.lifeyourchoice.core.model.CareerTrack.*
import com.lifeyourchoice.core.model.Category.*
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.NpcRole.*
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.SceneArt.*
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait.*
import com.lifeyourchoice.core.story.*

/** High-school years: friends, pressure, first choices that echo for decades. */
val SchoolStories = storyPack("school", "School Life") {

    scenario("sch_skip_school", SCHOOL, HALLWAY, 14..17, priority = 5) {
        text("In the hallway, {friend} leans close and whispers: “Let’s skip school today and go to the mall. It’ll be fun, and nobody will know.”")
        choice("Go with {friend.him}.") {
            knowledge(-3); discipline(-4); happiness(+5); trust(BEST_FRIEND, +4)
            trait(IMPULSIVE, 2); flag("skipped_school"); count("mistakes")
            result("You spend the day at the mall. It’s fun, but you can’t shake the feeling that you’ve crossed a line.")
        }
        choice("Refuse and go to class.") {
            knowledge(+2); discipline(+3); confidence(+1); trust(BEST_FRIEND, -2)
            trait(RESPONSIBLE, 2)
            result("{friend} rolls {friend.his} eyes and leaves alone. In class you keep up, but the empty seat beside you feels odd.")
        }
        choice("Tell {friend.him} you’ll meet after school.") {
            discipline(+1); happiness(+1); trust(BEST_FRIEND, +2)
            result("You split the difference. {friend} grins, and the day’s lessons stay intact.")
        }
        choice("Ask {friend.him} why {friend.he} doesn’t want to be in school.") {
            confidence(+2); trust(BEST_FRIEND, +4); trait(COMPASSIONATE, 2); hint()
            then("sch_bullied_friend")
            result("The grin slips. {friend} looks away for a moment, then quietly tells you the truth.")
        }
    }

    scenario("sch_bullied_friend", SCHOOL, HALLWAY, 14..18, branch = true, months = 3) {
        text("{friend} admits that a group of older students has been pushing {friend.him} around and taking {friend.his} lunch money. “That’s why I can’t face school,” {friend.he} says.")
        choice("Help {friend.him} confront the situation.") {
            trait(RISK_TAKER, 2); trait(LOYAL, 3); trust(BEST_FRIEND, +10); count("kindness")
            flag("helped_bullied_friend"); milestone("Stood up for a bullied friend"); hint()
            outcome(45) {
                bonus(atLeast(Stat.CONFIDENCE, 55), 20); bonus(atLeast(Stat.REPUTATION, 55), 10)
                confidence(+8); reputation(+5); friendship(+6)
                result("You both stand your ground in front of a crowd of onlookers. The bullies back off, and word spreads that you two are not to be pushed around.")
            }
            outcome(40) {
                bonus(atMost(Stat.CONFIDENCE, 45), 15)
                confidence(+2); friendship(+5); health(-3); reputation(-2)
                result("It gets messy and loud, and you both end up in the principal’s office. But {friend} knows you stood beside {friend.him}.")
            }
        }
        choice("Tell a teacher.") {
            knowledge(+1); confidence(+3); friendship(+5); trust(BEST_FRIEND, +6); trait(RESPONSIBLE, 3)
            flag("helped_bullied_friend"); milestone("Told a teacher about bullying"); hint()
            result("{friend} is nervous, but the teacher listens and acts quickly. The bullying slowly stops.")
        }
        choice("Ignore it. It’s not your problem.") {
            confidence(-3); friendship(-5); trust(BEST_FRIEND, -12); happiness(-3)
            flag("ignored_bullied_friend"); count("mistakes"); hint()
            result("{friend} stops talking about it. Over the weeks, the two of you drift apart, and you notice but say nothing.")
        }
        choice("Encourage {friend.him} to talk to {friend.his} parents.") {
            trait(COMPASSIONATE, 3); trust(BEST_FRIEND, +7); friendship(+4); count("kindness")
            result("It takes courage, but {friend} does it. {friend.His} parents act, and {friend.he} thanks you for pushing {friend.him} to speak up.")
        }
    }

    scenario("sch_exam_pressure", SCHOOL, CLASSROOM, 14..18, weight = 14) {
        text("The biggest exam of the term is tomorrow. You’re behind on the material, your phone keeps lighting up, and the night is getting shorter.")
        choice("Study all night.") {
            knowledge(+6); discipline(+3); energy(-18); health(-3); trait(DISCIPLINED, 2)
            result("Coffee, notes, flashcards. You walk into the exam exhausted, but you know the material.")
        }
        choice("Study for two hours, then sleep.") {
            knowledge(+3); discipline(+2); energy(-4); trait(RESPONSIBLE, 2)
            result("A steady plan. You wake up rested and manage a solid result.")
        }
        choice("Hang out with friends. You’ll wing it.") {
            knowledge(-2); happiness(+5); friendship(+3); trait(IMPULSIVE, 2)
            outcome(50) { bonus(atLeast(Stat.KNOWLEDGE, 60), 25); knowledge(+1); result("To your surprise, you scrape through on what you already knew.") }
            outcome(50) { bonus(atMost(Stat.KNOWLEDGE, 45), 20); knowledge(-3); confidence(-3); result("The paper is full of questions you can’t answer. It stings.") }
        }
        choice("Ask your best friend to study together.") {
            knowledge(+4); friendship(+4); trust(BEST_FRIEND, +3); energy(-8)
            result("You quiz each other until midnight. It’s more fun than studying alone.")
        }
    }

    scenario("sch_cheating_offer", SCHOOL, CLASSROOM, 14..18, weight = 9) {
        text("Before the test, a classmate slides you a folded paper. “It’s the answer sheet. Everybody who’s smart is using it.” The teacher is looking at the board.")
        choice("Take it. Just this once.") {
            trait(DISHONEST, 8); discipline(-4); flag("cheated_exam", "took_shortcut"); count("mistakes")
            hint(); schedule("pay_cheat_echo", 12, 25)
            outcome(65) { bonus(atLeast(Stat.CONFIDENCE, 55), 10); knowledge(-1); result("Nobody notices. You get a great grade, and a knot in your stomach that lasts all week.") }
            outcome(35) { reputation(-8); confidence(-5); result("The teacher spots the paper. You’re sent to the office, and your parents get a call.") }
        }
        choice("Refuse and focus on your own paper.") {
            trait(RESPONSIBLE, 3); discipline(+3); confidence(+2); flag("refused_cheating")
            result("It’s harder, and the grade is only average. But it’s yours, and you sleep fine.")
        }
        choice("Tell the teacher after the test.") {
            reputation(+3); friendship(-4); trait(RESPONSIBLE, 3); confidence(+1); hint()
            result("The teacher thanks you quietly. A few classmates give you cold looks in the hallway.")
        }
        choice("Offer to study with them next time instead.") {
            trait(COMPASSIONATE, 2); knowledge(+2); friendship(+2); count("kindness")
            result("They shrug, but later they actually show up at your table. It’s not a bad start.")
        }
    }

    scenario("sch_underdog", SCHOOL, CLASSROOM, 14..17, weight = 12) {
        text("{underdog}, a quiet classmate who’s new to the school, is eating lunch alone again. A few kids nearby are laughing about {underdog.him}.")
        choice("Sit with {underdog.him} and talk.") {
            trait(COMPASSIONATE, 4); confidence(+2); reputation(-1); happiness(+3); trust(UNDERDOG, +25)
            flag("helped_underdog"); count("kindness"); milestone("Befriended a lonely classmate"); hint()
            schedule("pay_underdog_returns", 12, 22)
            result("{underdog} is startled, then smiles properly for the first time. Turns out {underdog.he} is funny, and a lot smarter than people assume.")
        }
        choice("Join in the laughing.") {
            reputation(+2); confidence(+2); trait(DISHONEST, 1); trust(UNDERDOG, -20)
            flag("mocked_underdog"); count("mistakes"); hint()
            schedule("pay_underdog_mocked", 12, 22)
            result("You get a few laughs. {underdog} packs up and leaves without a word, and it doesn’t feel as good as you expected.")
        }
        choice("Stay out of it.") {
            discipline(+1); trust(UNDERDOG, -2)
            result("You eat with your friends. Nothing changes, and nothing happens.")
        }
        choice("Quietly tell the others to cut it out.") {
            confidence(+3); reputation(+3); trait(RESPONSIBLE, 2); trust(UNDERDOG, +12); count("kindness")
            flag("helped_underdog"); schedule("pay_underdog_returns", 14, 24)
            result("The laughing stops. {underdog} doesn’t say anything, but gives you a small, grateful nod.")
        }
    }

    scenario("sch_tryouts", SCHOOL, BASKETBALL_COURT, 14..17, weight = 10) {
        text("Basketball tryouts start this week. You’ve always wanted to be on the team, but the gym is full of players who look bigger, faster and more confident than you.")
        choice("Practice every morning before school.") {
            health(+5); discipline(+5); energy(-8); trait(DISCIPLINED, 3)
            outcome(60) { bonus(atLeast(Stat.HEALTH, 70), 15); reputation(+6); confidence(+6); friendship(+3); flag("on_the_team"); result("Your hard work shows. You make the team and earn a spot in the rotation.") }
            outcome(40) { confidence(+2); result("You don’t make the cut this year, but you’re fitter and tougher than you’ve ever been.") }
        }
        choice("Go with {friend}, just for fun.") {
            happiness(+5); friendship(+4); trust(BEST_FRIEND, +4); health(+2)
            result("You laugh through every drill. Neither of you makes the team, but you both have a story to tell.")
        }
        choice("Skip it. You’d only embarrass yourself.") {
            confidence(-4); happiness(-2); trait(RISK_TAKER, -1); count("risk_avoided")
            result("You tell yourself it’s no big deal. Watching the team practice from the bleachers, you’re not so sure.")
        }
        choice("Try the track team instead.") {
            health(+4); discipline(+3); confidence(+3); trait(AMBITIOUS, 1)
            result("Running suits you. You’re slower than you’d like, but nobody’s competing for your lane.")
        }
    }

    scenario("sch_rival_contest", SCHOOL, CLASSROOM, 14..18, weight = 10) {
        text("The school science fair is coming up. Your rival {rival} is already boasting about a prize-winning project. The winner gets a place in a regional competition.")
        choice("Work harder than ever on your own project.") {
            knowledge(+5); discipline(+4); energy(-10); trait(AMBITIOUS, 3)
            outcome(50) { bonus(atLeast(Stat.KNOWLEDGE, 55), 20); reputation(+7); confidence(+5); result("Your project places first. {rival} shakes your hand a little stiffly.") }
            outcome(50) { reputation(+2); result("You place second. {rival} wins, but you’re proud of how far you pushed.") }
        }
        choice("Suggest teaming up with {rival}.") {
            knowledge(+3); trust(RIVAL, +20); trait(COMPASSIONATE, 2); confidence(+3); hint()
            flag("allied_rival"); schedule("pay_rival_returns", 10, 20)
            result("{rival} is surprised, then agrees. Together you build something better than either of you could have alone.")
        }
        choice("Quietly spoil {rival}’s experiment.") {
            trait(DISHONEST, 7); reputation(-5); trust(RIVAL, -25); flag("sabotaged_rival", "took_shortcut"); count("mistakes"); hint()
            schedule("pay_sabotage_echo", 10, 20)
            result("{rival}’s project fails an hour before judging. Nobody knows it was you, but you do.")
        }
        choice("Skip the fair. It’s not worth the stress.") {
            happiness(+2); confidence(-3); count("risk_avoided"); trait(AMBITIOUS, -1)
            result("You spend the weekend relaxing. It’s peaceful, though you wonder what you might have built.")
        }
    }

    scenario("sch_teacher_conflict", SCHOOL, CLASSROOM, 14..18, weight = 9) {
        text("Your teacher accuses you of disrupting class in front of everyone. It wasn’t you, but {rival} smirks from the back row and no one speaks up.")
        choice("Argue back loudly.") {
            confidence(+2); reputation(-5); trait(IMPULSIVE, 3); discipline(-2)
            result("You’re sent to the principal’s office. You got your point across, but not in a way that helped.")
        }
        choice("Stay calm and explain after class.") {
            reputation(+4); trait(RESPONSIBLE, 3); confidence(+3); discipline(+2)
            flag("respected_by_teacher")
            result("Your teacher listens, apologises, and starts to see you as a student who can be trusted.")
        }
        choice("Ask {friend} to speak up for you.") {
            trust(BEST_FRIEND, +3); friendship(+3); confidence(+1); trait(LOYAL, 1)
            result("{friend} backs you up. It works, and you feel a little better knowing someone has your back.")
        }
        choice("Accept the blame to avoid a scene.") {
            reputation(-3); confidence(-3); happiness(-2); discipline(+1)
            result("It blows over, but you’re the one with the mark next to your name.")
        }
    }

    scenario("sch_study_vs_fun", SCHOOL, LIVING_ROOM, 14..18, weight = 12) {
        text("Your favourite band is playing a free concert downtown tonight. A big project is due tomorrow and you’ve barely started.")
        choice("Go to the concert. You can finish it in the morning.") {
            happiness(+8); friendship(+3); knowledge(-3); discipline(-3); trait(IMPULSIVE, 2)
            result("The night is unforgettable. The morning, less so: the project is rushed and shows it.")
        }
        choice("Stay home and finish the project.") {
            knowledge(+4); discipline(+4); happiness(-3); trait(DISCIPLINED, 3)
            result("You hand it in on time, polished and proud, but you keep hearing the music from your window.")
        }
        choice("Do an hour of work, then go for the second half.") {
            knowledge(+2); happiness(+4); discipline(+1); energy(-6)
            result("A tidy compromise. You get the best bits of the night and a decent grade.")
        }
        choice("Ask the teacher for an extension.") {
            happiness(+4); reputation(-2); discipline(-1); trait(RESPONSIBLE, 1)
            outcome(55) { bonus(has("respected_by_teacher"), 35); result("The teacher agrees, given your record. You enjoy the concert guilt-free.") }
            outcome(45) { knowledge(-2); reputation(-2); result("The answer is no, and now you’re stuck with a late night anyway.") }
        }
    }

    scenario("sch_help_classmate", SCHOOL, CLASSROOM, 14..18, weight = 10) {
        text("A classmate is on the verge of failing math. They ask if you could help them catch up before the next test. It would eat into your own study time.")
        choice("Tutor them every afternoon.") {
            knowledge(+3); energy(-8); trait(COMPASSIONATE, 3); friendship(+4); reputation(+3); count("kindness")
            result("It takes patience, but they pass. You’ve learned the topic better by explaining it.")
        }
        choice("Hand over your notes and wish them luck.") {
            knowledge(+1); friendship(+2); trait(COMPASSIONATE, 1)
            result("They’re grateful. It’s a small favour, but it helps.")
        }
        choice("Say you’re too busy.") {
            knowledge(+1); friendship(-2); discipline(+1)
            result("You keep your afternoons free. They figure it out some other way.")
        }
        choice("Offer to help for a small fee.") {
            money(+4); trait(AMBITIOUS, 2); reputation(-2); friendship(-1)
            result("They pay, a bit grudgingly. It’s your first real income, and a lesson in how it feels to be paid for knowledge.")
        }
    }

    scenario("sch_group_project", SCHOOL, CLASSROOM, 14..18, weight = 10) {
        text("In your group project, two teammates have done nothing for weeks. The deadline is close, and the grade will be shared.")
        choice("Do most of it yourself.") {
            knowledge(+3); energy(-14); trait(RESPONSIBLE, 3); reputation(+2); happiness(-3); discipline(+2)
            result("You pull it off, but you’re tired and resentful about carrying the others.")
        }
        choice("Report the situation to the teacher.") {
            reputation(+1); friendship(-4); trait(RESPONSIBLE, 2); confidence(+2)
            result("The teacher adjusts the grading. You lose some popularity, but not your integrity.")
        }
        choice("Divide the work and set clear deadlines.") {
            discipline(+4); confidence(+4); knowledge(+2); trait(DISCIPLINED, 2); reputation(+2)
            result("Giving everyone a clear role works wonders. It’s your first taste of leading a team.")
        }
        choice("Let it fail. They’ll learn.") {
            knowledge(-2); reputation(-3); happiness(-2)
            result("The project is a mess, and so is your grade. Nobody learns anything useful.")
        }
    }

    scenario("sch_late_party", SCHOOL, STREET, 15..18, weight = 9) {
        text("Older kids invite you to a late-night party across town. Your parents think you’re studying at a friend’s place. Everyone says it’ll be the night of the year.")
        choice("Sneak out and go.") {
            happiness(+6); family(-5); health(-3); trait(RISK_TAKER, 3); trait(IMPULSIVE, 2); flag("snuck_out"); count("risk_taken")
            outcome(55) { result("It’s loud, exciting, and you’re home by 2 a.m. unnoticed.") }
            outcome(45) { bonus(atMost(Stat.DISCIPLINE, 45), 10); family(-5); reputation(-3); result("Your parents are waiting at the door. The conversation goes on for a long time.") }
        }
        choice("Tell your parents and ask permission.") {
            family(+4); trait(RESPONSIBLE, 3); trait(DISHONEST, -1)
            outcome(50) { happiness(+3); result("They agree on one condition: they’ll pick you up at midnight. It’s a fair deal.") }
            outcome(50) { happiness(-2); result("They say no, but they appreciate that you asked. You sulk for exactly one evening.") }
        }
        choice("Say no and stay home.") {
            family(+2); discipline(+2); happiness(-2); count("risk_avoided")
            result("You watch a movie instead. You hear about the party on Monday, and it was, in fact, a mess.")
        }
        choice("Invite them to hang out at your place instead.") {
            friendship(+4); happiness(+3); family(+1); trait(RESPONSIBLE, 1)
            result("Pizza, board games and bad jokes. It’s not the party of the year, but it’s a good night.")
        }
    }

    scenario("sch_weekend_job", SCHOOL, STREET, 15..18, weight = 9) {
        text("The owner of a local shop offers you a weekend job. The pay is small but real, and you’re not sure how much time you’d have left for school and friends.")
        choice("Take the job.") {
            money(+8); discipline(+4); energy(-8); knowledge(-1); trait(RESPONSIBLE, 3); flag("early_worker")
            result("You learn how to be on time, deal with customers and count change. You also learn that weekends go fast.")
        }
        choice("Decline and concentrate on school.") {
            knowledge(+3); happiness(+1); money(-1)
            result("Your grades improve a bit. Your wallet stays light.")
        }
        choice("Offer to volunteer instead.") {
            reputation(+4); trait(COMPASSIONATE, 2); happiness(+2); count("kindness")
            result("The community centre is delighted to have you. It’s not paid, but people remember you.")
        }
        choice("Start your own small service, like washing cars.") {
            money(+5); confidence(+4); trait(AMBITIOUS, 3); trait(RISK_TAKER, 1); flag("early_entrepreneur")
            result("You make flyers and knock on doors. The first customer is your neighbour, and the second is her friend.")
        }
    }

    scenario("sch_found_wallet", SCHOOL, STREET, 14..18, weight = 8) {
        text("On the way home you find a wallet on the pavement. There’s cash inside, and an ID with an address two streets away.")
        choice("Return it to the owner.") {
            trait(RESPONSIBLE, 3); reputation(+4); happiness(+3); money(-1); count("kindness"); flag("returned_wallet")
            result("The owner is incredibly grateful and insists on shaking your hand. You walk home feeling ten feet tall.")
        }
        choice("Keep the cash and toss the wallet.") {
            money(+5); trait(DISHONEST, 6); happiness(-2); flag("took_shortcut"); count("mistakes")
            result("You buy snacks and a game. The spending doesn’t feel as satisfying as you’d hoped.")
        }
        choice("Hand it in to the police station.") {
            trait(RESPONSIBLE, 2); reputation(+2); knowledge(+1)
            result("The officer thanks you and takes your name. A good deed, with paperwork.")
        }
        choice("Leave it. It’s not your problem.") {
            discipline(+1); count("risk_avoided")
            result("You walk on. Somewhere, someone is still looking for it.")
        }
    }

    scenario("sch_club_choice", SCHOOL, HALLWAY, 15..17, weight = 14) {
        text("Club sign-up day. You can only commit to one this year, and the choice might shape what you’re good at later.")
        choice("Join the robotics and coding club.") {
            knowledge(+5); discipline(+2); confidence(+2); flag("interest_tech")
            result("Your first robot drives straight into a wall. By the end of term, it dodges them.")
        }
        choice("Join the art and design club.") {
            knowledge(+2); confidence(+3); happiness(+4); flag("interest_art")
            result("You discover you can make ideas visible. Everyone’s a critic, but you’re growing a thick skin.")
        }
        choice("Become a peer tutor and join the debate team.") {
            knowledge(+3); confidence(+4); reputation(+3); trait(COMPASSIONATE, 1); flag("interest_people")
            result("You find that you enjoy helping others understand things, and winning arguments politely.")
        }
        choice("Take the woodwork and auto-shop workshop.") {
            health(+2); discipline(+3); confidence(+3); flag("interest_build")
            result("You learn to measure twice and cut once. It’s satisfying to hold something you built.")
        }
    }

    scenario("sch_mentor_teacher", SCHOOL, CLASSROOM, 15..18, weight = 9) {
        text("Your teacher {mentor} stays after class. “You have more potential than your grades show,” {mentor.he} says. “I’d be glad to give you extra guidance, if you want it.”")
        choice("Gratefully accept.") {
            knowledge(+5); confidence(+4); discipline(+3); trust(MENTOR, +25); flag("has_mentor"); hint()
            milestone("Found a teacher who believed in you")
            result("Weekly sessions begin. {mentor} challenges you in ways you didn’t expect, and you grow into them.")
        }
        choice("Politely decline. You’ve got it covered.") {
            confidence(+1); trust(MENTOR, -5); trait(AMBITIOUS, 1)
            result("{mentor} nods. “The door stays open.”")
        }
        choice("Ask for advice, but not regular lessons.") {
            knowledge(+2); trust(MENTOR, +8)
            result("You take a few tips and run with them. It’s a small step but a useful one.")
        }
        choice("Suspect it’s a trick.") {
            trust(MENTOR, -10); confidence(-1); trait(DISHONEST, 1)
            result("You ignore the offer. {mentor} seems a bit hurt but doesn’t push.")
        }
    }

    scenario("sch_new_teen_move", SCHOOL, STREET, 15..17, weight = 6) {
        text("A younger student keeps following you after school, asking about your hobbies. {friend} thinks it’s hilarious. You’re not sure what to do.")
        choice("Take the kid under your wing.") {
            reputation(+2); trait(COMPASSIONATE, 2); happiness(+3); count("kindness"); friendship(+2)
            result("You show the kid the ropes. Within weeks they’re confident enough to introduce themselves to others.")
        }
        choice("Be polite, but keep your distance.") {
            discipline(+1)
            result("You’re friendly without being close. It’s awkward but fine.")
        }
        choice("Tease them like {friend} does.") {
            reputation(-3); trust(BEST_FRIEND, +2); trait(COMPASSIONATE, -2); count("mistakes")
            result("It gets a laugh. It also gets you a look from a teacher who’s been watching.")
        }
        choice("Introduce them to people who share their hobby.") {
            reputation(+3); friendship(+3); trait(RESPONSIBLE, 1)
            result("You solve it in five minutes. They’re not alone anymore.")
        }
    }
}
