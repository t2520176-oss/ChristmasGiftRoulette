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

/**
 * The shape of a life: graduation, the first career, turning thirty, midlife, retirement, later life.
 * The timeline is NOT fixed: each milestone is offered once the player reaches its age, and what
 * comes between depends on stats, relationships and past decisions.
 */
val MilestoneStories = storyPack("milestones", "Life Milestones") {

    scenario("ms_graduation", MILESTONE, CAMPUS, 17..19, priority = 100, title = "Graduation") {
        text("Graduation day. The caps are in the air, the photographs are taken, and the future suddenly feels very close. A whole chapter of your life is closing, and another is about to begin.")
        extra(has("helped_bullied_friend"), "{friend} finds you in the crowd, and gives you a hug that lasts a little longer than usual.")
        choice("Celebrate with your family.") {
            family(+6); happiness(+5); then("ms_next_step")
            result("Your parents are prouder than they know how to say. Your mother cries, and so does your father, quietly.")
        }
        choice("Celebrate with your friends.") {
            friendship(+6); happiness(+7); trust(BEST_FRIEND, +4); then("ms_next_step")
            result("A night you’ll remember forever, and a hundred photographs you’ll look at in decades.")
        }
        choice("Spend the evening reflecting on what’s next.") {
            knowledge(+2); discipline(+2); confidence(+3); then("ms_next_step")
            result("You sit on the empty bleachers as the sun sets, and for the first time you really think about who you want to become.")
        }
        choice("Thank the teacher who believed in you.") {
            showIf(has("has_mentor"))
            badge("Because {mentor} believed in you")
            trust(MENTOR, +12); happiness(+5); knowledge(+2); confidence(+3); then("ms_next_step")
            result("{mentor} waves off the thanks, but you can see {mentor.he} is moved. “Go and do something good,” {mentor.he} says.")
        }
    }

    scenario("ms_next_step", MILESTONE, CAMPUS, 17..21, branch = true, title = "What Next?") {
        text("The doors are open in every direction, and not all of them stay open. This is the first big decision of your life, and there is no single right answer.")
        choice("Go to university.") {
            education(Education.UNIVERSITY); money(-8); knowledge(+8); discipline(+2); trait(AMBITIOUS, 2)
            milestone("Chose university"); then("ms_uni_years")
            result("A big decision, a bigger campus, and a lot of coffee ahead of you.")
        }
        choice("Learn a trade or technical skill.") {
            education(Education.TRADE_SCHOOL); money(-3); knowledge(+4); discipline(+4); trait(RESPONSIBLE, 2)
            milestone("Chose a hands-on trade"); then("ms_trade_years")
            result("You pick tools over textbooks, or at least alongside them. You know where you’re headed.")
        }
        choice("Start working right away.") {
            job(EMPLOYEE, 1); money(+5); discipline(+3); trait(RESPONSIBLE, 2)
            milestone("Started working right after school"); then("ms_work_start")
            result("A real job, a real paycheque, and a real alarm clock.")
        }
        choice("Take a gap year to find out who you are.") {
            happiness(+4); money(-4); confidence(+4); trait(RISK_TAKER, 2); count("risk_taken")
            then("ms_gap_year")
            result("Everyone has an opinion about it. You nod, and book the first ticket.")
        }
    }

    scenario("ms_uni_years", MILESTONE, CAMPUS, 17..24, branch = true, months = 42, title = "University Years") {
        text("The next few years are a blur of lecture halls, deadlines, late-night pizza and big ideas. How you spend them will shape what you’re able to do later.")
        choice("Study seriously and aim high.") {
            knowledge(+10); discipline(+5); money(-3); happiness(-2); trait(DISCIPLINED, 3); milestone("Graduated from university")
            result("The late nights are worth it. You graduate with strong results and a solid grounding.")
        }
        choice("Balance study with a part-time job.") {
            knowledge(+6); money(+3); discipline(+3); energy(-6); trait(RESPONSIBLE, 2); milestone("Graduated from university")
            result("You learn how to manage time, and money. It’s not easy, and it’s very useful.")
        }
        choice("Enjoy campus life and friendships.") {
            friendship(+8); happiness(+7); knowledge(+3); discipline(-2); trust(BEST_FRIEND, +5); milestone("Graduated from university")
            result("You collect friends, stories and an average grade. The people you meet matter more than you know.")
        }
        choice("Start a project or club on the side.") {
            knowledge(+5); confidence(+6); reputation(+5); trait(AMBITIOUS, 2); flag("interest_tech"); milestone("Graduated from university")
            result("The project doesn’t change the world, but it changes you, and the people on it become lifelong allies.")
        }
    }

    scenario("ms_trade_years", MILESTONE, WORKSHOP, 17..24, branch = true, months = 30, title = "Learning a Trade") {
        text("You spend long days with your hands busy and your head full. The people who teach you are tough, practical and generous with what they know.")
        choice("Master the craft, step by step.") {
            knowledge(+5); discipline(+6); confidence(+4); trait(DISCIPLINED, 3); milestone("Completed a trade apprenticeship")
            result("You’re the one the instructors point to when new students ask what “good” looks like.")
        }
        choice("Learn the business side as well.") {
            knowledge(+5); money(+2); trait(AMBITIOUS, 2); flag("business_sense"); milestone("Completed a trade apprenticeship")
            result("You ask about invoices and customers while others ask about tools. It sets you apart.")
        }
        choice("Help the struggling students.") {
            reputation(+5); friendship(+5); trait(COMPASSIONATE, 3); count("kindness"); milestone("Completed a trade apprenticeship")
            result("You become the person people come to for help. It makes you better, too.")
        }
        choice("Take extra shifts to earn money.") {
            money(+6); energy(-8); discipline(+3); milestone("Completed a trade apprenticeship")
            result("You finish with experience, a little savings and tired hands.")
        }
    }

    scenario("ms_work_start", MILESTONE, OFFICE, 17..24, branch = true, months = 18, title = "First Job") {
        text("Work is nothing like school: no syllabus, no clear rules, and the lessons arrive without warning. You’re on your own, and getting the hang of it.")
        choice("Learn everything you can.") {
            knowledge(+6); career(+5); discipline(+3); energy(-6); trait(AMBITIOUS, 1)
            result("You ask questions, volunteer for new tasks and become useful quickly.")
        }
        choice("Save from every paycheque.") {
            money(+7); discipline(+4); trait(DISCIPLINED, 2); flag("saved_money")
            result("You live simply. After a year you’re stunned by how much has accumulated.")
        }
        choice("Enjoy your new independence.") {
            happiness(+7); friendship(+4); money(-5); trait(IMPULSIVE, 1)
            result("Your own money, your own time. It’s thrilling, and the budget agrees to disagree.")
        }
        choice("Take evening courses to get ahead.") {
            knowledge(+7); career(+3); energy(-10); trait(DISCIPLINED, 2); trait(AMBITIOUS, 2)
            result("Two jobs, really. A year later you’re ready for a step up.")
        }
    }

    scenario("ms_gap_year", MILESTONE, AIRPORT, 17..22, branch = true, months = 12, title = "Gap Year") {
        text("A year to yourself, with no one telling you what to do. It’s a rare thing, and you’re determined not to waste it.")
        choice("Travel the world on a shoestring.") {
            happiness(+8); confidence(+6); knowledge(+3); money(-6); trait(RISK_TAKER, 3); flag("adventure", "went_abroad"); count("risk_taken")
            milestone("Spent a year travelling")
            result("Hostels, trains, strangers and sunsets. You return older than a year ought to allow.")
        }
        choice("Volunteer abroad.") {
            reputation(+5); happiness(+6); knowledge(+3); trait(COMPASSIONATE, 4); flag("adventure"); count("kindness")
            milestone("Volunteered abroad for a year")
            result("The work is hard and the people are generous. You’ll never see a map the same way again.")
        }
        choice("Work and save.") {
            money(+10); discipline(+4); trait(RESPONSIBLE, 3); flag("saved_money"); education(Education.HIGH_SCHOOL)
            result("A boring, brilliant, practical year. You return with real savings and a plan.")
        }
        choice("Study on your own, online.") {
            knowledge(+8); discipline(+5); confidence(+3); education(Education.SELF_TAUGHT); trait(DISCIPLINED, 2)
            milestone("Taught yourself a new skill")
            result("A strange way to spend a year, and an excellent one. By the end you can do things you couldn’t imagine.")
        }
    }

    scenario("ms_career_univ", MILESTONE, OFFICE, 21..28, priority = 90, title = "Choosing a Career") {
        requires(educated(Education.UNIVERSITY), unemployed, businessAtMost(0))
        text("Your education is behind you, and the world is asking: “So, what do you do?” Your studies, your interests and your results all point somewhere. It’s time to pick a direction.")
        choice("Engineering.") {
            job(ENGINEER, 1); knowledge(+3); money(+4); confidence(+3); milestone("Began a career in engineering"); then("car_first_day")
            result("You join a small engineering team. The problems are real, and so are the deadlines.")
        }
        choice("Technology.") {
            job(TECH, 1); knowledge(+4); money(+5); confidence(+3); milestone("Began a career in technology"); then("car_first_day")
            result("You start as a junior developer. Everything is evolving, and so are you.")
        }
        choice("Teaching.") {
            job(TEACHER, 1); reputation(+4); happiness(+5); trait(COMPASSIONATE, 3); money(+1); milestone("Began a career as a teacher"); then("car_first_day")
            result("The pay isn’t spectacular, but when a student finally understands, you’d do it for free.")
        }
        choice("Medicine.") {
            enableIf("Medical training needs stronger studies. Build up your knowledge first.", atLeast(Stat.KNOWLEDGE, 66))
            job(DOCTOR, 1); knowledge(+5); reputation(+6); energy(-8); money(+2); trait(RESPONSIBLE, 3); milestone("Began a career in medicine"); then("car_first_day")
            result("Long hours, enormous responsibility and a sense of purpose that doesn’t go away.")
        }
        choice("Design.") {
            showIf(has("interest_art"))
            badge("Because you found your creative side at school")
            job(DESIGNER, 1); happiness(+5); confidence(+4); money(+2); milestone("Began a career in design"); then("car_first_day")
            result("You turn what you’ve always loved into a career. Every project feels like a small gift.")
        }
    }

    scenario("ms_career_trade", MILESTONE, WORKSHOP, 20..29, priority = 90, title = "Choosing a Career") {
        requires(educated(Education.TRADE_SCHOOL), unemployed, businessAtMost(0))
        text("The apprenticeship is done, and you know your craft. Now it’s about where to apply it: an employer’s workshop, a tech role, your own business or a steady, safe job.")
        choice("Work as a skilled tradesperson.") {
            job(SKILLED_WORKER, 2); money(+5); confidence(+4); discipline(+2); milestone("Began a career as a skilled worker"); then("car_first_day")
            result("You’re good at what you do, and people can see it.")
        }
        choice("Move into technology.") {
            enableIf("You’d need stronger technical knowledge for this.", atLeast(Stat.KNOWLEDGE, 45))
            job(TECH, 1); knowledge(+3); money(+3); milestone("Began a career in technology"); then("car_first_day")
            result("Your practical mindset is a rare asset in a world of theory.")
        }
        choice("Start your own small workshop.") {
            business(1); job(ENTREPRENEUR, 1); flag("started_business", "took_business_risk"); money(-6); confidence(+5); trait(RISK_TAKER, 3)
            count("risk_taken"); milestone("Opened your own workshop"); schedule("biz_first_year", 1, 2)
            result("Your name on the sign. Your tools on the wall. And an empty order book.")
        }
        choice("Take a stable job at a large company.") {
            job(EMPLOYEE, 1); money(+4); discipline(+2); count("risk_avoided"); milestone("Began a career as an employee"); then("car_first_day")
            result("A steady paycheque and a safe path. It may not be glamorous, but it’s a solid start.")
        }
    }

    scenario("ms_career_other", MILESTONE, OFFICE, 20..30, priority = 88, title = "Choosing a Career") {
        requires(educated(Education.HIGH_SCHOOL, Education.SELF_TAUGHT), unemployed, businessAtMost(0))
        text("You’re working things out without a degree on the wall, but with plenty of energy. Plenty of people have made it the unconventional way. What will you build?")
        choice("Join a company as an employee.") {
            job(EMPLOYEE, 1); money(+4); discipline(+2); milestone("Began a career as an employee"); then("car_first_day")
            result("You start at the bottom, and promise yourself you won’t stay there.")
        }
        choice("Become a skilled tradesperson.") {
            job(SKILLED_WORKER, 1); money(+3); discipline(+3); confidence(+3); milestone("Began a career as a skilled worker"); then("car_first_day")
            result("An apprenticeship at a local workshop. You get paid to learn, and you’re learning fast.")
        }
        choice("Build a creative career.") {
            job(CREATOR, 1); happiness(+5); confidence(+4); money(-1); trait(RISK_TAKER, 2); flag("creator_path"); count("risk_taken")
            milestone("Began a career as a creator"); then("car_first_day")
            result("Videos, designs, music, words. You post everything, and learn from every comment.")
        }
        choice("Go into technology.") {
            enableIf("You’d need stronger technical skills first.", atLeast(Stat.KNOWLEDGE, 52))
            job(TECH, 1); knowledge(+4); money(+3); milestone("Began a career in technology"); then("car_first_day")
            result("Self-taught, determined, stubborn. You land a junior role, and prove yourself in weeks.")
        }
        choice("Start something of your own.") {
            business(1); job(ENTREPRENEUR, 1); flag("started_business", "took_business_risk"); money(-5); confidence(+5); trait(RISK_TAKER, 3)
            count("risk_taken"); milestone("Started your first business"); schedule("biz_first_year", 1, 2)
            result("There’s no degree for what you’re about to do. You figure it out as you go.")
        }
    }

    scenario("ms_job_search", MILESTONE, OFFICE, 23..40, priority = 80, title = "Between Jobs") {
        requires(unemployed, businessAtMost(0), lacks("sold_business"))
        text("You’ve been drifting between short projects, odd jobs and good intentions. A friend gently says it’s time to find something with a future.")
        choice("Apply for an office job.") {
            job(EMPLOYEE, 1); money(+3); discipline(+3); milestone("Found steady work"); then("car_first_day")
            result("An application, an interview, a nervous handshake. You’re hired.")
        }
        choice("Learn a trade.") {
            job(SKILLED_WORKER, 1); money(+2); discipline(+4); knowledge(+3); milestone("Found a trade"); then("car_first_day")
            result("A local master takes you on as an apprentice. You have rediscovered a sense of purpose.")
        }
        choice("Retrain in technology.") {
            enableIf("You’d need stronger skills first.", atLeast(Stat.KNOWLEDGE, 45))
            job(TECH, 1); knowledge(+5); energy(-6); milestone("Retrained in technology"); then("car_first_day")
            result("Months of evening study pay off with an entry-level job.")
        }
        choice("Build a freelance creative career.") {
            job(CREATOR, 1); happiness(+3); confidence(+3); trait(RISK_TAKER, 1); flag("creator_path")
            milestone("Began a freelance creative career"); then("car_first_day")
            result("It’s unpredictable, and it’s yours.")
        }
    }

    scenario("ms_management_path", MILESTONE, MEETING, 30..50, weight = 12, title = "A Fork in the Road") {
        requires(employed, careerLevel(min = 3), careerIs(EMPLOYEE, ENGINEER, TECH, DESIGNER, SKILLED_WORKER), atLeast(Stat.CAREER, 40))
        text("You’re offered the chance to move into management: fewer hands-on tasks, more meetings, more responsibility, more pay. Some of your colleagues say you’d be great, and a few say you’d hate it.")
        choice("Take the management role.") {
            job(MANAGER, 3); money(+8); career(+6); reputation(+3); energy(-6); trait(AMBITIOUS, 2); milestone("Moved into management")
            result("Your calendar fills up with other people’s problems. You’re good at it, and it’s tiring.")
        }
        choice("Stay hands-on and keep growing as an expert.") {
            knowledge(+5); happiness(+4); career(+3); trait(DISCIPLINED, 1)
            result("You become the person everyone asks when things go wrong. It’s the work you love.")
        }
        choice("Ask for a trial period.") {
            career(+3); knowledge(+2); trait(RESPONSIBLE, 2)
            outcome(60) { bonus(atLeast(Stat.DISCIPLINE, 55), 12); job(MANAGER, 3); money(+6); career(+5); milestone("Moved into management"); result("The trial goes well, and they make it permanent.") }
            outcome(40) { happiness(+2); result("You find out management isn’t for you. It’s better to know.") }
        }
        choice("Decline politely.") {
            happiness(+2); career(-2); count("risk_avoided")
            result("You stay where you’re comfortable. Someone else takes the role.")
        }
    }

    scenario("ms_decade_review", MILESTONE, SKYLINE, 29..33, priority = 70, title = "Turning Thirty") {
        text("Your thirtieth birthday. A modest party, a few cards, and a long walk home. You find yourself taking stock of the years behind you, and the ones ahead.")
        extra(has("worked_too_much"), "You’ve worked hard, perhaps harder than was good for you.")
        extra(has("helped_bullied_friend"), "You think of the friend you stood up for years ago, and the person it made you.")
        extra(atMost(Stat.FRIENDSHIP, 40), "Some old friends are further away than you’d like.")
        extra(atLeast(Stat.MONEY, 65), "Financially, you’re doing better than you expected.")
        choice("Make your career the priority for the next ten years.") {
            career(+5); money(+4); trait(AMBITIOUS, 3); family(-2); flag("priority_career")
            result("You set goals and a deadline. The next decade will be about building something.")
        }
        choice("Put family and relationships first.") {
            family(+7); happiness(+5); trait(LOYAL, 2); flag("family_first")
            result("You promise yourself that the people in your life will always come before the calendar.")
        }
        choice("Rebuild your health and friendships.") {
            health(+6); friendship(+6); happiness(+4); energy(+5)
            result("Gym, hiking, long dinners. You catch up with the old crowd, and feel like yourself again.")
        }
        choice("Take a bold risk and reinvent yourself.") {
            confidence(+5); trait(RISK_TAKER, 3); happiness(+2); count("risk_taken"); schedule("biz_idea", 0, 1)
            result("You write down the thing you’ve been afraid to try, and put it on the fridge.")
        }
    }

    scenario("ms_midlife_review", MILESTONE, SKYLINE, 48..53, priority = 70, title = "Halfway Home") {
        text("You’ve passed the middle of your life, and the view from here is different. You can see further back than you can see ahead. Everything you have chosen has left a mark.")
        extra(has("ignored_parents"), "There are conversations you wish you had had sooner.")
        extra(atLeast(Stat.FAMILY, 75), "The people you love are close, and it shows.")
        extra(has("business_failed"), "You think about the business that failed, and, oddly, you’re grateful for it.")
        extra(has("supported_family"), "Your family’s gratitude is something you’ve never needed to ask for.")
        choice("Mend what’s broken in your relationships.") {
            family(+6); friendship(+6); happiness(+5); trait(COMPASSIONATE, 2)
            onlyIf(atMost(Stat.FAMILY, 55)) { count("redemptions"); note("It isn’t too late, you realise, and you start making calls.") }
            result("You write letters, make calls, and knock on doors. Not every conversation is easy. Every one is worth it.")
        }
        choice("Invest in your legacy by mentoring someone.") {
            reputation(+6); happiness(+5); trait(COMPASSIONATE, 3); flag("mentored"); count("kindness"); milestone("Became a mentor")
            result("You find a young person who needs what you know. Both of you learn from it.")
        }
        choice("Follow a long-delayed dream.") {
            happiness(+8); confidence(+5); money(-4); energy(-4); trait(RISK_TAKER, 2); flag("adventure")
            result("Whether it’s painting, travelling, building or writing, you stop postponing it.")
        }
        choice("Keep the course. It’s working.") {
            career(+3); money(+4); happiness(-1)
            result("A few adjustments, mostly more of the same. You’re comfortable, and a little restless.")
        }
    }

    scenario("ms_retirement", MILESTONE, PARK, 62..69, priority = 90, title = "Retirement") {
        text("The day is approaching when you’ll no longer have to set an alarm. People have a thousand opinions about what you should do with the time. You’ve got one decision to make.")
        choice("Retire fully and enjoy it.") {
            flag("retired"); happiness(+8); energy(+10); health(+3); career(-5); money(-2)
            milestone("Retired")
            result("You say your goodbyes. The first morning, you wake early out of habit, then realise you don’t have to, and smile.")
        }
        choice("Work part-time for a while.") {
            money(+3); happiness(+4); energy(-2); career(+1); flag("retired")
            milestone("Moved into a gentler working life")
            result("Two days a week of the work you actually like. The best of both.")
        }
        choice("Keep working. You love it.") {
            career(+3); happiness(+2); health(-3); energy(-5)
            result("Retirement is a word you’ll consider another day.")
        }
        choice("Retire, and devote yourself to something meaningful.") {
            flag("retired"); reputation(+7); happiness(+8); trait(COMPASSIONATE, 3); count("kindness"); milestone("Retired, and gave your time to others")
            result("Tutoring, volunteering, gardening for the neighbourhood. You’re busier than ever, and happier.")
        }
    }

    // ---------- Later life ----------

    scenario("lat_grandchildren", LATER_LIFE, LIVING_ROOM, 58..88, weight = 12, title = "Grandchildren") {
        requires(hasChildren)
        text("Your grandchildren come to stay for the holidays. The house fills with noise, crayons and questions. They want to know everything about you.")
        choice("Spend every day with them.") {
            happiness(+9); family(+7); energy(-8); health(+1)
            result("Fishing, stories and baking disasters. They’ll remember these weeks all their lives.")
        }
        choice("Help them financially, and with your wisdom.") {
            family(+5); money(-5); reputation(+3); trait(RESPONSIBLE, 1)
            result("You pay for lessons, tell them your mistakes and let them make their own.")
        }
        choice("Tell them the story of your life.") {
            family(+6); happiness(+6); reputation(+4); flag("mentored")
            result("They listen wide-eyed. When you finish, the oldest says: “Can you tell it again?”")
        }
        choice("Keep a loving distance. They have their own parents.") {
            family(+1); energy(+4)
            result("You’re there when they need you, and out of the way when they don’t.")
        }
    }

    scenario("lat_old_friends", LATER_LIFE, PARK, 60..88, weight = 11, title = "Old Friends") {
        extra(trustAtLeast(BEST_FRIEND, 65), "{friend}, your oldest friend, is among them, grey, wrinkled and still grinning that same grin.")
        text("The old group is planning one last big get-together. People are travelling from all over the country to be there.")
        choice("Go, and stay the whole week.") {
            friendship(+9); happiness(+9); energy(-6); money(-3); trust(BEST_FRIEND, +10)
            result("Seven days of old stories and new ones. Everyone says the same thing: “We should do this every year.”")
        }
        choice("Join for a day.") {
            friendship(+4); happiness(+4); energy(-3)
            result("A short, sweet visit. It’s enough to make you smile for days.")
        }
        choice("Stay home. You’re comfortable.") {
            friendship(-5); happiness(-3); energy(+3)
            result("You watch the photos online. You miss it more than you expected.")
        }
        choice("Host it at your home.") {
            friendship(+8); happiness(+8); energy(-10); money(-4); reputation(+3); trust(BEST_FRIEND, +8)
            result("The house is full again. You fall asleep that night, exhausted and radiant.")
        }
    }

    scenario("lat_health_routine", LATER_LIFE, PARK, 62..88, weight = 10, title = "Staying Well") {
        text("Your doctor has a simple message: movement, good food, company and sleep make a huge difference at your age. You can follow the advice, or wing it.")
        choice("Follow the routine faithfully.") {
            health(+8); energy(+8); happiness(+3); discipline(+3)
            result("A daily walk, simple meals, and a regular bedtime. Your doctor is thrilled.")
        }
        choice("Join a group exercise class.") {
            health(+6); friendship(+6); happiness(+5)
            result("Tai chi, swimming, chair yoga. You make friends faster than you gain flexibility.")
        }
        choice("Do the minimum.") {
            health(+1)
            result("A little here and a little there. You’re doing okay.")
        }
        choice("Ignore it. You’ve survived this long.") {
            health(-6); energy(-4); happiness(-1)
            result("You aren’t surprised when the next check-up isn’t great.")
        }
    }

    scenario("lat_memoir", LATER_LIFE, LIVING_ROOM, 64..90, weight = 9, title = "A Story Worth Telling") {
        text("A friend suggests you write down the story of your life, for yourself, for your family, and for anyone who might learn from it. It’s a bigger task than you thought.")
        choice("Write it, chapter by chapter.") {
            happiness(+6); knowledge(+2); reputation(+4); family(+4); flag("mentored")
            result("Memories you’d forgotten surface. When you finish, you have a stack of pages and a new understanding of your life.")
        }
        choice("Record stories for your family instead.") {
            family(+7); happiness(+5)
            result("You sit with your grandchildren, a microphone and a pot of tea. It’s an irreplaceable gift.")
        }
        choice("Teach what you know in a local class.") {
            reputation(+6); happiness(+5); friendship(+3); flag("mentored"); count("kindness")
            result("The classroom is full. You realise you never stopped being a teacher.")
        }
        choice("Leave it. Some stories are best untold.") {
            happiness(-1)
            result("A quiet decision. It stays with you.")
        }
    }

    scenario("lat_new_hobby", LATER_LIFE, PARK, 62..88, weight = 8, title = "Something New") {
        text("You’ve always wanted to try something new: painting, gardening, a language, an instrument. There’s time now, and nobody to stop you.")
        choice("Take it up with enthusiasm.") {
            happiness(+8); knowledge(+3); confidence(+4); energy(-2)
            result("You’re terrible at first, then merely bad, then, one afternoon, quite good. It’s the best feeling.")
        }
        choice("Try it with a friend.") {
            happiness(+7); friendship(+6); knowledge(+2)
            result("The company matters more than the hobby, though the hobby is a good excuse.")
        }
        choice("Think about it a little longer.") {
            happiness(-1)
            result("The weeks drift, and the idea stays on the shelf.")
        }
        choice("Teach a young person what you already know.") {
            reputation(+5); happiness(+6); flag("mentored"); count("kindness")
            result("You find a young learner who is delighted, and, to your surprise, so are you.")
        }
    }

    scenario("lat_loss", LATER_LIFE, LIVING_ROOM, 66..90, weight = 8, title = "Saying Goodbye") {
        text("A close friend of many years passes away. The funeral is full of people who loved them, and stories that make you laugh and cry at once.")
        extra(trustAtLeast(BEST_FRIEND, 75), "It’s someone from the old days. You were there from the very start.")
        choice("Speak at the service.") {
            happiness(+2); reputation(+3); friendship(+3); health(-1)
            result("Your voice shakes, but the words come. People tell you afterwards what it meant to them.")
        }
        choice("Spend time with their family in the weeks after.") {
            family(+3); friendship(+4); happiness(+2); trait(COMPASSIONATE, 2); count("kindness")
            result("Your friend’s family leans on you. You lean on them too, and everyone is a little less alone.")
        }
        choice("Withdraw for a while.") {
            happiness(-5); energy(-3); friendship(-2)
            result("Grief needs time. You take it, and it takes you.")
        }
        choice("Celebrate their life with a gathering.") {
            friendship(+6); happiness(+4); money(-2)
            result("Food, photographs, songs. It’s a wake that feels like a party.")
        }
    }

    scenario("ms_final_reflection", LATER_LIFE, PARK, 68..95, priority = 100, title = "Looking Back") {
        requires(nearEnd)
        text("You sit on a bench in the late afternoon light, and look back across the years: the decisions that felt small, and the ones that felt huge. Some brought joy, some brought pain, and every one built the life you’ve lived.")
        extra(has("helped_bullied_friend"), "A boy or girl afraid to go to school, a friend you chose to stand beside.")
        extra(has("business_failed"), "The business that failed, and the person you became afterwards.")
        extra(has("family_protected"), "The hard years when you held your family together.")
        extra(atMost(Stat.FAMILY, 40), "The distance that grew between you and some of the people you love.")
        choice("“I’d do it all again.”") {
            happiness(+8); confidence(+4)
            result("You smile. You have made your peace.")
        }
        choice("Share what you’ve learned with the young.") {
            reputation(+6); happiness(+5); flag("mentored"); count("kindness")
            result("The young listen, or pretend to. One of them will carry your words for the rest of their life.")
        }
        choice("Make peace with what’s left unsaid.") {
            happiness(+4); family(+5); friendship(+4)
            result("A few calls and a few letters. The weight you didn’t know you carried gets lighter.")
        }
        choice("Spend the days that remain with the people you love.") {
            family(+7); friendship(+5); happiness(+6)
            result("Meals, walks, laughter. The days are simple, and they’re enough.")
        }
    }
}
