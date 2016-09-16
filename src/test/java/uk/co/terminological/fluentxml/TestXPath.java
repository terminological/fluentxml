package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.co.terminological.fluentxml.Xml;
import uk.co.terminological.fluentxml.XmlDocElement;
import uk.co.terminological.fluentxml.XmlElement;
import uk.co.terminological.fluentxml.XmlException;
import uk.co.terminological.fluentxml.XmlXPath;

public class TestXPath {

	public static void main(String[] args) throws XmlException {
		TestXPath test = new TestXPath();
		test.testGetOneNamespace();
		test.testGetOneNoNs();

		Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/xhtmlExample.html"));
		XmlXPath<XmlDocElement> xpath = xml.doXpath("//body");
		XmlElement el = xpath.getOne(XmlElement.class);
		System.out.println(el.outerXml());
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetOneNamespace() throws XmlException {
		Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/namespaced.xml"));
		Object attribute = xml.doXpath("string(/documentNode/ex2:complexNode/@attribute)").getOne();
		assertTrue(attribute instanceof String);
		assertTrue(attribute.equals("complexValue"));
	}

	@Test
	public void testGetOneNoNs() throws XmlException {
		Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/schemaLess.xml"));
		Object attribute = xml.doXpath("string(/documentNode/complexNode/@attribute)").getOne();
		//System.out.println(attribute.getClass().getCanonicalName());
		assertTrue(attribute instanceof String);
		assertTrue(attribute.equals("complexValue"));
	}

	@Test
	public void testXPath() throws XmlException {
		{
			Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/schemaLess.xml"));
			XmlAttribute attribute = xml.doXpath("/documentNode/complexNode/@attribute").getOne(XmlAttribute.class);
			//System.out.println(attribute.getXPath());
			assertTrue(attribute.getXPath().equals("/documentNode[1]/complexNode[1]/@attribute"));
			XmlAttribute attribute2 = xml.doXpath(attribute.getXPath()).getOne(XmlAttribute.class);
			assertTrue(attribute2.equals(attribute));
		}
		{
			Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/namespaced.xml"));
			XmlAttribute attribute = xml.doXpath("/documentNode/ex2:complexNode/@attribute").getOne(XmlAttribute.class);
			//System.out.println(attribute.getXPath());
			assertTrue(attribute.getXPath().equals("/documentNode[1]/ex2:complexNode[1]/@attribute"));
			XmlAttribute attribute2 = xml.doXpath(attribute.getXPath()).getOne(XmlAttribute.class);
			assertTrue(attribute2.equals(attribute));
		}
	}

	@Test
	public void testStream() throws XmlException {
		
			Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/schemaLess.xml"));
			xml.content().stream().map(e -> e.getXPath())
				.anyMatch(s -> s.equals("/documentNode[1]/complexNode[1]/child1[1]"));
				//.forEach(s -> System.out.println(s));
		}
	
	
	@Test
	public void innerXml() throws XmlException {
		Xml xml = Xml.fromStream(TestXPath.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.doXpath("/documentNode/complexNode").getOne(XmlElement.class);
		//System.out.println(el.innerXml());
		assertTrue(el.outerXml().contains("complexNode attribute=\"complexValue\""));
		assertTrue(el.outerXml().contains("<child2>child2 text</child2>"));
	}
}
