package mf.javax.xml.stream.events;

public interface StartDocument extends XMLEvent {
    String getCharacterEncodingScheme();

    String getVersion();
}
