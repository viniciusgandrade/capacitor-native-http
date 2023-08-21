package br.com.gs3tecnologia.http

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import br.com.gs3tecnologia.http.interceptors.AddCookieInterceptor
import br.com.gs3tecnologia.http.interceptors.ReceivedCookiesInterceptor
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit

@CapacitorPlugin(name = "HttpNative")
class HttpNativePlugin : Plugin() {

  private lateinit var httpClient: OkHttpClient
  private lateinit var unsafeHttpClient: OkHttpClient
  private lateinit var certPath: String
  private lateinit var hostname: String

  @RequiresApi(Build.VERSION_CODES.O)
  @PluginMethod
  fun initialize(pluginCall: PluginCall) {
    hostname = pluginCall.getString("hostname", "").toString().replace("*.", "")
    certPath = pluginCall.getString("certPath").toString()

    val cert = String(Base64.getEncoder().encode((loadPublicKey().encoded)))
    val builder = CertificatePinner.Builder()
    if (hostname != null) {
      builder.add(hostname, "sha256/$cert")
      val certificatePinner: CertificatePinner = builder.build()
      httpClient = pluginCall.getInt("timeout", 30)?.let {
        OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .connectTimeout(it.toLong(), TimeUnit.SECONDS)
                .readTimeout(it.toLong(), TimeUnit.SECONDS)
                .writeTimeout(it.toLong(), TimeUnit.SECONDS)
                .callTimeout(it.toLong(), TimeUnit.SECONDS)
                .build()
      }!!
    }
    unsafeHttpClient = pluginCall.getInt("timeout", 30)?.let {
      UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder()
              .readTimeout(it.toLong(), TimeUnit.SECONDS)
              .writeTimeout(it.toLong(), TimeUnit.SECONDS)
              .callTimeout(it.toLong(), TimeUnit.SECONDS)
              .build()
    }!!

    httpClient = httpClient.newBuilder().addInterceptor(AddCookieInterceptor(context)).build()
    httpClient = httpClient.newBuilder().addInterceptor(ReceivedCookiesInterceptor(context)).build()

    PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()
    pluginCall.resolve()
  }

  @PluginMethod
  fun clearCookie(pluginCall: PluginCall) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()
    pluginCall.resolve()
  }

  @PluginMethod
  fun request(pluginCall: PluginCall) {
    pluginCall.setKeepAlive(true)
    when (val method = pluginCall.getString("method").toString()) {
      "GET" -> doGet(pluginCall)
      "POST" -> doPost(pluginCall)
      "PUT" -> doPut(pluginCall)
      else -> throw Exception("Método não implementado: $method")
    }
  }

  private fun doPut(pluginCall: PluginCall) {
    val url = pluginCall.getString("url").toString()
    val jsonHeaders = pluginCall.getObject("headers")
    val data = pluginCall.getObject("data")
    val builder = buildHeaders(jsonHeaders)

    val payload = data.toString()
    val requestBody = payload.toRequestBody()
    val request = url.let {
      Request.Builder().url(it).headers(builder.build()).method("PUT", requestBody).build()
    }

    makeRequest(request, pluginCall)
  }

  private fun doGet(pluginCall: PluginCall) {
    var url = pluginCall.getString("url").toString()
    val jsonHeaders = pluginCall.getObject("headers")
    val params = pluginCall.getObject("params")
    val encodeUrlParams = jsonHeaders.getString("Content-Type", "")
    val keys = params.keys()
    val urlQueryBuilder = StringBuilder()
    val builder = buildHeaders(jsonHeaders)

    while (keys.hasNext()) {
      val key = keys.next()

      try {
        val value = StringBuilder()
        val arr: JSONArray = params.getJSONArray(key)
        for (x in 0 until arr.length()) {
          value.append(key).append("=").append(arr.getString(x))
          if (x != arr.length() - 1) {
            value.append("&")
          }
        }
        if (urlQueryBuilder.isNotEmpty()) {
          urlQueryBuilder.append("&")
        }
        urlQueryBuilder.append(value)
      } catch (e: JSONException) {
        if (urlQueryBuilder.isNotEmpty()) {
          urlQueryBuilder.append("&")
        }
        urlQueryBuilder.append(key).append("=").append(params.getString(key))
      }
    }
    val urlQuery = urlQueryBuilder.toString()
    val uri = url.toUri()
    url = if (encodeUrlParams == "application/x-www-form-urlencoded") {
      val encodedUri = URI(uri.scheme, uri.authority, uri.path, urlQuery, uri.fragment)
      encodedUri.toURL().toString()
    } else {
      val unEncodedUrlString: String =
              (uri.scheme + "://" + uri.authority + uri.path) + (if (urlQuery != "") "?$urlQuery" else "") + if (uri.fragment != null) uri.fragment else ""
      URL(unEncodedUrlString).toString()
    }

    val request =
            url.let { Request.Builder().url(it).headers(builder.build()).method("GET", null).build() }

    makeRequest(request, pluginCall)
  }

  private fun doPost(pluginCall: PluginCall) {
    val jsonHeaders = pluginCall.getObject("headers")
    val builder: Headers.Builder = buildHeaders(jsonHeaders)

    val url = pluginCall.getString("url")

    val data = pluginCall.getObject("data")

    val request: Request? =
            if (jsonHeaders.getString("Content-Type", "") != "application/x-www-form-urlencoded") {
              val payload = data.toString()
              val requestBody = payload.toRequestBody()
              url?.let {
                Request.Builder().url(it).headers(builder.build()).method("POST", requestBody).build()
              }
            } else {
              val formBuilder = FormBody.Builder()
              val keys = data.keys()
              for (key in keys) {
                data.getString(key)?.let { formBuilder.add(key, it) }
              }
              val formBody: RequestBody = formBuilder.build()
              url?.let { Request.Builder().url(it).headers(builder.build()).post(formBody).build() }
            }

    makeRequest(request, pluginCall)
  }

  private fun makeRequest(request: Request?, pluginCall: PluginCall) {
    if (request != null) {
      var call: Call = if (request.url.host.contains(hostname)) {
        httpClient.newCall(request)
      } else {
        unsafeHttpClient.newCall(request)
      }
      call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          e.printStackTrace()
          val jsonObject = JSONObject()
          jsonObject.put("data", e.message)
          jsonObject.put("statusCode", 0)
        }

        override fun onResponse(call: Call, response: Response) {
          val responseBody = response.body ?: throw IOException("Response body is null")
          val ret = JSObject()
          if (response.code >= 300) {
            var body = responseBody.string()
            if (body.isEmpty()) {
              val ret = JSONObject()
              ret.put("msg", "Erro ao processar requisição")
              val jsonObject = JSONObject()
              jsonObject.put("data", ret)
              jsonObject.put("statusCode", response.code)
              pluginCall.reject(jsonObject.toString())
              return
            }
            val ret = JSONObject(body)
            val jsonObject = JSONObject(body)
            jsonObject.put("data", ret)
            jsonObject.put("statusCode", response.code)
            pluginCall.reject(jsonObject.toString())
            return
          }
          ret.put("data", responseBody.string())
          val jsonObject = JSONObject()

          for (i in 0 until response.headers.size) {
            jsonObject.putOpt(response.headers.name(i), response.headers.value(i))
          }
          ret.put("headers", jsonObject.toString())
          pluginCall.resolve(ret)
        }
      })
    } else {
      pluginCall.reject("Erro ao processar requisição")
    }
  }

  private fun buildHeaders(jsonHeaders: JSObject): Headers.Builder {
    val encodeUrlParams = jsonHeaders.getString("Content-Type", "application/json")
    jsonHeaders.put("Content-Type", encodeUrlParams)
    jsonHeaders.put("Accept", "application/json")
    val keys: Iterator<String> = jsonHeaders.keys()
    val builder: Headers.Builder = Headers.Builder()
    while (keys.hasNext()) {
      val key = keys.next()
      builder.add(key, jsonHeaders.get(key).toString())
    }

    System.getProperty("http.agent")?.let { builder.add("User-Agent", it) }
    return builder
  }

  private fun loadPublicKey(): X509Certificate {
    val ins = activity.application.assets.open(certPath).buffered()
    val cf = CertificateFactory.getInstance("X.509")
    return cf.generateCertificate(ins) as X509Certificate
  }
}
