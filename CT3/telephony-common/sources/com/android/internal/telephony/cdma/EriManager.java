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
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
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
        String[] mCallPromptId = {UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET};
        HashMap<Integer, EriInfo> mRoamIndTable = new HashMap<>();

        EriFile() {
        }
    }

    class EriDisplayInformation {
        int mEriIconIndex;
        int mEriIconMode;
        String mEriIconText;

        EriDisplayInformation(int eriIconIndex, int eriIconMode, String eriIconText) {
            this.mEriIconIndex = eriIconIndex;
            this.mEriIconMode = eriIconMode;
            this.mEriIconText = eriIconText;
        }

        public String toString() {
            return "EriDisplayInformation: { IconIndex: " + this.mEriIconIndex + " EriIconMode: " + this.mEriIconMode + " EriIconText: " + this.mEriIconText + " }";
        }
    }

    public EriManager(Phone phone, Context context, int eriFileSource) {
        this.mEriFileSource = 0;
        this.mPhone = phone;
        this.mContext = context;
        this.mEriFileSource = eriFileSource;
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
        XmlPullParser parser;
        int parsedEriEntries;
        PersistableBundle b;
        FileInputStream stream = null;
        Resources r = this.mContext.getResources();
        try {
            Rlog.d(LOG_TAG, "loadEriFileFromXml: check for alternate file");
            FileInputStream stream2 = new FileInputStream(r.getString(R.string.fp_power_button_bp_positive_button));
            try {
                parser = Xml.newPullParser();
                parser.setInput(stream2, null);
                Rlog.d(LOG_TAG, "loadEriFileFromXml: opened alternate file");
                stream = stream2;
            } catch (FileNotFoundException e) {
                stream = stream2;
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no alternate file");
                parser = null;
            } catch (XmlPullParserException e2) {
                stream = stream2;
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no parser for alternate file");
                parser = null;
            }
        } catch (FileNotFoundException e3) {
        } catch (XmlPullParserException e4) {
        }
        if (parser == null) {
            String eriFile = null;
            CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
            if (configManager != null && (b = configManager.getConfigForSubId(this.mPhone.getSubId())) != null) {
                eriFile = b.getString("carrier_eri_file_name_string");
            }
            Rlog.d(LOG_TAG, "eriFile = " + eriFile);
            if (eriFile == null) {
                Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                return;
            }
            try {
                parser = Xml.newPullParser();
                parser.setInput(this.mContext.getAssets().open(eriFile), null);
            } catch (IOException | XmlPullParserException e5) {
                Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile + ". Exception = " + e5.toString());
            }
        }
        try {
            try {
                XmlUtils.beginDocument(parser, "EriFile");
                this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
                this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
                this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
                parsedEriEntries = 0;
            } catch (Exception e6) {
                Rlog.e(LOG_TAG, "Got exception while loading ERI file.", e6);
                if (parser instanceof XmlResourceParser) {
                    ((XmlResourceParser) parser).close();
                }
                if (stream != null) {
                    try {
                        stream.close();
                        return;
                    } catch (IOException e7) {
                        return;
                    }
                }
                return;
            }
        } catch (Throwable th) {
            if (parser instanceof XmlResourceParser) {
            }
            if (stream != null) {
            }
            throw th;
        }
        while (true) {
            XmlUtils.nextElement(parser);
            String name = parser.getName();
            if (name == null) {
                break;
            }
            if (name.equals("CallPromptId")) {
                int id = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                String text = parser.getAttributeValue(null, "CallPromptText");
                if (id < 0 || id > 2) {
                    Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id + " CallPromptId");
                } else {
                    this.mEriFile.mCallPromptId[id] = text;
                }
            } else if (name.equals("EriInfo")) {
                int roamingIndicator = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                int iconIndex = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                int iconMode = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                String eriText = parser.getAttributeValue(null, "EriText");
                int callPromptId = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                int alertId = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                parsedEriEntries++;
                this.mEriFile.mRoamIndTable.put(Integer.valueOf(roamingIndicator), new EriInfo(roamingIndicator, iconIndex, iconMode, eriText, callPromptId, alertId));
            }
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
        if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
            Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
        }
        Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
        this.mIsEriFileLoaded = true;
        if (parser instanceof XmlResourceParser) {
            ((XmlResourceParser) parser).close();
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e9) {
            }
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

    private EriInfo getEriInfo(int roamingIndicator) {
        if (this.mEriFile.mRoamIndTable.containsKey(Integer.valueOf(roamingIndicator))) {
            return this.mEriFile.mRoamIndTable.get(Integer.valueOf(roamingIndicator));
        }
        return null;
    }

    private EriDisplayInformation getEriDisplayInformation(int roamInd, int defRoamInd) {
        EriInfo eriInfo;
        if (this.mIsEriFileLoaded && (eriInfo = getEriInfo(roamInd)) != null) {
            EriDisplayInformation ret = new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
            return ret;
        }
        switch (roamInd) {
            case 0:
                EriDisplayInformation ret2 = new EriDisplayInformation(0, 0, this.mContext.getText(R.string.ThreeWCMmi).toString());
                return ret2;
            case 1:
                EriDisplayInformation ret3 = new EriDisplayInformation(1, 0, this.mContext.getText(R.string.accept).toString());
                return ret3;
            case 2:
                EriDisplayInformation ret4 = new EriDisplayInformation(2, 1, this.mContext.getText(R.string.accessibility_autoclick_double_click).toString());
                return ret4;
            case 3:
                EriDisplayInformation ret5 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_drag).toString());
                return ret5;
            case 4:
                EriDisplayInformation ret6 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_left_click).toString());
                return ret6;
            case 5:
                EriDisplayInformation ret7 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_pause).toString());
                return ret7;
            case 6:
                EriDisplayInformation ret8 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_position).toString());
                return ret8;
            case 7:
                EriDisplayInformation ret9 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_right_click).toString());
                return ret9;
            case 8:
                EriDisplayInformation ret10 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_scroll).toString());
                return ret10;
            case 9:
                EriDisplayInformation ret11 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_scroll_down).toString());
                return ret11;
            case 10:
                EriDisplayInformation ret12 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_scroll_exit).toString());
                return ret12;
            case 11:
                EriDisplayInformation ret13 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_scroll_left).toString());
                return ret13;
            case 12:
                EriDisplayInformation ret14 = new EriDisplayInformation(roamInd, 0, this.mContext.getText(R.string.accessibility_autoclick_scroll_panel_title).toString());
                return ret14;
            default:
                if (!this.mIsEriFileLoaded) {
                    Rlog.d(LOG_TAG, "ERI File not loaded");
                    if (defRoamInd > 2) {
                        EriDisplayInformation ret15 = new EriDisplayInformation(2, 1, this.mContext.getText(R.string.accessibility_autoclick_double_click).toString());
                        return ret15;
                    }
                    switch (defRoamInd) {
                        case 0:
                            EriDisplayInformation ret16 = new EriDisplayInformation(0, 0, this.mContext.getText(R.string.ThreeWCMmi).toString());
                            return ret16;
                        case 1:
                            EriDisplayInformation ret17 = new EriDisplayInformation(1, 0, this.mContext.getText(R.string.accept).toString());
                            return ret17;
                        case 2:
                            EriDisplayInformation ret18 = new EriDisplayInformation(2, 1, this.mContext.getText(R.string.accessibility_autoclick_double_click).toString());
                            return ret18;
                        default:
                            EriDisplayInformation ret19 = new EriDisplayInformation(-1, -1, "ERI text");
                            return ret19;
                    }
                }
                EriInfo eriInfo2 = getEriInfo(roamInd);
                EriInfo defEriInfo = getEriInfo(defRoamInd);
                if (eriInfo2 == null) {
                    if (defEriInfo == null) {
                        Rlog.e(LOG_TAG, "ERI defRoamInd " + defRoamInd + " not found in ERI file ...on");
                        EriDisplayInformation ret20 = new EriDisplayInformation(0, 0, this.mContext.getText(R.string.ThreeWCMmi).toString());
                        return ret20;
                    }
                    EriDisplayInformation ret21 = new EriDisplayInformation(defEriInfo.iconIndex, defEriInfo.iconMode, defEriInfo.eriText);
                    return ret21;
                }
                EriDisplayInformation ret22 = new EriDisplayInformation(eriInfo2.iconIndex, eriInfo2.iconMode, eriInfo2.eriText);
                return ret22;
        }
    }

    public int getCdmaEriIconIndex(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconIndex;
    }

    public int getCdmaEriIconMode(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconMode;
    }

    public String getCdmaEriText(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconText;
    }
}
