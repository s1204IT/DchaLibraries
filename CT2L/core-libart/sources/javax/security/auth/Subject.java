package javax.security.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public final class Subject implements Serializable {
    private static final long serialVersionUID = -8308522755600156056L;
    private final Set<Principal> principals;
    private transient SecureSet<Object> privateCredentials;
    private transient SecureSet<Object> publicCredentials;
    private boolean readOnly;
    private static final AuthPermission _AS = new AuthPermission("doAs");
    private static final AuthPermission _AS_PRIVILEGED = new AuthPermission("doAsPrivileged");
    private static final AuthPermission _SUBJECT = new AuthPermission("getSubject");
    private static final AuthPermission _PRINCIPALS = new AuthPermission("modifyPrincipals");
    private static final AuthPermission _PRIVATE_CREDENTIALS = new AuthPermission("modifyPrivateCredentials");
    private static final AuthPermission _PUBLIC_CREDENTIALS = new AuthPermission("modifyPublicCredentials");
    private static final AuthPermission _READ_ONLY = new AuthPermission("setReadOnly");

    public Subject() {
        this.principals = new SecureSet(_PRINCIPALS);
        this.publicCredentials = new SecureSet<>(_PUBLIC_CREDENTIALS);
        this.privateCredentials = new SecureSet<>(_PRIVATE_CREDENTIALS);
        this.readOnly = false;
    }

    public Subject(boolean readOnly, Set<? extends Principal> subjPrincipals, Set<?> pubCredentials, Set<?> privCredentials) {
        if (subjPrincipals == null) {
            throw new NullPointerException("subjPrincipals == null");
        }
        if (pubCredentials == null) {
            throw new NullPointerException("pubCredentials == null");
        }
        if (privCredentials == null) {
            throw new NullPointerException("privCredentials == null");
        }
        this.principals = new SecureSet(this, _PRINCIPALS, subjPrincipals);
        this.publicCredentials = new SecureSet<>(this, _PUBLIC_CREDENTIALS, pubCredentials);
        this.privateCredentials = new SecureSet<>(this, _PRIVATE_CREDENTIALS, privCredentials);
        this.readOnly = readOnly;
    }

    public static <T> T doAs(Subject subject, PrivilegedAction<T> privilegedAction) {
        return (T) doAs_PrivilegedAction(subject, privilegedAction, AccessController.getContext());
    }

    public static <T> T doAsPrivileged(Subject subject, PrivilegedAction<T> privilegedAction, AccessControlContext accessControlContext) {
        return accessControlContext == null ? (T) doAs_PrivilegedAction(subject, privilegedAction, new AccessControlContext(new ProtectionDomain[0])) : (T) doAs_PrivilegedAction(subject, privilegedAction, accessControlContext);
    }

    private static <T> T doAs_PrivilegedAction(Subject subject, PrivilegedAction<T> privilegedAction, final AccessControlContext accessControlContext) {
        final SubjectDomainCombiner subjectDomainCombiner;
        if (subject == null) {
            subjectDomainCombiner = null;
        } else {
            subjectDomainCombiner = new SubjectDomainCombiner(subject);
        }
        return (T) AccessController.doPrivileged(privilegedAction, (AccessControlContext) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new AccessControlContext(accessControlContext, subjectDomainCombiner);
            }
        }));
    }

    public static <T> T doAs(Subject subject, PrivilegedExceptionAction<T> privilegedExceptionAction) throws PrivilegedActionException {
        return (T) doAs_PrivilegedExceptionAction(subject, privilegedExceptionAction, AccessController.getContext());
    }

    public static <T> T doAsPrivileged(Subject subject, PrivilegedExceptionAction<T> privilegedExceptionAction, AccessControlContext accessControlContext) throws PrivilegedActionException {
        return accessControlContext == null ? (T) doAs_PrivilegedExceptionAction(subject, privilegedExceptionAction, new AccessControlContext(new ProtectionDomain[0])) : (T) doAs_PrivilegedExceptionAction(subject, privilegedExceptionAction, accessControlContext);
    }

    private static <T> T doAs_PrivilegedExceptionAction(Subject subject, PrivilegedExceptionAction<T> privilegedExceptionAction, final AccessControlContext accessControlContext) throws PrivilegedActionException {
        final SubjectDomainCombiner subjectDomainCombiner;
        if (subject == null) {
            subjectDomainCombiner = null;
        } else {
            subjectDomainCombiner = new SubjectDomainCombiner(subject);
        }
        return (T) AccessController.doPrivileged(privilegedExceptionAction, (AccessControlContext) AccessController.doPrivileged(new PrivilegedAction<AccessControlContext>() {
            @Override
            public AccessControlContext run() {
                return new AccessControlContext(accessControlContext, subjectDomainCombiner);
            }
        }));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Subject that = (Subject) obj;
        return this.principals.equals(that.principals) && this.publicCredentials.equals(that.publicCredentials) && this.privateCredentials.equals(that.privateCredentials);
    }

    public Set<Principal> getPrincipals() {
        return this.principals;
    }

    public <T extends Principal> Set<T> getPrincipals(Class<T> c) {
        return ((SecureSet) this.principals).get(c);
    }

    public Set<Object> getPrivateCredentials() {
        return this.privateCredentials;
    }

    public <T> Set<T> getPrivateCredentials(Class<T> cls) {
        return (Set<T>) this.privateCredentials.get(cls);
    }

    public Set<Object> getPublicCredentials() {
        return this.publicCredentials;
    }

    public <T> Set<T> getPublicCredentials(Class<T> cls) {
        return (Set<T>) this.publicCredentials.get(cls);
    }

    public int hashCode() {
        return this.principals.hashCode() + this.privateCredentials.hashCode() + this.publicCredentials.hashCode();
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder("Subject:\n");
        Iterator<?> it = this.principals.iterator();
        while (it.hasNext()) {
            buf.append("\tPrincipal: ");
            buf.append(it.next());
            buf.append('\n');
        }
        Iterator<?> it2 = this.publicCredentials.iterator();
        while (it2.hasNext()) {
            buf.append("\tPublic Credential: ");
            buf.append(it2.next());
            buf.append('\n');
        }
        int offset = buf.length() - 1;
        Iterator<?> it3 = this.privateCredentials.iterator();
        while (it3.hasNext()) {
            try {
                buf.append("\tPrivate Credential: ");
                buf.append(it3.next());
                buf.append('\n');
            } catch (SecurityException e) {
                buf.delete(offset, buf.length());
                buf.append("\tPrivate Credentials: no accessible information\n");
            }
        }
        return buf.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.publicCredentials = new SecureSet<>(_PUBLIC_CREDENTIALS);
        this.privateCredentials = new SecureSet<>(_PRIVATE_CREDENTIALS);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public static Subject getSubject(final AccessControlContext context) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        PrivilegedAction<DomainCombiner> action = new PrivilegedAction<DomainCombiner>() {
            @Override
            public DomainCombiner run() {
                return context.getDomainCombiner();
            }
        };
        DomainCombiner combiner = (DomainCombiner) AccessController.doPrivileged(action);
        if (combiner == null || !(combiner instanceof SubjectDomainCombiner)) {
            return null;
        }
        return ((SubjectDomainCombiner) combiner).getSubject();
    }

    private void checkState() {
        if (this.readOnly) {
            throw new IllegalStateException("Set is read-only");
        }
    }

    private final class SecureSet<SST> extends AbstractSet<SST> implements Serializable {
        private static final int SET_Principal = 0;
        private static final int SET_PrivCred = 1;
        private static final int SET_PubCred = 2;
        private static final long serialVersionUID = 7911754171111800359L;
        private LinkedList<SST> elements;
        private transient AuthPermission permission;
        private int setType;

        protected SecureSet(AuthPermission perm) {
            this.permission = perm;
            this.elements = new LinkedList<>();
        }

        protected SecureSet(Subject subject, AuthPermission perm, Collection<? extends SST> s) {
            this(perm);
            boolean trust = s.getClass().getClassLoader() == null;
            for (SST o : s) {
                verifyElement(o);
                if (trust || !this.elements.contains(o)) {
                    this.elements.add(o);
                }
            }
        }

        private void verifyElement(Object o) {
            if (o != null) {
                if (this.permission == Subject._PRINCIPALS && !Principal.class.isAssignableFrom(o.getClass())) {
                    throw new IllegalArgumentException("Element is not instance of java.security.Principal");
                }
                return;
            }
            throw new NullPointerException("o == null");
        }

        @Override
        public boolean add(SST o) {
            verifyElement(o);
            Subject.this.checkState();
            if (this.elements.contains(o)) {
                return false;
            }
            this.elements.add(o);
            return true;
        }

        @Override
        public Iterator<SST> iterator() {
            return this.permission == Subject._PRIVATE_CREDENTIALS ? new SecureSet<SST>.SecureIterator(this.elements.iterator()) {
                @Override
                public SST next() {
                    SST obj = this.iterator.next();
                    return obj;
                }
            } : new SecureIterator(this.elements.iterator());
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException("c == null");
            }
            return super.retainAll(c);
        }

        @Override
        public int size() {
            return this.elements.size();
        }

        protected final <E> Set<E> get(final Class<E> c) {
            if (c == null) {
                throw new NullPointerException("c == null");
            }
            AbstractSet<E> s = new AbstractSet<E>() {
                private LinkedList<E> elements = new LinkedList<>();

                @Override
                public boolean add(E o) {
                    if (!c.isAssignableFrom(o.getClass())) {
                        throw new IllegalArgumentException("Invalid type: " + o.getClass());
                    }
                    if (this.elements.contains(o)) {
                        return false;
                    }
                    this.elements.add(o);
                    return true;
                }

                @Override
                public Iterator<E> iterator() {
                    return this.elements.iterator();
                }

                @Override
                public boolean retainAll(Collection<?> c2) {
                    if (c2 == null) {
                        throw new NullPointerException("c == null");
                    }
                    return super.retainAll(c2);
                }

                @Override
                public int size() {
                    return this.elements.size();
                }
            };
            for (SST o : this) {
                if (c.isAssignableFrom(o.getClass())) {
                    s.add(c.cast(o));
                }
            }
            return s;
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            switch (this.setType) {
                case 0:
                    this.permission = Subject._PRINCIPALS;
                    break;
                case 1:
                    this.permission = Subject._PRIVATE_CREDENTIALS;
                    break;
                case 2:
                    this.permission = Subject._PUBLIC_CREDENTIALS;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            for (SST element : this.elements) {
                verifyElement(element);
            }
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            if (this.permission != Subject._PRIVATE_CREDENTIALS) {
                if (this.permission == Subject._PRINCIPALS) {
                    this.setType = 0;
                } else {
                    this.setType = 2;
                }
            } else {
                this.setType = 1;
            }
            out.defaultWriteObject();
        }

        private class SecureIterator implements Iterator<SST> {
            protected Iterator<SST> iterator;

            protected SecureIterator(Iterator<SST> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasNext() {
                return this.iterator.hasNext();
            }

            @Override
            public SST next() {
                return this.iterator.next();
            }

            @Override
            public void remove() {
                Subject.this.checkState();
                this.iterator.remove();
            }
        }
    }
}
