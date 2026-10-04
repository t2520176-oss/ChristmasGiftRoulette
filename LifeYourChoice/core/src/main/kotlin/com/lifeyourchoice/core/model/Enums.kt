package com.lifeyourchoice.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Gender { BOY, GIRL }

/** Everything the player can see (and the story can change). All values live in 0..100. */
@Serializable
enum class Stat(val label: String) {
    HEALTH("Health"),
    KNOWLEDGE("Knowledge"),
    DISCIPLINE("Discipline"),
    CONFIDENCE("Confidence"),
    REPUTATION("Reputation"),
    FAMILY("Family"),
    FRIENDSHIP("Friendship"),
    MONEY("Money"),
    HAPPINESS("Happiness"),
    ENERGY("Energy"),
    CAREER("Career");

    companion object {
        /** Stats shown in the stats panel, in display order (Energy and Money are in the top bar too). */
        val panel = listOf(HEALTH, KNOWLEDGE, DISCIPLINE, CONFIDENCE, REPUTATION, FAMILY, FRIENDSHIP, MONEY, HAPPINESS)
    }
}

/** Hidden personality traits. Never shown as exact numbers; they steer opportunities and endings. */
@Serializable
enum class Trait(val label: String) {
    RESPONSIBLE("Responsible"),
    RISK_TAKER("Risk Taker"),
    LOYAL("Loyal"),
    AMBITIOUS("Ambitious"),
    COMPASSIONATE("Compassionate"),
    DISHONEST("Dishonest"),
    DISCIPLINED("Disciplined"),
    IMPULSIVE("Impulsive")
}

@Serializable
enum class Category(val defaultTitle: String) {
    SCHOOL("High School"),
    FAMILY("Family"),
    FRIENDSHIP("Friendship"),
    CAREER("Career"),
    MONEY("Money"),
    LOVE("Relationship"),
    BUSINESS("Business"),
    MAJOR("Life Event"),
    RANDOM("Life Happens"),
    MILESTONE("Milestone"),
    LATER_LIFE("Later Life")
}

/** Which illustrated backdrop a scenario uses. Rendering lives in the app module. */
@Serializable
enum class SceneArt {
    CLASSROOM, HALLWAY, BASKETBALL_COURT, LIVING_ROOM, NIGHT_CITY, APARTMENT, OFFICE, WORKSHOP,
    HOSPITAL, COFFEE_SHOP, MEETING, AIRPORT, WEDDING, NEW_HOME, SMALL_BUSINESS, SKYLINE,
    CAMPUS, PARK, STREET, KITCHEN
}

@Serializable
enum class Education(val label: String) {
    HIGH_SCHOOL("High School"),
    UNIVERSITY("University"),
    TRADE_SCHOOL("Trade School"),
    SELF_TAUGHT("Self-taught")
}

@Serializable
enum class RelationshipStatus(val label: String) {
    SINGLE("Single"),
    DATING("In a relationship"),
    MARRIED("Married")
}

/**
 * Career directions. [ladder] has five rungs; the player's `careerLevel` (1..5) indexes into it.
 * ENTREPRENEUR / BUSINESS_OWNER are reached through the business system as well as through choices.
 */
@Serializable
enum class CareerTrack(val label: String, val ladder: List<String>) {
    NONE("Unemployed", listOf("Job seeker", "Job seeker", "Job seeker", "Job seeker", "Job seeker")),
    EMPLOYEE("Employee", listOf("Trainee", "Office Associate", "Team Specialist", "Senior Associate", "Department Lead")),
    ENGINEER("Engineer", listOf("Junior Engineer", "Engineer", "Senior Engineer", "Lead Engineer", "Chief Engineer")),
    DESIGNER("Designer", listOf("Junior Designer", "Designer", "Senior Designer", "Art Director", "Creative Director")),
    TEACHER("Teacher", listOf("Student Teacher", "Teacher", "Senior Teacher", "Department Head", "Principal")),
    DOCTOR("Doctor", listOf("Medical Intern", "Resident Doctor", "Doctor", "Senior Physician", "Chief of Medicine")),
    CREATOR("Creator", listOf("Hobby Creator", "Rising Creator", "Full-time Creator", "Popular Creator", "Studio Founder")),
    ENTREPRENEUR("Entrepreneur", listOf("Startup Founder", "Small Business Founder", "Entrepreneur", "Serial Entrepreneur", "Business Leader")),
    BUSINESS_OWNER("Business Owner", listOf("Shop Owner", "Business Owner", "Company Owner", "Company Director", "Business Magnate")),
    SKILLED_WORKER("Skilled Worker", listOf("Apprentice", "Technician", "Skilled Tradesperson", "Master Craftsperson", "Workshop Foreman")),
    TECH("Technology Career", listOf("Junior Developer", "Developer", "Senior Developer", "Tech Lead", "Head of Technology")),
    MANAGER("Manager", listOf("Team Coordinator", "Supervisor", "Manager", "Senior Manager", "Director"));

    fun titleAt(level: Int): String = ladder[(level.coerceIn(1, 5)) - 1]
}

/** Persistent characters in the player's life. They remember how they were treated (trust 0..100). */
@Serializable
enum class NpcRole(val label: String) {
    BEST_FRIEND("Best friend"),
    UNDERDOG("The classmate you helped"),
    RIVAL("Rival"),
    MENTOR("Mentor"),
    PARTNER("Partner"),
    SIBLING("Sibling"),
    BOSS("Boss"),
    COWORKER("Coworker")
}

@Serializable
enum class Chapter(val label: String) {
    HIGH_SCHOOL("High School"),
    COMING_OF_AGE("Graduation"),
    NEXT_STEP("College / Training / Work"),
    FIRST_CAREER("First Career"),
    RELATIONSHIPS("Relationships & Career"),
    FAMILY_BUSINESS("Family & Business"),
    RESPONSIBILITIES("Major Responsibilities"),
    CONSEQUENCES("Consequences"),
    LATER_LIFE("Later Life");

    companion object {
        fun forAge(age: Int): Chapter = when {
            age < 18 -> HIGH_SCHOOL
            age < 20 -> COMING_OF_AGE
            age < 23 -> NEXT_STEP
            age < 27 -> FIRST_CAREER
            age < 32 -> RELATIONSHIPS
            age < 40 -> FAMILY_BUSINESS
            age < 50 -> RESPONSIBILITIES
            age < 65 -> CONSEQUENCES
            else -> LATER_LIFE
        }
    }
}
