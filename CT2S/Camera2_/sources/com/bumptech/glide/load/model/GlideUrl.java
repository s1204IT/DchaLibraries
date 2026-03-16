package com.bumptech.glide.load.model;

import android.text.TextUtils;
import java.net.MalformedURLException;
import java.net.URL;

public class GlideUrl {
    private String stringUrl;
    private URL url;

    public GlideUrl(URL url) {
        this.url = url;
        this.stringUrl = null;
    }

    public GlideUrl(String url) {
        this.stringUrl = url;
        this.url = null;
    }

    public URL toURL() throws MalformedURLException {
        if (this.url == null) {
            this.url = new URL(this.stringUrl);
        }
        return this.url;
    }

    public String toString() {
        if (TextUtils.isEmpty(this.stringUrl)) {
            this.stringUrl = this.url.toString();
        }
        return this.stringUrl;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlideUrl glideUrl = (GlideUrl) o;
        if (this.stringUrl != null) {
            if (glideUrl.stringUrl != null) {
                return this.stringUrl.equals(glideUrl.stringUrl);
            }
            return this.stringUrl.equals(glideUrl.url.toString());
        }
        if (glideUrl.stringUrl != null) {
            return this.url.toString().equals(glideUrl.stringUrl);
        }
        return this.url.equals(glideUrl.url);
    }

    public int hashCode() {
        return this.stringUrl != null ? this.stringUrl.hashCode() : this.url.toString().hashCode();
    }
}
