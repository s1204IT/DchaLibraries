package com.android.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.android.camera.data.LocalData;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.filmstrip.ImageData;

public class FixedLastDataAdapter extends AbstractLocalDataAdapterWrapper {
    private LocalData mLastData;
    private DataAdapter.Listener mListener;

    public FixedLastDataAdapter(Context context, LocalDataAdapter wrappedAdapter, LocalData lastData) {
        super(context, wrappedAdapter);
        if (lastData == null) {
            throw new AssertionError("data is null");
        }
        this.mLastData = lastData;
    }

    @Override
    public void setListener(DataAdapter.Listener listener) {
        super.setListener(listener);
        this.mListener = listener;
    }

    @Override
    public LocalData getLocalData(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getLocalData(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData;
        }
        return null;
    }

    @Override
    public void removeData(int dataID) {
        if (dataID < this.mAdapter.getTotalNumber()) {
            this.mAdapter.removeData(dataID);
        }
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        return this.mAdapter.findDataByContentUri(uri);
    }

    @Override
    public void updateData(final int pos, LocalData data) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (pos < totalNumber) {
            this.mAdapter.updateData(pos, data);
        } else if (pos == totalNumber) {
            this.mLastData = data;
            if (this.mListener != null) {
                this.mListener.onDataUpdated(new DataAdapter.UpdateReporter() {
                    @Override
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    @Override
                    public boolean isDataUpdated(int dataID) {
                        return dataID == pos;
                    }
                });
            }
        }
    }

    @Override
    public int getTotalNumber() {
        return this.mAdapter.getTotalNumber() + 1;
    }

    @Override
    public View getView(Context context, View recycled, int dataID, LocalData.ActionCallback actionCallback) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getView(context, recycled, dataID, actionCallback);
        }
        if (dataID == totalNumber) {
            return this.mLastData.getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, 0, null, false, actionCallback);
        }
        return null;
    }

    @Override
    public int getItemViewType(int dataId) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataId < totalNumber) {
            return this.mAdapter.getItemViewType(dataId);
        }
        if (dataId == totalNumber) {
            return this.mLastData.getItemViewType().ordinal();
        }
        return -1;
    }

    @Override
    public void resizeView(Context context, int dataID, View view, int w, int h) {
    }

    @Override
    public ImageData getImageData(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getImageData(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData;
        }
        return null;
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.canSwipeInFullScreen(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData.canSwipeInFullScreen();
        }
        return false;
    }

    @Override
    public AsyncTask updateMetadata(int dataId) {
        if (dataId < this.mAdapter.getTotalNumber()) {
            return this.mAdapter.updateMetadata(dataId);
        }
        MetadataLoader.loadMetadata(this.mContext, this.mLastData);
        return null;
    }

    @Override
    public boolean isMetadataUpdated(int dataId) {
        return dataId < this.mAdapter.getTotalNumber() ? this.mAdapter.isMetadataUpdated(dataId) : MetadataLoader.isMetadataCached(this.mLastData);
    }
}
