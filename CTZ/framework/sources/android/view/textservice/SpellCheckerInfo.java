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
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;
import android.util.AttributeSet;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import java.io.PrintWriter;
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
        public SpellCheckerInfo createFromParcel(Parcel parcel) {
            return new SpellCheckerInfo(parcel);
        }

        @Override
        public SpellCheckerInfo[] newArray(int i) {
            return new SpellCheckerInfo[i];
        }
    };

    public SpellCheckerInfo(Context context, ResolveInfo resolveInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        int next;
        this.mService = resolveInfo;
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        this.mId = new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString();
        PackageManager packageManager = context.getPackageManager();
        try {
            try {
                xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, SpellCheckerSession.SERVICE_META_DATA);
            } catch (Throwable th) {
                th = th;
                xmlResourceParserLoadXmlMetaData = null;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            if (xmlResourceParserLoadXmlMetaData == null) {
                throw new XmlPullParserException("No android.view.textservice.scs meta-data");
            }
            Resources resourcesForApplication = packageManager.getResourcesForApplication(serviceInfo.applicationInfo);
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
            do {
                next = xmlResourceParserLoadXmlMetaData.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            if (!"spell-checker".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                throw new XmlPullParserException("Meta-data does not start with spell-checker tag");
            }
            TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.SpellChecker);
            int resourceId = typedArrayObtainAttributes.getResourceId(0, 0);
            String string = typedArrayObtainAttributes.getString(1);
            typedArrayObtainAttributes.recycle();
            int depth = xmlResourceParserLoadXmlMetaData.getDepth();
            while (true) {
                int next2 = xmlResourceParserLoadXmlMetaData.next();
                if ((next2 == 3 && xmlResourceParserLoadXmlMetaData.getDepth() <= depth) || next2 == 1) {
                    break;
                }
                if (next2 == 2) {
                    if (!"subtype".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                        throw new XmlPullParserException("Meta-data in spell-checker does not start with subtype tag");
                    }
                    TypedArray typedArrayObtainAttributes2 = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.SpellChecker_Subtype);
                    this.mSubtypes.add(new SpellCheckerSubtype(typedArrayObtainAttributes2.getResourceId(0, 0), typedArrayObtainAttributes2.getString(1), typedArrayObtainAttributes2.getString(4), typedArrayObtainAttributes2.getString(2), typedArrayObtainAttributes2.getInt(3, 0)));
                }
            }
        } catch (Exception e2) {
            e = e2;
            Slog.e(TAG, "Caught exception: " + e);
            throw new XmlPullParserException("Unable to create context for: " + serviceInfo.packageName);
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
    }

    public SpellCheckerInfo(Parcel parcel) {
        this.mLabel = parcel.readInt();
        this.mId = parcel.readString();
        this.mSettingsActivityName = parcel.readString();
        this.mService = ResolveInfo.CREATOR.createFromParcel(parcel);
        parcel.readTypedList(this.mSubtypes, SpellCheckerSubtype.CREATOR);
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mLabel);
        parcel.writeString(this.mId);
        parcel.writeString(this.mSettingsActivityName);
        this.mService.writeToParcel(parcel, i);
        parcel.writeTypedList(this.mSubtypes);
    }

    public CharSequence loadLabel(PackageManager packageManager) {
        if (this.mLabel == 0 || packageManager == null) {
            return "";
        }
        return packageManager.getText(getPackageName(), this.mLabel, this.mService.serviceInfo.applicationInfo);
    }

    public Drawable loadIcon(PackageManager packageManager) {
        return this.mService.loadIcon(packageManager);
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

    public SpellCheckerSubtype getSubtypeAt(int i) {
        return this.mSubtypes.get(i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "mId=" + this.mId);
        printWriter.println(str + "mSettingsActivityName=" + this.mSettingsActivityName);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("Service:");
        printWriter.println(sb.toString());
        this.mService.dump(new PrintWriterPrinter(printWriter), str + "  ");
        int subtypeCount = getSubtypeCount();
        for (int i = 0; i < subtypeCount; i++) {
            SpellCheckerSubtype subtypeAt = getSubtypeAt(i);
            printWriter.println(str + "  Subtype #" + i + SettingsStringUtil.DELIMITER);
            printWriter.println(str + "    locale=" + subtypeAt.getLocale() + " languageTag=" + subtypeAt.getLanguageTag());
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);
            sb2.append("    extraValue=");
            sb2.append(subtypeAt.getExtraValue());
            printWriter.println(sb2.toString());
        }
    }
}
