@file:JvmName("NavTypeConverter_nonAndroidKt")
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package androidx.navigation.serialization

import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import kotlinx.serialization.descriptors.SerialDescriptor

@Suppress("UNCHECKED_CAST")
private fun SavedState.rawMap(): MutableMap<String, Any?> =
    SavedState::class.java.getMethod("getMap").invoke(this) as MutableMap<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun resolveEnumClass(serialName: String): Class<Enum<*>>? {
    val cleanName = serialName.removeSuffix("?")
    val candidates = listOf(
        cleanName,
        cleanName.substringBeforeLast('.') + "$" + cleanName.substringAfterLast('.'),
    )
    for (candidate in candidates) {
        val clazz = runCatching { Class.forName(candidate) }.getOrNull()
        if (clazz != null && Enum::class.java.isAssignableFrom(clazz)) {
            return clazz as Class<Enum<*>>
        }
    }
    return null
}

private class DesktopEnumNavType<D : Enum<*>>(
    private val enumClass: Class<D>,
    isNullableAllowed: Boolean,
) : NavType<D?>(isNullableAllowed) {

    override val name: String
        get() = enumClass.name

    override fun put(bundle: SavedState, key: String, value: D?) {
        bundle.rawMap()[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(bundle: SavedState, key: String): D? {
        val raw = bundle.rawMap()[key] ?: return null
        if (enumClass.isInstance(raw)) return raw as D
        if (raw is String) return parseValue(raw)
        return null
    }

    override fun parseValue(value: String): D? {
        if (value == "null" && isNullableAllowed) return null
        return enumClass.enumConstants?.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Enum value $value not found for type ${enumClass.name}."
        )
    }

    override fun serializeAsValue(value: D?): String = value?.name ?: "null"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesktopEnumNavType<*>) return false
        return enumClass == other.enumClass && isNullableAllowed == other.isNullableAllowed
    }

    override fun hashCode(): Int = 31 * enumClass.hashCode() + isNullableAllowed.hashCode()
}

private class DesktopEnumListNavType<D : Enum<*>>(
    private val enumClass: Class<D>,
) : CollectionNavType<List<D>>(isNullableAllowed = false) {

    override val name: String
        get() = "List<${enumClass.name}>"

    override fun put(bundle: SavedState, key: String, value: List<D>) {
        bundle.rawMap()[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(bundle: SavedState, key: String): List<D>? {
        val raw = bundle.rawMap()[key] ?: return null
        if (raw is List<*>) {
            return raw.mapNotNull { item ->
                when {
                    enumClass.isInstance(item) -> item as D
                    item is String -> parseElement(item)
                    else -> null
                }
            }
        }
        return null
    }

    override fun parseValue(value: String): List<D> = listOf(parseElement(value))

    override fun parseValue(value: String, previousValue: List<D>): List<D> =
        previousValue + parseElement(value)

    override fun serializeAsValues(value: List<D>): List<String> = value.map { it.name }

    override fun emptyCollection(): List<D> = emptyList()

    private fun parseElement(value: String): D =
        enumClass.enumConstants?.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Enum value $value not found for type ${enumClass.name}."
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesktopEnumListNavType<*>) return false
        return enumClass == other.enumClass
    }

    override fun hashCode(): Int = enumClass.hashCode()
}

fun SerialDescriptor.parseEnum(): NavType<*> {
    val enumClass = resolveEnumClass(serialName) ?: return DesktopEnumFallbackNavType
    return DesktopEnumNavType(enumClass, isNullableAllowed = false)
}

fun SerialDescriptor.parseNullableEnum(): NavType<*> {
    val enumClass = resolveEnumClass(serialName) ?: return DesktopEnumFallbackNavType
    return DesktopEnumNavType(enumClass, isNullableAllowed = true)
}

fun SerialDescriptor.parseEnumList(): NavType<*> {
    val elementSerialName = if (elementsCount > 0) getElementDescriptor(0).serialName else return DesktopEnumFallbackNavType
    val enumClass = resolveEnumClass(elementSerialName) ?: return DesktopEnumFallbackNavType
    return DesktopEnumListNavType(enumClass)
}

private val DesktopEnumFallbackNavType: NavType<*> by lazy {
    Class.forName("androidx.navigation.serialization.UNKNOWN")
        .getDeclaredField("INSTANCE")
        .get(null) as NavType<*>
}
