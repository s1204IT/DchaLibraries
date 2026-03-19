package android.provider;

import android.util.Base64;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.util.Preconditions;
import java.util.Collections;
import java.util.List;

public final class FontRequest {
    private final List<List<byte[]>> mCertificates;
    private final String mIdentifier;
    private final String mProviderAuthority;
    private final String mProviderPackage;
    private final String mQuery;

    public FontRequest(String str, String str2, String str3) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(str);
        this.mQuery = (String) Preconditions.checkNotNull(str3);
        this.mProviderPackage = (String) Preconditions.checkNotNull(str2);
        this.mCertificates = Collections.emptyList();
        this.mIdentifier = this.mProviderAuthority + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + this.mProviderPackage + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + this.mQuery;
    }

    public FontRequest(String str, String str2, String str3, List<List<byte[]>> list) {
        this.mProviderAuthority = (String) Preconditions.checkNotNull(str);
        this.mProviderPackage = (String) Preconditions.checkNotNull(str2);
        this.mQuery = (String) Preconditions.checkNotNull(str3);
        this.mCertificates = (List) Preconditions.checkNotNull(list);
        this.mIdentifier = this.mProviderAuthority + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + this.mProviderPackage + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + this.mQuery;
    }

    public String getProviderAuthority() {
        return this.mProviderAuthority;
    }

    public String getProviderPackage() {
        return this.mProviderPackage;
    }

    public String getQuery() {
        return this.mQuery;
    }

    public List<List<byte[]>> getCertificates() {
        return this.mCertificates;
    }

    public String getIdentifier() {
        return this.mIdentifier;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FontRequest {mProviderAuthority: " + this.mProviderAuthority + ", mProviderPackage: " + this.mProviderPackage + ", mQuery: " + this.mQuery + ", mCertificates:");
        for (int i = 0; i < this.mCertificates.size(); i++) {
            sb.append(" [");
            List<byte[]> list = this.mCertificates.get(i);
            for (int i2 = 0; i2 < list.size(); i2++) {
                sb.append(" \"");
                sb.append(Base64.encodeToString(list.get(i2), 0));
                sb.append("\"");
            }
            sb.append(" ]");
        }
        sb.append("}");
        return sb.toString();
    }
}
