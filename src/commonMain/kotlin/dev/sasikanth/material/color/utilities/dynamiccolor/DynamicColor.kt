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
package dev.sasikanth.material.color.utilities.dynamiccolor

import dev.sasikanth.material.color.utilities.contrast.Contrast
import dev.sasikanth.material.color.utilities.hct.Hct
import dev.sasikanth.material.color.utilities.palettes.TonalPalette
import dev.sasikanth.material.color.utilities.scheme.DynamicScheme
import dev.sasikanth.material.color.utilities.utils.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * A color that adjusts itself based on UI state, represented by DynamicScheme.
 *
 *
 * This color automatically adjusts to accommodate a desired contrast level, or other adjustments
 * such as differing in light mode versus dark mode, or what the theme is, or what the color that
 * produced the theme is, etc.
 *
 *
 * Colors without backgrounds do not change tone when contrast changes. Colors with backgrounds
 * become closer to their background as contrast lowers, and further when contrast increases.
 *
 *
 * Prefer the static constructors. They provide a much more simple interface, such as requiring
 * just a hexcode, or just a hexcode and a background.
 *
 *
 * Ultimately, each component necessary for calculating a color, adjusting it for a desired
 * contrast level, and ensuring it has a certain lightness/tone difference from another color, is
 * provided by a function that takes a DynamicScheme and returns a value. This ensures ultimate
 * flexibility, any desired behavior of a color for any design system, but it usually unnecessary.
 * See the default constructor for more information.
 */
// Prevent lint for Function.apply not being available on Android before API level 14 (4.0.1).
// "AndroidJdkLibsChecker" for Function, "NewApi" for Function.apply().
// A java_library Bazel rule with an Android constraint cannot skip these warnings without this
// annotation; another solution would be to create an android_library rule and supply
// AndroidManifest with an SDK set higher than 14.
class DynamicColor(
  hue: (DynamicScheme) -> Double,
  chroma: (DynamicScheme) -> Double,
  tone: (DynamicScheme) -> Double,
  opacity: ((DynamicScheme) -> Double)?,
  background: ((DynamicScheme) -> DynamicColor)?,
  toneMinContrast: (DynamicScheme) -> Double,
  toneMaxContrast: (DynamicScheme) -> Double,
  toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
) {
  val hue: (DynamicScheme) -> Double
  val chroma: (DynamicScheme) -> Double
  val tone: (DynamicScheme) -> Double
  val opacity: ((DynamicScheme) -> Double)?
  val background: ((DynamicScheme) -> DynamicColor)?
  val toneMinContrast: (DynamicScheme) -> Double
  val toneMaxContrast: (DynamicScheme) -> Double
  val toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
  private val hctCache: HashMap<DynamicScheme, Hct> = HashMap()

  /**
   * The base constructor for DynamicColor.
   *
   *
   * Functional arguments allow overriding without risks that come with subclasses. _Strongly_
   * prefer using one of the static convenience constructors. This class is arguably too flexible to
   * ensure it can support any scenario.
   *
   *
   * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
   * its background is principled and matches a11y guidance. That does not mean it's the desired
   * approach for _every_ design system, and every color pairing, always, in every case.
   *
   * @param hue given DynamicScheme, return the hue in HCT of the output color.
   * @param chroma given DynamicScheme, return chroma in HCT of the output color.
   * @param tone given DynamicScheme, return tone in HCT of the output color.
   * @param background given DynamicScheme, return the DynamicColor that is the background of this
   * DynamicColor. When this is provided, automated adjustments to lower and raise contrast are
   * made.
   * @param toneMinContrast given DynamicScheme, return tone in HCT/L* in L*a*b* this color should
   * be at minimum contrast. See toneMinContrastDefault for the default behavior, and strongly
   * consider using it unless you have strong opinions on a11y. The static constructors use it.
   * @param toneMaxContrast given DynamicScheme, return tone in HCT/L* in L*a*b* this color should
   * be at maximum contrast. See toneMaxContrastDefault for the default behavior, and strongly
   * consider using it unless you have strong opinions on a11y. The static constructors use it.
   * @param toneDeltaConstraint given DynamicScheme, return a ToneDeltaConstraint instance that
   * describes a requirement that this DynamicColor must always have some difference in tone/L*
   * from another DynamicColor.<br></br>
   * Unlikely to be useful unless a design system has some distortions where colors that don't
   * have a background/foreground relationship must have _some_ difference in tone, yet, not
   * enough difference to create meaningful contrast.
   */
  init {
    this.hue = hue
    this.chroma = chroma
    this.tone = tone
    this.opacity = opacity
    this.background = background
    this.toneMinContrast = toneMinContrast
    this.toneMaxContrast = toneMaxContrast
    this.toneDeltaConstraint = toneDeltaConstraint
  }

  fun getArgb(scheme: DynamicScheme): Int {
    val argb: Int = getHct(scheme).toInt()
    if (opacity == null) {
      return argb
    }
    val percentage: Double = opacity.invoke(scheme)
    val alpha: Int = MathUtils.clampInt(0, 255, round(percentage * 255).toInt())
    return argb and 0x00ffffff or (alpha shl 24)
  }

  fun getHct(scheme: DynamicScheme): Hct {
    val cachedAnswer: Hct? = hctCache.get(scheme)
    if (cachedAnswer != null) {
      return cachedAnswer
    }
    // This is crucial for aesthetics: we aren't simply the taking the standard color
    // and changing its tone for contrast. Rather, we find the tone for contrast, then
    // use the specified chroma from the palette to construct a new color.
    //
    // For example, this enables colors with standard tone of T90, which has limited chroma, to
    // "recover" intended chroma as contrast increases.
    val answer: Hct = Hct.from(hue.invoke(scheme), chroma.invoke(scheme), getTone(scheme))
    // NOMUTANTS--trivial test with onerous dependency injection requirement.
    if (hctCache.size > 4) {
      hctCache.clear()
    }
    // NOMUTANTS--trivial test with onerous dependency injection requirement.
    hctCache.put(scheme, answer)
    return answer
  }

  /** Returns the tone in HCT, ranging from 0 to 100, of the resolved color given scheme.  */
  fun getTone(scheme: DynamicScheme): Double {
    var answer: Double = tone.invoke(scheme)
    val decreasingContrast: Boolean = scheme.contrastLevel < 0.0
    if (scheme.contrastLevel != 0.0) {
      val startTone: Double = tone.invoke(scheme)
      val endTone: Double = if (decreasingContrast) toneMinContrast.invoke(scheme) else toneMaxContrast.invoke(scheme)
      val delta: Double = (endTone - startTone) * abs(scheme.contrastLevel)
      answer = delta + startTone
    }
    val bgDynamicColor: DynamicColor? = background?.invoke(scheme)
    var minRatio: Double = Contrast.RATIO_MIN
    var maxRatio: Double = Contrast.RATIO_MAX
    if (bgDynamicColor != null) {
      val bgHasBg = bgDynamicColor.background?.invoke(scheme) != null
      val standardRatio: Double = Contrast.ratioOfTones(tone.invoke(scheme), bgDynamicColor.tone.invoke(scheme))
      if (decreasingContrast) {
        val minContrastRatio: Double = Contrast.ratioOfTones(
          toneMinContrast.invoke(scheme), bgDynamicColor.toneMinContrast.invoke(scheme)
        )
        minRatio = if (bgHasBg) minContrastRatio else 1.0
        maxRatio = standardRatio
      } else {
        val maxContrastRatio: Double = Contrast.ratioOfTones(
          toneMaxContrast.invoke(scheme), bgDynamicColor.toneMaxContrast.invoke(scheme)
        )
        minRatio = if (bgHasBg) min(maxContrastRatio, standardRatio) else 1.0
        maxRatio = if (bgHasBg) max(maxContrastRatio, standardRatio) else 21.0
      }
    }
    val finalMinRatio = minRatio
    val finalMaxRatio = maxRatio
    val finalAnswer = answer
    answer = calculateDynamicTone(
      scheme,
      tone,
      { dynamicColor -> dynamicColor.getTone(scheme) },
      { _, _ -> finalAnswer },
      { _ -> bgDynamicColor },
      toneDeltaConstraint,
      { _ -> finalMinRatio }
    ) { _ -> finalMaxRatio }
    return answer
  }

  companion object {
    /**
     * Create a DynamicColor from a hex code.
     *
     *
     * Result has no background; thus no support for increasing/decreasing contrast for a11y.
     */
    fun fromArgb(argb: Int): DynamicColor {
      val hct: Hct = Hct.fromInt(argb)
      val palette: TonalPalette = TonalPalette.fromInt(argb)
      return fromPalette(
        { s -> palette },
        { s -> hct.getTone() })
    }

    /**
     * Create a DynamicColor from just a hex code.
     *
     *
     * Result has no background; thus cannot support increasing/decreasing contrast for a11y.
     *
     * @param argb A hex code.
     * @param tone Function that provides a tone given DynamicScheme. Useful for adjusting for dark
     * vs. light mode.
     */
    fun fromArgb(
      argb: Int,
      tone: (DynamicScheme) -> Double
    ): DynamicColor {
      return fromPalette({ s ->
        TonalPalette.fromInt(
          argb
        )
      }, tone)
    }

    /**
     * Create a DynamicColor.
     *
     *
     * If you don't understand HCT fully, or your design team doesn't, but wants support for
     * automated contrast adjustment, this method is _extremely_ useful: you can take a standard
     * design system expressed as hex codes, create DynamicColors corresponding to each color, and
     * then wire up backgrounds.
     *
     *
     * If the design system uses the same hex code on multiple backgrounds, define that in multiple
     * DynamicColors so that the background is accurate for each one. If you define a DynamicColor
     * with one background, and actually use it on another, DynamicColor can't guarantee contrast. For
     * example, if you use a color on both black and white, increasing the contrast on one necessarily
     * decreases contrast of the other.
     *
     * @param argb A hex code.
     * @param tone Function that provides a tone given DynamicScheme. (useful for dark vs. light mode)
     * @param background Function that provides background DynamicColor given DynamicScheme. Useful
     * for contrast, given a background, colors can adjust to increase/decrease contrast.
     */
    fun fromArgb(
      argb: Int,
      tone: (DynamicScheme) -> Double,
      background: (DynamicScheme) -> DynamicColor,
    ): DynamicColor {
      return fromPalette({ s ->
        TonalPalette.fromInt(
          argb
        )
      }, tone, background)
    }

    /**
     * Create a DynamicColor from:
     *
     * @param argb A hex code.
     * @param tone Function that provides a tone given DynamicScheme. (useful for dark vs. light mode)
     * @param background Function that provides background DynamicColor given DynamicScheme. Useful
     * for contrast, given a background, colors can adjust to increase/decrease contrast.
     * @param toneDeltaConstraint Function that provides a ToneDeltaConstraint given DynamicScheme.
     * Useful for ensuring lightness difference between colors that don't _require_ contrast or
     * have a formal background/foreground relationship.
     */
    fun fromArgb(
      argb: Int,
      tone: (DynamicScheme) -> Double,
      background: (DynamicScheme) -> DynamicColor,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
    ): DynamicColor {
      return fromPalette(
        { s -> TonalPalette.fromInt(argb) },
        tone,
        background,
        toneDeltaConstraint
      )
    }

    /**
     * Create a DynamicColor.
     *
     * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
     * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
     * a tonal palette, when contrast adjustments are made, intended chroma can be preserved. For
     * example, at T/L* 90, there is a significant limit to the amount of chroma. There is no
     * colorful red, a red that light is pink. By preserving the _intended_ chroma if lightness
     * lowers for contrast adjustments, the intended chroma is restored.
     * @param tone Function that provides a tone given DynamicScheme. (useful for dark vs. light mode)
     */
    fun fromPalette(
      palette: (DynamicScheme) -> TonalPalette,
      tone: (DynamicScheme) -> Double
    ): DynamicColor {
      return fromPalette(palette, tone, null, null)
    }

    /**
     * Create a DynamicColor.
     *
     * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
     * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
     * a tonal palette, when contrast adjustments are made, intended chroma can be preserved. For
     * example, at T/L* 90, there is a significant limit to the amount of chroma. There is no
     * colorful red, a red that light is pink. By preserving the _intended_ chroma if lightness
     * lowers for contrast adjustments, the intended chroma is restored.
     * @param tone Function that provides a tone given DynamicScheme. (useful for dark vs. light mode)
     * @param background Function that provides background DynamicColor given DynamicScheme. Useful
     * for contrast, given a background, colors can adjust to increase/decrease contrast.
     */
    fun fromPalette(
      palette: (DynamicScheme) -> TonalPalette,
      tone: (DynamicScheme) -> Double,
      background: ((DynamicScheme) -> DynamicColor)?
    ): DynamicColor {
      return fromPalette(palette, tone, background, null)
    }

    /**
     * Create a DynamicColor.
     *
     * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
     * defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
     * a tonal palette, when contrast adjustments are made, intended chroma can be preserved. For
     * example, at T/L* 90, there is a significant limit to the amount of chroma. There is no
     * colorful red, a red that light is pink. By preserving the _intended_ chroma if lightness
     * lowers for contrast adjustments, the intended chroma is restored.
     * @param tone Function that provides a tone given DynamicScheme. (useful for dark vs. light mode)
     * @param background Function that provides background DynamicColor given DynamicScheme. Useful
     * for contrast, given a background, colors can adjust to increase/decrease contrast.
     * @param toneDeltaConstraint Function that provides a ToneDeltaConstraint given DynamicScheme.
     * Useful for ensuring lightness difference between colors that don't _require_ contrast or
     * have a formal background/foreground relationship.
     */
    fun fromPalette(
      palette: (DynamicScheme) -> TonalPalette,
      tone: (DynamicScheme) -> Double,
      background: ((DynamicScheme) -> DynamicColor)?,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
    ): DynamicColor {
      return DynamicColor(
        { scheme -> palette.invoke(scheme).hue },
        { scheme -> palette.invoke(scheme).chroma },
        tone,
        null,
        background,
        { scheme ->
          toneMinContrastDefault(
            tone,
            background,
            scheme,
            toneDeltaConstraint
          )
        },
        { scheme ->
          toneMaxContrastDefault(
            tone,
            background,
            scheme,
            toneDeltaConstraint
          )
        },
        toneDeltaConstraint
      )
    }

    /**
     * The default algorithm for calculating the tone of a color at minimum contrast.<br></br>
     * If the original contrast ratio was >= 7.0, reach contrast 4.5.<br></br>
     * If the original contrast ratio was >= 3.0, reach contrast 3.0.<br></br>
     * If the original contrast ratio was < 3.0, reach that ratio.
     */
    fun toneMinContrastDefault(
      tone: (DynamicScheme) -> Double,
      background: ((DynamicScheme) -> DynamicColor)?,
      scheme: DynamicScheme,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
    ): Double {
      return calculateDynamicTone(
        scheme,
        tone,
        { c -> c.toneMinContrast.invoke(scheme) },
        { stdRatio, bgTone ->
          var answer: Double = tone.invoke(scheme)
          if (stdRatio >= Contrast.RATIO_70) {
            answer = contrastingTone(bgTone, Contrast.RATIO_45)
          } else if (stdRatio >= Contrast.RATIO_30) {
            answer = contrastingTone(bgTone, Contrast.RATIO_30)
          } else {
            val backgroundHasBackground =
              background?.invoke(scheme) != null && background.invoke(scheme).background != null && background.invoke(
                scheme
              ).background?.invoke(scheme) != null
            if (backgroundHasBackground) {
              answer = contrastingTone(bgTone, stdRatio)
            }
          }
          answer
        },
        background,
        toneDeltaConstraint,
        null,
        { standardRatio -> standardRatio })
    }

    /**
     * The default algorithm for calculating the tone of a color at maximum contrast.<br></br>
     * If the color's background has a background, reach contrast 7.0.<br></br>
     * If it doesn't, maintain the original contrast ratio.<br></br>
     *
     *
     * This ensures text on surfaces maintains its original, often detrimentally excessive,
     * contrast ratio. But, text on buttons can soften to not have excessive contrast.
     *
     *
     * Historically, digital design uses pure whites and black for text and surfaces. It's too much
     * of a jump at this point in history to introduce a dynamic contrast system _and_ insist that
     * text always had excessive contrast and should reach 7.0, it would deterimentally affect desire
     * to understand and use dynamic contrast.
     */
    fun toneMaxContrastDefault(
      tone: (DynamicScheme) -> Double,
      background: ((DynamicScheme) -> DynamicColor)?,
      scheme: DynamicScheme,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?
    ): Double {
      return calculateDynamicTone(
        scheme,
        tone,
        { c: DynamicColor -> c.toneMaxContrast.invoke(scheme) },
        { stdRatio, bgTone ->
          val backgroundHasBackground =
            background?.invoke(scheme) != null && background.invoke(scheme).background != null && background.invoke(
              scheme
            ).background?.invoke(scheme) != null
          if (backgroundHasBackground) {
            return@calculateDynamicTone contrastingTone(bgTone, Contrast.RATIO_70)
          } else {
            return@calculateDynamicTone contrastingTone(bgTone, max(Contrast.RATIO_70, stdRatio))
          }
        },
        background,
        toneDeltaConstraint,
        null,
        null
      )
    }

    /**
     * Core method for calculating a tone for under dynamic contrast.
     *
     *
     * It enforces important properties:<br></br>
     * #1. Desired contrast ratio is reached.<br></br>
     * As contrast increases from standard to max, the tones involved should always be at least the
     * standard ratio. For example, if a button is T90, and button text is T0, and the button is T0 at
     * max contrast, the button text cannot simply linearly interpolate from T0 to T100, or at some
     * point they'll both be at the same tone.
     *
     *
     * #2. Enable light foregrounds on midtones.<br></br>
     * The eye prefers light foregrounds on T50 to T60, possibly up to T70, but, contrast ratio 4.5
     * can't be reached with T100 unless the foreground is T50. Contrast ratio 4.5 is crucial, it
     * represents 'readable text', i.e. text smaller than ~40 dp / 1/4". So, if a tone is between T50
     * and T60, it is proactively changed to T49 to enable light foregrounds.
     *
     *
     * #3. Ensure tone delta with another color.<br></br>
     * In design systems, there may be colors that don't have a pure background/foreground
     * relationship, but, do require different tones for visual differentiation. ToneDeltaConstraint
     * models this requirement, and DynamicColor enforces it.
     */
    fun calculateDynamicTone(
      scheme: DynamicScheme,
      toneStandard: (DynamicScheme) -> Double,
      toneToJudge: (DynamicColor) -> Double,
      desiredTone: (Double, Double) -> Double,
      background: ((DynamicScheme) -> DynamicColor?)?,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?,
      minRatio: ((Double) -> Double)?,
      maxRatio: ((Double) -> Double)?
    ): Double {
      // Start with the tone with no adjustment for contrast.
      // If there is no background, don't perform any adjustment, return immediately.
      val toneStd: Double = toneStandard.invoke(scheme)
      var answer = toneStd
      val bgDynamic: DynamicColor = background?.invoke(scheme) ?: return answer
      val bgToneStd: Double = bgDynamic.tone.invoke(scheme)
      val stdRatio: Double = Contrast.ratioOfTones(toneStd, bgToneStd)

      // If there is a background, determine its tone after contrast adjustment.
      // Then, calculate the foreground tone that ensures the caller's desired contrast ratio is met.
      val bgTone: Double = toneToJudge.invoke(bgDynamic)
      val myDesiredTone: Double = desiredTone.invoke(stdRatio, bgTone)
      val currentRatio: Double = Contrast.ratioOfTones(bgTone, myDesiredTone)
      val minRatioRealized: Double = minRatio?.invoke(stdRatio) ?: Contrast.RATIO_MIN
      val maxRatioRealized: Double = maxRatio?.invoke(stdRatio) ?: Contrast.RATIO_MAX
      val desiredRatio: Double = MathUtils.clampDouble(minRatioRealized, maxRatioRealized, currentRatio)
      answer = if (desiredRatio == currentRatio) {
        myDesiredTone
      } else {
        contrastingTone(bgTone, desiredRatio)
      }

      // If the background has no background,  adjust the foreground tone to ensure that
      // it is dark enough to have a light foreground.
      if (bgDynamic.background?.invoke(scheme) == null) {
        answer = enableLightForeground(answer)
      }

      // If the caller has specified a constraint where it must have a certain  tone distance from
      // another color, enforce that constraint.
      answer = ensureToneDelta(answer, toneStd, scheme, toneDeltaConstraint, toneToJudge)
      return answer
    }

    fun ensureToneDelta(
      tone: Double,
      toneStandard: Double,
      scheme: DynamicScheme,
      toneDeltaConstraint: ((DynamicScheme) -> ToneDeltaConstraint)?,
      toneToDistanceFrom: (DynamicColor) -> Double
    ): Double {
      val constraint: ToneDeltaConstraint =
        (toneDeltaConstraint?.invoke(scheme)) ?: return tone
      val requiredDelta: Double = constraint.delta
      val keepAwayTone: Double = toneToDistanceFrom.invoke(constraint.keepAway)
      val delta: Double = abs(tone - keepAwayTone)
      if (delta >= requiredDelta) {
        return tone
      }
      return when (constraint.keepAwayPolarity) {
        TonePolarity.DARKER -> MathUtils.clampDouble(0.0, 100.0, keepAwayTone + requiredDelta)
        TonePolarity.LIGHTER -> MathUtils.clampDouble(0.0, 100.0, keepAwayTone - requiredDelta)
        TonePolarity.NO_PREFERENCE -> {
          val keepAwayToneStandard: Double = constraint.keepAway.tone.invoke(scheme)
          val preferLighten = toneStandard > keepAwayToneStandard
          val alterAmount: Double = abs(delta - requiredDelta)
          val lighten = if (preferLighten) tone + alterAmount <= 100.0 else tone < alterAmount
          if (lighten) tone + alterAmount else tone - alterAmount
        }
      }
    }

    /**
     * Given a background tone, find a foreground tone, while ensuring they reach a contrast ratio
     * that is as close to ratio as possible.
     */
    fun contrastingTone(bgTone: Double, ratio: Double): Double {
      val lighterTone: Double = Contrast.lighterUnsafe(bgTone, ratio)
      val darkerTone: Double = Contrast.darkerUnsafe(bgTone, ratio)
      val lighterRatio: Double = Contrast.ratioOfTones(lighterTone, bgTone)
      val darkerRatio: Double = Contrast.ratioOfTones(darkerTone, bgTone)
      val preferLighter = tonePrefersLightForeground(bgTone)
      return if (preferLighter) {
        // "Neglible difference" handles an edge case where the initial contrast ratio is high
        // (ex. 13.0), and the ratio passed to the function is that high ratio, and both the lighter
        // and darker ratio fails to pass that ratio.
        //
        // This was observed with Tonal Spot's On Primary Container turning black momentarily between
        // high and max contrast in light mode. PC's standard tone was T90, OPC's was T10, it was
        // light mode, and the contrast level was 0.6568521221032331.
        val negligibleDifference =
          abs(lighterRatio - darkerRatio) < 0.1 && lighterRatio < ratio && darkerRatio < ratio
        if (lighterRatio >= ratio || lighterRatio >= darkerRatio || negligibleDifference) {
          lighterTone
        } else {
          darkerTone
        }
      } else {
        if (darkerRatio >= ratio || darkerRatio >= lighterRatio) darkerTone else lighterTone
      }
    }

    /**
     * Adjust a tone down such that white has 4.5 contrast, if the tone is reasonably close to
     * supporting it.
     */
    fun enableLightForeground(tone: Double): Double {
      return if (tonePrefersLightForeground(tone) && !toneAllowsLightForeground(tone)) {
        49.0
      } else tone
    }

    /**
     * People prefer white foregrounds on ~T60-70. Observed over time, and also by Andrew Somers
     * during research for APCA.
     *
     *
     * T60 used as to create the smallest discontinuity possible when skipping down to T49 in order
     * to ensure light foregrounds.
     *
     *
     * Since `tertiaryContainer` in dark monochrome scheme requires a tone of 60, it should not be
     * adjusted. Therefore, 60 is excluded here.
     */
    fun tonePrefersLightForeground(tone: Double): Boolean {
      return round(tone) < 60
    }

    /** Tones less than ~T50 always permit white at 4.5 contrast.  */
    fun toneAllowsLightForeground(tone: Double): Boolean {
      return round(tone) <= 49
    }
  }
}
