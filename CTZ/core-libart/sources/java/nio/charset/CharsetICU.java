package java.nio.charset;

import libcore.icu.NativeConverter;

final class CharsetICU extends Charset {
    private final String icuCanonicalName;

    protected CharsetICU(String str, String str2, String[] strArr) {
        super(str, strArr);
        this.icuCanonicalName = str2;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return CharsetDecoderICU.newInstance(this, this.icuCanonicalName);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return CharsetEncoderICU.newInstance(this, this.icuCanonicalName);
    }

    @Override
    public boolean contains(Charset charset) {
        if (charset == null) {
            return false;
        }
        if (equals(charset)) {
            return true;
        }
        return NativeConverter.contains(name(), charset.name());
    }
}
