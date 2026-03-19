package sun.nio.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractUserDefinedFileAttributeView implements UserDefinedFileAttributeView, DynamicFileAttributeView {
    static final boolean $assertionsDisabled = false;

    protected AbstractUserDefinedFileAttributeView() {
    }

    protected void checkAccess(String str, boolean z, boolean z2) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            if (z) {
                securityManager.checkRead(str);
            }
            if (z2) {
                securityManager.checkWrite(str);
            }
            securityManager.checkPermission(new RuntimePermission("accessUserDefinedAttributes"));
        }
    }

    @Override
    public final String name() {
        return "user";
    }

    @Override
    public final void setAttribute(String str, Object obj) throws IOException {
        ByteBuffer byteBufferWrap;
        if (obj instanceof byte[]) {
            byteBufferWrap = ByteBuffer.wrap((byte[]) obj);
        } else {
            byteBufferWrap = (ByteBuffer) obj;
        }
        write(str, byteBufferWrap);
    }

    @Override
    public final Map<String, Object> readAttributes(String[] strArr) throws IOException {
        List<String> arrayList = new ArrayList<>();
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String str = strArr[i];
            if (str.equals("*")) {
                arrayList = list();
                break;
            }
            if (str.length() == 0) {
                throw new IllegalArgumentException();
            }
            arrayList.add(str);
            i++;
        }
    }
}
