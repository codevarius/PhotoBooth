package com.kshv.example.photobooth;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class FlickrFetcher {
    private static final String API_KEY = "";
    private static final String TAG = "FlickrFetcher";

    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final String GET_RECENT_METHOD = "flickr.photos.getRecent";
    private static final Uri DEFAULT_URI = Uri.parse("https://api.flickr.com/services/rest")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    List<FlickrItem> fetchData(int i, String query) {
        String jsonRawString = getRawJSONasString(i,query);
        return parseData(jsonRawString);
    }

    private List<FlickrItem> parseData(String jsonRawString) {
        List<FlickrItem> items = new ArrayList<>();

        try {
            JSONObject jsonBody = new JSONObject(jsonRawString);
            Gson gson = new Gson();
            JSONArray jsonArray = jsonBody.getJSONObject("photos").getJSONArray("photo");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject photo = jsonArray.getJSONObject(i);
                FlickrItem item = gson.fromJson(photo.toString(), FlickrItem.class);
                items.add(item);
                Log.i(TAG, "photo added to fragmento recycler");
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return items;
    }

    private String getRawJSONasString(int page, String query) {
        String urlRequest;
        if (query == null || query.equals("")){
            urlRequest = DEFAULT_URI.buildUpon()
                    .appendQueryParameter("method",GET_RECENT_METHOD)
                    .appendQueryParameter("page",String.valueOf(page))
                    .build().toString();
        }else{
            urlRequest = DEFAULT_URI.buildUpon()
                    .appendQueryParameter("method",SEARCH_METHOD)
                    .appendQueryParameter("text",query)
                    .appendQueryParameter("page",String.valueOf(page))
                    .build().toString();
        }
        Log.i(TAG, "urlRequest created: " + urlRequest);
        return new String(getUrlBytes(urlRequest));
    }

    byte[] getUrlBytes(String url_outer) {
        try {
            URL url = new URL(url_outer);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = con.getInputStream();
            int bytesRead;
            byte[] buffer = new byte[1024];

            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            con.disconnect();
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return null;
    }
}
