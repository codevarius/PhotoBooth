package com.kshv.example.photobooth;

import android.annotation.SuppressLint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Fragmento extends Fragment {
    private static RecyclerView mRecyclerViewWidget;
    private static final String TAG = "Fragmento";
    private static List<FlickrItem> flickrItems;
    private int currentPage = 1;
    private ThumbnailDownloader<FragmentRecyclerViewHolder> mThumbnailDownloader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        flickrItems = new ArrayList<>();
        setHasOptionsMenu(true);
        Log.i(TAG, "fragmento on create finished");
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.search_menu,menu);
        ((SearchView)menu.findItem(R.id.search_button).getActionView()).setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        flickrItems.clear();
                        mThumbnailDownloader.clearQueue();
                        setupAdapter(mRecyclerViewWidget);
                        currentPage = 1;
                        loadData(currentPage,query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragmento_layout, container, false);

        loadData(currentPage++, "");

        mThumbnailDownloader = new ThumbnailDownloader<>(new Handler(), new LruCache<>(
                (int) ((Runtime.getRuntime().maxMemory() / 1024) / 2)));
        mThumbnailDownloader.setThumbnailLIistener(
                (target, thumbnail) -> {
                    Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                    target.bind(drawable);
                });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "thumbnail downloader started");

        mRecyclerViewWidget = rootView.findViewById(R.id.fragmento_recycler);
        mRecyclerViewWidget.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRecyclerViewWidget.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    Log.i(TAG, "bottom of recycler reached");
                    loadData(currentPage++, "");
                }
            }
        });
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "thumbnail downloader destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @SuppressLint("StaticFieldLeak")
    private void loadData(final int i, String query) {
        new AsyncTask<Void, Void, List<FlickrItem>>() {
            @Override
            protected List<FlickrItem> doInBackground(Void... voids) {
                return new FlickrFetcher().fetchData(i,query);
            }

            @Override
            protected void onPostExecute(List<FlickrItem> flickrItemList) {
                flickrItems.addAll(flickrItemList);
                if (mRecyclerViewWidget.getAdapter() == null) {
                    setupAdapter(mRecyclerViewWidget);
                }
                mRecyclerViewWidget.getAdapter().notifyDataSetChanged();
            }
        }.execute();
    }

    private void setupAdapter(RecyclerView mRecyclerViewWidget) {
        mRecyclerViewWidget.setAdapter(new FragmentoRecyclerViewAdapter());
        Log.i(TAG, "adapter installed");
    }

    class FragmentRecyclerViewHolder extends RecyclerView.ViewHolder {
        FragmentRecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(Drawable img) {
            ((ImageView) itemView).setImageDrawable(img);
        }
    }

    class FragmentoRecyclerViewAdapter extends RecyclerView.Adapter<FragmentRecyclerViewHolder> {
        @NonNull
        @Override
        public FragmentRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FragmentRecyclerViewHolder(
                    getLayoutInflater().inflate(R.layout.recycler_itm, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentRecyclerViewHolder holder, int position) {
            ((ImageView) holder.itemView).setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
            mThumbnailDownloader.queueThumbnail(holder, flickrItems.get(position).getUrl_s());
        }

        @Override
        public int getItemCount() {
            return flickrItems.size();
        }
    }
}
