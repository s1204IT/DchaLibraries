package com.android.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.android.camera.data.LocalData;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.filmstrip.ImageData;

public class FixedFirstDataAdapter extends AbstractLocalDataAdapterWrapper implements DataAdapter.Listener {
    private static final Log.Tag TAG = new Log.Tag("FixedFirstDataAdpt");
    private LocalData mFirstData;
    private DataAdapter.Listener mListener;

    public FixedFirstDataAdapter(Context context, LocalDataAdapter wrappedAdapter, LocalData firstData) {
        super(context, wrappedAdapter);
        if (firstData == null) {
            throw new AssertionError("data is null");
        }
        this.mFirstData = firstData;
    }

    @Override
    public LocalData getLocalData(int dataID) {
        return dataID == 0 ? this.mFirstData : this.mAdapter.getLocalData(dataID - 1);
    }

    @Override
    public void removeData(int dataID) {
        if (dataID > 0) {
            this.mAdapter.removeData(dataID - 1);
        }
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        int pos = this.mAdapter.findDataByContentUri(uri);
        if (pos != -1) {
            return pos + 1;
        }
        return -1;
    }

    @Override
    public void updateData(int pos, LocalData data) {
        if (pos == 0) {
            this.mFirstData = data;
            if (this.mListener != null) {
                this.mListener.onDataUpdated(new DataAdapter.UpdateReporter() {
                    @Override
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    @Override
                    public boolean isDataUpdated(int dataID) {
                        return dataID == 0;
                    }
                });
                return;
            }
            return;
        }
        this.mAdapter.updateData(pos - 1, data);
    }

    @Override
    public int getTotalNumber() {
        return this.mAdapter.getTotalNumber() + 1;
    }

    @Override
    public View getView(Context context, View recycled, int dataID, LocalData.ActionCallback actionCallback) {
        return dataID == 0 ? this.mFirstData.getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, 0, null, false, actionCallback) : this.mAdapter.getView(context, recycled, dataID - 1, actionCallback);
    }

    @Override
    public int getItemViewType(int dataId) {
        return dataId == 0 ? this.mFirstData.getItemViewType().ordinal() : this.mAdapter.getItemViewType(dataId);
    }

    @Override
    public void resizeView(Context context, int dataID, View view, int w, int h) {
    }

    @Override
    public ImageData getImageData(int dataID) {
        return dataID == 0 ? this.mFirstData : this.mAdapter.getImageData(dataID - 1);
    }

    @Override
    public void setListener(DataAdapter.Listener listener) {
        this.mListener = listener;
        this.mAdapter.setListener(listener == null ? null : this);
        if (this.mListener != null) {
            this.mListener.onDataLoaded();
        }
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        return dataID == 0 ? this.mFirstData.canSwipeInFullScreen() : this.mAdapter.canSwipeInFullScreen(dataID - 1);
    }

    @Override
    public void onDataLoaded() {
        if (this.mListener != null) {
            this.mListener.onDataUpdated(new DataAdapter.UpdateReporter() {
                @Override
                public boolean isDataRemoved(int dataID) {
                    return false;
                }

                @Override
                public boolean isDataUpdated(int dataID) {
                    return dataID != 0;
                }
            });
        }
    }

    @Override
    public void onDataUpdated(final DataAdapter.UpdateReporter reporter) {
        this.mListener.onDataUpdated(new DataAdapter.UpdateReporter() {
            @Override
            public boolean isDataRemoved(int dataID) {
                return dataID != 0 && reporter.isDataRemoved(dataID + (-1));
            }

            @Override
            public boolean isDataUpdated(int dataID) {
                return dataID != 0 && reporter.isDataUpdated(dataID + (-1));
            }
        });
    }

    @Override
    public void onDataInserted(int dataID, ImageData data) {
        this.mListener.onDataInserted(dataID + 1, data);
    }

    @Override
    public void onDataRemoved(int dataID, ImageData data) {
        this.mListener.onDataRemoved(dataID + 1, data);
    }

    @Override
    public AsyncTask updateMetadata(int dataId) {
        if (dataId > 0) {
            return this.mAdapter.updateMetadata(dataId - 1);
        }
        MetadataLoader.loadMetadata(this.mContext, this.mFirstData);
        return null;
    }

    @Override
    public boolean isMetadataUpdated(int dataId) {
        return dataId > 0 ? this.mAdapter.isMetadataUpdated(dataId - 1) : this.mFirstData.isMetadataUpdated();
    }
}
