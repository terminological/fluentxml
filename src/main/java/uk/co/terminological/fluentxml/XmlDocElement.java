package uk.co.terminological.fluentxml;

import java.io.File;

import org.w3c.dom.Element;

public class XmlDocElement extends XmlElement {
	
	protected XmlDocElement(Xml xml,Element dom) {
		super(xml,dom);
	}
	
	@Override
	public XmlXsl<XmlDocElement> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	@Override
	public XmlXPath<XmlDocElement> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}
	
	public static XmlDocElement from(Element element) {
		return new XmlDocElement(Xml.fromDom(element.getOwnerDocument()), element);
	}
	
}
