package com.android.internal.telephony.uicc;

import android.os.Environment;
import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VoiceMailConstants {
    static final String LOG_TAG = "VoiceMailConstants";
    static final int NAME = 0;
    static final int NUMBER = 1;
    static final String PARTNER_VOICEMAIL_PATH = "etc/voicemail-conf.xml";
    static final int SIZE = 3;
    static final int TAG = 2;
    private HashMap<String, String[]> CarrierVmMap = new HashMap<>();

    VoiceMailConstants() {
        loadVoiceMail();
    }

    public boolean containsCarrier(String str) {
        return this.CarrierVmMap.containsKey(str);
    }

    String getCarrierName(String str) {
        return this.CarrierVmMap.get(str)[0];
    }

    String getVoiceMailNumber(String str) {
        return this.CarrierVmMap.get(str)[1];
    }

    String getVoiceMailTag(String str) {
        return this.CarrierVmMap.get(str)[2];
    }

    private void loadVoiceMail() {
        try {
            try {
                FileReader fileReader = new FileReader(new File(Environment.getRootDirectory(), PARTNER_VOICEMAIL_PATH));
                try {
                    try {
                        try {
                            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                            xmlPullParserNewPullParser.setInput(fileReader);
                            XmlUtils.beginDocument(xmlPullParserNewPullParser, "voicemail");
                            while (true) {
                                XmlUtils.nextElement(xmlPullParserNewPullParser);
                                if (!"voicemail".equals(xmlPullParserNewPullParser.getName())) {
                                    break;
                                } else {
                                    this.CarrierVmMap.put(xmlPullParserNewPullParser.getAttributeValue(null, "numeric"), new String[]{xmlPullParserNewPullParser.getAttributeValue(null, "carrier"), xmlPullParserNewPullParser.getAttributeValue(null, "vmnumber"), xmlPullParserNewPullParser.getAttributeValue(null, "vmtag")});
                                }
                            }
                            fileReader.close();
                        } catch (XmlPullParserException e) {
                            Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e);
                            fileReader.close();
                        }
                    } catch (Throwable th) {
                        try {
                            fileReader.close();
                        } catch (IOException e2) {
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e3);
                    fileReader.close();
                }
            } catch (FileNotFoundException e4) {
                Rlog.w(LOG_TAG, "Can't open " + Environment.getRootDirectory() + "/" + PARTNER_VOICEMAIL_PATH);
            }
        } catch (IOException e5) {
        }
    }
}
