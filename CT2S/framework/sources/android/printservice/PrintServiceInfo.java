package android.printservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public final class PrintServiceInfo implements Parcelable {
    private static final String TAG_PRINT_SERVICE = "print-service";
    private final String mAddPrintersActivityName;
    private final String mAdvancedPrintOptionsActivityName;
    private final String mId;
    private final ResolveInfo mResolveInfo;
    private final String mSettingsActivityName;
    private static final String LOG_TAG = PrintServiceInfo.class.getSimpleName();
    public static final Parcelable.Creator<PrintServiceInfo> CREATOR = new Parcelable.Creator<PrintServiceInfo>() {
        @Override
        public PrintServiceInfo createFromParcel(Parcel parcel) {
            return new PrintServiceInfo(parcel);
        }

        @Override
        public PrintServiceInfo[] newArray(int size) {
            return new PrintServiceInfo[size];
        }
    };

    public PrintServiceInfo(Parcel parcel) {
        this.mId = parcel.readString();
        this.mResolveInfo = (ResolveInfo) parcel.readParcelable(null);
        this.mSettingsActivityName = parcel.readString();
        this.mAddPrintersActivityName = parcel.readString();
        this.mAdvancedPrintOptionsActivityName = parcel.readString();
    }

    public PrintServiceInfo(ResolveInfo resolveInfo, String settingsActivityName, String addPrintersActivityName, String advancedPrintOptionsActivityName) {
        this.mId = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToString();
        this.mResolveInfo = resolveInfo;
        this.mSettingsActivityName = settingsActivityName;
        this.mAddPrintersActivityName = addPrintersActivityName;
        this.mAdvancedPrintOptionsActivityName = advancedPrintOptionsActivityName;
    }

    public static PrintServiceInfo create(ResolveInfo resolveInfo, Context context) {
        String settingsActivityName = null;
        String addPrintersActivityName = null;
        String advancedPrintOptionsActivityName = null;
        PackageManager packageManager = context.getPackageManager();
        XmlResourceParser parser = resolveInfo.serviceInfo.loadXmlMetaData(packageManager, PrintService.SERVICE_META_DATA);
        if (parser != null) {
            for (int type = 0; type != 1 && type != 2; type = parser.next()) {
                try {
                    try {
                        try {
                            try {
                            } catch (IOException ioe) {
                                Log.w(LOG_TAG, "Error reading meta-data:" + ioe);
                                if (parser != null) {
                                    parser.close();
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(LOG_TAG, "Unable to load resources for: " + resolveInfo.serviceInfo.packageName);
                            if (parser != null) {
                                parser.close();
                            }
                        }
                    } catch (XmlPullParserException xppe) {
                        Log.w(LOG_TAG, "Error reading meta-data:" + xppe);
                        if (parser != null) {
                            parser.close();
                        }
                    }
                } finally {
                    if (parser != null) {
                        parser.close();
                    }
                }
            }
            String nodeName = parser.getName();
            if (TAG_PRINT_SERVICE.equals(nodeName)) {
                Resources resources = packageManager.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
                AttributeSet allAttributes = Xml.asAttributeSet(parser);
                TypedArray attributes = resources.obtainAttributes(allAttributes, R.styleable.PrintService);
                settingsActivityName = attributes.getString(0);
                addPrintersActivityName = attributes.getString(1);
                advancedPrintOptionsActivityName = attributes.getString(3);
                attributes.recycle();
            } else {
                Log.e(LOG_TAG, "Ignoring meta-data that does not start with print-service tag");
            }
        }
        return new PrintServiceInfo(resolveInfo, settingsActivityName, addPrintersActivityName, advancedPrintOptionsActivityName);
    }

    public String getId() {
        return this.mId;
    }

    public ResolveInfo getResolveInfo() {
        return this.mResolveInfo;
    }

    public String getSettingsActivityName() {
        return this.mSettingsActivityName;
    }

    public String getAddPrintersActivityName() {
        return this.mAddPrintersActivityName;
    }

    public String getAdvancedOptionsActivityName() {
        return this.mAdvancedPrintOptionsActivityName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flagz) {
        parcel.writeString(this.mId);
        parcel.writeParcelable(this.mResolveInfo, 0);
        parcel.writeString(this.mSettingsActivityName);
        parcel.writeString(this.mAddPrintersActivityName);
        parcel.writeString(this.mAdvancedPrintOptionsActivityName);
    }

    public int hashCode() {
        return (this.mId == null ? 0 : this.mId.hashCode()) + 31;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            PrintServiceInfo other = (PrintServiceInfo) obj;
            return this.mId == null ? other.mId == null : this.mId.equals(other.mId);
        }
        return false;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrintServiceInfo{");
        builder.append("id=").append(this.mId);
        builder.append(", resolveInfo=").append(this.mResolveInfo);
        builder.append(", settingsActivityName=").append(this.mSettingsActivityName);
        builder.append(", addPrintersActivityName=").append(this.mAddPrintersActivityName);
        builder.append(", advancedPrintOptionsActivityName=").append(this.mAdvancedPrintOptionsActivityName);
        builder.append("}");
        return builder.toString();
    }
}
