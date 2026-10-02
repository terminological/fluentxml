package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;

/**
 * Tests for {@link XmlElement#fromUnsafe(Xml, Node)}.
 * Verifies that non-element nodes are caught with a descriptive exception
 * rather than silently casting to Element.
 */
public class TestElementFromUnsafe {

	/**
	 * fromUnsafe successfully wraps an element node.
	 */
	@Test
	public void testFromUnsafe_ElementNode() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		org.w3c.dom.Element domEl = (org.w3c.dom.Element) xml.asDocument().getDocumentElement();

		XmlElement result = XmlElement.fromUnsafe(xml, domEl);
		assertNotNull(result);
		assertEquals("root", result.getName());
	}

	/**
	 * fromUnsafe throws NotAnElementException for a text node
	 * with a descriptive message including the node type and name.
	 */
	@Test
	public void testFromUnsafe_TextNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root>text content</root>");
		Text textNode = (Text) xml.asDocument().getDocumentElement().getFirstChild();

		try {
			XmlElement.fromUnsafe(xml, textNode);
			fail("Should have thrown NotAnElementException");
		} catch (NotAnElementException e) {
			// Should mention the actual node type
			assertTrue(e.getMessage().contains("TEXT_NODE"));
			// Should mention the text content context
			assertTrue(e.getMessage().contains("text content") || e.getMessage().contains("root"));
		}
	}

	/**
	 * fromUnsafe throws NotAnElementException for an attribute node.
	 */
	@Test
	public void testFromUnsafe_AttributeNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root id='1'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@id").getOne();

		try {
			XmlElement.fromUnsafe(xml, attrNode);
			fail("Should have thrown NotAnElementException");
		} catch (NotAnElementException e) {
			assertTrue(e.getMessage().contains("ATTRIBUTE_NODE"));
		}
	}

	/**
	 * fromUnsafe throws NotAnElementException for a comment node.
	 */
	@Test
	public void testFromUnsafe_CommentNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root><!-- a comment --></root>");
		org.w3c.dom.Node comment = xml.asDocument().getDocumentElement().getFirstChild();

		try {
			XmlElement.fromUnsafe(xml, comment);
			fail("Should have thrown NotAnElementException");
		} catch (NotAnElementException e) {
			assertTrue(e.getMessage().contains("COMMENT_NODE"));
		}
	}

	/**
	 * fromUnsafe correctly wraps a nested element node (not the root).
	 */
	@Test
	public void testFromUnsafe_NestedElementNode() throws XmlException {
		Xml xml = Xml.fromString("<root><child>text</child></root>");
		org.w3c.dom.Node childNode = xml.asDocument().getElementsByTagName("child").item(0);

		XmlElement result = XmlElement.fromUnsafe(xml, childNode);
		assertNotNull(result);
		assertEquals("child", result.getName());
	}

	/**
	 * fromUnsafe with a null node throws NPE (expected JDK behavior, not caught by fromUnsafe).
	 */
	@Test(expected = NullPointerException.class)
	public void testFromUnsafe_NullNode() throws XmlException {
		XmlElement.fromUnsafe(Xml.create(), null);
	}
}
