package uk.co.terminological.maven;
/**
 * Heavily modified
 * 
 * Copyright (c) 2007 Espen Wiborg <espenhw@grumblesmurf.org>
 * 
 * Permission to use, copy, modify, and distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright
 * notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLConnection;



import org.apache.commons.io.IOUtils;
//import org.apache.cxf.maven_plugin.XSDToJavaMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.util.XMLInstance2Schema;
import org.xml.sax.SAXException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Goal which executes Castor on an xml file, and then generates JAXB sources from the result.
 * It will not be run if none of the input files have been modified since the last run.
 */
@Mojo( name = "xmltojava", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class XmlToJavaMojo extends AbstractMojo {
	/**
	 * The input file.
	 */
	@Parameter(required=true)
	XmlJavaExecution[] xmlJavaExecutions;

	//@Parameter(name="${project}")
	@Component
	private MavenProject mavenProject;

	//@Parameter(name="${session}")
	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	public void execute() throws MojoExecutionException, MojoFailureException {
		for (XmlJavaExecution conf: xmlJavaExecutions) {
			File outputDirectory = conf.getOutputDirectory();
			String packageName = conf.getPackageName();
			File inputFile;
			if (conf.getInputFile() != null && conf.getInputFile().exists()) {
				inputFile = conf.getInputFile();
			} else {
				try {
					URLConnection connection = conf.getInputUrl().openConnection();
					InputStream response;
					response = connection.getInputStream();
					inputFile = new File(outputDirectory,packageName+".source.xml");
					FileOutputStream out = new FileOutputStream(inputFile);
					IOUtils.copy(response, out);
					response.close();
					out.close();
				} catch (IOException e) {
					throw new MojoExecutionException(e.getLocalizedMessage());
				}

			}
			
			outputDirectory.mkdirs();

			long outputModified = outputDirectory.lastModified();


			boolean stale = conf.isForceUpdate();

			if (!inputFile.isFile())
				throw new MojoExecutionException("Input file " + inputFile.getAbsolutePath() + " does not exist as a file");
			if (inputFile.lastModified() > outputModified)
				stale = true;

			if (!stale) {
				getLog().info("Output is current, skipping castor invocation");
				return;
			}

			getLog().debug("Executing castor");

			try {
				File schemaFile;
				if (!conf.isSchema()) {
					schemaFile = new File(outputDirectory,packageName+".schema.xsd");
					Schema schema = new XMLInstance2Schema().createSchema(inputFile.getAbsolutePath());
					PrintWriter attempt = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(schemaFile), "UTF-8")));
					new XMLInstance2Schema().serializeSchema(attempt, schema);
					outputDirectory.setLastModified(System.currentTimeMillis());
				} else {
					schemaFile = inputFile;
				}

				executeMojo(
						plugin(
								groupId("org.apache.cxf"),
								artifactId("cxf-xjc-plugin"),
								version("2.6.0"),
								dependencies(
										dependency("commons-logging","commons-logging","1.1.1"),
										dependency("commons-lang","commons-lang","2.3"),
										dependency("commons-beanutils","commons-beanutils","1.8.3"))),
										goal("xsdtojava"),
										configuration(
												element(name("extensions"),
														element(name("extension"),"org.apache.cxf.xjcplugins:cxf-xjc-dv:2.6.0"),
														element(name("extension"),"org.jvnet.jaxb2_commons:jaxb2-fluent-api:3.0"),
														element(name("extension"),"org.jvnet.jaxb2_commons:jaxb2-basics:0.6.3")),
														element(name("sourceRoot"),outputDirectory.getAbsolutePath()),
														element(name("xsdOptions"),
																element(name("xsdOption"),
																		element(name("xsd"),schemaFile.getAbsolutePath()),
																		element(name("packagename"),packageName),
																		element(name("extensionArgs"),
																				element(name("extensionArg"),"-Xdv"),
																				element(name("extensionArg"),"-XtoString"),
																				element(name("extensionArg"),"-Xequals"),
																				element(name("extensionArg"),"-XhashCode"),
																				element(name("extensionArg"),"-Xcopyable"),
																				element(name("extensionArg"),"-Xfluent-api"))))),
																				executionEnvironment(
																						mavenProject,
																						mavenSession,
																						pluginManager)

						);

				executeMojo(plugin(
						groupId("org.codehaus.mojo"),
						artifactId("build-helper-maven-plugin"),
						version("1.9.1")
						),
						goal("add-source"),
						configuration(
								element(name("sources"), 
										element(name("source"),outputDirectory.getAbsolutePath())
										)
								),
								executionEnvironment(
										mavenProject,
										mavenSession,
										pluginManager)

						);

				//XSDToJavaMojo xsdMojo = new XSDToJavaMojo();
				//xsdMojo.setPluginContext(getPluginContext());
			} catch (SAXException | IOException e) {
				throw new MojoExecutionException(e.getLocalizedMessage());
			}

		}
	}
}

/* cxf usage example
<plugin>
<groupId>org.apache.cxf</groupId>
<artifactId>cxf-xjc-plugin</artifactId>
<version>2.6.0</version>
<configuration>
	<extensions>
		<extension>org.apache.cxf.xjcplugins:cxf-xjc-dv:2.6.0</extension>
		<extension>org.jvnet.jaxb2_commons:jaxb2-fluent-api:3.0</extension>
		<extension>org.jvnet.jaxb2_commons:jaxb2-basics:0.6.3</extension>
	</extensions>
</configuration>
<executions>
	<execution>
		<id>generate-sources</id>
		<phase>generate-sources</phase>
		<goals>
			<goal>xsdtojava</goal>
		</goals>
		<configuration>
			<sourceRoot>${basedir}/target/generated-sources/src/main/java</sourceRoot>
			<xsdOptions>
				<xsdOption>
					<xsd>${basedir}/target/trang/iana2skos.xsd</xsd>
					<packagename>com.bmj.informatics.ant.skos</packagename>
					<extensionArgs>
						<extensionArg>-Xdv</extensionArg>
						<extensionArg>-Xfluent-api</extensionArg>
						<extensionArg>-XtoString</extensionArg>
						<extensionArg>-Xequals</extensionArg>
						<extensionArg>-XhashCode</extensionArg>
						<extensionArg>-Xcopyable</extensionArg>
						<extensionArg>-XautoInheritance</extensionArg>
						<extensionArg>-XautoInheritance-xmlRootElementsImplement=java.io.Serializable</extensionArg>
						<extensionArg>-XautoInheritance-jaxbElementsImplement=java.io.Serializable</extensionArg>
						<extensionArg>-XautoInheritance-xmlTypesImplement=java.io.Serializable</extensionArg>
					</extensionArgs>
				</xsdOption>
			</xsdOptions>
		</configuration>
	</execution>
</executions>
<dependencies>
	<dependency>
		<groupId>commons-logging</groupId>
		<artifactId>commons-logging</artifactId>
		<version>1.1.1</version>
	</dependency>
	<dependency>
		<groupId>commons-lang</groupId>
		<artifactId>commons-lang</artifactId>
		<version>2.3</version>
	</dependency>
	<dependency>
		<groupId>commons-beanutils</groupId>
		<artifactId>commons-beanutils</artifactId>
		<version>1.8.3</version>
	</dependency>
</dependencies>
 */

/*
<groupId>org.codehaus.mojo</groupId>
<artifactId>build-helper-maven-plugin</artifactId>
<version>1.9.1</version>
<executions>
  <execution>
    <id>add-source</id>
    <phase>generate-sources</phase>
    <goals>
      <goal>add-source</goal>
    </goals>
    <configuration>
      <sources>
        <source>some directory</source>
        ...
      </sources>
 */