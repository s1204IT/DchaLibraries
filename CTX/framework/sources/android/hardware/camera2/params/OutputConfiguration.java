package android.hardware.camera2.params;

import android.annotation.SystemApi;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.hardware.camera2.utils.SurfaceUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OutputConfiguration implements Parcelable {
    public static final Parcelable.Creator<OutputConfiguration> CREATOR = new Parcelable.Creator<OutputConfiguration>() {
        @Override
        public OutputConfiguration createFromParcel(Parcel parcel) {
            return new OutputConfiguration(parcel);
        }

        @Override
        public OutputConfiguration[] newArray(int i) {
            return new OutputConfiguration[i];
        }
    };
    private static final int MAX_SURFACES_COUNT = 4;

    @SystemApi
    public static final int ROTATION_0 = 0;

    @SystemApi
    public static final int ROTATION_180 = 2;

    @SystemApi
    public static final int ROTATION_270 = 3;

    @SystemApi
    public static final int ROTATION_90 = 1;
    public static final int SURFACE_GROUP_ID_NONE = -1;
    private static final String TAG = "OutputConfiguration";
    private final int SURFACE_TYPE_SURFACE_TEXTURE;
    private final int SURFACE_TYPE_SURFACE_VIEW;
    private final int SURFACE_TYPE_UNKNOWN;
    private final int mConfiguredDataspace;
    private final int mConfiguredFormat;
    private final int mConfiguredGenerationId;
    private final Size mConfiguredSize;
    private final boolean mIsDeferredConfig;
    private boolean mIsShared;
    private String mPhysicalCameraId;
    private final int mRotation;
    private final int mSurfaceGroupId;
    private final int mSurfaceType;
    private ArrayList<Surface> mSurfaces;

    public OutputConfiguration(Surface surface) {
        this(-1, surface, 0);
    }

    public OutputConfiguration(int i, Surface surface) {
        this(i, surface, 0);
    }

    @SystemApi
    public OutputConfiguration(Surface surface, int i) {
        this(-1, surface, i);
    }

    @SystemApi
    public OutputConfiguration(int i, Surface surface, int i2) {
        this.SURFACE_TYPE_UNKNOWN = -1;
        this.SURFACE_TYPE_SURFACE_VIEW = 0;
        this.SURFACE_TYPE_SURFACE_TEXTURE = 1;
        Preconditions.checkNotNull(surface, "Surface must not be null");
        Preconditions.checkArgumentInRange(i2, 0, 3, "Rotation constant");
        this.mSurfaceGroupId = i;
        this.mSurfaceType = -1;
        this.mSurfaces = new ArrayList<>();
        this.mSurfaces.add(surface);
        this.mRotation = i2;
        this.mConfiguredSize = SurfaceUtils.getSurfaceSize(surface);
        this.mConfiguredFormat = SurfaceUtils.getSurfaceFormat(surface);
        this.mConfiguredDataspace = SurfaceUtils.getSurfaceDataspace(surface);
        this.mConfiguredGenerationId = surface.getGenerationId();
        this.mIsDeferredConfig = false;
        this.mIsShared = false;
        this.mPhysicalCameraId = null;
    }

    public <T> OutputConfiguration(Size size, Class<T> cls) {
        this.SURFACE_TYPE_UNKNOWN = -1;
        this.SURFACE_TYPE_SURFACE_VIEW = 0;
        this.SURFACE_TYPE_SURFACE_TEXTURE = 1;
        Preconditions.checkNotNull(cls, "surfaceSize must not be null");
        Preconditions.checkNotNull(cls, "klass must not be null");
        if (cls == SurfaceHolder.class) {
            this.mSurfaceType = 0;
        } else if (cls == SurfaceTexture.class) {
            this.mSurfaceType = 1;
        } else {
            this.mSurfaceType = -1;
            throw new IllegalArgumentException("Unknow surface source class type");
        }
        if (size.getWidth() == 0 || size.getHeight() == 0) {
            throw new IllegalArgumentException("Surface size needs to be non-zero");
        }
        this.mSurfaceGroupId = -1;
        this.mSurfaces = new ArrayList<>();
        this.mRotation = 0;
        this.mConfiguredSize = size;
        this.mConfiguredFormat = StreamConfigurationMap.imageFormatToInternal(34);
        this.mConfiguredDataspace = StreamConfigurationMap.imageFormatToDataspace(34);
        this.mConfiguredGenerationId = 0;
        this.mIsDeferredConfig = true;
        this.mIsShared = false;
        this.mPhysicalCameraId = null;
    }

    public void enableSurfaceSharing() {
        this.mIsShared = true;
    }

    public void setPhysicalCameraId(String str) {
        this.mPhysicalCameraId = str;
    }

    public boolean isForPhysicalCamera() {
        return this.mPhysicalCameraId != null;
    }

    public boolean isDeferredConfiguration() {
        return this.mIsDeferredConfig;
    }

    public void addSurface(Surface surface) {
        Preconditions.checkNotNull(surface, "Surface must not be null");
        if (this.mSurfaces.contains(surface)) {
            throw new IllegalStateException("Surface is already added!");
        }
        if (this.mSurfaces.size() == 1 && !this.mIsShared) {
            throw new IllegalStateException("Cannot have 2 surfaces for a non-sharing configuration");
        }
        if (this.mSurfaces.size() + 1 > 4) {
            throw new IllegalArgumentException("Exceeds maximum number of surfaces");
        }
        Size surfaceSize = SurfaceUtils.getSurfaceSize(surface);
        if (!surfaceSize.equals(this.mConfiguredSize)) {
            Log.w(TAG, "Added surface size " + surfaceSize + " is different than pre-configured size " + this.mConfiguredSize + ", the pre-configured size will be used.");
        }
        if (this.mConfiguredFormat != SurfaceUtils.getSurfaceFormat(surface)) {
            throw new IllegalArgumentException("The format of added surface format doesn't match");
        }
        if (this.mConfiguredFormat != 34 && this.mConfiguredDataspace != SurfaceUtils.getSurfaceDataspace(surface)) {
            throw new IllegalArgumentException("The dataspace of added surface doesn't match");
        }
        this.mSurfaces.add(surface);
    }

    public void removeSurface(Surface surface) {
        if (getSurface() == surface) {
            throw new IllegalArgumentException("Cannot remove surface associated with this output configuration");
        }
        if (!this.mSurfaces.remove(surface)) {
            throw new IllegalArgumentException("Surface is not part of this output configuration");
        }
    }

    public OutputConfiguration(OutputConfiguration outputConfiguration) {
        this.SURFACE_TYPE_UNKNOWN = -1;
        this.SURFACE_TYPE_SURFACE_VIEW = 0;
        this.SURFACE_TYPE_SURFACE_TEXTURE = 1;
        if (outputConfiguration == null) {
            throw new IllegalArgumentException("OutputConfiguration shouldn't be null");
        }
        this.mSurfaces = outputConfiguration.mSurfaces;
        this.mRotation = outputConfiguration.mRotation;
        this.mSurfaceGroupId = outputConfiguration.mSurfaceGroupId;
        this.mSurfaceType = outputConfiguration.mSurfaceType;
        this.mConfiguredDataspace = outputConfiguration.mConfiguredDataspace;
        this.mConfiguredFormat = outputConfiguration.mConfiguredFormat;
        this.mConfiguredSize = outputConfiguration.mConfiguredSize;
        this.mConfiguredGenerationId = outputConfiguration.mConfiguredGenerationId;
        this.mIsDeferredConfig = outputConfiguration.mIsDeferredConfig;
        this.mIsShared = outputConfiguration.mIsShared;
        this.mPhysicalCameraId = outputConfiguration.mPhysicalCameraId;
    }

    private OutputConfiguration(Parcel parcel) {
        this.SURFACE_TYPE_UNKNOWN = -1;
        this.SURFACE_TYPE_SURFACE_VIEW = 0;
        this.SURFACE_TYPE_SURFACE_TEXTURE = 1;
        int i = parcel.readInt();
        int i2 = parcel.readInt();
        int i3 = parcel.readInt();
        int i4 = parcel.readInt();
        int i5 = parcel.readInt();
        boolean z = parcel.readInt() == 1;
        boolean z2 = parcel.readInt() == 1;
        ArrayList<Surface> arrayList = new ArrayList<>();
        parcel.readTypedList(arrayList, Surface.CREATOR);
        String string = parcel.readString();
        Preconditions.checkArgumentInRange(i, 0, 3, "Rotation constant");
        this.mSurfaceGroupId = i2;
        this.mRotation = i;
        this.mSurfaces = arrayList;
        this.mConfiguredSize = new Size(i4, i5);
        this.mIsDeferredConfig = z;
        this.mIsShared = z2;
        this.mSurfaces = arrayList;
        if (this.mSurfaces.size() > 0) {
            this.mSurfaceType = -1;
            this.mConfiguredFormat = SurfaceUtils.getSurfaceFormat(this.mSurfaces.get(0));
            this.mConfiguredDataspace = SurfaceUtils.getSurfaceDataspace(this.mSurfaces.get(0));
            this.mConfiguredGenerationId = this.mSurfaces.get(0).getGenerationId();
        } else {
            this.mSurfaceType = i3;
            this.mConfiguredFormat = StreamConfigurationMap.imageFormatToInternal(34);
            this.mConfiguredDataspace = StreamConfigurationMap.imageFormatToDataspace(34);
            this.mConfiguredGenerationId = 0;
        }
        this.mPhysicalCameraId = string;
    }

    public int getMaxSharedSurfaceCount() {
        return 4;
    }

    public Surface getSurface() {
        if (this.mSurfaces.size() == 0) {
            return null;
        }
        return this.mSurfaces.get(0);
    }

    public List<Surface> getSurfaces() {
        return Collections.unmodifiableList(this.mSurfaces);
    }

    @SystemApi
    public int getRotation() {
        return this.mRotation;
    }

    public int getSurfaceGroupId() {
        return this.mSurfaceGroupId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (parcel == null) {
            throw new IllegalArgumentException("dest must not be null");
        }
        parcel.writeInt(this.mRotation);
        parcel.writeInt(this.mSurfaceGroupId);
        parcel.writeInt(this.mSurfaceType);
        parcel.writeInt(this.mConfiguredSize.getWidth());
        parcel.writeInt(this.mConfiguredSize.getHeight());
        parcel.writeInt(this.mIsDeferredConfig ? 1 : 0);
        parcel.writeInt(this.mIsShared ? 1 : 0);
        parcel.writeTypedList(this.mSurfaces);
        parcel.writeString(this.mPhysicalCameraId);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OutputConfiguration)) {
            return false;
        }
        OutputConfiguration outputConfiguration = (OutputConfiguration) obj;
        if (this.mRotation != outputConfiguration.mRotation || !this.mConfiguredSize.equals(outputConfiguration.mConfiguredSize) || this.mConfiguredFormat != outputConfiguration.mConfiguredFormat || this.mSurfaceGroupId != outputConfiguration.mSurfaceGroupId || this.mSurfaceType != outputConfiguration.mSurfaceType || this.mIsDeferredConfig != outputConfiguration.mIsDeferredConfig || this.mIsShared != outputConfiguration.mIsShared || this.mConfiguredFormat != outputConfiguration.mConfiguredFormat || this.mConfiguredDataspace != outputConfiguration.mConfiguredDataspace || this.mConfiguredGenerationId != outputConfiguration.mConfiguredGenerationId) {
            return false;
        }
        int iMin = Math.min(this.mSurfaces.size(), outputConfiguration.mSurfaces.size());
        for (int i = 0; i < iMin; i++) {
            if (this.mSurfaces.get(i) != outputConfiguration.mSurfaces.get(i)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        if (this.mIsDeferredConfig) {
            int[] iArr = new int[8];
            iArr[0] = this.mRotation;
            iArr[1] = this.mConfiguredSize.hashCode();
            iArr[2] = this.mConfiguredFormat;
            iArr[3] = this.mConfiguredDataspace;
            iArr[4] = this.mSurfaceGroupId;
            iArr[5] = this.mSurfaceType;
            iArr[6] = this.mIsShared ? 1 : 0;
            iArr[7] = this.mPhysicalCameraId != null ? this.mPhysicalCameraId.hashCode() : 0;
            return HashCodeHelpers.hashCode(iArr);
        }
        int[] iArr2 = new int[9];
        iArr2[0] = this.mRotation;
        iArr2[1] = this.mSurfaces.hashCode();
        iArr2[2] = this.mConfiguredGenerationId;
        iArr2[3] = this.mConfiguredSize.hashCode();
        iArr2[4] = this.mConfiguredFormat;
        iArr2[5] = this.mConfiguredDataspace;
        iArr2[6] = this.mSurfaceGroupId;
        iArr2[7] = this.mIsShared ? 1 : 0;
        iArr2[8] = this.mPhysicalCameraId != null ? this.mPhysicalCameraId.hashCode() : 0;
        return HashCodeHelpers.hashCode(iArr2);
    }
}
