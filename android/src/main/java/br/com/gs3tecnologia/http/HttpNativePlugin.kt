package br.com.gs3tecnologia.http;

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
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URI
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

@CapacitorPlugin(name = "HttpNative")
class HttpNativePlugin : Plugin() {

  private lateinit var httpClient: OkHttpClient;
  private lateinit var certPath: String

  @RequiresApi(Build.VERSION_CODES.O)
  @PluginMethod
  fun initialize(pluginCall: PluginCall) {
    val hostname = pluginCall.getString("hostname").toString()
    certPath = pluginCall.getString("certPath").toString()
    val cert = String(Base64.getEncoder().encode((loadPublicKey().encoded)))
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
      .add(hostname, "sha256/$cert")
      .build()
    httpClient = OkHttpClient.Builder()
      .certificatePinner(certificatePinner)
      .addNetworkInterceptor(AddCookiesInterceptor(this.context))
      .addNetworkInterceptor(ReceivedCookiesInterceptor(this.context))
      .build()
  }

  @PluginMethod
  fun doGet(pluginCall: PluginCall) {
    pluginCall.setKeepAlive(true)
    var url = pluginCall.getString("url").toString()
    val jsonHeaders = pluginCall.getObject("headers")
    val params = pluginCall.getObject("params")
    var encodeUrlParams = pluginCall.getBoolean("encodeUrlParams", false)
    val keys = params.keys();
    val urlQueryBuilder = StringBuilder()
    val builder = buildHeaders(jsonHeaders);

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
        if (urlQueryBuilder.length > 0) {
          urlQueryBuilder.append("&")
        }
        urlQueryBuilder.append(value)
      } catch (e: JSONException) {
        if (urlQueryBuilder.length > 0) {
          urlQueryBuilder.append("&")
        }
        urlQueryBuilder.append(key).append("=").append(params.getString(key))
      }
    }
    val urlQuery = urlQueryBuilder.toString()
    val uri = url.toUri()
    if (encodeUrlParams!!) {
      val encodedUri = URI(uri.scheme, uri.authority, uri.path, urlQuery, uri.fragment)
      url = encodedUri.toURL().toString()
    } else {
      val unEncodedUrlString: String =
        (uri.scheme + "://" + uri.authority + uri.path).toString() + (if (urlQuery != "") "?$urlQuery" else "") + if (uri.fragment != null) uri.fragment else ""
      url = URL(unEncodedUrlString).toString()
    }

    val request =
      url?.let { Request.Builder().url(it).headers(builder.build()).method("GET", null).build() }

    makeRequest(request, pluginCall);
  }

  @PluginMethod
  fun doPost(pluginCall: PluginCall) {
    pluginCall.setKeepAlive(true)
    val jsonHeaders = pluginCall.getObject("headers")
    val builder: Headers.Builder = buildHeaders(jsonHeaders)

    val url = pluginCall.getString("url")

    val data = pluginCall.getObject("data")
    val requestBodyString = pluginCall.getString("data")
    var request: Request? = null;

    if (data != null && data.length() > 0) {
      val payload = data.toString();
      val requestBody = payload.toRequestBody()
      request = url?.let { Request.Builder().url(it).headers(builder.build()).method("POST", requestBody).build() }
    } else {
      val formBuilder = FormBody.Builder();
      val params = requestBodyString?.split("&");
      if (params != null) {
        for (param in params) {
          formBuilder.addEncoded(param.split("=")[0], param.split("=")[1]);
        }
      }
      val formBody: RequestBody = formBuilder.build()
      request = url?.let { Request.Builder().url(it).headers(builder.build()).post(formBody).build() }
    }

    makeRequest(request, pluginCall)
  }

  private fun makeRequest(request: Request?, pluginCall: PluginCall) {
    if (request != null) {
      httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          e.printStackTrace();
          pluginCall.reject(e.message)
        }

        override fun onResponse(call: Call, response: Response) {
          val responseBody = response.body ?: throw IOException("Response body is null")
          val ret = JSObject()
          ret.put("data", responseBody.string())
          pluginCall.resolve(ret)
        }
      })
    } else {
      pluginCall.reject("Erro ao processar requisição")
    }
  }

  private fun buildHeaders(jsonHeaders: JSObject): Headers.Builder {
    val keys: Iterator<String> = jsonHeaders.keys()
    val builder: Headers.Builder = Headers.Builder()
    while (keys.hasNext()) {
      val key = keys.next()
      builder.add(key, jsonHeaders.get(key).toString())
    }

    builder.add("User-Agent", System.getProperty("http.agent"));
    return builder
  }

  private fun loadPublicKey(): X509Certificate {
    val ins = activity.application.assets.open(certPath).buffered()
    val cf = CertificateFactory.getInstance("X.509")
    val cert = cf.generateCertificate(ins) as X509Certificate
    return cert
  }
}
