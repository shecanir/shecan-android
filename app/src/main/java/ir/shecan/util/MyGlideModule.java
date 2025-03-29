package ir.shecan.util;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public class MyGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Customize Glide (e.g., set cache size)
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false; // Disable manifest parsing for better performance
    }
}