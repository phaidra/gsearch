package dk.defxws.fgslucene;

import java.util.Iterator;
import javax.xml.*;
import javax.xml.namespace.NamespaceContext;

public class FOXMLNamespaceContext implements NamespaceContext {

    public String getNamespaceURI(String prefix) {
        if (prefix == null) throw new NullPointerException("Null prefix");
        else if ("foxml".equals(prefix)) return "info:fedora/fedora-system:def/foxml#";
        else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
        return null;
    }

    // This method isn't necessary for XPath processing.
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    // This method isn't necessary for XPath processing either.
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }

}