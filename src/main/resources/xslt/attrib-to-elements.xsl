<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output exclude-result-prefixes="xsl xs" indent="yes" />

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:element name="{local-name()}">
			<xsl:value-of select="." />
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>   
    