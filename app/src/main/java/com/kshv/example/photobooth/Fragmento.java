package com.kshv.example.photobooth;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Fragmento extends Fragment {
    private View mRootView;
    private static RecyclerView mRecyclerViewWidget;
    public static final String TAG = "Fragmento";
    private static List<FlickrItem> flickrItems;
    private int currentPage = 1;
    private ThumbnailDownloader<FragmentRecyclerViewHolder> mThumbnailDownloader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setRetainInstance (true);
        flickrItems = new ArrayList<> ();
        Log.i (TAG, "fragmento on create finished");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate (R.layout.fragmento_layout, container, false);

        loadData (currentPage++);

        Handler responseHandler = new Handler ();
        mThumbnailDownloader = new ThumbnailDownloader<> (responseHandler);
        mThumbnailDownloader.setThumbnailLIistener (new ThumbnailDownloader.ThumbnailDownloadListener<FragmentRecyclerViewHolder> () {
            @Override
            public void onThumbnailDownloaded(FragmentRecyclerViewHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable (getResources (), thumbnail);
                target.bind (drawable);
            }
        });
        mThumbnailDownloader.start ();
        mThumbnailDownloader.getLooper ();
        Log.i (TAG, "thumbnail downloader started");

        mRecyclerViewWidget = mRootView.findViewById (R.id.fragmento_recycler);
        mRecyclerViewWidget.setLayoutManager (new GridLayoutManager (getContext (), 3));
        mRecyclerViewWidget.setOnScrollListener (new RecyclerView.OnScrollListener () {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled (recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically (1)) {
                    Log.i (TAG, "bottom of recycler reached");
                    loadData (currentPage++);
                }
            }
        });
        return mRootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy ();
        mThumbnailDownloader.quit ();
        Log.i (TAG, "thumbnail downloader destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView ();
        mThumbnailDownloader.clearQueue ();
    }

    @SuppressLint("StaticFieldLeak")
    private void loadData(final int i) {
        new AsyncTask<Void, Void, List<FlickrItem>> () {
            @Override
            protected List<FlickrItem> doInBackground(Void... voids) {
                return new FlickrFetcher ().fetchData (i);
            }

            @Override
            protected void onPostExecute(List<FlickrItem> flickrItemList) {
                flickrItems.addAll (flickrItemList);
                if (mRecyclerViewWidget.getAdapter () == null) {
                    setupAdapter (mRecyclerViewWidget);
                }
                mRecyclerViewWidget.getAdapter ().notifyDataSetChanged ();
            }
        }.execute ();
    }

    private void setupAdapter(RecyclerView mRecyclerViewWidget) {
        mRecyclerViewWidget.setAdapter (new FragmentoRecyclerViewAdapter ());
        Log.i (TAG, "adapter installed");
    }

    class FragmentRecyclerViewHolder extends RecyclerView.ViewHolder {
        FragmentRecyclerViewHolder(@NonNull View itemView) {
            super (itemView);
        }

        void bind(Drawable img) {
            ((ImageView) itemView).setImageDrawable (img);
        }
    }

    class FragmentoRecyclerViewAdapter extends RecyclerView.Adapter<FragmentRecyclerViewHolder> {
        @NonNull
        @Override
        public FragmentRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FragmentRecyclerViewHolder (
                    getLayoutInflater ().inflate (R.layout.recycler_itm, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentRecyclerViewHolder holder, int position) {
            mThumbnailDownloader.queueThumbnail (holder, flickrItems.get (position).getUrl_s ());
        }

        @Override
        public int getItemCount() {
            return flickrItems.size ();
        }
    }
}
