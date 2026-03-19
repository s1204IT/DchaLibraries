package com.android.gallery3d.data;

import android.content.Context;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class TagClustering extends Clustering {
    private ArrayList<ArrayList<Path>> mClusters;
    private String[] mNames;
    private String mUntaggedString;

    public TagClustering(Context context) {
        this.mUntaggedString = context.getResources().getString(R.string.untagged);
    }

    @Override
    public void run(MediaSet mediaSet) {
        final TreeMap treeMap = new TreeMap();
        final ArrayList<Path> arrayList = new ArrayList<>();
        mediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                Path path = mediaItem.getPath();
                String[] tags = mediaItem.getTags();
                if (tags == null || tags.length == 0) {
                    arrayList.add(path);
                    return;
                }
                for (String str : tags) {
                    ArrayList arrayList2 = (ArrayList) treeMap.get(str);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                        treeMap.put(str, arrayList2);
                    }
                    arrayList2.add(path);
                }
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        int size = treeMap.size();
        this.mClusters = new ArrayList<>();
        int i = 0;
        this.mNames = new String[size + (arrayList.size() > 0 ? 1 : 0)];
        for (Map.Entry entry : treeMap.entrySet()) {
            this.mNames[i] = (String) entry.getKey();
            this.mClusters.add((ArrayList) entry.getValue());
            i++;
        }
        if (arrayList.size() > 0) {
            this.mNames[i] = this.mUntaggedString;
            this.mClusters.add(arrayList);
        }
    }

    @Override
    public int getNumberOfClusters() {
        if (this.mClusters != null) {
            return this.mClusters.size();
        }
        return 0;
    }

    @Override
    public ArrayList<Path> getCluster(int i) {
        return this.mClusters.get(i);
    }

    @Override
    public String getClusterName(int i) {
        return this.mNames[i];
    }
}
