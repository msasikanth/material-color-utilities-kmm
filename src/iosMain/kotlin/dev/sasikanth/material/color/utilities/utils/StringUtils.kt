package dev.sasikanth.material.color.utilities.utils

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun String.format(red: Int, green: Int, blue: Int): String {
  return NSString.stringWithFormat(format = this, red, green, blue)
}
