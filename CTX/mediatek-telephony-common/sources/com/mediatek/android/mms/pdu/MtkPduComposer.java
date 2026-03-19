package com.mediatek.android.mms.pdu;

import android.content.Context;
import android.hardware.radio.V1_0.RadioCdmaSmsConst;
import android.util.Log;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPart;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MtkPduComposer extends PduComposer {
    private static final String LOG_TAG = "MtkPduComposer";
    private boolean mForBackup;

    public MtkPduComposer(Context context, GenericPdu genericPdu) {
        super(context, genericPdu);
        this.mForBackup = false;
    }

    public byte[] make() {
        int messageType = this.mPdu.getMessageType();
        Log.d(LOG_TAG, "make, type = " + messageType);
        switch (messageType) {
            case 128:
                if (makeSendReqPduEx() != 0) {
                    return null;
                }
                break;
            case 129:
            case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
            default:
                return null;
            case 130:
                if (makeNotifyIndEx() != 0) {
                    return null;
                }
                break;
            case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                if (makeNotifyRespEx() != 0) {
                    return null;
                }
                break;
            case 132:
                if (makeRetrievePduEx() != 0) {
                    return null;
                }
                break;
            case 133:
                if (makeAckInd() != 0) {
                    return null;
                }
                break;
            case 135:
                if (makeReadRecInd() != 0) {
                    return null;
                }
                break;
        }
        return this.mMessage.toByteArray();
    }

    private int makeSendReqPduEx() throws Throwable {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(128);
        appendOctet(152);
        byte[] textString = this.mPduHeader.getTextString(152);
        if (textString == null) {
            throw new IllegalArgumentException("Transaction-ID is null.");
        }
        appendTextString(textString);
        if (appendHeader(141) != 0) {
            return 1;
        }
        appendHeader(133);
        if (appendHeader(MtkPduHeaders.STATE_SKIP_RETRYING) != 0) {
            return 1;
        }
        boolean z = appendHeader(151) != 1;
        if (appendHeader(130) != 1) {
            z = true;
        }
        if (appendHeader(129) != 1) {
            z = true;
        }
        if (!z) {
            return 1;
        }
        appendHeader(150);
        appendHeader(138);
        appendHeader(136);
        appendHeader(143);
        appendHeader(RadioCdmaSmsConst.UDH_VAR_PIC_SIZE);
        appendHeader(144);
        appendOctet(132);
        makeMessageBodyEx(2);
        return 0;
    }

    private int makeNotifyIndEx() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(130);
        if (appendHeader(152) != 0 || appendHeader(141) != 0 || appendHeader(138) != 0) {
            return 1;
        }
        appendOctet(142);
        appendLongInteger(this.mPdu.getMessageSize());
        if (appendHeader(136) != 0) {
            return 1;
        }
        appendOctet(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX);
        byte[] contentLocation = this.mPdu.getContentLocation();
        if (contentLocation != null) {
            Log.d(LOG_TAG, "makeNotifyIndEx contentLocation != null");
            appendTextString(contentLocation);
        } else {
            Log.d(LOG_TAG, "makeNotifyIndEx contentLocation  = null");
        }
        EncodedStringValue subject = this.mPdu.getSubject();
        if (subject != null) {
            Log.d(LOG_TAG, "makeNotifyIndEx subject != null");
            appendOctet(150);
            appendEncodedString(subject);
        } else {
            Log.d(LOG_TAG, "makeNotifyIndEx subject  = null");
        }
        appendHeader(133);
        return (appendHeader(MtkPduHeaders.STATE_SKIP_RETRYING) == 0 && appendHeader(149) == 0) ? 0 : 1;
    }

    private int makeRetrievePduEx() throws Throwable {
        Log.d(LOG_TAG, "makeRetrievePduEx begin");
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(132);
        byte[] textString = this.mPduHeader.getTextString(152);
        if (textString == null) {
            Log.d(LOG_TAG, "Transaction ID is null");
        } else {
            appendOctet(152);
            appendTextString(textString);
        }
        if (appendHeader(141) != 0) {
            return 1;
        }
        appendHeader(133);
        if (appendHeader(MtkPduHeaders.STATE_SKIP_RETRYING) != 0) {
            return 1;
        }
        boolean z = appendHeader(151) != 1;
        if (appendHeader(130) != 1) {
            z = true;
        }
        if (appendHeader(129) != 1) {
            z = true;
        }
        if (!z) {
            return 1;
        }
        appendHeader(150);
        appendHeader(138);
        appendHeader(136);
        appendHeader(143);
        appendHeader(RadioCdmaSmsConst.UDH_VAR_PIC_SIZE);
        appendHeader(144);
        if (this.mForBackup) {
            appendHeader(155);
            Log.d(LOG_TAG, "set DATE_SENT");
            appendHeader(201);
        }
        appendOctet(132);
        makeMessageBodyEx(1);
        Log.d(LOG_TAG, "makeRetrievePduEx end");
        return 0;
    }

    private int makeMessageBodyEx(int i) throws Throwable {
        InputStream inputStreamOpenInputStream;
        int length;
        this.mStack.newbuf();
        PduComposer.PositionMarker positionMarkerMark = this.mStack.mark();
        Integer num = (Integer) mContentTypeMap.get(new String(this.mPduHeader.getTextString(132)));
        if (num == null) {
            return 1;
        }
        appendShortInteger(num.intValue());
        PduBody body = i == 1 ? this.mPdu.getBody() : i == 2 ? this.mPdu.getBody() : null;
        if (body == null || body.getPartsNum() == 0) {
            Log.d(LOG_TAG, "makeMessageBodyEx body == null");
            appendUintvarInteger(0L);
            this.mStack.pop();
            this.mStack.copy();
            return 0;
        }
        try {
            PduPart part = body.getPart(0);
            byte[] contentId = part.getContentId();
            if (contentId != null) {
                appendOctet(138);
                if (60 == contentId[0] && 62 == contentId[contentId.length - 1]) {
                    appendTextString(contentId);
                } else {
                    appendTextString("<" + new String(contentId) + ">");
                }
            }
            appendOctet(MtkPduHeaders.STATE_SKIP_RETRYING);
            appendTextString(part.getContentType());
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        int length2 = positionMarkerMark.getLength();
        this.mStack.pop();
        appendValueLength(length2);
        this.mStack.copy();
        int partsNum = body.getPartsNum();
        appendUintvarInteger(partsNum);
        for (int i2 = 0; i2 < partsNum; i2++) {
            MtkPduPart mtkPduPart = (MtkPduPart) body.getPart(i2);
            this.mStack.newbuf();
            PduComposer.PositionMarker positionMarkerMark2 = this.mStack.mark();
            this.mStack.newbuf();
            PduComposer.PositionMarker positionMarkerMark3 = this.mStack.mark();
            byte[] contentType = mtkPduPart.getContentType();
            if (contentType == null) {
                return 1;
            }
            Integer num2 = (Integer) mContentTypeMap.get(new String(contentType));
            if (num2 == null) {
                appendTextString(contentType);
            } else {
                appendShortInteger(num2.intValue());
            }
            byte[] name = mtkPduPart.getName();
            if ((name == null || name.length == 0) && (((name = mtkPduPart.getFilename()) == null || name.length == 0) && ((name = mtkPduPart.getContentLocation()) == null || name.length == 0))) {
                name = mtkPduPart.getContentId();
                if (name == null || name.length == 0) {
                    return 1;
                }
                Log.d(LOG_TAG, "makeMessageBodyEx name 1= " + name.toString());
            }
            if (name != null && name.length != 0) {
                Log.d(LOG_TAG, "makeMessageBodyEx name 2= " + name.toString());
            }
            appendOctet(133);
            appendTextString(name);
            int charset = mtkPduPart.getCharset();
            if (charset != 0) {
                appendOctet(129);
                appendShortInteger(charset);
            }
            int length3 = positionMarkerMark3.getLength();
            this.mStack.pop();
            appendValueLength(length3);
            this.mStack.copy();
            byte[] contentId2 = mtkPduPart.getContentId();
            if (contentId2 != null && contentId2.length != 0) {
                appendOctet(192);
                if (60 == contentId2[0] && 62 == contentId2[contentId2.length - 1]) {
                    appendQuotedString(contentId2);
                } else {
                    appendQuotedString("<" + new String(contentId2) + ">");
                }
            }
            byte[] contentLocation = mtkPduPart.getContentLocation();
            if (contentLocation != null && contentLocation.length != 0) {
                appendOctet(142);
                appendTextString(contentLocation);
            }
            int length4 = positionMarkerMark2.getLength();
            byte[] data = mtkPduPart.getData();
            if (data != null) {
                arraycopy(data, 0, data.length);
                length = data.length;
            } else {
                try {
                    byte[] bArr = new byte[1024];
                    inputStreamOpenInputStream = this.mResolver.openInputStream(mtkPduPart.getDataUri());
                    int i3 = 0;
                    while (true) {
                        try {
                            int i4 = inputStreamOpenInputStream.read(bArr);
                            if (i4 == -1) {
                                break;
                            }
                            this.mMessage.write(bArr, 0, i4);
                            this.mPosition += i4;
                            i3 += i4;
                        } catch (FileNotFoundException e2) {
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e3) {
                                }
                            }
                            return 1;
                        } catch (IOException e4) {
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e5) {
                                }
                            }
                            return 1;
                        } catch (RuntimeException e6) {
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e7) {
                                }
                            }
                            return 1;
                        } catch (Throwable th) {
                            th = th;
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e8) {
                                }
                            }
                            throw th;
                        }
                    }
                    if (inputStreamOpenInputStream != null) {
                        try {
                            inputStreamOpenInputStream.close();
                        } catch (IOException e9) {
                        }
                    }
                    length = i3;
                } catch (FileNotFoundException e10) {
                    inputStreamOpenInputStream = null;
                } catch (IOException e11) {
                    inputStreamOpenInputStream = null;
                } catch (RuntimeException e12) {
                    inputStreamOpenInputStream = null;
                } catch (Throwable th2) {
                    th = th2;
                    inputStreamOpenInputStream = null;
                }
            }
            if (length != positionMarkerMark2.getLength() - length4) {
                throw new RuntimeException("BUG: Length sanity check failed");
            }
            this.mStack.pop();
            appendUintvarInteger(length4);
            appendUintvarInteger(length);
            this.mStack.copy();
        }
        return 0;
    }

    private int makeNotifyRespEx() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX);
        if (appendHeader(152) != 0 || appendHeader(141) != 0 || appendHeader(149) != 0) {
            return 1;
        }
        appendHeader(145);
        return 0;
    }

    protected int appendHeader(int i) {
        if (i != 136) {
            if (i == 201) {
                Log.d(LOG_TAG, "DATE_SENT");
                long longInteger = this.mPduHeader.getLongInteger(i);
                Log.d(LOG_TAG, "date_sent = " + longInteger);
                if (-1 == longInteger) {
                    return 2;
                }
                appendOctet(i);
                appendDateValue(longInteger);
                return 0;
            }
            return super.appendHeader(i);
        }
        Log.d(LOG_TAG, "EXPIRY");
        long longInteger2 = this.mPduHeader.getLongInteger(i);
        if (-1 == longInteger2) {
            return 2;
        }
        appendOctet(i);
        this.mStack.newbuf();
        PduComposer.PositionMarker positionMarkerMark = this.mStack.mark();
        if (this.mForBackup) {
            Log.e(LOG_TAG, "absolute token");
            append(128);
        } else {
            Log.e(LOG_TAG, "relative token");
            append(129);
        }
        appendLongInteger(longInteger2);
        int length = positionMarkerMark.getLength();
        this.mStack.pop();
        appendValueLength(length);
        this.mStack.copy();
        return 0;
    }

    public byte[] make(boolean z) {
        this.mForBackup = z;
        return make();
    }
}
