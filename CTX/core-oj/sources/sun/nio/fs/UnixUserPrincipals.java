package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalNotFoundException;

class UnixUserPrincipals {
    static final User SPECIAL_OWNER = createSpecial("OWNER@");
    static final User SPECIAL_GROUP = createSpecial("GROUP@");
    static final User SPECIAL_EVERYONE = createSpecial("EVERYONE@");

    UnixUserPrincipals() {
    }

    private static User createSpecial(String str) {
        return new User(-1, str);
    }

    static class User implements UserPrincipal {
        private final int id;
        private final boolean isGroup;
        private final String name;

        private User(int i, boolean z, String str) {
            this.id = i;
            this.isGroup = z;
            this.name = str;
        }

        User(int i, String str) {
            this(i, false, str);
        }

        int uid() {
            if (this.isGroup) {
                throw new AssertionError();
            }
            return this.id;
        }

        int gid() {
            if (this.isGroup) {
                return this.id;
            }
            throw new AssertionError();
        }

        boolean isSpecial() {
            return this.id == -1;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof User)) {
                return false;
            }
            User user = (User) obj;
            if (this.id != user.id || this.isGroup != user.isGroup) {
                return false;
            }
            if (this.id != -1 || user.id != -1) {
                return true;
            }
            return this.name.equals(user.name);
        }

        @Override
        public int hashCode() {
            return this.id != -1 ? this.id : this.name.hashCode();
        }
    }

    static class Group extends User implements GroupPrincipal {
        Group(int i, String str) {
            super(i, true, str);
        }
    }

    static User fromUid(int i) {
        String string;
        try {
            string = Util.toString(UnixNativeDispatcher.getpwuid(i));
        } catch (UnixException e) {
            string = Integer.toString(i);
        }
        return new User(i, string);
    }

    static Group fromGid(int i) {
        String string;
        try {
            string = Util.toString(UnixNativeDispatcher.getgrgid(i));
        } catch (UnixException e) {
            string = Integer.toString(i);
        }
        return new Group(i, string);
    }

    private static int lookupName(String str, boolean z) throws IOException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("lookupUserInformation"));
        }
        try {
            int i = z ? UnixNativeDispatcher.getgrnam(str) : UnixNativeDispatcher.getpwnam(str);
            if (i == -1) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    throw new UserPrincipalNotFoundException(str);
                }
            }
            return i;
        } catch (UnixException e2) {
            throw new IOException(str + ": " + e2.errorString());
        }
    }

    static UserPrincipal lookupUser(String str) throws IOException {
        if (str.equals(SPECIAL_OWNER.getName())) {
            return SPECIAL_OWNER;
        }
        if (str.equals(SPECIAL_GROUP.getName())) {
            return SPECIAL_GROUP;
        }
        if (str.equals(SPECIAL_EVERYONE.getName())) {
            return SPECIAL_EVERYONE;
        }
        return new User(lookupName(str, false), str);
    }

    static GroupPrincipal lookupGroup(String str) throws IOException {
        return new Group(lookupName(str, true), str);
    }
}
