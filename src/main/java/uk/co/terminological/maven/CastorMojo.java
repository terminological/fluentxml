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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.util.XMLInstance2Schema;
import org.xml.sax.SAXException;

/**
 * Goal which executes Castor on an xml file.  It will not be run
 * if none of the input files have been modified since the last run.
 */
@Mojo( name = "castor", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class CastorMojo
extends AbstractMojo
{
	/**
	 * The input file.
	 */
	@Parameter(required=true)
	private File inputFile;

	/**
	 * The output file name.
	 */
	@Parameter(required=true)
	private File outputFile;

	public void execute()
			throws MojoExecutionException, MojoFailureException {
		outputFile.getParentFile().mkdirs();

		long outputModified = outputFile.lastModified();

		boolean stale = false;

		if (!inputFile.isFile())
			throw new MojoExecutionException("Input file " + inputFile.getAbsolutePath() + " does not exist as a file");
		if (inputFile.lastModified() > outputModified)
			stale = true;

		if (!stale) {
			getLog().info("Output is current, skipping trang invocation");
			return;
		}

		getLog().debug("Executing castor");

		try {
			
			Schema schema = new XMLInstance2Schema().createSchema(inputFile.getAbsolutePath());
			PrintWriter attempt = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8")));
			new XMLInstance2Schema().serializeSchema(attempt, schema);
			
		} catch (SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}