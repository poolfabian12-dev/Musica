package com.example.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates high-quality offline Christian Worship acoustic/piano audio
 * as a fail-safe so playback never fails even when offline or when external streams are unreachable.
 */
object WorshipAudioSynthesizer {

    private const val TAG = "WorshipSynthesizer"
    private const val SAMPLE_RATE = 44100
    private const val NUM_CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    // Notes frequencies in Hz (C4, D4, E4, F4, G4, A4, B4, C5, etc.)
    private val NOTES = mapOf(
        "C3" to 130.81, "G3" to 196.00, "A3" to 220.00, "F3" to 174.61,
        "C4" to 261.63, "D4" to 293.66, "E4" to 329.63, "F4" to 349.23,
        "G4" to 392.00, "A4" to 440.00, "B4" to 493.88, "C5" to 523.25,
        "D5" to 587.33, "E5" to 659.25, "G5" to 783.99
    )

    // Classic Christian Worship Chord Progression (C - G - Am - F)
    private val CHORD_PROGRESSION = listOf(
        listOf("C3", "C4", "E4", "G4", "C5"), // C Major (Adoración)
        listOf("G3", "B4", "D4", "G4", "D5"), // G Major (Alabanza)
        listOf("A3", "C4", "E4", "A4", "E5"), // A Minor (Quebranto y Fe)
        listOf("F3", "A4", "C4", "F4", "A4")  // F Major (Gracia y Paz)
    )

    suspend fun getOrCreateDefaultWorshipAudio(context: Context): String = withContext(Dispatchers.IO) {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) audioDir.mkdirs()

        val wavFile = File(audioDir, "worship_ambient_track.wav")
        if (wavFile.exists() && wavFile.length() > 50000) {
            return@withContext wavFile.absolutePath
        }

        try {
            generateWorshipWavFile(wavFile, durationSeconds = 30)
            Log.i(TAG, "Generated local worship audio file: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            return@withContext wavFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating worship audio: ${e.message}", e)
            return@withContext ""
        }
    }

    private fun generateWorshipWavFile(outputFile: File, durationSeconds: Int) {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val bytesPerSample = NUM_CHANNELS * (BITS_PER_SAMPLE / 8)
        val dataSize = totalSamples * bytesPerSample

        FileOutputStream(outputFile).use { fos ->
            // 1. Write standard 44-byte WAV header
            writeWavHeader(fos, totalSamples, dataSize)

            // 2. Synthesize harmonious piano/pad worship notes
            val chordDurationSec = 3.75 // Each chord lasts 3.75 seconds (8 chords = 30 seconds)
            val samplesPerChord = (SAMPLE_RATE * chordDurationSec).toInt()

            val buffer = ByteBuffer.allocate(bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val chordIndex = ((i / samplesPerChord) % CHORD_PROGRESSION.size)
                val chordNotes = CHORD_PROGRESSION[chordIndex]
                val timeInChord = (i % samplesPerChord).toDouble() / SAMPLE_RATE

                var sampleValue = 0.0

                for (noteName in chordNotes) {
                    val freq = NOTES[noteName] ?: 440.0
                    
                    // Fundamental tone + gentle overtones for warm acoustic/piano timbre
                    val envelope = exp(-1.2 * timeInChord) // Gentle exponential decay
                    val fundamental = sin(2.0 * PI * freq * timeInChord)
                    val harmonic2 = 0.4 * sin(2.0 * PI * (freq * 2.0) * timeInChord)
                    val harmonic3 = 0.15 * sin(2.0 * PI * (freq * 3.0) * timeInChord)
                    val shimmer = 0.05 * sin(2.0 * PI * (freq * 4.0) * timeInChord)

                    val noteSound = (fundamental + harmonic2 + harmonic3 + shimmer) * envelope
                    sampleValue += noteSound
                }

                // Add warm bass resonance
                val bassFreq = NOTES[chordNotes.first()] ?: 130.81
                val bassEnvelope = exp(-0.8 * timeInChord)
                val bass = 0.6 * sin(2.0 * PI * (bassFreq / 2.0) * timeInChord) * bassEnvelope
                sampleValue += bass

                // Normalize and avoid clipping
                val normalized = (sampleValue / (chordNotes.size + 1.2)).coerceIn(-0.95, 0.95)
                val pcmShort = (normalized * 32767.0).toInt().toShort()

                buffer.clear()
                buffer.putShort(pcmShort) // Left Channel
                buffer.putShort(pcmShort) // Right Channel
                fos.write(buffer.array())
            }
        }
    }

    private fun writeWavHeader(fos: FileOutputStream, totalSamples: Int, dataSize: Int) {
        val totalFileSize = 36 + dataSize
        val byteRate = SAMPLE_RATE * NUM_CHANNELS * (BITS_PER_SAMPLE / 8)
        val blockAlign = NUM_CHANNELS * (BITS_PER_SAMPLE / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalFileSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat (1 = PCM)
        header.putShort(NUM_CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(BITS_PER_SAMPLE.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)

        fos.write(header.array())
    }
}
