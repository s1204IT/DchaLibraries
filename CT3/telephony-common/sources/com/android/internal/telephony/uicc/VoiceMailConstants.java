package com.android.internal.telephony.uicc;

import android.os.Environment;
import android.provider.Telephony;
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

class VoiceMailConstants {
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

    boolean containsCarrier(String carrier) {
        return this.CarrierVmMap.containsKey(carrier);
    }

    String getCarrierName(String carrier) {
        String[] data = this.CarrierVmMap.get(carrier);
        return data[0];
    }

    String getVoiceMailNumber(String carrier) {
        String[] data = this.CarrierVmMap.get(carrier);
        return data[1];
    }

    String getVoiceMailTag(String carrier) {
        String[] data = this.CarrierVmMap.get(carrier);
        return data[2];
    }

    private void loadVoiceMail() {
        File vmFile = new File(Environment.getRootDirectory(), PARTNER_VOICEMAIL_PATH);
        try {
            FileReader vmReader = new FileReader(vmFile);
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(vmReader);
                        XmlUtils.beginDocument(parser, "voicemail");
                        while (true) {
                            XmlUtils.nextElement(parser);
                            String name = parser.getName();
                            if (!"voicemail".equals(name)) {
                                break;
                            }
                            String numeric = parser.getAttributeValue(null, Telephony.Carriers.NUMERIC);
                            String[] data = {parser.getAttributeValue(null, "carrier"), parser.getAttributeValue(null, "vmnumber"), parser.getAttributeValue(null, "vmtag")};
                            this.CarrierVmMap.put(numeric, data);
                        }
                        if (vmReader != null) {
                            try {
                                vmReader.close();
                            } catch (IOException e) {
                            }
                        }
                    } catch (Throwable th) {
                        if (vmReader != null) {
                            try {
                                vmReader.close();
                            } catch (IOException e2) {
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e3) {
                    Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e3);
                    if (vmReader != null) {
                        try {
                            vmReader.close();
                        } catch (IOException e4) {
                        }
                    }
                }
            } catch (IOException e5) {
                Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e5);
                if (vmReader != null) {
                    try {
                        vmReader.close();
                    } catch (IOException e6) {
                    }
                }
            }
        } catch (FileNotFoundException e7) {
            Rlog.w(LOG_TAG, "Can't open " + Environment.getRootDirectory() + "/" + PARTNER_VOICEMAIL_PATH);
        }
    }
}
