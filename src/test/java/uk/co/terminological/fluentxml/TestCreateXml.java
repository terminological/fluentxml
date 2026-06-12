package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestCreateXml {

	@Test
	public void testCreate() throws XmlException {
		Xml xml = Xml.create();
		XmlElement items = xml
				.withRoot("root")
				.withChildElement("items");
		items.withChildElement("item")
				.withAttribute("id", "1")
				.withText("value 1");
		items.withChildElement("item")
				.withAttribute("id", "2")
				.withText("value 2");
		items.withChildElement("item")
				.withAttribute("id", "3")
				.withText("value 3");

		// Verify structure
		assertEquals(3, items.childElements().size());
		assertEquals("items", items.getName());
		assertEquals("root", items.up().getName());

		// Verify output
		String output = xml.toString();
		assertTrue(output.contains("<root>"));
		assertTrue(output.contains("<items>"));
		assertTrue(output.contains("<item id=\"1\">value 1</item>"));
		assertTrue(output.contains("<item id=\"2\">value 2</item>"));
		assertTrue(output.contains("<item id=\"3\">value 3</item>"));
	}

	@Test
	public void testCreateWithWrite() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("test");
		root.withChildElement("item").withAttribute("n", "1").withText("one");

		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		xml.write(baos);
		String output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("test"));
		assertTrue(output.contains("item"));
	}
}
