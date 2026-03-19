package android.hardware.camera2;

import android.content.RestrictionsManager;
import android.graphics.Rect;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.PublicKey;
import android.hardware.camera2.impl.SyntheticKey;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.hardware.camera2.utils.SurfaceUtils;
import android.hardware.camera2.utils.TypeReference;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CaptureRequest extends CameraMetadata<Key<?>> implements Parcelable {
    private final String TAG;
    private boolean mIsPartOfCHSRequestList;
    private boolean mIsReprocess;
    private String mLogicalCameraId;
    private CameraMetadataNative mLogicalCameraSettings;
    private final HashMap<String, CameraMetadataNative> mPhysicalCameraSettings;
    private int mReprocessableSessionId;
    private int[] mStreamIdxArray;
    private boolean mSurfaceConverted;
    private int[] mSurfaceIdxArray;
    private final ArraySet<Surface> mSurfaceSet;
    private final Object mSurfacesLock;
    private Object mUserTag;
    private static final ArraySet<Surface> mEmptySurfaceSet = new ArraySet<>();
    public static final Parcelable.Creator<CaptureRequest> CREATOR = new Parcelable.Creator<CaptureRequest>() {
        @Override
        public CaptureRequest createFromParcel(Parcel parcel) {
            CaptureRequest captureRequest = new CaptureRequest();
            captureRequest.readFromParcel(parcel);
            return captureRequest;
        }

        @Override
        public CaptureRequest[] newArray(int i) {
            return new CaptureRequest[i];
        }
    };

    @PublicKey
    public static final Key<Integer> COLOR_CORRECTION_MODE = new Key<>("android.colorCorrection.mode", Integer.TYPE);

    @PublicKey
    public static final Key<ColorSpaceTransform> COLOR_CORRECTION_TRANSFORM = new Key<>("android.colorCorrection.transform", ColorSpaceTransform.class);

    @PublicKey
    public static final Key<RggbChannelVector> COLOR_CORRECTION_GAINS = new Key<>("android.colorCorrection.gains", RggbChannelVector.class);

    @PublicKey
    public static final Key<Integer> COLOR_CORRECTION_ABERRATION_MODE = new Key<>("android.colorCorrection.aberrationMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_AE_ANTIBANDING_MODE = new Key<>("android.control.aeAntibandingMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_AE_EXPOSURE_COMPENSATION = new Key<>("android.control.aeExposureCompensation", Integer.TYPE);

    @PublicKey
    public static final Key<Boolean> CONTROL_AE_LOCK = new Key<>("android.control.aeLock", Boolean.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_AE_MODE = new Key<>("android.control.aeMode", Integer.TYPE);

    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AE_REGIONS = new Key<>("android.control.aeRegions", MeteringRectangle[].class);

    @PublicKey
    public static final Key<Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key<>("android.control.aeTargetFpsRange", new TypeReference<Range<Integer>>() {
    });

    @PublicKey
    public static final Key<Integer> CONTROL_AE_PRECAPTURE_TRIGGER = new Key<>("android.control.aePrecaptureTrigger", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_AF_MODE = new Key<>("android.control.afMode", Integer.TYPE);

    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<>("android.control.afRegions", MeteringRectangle[].class);

    @PublicKey
    public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<>("android.control.afTrigger", Integer.TYPE);

    @PublicKey
    public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<>("android.control.awbLock", Boolean.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_AWB_MODE = new Key<>("android.control.awbMode", Integer.TYPE);

    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<>("android.control.awbRegions", MeteringRectangle[].class);

    @PublicKey
    public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<>("android.control.captureIntent", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<>("android.control.effectMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_MODE = new Key<>("android.control.mode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_SCENE_MODE = new Key<>("android.control.sceneMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key<>("android.control.videoStabilizationMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST = new Key<>("android.control.postRawSensitivityBoost", Integer.TYPE);

    @PublicKey
    public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<>("android.control.enableZsl", Boolean.TYPE);

    @PublicKey
    public static final Key<Integer> EDGE_MODE = new Key<>("android.edge.mode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> FLASH_MODE = new Key<>("android.flash.mode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> HOT_PIXEL_MODE = new Key<>("android.hotPixel.mode", Integer.TYPE);

    @SyntheticKey
    @PublicKey
    public static final Key<Location> JPEG_GPS_LOCATION = new Key<>("android.jpeg.gpsLocation", Location.class);
    public static final Key<double[]> JPEG_GPS_COORDINATES = new Key<>("android.jpeg.gpsCoordinates", double[].class);
    public static final Key<String> JPEG_GPS_PROCESSING_METHOD = new Key<>("android.jpeg.gpsProcessingMethod", String.class);
    public static final Key<Long> JPEG_GPS_TIMESTAMP = new Key<>("android.jpeg.gpsTimestamp", Long.TYPE);

    @PublicKey
    public static final Key<Integer> JPEG_ORIENTATION = new Key<>("android.jpeg.orientation", Integer.TYPE);

    @PublicKey
    public static final Key<Byte> JPEG_QUALITY = new Key<>("android.jpeg.quality", Byte.TYPE);

    @PublicKey
    public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key<>("android.jpeg.thumbnailQuality", Byte.TYPE);

    @PublicKey
    public static final Key<Size> JPEG_THUMBNAIL_SIZE = new Key<>("android.jpeg.thumbnailSize", Size.class);

    @PublicKey
    public static final Key<Float> LENS_APERTURE = new Key<>("android.lens.aperture", Float.TYPE);

    @PublicKey
    public static final Key<Float> LENS_FILTER_DENSITY = new Key<>("android.lens.filterDensity", Float.TYPE);

    @PublicKey
    public static final Key<Float> LENS_FOCAL_LENGTH = new Key<>("android.lens.focalLength", Float.TYPE);

    @PublicKey
    public static final Key<Float> LENS_FOCUS_DISTANCE = new Key<>("android.lens.focusDistance", Float.TYPE);

    @PublicKey
    public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<>("android.lens.opticalStabilizationMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<>("android.noiseReduction.mode", Integer.TYPE);
    public static final Key<Integer> REQUEST_ID = new Key<>(RestrictionsManager.REQUEST_KEY_ID, Integer.TYPE);

    @PublicKey
    public static final Key<Rect> SCALER_CROP_REGION = new Key<>("android.scaler.cropRegion", Rect.class);

    @PublicKey
    public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<>("android.sensor.exposureTime", Long.TYPE);

    @PublicKey
    public static final Key<Long> SENSOR_FRAME_DURATION = new Key<>("android.sensor.frameDuration", Long.TYPE);

    @PublicKey
    public static final Key<Integer> SENSOR_SENSITIVITY = new Key<>("android.sensor.sensitivity", Integer.TYPE);

    @PublicKey
    public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<>("android.sensor.testPatternData", int[].class);

    @PublicKey
    public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<>("android.sensor.testPatternMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> SHADING_MODE = new Key<>("android.shading.mode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<>("android.statistics.faceDetectMode", Integer.TYPE);

    @PublicKey
    public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<>("android.statistics.hotPixelMapMode", Boolean.TYPE);

    @PublicKey
    public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<>("android.statistics.lensShadingMapMode", Integer.TYPE);

    @PublicKey
    public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<>("android.statistics.oisDataMode", Integer.TYPE);
    public static final Key<float[]> TONEMAP_CURVE_BLUE = new Key<>("android.tonemap.curveBlue", float[].class);
    public static final Key<float[]> TONEMAP_CURVE_GREEN = new Key<>("android.tonemap.curveGreen", float[].class);
    public static final Key<float[]> TONEMAP_CURVE_RED = new Key<>("android.tonemap.curveRed", float[].class);

    @SyntheticKey
    @PublicKey
    public static final Key<TonemapCurve> TONEMAP_CURVE = new Key<>("android.tonemap.curve", TonemapCurve.class);

    @PublicKey
    public static final Key<Integer> TONEMAP_MODE = new Key<>("android.tonemap.mode", Integer.TYPE);

    @PublicKey
    public static final Key<Float> TONEMAP_GAMMA = new Key<>("android.tonemap.gamma", Float.TYPE);

    @PublicKey
    public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<>("android.tonemap.presetCurve", Integer.TYPE);
    public static final Key<Boolean> LED_TRANSMIT = new Key<>("android.led.transmit", Boolean.TYPE);

    @PublicKey
    public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key<>("android.blackLevel.lock", Boolean.TYPE);

    @PublicKey
    public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR = new Key<>("android.reprocess.effectiveExposureFactor", Float.TYPE);

    @PublicKey
    public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key<>("android.distortionCorrection.mode", Integer.TYPE);

    public static final class Key<T> {
        private final CameraMetadataNative.Key<T> mKey;

        public Key(String str, Class<T> cls, long j) {
            this.mKey = new CameraMetadataNative.Key<>(str, cls, j);
        }

        public Key(String str, Class<T> cls) {
            this.mKey = new CameraMetadataNative.Key<>(str, cls);
        }

        public Key(String str, TypeReference<T> typeReference) {
            this.mKey = new CameraMetadataNative.Key<>(str, typeReference);
        }

        public String getName() {
            return this.mKey.getName();
        }

        public long getVendorId() {
            return this.mKey.getVendorId();
        }

        public final int hashCode() {
            return this.mKey.hashCode();
        }

        public final boolean equals(Object obj) {
            return (obj instanceof Key) && ((Key) obj).mKey.equals(this.mKey);
        }

        public String toString() {
            return String.format("CaptureRequest.Key(%s)", this.mKey.getName());
        }

        public CameraMetadataNative.Key<T> getNativeKey() {
            return this.mKey;
        }

        Key(CameraMetadataNative.Key<?> key) {
            this.mKey = key;
        }
    }

    private CaptureRequest() {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet<>();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap<>();
        this.mIsPartOfCHSRequestList = false;
        this.mIsReprocess = false;
        this.mReprocessableSessionId = -1;
    }

    private CaptureRequest(CaptureRequest captureRequest) {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet<>();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap<>();
        this.mIsPartOfCHSRequestList = false;
        this.mLogicalCameraId = new String(captureRequest.mLogicalCameraId);
        for (Map.Entry<String, CameraMetadataNative> entry : captureRequest.mPhysicalCameraSettings.entrySet()) {
            this.mPhysicalCameraSettings.put(new String(entry.getKey()), new CameraMetadataNative(entry.getValue()));
        }
        this.mLogicalCameraSettings = this.mPhysicalCameraSettings.get(this.mLogicalCameraId);
        setNativeInstance(this.mLogicalCameraSettings);
        this.mSurfaceSet.addAll((ArraySet<? extends Surface>) captureRequest.mSurfaceSet);
        this.mIsReprocess = captureRequest.mIsReprocess;
        this.mIsPartOfCHSRequestList = captureRequest.mIsPartOfCHSRequestList;
        this.mReprocessableSessionId = captureRequest.mReprocessableSessionId;
        this.mUserTag = captureRequest.mUserTag;
    }

    private CaptureRequest(CameraMetadataNative cameraMetadataNative, boolean z, int i, String str, Set<String> set) {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet<>();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap<>();
        this.mIsPartOfCHSRequestList = false;
        if (set != null && z) {
            throw new IllegalArgumentException("Create a reprocess capture request with with more than one physical camera is not supported!");
        }
        this.mLogicalCameraId = str;
        this.mLogicalCameraSettings = CameraMetadataNative.move(cameraMetadataNative);
        this.mPhysicalCameraSettings.put(this.mLogicalCameraId, this.mLogicalCameraSettings);
        if (set != null) {
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                this.mPhysicalCameraSettings.put(it.next(), new CameraMetadataNative(this.mLogicalCameraSettings));
            }
        }
        setNativeInstance(this.mLogicalCameraSettings);
        this.mIsReprocess = z;
        if (z) {
            if (i == -1) {
                throw new IllegalArgumentException("Create a reprocess capture request with an invalid session ID: " + i);
            }
            this.mReprocessableSessionId = i;
            return;
        }
        this.mReprocessableSessionId = -1;
    }

    public <T> T get(Key<T> key) {
        return (T) this.mLogicalCameraSettings.get(key);
    }

    @Override
    protected <T> T getProtected(Key<?> key) {
        return (T) this.mLogicalCameraSettings.get(key);
    }

    @Override
    protected Class<Key<?>> getKeyClass() {
        return Key.class;
    }

    @Override
    public List<Key<?>> getKeys() {
        return super.getKeys();
    }

    public Object getTag() {
        return this.mUserTag;
    }

    public boolean isReprocess() {
        return this.mIsReprocess;
    }

    public boolean isPartOfCRequestList() {
        return this.mIsPartOfCHSRequestList;
    }

    public CameraMetadataNative getNativeCopy() {
        return new CameraMetadataNative(this.mLogicalCameraSettings);
    }

    public int getReprocessableSessionId() {
        if (!this.mIsReprocess || this.mReprocessableSessionId == -1) {
            throw new IllegalStateException("Getting the reprocessable session ID for a non-reprocess capture request is illegal.");
        }
        return this.mReprocessableSessionId;
    }

    public boolean equals(Object obj) {
        return (obj instanceof CaptureRequest) && equals((CaptureRequest) obj);
    }

    private boolean equals(CaptureRequest captureRequest) {
        return captureRequest != null && Objects.equals(this.mUserTag, captureRequest.mUserTag) && this.mSurfaceSet.equals(captureRequest.mSurfaceSet) && this.mPhysicalCameraSettings.equals(captureRequest.mPhysicalCameraSettings) && this.mLogicalCameraId.equals(captureRequest.mLogicalCameraId) && this.mLogicalCameraSettings.equals(captureRequest.mLogicalCameraSettings) && this.mIsReprocess == captureRequest.mIsReprocess && this.mReprocessableSessionId == captureRequest.mReprocessableSessionId;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCodeGeneric(this.mPhysicalCameraSettings, this.mSurfaceSet, this.mUserTag);
    }

    private void readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        if (i <= 0) {
            throw new RuntimeException("Physical camera count" + i + " should always be positive");
        }
        this.mLogicalCameraId = parcel.readString();
        this.mLogicalCameraSettings = new CameraMetadataNative();
        this.mLogicalCameraSettings.readFromParcel(parcel);
        setNativeInstance(this.mLogicalCameraSettings);
        this.mPhysicalCameraSettings.put(this.mLogicalCameraId, this.mLogicalCameraSettings);
        for (int i2 = 1; i2 < i; i2++) {
            String string = parcel.readString();
            CameraMetadataNative cameraMetadataNative = new CameraMetadataNative();
            cameraMetadataNative.readFromParcel(parcel);
            this.mPhysicalCameraSettings.put(string, cameraMetadataNative);
        }
        this.mIsReprocess = parcel.readInt() != 0;
        this.mReprocessableSessionId = -1;
        synchronized (this.mSurfacesLock) {
            this.mSurfaceSet.clear();
            Parcelable[] parcelableArray = parcel.readParcelableArray(Surface.class.getClassLoader());
            if (parcelableArray != null) {
                for (Parcelable parcelable : parcelableArray) {
                    this.mSurfaceSet.add((Surface) parcelable);
                }
            }
            if (parcel.readInt() != 0) {
                throw new RuntimeException("Reading cached CaptureRequest is not supported");
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPhysicalCameraSettings.size());
        parcel.writeString(this.mLogicalCameraId);
        this.mLogicalCameraSettings.writeToParcel(parcel, i);
        for (Map.Entry<String, CameraMetadataNative> entry : this.mPhysicalCameraSettings.entrySet()) {
            if (!entry.getKey().equals(this.mLogicalCameraId)) {
                parcel.writeString(entry.getKey());
                entry.getValue().writeToParcel(parcel, i);
            }
        }
        parcel.writeInt(this.mIsReprocess ? 1 : 0);
        synchronized (this.mSurfacesLock) {
            ArraySet<Surface> arraySet = this.mSurfaceConverted ? mEmptySurfaceSet : this.mSurfaceSet;
            parcel.writeParcelableArray((Surface[]) arraySet.toArray(new Surface[arraySet.size()]), i);
            if (this.mSurfaceConverted) {
                parcel.writeInt(this.mStreamIdxArray.length);
                for (int i2 = 0; i2 < this.mStreamIdxArray.length; i2++) {
                    parcel.writeInt(this.mStreamIdxArray[i2]);
                    parcel.writeInt(this.mSurfaceIdxArray[i2]);
                }
            } else {
                parcel.writeInt(0);
            }
        }
    }

    public boolean containsTarget(Surface surface) {
        return this.mSurfaceSet.contains(surface);
    }

    public Collection<Surface> getTargets() {
        return Collections.unmodifiableCollection(this.mSurfaceSet);
    }

    public String getLogicalCameraId() {
        return this.mLogicalCameraId;
    }

    public void convertSurfaceToStreamId(SparseArray<OutputConfiguration> sparseArray) {
        boolean z;
        synchronized (this.mSurfacesLock) {
            if (this.mSurfaceConverted) {
                Log.v("CaptureRequest-JV", "Cannot convert already converted surfaces!");
                return;
            }
            this.mStreamIdxArray = new int[this.mSurfaceSet.size()];
            this.mSurfaceIdxArray = new int[this.mSurfaceSet.size()];
            int i = 0;
            for (Surface surface : this.mSurfaceSet) {
                boolean z2 = false;
                int i2 = i;
                for (int i3 = 0; i3 < sparseArray.size(); i3++) {
                    int iKeyAt = sparseArray.keyAt(i3);
                    Iterator<Surface> it = sparseArray.valueAt(i3).getSurfaces().iterator();
                    int i4 = 0;
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        if (surface == it.next()) {
                            this.mStreamIdxArray[i2] = iKeyAt;
                            this.mSurfaceIdxArray[i2] = i4;
                            i2++;
                            z2 = true;
                            break;
                        }
                        i4++;
                    }
                    if (z2) {
                        break;
                    }
                }
                if (!z2) {
                    long surfaceId = SurfaceUtils.getSurfaceId(surface);
                    z = z2;
                    for (int i5 = 0; i5 < sparseArray.size(); i5++) {
                        int iKeyAt2 = sparseArray.keyAt(i5);
                        Iterator<Surface> it2 = sparseArray.valueAt(i5).getSurfaces().iterator();
                        int i6 = 0;
                        while (true) {
                            if (!it2.hasNext()) {
                                break;
                            }
                            if (surfaceId == SurfaceUtils.getSurfaceId(it2.next())) {
                                this.mStreamIdxArray[i2] = iKeyAt2;
                                this.mSurfaceIdxArray[i2] = i6;
                                i2++;
                                z = true;
                                break;
                            }
                            i6++;
                        }
                        if (z) {
                            break;
                        }
                    }
                } else {
                    z = z2;
                }
                i = i2;
                if (!z) {
                    this.mStreamIdxArray = null;
                    this.mSurfaceIdxArray = null;
                    throw new IllegalArgumentException("CaptureRequest contains unconfigured Input/Output Surface!");
                }
            }
            this.mSurfaceConverted = true;
        }
    }

    public void recoverStreamIdToSurface() {
        synchronized (this.mSurfacesLock) {
            if (!this.mSurfaceConverted) {
                Log.v("CaptureRequest-JV", "Cannot convert already converted surfaces!");
                return;
            }
            this.mStreamIdxArray = null;
            this.mSurfaceIdxArray = null;
            this.mSurfaceConverted = false;
        }
    }

    public static final class Builder {
        private final CaptureRequest mRequest;

        public Builder(CameraMetadataNative cameraMetadataNative, boolean z, int i, String str, Set<String> set) {
            this.mRequest = new CaptureRequest(cameraMetadataNative, z, i, str, set);
        }

        public void addTarget(Surface surface) {
            this.mRequest.mSurfaceSet.add(surface);
        }

        public void removeTarget(Surface surface) {
            this.mRequest.mSurfaceSet.remove(surface);
        }

        public <T> void set(Key<T> key, T t) {
            this.mRequest.mLogicalCameraSettings.set(key, t);
        }

        public <T> T get(Key<T> key) {
            return (T) this.mRequest.mLogicalCameraSettings.get(key);
        }

        public <T> Builder setPhysicalCameraKey(Key<T> key, T t, String str) {
            if (this.mRequest.mPhysicalCameraSettings.containsKey(str)) {
                ((CameraMetadataNative) this.mRequest.mPhysicalCameraSettings.get(str)).set(key, t);
                return this;
            }
            throw new IllegalArgumentException("Physical camera id: " + str + " is not valid!");
        }

        public <T> T getPhysicalCameraKey(Key<T> key, String str) {
            if (this.mRequest.mPhysicalCameraSettings.containsKey(str)) {
                return (T) ((CameraMetadataNative) this.mRequest.mPhysicalCameraSettings.get(str)).get(key);
            }
            throw new IllegalArgumentException("Physical camera id: " + str + " is not valid!");
        }

        public void setTag(Object obj) {
            this.mRequest.mUserTag = obj;
        }

        public void setPartOfCHSRequestList(boolean z) {
            this.mRequest.mIsPartOfCHSRequestList = z;
        }

        public CaptureRequest build() {
            return new CaptureRequest();
        }

        public boolean isEmpty() {
            return this.mRequest.mLogicalCameraSettings.isEmpty();
        }
    }
}
