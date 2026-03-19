package com.android.gallery3d.data;

import android.content.Context;
import android.content.res.Resources;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public class SizeClustering extends Clustering {
    private static final long[] SIZE_LEVELS = {0, 1048576, 10485760, 104857600, 1073741824, 2147483648L, 4294967296L};
    private ArrayList<Path>[] mClusters;
    private Context mContext;
    private long[] mMinSizes;
    private String[] mNames;

    public SizeClustering(Context context) {
        this.mContext = context;
    }

    @Override
    public void run(MediaSet mediaSet) {
        final ArrayList<Path>[] arrayListArr = new ArrayList[SIZE_LEVELS.length];
        mediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                long size = mediaItem.getSize();
                int i2 = 0;
                while (i2 < SizeClustering.SIZE_LEVELS.length - 1) {
                    int i3 = i2 + 1;
                    if (size < SizeClustering.SIZE_LEVELS[i3]) {
                        break;
                    } else {
                        i2 = i3;
                    }
                }
                ArrayList arrayList = arrayListArr[i2];
                if (arrayList == null) {
                    arrayList = new ArrayList();
                    arrayListArr[i2] = arrayList;
                }
                arrayList.add(mediaItem.getPath());
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        int i = 0;
        for (ArrayList<Path> arrayList : arrayListArr) {
            if (arrayList != null) {
                i++;
            }
        }
        this.mClusters = new ArrayList[i];
        this.mNames = new String[i];
        this.mMinSizes = new long[i];
        Resources resources = this.mContext.getResources();
        int i2 = 0;
        for (int length = arrayListArr.length - 1; length >= 0; length--) {
            if (arrayListArr[length] != null) {
                this.mClusters[i2] = arrayListArr[length];
                if (length == 0) {
                    this.mNames[i2] = String.format(resources.getString(R.string.size_below), getSizeString(length + 1));
                } else if (length == arrayListArr.length - 1) {
                    this.mNames[i2] = String.format(resources.getString(R.string.size_above), getSizeString(length));
                } else {
                    this.mNames[i2] = String.format(resources.getString(R.string.size_between), getSizeString(length), getSizeString(length + 1));
                }
                this.mMinSizes[i2] = SIZE_LEVELS[length];
                i2++;
            }
        }
    }

    private String getSizeString(int i) {
        long j = SIZE_LEVELS[i];
        if (j >= 1073741824) {
            return (j / 1073741824) + "GB";
        }
        return (j / 1048576) + "MB";
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.length;
    }

    @Override
    public ArrayList<Path> getCluster(int i) {
        return this.mClusters[i];
    }

    @Override
    public String getClusterName(int i) {
        return this.mNames[i];
    }

    public long getMinSize(int i) {
        return this.mMinSizes[i];
    }
}
