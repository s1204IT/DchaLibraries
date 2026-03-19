package java.nio.charset;

import libcore.icu.NativeConverter;

final class CharsetICU extends Charset {
    private final String icuCanonicalName;

    protected CharsetICU(String canonicalName, String icuCanonName, String[] aliases) {
        super(canonicalName, aliases);
        this.icuCanonicalName = icuCanonName;
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
    public boolean contains(Charset cs) {
        if (cs == null) {
            return false;
        }
        if (equals(cs)) {
            return true;
        }
        return NativeConverter.contains(name(), cs.name());
    }
}
