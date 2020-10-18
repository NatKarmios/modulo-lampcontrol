package com.karmios.modulo.lampcontrol

import com.github.kittinunf.fuel.coroutines.awaitString
import com.github.kittinunf.fuel.httpPost
import org.slf4j.Logger


suspend fun sendColorChange(endpoint: String, color: String, user: String, log: Logger?) {
    val content = """{"command": "set the light to $color", "converse": false, "user": "$user"}"""
    log?.info("Posting to '$endpoint' with '$content")
    try {
        endpoint.httpPost().header("Content-Type", "application/json").body(content).awaitString()
    } catch (ex: Exception) {
        log?.warn(ex.toString())
    }
}
