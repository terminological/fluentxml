/* Vendored from java-cssSelector-to-xpath V1.0.0RC1
   https://github.com/sam-rosenthal/java-cssSelector-to-xpath
   MIT License, Copyright (c) Sam Rosenthal.
   Only the package declaration changed: model and utilities are flattened into
   this package so the project no longer depends on a jitpack release candidate. */
package uk.co.terminological.fluentxml.css;

public class CssSelectorStringSplitterException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public CssSelectorStringSplitterException() {
		super();
	}
	
	public CssSelectorStringSplitterException(String errorMessage) {
		super(errorMessage);
	}
	
	public CssSelectorStringSplitterException(String errorMessage, Throwable cause) {
		super(errorMessage, cause);
	}

}
