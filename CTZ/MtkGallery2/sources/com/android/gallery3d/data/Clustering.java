package com.android.gallery3d.data;

import java.util.ArrayList;

public abstract class Clustering {
    protected boolean mStopEnumerate = false;

    public abstract ArrayList<Path> getCluster(int i);

    public abstract String getClusterName(int i);

    public abstract int getNumberOfClusters();

    public abstract void run(MediaSet mediaSet);

    public MediaItem getClusterCover(int i) {
        return null;
    }

    public boolean reGenerateName() {
        return true;
    }
}
