package uk.co.terminological.fluentxml;

import org.xml.sax.SAXException;

public class XmlException extends SAXException {

	public XmlException(String string, Exception arg0) {
		super(string,arg0);
	}

	public XmlException(String string) {
		super(string);
	}
}
