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

public class XmlXsl<T extends XmlNode> {

	T context;
	Transformer transformer;
	TransformerFactory tFactory;
	//Map<String, String> properties = new HashMap<String, String>();

	protected XmlXsl(T context, File xslt) throws XmlException {
		//try {
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
			throw new XmlException("Could not load xslt",e);
		}
	}
	
	/**
	 * @return Set an input parameter to the XSLT stylesheet 
	 */
	public XmlXsl<T> withProperty(String name, String value) {
		transformer.setParameter(name, value);
		return this;
	}
	
	/**
	 * @return Set the transformer to return unformatted xml 
	 */
	
	public XmlXsl<T> unformatted() {
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		return this;
	}
	
	/**
	 * @return Set the transformer to return text 
	 */
	public XmlXsl<T> text() {
		transformer.setOutputProperty(OutputKeys.METHOD, "text");
		return this;
	}
	
	/**
	 * @return Set the transformer to return xml 
	 */
	public XmlXsl<T> xml() {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		return this;
	}
	
	/**
	 * @return Set the transformer to return an Xml fragment (i.e. without Xml declaration) 
	 */
	public XmlXsl<T> fragment() {
		transformer.setOutputProperty(OutputKeys.STANDALONE, "omit");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		return this;
	}

	private void toStream(StreamResult result) throws XmlException {
		DOMSource source = new DOMSource(context.getRaw());
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new XmlException("The transformation failed", e);
		}
	}
	
	/**
	 * @return A fluent Xml object of the transformer output
	 * @throws XmlException
	 */
	public Xml toDocument() throws TransformerException {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		Xml out = Xml.create();
		DOMResult result = new DOMResult(out.content().getRaw());
		DOMSource source = new DOMSource(context.getRaw());
		transformer.transform(source, result);
		return out;
	}
	
	/**
	 * @return A plain text representation of the transformer output when the output is not Xml
	 * @throws XmlException
	 */
	public String asString() throws XmlException {
		transformer.setOutputProperty(OutputKeys.METHOD, "text");
		StringWriter out = new StringWriter();
		StreamResult result = new StreamResult(out);
		toStream(result);
		return out.toString();
	}
	
	/**
	 * @return A string representation of the Xml of the transformer output
	 * @throws XmlException
	 */
	public String asXml() throws XmlException {
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		StringWriter out = new StringWriter();
		StreamResult result = new StreamResult(out);
		toStream(result);
		return out.toString();
	}
	
	/**
	 * Writes the string representation of the Xml of the transformer output to a file
	 * @throws XmlException
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
	 * Writes the string representation of the Xml of the transformer output to an output stream
	 * @throws XmlException
	 */
	public void write(OutputStream out) throws XmlException {
		StreamResult result = new StreamResult(out);
		toStream(result);
	}
	
}
