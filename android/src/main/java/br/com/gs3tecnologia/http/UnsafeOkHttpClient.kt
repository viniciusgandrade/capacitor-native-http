import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.*

class UnsafeOkHttpClient {
  companion object {
    fun getUnsafeOkHttpClient(): OkHttpClient {
      try {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
          override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
          }

          override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
          }

          override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
          }
        })
        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        // Create an OkHttpClient that trusts all SSL certificates and ignores hostnames
        return OkHttpClient.Builder()
          .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
          .hostnameVerifier { _, _ -> true }
          .build()
      } catch (e: Exception) {
        throw RuntimeException(e)
      }
    }
  }
}
