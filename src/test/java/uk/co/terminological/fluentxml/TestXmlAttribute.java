package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
	public void setUp() throws Exception {}

	// ==================== Basic Access ====================

	@Test
	public void testGetName() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertEquals("attr", attr.getName());
	}

	@Test
	public void testGetValue() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertEquals("value", attr.getValue());
	}

	@Test
	// ==================== setValue ====================

	public void testSetValue() throws XmlException {
		var xml = Xml.fromString("<root attr='original'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		attr.setValue("new-value");
		assertEquals("new-value", attr.getValue());
	}

	@Test
	public void testSetValueFluent() throws XmlException {
		var xml = Xml.fromString("<root attr='original'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		var result = attr.setValue("new-value");
		assertSame(attr, result);
	}

	// ==================== Build Attribute ====================

	@Test
	public void testWithAttributeFluent() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item");
		el.withAttribute("id", el.getNs())
			.setValue("42");
		assertEquals(
			"42",
			el.getAttributeValue("id")
				.get()
		);
	}

	@Test
	public void testWithAttributeString() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item")
			.withAttribute("id", "123");
		assertEquals(
			"123",
			el.getAttributeValue("id")
				.get()
		);
	}

	// ==================== Factory ====================

	@Test
	public void testFromAttr() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertEquals("value", attr.getValue());
		assertEquals("attr", attr.getName());
	}

	// ==================== toString ====================

	@Test
	public void testToString() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		var s = attr.getTextContent()
			.get();
		assertTrue(s.equals("value")); // Attribute toString returns outerXml - value varies
	}

	// ==================== getAsNode ====================

	@Test
	public void testGetAsNode() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getAsNode());
	}

	// ==================== getAsAttribute ====================

	@Test
	public void testGetAsAttribute() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getAsAttribute());
	}

	// ==================== equals ====================

	@Test
	public void testEqualsSameAttribute() throws XmlException {
		var xml = Xml.fromString("<root attr='value'/>");
		var attrNode1 = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attrNode2 = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var a1 = XmlAttribute.from(attrNode1);
		var a2 = XmlAttribute.from(attrNode2);
		assertTrue(a1.equals(a2));
	}

	@Test
	public void testEqualsDifferentAttribute() throws XmlException {
		var xml = Xml.fromString("<root attr1='v1' attr2='v2'/>");
		var attrNode1 = (Attr) xml.doXpath("/root/@attr1")
			.getOne();
		var attrNode2 = (Attr) xml.doXpath("/root/@attr2")
			.getOne();
		var a1 = XmlAttribute.from(attrNode1);
		var a2 = XmlAttribute.from(attrNode2);
		assertFalse(a1.equals(a2));
	}

	@Test
	public void testEqualsNull() throws XmlException {
		var xml = Xml.fromString("<root attr='v'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertFalse(attr.equals(null));
	}

	// ==================== is() ====================

	@Test
	public void testIsAttribute() throws XmlException {
		var xml = Xml.fromString("<root attr='v'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertTrue(attr.is(XmlAttribute.class));
		assertFalse(attr.is(XmlElement.class));
	}

	// ==================== cast() ====================

	@Test
	public void testCastSelf() throws XmlException {
		var xml = Xml.fromString("<root attr='v'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		var casted = attr.cast(XmlAttribute.class);
		assertEquals("v", casted.getValue());
	}

	// ==================== getTextContent ====================

	@Test
	public void testGetTextContent() throws XmlException {
		var xml = Xml.fromString("<root attr='v'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		assertNotNull(attr.getTextContent());
		assertTrue(
			attr.getTextContent()
				.isPresent()
		);
	}

	// ==================== getXPath ====================

	@Test
	public void testGetXPath() throws XmlException {
		var xml = Xml.fromString("<root attr='v'/>");
		var attrNode = (Attr) xml.doXpath("/root/@attr")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		var xpath = attr.getXPath();
		assertTrue(xpath.contains("/root[1]/@attr"));
	}
}
