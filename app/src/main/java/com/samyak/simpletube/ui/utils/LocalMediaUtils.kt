package com.samyak.simpletube.ui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.samyak.simpletube.models.DirectoryTree
import com.samyak.simpletube.utils.cache
import com.samyak.simpletube.utils.retrieveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

const val TAG = "LocalMediaUtils"

/**
 * For easier debugging, set SCANNER_CRASH_AT_FIRST_ERROR to stop at first error
 */
const val SCANNER_CRASH_AT_FIRST_ERROR = false // crash at first FFmpeg scanner error. Currently not implemented
const val SYNC_SCANNER = false // true will not use multithreading for scanner
const val MAX_CONCURRENT_JOBS = 4
const val SCANNER_DEBUG = false

const val EXTRACTOR_DEBUG = false
const val DEBUG_SAVE_OUTPUT = false // ignored (will be false) when EXTRACTOR_DEBUG IS false
const val EXTRACTOR_TAG = "MetadataExtractor"

@OptIn(ExperimentalCoroutinesApi::class)
val scannerSession = Dispatchers.IO.limitedParallelism(MAX_CONCURRENT_JOBS)

// stuff to make this work
const val STORAGE_ROOT = "/storage/"
const val DEFAULT_SCAN_PATH = "/tree/primary:Music\n"
val ARTIST_SEPARATORS = Regex("\\s*;\\s*|\\s*ft\\.\\s*|\\s*feat\\.\\s*|\\s*&\\s*|\\s*,\\s*", RegexOption.IGNORE_CASE)
private var cachedDirectoryTree: DirectoryTree? = null


/**
 * ==========================
 * Various misc helpers
 * ==========================
 */


/**
 * Extract the album art from the audio file. The image is not resized
 * (did you mean to use getLocalThumbnail(path: String?, resize: Boolean)?).
 *
 * @param path Full path of audio file
 */
fun getLocalThumbnail(path: String?): Bitmap? = getLocalThumbnail(path, false)

/**
 * Extract the album art from the audio file
 *
 * @param path Full path of audio file
 * @param resize Whether to resize the Bitmap to a thumbnail size (300x300)
 */
fun getLocalThumbnail(path: String?, resize: Boolean): Bitmap? {
    if (path == null) {
        return null
    }
    // try cache lookup
    val cachedImage = if (resize) {
        retrieveImage(path)?.resizedImage
    } else {
        retrieveImage(path)?.image
    }

    if (cachedImage == null) {
//        Timber.tag(TAG).d("Cache miss on $path")
    } else {
        return cachedImage
    }

    val mData = MediaMetadataRetriever()

    var image: Bitmap = try {
        mData.setDataSource(path)
        val art = mData.embeddedPicture
        BitmapFactory.decodeByteArray(art, 0, art!!.size)
    } catch (e: Exception) {
        cache(path, null, resize)
        null
    } ?: return null

    if (resize) {
        image = Bitmap.createScaledBitmap(image, 100, 100, false)
    }

    cache(path, image, resize)
    return image
}


/**
 * Get cached DirectoryTree
 */
fun getDirectoryTree(): DirectoryTree? {
    if (cachedDirectoryTree == null) {
        return null
    }
    return cachedDirectoryTree
}

/**
 * Cache a DirectoryTree
 */
fun cacheDirectoryTree(new: DirectoryTree?) {
    cachedDirectoryTree = new
}