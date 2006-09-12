<?xml version="1.0" encoding="UTF-8"?> 
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
		
<!-- This xslt stylesheet presents a browseIndex page.
-->
	<xsl:output method="html" indent="yes" encoding="UTF-8"/>

	<xsl:param name="ERRORMESSAGE" select="''"/>
	
	<xsl:variable name="FIELDNAME" select="/resultPage/browseIndex/@fieldName"/>
	<xsl:variable name="INDEXNAME" select="/resultPage/@indexName"/>
	<xsl:variable name="STARTTERM" select="/resultPage/browseIndex/@startTerm"/>
	<xsl:variable name="TERMPAGESIZE" select="/resultPage/browseIndex/@termPageSize"/>
	<xsl:variable name="TERMTOTAL" select="/resultPage/browseIndex/@termTotal"/>
	<xsl:variable name="PAGELASTNO" select="/resultPage/browseIndex/terms/term[position()=last()]/@no"/>
	<xsl:variable name="PAGELASTTERM" select="/resultPage/browseIndex/terms/term[position()=last()]/text()"/>

	<xsl:include href="/home/gsp/fedora-2.1.1/server/jakarta-tomcat-5.0.28/webapps/fedoragsearch/WEB-INF/classes/config/rest/demoCommon.xslt"/>

	<xsl:variable name="EQCHAR">
		<xsl:choose>
			<xsl:when test="$INDEXNAME = 'DemoOnZebra'">=</xsl:when>
			<xsl:otherwise>:</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
							
	<xsl:template name="opSpecifics">
		
		<h2>browseIndex</h2>
			<form method="get" action="rest">
				<table border="3" cellpadding="5" cellspacing="0">
					<tr>
						<td>
							<input type="hidden" name="operation" value="browseIndex"/>
							Start term: <input type="text" name="startTerm" size="30" value="{$STARTTERM}"/> 
							Field name: <select name="fieldName">
					            <xsl:apply-templates select="browseIndex/fields"/>
							</select>
							<xsl:text> </xsl:text>Term page size: <input type="text" name="termPageSize" size="4" value="{$TERMPAGESIZE}"/> 
							<xsl:text> </xsl:text><input type="submit" value="Browse"/>
						</td>
					</tr>
				</table>
			</form>
			<p/>
			<xsl:if test="$TERMTOTAL = 0 and $STARTTERM and $STARTTERM != '' ">
				<p>No terms!</p>
	 		</xsl:if>
			<xsl:if test="$TERMTOTAL > 0">
	 			<table border="0" cellpadding="5" cellspacing="0">
					<tr>
						<xsl:if test="99999999 > $TERMTOTAL">
							<td><xsl:value-of select="$TERMTOTAL"/> terms found.
							</td>
	 					</xsl:if>
					  <xsl:if test="$TERMTOTAL > $PAGELASTNO">
	 					<td>
						<form method="get" action="rest">
							<input type="hidden" name="operation" value="browseIndex"/>
							<input type="hidden" name="fieldName" value="{$FIELDNAME}"/>
							<input type="hidden" name="indexName" value="{$INDEXNAME}"/>
							<input type="hidden" name="startTerm" value="{$PAGELASTTERM}"/>
							<input type="hidden" name="termPageSize" value="{$TERMPAGESIZE}"/>
							<input type="submit" value="Next term page"/>
						</form>
	 					</td>
	 				  </xsl:if>
					</tr>
				</table>
				<table border="3" cellpadding="5" cellspacing="0" bgcolor="silver">
					<xsl:apply-templates select="browseIndex/terms"/>
				</table>
	 		</xsl:if>
	</xsl:template>

	<xsl:template match="field">
		<xsl:variable name="THISFIELDNAME" select="text()"/>
		<xsl:choose>
			<xsl:when test="$FIELDNAME=$THISFIELDNAME">
				<option selected="true">
				    <xsl:attribute name="value">
				        <xsl:value-of select="$THISFIELDNAME"/>
					</xsl:attribute>
				    <xsl:value-of select="$THISFIELDNAME"/>
				</option>
			</xsl:when>
			<xsl:otherwise>
				<option>
				    <xsl:attribute name="value">
				        <xsl:value-of select="$THISFIELDNAME"/>
					</xsl:attribute>
				    <xsl:value-of select="$THISFIELDNAME"/>
				</option>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="term">
		<tr>
			<td>
				<xsl:value-of select="@no"/>.
				<a>
					<xsl:variable name="TERM" select="text()"/>
					<xsl:variable name="QUERYSTRING" select="concat('operation=gfindObjects', '&amp;', 'indexName=', $INDEXNAME, '&amp;', 'query=', $FIELDNAME, $EQCHAR, $TERM)"/>
					<xsl:attribute name="href">/fedoragsearch/rest?<xsl:value-of select="$QUERYSTRING"/>
					</xsl:attribute>
					<xsl:value-of select="$TERM"/>
					[<xsl:value-of select="@fieldtermhittotal"/>]
				</a>
			</td>
		</tr>
	</xsl:template>
	
</xsl:stylesheet>	





				



