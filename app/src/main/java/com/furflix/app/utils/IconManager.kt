package com.furflix.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.furflix.app.ui.theme.AppIcon

object IconManager {
    fun applyPendingIcon(context: Context, newIcon: AppIcon) {
        val packageManager = context.packageManager
        
        // Check if newIcon is already enabled
        val componentName = ComponentName(context, newIcon.aliasName)
        if (packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return // Already active, no need to touch the package manager
        }

        // Enable the new icon alias
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        
        // Disable all other aliases
        AppIcon.entries.filter { it != newIcon }.forEach { icon ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context, icon.aliasName),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
