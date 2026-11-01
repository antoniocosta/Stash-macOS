package android.view.accessibility

/** Desktop shim for AccessibilityManager (Android TalkBack state; VoiceOver is not reported). */
class AccessibilityManager internal constructor() {
    val isEnabled: Boolean get() = false
    val isTouchExplorationEnabled: Boolean get() = false
}
