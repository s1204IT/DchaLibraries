package com.android.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.android.camera.Storage;
import com.android.camera.data.LocalData;
import com.android.camera.data.LocalDataAdapter;
import com.android.camera.data.LocalMediaData;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.filmstrip.ImageData;
import com.android.camera.util.Callback;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CameraDataAdapter implements LocalDataAdapter {
    private static final int DEFAULT_DECODE_SIZE = 1600;
    private static final Log.Tag TAG = new Log.Tag("CameraDataAdapter");
    private final Context mContext;
    private DataAdapter.Listener mListener;
    private LocalDataAdapter.LocalDataListener mLocalDataListener;
    private LocalData mLocalDataToDelete;
    private final int mPlaceHolderResourceId;
    private int mSuggestedWidth = DEFAULT_DECODE_SIZE;
    private int mSuggestedHeight = DEFAULT_DECODE_SIZE;
    private long mLastPhotoId = -1;
    private LocalDataList mImages = new LocalDataList();

    public CameraDataAdapter(Context context, int placeholderResource) {
        this.mContext = context;
        this.mPlaceHolderResourceId = placeholderResource;
    }

    @Override
    public void setLocalDataListener(LocalDataAdapter.LocalDataListener listener) {
        this.mLocalDataListener = listener;
    }

    @Override
    public void requestLoadNewPhotos() {
        LoadNewPhotosTask ltask = new LoadNewPhotosTask(this.mLastPhotoId);
        ltask.execute(this.mContext.getContentResolver());
    }

    @Override
    public void requestLoad(Callback<Void> doneCallback) {
        QueryTask qtask = new QueryTask(doneCallback);
        qtask.execute(this.mContext);
    }

    @Override
    public AsyncTask updateMetadata(int dataId) {
        MetadataUpdateTask result = new MetadataUpdateTask();
        result.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Integer.valueOf(dataId));
        return result;
    }

    @Override
    public boolean isMetadataUpdated(int dataId) {
        if (dataId < 0 || dataId >= this.mImages.size()) {
            return true;
        }
        return this.mImages.get(dataId).isMetadataUpdated();
    }

    @Override
    public int getItemViewType(int dataId) {
        if (dataId < 0 || dataId >= this.mImages.size()) {
            return -1;
        }
        return this.mImages.get(dataId).getItemViewType().ordinal();
    }

    @Override
    public LocalData getLocalData(int dataID) {
        if (dataID < 0 || dataID >= this.mImages.size()) {
            return null;
        }
        return this.mImages.get(dataID);
    }

    @Override
    public int getTotalNumber() {
        return this.mImages.size();
    }

    @Override
    public ImageData getImageData(int id) {
        return getLocalData(id);
    }

    @Override
    public void suggestViewSizeBound(int w, int h) {
        this.mSuggestedWidth = w;
        this.mSuggestedHeight = h;
    }

    @Override
    public View getView(Context context, View recycled, int dataID, LocalData.ActionCallback actionCallback) {
        if (dataID >= this.mImages.size() || dataID < 0) {
            return null;
        }
        return this.mImages.get(dataID).getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, this.mPlaceHolderResourceId, this, false, actionCallback);
    }

    @Override
    public void resizeView(Context context, int dataID, View view, int w, int h) {
        if (dataID < this.mImages.size() && dataID >= 0) {
            this.mImages.get(dataID).loadFullImage(context, this.mSuggestedWidth, this.mSuggestedHeight, view, this);
        }
    }

    @Override
    public void setListener(DataAdapter.Listener listener) {
        this.mListener = listener;
        if (this.mImages.size() != 0) {
            this.mListener.onDataLoaded();
        }
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        if (dataID >= this.mImages.size() || dataID <= 0) {
            return true;
        }
        return this.mImages.get(dataID).canSwipeInFullScreen();
    }

    @Override
    public void removeData(int dataID) {
        LocalData d = this.mImages.remove(dataID);
        if (d != null) {
            executeDeletion();
            this.mLocalDataToDelete = d;
            this.mListener.onDataRemoved(dataID, d);
        }
    }

    @Override
    public boolean addData(LocalData newData) {
        Uri uri = newData.getUri();
        int pos = findDataByContentUri(uri);
        if (pos != -1) {
            Log.v(TAG, "found duplicate data: " + uri);
            updateData(pos, newData);
            return false;
        }
        insertData(newData);
        return true;
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        return this.mImages.indexOf(uri);
    }

    @Override
    public boolean undoDataRemoval() {
        if (this.mLocalDataToDelete == null) {
            return false;
        }
        LocalData d = this.mLocalDataToDelete;
        this.mLocalDataToDelete = null;
        insertData(d);
        return true;
    }

    @Override
    public boolean executeDeletion() {
        if (this.mLocalDataToDelete == null) {
            return false;
        }
        DeletionTask task = new DeletionTask();
        task.execute(this.mLocalDataToDelete);
        this.mLocalDataToDelete = null;
        return true;
    }

    @Override
    public void flush() {
        replaceData(new LocalDataList());
    }

    @Override
    public void refresh(Uri uri) {
        int pos = findDataByContentUri(uri);
        if (pos != -1) {
            LocalData data = this.mImages.get(pos);
            LocalData refreshedData = data.refresh(this.mContext);
            if (refreshedData == null && this.mListener != null) {
                this.mListener.onDataRemoved(pos, data);
            } else {
                updateData(pos, refreshedData);
            }
        }
    }

    @Override
    public void updateData(final int pos, LocalData data) {
        this.mImages.set(pos, data);
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

    private void insertData(LocalData data) {
        int pos = 0;
        Comparator<LocalData> comp = new LocalData.NewestFirstComparator();
        while (pos < this.mImages.size() && comp.compare(data, this.mImages.get(pos)) > 0) {
            pos++;
        }
        this.mImages.add(pos, data);
        if (this.mListener != null) {
            this.mListener.onDataInserted(pos, data);
        }
    }

    private void replaceData(LocalDataList list) {
        if (list.size() != 0 || this.mImages.size() != 0) {
            this.mImages = list;
            if (this.mListener != null) {
                this.mListener.onDataLoaded();
            }
        }
    }

    @Override
    public List<AsyncTask> preloadItems(List<Integer> items) {
        List<AsyncTask> result = new ArrayList<>();
        for (Integer id : items) {
            if (!isMetadataUpdated(id.intValue())) {
                result.add(updateMetadata(id.intValue()));
            }
        }
        return result;
    }

    @Override
    public void cancelItems(List<AsyncTask> loadTokens) {
        for (AsyncTask asyncTask : loadTokens) {
            if (asyncTask != null) {
                asyncTask.cancel(false);
            }
        }
    }

    @Override
    public List<Integer> getItemsInRange(int startPosition, int endPosition) {
        List<Integer> result = new ArrayList<>();
        for (int i = Math.max(0, startPosition); i < endPosition; i++) {
            result.add(Integer.valueOf(i));
        }
        return result;
    }

    @Override
    public int getCount() {
        return getTotalNumber();
    }

    private class LoadNewPhotosTask extends AsyncTask<ContentResolver, Void, List<LocalData>> {
        private final long mMinPhotoId;

        public LoadNewPhotosTask(long lastPhotoId) {
            this.mMinPhotoId = lastPhotoId;
        }

        @Override
        protected List<LocalData> doInBackground(ContentResolver... contentResolvers) {
            if (this.mMinPhotoId == -1) {
                return new ArrayList(0);
            }
            Log.v(CameraDataAdapter.TAG, "updating media metadata with photos newer than id: " + this.mMinPhotoId);
            ContentResolver cr = contentResolvers[0];
            return LocalMediaData.PhotoData.query(cr, LocalMediaData.PhotoData.CONTENT_URI, this.mMinPhotoId);
        }

        @Override
        protected void onPostExecute(List<LocalData> newPhotoData) {
            if (newPhotoData == null) {
                Log.w(CameraDataAdapter.TAG, "null data returned from new photos query");
                return;
            }
            Log.v(CameraDataAdapter.TAG, "new photos query return num items: " + newPhotoData.size());
            if (!newPhotoData.isEmpty()) {
                LocalData newestPhoto = newPhotoData.get(0);
                long newLastPhotoId = newestPhoto.getContentId();
                Log.v(CameraDataAdapter.TAG, "updating last photo id (old:new) " + CameraDataAdapter.this.mLastPhotoId + ":" + newLastPhotoId);
                CameraDataAdapter.this.mLastPhotoId = Math.max(CameraDataAdapter.this.mLastPhotoId, newLastPhotoId);
            }
            for (LocalData localData : newPhotoData) {
                Uri sessionUri = Storage.getSessionUriFromContentUri(localData.getUri());
                if (sessionUri == null) {
                    CameraDataAdapter.this.addData(localData);
                }
            }
        }
    }

    private class QueryTaskResult {
        public long mLastPhotoId;
        public LocalDataList mLocalDataList;

        public QueryTaskResult(LocalDataList localDataList, long lastPhotoId) {
            this.mLocalDataList = localDataList;
            this.mLastPhotoId = lastPhotoId;
        }
    }

    private class QueryTask extends AsyncTask<Context, Void, QueryTaskResult> {
        private static final int MAX_METADATA = 5;
        private final Callback<Void> mDoneCallback;

        public QueryTask(Callback<Void> doneCallback) {
            this.mDoneCallback = doneCallback;
        }

        @Override
        protected QueryTaskResult doInBackground(Context... contexts) {
            Context context = contexts[0];
            ContentResolver cr = context.getContentResolver();
            LocalDataList l = new LocalDataList();
            List<LocalData> photoData = LocalMediaData.PhotoData.query(cr, LocalMediaData.PhotoData.CONTENT_URI, -1L);
            List<LocalData> videoData = LocalMediaData.VideoData.query(cr, LocalMediaData.VideoData.CONTENT_URI, -1L);
            long lastPhotoId = -1;
            if (!photoData.isEmpty()) {
                lastPhotoId = photoData.get(0).getContentId();
            }
            if (photoData != null) {
                Log.v(CameraDataAdapter.TAG, "retrieved photo metadata, number of items: " + photoData.size());
                l.addAll(photoData);
            }
            if (videoData != null) {
                Log.v(CameraDataAdapter.TAG, "retrieved video metadata, number of items: " + videoData.size());
                l.addAll(videoData);
            }
            Log.v(CameraDataAdapter.TAG, "sorting video/photo metadata");
            l.sort(new LocalData.NewestFirstComparator());
            Log.v(CameraDataAdapter.TAG, "sorted video/photo metadata");
            for (int i = 0; i < 5 && i < l.size(); i++) {
                LocalData data = l.get(i);
                MetadataLoader.loadMetadata(context, data);
            }
            return CameraDataAdapter.this.new QueryTaskResult(l, lastPhotoId);
        }

        @Override
        protected void onPostExecute(QueryTaskResult result) {
            CameraDataAdapter.this.mLastPhotoId = result.mLastPhotoId;
            CameraDataAdapter.this.replaceData(result.mLocalDataList);
            if (this.mDoneCallback != null) {
                this.mDoneCallback.onCallback(null);
            }
            LoadNewPhotosTask ltask = CameraDataAdapter.this.new LoadNewPhotosTask(CameraDataAdapter.this.mLastPhotoId);
            ltask.execute(CameraDataAdapter.this.mContext.getContentResolver());
        }
    }

    private class DeletionTask extends AsyncTask<LocalData, Void, Void> {
        private DeletionTask() {
        }

        @Override
        protected Void doInBackground(LocalData... data) {
            for (int i = 0; i < data.length; i++) {
                if (!data[i].isDataActionSupported(2)) {
                    Log.v(CameraDataAdapter.TAG, "Deletion is not supported:" + data[i]);
                } else {
                    data[i].delete(CameraDataAdapter.this.mContext);
                }
            }
            return null;
        }
    }

    private class MetadataUpdateTask extends AsyncTask<Integer, Void, List<Integer>> {
        private MetadataUpdateTask() {
        }

        @Override
        protected List<Integer> doInBackground(Integer... dataId) {
            List<Integer> updatedList = new ArrayList<>();
            for (Integer id : dataId) {
                if (id.intValue() >= 0 && id.intValue() < CameraDataAdapter.this.mImages.size()) {
                    LocalData data = CameraDataAdapter.this.mImages.get(id.intValue());
                    if (MetadataLoader.loadMetadata(CameraDataAdapter.this.mContext, data)) {
                        updatedList.add(id);
                    }
                }
            }
            return updatedList;
        }

        @Override
        protected void onPostExecute(final List<Integer> updatedData) {
            if (CameraDataAdapter.this.mListener != null) {
                CameraDataAdapter.this.mListener.onDataUpdated(new DataAdapter.UpdateReporter() {
                    @Override
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    @Override
                    public boolean isDataUpdated(int dataID) {
                        return updatedData.contains(Integer.valueOf(dataID));
                    }
                });
            }
            if (CameraDataAdapter.this.mLocalDataListener != null) {
                CameraDataAdapter.this.mLocalDataListener.onMetadataUpdated(updatedData);
            }
        }
    }
}
