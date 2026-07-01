package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

object ToneSynthesizer {

    fun playCalmChime(isStudyEnd: Boolean) {
        Thread {
            try {
                val sampleRate = 44100
                // Beautiful triads of notes
                // For Study End (calm transition): A gentle soft major 7th / 9th chord arpeggio
                // For Break End: A sweet focused ringing major chord arpeggio to draw attention back to focus
                val freqs = if (isStudyEnd) {
                    // Calm relaxing chord (E4, G#4, B4, D#5)
                    doubleArrayOf(329.63, 415.30, 493.88, 587.33)
                } else {
                    // Focused ringing ascending chord (C5, E5, G5, C6)
                    doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                }

                val durationSeconds = 2.0
                val totalSamples = (sampleRate * durationSeconds).toInt()
                val buffer = FloatArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    var sampleValue = 0.0
                    
                    for (fIndex in freqs.indices) {
                        val freq = freqs[fIndex]
                        // Rolled onset: delay each note slightly for an arpeggio effect
                        val noteDelay = fIndex * 0.12
                        if (t >= noteDelay) {
                            val noteT = t - noteDelay
                            // Smooth exponential decay: fast initial drop then trailing ring
                            val decay = kotlin.math.exp(-2.5 * noteT)
                            sampleValue += sin(2.0 * Math.PI * freq * noteT) * decay * 0.22
                        }
                    }
                    
                    // Master volume and clipping check
                    buffer[i] = sampleValue.coerceIn(-0.9, 0.9).toFloat()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(sampleRate)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 4)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
                audioTrack.play()
                
                // Allow sound to play fully
                Thread.sleep((durationSeconds * 1000).toLong() + 200)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
