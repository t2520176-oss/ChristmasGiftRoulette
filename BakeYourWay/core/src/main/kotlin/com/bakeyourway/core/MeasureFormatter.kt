package com.bakeyourway.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** Kitchen-friendly number formatting and unit conversion (US cups / tbsp / tsp, grams, ml). */
object MeasureFormatter {
    const val TSP_PER_TBSP = 3.0
    const val TBSP_PER_CUP = 16.0
    const val TSP_PER_CUP = 48.0

    /** US cup in millilitres used for liquids in metric mode (240 ml "legal" cup). */
    const val ML_PER_CUP = 240.0

    /** One large egg beaten is about 3 tablespoons (about 50 g). */
    const val TBSP_BEATEN_EGG = 3.0

    private val CUP_FRACTIONS = doubleArrayOf(
        0.0, 1.0 / 8, 1.0 / 4, 1.0 / 3, 3.0 / 8, 1.0 / 2, 5.0 / 8, 2.0 / 3, 3.0 / 4, 7.0 / 8, 1.0,
    )
    private val SMALL_TSP_FRACTIONS = doubleArrayOf(1.0 / 8, 1.0 / 4, 1.0 / 2, 3.0 / 4, 1.0)
    private val QUARTERS = doubleArrayOf(0.0, 0.25, 0.5, 0.75, 1.0)
    private val HALVES = doubleArrayOf(0.0, 0.5, 1.0)

    // ----- Conversions --------------------------------------------------------------------

    /** Amount expressed in teaspoons, or null for GRAM / ML / PIECE units. */
    fun toTeaspoons(amount: Double, unit: MeasureUnit): Double? = when (unit) {
        MeasureUnit.CUP -> amount * TSP_PER_CUP
        MeasureUnit.TBSP -> amount * TSP_PER_TBSP
        MeasureUnit.TSP -> amount
        else -> null
    }

    /** Exact weight in grams using the ingredient's own density, or null when unknown. */
    fun toGrams(amount: Double, unit: MeasureUnit, def: IngredientDef): Double? {
        val perCup = def.gramsPerCup
        return when (unit) {
            MeasureUnit.GRAM -> amount
            MeasureUnit.PIECE -> def.gramsPerPiece?.let { it * amount }
            MeasureUnit.ML -> perCup?.let { amount / ML_PER_CUP * it }
            else -> {
                val tsp = toTeaspoons(amount, unit) ?: return null
                perCup?.let { tsp / TSP_PER_CUP * it }
            }
        }
    }

    /** Exact volume in cups using the ingredient's own density, or null for counted items. */
    fun toCups(amount: Double, unit: MeasureUnit, def: IngredientDef): Double? {
        val tsp = toTeaspoons(amount, unit)
        if (tsp != null) return tsp / TSP_PER_CUP
        val perCup = def.gramsPerCup ?: return null
        return when (unit) {
            MeasureUnit.GRAM -> amount / perCup
            MeasureUnit.ML -> amount / ML_PER_CUP
            else -> null
        }
    }

    // ----- Number text --------------------------------------------------------------------

    /** "1", "1.5", "0.25" - never trailing zeros. */
    fun trimNumber(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded == floor(rounded)) rounded.toLong().toString() else rounded.toString().trimEnd('0').trimEnd('.')
    }

    private fun fractionText(f: Double): String = when {
        near(f, 1.0 / 8) -> "1/8"
        near(f, 1.0 / 4) -> "1/4"
        near(f, 1.0 / 3) -> "1/3"
        near(f, 3.0 / 8) -> "3/8"
        near(f, 1.0 / 2) -> "1/2"
        near(f, 5.0 / 8) -> "5/8"
        near(f, 2.0 / 3) -> "2/3"
        near(f, 3.0 / 4) -> "3/4"
        near(f, 7.0 / 8) -> "7/8"
        else -> error("No text for fraction $f")
    }

    private fun near(a: Double, b: Double) = abs(a - b) < 1e-6

    private fun nearest(value: Double, candidates: DoubleArray): Double =
        candidates.minByOrNull { abs(it - value) }!!

    /** "1 1/2", "1/2", "2". [frac] must be one of the supported fractions (or 0). */
    private fun mixed(whole: Int, frac: Double): String = when {
        frac == 0.0 -> whole.toString()
        whole == 0 -> fractionText(frac)
        else -> "$whole ${fractionText(frac)}"
    }

    /** Splits [value] into whole + nearest fraction from [candidates], carrying 1.0 into the whole. */
    private fun splitNearest(value: Double, candidates: DoubleArray): Pair<Int, Double> {
        var whole = floor(value + 1e-9).toInt()
        var frac = nearest(value - whole, candidates)
        if (frac >= 1.0 - 1e-9) {
            whole += 1
            frac = 0.0
        }
        return whole to frac
    }

    // ----- Volume text --------------------------------------------------------------------

    /** Practical spoon/cup text for an amount given in teaspoons. */
    fun formatVolume(totalTsp: Double): String {
        if (totalTsp >= 12.0 - 1e-9) {
            val (whole, frac) = splitNearest(totalTsp / TSP_PER_CUP, CUP_FRACTIONS)
            return cupsText(whole, frac)
        }
        if (totalTsp >= 3.0 - 1e-9) {
            val tbsp = totalTsp / TSP_PER_TBSP
            val (whole, frac) = splitNearest(tbsp, HALVES)
            if (whole >= 4) return "1/4 cup"
            return "${mixed(whole, frac)} tbsp"
        }
        return formatTeaspoons(totalTsp)
    }

    /**
     * Cup text using only sizes that exist as measuring tools: 1/8, 3/8, 5/8 and 7/8 of a cup are written
     * as tablespoons ("6 tbsp", "1 cup + 2 tbsp").
     */
    private fun cupsText(whole: Int, frac: Double): String {
        fun unit(n: Int, f: Double) = if (n > 1 || (n == 1 && f > 0)) "cups" else "cup"
        return when {
            near(frac, 1.0 / 8) -> if (whole == 0) "2 tbsp" else "$whole ${unit(whole, 0.0)} + 2 tbsp"
            near(frac, 3.0 / 8) -> if (whole == 0) "6 tbsp" else "$whole ${unit(whole, 0.0)} + 6 tbsp"
            near(frac, 5.0 / 8) -> if (whole == 0) "1/2 cup + 2 tbsp" else "$whole 1/2 cups + 2 tbsp"
            near(frac, 7.0 / 8) -> if (whole == 0) "3/4 cup + 2 tbsp" else "$whole 3/4 cups + 2 tbsp"
            else -> "${mixed(whole, frac)} ${unit(whole, frac)}"
        }
    }

    /** Teaspoon-only text ("1/4 tsp", "3 tsp", "a pinch") used for leaveners, salt, spices and extracts. */
    fun formatTeaspoons(totalTsp: Double): String {
        if (totalTsp < 1.0 / 16) return "a pinch"
        if (totalTsp < 1.0) {
            val f = nearest(totalTsp, SMALL_TSP_FRACTIONS)
            return if (f >= 1.0) "1 tsp" else "${fractionText(f)} tsp"
        }
        val (whole, frac) = splitNearest(totalTsp, QUARTERS)
        return "${mixed(whole, frac)} tsp"
    }

    /** "2 cups", "1 1/2 cups", "1/4 cup" for a flour amount in cups (close fractions only, else decimals). */
    fun formatFlourCups(cups: Double): String {
        val (whole, frac) = splitNearest(cups, CUP_FRACTIONS)
        val exact = whole + frac
        val text = if (abs(exact - cups) <= 0.01) mixed(whole, frac) else trimNumber(cups)
        return "$text ${if (cups > 1.0 + 1e-9) "cups" else "cup"}"
    }

    fun formatGrams(grams: Double): String = "${roundWeight(grams)} g"

    fun formatMl(ml: Double): String = "${roundWeight(ml)} ml"

    /** Grams/ml are rounded to the nearest 5 (nearest 1 below 20) - no false precision. */
    fun roundWeight(value: Double): Int {
        val v = if (value < 20) value.roundToInt() else ((value / 5).roundToInt() * 5)
        return maxOf(v, 1)
    }

    // ----- Counted items ------------------------------------------------------------------

    /** Result of expressing an egg quantity in practical terms. */
    data class EggAmount(val wholeEggs: Int, val beatenEggTbsp: Double, val text: String)

    fun formatEggs(count: Double, def: IngredientDef): EggAmount {
        val single = def.pieceSingular ?: "large egg"
        val plural = def.piecePlural ?: "large eggs"
        var whole = floor(count + 1e-9).toInt()
        var tbsp = Math.round((count - whole) * TBSP_BEATEN_EGG * 2) / 2.0
        if (tbsp >= 2.5) {
            whole += 1
            tbsp = 0.0
        }
        if (whole == 0 && tbsp == 0.0) tbsp = 0.5
        val tbspText = mixedHalf(tbsp)
        val text = when {
            tbsp == 0.0 -> "$whole ${if (whole == 1) single else plural}"
            whole == 0 -> "about $tbspText tbsp beaten egg (${eggFraction(tbsp)})"
            else -> "$whole ${if (whole == 1) single else plural} + about $tbspText tbsp beaten egg"
        }
        return EggAmount(whole, tbsp, text)
    }

    private fun mixedHalf(v: Double): String {
        val whole = floor(v).toInt()
        return mixed(whole, v - whole)
    }

    private fun eggFraction(tbsp: Double): String = when {
        tbsp <= 0.5 -> "about 1/6 of an egg"
        tbsp <= 1.0 -> "about 1/3 of an egg"
        tbsp <= 1.5 -> "about 1/2 of an egg"
        tbsp <= 2.0 -> "about 2/3 of an egg"
        else -> "about 5/6 of an egg"
    }

    /** Count text for non-egg pieces, e.g. "1 1/2 medium bananas". */
    fun formatPieces(count: Double, def: IngredientDef, rule: RoundingRule): String {
        val n = when (rule) {
            RoundingRule.WHOLE -> maxOf(count.roundToInt(), 1).toDouble()
            RoundingRule.NONE -> (count * 100).roundToInt() / 100.0
            else -> maxOf(Math.round(count * 2) / 2.0, 0.5)
        }
        val whole = floor(n).toInt()
        val frac = n - whole
        val numberText = if (rule == RoundingRule.NONE) trimNumber(n) else mixed(whole, frac)
        val label = if (n == 1.0) (def.pieceSingular ?: def.name.lowercase()) else (def.piecePlural ?: def.name.lowercase())
        return "$numberText $label"
    }
}
