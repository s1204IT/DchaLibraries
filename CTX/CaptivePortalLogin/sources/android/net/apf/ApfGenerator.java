package android.net.apf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ApfGenerator {
    private boolean mGenerated;
    private final int mVersion;
    private final ArrayList<Instruction> mInstructions = new ArrayList<>();
    private final HashMap<String, Instruction> mLabels = new HashMap<>();
    private final Instruction mDropLabel = new Instruction(this, Opcodes.LABEL);
    private final Instruction mPassLabel = new Instruction(this, Opcodes.LABEL);

    public static class IllegalInstructionException extends Exception {
        IllegalInstructionException(String str) {
            super(str);
        }
    }

    private enum Opcodes {
        LABEL(-1),
        LDB(1),
        LDH(2),
        LDW(3),
        LDBX(4),
        LDHX(5),
        LDWX(6),
        ADD(7),
        MUL(8),
        DIV(9),
        AND(10),
        OR(11),
        SH(12),
        LI(13),
        JMP(14),
        JEQ(15),
        JNE(16),
        JGT(17),
        JLT(18),
        JSET(19),
        JNEBS(20),
        EXT(21),
        LDDW(22),
        STDW(23);

        final int value;

        Opcodes(int i) {
            this.value = i;
        }
    }

    private enum ExtendedOpcodes {
        LDM(0),
        STM(16),
        NOT(32),
        NEG(33),
        SWAP(34),
        MOVE(35);

        final int value;

        ExtendedOpcodes(int i) {
            this.value = i;
        }
    }

    public enum Register {
        R0(0),
        R1(1);

        final int value;

        Register(int i) {
            this.value = i;
        }
    }

    private class Instruction {
        private byte[] mCompareBytes;
        private boolean mHasImm;
        private int mImm;
        private boolean mImmSigned;
        private byte mImmSize;
        private String mLabel;
        private final byte mOpcode;
        private final byte mRegister;
        private String mTargetLabel;
        private byte mTargetLabelSize;
        int offset;

        Instruction(Opcodes opcodes, Register register) {
            this.mOpcode = (byte) opcodes.value;
            this.mRegister = (byte) register.value;
        }

        Instruction(ApfGenerator apfGenerator, Opcodes opcodes) {
            this(opcodes, Register.R0);
        }

        void setImm(int i, boolean z) {
            this.mHasImm = true;
            this.mImm = i;
            this.mImmSigned = z;
            this.mImmSize = calculateImmSize(i, z);
        }

        void setUnsignedImm(int i) {
            setImm(i, false);
        }

        void setSignedImm(int i) {
            setImm(i, true);
        }

        void setLabel(String str) throws IllegalInstructionException {
            if (ApfGenerator.this.mLabels.containsKey(str)) {
                throw new IllegalInstructionException("duplicate label " + str);
            }
            if (this.mOpcode != Opcodes.LABEL.value) {
                throw new IllegalStateException("adding label to non-label instruction");
            }
            this.mLabel = str;
            ApfGenerator.this.mLabels.put(str, this);
        }

        void setTargetLabel(String str) {
            this.mTargetLabel = str;
            this.mTargetLabelSize = (byte) 4;
        }

        void setCompareBytes(byte[] bArr) {
            if (this.mOpcode != Opcodes.JNEBS.value) {
                throw new IllegalStateException("adding compare bytes to non-JNEBS instruction");
            }
            this.mCompareBytes = bArr;
        }

        int size() {
            if (this.mOpcode == Opcodes.LABEL.value) {
                return 0;
            }
            int iGeneratedImmSize = this.mHasImm ? 1 + generatedImmSize() : 1;
            if (this.mTargetLabel != null) {
                iGeneratedImmSize += generatedImmSize();
            }
            if (this.mCompareBytes != null) {
                return iGeneratedImmSize + this.mCompareBytes.length;
            }
            return iGeneratedImmSize;
        }

        boolean shrink() throws IllegalInstructionException {
            if (this.mTargetLabel == null) {
                return false;
            }
            int size = size();
            byte b = this.mTargetLabelSize;
            this.mTargetLabelSize = calculateImmSize(calculateTargetLabelOffset(), false);
            if (this.mTargetLabelSize <= b) {
                return size() < size;
            }
            throw new IllegalStateException("instruction grew");
        }

        private byte generateImmSizeField() {
            byte bGeneratedImmSize = generatedImmSize();
            if (bGeneratedImmSize == 4) {
                return (byte) 3;
            }
            return bGeneratedImmSize;
        }

        private byte generateInstructionByte() {
            return (byte) ((generateImmSizeField() << 1) | (this.mOpcode << 3) | this.mRegister);
        }

        private int writeValue(int i, byte[] bArr, int i2) {
            int iGeneratedImmSize = generatedImmSize() - 1;
            while (iGeneratedImmSize >= 0) {
                bArr[i2] = (byte) ((i >> (iGeneratedImmSize * 8)) & 255);
                iGeneratedImmSize--;
                i2++;
            }
            return i2;
        }

        void generate(byte[] bArr) throws IllegalInstructionException {
            if (this.mOpcode == Opcodes.LABEL.value) {
                return;
            }
            int i = this.offset;
            int length = i + 1;
            bArr[i] = generateInstructionByte();
            if (this.mTargetLabel != null) {
                length = writeValue(calculateTargetLabelOffset(), bArr, length);
            }
            if (this.mHasImm) {
                length = writeValue(this.mImm, bArr, length);
            }
            if (this.mCompareBytes != null) {
                System.arraycopy(this.mCompareBytes, 0, bArr, length, this.mCompareBytes.length);
                length += this.mCompareBytes.length;
            }
            if (length - this.offset != size()) {
                throw new IllegalStateException("wrote " + (length - this.offset) + " but should have written " + size());
            }
        }

        private byte generatedImmSize() {
            return this.mImmSize > this.mTargetLabelSize ? this.mImmSize : this.mTargetLabelSize;
        }

        private int calculateTargetLabelOffset() throws IllegalInstructionException {
            Instruction instruction;
            if (this.mTargetLabel == "__DROP__") {
                instruction = ApfGenerator.this.mDropLabel;
            } else {
                instruction = this.mTargetLabel == "__PASS__" ? ApfGenerator.this.mPassLabel : (Instruction) ApfGenerator.this.mLabels.get(this.mTargetLabel);
            }
            if (instruction == null) {
                throw new IllegalInstructionException("label not found: " + this.mTargetLabel);
            }
            int size = instruction.offset - (this.offset + size());
            if (size < 0) {
                throw new IllegalInstructionException("backward branches disallowed; label: " + this.mTargetLabel);
            }
            return size;
        }

        private byte calculateImmSize(int i, boolean z) {
            if (i == 0) {
                return (byte) 0;
            }
            if (z && i >= -128 && i <= 127) {
                return (byte) 1;
            }
            if (!z && i >= 0 && i <= 255) {
                return (byte) 1;
            }
            if (z && i >= -32768 && i <= 32767) {
                return (byte) 2;
            }
            if (!z && i >= 0 && i <= 65535) {
                return (byte) 2;
            }
            return (byte) 4;
        }
    }

    ApfGenerator(int i) throws IllegalInstructionException {
        this.mVersion = i;
        requireApfVersion(2);
    }

    public static boolean supportsVersion(int i) {
        return i >= 2;
    }

    private void requireApfVersion(int i) throws IllegalInstructionException {
        if (this.mVersion < i) {
            throw new IllegalInstructionException("Requires APF >= " + i);
        }
    }

    private void addInstruction(Instruction instruction) {
        if (this.mGenerated) {
            throw new IllegalStateException("Program already generated");
        }
        this.mInstructions.add(instruction);
    }

    public ApfGenerator defineLabel(String str) throws IllegalInstructionException {
        Instruction instruction = new Instruction(this, Opcodes.LABEL);
        instruction.setLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJump(String str) {
        Instruction instruction = new Instruction(this, Opcodes.JMP);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad8(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDB, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad16(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDH, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad32(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDW, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoad16Indexed(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LDHX, register);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAdd(int i) {
        Instruction instruction = new Instruction(this, Opcodes.ADD);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAnd(int i) {
        Instruction instruction = new Instruction(this, Opcodes.AND);
        instruction.setUnsignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addAddR1() {
        addInstruction(new Instruction(Opcodes.ADD, Register.R1));
        return this;
    }

    public ApfGenerator addLoadImmediate(Register register, int i) {
        Instruction instruction = new Instruction(Opcodes.LI, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0Equals(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JEQ);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0NotEquals(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JNE);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0GreaterThan(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JGT);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0LessThan(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JLT);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfR0AnyBitsSet(int i, String str) {
        Instruction instruction = new Instruction(this, Opcodes.JSET);
        instruction.setUnsignedImm(i);
        instruction.setTargetLabel(str);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addJumpIfBytesNotEqual(Register register, byte[] bArr, String str) throws IllegalInstructionException {
        if (register == Register.R1) {
            throw new IllegalInstructionException("JNEBS fails with R1");
        }
        Instruction instruction = new Instruction(Opcodes.JNEBS, register);
        instruction.setUnsignedImm(bArr.length);
        instruction.setTargetLabel(str);
        instruction.setCompareBytes(bArr);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoadFromMemory(Register register, int i) throws IllegalInstructionException {
        if (i < 0 || i > 15) {
            throw new IllegalInstructionException("illegal memory slot number: " + i);
        }
        Instruction instruction = new Instruction(Opcodes.EXT, register);
        instruction.setUnsignedImm(ExtendedOpcodes.LDM.value + i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addLoadData(Register register, int i) throws IllegalInstructionException {
        requireApfVersion(3);
        Instruction instruction = new Instruction(Opcodes.LDDW, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    public ApfGenerator addStoreData(Register register, int i) throws IllegalInstructionException {
        requireApfVersion(3);
        Instruction instruction = new Instruction(Opcodes.STDW, register);
        instruction.setSignedImm(i);
        addInstruction(instruction);
        return this;
    }

    private int updateInstructionOffsets() {
        int size = 0;
        for (Instruction instruction : this.mInstructions) {
            instruction.offset = size;
            size += instruction.size();
        }
        return size;
    }

    public int programLengthOverEstimate() {
        return updateInstructionOffsets();
    }

    public byte[] generate() throws IllegalInstructionException {
        int iUpdateInstructionOffsets;
        if (this.mGenerated) {
            throw new IllegalStateException("Can only generate() once!");
        }
        this.mGenerated = true;
        int i = 10;
        while (true) {
            iUpdateInstructionOffsets = updateInstructionOffsets();
            this.mDropLabel.offset = iUpdateInstructionOffsets + 1;
            this.mPassLabel.offset = iUpdateInstructionOffsets;
            int i2 = i - 1;
            if (i == 0) {
                break;
            }
            boolean z = false;
            Iterator<Instruction> it = this.mInstructions.iterator();
            while (it.hasNext()) {
                if (it.next().shrink()) {
                    z = true;
                }
            }
            if (!z) {
                break;
            }
            i = i2;
        }
        byte[] bArr = new byte[iUpdateInstructionOffsets];
        Iterator<Instruction> it2 = this.mInstructions.iterator();
        while (it2.hasNext()) {
            it2.next().generate(bArr);
        }
        return bArr;
    }
}
