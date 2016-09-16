<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:param name="delim" select="','" />
	<xsl:param name="quote" select="'&quot;'" />
	<xsl:param name="break" select="'&#xA;'" />

	<xsl:output method="text" />
	<xsl:strip-space elements="*" />

	<xsl:template match="thead | caption">
	</xsl:template>

	<xsl:template match="tbody">
		<xsl:apply-templates />
	</xsl:template>

	<!-- SIC codes 2003 is slightly different in that it has some header rows 
		interspersed that need ignoring. These rows are distinguishable by the fact 
		that the headers are surrounded with <strong> tags. So, match those rows 
		and ignore them... -->
	<xsl:template match="tr[*/strong]">
	</xsl:template>

	<!-- Process the table tr tags. Output <sic_code>,"<sic_code_desc>" -->
	<xsl:template match="tr">
		<xsl:apply-templates />
		<xsl:if test="following-sibling::*">
			<xsl:value-of select="$break" />
		</xsl:if>
	</xsl:template>

	<xsl:template match="td|th">
		<xsl:variable name="value" select="string(.)"/>
		<!-- Quote the value if required -->
		<xsl:choose>
			<xsl:when test="contains($value, '&quot;')">
				<xsl:variable name="x"
					select="replace($value, '&quot;',  '&quot;&quot;')" />
				<xsl:value-of select="concat('&quot;', $x, '&quot;')" />
			</xsl:when>
			<xsl:when test="contains($value, $delim)">
				<xsl:value-of select="concat('&quot;', $value, '&quot;')" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$value" />
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="following-sibling::*">
			<xsl:value-of select="$delim" />
		</xsl:if>
	</xsl:template>


	<!-- Standard copy template -->
	<xsl:template match="@*|node()">
		<xsl:apply-templates />
	</xsl:template>

</xsl:stylesheet>