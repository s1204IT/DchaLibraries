package com.android.internal.telephony.gsm;

import android.telephony.PhoneNumberUtils;
import com.android.internal.midi.MidiConstants;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsAddress;
import java.text.ParseException;

public class GsmSmsAddress extends SmsAddress {
    static final int OFFSET_ADDRESS_LENGTH = 0;
    static final int OFFSET_ADDRESS_VALUE = 2;
    static final int OFFSET_TOA = 1;

    public GsmSmsAddress(byte[] bArr, int i, int i2) throws ParseException {
        this.origBytes = new byte[i2];
        System.arraycopy(bArr, i, this.origBytes, 0, i2);
        int i3 = this.origBytes[0] & 255;
        int i4 = this.origBytes[1] & 255;
        this.ton = (i4 >> 4) & 7;
        if ((i4 & 128) != 128) {
            throw new ParseException("Invalid TOA - high bit must be set. toa = " + i4, i + 1);
        }
        if (isAlphanumeric()) {
            this.address = GsmAlphabet.gsm7BitPackedToString(this.origBytes, 2, (i3 * 4) / 7);
            return;
        }
        int i5 = i2 - 1;
        byte b = this.origBytes[i5];
        if ((i3 & 1) == 1) {
            byte[] bArr2 = this.origBytes;
            bArr2[i5] = (byte) (bArr2[i5] | 240);
        }
        this.address = PhoneNumberUtils.calledPartyBCDToString(this.origBytes, 1, i5, 2);
        this.origBytes[i5] = b;
    }

    @Override
    public String getAddressString() {
        return this.address;
    }

    @Override
    public boolean isAlphanumeric() {
        return this.ton == 5;
    }

    @Override
    public boolean isNetworkSpecific() {
        return this.ton == 3;
    }

    public boolean isCphsVoiceMessageIndicatorAddress() {
        return (this.origBytes[0] & 255) == 4 && isAlphanumeric() && (this.origBytes[1] & MidiConstants.STATUS_CHANNEL_MASK) == 0;
    }

    public boolean isCphsVoiceMessageSet() {
        return isCphsVoiceMessageIndicatorAddress() && (this.origBytes[2] & 255) == 17;
    }

    public boolean isCphsVoiceMessageClear() {
        return isCphsVoiceMessageIndicatorAddress() && (this.origBytes[2] & 255) == 16;
    }
}
