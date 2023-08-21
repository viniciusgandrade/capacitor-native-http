package br.com.gs3tecnologia.http.interceptors

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AddCookieInterceptor(context: Context) : Interceptor {
  // We're storing our stuff in a database made just for cookies called PREF_COOKIES.
  // I reccomend you do this, and don't change this default value.
  private val context: Context

  init {
    this.context = context
  }

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val builder = request.newBuilder()
    if (request.url.toString().contains("/oauth2") || request.url.toString().contains("/auth")) {
      return chain.proceed(request)
    }
    val preferences = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
      CookieUtil.PREF_COOKIES, HashSet<String>()
    ) as HashSet<String>
    var cookieOnly = ""
    for (cookie in preferences) {
      if (cookie.contains("JSESSIONID")) {
        cookieOnly = cookie;
        break;
      }
    }
    if (cookieOnly.isNotEmpty()) {
      builder.addHeader("Cookie", cookieOnly)
      Log.v(
        "OkHttp",
        "Adding Header: $cookieOnly"
      )
    }

    return chain.proceed(builder.build())
  }
}
