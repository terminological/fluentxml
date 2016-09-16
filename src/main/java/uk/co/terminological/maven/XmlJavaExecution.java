package uk.co.terminological.maven;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugins.annotations.Parameter;

public class XmlJavaExecution {

	@Parameter
	private File inputFile;


	@Parameter
	private URL inputUrl;

	/**
	 * The output directory name.
	 */
	@Parameter(required=true)
	private File outputDirectory;

	/*
	 * Package name
	 */
	@Parameter(required=true)
	private String packageName;

	@Parameter(defaultValue = "false")
	private boolean forceUpdate;

	@Parameter(defaultValue = "false")
	private boolean isSchema;
	
	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public boolean isForceUpdate() {
		return forceUpdate;
	}

	public void setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
	}

	public URL getInputUrl() {
		return inputUrl;
	}

	public void setInputUrl(URL inputUrl) {
		this.inputUrl = inputUrl;
	}

	public boolean isSchema() {
		return isSchema;
	}

	public void setSchema(boolean isSchema) {
		this.isSchema = isSchema;
	}
}
