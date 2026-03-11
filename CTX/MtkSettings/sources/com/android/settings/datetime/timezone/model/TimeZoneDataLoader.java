package com.android.settings.datetime.timezone.model;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import com.android.settingslib.utils.AsyncLoader;

public class TimeZoneDataLoader extends AsyncLoader<TimeZoneData> {

    public interface OnDataReadyCallback {
        void onTimeZoneDataReady(TimeZoneData timeZoneData);
    }

    public TimeZoneDataLoader(Context context) {
        super(context);
    }

    @Override
    public TimeZoneData loadInBackground() {
        return TimeZoneData.getInstance();
    }

    @Override
    public void onDiscardResult(TimeZoneData timeZoneData) {
    }

    public static class LoaderCreator implements LoaderManager.LoaderCallbacks<TimeZoneData> {
        private final OnDataReadyCallback mCallback;
        private final Context mContext;

        public LoaderCreator(Context context, OnDataReadyCallback onDataReadyCallback) {
            this.mContext = context;
            this.mCallback = onDataReadyCallback;
        }

        @Override
        public Loader<TimeZoneData> onCreateLoader(int i, Bundle bundle) {
            return new TimeZoneDataLoader(this.mContext);
        }

        @Override
        public void onLoadFinished(Loader<TimeZoneData> loader, TimeZoneData timeZoneData) {
            if (this.mCallback != null) {
                this.mCallback.onTimeZoneDataReady(timeZoneData);
            }
        }

        @Override
        public void onLoaderReset(Loader<TimeZoneData> loader) {
        }
    }
}
