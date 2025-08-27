package com.android.systemui.screenshot;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

/* compiled from: GlobalScreenshot.java */
/* renamed from: com.android.systemui.screenshot.DeleteImageInBackgroundTask, reason: use source file name */
/* loaded from: classes.dex */
class GlobalScreenshot2 extends AsyncTask<Uri, Void, Void> {
    private Context mContext;

    GlobalScreenshot2(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.os.AsyncTask
    protected Void doInBackground(Uri... uriArr) {
        if (uriArr.length != 1) {
            return null;
        }
        this.mContext.getContentResolver().delete(uriArr[0], null, null);
        return null;
    }
}
