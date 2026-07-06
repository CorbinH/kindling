package io.github.inductiveautomation.kindling.docker.engine

/**
 * A snapshot of image-pull progress, emitted while the Engine API streams layer events.
 *
 * Byte totals are summed across all layers of the image currently being pulled, so they grow as
 * Docker discovers new layers. [totalBytes] is 0 until at least one layer reports a total.
 */
data class PullProgress(
    /** The image reference being pulled, e.g. `inductiveautomation/ignition:8.3.7`. */
    val image: String,
    /** 1-based position of this image among all images in the stack. */
    val imageIndex: Int,
    /** Total number of images that will be pulled for this stack. */
    val imageCount: Int,
    /** Bytes downloaded so far, summed across layers. */
    val downloadedBytes: Long,
    /** Best-known total download size, summed across layers (0 until known). */
    val totalBytes: Long,
    /** The most recent Docker status string, e.g. "Downloading", "Extracting". */
    val status: String,
) {
    /** Download fraction in `[0, 1]` for this image, or `null` while the total is unknown. */
    val fraction: Double?
        get() = if (totalBytes > 0) (downloadedBytes.toDouble() / totalBytes).coerceIn(0.0, 1.0) else null
}