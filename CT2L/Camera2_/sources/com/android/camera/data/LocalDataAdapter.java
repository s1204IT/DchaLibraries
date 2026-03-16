package com.android.camera.data;

import android.net.Uri;
import android.os.AsyncTask;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.util.Callback;
import com.android.camera.widget.Preloader;
import java.util.List;

public interface LocalDataAdapter extends DataAdapter, Preloader.ItemLoader<Integer, AsyncTask>, Preloader.ItemSource<Integer> {

    public interface LocalDataListener {
        void onMetadataUpdated(List<Integer> list);
    }

    boolean addData(LocalData localData);

    boolean executeDeletion();

    int findDataByContentUri(Uri uri);

    void flush();

    LocalData getLocalData(int i);

    boolean isMetadataUpdated(int i);

    void refresh(Uri uri);

    void removeData(int i);

    void requestLoad(Callback<Void> callback);

    void requestLoadNewPhotos();

    void setLocalDataListener(LocalDataListener localDataListener);

    boolean undoDataRemoval();

    void updateData(int i, LocalData localData);

    AsyncTask updateMetadata(int i);
}
