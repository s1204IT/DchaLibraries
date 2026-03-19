package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class GEOLocationElement extends ANQPElement {
    private static final int ALT_FRACTION_SIZE = 8;
    private static final int ALT_TYPE_WIDTH = 4;
    private static final int ALT_WIDTH = 30;
    private static final int DATUM_WIDTH = 8;
    private static final int ELEMENT_ID = 123;
    private static final int GEO_LOCATION_LENGTH = 16;
    private static final int LL_FRACTION_SIZE = 25;
    private static final int LL_WIDTH = 34;
    private static final double LOG2_FACTOR = 1.0d / Math.log(2.0d);
    private static final int RES_WIDTH = 6;
    private final RealValue mAltitude;
    private final AltitudeType mAltitudeType;
    private final Datum mDatum;
    private final RealValue mLatitude;
    private final RealValue mLongitude;

    public enum AltitudeType {
        Unknown,
        Meters,
        Floors;

        public static AltitudeType[] valuesCustom() {
            return values();
        }
    }

    public enum Datum {
        Unknown,
        WGS84,
        NAD83Land,
        NAD83Water;

        public static Datum[] valuesCustom() {
            return values();
        }
    }

    public static class RealValue {
        private final int mResolution;
        private final boolean mResolutionSet;
        private final double mValue;

        public RealValue(double value) {
            this.mValue = value;
            this.mResolution = Integer.MIN_VALUE;
            this.mResolutionSet = false;
        }

        public RealValue(double value, int resolution) {
            this.mValue = value;
            this.mResolution = resolution;
            this.mResolutionSet = true;
        }

        public double getValue() {
            return this.mValue;
        }

        public boolean isResolutionSet() {
            return this.mResolutionSet;
        }

        public int getResolution() {
            return this.mResolution;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%f", Double.valueOf(this.mValue)));
            if (this.mResolutionSet) {
                sb.append("+/-2^").append(this.mResolution);
            }
            return sb.toString();
        }
    }

    public GEOLocationElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        RealValue realValue;
        RealValue realValue2;
        AltitudeType altitudeType;
        RealValue realValue3;
        super(infoID);
        payload.get();
        int locLength = payload.get() & 255;
        if (locLength != 16) {
            throw new ProtocolException("GeoLocation length field value " + locLength + " incorrect, expected 16");
        }
        if (payload.remaining() != 16) {
            throw new ProtocolException("Bad buffer length " + payload.remaining() + ", expected 16");
        }
        ReverseBitStream reverseBitStream = new ReverseBitStream(payload, null);
        int rawLatRes = (int) reverseBitStream.sliceOff(6);
        double latitude = fixToFloat(reverseBitStream.sliceOff(34), 25, 34);
        if (rawLatRes != 0) {
            realValue = new RealValue(latitude, bitsToAbsResolution(rawLatRes, 34, 25));
        } else {
            realValue = new RealValue(latitude);
        }
        this.mLatitude = realValue;
        int rawLonRes = (int) reverseBitStream.sliceOff(6);
        double longitude = fixToFloat(reverseBitStream.sliceOff(34), 25, 34);
        if (rawLonRes != 0) {
            realValue2 = new RealValue(longitude, bitsToAbsResolution(rawLonRes, 34, 25));
        } else {
            realValue2 = new RealValue(longitude);
        }
        this.mLongitude = realValue2;
        int altType = (int) reverseBitStream.sliceOff(4);
        if (altType < AltitudeType.valuesCustom().length) {
            altitudeType = AltitudeType.valuesCustom()[altType];
        } else {
            altitudeType = AltitudeType.Unknown;
        }
        this.mAltitudeType = altitudeType;
        int rawAltRes = (int) reverseBitStream.sliceOff(6);
        double altitude = fixToFloat(reverseBitStream.sliceOff(30), 8, 30);
        if (rawAltRes != 0) {
            realValue3 = new RealValue(altitude, bitsToAbsResolution(rawAltRes, 30, 8));
        } else {
            realValue3 = new RealValue(altitude);
        }
        this.mAltitude = realValue3;
        int datumValue = (int) reverseBitStream.sliceOff(8);
        this.mDatum = datumValue < Datum.valuesCustom().length ? Datum.valuesCustom()[datumValue] : Datum.Unknown;
    }

    public RealValue getLatitude() {
        return this.mLatitude;
    }

    public RealValue getLongitude() {
        return this.mLongitude;
    }

    public RealValue getAltitude() {
        return this.mAltitude;
    }

    public AltitudeType getAltitudeType() {
        return this.mAltitudeType;
    }

    public Datum getDatum() {
        return this.mDatum;
    }

    public String toString() {
        return "GEOLocation{mLatitude=" + this.mLatitude + ", mLongitude=" + this.mLongitude + ", mAltitude=" + this.mAltitude + ", mAltitudeType=" + this.mAltitudeType + ", mDatum=" + this.mDatum + '}';
    }

    private static class ReverseBitStream {
        private int mBitoffset;
        private final byte[] mOctets;

        ReverseBitStream(ByteBuffer octets, ReverseBitStream reverseBitStream) {
            this(octets);
        }

        private ReverseBitStream(ByteBuffer octets) {
            this.mOctets = new byte[octets.remaining()];
            octets.get(this.mOctets);
        }

        private long sliceOff(int bits) {
            int bn = this.mBitoffset + bits;
            int remaining = bits;
            long value = 0;
            while (this.mBitoffset < bn) {
                int sbit = this.mBitoffset & 7;
                int octet = this.mBitoffset >>> 3;
                int width = Math.min(8 - sbit, remaining);
                value = (value << width) | ((long) getBits(this.mOctets[octet], sbit, width));
                this.mBitoffset += width;
                remaining -= width;
            }
            return value;
        }

        private static int getBits(byte b, int b0, int width) {
            int mask = (1 << width) - 1;
            return (b >> ((8 - b0) - width)) & mask;
        }
    }

    private static class BitStream {
        private int bitOffset;
        private final byte[] data;

        private BitStream(int octets) {
            this.data = new byte[octets];
        }

        private void append(long value, int width) {
            System.out.printf("Appending %x:%d\n", Long.valueOf(value), Integer.valueOf(width));
            int sbit = width - 1;
            while (sbit >= 0) {
                int b0 = this.bitOffset >>> 3;
                int dbit = this.bitOffset & 7;
                int shr = (sbit - 7) + dbit;
                int dmask = 255 >>> dbit;
                if (shr >= 0) {
                    this.data[b0] = (byte) (((long) (this.data[b0] & (~dmask))) | ((value >>> shr) & ((long) dmask)));
                    this.bitOffset += 8 - dbit;
                    sbit -= 8 - dbit;
                } else {
                    this.data[b0] = (byte) (((long) (this.data[b0] & (~dmask))) | ((value << (-shr)) & ((long) dmask)));
                    this.bitOffset += sbit + 1;
                    sbit = -1;
                }
            }
        }

        private byte[] getOctets() {
            return this.data;
        }
    }

    static double fixToFloat(long value, int fractionSize, int width) {
        long sign = 1 << (width - 1);
        if ((value & sign) != 0) {
            return (-((sign - 1) & (-value))) / (1 << fractionSize);
        }
        return ((sign - 1) & value) / (1 << fractionSize);
    }

    private static long floatToFix(double value, int fractionSize, int width) {
        return Math.round((1 << fractionSize) * value) & ((1 << width) - 1);
    }

    private static int getResolution(double variance) {
        return (int) Math.ceil(Math.log(variance) * LOG2_FACTOR);
    }

    private static int absResolutionToBits(int resolution, int fieldWidth, int fractionBits) {
        return ((fieldWidth - fractionBits) - 1) - resolution;
    }

    private static int bitsToAbsResolution(long bits, int fieldWidth, int fractionBits) {
        return ((fieldWidth - fractionBits) - 1) - ((int) bits);
    }
}
