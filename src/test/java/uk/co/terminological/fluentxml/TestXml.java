package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestXml {

	static Logger log = LoggerFactory.getLogger(TestXml.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFromStream() throws XmlException {
		log.debug("Test load xml");
		{
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
			assertTrue(xml.content().getNs().get().toString().equals("http://www.example.com"));
		}
		{
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
			assertFalse(xml.content().getNs().isPresent());
		}
	}

	@Test
	public void testFromHtml() throws XmlException {
		log.debug("Test load html");
		{
			Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
			assertTrue(xml.content().getNs().get().toString().equals("http://www.w3.org/1999/xhtml"));
			assertTrue(xml.content().getAsElement().getTagName().equals("html"));
		}
		{
			Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/underspecified.html"));
			assertFalse(xml.content().getNs().isPresent());
			assertTrue(xml.content().getAsElement().getTagName().equals("html"));
		}
		{
			Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/undefined.html"));
			assertFalse(xml.content().getNs().isPresent());
			assertTrue(xml.content().getAsElement().getTagName().equals("html"));
			assertTrue(xml.content()
				.streamChildElements()
				.filter(e -> e.getName().equals("body"))
				.flatMap(e -> e.streamChildElements())
				.anyMatch(e -> e.getName().equals("ul")));
			//xml.write(System.out);
		}
		
	}

	@Test 
	public void textJaxb() throws XmlException {
		JaxbPojo test = JaxbPojo.with(1, 2)
				.andItem("red", 1)
				.andItem("green", 2)
				.andItem("blue", 4);
		String s = Xml.fromJAXB(test).toString();
		
		System.out.println(s);
		
		JaxbPojo roundTrip = Xml.fromString(s).unmarshalAs(JaxbPojo.class);
		assertTrue(roundTrip.x==1);
		assertTrue(roundTrip.y==2);
		assertTrue(roundTrip.items.stream().anyMatch(i -> i.text.equals("green") && i.z==2));
		
		Xml x = Xml.fromJAXB(test,"http://example.com");
		x.write(System.out);
		assertTrue(x.content().getNs().get().toString().equals("http://example.com"));
	}
	
	@XmlRootElement(name="test")
	public static class JaxbPojo {
		public int x;
		public int y;
		public List<NestedJaxbPojo> items;
		
		public static JaxbPojo with(int x, int y) {
			JaxbPojo out = new JaxbPojo(); out.x = x; out.y = y; out.items=new ArrayList<>(); return out; 
		}
		public JaxbPojo andItem(String text, int z) {
			NestedJaxbPojo out = new NestedJaxbPojo(); out.text = text; out.z = z; this.items.add(out); return this; 
		}
	}
	
	@XmlType(name="item")
	public static class NestedJaxbPojo {
		public String text;
		@XmlAttribute(name="id")
		public int z;
		
	}
	
}
