package libcore.reflect;

import dalvik.bytecode.Opcodes;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import libcore.util.EmptyArray;

public final class GenericSignatureParser {
    char[] buffer;
    private boolean eof;
    public ListOfTypes exceptionTypes;
    public Type fieldType;
    public TypeVariable[] formalTypeParameters;
    GenericDeclaration genericDecl;
    String identifier;
    public ListOfTypes interfaceTypes;
    public ClassLoader loader;
    public ListOfTypes parameterTypes;
    int pos;
    public Type returnType;
    public Type superclassType;
    char symbol;

    public GenericSignatureParser(ClassLoader loader) {
        this.loader = loader;
    }

    void setInput(GenericDeclaration genericDecl, String input) {
        if (input != null) {
            this.genericDecl = genericDecl;
            this.buffer = input.toCharArray();
            this.eof = false;
            scanSymbol();
            return;
        }
        this.eof = true;
    }

    public void parseForClass(GenericDeclaration genericDecl, String signature) {
        setInput(genericDecl, signature);
        if (!this.eof) {
            parseClassSignature();
            return;
        }
        if (genericDecl instanceof Class) {
            Class c = (Class) genericDecl;
            this.formalTypeParameters = EmptyArray.TYPE_VARIABLE;
            this.superclassType = c.getSuperclass();
            Class<?>[] interfaces = c.getInterfaces();
            if (interfaces.length == 0) {
                this.interfaceTypes = ListOfTypes.EMPTY;
                return;
            } else {
                this.interfaceTypes = new ListOfTypes(interfaces);
                return;
            }
        }
        this.formalTypeParameters = EmptyArray.TYPE_VARIABLE;
        this.superclassType = Object.class;
        this.interfaceTypes = ListOfTypes.EMPTY;
    }

    public void parseForMethod(GenericDeclaration genericDecl, String signature, Class<?>[] rawExceptionTypes) {
        setInput(genericDecl, signature);
        if (!this.eof) {
            parseMethodTypeSignature(rawExceptionTypes);
            return;
        }
        Method m = (Method) genericDecl;
        this.formalTypeParameters = EmptyArray.TYPE_VARIABLE;
        Class<?>[] parameterTypes = m.getParameterTypes();
        if (parameterTypes.length == 0) {
            this.parameterTypes = ListOfTypes.EMPTY;
        } else {
            this.parameterTypes = new ListOfTypes(parameterTypes);
        }
        Class<?>[] exceptionTypes = m.getExceptionTypes();
        if (exceptionTypes.length == 0) {
            this.exceptionTypes = ListOfTypes.EMPTY;
        } else {
            this.exceptionTypes = new ListOfTypes(exceptionTypes);
        }
        this.returnType = m.getReturnType();
    }

    public void parseForConstructor(GenericDeclaration genericDecl, String signature, Class<?>[] rawExceptionTypes) {
        setInput(genericDecl, signature);
        if (!this.eof) {
            parseMethodTypeSignature(rawExceptionTypes);
            return;
        }
        Constructor c = (Constructor) genericDecl;
        this.formalTypeParameters = EmptyArray.TYPE_VARIABLE;
        Class<?>[] parameterTypes = c.getParameterTypes();
        if (parameterTypes.length == 0) {
            this.parameterTypes = ListOfTypes.EMPTY;
        } else {
            this.parameterTypes = new ListOfTypes(parameterTypes);
        }
        Class<?>[] exceptionTypes = c.getExceptionTypes();
        if (exceptionTypes.length == 0) {
            this.exceptionTypes = ListOfTypes.EMPTY;
        } else {
            this.exceptionTypes = new ListOfTypes(exceptionTypes);
        }
    }

    public void parseForField(GenericDeclaration genericDecl, String signature) {
        setInput(genericDecl, signature);
        if (!this.eof) {
            this.fieldType = parseFieldTypeSignature();
        }
    }

    void parseClassSignature() {
        parseOptFormalTypeParameters();
        this.superclassType = parseClassTypeSignature();
        this.interfaceTypes = new ListOfTypes(16);
        while (this.symbol > 0) {
            this.interfaceTypes.add(parseClassTypeSignature());
        }
    }

    void parseOptFormalTypeParameters() {
        ListOfVariables typeParams = new ListOfVariables();
        if (this.symbol == '<') {
            scanSymbol();
            typeParams.add(parseFormalTypeParameter());
            while (this.symbol != '>' && this.symbol > 0) {
                typeParams.add(parseFormalTypeParameter());
            }
            expect('>');
        }
        this.formalTypeParameters = typeParams.getArray();
    }

    TypeVariableImpl<GenericDeclaration> parseFormalTypeParameter() {
        scanIdentifier();
        String name = this.identifier.intern();
        ListOfTypes bounds = new ListOfTypes(8);
        expect(':');
        if (this.symbol == 'L' || this.symbol == '[' || this.symbol == 'T') {
            bounds.add(parseFieldTypeSignature());
        }
        while (this.symbol == ':') {
            scanSymbol();
            bounds.add(parseFieldTypeSignature());
        }
        return new TypeVariableImpl<>(this.genericDecl, name, bounds);
    }

    Type parseFieldTypeSignature() {
        switch (this.symbol) {
            case Opcodes.OP_APUT_WIDE:
                return parseClassTypeSignature();
            case Opcodes.OP_IGET_OBJECT:
                return parseTypeVariableSignature();
            case '[':
                scanSymbol();
                return new GenericArrayTypeImpl(parseTypeSignature());
            default:
                throw new GenericSignatureFormatError();
        }
    }

    Type parseClassTypeSignature() {
        expect('L');
        StringBuilder qualIdent = new StringBuilder();
        scanIdentifier();
        while (this.symbol == '/') {
            scanSymbol();
            qualIdent.append(this.identifier).append(".");
            scanIdentifier();
        }
        qualIdent.append(this.identifier);
        ListOfTypes typeArgs = parseOptTypeArguments();
        ParameterizedTypeImpl parentType = new ParameterizedTypeImpl(null, qualIdent.toString(), typeArgs, this.loader);
        ParameterizedTypeImpl type = parentType;
        while (this.symbol == '.') {
            scanSymbol();
            scanIdentifier();
            qualIdent.append("$").append(this.identifier);
            ListOfTypes typeArgs2 = parseOptTypeArguments();
            type = new ParameterizedTypeImpl(parentType, qualIdent.toString(), typeArgs2, this.loader);
        }
        expect(';');
        return type;
    }

    ListOfTypes parseOptTypeArguments() {
        ListOfTypes typeArgs = new ListOfTypes(8);
        if (this.symbol == '<') {
            scanSymbol();
            typeArgs.add(parseTypeArgument());
            while (this.symbol != '>' && this.symbol > 0) {
                typeArgs.add(parseTypeArgument());
            }
            expect('>');
        }
        return typeArgs;
    }

    Type parseTypeArgument() {
        ListOfTypes extendsBound = new ListOfTypes(1);
        ListOfTypes superBound = new ListOfTypes(1);
        if (this.symbol == '*') {
            scanSymbol();
            extendsBound.add(Object.class);
            return new WildcardTypeImpl(extendsBound, superBound);
        }
        if (this.symbol == '+') {
            scanSymbol();
            extendsBound.add(parseFieldTypeSignature());
            return new WildcardTypeImpl(extendsBound, superBound);
        }
        if (this.symbol == '-') {
            scanSymbol();
            superBound.add(parseFieldTypeSignature());
            extendsBound.add(Object.class);
            return new WildcardTypeImpl(extendsBound, superBound);
        }
        return parseFieldTypeSignature();
    }

    TypeVariableImpl<GenericDeclaration> parseTypeVariableSignature() {
        expect('T');
        scanIdentifier();
        expect(';');
        return new TypeVariableImpl<>(this.genericDecl, this.identifier);
    }

    Type parseTypeSignature() {
        switch (this.symbol) {
            case 'B':
                scanSymbol();
                return Byte.TYPE;
            case 'C':
                scanSymbol();
                return Character.TYPE;
            case Opcodes.OP_AGET:
                scanSymbol();
                return Double.TYPE;
            case 'F':
                scanSymbol();
                return Float.TYPE;
            case Opcodes.OP_AGET_CHAR:
                scanSymbol();
                return Integer.TYPE;
            case Opcodes.OP_AGET_SHORT:
                scanSymbol();
                return Long.TYPE;
            case Opcodes.OP_IGET_WIDE:
                scanSymbol();
                return Short.TYPE;
            case Opcodes.OP_IPUT_WIDE:
                scanSymbol();
                return Boolean.TYPE;
            default:
                return parseFieldTypeSignature();
        }
    }

    void parseMethodTypeSignature(Class<?>[] rawExceptionTypes) {
        parseOptFormalTypeParameters();
        this.parameterTypes = new ListOfTypes(16);
        expect('(');
        while (this.symbol != ')' && this.symbol > 0) {
            this.parameterTypes.add(parseTypeSignature());
        }
        expect(')');
        this.returnType = parseReturnType();
        if (this.symbol == '^') {
            this.exceptionTypes = new ListOfTypes(8);
            do {
                scanSymbol();
                if (this.symbol == 'T') {
                    this.exceptionTypes.add(parseTypeVariableSignature());
                } else {
                    this.exceptionTypes.add(parseClassTypeSignature());
                }
            } while (this.symbol == '^');
            return;
        }
        if (rawExceptionTypes != null) {
            this.exceptionTypes = new ListOfTypes(rawExceptionTypes);
        } else {
            this.exceptionTypes = new ListOfTypes(0);
        }
    }

    Type parseReturnType() {
        if (this.symbol != 'V') {
            return parseTypeSignature();
        }
        scanSymbol();
        return Void.TYPE;
    }

    void scanSymbol() {
        if (!this.eof) {
            if (this.pos < this.buffer.length) {
                this.symbol = this.buffer[this.pos];
                this.pos++;
                return;
            } else {
                this.symbol = (char) 0;
                this.eof = true;
                return;
            }
        }
        throw new GenericSignatureFormatError();
    }

    void expect(char c) {
        if (this.symbol == c) {
            scanSymbol();
            return;
        }
        throw new GenericSignatureFormatError();
    }

    static boolean isStopSymbol(char ch) {
        switch (ch) {
            case '.':
            case Opcodes.OP_CMPL_DOUBLE:
            case Opcodes.OP_IF_LTZ:
            case Opcodes.OP_IF_GEZ:
            case Opcodes.OP_IF_GTZ:
                return true;
            default:
                return false;
        }
    }

    void scanIdentifier() {
        if (!this.eof) {
            StringBuilder identBuf = new StringBuilder(32);
            if (!isStopSymbol(this.symbol)) {
                identBuf.append(this.symbol);
                do {
                    char ch = this.buffer[this.pos];
                    if ((ch >= 'a' && ch <= 'z') || ((ch >= 'A' && ch <= 'Z') || !isStopSymbol(ch))) {
                        identBuf.append(ch);
                        this.pos++;
                    } else {
                        this.identifier = identBuf.toString();
                        scanSymbol();
                        return;
                    }
                } while (this.pos != this.buffer.length);
                this.identifier = identBuf.toString();
                this.symbol = (char) 0;
                this.eof = true;
                return;
            }
            this.symbol = (char) 0;
            this.eof = true;
            throw new GenericSignatureFormatError();
        }
        throw new GenericSignatureFormatError();
    }
}
