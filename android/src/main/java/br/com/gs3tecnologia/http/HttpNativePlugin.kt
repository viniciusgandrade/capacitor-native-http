package br.com.gs3tecnologia.http

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
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
  private lateinit var certPath: String

  @RequiresApi(Build.VERSION_CODES.O)
  @PluginMethod
  fun initialize(pluginCall: PluginCall) {
    val hosts = pluginCall.getArray("hostname")
    certPath = pluginCall.getString("certPath").toString()
    val cert = String(Base64.getEncoder().encode((loadPublicKey().encoded)))
    val builder = CertificatePinner.Builder()
    hosts.toList<String>().forEach { host ->
      if (host.indexOf("brbcard.com.br") == -1) {
        builder.add(host)
      } else {
        builder.add(host, "sha256/$cert")
      }
    }

    val certificatePinner: CertificatePinner = builder.build()

    httpClient = pluginCall.getInt("timeout", 30)?.let {
      OkHttpClient.Builder()
              .certificatePinner(certificatePinner)
              .connectTimeout(it.toLong(), TimeUnit.SECONDS)
              .readTimeout(it.toLong(), TimeUnit.SECONDS)
              .writeTimeout(it.toLong(), TimeUnit.SECONDS)
              .callTimeout(it.toLong(), TimeUnit.SECONDS)
              .addNetworkInterceptor(AddCookiesInterceptor(this.context))
              .addNetworkInterceptor(ReceivedCookiesInterceptor(this.context))
              .build()
    }!!
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
    val request = url.let { Request.Builder().url(it).headers(builder.build()).method("PUT", requestBody).build() }

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

    val request: Request? = if (jsonHeaders.getString("Content-Type", "") != "application/x-www-form-urlencoded") {
      val payload = data.toString()
      val requestBody = payload.toRequestBody()
      url?.let { Request.Builder().url(it).headers(builder.build()).method("POST", requestBody).build() }
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
      httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          e.printStackTrace()
          pluginCall.reject(e.message)
        }

        override fun onResponse(call: Call, response: Response) {
          val responseBody = response.body ?: throw IOException("Response body is null")
          val ret = JSObject()
          if (response.code >= 300) {
            var body = responseBody.string()
            if (body.isEmpty()) {
              body = "{\"msg\": \"Erro ao processar requisição\"}"
            }
            pluginCall.reject(body)
            return
          }
          ret.put("data", responseBody.string())
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
