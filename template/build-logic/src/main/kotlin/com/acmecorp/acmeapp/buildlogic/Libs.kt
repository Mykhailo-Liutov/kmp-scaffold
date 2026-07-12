package com.acmecorp.acmeapp.buildlogic

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.use.PluginDependency

val Project.libs: LibrariesForLibs get() = the<LibrariesForLibs>()

fun PluginManager.apply(plugin: Provider<PluginDependency>) = apply(plugin.get().pluginId)
