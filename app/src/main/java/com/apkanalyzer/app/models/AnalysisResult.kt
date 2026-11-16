package com.apkanalyzer.app.models

data class AnalysisResult(
    val className: String,
    val methodName: String,
    val category: String,
    val importance: Int,
    val confidence: Float,
    val evidence: List<String>,
    val description: String
)
