package com.karmios.modulo.lampcontrol

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.util.authorId
import com.karmios.modulo.api.Modulo
import com.karmios.modulo.api.ModuloCmd
import com.karmios.modulo.api.ModuloModule
import com.karmios.modulo.api.persist.ModuleSavedData
import com.karmios.modulo.api.persist.ModuleSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import org.slf4j.LoggerFactory

@Suppress("unused")
class LampControlPlugin(wrapper: PluginWrapper) : Plugin(wrapper)

class LampControlSettings(
    val cooldown: Long = 3000,
    val colorReference: String = "https://www.w3schools.com/colors/colors_names.asp",
    val assistantUser: String = ""
) : ModuleSettings()

class LampControlSavedData(
    var endpoint: String = ""
): ModuleSavedData()

@Suppress("unused")
@Extension
class LampControl : ModuloModule<LampControlSettings, LampControlSavedData>() {
    override val name = "Lamp Control"

    override val defaultSettings = LampControlSettings()
    override val defaultSavedData = LampControlSavedData()

    private val log = LoggerFactory.getLogger(this::class.java)

    private var queue = mutableListOf<Pair<Message, String>>()
    private var looping = false

    private suspend fun Modulo.setEndpoint(msg: Message) {
        savedData.endpoint = msg.content.split(" ").drop(1).joinToString(" ")
        savedData.save()
        with(bot) {
            msg.reply("Endpoint set!")
        }
    }

    private suspend fun Modulo.loop() {
        if (!looping) {
            looping = true
            scope.launch {
                while(queue.isNotEmpty()) {
                    val (msg, color) = queue.removeAt(0)
                    sendColorChange("http://${savedData.endpoint}/assistant", color, settings.assistantUser, log)
                    with(bot) {
                        msg.reply("<@${msg.authorId}>, changed color to $color.")
                    }
                    delay(settings.cooldown)
                }
                looping = false
            }
        }
    }

    private suspend fun Modulo.changeColor(msg: Message) {
        val args = msg.content.split(" ").filter(String::isNotEmpty).drop(1)
        val color = args.joinToString(" ").toLowerCase()

        if (savedData.endpoint == "") {
            with(bot) {
                msg.reply("<@${msg.authorId}>, endpoint hasn't been set yet!")
            }
        }

        if (color == "black") {
            with(bot) {
                msg.reply("<@${msg.authorId}>, nice try :))")
                return
            }
        }

        if (color.replace(" ", "") !in colorNames) {
            with(bot) {
                msg.reply("<@${msg.authorId}>, that isn't a valid color!\nCheck out ${settings.colorReference} for a list of valid colors.")
            }
            return
        }

        queue.find { (queued, _) -> queued.authorId == msg.authorId }?.let {
            with(bot) {
                msg.reply("<@${msg.authorId}>, you already have a color change queued! Try again in a moment.")
                return
            }
        }

        queue.add(msg to color)
        loop()
    }

    override val commands = listOf(
        ModuloCmd(
            "lightColor",
            { changeColor(it) },
            description = "Change the light color!",
            usageExamples = listOf("[color]", "aqua", "red", "Lime Green")
        ),
        ModuloCmd(
            "setLampEndpoint",
            { setEndpoint(it) },
            description = "Set the endpoint for Assistant Relay"
        )
    )
}
