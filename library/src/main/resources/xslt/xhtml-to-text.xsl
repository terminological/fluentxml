<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:transform version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:strip-space elements="*"/>

	<!--- Store variable for new line. - -->
	<xsl:variable name="new-line" select="'&#010;'" />

	<!--- Store variable for double-new line. - -->
	<xsl:variable name="new-lines" select="concat( $new-line, $new-line )" />


	<!--- Match the root node plus any nodes that are not matched specifically 
		by the templates defined below. - -->
	<xsl:template match="*">
		<xsl:apply-templates select="text()|*" />
	</xsl:template>

	<!--- For all text nodes, output trimmed value. - -->
	<xsl:template match="text()">
		<xsl:value-of select="." />
	</xsl:template>

	<!--- Denote primary header with hrule. - -->
	<xsl:template match="h1">
		<xsl:apply-templates select="text()|*" />
		<xsl:value-of select="$new-line" />
		<xsl:text>=================================</xsl:text>
		<xsl:value-of select="$new-line" />
	</xsl:template>

	<!--- Denote secondary headers with hash marks. - -->
	<xsl:template match="h2|h3|h4|h5|h6">
		<xsl:apply-templates select="text()|*" />
		<xsl:value-of select="$new-line" />
		<xsl:text>---------------------------------</xsl:text>
		<xsl:value-of select="$new-line" />
	</xsl:template>

	<!--- Turn block level elements into text-only. - -->
	<xsl:template match="p|ol|ul|pre|address|blockquote|dl|div|fieldset|form|noscript">
		<xsl:value-of select="$new-line" />
		<xsl:apply-templates select="text()|*" />
	</xsl:template>

	<!--- Turn List items into bracketed values. - -->
	<xsl:template match="li">
		<xsl:value-of select="$new-line" />
		<xsl:text>* </xsl:text>
		<xsl:apply-templates select="text()|*" />
	</xsl:template>

	<!--- Add new line after table. - -->
	<xsl:template match="table">
		<xsl:value-of select="$new-line" />
		<xsl:apply-templates select="*" />
	</xsl:template>

	<!--- Turn table rows into bracketed values. - -->
	<xsl:template match="tr">
		<xsl:apply-templates select="*" />
		<xsl:value-of select="$new-line" />
	</xsl:template>

	<!--- Bracket table values. - -->
	<xsl:template match="td">
		<xsl:value-of select="'&#013;'" />
		<xsl:apply-templates select="text()|*" />
	</xsl:template>

	<!--- Strip out any inline tags (and start them off with an initial space 
		so that nested and sibling tags don't get concatenated text). - -->
	<xsl:template match="a|abbr|acronym|b|bdo|big|button|cite|code|dfn|em|i|img|input|kbd|label|map|object|q|samp|select|small|span|strong|sub|sup|textarea|time|tt|var">
		<xsl:value-of select="text()" />
		<xsl:text> </xsl:text>
	</xsl:template>

	<!--- Replace hrule with manual dashes. NOTE: template also named for manual 
		execution. - -->
	<xsl:template match="hr" name="hr">
		<xsl:value-of select="$new-lines" />
	</xsl:template>

	<!--- Replace break tag with new line. - -->
	<xsl:template match="br">
		<xsl:value-of select="$new-line" />
	</xsl:template>

	<!--- Remove script tags. - -->
	<xsl:template match="script">
	</xsl:template>

</xsl:transform>