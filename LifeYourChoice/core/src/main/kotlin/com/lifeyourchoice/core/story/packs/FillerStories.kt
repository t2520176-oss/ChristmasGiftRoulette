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

/** Gentle everyday moments. Rarely chosen, but they guarantee the story never runs dry. */
val FillerStories = storyPack("filler", "Everyday Life") {

    scenario("fill_quiet_stretch", RANDOM, LIVING_ROOM, 14..99, weight = 1, repeatable = true, title = "A Quiet Stretch") {
        text("Nothing dramatic happens for a while. The days have a comfortable rhythm, and you have a rare chance to decide what to do with your free time.")
        choice("Spend it with people you care about.") {
            family(+3); friendship(+3); happiness(+3)
            result("Nothing happens, and that’s exactly the point. It’s a good few weeks.")
        }
        choice("Learn something new.") {
            knowledge(+4); discipline(+2); energy(-2)
            result("A book, a video, a workshop. You come out of it knowing something you didn’t before.")
        }
        choice("Look after your health.") {
            health(+4); energy(+4); discipline(+1)
            result("Walks, early nights and proper meals. You feel sharper.")
        }
        choice("Save a little, plan a little.") {
            money(+3); discipline(+2); trait(RESPONSIBLE, 1)
            result("A small plan today is a big difference later.")
        }
    }

    scenario("fill_old_photograph", RANDOM, LIVING_ROOM, 20..99, weight = 1, repeatable = true, title = "An Old Photograph") {
        text("While tidying up, you find an old photograph: faces you haven’t seen in years, a place you used to love. For a moment, the room goes quiet.")
        choice("Call someone from the picture.") {
            friendship(+4); happiness(+4)
            result("A surprised voice on the other end, and a long, warm conversation.")
        }
        choice("Put it on the wall.") {
            happiness(+3); family(+1)
            result("Now it greets you every morning, and reminds you of who you are.")
        }
        choice("Tuck it away again.") {
            happiness(-1)
            result("Some memories are better kept in a drawer.")
        }
        choice("Show it to your family and tell the story.") {
            family(+4); happiness(+3)
            result("They ask a hundred questions, and you’re delighted to answer every one.")
        }
    }

    scenario("fill_new_habit", RANDOM, APARTMENT, 16..99, weight = 1, repeatable = true, title = "A Small Habit") {
        text("You’ve been meaning to start a small daily habit: reading, stretching, writing, calling your family. Today seems like a good day to begin.")
        choice("Start immediately, and commit to it.") {
            discipline(+4); happiness(+2); trait(DISCIPLINED, 1)
            result("Day one is easy. Day thirty is a quiet victory.")
        }
        choice("Start small. Just five minutes a day.") {
            discipline(+3); happiness(+2)
            result("Five minutes is nothing, and it turns out to be everything.")
        }
        choice("Wait for a better time.") {
            discipline(-1)
            result("The better time somehow never arrives.")
        }
        choice("Invite a friend to do it with you.") {
            friendship(+3); discipline(+3); happiness(+3)
            result("Doing it together makes it easy, and fun.")
        }
    }
}
