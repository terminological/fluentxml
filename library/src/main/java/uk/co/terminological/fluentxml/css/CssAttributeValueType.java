/* Vendored from java-cssSelector-to-xpath V1.0.0RC1
   https://github.com/sam-rosenthal/java-cssSelector-to-xpath
   MIT License, Copyright (c) Sam Rosenthal.
   Only the package declaration changed: model and utilities are flattened into
   this package so the project no longer depends on a jitpack release candidate. */
package uk.co.terminological.fluentxml.css;

public enum CssAttributeValueType 
{
	EQUAL("="), 
	TILDA_EQUAL("~="),
	PIPE_EQUAL("|="),
	CARROT_EQUAL("^="),
	DOLLAR_SIGN_EQUAL("$="),
	STAR_EQUAL("*=");

	private String equalString;

	private CssAttributeValueType(String nameIn)
	{
		this.equalString=nameIn;
	}

	public String getEqualStringName() {
		return equalString;
	}
	public static CssAttributeValueType valueTypeString(String unknownString) {
		if(unknownString==null)
		{
			return null;
		}

		switch (unknownString) 
		{
			case "=": 
                 return EQUAL;
        	case "~=":  
        		return TILDA_EQUAL;
        	case "|=": 
        		return PIPE_EQUAL;
        	case "$=":  
        		return DOLLAR_SIGN_EQUAL;
        	case "^=": 
        		return CARROT_EQUAL;
        	case "*=":  
        		return STAR_EQUAL;
        	default:
        		throw new IllegalArgumentException(unknownString);
		}
	}
}
