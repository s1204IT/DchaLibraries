package java.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

final class UnresolvedPermissionCollection extends PermissionCollection implements Serializable {
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("permissions", Hashtable.class)};
    private static final long serialVersionUID = -7176153071733132400L;
    private transient Map<String, List<UnresolvedPermission>> perms = new HashMap(11);

    @Override
    public void add(Permission permission) {
        List<UnresolvedPermission> arrayList;
        if (!(permission instanceof UnresolvedPermission)) {
            throw new IllegalArgumentException("invalid permission: " + ((Object) permission));
        }
        UnresolvedPermission unresolvedPermission = (UnresolvedPermission) permission;
        synchronized (this) {
            arrayList = this.perms.get(unresolvedPermission.getName());
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.perms.put(unresolvedPermission.getName(), arrayList);
            }
        }
        synchronized (arrayList) {
            arrayList.add(unresolvedPermission);
        }
    }

    List<UnresolvedPermission> getUnresolvedPermissions(Permission permission) {
        List<UnresolvedPermission> list;
        synchronized (this) {
            list = this.perms.get(permission.getClass().getName());
        }
        return list;
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        ArrayList arrayList = new ArrayList();
        synchronized (this) {
            for (List<UnresolvedPermission> list : this.perms.values()) {
                synchronized (list) {
                    arrayList.addAll(list);
                }
            }
        }
        return Collections.enumeration(arrayList);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        Hashtable hashtable = new Hashtable(this.perms.size() * 2);
        synchronized (this) {
            for (Map.Entry<String, List<UnresolvedPermission>> entry : this.perms.entrySet()) {
                List<UnresolvedPermission> value = entry.getValue();
                Vector vector = new Vector(value.size());
                synchronized (value) {
                    vector.addAll(value);
                }
                hashtable.put(entry.getKey(), vector);
            }
        }
        objectOutputStream.putFields().put("permissions", hashtable);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        Hashtable hashtable = (Hashtable) objectInputStream.readFields().get("permissions", (Object) null);
        this.perms = new HashMap(hashtable.size() * 2);
        for (Map.Entry entry : hashtable.entrySet()) {
            Vector vector = (Vector) entry.getValue();
            ArrayList arrayList = new ArrayList(vector.size());
            arrayList.addAll(vector);
            this.perms.put((String) entry.getKey(), arrayList);
        }
    }
}
