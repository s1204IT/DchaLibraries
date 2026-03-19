package com.android.internal.telephony;

import com.android.internal.telephony.cat.BipUtils;
import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduPart;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import java.util.HashMap;

public class WspTypeDecoder {
    public static final String CONTENT_MIME_TYPE_B_CONNECTIVITY = "application/vnd.wap.connectivity-wbxml";
    public static final String CONTENT_MIME_TYPE_B_VND_SULP_INIT = "application/vnd.omaloc-supl-init";
    public static final int CONTENT_TYPE_B_CONNECTIVITY = 53;
    public static final String CONTENT_TYPE_B_MMS = "application/vnd.wap.mms-message";
    public static final String CONTENT_TYPE_B_PUSH_CO = "application/vnd.wap.coc";
    public static final String CONTENT_TYPE_B_PUSH_SYNCML_NOTI = "application/vnd.syncml.notification";
    public static final int PARAMETER_ID_X_WAP_APPLICATION_ID = 47;
    public static final int PDU_TYPE_CONFIRMED_PUSH = 7;
    public static final int PDU_TYPE_PUSH = 6;
    private static final int Q_VALUE = 0;
    private static final int WAP_PDU_LENGTH_QUOTE = 31;
    private static final int WAP_PDU_SHORT_LENGTH_MAX = 30;
    private static final HashMap<Integer, String> WELL_KNOWN_X_WAP_APPLICATION_ID;
    HashMap<String, String> mContentParameters;
    int mDataLength;
    HashMap<String, String> mHeaders;
    String mStringValue;
    long mUnsigned32bit;
    byte[] mWspData;
    private static final HashMap<Integer, String> WELL_KNOWN_MIME_TYPES = new HashMap<>();
    private static final HashMap<Integer, String> WELL_KNOWN_PARAMETERS = new HashMap<>();
    private static final HashMap<Integer, String> WELL_KNOWN_HEADERS = new HashMap<>();

    static {
        WELL_KNOWN_MIME_TYPES.put(0, "*/*");
        WELL_KNOWN_MIME_TYPES.put(1, "text/*");
        WELL_KNOWN_MIME_TYPES.put(2, ContentType.TEXT_HTML);
        WELL_KNOWN_MIME_TYPES.put(3, ContentType.TEXT_PLAIN);
        WELL_KNOWN_MIME_TYPES.put(4, "text/x-hdml");
        WELL_KNOWN_MIME_TYPES.put(5, "text/x-ttml");
        WELL_KNOWN_MIME_TYPES.put(6, ContentType.TEXT_VCALENDAR);
        WELL_KNOWN_MIME_TYPES.put(7, ContentType.TEXT_VCARD);
        WELL_KNOWN_MIME_TYPES.put(8, "text/vnd.wap.wml");
        WELL_KNOWN_MIME_TYPES.put(9, "text/vnd.wap.wmlscript");
        WELL_KNOWN_MIME_TYPES.put(10, "text/vnd.wap.wta-event");
        WELL_KNOWN_MIME_TYPES.put(11, "multipart/*");
        WELL_KNOWN_MIME_TYPES.put(12, "multipart/mixed");
        WELL_KNOWN_MIME_TYPES.put(13, "multipart/form-data");
        WELL_KNOWN_MIME_TYPES.put(14, "multipart/byterantes");
        WELL_KNOWN_MIME_TYPES.put(15, "multipart/alternative");
        WELL_KNOWN_MIME_TYPES.put(16, "application/*");
        WELL_KNOWN_MIME_TYPES.put(17, "application/java-vm");
        WELL_KNOWN_MIME_TYPES.put(18, "application/x-www-form-urlencoded");
        WELL_KNOWN_MIME_TYPES.put(19, "application/x-hdmlc");
        WELL_KNOWN_MIME_TYPES.put(20, "application/vnd.wap.wmlc");
        WELL_KNOWN_MIME_TYPES.put(21, "application/vnd.wap.wmlscriptc");
        WELL_KNOWN_MIME_TYPES.put(22, "application/vnd.wap.wta-eventc");
        WELL_KNOWN_MIME_TYPES.put(23, "application/vnd.wap.uaprof");
        WELL_KNOWN_MIME_TYPES.put(24, "application/vnd.wap.wtls-ca-certificate");
        WELL_KNOWN_MIME_TYPES.put(25, "application/vnd.wap.wtls-user-certificate");
        WELL_KNOWN_MIME_TYPES.put(26, "application/x-x509-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(27, "application/x-x509-user-cert");
        WELL_KNOWN_MIME_TYPES.put(28, ContentType.IMAGE_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(29, ContentType.IMAGE_GIF);
        WELL_KNOWN_MIME_TYPES.put(30, ContentType.IMAGE_JPEG);
        WELL_KNOWN_MIME_TYPES.put(31, "image/tiff");
        WELL_KNOWN_MIME_TYPES.put(32, ContentType.IMAGE_PNG);
        WELL_KNOWN_MIME_TYPES.put(33, ContentType.IMAGE_WBMP);
        WELL_KNOWN_MIME_TYPES.put(34, "application/vnd.wap.multipart.*");
        WELL_KNOWN_MIME_TYPES.put(35, ContentType.MULTIPART_MIXED);
        WELL_KNOWN_MIME_TYPES.put(36, "application/vnd.wap.multipart.form-data");
        WELL_KNOWN_MIME_TYPES.put(37, "application/vnd.wap.multipart.byteranges");
        WELL_KNOWN_MIME_TYPES.put(38, ContentType.MULTIPART_ALTERNATIVE);
        WELL_KNOWN_MIME_TYPES.put(39, "application/xml");
        WELL_KNOWN_MIME_TYPES.put(40, "text/xml");
        WELL_KNOWN_MIME_TYPES.put(41, "application/vnd.wap.wbxml");
        WELL_KNOWN_MIME_TYPES.put(42, "application/x-x968-cross-cert");
        WELL_KNOWN_MIME_TYPES.put(43, "application/x-x968-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(44, "application/x-x968-user-cert");
        WELL_KNOWN_MIME_TYPES.put(45, "text/vnd.wap.si");
        WELL_KNOWN_MIME_TYPES.put(46, "application/vnd.wap.sic");
        WELL_KNOWN_MIME_TYPES.put(47, "text/vnd.wap.sl");
        WELL_KNOWN_MIME_TYPES.put(48, "application/vnd.wap.slc");
        WELL_KNOWN_MIME_TYPES.put(49, "text/vnd.wap.co");
        WELL_KNOWN_MIME_TYPES.put(50, CONTENT_TYPE_B_PUSH_CO);
        WELL_KNOWN_MIME_TYPES.put(51, ContentType.MULTIPART_RELATED);
        WELL_KNOWN_MIME_TYPES.put(52, "application/vnd.wap.sia");
        WELL_KNOWN_MIME_TYPES.put(53, "text/vnd.wap.connectivity-xml");
        WELL_KNOWN_MIME_TYPES.put(54, CONTENT_MIME_TYPE_B_CONNECTIVITY);
        WELL_KNOWN_MIME_TYPES.put(55, "application/pkcs7-mime");
        WELL_KNOWN_MIME_TYPES.put(56, "application/vnd.wap.hashed-certificate");
        WELL_KNOWN_MIME_TYPES.put(57, "application/vnd.wap.signed-certificate");
        WELL_KNOWN_MIME_TYPES.put(58, "application/vnd.wap.cert-response");
        WELL_KNOWN_MIME_TYPES.put(59, ContentType.APP_XHTML);
        WELL_KNOWN_MIME_TYPES.put(60, "application/wml+xml");
        WELL_KNOWN_MIME_TYPES.put(61, "text/css");
        WELL_KNOWN_MIME_TYPES.put(62, "application/vnd.wap.mms-message");
        WELL_KNOWN_MIME_TYPES.put(63, "application/vnd.wap.rollover-certificate");
        WELL_KNOWN_MIME_TYPES.put(64, "application/vnd.wap.locc+wbxml");
        WELL_KNOWN_MIME_TYPES.put(65, "application/vnd.wap.loc+xml");
        WELL_KNOWN_MIME_TYPES.put(66, "application/vnd.syncml.dm+wbxml");
        WELL_KNOWN_MIME_TYPES.put(67, "application/vnd.syncml.dm+xml");
        WELL_KNOWN_MIME_TYPES.put(68, CONTENT_TYPE_B_PUSH_SYNCML_NOTI);
        WELL_KNOWN_MIME_TYPES.put(69, ContentType.APP_WAP_XHTML);
        WELL_KNOWN_MIME_TYPES.put(70, "application/vnd.wv.csp.cir");
        WELL_KNOWN_MIME_TYPES.put(71, "application/vnd.oma.dd+xml");
        WELL_KNOWN_MIME_TYPES.put(72, "application/vnd.oma.drm.message");
        WELL_KNOWN_MIME_TYPES.put(73, ContentType.APP_DRM_CONTENT);
        WELL_KNOWN_MIME_TYPES.put(74, "application/vnd.oma.drm.rights+xml");
        WELL_KNOWN_MIME_TYPES.put(75, "application/vnd.oma.drm.rights+wbxml");
        WELL_KNOWN_MIME_TYPES.put(76, "application/vnd.wv.csp+xml");
        WELL_KNOWN_MIME_TYPES.put(77, "application/vnd.wv.csp+wbxml");
        WELL_KNOWN_MIME_TYPES.put(78, "application/vnd.syncml.ds.notification");
        WELL_KNOWN_MIME_TYPES.put(79, ContentType.AUDIO_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(80, ContentType.VIDEO_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(81, "application/vnd.oma.dd2+xml");
        WELL_KNOWN_MIME_TYPES.put(82, "application/mikey");
        WELL_KNOWN_MIME_TYPES.put(83, "application/vnd.oma.dcd");
        WELL_KNOWN_MIME_TYPES.put(84, "application/vnd.oma.dcdc");
        WELL_KNOWN_MIME_TYPES.put(513, "application/vnd.uplanet.cacheop-wbxml");
        WELL_KNOWN_MIME_TYPES.put(514, "application/vnd.uplanet.signal");
        WELL_KNOWN_MIME_TYPES.put(515, "application/vnd.uplanet.alert-wbxml");
        WELL_KNOWN_MIME_TYPES.put(516, "application/vnd.uplanet.list-wbxml");
        WELL_KNOWN_MIME_TYPES.put(517, "application/vnd.uplanet.listcmd-wbxml");
        WELL_KNOWN_MIME_TYPES.put(518, "application/vnd.uplanet.channel-wbxml");
        WELL_KNOWN_MIME_TYPES.put(519, "application/vnd.uplanet.provisioning-status-uri");
        WELL_KNOWN_MIME_TYPES.put(520, "x-wap.multipart/vnd.uplanet.header-set");
        WELL_KNOWN_MIME_TYPES.put(521, "application/vnd.uplanet.bearer-choice-wbxml");
        WELL_KNOWN_MIME_TYPES.put(522, "application/vnd.phonecom.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(523, "application/vnd.nokia.syncset+wbxml");
        WELL_KNOWN_MIME_TYPES.put(524, "image/x-up-wpng");
        WELL_KNOWN_MIME_TYPES.put(768, "application/iota.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(769, "application/iota.mmc-xml");
        WELL_KNOWN_MIME_TYPES.put(770, "application/vnd.syncml+xml");
        WELL_KNOWN_MIME_TYPES.put(771, "application/vnd.syncml+wbxml");
        WELL_KNOWN_MIME_TYPES.put(772, "text/vnd.wap.emn+xml");
        WELL_KNOWN_MIME_TYPES.put(773, "text/calendar");
        WELL_KNOWN_MIME_TYPES.put(774, "application/vnd.omads-email+xml");
        WELL_KNOWN_MIME_TYPES.put(775, "application/vnd.omads-file+xml");
        WELL_KNOWN_MIME_TYPES.put(776, "application/vnd.omads-folder+xml");
        WELL_KNOWN_MIME_TYPES.put(777, "text/directory;profile=vCard");
        WELL_KNOWN_MIME_TYPES.put(778, "application/vnd.wap.emn+wbxml");
        WELL_KNOWN_MIME_TYPES.put(779, "application/vnd.nokia.ipdc-purchase-response");
        WELL_KNOWN_MIME_TYPES.put(780, "application/vnd.motorola.screen3+xml");
        WELL_KNOWN_MIME_TYPES.put(781, "application/vnd.motorola.screen3+gzip");
        WELL_KNOWN_MIME_TYPES.put(782, "application/vnd.cmcc.setting+wbxml");
        WELL_KNOWN_MIME_TYPES.put(783, "application/vnd.cmcc.bombing+wbxml");
        WELL_KNOWN_MIME_TYPES.put(784, "application/vnd.docomo.pf");
        WELL_KNOWN_MIME_TYPES.put(785, "application/vnd.docomo.ub");
        WELL_KNOWN_MIME_TYPES.put(786, CONTENT_MIME_TYPE_B_VND_SULP_INIT);
        WELL_KNOWN_MIME_TYPES.put(787, "application/vnd.oma.group-usage-list+xml");
        WELL_KNOWN_MIME_TYPES.put(788, "application/oma-directory+xml");
        WELL_KNOWN_MIME_TYPES.put(789, "application/vnd.docomo.pf2");
        WELL_KNOWN_MIME_TYPES.put(790, "application/vnd.oma.drm.roap-trigger+wbxml");
        WELL_KNOWN_MIME_TYPES.put(791, "application/vnd.sbm.mid2");
        WELL_KNOWN_MIME_TYPES.put(792, "application/vnd.wmf.bootstrap");
        WELL_KNOWN_MIME_TYPES.put(793, "application/vnc.cmcc.dcd+xml");
        WELL_KNOWN_MIME_TYPES.put(794, "application/vnd.sbm.cid");
        WELL_KNOWN_MIME_TYPES.put(795, "application/vnd.oma.bcast.provisioningtrigger");
        WELL_KNOWN_PARAMETERS.put(0, "Q");
        WELL_KNOWN_PARAMETERS.put(1, "Charset");
        WELL_KNOWN_PARAMETERS.put(2, "Level");
        WELL_KNOWN_PARAMETERS.put(3, PplSmsFilterExtension.INSTRUCTION_KEY_TYPE);
        WELL_KNOWN_PARAMETERS.put(7, "Differences");
        WELL_KNOWN_PARAMETERS.put(8, "Padding");
        WELL_KNOWN_PARAMETERS.put(9, PplSmsFilterExtension.INSTRUCTION_KEY_TYPE);
        WELL_KNOWN_PARAMETERS.put(14, "Max-Age");
        WELL_KNOWN_PARAMETERS.put(16, "Secure");
        WELL_KNOWN_PARAMETERS.put(17, "SEC");
        WELL_KNOWN_PARAMETERS.put(18, "MAC");
        WELL_KNOWN_PARAMETERS.put(19, "Creation-date");
        WELL_KNOWN_PARAMETERS.put(20, "Modification-date");
        WELL_KNOWN_PARAMETERS.put(21, "Read-date");
        WELL_KNOWN_PARAMETERS.put(22, "Size");
        WELL_KNOWN_PARAMETERS.put(23, PduPart.PARA_NAME);
        WELL_KNOWN_PARAMETERS.put(24, "Filename");
        WELL_KNOWN_PARAMETERS.put(25, "Start");
        WELL_KNOWN_PARAMETERS.put(26, "Start-info");
        WELL_KNOWN_PARAMETERS.put(27, "Comment");
        WELL_KNOWN_PARAMETERS.put(28, "Domain");
        WELL_KNOWN_PARAMETERS.put(29, "Path");
        WELL_KNOWN_HEADERS.put(0, "Accept");
        WELL_KNOWN_HEADERS.put(1, "Accept-Charset");
        WELL_KNOWN_HEADERS.put(2, "Accept-Encoding");
        WELL_KNOWN_HEADERS.put(3, "Accept-Language");
        WELL_KNOWN_HEADERS.put(4, "Accept-Ranges");
        WELL_KNOWN_HEADERS.put(5, "Age");
        WELL_KNOWN_HEADERS.put(6, "Allow");
        WELL_KNOWN_HEADERS.put(7, "Authorization");
        WELL_KNOWN_HEADERS.put(8, "Cache-Control");
        WELL_KNOWN_HEADERS.put(9, "Connection");
        WELL_KNOWN_HEADERS.put(10, "Content-Base");
        WELL_KNOWN_HEADERS.put(11, "Content-Encoding");
        WELL_KNOWN_HEADERS.put(12, "Content-Language");
        WELL_KNOWN_HEADERS.put(13, "Content-Length");
        WELL_KNOWN_HEADERS.put(14, PduPart.CONTENT_LOCATION);
        WELL_KNOWN_HEADERS.put(15, "Content-MD5");
        WELL_KNOWN_HEADERS.put(16, "Content-Range");
        WELL_KNOWN_HEADERS.put(17, PduPart.CONTENT_TYPE);
        WELL_KNOWN_HEADERS.put(18, "Date");
        WELL_KNOWN_HEADERS.put(19, "Etag");
        WELL_KNOWN_HEADERS.put(20, "Expires");
        WELL_KNOWN_HEADERS.put(21, PplSmsFilterExtension.INSTRUCTION_KEY_FROM);
        WELL_KNOWN_HEADERS.put(22, "Host");
        WELL_KNOWN_HEADERS.put(23, "If-Modified-Since");
        WELL_KNOWN_HEADERS.put(24, "If-Match");
        WELL_KNOWN_HEADERS.put(25, "If-None-Match");
        WELL_KNOWN_HEADERS.put(26, "If-Range");
        WELL_KNOWN_HEADERS.put(27, "If-Unmodified-Since");
        WELL_KNOWN_HEADERS.put(28, "Location");
        WELL_KNOWN_HEADERS.put(29, "Last-Modified");
        WELL_KNOWN_HEADERS.put(30, "Max-Forwards");
        WELL_KNOWN_HEADERS.put(31, "Pragma");
        WELL_KNOWN_HEADERS.put(32, "Proxy-Authenticate");
        WELL_KNOWN_HEADERS.put(33, "Proxy-Authorization");
        WELL_KNOWN_HEADERS.put(34, "Public");
        WELL_KNOWN_HEADERS.put(35, "Range");
        WELL_KNOWN_HEADERS.put(36, "Referer");
        WELL_KNOWN_HEADERS.put(37, "Retry-After");
        WELL_KNOWN_HEADERS.put(38, "Server");
        WELL_KNOWN_HEADERS.put(39, "Transfer-Encoding");
        WELL_KNOWN_HEADERS.put(40, "Upgrade");
        WELL_KNOWN_HEADERS.put(41, "User-Agent");
        WELL_KNOWN_HEADERS.put(42, "Vary");
        WELL_KNOWN_HEADERS.put(43, "Via");
        WELL_KNOWN_HEADERS.put(44, "Warning");
        WELL_KNOWN_HEADERS.put(45, "WWW-Authenticate");
        WELL_KNOWN_HEADERS.put(46, PduPart.CONTENT_DISPOSITION);
        WELL_KNOWN_HEADERS.put(47, "X-Wap-Application-Id");
        WELL_KNOWN_HEADERS.put(48, "X-Wap-Content-URI");
        WELL_KNOWN_HEADERS.put(49, "X-Wap-Initiator-URI");
        WELL_KNOWN_HEADERS.put(50, "Accept-Application");
        WELL_KNOWN_HEADERS.put(51, "Bearer-Indication");
        WELL_KNOWN_HEADERS.put(52, "Push-Flag");
        WELL_KNOWN_HEADERS.put(53, "Profile");
        WELL_KNOWN_HEADERS.put(54, "Profile-Diff");
        WELL_KNOWN_HEADERS.put(55, "Profile-Warning");
        WELL_KNOWN_HEADERS.put(56, "Expect");
        WELL_KNOWN_HEADERS.put(57, "TE");
        WELL_KNOWN_HEADERS.put(58, "Trailer");
        WELL_KNOWN_HEADERS.put(59, "Accept-Charset");
        WELL_KNOWN_HEADERS.put(60, "Accept-Encoding");
        WELL_KNOWN_HEADERS.put(61, "Cache-Control");
        WELL_KNOWN_HEADERS.put(62, "Content-Range");
        WELL_KNOWN_HEADERS.put(63, "X-Wap-Tod");
        WELL_KNOWN_HEADERS.put(64, PduPart.CONTENT_ID);
        WELL_KNOWN_HEADERS.put(65, "Set-Cookie");
        WELL_KNOWN_HEADERS.put(66, "Cookie");
        WELL_KNOWN_HEADERS.put(67, "Encoding-Version");
        WELL_KNOWN_HEADERS.put(68, "Profile-Warning");
        WELL_KNOWN_HEADERS.put(69, PduPart.CONTENT_DISPOSITION);
        WELL_KNOWN_HEADERS.put(70, "X-WAP-Security");
        WELL_KNOWN_HEADERS.put(71, "Cache-Control");
        WELL_KNOWN_HEADERS.put(72, "Expect");
        WELL_KNOWN_HEADERS.put(73, "X-Wap-Loc-Invocation");
        WELL_KNOWN_HEADERS.put(74, "X-Wap-Loc-Delivery");
        WELL_KNOWN_X_WAP_APPLICATION_ID = new HashMap<>();
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(0, "x-wap-application:*");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(1, "x-wap-application:push.sia");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(2, "x-wap-application:wml.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(3, "x-wap-application:wta.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(4, "x-wap-application:mms.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(5, "x-wap-application:push.syncml");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(6, "x-wap-application:loc.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(7, "x-wap-application:syncml.dm");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(8, "x-wap-application:drm.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(9, "x-wap-application:emn.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(10, "x-wap-application:wv.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(16, "x-oma-application:ulp.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(17, "x-oma-application:dlota.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(18, "x-oma-application:java-ams");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(19, "x-oma-application:bcast.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(20, "x-oma-application:dpe.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(21, "x-oma-application:cpm:ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(Integer.valueOf(WapPushManagerParams.FURTHER_PROCESSING), "x-wap-microsoft:localcontent.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32769, "x-wap-microsoft:IMclient.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32770, "x-wap-docomo:imode.mail.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32771, "x-wap-docomo:imode.mr.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32772, "x-wap-docomo:imode.mf.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32773, "x-motorola:location.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32774, "x-motorola:now.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32775, "x-motorola:otaprov.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32776, "x-motorola:browser.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32777, "x-motorola:splash.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32779, "x-wap-nai:mvsw.command");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(32784, "x-wap-openwave:iota.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36864, "x-wap-docomo:imode.mail2.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36865, "x-oma-nec:otaprov.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36866, "x-oma-nokia:call.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36867, "x-oma-coremobility:sqa.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36868, "x-oma-docomo:doja.jam.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36880, "x-oma-nokia:sip.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36881, "x-oma-vodafone:otaprov.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36882, "x-hutchison:ad.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36883, "x-oma-nokia:voip.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36884, "x-oma-docomo:voice.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36885, "x-oma-docomo:browser.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36886, "x-oma-docomo:dan.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36887, "x-oma-nokia:vs.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36888, "x-oma-nokia:voip.ext1.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36889, "x-wap-vodafone:casting.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36890, "x-oma-docomo:imode.data.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36891, "x-oma-snapin:otaprov.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36892, "x-oma-nokia:vrs.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36893, "x-oma-nokia:vrpg.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36894, "x-oma-motorola:screen3.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36895, "x-oma-docomo:device.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36896, "x-oma-nokia:msc.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36897, "x-3gpp2:lcs.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36898, "x-wap-vodafone:dcd.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36899, "x-3gpp:mbms.service.announcement.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36900, "x-oma-vodafone:dltmtbl.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36901, "x-oma-vodafone:dvcctl.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36902, "x-oma-cmcc:mail.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36903, "x-oma-nokia:vmb.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36904, "x-oma-nokia:ldapss.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36905, "x-hutchison:al.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36906, "x-oma-nokia:uma.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36907, "x-oma-nokia:news.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36908, "x-oma-docomo:pf");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36909, "x-oma-docomo:ub>");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36910, "x-oma-nokia:nat.traversal.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36911, "x-oma-intromobile:intropad.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36912, "x-oma-docomo:uin.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36913, "x-oma-nokia:iptv.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36914, "x-hutchison:il.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36915, "x-oma-nokia:voip.general.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36916, "x-microsoft:drm.meter");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36917, "x-microsoft:drm.license");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36918, "x-oma-docomo:ic.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36919, "x-oma-slingmedia:SPM.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36920, "x-cibenix:odp.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36921, "x-oma-motorola:voip.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36922, "x-oma-motorola:ims");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36923, "x-oma-docomo:imode.remote.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36924, "x-oma-docomo:device.ctl.um");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36925, "x-microsoft:playready.drm.initiator");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36926, "x-microsoft:playready.drm");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36927, "x-oma-sbm:ms.mexa.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36928, "urn:oma:drms:org-LGE:L650V");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36929, "x-oma-docomo:um");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36930, "x-oma-docomo:uin.um");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36931, "urn:oma:drms:org-LGE:KU450");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36932, "x-wap-microsoft:cfgmgr.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36933, "x-3gpp:mbms.download.delivery.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36934, "x-oma-docomo:star.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36935, "urn:oma:drms:org-LGE:KU380");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36936, "x-oma-docomo:pf2");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36937, "x-oma-motorola:blogcentral.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36938, "x-oma-docomo:imode.agent.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36939, "x-wap-application:push.sia");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36940, "x-oma-nokia:destination.network.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36941, "x-oma-sbm:mid2.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36942, "x-carrieriq:avm.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36943, "x-oma-sbm:ms.xml.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36944, "urn:dvb:ipdc:notification:2008");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36945, "x-oma-docomo:imode.mvch.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36946, "x-oma-motorola:webui.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36947, "x-oma-sbm:cid.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36948, "x-oma-nokia:vcc.v1.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36949, "x-oma-docomo:open.ctl");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36950, "x-oma-docomo:sp.mail.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36951, "x-essoy-application:push.erace");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36952, "x-oma-docomo:open.fu");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36953, "x-samsung:osp.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36954, "x-oma-docomo:imode.mchara.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36955, "X-Wap-Application-Id:x-oma-application: scidm.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36956, "x-oma-docomo:xmd.mail.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36957, "x-oma-application:pal.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36958, "x-oma-docomo:imode.relation.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36959, "x-oma-docomo:xmd.storage.ua");
        WELL_KNOWN_X_WAP_APPLICATION_ID.put(36960, "x-oma-docomo:xmd.lcsapp.ua");
    }

    public WspTypeDecoder(byte[] pdu) {
        this.mWspData = pdu;
    }

    public boolean decodeTextString(int startIndex) {
        int index = startIndex;
        while (this.mWspData[index] != 0) {
            index++;
        }
        this.mDataLength = (index - startIndex) + 1;
        if (this.mWspData[startIndex] == 127) {
            this.mStringValue = new String(this.mWspData, startIndex + 1, this.mDataLength - 2);
            return true;
        }
        this.mStringValue = new String(this.mWspData, startIndex, this.mDataLength - 1);
        return true;
    }

    public boolean decodeTokenText(int startIndex) {
        int index = startIndex;
        while (this.mWspData[index] != 0) {
            index++;
        }
        this.mDataLength = (index - startIndex) + 1;
        this.mStringValue = new String(this.mWspData, startIndex, this.mDataLength - 1);
        return true;
    }

    public boolean decodeShortInteger(int startIndex) {
        if ((this.mWspData[startIndex] & BipUtils.TCP_STATUS_ESTABLISHED) == 0) {
            return false;
        }
        this.mUnsigned32bit = this.mWspData[startIndex] & 127;
        this.mDataLength = 1;
        return true;
    }

    public boolean decodeLongInteger(int startIndex) {
        int lengthMultiOctet = this.mWspData[startIndex] & PplMessageManager.Type.INVALID;
        if (lengthMultiOctet > 30) {
            return false;
        }
        this.mUnsigned32bit = 0L;
        for (int i = 1; i <= lengthMultiOctet; i++) {
            this.mUnsigned32bit = (this.mUnsigned32bit << 8) | ((long) (this.mWspData[startIndex + i] & PplMessageManager.Type.INVALID));
        }
        this.mDataLength = lengthMultiOctet + 1;
        return true;
    }

    public boolean decodeIntegerValue(int startIndex) {
        if (decodeShortInteger(startIndex)) {
            return true;
        }
        return decodeLongInteger(startIndex);
    }

    public boolean decodeUintvarInteger(int startIndex) {
        int index = startIndex;
        this.mUnsigned32bit = 0L;
        while ((this.mWspData[index] & BipUtils.TCP_STATUS_ESTABLISHED) != 0) {
            if (index - startIndex >= 4) {
                return false;
            }
            this.mUnsigned32bit = (this.mUnsigned32bit << 7) | ((long) (this.mWspData[index] & 127));
            index++;
        }
        this.mUnsigned32bit = (this.mUnsigned32bit << 7) | ((long) (this.mWspData[index] & 127));
        this.mDataLength = (index - startIndex) + 1;
        return true;
    }

    public boolean decodeValueLength(int startIndex) {
        if ((this.mWspData[startIndex] & PplMessageManager.Type.INVALID) > 31) {
            return false;
        }
        if (this.mWspData[startIndex] < 31) {
            this.mUnsigned32bit = this.mWspData[startIndex];
            this.mDataLength = 1;
        } else {
            decodeUintvarInteger(startIndex + 1);
            this.mDataLength++;
        }
        return true;
    }

    public boolean decodeExtensionMedia(int startIndex) {
        int index = startIndex;
        this.mDataLength = 0;
        this.mStringValue = null;
        int length = this.mWspData.length;
        boolean rtrn = startIndex < length;
        while (index < length && this.mWspData[index] != 0) {
            index++;
        }
        this.mDataLength = (index - startIndex) + 1;
        this.mStringValue = new String(this.mWspData, startIndex, this.mDataLength - 1);
        return rtrn;
    }

    public boolean decodeConstrainedEncoding(int startIndex) {
        if (decodeShortInteger(startIndex)) {
            this.mStringValue = null;
            return true;
        }
        return decodeExtensionMedia(startIndex);
    }

    public boolean decodeContentType(int startIndex) {
        this.mContentParameters = new HashMap<>();
        try {
            if (!decodeValueLength(startIndex)) {
                boolean found = decodeConstrainedEncoding(startIndex);
                if (found) {
                    expandWellKnownMimeType();
                }
                return found;
            }
            int headersLength = (int) this.mUnsigned32bit;
            int mediaPrefixLength = getDecodedDataLength();
            if (decodeIntegerValue(startIndex + mediaPrefixLength)) {
                this.mDataLength += mediaPrefixLength;
                int readLength = this.mDataLength;
                this.mStringValue = null;
                expandWellKnownMimeType();
                long wellKnownValue = this.mUnsigned32bit;
                String mimeType = this.mStringValue;
                if (!readContentParameters(this.mDataLength + startIndex, headersLength - (this.mDataLength - mediaPrefixLength), 0)) {
                    return false;
                }
                this.mDataLength += readLength;
                this.mUnsigned32bit = wellKnownValue;
                this.mStringValue = mimeType;
                return true;
            }
            if (decodeExtensionMedia(startIndex + mediaPrefixLength)) {
                this.mDataLength += mediaPrefixLength;
                int readLength2 = this.mDataLength;
                expandWellKnownMimeType();
                long wellKnownValue2 = this.mUnsigned32bit;
                String mimeType2 = this.mStringValue;
                if (readContentParameters(this.mDataLength + startIndex, headersLength - (this.mDataLength - mediaPrefixLength), 0)) {
                    this.mDataLength += readLength2;
                    this.mUnsigned32bit = wellKnownValue2;
                    this.mStringValue = mimeType2;
                    return true;
                }
            }
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean readContentParameters(int startIndex, int leftToRead, int accumulator) {
        int totalRead;
        String param;
        int totalRead2;
        String value;
        if (leftToRead > 0) {
            byte nextByte = this.mWspData[startIndex];
            if ((nextByte & BipUtils.TCP_STATUS_ESTABLISHED) == 0 && nextByte > 31) {
                decodeTokenText(startIndex);
                param = this.mStringValue;
                totalRead = this.mDataLength + 0;
            } else {
                if (!decodeIntegerValue(startIndex)) {
                    return false;
                }
                totalRead = this.mDataLength + 0;
                int wellKnownParameterValue = (int) this.mUnsigned32bit;
                param = WELL_KNOWN_PARAMETERS.get(Integer.valueOf(wellKnownParameterValue));
                if (param == null) {
                    param = "unassigned/0x" + Long.toHexString(wellKnownParameterValue);
                }
                if (wellKnownParameterValue == 0) {
                    if (!decodeUintvarInteger(startIndex + totalRead)) {
                        return false;
                    }
                    int totalRead3 = totalRead + this.mDataLength;
                    this.mContentParameters.put(param, String.valueOf(this.mUnsigned32bit));
                    return readContentParameters(startIndex + totalRead3, leftToRead - totalRead3, accumulator + totalRead3);
                }
            }
            if (decodeNoValue(startIndex + totalRead)) {
                totalRead2 = totalRead + this.mDataLength;
                value = null;
            } else if (decodeIntegerValue(startIndex + totalRead)) {
                totalRead2 = totalRead + this.mDataLength;
                int intValue = (int) this.mUnsigned32bit;
                value = String.valueOf(intValue);
            } else {
                decodeTokenText(startIndex + totalRead);
                totalRead2 = totalRead + this.mDataLength;
                value = this.mStringValue;
                if (value.startsWith("\"")) {
                    value = value.substring(1);
                }
            }
            this.mContentParameters.put(param, value);
            return readContentParameters(startIndex + totalRead2, leftToRead - totalRead2, accumulator + totalRead2);
        }
        this.mDataLength = accumulator;
        return true;
    }

    private boolean decodeNoValue(int startIndex) {
        if (this.mWspData[startIndex] != 0) {
            return false;
        }
        this.mDataLength = 1;
        return true;
    }

    private void expandWellKnownMimeType() {
        if (this.mStringValue == null) {
            int binaryContentType = (int) this.mUnsigned32bit;
            this.mStringValue = WELL_KNOWN_MIME_TYPES.get(Integer.valueOf(binaryContentType));
        } else {
            this.mUnsigned32bit = -1L;
        }
    }

    public boolean decodeContentLength(int startIndex) {
        return decodeIntegerValue(startIndex);
    }

    public boolean decodeContentLocation(int startIndex) {
        return decodeTextString(startIndex);
    }

    public boolean decodeXWapApplicationId(int startIndex) {
        if (decodeIntegerValue(startIndex)) {
            this.mStringValue = null;
            return true;
        }
        return decodeTextString(startIndex);
    }

    public boolean seekXWapApplicationId(int startIndex, int endIndex) {
        int index = startIndex;
        while (index <= endIndex) {
            try {
                if (decodeIntegerValue(index)) {
                    int fieldValue = (int) getValue32();
                    if (fieldValue == 47) {
                        this.mUnsigned32bit = index + 1;
                        return true;
                    }
                } else if (!decodeTextString(index)) {
                    return false;
                }
                int index2 = index + getDecodedDataLength();
                if (index2 > endIndex) {
                    return false;
                }
                byte val = this.mWspData[index2];
                if (val >= 0 && val <= 30) {
                    index = index2 + this.mWspData[index2] + 1;
                } else if (val == 31) {
                    if (index2 + 1 >= endIndex) {
                        return false;
                    }
                    int index3 = index2 + 1;
                    if (!decodeUintvarInteger(index3)) {
                        return false;
                    }
                    index = index3 + getDecodedDataLength();
                } else if (31 < val && val <= 127) {
                    if (!decodeTextString(index2)) {
                        return false;
                    }
                    index = index2 + getDecodedDataLength();
                } else {
                    index = index2 + 1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
        return false;
    }

    public boolean decodeXWapContentURI(int startIndex) {
        return decodeTextString(startIndex);
    }

    public boolean decodeXWapInitiatorURI(int startIndex) {
        return decodeTextString(startIndex);
    }

    public int getDecodedDataLength() {
        return this.mDataLength;
    }

    public long getValue32() {
        return this.mUnsigned32bit;
    }

    public String getValueString() {
        return this.mStringValue;
    }

    public HashMap<String, String> getContentParameters() {
        return this.mContentParameters;
    }

    public void decodeHeaders(int startIndex, int headerLength) {
        String headerName;
        String headerValue;
        this.mHeaders = new HashMap<>();
        int index = startIndex;
        while (index < startIndex + headerLength) {
            decodeHeaderFieldName(index);
            index += getDecodedDataLength();
            expandWellKnownHeadersName();
            int intValues = (int) this.mUnsigned32bit;
            if (this.mStringValue != null) {
                headerName = this.mStringValue;
            } else if (intValues >= 0) {
                headerName = String.valueOf(intValues);
            }
            decodeHeaderFieldValues(index);
            index += getDecodedDataLength();
            int intValues2 = (int) this.mUnsigned32bit;
            if (this.mStringValue != null) {
                headerValue = this.mStringValue;
            } else if (intValues2 >= 0) {
                headerValue = String.valueOf(intValues2);
            }
            this.mHeaders.put(headerName, headerValue);
        }
    }

    public boolean decodeHeaderFieldName(int startIndex) {
        if (decodeShortInteger(startIndex)) {
            this.mStringValue = null;
            return true;
        }
        return decodeTextString(startIndex);
    }

    public boolean decodeHeaderFieldValues(int startIndex) {
        byte first = this.mWspData[startIndex];
        if (first == 31 && decodeUintvarInteger(startIndex + 1)) {
            this.mStringValue = null;
            this.mDataLength++;
            return true;
        }
        if (decodeIntegerValue(startIndex)) {
            this.mStringValue = null;
            return true;
        }
        return decodeTextString(startIndex);
    }

    public void expandWellKnownHeadersName() {
        if (this.mStringValue == null) {
            int binaryHeadersName = (int) this.mUnsigned32bit;
            this.mStringValue = WELL_KNOWN_HEADERS.get(Integer.valueOf(binaryHeadersName));
        } else {
            this.mUnsigned32bit = -1L;
        }
    }

    public HashMap<String, String> getHeaders() {
        expandWellKnownXWapApplicationIdName();
        return this.mHeaders;
    }

    public void expandWellKnownXWapApplicationIdName() {
        String value;
        try {
            int binaryCode = Integer.valueOf(this.mHeaders.get("X-Wap-Application-Id")).intValue();
            if (binaryCode == -1 || (value = WELL_KNOWN_X_WAP_APPLICATION_ID.get(Integer.valueOf(binaryCode))) == null) {
                return;
            }
            this.mHeaders.put("X-Wap-Application-Id", value);
        } catch (Exception e) {
        }
    }
}
