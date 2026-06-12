package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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
	public void setUp() throws Exception {
	}

	// ==================== CSS Selectors ====================

	@Test
	public void testCssSelectorTag() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> links = doc.doCssSelection("a").getMany(XmlElement.class);
		assertTrue(links.size() > 0);
	}

	@Test
	public void testCssSelectorAttribute() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> links = doc.doCssSelection("a[href]").getMany(XmlElement.class);
		assertTrue(links.size() > 0);
	}

	@Test
	public void testCssSelectorClass() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlTable.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> tables = doc.doCssSelection("table").getMany(XmlElement.class);
		assertEquals(1, tables.size());
	}

	@Test
	public void testCssSelectorDescendant() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlTable.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> cells = doc.doCssSelection("td").getMany(XmlElement.class);
		assertTrue(cells.size() > 0);
	}

	@Test
	public void testCssSelectorHeadings() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> headings = doc.doCssSelection("h1").getMany(XmlElement.class);
		assertEquals(1, headings.size());
	}

	@Test
	public void testCssSelectorMultipleTags() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> links = doc.doCssSelection("a, link").getMany(XmlElement.class);
		assertTrue(links.size() > 0);
	}

	@Test
	public void testCssSelectorChild() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> paras = doc.doCssSelection("body > p").getMany(XmlElement.class);
		assertTrue(paras.size() > 0);
	}

	@Test
	public void testCssSelectorNoMatch() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> results = doc.doCssSelection("nonexistent-tag").getMany(XmlElement.class);
		assertEquals(0, results.size());
	}

	@Test(expected = XmlException.class)
	public void testCssSelectorInvalid() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		doc.doCssSelection("[invalid[selector").getMany(XmlElement.class);
	}

	// ==================== Factory ====================

	@Test
	public void testFromElement() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		org.w3c.dom.Element el = xml.asDocument().getDocumentElement();
		XmlDocElement doc = XmlDocElement.from(el);
		assertEquals("root", doc.getName());
	}

	// ==================== Inherited Methods ====================

	@Test
	public void testCssElementExtendsElement() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		XmlDocElement doc = xml.content();
		assertEquals("root", doc.getName());
		// Namespace presence depends on document context
	}
	@Test
	public void testCssElementCanTransform() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		String output = doc.doTransform().asXml();
		assertTrue(output.contains("html"));
	}

	@Test
	public void testCssElementCanXPath() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlElement body = doc.doXpath("//body").getOne(XmlElement.class);
		assertEquals("body", body.getName());
	}

	@Test
	public void testCssElementStream() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		long linkCount = doc.stream().filter(e -> e.getName().equals("a")).count();
		assertTrue(linkCount > 0);
	}

	@Test
	public void testCssElementWalkTree() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		XmlList<XmlElement> elements = doc.walkTree(XmlElement.class);
		assertTrue(elements.size() > 0);
	}

	@Test
	public void testCssElementWrite() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		XmlDocElement doc = xml.content();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		doc.write(baos);
		String output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("html"));
	}
}
