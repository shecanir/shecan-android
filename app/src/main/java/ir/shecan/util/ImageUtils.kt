package ir.shecan.util

import android.content.Context
import android.graphics.drawable.Animatable
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest

object ImageUtils {

    fun loadImage(context: Context, imageUrl: String, imageView: ImageView) {
        val isGif = imageUrl.lowercase().endsWith(".gif")

        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (isGif) {
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory()) // Corrected
                    } else {
                        add(GifDecoder.Factory()) // Corrected
                    }
                }
            }
            .diskCachePolicy(CachePolicy.DISABLED) // Disable disk cache
            .memoryCachePolicy(CachePolicy.DISABLED) // Disable memory cache
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