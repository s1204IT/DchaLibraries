package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class AID_REF_DO extends BerTlv {
    public static final int TAG = 79;
    public static final int TAG_DEFAULT_APPLICATION = 192;
    private byte[] mAid;

    public AID_REF_DO(byte[] bArr, int i, int i2, int i3) {
        super(bArr, i, i2, i3);
        this.mAid = new byte[0];
    }

    public AID_REF_DO(int i, byte[] bArr) {
        super(bArr, i, 0, bArr == null ? 0 : bArr.length);
        this.mAid = new byte[0];
        if (bArr != null) {
            this.mAid = bArr;
        }
    }

    public AID_REF_DO(int i) {
        super(null, i, 0, 0);
        this.mAid = new byte[0];
    }

    public static boolean equals(AID_REF_DO aid_ref_do, AID_REF_DO aid_ref_do2) {
        if (aid_ref_do == null) {
            return aid_ref_do2 == null;
        }
        return aid_ref_do.equals(aid_ref_do2);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sb.append("AID_REF_DO: ");
        try {
            build(byteArrayOutputStream);
            sb.append(BerTlv.toHex(byteArrayOutputStream.toByteArray()));
        } catch (Exception e) {
            sb.append(e.getLocalizedMessage());
        }
        return sb.toString();
    }

    public byte[] getAid() {
        return this.mAid;
    }

    @Override
    public void interpret() throws ParserException {
        this.mAid = null;
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getTag() == 192) {
            if (getValueLength() != 0) {
                throw new ParserException("Invalid value length for AID-REF-DO!");
            }
        } else {
            if (getTag() == 79) {
                if ((getValueLength() < 5 || getValueLength() > 16) && getValueLength() != 0) {
                    throw new ParserException("Invalid value length for AID-REF-DO!");
                }
                if (getValueLength() + valueIndex > rawData.length) {
                    throw new ParserException("Not enough data for AID-REF-DO!");
                }
                this.mAid = new byte[getValueLength()];
                System.arraycopy(rawData, valueIndex, this.mAid, 0, getValueLength());
                return;
            }
            throw new ParserException("Invalid Tag for AID-REF-DO!");
        }
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        if (getTag() == 192) {
            if (this.mAid.length > 0) {
                throw new DO_Exception("No value allowed for default selected application!");
            }
            byteArrayOutputStream.write(getTag());
            byteArrayOutputStream.write(0);
            return;
        }
        if (getTag() == 79) {
            if (getValueLength() != 0 && (getValueLength() < 5 || getValueLength() > 16)) {
                throw new DO_Exception("Invalid length of AID!");
            }
            byteArrayOutputStream.write(getTag());
            byteArrayOutputStream.write(this.mAid.length);
            try {
                byteArrayOutputStream.write(this.mAid);
                return;
            } catch (IOException e) {
                throw new DO_Exception("AID could not be written!");
            }
        }
        throw new DO_Exception("AID-REF-DO must either be C0 or 4F!");
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof AID_REF_DO) && getTag() == obj.getTag()) {
            return Arrays.equals(this.mAid, obj.mAid);
        }
        return false;
    }
}
