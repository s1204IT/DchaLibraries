package com.android.gallery3d.data;

import android.content.Context;
import android.graphics.Rect;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.picasasource.PicasaSource;
import java.util.ArrayList;
import java.util.TreeMap;

public class FaceClustering extends Clustering {
    private FaceCluster[] mClusters;
    private Context mContext;
    private String mUntaggedString;

    private class FaceCluster {
        int mCoverFaceIndex;
        MediaItem mCoverItem;
        Rect mCoverRegion;
        String mName;
        ArrayList<Path> mPaths = new ArrayList<>();

        public FaceCluster(String name) {
            this.mName = name;
        }

        public void add(MediaItem item, int faceIndex) {
            Path path = item.getPath();
            this.mPaths.add(path);
            Face[] faces = item.getFaces();
            if (faces != null) {
                Face face = faces[faceIndex];
                if (this.mCoverItem == null) {
                    this.mCoverItem = item;
                    this.mCoverRegion = face.getPosition();
                    this.mCoverFaceIndex = faceIndex;
                    return;
                }
                Rect region = face.getPosition();
                if (this.mCoverRegion.width() < region.width() && this.mCoverRegion.height() < region.height()) {
                    this.mCoverItem = item;
                    this.mCoverRegion = face.getPosition();
                    this.mCoverFaceIndex = faceIndex;
                }
            }
        }

        public int size() {
            return this.mPaths.size();
        }

        public MediaItem getCover() {
            if (this.mCoverItem != null) {
                if (PicasaSource.isPicasaImage(this.mCoverItem)) {
                    return PicasaSource.getFaceItem(FaceClustering.this.mContext, this.mCoverItem, this.mCoverFaceIndex);
                }
                return this.mCoverItem;
            }
            return null;
        }
    }

    public FaceClustering(Context context) {
        this.mUntaggedString = context.getResources().getString(R.string.untagged);
        this.mContext = context;
    }

    @Override
    public void run(MediaSet baseSet) {
        final TreeMap<Face, FaceCluster> map = new TreeMap<>();
        final FaceCluster untagged = new FaceCluster(this.mUntaggedString);
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                Face[] faces = item.getFaces();
                if (faces == null || faces.length == 0) {
                    untagged.add(item, -1);
                    return;
                }
                for (int j = 0; j < faces.length; j++) {
                    Face face = faces[j];
                    FaceCluster cluster = (FaceCluster) map.get(face);
                    if (cluster == null) {
                        cluster = FaceClustering.this.new FaceCluster(face.getName());
                        map.put(face, cluster);
                    }
                    cluster.add(item, j);
                }
            }
        });
        int m = map.size();
        this.mClusters = (FaceCluster[]) map.values().toArray(new FaceCluster[(untagged.size() > 0 ? 1 : 0) + m]);
        if (untagged.size() > 0) {
            this.mClusters[m] = untagged;
        }
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.length;
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        return this.mClusters[index].mPaths;
    }

    @Override
    public String getClusterName(int index) {
        return this.mClusters[index].mName;
    }

    @Override
    public MediaItem getClusterCover(int index) {
        return this.mClusters[index].getCover();
    }
}
