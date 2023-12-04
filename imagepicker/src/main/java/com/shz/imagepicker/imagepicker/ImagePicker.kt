@file:Suppress("unused", "NOTHING_TO_INLINE")

package com.shz.imagepicker.imagepicker

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.shz.imagepicker.imagepicker.exception.NothingToLaunchException
import com.shz.imagepicker.imagepicker.model.GalleryPicker
import com.shz.imagepicker.imagepicker.model.PickedResult
import java.io.File

/**
 * Main **ImagePicker** SDK class.
 * Contains configuration models and main business logic of.
 *
 * @see ImagePicker.Builder
 *
 * @author ShiftHackZ (Dmitriy Moroz)
 */
class ImagePicker private constructor(
    private val authority: String,
    private val callback: ImagePickerCallback,
    private val errorCallback: ImagePickerErrorCallback,
    private val useCamera: Boolean,
    private val useGallery: Boolean,
    private val autoRotate: Boolean,
    private val multipleSelection: Boolean,
    private val minimumSelectionCount: Int,
    private val maximumSelectionCount: Int,
    private val galleryPicker: GalleryPicker,
    private val loadDelegate: ImagePickerLoadDelegate,
) {
    /**
     * Checks for access Camera or Media permissions, then
     * launches **ImagePicker** according to defined configuration.
     *
     * Possible use cases:
     * - When only Builder.useCamera(true) launches Camera Picker;
     * - When only Builder.useGallery(true) launches Native or Custom Gallery Picker;
     * - When both Builder.useCamera(true) and Builder.useGallery(true) interactive dialog will be launched;
     * - Otherwise, will deliver [NothingToLaunchException] as [PickedResult.Error] to [ImagePickerCallback].
     *
     * @param context it is recommended to pass Activity [Context] here.
     *
     * @see ImagePicker.Builder
     */
    fun launch(context: Context) = with(ImagePickerLauncher(context)) {
        when {
            useCamera && !useGallery -> launchCameraPicker(authority, callback, errorCallback)
            !useCamera && useGallery -> launchGalleryPicker(
                callback = callback,
                delegate = loadDelegate,
                multipleSelection = multipleSelection,
                autoRotate = autoRotate,
                minimum = minimumSelectionCount,
                maximum = maximumSelectionCount,
                galleryPicker = galleryPicker,
            )
            useCamera && useGallery -> launchDialog(
                authority = authority,
                callback = callback,
                delegate = loadDelegate,
                multipleSelection = multipleSelection,
                autoRotate = autoRotate,
                minimum = minimumSelectionCount,
                maximum = maximumSelectionCount,
                galleryPicker = galleryPicker,
            )
            else -> deliverThrowable(callback, NothingToLaunchException())
        }
    }

    /**
     * Builder class that allows to configure and build [ImagePicker] instance.
     *
     * @param authority string of your FileProvider, defined in AndroidManifest.xml
     * @param callback instance of [ImagePickerCallback] that will receive pick-operation result as [PickedResult]
     *
     * @see ImagePicker
     */
    class Builder(
        private val authority: String,
        private val callback: ImagePickerCallback,
        private val errorCallback: ImagePickerErrorCallback,
    ) {
        private var useCamera: Boolean = false
        private var useGallery: Boolean = false
        private var autoRotate: Boolean = false
        private var multipleSelection: Boolean = false
        private var minimumSelectionCount: Int = 1
        private var maximumSelectionCount: Int = Int.MAX_VALUE
        private var galleryPicker: GalleryPicker = GalleryPicker.NATIVE
        private var loadDelegate: ImagePickerLoadDelegate = defaultImagePickerLoadDelegate

        /**
         * Must be called after desired parameters were passed to receive [ImagePicker] instance.
         *
         * @see ImagePicker
         *
         * @return instance of [ImagePicker]
         * @throws IllegalArgumentException on incorrect minimumSelectionCount, maximumSelectionCount values.
         */
        fun build(): ImagePicker {
            require(minimumSelectionCount >= 1) {
                "Parameter minimumSelectionCount must be bigger or equal 1"
            }
            require(maximumSelectionCount > minimumSelectionCount) {
                "Parameter maximumSelectionCount must be bigger than minimumSelectionCount"
            }
            return ImagePicker(
                authority = authority,
                callback = callback,
                errorCallback = errorCallback,
                useCamera = useCamera,
                useGallery = useGallery,
                autoRotate = autoRotate,
                multipleSelection = multipleSelection,
                minimumSelectionCount = minimumSelectionCount,
                maximumSelectionCount = maximumSelectionCount,
                galleryPicker = galleryPicker,
                loadDelegate = loadDelegate,
            )
        }

        /**
         * Configures if Camera Picker is allowed to launch.
         *
         * @param useCamera if true will allow launch of Camera Picker.
         *
         * @return [ImagePicker.Builder]
         */
        fun useCamera(useCamera: Boolean = true) = apply {
            this.useCamera = useCamera
        }

        /**
         * Configures if Gallery Picker is allowed to launch.
         *
         * @param useGallery if true will allow launch of Gallery Picker.
         *
         * @return [ImagePicker.Builder]
         */
        fun useGallery(useGallery: Boolean = true) = apply {
            this.useGallery = useGallery
        }

        /**
         * Configures if Gallery Picker is allowed to auto rotate images with wrong EXIF rotation.
         * If parameter is set to true, ImagePicker will re-write the original file with rotated one.
         *
         * @param autoRotate if true will allow auto rotate on any Gallery Picker.
         *
         * @return [ImagePicker.Builder]
         */
        fun autoRotate(autoRotate: Boolean = false) = apply {
            this.autoRotate = autoRotate
        }

        /**
         * Configures if multiple image selection is allowed in Gallery Picker.
         *
         * @param multipleSelection if true will allow multiple image selection in Gallery Picker.
         *
         * @return [ImagePicker.Builder]
         */
        fun multipleSelection(multipleSelection: Boolean = true) = apply {
            this.multipleSelection = multipleSelection
        }

        /**
         * Configures minimum amount of images that user will need to select.
         *
         * **Affects only** [GalleryPicker.CUSTOM].
         *
         * @param minimumSelectionCount minimum allowed selection in [GalleryPicker.CUSTOM].
         *
         * @return [ImagePicker.Builder]
         */
        fun minimumSelectionCount(minimumSelectionCount: Int) = apply {
            this.minimumSelectionCount = minimumSelectionCount
        }

        /**
         * Configures maximum amount of images that user will need to select.
         *
         * **Affects only** [GalleryPicker.CUSTOM].
         *
         * @param maximumSelectionCount maximum allowed selection in [GalleryPicker.CUSTOM].
         *
         * @return [ImagePicker.Builder]
         */
        fun maximumSelectionCount(maximumSelectionCount: Int) = apply {
            this.maximumSelectionCount = maximumSelectionCount
        }

        /**
         * Defines type of Gallery Picker, represented as [GalleryPicker].
         *
         * Possible values:
         * - [GalleryPicker.NATIVE] will launch Gallery Picker that is build in your Android ROM.
         * - [GalleryPicker.CUSTOM] will launch Custom Gallery picker, provided by this library.
         *
         * @see GalleryPicker
         *
         * @param galleryPicker type of desired [GalleryPicker].
         *
         * @return [ImagePicker.Builder]
         */
        fun galleryPicker(galleryPicker: GalleryPicker) = apply {
            this.galleryPicker = galleryPicker
        }

        fun loadDelegate(loadDelegate: ImagePickerLoadDelegate) = apply {
            this.loadDelegate = loadDelegate
        }

        fun loadDelegate(loadDelegate: (ImageView, File) -> Unit) = apply {
            this.loadDelegate = ImagePickerLoadDelegate { iv, file -> loadDelegate(iv, file) }
        }

        companion object {
            /**
             * Must be called after desired parameters were passed to receive [ImagePicker] instance.
             *
             * @param authority string of your FileProvider, defined in AndroidManifest.xml;
             * @param callback instance of [ImagePickerCallback] that will receive pick-operation result as [PickedResult];
             * @param block applicable kotlin builder block.
             *
             * @see ImagePicker
             *
             * @return instance of [ImagePicker]
             */
            inline fun build(
                authority: String,
                callback: ImagePickerCallback,
                errorCallback: ImagePickerErrorCallback,
                block: Builder.() -> Unit,
            ) = Builder(authority, callback, errorCallback)
                .apply(block)
                .build()
        }
    }

    companion object {
        private const val TAG = "ShzImagePicker"

        internal inline fun deliverThrowable(clb: ImagePickerCallback, t: Throwable) {
            t.also(::errorLog)
                .let(PickedResult::Error)
                .let(clb::onImagePickerResult)
        }

        internal inline fun errorLog(t: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, t.message.toString(), t)
        }

        internal inline fun errorLog(message: String, t: Throwable? = null) {
            if (BuildConfig.DEBUG) Log.e(TAG, message, t)
        }

        internal inline fun debugLog(message: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, message)
        }
    }
}
