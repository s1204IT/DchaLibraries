package com.mediatek.camera.common.loader;

import android.content.Context;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.photo.PhotoModeEntry;
import com.mediatek.camera.common.mode.photo.intent.IntentPhotoModeEntry;
import com.mediatek.camera.common.mode.video.VideoModeEntry;
import com.mediatek.camera.common.mode.video.intentvideo.IntentVideoModeEntry;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.feature.mode.longexposure.LongExposureModeEntry;
import com.mediatek.camera.feature.mode.panorama.PanoramaEntry;
import com.mediatek.camera.feature.mode.slowmotion.SlowMotionEntry;
import com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoEntry;
import com.mediatek.camera.feature.setting.CameraSwitcherEntry;
import com.mediatek.camera.feature.setting.ContinuousShotEntry;
import com.mediatek.camera.feature.setting.aaaroidebug.AaaRoiDebugEntry;
import com.mediatek.camera.feature.setting.ais.AISEntry;
import com.mediatek.camera.feature.setting.antiflicker.AntiFlickerEntry;
import com.mediatek.camera.feature.setting.dng.DngEntry;
import com.mediatek.camera.feature.setting.eis.EISEntry;
import com.mediatek.camera.feature.setting.exposure.ExposureEntry;
import com.mediatek.camera.feature.setting.facedetection.FaceDetectionEntry;
import com.mediatek.camera.feature.setting.flash.FlashEntry;
import com.mediatek.camera.feature.setting.focus.FocusEntry;
import com.mediatek.camera.feature.setting.format.FormatEntry;
import com.mediatek.camera.feature.setting.hdr.HdrEntry;
import com.mediatek.camera.feature.setting.iso.ISOEntry;
import com.mediatek.camera.feature.setting.microphone.MicroPhoneEntry;
import com.mediatek.camera.feature.setting.noisereduction.NoiseReductionEntry;
import com.mediatek.camera.feature.setting.picturesize.PictureSizeEntry;
import com.mediatek.camera.feature.setting.postview.PostViewEntry;
import com.mediatek.camera.feature.setting.previewmode.PreviewModeEntry;
import com.mediatek.camera.feature.setting.scenemode.SceneModeEntry;
import com.mediatek.camera.feature.setting.selftimer.SelfTimerEntry;
import com.mediatek.camera.feature.setting.shutterspeed.ShutterSpeedEntry;
import com.mediatek.camera.feature.setting.videoquality.VideoQualityEntry;
import com.mediatek.camera.feature.setting.whitebalance.WhiteBalanceEntry;
import com.mediatek.camera.feature.setting.zoom.ZoomEntry;
import com.mediatek.camera.feature.setting.zsd.ZSDEntry;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureLoader {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FeatureLoader.class.getSimpleName());
    private static ConcurrentHashMap<String, IFeatureEntry> sBuildInEntries = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, IFeatureEntry> sPluginEntries = new ConcurrentHashMap<>();

    public static void updateSettingCurrentModeKey(Context context, String str) {
        LogHelper.d(TAG, "[updateCurrentModeKey] current mode key:" + str);
        if (sBuildInEntries.size() <= 0) {
            loadBuildInFeatures(context);
        }
    }

    public static void notifySettingBeforeOpenCamera(Context context, String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
        LogHelper.d(TAG, "[notifySettingBeforeOpenCamera] id:" + str + ", api:" + cameraApi);
        if (sBuildInEntries.size() <= 0) {
            loadBuildInFeatures(context);
        }
        Iterator<Map.Entry<String, IFeatureEntry>> it = sBuildInEntries.entrySet().iterator();
        while (it.hasNext()) {
            IFeatureEntry value = it.next().getValue();
            if (ICameraSetting.class.equals(value.getType())) {
                value.notifyBeforeOpenCamera(str, cameraApi);
            }
        }
    }

    public static ConcurrentHashMap<String, IFeatureEntry> loadPluginFeatures(Context context) {
        return sPluginEntries;
    }

    public static ConcurrentHashMap<String, IFeatureEntry> loadBuildInFeatures(Context context) {
        if (sBuildInEntries.size() > 0) {
            return sBuildInEntries;
        }
        IPerformanceProfile iPerformanceProfileCreate = PerformanceTracker.create(TAG, "Build-in Loading");
        iPerformanceProfileCreate.start();
        sBuildInEntries = new ConcurrentHashMap<>(loadClasses(context));
        iPerformanceProfileCreate.stop();
        return sBuildInEntries;
    }

    private static LinkedHashMap<String, IFeatureEntry> loadClasses(Context context) {
        LinkedHashMap<String, IFeatureEntry> linkedHashMap = new LinkedHashMap<>();
        DeviceSpec deviceSpec = CameraApiHelper.getDeviceSpec(context);
        PostViewEntry postViewEntry = new PostViewEntry(context, context.getResources());
        postViewEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.postview.PostViewEntry", postViewEntry);
        CameraSwitcherEntry cameraSwitcherEntry = new CameraSwitcherEntry(context, context.getResources());
        cameraSwitcherEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.CameraSwitcherEntry", cameraSwitcherEntry);
        ContinuousShotEntry continuousShotEntry = new ContinuousShotEntry(context, context.getResources());
        continuousShotEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.ContinuousShotEntry", continuousShotEntry);
        DngEntry dngEntry = new DngEntry(context, context.getResources());
        dngEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.dng.DngEntry", dngEntry);
        SelfTimerEntry selfTimerEntry = new SelfTimerEntry(context, context.getResources());
        selfTimerEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.selftimer.SelfTimerEntry", selfTimerEntry);
        FaceDetectionEntry faceDetectionEntry = new FaceDetectionEntry(context, context.getResources());
        faceDetectionEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.facedetection.FaceDetectionEntry", faceDetectionEntry);
        FlashEntry flashEntry = new FlashEntry(context, context.getResources());
        flashEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.flash.FlashEntry", flashEntry);
        HdrEntry hdrEntry = new HdrEntry(context, context.getResources());
        hdrEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.hdr.HdrEntry", hdrEntry);
        PictureSizeEntry pictureSizeEntry = new PictureSizeEntry(context, context.getResources());
        pictureSizeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.picturesize.PictureSizeEntry", pictureSizeEntry);
        PreviewModeEntry previewModeEntry = new PreviewModeEntry(context, context.getResources());
        previewModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.previewmode.PreviewModeEntry", previewModeEntry);
        VideoQualityEntry videoQualityEntry = new VideoQualityEntry(context, context.getResources());
        videoQualityEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.videoquality.VideoQualityEntry", videoQualityEntry);
        ZoomEntry zoomEntry = new ZoomEntry(context, context.getResources());
        zoomEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.zoom.ZoomEntry", zoomEntry);
        FocusEntry focusEntry = new FocusEntry(context, context.getResources());
        focusEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.focus.FocusEntry", focusEntry);
        ExposureEntry exposureEntry = new ExposureEntry(context, context.getResources());
        exposureEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.exposure.ExposureEntry", exposureEntry);
        MicroPhoneEntry microPhoneEntry = new MicroPhoneEntry(context, context.getResources());
        microPhoneEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.microphone.MicroPhoneEntry", microPhoneEntry);
        NoiseReductionEntry noiseReductionEntry = new NoiseReductionEntry(context, context.getResources());
        noiseReductionEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.noisereduction.NoiseReductionEntry", noiseReductionEntry);
        EISEntry eISEntry = new EISEntry(context, context.getResources());
        eISEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.eis.EISEntry", eISEntry);
        AISEntry aISEntry = new AISEntry(context, context.getResources());
        aISEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.ais.AISEntry", aISEntry);
        SceneModeEntry sceneModeEntry = new SceneModeEntry(context, context.getResources());
        sceneModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.scenemode.SceneModeEntry", sceneModeEntry);
        WhiteBalanceEntry whiteBalanceEntry = new WhiteBalanceEntry(context, context.getResources());
        whiteBalanceEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.whitebalance.WhiteBalanceEntry", whiteBalanceEntry);
        AntiFlickerEntry antiFlickerEntry = new AntiFlickerEntry(context, context.getResources());
        antiFlickerEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.antiflicker.AntiFlickerEntry", antiFlickerEntry);
        ZSDEntry zSDEntry = new ZSDEntry(context, context.getResources());
        zSDEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.zsd.ZSDEntry", zSDEntry);
        ISOEntry iSOEntry = new ISOEntry(context, context.getResources());
        iSOEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.iso.ISOEntry", iSOEntry);
        AaaRoiDebugEntry aaaRoiDebugEntry = new AaaRoiDebugEntry(context, context.getResources());
        aaaRoiDebugEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.aaaroidebug.AaaRoiDebugEntry", aaaRoiDebugEntry);
        SdofPhotoEntry sdofPhotoEntry = new SdofPhotoEntry(context, context.getResources());
        sdofPhotoEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoEntry", sdofPhotoEntry);
        PanoramaEntry panoramaEntry = new PanoramaEntry(context, context.getResources());
        panoramaEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.mode.panorama.PanoramaEntry", panoramaEntry);
        ShutterSpeedEntry shutterSpeedEntry = new ShutterSpeedEntry(context, context.getResources());
        shutterSpeedEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.shutterspeed.ShutterSpeedEntry", shutterSpeedEntry);
        LongExposureModeEntry longExposureModeEntry = new LongExposureModeEntry(context, context.getResources());
        longExposureModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.mode.longexposure.LongExposureModeEntry", longExposureModeEntry);
        PhotoModeEntry photoModeEntry = new PhotoModeEntry(context, context.getResources());
        photoModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.common.mode.photo.PhotoModeEntry", photoModeEntry);
        VideoModeEntry videoModeEntry = new VideoModeEntry(context, context.getResources());
        videoModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.common.mode.video.VideoModeEntry", videoModeEntry);
        IntentVideoModeEntry intentVideoModeEntry = new IntentVideoModeEntry(context, context.getResources());
        intentVideoModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoModeEntry", intentVideoModeEntry);
        IntentPhotoModeEntry intentPhotoModeEntry = new IntentPhotoModeEntry(context, context.getResources());
        intentPhotoModeEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.common.mode.photo.intent.IntentPhotoModeEntry", intentPhotoModeEntry);
        SlowMotionEntry slowMotionEntry = new SlowMotionEntry(context, context.getResources());
        slowMotionEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.mode.slowmotion.SlowMotionEntry", slowMotionEntry);
        FormatEntry formatEntry = new FormatEntry(context, context.getResources());
        formatEntry.setDeviceSpec(deviceSpec);
        linkedHashMap.put("com.mediatek.camera.feature.setting.format.FormatEntry", formatEntry);
        return linkedHashMap;
    }
}
