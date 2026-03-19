package java.nio.file.attribute;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class AclEntry {
    private final Set<AclEntryFlag> flags;
    private volatile int hash;
    private final Set<AclEntryPermission> perms;
    private final AclEntryType type;
    private final UserPrincipal who;

    private AclEntry(AclEntryType aclEntryType, UserPrincipal userPrincipal, Set<AclEntryPermission> set, Set<AclEntryFlag> set2) {
        this.type = aclEntryType;
        this.who = userPrincipal;
        this.perms = set;
        this.flags = set2;
    }

    public static final class Builder {
        static final boolean $assertionsDisabled = false;
        private Set<AclEntryFlag> flags;
        private Set<AclEntryPermission> perms;
        private AclEntryType type;
        private UserPrincipal who;

        private Builder(AclEntryType aclEntryType, UserPrincipal userPrincipal, Set<AclEntryPermission> set, Set<AclEntryFlag> set2) {
            this.type = aclEntryType;
            this.who = userPrincipal;
            this.perms = set;
            this.flags = set2;
        }

        public AclEntry build() {
            if (this.type == null) {
                throw new IllegalStateException("Missing type component");
            }
            if (this.who == null) {
                throw new IllegalStateException("Missing who component");
            }
            return new AclEntry(this.type, this.who, this.perms, this.flags);
        }

        public Builder setType(AclEntryType aclEntryType) {
            if (aclEntryType == null) {
                throw new NullPointerException();
            }
            this.type = aclEntryType;
            return this;
        }

        public Builder setPrincipal(UserPrincipal userPrincipal) {
            if (userPrincipal == null) {
                throw new NullPointerException();
            }
            this.who = userPrincipal;
            return this;
        }

        private static void checkSet(Set<?> set, Class<?> cls) {
            for (Object obj : set) {
                if (obj == null) {
                    throw new NullPointerException();
                }
                cls.cast(obj);
            }
        }

        public Builder setPermissions(Set<AclEntryPermission> set) {
            Set<AclEntryPermission> setCopyOf;
            if (set.isEmpty()) {
                setCopyOf = Collections.emptySet();
            } else {
                setCopyOf = EnumSet.copyOf(set);
                checkSet(setCopyOf, AclEntryPermission.class);
            }
            this.perms = setCopyOf;
            return this;
        }

        public Builder setPermissions(AclEntryPermission... aclEntryPermissionArr) {
            EnumSet enumSetNoneOf = EnumSet.noneOf(AclEntryPermission.class);
            for (AclEntryPermission aclEntryPermission : aclEntryPermissionArr) {
                if (aclEntryPermission == null) {
                    throw new NullPointerException();
                }
                enumSetNoneOf.add(aclEntryPermission);
            }
            this.perms = enumSetNoneOf;
            return this;
        }

        public Builder setFlags(Set<AclEntryFlag> set) {
            Set<AclEntryFlag> setCopyOf;
            if (set.isEmpty()) {
                setCopyOf = Collections.emptySet();
            } else {
                setCopyOf = EnumSet.copyOf(set);
                checkSet(setCopyOf, AclEntryFlag.class);
            }
            this.flags = setCopyOf;
            return this;
        }

        public Builder setFlags(AclEntryFlag... aclEntryFlagArr) {
            EnumSet enumSetNoneOf = EnumSet.noneOf(AclEntryFlag.class);
            for (AclEntryFlag aclEntryFlag : aclEntryFlagArr) {
                if (aclEntryFlag == null) {
                    throw new NullPointerException();
                }
                enumSetNoneOf.add(aclEntryFlag);
            }
            this.flags = enumSetNoneOf;
            return this;
        }
    }

    public static Builder newBuilder() {
        return new Builder(null, null, Collections.emptySet(), Collections.emptySet());
    }

    public static Builder newBuilder(AclEntry aclEntry) {
        return new Builder(aclEntry.type, aclEntry.who, aclEntry.perms, aclEntry.flags);
    }

    public AclEntryType type() {
        return this.type;
    }

    public UserPrincipal principal() {
        return this.who;
    }

    public Set<AclEntryPermission> permissions() {
        return new HashSet(this.perms);
    }

    public Set<AclEntryFlag> flags() {
        return new HashSet(this.flags);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof AclEntry)) {
            return false;
        }
        AclEntry aclEntry = (AclEntry) obj;
        if (this.type == aclEntry.type && this.who.equals(aclEntry.who) && this.perms.equals(aclEntry.perms) && this.flags.equals(aclEntry.flags)) {
            return true;
        }
        return false;
    }

    private static int hash(int i, Object obj) {
        return (i * 127) + obj.hashCode();
    }

    public int hashCode() {
        if (this.hash != 0) {
            return this.hash;
        }
        this.hash = hash(hash(hash(this.type.hashCode(), this.who), this.perms), this.flags);
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.who.getName());
        sb.append(':');
        Iterator<AclEntryPermission> it = this.perms.iterator();
        while (it.hasNext()) {
            sb.append(it.next().name());
            sb.append('/');
        }
        sb.setLength(sb.length() - 1);
        sb.append(':');
        if (!this.flags.isEmpty()) {
            Iterator<AclEntryFlag> it2 = this.flags.iterator();
            while (it2.hasNext()) {
                sb.append(it2.next().name());
                sb.append('/');
            }
            sb.setLength(sb.length() - 1);
            sb.append(':');
        }
        sb.append(this.type.name());
        return sb.toString();
    }
}
