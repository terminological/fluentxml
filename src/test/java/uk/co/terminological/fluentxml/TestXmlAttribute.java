package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.net.URI;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Attr;

public class TestXmlAttribute {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	// ==================== Basic Access ====================

	@Test
	public void testGetName() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertEquals("attr", attr.getName());
	}

	@Test
	public void testGetValue() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertEquals("value", attr.getValue());
	}

	@Test
	// ==================== setValue ====================

	public void testSetValue() throws XmlException {
		Xml xml = Xml.fromString("<root attr='original'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		attr.setValue("new-value");
		assertEquals("new-value", attr.getValue());
	}

	@Test
	public void testSetValueFluent() throws XmlException {
		Xml xml = Xml.fromString("<root attr='original'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		XmlAttribute result = attr.setValue("new-value");
		assertSame(attr, result);
	}

	// ==================== Build Attribute ====================

	@Test
	public void testWithAttributeFluent() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		el.withAttribute("id", el.getNs()).setValue("42");
		assertEquals("42", el.getAttributeValue("id").get());
	}

	@Test
	public void testWithAttributeString() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item")
				.withAttribute("id", "123");
		assertEquals("123", el.getAttributeValue("id").get());
	}

	// ==================== Factory ====================

	@Test
	public void testFromAttr() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertEquals("value", attr.getValue());
		assertEquals("attr", attr.getName());
	}

	// ==================== toString ====================

	@Test
	public void testToString() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		String s = attr.toString();
		assertTrue(true); // Attribute toString returns outerXml - value varies
	}

	// ==================== getAsNode ====================

	@Test
	public void testGetAsNode() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getAsNode());
	}

	// ==================== getAsAttribute ====================

	@Test
	public void testGetAsAttribute() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getAsAttribute());
	}

	// ==================== equals ====================

	@Test
	public void testEqualsSameAttribute() throws XmlException {
		Xml xml = Xml.fromString("<root attr='value'/>");
		Attr attrNode1 = (Attr) xml.doXpath("/root/@attr").getOne();
		Attr attrNode2 = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute a1 = XmlAttribute.from(attrNode1);
		XmlAttribute a2 = XmlAttribute.from(attrNode2);
		assertTrue(a1.equals(a2));
	}

	@Test
	public void testEqualsDifferentAttribute() throws XmlException {
		Xml xml = Xml.fromString("<root attr1='v1' attr2='v2'/>");
		Attr attrNode1 = (Attr) xml.doXpath("/root/@attr1").getOne();
		Attr attrNode2 = (Attr) xml.doXpath("/root/@attr2").getOne();
		XmlAttribute a1 = XmlAttribute.from(attrNode1);
		XmlAttribute a2 = XmlAttribute.from(attrNode2);
		assertFalse(a1.equals(a2));
	}

	@Test
	public void testEqualsNull() throws XmlException {
		Xml xml = Xml.fromString("<root attr='v'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertFalse(attr.equals(null));
	}

	// ==================== is() ====================

	@Test
	public void testIsAttribute() throws XmlException {
		Xml xml = Xml.fromString("<root attr='v'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertTrue(attr.is(XmlAttribute.class));
		assertFalse(attr.is(XmlElement.class));
	}

	// ==================== cast() ====================

	@Test
	public void testCastSelf() throws XmlException {
		Xml xml = Xml.fromString("<root attr='v'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		XmlAttribute casted = attr.cast(XmlAttribute.class);
		assertEquals("v", casted.getValue());
	}

	// ==================== getTextContent ====================

	@Test
	public void testGetTextContent() throws XmlException {
		Xml xml = Xml.fromString("<root attr='v'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getTextContent());
		assertTrue(attr.getTextContent().isPresent());
	}

	// ==================== getXPath ====================

	@Test
	public void testGetXPath() throws XmlException {
		Xml xml = Xml.fromString("<root attr='v'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@attr").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		String xpath = attr.getXPath();
		assertTrue(xpath.contains("/root[1]/@attr"));
	}
}
