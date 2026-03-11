package com.android.systemui.screenshot;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

class DeleteImageInBackgroundTask extends AsyncTask<Uri, Void, Void> {
    private Context mContext;

    DeleteImageInBackgroundTask(Context context) {
        this.mContext = context;
    }

    @Override
    public Void doInBackground(Uri... params) {
        if (params.length != 1) {
            return null;
        }
        Uri screenshotUri = params[0];
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.delete(screenshotUri, null, null);
        return null;
    }
}
