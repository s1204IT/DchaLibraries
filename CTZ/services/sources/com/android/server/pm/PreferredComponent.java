package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PreferredComponent {
    private static final String ATTR_ALWAYS = "always";
    private static final String ATTR_MATCH = "match";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SET = "set";
    private static final String TAG_SET = "set";
    public boolean mAlways;
    private final Callbacks mCallbacks;
    public final ComponentName mComponent;
    public final int mMatch;
    private String mParseError;
    final String[] mSetClasses;
    final String[] mSetComponents;
    final String[] mSetPackages;
    final String mShortComponent;

    public interface Callbacks {
        boolean onReadTag(String str, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException;
    }

    public PreferredComponent(Callbacks callbacks, int i, ComponentName[] componentNameArr, ComponentName componentName, boolean z) {
        this.mCallbacks = callbacks;
        this.mMatch = 268369920 & i;
        this.mComponent = componentName;
        this.mAlways = z;
        this.mShortComponent = componentName.flattenToShortString();
        this.mParseError = null;
        if (componentNameArr != null) {
            int length = componentNameArr.length;
            String[] strArr = new String[length];
            String[] strArr2 = new String[length];
            String[] strArr3 = new String[length];
            for (int i2 = 0; i2 < length; i2++) {
                ComponentName componentName2 = componentNameArr[i2];
                if (componentName2 == null) {
                    this.mSetPackages = null;
                    this.mSetClasses = null;
                    this.mSetComponents = null;
                    return;
                } else {
                    strArr[i2] = componentName2.getPackageName().intern();
                    strArr2[i2] = componentName2.getClassName().intern();
                    strArr3[i2] = componentName2.flattenToShortString();
                }
            }
            this.mSetPackages = strArr;
            this.mSetClasses = strArr2;
            this.mSetComponents = strArr3;
            return;
        }
        this.mSetPackages = null;
        this.mSetClasses = null;
        this.mSetComponents = null;
    }

    public PreferredComponent(Callbacks callbacks, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mCallbacks = callbacks;
        this.mShortComponent = xmlPullParser.getAttributeValue(null, "name");
        this.mComponent = ComponentName.unflattenFromString(this.mShortComponent);
        if (this.mComponent == null) {
            this.mParseError = "Bad activity name " + this.mShortComponent;
        }
        String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_MATCH);
        int i = 0;
        this.mMatch = attributeValue != null ? Integer.parseInt(attributeValue, 16) : 0;
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "set");
        int i2 = attributeValue2 != null ? Integer.parseInt(attributeValue2) : 0;
        String attributeValue3 = xmlPullParser.getAttributeValue(null, ATTR_ALWAYS);
        this.mAlways = attributeValue3 != null ? Boolean.parseBoolean(attributeValue3) : true;
        String[] strArr = i2 > 0 ? new String[i2] : null;
        String[] strArr2 = i2 > 0 ? new String[i2] : null;
        String[] strArr3 = i2 > 0 ? new String[i2] : null;
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                if (name.equals("set")) {
                    String attributeValue4 = xmlPullParser.getAttributeValue(null, "name");
                    if (attributeValue4 == null) {
                        if (this.mParseError == null) {
                            this.mParseError = "No name in set tag in preferred activity " + this.mShortComponent;
                        }
                    } else if (i >= i2) {
                        if (this.mParseError == null) {
                            this.mParseError = "Too many set tags in preferred activity " + this.mShortComponent;
                        }
                    } else {
                        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue4);
                        if (componentNameUnflattenFromString == null) {
                            if (this.mParseError == null) {
                                this.mParseError = "Bad set name " + attributeValue4 + " in preferred activity " + this.mShortComponent;
                            }
                        } else {
                            strArr[i] = componentNameUnflattenFromString.getPackageName();
                            strArr2[i] = componentNameUnflattenFromString.getClassName();
                            strArr3[i] = attributeValue4;
                            i++;
                        }
                    }
                    XmlUtils.skipCurrentTag(xmlPullParser);
                } else if (!this.mCallbacks.onReadTag(name, xmlPullParser)) {
                    Slog.w("PreferredComponent", "Unknown element: " + xmlPullParser.getName());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        if (i != i2 && this.mParseError == null) {
            this.mParseError = "Not enough set tags (expected " + i2 + " but found " + i + ") in " + this.mShortComponent;
        }
        this.mSetPackages = strArr;
        this.mSetClasses = strArr2;
        this.mSetComponents = strArr3;
    }

    public String getParseError() {
        return this.mParseError;
    }

    public void writeToXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
        int length = this.mSetClasses != null ? this.mSetClasses.length : 0;
        xmlSerializer.attribute(null, "name", this.mShortComponent);
        if (z) {
            if (this.mMatch != 0) {
                xmlSerializer.attribute(null, ATTR_MATCH, Integer.toHexString(this.mMatch));
            }
            xmlSerializer.attribute(null, ATTR_ALWAYS, Boolean.toString(this.mAlways));
            xmlSerializer.attribute(null, "set", Integer.toString(length));
            for (int i = 0; i < length; i++) {
                xmlSerializer.startTag(null, "set");
                xmlSerializer.attribute(null, "name", this.mSetComponents[i]);
                xmlSerializer.endTag(null, "set");
            }
        }
    }

    public boolean sameSet(List<ResolveInfo> list) {
        int i;
        boolean z;
        if (this.mSetPackages == null) {
            return list == null;
        }
        if (list == null) {
            return false;
        }
        int size = list.size();
        int length = this.mSetPackages.length;
        int i2 = 0;
        int i3 = 0;
        while (i2 < size) {
            ActivityInfo activityInfo = list.get(i2).activityInfo;
            int i4 = 0;
            while (true) {
                if (i4 < length) {
                    if (!this.mSetPackages[i4].equals(activityInfo.packageName) || !this.mSetClasses[i4].equals(activityInfo.name)) {
                        i4++;
                    } else {
                        i = i3 + 1;
                        z = true;
                        break;
                    }
                } else {
                    i = i3;
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
            i2++;
            i3 = i;
        }
        return i3 == length;
    }

    public boolean sameSet(ComponentName[] componentNameArr) {
        if (this.mSetPackages == null) {
            return false;
        }
        int length = componentNameArr.length;
        int length2 = this.mSetPackages.length;
        int i = 0;
        int i2 = 0;
        while (true) {
            boolean z = true;
            if (i >= length) {
                return i2 == length2;
            }
            ComponentName componentName = componentNameArr[i];
            int i3 = 0;
            while (true) {
                if (i3 < length2) {
                    if (!this.mSetPackages[i3].equals(componentName.getPackageName()) || !this.mSetClasses[i3].equals(componentName.getClassName())) {
                        i3++;
                    } else {
                        i2++;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
            i++;
        }
    }

    public boolean isSuperset(List<ResolveInfo> list) {
        boolean z;
        if (this.mSetPackages == null) {
            return list == null;
        }
        if (list == null) {
            return true;
        }
        int size = list.size();
        int length = this.mSetPackages.length;
        if (length < size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            ActivityInfo activityInfo = list.get(i).activityInfo;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    if (!this.mSetPackages[i2].equals(activityInfo.packageName) || !this.mSetClasses[i2].equals(activityInfo.name)) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
        }
        return true;
    }

    public ComponentName[] discardObsoleteComponents(List<ResolveInfo> list) {
        if (this.mSetPackages == null || list == null) {
            return new ComponentName[0];
        }
        int size = list.size();
        int length = this.mSetPackages.length;
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < size; i++) {
            ActivityInfo activityInfo = list.get(i).activityInfo;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                if (!this.mSetPackages[i2].equals(activityInfo.packageName) || !this.mSetClasses[i2].equals(activityInfo.name)) {
                    i2++;
                } else {
                    arrayList.add(new ComponentName(this.mSetPackages[i2], this.mSetClasses[i2]));
                    break;
                }
            }
        }
        return (ComponentName[]) arrayList.toArray(new ComponentName[arrayList.size()]);
    }

    public void dump(PrintWriter printWriter, String str, Object obj) {
        printWriter.print(str);
        printWriter.print(Integer.toHexString(System.identityHashCode(obj)));
        printWriter.print(' ');
        printWriter.println(this.mShortComponent);
        printWriter.print(str);
        printWriter.print(" mMatch=0x");
        printWriter.print(Integer.toHexString(this.mMatch));
        printWriter.print(" mAlways=");
        printWriter.println(this.mAlways);
        if (this.mSetComponents != null) {
            printWriter.print(str);
            printWriter.println("  Selected from:");
            for (int i = 0; i < this.mSetComponents.length; i++) {
                printWriter.print(str);
                printWriter.print("    ");
                printWriter.println(this.mSetComponents[i]);
            }
        }
    }
}
