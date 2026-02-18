package io.Jhonyrod.HueRuler

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.android.*
import kotlinx.serialization.json.*

class Request {
	private val client = HttpClient(Android)
	
	suspend fun fetch():String{
		return try {
			val json = client
			.get("https://mockly.me/users?count=5")
			.bodyAsText()
			tree(Json.parseToJsonElement(json))
		}catch(e: Exception){
			"Failed to load logs: ${e.message}"}
	}
	
	private fun tree(element:JsonElement):String{
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