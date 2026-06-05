package com.darkrockstudios.apps.c2paverify.datasource.trustlist

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Fetches the official C2PA trust list PEM over HTTPS (Ktor). Stateless data source.
 */
class TrustListRemoteDataSource(private val client: HttpClient) {

	suspend fun fetchCaAnchorsPem(): String = client.get(CA_TRUST_LIST_URL).bodyAsText()

	companion object {
		const val CA_TRUST_LIST_URL =
			"https://raw.githubusercontent.com/c2pa-org/conformance-public/main/trust-list/C2PA-TRUST-LIST.pem"
	}
}
