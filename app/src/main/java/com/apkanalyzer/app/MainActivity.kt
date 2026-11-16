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
        "trial",
        "in_app_purchase"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 35, 24, 24)
        root.setBackgroundColor(Color.rgb(15, 23, 42))

        val title = TextView(this)
        title.text = "APK AI ANALYZER"
        title.textSize = 27f
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.CYAN)

        val subtitle = TextView(this)
        subtitle.text = "محلل التطبيقات الذكي v2.0"
        subtitle.textSize = 18f
        subtitle.gravity = Gravity.CENTER
        subtitle.setTextColor(Color.WHITE)

        val button = Button(this)
        button.text = "اختيار ملف APK"

        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/vnd.android.package-archive"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, 100)
        }

        output = TextView(this)
        output.text = "اختر APK لبدء التحليل"
        output.textSize = 15f
        output.setTextColor(Color.WHITE)
        output.setPadding(12, 25, 12, 25)

        val scroll = ScrollView(this)
        scroll.addView(output)

        root.addView(title)
        root.addView(subtitle)
        root.addView(button)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 &&
            resultCode == RESULT_OK) {

            val uri = data?.data

            if (uri != null) {
                analyzeAPK(uri)
            }
        }
    }

    private fun analyzeAPK(uri: Uri) {

        output.text = "جارٍ تحليل التطبيق..."

        Thread {

            try {

                val file = File(cacheDir, "analysis.apk")

                val input = contentResolver.openInputStream(uri)
                    ?: throw Exception("تعذر فتح الملف")

                input.use { source ->
                    file.outputStream().use { destination ->
                        source.copyTo(destination)
                    }
                }

                val flags = if (
                    android.os.Build.VERSION.SDK_INT >= 33
                ) {
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_PERMISSIONS.toLong()
                    )
                } else {
                    null
                }

                val info = if (
                    android.os.Build.VERSION.SDK_INT >= 33
                ) {
                    packageManager.getPackageArchiveInfo(
                        file.absolutePath,
                        flags!!
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

                            val content = zip.getInputStream(entry).use {
                                it.readBytes().toString(
                                    Charsets.ISO_8859_1
                                )
                            }

                            for (keyword in keywords) {

                                var position = 0

                                while (true) {

                                    val index = content.indexOf(
                                        keyword,
                                        position,
                                        ignoreCase = true
                                    )

                                    if (index < 0) break

                                    findings.add(
                                        "$keyword في $name عند الموضع $index"
                                    )

                                    position = index + keyword.length

                                    if (findings.size >= 200) break
                                }
                            }
                        }

                        if (name.endsWith(".so")) {
                            libraries.add(name)
                        }
                    }
                }

                val permissions =
                    info.requestedPermissions?.joinToString("\n") {
                        "• $it"
                    } ?: "لا توجد صلاحيات"

                val report = buildString {

                    appendLine("اكتمل التحليل بنجاح")
                    appendLine("====================")
                    appendLine("اسم الحزمة: ${info.packageName}")
                    appendLine("الإصدار: ${info.versionName}")
                    appendLine("الحجم: ${file.length() / 1024} KB")

                    appendLine()
                    appendLine("ملفات DEX: ${dexFiles.size}")

                    dexFiles.forEach {
                        appendLine(it)
                    }

                    appendLine()
                    appendLine("مؤشرات الاشتراكات والميزات")
                    appendLine("عدد النتائج: ${findings.size}")

                    if (findings.isEmpty()) {
                        appendLine("لم يتم العثور على مؤشرات نصية.")
                    } else {
                        findings.forEach {
                            appendLine("• $it")
                        }
                    }

                    appendLine()
                    appendLine("المكتبات الأصلية: ${libraries.size}")

                    libraries.forEach {
                        appendLine(it)
                    }

                    appendLine()
                    appendLine("الصلاحيات:")
                    appendLine(permissions)

                    appendLine()
                    appendLine("ملاحظة:")
                    appendLine(
                        "هذه نتائج بحث نصي أولي وليست إثباتاً " +
                        "على وجود ميزة مدفوعة أو آلية قفل."
                    )
                }

                runOnUiThread {
                    output.text = report
                }

            } catch (e: Exception) {

                runOnUiThread {
                    output.text =
                        "حدث خطأ أثناء التحليل:\n${e.message}"
                }
            }

        }.start()
    }
}