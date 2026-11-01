package android.content.res

import android.util.DisplayMetrics
import com.stash.desktop.res.DesktopDisplay
import com.stash.desktop.res.DesktopResources
import java.io.InputStream
import java.util.Locale
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser

/**
 * Desktop shim for android.content.res.Resources over the generated resource table
 * ([DesktopResources]). Each instance resolves against the [Configuration] its Context sees
 * (system values plus any override), so locale and density overrides behave as on Android.
 */
open class Resources internal constructor(private val configurationSource: () -> Configuration) {

    open val configuration: Configuration get() = configurationSource()

    open val displayMetrics: DisplayMetrics
        get() {
            val c = configuration
            return DisplayMetrics().apply {
                densityDpi = c.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
                density = densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
                scaledDensity = density * (c.fontScale.takeIf { it > 0f } ?: 1f)
                widthPixels = (c.screenWidthDp * density).toInt()
                heightPixels = (c.screenHeightDp * density).toInt()
                xdpi = densityDpi.toFloat()
                ydpi = densityDpi.toFloat()
            }
        }

    private val locale: Locale get() = configuration.locales[0] ?: Locale.getDefault()

    open fun getString(id: Int): String = DesktopResources.string(id, locale)

    open fun getString(id: Int, vararg formatArgs: Any?): String = String.format(locale, getString(id), *formatArgs)

    open fun getText(id: Int): CharSequence = getString(id)

    open fun getQuantityString(id: Int, quantity: Int): String = DesktopResources.quantityString(id, quantity, locale)

    open fun getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String =
        String.format(locale, getQuantityString(id, quantity), *formatArgs)

    open fun getQuantityText(id: Int, quantity: Int): CharSequence = getQuantityString(id, quantity)

    /** The generated `R.<defType>.<name>` id of [defPackage]'s namespace, or 0 (Android's "not found"). */
    open fun getIdentifier(name: String, defType: String, defPackage: String): Int =
        runCatching { Class.forName("$defPackage.R\$$defType").getField(name).getInt(null) }.getOrDefault(0)

    open fun openRawResource(id: Int): InputStream = DesktopResources.open(id)

    open fun getXml(id: Int): XmlResourceParser = XmlResourceParser(DesktopResources.open(id))

    class NotFoundException(message: String? = null) : RuntimeException(message)

    companion object {
        private val SYSTEM = Resources { DesktopDisplay.configuration() }

        @JvmStatic
        fun getSystem(): Resources = SYSTEM
    }
}

/** Android's compiled-XML parser, here a namespace-aware pull parser over the resource's source XML. */
class XmlResourceParser private constructor(private val stream: InputStream, parser: KXmlParser) :
    XmlPullParser by parser,
    AutoCloseable {
    internal constructor(stream: InputStream) : this(
        stream,
        KXmlParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(stream, null)
        },
    )

    override fun close() = stream.close()
}
