package com.android.internal.telephony.cdma;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.telephony.Phone;
import com.android.internal.util.XmlUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class EriManager {
    private static final boolean DBG = true;
    static final int ERI_FROM_FILE_SYSTEM = 1;
    static final int ERI_FROM_MODEM = 2;
    public static final int ERI_FROM_XML = 0;
    private static final String LOG_TAG = "EriManager";
    private static final boolean VDBG = false;
    private Context mContext;
    private EriFile mEriFile = new EriFile();
    private int mEriFileSource;
    private boolean mIsEriFileLoaded;
    private final Phone mPhone;

    class EriFile {
        int mVersionNumber = -1;
        int mNumberOfEriEntries = 0;
        int mEriFileType = -1;
        String[] mCallPromptId = {"", "", ""};
        HashMap<Integer, EriInfo> mRoamIndTable = new HashMap<>();

        EriFile() {
        }
    }

    class EriDisplayInformation {
        int mEriIconIndex;
        int mEriIconMode;
        String mEriIconText;

        EriDisplayInformation(int i, int i2, String str) {
            this.mEriIconIndex = i;
            this.mEriIconMode = i2;
            this.mEriIconText = str;
        }

        public String toString() {
            return "EriDisplayInformation: { IconIndex: " + this.mEriIconIndex + " EriIconMode: " + this.mEriIconMode + " EriIconText: " + this.mEriIconText + " }";
        }
    }

    public EriManager(Phone phone, Context context, int i) {
        this.mEriFileSource = 0;
        this.mPhone = phone;
        this.mContext = context;
        this.mEriFileSource = i;
    }

    public void dispose() {
        this.mEriFile = new EriFile();
        this.mIsEriFileLoaded = false;
    }

    public void loadEriFile() {
        switch (this.mEriFileSource) {
            case 1:
                loadEriFileFromFileSystem();
                break;
            case 2:
                loadEriFileFromModem();
                break;
            default:
                loadEriFileFromXml();
                break;
        }
    }

    private void loadEriFileFromModem() {
    }

    private void loadEriFileFromFileSystem() {
    }

    private void loadEriFileFromXml() {
        FileInputStream fileInputStream;
        XmlPullParser xmlPullParserNewPullParser;
        XmlPullParser xmlPullParserNewPullParser2;
        int i;
        Exception e;
        PersistableBundle configForSubId;
        Resources resources = this.mContext.getResources();
        try {
            Rlog.d(LOG_TAG, "loadEriFileFromXml: check for alternate file");
            fileInputStream = new FileInputStream(resources.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_ENTRY));
            try {
                xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, null);
                Rlog.d(LOG_TAG, "loadEriFileFromXml: opened alternate file");
            } catch (FileNotFoundException e2) {
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no alternate file");
                xmlPullParserNewPullParser = null;
            } catch (XmlPullParserException e3) {
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no parser for alternate file");
                xmlPullParserNewPullParser = null;
            }
        } catch (FileNotFoundException e4) {
            fileInputStream = null;
        } catch (XmlPullParserException e5) {
            fileInputStream = null;
        }
        if (xmlPullParserNewPullParser == null) {
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
            String string = (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) == null) ? null : configForSubId.getString("carrier_eri_file_name_string");
            Rlog.d(LOG_TAG, "eriFile = " + string);
            if (string == null) {
                Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                return;
            }
            try {
                xmlPullParserNewPullParser2 = Xml.newPullParser();
                try {
                    xmlPullParserNewPullParser2.setInput(this.mContext.getAssets().open(string), null);
                } catch (IOException | XmlPullParserException e6) {
                    e = e6;
                    Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + string + ". Exception = " + e.toString());
                }
            } catch (IOException | XmlPullParserException e7) {
                xmlPullParserNewPullParser2 = xmlPullParserNewPullParser;
                e = e7;
            }
        } else {
            xmlPullParserNewPullParser2 = xmlPullParserNewPullParser;
        }
        try {
            try {
                try {
                    XmlUtils.beginDocument(xmlPullParserNewPullParser2, "EriFile");
                    this.mEriFile.mVersionNumber = Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "VersionNumber"));
                    this.mEriFile.mNumberOfEriEntries = Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "NumberOfEriEntries"));
                    this.mEriFile.mEriFileType = Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "EriFileType"));
                    i = 0;
                } catch (Exception e8) {
                    Rlog.e(LOG_TAG, "Got exception while loading ERI file.", e8);
                    if (xmlPullParserNewPullParser2 instanceof XmlResourceParser) {
                        ((XmlResourceParser) xmlPullParserNewPullParser2).close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                        return;
                    }
                    return;
                }
            } catch (IOException e9) {
                return;
            }
        } catch (Throwable th) {
            if (xmlPullParserNewPullParser2 instanceof XmlResourceParser) {
            }
            if (fileInputStream != null) {
            }
            throw th;
        }
        while (true) {
            XmlUtils.nextElement(xmlPullParserNewPullParser2);
            String name = xmlPullParserNewPullParser2.getName();
            if (name == null) {
                break;
            }
            if (name.equals("CallPromptId")) {
                int i2 = Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "Id"));
                String attributeValue = xmlPullParserNewPullParser2.getAttributeValue(null, "CallPromptText");
                if (i2 < 0 || i2 > 2) {
                    Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + i2 + " CallPromptId");
                } else {
                    this.mEriFile.mCallPromptId[i2] = attributeValue;
                }
            } else if (name.equals("EriInfo")) {
                int i3 = Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "RoamingIndicator"));
                i++;
                this.mEriFile.mRoamIndTable.put(Integer.valueOf(i3), new EriInfo(i3, Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "IconIndex")), Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "IconMode")), xmlPullParserNewPullParser2.getAttributeValue(null, "EriText"), Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "CallPromptId")), Integer.parseInt(xmlPullParserNewPullParser2.getAttributeValue(null, "AlertId"))));
            }
            if (xmlPullParserNewPullParser2 instanceof XmlResourceParser) {
                ((XmlResourceParser) xmlPullParserNewPullParser2).close();
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e10) {
                }
            }
            throw th;
        }
        if (i != this.mEriFile.mNumberOfEriEntries) {
            Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + i + " parsed!");
        }
        Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
        this.mIsEriFileLoaded = true;
        if (xmlPullParserNewPullParser2 instanceof XmlResourceParser) {
            ((XmlResourceParser) xmlPullParserNewPullParser2).close();
        }
        if (fileInputStream != null) {
            fileInputStream.close();
        }
    }

    public int getEriFileVersion() {
        return this.mEriFile.mVersionNumber;
    }

    public int getEriNumberOfEntries() {
        return this.mEriFile.mNumberOfEriEntries;
    }

    public int getEriFileType() {
        return this.mEriFile.mEriFileType;
    }

    public boolean isEriFileLoaded() {
        return this.mIsEriFileLoaded;
    }

    private EriInfo getEriInfo(int i) {
        if (this.mEriFile.mRoamIndTable.containsKey(Integer.valueOf(i))) {
            return this.mEriFile.mRoamIndTable.get(Integer.valueOf(i));
        }
        return null;
    }

    private EriDisplayInformation getEriDisplayInformation(int i, int i2) {
        EriInfo eriInfo;
        if (this.mIsEriFileLoaded && (eriInfo = getEriInfo(i)) != null) {
            return new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
        }
        switch (i) {
            case 0:
                return new EriDisplayInformation(0, 0, this.mContext.getText(R.string.loading).toString());
            case 1:
                return new EriDisplayInformation(1, 0, this.mContext.getText(R.string.locale_replacement).toString());
            case 2:
                return new EriDisplayInformation(2, 1, this.mContext.getText(R.string.location_service).toString());
            case 3:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lock_pattern_view_aspect).toString());
            case 4:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lock_to_app_unlock_password).toString());
            case 5:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lock_to_app_unlock_pattern).toString());
            case 6:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lock_to_app_unlock_pin).toString());
            case 7:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lockscreen_access_pattern_area).toString());
            case 8:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lockscreen_access_pattern_cell_added).toString());
            case 9:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.lockscreen_access_pattern_cell_added_verbose).toString());
            case 10:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.locale_search_menu).toString());
            case 11:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.location_changed_notification_text).toString());
            case 12:
                return new EriDisplayInformation(i, 0, this.mContext.getText(R.string.location_changed_notification_title).toString());
            default:
                if (!this.mIsEriFileLoaded) {
                    Rlog.d(LOG_TAG, "ERI File not loaded");
                    if (i2 > 2) {
                        return new EriDisplayInformation(2, 1, this.mContext.getText(R.string.location_service).toString());
                    }
                    switch (i2) {
                        case 0:
                            return new EriDisplayInformation(0, 0, this.mContext.getText(R.string.loading).toString());
                        case 1:
                            return new EriDisplayInformation(1, 0, this.mContext.getText(R.string.locale_replacement).toString());
                        case 2:
                            return new EriDisplayInformation(2, 1, this.mContext.getText(R.string.location_service).toString());
                        default:
                            return new EriDisplayInformation(-1, -1, "ERI text");
                    }
                }
                EriInfo eriInfo2 = getEriInfo(i);
                EriInfo eriInfo3 = getEriInfo(i2);
                if (eriInfo2 == null) {
                    if (eriInfo3 == null) {
                        Rlog.e(LOG_TAG, "ERI defRoamInd " + i2 + " not found in ERI file ...on");
                        return new EriDisplayInformation(0, 0, this.mContext.getText(R.string.loading).toString());
                    }
                    return new EriDisplayInformation(eriInfo3.iconIndex, eriInfo3.iconMode, eriInfo3.eriText);
                }
                return new EriDisplayInformation(eriInfo2.iconIndex, eriInfo2.iconMode, eriInfo2.eriText);
        }
    }

    public int getCdmaEriIconIndex(int i, int i2) {
        return getEriDisplayInformation(i, i2).mEriIconIndex;
    }

    public int getCdmaEriIconMode(int i, int i2) {
        return getEriDisplayInformation(i, i2).mEriIconMode;
    }

    public String getCdmaEriText(int i, int i2) {
        return getEriDisplayInformation(i, i2).mEriIconText;
    }
}
