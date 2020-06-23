/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("PRE_RELEASE_CLASS")

package net.mamoe.mirai.console.codegen

import org.intellij.lang.annotations.Language
import java.io.File


fun main() {
    println(File("").absolutePath) // default project base dir

    File("backend/mirai-console/src/main/kotlin/net/mamoe/mirai/console/setting/_Setting.kt").apply {
        createNewFile()
    }.writeText(buildString {
        appendLine(COPYRIGHT)
        appendLine()
        appendLine(FILE_SUPPRESS)
        appendLine()
        appendLine(PACKAGE)
        appendLine()
        appendLine(IMPORTS)
        appendLine()
        appendLine()
        appendLine(DO_NOT_MODIFY)
        appendLine()
        appendLine()
        appendLine(genAllValueUseSite())
    })
}

private val DO_NOT_MODIFY = """
/**
 * !!! This file is auto-generated by backend/codegen/src/kotlin/net.mamoe.mirai.console.codegen.SettingValueUseSiteCodegen.kt
 * !!! DO NOT MODIFY THIS FILE MANUALLY
 */
""".trimIndent()

private val PACKAGE = """
package net.mamoe.mirai.console.setting
""".trimIndent()

internal val FILE_SUPPRESS = """
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
""".trimIndent()
private val IMPORTS = """
import net.mamoe.mirai.console.setting.internal.valueImpl
import kotlin.internal.LowPriorityInOverloadResolution
""".trimIndent()

fun genAllValueUseSite(): String = buildString {
    fun appendln(@Language("kt") code: String) {
        this.appendLine(code.trimIndent())
    }
    // PRIMITIVE
    for (number in NUMBERS + OTHER_PRIMITIVES) {
        appendln(genValueUseSite(number, number))
    }

    // PRIMITIVE ARRAYS
    for (number in NUMBERS + OTHER_PRIMITIVES.filterNot { it == "String" }) {
        appendln(genValueUseSite("${number}Array", "${number}Array"))
    }

    // TYPED ARRAYS
    for (number in NUMBERS + OTHER_PRIMITIVES) {
        appendln(genValueUseSite("Array<${number}>", "Typed${number}Array"))
    }

    // PRIMITIVE LISTS / PRIMITIVE SETS
    for (collectionName in listOf("List", "Set")) {
        for (number in NUMBERS + OTHER_PRIMITIVES) {
            appendln(genValueUseSite("${collectionName}<${number}>", "${number}${collectionName}"))
        }
    }

    // MUTABLE LIST / MUTABLE SET
    for (collectionName in listOf("List", "Set")) {
        for (number in NUMBERS + OTHER_PRIMITIVES) {
            appendLine()
            appendln(
                """
                @JvmName("valueMutable")
                fun Setting.value(default: Mutable${collectionName}<${number}>): Mutable${number}${collectionName}Value = valueImpl(default)
            """.trimIndent()
            )
        }
    }

    // SPECIAL
    appendLine()
    appendln(
        """
            fun <T : Setting> Setting.value(default: T): Value<T> {
                require(this::class != default::class) {
                    "Recursive nesting is prohibited"
                }
                return valueImpl(default).also {
                    if (default is Setting.NestedSetting) {
                        default.attachedValue = it
                    }
                }
            }

            inline fun <T : Setting> Setting.value(default: T, crossinline initializer: T.() -> Unit): Value<T> =
                value(default).also { it.value.apply(initializer) }

            inline fun <reified T : Setting> Setting.value(default: List<T>): SettingListValue<T> = valueImpl(default)

            @JvmName("valueMutable")
            inline fun <reified T : Setting> Setting.value(default: MutableList<T>): MutableSettingListValue<T> = valueImpl(default)


            inline fun <reified T : Setting> Setting.value(default: Set<T>): SettingSetValue<T> = valueImpl(default)

            @JvmName("valueMutable")
            inline fun <reified T : Setting> Setting.value(default: MutableSet<T>): MutableSettingSetValue<T> = valueImpl(default)
            
            /**
             * 创建一个只引用对象而不跟踪其属性的值.
             *
             * @param T 类型. 必须拥有 [kotlinx.serialization.Serializable] 注解 (因此编译器会自动生成序列化器)
             */
            @DangerousReferenceOnlyValue
            @JvmName("valueDynamic")
            @LowPriorityInOverloadResolution
            inline fun <reified T : Any> Setting.value(default: T): Value<T> = valueImpl(default)
            
            @RequiresOptIn(
                ""${'"'}
                这种只保存引用的 Value 可能会导致意料之外的结果, 在使用时须保持谨慎.
                对值的改变不会触发自动保存, 也不会同步到 UI 中. 在 UI 中只能编辑序列化之后的值.
            ""${'"'}, level = RequiresOptIn.Level.WARNING
            )
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.FUNCTION)
            annotation class DangerousReferenceOnlyValue
        """
    )
}

fun genValueUseSite(kotlinTypeName: String, miraiValueName: String): String =
    """
        fun Setting.value(default: $kotlinTypeName): ${miraiValueName}Value = valueImpl(default)
    """.trimIndent()

