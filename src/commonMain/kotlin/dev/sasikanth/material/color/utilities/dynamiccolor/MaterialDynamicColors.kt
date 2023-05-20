/*
 * Copyright 2023 Google LLC
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

import dev.sasikanth.material.color.utilities.dislike.DislikeAnalyzer
import dev.sasikanth.material.color.utilities.hct.Hct
import dev.sasikanth.material.color.utilities.hct.ViewingConditions
import dev.sasikanth.material.color.utilities.scheme.DynamicScheme
import dev.sasikanth.material.color.utilities.scheme.Variant
import kotlin.math.abs
import kotlin.math.max

/** Named colors, otherwise known as tokens, or roles, in the Material Design system.  */ // Prevent lint for Function.apply not being available on Android before API level 14 (4.0.1).
// "AndroidJdkLibsChecker" for Function, "NewApi" for Function.apply().
// A java_library Bazel rule with an Android constraint cannot skip these warnings without this
// annotation; another solution would be to create an android_library rule and supply
// AndroidManifest with an SDK set higher than 14.
class MaterialDynamicColors {

  fun highestSurface(s: DynamicScheme): DynamicColor {
    return if (s.isDark) surfaceBright() else surfaceDim()
  }

  fun background(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 6.0 else 98.0 }
  }

  fun onBackground(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralPalette }, { s -> if (s.isDark) 90.0 else 10.0 }) { s -> background() }
  }

  fun surface(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 6.0 else 98.0 }
  }

  fun inverseSurface(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 90.0 else 20.0 }
  }

  fun surfaceBright(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 24.0 else 98.0 }
  }

  fun surfaceDim(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 6.0 else 87.0 }
  }

  fun surfaceContainerLowest(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 4.0 else 100.0 }
  }

  fun surfaceContainerLow(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 10.0 else 96.0 }
  }

  fun surfaceContainer(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 12.0 else 94.0 }
  }

  fun surfaceContainerHigh(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 17.0 else 92.0 }
  }

  fun surfaceContainerHighest(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 22.0 else 90.0 }
  }

  fun onSurface(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralPalette }, { s -> if (s.isDark) 90.0 else 10.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun inverseOnSurface(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralPalette }, { s -> if (s.isDark) 20.0 else 95.0 }) { s -> inverseSurface() }
  }

  fun surfaceVariant(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralVariantPalette }) { s -> if (s.isDark) 30.0 else 90.0 }
  }

  fun onSurfaceVariant(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralVariantPalette }, { s -> if (s.isDark) 80.0 else 30.0 }) { s -> surfaceVariant() }
  }

  fun outline(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralVariantPalette }, { s -> if (s.isDark) 60.0 else 50.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun outlineVariant(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.neutralVariantPalette }, { s -> if (s.isDark) 30.0 else 80.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun shadow(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> 0.0 }
  }

  fun scrim(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> 0.0 }
  }

  fun surfaceTint(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.primaryPalette }) { s -> if (s.isDark) 80.0 else 40.0 }
  }

  fun primaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isFidelity(s)) {
          return@fromPalette performAlbers(s.sourceColorHct, s)
        }
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 85.0 else 25.0
        }
        if (s.isDark) 30.0 else 90.0
      }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onPrimaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isFidelity(s)) {
          return@fromPalette DynamicColor.contrastingTone(primaryContainer().tone.invoke(s), 4.5)
        }
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 0.0 else 100.0
        }
        if (s.isDark) 90.0 else 10.0
      },
      { s -> primaryContainer() },
      null
    )
  }

  fun primary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 100.0 else 0.0
        }
        if (s.isDark) 80.0 else 40.0
      }, { s: DynamicScheme -> highestSurface(s) }
    ) { s ->
      ToneDeltaConstraint(
        CONTAINER_ACCENT_TONE_DELTA,
        primaryContainer(),
        if (s.isDark) TonePolarity.DARKER else TonePolarity.LIGHTER
      )
    }
  }

  fun inversePrimary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette }, { s -> if (s.isDark) 40.0 else 80.0 }) { s -> inverseSurface() }
  }

  fun onPrimary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 10.0 else 90.0
        }
        if (s.isDark) 20.0 else 100.0
      }
    ) { s -> primary() }
  }

  fun secondaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 30.0 else 85.0
        }
        val initialTone = if (s.isDark) 30.0 else 90.0
        if (!isFidelity(s)) {
          return@fromPalette initialTone
        }
        var answer = findDesiredChromaByTone(
          s.secondaryPalette.hue,
          s.secondaryPalette.chroma,
          initialTone,
          !s.isDark
        )
        answer = performAlbers(s.secondaryPalette.getHct(answer), s)
        answer
      }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onSecondaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s ->
        if (!isFidelity(s)) {
          return@fromPalette if (s.isDark) 90.0 else 10.0
        }
        DynamicColor.contrastingTone(secondaryContainer().tone.invoke(s), 4.5)
      }
    ) { s -> secondaryContainer() }
  }

  fun secondary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s -> if (s.isDark) 80.0 else 40.0 }, { s: DynamicScheme -> highestSurface(s) }
    ) { s ->
      ToneDeltaConstraint(
        CONTAINER_ACCENT_TONE_DELTA,
        secondaryContainer(),
        if (s.isDark) TonePolarity.DARKER else TonePolarity.LIGHTER
      )
    }
  }

  fun onSecondary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 10.0 else 100.0
        }
        if (s.isDark) 20.0 else 100.0
      }
    ) { s -> secondary() }
  }

  fun tertiaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 60.0 else 49.0
        }
        if (!isFidelity(s)) {
          return@fromPalette if (s.isDark) 30.0 else 90.0
        }
        val albersTone = performAlbers(s.tertiaryPalette.getHct(s.sourceColorHct.getTone()), s)
        val proposedHct: Hct = s.tertiaryPalette.getHct(albersTone)
        DislikeAnalyzer.fixIfDisliked(proposedHct).getTone()
      }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onTertiaryContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 0.0 else 100.0
        }
        if (!isFidelity(s)) {
          return@fromPalette if (s.isDark) 90.0 else 10.0
        }
        DynamicColor.contrastingTone(tertiaryContainer().tone.invoke(s), 4.5)
      }
    ) { s -> tertiaryContainer() }
  }

  fun tertiary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 90.0 else 25.0
        }
        if (s.isDark) 80.0 else 40.0
      }, { s: DynamicScheme -> highestSurface(s) }
    ) { s ->
      ToneDeltaConstraint(
        CONTAINER_ACCENT_TONE_DELTA,
        tertiaryContainer(),
        if (s.isDark) TonePolarity.DARKER else TonePolarity.LIGHTER
      )
    }
  }

  fun onTertiary(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 10.0 else 90.0
        }
        if (s.isDark) 20.0 else 100.0
      }
    ) { s -> tertiary() }
  }

  fun errorContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.errorPalette }, { s -> if (s.isDark) 30.0 else 90.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onErrorContainer(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.errorPalette }, { s -> if (s.isDark) 90.0 else 10.0 }) { s -> errorContainer() }
  }

  fun error(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.errorPalette },
      { s -> if (s.isDark) 80.0 else 40.0 }, { s: DynamicScheme -> highestSurface(s) }
    ) { s ->
      ToneDeltaConstraint(
        CONTAINER_ACCENT_TONE_DELTA,
        errorContainer(),
        if (s.isDark) TonePolarity.DARKER else TonePolarity.LIGHTER
      )
    }
  }

  fun onError(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.errorPalette }, { s -> if (s.isDark) 20.0 else 100.0 }) { s -> error() }
  }

  fun primaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 100.0 else 10.0
        }
        90.0
      }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun primaryFixedDim(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 90.0 else 20.0
        }
        80.0
      }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onPrimaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 10.0 else 90.0
        }
        10.0
      }
    ) { s -> primaryFixedDim() }
  }

  fun onPrimaryFixedVariant(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.primaryPalette },
      { s ->
        if (isMonochrome(s)) {
          return@fromPalette if (s.isDark) 30.0 else 70.0
        }
        30.0
      }
    ) { s -> primaryFixedDim() }
  }

  fun secondaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s -> if (isMonochrome(s)) 80.0 else 90.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun secondaryFixedDim(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s -> if (isMonochrome(s)) 70.0 else 80.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onSecondaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette }, { s -> 10.0 }) { s -> secondaryFixedDim() }
  }

  fun onSecondaryFixedVariant(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.secondaryPalette },
      { s -> if (isMonochrome(s)) 25.0 else 30.0 }
    ) { s -> secondaryFixedDim() }
  }

  fun tertiaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s -> if (isMonochrome(s)) 40.0 else 90.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun tertiaryFixedDim(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette },
      { s -> if (isMonochrome(s)) 30.0 else 80.0 }) { s: DynamicScheme -> highestSurface(s) }
  }

  fun onTertiaryFixed(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette }, { s -> if (isMonochrome(s)) 90.0 else 10.0 }) { s -> tertiaryFixedDim() }
  }

  fun onTertiaryFixedVariant(): DynamicColor {
    return DynamicColor.fromPalette(
      { s -> s.tertiaryPalette }, { s -> if (isMonochrome(s)) 70.0 else 30.0 }) { s -> tertiaryFixedDim() }
  }

  /**
   * These colors were present in Android framework before Android U, and used by MDC controls. They
   * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
   * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
   * and those won't adjust for contrast, avoiding issues.
   *
   *
   * * For example, if the same color is on a white background _and_ black background, there's no
   * way to increase contrast with either without losing contrast with the other.
   */
  // colorControlActivated documented as colorAccent in M3 & GM3.
  // colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
  // Android used Material's Container as Primary/Secondary/Tertiary at launch.
  // Therefore, this is a duplicated version of Primary Container.

  fun controlActivated(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.primaryPalette }, { s -> if (s.isDark) 30.0 else 90.0 }, null)
  }

  // colorControlNormal documented as textColorSecondary in M3 & GM3.
  // In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
  // which is Neutral Variant T30/80 in light/dark.

  fun controlNormal(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralVariantPalette }) { s -> if (s.isDark) 80.0 else 30.0 }
  }

  // colorControlHighlight documented, in both M3 & GM3:
  // Light mode: #1f000000 dark mode: #33ffffff.
  // These are black and white with some alpha.
  // 1F hex = 31 decimal; 31 / 255 = 12% alpha.
  // 33 hex = 51 decimal; 51 / 255 = 20% alpha.
  // DynamicColors do not support alpha currently, and _may_ not need it for this use case,
  // depending on how MDC resolved alpha for the other cases.
  // Returning black in dark mode, white in light mode.

  fun controlHighlight(): DynamicColor {
    return DynamicColor(
      { s -> 0.0 },
      { s -> 0.0 },
      { s -> if (s.isDark) 100.0 else 0.0 },
      { s -> if (s.isDark) 0.20 else 0.12 },
      null,
      { scheme -> DynamicColor.toneMinContrastDefault({ s -> if (s.isDark) 100.0 else 0.0 }, null, scheme, null) },
      { scheme -> DynamicColor.toneMaxContrastDefault({ s -> if (s.isDark) 100.0 else 0.0 }, null, scheme, null) },
      null
    )
  }

  // textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.

  fun textPrimaryInverse(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 10.0 else 90.0 }
  }

  // textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
  // NV30/NV80

  fun textSecondaryAndTertiaryInverse(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralVariantPalette }) { s -> if (s.isDark) 30.0 else 80.0 }
  }

  // textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90

  fun textPrimaryInverseDisableOnly(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 10.0 else 90.0 }
  }

  // textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
  // in both M3 & GM3, as N10/N90

  fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 10.0 else 90.0 }
  }

  // textColorHintInverse documented, in both M3 & GM3, as N10/N90

  fun textHintInverse(): DynamicColor {
    return DynamicColor.fromPalette({ s -> s.neutralPalette }) { s -> if (s.isDark) 10.0 else 90.0 }
  }

  companion object {
    const val CONTAINER_ACCENT_TONE_DELTA = 15.0

    private fun viewingConditionsForAlbers(scheme: DynamicScheme): ViewingConditions {
      return ViewingConditions.defaultWithBackgroundLstar(if (scheme.isDark) 30.0 else 80.0)
    }

    private fun isFidelity(scheme: DynamicScheme): Boolean {
      return scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT
    }

    private fun isMonochrome(scheme: DynamicScheme): Boolean {
      return scheme.variant == Variant.MONOCHROME
    }

    fun findDesiredChromaByTone(
      hue: Double, chroma: Double, tone: Double, byDecreasingTone: Boolean
    ): Double {
      var answer = tone
      var closestToChroma: Hct = Hct.from(hue, chroma, tone)
      if (closestToChroma.getChroma() < chroma) {
        var chromaPeak: Double = closestToChroma.getChroma()
        while (closestToChroma.getChroma() < chroma) {
          answer += if (byDecreasingTone) -1.0 else 1.0
          val potentialSolution: Hct = Hct.from(hue, chroma, answer)
          if (chromaPeak > potentialSolution.getChroma()) {
            break
          }
          if (abs(potentialSolution.getChroma() - chroma) < 0.4) {
            break
          }
          val potentialDelta: Double = abs(potentialSolution.getChroma() - chroma)
          val currentDelta: Double = abs(closestToChroma.getChroma() - chroma)
          if (potentialDelta < currentDelta) {
            closestToChroma = potentialSolution
          }
          chromaPeak = max(chromaPeak, potentialSolution.getChroma())
        }
      }
      return answer
    }

    fun performAlbers(prealbers: Hct, scheme: DynamicScheme): Double {
      val albersd: Hct = prealbers.inViewingConditions(viewingConditionsForAlbers(scheme))
      return if (DynamicColor.tonePrefersLightForeground(prealbers.getTone())
        && !DynamicColor.toneAllowsLightForeground(albersd.getTone())
      ) {
        DynamicColor.enableLightForeground(prealbers.getTone())
      } else {
        DynamicColor.enableLightForeground(albersd.getTone())
      }
    }
  }
}
