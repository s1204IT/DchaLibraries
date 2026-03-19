package mf.org.w3c.dom;

public interface CharacterData extends Node {
    void appendData(String str) throws DOMException;

    void deleteData(int i, int i2) throws DOMException;

    String getData() throws DOMException;

    void insertData(int i, String str) throws DOMException;

    void setData(String str) throws DOMException;
}
