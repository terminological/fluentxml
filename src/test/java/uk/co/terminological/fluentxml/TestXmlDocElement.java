package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlDocElement {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {}

	// ==================== Factory ====================

	@Test
	public void testFromElement() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var el = xml.asDocument()
			.getDocumentElement();
		var doc = XmlDocElement.from(el);
		assertEquals("root", doc.getName());
	}

	// ==================== Inherited Methods ====================

	@Test
	public void testCssElementExtendsElement() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var doc = xml.content();
		assertEquals("root", doc.getName());
		// Namespace presence depends on document context
	}
	@Test
	public void testCssElementCanTransform() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		var output = doc.doTransform()
			.asXml();
		assertTrue(output.contains("html"));
	}

	@Test
	public void testCssElementCanXPath() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		var body = doc.doXpath("//body")
			.getOne(XmlElement.class);
		assertEquals("body", body.getName());
	}

	@Test
	public void testCssSelection() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		assertEquals(10, doc.doCssSelection("a[href]").getMany(XmlElement.class).size());
		assertEquals("body", doc.doCssSelection("body").getOne(XmlElement.class).getName());
	}

	@Test
	public void testCssElementStream() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		var linkCount = doc.stream()
			.filter(
				e -> e.getName()
					.equals("a")
			)
			.count();
		assertTrue(linkCount > 0);
	}

	@Test
	public void testCssElementWalkTree() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		XmlList<XmlElement> elements = doc.walkTree(XmlElement.class);
		assertTrue(elements.size() > 0);
	}

	@Test
	public void testCssElementWrite() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var doc = xml.content();
		var baos = new ByteArrayOutputStream();
		doc.write(baos);
		var output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("html"));
	}
}
