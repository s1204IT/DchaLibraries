package com.android.gallery3d.filtershow.pipeline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.io.File;

public class ProcessingService extends Service {
    private FilterShowActivity mFiltershowActivity;
    private FullresRenderingRequestTask mFullresRenderingRequestTask;
    private HighresRenderingRequestTask mHighresRenderingRequestTask;
    private ImageSavingTask mImageSavingTask;
    private int mNotificationId;
    private ProcessingTaskController mProcessingTaskController;
    private RenderingRequestTask mRenderingRequestTask;
    private UpdatePreviewTask mUpdatePreviewTask;
    private NotificationManager mNotifyMgr = null;
    private Notification.Builder mBuilder = null;
    private final IBinder mBinder = new LocalBinder();
    private boolean mSaving = false;
    private boolean mNeedsAlive = false;

    public void setFiltershowActivity(FilterShowActivity filtershowActivity) {
        this.mFiltershowActivity = filtershowActivity;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        if (this.mUpdatePreviewTask != null) {
            this.mUpdatePreviewTask.setOriginal(originalBitmap);
            this.mHighresRenderingRequestTask.setOriginal(originalBitmap);
            this.mFullresRenderingRequestTask.setOriginal(originalBitmap);
            this.mRenderingRequestTask.setOriginal(originalBitmap);
        }
    }

    public void updatePreviewBuffer() {
        this.mHighresRenderingRequestTask.stop();
        this.mFullresRenderingRequestTask.stop();
        this.mUpdatePreviewTask.updatePreview();
    }

    public void postRenderingRequest(RenderingRequest request) {
        this.mRenderingRequestTask.postRenderingRequest(request);
    }

    public void postHighresRenderingRequest(ImagePreset preset, float scaleFactor, RenderingRequestCaller caller) {
        RenderingRequest request = new RenderingRequest();
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(scaleFactor);
        request.setImagePreset(passedPreset);
        request.setType(5);
        request.setCaller(caller);
        this.mHighresRenderingRequestTask.postRenderingRequest(request);
    }

    public void postFullresRenderingRequest(ImagePreset preset, float scaleFactor, Rect bounds, Rect destination, RenderingRequestCaller caller) {
        RenderingRequest request = new RenderingRequest();
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(scaleFactor);
        request.setImagePreset(passedPreset);
        request.setType(4);
        request.setCaller(caller);
        request.setBounds(bounds);
        request.setDestination(destination);
        passedPreset.setPartialRendering(true, bounds);
        this.mFullresRenderingRequestTask.postRenderingRequest(request);
    }

    public void setHighresPreviewScaleFactor(float highResPreviewScale) {
        this.mHighresRenderingRequestTask.setHighresPreviewScaleFactor(highResPreviewScale);
    }

    public void setPreviewScaleFactor(float previewScale) {
        this.mHighresRenderingRequestTask.setPreviewScaleFactor(previewScale);
        this.mFullresRenderingRequestTask.setPreviewScaleFactor(previewScale);
        this.mRenderingRequestTask.setPreviewScaleFactor(previewScale);
    }

    public void setOriginalBitmapHighres(Bitmap originalHires) {
        this.mHighresRenderingRequestTask.setOriginalBitmapHighres(originalHires);
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public ProcessingService getService() {
            return ProcessingService.this;
        }
    }

    public static Intent getSaveIntent(Context context, ImagePreset preset, File destination, Uri selectedImageUri, Uri sourceImageUri, boolean doFlatten, int quality, float sizeFactor, boolean needsExit) {
        Intent processIntent = new Intent(context, (Class<?>) ProcessingService.class);
        processIntent.putExtra("sourceUri", sourceImageUri.toString());
        processIntent.putExtra("selectedUri", selectedImageUri.toString());
        processIntent.putExtra("quality", quality);
        processIntent.putExtra("sizeFactor", sizeFactor);
        if (destination != null) {
            processIntent.putExtra("destinationFile", destination.toString());
        }
        processIntent.putExtra("preset", preset.getJsonString("Saved"));
        processIntent.putExtra("saving", true);
        processIntent.putExtra("exit", needsExit);
        if (doFlatten) {
            processIntent.putExtra("flatten", true);
        }
        return processIntent;
    }

    @Override
    public void onCreate() {
        this.mProcessingTaskController = new ProcessingTaskController(this);
        this.mImageSavingTask = new ImageSavingTask(this);
        this.mUpdatePreviewTask = new UpdatePreviewTask();
        this.mHighresRenderingRequestTask = new HighresRenderingRequestTask();
        this.mFullresRenderingRequestTask = new FullresRenderingRequestTask();
        this.mRenderingRequestTask = new RenderingRequestTask();
        this.mProcessingTaskController.add(this.mImageSavingTask);
        this.mProcessingTaskController.add(this.mUpdatePreviewTask);
        this.mProcessingTaskController.add(this.mHighresRenderingRequestTask);
        this.mProcessingTaskController.add(this.mFullresRenderingRequestTask);
        this.mProcessingTaskController.add(this.mRenderingRequestTask);
        setupPipeline();
    }

    @Override
    public void onDestroy() {
        tearDownPipeline();
        this.mProcessingTaskController.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mNeedsAlive = true;
        if (intent != null && intent.getBooleanExtra("saving", false)) {
            String presetJson = intent.getStringExtra("preset");
            String source = intent.getStringExtra("sourceUri");
            String selected = intent.getStringExtra("selectedUri");
            String destination = intent.getStringExtra("destinationFile");
            int quality = intent.getIntExtra("quality", 100);
            float sizeFactor = intent.getFloatExtra("sizeFactor", 1.0f);
            boolean flatten = intent.getBooleanExtra("flatten", false);
            boolean exit = intent.getBooleanExtra("exit", false);
            Uri sourceUri = Uri.parse(source);
            Uri selectedUri = null;
            if (selected != null) {
                selectedUri = Uri.parse(selected);
            }
            File destinationFile = null;
            if (destination != null) {
                destinationFile = new File(destination);
            }
            ImagePreset preset = new ImagePreset();
            preset.readJsonFromString(presetJson);
            this.mNeedsAlive = false;
            this.mSaving = true;
            handleSaveRequest(sourceUri, selectedUri, destinationFile, preset, MasterImage.getImage().getHighresImage(), flatten, quality, sizeFactor, exit);
            return 3;
        }
        return 3;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public void onStart() {
        this.mNeedsAlive = true;
        if (!this.mSaving && this.mFiltershowActivity != null) {
            this.mFiltershowActivity.updateUIAfterServiceStarted();
        }
    }

    public void handleSaveRequest(Uri sourceUri, Uri selectedUri, File destinationFile, ImagePreset preset, Bitmap previewImage, boolean flatten, int quality, float sizeFactor, boolean exit) {
        this.mNotifyMgr = (NotificationManager) getSystemService("notification");
        this.mNotifyMgr.cancelAll();
        this.mBuilder = new Notification.Builder(this).setSmallIcon(R.drawable.filtershow_button_fx).setContentTitle(getString(R.string.filtershow_notification_label)).setContentText(getString(R.string.filtershow_notification_message));
        startForeground(this.mNotificationId, this.mBuilder.build());
        updateProgress(6, 0);
        this.mImageSavingTask.saveImage(sourceUri, selectedUri, destinationFile, preset, previewImage, flatten, quality, sizeFactor, exit);
    }

    public void updateNotificationWithBitmap(Bitmap bitmap) {
        this.mBuilder.setLargeIcon(bitmap);
        this.mNotifyMgr.notify(this.mNotificationId, this.mBuilder.build());
    }

    public void updateProgress(int max, int current) {
        this.mBuilder.setProgress(max, current, false);
        this.mNotifyMgr.notify(this.mNotificationId, this.mBuilder.build());
    }

    public void completePreviewSaveImage(Uri result, boolean exit) {
        if (exit && !this.mNeedsAlive && !this.mFiltershowActivity.isSimpleEditAction()) {
            this.mFiltershowActivity.completeSaveImage(result);
        }
    }

    public void completeSaveImage(Uri result, boolean exit) {
        this.mNotifyMgr.cancel(this.mNotificationId);
        if (!exit) {
            stopForeground(true);
            stopSelf();
            return;
        }
        stopForeground(true);
        stopSelf();
        if (this.mNeedsAlive) {
            this.mFiltershowActivity.updateUIAfterServiceStarted();
        } else if (exit || this.mFiltershowActivity.isSimpleEditAction()) {
            this.mFiltershowActivity.completeSaveImage(result);
        }
    }

    private void setupPipeline() {
        Resources res = getResources();
        FiltersManager.setResources(res);
        CachingPipeline.createRenderscriptContext(this);
        FiltersManager filtersManager = FiltersManager.getManager();
        filtersManager.addLooks(this);
        filtersManager.addBorders(this);
        filtersManager.addTools(this);
        filtersManager.addEffects();
        FiltersManager highresFiltersManager = FiltersManager.getHighresManager();
        highresFiltersManager.addLooks(this);
        highresFiltersManager.addBorders(this);
        highresFiltersManager.addTools(this);
        highresFiltersManager.addEffects();
    }

    private void tearDownPipeline() {
        ImageFilter.resetStatics();
        FiltersManager.getPreviewManager().freeRSFilterScripts();
        FiltersManager.getManager().freeRSFilterScripts();
        FiltersManager.getHighresManager().freeRSFilterScripts();
        FiltersManager.reset();
        CachingPipeline.destroyRenderScriptContext();
    }

    static {
        System.loadLibrary("jni_filtershow_filters");
    }
}
