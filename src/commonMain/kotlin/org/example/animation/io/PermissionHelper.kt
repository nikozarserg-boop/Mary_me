package org.example.animation.io

/**
 * Кроссплатформенная проверка разрешений к файлам.
 * Возвращает true, если есть доступ к файлам для текущей платформы.
 */
expect fun hasStoragePermissions(): Boolean

/**
 * Инициализирует проверку разрешений.
 * На Android - реализовано через Activity.
 * На других платформах - всегда true.
 */
expect fun requestStoragePermissions(onComplete: (Boolean) -> Unit)