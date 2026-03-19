package android.printservice;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

@SystemApi
public final class PrintServiceInfo implements Parcelable {
    private static final String TAG_PRINT_SERVICE = "print-service";
    private final String mAddPrintersActivityName;
    private final String mAdvancedPrintOptionsActivityName;
    private final String mId;
    private boolean mIsEnabled;
    private final ResolveInfo mResolveInfo;
    private final String mSettingsActivityName;
    private static final String LOG_TAG = PrintServiceInfo.class.getSimpleName();
    public static final Parcelable.Creator<PrintServiceInfo> CREATOR = new Parcelable.Creator<PrintServiceInfo>() {
        @Override
        public PrintServiceInfo createFromParcel(Parcel parcel) {
            return new PrintServiceInfo(parcel);
        }

        @Override
        public PrintServiceInfo[] newArray(int i) {
            return new PrintServiceInfo[i];
        }
    };

    public PrintServiceInfo(Parcel parcel) {
        this.mId = parcel.readString();
        this.mIsEnabled = parcel.readByte() != 0;
        this.mResolveInfo = (ResolveInfo) parcel.readParcelable(null);
        this.mSettingsActivityName = parcel.readString();
        this.mAddPrintersActivityName = parcel.readString();
        this.mAdvancedPrintOptionsActivityName = parcel.readString();
    }

    public PrintServiceInfo(ResolveInfo resolveInfo, String str, String str2, String str3) {
        this.mId = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToString();
        this.mResolveInfo = resolveInfo;
        this.mSettingsActivityName = str;
        this.mAddPrintersActivityName = str2;
        this.mAdvancedPrintOptionsActivityName = str3;
    }

    public ComponentName getComponentName() {
        return new ComponentName(this.mResolveInfo.serviceInfo.packageName, this.mResolveInfo.serviceInfo.name);
    }

    public static PrintServiceInfo create(Context context, ResolveInfo resolveInfo) {
        String string;
        String string2;
        PackageManager packageManager = context.getPackageManager();
        XmlResourceParser xmlResourceParserLoadXmlMetaData = resolveInfo.serviceInfo.loadXmlMetaData(packageManager, PrintService.SERVICE_META_DATA);
        String str = null;
        if (xmlResourceParserLoadXmlMetaData != null) {
            for (int next = 0; next != 1 && next != 2; next = xmlResourceParserLoadXmlMetaData.next()) {
                try {
                    try {
                    } finally {
                        if (xmlResourceParserLoadXmlMetaData != null) {
                            xmlResourceParserLoadXmlMetaData.close();
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    string = null;
                    string2 = null;
                    Log.e(LOG_TAG, "Unable to load resources for: " + resolveInfo.serviceInfo.packageName);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                    }
                    return new PrintServiceInfo(resolveInfo, str, string, string2);
                } catch (IOException e2) {
                    e = e2;
                    string = null;
                    string2 = null;
                    Log.w(LOG_TAG, "Error reading meta-data:" + e);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                    }
                    return new PrintServiceInfo(resolveInfo, str, string, string2);
                } catch (XmlPullParserException e3) {
                    e = e3;
                    string = null;
                    string2 = null;
                    Log.w(LOG_TAG, "Error reading meta-data:" + e);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                    }
                    return new PrintServiceInfo(resolveInfo, str, string, string2);
                }
            }
            if (TAG_PRINT_SERVICE.equals(xmlResourceParserLoadXmlMetaData.getName())) {
                TypedArray typedArrayObtainAttributes = packageManager.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo).obtainAttributes(Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData), R.styleable.PrintService);
                String string3 = typedArrayObtainAttributes.getString(0);
                try {
                    string = typedArrayObtainAttributes.getString(1);
                    try {
                        string2 = typedArrayObtainAttributes.getString(3);
                    } catch (PackageManager.NameNotFoundException e4) {
                        string2 = null;
                    } catch (IOException e5) {
                        e = e5;
                        string2 = null;
                    } catch (XmlPullParserException e6) {
                        e = e6;
                        string2 = null;
                    }
                    try {
                        typedArrayObtainAttributes.recycle();
                        str = string3;
                    } catch (PackageManager.NameNotFoundException e7) {
                        str = string3;
                        Log.e(LOG_TAG, "Unable to load resources for: " + resolveInfo.serviceInfo.packageName);
                        if (xmlResourceParserLoadXmlMetaData != null) {
                        }
                        return new PrintServiceInfo(resolveInfo, str, string, string2);
                    } catch (IOException e8) {
                        e = e8;
                        str = string3;
                        Log.w(LOG_TAG, "Error reading meta-data:" + e);
                        if (xmlResourceParserLoadXmlMetaData != null) {
                        }
                        return new PrintServiceInfo(resolveInfo, str, string, string2);
                    } catch (XmlPullParserException e9) {
                        e = e9;
                        str = string3;
                        Log.w(LOG_TAG, "Error reading meta-data:" + e);
                        if (xmlResourceParserLoadXmlMetaData != null) {
                        }
                        return new PrintServiceInfo(resolveInfo, str, string, string2);
                    }
                } catch (PackageManager.NameNotFoundException e10) {
                    string = null;
                    string2 = null;
                } catch (IOException e11) {
                    e = e11;
                    string = null;
                    string2 = null;
                } catch (XmlPullParserException e12) {
                    e = e12;
                    string = null;
                    string2 = null;
                }
            } else {
                Log.e(LOG_TAG, "Ignoring meta-data that does not start with print-service tag");
                string = null;
                string2 = null;
            }
        } else {
            string = null;
            string2 = null;
        }
        return new PrintServiceInfo(resolveInfo, str, string, string2);
    }

    public String getId() {
        return this.mId;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public void setIsEnabled(boolean z) {
        this.mIsEnabled = z;
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mId);
        parcel.writeByte(this.mIsEnabled ? (byte) 1 : (byte) 0);
        parcel.writeParcelable(this.mResolveInfo, 0);
        parcel.writeString(this.mSettingsActivityName);
        parcel.writeString(this.mAddPrintersActivityName);
        parcel.writeString(this.mAdvancedPrintOptionsActivityName);
    }

    public int hashCode() {
        return 31 + (this.mId == null ? 0 : this.mId.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrintServiceInfo printServiceInfo = (PrintServiceInfo) obj;
        if (this.mId == null) {
            if (printServiceInfo.mId != null) {
                return false;
            }
        } else if (!this.mId.equals(printServiceInfo.mId)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "PrintServiceInfo{id=" + this.mId + "isEnabled=" + this.mIsEnabled + ", resolveInfo=" + this.mResolveInfo + ", settingsActivityName=" + this.mSettingsActivityName + ", addPrintersActivityName=" + this.mAddPrintersActivityName + ", advancedPrintOptionsActivityName=" + this.mAdvancedPrintOptionsActivityName + "}";
    }
}
