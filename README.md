# FLUENTXML

## Fluent XML manipulation library. 

Entry point are static methods on uk.co.terminological.fluentxml.Xml

Create some xml:

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

Stream the elements:

	Xml xml = Xml.fromStream( ...an input stream... );
	xml.content()
		.stream()
		.map(e -> e.getXPath())
		.forEach(s -> System.out.println(s));

Find a node with XPath:

	Xml xml = Xml.fromStream( ...an input stream... );
	XmlAttribute attribute = xml
		.doXpath("/documentNode/complexNode/@attribute")
		.getOne(XmlAttribute.class);

Execute some XSLT:

	Xml xml = Xml.fromStream(is);
	xml
		.doTransform(XmlTransforms.XML_TO_JSON) 	// <-- some built in transforms
		.withProperty("use-badgerfish", "true")
		.withProperty("use-namespaces", "false")
		.withProperty("skip-root", "true")
		.text() 										// <-- tell XSLT engine what to expect as output
		.write(System.out);


## Maven build plugins

Goals:

1) xmltojava: creates JAXB code from sample XML files

			<plugin>
				<groupId>uk.co.terminological</groupId>
				<artifactId>xml-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
				<executions>
					<execution>
						<id>xmltojava</id>
						<phase>generate-sources</phase>
						<goals>
							<goal>xmltojava</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<xmlJavaExecutions>
						<xmlJavaExecution>
							<inputFile> <!--INPUT XML EXAMPLE FILE--> </inputFile>
							<outputDirectory>${basedir}/target/xmltojava/src/main/java</outputDirectory>
							<packageName>com.example.package</packageName>
							<forceUpdate>true</forceUpdate>
						</xmlJavaExecution>
						<xmlJavaExecution>
							<inputUrl> <!--INPUT XML EXAMPLE FILE--> </inputUrl>
							<outputDirectory>${basedir}/target/xmltojava/src/main/java</outputDirectory>
							<packageName>com.example.package2</packageName>
							<forceUpdate>true</forceUpdate>
						</xmlJavaExecution>
					</xmlJavaExecutions>
				</configuration>
			</plugin>
			
2) castor: create a schema file from an xml example

			<plugin>
				<groupId>uk.co.terminological</groupId>
				<artifactId>xml-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
				<executions>
					<execution>
						<id>xmltoxsd</id>
						<phase>generate-sources</phase>
						<goals>
							<goal>castor</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<inputFile>src/main/resources/sample.xml</inputFile>
					<outputFile>${basedir}/target/xmltojava/src/main/resources/sample.xsd</outputFile>
				</configuration>
			</plugin>