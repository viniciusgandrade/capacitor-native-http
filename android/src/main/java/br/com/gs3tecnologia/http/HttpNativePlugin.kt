package br.com.gs3tecnologia.http;

import android.os.Build
import androidx.annotation.RequiresApi
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit

@CapacitorPlugin(name = "HttpNative")
class HttpNativePlugin : Plugin() {
  @RequiresApi(Build.VERSION_CODES.O)
  @PluginMethod
  fun doPost(pluginCall: PluginCall) {
    pluginCall.setKeepAlive(true)
    val url = pluginCall.getString("url")
    val data = pluginCall.getObject("data")
    val jsonHeaders = pluginCall.getObject("headers")
    val payload = data.toString();
    val requestBody = payload.toRequestBody()
    val keys: Iterator<String> = jsonHeaders.keys()
    val builder: Headers.Builder = Headers.Builder()
    while (keys.hasNext()) {
      val key = keys.next()
      builder.add(key, jsonHeaders.get(key).toString())
    }

    builder.add("User-Agent", System.getProperty("http.agent"));

    val request =
      url?.let { Request.Builder().url(it).headers(builder.build()).method("POST", requestBody).build() }

    val cert = String(Base64.getEncoder().encode((loadPublicKey().encoded)))
    val hostname = "brbcard.com.br"
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
      .add(hostname, "sha256/$cert")
      .build()
    val httpClient: OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .callTimeout(30, TimeUnit.SECONDS)
      .certificatePinner(certificatePinner)
      .build()

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

  private fun loadPublicKey(): X509Certificate {
    val ins = activity.application.assets.open("public/certificates/brbcard_22.cer").buffered()
    val cf = CertificateFactory.getInstance("X.509")
    val cert = cf.generateCertificate(ins) as X509Certificate
    return cert
  }
}
