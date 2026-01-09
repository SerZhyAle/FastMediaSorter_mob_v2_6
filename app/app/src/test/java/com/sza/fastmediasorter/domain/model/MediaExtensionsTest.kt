package com.sza.fastmediasorter.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MediaExtensions utility object.
 * Tests file extension detection and categorization.
 */
class MediaExtensionsTest {

    @Test
    fun `isImage returns true for common image formats`() {
        assertTrue("Expected jpg", MediaExtensions.isImage("jpg"))
        assertTrue("Expected jpeg", MediaExtensions.isImage("jpeg"))
        assertTrue("Expected png", MediaExtensions.isImage("png"))
        assertTrue("Expected gif", MediaExtensions.isImage("gif"))
        assertTrue("Expected bmp", MediaExtensions.isImage("bmp"))
        assertTrue("Expected webp", MediaExtensions.isImage("webp"))
    }

    @Test
    fun `isImage returns true for RAW formats`() {
        assertTrue("Expected raw", MediaExtensions.isImage("raw"))
        assertTrue("Expected cr2", MediaExtensions.isImage("cr2"))
        assertTrue("Expected nef", MediaExtensions.isImage("nef"))
        assertTrue("Expected arw", MediaExtensions.isImage("arw"))
        assertTrue("Expected dng", MediaExtensions.isImage("dng"))
    }

    @Test
    fun `isImage is case insensitive`() {
        assertTrue("Expected JPG", MediaExtensions.isImage("JPG"))
        assertTrue("Expected Png", MediaExtensions.isImage("Png"))
        assertTrue("Expected JPEG", MediaExtensions.isImage("JPEG"))
    }

    @Test
    fun `isImage returns false for non-image formats`() {
        assertFalse("Expected false for mp4", MediaExtensions.isImage("mp4"))
        assertFalse("Expected false for mp3", MediaExtensions.isImage("mp3"))
        assertFalse("Expected false for txt", MediaExtensions.isImage("txt"))
        assertFalse("Expected false for pdf", MediaExtensions.isImage("pdf"))
    }

    @Test
    fun `isVideo returns true for common video formats`() {
        assertTrue("Expected mp4", MediaExtensions.isVideo("mp4"))
        assertTrue("Expected mkv", MediaExtensions.isVideo("mkv"))
        assertTrue("Expected avi", MediaExtensions.isVideo("avi"))
        assertTrue("Expected mov", MediaExtensions.isVideo("mov"))
        assertTrue("Expected wmv", MediaExtensions.isVideo("wmv"))
        assertTrue("Expected webm", MediaExtensions.isVideo("webm"))
    }

    @Test
    fun `isVideo returns true for less common formats`() {
        assertTrue("Expected 3gp", MediaExtensions.isVideo("3gp"))
        assertTrue("Expected mts", MediaExtensions.isVideo("mts"))
        assertTrue("Expected vob", MediaExtensions.isVideo("vob"))
        assertTrue("Expected ogv", MediaExtensions.isVideo("ogv"))
    }

    @Test
    fun `isVideo is case insensitive`() {
        assertTrue("Expected MP4", MediaExtensions.isVideo("MP4"))
        assertTrue("Expected Mkv", MediaExtensions.isVideo("Mkv"))
        assertTrue("Expected AVI", MediaExtensions.isVideo("AVI"))
    }

    @Test
    fun `isVideo returns false for non-video formats`() {
        assertFalse("Expected false for jpg", MediaExtensions.isVideo("jpg"))
        assertFalse("Expected false for mp3", MediaExtensions.isVideo("mp3"))
        assertFalse("Expected false for txt", MediaExtensions.isVideo("txt"))
    }

    @Test
    fun `isAudio returns true for common audio formats`() {
        assertTrue("Expected mp3", MediaExtensions.isAudio("mp3"))
        assertTrue("Expected wav", MediaExtensions.isAudio("wav"))
        assertTrue("Expected flac", MediaExtensions.isAudio("flac"))
        assertTrue("Expected aac", MediaExtensions.isAudio("aac"))
        assertTrue("Expected ogg", MediaExtensions.isAudio("ogg"))
        assertTrue("Expected m4a", MediaExtensions.isAudio("m4a"))
    }

    @Test
    fun `isAudio returns true for less common formats`() {
        assertTrue("Expected opus", MediaExtensions.isAudio("opus"))
        assertTrue("Expected ape", MediaExtensions.isAudio("ape"))
        assertTrue("Expected midi", MediaExtensions.isAudio("midi"))
        assertTrue("Expected mka", MediaExtensions.isAudio("mka"))
    }

    @Test
    fun `isAudio is case insensitive`() {
        assertTrue("Expected MP3", MediaExtensions.isAudio("MP3"))
        assertTrue("Expected Flac", MediaExtensions.isAudio("Flac"))
        assertTrue("Expected WAV", MediaExtensions.isAudio("WAV"))
    }

    @Test
    fun `isAudio returns false for non-audio formats`() {
        assertFalse("Expected false for jpg", MediaExtensions.isAudio("jpg"))
        assertFalse("Expected false for mp4", MediaExtensions.isAudio("mp4"))
        assertFalse("Expected false for txt", MediaExtensions.isAudio("txt"))
    }

    @Test
    fun `isMedia returns true for any media format`() {
        assertTrue("Expected true for jpg", MediaExtensions.isMedia("jpg"))
        assertTrue("Expected true for mp4", MediaExtensions.isMedia("mp4"))
        assertTrue("Expected true for mp3", MediaExtensions.isMedia("mp3"))
        assertTrue("Expected true for png", MediaExtensions.isMedia("png"))
        assertTrue("Expected true for mkv", MediaExtensions.isMedia("mkv"))
        assertTrue("Expected true for flac", MediaExtensions.isMedia("flac"))
    }

    @Test
    fun `isMedia returns false for non-media formats`() {
        assertFalse("Expected false for txt", MediaExtensions.isMedia("txt"))
        assertFalse("Expected false for pdf", MediaExtensions.isMedia("pdf"))
        assertFalse("Expected false for doc", MediaExtensions.isMedia("doc"))
        assertFalse("Expected false for zip", MediaExtensions.isMedia("zip"))
    }

    @Test
    fun `isMedia is case insensitive`() {
        assertTrue("Expected true for JPG", MediaExtensions.isMedia("JPG"))
        assertTrue("Expected true for MP4", MediaExtensions.isMedia("MP4"))
        assertTrue("Expected true for Mp3", MediaExtensions.isMedia("Mp3"))
    }

    @Test
    fun `getMediaType returns correct type for images`() {
        assertEquals("image", MediaExtensions.getMediaType("jpg"))
        assertEquals("image", MediaExtensions.getMediaType("png"))
        assertEquals("image", MediaExtensions.getMediaType("gif"))
        assertEquals("image", MediaExtensions.getMediaType("webp"))
    }

    @Test
    fun `getMediaType returns correct type for videos`() {
        assertEquals("video", MediaExtensions.getMediaType("mp4"))
        assertEquals("video", MediaExtensions.getMediaType("mkv"))
        assertEquals("video", MediaExtensions.getMediaType("avi"))
        assertEquals("video", MediaExtensions.getMediaType("mov"))
    }

    @Test
    fun `getMediaType returns correct type for audio`() {
        assertEquals("audio", MediaExtensions.getMediaType("mp3"))
        assertEquals("audio", MediaExtensions.getMediaType("wav"))
        assertEquals("audio", MediaExtensions.getMediaType("flac"))
        assertEquals("audio", MediaExtensions.getMediaType("ogg"))
    }

    @Test
    fun `getMediaType returns null for non-media`() {
        assertNull("Expected null for txt", MediaExtensions.getMediaType("txt"))
        assertNull("Expected null for pdf", MediaExtensions.getMediaType("pdf"))
        assertNull("Expected null for doc", MediaExtensions.getMediaType("doc"))
    }

    @Test
    fun `getMediaType is case insensitive`() {
        assertEquals("image", MediaExtensions.getMediaType("JPG"))
        assertEquals("video", MediaExtensions.getMediaType("MP4"))
        assertEquals("audio", MediaExtensions.getMediaType("MP3"))
    }

    @Test
    fun `IMAGE set contains expected extensions`() {
        val expectedImages = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif",
            "tiff", "tif", "ico", "svg", "raw", "cr2", "nef", "arw", "dng"
        )
        assertEquals("Expected IMAGE set to match", expectedImages, MediaExtensions.IMAGE)
    }

    @Test
    fun `VIDEO set contains expected extensions`() {
        val expectedVideos = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
            "mpg", "mpeg", "3gp", "3g2", "mts", "m2ts", "vob", "ogv"
        )
        assertEquals("Expected VIDEO set to match", expectedVideos, MediaExtensions.VIDEO)
    }

    @Test
    fun `AUDIO set contains expected extensions`() {
        val expectedAudio = setOf(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus",
            "oga", "mid", "midi", "ape", "mka"
        )
        assertEquals("Expected AUDIO set to match", expectedAudio, MediaExtensions.AUDIO)
    }

    @Test
    fun `ALL_MEDIA contains all image video and audio extensions`() {
        val expected = MediaExtensions.IMAGE + MediaExtensions.VIDEO + MediaExtensions.AUDIO
        assertEquals("Expected ALL_MEDIA to be union", expected, MediaExtensions.ALL_MEDIA)
    }

    @Test
    fun `ALL_MEDIA has no duplicates`() {
        val allCount = MediaExtensions.ALL_MEDIA.size
        val combinedCount = MediaExtensions.IMAGE.size + MediaExtensions.VIDEO.size + MediaExtensions.AUDIO.size
        assertEquals("Expected no duplicates", combinedCount, allCount)
    }

    @Test
    fun `empty string is not media`() {
        assertFalse("Expected false for empty", MediaExtensions.isMedia(""))
        assertFalse("Expected false for empty", MediaExtensions.isImage(""))
        assertFalse("Expected false for empty", MediaExtensions.isVideo(""))
        assertFalse("Expected false for empty", MediaExtensions.isAudio(""))
    }

    @Test
    fun `getMediaType returns null for empty string`() {
        assertNull("Expected null for empty", MediaExtensions.getMediaType(""))
    }

    @Test
    fun `extension with dots is handled correctly`() {
        // Some systems might pass ".jpg" instead of "jpg"
        assertFalse("Expected false for .jpg (with dot)", MediaExtensions.isImage(".jpg"))
        // Note: The implementation expects extension without dot
    }

    @Test
    fun `special characters in extension return false`() {
        assertFalse("Expected false for jpg!", MediaExtensions.isMedia("jpg!"))
        assertFalse("Expected false for mp4?", MediaExtensions.isMedia("mp4?"))
        assertFalse("Expected false for mp3#", MediaExtensions.isMedia("mp3#"))
    }
}
