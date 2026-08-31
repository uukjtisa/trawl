package com.junkfood.seal.util

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// Cover art for audio extracted from a resolved download, made from the video itself.
//
// WHY NOT FETCH THE PLATFORM'S THUMBNAIL. That was the first attempt and it does not work, for
// two separate reasons measured on 2026-08-31:
//
//   1. --embed-thumbnail embeds from yt-dlp's `thumbnails` LIST, assembled during extraction. A
//      resolved download points yt-dlp at a bare CDN file whose generic extractor reports none,
//      and setting the scalar `thumbnail` field afterwards with --parse-metadata changes nothing.
//      Confirmed on device: a 95,979-byte m4a with no covr atom and no JPEG anywhere in it.
//   2. DirectFileCdn has no thumbnail to fetch AT ALL. It resolves an anonymous file on a CDN.
//      There is no page, no metadata and nobody to ask.
//
// The video is right there. Taking a frame from it needs no cooperation from the platform, works
// identically for every resolver including the anonymous one, and cannot go stale.
//
// WHY IT IS A SEPARATE STEP AND NOT --ppa. yt-dlp's ExtractAudio post-processor passes -vn, so
// appending `-map 0:v` to it fights an argument already on the command line. Running afterwards
// also means this cannot break a download: it operates on a file that has already been written,
// every failure path leaves that file exactly as it was, and the worst outcome is the blank cover
// we have today.

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "CoverArt"

/** Containers that can carry attached cover art. Ogg and Opus cannot, in any portable way. */
private val COVER_CAPABLE = setOf("m4a", "mp3", "mp4", "flac")

object CoverArt {

    /**
     * The bundled ffmpeg.
     *
     * Shipped by youtubedl-android's ffmpeg module as a JNI library so that Android extracts it to
     * the native library directory and marks it executable -- which is the only place an app may
     * execute a binary from on a modern device. The wrapper class exposes init() and nothing else,
     * so the binary is invoked directly rather than through it.
     */
    private fun binary(context: Context): File? =
        File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so").takeIf {
            it.exists() && it.canExecute()
        }

    /**
     * Where the ffmpeg shared objects are unpacked.
     *
     * The binary in the native library directory is DYNAMICALLY linked, and its libraries are not
     * beside it -- youtubedl-android unpacks them under no_backup at runtime. Without them on
     * LD_LIBRARY_PATH the process dies before main() with
     *
     *     CANNOT LINK EXECUTABLE "libffmpeg.so": library "libavdevice.so.61" not found
     *
     * BOTH package directories are needed, which is the part that is not obvious. ffmpeg's own
     * usr/lib satisfies libav*, and then libfontconfig inside it pulls libexpat.so.1, which ships
     * with the PYTHON package instead. Listing only ffmpeg's gets you past the first error
     * straight into the second one.
     */
    private fun libDirs(context: Context): List<File> =
        listOf(
            File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib"),
            File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr/lib"),
        )

    private fun run(context: Context, args: List<String>, timeoutSeconds: Long = 60): Boolean {
        val bin = binary(context) ?: return false
        return runCatching {
                val builder =
                    ProcessBuilder(listOf(bin.absolutePath) + args).redirectErrorStream(true)
                val libs = libDirs(context).filter { it.isDirectory }
                if (libs.isEmpty()) {
                    TrawlLog.i("$TAG: ffmpeg libraries are not unpacked yet")
                    return false
                }
                val existing = builder.environment()["LD_LIBRARY_PATH"]
                builder.environment()["LD_LIBRARY_PATH"] =
                    (libs.map { it.absolutePath } + listOfNotNull(existing?.takeIf { it.isNotBlank() }))
                        .joinToString(":")
                val p = builder.start()
                // Drained rather than ignored: a filled pipe buffer deadlocks the child, and a
                // deadlocked ffmpeg would hold the timeout open for no reason.
                val out = p.inputStream.bufferedReader().use { it.readText() }
                val finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                if (!finished) {
                    p.destroyForcibly()
                    TrawlLog.i("$TAG: ffmpeg timed out after ${timeoutSeconds}s")
                    return false
                }
                if (p.exitValue() != 0) {
                    TrawlLog.i("$TAG: ffmpeg exited ${p.exitValue()} - ${out.takeLast(240)}")
                    false
                } else true
            }
            .getOrElse {
                TrawlLog.i("$TAG: ffmpeg could not be run - ${it.message}")
                false
            }
    }

    /** `-headers` wants one blob with CRLF between fields. Blank values are skipped. */
    private fun headerBlob(headers: Map<String, String>): String? =
        headers
            .filterValues { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString("\r\n") { "${it.key}: ${it.value}" }
            ?.plus("\r\n")

    /**
     * Take a frame from [sourceUrl] and attach it to [audioFile] as cover art.
     *
     * Returns true only when the audio file was actually replaced by one carrying the picture.
     * Every other outcome leaves it untouched.
     *
     * The seek is one second in, not zero. A great many clips open on a black or near-black frame,
     * and a black square is a worse cover than none -- it looks like a bug rather than an absence.
     */
    fun embedFrameFrom(
        context: Context,
        sourceUrl: String,
        headers: Map<String, String>,
        audioFile: File,
        seekSeconds: Int = 1,
    ): Boolean {
        if (!audioFile.exists() || audioFile.length() == 0L) return false
        if (audioFile.extension.lowercase() !in COVER_CAPABLE) {
            TrawlLog.i("$TAG: ${audioFile.extension} cannot carry attached art, skipping")
            return false
        }

        val work = File(audioFile.parentFile, ".trawl-cover-${audioFile.nameWithoutExtension.hashCode()}")
        val frame = File(work.absolutePath + ".jpg")
        val merged = File(work.absolutePath + "." + audioFile.extension)

        try {
            val blob = headerBlob(headers)
            val grab = buildList {
                add("-y")
                if (blob != null) { add("-headers"); add(blob) }
                // Seeking BEFORE -i keeps this cheap: ffmpeg jumps rather than decoding up to the
                // point, which over a network source is the difference between one range request
                // and streaming the whole file.
                add("-ss"); add(seekSeconds.toString())
                add("-i"); add(sourceUrl)
                add("-frames:v"); add("1")
                add("-q:v"); add("3")
                add("-vf"); add("scale='min(600,iw)':-2")
                add(frame.absolutePath)
            }
            if (!run(context, grab) || !frame.exists() || frame.length() == 0L) {
                // A clip shorter than the seek yields nothing. Worth one retry from the very
                // start before giving up, since that case is common on short-form video.
                val fallback = grab.toMutableList()
                val i = fallback.indexOf("-ss")
                if (i >= 0) fallback[i + 1] = "0"
                if (!run(context, fallback) || !frame.exists() || frame.length() == 0L) {
                    TrawlLog.i("$TAG: no frame could be taken from the source")
                    return false
                }
            }

            val embed =
                listOf(
                    "-y",
                    "-i", audioFile.absolutePath,
                    "-i", frame.absolutePath,
                    "-map", "0:a",
                    "-map", "1:v",
                    // Copy the audio: this step must not re-encode. Re-encoding to attach a
                    // picture would cost quality for a thumbnail, which is a bad trade.
                    "-c:a", "copy",
                    "-c:v", "mjpeg",
                    "-disposition:v:0", "attached_pic",
                    "-map_metadata", "0",
                    merged.absolutePath,
                )
            if (!run(context, embed) || !merged.exists() || merged.length() == 0L) {
                TrawlLog.i("$TAG: the picture could not be attached")
                return false
            }

            // Only now is the original touched, and only by a rename over it.
            val ok = merged.renameTo(audioFile) || (audioFile.delete() && merged.renameTo(audioFile))
            if (ok) TrawlLog.i("$TAG: cover art attached to ${audioFile.name}")
            return ok
        } finally {
            runCatching { if (frame.exists()) frame.delete() }
            runCatching { if (merged.exists()) merged.delete() }
        }
    }
}
