package java.io;

import java.io.EmulatedFields;
import java.io.ObjectOutputStream;

class EmulatedFieldsForDumping extends ObjectOutputStream.PutField {
    private EmulatedFields emulatedFields;
    private final ObjectOutputStream oos;

    EmulatedFieldsForDumping(ObjectOutputStream oos, ObjectStreamClass streamClass) {
        this.oos = oos;
        this.emulatedFields = new EmulatedFields(streamClass.fields(), (ObjectStreamField[]) null);
    }

    EmulatedFields emulatedFields() {
        return this.emulatedFields;
    }

    @Override
    public void put(String name, byte value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, char value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, double value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, float value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, int value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, long value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, Object value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, short value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    public void put(String name, boolean value) {
        this.emulatedFields.put(name, value);
    }

    @Override
    @Deprecated
    public void write(ObjectOutput output) throws IOException {
        if (!output.equals(this.oos)) {
            throw new IllegalArgumentException("Attempting to write to a different stream than the one that created this PutField");
        }
        EmulatedFields.ObjectSlot[] arr$ = this.emulatedFields.slots();
        for (EmulatedFields.ObjectSlot slot : arr$) {
            Object fieldValue = slot.getFieldValue();
            Class<?> type = slot.getField().getType();
            if (type == Integer.TYPE) {
                output.writeInt(fieldValue != null ? ((Integer) fieldValue).intValue() : 0);
            } else if (type == Byte.TYPE) {
                output.writeByte(fieldValue != null ? ((Byte) fieldValue).byteValue() : (byte) 0);
            } else if (type == Character.TYPE) {
                output.writeChar(fieldValue != null ? ((Character) fieldValue).charValue() : (char) 0);
            } else if (type == Short.TYPE) {
                output.writeShort(fieldValue != null ? ((Short) fieldValue).shortValue() : (short) 0);
            } else if (type == Boolean.TYPE) {
                output.writeBoolean(fieldValue != null ? ((Boolean) fieldValue).booleanValue() : false);
            } else if (type == Long.TYPE) {
                output.writeLong(fieldValue != null ? ((Long) fieldValue).longValue() : 0L);
            } else if (type == Float.TYPE) {
                output.writeFloat(fieldValue != null ? ((Float) fieldValue).floatValue() : 0.0f);
            } else if (type == Double.TYPE) {
                output.writeDouble(fieldValue != null ? ((Double) fieldValue).doubleValue() : 0.0d);
            } else {
                output.writeObject(fieldValue);
            }
        }
    }
}
