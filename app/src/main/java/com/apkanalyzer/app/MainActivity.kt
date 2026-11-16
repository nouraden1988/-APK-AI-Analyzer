package com.apkanalyzer.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.zip.ZipFile

class MainActivity : Activity() {

    private lateinit var output: TextView

    private val keywords = listOf(
        "premium",
        "subscription",
        "subscribe",
        "pro_version",
        "is_premium",
        "isPro",
        "paid",
        "purchase",
        "billing",
        "license",
        "entitlement",
        "unlock",
        "feature_locked",
        "trial",
        "in_app_purchase",
        "google_play_billing"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 35, 24, 24)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val title = TextView(this).apply {
            text = "APK AI ANALYZER"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.CYAN)
        }

        val subtitle = TextView(this).apply {
            text = "محلل التطبيقات الذكي v2.0"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val button = Button(this).apply {
            text = "📦 اختيار ملف APK"
            textSize = 17f

            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {
                    type = "application/vnd.android.package-archive"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                startActivityForResult(intent, 100)
            }
        }

        output = TextView(this).apply {
            text = "اختر APK لبدء التحليل الذكي"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(12, 25, 12, 25)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(button)

        val scroll = ScrollView(this)
        scroll.addView(output)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 100 &&
            resultCode == RESULT_OK) {

            data?.data?.let {
                analyzeAPK(it)
            }
        }
    }

    private fun analyzeAPK(uri: Uri) {

        output.text = "⏳ جارٍ فحص APK..."

        Thread {

            try {

                val file = File(
                    cacheDir,
                    "analysis.apk"
                )

                contentResolver.openInputStream(uri)?.use {
                    input ->
                    file.outputStream().use {
                        output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("تعذر قراءة الملف")

                val info = if (
                    android.os.Build.VERSION.SDK_INT >= 33
                ) {

                    packageManager.getPackageArchiveInfo(
                        file.absolutePath,
                        PackageManager.PackageInfoFlags.of(
                            PackageManager.GET_PERMISSIONS.toLong()
                        )
                    )

                } else {

                    @Suppress("DEPRECATION")
                    packageManager.getPackageArchiveInfo(
                        file.absolutePath,
                        PackageManager.GET_PERMISSIONS
                    )
                } ?: throw Exception("ملف APK غير صالح")

                val dexFiles = mutableListOf<String>()
                val libraries = mutableListOf<String>()
                val findings = mutableListOf<String>()

                ZipFile(file).use { zip ->

                    val entries = zip.entries()

                    while (entries.hasMoreElements()) {

                        val entry = entries.nextElement()
                        val name = entry.name

                        if (name.endsWith(".dex")) {

                            dexFiles.add(name)

                            val bytes = zip.getInputStream(
                                entry
                            ).use {
                                it.readBytes()
                            }

                            val content = String(
                                bytes,
                                Charsets.ISO_8859_1
                            )

                            for (keyword in keywords) {

                                val index = content.indexOf(
                                    keyword,
                                    ignoreCase = true
                                )

                                if (index