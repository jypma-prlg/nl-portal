/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.zgw.taak

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException
import java.util.function.Consumer

object TestHelper {
    val ERRORS_JSON_PATH = "$.errors"
    val EXTENSIONS_JSON_PATH = "$.extensions"
    private val logger = KotlinLogging.logger {}

    fun mockResponseFromFile(fileName: String): MockResponse =
        MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody(readFileAsString(fileName))

    private fun readFileAsString(fileName: String): String = this::class.java.getResource(fileName)!!.readText(Charsets.UTF_8)

    fun WebTestClient.ResponseSpec.verifyOnlyDataExists(basePath: String): WebTestClient.BodyContentSpec =
        this
            .expectBody()
            .consumeWith(Consumer { t -> logger.info { t } })
            .jsonPath(basePath)
            .exists()
            .jsonPath(ERRORS_JSON_PATH)
            .doesNotExist()
            .jsonPath(EXTENSIONS_JSON_PATH)
            .doesNotExist()

    fun mockJsonResponse(body: String): MockResponse =
        MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody(body)

    fun resultPage(vararg results: String): String =
        """
        {
            "count": ${results.size},
            "next": null,
            "previous": null,
            "results": [${results.joinToString(",")}]
        }
        """.trimIndent()

    /** The URL of a recorded request; fails with a readable message instead of a `!!` when the server did not record one. */
    val RecordedRequest.url: HttpUrl
        get() = checkNotNull(requestUrl) { "MockWebServer recorded no request URL for $requestLine" }

    /** Asserts that [block] fails with a [ResponseStatusException] carrying [expected], and returns it for further checks. */
    inline fun assertStatus(
        expected: HttpStatus,
        block: () -> Unit,
    ): ResponseStatusException {
        val exception = assertThrows<ResponseStatusException> { block() }
        assertEquals(expected, exception.statusCode)
        return exception
    }
}