package com.mediatek.gallery3d.video;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import com.mediatek.galleryportable.Log;
import java.io.File;

public class VideoTitleHooker extends MovieHooker {
    public static final String SCREEN_ORIENTATION_LANDSCAPE = "SCREEN_ORIENTATION_LANDSCAPE";
    public static final String STREAMING_VIDEO_TITLE = "STREAMING_VIDEO_TITLE";
    private static final String TAG = "VP_TitleHooker";
    private boolean mIsLandscape = false;
    private String mVideoTitle = null;

    @Override
    public void onCreate(Bundle bundle) {
        Intent intent = getIntent();
        this.mIsLandscape = intent.getBooleanExtra(SCREEN_ORIENTATION_LANDSCAPE, false);
        if (this.mIsLandscape) {
            getContext().setRequestedOrientation(0);
        }
        this.mVideoTitle = intent.getStringExtra(STREAMING_VIDEO_TITLE);
    }

    @Override
    public void onResume() {
        enhanceActionBar();
    }

    private void enhanceActionBar() {
        final IMovieItem movieItem = getMovieItem();
        if (movieItem == null) {
            Log.e(TAG, "enhanceActionBar, movieItem == null");
            return;
        }
        final Uri uri = movieItem.getUri();
        final String scheme = movieItem.getUri().getScheme();
        movieItem.getUri().getAuthority();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voidArr) {
                String titleFromUri;
                if (VideoTitleHooker.this.mVideoTitle != null) {
                    String str = VideoTitleHooker.this.mVideoTitle;
                    Log.d(VideoTitleHooker.TAG, "enhanceActionBar() task return mVideoTitle " + str);
                    return str;
                }
                if ("file".equals(scheme)) {
                    titleFromUri = movieItem.getDisplayName();
                    if (titleFromUri == null) {
                        titleFromUri = VideoTitleHooker.this.getTitleFromUri(uri);
                    }
                } else if (!"content".equals(scheme)) {
                    titleFromUri = VideoTitleHooker.this.getTitleFromUri(uri);
                } else {
                    titleFromUri = movieItem.getDisplayName();
                    if (titleFromUri == null && movieItem.getVideoPath() != null) {
                        titleFromUri = new File(movieItem.getVideoPath()).getName();
                    }
                }
                Log.d(VideoTitleHooker.TAG, "enhanceActionBar() task return " + titleFromUri);
                return titleFromUri;
            }

            @Override
            protected void onPostExecute(String str) {
                Log.d(VideoTitleHooker.TAG, "onPostExecute(" + str + ") movieItem=" + movieItem + ", mMovieItem=" + VideoTitleHooker.this.getMovieItem());
                movieItem.setTitle(str);
                if (movieItem == VideoTitleHooker.this.getMovieItem()) {
                    VideoTitleHooker.this.setActionBarTitle(str);
                }
            }
        }.execute(new Void[0]);
        Log.d(TAG, "enhanceActionBar() " + movieItem);
    }

    private void setActionBarTitle(String str) {
        ActionBar actionBar = getContext().getActionBar();
        Log.d(TAG, "setActionBarTitle(" + str + ") actionBar = " + actionBar);
        if (actionBar != null && str != null) {
            actionBar.setTitle(str);
        }
    }

    private String getTitleFromUri(Uri uri) {
        String strDecode = Uri.decode(uri.getLastPathSegment());
        Log.d(TAG, "getTitleFromUri() return " + strDecode);
        return strDecode;
    }
}
