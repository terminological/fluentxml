package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class XmlElement extends XmlNode {

	protected XmlElement(Xml xml, Element element) {
		super(xml, element);
	}

	public Optional<URI> getNs() {
		return rawContext.getNamespaceURI() == null ? Optional.empty() : Optional.of(URI.create(rawContext.getNamespaceURI()));
	}

	@Override
	public XmlXsl<? extends XmlElement> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	@Override
	public XmlXPath<? extends XmlElement> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}

	public static XmlElement from(Element element) {
		return new XmlElement(Xml.fromDom(element.getOwnerDocument()), element);
	}
	
	public Element getAsElement() {
		return (Element) rawContext;
	}
	
	public XmlElement withChildElement(String name) {
		return withChildElement(name,null);
	}
	
	public XmlElement withChildElement(String name, URI namespace) {
		Element el = namespace != null ?
			this.getDom().createElementNS(namespace.toString(), name) :
				this.getDom().createElement(name);
		this.getRaw().appendChild(el);
		return from(el);
	}
	
	public XmlElement up() {
		return XmlElement.from((Element) this.getRaw().getParentNode());
	}
	
	public XmlList<XmlElement> childElements() {
		return XmlList.create(XmlElement.class,((Element) this.getRaw()).getChildNodes());
	}
	
	public Stream<XmlElement> streamChildElements() {
		return childElements().stream();
	}
	
	public Stream<XmlElement> stream() {
		return 
			Stream.concat(
					Stream.of(this), 	
					streamChildElements()
						.flatMap(e -> e.stream()));
	}
	
	public XmlList<XmlAttribute> attributes() {
		return XmlList.create(XmlAttribute.class,((Element) this.getRaw()).getAttributes());
	}
	
	public Stream<XmlAttribute> streamAttributes() {
		return attributes().stream();
	}
	
	public XmlElement withAttribute(String attr, URI namespace, String value) {
		withAttribute(attr, Optional.of(namespace)).setValue(value);
		return this;
	}
	
	public XmlElement withAttribute(String attr, String value) {
		withAttribute(attr, this.getNs()).setValue(value);
		return this;
	}
	
	public XmlAttribute withAttribute(String attr) {
		return withAttribute(attr, this.getNs());
	}
	
	public XmlAttribute withAttribute(String attr, Optional<URI> namespace) {
		if (namespace.isPresent()) {
			Attr at = this.getDom().createAttributeNS(namespace.get().toString(), attr);
			((Element) this.getRaw()).setAttributeNode(at);
			return XmlAttribute.from(at);
		} else {
			Attr at = this.getDom().createAttribute(attr);
			((Element) this.getRaw()).setAttributeNodeNS(at);
			return XmlAttribute.from(at);
		}
	}
	
	public XmlText withText(String text) {
		Text tn = this.getDom().createTextNode(text);
		((Element) this.getRaw()).appendChild(tn);
		return XmlText.from(tn);
	}
	
	public XmlElement appendText(String text) {
		Text tn = this.getDom().createTextNode(text);
		((Element) this.getRaw()).appendChild(tn);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T unmarshalAs(Class<T> clzz) throws XmlException {
		try {
			Map<String,Object> properties = new HashMap<String,Object>();
			properties.put(JAXBContextProperties.NAMESPACE_PREFIX_MAPPER, xml.getNsPrefixMapper());
			if (this.getNs().isPresent()) properties.put(JAXBContextProperties.DEFAULT_TARGET_NAMESPACE, this.getNs().get());
			JAXBContext jc = JAXBContextFactory.createContext(new Class[] {clzz}, properties);
			Unmarshaller u = jc.createUnmarshaller();
			return (T) u.unmarshal(this.getRaw());
		} catch (ClassCastException | JAXBException e) {
			throw new XmlException("Could not convert Xml to "+clzz.getCanonicalName(),e);
		}
	}

	public XmlXsl<? extends XmlElement> doTransform(XmlTransforms switchNs) throws XmlException {
		return this.doTransform(switchNs.getFile());
	}
	
	public String getName() { return rawContext.getNodeName(); }
}
