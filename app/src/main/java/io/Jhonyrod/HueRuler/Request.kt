package io.Jhonyrod.HueRuler

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.android.*
import com.appstractive.dnssd.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import java.security.cert.X509Certificate as X509C
import java.security.SecureRandom as SRandom
import javax.net.ssl.*

private const val HUE_ST = "_hue._tcp."
//Removed for privacy
private const val KEY = "nouser/config"

class Request {
	private val tmf=TrustManagerFactory.getInstance(
		TrustManagerFactory
		.getDefaultAlgorithm()
	).apply { init(keyStore) }
	
	private val sslContext = SSLContext
	.getInstance("TLS").apply {
		init(null, tmf.trustManagers, SRandom())
	}
	
	private val servs =
	mutableMapOf<String, DiscoveredService>()
	private val snums = mutableMapOf<String,String>()
	private val client = HttpClient(Android) {
		engine { sslManager = { conn ->
            conn.sslSocketFactory = sslContext
			.socketFactory
			conn.hostnameVerifier = HostnameVerifier{
				hn, ss ->
				val expectedSN = snums[hn] ?:
				return@HostnameVerifier false
				runCatching {
					val leaf = ss.peerCertificates[0]
					as X509C
					expectedSN.uppercase().equals(
						subCN(leaf),
						ignoreCase = true
					)
				}.getOrDefault(false)
			}
		}}
	}
	
	suspend fun discover() {
		discoverServices(HUE_ST).collect {
			when (it) {
				is DiscoveryEvent.Discovered -> {
					servs[it.service.key]=it.service
					it.resolve()
				}
				is DiscoveryEvent.Removed ->
				servs.remove(it.service.key)
				is DiscoveryEvent.Resolved -> {
					servs[it.service.key]=it.service
					val bID =
					it.service.txt["bridgeid"]?:
					return@collect
					for(addr in it.service.addresses)
					    snums[addr] = String(bID)
				}
			}
		}
	}
	
	suspend fun fetch():String {
		return if (!servs.values.any {
			it.addresses.isNotEmpty()
		}) "No bridges found/resolved"
		else try {
			val serv = servs.values.first()
			val addr = serv.addresses.first() + ':' + serv.port
			val json = client.get("https://$addr/api/$KEY").bodyAsText()
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