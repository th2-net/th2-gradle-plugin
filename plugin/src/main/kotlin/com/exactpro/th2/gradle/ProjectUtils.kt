package com.exactpro.th2.gradle

import org.gradle.api.plugins.PluginManager

/**
 * Returns `true` if at least one of the specified plugins is applied
 */
fun PluginManager.hasAnyPlugin(vararg ids: String): Boolean {
    return ids.any(::hasPlugin)
}