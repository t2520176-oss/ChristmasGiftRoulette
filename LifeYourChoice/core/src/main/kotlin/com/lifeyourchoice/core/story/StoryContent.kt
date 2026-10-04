package com.lifeyourchoice.core.story

import com.lifeyourchoice.core.story.packs.BusinessStories
import com.lifeyourchoice.core.story.packs.CareerStories
import com.lifeyourchoice.core.story.packs.FillerStories
import com.lifeyourchoice.core.story.packs.MajorStories
import com.lifeyourchoice.core.story.packs.MilestoneStories
import com.lifeyourchoice.core.story.packs.PayoffStories
import com.lifeyourchoice.core.story.packs.RandomStories
import com.lifeyourchoice.core.story.packs.FamilyStories
import com.lifeyourchoice.core.story.packs.FriendshipStories
import com.lifeyourchoice.core.story.packs.LoveStories
import com.lifeyourchoice.core.story.packs.MoneyStories
import com.lifeyourchoice.core.story.packs.SchoolStories

/** The built-in stories shipped with Version 1. Add new [StoryPack]s here (or load them from elsewhere). */
object StoryContent {
    fun library(): StoryLibrary = StoryLibrary(
        listOf(
            SchoolStories, FamilyStories, FriendshipStories, CareerStories, MoneyStories, LoveStories,
            BusinessStories, MajorStories, RandomStories, MilestoneStories, PayoffStories, FillerStories
        )
    )
}
