package uk.co.terminological.fluentxml;

import java.io.File;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class XmlText extends XmlNode {

	protected XmlText(Xml xml, Node node) {
		super(xml, node);
	}

	public Text getAsTextNode() {
		return (Text) rawContext;
	}
	
	@Override
	public XmlXsl<XmlNode> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt((XmlNode) this, xslt);
	}

	@Override
	public XmlXPath<XmlNode> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath((XmlNode) this, xpath);
	}

	public static XmlText from(Node textNode) {
		if (textNode.getNodeType() != Node.TEXT_NODE) throw new ClassCastException("Not a text node");
		return new XmlText(Xml.fromDom(textNode.getOwnerDocument()), (Node) textNode);
	}
	
	public String getValue() { return rawContext.getNodeValue(); }
	
	public String toString() {return getValue();}
}
