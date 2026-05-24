package com.example.fitnesscoachai.ui.workout.plank

import ai.onnxruntime.*
import android.content.Context
import android.util.Log

class PlankModel(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    // Имя входа определяется автоматически из модели
    private val inputName: String

    init {
        val modelBytes = context.assets
            .open("plank_model.onnx")
            .readBytes()
        session   = env.createSession(modelBytes, OrtSession.SessionOptions())
        inputName = session.inputNames.iterator().next()   // "input" в новой модели
        Log.d("PlankModel", "Loaded. inputName=$inputName  " +
                "outputs=${session.outputNames.toList()}")
    }

    data class PredictResult(
        val label: String,
        val confidence: Float?
    )

    fun predict(features: FloatArray): PredictResult {
        return try {
            OnnxTensor.createTensor(env, arrayOf(features)).use { tensor ->
                val results = session.run(mapOf(inputName to tensor))

                val rawLabel = (results[0].value as Array<*>)[0].toString()

                val confidence: Float? = try {
                    @Suppress("UNCHECKED_CAST")
                    val probMaps = results[1].value as Array<Map<String, Float>>
                    probMaps[0][rawLabel]
                } catch (e: Exception) {
                    null
                }

                results.close()

                val normalizedLabel = when (rawLabel.lowercase().trim()) {
                    "correct", "1" -> "correct"
                    else           -> "incorrect"
                }

                PredictResult(normalizedLabel, confidence)
            }
        } catch (e: Exception) {
            Log.e("PlankModel", "predict error: ${e.message}", e)
            PredictResult("incorrect", null)
        }
    }

    fun close() {
        session.close()
        env.close()
    }
}