package ir.shecan.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.shecan.data.modelDto.BannerViewModel;

public class SlideshowView extends RelativeLayout {

    private static final String TAG = "SlideshowView";
    private static final long DEFAULT_SLIDE_DURATION_MS = 5000L;
    private static final int MAX_IMAGE_REDIRECTS = 5;
    private static final int IMAGE_CONNECT_TIMEOUT_MS = 10000;
    private static final int IMAGE_READ_TIMEOUT_MS = 15000;
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newSingleThreadExecutor();

    private ImageView imageView;
    private LinearLayout indicatorLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> slides = new ArrayList<>();
    private final List<BannerViewModel> bannerList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;
    private float downX;
    private float downY;
    private boolean swiping;
    private int touchSlop;
    private int minBannerHeight;
    private int defaultBannerHeight;
    private int measuredBannerHeight;
    private boolean roundLoadedImage = true;
    private boolean testSlidesEnabled = false;
    private int loadGeneration = 0;

    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            showNext();
            scheduleNextSlide();
        }
    };

    public SlideshowView(Context context) {
        super(context);
        init();
    }

    public SlideshowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClipToOutline(false);
        setMinimumHeight(dp(112));
        minBannerHeight = dp(112);
        defaultBannerHeight = dp(112);
        measuredBannerHeight = defaultBannerHeight;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        imageView = new ImageView(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        imageView.setAdjustViewBounds(false);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(lp);
        imageView.setBackground(makeRoundRect(Color.argb(28, 39, 69, 58), dp(28)));
        imageView.setOnTouchListener((v, event) -> handleTouch(event));
        addView(imageView);

        indicatorLayout = new LinearLayout(getContext());
        indicatorLayout.setGravity(Gravity.CENTER);
        indicatorLayout.setOrientation(LinearLayout.HORIZONTAL);
        indicatorLayout.setPadding(dp(8), dp(4), dp(8), dp(4));
        indicatorLayout.setBackground(makeRoundRect(Color.argb(90, 6, 31, 22), dp(12)));
        LayoutParams indicatorLp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        indicatorLp.addRule(ALIGN_PARENT_BOTTOM, TRUE);
        indicatorLp.addRule(CENTER_HORIZONTAL, TRUE);
        indicatorLp.setMargins(0, 0, 0, dp(8));
        addView(indicatorLayout, indicatorLp);

        setOnTouchListener((v, event) -> handleTouch(event));
    }

    // گرفتن لیست عکس‌ها
    public void setSlides(List<String> slideList) {
        slides.clear();
        if (slideList != null) {
            for (String slide : slideList) {
                String normalized = normalizeSlide(slide);
                if (normalized != null) slides.add(normalized);
            }
        }
        addTestSlidesIfNeeded();
        currentIndex = 0;
        updateIndicators();
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    private void showSlide(String item) {
        item = normalizeSlide(item);
        if (item == null) return;
        int generation = ++loadGeneration;

        int radius = (int) (30 * getResources().getDisplayMetrics().density);

        RequestOptions options = new RequestOptions()
                .placeholder(makeRoundRect(Color.argb(28, 39, 69, 58), radius))
                .error(makeRoundRect(Color.argb(28, 39, 69, 58), radius));

        if (!roundLoadedImage) {
            options = options.transform(new FitCenter());
        } else {
            options = options.transform(new FitCenter(), new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radius));
        }

        if (isHttpUrl(item)) {
            loadRemoteImage(item, options, radius, generation);
        } else {
            if (!looksLikeBase64Image(item)) {
                imageView.setImageDrawable(makeRoundRect(Color.argb(28, 39, 69, 58), radius));
                return;
            }
            try {
                byte[] bytes = Base64.decode(stripDataUriPrefix(item), Base64.DEFAULT);
                Glide.with(getContext())
                        .load(bytes)
                        .listener(imageSizeListener())
                        .apply(options)
                        .into(imageView);
            } catch (IllegalArgumentException ignored) {
                imageView.setImageDrawable(makeRoundRect(Color.argb(28, 39, 69, 58), radius));
            }
        }
    }

    private void loadRemoteImage(String url, RequestOptions options, int radius, int generation) {
        imageView.setImageDrawable(makeRoundRect(Color.argb(28, 39, 69, 58), radius));
        IMAGE_EXECUTOR.execute(() -> {
            try {
                byte[] imageBytes = downloadImageBytes(url);
                handler.post(() -> {
                    if (generation != loadGeneration) return;
                    Glide.with(getContext())
                            .load(imageBytes)
                            .listener(imageSizeListener())
                            .apply(options)
                            .into(imageView);
                });
            } catch (Exception e) {
                Log.e(TAG, "Banner image download failed: " + url, e);
                handler.post(() -> {
                    if (generation != loadGeneration) return;
                    imageView.setImageDrawable(makeRoundRect(Color.argb(28, 39, 69, 58), radius));
                });
            }
        });
    }

    private byte[] downloadImageBytes(String url) throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpURLConnection connection = openImageConnection(url, 0, cookieManager);
        try (InputStream inputStream = connection.getInputStream()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openImageConnection(String url, int redirects, CookieManager cookieManager) throws Exception {
        if (redirects > MAX_IMAGE_REDIRECTS) {
            throw new IllegalStateException("Too many banner image redirects");
        }

        URL imageUrl = new URL(url);
        URI uri = imageUrl.toURI();
        HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(IMAGE_CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Shecan");
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/png,image/*,*/*;q=0.8");
        addCookies(connection, uri, cookieManager);

        int code = connection.getResponseCode();
        storeCookies(connection, uri, cookieManager);
        if (code >= 300 && code < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.trim().isEmpty()) {
                throw new IllegalStateException("Banner image redirect without location");
            }
            URL nextUrl = new URL(imageUrl, location);
            return openImageConnection(nextUrl.toString(), redirects + 1, cookieManager);
        }

        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Banner image response code: " + code);
        }
        return connection;
    }

    private void addCookies(HttpURLConnection connection, URI uri, CookieManager cookieManager) throws Exception {
        Map<String, List<String>> cookieHeaders = cookieManager.get(uri, connection.getRequestProperties());
        for (Map.Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), joinCookies(entry.getValue()));
        }
    }

    private void storeCookies(HttpURLConnection connection, URI uri, CookieManager cookieManager) throws Exception {
        cookieManager.put(uri, connection.getHeaderFields());
    }

    private String joinCookies(List<String> cookies) {
        StringBuilder builder = new StringBuilder();
        for (String cookie : cookies) {
            if (builder.length() > 0) builder.append("; ");
            builder.append(cookie);
        }
        return builder.toString();
    }

    private void showNext() {
        if (slides.isEmpty()) return;
        currentIndex = (currentIndex + 1) % slides.size();
        showCurrent();
    }

    private void showPrevious() {
        if (slides.isEmpty()) return;
        currentIndex = (currentIndex - 1 + slides.size()) % slides.size();
        showCurrent();
    }

    private void showCurrent() {
        showSlide(slides.get(currentIndex));
        updateIndicators();
    }

    // شروع اتوماتیک
    public void start() {
        if (!isRunning && slides.size() > 1) {
            isRunning = true;
            scheduleNextSlide();
        }
    }

    // توقف اتوماتیک
    public void stop() {
        isRunning = false;
        handler.removeCallbacks(slideRunnable);
    }

    private void scheduleNextSlide() {
        handler.removeCallbacks(slideRunnable);
        if (isRunning && slides.size() > 1) {
            handler.postDelayed(slideRunnable, getCurrentSlideDurationMs());
        }
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setRoundLoadedImage(boolean roundLoadedImage) {
        this.roundLoadedImage = roundLoadedImage;
        if (!slides.isEmpty()) showCurrent();
    }

    public void setTestSlidesEnabled(boolean testSlidesEnabled) {
        this.testSlidesEnabled = testSlidesEnabled;
        addTestSlidesIfNeeded();
        updateIndicators();
        stop();
        start();
    }

    public void setBanners(List<BannerViewModel> banners) {
        stop();
        bannerList.clear();
        slides.clear();

        if (banners != null) {
            for (BannerViewModel b : banners) {
                String slide = resolveBannerImage(b);
                if (slide == null) continue;
                bannerList.add(b);
                slides.add(slide);
            }
        }
        addTestSlidesIfNeeded();

        currentIndex = 0;
        updateIndicators();
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    public BannerViewModel getCurrentBanner() {
        if (bannerList.isEmpty()) return null;
        if (currentIndex >= bannerList.size()) return bannerList.get(0);
        return bannerList.get(currentIndex);
    }

    private String firstNotEmpty(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : second;
    }

    private String resolveBannerImage(BannerViewModel banner) {
        if (banner == null) return null;

        String base64 = normalizeSlide(banner.getImageBase64());
        if (base64 != null && (isHttpUrl(base64) || looksLikeBase64Image(base64))) {
            return base64;
        }

        return normalizeSlide(firstNotEmpty(banner.getImageURL(), base64));
    }

    private String normalizeSlide(String slide) {
        if (slide == null) return null;
        String normalized = slide.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isHttpUrl(String value) {
        if (value == null) return false;
        String lower = value.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private boolean looksLikeBase64Image(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        if (normalized.startsWith("data:image")) return true;
        return normalized.length() > 32 && normalized.matches("^[A-Za-z0-9+/=\\r\\n]+$");
    }

    private String stripDataUriPrefix(String value) {
        int commaIndex = value != null ? value.indexOf(',') : -1;
        if (commaIndex >= 0 && value.substring(0, commaIndex).contains("base64")) {
            return value.substring(commaIndex + 1);
        }
        return value;
    }

    private long getCurrentSlideDurationMs() {
        BannerViewModel banner = getCurrentBanner();
        return banner != null ? banner.getDurationMs() : DEFAULT_SLIDE_DURATION_MS;
    }

    private void addTestSlidesIfNeeded() {
        if (!testSlidesEnabled || slides.size() != 1) return;
        String slide = slides.get(0);
        slides.add(slide);
        slides.add(slide);

        if (bannerList.size() == 1) {
            BannerViewModel banner = bannerList.get(0);
            bannerList.add(banner);
            bannerList.add(banner);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resolvedHeight = Math.max(minBannerHeight, measuredBannerHeight);

        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            resolvedHeight = Math.max(minBannerHeight, requestedHeight);
        } else if (mode == MeasureSpec.AT_MOST) {
            resolvedHeight = Math.min(Math.max(minBannerHeight, resolvedHeight), requestedHeight);
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(resolvedHeight, MeasureSpec.EXACTLY));
    }

    private RequestListener<Drawable> imageSizeListener() {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                updateHeightFrom(resource);
                return false;
            }
        };
    }

    private void updateHeightFrom(Drawable drawable) {
        if (drawable == null) return;
        int imageWidth = drawable.getIntrinsicWidth();
        int imageHeight = drawable.getIntrinsicHeight();
        int viewWidth = getWidth();
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0) return;

        int nextHeight = Math.max(minBannerHeight, Math.round(viewWidth * (imageHeight / (float) imageWidth)));
        if (nextHeight != measuredBannerHeight) {
            measuredBannerHeight = nextHeight;
            requestLayout();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean handleTouch(MotionEvent event) {
        if (slides.size() <= 1) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                swiping = false;
                stop();
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    swiping = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (swiping) {
                    float totalDx = event.getX() - downX;
                    if (Math.abs(totalDx) > dp(48)) {
                        if (totalDx < 0) showNext();
                        else showPrevious();
                    }
                    start();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                start();
                performClick();
                return false;
            default:
                return false;
        }
    }

    private void updateIndicators() {
        indicatorLayout.removeAllViews();
        indicatorLayout.setVisibility(slides.size() > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < slides.size(); i++) {
            View dot = new View(getContext());
            boolean active = i == currentIndex;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    active ? dp(18) : dp(7),
                    dp(7)
            );
            params.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(params);
            dot.setBackground(makeRoundRect(
                    active ? Color.argb(240, 255, 255, 255) : Color.argb(130, 255, 255, 255),
                    dp(4)
            ));
            final int index = i;
            dot.setOnClickListener(v -> {
                currentIndex = index;
                showCurrent();
                stop();
                start();
            });
            indicatorLayout.addView(dot);
        }
    }

    private GradientDrawable makeRoundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
