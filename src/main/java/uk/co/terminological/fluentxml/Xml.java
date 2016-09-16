package uk.co.terminological.fluentxml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.oxm.NamespacePrefixMapper;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Xml {

	//Fluent constructors
	public static Logger log = LoggerFactory.getLogger(Xml.class);

	boolean nsScanned = false;
	boolean nsDeepScanned = false;
	
	private Document dom;
	private DocumentBuilderFactory dbf;
	private HashMap<String,String> contexts = new HashMap<String,String>();
	private HashMap<String,String> nScontexts = new HashMap<String,String>();

	public static final URI W3C_XML_SCHEMA_URI = URI.create("http://www.w3.org/2001/XMLSchema");
	public static final URI W3C_XHTML_URI = URI.create("http://www.w3.org/1999/xhtml");
	public static final URI W3C_XHTML_BASIC_URI = URI.create("http://www.w3.org/TR/xhtml-basic/xhtml-basic10.dtd");

	
	
	protected Xml() {
		/*System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
				"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");*/
		dbf = DocumentBuilderFactoryImpl.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setXIncludeAware(true);
		dbf.setValidating(false);
	}

	public static Xml fromDom(Document input) {
		Xml out = new Xml();
		out.dom = input;
		//out.discoverDefaultNs();
		return out;
	}
	
	public Document asDocument() {
		return dom;
	}

	public static Xml create() {
		Xml out = new Xml();
		try {
			out.dom = out.dbf.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		//out.discoverDefaultNs();
		return out;
	}
	
	public XmlDocElement withRoot(String name, URI namespace) {
		Element el = dom.createElementNS(name, namespace.toString());
		dom.appendChild(el);
		return XmlDocElement.from(el);
	}

	public XmlDocElement withRoot(String name) {
		Element el = dom.createElement(name);
		dom.appendChild(el);
		return XmlDocElement.from(el);
	}
	
	public static Xml fromString(String input) throws XmlException {
		try {
			return fromStream(new ByteArrayInputStream(input.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e); 
		}
	}

	public static Xml fromStream(InputStream input) throws XmlException {
		return fromStream(input,null);
	}

	public static Xml fromFile(File file) throws XmlException, FileNotFoundException {
		return fromStream(new FileInputStream(file), file.toURI());
	}

	public static Xml fromFile(File file, File schema) throws XmlException, FileNotFoundException {
		return fromStream(new FileInputStream(file), file.toURI(), schema);
	}
	
	public static Xml fromBytes(byte[] byteArray) throws XmlException {
		return fromStream(new ByteArrayInputStream(byteArray));
	}

	protected void discoverDefaultNs() {
		if (nsScanned) return;
		this.withNamespaceAbbreviation("xs",W3C_XML_SCHEMA_URI);
		if (dom.getDocumentElement() != null) {
			NamedNodeMap nnm = dom.getDocumentElement().getAttributes();
			for (int i=0; i<nnm.getLength(); i++) {
				if (nnm.item(i).getPrefix() != null && nnm.item(i).getPrefix().equals("xmlns")) {
					this.contexts.put(nnm.item(i).getLocalName(), nnm.item(i).getNodeValue());
					this.nScontexts.put(nnm.item(i).getNodeValue(),nnm.item(i).getLocalName());
				};
			}
		}
		nsScanned = true;
	}
	
	protected void deepScanNs() {
		if (nsDeepScanned) return;
		DocumentTraversal dt = (DocumentTraversal) this.dom;
		NodeIterator i = dt.createNodeIterator(this.dom, NodeFilter.SHOW_ELEMENT, 
				null, false);
		Element element = (Element) i.nextNode();
		while (element != null) {
			String prefix = element.getPrefix();
			if (prefix != null && !contexts.containsKey(prefix)) {
				String nsUri = element.getNamespaceURI();
				this.contexts.put(prefix, nsUri);
				this.nScontexts.put(nsUri,prefix);
			}
			NamedNodeMap nnm = element.getAttributes();
			for (int j=0; j<nnm.getLength(); j++) {
				if (nnm.item(j).getPrefix() != null && nnm.item(j).getPrefix().equals("xmlns")) {
					contexts.put(nnm.item(j).getLocalName(), nnm.item(j).getNodeValue());
				};
			}
			element = (Element) i.nextNode();
		}
		nsDeepScanned = true;
	}

	public static Xml fromStream(InputStream input, URI uri) throws XmlException {
		try {
			Xml out = new Xml();
			out.dbf.setValidating(false);
			DocumentBuilder db = out.dbf.newDocumentBuilder();
			db.setEntityResolver(noopEntityResolver());
			if (uri==null) out.dom = db.parse(input);
			else out.dom = db.parse(input, uri.toString());
			return out;
		} catch (ParserConfigurationException | IOException e) {
			throw new RuntimeException(e); 
		} catch (SAXException e) {
			throw new XmlException("Document parsing error", e);
		}
	}

	public static Xml fromStream(InputStream input, URI uri, File schema) throws XmlException {
		try {
			Xml out = new Xml();
			Schema s = SchemaFactory.newInstance(W3C_XML_SCHEMA_URI.toString()).newSchema(schema);
			out.dbf.setSchema(s);
			out.dbf.setValidating(true);
			DocumentBuilder db = out.dbf.newDocumentBuilder();
			db.setEntityResolver(defaultEntityResolver());
			db.setErrorHandler(new ErrorHandler() {
				public void error(SAXParseException arg0) throws SAXException {
					throw new XmlException("Document failed validation: "+arg0.getMessage(),arg0);
				}
				public void fatalError(SAXParseException arg0) throws SAXException {
					throw new XmlException("Document could not be parsed"+arg0.getMessage(),arg0);
				}
				public void warning(SAXParseException arg0) throws SAXException {}
			});
			if (uri==null) out.dom = db.parse(input);
			else out.dom = db.parse(input, uri.toString());
			return out;
		} catch (ParserConfigurationException | IOException e) {
			throw new RuntimeException(e); 
		} catch (SAXException e) {
			throw new XmlException("Document parsing error or invalid schema", e);
		}
	}

	public static Xml fromHtmlStream(InputStream is) throws XmlException {
		HtmlCleaner cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setNamespacesAware(true);
		TagNode node;
		try {
			node = cleaner.clean(is);
			String xml = new SimpleXmlSerializer(props).getAsString(node);
			Xml dom = Xml.fromString(xml);
			log.debug("Cleaning complete");
			return dom;
		} catch (IOException e) {
			throw new XmlException("HtmlCleaner could not process HTML input stream",e);
		}
		/*try {
			return Xml.fromString(Jsoup.parse(is,null,"").outerHtml());
		} catch (IOException e) {
			throw new XmlException("JSoup could not process HTML input stream",e);
		}*/
	}
	
	public static Xml fromJAXB(Object o) throws XmlException {
		return fromJAXB(o,null);
	}

	public static Xml fromJAXB(Object o, String defaultNs) throws XmlException {
		try {
			Map<String,Object> properties = new HashMap<String,Object>();
			Xml out = Xml.create();
			properties.put(JAXBContextProperties.NAMESPACE_PREFIX_MAPPER, out.getNsPrefixMapper());
			if (defaultNs != null) properties.put(JAXBContextProperties.DEFAULT_TARGET_NAMESPACE, defaultNs);
			JAXBContext jc = JAXBContextFactory.createContext(new Class[] {o.getClass()}, properties);
			Marshaller m = jc.createMarshaller();
			m.marshal(o, out.dom);
			return out;
		} catch (JAXBException e) {
			throw new XmlException("Couldn't marshal object to xml: "+o.getClass().getCanonicalName(),e);
		}
	}

	public <T> T toJAXB(Class<T> clzz) throws XmlException {
		return this.content().unmarshalAs(clzz);
	}

	public Xml clone() {
		return Xml.fromDom((Document) dom.cloneNode(true));
	}

	//Fluent setters

	public Xml withNamespaceAbbreviation(String abbrev, URI ns) {
		if (null == contexts) contexts = new HashMap<String,String>();
		contexts.put(abbrev, ns.toString());
		nScontexts.put(ns.toString(),abbrev);
		return this;
	}


	//Fluent actions

	public XmlXsl<XmlDocElement> doTransform() throws XmlException {
		return doTransform((File) null);
	}
	
	public XmlXsl<XmlDocElement> doTransform(File xslt) throws XmlException {
		//this.discoverDefaultNs();
		return new XmlXsl<XmlDocElement>(this.content(), xslt);
	}

	public XmlXsl<? extends XmlNode> doTransform(XmlTransforms xslt) throws XmlException {
		//this.discoverDefaultNs();
		return new XmlXsl<XmlDocElement>(this.content(), xslt.getFile());
	}

	public XmlXPath<XmlDocElement> doXpath(String xpath) throws XmlException {
		return doXpath(xpath, "ns");
	}

	public XmlXPath<XmlDocElement> doXpath(String xpath, String defNsAbbr) throws XmlException {
		//this.discoverDefaultNs();
		return new XmlXPath<XmlDocElement>(this.content(), xpath, defNsAbbr); 
	}

	public <T> T unmarshalAs(Class<T> clzz) throws XmlException {
		return this.content().unmarshalAs(clzz);
	}

	//Utilities / protected getters

	public XmlDocElement content() {
		return new XmlDocElement(this, dom.getDocumentElement());
	}

	protected HashMap<String,String> getAbbrevs() {
		discoverDefaultNs();
		return contexts;
	}

	protected NamespacePrefixMapper getNsPrefixMapper() {
		discoverDefaultNs();
		return new NamespacePrefixMapper() {
			@Override
			public String getPreferredPrefix(java.lang.String namespaceUri, java.lang.String suggestion, boolean requirePrefix) {
				if (nScontexts.containsKey(namespaceUri)) return nScontexts.get(namespaceUri);
				if (requirePrefix) {
					contexts.put(suggestion, namespaceUri);
					nScontexts.put(namespaceUri, suggestion);
					return suggestion;
				} else {
					return null;
				}
			}
		};
	}

	private static EntityResolver noopEntityResolver() {
		return new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
				System.out.println("Ignoring " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			}
		};
	}

	private static EntityResolver defaultEntityResolver() {
		return new EntityResolver() {
			public InputSource resolveEntity (String publicId, String systemId) throws SAXException, IOException
			{
				log.debug("resolving: "+systemId);
				try {
					if (systemId.equals(W3C_XML_SCHEMA_URI.toString())) {
						URL url = Xml.class.getResource("/schema/structures.xsd");
						return new InputSource(new FileReader(url.getFile()));
					} else if (systemId.equals(W3C_XHTML_URI.toString())) {
						URL url = Xml.class.getResource("/schema/xhtml-strict.xsd");
						return new InputSource(new FileReader(url.getFile()));
					} else if (systemId.equals(W3C_XHTML_BASIC_URI.toString())) {
						URL url = Xml.class.getResource("/schema/xhtml-basic11.dtd");
						return new InputSource(new FileReader(url.getFile()));
					}
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
				return noopEntityResolver().resolveEntity(publicId, systemId);
			}
		};
	}
	
	public String toString() {
		return this.content().toString();
	}

	public boolean awareOfPrefix(String prefix) {
		return contexts.containsKey(prefix);
	}

	public static XMLGregorianCalendar dateTime() {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static XMLGregorianCalendar dateTime(final long timeStamp) {
		try {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis(timeStamp);
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static String asString(Document input) {
		try {
			return Xml.fromDom(input).doTransform().asString();
		} catch (XmlException e) {
			//This really shoudln't throw these exceptions unless there is a some sort of unrecoverable problem with the 
			//transformer stack.
			throw new RuntimeException(e);
		}
	}

	public void write(OutputStream out) throws XmlException {
		this.doTransform().write(out);
	}

	

}
