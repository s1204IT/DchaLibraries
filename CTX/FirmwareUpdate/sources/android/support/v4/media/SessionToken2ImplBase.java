package android.support.v4.media;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.IMediaSession2;
import android.support.v4.media.SessionToken2;
import android.text.TextUtils;

final class SessionToken2ImplBase implements SessionToken2.SupportLibraryImpl {
    private final ComponentName mComponentName;
    private final IMediaSession2 mISession2;
    private final String mPackageName;
    private final String mServiceName;
    private final String mSessionId;
    private final int mType;
    private final int mUid;

    SessionToken2ImplBase(int i, int i2, String str, String str2, String str3, IMediaSession2 iMediaSession2) {
        this.mUid = i;
        this.mType = i2;
        this.mPackageName = str;
        this.mServiceName = str2;
        this.mComponentName = this.mType == 0 ? null : new ComponentName(str, str2);
        this.mSessionId = str3;
        this.mISession2 = iMediaSession2;
    }

    public static SessionToken2ImplBase fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int i = bundle.getInt("android.media.token.uid");
        int i2 = bundle.getInt("android.media.token.type", -1);
        String string = bundle.getString("android.media.token.package_name");
        String string2 = bundle.getString("android.media.token.service_name");
        String string3 = bundle.getString("android.media.token.session_id");
        IMediaSession2 iMediaSession2AsInterface = IMediaSession2.Stub.asInterface(BundleCompat.getBinder(bundle, "android.media.token.session_binder"));
        switch (i2) {
            case 0:
                if (iMediaSession2AsInterface == null) {
                    throw new IllegalArgumentException("Unexpected token for session, binder=" + iMediaSession2AsInterface);
                }
                break;
            case 1:
            case 2:
                if (TextUtils.isEmpty(string2)) {
                    throw new IllegalArgumentException("Session service needs service name");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type");
        }
        if (TextUtils.isEmpty(string) || string3 == null) {
            throw new IllegalArgumentException("Package name nor ID cannot be null.");
        }
        return new SessionToken2ImplBase(i, i2, string, string2, string3, iMediaSession2AsInterface);
    }

    private boolean sessionBinderEquals(IMediaSession2 iMediaSession2, IMediaSession2 iMediaSession22) {
        return (iMediaSession2 == null || iMediaSession22 == null) ? iMediaSession2 == iMediaSession22 : iMediaSession2.asBinder().equals(iMediaSession22.asBinder());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2ImplBase)) {
            return false;
        }
        SessionToken2ImplBase sessionToken2ImplBase = (SessionToken2ImplBase) obj;
        return this.mUid == sessionToken2ImplBase.mUid && TextUtils.equals(this.mPackageName, sessionToken2ImplBase.mPackageName) && TextUtils.equals(this.mServiceName, sessionToken2ImplBase.mServiceName) && TextUtils.equals(this.mSessionId, sessionToken2ImplBase.mSessionId) && this.mType == sessionToken2ImplBase.mType && sessionBinderEquals(this.mISession2, sessionToken2ImplBase.mISession2);
    }

    public int hashCode() {
        return ((((((((this.mServiceName != null ? this.mServiceName.hashCode() : 0) * 31) + this.mSessionId.hashCode()) * 31) + this.mPackageName.hashCode()) * 31) + this.mUid) * 31) + this.mType;
    }

    public String toString() {
        return "SessionToken {pkg=" + this.mPackageName + " id=" + this.mSessionId + " type=" + this.mType + " service=" + this.mServiceName + " IMediaSession2=" + this.mISession2 + "}";
    }
}
