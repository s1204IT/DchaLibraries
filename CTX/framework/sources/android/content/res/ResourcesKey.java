package android.content.res;

import android.text.TextUtils;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Arrays;
import java.util.Objects;

public final class ResourcesKey {
    public final CompatibilityInfo mCompatInfo;
    public final int mDisplayId;
    private final int mHash;
    public final String[] mLibDirs;
    public final String[] mOverlayDirs;
    public final Configuration mOverrideConfiguration;
    public final String mResDir;
    public final String[] mSplitResDirs;

    public ResourcesKey(String str, String[] strArr, String[] strArr2, String[] strArr3, int i, Configuration configuration, CompatibilityInfo compatibilityInfo) {
        this.mResDir = str;
        this.mSplitResDirs = strArr;
        this.mOverlayDirs = strArr2;
        this.mLibDirs = strArr3;
        this.mDisplayId = i;
        this.mOverrideConfiguration = new Configuration(configuration == null ? Configuration.EMPTY : configuration);
        this.mCompatInfo = compatibilityInfo == null ? CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO : compatibilityInfo;
        this.mHash = (31 * (((((((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + Objects.hashCode(this.mResDir)) * 31) + Arrays.hashCode(this.mSplitResDirs)) * 31) + Arrays.hashCode(this.mOverlayDirs)) * 31) + Arrays.hashCode(this.mLibDirs)) * 31) + this.mDisplayId) * 31) + Objects.hashCode(this.mOverrideConfiguration))) + Objects.hashCode(this.mCompatInfo);
    }

    public boolean hasOverrideConfiguration() {
        return !Configuration.EMPTY.equals(this.mOverrideConfiguration);
    }

    public boolean isPathReferenced(String str) {
        return (this.mResDir != null && this.mResDir.startsWith(str)) || anyStartsWith(this.mSplitResDirs, str) || anyStartsWith(this.mOverlayDirs, str) || anyStartsWith(this.mLibDirs, str);
    }

    private static boolean anyStartsWith(String[] strArr, String str) {
        if (strArr != null) {
            for (String str2 : strArr) {
                if (str2 != null && str2.startsWith(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        return this.mHash;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ResourcesKey)) {
            return false;
        }
        ResourcesKey resourcesKey = (ResourcesKey) obj;
        return this.mHash == resourcesKey.mHash && Objects.equals(this.mResDir, resourcesKey.mResDir) && Arrays.equals(this.mSplitResDirs, resourcesKey.mSplitResDirs) && Arrays.equals(this.mOverlayDirs, resourcesKey.mOverlayDirs) && Arrays.equals(this.mLibDirs, resourcesKey.mLibDirs) && this.mDisplayId == resourcesKey.mDisplayId && Objects.equals(this.mOverrideConfiguration, resourcesKey.mOverrideConfiguration) && Objects.equals(this.mCompatInfo, resourcesKey.mCompatInfo);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourcesKey{");
        sb.append(" mHash=");
        sb.append(Integer.toHexString(this.mHash));
        sb.append(" mResDir=");
        sb.append(this.mResDir);
        sb.append(" mSplitDirs=[");
        if (this.mSplitResDirs != null) {
            sb.append(TextUtils.join(",", this.mSplitResDirs));
        }
        sb.append("]");
        sb.append(" mOverlayDirs=[");
        if (this.mOverlayDirs != null) {
            sb.append(TextUtils.join(",", this.mOverlayDirs));
        }
        sb.append("]");
        sb.append(" mLibDirs=[");
        if (this.mLibDirs != null) {
            sb.append(TextUtils.join(",", this.mLibDirs));
        }
        sb.append("]");
        sb.append(" mDisplayId=");
        sb.append(this.mDisplayId);
        sb.append(" mOverrideConfig=");
        sb.append(Configuration.resourceQualifierString(this.mOverrideConfiguration));
        sb.append(" mCompatInfo=");
        sb.append(this.mCompatInfo);
        sb.append("}");
        return sb.toString();
    }
}
