package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape
import org.example.animation.io.encodeRawToPng

class AbrBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("abr")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        if (bytes.size < 2) return emptyList()
        val version = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
        
        return try {
            when (version) {
                1, 2 -> parseV1V2(bytes)
                6, 7, 10 -> parseV6Plus(bytes)
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseV1V2(bytes: ByteArray): List<BrushPreset> {
        // Парсер v1/v2 — упрощённый
        return emptyList() // TODO: Реализовать при необходимости, но v6+ более распространён
    }

    private fun parseV6Plus(bytes: ByteArray): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        var offset = 2
        
        while (offset < bytes.size - 8) {
            val signature = bytes.sliceArray(offset until offset + 4).decodeToString()
            if (signature != "8BIM") {
                offset++
                continue
            }
            offset += 4
            val tag = bytes.sliceArray(offset until offset + 4).decodeToString()
            offset += 4
            
            // Читаем длину (4 байта)
            val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                         ((bytes[offset+1].toInt() and 0xFF) shl 16) or
                         ((bytes[offset+2].toInt() and 0xFF) shl 8) or
                         (bytes[offset+3].toInt() and 0xFF)
            offset += 4
            
            val nextOffset = offset + length
            
            if (tag == "samp") {
                val brushes = parseSampBlock(bytes.sliceArray(offset until nextOffset))
                results.addAll(brushes)
            }
            
            offset = nextOffset
            if (offset % 2 != 0) offset++ // выравнивание
        }
        return results
    }

    private fun parseSampBlock(data: ByteArray): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        var offset = 0
        
        while (offset < data.size - 10) {
            // Упрощённый парсер блока
            // В ABR v6+ samp содержит несколько сэмплированных кистей
            // Каждая начинается с длины и некоторого ID
            offset += 4 // Пропускаем длину записи

            // Ищем имя или подпись внутри samp при наличии
            // Пока извлекаем то, что похоже на grayscale bitmap
            // (диаметр, пиксели, сжатие)

            // Этот раздел сильно зависит от внутреннего RLE ABR
            // Необходимо реализовать декодирование PackBits
            offset += 10 // пропускаем заглушку
            break // пока заглушка
        }
        return results
    }

    fun decodePackBits(input: ByteArray, outputSize: Int): ByteArray {
        val output = ByteArray(outputSize)
        var i = 0
        var j = 0
        while (i < input.size && j < outputSize) {
            val n = input[i++].toInt()
            if (n >= 0) {
                val len = n + 1
                for (k in 0 until len) {
                    if (j < outputSize && i < input.size) output[j++] = input[i++]
                }
            } else if (n > -128) {
                val len = -n + 1
                val b = input[i++]
                for (k in 0 until len) {
                    if (j < outputSize) output[j++] = b
                }
            }
        }
        return output
    }
}
