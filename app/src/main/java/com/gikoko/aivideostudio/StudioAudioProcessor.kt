@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gikoko.aivideostudio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sign

/**
 * Lightweight offline PCM16 audio processor used by the mobile editor.
 * It provides 3-band tone shaping, a soft-knee style compressor and a noise gate.
 * It is deliberately allocation-light for low-memory ARMv7 phones.
 */
internal class StudioAudioProcessor(
    private val settings: StudioAudioSettings
) : BaseAudioProcessor() {
    private var channels = 2
    private var sampleRate = 44100
    private var lowState = FloatArray(2)
    private var midState = FloatArray(2)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        lowState = FloatArray(channels)
        midState = FloatArray(channels)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputBytes = inputBuffer.remaining()
        if (inputBytes <= 0) return
        val input = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val outputBuffer = replaceOutputBuffer(inputBytes).order(ByteOrder.nativeOrder())
        val output = outputBuffer.asShortBuffer()

        val lowAlpha = (1.0 - exp(-2.0 * PI * 250.0 / sampleRate)).toFloat()
        val midAlpha = (1.0 - exp(-2.0 * PI * 3200.0 / sampleRate)).toFloat()
        val lowGain = 10.0.pow(settings.lowDb / 20.0).toFloat()
        val midGain = 10.0.pow(settings.midDb / 20.0).toFloat()
        val highGain = 10.0.pow(settings.highDb / 20.0).toFloat()
        val master = settings.masterGain.coerceIn(0f, 2f)
        val gateThreshold = 10.0.pow(settings.noiseGateDb / 20.0).toFloat()
        val compressorThreshold = 10.0.pow(settings.compressorThresholdDb / 20.0).toFloat()
        val ratio = settings.compressorRatio.coerceIn(1f, 8f)

        var frameChannel = 0
        while (input.hasRemaining()) {
            val raw = input.get().toFloat() / 32768f
            val channel = frameChannel
            lowState[channel] += lowAlpha * (raw - lowState[channel])
            midState[channel] += midAlpha * (raw - midState[channel])

            val low = lowState[channel]
            val mid = midState[channel] - lowState[channel]
            val high = raw - midState[channel]
            var sample = (low * lowGain + mid * midGain + high * highGain) * master

            val magnitude = abs(sample)
            if (magnitude < gateThreshold) {
                sample = 0f
            } else if (magnitude > compressorThreshold) {
                val over = magnitude - compressorThreshold
                val compressed = compressorThreshold + over / ratio
                sample = sign(sample) * compressed
            }

            val clipped = sample.coerceIn(-0.9999f, 0.9999f)
            output.put((clipped * 32767f).toInt().toShort())
            frameChannel++
            if (frameChannel >= channels) frameChannel = 0
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(output.position() * 2)
        outputBuffer.flip()
    }
}
