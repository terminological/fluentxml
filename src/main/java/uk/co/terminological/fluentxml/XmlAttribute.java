package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URI;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

public class XmlAttribute extends XmlNode {

	protected XmlAttribute(Xml xml, Node node) {
		super(xml, node);
	}

	@Override
	public XmlXsl<XmlAttribute> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	public Attr getAsAttribute() {
		return (Attr) rawContext;
	}
	
	public URI getNs() {
		return URI.create(rawContext.getNamespaceURI());
	}
	
	public XmlAttribute setValue(String value) {
		((Attr) getRaw()).setValue(value);
		return this;
	}
	
	@Override
	public XmlXPath<XmlAttribute> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}
	
	public static XmlAttribute from(Attr attr) {
		return new XmlAttribute(Xml.fromDom(attr.getOwnerDocument()), attr);
	}
	
	public String getValue() { return rawContext.getNodeValue(); }
	public String getName() { return rawContext.getNodeName(); }
}
