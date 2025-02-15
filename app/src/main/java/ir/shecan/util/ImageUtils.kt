package ir.shecan.util

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder


object ImageUtils {

    fun loadImageWithCoil(context: Context, imageUrl: String, imageView: ImageView) {
        val isGif = imageUrl.lowercase().endsWith(".gif")
        val isWebP = imageUrl.lowercase().endsWith(".webp")
        val isAvif = imageUrl.lowercase().endsWith(".avif")

        val imageLoader = ImageLoader.Builder(context)
            .components {
                when {
                    isGif -> {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory()) // API 28+ uses ImageDecoder
                        } else {
                            add(GifDecoder.Factory()) // Older APIs use GifDecoder
                        }
                    }
                    isAvif || isWebP -> {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory()) // WebP/AVIF support from API 28+
                        } else {
                            add(BitmapFactoryDecoder.Factory())
                        }
                    }
                    else -> {
                        add(BitmapFactoryDecoder.Factory())
                    }
                }
            }
            .build()

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .target(
                onSuccess = { result ->
                    imageView.setImageDrawable(result)
                    imageView.visibility = ImageView.VISIBLE
                    (result as? Animatable)?.start()
                },
                onError = { error->
                    imageView.visibility = ImageView.GONE
                }
            )
            .build()

        imageLoader.enqueue(request)
    }

    fun loadImageWithGlide(context: Context, imageUrl: String, imageView: ImageView){
        Glide.with(context)
//            .asGif()
            .load(imageUrl)
            .listener(object : RequestListener<Drawable> {

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.setImageDrawable(resource) // Set the loaded image
                    imageView.visibility = View.VISIBLE // Show ImageView
                    return false // Return false to let Glide handle setting the image
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.visibility = View.GONE // Hide ImageView on error
                    return false // Return false to allow Glide to handle the error placeholder
                }
            })
            .into(imageView)
    }

    fun loadImage(context: Context, imageUrl: String, imageView: SimpleDraweeView){
        val uri = Uri.parse(imageUrl)
        val isGif = imageUrl.lowercase().endsWith(".gif")
        val isWebP = imageUrl.lowercase().endsWith(".webp")
        val isAvif = imageUrl.lowercase().endsWith(".avif")

        imageView.visibility = View.VISIBLE

        val decodeOptions = ImageDecodeOptions.newBuilder()
            .setForceStaticImage(isAvif) // AVIF animation not supported, force static
            .build()

        val imageRequest = ImageRequestBuilder.newBuilderWithSource(uri)
            .setImageDecodeOptions(decodeOptions)
//            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
            .setProgressiveRenderingEnabled(true) // Enables progressive loading for large images
            .build()

        // Controller Listener for success and failure
        val listener = object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(
                id: String?,
                imageInfo: ImageInfo?,
                animatable: Animatable?
            ) {
                if (imageInfo != null) {

                }
                imageView.visibility = View.VISIBLE
                animatable?.start() // Start animation if it's a GIF or animated WebP
            }

            override fun onFailure(id: String?, throwable: Throwable?) {
                imageView.visibility = View.GONE
            }
        }

        val controller = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequest)
            .setAutoPlayAnimations(isGif || isWebP) // Enable animations for GIF/WebP
            .setOldController(imageView.controller)
            .setControllerListener(listener)
            .build()

        imageView.controller = controller
    }
}