/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.documentenapi.service.impl

import nl.nlportal.documentenapi.client.ClamAVVirusScanConfig.ClamAVVirusScanConfigProperties
import nl.nlportal.documentenapi.domain.VirusScanResult
import nl.nlportal.documentenapi.domain.VirusScanStatus
import nl.nlportal.documentenapi.service.VirusScanService
import xyz.capybara.clamav.ClamavClient
import xyz.capybara.clamav.commands.scan.result.ScanResult
import java.io.ByteArrayInputStream

class ClamAVService(
    private val clamAVVirusScanConfigProperties: ClamAVVirusScanConfigProperties,
) : VirusScanService {
    override fun scan(content: ByteArray): VirusScanResult {
        val clamAVClient =
            ClamavClient(
                clamAVVirusScanConfigProperties.hostName,
                clamAVVirusScanConfigProperties.port,
            )
        return ByteArrayInputStream(content).use { inputStream ->
            when (val scanResult = clamAVClient.scan(inputStream)) {
                is ScanResult.OK -> VirusScanResult(VirusScanStatus.OK, mapOf())
                is ScanResult.VirusFound -> VirusScanResult(VirusScanStatus.VIRUS_FOUND, scanResult.foundViruses)
            }
        }
    }
}