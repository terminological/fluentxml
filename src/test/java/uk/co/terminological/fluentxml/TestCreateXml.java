package uk.co.terminological.fluentxml;

import org.junit.Test;

public class TestCreateXml {

	@Test
	public void testCreate() throws XmlException{
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
		xml.write(System.out);
	}
}
