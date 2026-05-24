package com.example.fitnesscoachai.ui.workout.pushup

import ai.onnxruntime.*
import android.content.Context
import android.util.Log

class PushupModel(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets
            .open("pushup_model.onnx")
            .readBytes()
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

                val confidence: Float? = try {
                    @Suppress("UNCHECKED_CAST")
                    val probMaps = results[1].value as Array<Map<String, Float>>
                    val probMap = probMaps[0]
                    probMap[rawLabel]
                } catch (e: Exception) {
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
            Log.e("PushupModel", "predict error: ${e.message}", e)
            PredictResult("incorrect", null)
        }
    }

    fun close() {
        session.close()
        env.close()
    }
}