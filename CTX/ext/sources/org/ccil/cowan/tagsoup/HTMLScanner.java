package org.ccil.cowan.tagsoup;

import gov.nist.core.Separators;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Array;
import javax.sip.header.WarningHeader;
import javax.sip.message.Response;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class HTMLScanner implements Scanner, Locator {
    private static final int A_ADUP = 1;
    private static final int A_ADUP_SAVE = 2;
    private static final int A_ADUP_STAGC = 3;
    private static final int A_ANAME = 4;
    private static final int A_ANAME_ADUP = 5;
    private static final int A_ANAME_ADUP_STAGC = 6;
    private static final int A_AVAL = 7;
    private static final int A_AVAL_STAGC = 8;
    private static final int A_CDATA = 9;
    private static final int A_CMNT = 10;
    private static final int A_DECL = 11;
    private static final int A_EMPTYTAG = 12;
    private static final int A_ENTITY = 13;
    private static final int A_ENTITY_START = 14;
    private static final int A_ETAG = 15;
    private static final int A_GI = 16;
    private static final int A_GI_STAGC = 17;
    private static final int A_LT = 18;
    private static final int A_LT_PCDATA = 19;
    private static final int A_MINUS = 20;
    private static final int A_MINUS2 = 21;
    private static final int A_MINUS3 = 22;
    private static final int A_PCDATA = 23;
    private static final int A_PI = 24;
    private static final int A_PITARGET = 25;
    private static final int A_PITARGET_PI = 26;
    private static final int A_SAVE = 27;
    private static final int A_SKIP = 28;
    private static final int A_SP = 29;
    private static final int A_STAGC = 30;
    private static final int A_UNGET = 31;
    private static final int A_UNSAVE_PCDATA = 32;
    private static final int S_ANAME = 1;
    private static final int S_APOS = 2;
    private static final int S_AVAL = 3;
    private static final int S_BB = 4;
    private static final int S_BBC = 5;
    private static final int S_BBCD = 6;
    private static final int S_BBCDA = 7;
    private static final int S_BBCDAT = 8;
    private static final int S_BBCDATA = 9;
    private static final int S_CDATA = 10;
    private static final int S_CDATA2 = 11;
    private static final int S_CDSECT = 12;
    private static final int S_CDSECT1 = 13;
    private static final int S_CDSECT2 = 14;
    private static final int S_COM = 15;
    private static final int S_COM2 = 16;
    private static final int S_COM3 = 17;
    private static final int S_COM4 = 18;
    private static final int S_DECL = 19;
    private static final int S_DECL2 = 20;
    private static final int S_DONE = 21;
    private static final int S_EMPTYTAG = 22;
    private static final int S_ENT = 23;
    private static final int S_EQ = 24;
    private static final int S_ETAG = 25;
    private static final int S_GI = 26;
    private static final int S_NCR = 27;
    private static final int S_PCDATA = 28;
    private static final int S_PI = 29;
    private static final int S_PITARGET = 30;
    private static final int S_QUOT = 31;
    private static final int S_STAGC = 32;
    private static final int S_TAG = 33;
    private static final int S_TAGWS = 34;
    private static final int S_XNCR = 35;
    static short[][] statetableIndex;
    static int statetableIndexMaxChar;
    private int theCurrentColumn;
    private int theCurrentLine;
    private int theLastColumn;
    private int theLastLine;
    int theNextState;
    private String thePublicid;
    int theSize;
    int theState;
    private String theSystemid;
    private static int[] statetable = {1, 47, 5, 22, 1, 61, 4, 3, 1, 62, 6, 28, 1, 0, 27, 1, 1, -1, 6, 21, 1, 32, 4, 24, 1, 10, 4, 24, 1, 9, 4, 24, 2, 39, 7, 34, 2, 0, 27, 2, 2, -1, 8, 21, 2, 32, 29, 2, 2, 10, 29, 2, 2, 9, 29, 2, 3, 34, 28, 31, 3, 39, 28, 2, 3, 62, 8, 28, 3, 0, 27, 32, 3, -1, 8, 21, 3, 32, 28, 3, 3, 10, 28, 3, 3, 9, 28, 3, 4, 67, 28, 5, 4, 0, 28, 19, 4, -1, 28, 21, 5, 68, 28, 6, 5, 0, 28, 19, 5, -1, 28, 21, 6, 65, 28, 7, 6, 0, 28, 19, 6, -1, 28, 21, 7, 84, 28, 8, 7, 0, 28, 19, 7, -1, 28, 21, 8, 65, 28, 9, 8, 0, 28, 19, 8, -1, 28, 21, 9, 91, 28, 12, 9, 0, 28, 19, 9, -1, 28, 21, 10, 60, 27, 11, 10, 0, 27, 10, 10, -1, 23, 21, 11, 47, 32, 25, 11, 0, 27, 10, 11, -1, 32, 21, 12, 93, 27, 13, 12, 0, 27, 12, 12, -1, 28, 21, 13, 93, 27, 14, 13, 0, 27, 12, 13, -1, 28, 21, 14, 62, 9, 28, 14, 0, 27, 12, 14, -1, 28, 21, 15, 45, 28, 16, 15, 0, 27, 16, 15, -1, 10, 21, 16, 45, 28, 17, 16, 0, 27, 16, 16, -1, 10, 21, 17, 45, 28, 18, 17, 0, 20, 16, 17, -1, 10, 21, 18, 45, 22, 18, 18, 62, 10, 28, 18, 0, 21, 16, 18, -1, 10, 21, 19, 45, 28, 15, 19, 62, 28, 28, 19, 91, 28, 4, 19, 0, 27, 20, 19, -1, 28, 21, 20, 62, 11, 28, 20, 0, 27, 20, 20, -1, 28, 21, 22, 62, 12, 28, 22, 0, 27, 1, 22, 32, 28, 34, 22, 10, 28, 34, 22, 9, 28, 34, 23, 0, 13, 23, 23, -1, 13, 21, 24, 61, 28, 3, 24, 62, 3, 28, 24, 0, 2, 1, 24, -1, 3, 21, 24, 32, 28, 24, 24, 10, 28, 24, 24, 9, 28, 24, 25, 62, 15, 28, 25, 0, 27, 25, 25, -1, 15, 21, 25, 32, 28, 25, 25, 10, 28, 25, 25, 9, 28, 25, 26, 47, 28, 22, 26, 62, 17, 28, 26, 0, 27, 26, 26, -1, 28, 21, 26, 32, 16, 34, 26, 10, 16, 34, 26, 9, 16, 34, 27, 0, 13, 27, 27, -1, 13, 21, 28, 38, 14, 23, 28, 60, 23, 33, 28, 0, 27, 28, 28, -1, 23, 21, 29, 62, 24, 28, 29, 0, 27, 29, 29, -1, 24, 21, 30, 62, 26, 28, 30, 0, 27, 30, 30, -1, 26, 21, 30, 32, 25, 29, 30, 10, 25, 29, 30, 9, 25, 29, 31, 34, 7, 34, 31, 0, 27, 31, 31, -1, 8, 21, 31, 32, 29, 31, 31, 10, 29, 31, 31, 9, 29, 31, 32, 62, 8, 28, 32, 0, 27, 32, 32, -1, 8, 21, 32, 32, 7, 34, 32, 10, 7, 34, 32, 9, 7, 34, 33, 33, 28, 19, 33, 47, 28, 25, 33, 60, 27, 33, 33, 63, 28, 30, 33, 0, 27, 26, 33, -1, 19, 21, 33, 32, 18, 28, 33, 10, 18, 28, 33, 9, 18, 28, 34, 47, 28, 22, 34, 62, 30, 28, 34, 0, 27, 1, 34, -1, 30, 21, 34, 32, 28, 34, 34, 10, 28, 34, 34, 9, 28, 34, 35, 0, 13, 35, 35, -1, 13, 21};
    private static final String[] debug_actionnames = {"", "A_ADUP", "A_ADUP_SAVE", "A_ADUP_STAGC", "A_ANAME", "A_ANAME_ADUP", "A_ANAME_ADUP_STAGC", "A_AVAL", "A_AVAL_STAGC", "A_CDATA", "A_CMNT", "A_DECL", "A_EMPTYTAG", "A_ENTITY", "A_ENTITY_START", "A_ETAG", "A_GI", "A_GI_STAGC", "A_LT", "A_LT_PCDATA", "A_MINUS", "A_MINUS2", "A_MINUS3", "A_PCDATA", "A_PI", "A_PITARGET", "A_PITARGET_PI", "A_SAVE", "A_SKIP", "A_SP", "A_STAGC", "A_UNGET", "A_UNSAVE_PCDATA"};
    private static final String[] debug_statenames = {"", "S_ANAME", "S_APOS", "S_AVAL", "S_BB", "S_BBC", "S_BBCD", "S_BBCDA", "S_BBCDAT", "S_BBCDATA", "S_CDATA", "S_CDATA2", "S_CDSECT", "S_CDSECT1", "S_CDSECT2", "S_COM", "S_COM2", "S_COM3", "S_COM4", "S_DECL", "S_DECL2", "S_DONE", "S_EMPTYTAG", "S_ENT", "S_EQ", "S_ETAG", "S_GI", "S_NCR", "S_PCDATA", "S_PI", "S_PITARGET", "S_QUOT", "S_STAGC", "S_TAG", "S_TAGWS", "S_XNCR"};
    char[] theOutputBuffer = new char[Response.OK];
    int[] theWinMap = {8364, 65533, 8218, Response.PAYMENT_REQUIRED, 8222, 8230, 8224, 8225, 710, 8240, 352, 8249, 338, 65533, 381, 65533, 65533, 8216, 8217, 8220, 8221, 8226, 8211, 8212, 732, 8482, 353, 8250, 339, 65533, 382, 376};

    static {
        int i = -1;
        int i2 = -1;
        for (int i3 = 0; i3 < statetable.length; i3 += 4) {
            if (statetable[i3] > i) {
                i = statetable[i3];
            }
            int i4 = i3 + 1;
            if (statetable[i4] > i2) {
                i2 = statetable[i4];
            }
        }
        statetableIndexMaxChar = i2 + 1;
        statetableIndex = (short[][]) Array.newInstance((Class<?>) short.class, i + 1, i2 + 3);
        for (int i5 = 0; i5 <= i; i5++) {
            for (int i6 = -2; i6 <= i2; i6++) {
                int i7 = -1;
                int i8 = 0;
                int i9 = 0;
                while (i8 < statetable.length) {
                    if (i5 != statetable[i8]) {
                        if (i9 != 0) {
                            break;
                        }
                    } else {
                        int i10 = i8 + 1;
                        if (statetable[i10] == 0) {
                            i9 = statetable[i8 + 2];
                            i7 = i8;
                        } else if (statetable[i10] == i6) {
                            int i11 = statetable[i8 + 2];
                            break;
                        }
                    }
                    i8 += 4;
                }
                i8 = i7;
                statetableIndex[i5][i6 + 2] = (short) i8;
            }
        }
    }

    private void unread(PushbackReader pushbackReader, int i) throws IOException {
        if (i != -1) {
            pushbackReader.unread(i);
        }
    }

    @Override
    public int getLineNumber() {
        return this.theLastLine;
    }

    @Override
    public int getColumnNumber() {
        return this.theLastColumn;
    }

    @Override
    public String getPublicId() {
        return this.thePublicid;
    }

    @Override
    public String getSystemId() {
        return this.theSystemid;
    }

    @Override
    public void resetDocumentLocator(String str, String str2) {
        this.thePublicid = str;
        this.theSystemid = str2;
        this.theCurrentColumn = 0;
        this.theCurrentLine = 0;
        this.theLastColumn = 0;
        this.theLastLine = 0;
    }

    @Override
    public void scan(Reader reader, ScanHandler scanHandler) throws SAXException, IOException {
        PushbackReader pushbackReader;
        int i;
        int i2;
        this.theState = 28;
        if (reader instanceof BufferedReader) {
            pushbackReader = new PushbackReader(reader, 5);
        } else {
            pushbackReader = new PushbackReader(new BufferedReader(reader), 5);
        }
        int i3 = pushbackReader.read();
        if (i3 != 65279) {
            unread(pushbackReader, i3);
        }
        while (this.theState != 21) {
            int i4 = pushbackReader.read();
            if (i4 >= 128 && i4 <= 159) {
                i4 = this.theWinMap[i4 - 128];
            }
            if (i4 == 13 && (i4 = pushbackReader.read()) != 10) {
                unread(pushbackReader, i4);
                i4 = 10;
            }
            if (i4 == 10) {
                this.theCurrentLine++;
                this.theCurrentColumn = 0;
            } else {
                this.theCurrentColumn++;
            }
            if (i4 >= 32 || i4 == 10 || i4 == 9 || i4 == -1) {
                if (i4 < -1 || i4 >= statetableIndexMaxChar) {
                    i = -2;
                } else {
                    i = i4;
                }
                short s = statetableIndex[this.theState][i + 2];
                if (s != -1) {
                    i2 = statetable[s + 2];
                    this.theNextState = statetable[s + 3];
                } else {
                    i2 = 0;
                }
                switch (i2) {
                    case 0:
                        throw new Error("HTMLScanner can't cope with " + Integer.toString(i4) + " in state " + Integer.toString(this.theState));
                    case 1:
                        scanHandler.adup(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 2:
                        scanHandler.adup(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case 3:
                        scanHandler.adup(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.stagc(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 4:
                        scanHandler.aname(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 5:
                        scanHandler.aname(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.adup(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 6:
                        scanHandler.aname(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.adup(this.theOutputBuffer, 0, this.theSize);
                        scanHandler.stagc(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 7:
                        scanHandler.aval(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 8:
                        scanHandler.aval(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.stagc(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 9:
                        mark();
                        if (this.theSize > 1) {
                            this.theSize -= 2;
                        }
                        scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.ATTRIBUTE_NOT_UNDERSTOOD:
                        mark();
                        scanHandler.cmnt(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 11:
                        scanHandler.decl(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 12:
                        mark();
                        if (this.theSize > 0) {
                            scanHandler.gi(this.theOutputBuffer, 0, this.theSize);
                        }
                        this.theSize = 0;
                        scanHandler.stage(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 13:
                        mark();
                        char c = (char) i4;
                        if (this.theState == 23 && c == '#') {
                            this.theNextState = 27;
                            save(i4, scanHandler);
                        } else if (this.theState == 27 && (c == 'x' || c == 'X')) {
                            this.theNextState = 35;
                            save(i4, scanHandler);
                        } else if (this.theState == 23 && Character.isLetterOrDigit(c)) {
                            save(i4, scanHandler);
                        } else if (this.theState == 27 && Character.isDigit(c)) {
                            save(i4, scanHandler);
                        } else if (this.theState == 35 && (Character.isDigit(c) || "abcdefABCDEF".indexOf(c) != -1)) {
                            save(i4, scanHandler);
                        } else {
                            scanHandler.entity(this.theOutputBuffer, 1, this.theSize - 1);
                            int entity = scanHandler.getEntity();
                            if (entity != 0) {
                                this.theSize = 0;
                                if (entity >= 128 && entity <= 159) {
                                    entity = this.theWinMap[entity - 128];
                                }
                                if (entity >= 32 && (entity < 55296 || entity > 57343)) {
                                    if (entity <= 65535) {
                                        save(entity, scanHandler);
                                    } else {
                                        int i5 = entity - HTMLModels.M_OPTION;
                                        save((i5 >> 10) + 55296, scanHandler);
                                        save((i5 & 1023) + 56320, scanHandler);
                                    }
                                }
                                if (i4 != 59) {
                                    unread(pushbackReader, i4);
                                    this.theCurrentColumn--;
                                }
                            } else {
                                unread(pushbackReader, i4);
                                this.theCurrentColumn--;
                            }
                            this.theNextState = 28;
                        }
                        this.theState = this.theNextState;
                        break;
                    case 14:
                        scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case 15:
                        scanHandler.etag(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 16:
                        scanHandler.gi(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 17:
                        scanHandler.gi(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.stagc(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 18:
                        mark();
                        save(60, scanHandler);
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case 19:
                        mark();
                        save(60, scanHandler);
                        scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 20:
                        save(45, scanHandler);
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.INCOMPATIBLE_MEDIA_FORMAT:
                        save(45, scanHandler);
                        save(32, scanHandler);
                        save(45, scanHandler);
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.INCOMPATIBLE_NETWORK_ADDRESS_FORMATS:
                        save(45, scanHandler);
                        save(32, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.INCOMPATIBLE_NETWORK_PROTOCOL:
                        mark();
                        scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.INCOMPATIBLE_TRANSPORT_PROTOCOL:
                        mark();
                        scanHandler.pi(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 25:
                        scanHandler.pitarget(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 26:
                        scanHandler.pitarget(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        scanHandler.pi(this.theOutputBuffer, 0, this.theSize);
                        this.theState = this.theNextState;
                        break;
                    case 27:
                        save(i4, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case 28:
                        this.theState = this.theNextState;
                        break;
                    case 29:
                        save(32, scanHandler);
                        this.theState = this.theNextState;
                        break;
                    case WarningHeader.INSUFFICIENT_BANDWIDTH:
                        scanHandler.stagc(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    case 31:
                        unread(pushbackReader, i4);
                        this.theCurrentColumn--;
                        this.theState = this.theNextState;
                        break;
                    case 32:
                        if (this.theSize > 0) {
                            this.theSize--;
                        }
                        scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                        this.theSize = 0;
                        this.theState = this.theNextState;
                        break;
                    default:
                        throw new Error("Can't process state " + i2);
                }
            }
        }
        scanHandler.eof(this.theOutputBuffer, 0, 0);
    }

    private void mark() {
        this.theLastColumn = this.theCurrentColumn;
        this.theLastLine = this.theCurrentLine;
    }

    @Override
    public void startCDATA() {
        this.theNextState = 10;
    }

    private void save(int i, ScanHandler scanHandler) throws SAXException, IOException {
        if (this.theSize >= this.theOutputBuffer.length - 20) {
            if (this.theState == 28 || this.theState == 10) {
                scanHandler.pcdata(this.theOutputBuffer, 0, this.theSize);
                this.theSize = 0;
            } else {
                char[] cArr = new char[this.theOutputBuffer.length * 2];
                System.arraycopy(this.theOutputBuffer, 0, cArr, 0, this.theSize + 1);
                this.theOutputBuffer = cArr;
            }
        }
        char[] cArr2 = this.theOutputBuffer;
        int i2 = this.theSize;
        this.theSize = i2 + 1;
        cArr2[i2] = (char) i;
    }

    public static void main(String[] strArr) throws SAXException, IOException {
        HTMLScanner hTMLScanner = new HTMLScanner();
        InputStreamReader inputStreamReader = new InputStreamReader(System.in, "UTF-8");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(System.out, "UTF-8");
        hTMLScanner.scan(inputStreamReader, new PYXWriter(outputStreamWriter));
        outputStreamWriter.close();
    }

    private static String nicechar(int i) {
        if (i == 10) {
            return "\\n";
        }
        if (i < 32) {
            return "0x" + Integer.toHexString(i);
        }
        return Separators.QUOTE + ((char) i) + Separators.QUOTE;
    }
}
