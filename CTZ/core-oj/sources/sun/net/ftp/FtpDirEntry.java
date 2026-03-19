package sun.net.ftp;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class FtpDirEntry {
    private Date created;
    private HashMap<String, String> facts;
    private String group;
    private Date lastModified;
    private final String name;
    private boolean[][] permissions;
    private long size;
    private Type type;
    private String user;

    public enum Type {
        FILE,
        DIR,
        PDIR,
        CDIR,
        LINK
    }

    public enum Permission {
        USER(0),
        GROUP(1),
        OTHERS(2);

        int value;

        Permission(int i) {
            this.value = i;
        }
    }

    private FtpDirEntry() {
        this.user = null;
        this.group = null;
        this.size = -1L;
        this.created = null;
        this.lastModified = null;
        this.type = Type.FILE;
        this.permissions = null;
        this.facts = new HashMap<>();
        this.name = null;
    }

    public FtpDirEntry(String str) {
        this.user = null;
        this.group = null;
        this.size = -1L;
        this.created = null;
        this.lastModified = null;
        this.type = Type.FILE;
        this.permissions = null;
        this.facts = new HashMap<>();
        this.name = str;
    }

    public String getName() {
        return this.name;
    }

    public String getUser() {
        return this.user;
    }

    public FtpDirEntry setUser(String str) {
        this.user = str;
        return this;
    }

    public String getGroup() {
        return this.group;
    }

    public FtpDirEntry setGroup(String str) {
        this.group = str;
        return this;
    }

    public long getSize() {
        return this.size;
    }

    public FtpDirEntry setSize(long j) {
        this.size = j;
        return this;
    }

    public Type getType() {
        return this.type;
    }

    public FtpDirEntry setType(Type type) {
        this.type = type;
        return this;
    }

    public Date getLastModified() {
        return this.lastModified;
    }

    public FtpDirEntry setLastModified(Date date) {
        this.lastModified = date;
        return this;
    }

    public boolean canRead(Permission permission) {
        if (this.permissions != null) {
            return this.permissions[permission.value][0];
        }
        return false;
    }

    public boolean canWrite(Permission permission) {
        if (this.permissions != null) {
            return this.permissions[permission.value][1];
        }
        return false;
    }

    public boolean canExexcute(Permission permission) {
        if (this.permissions != null) {
            return this.permissions[permission.value][2];
        }
        return false;
    }

    public FtpDirEntry setPermissions(boolean[][] zArr) {
        this.permissions = zArr;
        return this;
    }

    public FtpDirEntry addFact(String str, String str2) {
        this.facts.put(str.toLowerCase(), str2);
        return this;
    }

    public String getFact(String str) {
        return this.facts.get(str.toLowerCase());
    }

    public Date getCreated() {
        return this.created;
    }

    public FtpDirEntry setCreated(Date date) {
        this.created = date;
        return this;
    }

    public String toString() {
        if (this.lastModified == null) {
            return this.name + " [" + ((Object) this.type) + "] (" + this.user + " / " + this.group + ") " + this.size;
        }
        return this.name + " [" + ((Object) this.type) + "] (" + this.user + " / " + this.group + ") {" + this.size + "} " + DateFormat.getDateInstance().format(this.lastModified);
    }
}
