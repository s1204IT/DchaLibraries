package com.android.settings.search;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.overlay.FeatureFactory;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class DeviceIndexUpdateJobService extends JobService {

    @VisibleForTesting
    protected boolean mRunningJob;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        if (!this.mRunningJob) {
            this.mRunningJob = true;
            Thread thread = new Thread(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.updateIndex(jobParameters);
                }
            });
            thread.setPriority(1);
            thread.start();
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (!this.mRunningJob) {
            return false;
        }
        this.mRunningJob = false;
        return true;
    }

    @VisibleForTesting
    public void updateIndex(JobParameters jobParameters) {
        DeviceIndexFeatureProvider deviceIndexFeatureProvider = FeatureFactory.getFactory(this).getDeviceIndexFeatureProvider();
        SliceViewManager sliceViewManager = getSliceViewManager();
        Uri uriBuild = new Uri.Builder().scheme("content").authority("com.android.settings.slices").build();
        Uri uriBuild2 = new Uri.Builder().scheme("content").authority("android.settings.slices").build();
        Collection<Uri> sliceDescendants = sliceViewManager.getSliceDescendants(uriBuild);
        sliceDescendants.addAll(sliceViewManager.getSliceDescendants(uriBuild2));
        deviceIndexFeatureProvider.clearIndex(this);
        for (Uri uri : sliceDescendants) {
            if (!this.mRunningJob) {
                return;
            }
            Slice sliceBindSliceSynchronous = bindSliceSynchronous(sliceViewManager, uri);
            SliceMetadata metadata = getMetadata(sliceBindSliceSynchronous);
            CharSequence charSequenceFindTitle = findTitle(sliceBindSliceSynchronous, metadata);
            if (charSequenceFindTitle != null) {
                deviceIndexFeatureProvider.index(this, charSequenceFindTitle, uri, DeviceIndexFeatureProvider.createDeepLink(new Intent("com.android.settings.action.VIEW_SLICE").setPackage(getPackageName()).putExtra("slice", uri.toString()).toUri(2)), metadata.getSliceKeywords());
            }
        }
        jobFinished(jobParameters, false);
    }

    protected SliceViewManager getSliceViewManager() {
        return SliceViewManager.getInstance(this);
    }

    protected SliceMetadata getMetadata(Slice slice) {
        return SliceMetadata.from(this, slice);
    }

    protected CharSequence findTitle(Slice slice, SliceMetadata sliceMetadata) {
        ListContent listContent = new ListContent(null, slice);
        SliceItem headerItem = listContent.getHeaderItem();
        if (headerItem == null) {
            if (listContent.getRowItems().size() == 0) {
                return null;
            }
            headerItem = listContent.getRowItems().get(0);
        }
        SliceItem sliceItemFind = SliceQuery.find(headerItem, "text", "title", (String) null);
        if (sliceItemFind != null) {
            return sliceItemFind.getText();
        }
        SliceItem sliceItemFind2 = SliceQuery.find(headerItem, "text", "large", (String) null);
        if (sliceItemFind2 != null) {
            return sliceItemFind2.getText();
        }
        SliceItem sliceItemFind3 = SliceQuery.find(headerItem, "text");
        if (sliceItemFind3 != null) {
            return sliceItemFind3.getText();
        }
        return null;
    }

    protected Slice bindSliceSynchronous(final SliceViewManager sliceViewManager, final Uri uri) {
        final Slice[] sliceArr = new Slice[1];
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        SliceViewManager.SliceCallback sliceCallback = new SliceViewManager.SliceCallback() {
            @Override
            public void onSliceUpdated(Slice slice) {
                try {
                    if (SliceMetadata.from(DeviceIndexUpdateJobService.this, slice).getLoadingState() == 2) {
                        sliceArr[0] = slice;
                        countDownLatch.countDown();
                        sliceViewManager.unregisterSliceCallback(uri, this);
                    }
                } catch (Exception e) {
                    Log.w("DeviceIndexUpdate", uri + " cannot be indexed", e);
                    sliceArr[0] = slice;
                }
            }
        };
        sliceViewManager.registerSliceCallback(uri, sliceCallback);
        sliceCallback.onSliceUpdated(sliceViewManager.bindSlice(uri));
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
        }
        return sliceArr[0];
    }
}
