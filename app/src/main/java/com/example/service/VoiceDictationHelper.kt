package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.ui.i18n.AppLanguage

/**
 * On-device Android SpeechRecognizer controller for hands-free SMS voice dictation
 * using the device microphone (RECORD_AUDIO permission).
 */
class VoiceDictationHelper(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onRmsLevelChanged: (Float) -> Unit,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun startListening(language: AppLanguage) {
        if (!hasMicrophonePermission(context)) {
            onError(
                if (language == AppLanguage.EN) "Microphone permission is required for voice dictation."
                else "Ruhusa ya Maikrofoni (Mic) inahitajika ili kuongea ujumbe."
            )
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError(
                if (language == AppLanguage.EN) "On-device speech recognition service is unavailable on this device."
                else "Huduma ya kutambua sauti haipatikani kwenye kifaa hiki."
            )
            return
        }

        stopListening()

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChanged(true)
            }

            override fun onBeginningOfSpeech() {
                onListeningChanged(true)
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
                onRmsLevelChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                onListeningChanged(false)
                onRmsLevelChanged(0f)
            }

            override fun onError(error: Int) {
                onListeningChanged(false)
                onRmsLevelChanged(0f)
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError(
                        if (language == AppLanguage.EN) "No speech detected. Tap the mic and speak clearly."
                        else "Hakuna sauti iliyosikika. Gusa Mic na ongea wazi."
                    )
                } else if (error != SpeechRecognizer.ERROR_CLIENT) {
                    onError(
                        if (language == AppLanguage.EN) "Voice capture stopped (code $error)."
                        else "Kurekodi sauti kumesimama (msimbo $error)."
                    )
                }
            }

            override fun onResults(results: Bundle?) {
                onListeningChanged(false)
                onRmsLevelChanged(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()?.trim().orEmpty()
                if (best.isNotEmpty()) {
                    onFinalTranscript(best)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    onPartialTranscript(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val localeTag = if (language == AppLanguage.SW) "sw-TZ" else "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        onListeningChanged(true)
        sr.startListening(intent)
    }

    fun stopListening() {
        runCatching {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
        onListeningChanged(false)
        onRmsLevelChanged(0f)
    }

    companion object {
        fun hasMicrophonePermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
