package com.example.fitnesscoachai.ui.workout.shoulderpress

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log

class ShoulderPressModel(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("shoulderpress_model.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    data class PredictResult(
        val label: String,
        val confidence: Float?
    )

    fun predict(features: FloatArray): PredictResult {
        return try {
            OnnxTensor.createTensor(env, arrayOf(features)).use { tensor ->
                val results = session.run(mapOf("input" to tensor))

                val rawLabel = (results[0].value as Array<*>)[0].toString()

                val confidence = try {
                    @Suppress("UNCHECKED_CAST")
                    val probMaps = results[1].value as Array<Map<String, Float>>
                    probMaps[0][rawLabel]
                } catch (_: Exception) {
                    null
                }

                results.close()

                val normalizedLabel = when (rawLabel.lowercase().trim()) {
                    "correct", "1" -> "correct"
                    else -> "incorrect"
                }

                PredictResult(normalizedLabel, confidence)
            }
        } catch (e: Exception) {
            Log.e("ShoulderPressModel", "predict error: ${e.message}", e)
            PredictResult("incorrect", null)
        }
    }

    fun close() {
        try {
            session.close()
        } catch (_: Exception) {
        }

        try {
            env.close()
        } catch (_: Exception) {
        }
    }
}