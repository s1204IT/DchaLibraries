package mf.org.apache.xerces.util;

public final class SynchronizedSymbolTable extends SymbolTable {
    protected SymbolTable fSymbolTable;

    public SynchronizedSymbolTable(SymbolTable symbolTable) {
        this.fSymbolTable = symbolTable;
    }

    public SynchronizedSymbolTable() {
        this.fSymbolTable = new SymbolTable();
    }

    public SynchronizedSymbolTable(int size) {
        this.fSymbolTable = new SymbolTable(size);
    }

    @Override
    public String addSymbol(String symbol) {
        String strAddSymbol;
        synchronized (this.fSymbolTable) {
            strAddSymbol = this.fSymbolTable.addSymbol(symbol);
        }
        return strAddSymbol;
    }

    @Override
    public String addSymbol(char[] buffer, int offset, int length) {
        String strAddSymbol;
        synchronized (this.fSymbolTable) {
            strAddSymbol = this.fSymbolTable.addSymbol(buffer, offset, length);
        }
        return strAddSymbol;
    }

    @Override
    public boolean containsSymbol(String symbol) {
        boolean zContainsSymbol;
        synchronized (this.fSymbolTable) {
            zContainsSymbol = this.fSymbolTable.containsSymbol(symbol);
        }
        return zContainsSymbol;
    }

    @Override
    public boolean containsSymbol(char[] buffer, int offset, int length) {
        boolean zContainsSymbol;
        synchronized (this.fSymbolTable) {
            zContainsSymbol = this.fSymbolTable.containsSymbol(buffer, offset, length);
        }
        return zContainsSymbol;
    }
}
