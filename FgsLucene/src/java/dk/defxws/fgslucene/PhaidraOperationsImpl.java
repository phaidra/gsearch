package dk.defxws.fgslucene;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.defxws.fedoragsearch.server.TransformerToText;
import dk.defxws.fedoragsearch.server.errors.FedoraObjectNotFoundException;
import dk.defxws.fedoragsearch.server.errors.GenericSearchException;

/**
 * Implementierung des Interfaces Operations, auf die Beduerfnisse von
 * Phaidra angepasst. Da Generic Search bei uns immer auf der selben
 * Maschine laeuft wie Fedora selbst, ist das Holen von Fedora-Objekten
 * bzw. -Datastreams ueber SOAP-Aufrufe nicht notwendig - das performt
 * ueber direkten Zugriff ins Filesystem bzw. Datenbank wesentlich
 * besser.
 * 
 * @author Thomas Wana <thomas.wana@univie.ac.at>
 *
 */
public class PhaidraOperationsImpl extends OperationsImpl {
	
    private static final Logger logger = Logger.getLogger(PhaidraOperationsImpl.class);
    
    private byte[] _getFoxmlFromPid(String pid, String repositoryName)
    			throws FedoraObjectNotFoundException
    {
    	 if (logger.isDebugEnabled())
             logger.debug("_getFoxmlFromPid" +
                     " pid="+pid +
                     " repositoryName="+repositoryName);
        
         Connection conn = null;
 		PreparedStatement pstmt = null;
 		ResultSet res = null;
 		byte[] bytes = null;
 		try
 		{
 			// get DB connection from pool
 			Context initContext = new InitialContext();
 			Context envContext  = (Context)initContext.lookup("java:/comp/env"); 			 			
 			
 			DataSource ds = (DataSource)envContext.lookup("jdbc/fedora");
 			conn = ds.getConnection();
 			
 			// It is simple as that!
 			pstmt = conn.prepareStatement("SELECT path FROM objectPaths WHERE token = ?");
 			pstmt.setString(1, pid);
 			
 			res = pstmt.executeQuery();
 			
 			if(!res.next())
 			{
 				throw new Exception("Query returned no results");
 			}
 			String filepath = res.getString(1);
 			logger.debug("FOXML path for PID "+pid+" = "+filepath);
 			
 			// Read file into byte array
 			File file = new File(filepath);
 			InputStream is = new FileInputStream(file);
 			long length = file.length();
 			    
 		    if (length > Integer.MAX_VALUE) {
 		        throw new Exception("File "+filepath+" is too large");
 		    }
 		    
 		    // Create the byte array to hold the data
 	        bytes = new byte[(int)length];
 	    
 	        // Read in the bytes
 	        int offset = 0;
 	        int numRead = 0;
 	        while (offset < bytes.length
 	               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
 	            offset += numRead;
 	        }
 	    
 	        // Ensure all the bytes have been read in
 	        if (offset < bytes.length) {
 	            throw new Exception("Could not completely read file "+file.getName());
 	        }
 	    
 	        // Close the input stream and return bytes
 	        is.close();
 		}
 		catch(Exception e)
 		{
 			logger.error("error while querying database: " + e.toString());
 			throw new FedoraObjectNotFoundException("Couldn't get foxml of Fedora Object "+pid, e);
 		}
 		finally
 		{
 			try
 			{
 				if(res!=null)
 					res.close();
 				if(pstmt!=null)
 					pstmt.close();
 				if(conn!=null)
 					conn.close();
 			}
 			catch(Exception e)
 			{
 				// don't care
 			}
 		}
 		
 		return bytes;
    }
	
    /**
     * FOXML eines Objekts direkt aus dem Filesystem lesen und nicht
     * ueber API-M holen. Erspart einen apim.export-Aufruf pro Indexierung.
     */
    public void getFoxmlFromPid(
            String pid,
            String repositoryName)
    throws java.rmi.RemoteException {
   	 	if (logger.isInfoEnabled())
         logger.info("getFoxmlFromPid" +
                 " pid="+pid +
                 " repositoryName="+repositoryName);
    	this.foxmlRecord = _getFoxmlFromPid(pid, repositoryName);
    }
    
    public String getDatastreamText(
            String pid,
            String repositoryName,
            String dsId,
    		String fedoraSoap,
    		String fedoraUser,
    		String fedoraPass,
    		String trustStorePath,
    		String trustStorePass)
    {
        if (logger.isInfoEnabled())
            logger.info("getDatastreamText"
            		+" pid="+pid
            		+" repositoryName="+repositoryName
            		+" dsId="+dsId
            		+" fedoraSoap="+fedoraSoap
            		+" fedoraUser="+fedoraUser
            		+" fedoraPass="+fedoraPass
            		+" trustStorePath="+trustStorePath
            		+" trustStorePass="+trustStorePass);
        StringBuffer dsBuffer = new StringBuffer();
        
        try
        {
        
	        // FOXML des Objekts holen
	        byte[] foxml = _getFoxmlFromPid(pid, repositoryName);
	        
	        // FOXML parsen
	        Document doc = null;
	        try
	        {
		        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        factory.setNamespaceAware(true); // never forget this!
		        DocumentBuilder builder = factory.newDocumentBuilder();
		        doc = builder.parse(new ByteArrayInputStream(foxml));
	        }
	        catch(Exception ex)
	        {
	        	throw new GenericSearchException("Error parsing FOXML: "+ex.toString());
	        }
	        
	        // <foxml:datastream ID="OCTETS" STATE="A" CONTROL_GROUP="M" VERSIONABLE="true">
	        // <foxml:datastreamVersion ID="OCTETS.0" LABEL="12-13-2007_8307824.pdf" CREATED="2009-04-09T16:25:58.246Z" MIMETYPE="application/pdf">
	        // <foxml:contentLocation TYPE="INTERNAL_ID" REF="o:4868+OCTETS+OCTETS.0"/>
	        // </foxml:datastreamVersion>
	        // </foxml:datastream>
	
	        String mimetype = "";
	        String filepath = "";
	        String token = "";
	        try
	        {
		        XPath xpath = XPathFactory.newInstance().newXPath();
		        xpath.setNamespaceContext(new FOXMLNamespaceContext());
		        Node n = (Node)xpath.evaluate("/foxml:digitalObject/foxml:datastream[@ID='"+dsId+"']/foxml:datastreamVersion[last()]", doc, XPathConstants.NODE);
		        
		        mimetype = n.getAttributes().getNamedItem("MIMETYPE").getNodeValue();
		        Node nc = (Node)xpath.evaluate("foxml:contentLocation[@TYPE='INTERNAL_ID']/@REF", n, XPathConstants.NODE);
		        token = nc.getNodeValue(); 
	        }
	        catch(Exception ex)
	        {
	        	throw new GenericSearchException("Error during XPath query: "+ex.toString());
	        }
	        
	        logger.debug("After XPath query: PID = "+pid+", dsId = "+dsId+", mimetype = "+mimetype+", token = "+token);	      
	        
	        Connection conn = null;
			PreparedStatement pstmt = null;
			ResultSet res = null;
			ds = null;
			try
			{
				// get DB connection from pool
				Context initContext = new InitialContext();
				Context envContext  = (Context)initContext.lookup("java:/comp/env");
				DataSource dso = (DataSource)envContext.lookup("jdbc/fedora");
				conn = dso.getConnection();
				
				// It is simple as that!
				pstmt = conn.prepareStatement("SELECT path FROM datastreamPaths WHERE token = ?");
				pstmt.setString(1, token);
				
				res = pstmt.executeQuery();
				
				if(!res.next())
				{
					throw new Exception("Query returned no results");
				}
				filepath = res.getString(1);
				logger.debug("Datastream path for ds "+dsId+" = "+filepath);
				
				// Read file into byte array
				File file = new File(filepath);
				InputStream is = new FileInputStream(file);
				long length = file.length();
				    
			    if (length > Integer.MAX_VALUE) {
			        throw new Exception("File "+filepath+" is too large");
			    }
			    
			    // Create the byte array to hold the data
		        ds = new byte[(int)length];
		    
		        // Read in the bytes
		        int offset = 0;
		        int numRead = 0;
		        while (offset < ds.length
		               && (numRead=is.read(ds, offset, ds.length-offset)) >= 0) {
		            offset += numRead;
		        }
		    
		        // Ensure all the bytes have been read in
		        if (offset < ds.length) {
		            throw new Exception("Could not completely read file "+file.getName());
		        }
		    
		        // Close the input stream and return bytes
		        is.close();
			}
			catch(Exception e)
			{
				logger.error("error while querying database: " + e.toString());
				throw new GenericSearchException("Couldn't get path of datastream "+dsId, e);
			}
			finally
			{
				try
				{
					if(res!=null)
						res.close();
					if(pstmt!=null)
						pstmt.close();
					if(conn!=null)
						conn.close();
				}
				catch(Exception e)
				{
					// don't care
				}
			}
	        
	        if (ds != null) {
	            dsBuffer = (new TransformerToText().getText(ds, mimetype));
	        }
	        if (logger.isDebugEnabled())
	            logger.debug("getDatastreamText" +
	                    " pid="+pid+
	                    " dsId="+dsId+
	                    " mimetype="+mimetype+
	                    " dsBuffer="+dsBuffer.toString());
	        return dsBuffer.toString(); 
        }
        catch(Exception ex2)
        {
        	logger.warn("Caught exception during operation - calling upstream's OperationsImpl "+
        			"to get the datastream the traditional way. Exception was: "+ex2.toString());
        	return super.getDatastreamText(pid, repositoryName, dsId, fedoraSoap, fedoraUser, fedoraPass,
        			trustStorePath, trustStorePass);
        }
    }
}