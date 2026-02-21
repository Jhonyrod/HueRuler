package io.Jhonyrod.HueRuler

//import android.content.Context
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.android.*
import com.appstractive.dnssd.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import javax.net.ssl.TrustManagerFactory

private const val HUE_ST = "_hue._tcp."

//Removed for privacy
private const val KEY = "nouser/config"

class Request {//private val context:Context) {
	
	private val servs =
	mutableMapOf<String, DiscoveredService>()
	private val client = HttpClient(Android) {
		engine { sslManager = { conn ->
			val tmf = TrustManagerFactory
			.getInstance(
				TrustManagerFactory
				.getDefaultAlgorithm()
			)
            tmf.init(keyStore)
            val cont = javax.net.ssl
			.SSLContext.getInstance("TLS")
            cont.init(null, tmf.trustManagers, null)
            conn.sslSocketFactory = cont.socketFactory
			conn.hostnameVerifier = javax.net.ssl
			.HostnameVerifier { hn, _ ->
                servs.values.any {
                	it.addresses.contains(hn) }
			}
		}}
	}
	
	suspend fun discover() {
		discoverServices(HUE_ST).collect {
			when (it) {
				is DiscoveryEvent.Discovered -> {
					servs[it.service.key] = it.service
					it.resolve()
				}
				is DiscoveryEvent.Removed ->
				servs.remove(it.service.key)
				is DiscoveryEvent.Resolved ->
				servs[it.service.key] = it.service
			}
		}
	}
	
	suspend fun fetch():String {
		return if (!servs.values.any {
			it.addresses.isNotEmpty()
		}) "No bridges found/resolved"
		else try {
			val serv = servs
			.values
			.first()
			val addr = serv.addresses.first() + ':' + serv.port
			val json = client
			.get("https://$addr/api/$KEY")
			//.get("https://mockly.me/users?count=5")
			.bodyAsText()
			//json
			tree(Json.parseToJsonElement(json))
		}catch(e: CancellationException) {
			"The fetching was cancelled. Try again."
		}catch(e: Exception){
			"Failed to load logs: ${e.message}"}
	}
	
	private fun tree(element:JsonElement):String {
		val ret = StringBuilder()
		when (element){
			is JsonPrimitive ->
			ret.appendLine(element.content)
			is JsonObject ->
			element.entries.forEach { (k, v) ->
				ret.appendLine(k)
				ret.append(tree(v))
			}
			is JsonArray ->
			element.forEachIndexed { i, c ->
				ret.appendLine("[$i]")
				ret.append(tree(c))
			}
		}
		return ret.toString()
	}
	
	fun close() = client.close()
}