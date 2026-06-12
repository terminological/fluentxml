package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Text;

public class TestXmlText {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	// ==================== Basic Access ====================

	@Test
	public void testGetValue() throws XmlException {
		Xml xml = Xml.fromString("<root>hello world</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertEquals("hello world", text.getValue());
	}

	@Test
	public void testGetValueWithWhitespace() throws XmlException {
		Xml xml = Xml.fromString("<root>  spaced  </root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertEquals("  spaced  ", text.getValue());
	}

	@Test
	public void testToStringReturnsValue() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertEquals("hello", text.toString());
	}

	// ==================== Factory ====================

	@Test
	public void testFromText() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text domText = (Text) xml.asDocument().getDocumentElement().getFirstChild();
		XmlText text = XmlText.from(domText);
		assertEquals("hello", text.getValue());
	}

	@Test(expected = ClassCastException.class)
	public void testFromNonTextThrows() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		org.w3c.dom.Element el = xml.asDocument().getDocumentElement();
		XmlText.from(el);
	}

	// ==================== getAsNode ====================

	@Test
	public void testGetAsNode() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertNotNull(text.getAsNode());
	}

	// ==================== getAsTextNode ====================

	@Test
	public void testGetAsTextNode() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertNotNull(text.getAsTextNode());
	}

	// ==================== getXPath ====================

	@Test
	public void testGetXPath() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		String xpath = text.getXPath();
		assertTrue(xpath.contains("/root[1]/text()[1]"));
	}

	// ==================== getXml ====================

	@Test
	public void testGetXml() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertNotNull(text.getXml());
	}

	// ==================== is() ====================

	@Test
	public void testIsText() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertTrue(text.is(XmlText.class));
		assertFalse(text.is(XmlElement.class));
	}

	// ==================== cast() ====================

	@Test
	public void testCastSelf() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		XmlText casted = text.cast(XmlText.class);
		assertEquals("hello", casted.getValue());
	}

	// ==================== getTextContent ====================

	@Test
	public void testGetTextContent() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertNotNull(text.getTextContent());
		assertTrue(text.getTextContent().isPresent());
		assertEquals("hello", text.getTextContent().get());
	}

	// ==================== equals ====================

	@Test
	public void testEqualsSameText() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode1 = (Text) xml.doXpath("/root/text()").getOne();
		Text textNode2 = (Text) xml.doXpath("/root/text()").getOne();
		XmlText t1 = XmlText.from(textNode1);
		XmlText t2 = XmlText.from(textNode2);
		assertTrue(t1.equals(t2));
	}

	@Test
	public void testEqualsNull() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlText text = XmlText.from(textNode);
		assertFalse(text.equals(null));
	}
}
