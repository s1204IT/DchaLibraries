package gov.nist.javax.sip.parser;

import gov.nist.core.Host;
import gov.nist.core.HostNameParser;
import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelephoneNumber;
import gov.nist.javax.sip.header.ExtensionHeaderImpl;
import gov.nist.javax.sip.header.NameMap;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class StringMsgParser {
    private static boolean computeContentLengthFromMessage = false;
    private ParseExceptionListener parseExceptionListener;
    private String rawStringMessage;
    protected boolean readBody;
    private boolean strict;

    public StringMsgParser() {
        this.readBody = true;
    }

    public StringMsgParser(ParseExceptionListener parseExceptionListener) {
        this();
        this.parseExceptionListener = parseExceptionListener;
    }

    public void setParseExceptionListener(ParseExceptionListener parseExceptionListener) {
        this.parseExceptionListener = parseExceptionListener;
    }

    public SIPMessage parseSIPMessage(byte[] bArr) throws ParseException {
        int i;
        int i2;
        String str = null;
        if (bArr == null || bArr.length == 0) {
            return null;
        }
        int i3 = 0;
        while (bArr[i3] < 32) {
            try {
                i3++;
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        SIPMessage sIPMessageProcessFirstLine = null;
        boolean z = true;
        while (true) {
            int i4 = i3;
            while (bArr[i4] != 13 && bArr[i4] != 10) {
                try {
                    i4++;
                } catch (ArrayIndexOutOfBoundsException e2) {
                    i = i4;
                }
            }
            try {
                String strTrimEndOfLine = trimEndOfLine(new String(bArr, i3, i4 - i3, "UTF-8"));
                if (strTrimEndOfLine.length() == 0) {
                    if (str != null && sIPMessageProcessFirstLine != null) {
                        processHeader(str, sIPMessageProcessFirstLine);
                    }
                } else if (z) {
                    sIPMessageProcessFirstLine = processFirstLine(strTrimEndOfLine);
                } else {
                    char cCharAt = strTrimEndOfLine.charAt(0);
                    if (cCharAt == '\t' || cCharAt == ' ') {
                        if (str == null) {
                            throw new ParseException("Bad header continuation.", 0);
                        }
                        str = str + strTrimEndOfLine.substring(1);
                    } else {
                        if (str != null && sIPMessageProcessFirstLine != null) {
                            processHeader(str, sIPMessageProcessFirstLine);
                        }
                        str = strTrimEndOfLine;
                    }
                }
                if (bArr[i4] == 13 && bArr.length > (i2 = i4 + 1) && bArr[i2] == 10) {
                    i4 = i2;
                }
                i = i4 + 1;
                if (strTrimEndOfLine.length() <= 0) {
                    break;
                }
                i3 = i;
                z = false;
            } catch (UnsupportedEncodingException e3) {
                throw new ParseException("Bad message encoding!", 0);
            }
        }
        if (sIPMessageProcessFirstLine == null) {
            throw new ParseException("Bad message", 0);
        }
        sIPMessageProcessFirstLine.setSize(i);
        if (this.readBody && sIPMessageProcessFirstLine.getContentLength() != null && sIPMessageProcessFirstLine.getContentLength().getContentLength() != 0) {
            int length = bArr.length - i;
            byte[] bArr2 = new byte[length];
            System.arraycopy(bArr, i, bArr2, 0, length);
            sIPMessageProcessFirstLine.setMessageContent(bArr2, computeContentLengthFromMessage, sIPMessageProcessFirstLine.getContentLength().getContentLength());
        }
        return sIPMessageProcessFirstLine;
    }

    public SIPMessage parseSIPMessage(String str) throws ParseException {
        int i;
        int i2;
        String str2 = null;
        if (str == null || str.length() == 0) {
            return null;
        }
        this.rawStringMessage = str;
        int i3 = 0;
        while (str.charAt(i3) < ' ') {
            try {
                i3++;
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            } catch (StringIndexOutOfBoundsException e2) {
                return null;
            }
        }
        SIPMessage sIPMessageProcessFirstLine = null;
        boolean z = true;
        while (true) {
            try {
                char cCharAt = str.charAt(i3);
                int i4 = i3;
                while (cCharAt != '\r' && cCharAt != '\n') {
                    i4++;
                    try {
                        cCharAt = str.charAt(i4);
                    } catch (ArrayIndexOutOfBoundsException e3) {
                        i = i4;
                    } catch (StringIndexOutOfBoundsException e4) {
                        i = i4;
                    }
                }
                String strTrimEndOfLine = trimEndOfLine(str.substring(i3, i4));
                if (strTrimEndOfLine.length() == 0) {
                    if (str2 != null) {
                        processHeader(str2, sIPMessageProcessFirstLine);
                    }
                } else if (z) {
                    sIPMessageProcessFirstLine = processFirstLine(strTrimEndOfLine);
                } else {
                    char cCharAt2 = strTrimEndOfLine.charAt(0);
                    if (cCharAt2 == '\t' || cCharAt2 == ' ') {
                        if (str2 == null) {
                            throw new ParseException("Bad header continuation.", 0);
                        }
                        str2 = str2 + strTrimEndOfLine.substring(1);
                    } else {
                        if (str2 != null) {
                            processHeader(str2, sIPMessageProcessFirstLine);
                        }
                        str2 = strTrimEndOfLine;
                    }
                }
                if (str.charAt(i4) != '\r' || str.length() <= (i2 = i4 + 1) || str.charAt(i2) != '\n') {
                    i2 = i4;
                }
                i = i2 + 1;
                if (strTrimEndOfLine.length() <= 0) {
                    break;
                }
                i3 = i;
                z = false;
            } catch (ArrayIndexOutOfBoundsException e5) {
                i = i3;
            } catch (StringIndexOutOfBoundsException e6) {
                i = i3;
            }
        }
        sIPMessageProcessFirstLine.setSize(i);
        if (this.readBody && sIPMessageProcessFirstLine.getContentLength() != null) {
            if (sIPMessageProcessFirstLine.getContentLength().getContentLength() != 0) {
                sIPMessageProcessFirstLine.setMessageContent(str.substring(i), this.strict, computeContentLengthFromMessage, sIPMessageProcessFirstLine.getContentLength().getContentLength());
            } else if (!computeContentLengthFromMessage && sIPMessageProcessFirstLine.getContentLength().getContentLength() == 0 && !str.endsWith("\r\n\r\n") && this.strict) {
                throw new ParseException("Extraneous characters at the end of the message ", i);
            }
        }
        return sIPMessageProcessFirstLine;
    }

    private String trimEndOfLine(String str) {
        if (str == null) {
            return str;
        }
        int length = str.length() - 1;
        while (length >= 0 && str.charAt(length) <= ' ') {
            length--;
        }
        if (length == str.length() - 1) {
            return str;
        }
        if (length == -1) {
            return "";
        }
        return str.substring(0, length + 1);
    }

    private SIPMessage processFirstLine(String str) throws ParseException {
        SIPMessage sIPResponse;
        if (!str.startsWith(SIPConstants.SIP_VERSION_STRING)) {
            sIPResponse = new SIPRequest();
            try {
                ((SIPRequest) sIPResponse).setRequestLine(new RequestLineParser(str + Separators.RETURN).parse());
            } catch (ParseException e) {
                if (this.parseExceptionListener != null) {
                    this.parseExceptionListener.handleException(e, sIPResponse, RequestLine.class, str, this.rawStringMessage);
                } else {
                    throw e;
                }
            }
        } else {
            sIPResponse = new SIPResponse();
            try {
                ((SIPResponse) sIPResponse).setStatusLine(new StatusLineParser(str + Separators.RETURN).parse());
            } catch (ParseException e2) {
                if (this.parseExceptionListener != null) {
                    this.parseExceptionListener.handleException(e2, sIPResponse, StatusLine.class, str, this.rawStringMessage);
                } else {
                    throw e2;
                }
            }
        }
        return sIPResponse;
    }

    private void processHeader(String str, SIPMessage sIPMessage) throws ParseException {
        if (str == null || str.length() == 0) {
            return;
        }
        try {
            try {
                sIPMessage.attachHeader(ParserFactory.createParser(str + Separators.RETURN).parse(), false);
            } catch (ParseException e) {
                if (this.parseExceptionListener != null) {
                    Class<ExtensionHeaderImpl> classFromName = NameMap.getClassFromName(Lexer.getHeaderName(str));
                    if (classFromName == null) {
                        classFromName = ExtensionHeaderImpl.class;
                    }
                    this.parseExceptionListener.handleException(e, sIPMessage, classFromName, str, this.rawStringMessage);
                }
            }
        } catch (ParseException e2) {
            this.parseExceptionListener.handleException(e2, sIPMessage, null, str, this.rawStringMessage);
        }
    }

    public AddressImpl parseAddress(String str) throws ParseException {
        return new AddressParser(str).address(true);
    }

    public Host parseHost(String str) throws ParseException {
        return new HostNameParser(new Lexer("charLexer", str)).host();
    }

    public TelephoneNumber parseTelephoneNumber(String str) throws ParseException {
        return new URLParser(str).parseTelephoneNumber(true);
    }

    public SipUri parseSIPUrl(String str) throws ParseException {
        try {
            return new URLParser(str).sipURL(true);
        } catch (ClassCastException e) {
            throw new ParseException(str + " Not a SIP URL ", 0);
        }
    }

    public GenericURI parseUrl(String str) throws ParseException {
        return new URLParser(str).parse();
    }

    public SIPHeader parseSIPHeader(String str) throws ParseException {
        int length = str.length() - 1;
        int i = 0;
        while (str.charAt(i) <= ' ') {
            try {
                i++;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new ParseException("Empty header.", 0);
            }
        }
        while (str.charAt(length) <= ' ') {
            length--;
        }
        StringBuffer stringBuffer = new StringBuffer(length + 1);
        boolean z = false;
        int i2 = i;
        while (i <= length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\r' || cCharAt == '\n') {
                if (!z) {
                    stringBuffer.append(str.substring(i2, i));
                    z = true;
                }
            } else if (z) {
                if (cCharAt == ' ' || cCharAt == '\t') {
                    stringBuffer.append(' ');
                    i2 = i + 1;
                    z = false;
                } else {
                    z = false;
                    i2 = i;
                }
            }
            i++;
        }
        stringBuffer.append(str.substring(i2, i));
        stringBuffer.append('\n');
        HeaderParser headerParserCreateParser = ParserFactory.createParser(stringBuffer.toString());
        if (headerParserCreateParser == null) {
            throw new ParseException("could not create parser", 0);
        }
        return headerParserCreateParser.parse();
    }

    public RequestLine parseSIPRequestLine(String str) throws ParseException {
        return new RequestLineParser(str + Separators.RETURN).parse();
    }

    public StatusLine parseSIPStatusLine(String str) throws ParseException {
        return new StatusLineParser(str + Separators.RETURN).parse();
    }

    public static void setComputeContentLengthFromMessage(boolean z) {
        computeContentLengthFromMessage = z;
    }

    public static void main(String[] strArr) throws ParseException {
        String[] strArr2 = {"SIP/2.0 200 OK\r\nTo: \"The Little Blister\" <sip:LittleGuy@there.com>;tag=469bc066\r\nFrom: \"The Master Blaster\" <sip:BigGuy@here.com>;tag=11\r\nVia: SIP/2.0/UDP 139.10.134.246:5060;branch=z9hG4bK8b0a86f6_1030c7d18e0_17;received=139.10.134.246\r\nCall-ID: 1030c7d18ae_a97b0b_b@8b0a86f6\r\nCSeq: 1 SUBSCRIBE\r\nContact: <sip:172.16.11.162:5070>\r\nContent-Length: 0\r\n\r\n", "SIP/2.0 180 Ringing\r\nVia: SIP/2.0/UDP 172.18.1.29:5060;branch=z9hG4bK43fc10fb4446d55fc5c8f969607991f4\r\nTo: \"0440\" <sip:0440@212.209.220.131>;tag=2600\r\nFrom: \"Andreas\" <sip:andreas@e-horizon.se>;tag=8524\r\nCall-ID: f51a1851c5f570606140f14c8eb64fd3@172.18.1.29\r\nCSeq: 1 INVITE\r\nMax-Forwards: 70\r\nRecord-Route: <sip:212.209.220.131:5060>\r\nContent-Length: 0\r\n\r\n", "REGISTER sip:nist.gov SIP/2.0\r\nVia: SIP/2.0/UDP 129.6.55.182:14826\r\nMax-Forwards: 70\r\nFrom: <sip:mranga@nist.gov>;tag=6fcd5c7ace8b4a45acf0f0cd539b168b;epid=0d4c418ddf\r\nTo: <sip:mranga@nist.gov>\r\nCall-ID: c5679907eb954a8da9f9dceb282d7230@129.6.55.182\r\nCSeq: 1 REGISTER\r\nContact: <sip:129.6.55.182:14826>;methods=\"INVITE, MESSAGE, INFO, SUBSCRIBE, OPTIONS, BYE, CANCEL, NOTIFY, ACK, REFER\"\r\nUser-Agent: RTC/(Microsoft RTC)\r\nEvent:  registration\r\nAllow-Events: presence\r\nContent-Length: 0\r\n\r\nINVITE sip:littleguy@there.com:5060 SIP/2.0\r\nVia: SIP/2.0/UDP 65.243.118.100:5050\r\nFrom: M. Ranganathan  <sip:M.Ranganathan@sipbakeoff.com>;tag=1234\r\nTo: \"littleguy@there.com\" <sip:littleguy@there.com:5060> \r\nCall-ID: Q2AboBsaGn9!?x6@sipbakeoff.com \r\nCSeq: 1 INVITE \r\nContent-Length: 247\r\n\r\nv=0\r\no=4855 13760799956958020 13760799956958020 IN IP4  129.6.55.78\r\ns=mysession session\r\np=+46 8 52018010\r\nc=IN IP4  129.6.55.78\r\nt=0 0\r\nm=audio 6022 RTP/AVP 0 4 18\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:4 G723/8000\r\na=rtpmap:18 G729A/8000\r\na=ptime:20\r\n"};
        for (int i = 0; i < 20; i++) {
            new Thread(new Runnable(strArr2) {
                String[] messages;

                {
                    this.messages = strArr2;
                }

                @Override
                public void run() {
                    for (int i2 = 0; i2 < this.messages.length; i2++) {
                        try {
                            SIPMessage sIPMessage = new StringMsgParser().parseSIPMessage(this.messages[i2]);
                            System.out.println(" i = " + i2 + " branchId = " + sIPMessage.getTopmostVia().getBranch());
                        } catch (ParseException e) {
                        }
                    }
                }
            }).start();
        }
    }

    public void setStrict(boolean z) {
        this.strict = z;
    }
}
