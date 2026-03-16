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
    public void run(MediaSet baseSet) {
        final TreeMap<String, ArrayList<Path>> map = new TreeMap<>();
        final ArrayList<Path> untagged = new ArrayList<>();
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                Path path = item.getPath();
                String[] tags = item.getTags();
                if (tags == null || tags.length == 0) {
                    untagged.add(path);
                    return;
                }
                for (String key : tags) {
                    ArrayList<Path> list = (ArrayList) map.get(key);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(key, list);
                    }
                    list.add(path);
                }
            }
        });
        int m = map.size();
        this.mClusters = new ArrayList<>();
        this.mNames = new String[(untagged.size() > 0 ? 1 : 0) + m];
        int i = 0;
        for (Map.Entry<String, ArrayList<Path>> entry : map.entrySet()) {
            this.mNames[i] = entry.getKey();
            this.mClusters.add(entry.getValue());
            i++;
        }
        if (untagged.size() > 0) {
            int i2 = i + 1;
            this.mNames[i] = this.mUntaggedString;
            this.mClusters.add(untagged);
        }
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        return this.mClusters.get(index);
    }

    @Override
    public String getClusterName(int index) {
        return this.mNames[index];
    }
}
