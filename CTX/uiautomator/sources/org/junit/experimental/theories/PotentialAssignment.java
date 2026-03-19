package org.junit.experimental.theories;

public abstract class PotentialAssignment {
    public abstract String getDescription() throws CouldNotGenerateValueException;

    public abstract Object getValue() throws CouldNotGenerateValueException;

    public static class CouldNotGenerateValueException extends Exception {
        private static final long serialVersionUID = 1;

        public CouldNotGenerateValueException() {
        }

        public CouldNotGenerateValueException(Throwable th) {
            super(th);
        }
    }

    public static PotentialAssignment forValue(final String str, final Object obj) {
        return new PotentialAssignment() {
            @Override
            public Object getValue() {
                return obj;
            }

            public String toString() {
                return String.format("[%s]", obj);
            }

            @Override
            public String getDescription() {
                String str2;
                if (obj == null) {
                    str2 = "null";
                } else {
                    try {
                        str2 = String.format("\"%s\"", obj);
                    } catch (Throwable th) {
                        str2 = String.format("[toString() threw %s: %s]", th.getClass().getSimpleName(), th.getMessage());
                    }
                }
                return String.format("%s <from %s>", str2, str);
            }
        };
    }
}
