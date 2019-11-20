package com.kshv.example.photobooth;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private LruCache<String, Bitmap> mCache;
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponderHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    ThumbnailDownloader(Handler responseHandler, LruCache<String, Bitmap> stringBitmapLruCache) {
        super(TAG);
        mResponderHandler = responseHandler;
        mCache = stringBitmapLruCache;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    //Log.i(TAG, "got request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);

        if (url == null) {
            return;
        }

        final Bitmap bitmap;
        if (mCache.get(url) == null) {
            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mCache.trimToSize(mCache.maxSize());
            mCache.put(url, bitmap);
            Log.i(TAG, "bitmap decoded and loaded to cache");
        }else{
            bitmap = mCache.get(url);
            Log.i(TAG,"bitmap loaded from cache");
        }

        mResponderHandler.post(() -> {

            if (mRequestMap.get(target) == null
                    || !Objects.requireNonNull(mRequestMap.get(target)).equals(url) || mHasQuit) {
                return;
            }
            mRequestMap.remove(target);
            mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
        });
    }

    void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    void queueThumbnail(T target, String url) {
        //Log.i(TAG, "Got url: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    void setThumbnailLIistener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }
}
