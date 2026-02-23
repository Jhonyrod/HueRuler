package io.Jhonyrod.HueRuler

import com.appstractive.dnssd.*

internal val servs =
mutableMapOf<String, DiscoveredService>()
internal val snums = mutableMapOf<String,String>()

suspend fun discover(type:String) {
	discoverServices(type).collect {
		when (it) {
			is DiscoveryEvent.Discovered -> {
				servs[it.service.key]=it.service
				it.resolve()
			}
			is DiscoveryEvent.Removed ->
			servs.remove(it.service.key)
			is DiscoveryEvent.Resolved -> {
				servs[it.service.key]=it.service
				val bID = it.service.txt["bridgeid"]
				?: return@collect
				for(addr in it.service.addresses)
				snums[addr] = String(bID)
			}
		}
	}
}