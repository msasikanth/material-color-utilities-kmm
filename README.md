# Material Color Utilities

KMM port of [Material Color Utilities Java package](https://github.com/material-foundation/material-color-utilities/tree/main/java)

### Dependency

```kotlin
implementation("dev.sasikanth:material-color-utilities:<version>")
```

### Usage

```kotlin
import dev.sasikanth.material.color.utilities.dynamiccolor.MaterialDynamicColors
import dev.sasikanth.material.color.utilities.hct.Hct
import dev.sasikanth.material.color.utilities.quantize.QuantizerCelebi
import dev.sasikanth.material.color.utilities.scheme.SchemeContent
import dev.sasikanth.material.color.utilities.score.Score

val seedColor = Score.score(
  QuantizerCelebi.quantize(bitmapPixels, 128)
)[0] // or specific AARRGGBB color int

val scheme = SchemeContent(
  sourceColorHct = Hct.fromInt(seedColor),
  isDark = true,
  contrastLevel = 0.0
)

val dynamicColors = MaterialDynamicColors()
// Create list of Material tokens you want to use/update
// based on the scheme
val tokens = mapOf(
  "primary" to dynamicColors.primary(),
  "secondary" to dynamicColors.secondary()
)

val colorsOutput = mutableMapOf<String, Int>()
for (token in tokens) {
  colorsOutput[token.key] = token.value.getArgb(scheme)
}

// Use/Update theme colors

```
