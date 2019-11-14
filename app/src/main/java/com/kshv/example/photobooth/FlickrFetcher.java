package com.kshv.example.photobooth;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.GsonBuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class FlickrFetcher {
    private static final String API_KEY = "f066ddd6bcf0f9329bcad7bdad634acb";
    public static final String TAG = "FlickrFetcher";

    public List<FlickrItem> fetchData(int i) {
        String jsonRawString = getRawJSONasString (i);
        return parseData (new ArrayList<FlickrItem> (), jsonRawString);
    }

    private List<FlickrItem> parseData(ArrayList<FlickrItem> flickrItems, String jsonRawString) {
        List<FlickrItem> items = new ArrayList<> ();

        try {
            JSONObject jsonBody = new JSONObject (jsonRawString);
            Gson gson = new Gson ();
            JSONArray jsonArray = jsonBody.getJSONObject ("photos").getJSONArray ("photo");
            for (int i = 0; i < jsonArray.length (); i++) {
                JSONObject photo = jsonArray.getJSONObject (i);
                FlickrItem item = gson.fromJson (photo.toString (),FlickrItem.class);
                items.add (item);
                Log.i (TAG, "photo added to fragmento recycler");
            }
        } catch (JSONException e) {
            Log.e (TAG, e.getLocalizedMessage ());
        }

        return items;
    }

    private String getRawJSONasString(int page) {
        String urlRequest = Uri.parse ("https://api.flickr.com/services/rest")
                .buildUpon ()
                .appendQueryParameter ("method", "flickr.photos.getRecent")
                .appendQueryParameter ("api_key", API_KEY)
                .appendQueryParameter ("format", "json")
                .appendQueryParameter ("nojsoncallback", "1")
                .appendQueryParameter ("extras", "url_s")
                .appendQueryParameter ("page", String.valueOf (page))
                .build ().toString ();
        Log.i (TAG, "urlRequest created: " + urlRequest);
        return new String (getUrlBytes (urlRequest));
    }

    public byte[] getUrlBytes(String url_outer) {
        try {
            URL url = new URL (url_outer);
            HttpURLConnection con = (HttpURLConnection) url.openConnection ();

            ByteArrayOutputStream out = new ByteArrayOutputStream ();
            InputStream in = con.getInputStream ();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while ((bytesRead = in.read (buffer)) > 0) {
                out.write (buffer, 0, bytesRead);
            }
            out.close ();
            con.disconnect ();
            return out.toByteArray ();
        } catch (MalformedURLException e) {
            Log.e (TAG, e.getLocalizedMessage ());
        } catch (IOException e) {
            Log.e (TAG, e.getLocalizedMessage ());
        }

        return null;
    }
}
