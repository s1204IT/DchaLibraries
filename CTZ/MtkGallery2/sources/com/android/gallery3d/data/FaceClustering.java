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

        public FaceCluster(String str) {
            this.mName = str;
        }

        public void add(MediaItem mediaItem, int i) {
            this.mPaths.add(mediaItem.getPath());
            Face[] faces = mediaItem.getFaces();
            if (faces != null) {
                Face face = faces[i];
                if (this.mCoverItem == null) {
                    this.mCoverItem = mediaItem;
                    this.mCoverRegion = face.getPosition();
                    this.mCoverFaceIndex = i;
                    return;
                }
                Rect position = face.getPosition();
                if (this.mCoverRegion.width() < position.width() && this.mCoverRegion.height() < position.height()) {
                    this.mCoverItem = mediaItem;
                    this.mCoverRegion = face.getPosition();
                    this.mCoverFaceIndex = i;
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
    public void run(MediaSet mediaSet) {
        final TreeMap treeMap = new TreeMap();
        final FaceCluster faceCluster = new FaceCluster(this.mUntaggedString);
        mediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                Face[] faces = mediaItem.getFaces();
                if (faces == null || faces.length == 0) {
                    faceCluster.add(mediaItem, -1);
                    return;
                }
                for (int i2 = 0; i2 < faces.length; i2++) {
                    Face face = faces[i2];
                    FaceCluster faceCluster2 = (FaceCluster) treeMap.get(face);
                    if (faceCluster2 == null) {
                        faceCluster2 = FaceClustering.this.new FaceCluster(face.getName());
                        treeMap.put(face, faceCluster2);
                    }
                    faceCluster2.add(mediaItem, i2);
                }
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        int size = treeMap.size();
        this.mClusters = (FaceCluster[]) treeMap.values().toArray(new FaceCluster[(faceCluster.size() > 0 ? 1 : 0) + size]);
        if (faceCluster.size() > 0) {
            this.mClusters[size] = faceCluster;
        }
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.length;
    }

    @Override
    public ArrayList<Path> getCluster(int i) {
        return this.mClusters[i].mPaths;
    }

    @Override
    public String getClusterName(int i) {
        return this.mClusters[i].mName;
    }

    @Override
    public MediaItem getClusterCover(int i) {
        return this.mClusters[i].getCover();
    }
}
