package android.hardware.camera2.impl;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.MarshalRegistry;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.marshal.impl.MarshalQueryableArray;
import android.hardware.camera2.marshal.impl.MarshalQueryableBlackLevelPattern;
import android.hardware.camera2.marshal.impl.MarshalQueryableBoolean;
import android.hardware.camera2.marshal.impl.MarshalQueryableColorSpaceTransform;
import android.hardware.camera2.marshal.impl.MarshalQueryableEnum;
import android.hardware.camera2.marshal.impl.MarshalQueryableHighSpeedVideoConfiguration;
import android.hardware.camera2.marshal.impl.MarshalQueryableMeteringRectangle;
import android.hardware.camera2.marshal.impl.MarshalQueryableNativeByteToInteger;
import android.hardware.camera2.marshal.impl.MarshalQueryablePair;
import android.hardware.camera2.marshal.impl.MarshalQueryableParcelable;
import android.hardware.camera2.marshal.impl.MarshalQueryablePrimitive;
import android.hardware.camera2.marshal.impl.MarshalQueryableRange;
import android.hardware.camera2.marshal.impl.MarshalQueryableRect;
import android.hardware.camera2.marshal.impl.MarshalQueryableReprocessFormatsMap;
import android.hardware.camera2.marshal.impl.MarshalQueryableRggbChannelVector;
import android.hardware.camera2.marshal.impl.MarshalQueryableSize;
import android.hardware.camera2.marshal.impl.MarshalQueryableSizeF;
import android.hardware.camera2.marshal.impl.MarshalQueryableStreamConfiguration;
import android.hardware.camera2.marshal.impl.MarshalQueryableStreamConfigurationDuration;
import android.hardware.camera2.marshal.impl.MarshalQueryableString;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.HighSpeedVideoConfiguration;
import android.hardware.camera2.params.LensShadingMap;
import android.hardware.camera2.params.OisSample;
import android.hardware.camera2.params.ReprocessFormatsMap;
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.params.StreamConfigurationDuration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.hardware.camera2.utils.TypeReference;
import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Size;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class CameraMetadataNative implements Parcelable {
    private static final String CELLID_PROCESS = "CELLID";
    private static final boolean DEBUG = false;
    private static final int FACE_LANDMARK_SIZE = 6;
    private static final String GPS_PROCESS = "GPS";
    public static final int NATIVE_JPEG_FORMAT = 33;
    public static final int NUM_TYPES = 6;
    private static final String TAG = "CameraMetadataJV";
    public static final int TYPE_BYTE = 0;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_INT32 = 1;
    public static final int TYPE_INT64 = 3;
    public static final int TYPE_RATIONAL = 5;
    private static final HashMap<Key<?>, SetCommand> sSetCommandMap;
    private long mMetadataPtr;
    public static final Parcelable.Creator<CameraMetadataNative> CREATOR = new Parcelable.Creator<CameraMetadataNative>() {
        @Override
        public CameraMetadataNative createFromParcel(Parcel parcel) {
            CameraMetadataNative cameraMetadataNative = new CameraMetadataNative();
            cameraMetadataNative.readFromParcel(parcel);
            return cameraMetadataNative;
        }

        @Override
        public CameraMetadataNative[] newArray(int i) {
            return new CameraMetadataNative[i];
        }
    };
    private static final HashMap<Key<?>, GetCommand> sGetCommandMap = new HashMap<>();

    private native long nativeAllocate();

    private native long nativeAllocateCopy(CameraMetadataNative cameraMetadataNative) throws NullPointerException;

    private native synchronized void nativeClose();

    private native synchronized void nativeDump() throws IOException;

    private native synchronized ArrayList nativeGetAllVendorKeys(Class cls);

    private native synchronized int nativeGetEntryCount();

    private static native int nativeGetTagFromKey(String str, long j) throws IllegalArgumentException;

    private native synchronized int nativeGetTagFromKeyLocal(String str) throws IllegalArgumentException;

    private static native int nativeGetTypeFromTag(int i, long j) throws IllegalArgumentException;

    private native synchronized int nativeGetTypeFromTagLocal(int i) throws IllegalArgumentException;

    private native synchronized boolean nativeIsEmpty();

    private native synchronized void nativeReadFromParcel(Parcel parcel);

    private native synchronized byte[] nativeReadValues(int i);

    private static native int nativeSetupGlobalVendorTagDescriptor();

    private native synchronized void nativeSwap(CameraMetadataNative cameraMetadataNative) throws NullPointerException;

    private native synchronized void nativeWriteToParcel(Parcel parcel);

    private native synchronized void nativeWriteValues(int i, byte[] bArr);

    public static class Key<T> {
        private final String mFallbackName;
        private boolean mHasTag;
        private final int mHash;
        private final String mName;
        private int mTag;
        private final Class<T> mType;
        private final TypeReference<T> mTypeReference;
        private long mVendorId;

        public Key(String str, Class<T> cls, long j) {
            this.mVendorId = Long.MAX_VALUE;
            if (str == null) {
                throw new NullPointerException("Key needs a valid name");
            }
            if (cls == null) {
                throw new NullPointerException("Type needs to be non-null");
            }
            this.mName = str;
            this.mFallbackName = null;
            this.mType = cls;
            this.mVendorId = j;
            this.mTypeReference = TypeReference.createSpecializedTypeReference((Class) cls);
            this.mHash = this.mName.hashCode() ^ this.mTypeReference.hashCode();
        }

        public Key(String str, String str2, Class<T> cls) {
            this.mVendorId = Long.MAX_VALUE;
            if (str == null) {
                throw new NullPointerException("Key needs a valid name");
            }
            if (cls == null) {
                throw new NullPointerException("Type needs to be non-null");
            }
            this.mName = str;
            this.mFallbackName = str2;
            this.mType = cls;
            this.mTypeReference = TypeReference.createSpecializedTypeReference((Class) cls);
            this.mHash = this.mName.hashCode() ^ this.mTypeReference.hashCode();
        }

        public Key(String str, Class<T> cls) {
            this.mVendorId = Long.MAX_VALUE;
            if (str == null) {
                throw new NullPointerException("Key needs a valid name");
            }
            if (cls == null) {
                throw new NullPointerException("Type needs to be non-null");
            }
            this.mName = str;
            this.mFallbackName = null;
            this.mType = cls;
            this.mTypeReference = TypeReference.createSpecializedTypeReference((Class) cls);
            this.mHash = this.mName.hashCode() ^ this.mTypeReference.hashCode();
        }

        public Key(String str, TypeReference<T> typeReference) {
            this.mVendorId = Long.MAX_VALUE;
            if (str == null) {
                throw new NullPointerException("Key needs a valid name");
            }
            if (typeReference == null) {
                throw new NullPointerException("TypeReference needs to be non-null");
            }
            this.mName = str;
            this.mFallbackName = null;
            this.mType = typeReference.getRawType();
            this.mTypeReference = typeReference;
            this.mHash = this.mName.hashCode() ^ this.mTypeReference.hashCode();
        }

        public final String getName() {
            return this.mName;
        }

        public final int hashCode() {
            return this.mHash;
        }

        public final boolean equals(Object obj) {
            Key<T> nativeKey;
            if (this == obj) {
                return true;
            }
            if (obj == null || hashCode() != obj.hashCode()) {
                return false;
            }
            if (obj instanceof CaptureResult.Key) {
                nativeKey = ((CaptureResult.Key) obj).getNativeKey();
            } else if (obj instanceof CaptureRequest.Key) {
                nativeKey = ((CaptureRequest.Key) obj).getNativeKey();
            } else if (obj instanceof CameraCharacteristics.Key) {
                nativeKey = ((CameraCharacteristics.Key) obj).getNativeKey();
            } else {
                if (!(obj instanceof Key)) {
                    return false;
                }
                nativeKey = (Key) obj;
            }
            if (this.mName.equals(nativeKey.mName) && this.mTypeReference.equals(nativeKey.mTypeReference)) {
                return true;
            }
            return false;
        }

        public final int getTag() {
            if (!this.mHasTag) {
                this.mTag = CameraMetadataNative.getTag(this.mName, this.mVendorId);
                this.mHasTag = true;
            }
            return this.mTag;
        }

        public final Class<T> getType() {
            return this.mType;
        }

        public final long getVendorId() {
            return this.mVendorId;
        }

        public final TypeReference<T> getTypeReference() {
            return this.mTypeReference;
        }
    }

    private static String translateLocationProviderToProcess(String str) {
        if (str == null) {
            return null;
        }
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != 102570) {
            if (iHashCode == 1843485230 && str.equals(LocationManager.NETWORK_PROVIDER)) {
                b = 1;
            }
        } else if (str.equals(LocationManager.GPS_PROVIDER)) {
            b = 0;
        }
        switch (b) {
        }
        return null;
    }

    private static String translateProcessToLocationProvider(String str) {
        if (str == null) {
            return null;
        }
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != 70794) {
            if (iHashCode == 1984215549 && str.equals(CELLID_PROCESS)) {
                b = 1;
            }
        } else if (str.equals(GPS_PROCESS)) {
            b = 0;
        }
        switch (b) {
        }
        return null;
    }

    public CameraMetadataNative() {
        this.mMetadataPtr = nativeAllocate();
        if (this.mMetadataPtr == 0) {
            throw new OutOfMemoryError("Failed to allocate native CameraMetadata");
        }
    }

    public CameraMetadataNative(CameraMetadataNative cameraMetadataNative) {
        this.mMetadataPtr = nativeAllocateCopy(cameraMetadataNative);
        if (this.mMetadataPtr == 0) {
            throw new OutOfMemoryError("Failed to allocate native CameraMetadata");
        }
    }

    public static CameraMetadataNative move(CameraMetadataNative cameraMetadataNative) {
        CameraMetadataNative cameraMetadataNative2 = new CameraMetadataNative();
        cameraMetadataNative2.swap(cameraMetadataNative);
        return cameraMetadataNative2;
    }

    static {
        sGetCommandMap.put(CameraCharacteristics.SCALER_AVAILABLE_FORMATS.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getAvailableFormats();
            }
        });
        sGetCommandMap.put(CaptureResult.STATISTICS_FACES.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getFaces();
            }
        });
        sGetCommandMap.put(CaptureResult.STATISTICS_FACE_RECTANGLES.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getFaceRectangles();
            }
        });
        sGetCommandMap.put(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getStreamConfigurationMap();
            }
        });
        sGetCommandMap.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AE.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxRegions(key);
            }
        });
        sGetCommandMap.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxRegions(key);
            }
        });
        sGetCommandMap.put(CameraCharacteristics.CONTROL_MAX_REGIONS_AF.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxRegions(key);
            }
        });
        sGetCommandMap.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxNumOutputs(key);
            }
        });
        sGetCommandMap.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxNumOutputs(key);
            }
        });
        sGetCommandMap.put(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getMaxNumOutputs(key);
            }
        });
        sGetCommandMap.put(CaptureRequest.TONEMAP_CURVE.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getTonemapCurve();
            }
        });
        sGetCommandMap.put(CaptureResult.JPEG_GPS_LOCATION.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getGpsLocation();
            }
        });
        sGetCommandMap.put(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getLensShadingMap();
            }
        });
        sGetCommandMap.put(CaptureResult.STATISTICS_OIS_SAMPLES.getNativeKey(), new GetCommand() {
            @Override
            public <T> T getValue(CameraMetadataNative cameraMetadataNative, Key<T> key) {
                return (T) cameraMetadataNative.getOisSamples();
            }
        });
        sSetCommandMap = new HashMap<>();
        sSetCommandMap.put(CameraCharacteristics.SCALER_AVAILABLE_FORMATS.getNativeKey(), new SetCommand() {
            @Override
            public <T> void setValue(CameraMetadataNative cameraMetadataNative, T t) {
                cameraMetadataNative.setAvailableFormats((int[]) t);
            }
        });
        sSetCommandMap.put(CaptureResult.STATISTICS_FACE_RECTANGLES.getNativeKey(), new SetCommand() {
            @Override
            public <T> void setValue(CameraMetadataNative cameraMetadataNative, T t) {
                cameraMetadataNative.setFaceRectangles((Rect[]) t);
            }
        });
        sSetCommandMap.put(CaptureResult.STATISTICS_FACES.getNativeKey(), new SetCommand() {
            @Override
            public <T> void setValue(CameraMetadataNative cameraMetadataNative, T t) {
                cameraMetadataNative.setFaces((Face[]) t);
            }
        });
        sSetCommandMap.put(CaptureRequest.TONEMAP_CURVE.getNativeKey(), new SetCommand() {
            @Override
            public <T> void setValue(CameraMetadataNative cameraMetadataNative, T t) {
                cameraMetadataNative.setTonemapCurve((TonemapCurve) t);
            }
        });
        sSetCommandMap.put(CaptureResult.JPEG_GPS_LOCATION.getNativeKey(), new SetCommand() {
            @Override
            public <T> void setValue(CameraMetadataNative cameraMetadataNative, T t) {
                cameraMetadataNative.setGpsLocation((Location) t);
            }
        });
        registerAllMarshalers();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        nativeWriteToParcel(parcel);
    }

    public <T> T get(CameraCharacteristics.Key<T> key) {
        return (T) get(key.getNativeKey());
    }

    public <T> T get(CaptureResult.Key<T> key) {
        return (T) get(key.getNativeKey());
    }

    public <T> T get(CaptureRequest.Key<T> key) {
        return (T) get(key.getNativeKey());
    }

    public <T> T get(Key<T> key) {
        Preconditions.checkNotNull(key, "key must not be null");
        GetCommand getCommand = sGetCommandMap.get(key);
        if (getCommand != null) {
            return (T) getCommand.getValue(this, key);
        }
        return (T) getBase(key);
    }

    public void readFromParcel(Parcel parcel) {
        nativeReadFromParcel(parcel);
    }

    public static void setupGlobalVendorTagDescriptor() throws ServiceSpecificException {
        int iNativeSetupGlobalVendorTagDescriptor = nativeSetupGlobalVendorTagDescriptor();
        if (iNativeSetupGlobalVendorTagDescriptor != 0) {
            throw new ServiceSpecificException(iNativeSetupGlobalVendorTagDescriptor, "Failure to set up global vendor tags");
        }
    }

    public <T> void set(Key<T> key, T t) {
        SetCommand setCommand = sSetCommandMap.get(key);
        if (setCommand != null) {
            setCommand.setValue(this, t);
        } else {
            setBase(key, t);
        }
    }

    public <T> void set(CaptureRequest.Key<T> key, T t) {
        set(key.getNativeKey(), t);
    }

    public <T> void set(CaptureResult.Key<T> key, T t) {
        set(key.getNativeKey(), t);
    }

    public <T> void set(CameraCharacteristics.Key<T> key, T t) {
        set(key.getNativeKey(), t);
    }

    private void close() {
        nativeClose();
        this.mMetadataPtr = 0L;
    }

    private <T> T getBase(CameraCharacteristics.Key<T> key) {
        return (T) getBase(key.getNativeKey());
    }

    private <T> T getBase(CaptureResult.Key<T> key) {
        return (T) getBase(key.getNativeKey());
    }

    private <T> T getBase(CaptureRequest.Key<T> key) {
        return (T) getBase(key.getNativeKey());
    }

    private <T> T getBase(Key<T> key) {
        byte[] values;
        int iNativeGetTagFromKeyLocal = nativeGetTagFromKeyLocal(key.getName());
        byte[] values2 = readValues(iNativeGetTagFromKeyLocal);
        if (values2 == null) {
            if (((Key) key).mFallbackName == null || (values = readValues((iNativeGetTagFromKeyLocal = nativeGetTagFromKeyLocal(((Key) key).mFallbackName)))) == null) {
                return null;
            }
            values2 = values;
        }
        return (T) getMarshalerForKey(key, nativeGetTypeFromTagLocal(iNativeGetTagFromKeyLocal)).unmarshal(ByteBuffer.wrap(values2).order(ByteOrder.nativeOrder()));
    }

    private int[] getAvailableFormats() {
        int[] iArr = (int[]) getBase(CameraCharacteristics.SCALER_AVAILABLE_FORMATS);
        if (iArr != null) {
            for (int i = 0; i < iArr.length; i++) {
                if (iArr[i] == 33) {
                    iArr[i] = 256;
                }
            }
        }
        return iArr;
    }

    private boolean setFaces(Face[] faceArr) {
        int[] iArr;
        if (faceArr == null) {
            return false;
        }
        int length = faceArr.length;
        boolean z = true;
        for (Face face : faceArr) {
            if (face == null) {
                length--;
                Log.w(TAG, "setFaces - null face detected, skipping");
            } else if (face.getId() == -1) {
                z = false;
            }
        }
        Rect[] rectArr = new Rect[length];
        byte[] bArr = new byte[length];
        int[] iArr2 = null;
        if (z) {
            iArr2 = new int[length];
            iArr = new int[length * 6];
        } else {
            iArr = null;
        }
        int i = 0;
        for (Face face2 : faceArr) {
            if (face2 != null) {
                rectArr[i] = face2.getBounds();
                bArr[i] = (byte) face2.getScore();
                if (z) {
                    iArr2[i] = face2.getId();
                    int i2 = i * 6;
                    iArr[i2 + 0] = face2.getLeftEyePosition().x;
                    iArr[i2 + 1] = face2.getLeftEyePosition().y;
                    iArr[i2 + 2] = face2.getRightEyePosition().x;
                    iArr[i2 + 3] = face2.getRightEyePosition().y;
                    iArr[i2 + 4] = face2.getMouthPosition().x;
                    iArr[i2 + 5] = face2.getMouthPosition().y;
                }
                i++;
            }
        }
        set(CaptureResult.STATISTICS_FACE_RECTANGLES, rectArr);
        set(CaptureResult.STATISTICS_FACE_IDS, iArr2);
        set(CaptureResult.STATISTICS_FACE_LANDMARKS, iArr);
        set(CaptureResult.STATISTICS_FACE_SCORES, bArr);
        return true;
    }

    private Face[] getFaces() {
        Integer num = (Integer) get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
        byte[] bArr = (byte[]) get(CaptureResult.STATISTICS_FACE_SCORES);
        Rect[] rectArr = (Rect[]) get(CaptureResult.STATISTICS_FACE_RECTANGLES);
        int[] iArr = (int[]) get(CaptureResult.STATISTICS_FACE_IDS);
        int[] iArr2 = (int[]) get(CaptureResult.STATISTICS_FACE_LANDMARKS);
        int i = 0;
        if (areValuesAllNull(num, bArr, rectArr, iArr, iArr2)) {
            return null;
        }
        if (num == null) {
            Log.w(TAG, "Face detect mode metadata is null, assuming the mode is SIMPLE");
            num = 1;
        } else {
            if (num.intValue() == 0) {
                return new Face[0];
            }
            if (num.intValue() != 1 && num.intValue() != 2) {
                Log.w(TAG, "Unknown face detect mode: " + num);
                return new Face[0];
            }
        }
        if (bArr == null || rectArr == null) {
            Log.w(TAG, "Expect face scores and rectangles to be non-null");
            return new Face[0];
        }
        if (bArr.length != rectArr.length) {
            Log.w(TAG, String.format("Face score size(%d) doesn match face rectangle size(%d)!", Integer.valueOf(bArr.length), Integer.valueOf(rectArr.length)));
        }
        int iMin = Math.min(bArr.length, rectArr.length);
        if (num.intValue() == 2) {
            if (iArr == null || iArr2 == null) {
                Log.w(TAG, "Expect face ids and landmarks to be non-null for FULL mode,fallback to SIMPLE mode");
                num = 1;
            } else {
                if (iArr.length != iMin || iArr2.length != iMin * 6) {
                    Log.w(TAG, String.format("Face id size(%d), or face landmark size(%d) don'tmatch face number(%d)!", Integer.valueOf(iArr.length), Integer.valueOf(iArr2.length * 6), Integer.valueOf(iMin)));
                }
                iMin = Math.min(Math.min(iMin, iArr.length), iArr2.length / 6);
            }
        }
        ArrayList arrayList = new ArrayList();
        if (num.intValue() == 1) {
            while (i < iMin) {
                if (bArr[i] <= 100 && bArr[i] >= 1) {
                    arrayList.add(new Face(rectArr[i], bArr[i]));
                }
                i++;
            }
        } else {
            while (i < iMin) {
                if (bArr[i] <= 100 && bArr[i] >= 1 && iArr[i] >= 0) {
                    int i2 = i * 6;
                    arrayList.add(new Face(rectArr[i], bArr[i], iArr[i], new Point(iArr2[i2], iArr2[i2 + 1]), new Point(iArr2[i2 + 2], iArr2[i2 + 3]), new Point(iArr2[i2 + 4], iArr2[i2 + 5])));
                }
                i++;
            }
        }
        Face[] faceArr = new Face[arrayList.size()];
        arrayList.toArray(faceArr);
        return faceArr;
    }

    private Rect[] getFaceRectangles() {
        Rect[] rectArr = (Rect[]) getBase(CaptureResult.STATISTICS_FACE_RECTANGLES);
        if (rectArr == null) {
            return null;
        }
        Rect[] rectArr2 = new Rect[rectArr.length];
        for (int i = 0; i < rectArr.length; i++) {
            rectArr2[i] = new Rect(rectArr[i].left, rectArr[i].top, rectArr[i].right - rectArr[i].left, rectArr[i].bottom - rectArr[i].top);
        }
        return rectArr2;
    }

    private LensShadingMap getLensShadingMap() {
        float[] fArr = (float[]) getBase(CaptureResult.STATISTICS_LENS_SHADING_MAP);
        Size size = (Size) get(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE);
        if (fArr == null) {
            return null;
        }
        if (size == null) {
            Log.w(TAG, "getLensShadingMap - Lens shading map size was null.");
            return null;
        }
        return new LensShadingMap(fArr, size.getHeight(), size.getWidth());
    }

    private Location getGpsLocation() {
        String str = (String) get(CaptureResult.JPEG_GPS_PROCESSING_METHOD);
        double[] dArr = (double[]) get(CaptureResult.JPEG_GPS_COORDINATES);
        Long l = (Long) get(CaptureResult.JPEG_GPS_TIMESTAMP);
        if (areValuesAllNull(str, dArr, l)) {
            return null;
        }
        Location location = new Location(translateProcessToLocationProvider(str));
        if (l != null) {
            location.setTime(l.longValue() * 1000);
        } else {
            Log.w(TAG, "getGpsLocation - No timestamp for GPS location.");
        }
        if (dArr != null) {
            location.setLatitude(dArr[0]);
            location.setLongitude(dArr[1]);
            location.setAltitude(dArr[2]);
        } else {
            Log.w(TAG, "getGpsLocation - No coordinates for GPS location");
        }
        return location;
    }

    private boolean setGpsLocation(Location location) {
        if (location == null) {
            return false;
        }
        double[] dArr = {location.getLatitude(), location.getLongitude(), location.getAltitude()};
        String strTranslateLocationProviderToProcess = translateLocationProviderToProcess(location.getProvider());
        set(CaptureRequest.JPEG_GPS_TIMESTAMP, Long.valueOf(location.getTime() / 1000));
        set(CaptureRequest.JPEG_GPS_COORDINATES, dArr);
        if (strTranslateLocationProviderToProcess == null) {
            Log.w(TAG, "setGpsLocation - No process method, Location is not from a GPS or NETWORKprovider");
        } else {
            setBase(CaptureRequest.JPEG_GPS_PROCESSING_METHOD, strTranslateLocationProviderToProcess);
        }
        return true;
    }

    private StreamConfigurationMap getStreamConfigurationMap() {
        StreamConfiguration[] streamConfigurationArr = (StreamConfiguration[]) getBase(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS);
        StreamConfigurationDuration[] streamConfigurationDurationArr = (StreamConfigurationDuration[]) getBase(CameraCharacteristics.SCALER_AVAILABLE_MIN_FRAME_DURATIONS);
        StreamConfigurationDuration[] streamConfigurationDurationArr2 = (StreamConfigurationDuration[]) getBase(CameraCharacteristics.SCALER_AVAILABLE_STALL_DURATIONS);
        StreamConfiguration[] streamConfigurationArr2 = (StreamConfiguration[]) getBase(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_STREAM_CONFIGURATIONS);
        StreamConfigurationDuration[] streamConfigurationDurationArr3 = (StreamConfigurationDuration[]) getBase(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_MIN_FRAME_DURATIONS);
        StreamConfigurationDuration[] streamConfigurationDurationArr4 = (StreamConfigurationDuration[]) getBase(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_STALL_DURATIONS);
        HighSpeedVideoConfiguration[] highSpeedVideoConfigurationArr = (HighSpeedVideoConfiguration[]) getBase(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS);
        ReprocessFormatsMap reprocessFormatsMap = (ReprocessFormatsMap) getBase(CameraCharacteristics.SCALER_AVAILABLE_INPUT_OUTPUT_FORMATS_MAP);
        int[] iArr = (int[]) getBase(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        int length = iArr.length;
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (iArr[i] != 6) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        return new StreamConfigurationMap(streamConfigurationArr, streamConfigurationDurationArr, streamConfigurationDurationArr2, streamConfigurationArr2, streamConfigurationDurationArr3, streamConfigurationDurationArr4, highSpeedVideoConfigurationArr, reprocessFormatsMap, z);
    }

    private <T> Integer getMaxRegions(Key<T> key) {
        int[] iArr = (int[]) getBase(CameraCharacteristics.CONTROL_MAX_REGIONS);
        if (iArr == null) {
            return null;
        }
        if (key.equals(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)) {
            return Integer.valueOf(iArr[0]);
        }
        if (key.equals(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)) {
            return Integer.valueOf(iArr[1]);
        }
        if (key.equals(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)) {
            return Integer.valueOf(iArr[2]);
        }
        throw new AssertionError("Invalid key " + key);
    }

    private <T> Integer getMaxNumOutputs(Key<T> key) {
        int[] iArr = (int[]) getBase(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS);
        if (iArr == null) {
            return null;
        }
        if (key.equals(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW)) {
            return Integer.valueOf(iArr[0]);
        }
        if (key.equals(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC)) {
            return Integer.valueOf(iArr[1]);
        }
        if (key.equals(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING)) {
            return Integer.valueOf(iArr[2]);
        }
        throw new AssertionError("Invalid key " + key);
    }

    private <T> TonemapCurve getTonemapCurve() {
        float[] fArr = (float[]) getBase(CaptureRequest.TONEMAP_CURVE_RED);
        float[] fArr2 = (float[]) getBase(CaptureRequest.TONEMAP_CURVE_GREEN);
        float[] fArr3 = (float[]) getBase(CaptureRequest.TONEMAP_CURVE_BLUE);
        if (areValuesAllNull(fArr, fArr2, fArr3)) {
            return null;
        }
        if (fArr == null || fArr2 == null || fArr3 == null) {
            Log.w(TAG, "getTonemapCurve - missing tone curve components");
            return null;
        }
        return new TonemapCurve(fArr, fArr2, fArr3);
    }

    private OisSample[] getOisSamples() {
        long[] jArr = (long[]) getBase(CaptureResult.STATISTICS_OIS_TIMESTAMPS);
        float[] fArr = (float[]) getBase(CaptureResult.STATISTICS_OIS_X_SHIFTS);
        float[] fArr2 = (float[]) getBase(CaptureResult.STATISTICS_OIS_Y_SHIFTS);
        if (jArr == null) {
            if (fArr != null) {
                throw new AssertionError("timestamps is null but xShifts is not");
            }
            if (fArr2 != null) {
                throw new AssertionError("timestamps is null but yShifts is not");
            }
            return null;
        }
        if (fArr == null) {
            throw new AssertionError("timestamps is not null but xShifts is");
        }
        if (fArr2 == null) {
            throw new AssertionError("timestamps is not null but yShifts is");
        }
        if (fArr.length != jArr.length) {
            throw new AssertionError(String.format("timestamps has %d entries but xShifts has %d", Integer.valueOf(jArr.length), Integer.valueOf(fArr.length)));
        }
        if (fArr2.length != jArr.length) {
            throw new AssertionError(String.format("timestamps has %d entries but yShifts has %d", Integer.valueOf(jArr.length), Integer.valueOf(fArr2.length)));
        }
        OisSample[] oisSampleArr = new OisSample[jArr.length];
        for (int i = 0; i < jArr.length; i++) {
            oisSampleArr[i] = new OisSample(jArr[i], fArr[i], fArr2[i]);
        }
        return oisSampleArr;
    }

    private <T> void setBase(CameraCharacteristics.Key<T> key, T t) {
        setBase(key.getNativeKey(), t);
    }

    private <T> void setBase(CaptureResult.Key<T> key, T t) {
        setBase(key.getNativeKey(), t);
    }

    private <T> void setBase(CaptureRequest.Key<T> key, T t) {
        setBase(key.getNativeKey(), t);
    }

    private <T> void setBase(Key<T> key, T t) {
        int iNativeGetTagFromKeyLocal = nativeGetTagFromKeyLocal(key.getName());
        if (t == null) {
            writeValues(iNativeGetTagFromKeyLocal, null);
            return;
        }
        Marshaler marshalerForKey = getMarshalerForKey(key, nativeGetTypeFromTagLocal(iNativeGetTagFromKeyLocal));
        byte[] bArr = new byte[marshalerForKey.calculateMarshalSize(t)];
        marshalerForKey.marshal(t, ByteBuffer.wrap(bArr).order(ByteOrder.nativeOrder()));
        writeValues(iNativeGetTagFromKeyLocal, bArr);
    }

    private boolean setAvailableFormats(int[] iArr) {
        if (iArr == null) {
            return false;
        }
        int[] iArr2 = new int[iArr.length];
        for (int i = 0; i < iArr.length; i++) {
            iArr2[i] = iArr[i];
            if (iArr[i] == 256) {
                iArr2[i] = 33;
            }
        }
        setBase(CameraCharacteristics.SCALER_AVAILABLE_FORMATS, iArr2);
        return true;
    }

    private boolean setFaceRectangles(Rect[] rectArr) {
        if (rectArr == null) {
            return false;
        }
        Rect[] rectArr2 = new Rect[rectArr.length];
        for (int i = 0; i < rectArr2.length; i++) {
            rectArr2[i] = new Rect(rectArr[i].left, rectArr[i].top, rectArr[i].right + rectArr[i].left, rectArr[i].bottom + rectArr[i].top);
        }
        setBase(CaptureResult.STATISTICS_FACE_RECTANGLES, rectArr2);
        return true;
    }

    private <T> boolean setTonemapCurve(TonemapCurve tonemapCurve) {
        if (tonemapCurve == null) {
            return false;
        }
        float[][] fArr = new float[3][];
        for (int i = 0; i <= 2; i++) {
            fArr[i] = new float[tonemapCurve.getPointCount(i) * 2];
            tonemapCurve.copyColorCurve(i, fArr[i], 0);
        }
        setBase(CaptureRequest.TONEMAP_CURVE_RED, fArr[0]);
        setBase(CaptureRequest.TONEMAP_CURVE_GREEN, fArr[1]);
        setBase(CaptureRequest.TONEMAP_CURVE_BLUE, fArr[2]);
        return true;
    }

    public void swap(CameraMetadataNative cameraMetadataNative) {
        nativeSwap(cameraMetadataNative);
    }

    public int getEntryCount() {
        return nativeGetEntryCount();
    }

    public boolean isEmpty() {
        return nativeIsEmpty();
    }

    public <K> ArrayList<K> getAllVendorKeys(Class<K> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        return nativeGetAllVendorKeys(cls);
    }

    public static int getTag(String str) {
        return nativeGetTagFromKey(str, Long.MAX_VALUE);
    }

    public static int getTag(String str, long j) {
        return nativeGetTagFromKey(str, j);
    }

    public static int getNativeType(int i, long j) {
        return nativeGetTypeFromTag(i, j);
    }

    public void writeValues(int i, byte[] bArr) {
        nativeWriteValues(i, bArr);
    }

    public byte[] readValues(int i) {
        return nativeReadValues(i);
    }

    public void dumpToLog() {
        try {
            nativeDump();
        } catch (IOException e) {
            Log.wtf(TAG, "Dump logging failed", e);
        }
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private static <T> Marshaler<T> getMarshalerForKey(Key<T> key, int i) {
        return MarshalRegistry.getMarshaler(key.getTypeReference(), i);
    }

    private static void registerAllMarshalers() {
        for (MarshalQueryable marshalQueryable : new MarshalQueryable[]{new MarshalQueryablePrimitive(), new MarshalQueryableEnum(), new MarshalQueryableArray(), new MarshalQueryableBoolean(), new MarshalQueryableNativeByteToInteger(), new MarshalQueryableRect(), new MarshalQueryableSize(), new MarshalQueryableSizeF(), new MarshalQueryableString(), new MarshalQueryableReprocessFormatsMap(), new MarshalQueryableRange(), new MarshalQueryablePair(), new MarshalQueryableMeteringRectangle(), new MarshalQueryableColorSpaceTransform(), new MarshalQueryableStreamConfiguration(), new MarshalQueryableStreamConfigurationDuration(), new MarshalQueryableRggbChannelVector(), new MarshalQueryableBlackLevelPattern(), new MarshalQueryableHighSpeedVideoConfiguration(), new MarshalQueryableParcelable()}) {
            MarshalRegistry.registerMarshalQueryable(marshalQueryable);
        }
    }

    private static boolean areValuesAllNull(Object... objArr) {
        for (Object obj : objArr) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }
}
