package java.sql;

class DriverInfo {
    final Driver driver;

    DriverInfo(Driver driver) {
        this.driver = driver;
    }

    public boolean equals(Object obj) {
        return (obj instanceof DriverInfo) && this.driver == ((DriverInfo) obj).driver;
    }

    public int hashCode() {
        return this.driver.hashCode();
    }

    public String toString() {
        return "driver[className=" + ((Object) this.driver) + "]";
    }
}
