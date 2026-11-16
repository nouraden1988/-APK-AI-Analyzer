package com.apkanalyzer.app.detection

import com.apkanalyzer.app.models.AnalysisResult

class FeatureDetector {

    fun classify(
        className: String,
        methodName: String,
        evidence: List<String>
    ): AnalysisResult {

        val text = (
            className + " " +
            methodName + " " +
            evidence.joinToString(" ")
        ).lowercase()

        val category = when {
            "subscription" in text -> "SUBSCRIPTION"
            "billing" in text -> "BILLING"
            "premium" in text -> "PREMIUM_INDICATOR"
            "license" in text -> "LICENSE"
            "trial" in text -> "TRIAL"
            "entitlement" in text -> "ENTITLEMENT"
            else -> "UNKNOWN"
        }

        val importance = when (category) {
            "SUBSCRIPTION" -> 90
            "BILLING" -> 85
            "ENTITLEMENT" -> 85
            "PREMIUM_INDICATOR" -> 80
            "LICENSE" -> 75
            "TRIAL" -> 65
            else -> 10
        }

        return AnalysisResult(
            className = className,
            methodName = methodName,
            category = category,
            importance = importance,
            confidence = if (category == "UNKNOWN") 0.2f else 0.6f,
            evidence = evidence,
            description = "تصنيف أولي مبني على المؤشرات النصية"
        )
    }
}
