/* Vendored from java-cssSelector-to-xpath V1.0.0RC1
   https://github.com/sam-rosenthal/java-cssSelector-to-xpath
   MIT License, Copyright (c) Sam Rosenthal.
   Only the package declaration changed: model and utilities are flattened into
   this package so the project no longer depends on a jitpack release candidate. */
package uk.co.terminological.fluentxml.css;


public class CssElementCombinatorPair {
	private CssCombinatorType combinatorType;
	private CssElementAttributes cssElementAttributes;
	
	public CssElementCombinatorPair(CssCombinatorType combinatorTypeIn, String cssElementAttributesStringIn) throws CssSelectorStringSplitterException
	{
		this.combinatorType=combinatorTypeIn;
		this.cssElementAttributes=new CssElementAttributeParser().createElementAttribute(cssElementAttributesStringIn);
	}
	public CssCombinatorType getCombinatorType() {
		return combinatorType;
	}
	public CssElementAttributes getCssElementAttributes() {
		return cssElementAttributes;
	}
	
	@Override
	public String toString()
	{
		return "(Combinator="+this.getCombinatorType()+", "+this.cssElementAttributes+")";
	}
	@Override
	public boolean equals(Object cssElementCombinatorPair)
	{
		if(cssElementCombinatorPair==null)
		{
			return false;
		}
		return this.toString().equals(cssElementCombinatorPair.toString());
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
