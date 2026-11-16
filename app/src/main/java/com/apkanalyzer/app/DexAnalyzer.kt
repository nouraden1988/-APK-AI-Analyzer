package com.apkanalyzer.app.analyzer

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.iface.ClassDef
import java.io.File

data class DexMethodInfo(
    val className: String,
    val methodName: String,
    val returnType: String,
    val parameters: List<String>
)

data class DexAnalysisResult(
    val classes: List<String>,
    val methods: List<DexMethodInfo>
)

class DexAnalyzer {

    fun analyze(apkFile: File): DexAnalysisResult {

        val classes = mutableListOf<String>()
        val methods = mutableListOf<DexMethodInfo>()

        val dexFiles = apkFile.inputStream().use { input ->
            DexFileFactory.loadDexContainer(
                apkFile,
                null
            )
        }

        for (entry in dexFiles.dexEntryNames) {

            val dex = dexFiles.getEntry(entry)?.dexFile
                ?: continue

            for (classDef in dex.classes) {

                classes.add(classDef.type)

                for (method in classDef.methods) {

                    methods.add(
                        DexMethodInfo(
                            className = classDef.type,
                            methodName = method.name,
                            returnType = method.returnType,
                            parameters = method.parameterTypes.map {
                                it.toString()
                            }
                        )
                    )
                }
            }
        }

        return DexAnalysisResult(
            classes = classes,
            methods = methods
        )
    }
}
