package android.speech.srec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WaveHeader {
    public static final short FORMAT_ALAW = 6;
    public static final short FORMAT_PCM = 1;
    public static final short FORMAT_ULAW = 7;
    private static final int HEADER_LENGTH = 44;
    private static final String TAG = "WaveHeader";
    private short mBitsPerSample;
    private short mFormat;
    private int mNumBytes;
    private short mNumChannels;
    private int mSampleRate;

    public WaveHeader() {
    }

    public WaveHeader(short format, short numChannels, int sampleRate, short bitsPerSample, int numBytes) {
        this.mFormat = format;
        this.mSampleRate = sampleRate;
        this.mNumChannels = numChannels;
        this.mBitsPerSample = bitsPerSample;
        this.mNumBytes = numBytes;
    }

    public short getFormat() {
        return this.mFormat;
    }

    public WaveHeader setFormat(short format) {
        this.mFormat = format;
        return this;
    }

    public short getNumChannels() {
        return this.mNumChannels;
    }

    public WaveHeader setNumChannels(short numChannels) {
        this.mNumChannels = numChannels;
        return this;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public WaveHeader setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
        return this;
    }

    public short getBitsPerSample() {
        return this.mBitsPerSample;
    }

    public WaveHeader setBitsPerSample(short bitsPerSample) {
        this.mBitsPerSample = bitsPerSample;
        return this;
    }

    public int getNumBytes() {
        return this.mNumBytes;
    }

    public WaveHeader setNumBytes(int numBytes) {
        this.mNumBytes = numBytes;
        return this;
    }

    public int read(InputStream in) throws IOException {
        readId(in, "RIFF");
        int i = readInt(in) - 36;
        readId(in, "WAVE");
        readId(in, "fmt ");
        if (16 != readInt(in)) {
            throw new IOException("fmt chunk length not 16");
        }
        this.mFormat = readShort(in);
        this.mNumChannels = readShort(in);
        this.mSampleRate = readInt(in);
        int byteRate = readInt(in);
        short blockAlign = readShort(in);
        this.mBitsPerSample = readShort(in);
        if (byteRate != ((this.mNumChannels * this.mSampleRate) * this.mBitsPerSample) / 8) {
            throw new IOException("fmt.ByteRate field inconsistent");
        }
        if (blockAlign != (this.mNumChannels * this.mBitsPerSample) / 8) {
            throw new IOException("fmt.BlockAlign field inconsistent");
        }
        readId(in, "data");
        this.mNumBytes = readInt(in);
        return 44;
    }

    private static void readId(InputStream in, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != in.read()) {
                throw new IOException(id + " tag not present");
            }
        }
    }

    private static int readInt(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    private static short readShort(InputStream in) throws IOException {
        return (short) (in.read() | (in.read() << 8));
    }

    public int write(OutputStream out) throws IOException {
        writeId(out, "RIFF");
        writeInt(out, this.mNumBytes + 36);
        writeId(out, "WAVE");
        writeId(out, "fmt ");
        writeInt(out, 16);
        writeShort(out, this.mFormat);
        writeShort(out, this.mNumChannels);
        writeInt(out, this.mSampleRate);
        writeInt(out, ((this.mNumChannels * this.mSampleRate) * this.mBitsPerSample) / 8);
        writeShort(out, (short) ((this.mNumChannels * this.mBitsPerSample) / 8));
        writeShort(out, this.mBitsPerSample);
        writeId(out, "data");
        writeInt(out, this.mNumBytes);
        return 44;
    }

    private static void writeId(OutputStream out, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            out.write(id.charAt(i));
        }
    }

    private static void writeInt(OutputStream out, int val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
        out.write(val >> 16);
        out.write(val >> 24);
    }

    private static void writeShort(OutputStream out, short val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
    }

    public String toString() {
        return String.format("WaveHeader format=%d numChannels=%d sampleRate=%d bitsPerSample=%d numBytes=%d", Short.valueOf(this.mFormat), Short.valueOf(this.mNumChannels), Integer.valueOf(this.mSampleRate), Short.valueOf(this.mBitsPerSample), Integer.valueOf(this.mNumBytes));
    }
}
