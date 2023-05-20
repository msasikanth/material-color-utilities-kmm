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

import dev.sasikanth.material.color.utilities.dynamiccolor.DynamicColor
import dev.sasikanth.material.color.utilities.dynamiccolor.TonePolarity

/**
 * Documents a constraint between two DynamicColors, in which their tones must have a certain
 * distance from each other.
 */
class ToneDeltaConstraint(val delta: Double, keepAway: DynamicColor, keepAwayPolarity: TonePolarity) {
  val keepAway: DynamicColor
  val keepAwayPolarity: TonePolarity

  /**
   * @param delta the difference in tone required
   * @param keepAway the color to distance in tone from
   * @param keepAwayPolarity whether the color to keep away from must be lighter, darker, or no
   * preference, in which case it should
   */
  init {
    this.keepAway = keepAway
    this.keepAwayPolarity = keepAwayPolarity
  }
}
