package com.lifeyourchoice.app.ui.art

import androidx.compose.ui.graphics.Color
import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.PlayerLook

enum class HairStyle { SHORT_MESSY, SWEPT, CURLY_SHORT, LONG_WAVY, PONYTAIL, BOB }

enum class OutfitStyle { HOODIE, JACKET, SHIRT, COAT }

/** Resolved colours and shapes for one character. Everything is drawn in code. */
class Look(
    val name: String,
    val skin: Color,
    val hair: Color,
    val style: HairStyle,
    val top: Color,
    val topAccent: Color,
    val outfit: OutfitStyle? = null,
    val pants: Color = Color(0xFF1E2744)
)

/** Palettes used by character creation. Indexes are stored in [PlayerLook]. */
object Palettes {
    val skins = listOf(Color(0xFFF3CFB0), Color(0xFFE8BE98), Color(0xFFD9A074), Color(0xFFB87D56), Color(0xFF9A6644), Color(0xFF6F4630))
    val hairColors = listOf(Color(0xFF1A1420), Color(0xFF3A2014), Color(0xFF4A2E1B), Color(0xFF7A3F1D), Color(0xFFC9A24A), Color(0xFF9B3B22))
    val outfits = listOf(Color(0xFF263050), Color(0xFF1F5A99), Color(0xFF8A2F2F), Color(0xFF2F6E5B), Color(0xFF6C3A8A), Color(0xFF9AA0B8))
    val voiceNames = listOf("Warm", "Bright", "Deep")

    /** The six hair options shown in character creation: (style index, colour index). */
    val hairOptions = listOf(0 to 0, 1 to 2, 2 to 1, 0 to 4, 1 to 3, 2 to 5)

    fun hairStyleFor(gender: Gender, i: Int): HairStyle {
        val list = if (gender == Gender.BOY) listOf(HairStyle.SHORT_MESSY, HairStyle.SWEPT, HairStyle.CURLY_SHORT)
        else listOf(HairStyle.LONG_WAVY, HairStyle.PONYTAIL, HairStyle.BOB)
        return list[i.coerceIn(0, 2)]
    }
}

object Looks {
    fun of(gender: Gender, appearance: Int): Look = forPlayer(gender, PlayerLook.fromPreset(gender, appearance))

    val boys: List<Look> get() = (0..2).map { of(Gender.BOY, it) }
    val girls: List<Look> get() = (0..2).map { of(Gender.GIRL, it) }

    fun forPlayer(gender: Gender, pl: PlayerLook): Look {
        val skin = Palettes.skins[pl.skin.coerceIn(0, Palettes.skins.size - 1)]
        val hair = Palettes.hairColors[pl.hairColor.coerceIn(0, Palettes.hairColors.size - 1)]
        val top = Palettes.outfits[pl.outfit.coerceIn(0, Palettes.outfits.size - 1)]
        return Look("You", skin, hair, Palettes.hairStyleFor(gender, pl.hairStyle), top, Color(0xFFEAF1FF))
    }

    private fun h(seed: Long, salt: Int, mod: Int): Int = (((seed * 31 + salt * 17) % 1000 + 1000) % 1000).toInt() % mod

    /** A stable look for a non-player character, derived from the life's seed so they never change. */
    fun forActor(actor: ActorId, gender: Gender, seed: Long): Look {
        val skin = Palettes.skins[h(seed, actor.ordinal * 3 + 1, Palettes.skins.size)]
        val hairIdx = h(seed, actor.ordinal * 3 + 2, Palettes.hairColors.size)
        val style = Palettes.hairStyleFor(gender, h(seed, actor.ordinal * 3 + 3, 3))
        val casual = Palettes.outfits[h(seed, actor.ordinal * 5 + 4, Palettes.outfits.size)]
        return when (actor) {
            ActorId.FATHER -> Look("Father", skin, Palettes.hairColors[1], HairStyle.SHORT_MESSY, Color(0xFF4A5A78), Color.White, OutfitStyle.SHIRT, Color(0xFF2A2F3F))
            ActorId.MOTHER -> Look("Mother", skin, Palettes.hairColors[hairIdx], HairStyle.BOB, Color(0xFFB07A8A), Color.White, OutfitStyle.SHIRT, Color(0xFF3A2F45))
            ActorId.TEACHER -> Look("Teacher", skin, Palettes.hairColors[hairIdx], style, Color(0xFF6A5040), Color.White, OutfitStyle.JACKET, Color(0xFF2A2A33))
            ActorId.BOSS -> Look("Boss", skin, Palettes.hairColors[hairIdx], style, Color(0xFF2B2F3F), Color.White, OutfitStyle.JACKET, Color(0xFF22242E))
            ActorId.INTERVIEWER -> Look("Interviewer", skin, Palettes.hairColors[hairIdx], style, Color(0xFF1E2A48), Color.White, OutfitStyle.JACKET, Color(0xFF1A2038))
            ActorId.DOCTOR -> Look("Doctor", skin, Palettes.hairColors[hairIdx], style, Color(0xFFF0F5FA), Color(0xFF3F9AA8), OutfitStyle.COAT, Color(0xFF3A4A5A))
            ActorId.MENTOR -> Look("Mentor", skin, Palettes.hairColors[hairIdx], style, Color(0xFF5A4A6A), Color.White, OutfitStyle.JACKET, Color(0xFF2A2630))
            ActorId.CHILD -> Look("Child", skin, Palettes.hairColors[hairIdx], style, Color(0xFFE5B83C), Color.White, OutfitStyle.HOODIE, Color(0xFF2F4F8F))
            ActorId.CUSTOMER, ActorId.STRANGER -> Look("Stranger", skin, Palettes.hairColors[hairIdx], style, casual, Color.White, OutfitStyle.SHIRT, Color(0xFF2A3040))
            else -> Look(actor.label, skin, Palettes.hairColors[hairIdx], style, casual, Color(0xFFEAF1FF), null, Color(0xFF232B45))
        }
    }
}
