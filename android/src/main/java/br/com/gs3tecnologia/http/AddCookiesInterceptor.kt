package br.com.gs3tecnologia.http

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AddCookiesInterceptor(context: Context, onlyCookie: String = "") : Interceptor {
    // We're storing our stuff in a database made just for cookies called PREF_COOKIES.
    // I reccomend you do this, and don't change this default value.
    private val context: Context
    private val onlyCookie: String

    init {
        this.onlyCookie = onlyCookie
        this.context = context
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                PREF_COOKIES, HashSet<String>()
        ) as HashSet<String>
        if (onlyCookie.isEmpty()) {
            for (cookie in preferences) {
                builder.addHeader("Cookie", cookie)
                Log.v(
                        "OkHttp",
                        "Adding Header: $cookie"
                )
            }
        } else {
            var cookieOnly = ""
            for (cookie in preferences) {
                if (cookie.contains(onlyCookie)) {
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
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val PREF_COOKIES = "PREF_COOKIES"
    }
}
