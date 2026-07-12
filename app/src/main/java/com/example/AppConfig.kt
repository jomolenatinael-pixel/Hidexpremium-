package com.example

import android.content.Context

object AppConfig {
    const val DEVELOPER_NAME = "Natinael Jomole"
    const val DEVELOPER_EMAIL = "natijommar@gmail.com"

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
