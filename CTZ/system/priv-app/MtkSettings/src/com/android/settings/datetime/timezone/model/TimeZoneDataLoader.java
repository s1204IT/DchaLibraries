package com.android.settings.datetime.timezone.model;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import com.android.settingslib.utils.AsyncLoader;

/* loaded from: classes.dex */
public class TimeZoneDataLoader extends AsyncLoader<TimeZoneData> {

    public interface OnDataReadyCallback {
        void onTimeZoneDataReady(TimeZoneData timeZoneData);
    }

    public TimeZoneDataLoader(Context context) {
        super(context);
    }

    /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
    @Override // android.content.AsyncTaskLoader
    public TimeZoneData loadInBackground() {
        return TimeZoneData.getInstance();
    }

    /* JADX DEBUG: Method merged with bridge method: onDiscardResult(Ljava/lang/Object;)V */
    @Override // com.android.settingslib.utils.AsyncLoader
    protected void onDiscardResult(TimeZoneData timeZoneData) {
    }

    public static class LoaderCreator implements LoaderManager.LoaderCallbacks<TimeZoneData> {
        private final OnDataReadyCallback mCallback;
        private final Context mContext;

        public LoaderCreator(Context context, OnDataReadyCallback onDataReadyCallback) {
            this.mContext = context;
            this.mCallback = onDataReadyCallback;
        }

        /* JADX DEBUG: Return type fixed from 'android.content.Loader' to match base method */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<TimeZoneData> onCreateLoader(int i, Bundle bundle) {
            return new TimeZoneDataLoader(this.mContext);
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<TimeZoneData> loader, TimeZoneData timeZoneData) {
            if (this.mCallback != null) {
                this.mCallback.onTimeZoneDataReady(timeZoneData);
            }
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<TimeZoneData> loader) {
        }
    }
}
