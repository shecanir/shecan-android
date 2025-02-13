package ir.shecan.util

import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

object ImageUtils {

    fun loadImage(context: Context, imageUrl: String, imageView: ImageView) {
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
//                        add(BitmapFactoryDecoder.Factory())
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
                onError = {
                    imageView.visibility = ImageView.GONE
                }
            )
            .build()

        imageLoader.enqueue(request)
    }
}