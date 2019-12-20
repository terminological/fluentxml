# FLUENTXML

## Maven dependency

### As a library

```XML
	<repositories>
		<repository>
			<id>jitpack.io</id>
			<url>https://jitpack.io</url>
		</repository>
	</repositories>
```

```XML
	<dependency>
		<groupId>com.github.terminological</groupId>
		<artifactId>fluentxml</artifactId>
		<version>1.0</version>
	</dependency>
```

OR

```XML
	<dependency>
		<groupId>com.github.terminological</groupId>
		<artifactId>fluentxml</artifactId>
		<version>master-SNAPSHOT</version>
	</dependency>
```


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

Find a node with XPath 2.0:

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

Other things you can do:
* Get XML from poorly formed HTML
* Load XML from a JAXB object
* Marshal XML to a JAXB object
* Write pretty printed XML to a file
* Select XML nodes based on CSS selectors (Document level)
* Get text or formatted XML string for any node
* Navigate document by walking the tree / xpath / selecting child element with java 8 streams support
* Modify content of XML using fluent methods
* Create XML from scratch using fluent methods
* Type safe node accessors - no more checking you have the right node type
* Apply a range of built in XML transforms to the content:
** change element case
** strip comments / namespaces
** convert XHTML content to csv / markdown / text
** covert XML to JSON or YAML

## Maven build plugins

Goals:

1) xmltojava: creates JAXB code from sample XML files


```XML
	<!-- Resolve maven plugin on github -->
	<pluginRepositories>
		<pluginRepository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</pluginRepository>
	</pluginRepositories>
```

```XML
			<plugin>
				<groupId>com.github.terminological</groupId>
				<artifactId>fluentxml</artifactId>
				<version>1.0</version>
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
```
			
2) castor: create a schema file from an xml example

```XML
			<plugin>
				<groupId>com.github.terminological</groupId>
				<artifactId>fluent</artifactId>
				<version>1.0</version>
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
```