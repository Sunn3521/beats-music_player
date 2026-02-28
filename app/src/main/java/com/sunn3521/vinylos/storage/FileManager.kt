package com.sunn3521.vinylos.storage

import android.content.Context
import com.sunn3521.vinylos.util.Constants
import java.io.File

object FileManager {

    private fun getMusicDirectory(context: Context): File {
        val musicDir = File(context.filesDir, Constants.MUSIC_FOLDER)
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }
        return musicDir
    }

    fun getLocalFile(context: Context, fileName: String): File {
        return File(getMusicDirectory(context), fileName)
    }
}