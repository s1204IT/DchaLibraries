package com.android.gallery3d.filtershow.pipeline;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.mediatek.gallery3d.util.Log;
import java.io.File;
import mf.org.apache.xerces.impl.xpath.XPath;

public class ProcessingService extends Service {
    private static String sUsePresent;
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

    public void setFiltershowActivity(FilterShowActivity filterShowActivity) {
        this.mFiltershowActivity = filterShowActivity;
    }

    public void setOriginalBitmap(Bitmap bitmap) {
        if (this.mUpdatePreviewTask == null) {
            return;
        }
        this.mUpdatePreviewTask.setOriginal(bitmap);
        this.mHighresRenderingRequestTask.setOriginal(bitmap);
        this.mFullresRenderingRequestTask.setOriginal(bitmap);
        this.mRenderingRequestTask.setOriginal(bitmap);
    }

    public void updatePreviewBuffer() {
        this.mHighresRenderingRequestTask.stop();
        this.mFullresRenderingRequestTask.stop();
        this.mUpdatePreviewTask.updatePreview();
    }

    public void postRenderingRequest(RenderingRequest renderingRequest) {
        this.mRenderingRequestTask.postRenderingRequest(renderingRequest);
    }

    public void postHighresRenderingRequest(ImagePreset imagePreset, float f, RenderingRequestCaller renderingRequestCaller) {
        RenderingRequest renderingRequest = new RenderingRequest();
        ImagePreset imagePreset2 = new ImagePreset(imagePreset);
        renderingRequest.setOriginalImagePreset(imagePreset);
        renderingRequest.setScaleFactor(f);
        renderingRequest.setImagePreset(imagePreset2);
        renderingRequest.setType(5);
        renderingRequest.setCaller(renderingRequestCaller);
        this.mHighresRenderingRequestTask.postRenderingRequest(renderingRequest);
    }

    public void postFullresRenderingRequest(ImagePreset imagePreset, float f, Rect rect, Rect rect2, RenderingRequestCaller renderingRequestCaller) {
        RenderingRequest renderingRequest = new RenderingRequest();
        ImagePreset imagePreset2 = new ImagePreset(imagePreset);
        renderingRequest.setOriginalImagePreset(imagePreset);
        renderingRequest.setScaleFactor(f);
        renderingRequest.setImagePreset(imagePreset2);
        renderingRequest.setType(4);
        renderingRequest.setCaller(renderingRequestCaller);
        renderingRequest.setBounds(rect);
        renderingRequest.setDestination(rect2);
        imagePreset2.setPartialRendering(true, rect);
        this.mFullresRenderingRequestTask.postRenderingRequest(renderingRequest);
    }

    public void setHighresPreviewScaleFactor(float f) {
        this.mHighresRenderingRequestTask.setHighresPreviewScaleFactor(f);
    }

    public void setPreviewScaleFactor(float f) {
        this.mHighresRenderingRequestTask.setPreviewScaleFactor(f);
        this.mFullresRenderingRequestTask.setPreviewScaleFactor(f);
        this.mRenderingRequestTask.setPreviewScaleFactor(f);
    }

    public void setOriginalBitmapHighres(Bitmap bitmap) {
        this.mHighresRenderingRequestTask.setOriginalBitmapHighres(bitmap);
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public ProcessingService getService() {
            return ProcessingService.this;
        }
    }

    public static Intent getSaveIntent(Context context, ImagePreset imagePreset, File file, Uri uri, Uri uri2, boolean z, int i, float f, boolean z2) {
        Intent intent = new Intent(context, (Class<?>) ProcessingService.class);
        intent.putExtra("sourceUri", uri2.toString());
        intent.putExtra("selectedUri", uri.toString());
        intent.putExtra("quality", i);
        intent.putExtra("sizeFactor", f);
        if (file != null) {
            intent.putExtra("destinationFile", file.toString());
        }
        sUsePresent = imagePreset.getJsonString("Saved");
        Log.d("ProcessingService", "<getSaveIntent> sUsePresent = " + sUsePresent);
        intent.putExtra("saving", true);
        intent.putExtra("exit", z2);
        if (z) {
            intent.putExtra("flatten", true);
        }
        return intent;
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
    public int onStartCommand(Intent intent, int i, int i2) {
        Uri uri;
        this.mNeedsAlive = true;
        if (intent != null && intent.getBooleanExtra("saving", false)) {
            String stringExtra = intent.getStringExtra("preset");
            if (stringExtra == null) {
                stringExtra = sUsePresent;
                Log.d("ProcessingService", "<onStartCommand> presetJson = " + stringExtra);
            }
            String stringExtra2 = intent.getStringExtra("sourceUri");
            String stringExtra3 = intent.getStringExtra("selectedUri");
            String stringExtra4 = intent.getStringExtra("destinationFile");
            int intExtra = intent.getIntExtra("quality", 100);
            float floatExtra = intent.getFloatExtra("sizeFactor", 1.0f);
            boolean booleanExtra = intent.getBooleanExtra("flatten", false);
            boolean booleanExtra2 = intent.getBooleanExtra("exit", false);
            Uri uri2 = Uri.parse(stringExtra2);
            File file = null;
            if (stringExtra3 != null) {
                uri = Uri.parse(stringExtra3);
            } else {
                uri = null;
            }
            if (stringExtra4 != null) {
                file = new File(stringExtra4);
            }
            ImagePreset imagePreset = new ImagePreset();
            if (stringExtra != null) {
                imagePreset.readJsonFromString(stringExtra);
            }
            this.mNeedsAlive = false;
            this.mSaving = true;
            handleSaveRequest(uri2, uri, file, imagePreset, MasterImage.getImage().getHighresImage(), booleanExtra, intExtra, floatExtra, booleanExtra2);
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

    public void handleSaveRequest(Uri uri, Uri uri2, File file, ImagePreset imagePreset, Bitmap bitmap, boolean z, int i, float f, boolean z2) {
        this.mNotifyMgr = (NotificationManager) getSystemService("notification");
        this.mNotifyMgr.cancelAll();
        this.mBuilder = new Notification.Builder(this).setSmallIcon(R.drawable.filtershow_button_fx).setContentTitle(getString(R.string.filtershow_notification_label)).setContentText(getString(R.string.filtershow_notification_message));
        createChannel(this.mBuilder);
        startForeground(this.mNotificationId, this.mBuilder.build());
        updateProgress(6, 0);
        this.mImageSavingTask.saveImage(uri, uri2, file, imagePreset, bitmap, z, i, f, z2);
    }

    public void updateNotificationWithBitmap(Bitmap bitmap) {
        this.mBuilder.setLargeIcon(bitmap);
        this.mNotifyMgr.notify(this.mNotificationId, this.mBuilder.build());
    }

    public void updateProgress(int i, int i2) {
        this.mBuilder.setProgress(i, i2, false);
        this.mNotifyMgr.notify(this.mNotificationId, this.mBuilder.build());
    }

    public void completePreviewSaveImage(Uri uri, boolean z) {
        if (z && !this.mNeedsAlive && !this.mFiltershowActivity.isSimpleEditAction()) {
            this.mFiltershowActivity.completeSaveImage(uri);
        }
    }

    public void completeSaveImage(Uri uri, boolean z) {
        this.mSaving = false;
        this.mNotifyMgr.cancel(this.mNotificationId);
        if (!z) {
            stopForeground(true);
            stopSelf();
            if (this.mNeedsAlive) {
                this.mFiltershowActivity.updateUIAfterServiceStarted();
                return;
            }
            return;
        }
        stopForeground(true);
        stopSelf();
        if (this.mNeedsAlive) {
            this.mFiltershowActivity.updateUIAfterServiceStarted();
        } else if (z || this.mFiltershowActivity.isSimpleEditAction()) {
            this.mFiltershowActivity.completeSaveImage(uri);
        }
    }

    private void setupPipeline() {
        Resources resources = getResources();
        FiltersManager.setBorderSampleSize(parseBorderSampleSize());
        FiltersManager.setResources(resources);
        CachingPipeline.createRenderscriptContext(this);
        FiltersManager manager = FiltersManager.getManager();
        manager.addLooks(this);
        manager.addBorders(this);
        manager.addTools(this);
        manager.addEffects();
        FiltersManager highresManager = FiltersManager.getHighresManager();
        highresManager.addLooks(this);
        highresManager.addBorders(this);
        highresManager.addTools(this);
        highresManager.addEffects();
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
        System.loadLibrary("jni_filtershow_filters_mtk");
    }

    public boolean isRenderingTaskBusy() {
        return this.mRenderingRequestTask.isProcessingTaskBusy();
    }

    private int parseBorderSampleSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        int iMin = Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels);
        if (iMin >= 1152) {
            return 4;
        }
        if (iMin >= 1080) {
            return 3;
        }
        return 2;
    }

    @TargetApi(XPath.Tokens.EXPRTOKEN_OPERATOR_EQUAL)
    private void createChannel(Notification.Builder builder) {
        Log.i("ProcessingService", "<createChannel> SDK: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 26) {
            this.mNotifyMgr.createNotificationChannel(new NotificationChannel("filtershow_channel", "filtershow", 2));
            builder.setChannelId("filtershow_channel");
        }
    }
}
