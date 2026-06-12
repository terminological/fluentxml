package uk.co.terminological.fluentxml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;

/**
 * Represents an XSLT transformation configured on a context node. Provides fluent
 * methods for setting parameters, output format, and executing the transform.
 *
 * <h2>Usage - Identity transform</h2>
 * <pre>{@code
 * // Transform document to formatted XML string
 * String xml = xml.doTransform().asXml();
 *
 * // Transform to text
 * String text = xml.doTransform().text().asString();
 *
 * // Write to file
 * xml.doTransform().toFile(new File("output.xml"));
 *
 * // Write to output stream
 * xml.doTransform().write(System.out);
 * }</pre>
 *
 * <h2>Usage - XSLT stylesheet</h2>
 * <pre>{@code
 * // Built-in transforms
 * String json = xml.doTransform(XmlTransforms.XML_TO_JSON)
 *     .withProperty("use-badgerfish", "true")
 *     .withProperty("skip-root", "true")
 *     .asString();
 *
 * // Custom XSLT file
 * String result = xml.doTransform(new File("transform.xsl"))
 *     .withProperty("param-name", "value")
 *     .asXml();
 *
 * // Transform to new Document
 * Xml newDoc = xml.doTransform().toDocument();
 * }</pre>
 *
 * <h2>Output modes</h2>
 * <ul>
 *   <li>{@code asXml()} - formatted XML output (default)</li>
 *   <li>{@code asString()} - text output</li>
 *   <li>{@code toDocument()} - returns a new Xml document</li>
 *   <li>{@code toFile(File)} - writes to a file</li>
 *   <li>{@code write(OutputStream)} - writes to any stream</li>
 *   <li>{@code fragment()} - omit XML declaration</li>
 *   <li>{@code unformatted()} - no indentation</li>
 * </ul>
 *
 * @author rchallen
 * @see Xml
 * @see XmlNode
 * @see XmlTransforms
 */
public class XmlXsl<T extends XmlNode> {

	/** The context node being transformed */
	T context;
	/** The configured XSLT transformer */
	Transformer transformer;
	/** The transformer factory */
	TransformerFactory tFactory;

	/**
	 * Creates a new XmlXsl configured with the given context node and optional stylesheet.
	 * @param context the node to transform
	 * @param xslt the XSLT stylesheet file, or null for identity transform
	 * @throws XmlException if the XSLT cannot be loaded
	 */
	protected XmlXsl(T context, File xslt) throws XmlException {
		try {
			this.context = context;
			tFactory = TransformerFactoryImpl.newInstance();
			if (xslt == null) {
				transformer = tFactory.newTransformer();
			} else {
				transformer = tFactory.newTransformer(new StreamSource(xslt));
			}
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		} catch (TransformerConfigurationException e) {
			throw new XmlException("Could not load xslt", e);
		}
	}

	// ==================== Configuration ====================

	/**
	 * Sets an XSLT parameter on the transformer.
	 * @param name the parameter name
	 * @param value the parameter value
	 * @return this XmlXsl for chaining
	 */
	public XmlXsl<T> withProperty(String name, String value) {
		transformer.setParameter(name, value);
		return this;
	}

	/**
	 * Sets output to unformatted (no indentation).
	 * @return this XmlXsl for chaining
	 */
	public XmlXsl<T> unformatted() {
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		return this;
	}

	/**
	 * Sets output method to text.
	 * @return this XmlXsl for chaining
	 */
	public XmlXsl<T> text() {
		transformer.setOutputProperty(OutputKeys.METHOD, "text");
		return this;
	}

	/**
	 * Sets output method to XML (default).
	 * @return this XmlXsl for chaining
	 */
	public XmlXsl<T> xml() {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		return this;
	}

	/**
	 * Configures output as an XML fragment (no XML declaration).
	 * @return this XmlXsl for chaining
	 */
	public XmlXsl<T> fragment() {
		transformer.setOutputProperty(OutputKeys.STANDALONE, "omit");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		return this;
	}

	// ==================== Execution ====================

	/**
	 * Executes the transform and returns the result as a new Xml Document.
	 * Sets output method to XML.
	 * @return a new Xml instance containing the transformed result
	 * @throws XmlException if the transform fails
	 */
	public Xml toDocument() throws XmlException {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		Xml out = Xml.create();
		DOMResult result = new DOMResult(out.content().getRaw());
		DOMSource source = new DOMSource(context.getRaw());
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new XmlException("The transformation failed", e);
		}
		return out;
	}

	/**
	 * Executes the transform and returns the result as a text string.
	 * Sets output method to text.
	 * @return the text result string
	 * @throws XmlException if the transform fails
	 */
	public String asString() throws XmlException {
		transformer.setOutputProperty(OutputKeys.METHOD, "text");
		StringWriter out = new StringWriter();
		StreamResult result = new StreamResult(out);
		toStream(result);
		return out.toString();
	}

	/**
	 * Executes the transform and returns the result as an XML string.
	 * Sets output method to XML.
	 * @return the XML result string
	 * @throws XmlException if the transform fails
	 */
	public String asXml() throws XmlException {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		StringWriter out = new StringWriter();
		StreamResult result = new StreamResult(out);
		toStream(result);
		return out.toString();
	}

	/**
	 * Executes the transform and writes the result to a file.
	 * Creates parent directories if needed.
	 * @param out the output file
	 * @throws XmlException if the transform fails
	 */
	public void toFile(File out) throws XmlException {
		out.getParentFile().mkdirs();
		StreamResult result;
		try {
			result = new StreamResult(
					new BufferedWriter(
							new OutputStreamWriter(
									new FileOutputStream(out), "UTF-8")));
			toStream(result);
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes the transform and writes the result to an output stream.
	 * @param out the output stream
	 * @throws XmlException if the transform fails
	 */
	public void write(OutputStream out) throws XmlException {
		StreamResult result = new StreamResult(out);
		toStream(result);
	}

	private void toStream(StreamResult result) throws XmlException {
		DOMSource source = new DOMSource(context.getRaw());
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new XmlException("The transformation failed", e);
		}
	}
}
