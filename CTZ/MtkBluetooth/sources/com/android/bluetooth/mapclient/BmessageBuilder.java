package com.android.bluetooth.mapclient;

import com.android.vcard.VCardEntry;
import java.util.Iterator;
import java.util.List;

class BmessageBuilder {
    private static final String BBODY_BEGIN = "BEGIN:BBODY";
    private static final String BBODY_CHARSET = "CHARSET:";
    private static final String BBODY_ENCODING = "ENCODING:";
    private static final String BBODY_END = "END:BBODY";
    private static final String BBODY_LANGUAGE = "LANGUAGE:";
    private static final String BBODY_LENGTH = "LENGTH:";
    private static final String BENV_BEGIN = "BEGIN:BENV";
    private static final String BENV_END = "END:BENV";
    private static final String BMSG_BEGIN = "BEGIN:BMSG";
    private static final String BMSG_END = "END:BMSG";
    private static final String BMSG_FOLDER = "FOLDER:";
    private static final String BMSG_STATUS = "STATUS:";
    private static final String BMSG_TYPE = "TYPE:";
    private static final String BMSG_VERSION = "VERSION:1.0";
    private static final String CRLF = "\r\n";
    private static final String MSG_BEGIN = "BEGIN:MSG";
    private static final String MSG_END = "END:MSG";
    private static final String VCARD_BEGIN = "BEGIN:VCARD";
    private static final String VCARD_EMAIL = "EMAIL:";
    private static final String VCARD_END = "END:VCARD";
    private static final String VCARD_N = "N:";
    private static final String VCARD_TEL = "TEL:";
    private static final String VCARD_VERSION = "VERSION:2.1";
    private final StringBuilder mBmsg = new StringBuilder();

    private BmessageBuilder() {
    }

    public static String createBmessage(Bmessage bmessage) {
        BmessageBuilder bmessageBuilder = new BmessageBuilder();
        bmessageBuilder.build(bmessage);
        return bmessageBuilder.mBmsg.toString();
    }

    private void build(Bmessage bmessage) {
        int length = MSG_BEGIN.length() + MSG_END.length() + (3 * "\r\n".length()) + bmessage.mMessage.getBytes().length;
        StringBuilder sb = this.mBmsg;
        sb.append(BMSG_BEGIN);
        sb.append("\r\n");
        StringBuilder sb2 = this.mBmsg;
        sb2.append(BMSG_VERSION);
        sb2.append("\r\n");
        StringBuilder sb3 = this.mBmsg;
        sb3.append(BMSG_STATUS);
        sb3.append(bmessage.mBmsgStatus);
        sb3.append("\r\n");
        StringBuilder sb4 = this.mBmsg;
        sb4.append(BMSG_TYPE);
        sb4.append(bmessage.mBmsgType);
        sb4.append("\r\n");
        StringBuilder sb5 = this.mBmsg;
        sb5.append(BMSG_FOLDER);
        sb5.append(bmessage.mBmsgFolder);
        sb5.append("\r\n");
        Iterator<VCardEntry> it = bmessage.mOriginators.iterator();
        while (it.hasNext()) {
            buildVcard(it.next());
        }
        StringBuilder sb6 = this.mBmsg;
        sb6.append(BENV_BEGIN);
        sb6.append("\r\n");
        Iterator<VCardEntry> it2 = bmessage.mRecipients.iterator();
        while (it2.hasNext()) {
            buildVcard(it2.next());
        }
        StringBuilder sb7 = this.mBmsg;
        sb7.append(BBODY_BEGIN);
        sb7.append("\r\n");
        if (bmessage.mBbodyEncoding != null) {
            StringBuilder sb8 = this.mBmsg;
            sb8.append(BBODY_ENCODING);
            sb8.append(bmessage.mBbodyEncoding);
            sb8.append("\r\n");
        }
        if (bmessage.mBbodyCharset != null) {
            StringBuilder sb9 = this.mBmsg;
            sb9.append(BBODY_CHARSET);
            sb9.append(bmessage.mBbodyCharset);
            sb9.append("\r\n");
        }
        if (bmessage.mBbodyLanguage != null) {
            StringBuilder sb10 = this.mBmsg;
            sb10.append(BBODY_LANGUAGE);
            sb10.append(bmessage.mBbodyLanguage);
            sb10.append("\r\n");
        }
        StringBuilder sb11 = this.mBmsg;
        sb11.append(BBODY_LENGTH);
        sb11.append(length);
        sb11.append("\r\n");
        StringBuilder sb12 = this.mBmsg;
        sb12.append(MSG_BEGIN);
        sb12.append("\r\n");
        StringBuilder sb13 = this.mBmsg;
        sb13.append(bmessage.mMessage);
        sb13.append("\r\n");
        StringBuilder sb14 = this.mBmsg;
        sb14.append(MSG_END);
        sb14.append("\r\n");
        StringBuilder sb15 = this.mBmsg;
        sb15.append(BBODY_END);
        sb15.append("\r\n");
        StringBuilder sb16 = this.mBmsg;
        sb16.append(BENV_END);
        sb16.append("\r\n");
        StringBuilder sb17 = this.mBmsg;
        sb17.append(BMSG_END);
        sb17.append("\r\n");
    }

    private void buildVcard(VCardEntry vCardEntry) {
        String strBuildVcardN = buildVcardN(vCardEntry);
        List<VCardEntry.PhoneData> phoneList = vCardEntry.getPhoneList();
        List<VCardEntry.EmailData> emailList = vCardEntry.getEmailList();
        StringBuilder sb = this.mBmsg;
        sb.append(VCARD_BEGIN);
        sb.append("\r\n");
        StringBuilder sb2 = this.mBmsg;
        sb2.append(VCARD_VERSION);
        sb2.append("\r\n");
        StringBuilder sb3 = this.mBmsg;
        sb3.append(VCARD_N);
        sb3.append(strBuildVcardN);
        sb3.append("\r\n");
        if (phoneList != null && phoneList.size() > 0) {
            StringBuilder sb4 = this.mBmsg;
            sb4.append(VCARD_TEL);
            sb4.append(phoneList.get(0).getNumber());
            sb4.append("\r\n");
        }
        if (emailList != null && emailList.size() > 0) {
            StringBuilder sb5 = this.mBmsg;
            sb5.append(VCARD_EMAIL);
            sb5.append(emailList.get(0).getAddress());
            sb5.append("\r\n");
        }
        StringBuilder sb6 = this.mBmsg;
        sb6.append(VCARD_END);
        sb6.append("\r\n");
    }

    private String buildVcardN(VCardEntry vCardEntry) {
        VCardEntry.NameData nameData = vCardEntry.getNameData();
        StringBuilder sb = new StringBuilder();
        sb.append(nameData.getFamily());
        sb.append(";");
        sb.append(nameData.getGiven() == null ? "" : nameData.getGiven());
        sb.append(";");
        sb.append(nameData.getMiddle() == null ? "" : nameData.getMiddle());
        sb.append(";");
        sb.append(nameData.getPrefix() == null ? "" : nameData.getPrefix());
        sb.append(";");
        sb.append(nameData.getSuffix() == null ? "" : nameData.getSuffix());
        return sb.toString();
    }
}
