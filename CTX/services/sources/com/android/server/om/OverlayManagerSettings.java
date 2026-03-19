package com.android.server.om;

import android.content.om.OverlayInfo;
import android.util.ArrayMap;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.om.OverlayManagerSettings;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class OverlayManagerSettings {
    private final ArrayList<SettingsItem> mItems = new ArrayList<>();

    public static ArrayMap m15lambda$bXuJGR0fITXNwGnQfQHv9KSXgY() {
        return new ArrayMap();
    }

    OverlayManagerSettings() {
    }

    void init(String str, int i, String str2, String str3, boolean z, int i2, String str4) {
        remove(str, i);
        SettingsItem settingsItem = new SettingsItem(str, i, str2, str3, z, i2, str4);
        if (!z) {
            this.mItems.add(settingsItem);
            return;
        }
        settingsItem.setEnabled(true);
        int size = this.mItems.size() - 1;
        while (size >= 0) {
            SettingsItem settingsItem2 = this.mItems.get(size);
            if (settingsItem2.mIsStatic && settingsItem2.mPriority <= i2) {
                break;
            }
            size--;
        }
        int i3 = size + 1;
        if (i3 == this.mItems.size()) {
            this.mItems.add(settingsItem);
        } else {
            this.mItems.add(i3, settingsItem);
        }
    }

    boolean remove(String str, int i) {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            return false;
        }
        this.mItems.remove(iSelect);
        return true;
    }

    OverlayInfo getOverlayInfo(String str, int i) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).getOverlayInfo();
    }

    boolean setBaseCodePath(String str, int i, String str2) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).setBaseCodePath(str2);
    }

    boolean setCategory(String str, int i, String str2) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).setCategory(str2);
    }

    boolean getEnabled(String str, int i) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).isEnabled();
    }

    boolean setEnabled(String str, int i, boolean z) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).setEnabled(z);
    }

    int getState(String str, int i) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).getState();
    }

    boolean setState(String str, int i, int i2) throws BadKeyException {
        int iSelect = select(str, i);
        if (iSelect < 0) {
            throw new BadKeyException(str, i);
        }
        return this.mItems.get(iSelect).setState(i2);
    }

    List<OverlayInfo> getOverlaysForTarget(String str, int i) {
        return (List) selectWhereTarget(str, i).filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$getOverlaysForTarget$0((OverlayManagerSettings.SettingsItem) obj);
            }
        }).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((OverlayManagerSettings.SettingsItem) obj).getOverlayInfo();
            }
        }).collect(Collectors.toList());
    }

    static boolean lambda$getOverlaysForTarget$0(SettingsItem settingsItem) {
        return (settingsItem.isStatic() && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(settingsItem.getTargetPackageName())) ? false : true;
    }

    ArrayMap<String, List<OverlayInfo>> getOverlaysForUser(int i) {
        return (ArrayMap) selectWhereUser(i).filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$getOverlaysForUser$2((OverlayManagerSettings.SettingsItem) obj);
            }
        }).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((OverlayManagerSettings.SettingsItem) obj).getOverlayInfo();
            }
        }).collect(Collectors.groupingBy(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((OverlayInfo) obj).targetPackageName;
            }
        }, new Supplier() {
            @Override
            public final Object get() {
                return OverlayManagerSettings.m15lambda$bXuJGR0fITXNwGnQfQHv9KSXgY();
            }
        }, Collectors.toList()));
    }

    static boolean lambda$getOverlaysForUser$2(SettingsItem settingsItem) {
        return (settingsItem.isStatic() && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(settingsItem.getTargetPackageName())) ? false : true;
    }

    int[] getUsers() {
        return this.mItems.stream().mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((OverlayManagerSettings.SettingsItem) obj).getUserId();
            }
        }).distinct().toArray();
    }

    boolean removeUser(int i) {
        int i2 = 0;
        boolean z = false;
        while (i2 < this.mItems.size()) {
            if (this.mItems.get(i2).getUserId() == i) {
                this.mItems.remove(i2);
                i2--;
                z = true;
            }
            i2++;
        }
        return z;
    }

    boolean setPriority(String str, String str2, int i) {
        int iSelect;
        int iSelect2;
        if (str.equals(str2) || (iSelect = select(str, i)) < 0 || (iSelect2 = select(str2, i)) < 0) {
            return false;
        }
        SettingsItem settingsItem = this.mItems.get(iSelect);
        if (!settingsItem.getTargetPackageName().equals(this.mItems.get(iSelect2).getTargetPackageName())) {
            return false;
        }
        this.mItems.remove(iSelect);
        int iSelect3 = select(str2, i) + 1;
        this.mItems.add(iSelect3, settingsItem);
        return iSelect != iSelect3;
    }

    boolean setLowestPriority(String str, int i) {
        int iSelect = select(str, i);
        if (iSelect <= 0) {
            return false;
        }
        SettingsItem settingsItem = this.mItems.get(iSelect);
        this.mItems.remove(settingsItem);
        this.mItems.add(0, settingsItem);
        return true;
    }

    boolean setHighestPriority(String str, int i) {
        int iSelect = select(str, i);
        if (iSelect < 0 || iSelect == this.mItems.size() - 1) {
            return false;
        }
        SettingsItem settingsItem = this.mItems.get(iSelect);
        this.mItems.remove(iSelect);
        this.mItems.add(settingsItem);
        return true;
    }

    void dump(PrintWriter printWriter) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("Settings");
        indentingPrintWriter.increaseIndent();
        if (this.mItems.isEmpty()) {
            indentingPrintWriter.println("<none>");
            return;
        }
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            SettingsItem settingsItem = this.mItems.get(i);
            indentingPrintWriter.println(settingsItem.mPackageName + ":" + settingsItem.getUserId() + " {");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.print("mPackageName.......: ");
            indentingPrintWriter.println(settingsItem.mPackageName);
            indentingPrintWriter.print("mUserId............: ");
            indentingPrintWriter.println(settingsItem.getUserId());
            indentingPrintWriter.print("mTargetPackageName.: ");
            indentingPrintWriter.println(settingsItem.getTargetPackageName());
            indentingPrintWriter.print("mBaseCodePath......: ");
            indentingPrintWriter.println(settingsItem.getBaseCodePath());
            indentingPrintWriter.print("mState.............: ");
            indentingPrintWriter.println(OverlayInfo.stateToString(settingsItem.getState()));
            indentingPrintWriter.print("mIsEnabled.........: ");
            indentingPrintWriter.println(settingsItem.isEnabled());
            indentingPrintWriter.print("mIsStatic..........: ");
            indentingPrintWriter.println(settingsItem.isStatic());
            indentingPrintWriter.print("mPriority..........: ");
            indentingPrintWriter.println(settingsItem.mPriority);
            indentingPrintWriter.print("mCategory..........: ");
            indentingPrintWriter.println(settingsItem.mCategory);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("}");
        }
    }

    void restore(InputStream inputStream) throws XmlPullParserException, IOException {
        Serializer.restore(this.mItems, inputStream);
    }

    void persist(OutputStream outputStream) throws XmlPullParserException, IOException {
        Serializer.persist(this.mItems, outputStream);
    }

    private static final class Serializer {
        private static final String ATTR_BASE_CODE_PATH = "baseCodePath";
        private static final String ATTR_CATEGORY = "category";
        private static final String ATTR_IS_ENABLED = "isEnabled";
        private static final String ATTR_IS_STATIC = "isStatic";
        private static final String ATTR_PACKAGE_NAME = "packageName";
        private static final String ATTR_PRIORITY = "priority";
        private static final String ATTR_STATE = "state";
        private static final String ATTR_TARGET_PACKAGE_NAME = "targetPackageName";
        private static final String ATTR_USER_ID = "userId";
        private static final String ATTR_VERSION = "version";
        private static final int CURRENT_VERSION = 3;
        private static final String TAG_ITEM = "item";
        private static final String TAG_OVERLAYS = "overlays";

        private Serializer() {
        }

        public static void restore(ArrayList<SettingsItem> arrayList, InputStream inputStream) throws XmlPullParserException, IOException {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            Throwable th = null;
            try {
                arrayList.clear();
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(inputStreamReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, TAG_OVERLAYS);
                int intAttribute = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_VERSION);
                if (intAttribute != 3) {
                    upgrade(intAttribute);
                }
                int depth = xmlPullParserNewPullParser.getDepth();
                while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth)) {
                    String name = xmlPullParserNewPullParser.getName();
                    byte b = -1;
                    if (name.hashCode() == 3242771 && name.equals("item")) {
                        b = 0;
                    }
                    if (b == 0) {
                        arrayList.add(restoreRow(xmlPullParserNewPullParser, depth + 1));
                    }
                }
                inputStreamReader.close();
            } catch (Throwable th2) {
                if (0 != 0) {
                    try {
                        inputStreamReader.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    inputStreamReader.close();
                }
                throw th2;
            }
        }

        private static void upgrade(int i) throws XmlPullParserException {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    throw new XmlPullParserException("old version " + i + "; ignoring");
                default:
                    throw new XmlPullParserException("unrecognized version " + i);
            }
        }

        private static SettingsItem restoreRow(XmlPullParser xmlPullParser, int i) throws IOException {
            return new SettingsItem(XmlUtils.readStringAttribute(xmlPullParser, "packageName"), XmlUtils.readIntAttribute(xmlPullParser, ATTR_USER_ID), XmlUtils.readStringAttribute(xmlPullParser, ATTR_TARGET_PACKAGE_NAME), XmlUtils.readStringAttribute(xmlPullParser, ATTR_BASE_CODE_PATH), XmlUtils.readIntAttribute(xmlPullParser, "state"), XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_IS_ENABLED), XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_IS_STATIC), XmlUtils.readIntAttribute(xmlPullParser, ATTR_PRIORITY), XmlUtils.readStringAttribute(xmlPullParser, ATTR_CATEGORY));
        }

        public static void persist(ArrayList<SettingsItem> arrayList, OutputStream outputStream) throws XmlPullParserException, IOException {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outputStream, "utf-8");
            fastXmlSerializer.startDocument((String) null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag((String) null, TAG_OVERLAYS);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 3);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                persistRow(fastXmlSerializer, arrayList.get(i));
            }
            fastXmlSerializer.endTag((String) null, TAG_OVERLAYS);
            fastXmlSerializer.endDocument();
        }

        private static void persistRow(FastXmlSerializer fastXmlSerializer, SettingsItem settingsItem) throws IOException {
            fastXmlSerializer.startTag((String) null, "item");
            XmlUtils.writeStringAttribute(fastXmlSerializer, "packageName", settingsItem.mPackageName);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_USER_ID, settingsItem.mUserId);
            XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_TARGET_PACKAGE_NAME, settingsItem.mTargetPackageName);
            XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_BASE_CODE_PATH, settingsItem.mBaseCodePath);
            XmlUtils.writeIntAttribute(fastXmlSerializer, "state", settingsItem.mState);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_IS_ENABLED, settingsItem.mIsEnabled);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_IS_STATIC, settingsItem.mIsStatic);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_PRIORITY, settingsItem.mPriority);
            XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_CATEGORY, settingsItem.mCategory);
            fastXmlSerializer.endTag((String) null, "item");
        }
    }

    private static final class SettingsItem {
        private String mBaseCodePath;
        private OverlayInfo mCache;
        private String mCategory;
        private boolean mIsEnabled;
        private boolean mIsStatic;
        private final String mPackageName;
        private int mPriority;
        private int mState;
        private final String mTargetPackageName;
        private final int mUserId;

        SettingsItem(String str, int i, String str2, String str3, int i2, boolean z, boolean z2, int i3, String str4) {
            this.mPackageName = str;
            this.mUserId = i;
            this.mTargetPackageName = str2;
            this.mBaseCodePath = str3;
            this.mState = i2;
            this.mIsEnabled = z || z2;
            this.mCategory = str4;
            this.mCache = null;
            this.mIsStatic = z2;
            this.mPriority = i3;
        }

        SettingsItem(String str, int i, String str2, String str3, boolean z, int i2, String str4) {
            this(str, i, str2, str3, -1, false, z, i2, str4);
        }

        private String getTargetPackageName() {
            return this.mTargetPackageName;
        }

        private int getUserId() {
            return this.mUserId;
        }

        private String getBaseCodePath() {
            return this.mBaseCodePath;
        }

        private boolean setBaseCodePath(String str) {
            if (!this.mBaseCodePath.equals(str)) {
                this.mBaseCodePath = str;
                invalidateCache();
                return true;
            }
            return false;
        }

        private int getState() {
            return this.mState;
        }

        private boolean setState(int i) {
            if (this.mState != i) {
                this.mState = i;
                invalidateCache();
                return true;
            }
            return false;
        }

        private boolean isEnabled() {
            return this.mIsEnabled;
        }

        private boolean setEnabled(boolean z) {
            if (this.mIsStatic || this.mIsEnabled == z) {
                return false;
            }
            this.mIsEnabled = z;
            invalidateCache();
            return true;
        }

        private boolean setCategory(String str) {
            if (!Objects.equals(this.mCategory, str)) {
                this.mCategory = str.intern();
                invalidateCache();
                return true;
            }
            return false;
        }

        private OverlayInfo getOverlayInfo() {
            if (this.mCache == null) {
                this.mCache = new OverlayInfo(this.mPackageName, this.mTargetPackageName, this.mCategory, this.mBaseCodePath, this.mState, this.mUserId, this.mPriority, this.mIsStatic);
            }
            return this.mCache;
        }

        private void invalidateCache() {
            this.mCache = null;
        }

        private boolean isStatic() {
            return this.mIsStatic;
        }

        private int getPriority() {
            return this.mPriority;
        }
    }

    private int select(String str, int i) {
        int size = this.mItems.size();
        for (int i2 = 0; i2 < size; i2++) {
            SettingsItem settingsItem = this.mItems.get(i2);
            if (settingsItem.mUserId == i && settingsItem.mPackageName.equals(str)) {
                return i2;
            }
        }
        return -1;
    }

    static boolean lambda$selectWhereUser$6(int i, SettingsItem settingsItem) {
        return settingsItem.mUserId == i;
    }

    private Stream<SettingsItem> selectWhereUser(final int i) {
        return this.mItems.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$selectWhereUser$6(i, (OverlayManagerSettings.SettingsItem) obj);
            }
        });
    }

    private Stream<SettingsItem> selectWhereTarget(final String str, int i) {
        return selectWhereUser(i).filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((OverlayManagerSettings.SettingsItem) obj).getTargetPackageName().equals(str);
            }
        });
    }

    static final class BadKeyException extends RuntimeException {
        BadKeyException(String str, int i) {
            super("Bad key mPackageName=" + str + " mUserId=" + i);
        }
    }
}
