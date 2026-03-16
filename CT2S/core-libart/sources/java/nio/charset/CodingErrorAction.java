package java.nio.charset;

public class CodingErrorAction {
    public static final CodingErrorAction IGNORE = new CodingErrorAction("IGNORE");
    public static final CodingErrorAction REPLACE = new CodingErrorAction("REPLACE");
    public static final CodingErrorAction REPORT = new CodingErrorAction("REPORT");
    private String action;

    private CodingErrorAction(String action) {
        this.action = action;
    }

    public String toString() {
        return "Action: " + this.action;
    }
}
