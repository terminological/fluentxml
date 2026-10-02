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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
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

/**
 * Entry point for the FluentXML library. Provides factory methods for creating and loading XML documents,
 * as well as fluent methods for XPath querying, XSLT transformation, JAXB binding, and output.
 *
 * <h2>Creating XML from scratch</h2>
 * <pre>{@code
 * Xml xml = Xml.create();
 * XmlElement items = xml
 *     .withRoot("root")
 *     .withChildElement("items");
 * items.withChildElement("item")
 *     .withAttribute("id", "1")
 *     .withText("value 1");
 * xml.write(System.out);
 * }</pre>
 *
 * <h2>Loading XML</h2>
 * <pre>{@code
 * Xml xml = Xml.fromFile(new File("data.xml"));
 * Xml xml = Xml.fromString("<root><item>hello</item></root>");
 * Xml xml = Xml.fromStream(inputStream);
 * Xml xml = Xml.fromHtmlStream(htmlInputStream);  // Auto-clean malformed HTML
 * }</pre>
 *
 * <h2>XPath queries (XPath 2.0)</h2>
 * <pre>{@code
 * XmlAttribute attr = xml.doXpath("/root/item/@id").getOne(XmlAttribute.class);
 * XmlList<XmlElement> items = xml.doXpath("//item").getMany(XmlElement.class);
 * }</pre>
 *
 * <h2>XSLT transformation</h2>
 * <pre>{@code
 * String json = xml.doTransform(XmlTransforms.XML_TO_JSON)
 *     .withProperty("use-badgerfish", "true")
 *     .withProperty("skip-root", "true")
 *     .asString();
 * }</pre>
 *
 * <h2>JAXB binding</h2>
 * <pre>{@code
 * MyObject obj = Xml.fromJAXB(source).toJAXB(MyObject.class);
 * Xml xml = Xml.fromJAXB(myObject);
 * }</pre>
 *
 * <h2>Built-in transforms</h2>
 * <ul>
 *   <li>{@link XmlTransforms#ELEMENTS_TO_LOWER_CASE}</li>
 *   <li>{@link XmlTransforms#ELEMENTS_TO_UPPER_CASE}</li>
 *   <li>{@link XmlTransforms#ATTRIB_TO_ELEMENTS}</li>
 *   <li>{@link XmlTransforms#STRIP_NS}</li>
 *   <li>{@link XmlTransforms#STRIP_COMMENTS}</li>
 *   <li>{@link XmlTransforms#XML_TO_YAML}</li>
 *   <li>{@link XmlTransforms#XML_TO_JSON}</li>
 *   <li>{@link XmlTransforms#XHTML_TO_MARKDOWN}</li>
 *   <li>{@link XmlTransforms#XHTML_TO_TEXT}</li>
 *   <li>{@link XmlTransforms#XHTML_TABLE_TO_CSV}</li>
 * </ul>
 *
 * @author rchallen
 * @see XmlElement
 * @see XmlDocElement
 * @see XmlNode
 * @see XmlXPath
 * @see XmlXsl
 * @see XmlTransforms
 * @see XmlList
 */
public class Xml {

	/** Fluent XML namespace abbreviation context: maps prefix/abbreviation to namespace URI */
	HashMap<String,String> contexts = new HashMap<String,String>();
	/** Reverse lookup: namespace URI to abbreviation */
	HashMap<String,String> nScontexts = new HashMap<String,String>();

	private Document dom;
	private DocumentBuilderFactory dbf;
	private boolean nsScanned = false;
	private boolean nsDeepScanned = false;

	/** Logger for this class */
	public static Logger log = LoggerFactory.getLogger(Xml.class);

	/** W3C XML Schema namespace URI */
	public static final URI W3C_XML_SCHEMA_URI = URI.create("http://www.w3.org/2001/XMLSchema");
	/** W3C XHTML namespace URI */
	public static final URI W3C_XHTML_URI = URI.create("http://www.w3.org/1999/xhtml");
	/** W3C XHTML Basic namespace URI */
	public static final URI W3C_XHTML_BASIC_URI = URI.create("http://www.w3.org/TR/xhtml-basic/xhtml-basic10.dtd");

	/**
	 * Creates a new empty Xml document with a DocumentBuilderFactory configured
	 * for namespace awareness and XInclude support.
	 */
	protected Xml() {
		dbf = DocumentBuilderFactoryImpl.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setXIncludeAware(true);
		dbf.setValidating(false);
	}

	// ==================== Factory Methods ====================

	/**
	 * Creates a new empty XML document.
	 * @return a new Xml instance with an empty document
	 */
	public static Xml create() {
		Xml out = new Xml();
		try {
			out.dom = out.dbf.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		return out;
	}

	/**
	 * Wraps an existing W3C DOM Document.
	 * @param input the DOM Document to wrap
	 * @return an Xml instance wrapping the given document
	 */
	public static Xml fromDom(Document input) {
		Xml out = new Xml();
		out.dom = input;
		return out;
	}

	/**
	 * Parses XML from a string.
	 * @param input the XML string
	 * @return an Xml instance containing the parsed document
	 * @throws XmlException if the XML is malformed
	 */
	public static Xml fromString(String input) throws XmlException {
		try {
			return fromStream(new ByteArrayInputStream(input.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses XML from an input stream.
	 * @param input the input stream containing XML
	 * @return an Xml instance containing the parsed document
	 * @throws XmlException if the XML is malformed
	 */
	public static Xml fromStream(InputStream input) throws XmlException {
		return fromStream(input, null);
	}

	/**
	 * Parses XML from an input stream with a base URI for resolving relative references.
	 * @param input the input stream containing XML
	 * @param uri the base URI for resolving references, or null
	 * @return an Xml instance containing the parsed document
	 * @throws XmlException if the XML is malformed
	 */
	public static Xml fromStream(InputStream input, URI uri) throws XmlException {
		try {
			Xml out = new Xml();
			out.dbf.setValidating(false);
			DocumentBuilder db = out.dbf.newDocumentBuilder();
			db.setEntityResolver(noopEntityResolver());
			if (uri == null) out.dom = db.parse(input);
			else out.dom = db.parse(input, uri.toString());
			return out;
		} catch (ParserConfigurationException | IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new XmlException("Document parsing error", e);
		}
	}

	/**
	 * Parses and validates XML against a W3C XML Schema.
	 * @param input the input stream containing XML
	 * @param uri the base URI for resolving references, or null
	 * @param schema the schema file to validate against
	 * @return an Xml instance containing the validated document
	 * @throws XmlException if parsing fails or validation fails
	 */
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
					throw new XmlException("Document failed validation: " + arg0.getMessage(), arg0);
				}
				public void fatalError(SAXParseException arg0) throws SAXException {
					throw new XmlException("Document could not be parsed" + arg0.getMessage(), arg0);
				}
				public void warning(SAXParseException arg0) throws SAXException {}
			});
			if (uri == null) out.dom = db.parse(input);
			else out.dom = db.parse(input, uri.toString());
			return out;
		} catch (ParserConfigurationException | IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new XmlException("Document parsing error or invalid schema", e);
		}
	}

	/**
	 * Parses XML from a file.
	 * @param file the XML file
	 * @return an Xml instance containing the parsed document
	 * @throws XmlException if the XML is malformed
	 * @throws FileNotFoundException if the file does not exist
	 */
	public static Xml fromFile(File file) throws XmlException, FileNotFoundException {
		return fromStream(new FileInputStream(file), file.toURI());
	}

	/**
	 * Parses and validates XML from a file against a schema.
	 * @param file the XML file
	 * @param schema the schema file
	 * @return an Xml instance containing the validated document
	 * @throws XmlException if parsing or validation fails
	 * @throws FileNotFoundException if either file does not exist
	 */
	public static Xml fromFile(File file, File schema) throws XmlException, FileNotFoundException {
		return fromStream(new FileInputStream(file), file.toURI(), schema);
	}

	/**
	 * Parses XML from a byte array.
	 * @param byteArray the XML bytes
	 * @return an Xml instance containing the parsed document
	 * @throws XmlException if the XML is malformed
	 */
	public static Xml fromBytes(byte[] byteArray) throws XmlException {
		return fromStream(new ByteArrayInputStream(byteArray));
	}

	/**
	 * Cleans malformed HTML using HtmlCleaner and returns it as well-formed XML.
	 * <p>This is useful for processing poorly-formed HTML documents that would otherwise
	 * fail to parse as XML. The HTML is cleaned and serialized as XHTML before parsing.</p>
	 * @param is the input stream containing HTML
	 * @return an Xml instance containing the cleaned XML
	 * @throws XmlException if the HTML cannot be cleaned or parsed
	 */
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
			throw new XmlException("HtmlCleaner could not process HTML input stream", e);
		}
	}

	/**
	 * Marshals a JAXB-annotated object into an Xml document.
	 * @param o the JAXB object to marshal
	 * @return an Xml instance containing the marshalled XML
	 * @throws XmlException if marshalling fails
	 */
	public static Xml fromJAXB(Object o) throws XmlException {
		return fromJAXB(o, null);
	}

	/**
	 * Marshals a JAXB-annotated object into an Xml document with a default namespace.
	 * @param o the JAXB object to marshal
	 * @param defaultNs the default namespace URI, or null for no namespace
	 * @return an Xml instance containing the marshalled XML
	 * @throws XmlException if marshalling fails
	 */
	public static Xml fromJAXB(Object o, String defaultNs) throws XmlException {
		try {
			Map<String, Object> properties = new HashMap<>();
			Xml out = Xml.create();
			properties.put(JAXBContextProperties.NAMESPACE_PREFIX_MAPPER, out.getNsPrefixMapper());
			if (defaultNs != null) properties.put(JAXBContextProperties.DEFAULT_TARGET_NAMESPACE, defaultNs);
			JAXBContext jc = JAXBContextFactory.createContext(new Class<?>[] {o.getClass()}, properties);
			Marshaller m = jc.createMarshaller();
			m.marshal(o, out.dom);
			return out;
		} catch (JAXBException e) {
			throw new XmlException("Couldn't marshal object to xml: " + o.getClass().getCanonicalName(), e);
		}
	}

	/**
	 * Unmarshals this document into a JAXB-annotated object.
	 * @param clzz the target class
	 * @param <T> the target type
	 * @return the unmarshalled object
	 * @throws XmlException if unmarshalling fails
	 */
	public <T> T toJAXB(Class<T> clzz) throws XmlException {
		return this.content().unmarshalAs(clzz);
	}

	/**
	 * Creates a deep clone of this Xml document.
	 * @return a new Xml instance with an independent copy of the document
	 */
	public Xml clone() {
		return Xml.fromDom((Document) dom.cloneNode(true));
	}

	// ==================== Fluent Builders ====================

	/**
	 * Sets the root element with a namespace.
	 * @param name the root element name (may include prefix)
	 * @param namespace the namespace URI
	 * @return a XmlDocElement for further fluent building
	 */
	public XmlDocElement withRoot(String name, URI namespace) {
		Element el = dom.createElementNS(namespace.toString(), name);
		dom.appendChild(el);
		return XmlDocElement.from(el);
	}

	/**
	 * Sets the root element (no namespace).
	 * @param name the root element name
	 * @return a XmlDocElement for further fluent building
	 */
	public XmlDocElement withRoot(String name) {
		Element el = dom.createElement(name);
		dom.appendChild(el);
		return XmlDocElement.from(el);
	}

	// ==================== Fluent Actions ====================

	/**
	 * Registers a namespace abbreviation for use in XPath queries.
	 * @param abbrev the prefix/abbreviation (e.g., "ex", "ns")
	 * @param ns the namespace URI
	 * @return this Xml instance for chaining
	 */
	public Xml withNamespaceAbbreviation(String abbrev, URI ns) {
		if (null == contexts) contexts = new HashMap<>();
		contexts.put(abbrev, ns.toString());
		nScontexts.put(ns.toString(), abbrev);
		return this;
	}

	/**
	 * Starts an XSLT transformation on this document.
	 * @return an XmlXsl for configuring and executing the transform
	 * @throws XmlException if the transformer cannot be initialized
	 */
	public XmlXsl<XmlDocElement> doTransform() throws XmlException {
		return doTransform((File) null);
	}

	/**
	 * Starts an XSLT transformation using a stylesheet file.
	 * @param xslt the XSLT stylesheet file
	 * @return an XmlXsl for configuring and executing the transform
	 * @throws XmlException if the XSLT cannot be loaded
	 */
	public XmlXsl<XmlDocElement> doTransform(File xslt) throws XmlException {
		return new XmlXsl<XmlDocElement>(this.content(), xslt);
	}

	/**
	 * Starts a built-in transformation.
	 * @param xslt one of the {@link XmlTransforms} built-in transforms
	 * @return an XmlXsl for configuring and executing the transform
	 * @throws XmlException if the transform cannot be initialized
	 */
	public XmlXsl<? extends XmlNode> doTransform(XmlTransforms xslt) throws XmlException {
		return new XmlXsl<XmlDocElement>(this.content(), xslt.getFile());
	}

	/**
	 * Executes an XPath 2.0 query on this document, using "ns" as the default namespace abbreviation.
	 * @param xpath the XPath expression
	 * @return an XmlXPath for retrieving results
	 * @throws XmlException if the XPath cannot be compiled
	 */
	public XmlXPath<XmlDocElement> doXpath(String xpath) throws XmlException {
		return doXpath(xpath, "ns");
	}

	/**
	 * Executes an XPath 2.0 query on this document.
	 * @param xpath the XPath expression
	 * @param defNsAbbr the abbreviation for the default namespace (e.g., "ns", "ex")
	 * @return an XmlXPath for retrieving results
	 * @throws XmlException if the XPath cannot be compiled
	 */
	public XmlXPath<XmlDocElement> doXpath(String xpath, String defNsAbbr) throws XmlException {
		return new XmlXPath<XmlDocElement>(this.content(), xpath, defNsAbbr);
	}

	/**
	 * Unmarshals this document into a JAXB-annotated object.
	 * @param clzz the target class
	 * @param <T> the target type
	 * @return the unmarshalled object
	 * @throws XmlException if unmarshalling fails
	 */
	public <T> T unmarshalAs(Class<T> clzz) throws XmlException {
		return this.content().unmarshalAs(clzz);
	}

	// ==================== Accessors ====================

	/**
	 * Returns the root element as an XmlDocElement.
	 * @return the document root
	 */
	public XmlDocElement content() {
		return new XmlDocElement(this, dom.getDocumentElement());
	}

	/**
	 * Returns the underlying W3C DOM Document.
	 * @return the raw DOM Document
	 */
	public Document asDocument() {
		return dom;
	}

	/**
	 * Returns the registered namespace abbreviations.
	 * @return map of prefix to namespace URI
	 */
	HashMap<String, String> getAbbrevs() {
		discoverDefaultNs();
		return contexts;
	}

	/**
	 * Checks if a namespace prefix is registered.
	 * @param prefix the prefix to check
	 * @return true if the prefix is registered
	 */
	public boolean awareOfPrefix(String prefix) {
		return contexts.containsKey(prefix);
	}

	/**
	 * Writes this document as formatted XML to the given output stream.
	 * @param out the output stream
	 * @throws XmlException if the write fails
	 */
	public void write(OutputStream out) throws XmlException {
		this.doTransform().write(out);
	}

	/**
	 * Returns a string representation of this document as formatted XML.
	 * @return the XML string
	 */
	public String toString() {
		return this.content().toString();
	}

	// ==================== Utilities ====================

	/**
	 * Creates a Calendar instance in XMLGregorianCalendar format.
	 * @return the current date/time as XMLGregorianCalendar
	 */
	public static XMLGregorianCalendar dateTime() {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a Calendar instance in XMLGregorianCalendar format from a timestamp.
	 * @param timeStamp milliseconds since epoch
	 * @return the date/time as XMLGregorianCalendar
	 */
	public static XMLGregorianCalendar dateTime(final long timeStamp) {
		try {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis(timeStamp);
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts a DOM Document to a formatted XML string.
	 * @param input the Document to convert
	 * @return the formatted XML string
	 */
	public static String asString(Document input) {
		try {
			return Xml.fromDom(input).doTransform().asString();
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}

	// ==================== Private helpers ====================

	protected void discoverDefaultNs() {
		if (nsScanned) return;
		this.withNamespaceAbbreviation("xs", W3C_XML_SCHEMA_URI);
		if (dom.getDocumentElement() != null) {
			NamedNodeMap nnm = dom.getDocumentElement().getAttributes();
			for (int i = 0; i < nnm.getLength(); i++) {
				if (nnm.item(i).getPrefix() != null && nnm.item(i).getPrefix().equals("xmlns")) {
					this.contexts.put(nnm.item(i).getLocalName(), nnm.item(i).getNodeValue());
					this.nScontexts.put(nnm.item(i).getNodeValue(), nnm.item(i).getLocalName());
				}
			}
		}
		nsScanned = true;
	}

	protected void deepScanNs() {
		if (nsDeepScanned) return;
		DocumentTraversal dt = (DocumentTraversal) this.dom;
		NodeIterator i = dt.createNodeIterator(this.dom, NodeFilter.SHOW_ELEMENT, null, false);
		Element element = (Element) i.nextNode();
		while (element != null) {
			String prefix = element.getPrefix();
			if (prefix != null && !contexts.containsKey(prefix)) {
				String nsUri = element.getNamespaceURI();
				this.contexts.put(prefix, nsUri);
				this.nScontexts.put(nsUri, prefix);
			}
			NamedNodeMap nnm = element.getAttributes();
			for (int j = 0; j < nnm.getLength(); j++) {
				if (nnm.item(j).getPrefix() != null && nnm.item(j).getPrefix().equals("xmlns")) {
					contexts.put(nnm.item(j).getLocalName(), nnm.item(j).getNodeValue());
				}
			}
			element = (Element) i.nextNode();
		}
		nsDeepScanned = true;
	}

	protected NamespacePrefixMapper getNsPrefixMapper() {
		discoverDefaultNs();
		return new NamespacePrefixMapper() {
			@Override
			public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
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
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				log.debug("Ignoring xml entity: " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			}
		};
	}

	private static EntityResolver defaultEntityResolver() {
		return new EntityResolver() {
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				log.debug("resolving: " + systemId);
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
}
