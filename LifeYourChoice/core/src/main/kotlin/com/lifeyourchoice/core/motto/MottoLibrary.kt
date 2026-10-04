package com.lifeyourchoice.core.motto

enum class MottoCategory(val label: String) {
    FAMILY("Family"),
    FRIENDSHIP("Friendship"),
    SUCCESS("Success"),
    SUCCESS_AT_A_COST("Success at a Cost"),
    FAILURE("Failure"),
    COMEBACK("Comeback"),
    MONEY("Money"),
    CAREER("Career"),
    BUSINESS("Business"),
    HEALTH("Health"),
    LOVE("Love"),
    COURAGE("Courage"),
    DISCIPLINE("Discipline"),
    KINDNESS("Kindness"),
    TRUST("Trust"),
    AMBITION("Ambition"),
    RISK("Risk"),
    RISK_WISDOM("Wisdom"),
    HAPPINESS("Happiness"),
    REGRET("Regret"),
    SECOND_CHANCES("Second Chances"),
    BALANCED_LIFE("Balanced Life")
}

class Motto(val id: String, val category: MottoCategory, val text: String)

/** Original lines written for this game. Six per category; ids are stable so history survives updates. */
object MottoLibrary {
    private val lines: Map<MottoCategory, List<String>> = mapOf(
        MottoCategory.FAMILY to listOf(
            "The dinners you showed up for became the memories that carried you.",
            "Home is not where you ended up. It is who you came back to.",
            "A family is built in ordinary hours, not in grand gestures.",
            "What you build matters, but who you build it with matters more.",
            "The people at your table were the real measure of your life.",
            "Love at home is a quiet success that never makes the news."
        ),
        MottoCategory.FRIENDSHIP to listOf(
            "A true friend is a promise you keep long after it is convenient.",
            "You were rich in the one thing no one can invoice: people who stayed.",
            "Friends remembered who you were when nobody was watching.",
            "The hand you reached out years ago became the hand that pulled you up.",
            "Loyalty is a slow gift that always arrives on time.",
            "Some friendships are simply the long way home."
        ),
        MottoCategory.SUCCESS to listOf(
            "Success isn’t never falling. It’s choosing to rise every time you do.",
            "You measured success in more than one currency.",
            "Doors opened because you kept knocking with clean hands.",
            "The finish line looked different from the one you imagined, and better.",
            "You climbed with care, and it showed at the top.",
            "Winning was never one moment. It was a thousand ordinary ones done well."
        ),
        MottoCategory.SUCCESS_AT_A_COST to listOf(
            "A full bank account cannot replace an empty chair beside you.",
            "Winning means little when you have nobody left to celebrate with.",
            "You climbed so high that you forgot to bring anyone with you.",
            "Every yes to the work was a quiet no to something that mattered.",
            "The summit is lonely when you leave everyone at the bottom.",
            "You paid for success with years you can never buy back."
        ),
        MottoCategory.FAILURE to listOf(
            "Failure took your plan, but it left you the lesson.",
            "Every fall taught your feet something your success never could.",
            "You were never defeated. You were only unfinished.",
            "Some losses are simply tuition for a wiser life.",
            "A stumble is just a sentence that hasn’t ended yet.",
            "You failed forward, and that still counts as moving."
        ),
        MottoCategory.COMEBACK to listOf(
            "Sometimes the life you rebuild becomes stronger than the life you lost.",
            "Your worst decision does not have to become your final decision.",
            "You rose from the rubble and built again with better bricks.",
            "The comeback is always louder than the fall.",
            "You learned to start again without starting over.",
            "What broke you also showed you where to build stronger."
        ),
        MottoCategory.MONEY to listOf(
            "Money opens doors, but only character decides what you do inside.",
            "Savings are quiet courage you build before the storm.",
            "You learned that enough is a number only you can set.",
            "Wealth is the freedom to choose, not the thing you chose.",
            "A careful coin today is a calm night years from now.",
            "You could own little and still live richly."
        ),
        MottoCategory.CAREER to listOf(
            "Your work was never just a job. It was the shape of your days.",
            "You earned your title one honest shift at a time.",
            "Skill spoke for you in rooms you had not yet entered.",
            "A career is a long conversation between who you are and what you do.",
            "You built a reputation that arrived before you did.",
            "Good work leaves a signature, even without your name on it."
        ),
        MottoCategory.BUSINESS to listOf(
            "You built something from nothing, and that nothing taught you everything.",
            "Every company begins as a stubborn idea someone refused to drop.",
            "Customers remember how you treated them, long after they forget what you charged.",
            "The best partnerships are built on trust long before the contract.",
            "A business is a promise you renew every morning.",
            "You did not just create a company. You created chances for others."
        ),
        MottoCategory.HEALTH to listOf(
            "Health is the quiet investment you only notice when it is gone.",
            "Rest is not the enemy of ambition. It keeps it alive.",
            "You cannot enjoy a life you were too tired to attend.",
            "The body keeps the ledger the calendar forgot.",
            "Take care of the engine. The journey is long.",
            "Strong days are built from small habits no one applauds."
        ),
        MottoCategory.LOVE to listOf(
            "Love is made of small choices repeated every day.",
            "To be loved well, you first had to learn to listen well.",
            "Some people are home in the shape of a person.",
            "Love is proven not by grand gestures but by showing up again.",
            "The right person makes the long road feel short.",
            "You loved with your whole heart, even when it was risky."
        ),
        MottoCategory.COURAGE to listOf(
            "Courage was never the absence of fear. It was moving anyway.",
            "You said yes to the unknown, and the unknown said yes back.",
            "The brave choice rarely feels brave while you are making it.",
            "You bet on yourself when nobody else had placed a bet.",
            "Fear told you to wait. You went and found out.",
            "You did not wait to feel ready. You became ready by going."
        ),
        MottoCategory.DISCIPLINE to listOf(
            "Discipline created opportunities that luck never could.",
            "Small habits, kept faithfully, built a staircase to everything you wanted.",
            "You did the work when no one was clapping, and it paid in ways applause never does.",
            "Consistency was your quiet superpower.",
            "You trusted the process while others chased shortcuts.",
            "Day by day, you became someone who follows through."
        ),
        MottoCategory.KINDNESS to listOf(
            "The people you helped became part of the life you built.",
            "Small acts of kindness planted trees you later rested beneath.",
            "You gave without keeping score, and life kept a better one for you.",
            "Kindness cost you moments and returned you years.",
            "You were someone’s turning point and never knew it.",
            "A gentle word from you carried farther than you imagined."
        ),
        MottoCategory.TRUST to listOf(
            "Trust is built in years and spent in moments, and you learned to guard it.",
            "Your word became the most valuable thing you owned.",
            "You kept your promises, and your promises kept you.",
            "Broken trust is an expensive teacher, but a lasting one.",
            "Being believed is a quiet kind of wealth.",
            "Honesty was slower, but it never needed to be remembered."
        ),
        MottoCategory.AMBITION to listOf(
            "You aimed high, and even your misses landed farther than most.",
            "Ambition was the fire. Character decided what it warmed.",
            "You refused to shrink your dreams to fit the room.",
            "You chased more than a paycheck. You chased a possibility.",
            "Wanting more is only worthwhile if you know what it is for.",
            "You kept your eyes on the horizon and your feet on the ground."
        ),
        MottoCategory.RISK to listOf(
            "Calculated risks opened doors that caution never could.",
            "You learned that regret outweighs failure, so you leapt.",
            "The right risk, taken with open eyes, is just courage with a plan.",
            "A safe road protects you from failure, but it can also hide your possibilities.",
            "You did not gamble. You invested in who you could become.",
            "Some doors only open for people willing to be uncomfortable."
        ),
        MottoCategory.RISK_WISDOM to listOf(
            "Courage is knowing which risks are worth taking.",
            "Boldness without a plan is just hope in a hurry.",
            "You learned the difference between brave and reckless the hard way.",
            "Every failed gamble bought you a sharper eye.",
            "Next time you will leap with a map.",
            "Speed felt exciting. Patience would have felt wiser."
        ),
        MottoCategory.HAPPINESS to listOf(
            "Happiness was rarely in the destination. It was in who walked beside you.",
            "You found joy in places most people hurried past.",
            "Contentment is the richest thing a person can own.",
            "You learned to enjoy the road, not just the arrival.",
            "A light heart is its own kind of wealth.",
            "You did not chase happiness. You made room for it."
        ),
        MottoCategory.REGRET to listOf(
            "Some regrets are just love that arrived late.",
            "You cannot rewrite what was. You can only honor what remains.",
            "The words you did not say were heavier than the ones you did.",
            "Time was the one thing you could never earn back.",
            "Regret is a teacher who arrives after the lesson was due.",
            "What you wish you had done becomes a map for those who follow."
        ),
        MottoCategory.SECOND_CHANCES to listOf(
            "You were always more than the mistakes you made when you were young.",
            "It is never too late to become the person you needed all along.",
            "Second chances are earned by the person you choose to be next.",
            "You turned early mistakes into the foundation of a better life.",
            "Every ending you survived became a new beginning.",
            "You proved that where you start is not where you have to stay."
        ),
        MottoCategory.BALANCED_LIFE to listOf(
            "A good life is built in balance, not in extremes.",
            "Health, home, work, and joy: you kept them all in the air.",
            "You did not win any single race. You won the long one.",
            "A well-balanced life rarely makes headlines, but it fills every hour.",
            "You gave each part of life its season, and each part gave back.",
            "You did not choose an easy life. You chose one worth remembering."
        )
    )

    val all: List<Motto> = lines.flatMap { (cat, texts) ->
        texts.mapIndexed { i, t -> Motto("${cat.name.lowercase()}_${i + 1}", cat, t) }
    }

    private val byId = all.associateBy { it.id }

    fun byCategory(c: MottoCategory): List<Motto> = all.filter { it.category == c }

    fun get(id: String): Motto? = byId[id]
}
