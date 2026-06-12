package uk.co.terminological.fluentxml;

import java.io.File;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Represents text content or CDATA section within an XML document.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * XmlText text = element.withText("hello world");
 * String value = text.getValue();
 * System.out.println(text);  // toString returns getValue()
 * }</pre>
 *
 * @author rchallen
 * @see XmlElement
 * @see XmlNode
 */
public class XmlText extends XmlNode {

	/**
	 * Creates an XmlText wrapping the given DOM text node.
	 * @param xml the parent document
	 * @param node the raw DOM text or CDATA node
	 */
	protected XmlText(Xml xml, Node node) {
		super(xml, node);
	}

	/**
	 * Returns the raw W3C DOM Text node (or CDATA section).
	 * @return the underlying Text node
	 */
	public Text getAsTextNode() {
		return (Text) rawContext;
	}

	/**
	 * Returns the text content of this node.
	 * @return the text string
	 */
	public String getValue() {
		return rawContext.getNodeValue();
	}

	/**
	 * Returns the text content (same as {@link #getValue()}).
	 * @return the text string
	 */
	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public XmlXPath<XmlNode> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath((XmlNode) this, xpath);
	}

	@Override
	public XmlXsl<XmlNode> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt((XmlNode) this, xslt);
	}

	/**
	 * Creates an XmlText from a raw DOM Text or CDATA node.
	 * @param textNode the DOM Text or CDATA node
	 * @return an XmlText wrapping the node
	 * @throws ClassCastException if the node is not a text or CDATA node
	 */
	public static XmlText from(Node textNode) {
		if (textNode.getNodeType() != Node.TEXT_NODE && textNode.getNodeType() != Node.CDATA_SECTION_NODE)
			throw new ClassCastException("Not a text node");
		return new XmlText(Xml.fromDom(textNode.getOwnerDocument()), (Node) textNode);
	}
}
