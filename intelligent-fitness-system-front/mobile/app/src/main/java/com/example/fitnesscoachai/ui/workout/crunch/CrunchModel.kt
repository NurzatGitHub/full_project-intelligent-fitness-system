package com.example.fitnesscoachai.ui.workout.crunch

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log

class CrunchModel(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets
            .open("crunch_model.onnx")
            .readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
        Log.d("CrunchModel", "inputs=${session.inputNames}, outputs=${session.outputNames}")
    }

    data class PredictResult(
        val label: String,       // "correct" | "incorrect"
        val confidence: Float?
    )

    fun predict(features: FloatArray): PredictResult {
        return try {
            OnnxTensor.createTensor(env, arrayOf(features)).use { tensor ->
                session.run(mapOf("input" to tensor)).use { results ->

                    val rawLabel = try {
                        @Suppress("UNCHECKED_CAST")
                        val labelValue = results["output_label"].get() as OnnxValue
                        (labelValue.value as Array<*>)[0].toString()
                    } catch (e: Exception) {
                        @Suppress("UNCHECKED_CAST")
                        (results[0].value as Array<*>)[0].toString()
                    }

                    val confidence: Float? = try {
                        @Suppress("UNCHECKED_CAST")
                        val probValue = results["output_probability"].get() as OnnxValue
                        val probMaps = probValue.value as Array<Map<String, Float>>
                        probMaps[0][rawLabel]
                    } catch (e: Exception) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val probMaps = results[1].value as Array<Map<String, Float>>
                            probMaps[0][rawLabel]
                        } catch (e2: Exception) {
                            null
                        }
                    }

                    val normalizedLabel = when (rawLabel.lowercase().trim()) {
                        "correct", "1" -> "correct"
                        else -> "incorrect"
                    }

                    // Если модель не уверена (< 0.65) — не штрафуем пользователя
                    val finalLabel = if (
                        normalizedLabel == "incorrect" &&
                        confidence != null &&
                        confidence < 0.65f
                    ) "correct" else normalizedLabel

                    PredictResult(finalLabel, confidence)
                }
            }
        } catch (e: Exception) {
            Log.e("CrunchModel", "predict error: ${e.message}", e)
            PredictResult("incorrect", null)
        }
    }

    fun close() {
        try { session.close() } catch (_: Exception) {}
        try { env.close()     } catch (_: Exception) {}
    }
}
