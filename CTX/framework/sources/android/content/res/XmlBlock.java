package android.content.res;

import android.app.Instrumentation;
import android.util.TypedValue;
import com.android.internal.util.XmlUtils;
import dalvik.annotation.optimization.FastNative;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.xmlpull.v1.XmlPullParserException;

final class XmlBlock implements AutoCloseable {
    private static final boolean DEBUG = false;
    private final AssetManager mAssets;
    private final long mNative;
    private boolean mOpen;
    private int mOpenCount;
    final StringBlock mStrings;

    private static final native long nativeCreate(byte[] bArr, int i, int i2);

    private static final native long nativeCreateParseState(long j);

    private static final native void nativeDestroy(long j);

    private static final native void nativeDestroyParseState(long j);

    @FastNative
    private static final native int nativeGetAttributeCount(long j);

    @FastNative
    private static final native int nativeGetAttributeData(long j, int i);

    @FastNative
    private static final native int nativeGetAttributeDataType(long j, int i);

    @FastNative
    private static final native int nativeGetAttributeIndex(long j, String str, String str2);

    @FastNative
    private static final native int nativeGetAttributeName(long j, int i);

    @FastNative
    private static final native int nativeGetAttributeNamespace(long j, int i);

    @FastNative
    private static final native int nativeGetAttributeResource(long j, int i);

    @FastNative
    private static final native int nativeGetAttributeStringValue(long j, int i);

    @FastNative
    private static final native int nativeGetClassAttribute(long j);

    @FastNative
    private static final native int nativeGetIdAttribute(long j);

    @FastNative
    private static final native int nativeGetLineNumber(long j);

    @FastNative
    static final native int nativeGetName(long j);

    @FastNative
    private static final native int nativeGetNamespace(long j);

    private static final native long nativeGetStringBlock(long j);

    @FastNative
    private static final native int nativeGetStyleAttribute(long j);

    @FastNative
    private static final native int nativeGetText(long j);

    @FastNative
    static final native int nativeNext(long j);

    static int access$008(XmlBlock xmlBlock) {
        int i = xmlBlock.mOpenCount;
        xmlBlock.mOpenCount = i + 1;
        return i;
    }

    public XmlBlock(byte[] bArr) {
        this.mOpen = true;
        this.mOpenCount = 1;
        this.mAssets = null;
        this.mNative = nativeCreate(bArr, 0, bArr.length);
        this.mStrings = new StringBlock(nativeGetStringBlock(this.mNative), false);
    }

    public XmlBlock(byte[] bArr, int i, int i2) {
        this.mOpen = true;
        this.mOpenCount = 1;
        this.mAssets = null;
        this.mNative = nativeCreate(bArr, i, i2);
        this.mStrings = new StringBlock(nativeGetStringBlock(this.mNative), false);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (this.mOpen) {
                this.mOpen = false;
                decOpenCountLocked();
            }
        }
    }

    private void decOpenCountLocked() {
        this.mOpenCount--;
        if (this.mOpenCount == 0) {
            nativeDestroy(this.mNative);
            if (this.mAssets != null) {
                this.mAssets.xmlBlockGone(hashCode());
            }
        }
    }

    public XmlResourceParser newParser() {
        synchronized (this) {
            if (this.mNative != 0) {
                return new Parser(nativeCreateParseState(this.mNative), this);
            }
            return null;
        }
    }

    final class Parser implements XmlResourceParser {
        private final XmlBlock mBlock;
        long mParseState;
        private boolean mStarted = false;
        private boolean mDecNextDepth = false;
        private int mDepth = 0;
        private int mEventType = 0;

        Parser(long j, XmlBlock xmlBlock) {
            this.mParseState = j;
            this.mBlock = xmlBlock;
            XmlBlock.access$008(xmlBlock);
        }

        @Override
        public void setFeature(String str, boolean z) throws XmlPullParserException {
            if ("http://xmlpull.org/v1/doc/features.html#process-namespaces".equals(str) && z) {
                return;
            }
            if ("http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes".equals(str) && z) {
                return;
            }
            throw new XmlPullParserException("Unsupported feature: " + str);
        }

        @Override
        public boolean getFeature(String str) {
            return "http://xmlpull.org/v1/doc/features.html#process-namespaces".equals(str) || "http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes".equals(str);
        }

        @Override
        public void setProperty(String str, Object obj) throws XmlPullParserException {
            throw new XmlPullParserException("setProperty() not supported");
        }

        @Override
        public Object getProperty(String str) {
            return null;
        }

        @Override
        public void setInput(Reader reader) throws XmlPullParserException {
            throw new XmlPullParserException("setInput() not supported");
        }

        @Override
        public void setInput(InputStream inputStream, String str) throws XmlPullParserException {
            throw new XmlPullParserException("setInput() not supported");
        }

        @Override
        public void defineEntityReplacementText(String str, String str2) throws XmlPullParserException {
            throw new XmlPullParserException("defineEntityReplacementText() not supported");
        }

        @Override
        public String getNamespacePrefix(int i) throws XmlPullParserException {
            throw new XmlPullParserException("getNamespacePrefix() not supported");
        }

        @Override
        public String getInputEncoding() {
            return null;
        }

        @Override
        public String getNamespace(String str) {
            throw new RuntimeException("getNamespace() not supported");
        }

        @Override
        public int getNamespaceCount(int i) throws XmlPullParserException {
            throw new XmlPullParserException("getNamespaceCount() not supported");
        }

        @Override
        public String getPositionDescription() {
            return "Binary XML file line #" + getLineNumber();
        }

        @Override
        public String getNamespaceUri(int i) throws XmlPullParserException {
            throw new XmlPullParserException("getNamespaceUri() not supported");
        }

        @Override
        public int getColumnNumber() {
            return -1;
        }

        @Override
        public int getDepth() {
            return this.mDepth;
        }

        @Override
        public String getText() {
            int iNativeGetText = XmlBlock.nativeGetText(this.mParseState);
            if (iNativeGetText >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetText).toString();
            }
            return null;
        }

        @Override
        public int getLineNumber() {
            return XmlBlock.nativeGetLineNumber(this.mParseState);
        }

        @Override
        public int getEventType() throws XmlPullParserException {
            return this.mEventType;
        }

        @Override
        public boolean isWhitespace() throws XmlPullParserException {
            return false;
        }

        @Override
        public String getPrefix() {
            throw new RuntimeException("getPrefix not supported");
        }

        @Override
        public char[] getTextCharacters(int[] iArr) {
            String text = getText();
            if (text != null) {
                iArr[0] = 0;
                iArr[1] = text.length();
                char[] cArr = new char[text.length()];
                text.getChars(0, text.length(), cArr, 0);
                return cArr;
            }
            return null;
        }

        @Override
        public String getNamespace() {
            int iNativeGetNamespace = XmlBlock.nativeGetNamespace(this.mParseState);
            return iNativeGetNamespace >= 0 ? XmlBlock.this.mStrings.get(iNativeGetNamespace).toString() : "";
        }

        @Override
        public String getName() {
            int iNativeGetName = XmlBlock.nativeGetName(this.mParseState);
            if (iNativeGetName >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetName).toString();
            }
            return null;
        }

        @Override
        public String getAttributeNamespace(int i) {
            int iNativeGetAttributeNamespace = XmlBlock.nativeGetAttributeNamespace(this.mParseState, i);
            if (iNativeGetAttributeNamespace >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetAttributeNamespace).toString();
            }
            if (iNativeGetAttributeNamespace == -1) {
                return "";
            }
            throw new IndexOutOfBoundsException(String.valueOf(i));
        }

        @Override
        public String getAttributeName(int i) {
            int iNativeGetAttributeName = XmlBlock.nativeGetAttributeName(this.mParseState, i);
            if (iNativeGetAttributeName >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetAttributeName).toString();
            }
            throw new IndexOutOfBoundsException(String.valueOf(i));
        }

        @Override
        public String getAttributePrefix(int i) {
            throw new RuntimeException("getAttributePrefix not supported");
        }

        @Override
        public boolean isEmptyElementTag() throws XmlPullParserException {
            return false;
        }

        @Override
        public int getAttributeCount() {
            if (this.mEventType == 2) {
                return XmlBlock.nativeGetAttributeCount(this.mParseState);
            }
            return -1;
        }

        @Override
        public String getAttributeValue(int i) {
            int iNativeGetAttributeStringValue = XmlBlock.nativeGetAttributeStringValue(this.mParseState, i);
            if (iNativeGetAttributeStringValue >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetAttributeStringValue).toString();
            }
            int iNativeGetAttributeDataType = XmlBlock.nativeGetAttributeDataType(this.mParseState, i);
            if (iNativeGetAttributeDataType == 0) {
                throw new IndexOutOfBoundsException(String.valueOf(i));
            }
            return TypedValue.coerceToString(iNativeGetAttributeDataType, XmlBlock.nativeGetAttributeData(this.mParseState, i));
        }

        @Override
        public String getAttributeType(int i) {
            return "CDATA";
        }

        @Override
        public boolean isAttributeDefault(int i) {
            return false;
        }

        @Override
        public int nextToken() throws XmlPullParserException, IOException {
            return next();
        }

        @Override
        public String getAttributeValue(String str, String str2) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeValue(iNativeGetAttributeIndex);
            }
            return null;
        }

        @Override
        public int next() throws XmlPullParserException, IOException {
            if (!this.mStarted) {
                this.mStarted = true;
                return 0;
            }
            if (this.mParseState == 0) {
                return 1;
            }
            int iNativeNext = XmlBlock.nativeNext(this.mParseState);
            if (this.mDecNextDepth) {
                this.mDepth--;
                this.mDecNextDepth = false;
            }
            switch (iNativeNext) {
                case 2:
                    this.mDepth++;
                    break;
                case 3:
                    this.mDecNextDepth = true;
                    break;
            }
            this.mEventType = iNativeNext;
            if (iNativeNext == 1) {
                close();
            }
            return iNativeNext;
        }

        @Override
        public void require(int i, String str, String str2) throws XmlPullParserException, IOException {
            if (i != getEventType() || ((str != null && !str.equals(getNamespace())) || (str2 != null && !str2.equals(getName())))) {
                throw new XmlPullParserException("expected " + TYPES[i] + getPositionDescription());
            }
        }

        @Override
        public String nextText() throws XmlPullParserException, IOException {
            if (getEventType() != 2) {
                throw new XmlPullParserException(getPositionDescription() + ": parser must be on START_TAG to read next text", this, null);
            }
            int next = next();
            if (next == 4) {
                String text = getText();
                if (next() != 3) {
                    throw new XmlPullParserException(getPositionDescription() + ": event TEXT it must be immediately followed by END_TAG", this, null);
                }
                return text;
            }
            if (next == 3) {
                return "";
            }
            throw new XmlPullParserException(getPositionDescription() + ": parser must be on START_TAG or TEXT to read text", this, null);
        }

        @Override
        public int nextTag() throws XmlPullParserException, IOException {
            int next = next();
            if (next == 4 && isWhitespace()) {
                next = next();
            }
            if (next != 2 && next != 3) {
                throw new XmlPullParserException(getPositionDescription() + ": expected start or end tag", this, null);
            }
            return next;
        }

        @Override
        public int getAttributeNameResource(int i) {
            return XmlBlock.nativeGetAttributeResource(this.mParseState, i);
        }

        @Override
        public int getAttributeListValue(String str, String str2, String[] strArr, int i) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeListValue(iNativeGetAttributeIndex, strArr, i);
            }
            return i;
        }

        @Override
        public boolean getAttributeBooleanValue(String str, String str2, boolean z) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeBooleanValue(iNativeGetAttributeIndex, z);
            }
            return z;
        }

        @Override
        public int getAttributeResourceValue(String str, String str2, int i) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeResourceValue(iNativeGetAttributeIndex, i);
            }
            return i;
        }

        @Override
        public int getAttributeIntValue(String str, String str2, int i) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeIntValue(iNativeGetAttributeIndex, i);
            }
            return i;
        }

        @Override
        public int getAttributeUnsignedIntValue(String str, String str2, int i) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeUnsignedIntValue(iNativeGetAttributeIndex, i);
            }
            return i;
        }

        @Override
        public float getAttributeFloatValue(String str, String str2, float f) {
            int iNativeGetAttributeIndex = XmlBlock.nativeGetAttributeIndex(this.mParseState, str, str2);
            if (iNativeGetAttributeIndex >= 0) {
                return getAttributeFloatValue(iNativeGetAttributeIndex, f);
            }
            return f;
        }

        @Override
        public int getAttributeListValue(int i, String[] strArr, int i2) {
            int iNativeGetAttributeDataType = XmlBlock.nativeGetAttributeDataType(this.mParseState, i);
            int iNativeGetAttributeData = XmlBlock.nativeGetAttributeData(this.mParseState, i);
            if (iNativeGetAttributeDataType == 3) {
                return XmlUtils.convertValueToList(XmlBlock.this.mStrings.get(iNativeGetAttributeData), strArr, i2);
            }
            return iNativeGetAttributeData;
        }

        @Override
        public boolean getAttributeBooleanValue(int i, boolean z) {
            int iNativeGetAttributeDataType = XmlBlock.nativeGetAttributeDataType(this.mParseState, i);
            if (iNativeGetAttributeDataType < 16 || iNativeGetAttributeDataType > 31) {
                return z;
            }
            return XmlBlock.nativeGetAttributeData(this.mParseState, i) != 0;
        }

        @Override
        public int getAttributeResourceValue(int i, int i2) {
            if (XmlBlock.nativeGetAttributeDataType(this.mParseState, i) == 1) {
                return XmlBlock.nativeGetAttributeData(this.mParseState, i);
            }
            return i2;
        }

        @Override
        public int getAttributeIntValue(int i, int i2) {
            int iNativeGetAttributeDataType = XmlBlock.nativeGetAttributeDataType(this.mParseState, i);
            if (iNativeGetAttributeDataType >= 16 && iNativeGetAttributeDataType <= 31) {
                return XmlBlock.nativeGetAttributeData(this.mParseState, i);
            }
            return i2;
        }

        @Override
        public int getAttributeUnsignedIntValue(int i, int i2) {
            int iNativeGetAttributeDataType = XmlBlock.nativeGetAttributeDataType(this.mParseState, i);
            if (iNativeGetAttributeDataType >= 16 && iNativeGetAttributeDataType <= 31) {
                return XmlBlock.nativeGetAttributeData(this.mParseState, i);
            }
            return i2;
        }

        @Override
        public float getAttributeFloatValue(int i, float f) {
            if (XmlBlock.nativeGetAttributeDataType(this.mParseState, i) == 4) {
                return Float.intBitsToFloat(XmlBlock.nativeGetAttributeData(this.mParseState, i));
            }
            throw new RuntimeException("not a float!");
        }

        @Override
        public String getIdAttribute() {
            int iNativeGetIdAttribute = XmlBlock.nativeGetIdAttribute(this.mParseState);
            if (iNativeGetIdAttribute >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetIdAttribute).toString();
            }
            return null;
        }

        @Override
        public String getClassAttribute() {
            int iNativeGetClassAttribute = XmlBlock.nativeGetClassAttribute(this.mParseState);
            if (iNativeGetClassAttribute >= 0) {
                return XmlBlock.this.mStrings.get(iNativeGetClassAttribute).toString();
            }
            return null;
        }

        @Override
        public int getIdAttributeResourceValue(int i) {
            return getAttributeResourceValue(null, Instrumentation.REPORT_KEY_IDENTIFIER, i);
        }

        @Override
        public int getStyleAttribute() {
            return XmlBlock.nativeGetStyleAttribute(this.mParseState);
        }

        @Override
        public void close() {
            synchronized (this.mBlock) {
                if (this.mParseState != 0) {
                    XmlBlock.nativeDestroyParseState(this.mParseState);
                    this.mParseState = 0L;
                    this.mBlock.decOpenCountLocked();
                }
            }
        }

        protected void finalize() throws Throwable {
            close();
        }

        final CharSequence getPooledString(int i) {
            return XmlBlock.this.mStrings.get(i);
        }
    }

    protected void finalize() throws Throwable {
        close();
    }

    XmlBlock(AssetManager assetManager, long j) {
        this.mOpen = true;
        this.mOpenCount = 1;
        this.mAssets = assetManager;
        this.mNative = j;
        this.mStrings = new StringBlock(nativeGetStringBlock(j), false);
    }
}
