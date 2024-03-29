package br.com.gs3tecnologia.http.interceptors

import android.content.Context
import androidx.preference.PreferenceManager
import br.com.gs3tecnologia.http.interceptors.CookieUtil
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException


class ReceivedCookiesInterceptor(context: Context) : Interceptor {
  private val context: Context

  init {
    this.context = context
  }

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalResponse: Response = chain.proceed(chain.request())
    if (!originalResponse.headers("Set-Cookie").isEmpty()) {
      val cookies = PreferenceManager.getDefaultSharedPreferences(context)
        .getStringSet(CookieUtil.PREF_COOKIES, HashSet()) as HashSet<String>?
      for (header in originalResponse.headers("Set-Cookie")) {
        cookies!!.add(header)
      }
      val memes = PreferenceManager.getDefaultSharedPreferences(context).edit()
      memes.putStringSet(CookieUtil.PREF_COOKIES, cookies).apply()
      memes.commit()
    }
    return originalResponse
  }
}
