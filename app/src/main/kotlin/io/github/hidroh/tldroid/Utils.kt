package io.github.hidroh.tldroid

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.StyleRes
import okio.Okio
import java.io.IOException
import java.io.InputStream

internal object Utils {
  private const val KEY_THEME = "pref:theme"
  private const val VAL_THEME_SOLARIZED = "theme:solarized"
  private const val VAL_THEME_AFTERGLOW = "theme:afterglow"
  private const val VAL_THEME_TOMORROW = "theme:tomorrow"

  @Throws(IOException::class)
  fun readUtf8(inputStream: InputStream): String {
    return Okio.buffer(Okio.source(inputStream)).readUtf8()
  }

  fun saveTheme(context: Context, @StyleRes themeRes: Int) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(KEY_THEME, when (themeRes) {
          R.style.AppTheme_Afterglow -> VAL_THEME_AFTERGLOW
          R.style.AppTheme_Tomorrow -> VAL_THEME_TOMORROW
          else -> VAL_THEME_SOLARIZED
        })
        .apply()
  }

  @StyleRes
  fun loadTheme(context: Context): Int {
    val theme = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_THEME, VAL_THEME_SOLARIZED)
    return when (theme) {
      VAL_THEME_AFTERGLOW -> R.style.AppTheme_Afterglow
      VAL_THEME_TOMORROW -> R.style.AppTheme_Tomorrow
      else -> R.style.AppTheme
    }
  }
}
