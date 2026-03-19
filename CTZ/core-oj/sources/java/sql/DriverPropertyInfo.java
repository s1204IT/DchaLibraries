package java.sql;

public class DriverPropertyInfo {
    public String name;
    public String value;
    public String description = null;
    public boolean required = false;
    public String[] choices = null;

    public DriverPropertyInfo(String str, String str2) {
        this.value = null;
        this.name = str;
        this.value = str2;
    }
}
