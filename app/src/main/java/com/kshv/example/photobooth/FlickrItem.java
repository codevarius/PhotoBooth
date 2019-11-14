package com.kshv.example.photobooth;

class FlickrItem {
    private String title, id, url_s;

    public FlickrItem(String... args) {
        title = args[0];
        id = args[1];
        url_s = args[2];
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getUrl_s() {
        return url_s;
    }
}
