package java.io;

class EmulatedFields {
    private ObjectStreamField[] declaredFields;
    private ObjectSlot[] slotsToSerialize;

    static class ObjectSlot {
        boolean defaulted = true;
        ObjectStreamField field;
        Object fieldValue;

        ObjectSlot() {
        }

        public ObjectStreamField getField() {
            return this.field;
        }

        public Object getFieldValue() {
            return this.fieldValue;
        }
    }

    public EmulatedFields(ObjectStreamField[] fields, ObjectStreamField[] declared) {
        buildSlots(fields);
        this.declaredFields = declared;
    }

    private void buildSlots(ObjectStreamField[] fields) {
        this.slotsToSerialize = new ObjectSlot[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectSlot s = new ObjectSlot();
            this.slotsToSerialize[i] = s;
            s.field = fields[i];
        }
    }

    public boolean defaulted(String name) throws IllegalArgumentException {
        ObjectSlot slot = findSlot(name, null);
        if (slot == null) {
            throw new IllegalArgumentException("no field '" + name + "'");
        }
        return slot.defaulted;
    }

    private ObjectSlot findSlot(String fieldName, Class<?> fieldType) {
        boolean isPrimitive = fieldType != null && fieldType.isPrimitive();
        for (int i = 0; i < this.slotsToSerialize.length; i++) {
            ObjectSlot slot = this.slotsToSerialize[i];
            if (slot.field.getName().equals(fieldName)) {
                if (isPrimitive) {
                    if (slot.field.getType() == fieldType) {
                        return slot;
                    }
                } else if (fieldType == null || slot.field.getType().isAssignableFrom(fieldType)) {
                    return slot;
                }
            }
        }
        if (this.declaredFields != null) {
            for (int i2 = 0; i2 < this.declaredFields.length; i2++) {
                ObjectStreamField field = this.declaredFields[i2];
                if (field.getName().equals(fieldName)) {
                    if (isPrimitive) {
                        if (fieldType == field.getType()) {
                            ObjectSlot slot2 = new ObjectSlot();
                            slot2.field = field;
                            slot2.defaulted = true;
                            return slot2;
                        }
                    } else if (fieldType == null || field.getType().isAssignableFrom(fieldType)) {
                        ObjectSlot slot22 = new ObjectSlot();
                        slot22.field = field;
                        slot22.defaulted = true;
                        return slot22;
                    }
                }
            }
        }
        return null;
    }

    private ObjectSlot findMandatorySlot(String name, Class<?> type) {
        ObjectSlot slot = findSlot(name, type);
        if (slot == null || (type == null && slot.field.getType().isPrimitive())) {
            throw new IllegalArgumentException("no field '" + name + "' of type " + type);
        }
        return slot;
    }

    public byte get(String name, byte defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Byte.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        byte defaultValue2 = ((Byte) slot.fieldValue).byteValue();
        return defaultValue2;
    }

    public char get(String name, char defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Character.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        char defaultValue2 = ((Character) slot.fieldValue).charValue();
        return defaultValue2;
    }

    public double get(String name, double defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Double.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        double defaultValue2 = ((Double) slot.fieldValue).doubleValue();
        return defaultValue2;
    }

    public float get(String name, float defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Float.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        float defaultValue2 = ((Float) slot.fieldValue).floatValue();
        return defaultValue2;
    }

    public int get(String name, int defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Integer.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        int defaultValue2 = ((Integer) slot.fieldValue).intValue();
        return defaultValue2;
    }

    public long get(String name, long defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Long.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        long defaultValue2 = ((Long) slot.fieldValue).longValue();
        return defaultValue2;
    }

    public Object get(String name, Object defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, null);
        if (slot.defaulted) {
            return defaultValue;
        }
        Object defaultValue2 = slot.fieldValue;
        return defaultValue2;
    }

    public short get(String name, short defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Short.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        short defaultValue2 = ((Short) slot.fieldValue).shortValue();
        return defaultValue2;
    }

    public boolean get(String name, boolean defaultValue) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Boolean.TYPE);
        if (slot.defaulted) {
            return defaultValue;
        }
        boolean defaultValue2 = ((Boolean) slot.fieldValue).booleanValue();
        return defaultValue2;
    }

    public void put(String name, byte value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Byte.TYPE);
        slot.fieldValue = Byte.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, char value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Character.TYPE);
        slot.fieldValue = Character.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, double value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Double.TYPE);
        slot.fieldValue = Double.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, float value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Float.TYPE);
        slot.fieldValue = Float.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, int value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Integer.TYPE);
        slot.fieldValue = Integer.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, long value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Long.TYPE);
        slot.fieldValue = Long.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, Object value) throws IllegalArgumentException {
        Class<?> valueClass = null;
        if (value != null) {
            valueClass = value.getClass();
        }
        ObjectSlot slot = findMandatorySlot(name, valueClass);
        slot.fieldValue = value;
        slot.defaulted = false;
    }

    public void put(String name, short value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Short.TYPE);
        slot.fieldValue = Short.valueOf(value);
        slot.defaulted = false;
    }

    public void put(String name, boolean value) throws IllegalArgumentException {
        ObjectSlot slot = findMandatorySlot(name, Boolean.TYPE);
        slot.fieldValue = Boolean.valueOf(value);
        slot.defaulted = false;
    }

    public ObjectSlot[] slots() {
        return this.slotsToSerialize;
    }
}
