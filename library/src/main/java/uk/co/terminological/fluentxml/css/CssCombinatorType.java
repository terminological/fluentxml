/* Vendored from java-cssSelector-to-xpath V1.0.0RC1
   https://github.com/sam-rosenthal/java-cssSelector-to-xpath
   MIT License, Copyright (c) Sam Rosenthal.
   Only the package declaration changed: model and utilities are flattened into
   this package so the project no longer depends on a jitpack release candidate. */
package uk.co.terminological.fluentxml.css;

public enum CssCombinatorType {
	SPACE(' ',"//"), 
	PLUS('+',"/following-sibling::*[1]/self::"),
	GREATER_THAN('>',"/"),
	TILDA('~',"/following-sibling::");
	
	private char typeChar;
	private String xpath;

	private CssCombinatorType(char typeCharIn, String xpathIn)
	{
		this.typeChar=typeCharIn;
		this.xpath=xpathIn;
	}
	public char getCombinatorChar() 
	{
		return typeChar;
	}
	
	public String getXpath()
	{
		return xpath;
	}
	public static CssCombinatorType combinatorTypeChar(String unknownString) {
		if(unknownString==null)
		{
			return null;
		}

		switch (unknownString) 
		{
			case " ": 
                 return SPACE;
        	case "+":  
        		return PLUS;
        	case ">": 
        		return GREATER_THAN;
        	case "~":  
        		return TILDA;
        	default:
        		throw new IllegalArgumentException(unknownString);
		}
	}

}
