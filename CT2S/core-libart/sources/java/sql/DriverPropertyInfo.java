package java.sql;

public class DriverPropertyInfo {
    public String name;
    public String value;
    public String[] choices = null;
    public String description = null;
    public boolean required = false;

    public DriverPropertyInfo(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
