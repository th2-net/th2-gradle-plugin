package com.exactpro.th2.gradle

import java.io.File

internal operator fun File.div(path: String): File = resolve(path)
