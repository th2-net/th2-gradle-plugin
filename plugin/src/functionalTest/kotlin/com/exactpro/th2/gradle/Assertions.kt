package com.exactpro.th2.gradle

import java.io.File
import kotlin.test.assertTrue

internal fun assertFileExist(file: File) {
    assertTrue(file.exists(), "file '$file' does not exist")
}
