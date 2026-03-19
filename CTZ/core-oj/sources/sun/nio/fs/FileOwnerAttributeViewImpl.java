package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;

final class FileOwnerAttributeViewImpl implements FileOwnerAttributeView, DynamicFileAttributeView {
    private static final String OWNER_NAME = "owner";
    private final boolean isPosixView = false;
    private final FileAttributeView view;

    FileOwnerAttributeViewImpl(PosixFileAttributeView posixFileAttributeView) {
        this.view = posixFileAttributeView;
    }

    FileOwnerAttributeViewImpl(AclFileAttributeView aclFileAttributeView) {
        this.view = aclFileAttributeView;
    }

    @Override
    public String name() {
        return OWNER_NAME;
    }

    @Override
    public void setAttribute(String str, Object obj) throws IOException {
        if (str.equals(OWNER_NAME)) {
            setOwner((UserPrincipal) obj);
            return;
        }
        throw new IllegalArgumentException("'" + name() + ":" + str + "' not recognized");
    }

    @Override
    public Map<String, Object> readAttributes(String[] strArr) throws IOException {
        HashMap map = new HashMap();
        for (String str : strArr) {
            if (str.equals("*") || str.equals(OWNER_NAME)) {
                map.put(OWNER_NAME, getOwner());
            } else {
                throw new IllegalArgumentException("'" + name() + ":" + str + "' not recognized");
            }
        }
        return map;
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        if (this.isPosixView) {
            return ((PosixFileAttributeView) this.view).readAttributes().owner();
        }
        return ((AclFileAttributeView) this.view).getOwner();
    }

    @Override
    public void setOwner(UserPrincipal userPrincipal) throws IOException {
        if (this.isPosixView) {
            ((PosixFileAttributeView) this.view).setOwner(userPrincipal);
        } else {
            ((AclFileAttributeView) this.view).setOwner(userPrincipal);
        }
    }
}
