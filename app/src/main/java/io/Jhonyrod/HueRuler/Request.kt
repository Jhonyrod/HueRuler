package io.Jhonyrod.HueRuler

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.okhttp.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import java.security.cert.X509Certificate as X509C

//Removed for privacy
private const val KEY = "nouser/config"

class Request {
	private val client:HttpClient by lazy {hcb(snums)}
	
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