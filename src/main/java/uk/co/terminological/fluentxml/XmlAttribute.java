package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URI;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Represents an XML attribute in a fluent XML document.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * XmlAttribute attr = element.withAttribute("id", "123");
 * String value = attr.getValue();
 * String name = attr.getName();
 * URI ns = attr.getNs();
 * attr.setValue("new-value");
 * }</pre>
 *
 * @author rchallen
 * @see XmlElement
 * @see XmlNode
 */
public class XmlAttribute extends XmlNode {

	/**
	 * Creates an XmlAttribute wrapping the given DOM Attr.
	 * @param xml the parent document
	 * @param node the raw DOM attribute node
	 */
	protected XmlAttribute(Xml xml, Node node) {
		super(xml, node);
	}

	/**
	 * Returns the raw W3C DOM Attr.
	 * @return the underlying Attr
	 */
	public Attr getAsAttribute() {
		return (Attr) rawContext;
	}

	/**
	 * Returns the namespace URI of this attribute.
	 * @return the namespace URI (never null for xmlns attributes)
	 */
	public URI getNs() {
		return URI.create(rawContext.getNamespaceURI());
	}

	/**
	 * Returns the attribute's value.
	 * @return the string value
	 */
	public String getValue() {
		return rawContext.getNodeValue();
	}

	/**
	 * Returns the attribute's name.
	 * @return the attribute name (may include namespace prefix)
	 */
	public String getName() {
		return rawContext.getNodeName();
	}

	/**
	 * Sets the attribute's value.
	 * @param value the new value
	 * @return this attribute for chaining
	 */
	public XmlAttribute setValue(String value) {
		((Attr) getRaw()).setValue(value);
		return this;
	}

	@Override
	public XmlXPath<XmlAttribute> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}

	@Override
	public XmlXsl<XmlAttribute> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	/**
	 * Creates an XmlAttribute from a raw DOM Attr.
	 * @param attr the DOM Attr
	 * @return an XmlAttribute wrapping the attribute
	 */
	public static XmlAttribute from(Attr attr) {
		return new XmlAttribute(Xml.fromDom(attr.getOwnerDocument()), attr);
	}
}
