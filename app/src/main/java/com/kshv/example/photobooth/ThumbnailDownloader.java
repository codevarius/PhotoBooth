package com.kshv.example.photobooth;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    public static final String TAG = "ThumbnailDownloader";
    public static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<> ();
    private Handler mResponderHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailLIistener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super (TAG);
        mResponderHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler () {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i (TAG, "got request for URL: " + mRequestMap.get (target));
                    handleRequest (target);
                }
            }
        };
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get (target);

        if (url == null) {
            return;
        }

        byte[] bitmapBytes = new FlickrFetcher ().getUrlBytes (url);
        final Bitmap bitmap = BitmapFactory.decodeByteArray (bitmapBytes, 0, bitmapBytes.length);
        Log.i (TAG, "bitmap decoded");

        mResponderHandler.post (new Runnable () {
            @Override
            public void run() {
                if (mRequestMap.get (target) != url || mHasQuit) {
                    return;
                }
                mRequestMap.remove (target);
                mThumbnailDownloadListener.onThumbnailDownloaded (target, bitmap);
            }
        });
    }

    public void clearQueue() {
        mRequestHandler.removeMessages (MESSAGE_DOWNLOAD);
        mRequestMap.clear ();
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit ();
    }

    public void queueThumbnail(T target, String url) {
        Log.i (TAG, "Got url: " + url);

        if (url == null) {
            mRequestMap.remove (target);
        } else {
            mRequestMap.put (target, url);
            mRequestHandler.obtainMessage (MESSAGE_DOWNLOAD, target).sendToTarget ();
        }
    }
}
