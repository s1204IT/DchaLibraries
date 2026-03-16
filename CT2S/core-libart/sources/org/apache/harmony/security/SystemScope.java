package org.apache.harmony.security;

import java.security.Identity;
import java.security.IdentityScope;
import java.security.KeyManagementException;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.Hashtable;

public class SystemScope extends IdentityScope {
    private static final long serialVersionUID = -4810285697932522607L;
    private Hashtable keys;
    private Hashtable names;

    public SystemScope() {
        this.names = new Hashtable();
        this.keys = new Hashtable();
    }

    public SystemScope(String name) {
        super(name);
        this.names = new Hashtable();
        this.keys = new Hashtable();
    }

    public SystemScope(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
        this.names = new Hashtable();
        this.keys = new Hashtable();
    }

    @Override
    public int size() {
        return this.names.size();
    }

    @Override
    public synchronized Identity getIdentity(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        return (Identity) this.names.get(name);
    }

    @Override
    public synchronized Identity getIdentity(PublicKey key) {
        return key == null ? null : (Identity) this.keys.get(key);
    }

    @Override
    public synchronized void addIdentity(Identity identity) throws KeyManagementException {
        if (identity == null) {
            throw new NullPointerException("identity == null");
        }
        String name = identity.getName();
        if (this.names.containsKey(name)) {
            throw new KeyManagementException("name '" + name + "' is already used");
        }
        PublicKey key = identity.getPublicKey();
        if (key != null && this.keys.containsKey(key)) {
            throw new KeyManagementException("key '" + key + "' is already used");
        }
        this.names.put(name, identity);
        if (key != null) {
            this.keys.put(key, identity);
        }
    }

    @Override
    public synchronized void removeIdentity(Identity identity) throws KeyManagementException {
        if (identity == null) {
            throw new NullPointerException("identity == null");
        }
        String name = identity.getName();
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        boolean contains = this.names.containsKey(name);
        this.names.remove(name);
        PublicKey key = identity.getPublicKey();
        if (key != null) {
            contains = contains || this.keys.containsKey(key);
            this.keys.remove(key);
        }
        if (!contains) {
            throw new KeyManagementException("identity not found");
        }
    }

    @Override
    public Enumeration identities() {
        return this.names.elements();
    }
}
