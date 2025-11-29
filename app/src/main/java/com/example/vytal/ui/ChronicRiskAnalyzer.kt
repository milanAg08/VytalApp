package com.example.vytal.ui

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class ChronicRiskAnalyzer(private val context: Context) {

    private val seqLen = 20
    private val vocab: Map<String, Int>
    private val labels: List<String>
    private val tflite: Interpreter

    init {
        vocab = loadVocab("models/vocab.json")
        labels = loadLabels("models/labels.json")
        val modelBuffer = loadModel("models/chronic_risk_classifier.tflite")
        tflite = Interpreter(modelBuffer)
        android.util.Log.d("ChronicRiskAnalyzer", "Model loaded successfully. Vocab size: ${vocab.size}, Labels: ${labels.size}")
    }

    private fun loadVocab(path: String): Map<String, Int> {
        val reader = InputStreamReader(context.assets.open(path))
        val json = JSONObject(reader.readText())
        val map = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getInt(key)
        }
        return map
    }

    private fun loadLabels(path: String): List<String> {
        val array = org.json.JSONArray(
            InputStreamReader(context.assets.open(path)).readText()
        )
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) list.add(array.getString(i))
        return list
    }

    private fun loadModel(path: String): ByteBuffer {
        try {
            val fd = context.assets.openFd(path)
            val input = fd.createInputStream()
            val bytes = ByteArray(fd.length.toInt())
            input.read(bytes)
            input.close()
            fd.close()
            
            val bb = ByteBuffer.allocateDirect(bytes.size)
            bb.order(ByteOrder.nativeOrder())
            bb.put(bytes)
            bb.rewind() // Reset position to beginning
            android.util.Log.d("ChronicRiskAnalyzer", "Model loaded: ${bytes.size} bytes")
            return bb
        } catch (e: Exception) {
            android.util.Log.e("ChronicRiskAnalyzer", "Error loading model file: ${e.message}", e)
            throw e
        }
    }

    private fun tokenize(text: String): IntArray {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9_\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val ids = tokens.map { vocab[it] ?: vocab["[UNK]"] ?: 1 }

        val padded = if (ids.size >= seqLen)
            ids.take(seqLen)
        else
            ids + List(seqLen - ids.size) { 0 }

        return padded.toIntArray()
    }

    fun predict(text: String): List<Pair<String, Float>> {
        try {
            val inputIds = tokenize(text)

            // Get input tensor details to check the expected type
            val inputTensor = tflite.getInputTensor(0)
            val inputDataType = inputTensor.dataType()
            
            // Convert input based on model's expected type
            val input = if (inputDataType == org.tensorflow.lite.DataType.FLOAT32) {
                // Model expects float32, convert int array to float array
                Array(1) { inputIds.map { it.toFloat() }.toFloatArray() }
            } else {
                // Model expects int32 (normal for embeddings)
                Array(1) { inputIds }
            }
            
            val output = Array(1) { FloatArray(labels.size) }

            tflite.run(input, output)

            val logits = output[0]

            val exps = logits.map { exp(it.toDouble()) }
            val sum = exps.sum()
            val probs = exps.map { (it / sum).toFloat() }

            return labels.zip(probs).sortedByDescending { it.second }
        } catch (e: Exception) {
            android.util.Log.e("ChronicRiskAnalyzer", "Prediction error: ${e.message}", e)
            // Return a default prediction in case of error
            return labels.map { it to (1.0f / labels.size) }.sortedByDescending { it.second }
        }
    }
}
