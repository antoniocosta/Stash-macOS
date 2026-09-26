package android.app

/** Desktop shim for android.app.NotificationChannel (kept by [NotificationManager] for bookkeeping only). */
class NotificationChannel(val id: String, var name: CharSequence, var importance: Int) {
    var description: String? = null
    private var showBadge = true

    fun setShowBadge(showBadge: Boolean) { this.showBadge = showBadge }
    fun canShowBadge(): Boolean = showBadge
}
