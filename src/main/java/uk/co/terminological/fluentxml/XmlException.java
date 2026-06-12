package uk.co.terminological.fluentxml;

import org.xml.sax.SAXException;

/**
 * Exception thrown by FluentXML operations when XML parsing, transformation,
 * or XPath evaluation fails.
 *
 * <p>This exception wraps the underlying cause which may be a SAXException,
 * IOException, JAXBException, or other runtime exception depending on the operation.
 *
 * <h2>When thrown</h2>
 * <ul>
 *   <li>{@link Xml#fromStream(InputStream)} - malformed XML</li>
 *   <li>{@link Xml#doXpath(String)} - invalid XPath expression</li>
 *   <li>{@link XmlXPath#getOne(Class)} - zero or multiple results</li>
 *   <li>{@link Xml#doTransform(XmlTransforms)} - XSLT execution failure</li>
 *   <li>{@link XmlElement#unmarshalAs(Class)} - JAXB unmarshalling failure</li>
 * </ul>
 *
 * @author rchallen
 * @see Xml
 * @see XmlXPath
 * @see XmlXsl
 */
public class XmlException extends SAXException {

	/**
	 * Creates an XmlException with a detail message and cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public XmlException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Creates an XmlException with a detail message.
	 * @param message the detail message
	 */
	public XmlException(String message) {
		super(message);
	}
}
