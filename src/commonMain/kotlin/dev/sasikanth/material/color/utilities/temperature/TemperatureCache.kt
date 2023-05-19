/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sasikanth.material.color.utilities.temperature

import dev.sasikanth.material.color.utilities.hct.Hct
import dev.sasikanth.material.color.utilities.utils.MathUtils
import dev.sasikanth.material.color.utilities.utils.ColorUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Design utilities using color temperature theory.
 *
 *
 * Analogous colors, complementary color, and cache to efficiently, lazily, generate data for
 * calculations when needed.
 */
class TemperatureCache {
  private lateinit var input: Hct
  private var precomputedComplement: Hct? = null
  private var precomputedHctsByTemp: List<Hct>? = null
  private var precomputedHctsByHue: List<Hct>? = null
  private var precomputedTempsByHct: Map<Hct, Double>? = null

  private constructor() {
    throw UnsupportedOperationException()
  }

  /**
   * Create a cache that allows calculation of ex. complementary and analogous colors.
   *
   * @param input Color to find complement/analogous colors of. Any colors will have the same tone,
   * and chroma as the input color, modulo any restrictions due to the other hues having lower
   * limits on chroma.
   */
  constructor(input: Hct) {
    this.input = input
  }// Find the color in the other section, closest to the inverse percentile
  // of the input color. This is the complement.
  /**
   * A color that complements the input color aesthetically.
   *
   *
   * In art, this is usually described as being across the color wheel. History of this shows
   * intent as a color that is just as cool-warm as the input color is warm-cool.
   */
  val complement: Hct
    get() {
      if (precomputedComplement != null) {
        return precomputedComplement!!
      }
      val coldestHue: Double = coldest.getHue()
      val coldestTemp = tempsByHct!![coldest]!!
      val warmestHue: Double = warmest.getHue()
      val warmestTemp = tempsByHct!![warmest]!!
      val range = warmestTemp - coldestTemp
      val startHueIsColdestToWarmest = isBetween(input.getHue(), coldestHue, warmestHue)
      val startHue = if (startHueIsColdestToWarmest) warmestHue else coldestHue
      val endHue = if (startHueIsColdestToWarmest) coldestHue else warmestHue
      val directionOfRotation = 1.0
      var smallestError = 1000.0
      var answer: Hct? = hctsByHue!![input.getHue().roundToInt()]
      val complementRelativeTemp = 1.0 - getRelativeTemperature(input)
      // Find the color in the other section, closest to the inverse percentile
      // of the input color. This is the complement.
      var hueAddend = 0.0
      while (hueAddend <= 360.0) {
        val hue: Double = MathUtils.sanitizeDegreesDouble(
          startHue + directionOfRotation * hueAddend
        )
        if (!isBetween(hue, startHue, endHue)) {
          hueAddend += 1.0
          continue
        }
        val possibleAnswer: Hct = hctsByHue!![round(hue).toInt()]
        val relativeTemp = (tempsByHct!![possibleAnswer]!! - coldestTemp) / range
        val error: Double = abs(complementRelativeTemp - relativeTemp)
        if (error < smallestError) {
          smallestError = error
          answer = possibleAnswer
        }
        hueAddend += 1.0
      }
      precomputedComplement = answer
      return precomputedComplement!!
    }

  /**
   * 5 colors that pair well with the input color.
   *
   *
   * The colors are equidistant in temperature and adjacent in hue.
   */
  val analogousColors: List<Hct>
    get() = getAnalogousColors(5, 12)

  /**
   * A set of colors with differing hues, equidistant in temperature.
   *
   *
   * In art, this is usually described as a set of 5 colors on a color wheel divided into 12
   * sections. This method allows provision of either of those values.
   *
   *
   * Behavior is undefined when count or divisions is 0. When divisions < count, colors repeat.
   *
   * @param count The number of colors to return, includes the input color.
   * @param divisions The number of divisions on the color wheel.
   */
  fun getAnalogousColors(count: Int, divisions: Int): List<Hct> {
    // The starting hue is the hue of the input color.
    val startHue = input.getHue().roundToInt()
    val startHct: Hct = hctsByHue!![startHue]
    var lastTemp = getRelativeTemperature(startHct)
    val allColors: MutableList<Hct> = ArrayList<Hct>()
    allColors.add(startHct)
    var absoluteTotalTempDelta = 0.0
    for (i in 0..359) {
      val hue: Int = MathUtils.sanitizeDegreesInt(startHue + i)
      val hct: Hct = hctsByHue!![hue]
      val temp = getRelativeTemperature(hct)
      val tempDelta: Double = abs(temp - lastTemp)
      lastTemp = temp
      absoluteTotalTempDelta += tempDelta
    }
    var hueAddend = 1
    val tempStep = absoluteTotalTempDelta / divisions.toDouble()
    var totalTempDelta = 0.0
    lastTemp = getRelativeTemperature(startHct)
    while (allColors.size < divisions) {
      val hue: Int = MathUtils.sanitizeDegreesInt(startHue + hueAddend)
      val hct: Hct = hctsByHue!![hue]
      val temp = getRelativeTemperature(hct)
      val tempDelta: Double = abs(temp - lastTemp)
      totalTempDelta += tempDelta
      var desiredTotalTempDeltaForIndex = allColors.size * tempStep
      var indexSatisfied = totalTempDelta >= desiredTotalTempDeltaForIndex
      var indexAddend = 1
      // Keep adding this hue to the answers until its temperature is
      // insufficient. This ensures consistent behavior when there aren't
      // `divisions` discrete steps between 0 and 360 in hue with `tempStep`
      // delta in temperature between them.
      //
      // For example, white and black have no analogues: there are no other
      // colors at T100/T0. Therefore, they should just be added to the array
      // as answers.
      while (indexSatisfied && allColors.size < divisions) {
        allColors.add(hct)
        desiredTotalTempDeltaForIndex = (allColors.size + indexAddend) * tempStep
        indexSatisfied = totalTempDelta >= desiredTotalTempDeltaForIndex
        indexAddend++
      }
      lastTemp = temp
      hueAddend++
      if (hueAddend > 360) {
        while (allColors.size < divisions) {
          allColors.add(hct)
        }
        break
      }
    }
    val answers: MutableList<Hct> = ArrayList<Hct>()
    answers.add(input)
    val ccwCount: Int = floor((count.toDouble() - 1.0) / 2.0).toInt()
    for (i in 1 until ccwCount + 1) {
      var index = 0 - i
      while (index < 0) {
        index += allColors.size
      }
      if (index >= allColors.size) {
        index %= allColors.size
      }
      answers.add(0, allColors[index])
    }
    val cwCount = count - ccwCount - 1
    for (i in 1 until cwCount + 1) {
      var index = i
      while (index < 0) {
        index += allColors.size
      }
      if (index >= allColors.size) {
        index %= allColors.size
      }
      answers.add(allColors[index])
    }
    return answers
  }

  /**
   * Temperature relative to all colors with the same chroma and tone.
   *
   * @param hct HCT to find the relative temperature of.
   * @return Value on a scale from 0 to 1.
   */
  fun getRelativeTemperature(hct: Hct): Double {
    val range = tempsByHct!![warmest]!! - tempsByHct!![coldest]!!
    val differenceFromColdest = tempsByHct!![hct]!! - tempsByHct!![coldest]!!
    // Handle when there's no difference in temperature between warmest and
    // coldest: for example, at T100, only one color is available, white.
    return if (range == 0.0) {
      0.5
    } else differenceFromColdest / range
  }

  /** Coldest color with same chroma and tone as input.  */
  private val coldest: Hct
    get() = hctsByTemp!![0]

  /**
   * HCTs for all colors with the same chroma/tone as the input.
   *
   *
   * Sorted by hue, ex. index 0 is hue 0.
   */
  private val hctsByHue: List<Hct>?
    get() {
      if (precomputedHctsByHue != null) {
        return precomputedHctsByHue
      }
      val hcts: MutableList<Hct> = ArrayList<Hct>()
      var hue = 0.0
      while (hue <= 360.0) {
        val colorAtHue: Hct = Hct.from(hue, input.getChroma(), input.getTone())
        hcts.add(colorAtHue)
        hue += 1.0
      }
      precomputedHctsByHue = hcts.toList()
      return precomputedHctsByHue
    }

  /**
   * HCTs for all colors with the same chroma/tone as the input.
   *
   *
   * Sorted from coldest first to warmest last.
   */
  // Prevent lint for Comparator not being available on Android before API level 24, 7.0, 2016.
  // "AndroidJdkLibsChecker" for one linter, "NewApi" for another.
  // A java_library Bazel rule with an Android constraint cannot skip these warnings without this
  // annotation; another solution would be to create an android_library rule and supply
  // AndroidManifest with an SDK set higher than 23.
  private val hctsByTemp: List<Hct>?
    get() {
      if (precomputedHctsByTemp != null) {
        return precomputedHctsByTemp
      }
      val hcts: MutableList<Hct> = ArrayList(hctsByHue.orEmpty())
      hcts.add(input)
      val temperaturesComparator =
        Comparator<Hct> { a, b -> tempsByHct?.get(a)?.compareTo(tempsByHct?.get(b) ?: 0.0) ?: 0 }
      hcts.sortWith(temperaturesComparator)
      precomputedHctsByTemp = hcts
      return precomputedHctsByTemp
    }

  /** Keys of HCTs in getHctsByTemp, values of raw temperature.  */
  private val tempsByHct: Map<Hct, Double>?
    get() {
      if (precomputedTempsByHct != null) {
        return precomputedTempsByHct
      }
      val allHcts: MutableList<Hct> = ArrayList(hctsByHue.orEmpty())
      allHcts.add(input)
      val temperaturesByHct: MutableMap<Hct, Double> = HashMap()
      for (hct in allHcts) {
        temperaturesByHct[hct] = rawTemperature(hct)
      }
      precomputedTempsByHct = temperaturesByHct
      return precomputedTempsByHct
    }

  /** Warmest color with same chroma and tone as input.  */
  private val warmest: Hct
    get() = hctsByTemp!![hctsByTemp!!.size - 1]

  companion object {
    /**
     * Value representing cool-warm factor of a color. Values below 0 are considered cool, above,
     * warm.
     *
     *
     * Color science has researched emotion and harmony, which art uses to select colors. Warm-cool
     * is the foundation of analogous and complementary colors. See: - Li-Chen Ou's Chapter 19 in
     * Handbook of Color Psychology (2015). - Josef Albers' Interaction of Color chapters 19 and 21.
     *
     *
     * Implementation of Ou, Woodcock and Wright's algorithm, which uses Lab/LCH color space.
     * Return value has these properties:<br></br>
     * - Values below 0 are cool, above 0 are warm.<br></br>
     * - Lower bound: -9.66. Chroma is infinite. Assuming max of Lab chroma 130.<br></br>
     * - Upper bound: 8.61. Chroma is infinite. Assuming max of Lab chroma 130.
     */
    fun rawTemperature(color: Hct): Double {
      val lab: DoubleArray = ColorUtils.labFromArgb(color.toInt())
      val hue: Double = MathUtils.sanitizeDegreesDouble(
        MathUtils.toDegrees(
          atan2(
            lab[2], lab[1]
          )
        )
      )
      val chroma: Double = hypot(lab[1], lab[2])
      return (-0.5
          + (0.02
          * chroma.pow(1.07)
          * cos(MathUtils.toRadians(MathUtils.sanitizeDegreesDouble(hue - 50.0)))))
    }

    /** Determines if an angle is between two other angles, rotating clockwise.  */
    private fun isBetween(angle: Double, a: Double, b: Double): Boolean {
      return if (a < b) {
        angle in a..b
      } else a <= angle || angle <= b
    }
  }
}
