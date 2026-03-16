package android.view.textservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;

public final class SpellCheckerInfo implements Parcelable {
    private final String mId;
    private final int mLabel;
    private final ResolveInfo mService;
    private final String mSettingsActivityName;
    private final ArrayList<SpellCheckerSubtype> mSubtypes = new ArrayList<>();
    private static final String TAG = SpellCheckerInfo.class.getSimpleName();
    public static final Parcelable.Creator<SpellCheckerInfo> CREATOR = new Parcelable.Creator<SpellCheckerInfo>() {
        @Override
        public SpellCheckerInfo createFromParcel(Parcel source) {
            return new SpellCheckerInfo(source);
        }

        @Override
        public SpellCheckerInfo[] newArray(int size) {
            return new SpellCheckerInfo[size];
        }
    };

    public SpellCheckerInfo(Context context, ResolveInfo service) throws XmlPullParserException, IOException {
        int type;
        this.mService = service;
        ServiceInfo si = service.serviceInfo;
        this.mId = new ComponentName(si.packageName, si.name).flattenToShortString();
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            try {
                parser = si.loadXmlMetaData(pm, SpellCheckerSession.SERVICE_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No android.view.textservice.scs meta-data");
                }
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"spell-checker".equals(nodeName)) {
                    throw new XmlPullParserException("Meta-data does not start with spell-checker tag");
                }
                TypedArray sa = res.obtainAttributes(attrs, R.styleable.SpellChecker);
                int label = sa.getResourceId(0, 0);
                String settingsActivityComponent = sa.getString(1);
                sa.recycle();
                int depth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if ((type2 == 3 && parser.getDepth() <= depth) || type2 == 1) {
                        break;
                    }
                    if (type2 == 2) {
                        String subtypeNodeName = parser.getName();
                        if (!"subtype".equals(subtypeNodeName)) {
                            throw new XmlPullParserException("Meta-data in spell-checker does not start with subtype tag");
                        }
                        TypedArray a = res.obtainAttributes(attrs, R.styleable.SpellChecker_Subtype);
                        SpellCheckerSubtype subtype = new SpellCheckerSubtype(a.getResourceId(0, 0), a.getString(1), a.getString(2));
                        this.mSubtypes.add(subtype);
                    }
                }
                this.mLabel = label;
                this.mSettingsActivityName = settingsActivityComponent;
            } catch (Exception e) {
                Slog.e(TAG, "Caught exception: " + e);
                throw new XmlPullParserException("Unable to create context for: " + si.packageName);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public SpellCheckerInfo(Parcel source) {
        this.mLabel = source.readInt();
        this.mId = source.readString();
        this.mSettingsActivityName = source.readString();
        this.mService = ResolveInfo.CREATOR.createFromParcel(source);
        source.readTypedList(this.mSubtypes, SpellCheckerSubtype.CREATOR);
    }

    public String getId() {
        return this.mId;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public String getPackageName() {
        return this.mService.serviceInfo.packageName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mLabel);
        dest.writeString(this.mId);
        dest.writeString(this.mSettingsActivityName);
        this.mService.writeToParcel(dest, flags);
        dest.writeTypedList(this.mSubtypes);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return (this.mLabel == 0 || pm == null) ? ProxyInfo.LOCAL_EXCL_LIST : pm.getText(getPackageName(), this.mLabel, this.mService.serviceInfo.applicationInfo);
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public ServiceInfo getServiceInfo() {
        return this.mService.serviceInfo;
    }

    public String getSettingsActivity() {
        return this.mSettingsActivityName;
    }

    public int getSubtypeCount() {
        return this.mSubtypes.size();
    }

    public SpellCheckerSubtype getSubtypeAt(int index) {
        return this.mSubtypes.get(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
