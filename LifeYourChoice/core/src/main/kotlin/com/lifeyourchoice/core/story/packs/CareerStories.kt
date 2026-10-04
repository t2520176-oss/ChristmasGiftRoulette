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

/** Work life: bosses, promotions, overtime, ethics, changing jobs and leading others. */
val CareerStories = storyPack("career", "Career") {

    scenario("car_first_day", CAREER, OFFICE, 18..40, title = "First Job", branch = true, months = 4) {
        text("It’s your first day. Everyone seems to know exactly what they’re doing, and you feel like the only person without a map. Your boss, {boss}, hands you a stack of unfamiliar tasks.")
        choice("Ask questions, even the obvious ones.") {
            knowledge(+4); confidence(+3); reputation(+2); trait(RESPONSIBLE, 2)
            result("Nobody laughs. Two colleagues quietly tell you they were just as lost at the start.")
        }
        choice("Keep your head down and figure it out alone.") {
            knowledge(+2); discipline(+3); confidence(-2); energy(-6)
            result("It takes longer, and one or two mistakes slip through, but you learn deeply.")
        }
        choice("Work late to prove yourself.") {
            career(+5); energy(-12); health(-2); trait(AMBITIOUS, 2); count("overwork")
            result("{boss} notices the extra hours. So does your body.")
        }
        choice("Find {coworker} and ask for a quick tour.") {
            friendship(+4); trust(COWORKER, +12); knowledge(+3); confidence(+2)
            result("{coworker} shows you the shortcuts and the best coffee machine. You feel like part of the team already.")
        }
    }

    scenario("car_overtime", CAREER, OFFICE, 22..50, weight = 14) {
        requires(employed)
        text("Your boss asks you to work overtime on a big project. It’s a chance to impress {boss.him}, but you’re already exhausted and haven’t seen your family for days.")
        choice("Accept and work overtime.") {
            money(+8); career(+5); health(-4); family(-3); energy(-14)
            trait(AMBITIOUS, 2); flag("worked_too_much"); count("overwork"); trust(BOSS, +8)
            result("The project is a success, and you’re noticed. You also feel the cost in your sleep and in your home.")
        }
        choice("Politely decline. You need rest.") {
            health(+3); family(+3); energy(+8); career(-3); trait(RESPONSIBLE, 1); count("overwork", -1)
            result("{boss} is disappointed but professional. You’re home for dinner and sleep like a stone.")
        }
        choice("Negotiate a different schedule.") {
            career(+2); family(+1); discipline(+2); confidence(+3); trait(RESPONSIBLE, 2)
            outcome(60) { bonus(atLeast(Stat.REPUTATION, 55), 20); result("{boss} agrees to a half-remote plan. The project gets done, and so do you.") }
            outcome(40) { career(-2); energy(-5); result("The compromise only half works. Your hours are long anyway.") }
        }
        choice("Ask a coworker to help share the load.") {
            career(+2); trust(COWORKER, +6); friendship(+2); energy(-6); trait(RESPONSIBLE, 1)
            result("{coworker} agrees. Together you ship the project, and you owe each other a lunch.")
        }
    }

    scenario("car_bad_boss", CAREER, OFFICE, 23..45, weight = 10) {
        requires(employed)
        text("{boss} presents your work in a meeting as {boss.his} own idea. It was the best thing you’ve done this year, and there was no mention of your name.")
        choice("Confront {boss.him} privately.") {
            confidence(+4); trust(BOSS, -10); reputation(+1); trait(RISK_TAKER, 1)
            outcome(50) { bonus(atLeast(Stat.CONFIDENCE, 55), 15); career(+3); result("{boss} apologises, and makes sure you’re credited next time.") }
            outcome(50) { career(-3); result("{boss} gets defensive. Things become frosty.") }
        }
        choice("Document everything and go to HR.") {
            reputation(+2); trait(RESPONSIBLE, 3); discipline(+3); energy(-5)
            result("It’s slow and awkward, but there’s now a paper trail, and a quiet policy change.")
        }
        choice("Endure it. Don’t make waves.") {
            happiness(-5); confidence(-3); health(-2); count("risk_avoided")
            result("You swallow your frustration. It sits in your stomach for months.")
        }
        choice("Start quietly looking for another job.") {
            happiness(+1); career(+1); trait(AMBITIOUS, 2); knowledge(+1)
            schedule("car_change_jobs", 1, 2)
            result("Updating your résumé feels strangely good. Options exist, and you know it.")
        }
    }

    scenario("car_promotion", CAREER, MEETING, 25..55, weight = 12) {
        requires(employed, careerLevel(max = 2), atLeast(Stat.CAREER, 25))
        boost(atLeast(Stat.DISCIPLINE, 65), 8)
        text("A promotion opens up, and you’re on the shortlist along with {coworker}. The decision comes down to the next two weeks.")
        choice("Ask {boss} directly to consider you.") {
            confidence(+4); trait(AMBITIOUS, 3); trust(BOSS, +2)
            outcome(50) { bonus(atLeast(Stat.CAREER, 55), 20); bonus(atLeast(Stat.REPUTATION, 60), 10); promote(); money(+8); career(+8); milestone("Earned a promotion"); result("You’re promoted. The raise is real, and so is the new pressure.") }
            outcome(50) { career(+2); result("Not this time, but {boss} says you’re “next in line.”") }
        }
        choice("Let your work speak for itself.") {
            discipline(+2); trait(DISCIPLINED, 2)
            outcome(45) { bonus(atLeast(Stat.DISCIPLINE, 65), 25); bonus(has("helped_coworker"), 10); promote(); money(+8); career(+7); milestone("Earned a promotion"); result("Your steady results speak louder than any pitch. The promotion is yours.") }
            outcome(55) { career(+1); result("Quiet work doesn’t always get noticed. {coworker} gets the job.") }
        }
        choice("Support {coworker} and withdraw.") {
            trust(COWORKER, +20); trait(COMPASSIONATE, 3); friendship(+3); reputation(+3); career(-1)
            flag("helped_coworker"); count("kindness"); hint()
            result("{coworker} is stunned. The two of you grow closer, and you’ll be remembered for it.")
        }
        choice("Undermine {coworker}’s chances.") {
            trait(DISHONEST, 7); reputation(-5); trust(COWORKER, -25); flag("took_shortcut"); count("mistakes")
            outcome(55) { bonus(atLeast(Stat.CAREER, 40), 10); promote(); money(+7); career(+5); result("You get the promotion. It was a nasty route, and some people suspect it.") }
            outcome(45) { career(-4); result("It backfires: your tactics come to light and the role goes to someone else.") }
        }
    }

    scenario("car_senior_promotion", CAREER, MEETING, 34..60, weight = 11, title = "Senior Role") {
        requires(employed, careerLevel(min = 3, max = 4), atLeast(Stat.CAREER, 50), lacks("senior_role_offered"))
        boost(atLeast(Stat.REPUTATION, 65), 6)
        text("Years of work have made you one of the most experienced people in the company. A senior position is opening, with real authority and real pressure, and the company wants to know if you’re ready.")
        choice("Step up and take the role.") {
            confidence(+4); trait(AMBITIOUS, 2); flag("senior_role_offered"); count("overwork")
            outcome(60) { bonus(atLeast(Stat.REPUTATION, 60), 15); bonus(atLeast(Stat.DISCIPLINE, 60), 10); promote(); money(+9); career(+8); reputation(+5); milestone("Reached a senior position"); result("You’re confirmed in the role. The authority is real, and so is the weight of it.") }
            outcome(40) { career(+3); money(+3); result("The role goes to an outside hire, and you’re asked to train them. Frustrating, but your value is noted.") }
        }
        choice("Ask to shape the role around how you work best.") {
            confidence(+4); flag("senior_role_offered"); trait(RESPONSIBLE, 2)
            outcome(55) { bonus(atLeast(Stat.REPUTATION, 60), 20); promote(); money(+6); career(+6); happiness(+3); milestone("Reached a senior position"); result("Your terms are accepted, a rare compliment. You grow into the role your own way.") }
            outcome(45) { career(+2); result("They like your ideas, but not the conditions. It’s a respectful stalemate.") }
        }
        choice("Recommend someone from your team and mentor them.") {
            reputation(+6); trust(COWORKER, +10); trait(COMPASSIONATE, 3); flag("senior_role_offered", "mentored"); count("kindness")
            result("Your protégé is promoted, and you become the quiet backbone of the department.")
        }
        choice("Turn it down. You’ve got enough on your plate.") {
            happiness(+3); energy(+5); career(-2); flag("senior_role_offered"); count("risk_avoided")
            result("You keep your evenings, your hobbies and your sanity.")
        }
    }

    scenario("car_coworker_conflict", CAREER, OFFICE, 23..55, weight = 9) {
        requires(employed)
        text("You and {coworker} disagree about how to handle a major project. Both of you are sure you’re right, and the team is quietly choosing sides.")
        choice("Hear {coworker} out and find a middle path.") {
            trust(COWORKER, +10); knowledge(+2); reputation(+3); trait(RESPONSIBLE, 2); confidence(+2)
            result("The combined plan is better than either of the originals. The team notices.")
        }
        choice("Push your plan harder.") {
            confidence(+2); trust(COWORKER, -8); career(+2); trait(IMPULSIVE, 1)
            result("You win the vote. The victory feels hollow when the room goes quiet.")
        }
        choice("Escalate to {boss}.") {
            trust(BOSS, -2); trust(COWORKER, -10); reputation(-1)
            result("{boss} decides, but resents being pulled in. Neither of you is happy.")
        }
        choice("Let {coworker} have it. It’s not worth the stress.") {
            happiness(+1); confidence(-3); trust(COWORKER, +4); count("risk_avoided")
            result("Peace is restored. You’re not sure it was the right call.")
        }
    }

    scenario("car_change_jobs", CAREER, OFFICE, 25..52, weight = 10, title = "Career Crossroads") {
        requires(employed)
        text("A recruiter calls with an offer: a better title and a bigger salary at a competitor, but the culture is unknown and you’d have to start over with a new team.")
        choice("Take the new job.") {
            money(+9); career(+5); confidence(+4); friendship(-3); trait(RISK_TAKER, 2); count("risk_taken")
            promote(); milestone("Switched employers for a better role")
            result("The first month is rough, but the bigger challenge is exactly what you needed.")
        }
        choice("Use the offer to negotiate a raise at your current job.") {
            money(+5); career(+2); trust(BOSS, -5); trait(AMBITIOUS, 1)
            outcome(60) { bonus(atLeast(Stat.REPUTATION, 60), 15); money(+3); result("They match most of it. You stay, with a quiet new leverage.") }
            outcome(40) { reputation(-2); result("They refuse. The mood at work is a little different now.") }
        }
        choice("Stay where you are. Loyalty matters.") {
            happiness(+1); trait(LOYAL, 2); count("risk_avoided"); trust(BOSS, +8)
            result("You stay, with a faint question mark that doesn’t quite go away.")
        }
        choice("Ask {mentor} for advice.") {
            trust(MENTOR, +8); knowledge(+2); trait(RESPONSIBLE, 2)
            result("{mentor} listens carefully, then asks you what you want to be doing in ten years. It clarifies everything.")
        }
    }

    scenario("car_career_vs_family", CAREER, OFFICE, 28..52, weight = 11) {
        requires(employed, anyOf(hasChildren, status(RelationshipStatus.MARRIED)))
        text("The biggest project of your career has a launch event tonight, the same night as a family event you promised to attend. You can’t do both.")
        choice("Go to the launch. The career won’t wait.") {
            career(+8); money(+5); family(-8); happiness(-4); trait(AMBITIOUS, 3); flag("worked_too_much"); count("overwork")
            result("The launch is a triumph. The apology at home is awkward.")
        }
        choice("Go to the family event.") {
            family(+9); happiness(+5); career(-4); trait(RESPONSIBLE, 2); flag("family_first"); hint()
            milestone("Chose family over a career moment")
            result("You miss a big night, but the look on your family’s faces is something you’ll remember much longer.")
        }
        choice("Attend the first hour of each.") {
            career(+2); family(+2); energy(-12); trait(DISCIPLINED, 1)
            result("You run between venues in your best clothes. It’s exhausting and a little ridiculous, but you make it.")
        }
        choice("Ask your team to cover the launch.") {
            family(+5); trust(COWORKER, +4); career(-1); trait(RESPONSIBLE, 2)
            outcome(60) { bonus(trustAtLeast(COWORKER, 60), 25); result("They step up beautifully. The launch goes well without you.") }
            outcome(40) { career(-3); result("The launch is messy without you. You hear about it for weeks.") }
        }
    }

    scenario("car_leadership", CAREER, MEETING, 27..55, weight = 10) {
        requires(employed, careerLevel(min = 2))
        text("You’ve been asked to lead a team of five on a tight deadline. Two of them have more experience than you, and one of them has already questioned your approach.")
        choice("Set clear goals and listen to each person.") {
            reputation(+5); confidence(+4); discipline(+3); knowledge(+2); trait(RESPONSIBLE, 3)
            outcome(65) { bonus(atLeast(Stat.DISCIPLINE, 55), 15); career(+8); money(+4); result("The team delivers on time and proud of it. People ask to be on your next project.") }
            outcome(35) { career(+3); result("It’s tough, but the team holds together and does well enough.") }
        }
        choice("Take control and decide everything.") {
            career(+3); confidence(+1); reputation(-3); energy(-8); happiness(-2)
            result("The deadline is met, but people grumble. You’re drained.")
        }
        choice("Delegate and trust the experienced ones.") {
            career(+4); reputation(+2); trait(RESPONSIBLE, 1); trust(COWORKER, +5)
            result("Trusting people brings out their best. You learn to lead without hovering.")
        }
        choice("Do it all yourself to be safe.") {
            career(+2); energy(-16); health(-3); happiness(-3); count("overwork")
            result("It gets done, at a cost. The team feels sidelined.")
        }
    }

    scenario("car_unethical_order", CAREER, OFFICE, 25..55, weight = 9) {
        requires(employed)
        text("{boss} quietly asks you to adjust some numbers in a report so a client doesn’t see a problem. “Nobody will ever notice,” {boss.he} says. “Everyone does it.”")
        choice("Do it. It’s the boss’s call.") {
            money(+3); career(+3); trait(DISHONEST, 8); reputation(-3); trust(BOSS, +10); flag("took_shortcut"); count("mistakes"); hint()
            result("The report goes out. Nothing happens, and the knot in your stomach stays for weeks.")
        }
        choice("Refuse, politely but firmly.") {
            reputation(+4); confidence(+5); trait(RESPONSIBLE, 3); trait(DISHONEST, -2); trust(BOSS, -12)
            flag("refused_unethical", "protected_reputation"); milestone("Turned down unethical work"); hint()
            outcome(55) { bonus(atLeast(Stat.REPUTATION, 55), 15); result("{boss} backs off, a bit embarrassed. Things stay civil.") }
            outcome(45) { career(-5); money(-3); result("Your next review is conspicuously cooler. It costs you, but you hold your head high.") }
        }
        choice("Report it to compliance anonymously.") {
            reputation(+2); trait(RESPONSIBLE, 3); confidence(+2); count("risk_taken"); flag("refused_unethical")
            result("An inquiry follows. It’s messy, but the numbers get corrected.")
        }
        choice("Ask for time, and quietly prepare an alternative.") {
            discipline(+3); knowledge(+2); trait(RESPONSIBLE, 2); energy(-6)
            result("You present a legitimate version that solves the real problem. {boss} reluctantly accepts it.")
        }
    }

    scenario("car_mentor_meets", CAREER, COFFEE_SHOP, 22..34, weight = 11) {
        requires(employed)
        text("A senior colleague, {mentor}, takes you for coffee. “I see myself in you,” {mentor.he} says. “If you want, I can show you how this place really works.”")
        choice("Say yes, and take notes.") {
            knowledge(+5); career(+5); confidence(+3); trust(MENTOR, +25); flag("has_mentor"); hint()
            milestone("Found a mentor at work")
            result("{mentor} shares hard-earned lessons you won’t find in any handbook.")
        }
        choice("Politely decline.") {
            confidence(+1); trust(MENTOR, -3); trait(AMBITIOUS, 1)
            result("You’d rather find your own path. {mentor} smiles. “Fair enough.”")
        }
        choice("Accept, but ask for honest feedback too.") {
            knowledge(+4); confidence(+4); discipline(+2); trust(MENTOR, +18); flag("has_mentor")
            result("You’re not afraid of hard truths. {mentor} is impressed, and a little relieved.")
        }
        choice("Suspect an ulterior motive.") {
            trust(MENTOR, -8); happiness(-1); trait(DISHONEST, 1)
            result("You keep your distance. It’s hard to say whether that was wise.")
        }
    }

    scenario("car_training", CAREER, OFFICE, 27..52, weight = 9) {
        requires(employed)
        text("Your company offers a training course in a new technology, held after hours. It’s optional, unpaid and tiring, but everyone says it will matter in a few years.")
        choice("Sign up and attend every session.") {
            knowledge(+6); career(+4); energy(-10); discipline(+3); trait(DISCIPLINED, 3); trait(AMBITIOUS, 1)
            result("It’s hard going. Six months later, you’re the one the others ask for help.")
        }
        choice("Skip it. You have a life.") {
            energy(+4); happiness(+2); knowledge(-1); career(-1)
            result("You enjoy your evenings. The new technology arrives anyway.")
        }
        choice("Learn it by yourself, at your own pace.") {
            knowledge(+4); discipline(+4); energy(-6); trait(DISCIPLINED, 2)
            result("Slower, but personal. You learn it by building something you care about.")
        }
        choice("Ask the company to make it part of working hours.") {
            confidence(+3); reputation(+2); trait(RISK_TAKER, 1)
            outcome(50) { bonus(atLeast(Stat.REPUTATION, 60), 20); knowledge(+5); career(+3); result("They agree. Half the department thanks you.") }
            outcome(50) { career(-1); result("Request denied. You’ve made your point, though.") }
        }
    }

    scenario("car_help_coworker", CAREER, OFFICE, 23..52, weight = 11) {
        requires(employed)
        text("{coworker} is drowning in work and looks close to tears. “I’m going to miss the deadline,” {coworker.he} whispers. Your own workload isn’t light either.")
        choice("Stay late to help.") {
            trust(COWORKER, +25); trait(COMPASSIONATE, 3); energy(-10); career(+1); friendship(+3)
            flag("helped_coworker"); count("kindness"); hint()
            result("Together you finish with minutes to spare. {coworker} won’t forget this.")
        }
        choice("Offer a quick tip and keep going.") {
            trust(COWORKER, +8); trait(COMPASSIONATE, 1); energy(-2)
            result("It’s a small thing, but it takes the edge off.")
        }
        choice("Tell {boss}, so the workload is redistributed.") {
            trust(COWORKER, -5); trust(BOSS, +4); trait(RESPONSIBLE, 2); reputation(+1)
            result("The workload is rebalanced. {coworker} isn’t sure whether to thank you or not.")
        }
        choice("Say you have your own deadlines.") {
            career(+2); trust(COWORKER, -8); energy(+1)
            result("You protect your own time. The office feels a bit cooler afterwards.")
        }
    }

    scenario("car_burnout", CAREER, APARTMENT, 28..58, weight = 10, title = "Burnout") {
        requires(employed)
        boost(counterAtLeast("overwork", 2), 12)
        boost(atMost(Stat.ENERGY, 35), 10)
        text("You wake up one morning and can’t make yourself get out of bed. Work looks like a mountain. You’re tired in a way sleep isn’t fixing.")
        choice("Take a proper break and see a doctor.") {
            health(+8); energy(+18); career(-2); happiness(+4); trait(RESPONSIBLE, 2); count("overwork", -3); count("redemptions")
            result("The doctor’s advice is simple and uncomfortable: stop. You do, and slowly you come back to life.")
        }
        choice("Push through. Everyone’s tired.") {
            career(+3); health(-8); energy(-10); happiness(-5); flag("worked_too_much"); count("overwork")
            result("You keep going. Your body keeps score, and it will present you the bill later.")
        }
        choice("Talk to {boss} about your workload.") {
            confidence(+3); energy(+6); career(-1); trait(RESPONSIBLE, 2); count("overwork", -2)
            result("{boss} is more understanding than you expected. Some tasks are taken off your plate.")
        }
        choice("Take a long weekend away with someone you love.") {
            energy(+12); happiness(+7); family(+4); money(-4); health(+3); count("overwork", -1)
            result("Two days of doing very little. You return slower, but clearer.")
        }
    }

    scenario("car_side_project", CAREER, APARTMENT, 22..42, weight = 9) {
        requires(employed)
        text("In the evenings you’ve been tinkering with a personal project: videos, designs, software, music. Friends say it’s actually good. It could turn into something, or stay a hobby.")
        choice("Go all in. Launch it publicly.") {
            confidence(+5); happiness(+5); energy(-12); trait(RISK_TAKER, 2); trait(AMBITIOUS, 2); flag("creator_path"); count("risk_taken")
            outcome(45) { bonus(atLeast(Stat.DISCIPLINE, 60), 15); money(+6); reputation(+6); result("A small audience grows. Within a year, strangers are paying for your work.") }
            outcome(55) { money(-2); result("A few kind comments, and not much else. You’ve learned a lot.") }
        }
        choice("Keep it as a hobby.") {
            happiness(+4); energy(-3); discipline(+1)
            result("It keeps you creative and a little less stressed.")
        }
        choice("Ask {friend} for honest feedback before deciding.") {
            trust(BEST_FRIEND, +5); knowledge(+2); trait(RESPONSIBLE, 1)
            result("{friend}’s notes are brutal and generous. Your next version is much stronger.")
        }
        choice("Drop it. There’s no time for this.") {
            happiness(-3); discipline(+1); count("risk_avoided")
            result("Evenings are quiet. Part of you wonders what that project could have become.")
        }
    }

    scenario("car_award", CAREER, MEETING, 30..58, weight = 7) {
        requires(employed, careerLevel(min = 2))
        text("You’re named “Employee of the Year” in front of the whole company. The reality is that a lot of the success was thanks to the quiet effort of others in your team.")
        choice("Share the credit publicly.") {
            reputation(+8); trust(COWORKER, +10); confidence(+3); trait(COMPASSIONATE, 2); trait(RESPONSIBLE, 1)
            result("Your team beams when you name them. People remember a leader like that.")
        }
        choice("Accept the praise graciously.") {
            reputation(+4); career(+4); confidence(+3)
            result("A warm moment. A few colleagues give you a polite, noncommittal clap.")
        }
        choice("Downplay it and move on.") {
            reputation(+2); confidence(-1); happiness(-1)
            result("You’re uncomfortable with the attention. Your team wishes you’d let them cheer for you.")
        }
        choice("Use it to ask for a raise.") {
            money(+7); career(+3); reputation(-2); trait(AMBITIOUS, 2)
            result("They agree, quietly. A few people mutter about the timing.")
        }
    }
}
