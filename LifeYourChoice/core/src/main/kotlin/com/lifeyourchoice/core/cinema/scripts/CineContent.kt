package com.lifeyourchoice.core.cinema.scripts

import com.lifeyourchoice.core.cinema.CineLibrary

/** All built-in cinematics. Add a [com.lifeyourchoice.core.cinema.CinePack] here to add scenes. */
object CineContent {
    fun library(): CineLibrary = CineLibrary(
        listOf(
            SchoolCine, FamilyCine, FriendCine, CareerCine, LoveCine, BusinessCine, MajorCine, PayoffCine, LifeCine,
            YouthCine, WorkMoneyCine, HomeLoveCine, LaterLifeCine
        )
    )
}
