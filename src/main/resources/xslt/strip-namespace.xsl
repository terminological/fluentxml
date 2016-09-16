<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:param name="ns" select="namespace-uri()"/>
    <xsl:output exclude-result-prefixes="xsl xs" indent="yes" />
    
    <xsl:template match="*">
        <xsl:element name="{name()}" namespace="{$ns}">
            <xsl:apply-templates select="@* | node()" />
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="@*">
        <xsl:attribute name="{name()}" namespace="{namespace-uri()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>
    
</xsl:stylesheet>   
    