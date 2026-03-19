package junit.runner;

public class Version {
    private Version() {
    }

    public static String id() {
        return "4.12-SNAPSHOT";
    }

    public static void main(String[] strArr) {
        System.out.println(id());
    }
}
