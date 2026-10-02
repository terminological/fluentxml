/* Vendored from java-cssSelector-to-xpath V1.0.0RC1
   https://github.com/sam-rosenthal/java-cssSelector-to-xpath
   MIT License, Copyright (c) Sam Rosenthal.
   Only the package declaration changed: model and utilities are flattened into
   this package so the project no longer depends on a jitpack release candidate. */
package uk.co.terminological.fluentxml.css;

import java.util.ArrayList;
import java.util.List;
//Take a string and the format of the string is...
//xxx[yyy]...[zzz]
//-->break it up into a list = xxx,[yyy],..,[zzz]
//if xxx DNE==>break into list=*,[yyy],...,[zzz]
//
//create new class
//make into css element attribute
//fills this stuff,
//only has constructor and getters
public class CssElementAttributes
{
	private String element;
	private List<CssAttribute> cssAttributeList;

	public CssElementAttributes(String elementIn, List<CssAttribute> cssAttributeListIn) throws CssSelectorStringSplitterException
	{
		this.element=elementIn;
		this.cssAttributeList=new ArrayList<>(cssAttributeListIn);
	}
	public String getElement() 
	{
		return element;
	}

	public List<CssAttribute> getCssAttributeList() 
	{
		return cssAttributeList;
	}
	
	@Override
	public String toString()
	{
		return "Element="+this.element+", CssAttributeList="+this.cssAttributeList;
	}
	
	@Override
	public boolean equals(Object cssElementAttributes)
	{
		if(cssElementAttributes==null)
		{
			return false;
		}
		return this.toString().equals(cssElementAttributes.toString());
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
}