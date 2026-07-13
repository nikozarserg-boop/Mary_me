package org.example.animation.io

/**
 * JVM реализация проверки разрешений.
 * На desktop-платформе разрешения не требуются.
 */
actual fun hasStoragePermissions(): Boolean = true

actual fun requestStoragePermissions(onComplete: (Boolean) -> Unit) {
    onComplete(true)
}