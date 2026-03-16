package android.test;

public class InstrumentationUtils {
    public static int getMenuIdentifier(Class cls, String identifier) {
        try {
            Integer field = (Integer) cls.getDeclaredField(identifier).get(cls);
            int id = field.intValue();
            return id;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            return -1;
        }
    }
}
