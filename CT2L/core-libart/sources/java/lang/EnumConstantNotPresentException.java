package java.lang;

public class EnumConstantNotPresentException extends RuntimeException {
    private static final long serialVersionUID = -6046998521960521108L;
    private final String constantName;
    private final Class<? extends Enum> enumType;

    public EnumConstantNotPresentException(Class<? extends Enum> enumType, String constantName) {
        super("enum constant " + enumType.getName() + "." + constantName + " is missing");
        this.enumType = enumType;
        this.constantName = constantName;
    }

    public Class<? extends Enum> enumType() {
        return this.enumType;
    }

    public String constantName() {
        return this.constantName;
    }
}
