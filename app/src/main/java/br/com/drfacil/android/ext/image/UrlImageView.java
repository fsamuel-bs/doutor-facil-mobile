package br.com.drfacil.android.ext.image;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import br.com.drfacil.android.R;
import br.com.drfacil.android.helpers.AsyncHelper;
import br.com.drfacil.android.helpers.CustomHelper;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;

/* TODO: Add loading placeholder */
public class UrlImageView extends ImageView {

    public static final int FADE_DURATION = 400;

    private static Downloader<CachedBitmapDownloader.BitmapInfo> sDownloader;

    private String mUrl;
    private ColorDrawable mPlaceholderDrawable = new ColorDrawable(0x00000000);
    private Downloader<CachedBitmapDownloader.BitmapInfo> mDownloader;
    private ListenableFuture<CachedBitmapDownloader.BitmapInfo> mFuture;

    public UrlImageView(Context context) {
        super(context);
        init(null);
    }

    public UrlImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public UrlImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        initCache();
        initView();
        if (attrs != null) {
            initAttrs(attrs);
        }
    }

    private void initCache() {
        if (sDownloader == null) {
            sDownloader = CachedBitmapDownloader.getInstance(getContext());
        }
        mDownloader = sDownloader;
    }

    private void initView() {
        if (getDrawable() == null) {
            setImageDrawable(mPlaceholderDrawable);
        }
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.UrlImageView);
        try {
            setUrl(array.getString(R.styleable.UrlImageView_url));
        } finally {
            array.recycle();
        }
    }

    public void setUrl(String url) {
        if (Objects.equal(mUrl, url)) return;
        mUrl = url;
        setPlaceholder();
        if (url == null) return;
        if (mFuture != null) {
            // Let the outstanding request, because we cannot cancel it successfully because getInputStream() is not
            // interruptible, so we allow it to finish and go to cache. Hopefully there won't be too many requests
            // simultaneously for the same url, so as not to overflow the executor service queue.
            // TODO: This approach is rather hacky, fix it properly
            // mFuture.cancel(false);
        }
        mFuture = mDownloader.download(url);
        AsyncHelper.addCallbackOnUiThread(mFuture, new ImageDownloadedCallback(url));
    }

    public String getUrl() {
        return mUrl;
    }

    private void setPlaceholder() {
        setImageDrawable(mPlaceholderDrawable);
    }

    private class ImageDownloadedCallback implements FutureCallback<CachedBitmapDownloader.BitmapInfo> {

        private final String mUrl;

        public ImageDownloadedCallback(String url) {
            mUrl = url;
        }

        @Override
        public void onSuccess(CachedBitmapDownloader.BitmapInfo bitmapInfo) {
            if (onFinish()) return;
            if (bitmapInfo.origin == CachedBitmapDownloader.BitmapOrigin.INTERNET) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmapInfo.bitmap);
                Drawable[] layers = { getDrawable(), drawable };
                TransitionDrawable transition = new TransitionDrawable(layers);
                setImageDrawable(transition);
                transition.startTransition(FADE_DURATION);
            } else {
                setImageBitmap(bitmapInfo.bitmap);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (onFinish()) return;
            if (!(t instanceof CancellationException)) {
                CustomHelper.logException(t);
            }
            setPlaceholder();
        }

        // True indicates that it has handled the callback, so return immediately
        private boolean onFinish() {
            boolean handled = !mUrl.equals(UrlImageView.this.mUrl);
            if (!handled) {
                // This is our request, thus we can clean mFuture now
                mFuture = null;
            }
            return handled;
        }

    }
}
