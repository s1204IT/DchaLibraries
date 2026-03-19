package android.content;

import android.content.pm.RegisteredServicesCache;
import android.content.pm.XmlSerializerAndParser;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.SparseArray;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SyncAdaptersCache extends RegisteredServicesCache<SyncAdapterType> {
    private static final String ATTRIBUTES_NAME = "sync-adapter";
    private static final String SERVICE_INTERFACE = "android.content.SyncAdapter";
    private static final String SERVICE_META_DATA = "android.content.SyncAdapter";
    private static final String TAG = "Account";
    private static final MySerializer sSerializer = new MySerializer();

    @GuardedBy("mServicesLock")
    private SparseArray<ArrayMap<String, String[]>> mAuthorityToSyncAdapters;

    public SyncAdaptersCache(Context context) {
        super(context, "android.content.SyncAdapter", "android.content.SyncAdapter", ATTRIBUTES_NAME, sSerializer);
        this.mAuthorityToSyncAdapters = new SparseArray<>();
    }

    @Override
    public SyncAdapterType parseServiceAttributes(Resources resources, String str, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.SyncAdapter);
        try {
            String string = typedArrayObtainAttributes.getString(2);
            String string2 = typedArrayObtainAttributes.getString(1);
            if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                return new SyncAdapterType(string, string2, typedArrayObtainAttributes.getBoolean(3, true), typedArrayObtainAttributes.getBoolean(4, true), typedArrayObtainAttributes.getBoolean(6, false), typedArrayObtainAttributes.getBoolean(5, false), typedArrayObtainAttributes.getString(0), str);
            }
            return null;
        } finally {
            typedArrayObtainAttributes.recycle();
        }
    }

    @Override
    protected void onServicesChangedLocked(int i) {
        synchronized (this.mServicesLock) {
            ArrayMap<String, String[]> arrayMap = this.mAuthorityToSyncAdapters.get(i);
            if (arrayMap != null) {
                arrayMap.clear();
            }
        }
        super.onServicesChangedLocked(i);
    }

    public String[] getSyncAdapterPackagesForAuthority(String str, int i) {
        synchronized (this.mServicesLock) {
            ArrayMap<String, String[]> arrayMap = this.mAuthorityToSyncAdapters.get(i);
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
                this.mAuthorityToSyncAdapters.put(i, arrayMap);
            }
            if (arrayMap.containsKey(str)) {
                return arrayMap.get(str);
            }
            Collection<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> allServices = getAllServices(i);
            ArrayList arrayList = new ArrayList();
            for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo : allServices) {
                if (str.equals(serviceInfo.type.authority) && serviceInfo.componentName != null) {
                    arrayList.add(serviceInfo.componentName.getPackageName());
                }
            }
            String[] strArr = new String[arrayList.size()];
            arrayList.toArray(strArr);
            arrayMap.put(str, strArr);
            return strArr;
        }
    }

    @Override
    protected void onUserRemoved(int i) {
        synchronized (this.mServicesLock) {
            this.mAuthorityToSyncAdapters.remove(i);
        }
        super.onUserRemoved(i);
    }

    static class MySerializer implements XmlSerializerAndParser<SyncAdapterType> {
        MySerializer() {
        }

        @Override
        public void writeAsXml(SyncAdapterType syncAdapterType, XmlSerializer xmlSerializer) throws IOException {
            xmlSerializer.attribute(null, ContactsContract.Directory.DIRECTORY_AUTHORITY, syncAdapterType.authority);
            xmlSerializer.attribute(null, "accountType", syncAdapterType.accountType);
        }

        @Override
        public SyncAdapterType createFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            return SyncAdapterType.newKey(xmlPullParser.getAttributeValue(null, ContactsContract.Directory.DIRECTORY_AUTHORITY), xmlPullParser.getAttributeValue(null, "accountType"));
        }
    }
}
