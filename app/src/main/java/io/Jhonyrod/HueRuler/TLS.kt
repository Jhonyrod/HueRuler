package io.Jhonyrod.HueRuler

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import at.asitplus.signum.indispensable.*
import at.asitplus.signum.indispensable.pki.*
import at.asitplus.signum.indispensable.asn1.*
import at.asitplus.signum.indispensable.asn1.encoding.*
import okhttp3.tls.HandshakeCertificates as HsCs
import java.security.cert.X509Certificate as JX509

private const val curr = 
"""-----BEGIN CERTIFICATE-----
MIICMjCCAdigAwIBAgIUO7FSLbaxikuXAljzVaurLXWmFw4wCgYIKoZIzj0EAwIw
OTELMAkGA1UEBhMCTkwxFDASBgNVBAoMC1BoaWxpcHMgSHVlMRQwEgYDVQQDDAty
b290LWJyaWRnZTAiGA8yMDE3MDEwMTAwMDAwMFoYDzIwMzgwMTE5MDMxNDA3WjA5
MQswCQYDVQQGEwJOTDEUMBIGA1UECgwLUGhpbGlwcyBIdWUxFDASBgNVBAMMC3Jv
b3QtYnJpZGdlMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjNw2tx2AplOf9x86
aTdvEcL1FU65QDxziKvBpW9XXSIcibAeQiKxegpq8Exbr9v6LBnYbna2VcaK0G22
jOKkTqOBuTCBtjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAdBgNV
HQ4EFgQUZ2ONTFrDT6o8ItRnKfqWKnHFGmQwdAYDVR0jBG0wa4AUZ2ONTFrDT6o8
ItRnKfqWKnHFGmShPaQ7MDkxCzAJBgNVBAYTAk5MMRQwEgYDVQQKDAtQaGlsaXBz
IEh1ZTEUMBIGA1UEAwwLcm9vdC1icmlkZ2WCFDuxUi22sYpLlwJY81Wrqy11phcO
MAoGCCqGSM49BAMCA0gAMEUCIEBYYEOsa07TH7E5MJnGw557lVkORgit2Rm1h3B2
sFgDAiEA1Fj/C3AN5psFMjo0//mrQebo0eKd3aWRx+pQY08mk48=
-----END CERTIFICATE-----"""
private const val next =
"""-----BEGIN CERTIFICATE-----
MIIBzDCCAXOgAwIBAgICEAAwCgYIKoZIzj0EAwIwPDELMAkGA1UEBhMCTkwxFDAS
BgNVBAoMC1NpZ25pZnkgSHVlMRcwFQYDVQQDDA5IdWUgUm9vdCBDQSAwMTAgFw0y
NTAyMjUwMDAwMDBaGA8yMDUwMTIzMTIzNTk1OVowPDELMAkGA1UEBhMCTkwxFDAS
BgNVBAoMC1NpZ25pZnkgSHVlMRcwFQYDVQQDDA5IdWUgUm9vdCBDQSAwMTBZMBMG
ByqGSM49AgEGCCqGSM49AwEHA0IABFfOO0jfSAUXGQ9kjEDzyBrcMQ3ItyA5krE+
cyvb1Y3xFti7KlAad8UOnAx0FBLn7HZrlmIwm1QnX0fK3LPM13mjYzBhMB0GA1Ud
DgQWBBTF1pSpsCASX/z0VHLigxU2CAaqoTAfBgNVHSMEGDAWgBTF1pSpsCASX/z0
VHLigxU2CAaqoTAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAKBggq
hkjOPQQDAgNHADBEAiAk7duT+IHbOGO4UUuGLAEpyYejGZK9Z7V9oSfnvuQ5BQIg
IYSgwwxHXm73/JgcU9lAM6c8Bmu3UE3kBIUwBs1qXFw=
-----END CERTIFICATE-----"""

private fun pem2jca(pem:String)=
X509Certificate
.decodeFromPem(pem)
.getOrThrow()
.toJcaCertificateBlocking()
.getOrThrow()

private fun subCN(leaf:X509Certificate):String? =
leaf.tbsCertificate.subjectName
.flatMap { it.attrsAndValues }
.firstOrNull {it is AttributeTypeAndValue.CommonName}
?.value?.asPrimitive()?.decodeToString()

fun hcb(hostnames:Map<String, String>):HttpClient {
	val certs = HsCs.Builder()
	.addTrustedCertificate(pem2jca(curr))
	.addTrustedCertificate(pem2jca(next))
	.build()
	return HttpClient(OkHttp) { engine { config {
		sslSocketFactory(
			certs.sslSocketFactory(),
			certs.trustManager
		)
		hostnameVerifier { hostname, session ->
			val expectedSN = hostnames[hostname] ?:
			return@hostnameVerifier false
			runCatching {
				val leaf = session.peerCertificates[0]
				as JX509
				expectedSN.equals(
					subCN(
						leaf
						.toKmpCertificate()
						.getOrThrow()
					),
					ignoreCase = true
				)
			}.getOrDefault(false)
		}
	}}}
}