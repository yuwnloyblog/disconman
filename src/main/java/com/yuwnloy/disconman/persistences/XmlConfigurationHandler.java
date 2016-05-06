package com.yuwnloy.disconman.persistences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class XmlConfigurationHandler implements IConfigurationHandler{
  private final static String CLASS_NAME = XmlConfigurationHandler.class.getName();    
  private static Logger s_logger = Logger.getLogger(CLASS_NAME);
  private static XmlConfigurationHandler instance = null;
  private XmlConfigurationHandler() {}
 
  /**
   * 
   * @return
   */
  public static synchronized XmlConfigurationHandler getInstance() {
    final String loggerMethodName = "getInstance";
    s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to getInstance");      
    if (instance == null) {
      instance = new XmlConfigurationHandler();
    }
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to getInstance and return instance"); 
    return instance;
  }
  public static void Clear(){
      final String loggerMethodName = "Clear";
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to Clear");      
	  instance = null;
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to Clear and instance = null");  
  }
  
  /**
   * Get all the MBean from an xml config file
   * @param xmlFile
   * @return
   */
  public Collection<XmlConfiguration> parseFile(File xmlFile) {      
	  Collection<XmlConfiguration> configList = new ArrayList<XmlConfiguration>();
      if (this.validateWellformed(xmlFile)) {
        if (this.validateSchema(xmlFile)) {
          DocumentBuilder db = null;
          Document doc = null;
          try {
            db = this.getDocumentBuilder();
	    if (db != null) {
              doc = db.parse(xmlFile);
              configList = this.parseDocument(doc);
            }
          } catch (SAXException e) {
            s_logger.log(Level.WARNING, 
                String.format("Faild to parse the configuration file: '%s' by SAX!\n%s", 
                    xmlFile.getAbsolutePath(), e.getMessage()), e);
          } catch (IOException e) {
	    s_logger.log(Level.WARNING, 
                String.format("Faild to find the configuration file: '%s'!\n%s",
                    xmlFile.getAbsolutePath(), e.getMessage()), e);
          }
	} else {
          s_logger.log(Level.WARNING,
                String.format("The xml file '%s' can not match schema!", xmlFile.getAbsoluteFile()));				
        }
      } else {
        s_logger.log(Level.WARNING,
                String.format("The xml file '%s' is not wellformed!", xmlFile.getAbsoluteFile()));
      }
      return configList;
  }
  
  /**
   * Get all the MBean from an xml style string
   * @param xmlContent
   * @return
   */
  public List<XmlConfiguration> parseXml(String xmlContent) {
    final String loggerMethodName = "parseFile(String xmlContent)";
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to parseFile");
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "xmlContent = " + xmlContent);
    
    List<XmlConfiguration> configList = null;
    
    if (this.validateWellformed(xmlContent)) {
      if (this.validateSchema(xmlContent)) {
    	configList = new ArrayList<XmlConfiguration>();
        try {
          DocumentBuilder db = this.getDocumentBuilder();
          if (db != null) {
            StringReader read = new StringReader(xmlContent);
            Document doc = db.parse(new InputSource(read));
            configList = this.parseDocument(doc);
          }
        } catch (SAXException e) {
          s_logger.log(Level.WARNING,
                String.format("Faild to parse the XML content: '%s' by SAX!\n%s", 
                        xmlContent, e.getMessage()), e);
        } catch (IOException e) {
          s_logger.log(Level.WARNING, String.format("Faild to find the XML content: '%s'!\n%s", 
                        xmlContent, e.getMessage()), e);
        }
      } else {
        s_logger.log(Level.WARNING, String.format("The xml content '%s' can not match schema!", xmlContent));
      }
    } else {
      s_logger.log(Level.WARNING, String.format("The xml content '%s' is not wellformed!", xmlContent));
    }	
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to parseFile and return configList");
    return configList;
  }

  /**
   * get the DocumentBuilder
   * @return
   */
  private DocumentBuilder getDocumentBuilder() {
    final String loggerMethodName = "getDocumentBuilder";
    s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to getDocumentBuilder");      
    
    DocumentBuilderFactory dfb = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = null;
    
    try {
      db = dfb.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      s_logger.log(Level.WARNING, String.format("Can not to create DocumentBuilder!\n%s", e.getMessage()), e);
    }
    s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to getDocumentBuilder and return DocumentBuilder");
    return db;
  }

  /**
   * Get all the MBeans from XML Document
   * @param doc
   * @return
   */
  private List<XmlConfiguration> parseDocument(Document doc) {
    
    List<XmlConfiguration> configList = new ArrayList<XmlConfiguration>();
    Element root = doc.getDocumentElement();
    NodeList sectionNodeList = root.getChildNodes();
    String description = "";
    for (int i = 0; i < sectionNodeList.getLength(); i++) {
      Node sectionNode = sectionNodeList.item(i);
      if (sectionNode.getNodeType() == Element.ELEMENT_NODE) {//this is an element
    	XmlConfiguration xmlConfig = this.parseSection(sectionNode);
        //set the description of mbean
        xmlConfig.setDescription(description);
        configList.add(xmlConfig);
        description = "";
      } else if (sectionNode.getNodeType() == Element.COMMENT_NODE) {//this is a comment
    	String comment = sectionNode.getNodeValue().trim();
    	if(comment.startsWith("Description:")){
    	  description = comment.substring(12).trim();
    	}
      }
    }
    return configList;
  }

  /**
   * Parse the each configurationSection, each section will
   * be an MBean
   * @param sectionNode 
   * @return a {@link XmlConfiguration}
   */
  private XmlConfiguration parseSection(Node sectionNode) {
    
    XmlConfiguration config = new XmlConfiguration();
    //set the domain
    config.setDomain(this.getAttributeValue(sectionNode, "domain"));
    //set the name 
    String configName = this.getAttributeValue(sectionNode, "name");
    if(!configName.contains(",")){
      //configName = "type="+configName;
    	int index  = configName.lastIndexOf(":");
    	if(index!=-1)
    	{
    		configName = configName.substring(0,index+1)+"type="+configName.substring(index+1,configName.length());
    	}else
		{	
			configName = "type="+configName;
		}
    }
    config.setName(configName);
    //config.setName(this.getAttributeValue(sectionNode, "name"));
		
    NodeList attNodeList = sectionNode.getChildNodes();
    String description = "";
    String dataType = "";
    String defaultValue = "";
    for (int i = 0; i < attNodeList.getLength(); i++) {
      Node attNode = attNodeList.item(i);      
      if (attNode.getNodeType() == Element.ELEMENT_NODE) {//element
        String attName = this.getAttributeValue(attNode, "name");
        String attValue = attNode.getTextContent().trim();        
        //add the detail of attribute
        AttributeDetail detail = new AttributeDetail();
        
        detail.setDescription(description);
        detail.setDataType(dataType);
        detail.setValue(DataConvert.convert(detail.getDataTypeClass(), attValue));
        detail.setDefaultValue(DataConvert.convert(detail.getDataTypeClass(), defaultValue));
        
        //valueFromPersistence.setDefaultValueVersion(Integer.valueOf(defaultValueVersion));
        config.addAttDetail(attName, detail);
        //config.addAttribute(attName,detail.getValue());
        
        
        description = "";
        dataType = "";
        defaultValue = "";
      } else if (attNode.getNodeType() == Element.COMMENT_NODE){//comment
    	String comment = attNode.getNodeValue().trim();
    	if (comment.startsWith("Description:")) {
    	  description = comment.substring(12).trim();
    	} else if (comment.startsWith("Type:")) {
    	  String[] strArray = comment.split(",");
    	  if (strArray!=null && strArray.length>0) {
    		for (int j=0; j<strArray.length; j++) {
    		  String item = strArray[j].trim();
    		  if (item.startsWith("Type:")) {
    			dataType = item.substring(5).trim();
    		  } else if (item.startsWith("Default value:")) {
    			defaultValue = item.substring(14).trim();
    		  }
    		}
    	  }
    	}
      }
    }
    return config;
  }

  /**
   * Get the attribute's value of node
   * @param node
   * @param attName
   * @return
   */
  private String getAttributeValue(Node node, String attName) {   
    String ret = null;
    if (node != null) {
      NamedNodeMap attMap = node.getAttributes();
      Node att = attMap.getNamedItem(attName);
      ret = att.getNodeValue();
    }    
    return ret;
  }

  /**
   * write configuration into xml file
   * @param xmlFile
   * @param configList
   */
  public void writeToFile(File xmlFile, Collection<XmlConfiguration> configList) {    
    FileOutputStream fos=null;
    try {
      //generate xml document
      Document doc = this.writeToDocument(configList);
     // FileOutputStream fos=null;
      if (doc != null) {
       // FileOutputStream fos;
        fos = new FileOutputStream(xmlFile);
        StreamResult result = new StreamResult(fos);
        this.transform(doc, result);
        //enhance mbean log
        if (!xmlFile.exists()) { 
            s_logger.logp(Level.WARNING, CLASS_NAME, null, "xmlFile doesn't exist. Target XML configuration file:'%s'" + xmlFile.getAbsoluteFile());
        }else{
            xmlFileDiagnosticLog(Level.INFO, xmlFile);  
        }
      }
    } catch (FileNotFoundException e) {
      s_logger.log(Level.WARNING,String.format("Can not found the target XML configuration file:'%s'.\n%s", xmlFile.getAbsoluteFile(),e.getMessage()),e);
    }finally{
		//fix the fortify bug
		if(fos!=null)
		{
		 try {
                   //passed to the operating system for writing;
                   //The flush method inherited from OutputStream does nothing.
                   fos.flush();
                   fos.close();
                 } catch (IOException e) {
                       s_logger.log(Level.WARNING,String.format("Faild to close fileOutputStream."),e);
                 }
		}



    }  
  }
    /**
     * write configuration into xml file
     * @param level
     * @param xmlFile
     */
    public void xmlFileDiagnosticLog(Level level, File xmlFile){
        final String loggerMethodName = "xmlFileDiagnosticLog";
        if (xmlFile.getParentFile() != null){
            s_logger.logp(level, CLASS_NAME, loggerMethodName, "Directory: " + xmlFile.getParentFile() 
                                                      +", Total Files number: " + xmlFile.getParentFile().listFiles().length 
                                                      + ", Total Space: " + xmlFile.getParentFile().getTotalSpace() + " bytes"
                                                      + ", Free Space: " + xmlFile.getParentFile().getFreeSpace() + " bytes");
        }else{
            s_logger.logp(level, CLASS_NAME, loggerMethodName, "xmlFile Directory is null, xmlFile = " + xmlFile.getAbsoluteFile());
        }
    }
  
  /**
   * backup sourceFile to targetFile
   * @param sourceFile
   * @param targetFile
   * @throws IOException
   */
  public void backupXmlFile(File sourceFile,File targetFile) throws IOException{
	/*BufferedInputStream inBuff = null;
    BufferedOutputStream outBuff = null;
    try {
        // build the inputstream buffer
        inBuff = new BufferedInputStream(new FileInputStream(sourceFile));
        // build the outputstream buffer
        outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

        byte[] b = new byte[1024 * 5];
        int len;
        while ((len = inBuff.read(b)) != -1) {
            outBuff.write(b, 0, len);
        }
        outBuff.flush();
    } finally {
        if (inBuff != null)
            inBuff.close();
        if (outBuff != null)
            outBuff.close();
    }*/   
  }

  /**
   * write configuration into xml style String
   * @param configList
   * @return
   */
  public String writeToString(Collection<XmlConfiguration> configList) {
      final String loggerMethodName = "writeToString";
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to writeToString");      
      
    String ret = null;
    //generate xml document
    Document doc = this.writeToDocument(configList);
    if (doc != null) {
      StringWriter writer = new StringWriter();
      StreamResult strResult = new StreamResult(writer);
      this.transform(doc, strResult);
      ret = strResult.getWriter().toString();  
			 
      try {  
        writer.close();   
      } catch (IOException e) {   
        s_logger.log(Level.WARNING,String.format("Faild to close StringWriter!\n%s",e.getMessage()),e); 
      }
    }
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to writeToString and return = " + ret);     
    return ret;
  }
  
  /**
   * transform docment
   * @param doc
   * @param result
   */
  private void transform(Document doc, Result result) {
      final String loggerMethodName = "transform";
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to transform");
      
    TransformerFactory factory = TransformerFactory.newInstance();
    try {
      Transformer trans = factory.newTransformer();
      trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "xml");
      trans.transform(new DOMSource(doc.getDocumentElement()), result);
    } catch (TransformerException e) {
      s_logger.log(Level.WARNING,String.format("Faild to transform xml document.\n%s",e.getMessage()),e);
    }
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to transform");    
  }
  
  /**
   * write configuration into xml document
   * @param configList
   * @return
  */
  private Document writeToDocument(Collection<XmlConfiguration> configList) {      
    DocumentBuilder db = this.getDocumentBuilder();
    Document doc = db.newDocument();
    Element root = doc.createElement("ConfigurationSnapshot");
    doc.appendChild(root);
    
    if (configList != null) {
      for (XmlConfiguration config : configList) {
    	//add the comment element
    	if(config.getDescription()!=null && !config.getDescription().trim().equals("")){
    		Comment commEle = doc.createComment("Description:"+config.getDescription());
    		root.appendChild(commEle);
    	}
        this.writeSection(doc, root, config);
      }
    } 
    return doc;		
  }
  
  /**
   * generate a MBean node by configuration
   * @param doc
   * @param fatherEle
   * @param config
   */
  private void writeSection(Document doc,Element fatherEle, XmlConfiguration config) {
      final String loggerMethodName = "writeSection";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to writeSection");       
    if (config != null) {
      Element sectionEle = doc.createElement("ConfigurationSection");
      sectionEle.setAttribute("domain", config.getDomain());
      String configName = config.getName();
      if(!configName.contains(","))
      {
    	configName = configName.replace("type=", "");
      }
      sectionEle.setAttribute("name", configName);
      fatherEle.appendChild(sectionEle);
			
      //attribute List
      if (config.getAttDetailMap() != null) {
        for (String attName : config.getAttDetailMap().keySet()) {
          //add the attribute's comment
          if(config.getAttDetailMap() != null){
        	AttributeDetail detail = config.getAttDetailMap().get(attName);
        	if(detail != null){
        	  String desc = detail.getDescription();
        	  if(desc != null){
        		Comment comDesc = doc.createComment("Description:"+desc);
        		sectionEle.appendChild(comDesc);
        	  }
        	  String type = detail.getDataType();        	  
        	  Object defaultvalue = detail.getDefaultValue();
        	  if(type != null || defaultvalue != null){
        		Comment comType = doc.createComment("Type:"+type+", Default value:"+defaultvalue);
        		sectionEle.appendChild(comType);
        	  }
        	}
          }                    
          //add the attribute
          Element attEle = doc.createElement("Attribute");
          attEle.setAttribute("name", attName);
          String value = null;
//          if(config.getAttMap().get(attName)!=null)
//          {
//        	  value = config.getAttMap().get(attName)+"";
//          }
	      attEle.setTextContent(value);
	      //add this attribute node
	      sectionEle.appendChild(attEle);
        }
      }
    }
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to writeSection");  
  }
  
  /**
   * Validate the xml file whether is wellformed.
   * @param xmlFile
   * @return
   */
  public boolean validateWellformed(File xmlFile) {
      final String loggerMethodName = "validateWellformed(File xmlFile)";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to validateWellformed");       
      
    boolean ret = true;
    
    try {
      //this.validateWellformed(new InputSource(xmlFile.getAbsolutePath()));
      this.validateWellformed(new InputSource(new FileInputStream(xmlFile)));
    } catch (ParserConfigurationException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Faild to parse XML file: '%s' when validate it form!\n%s",
                        xmlFile.getAbsoluteFile(), e.getMessage()), e);
    } catch (SAXException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to read the configuration file: '%s' by SAX when validate it form!\n%s",
                        xmlFile.getAbsoluteFile(), e.getMessage()), e);
    } catch (IOException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to load the xml file: '%s' when validate it form!\n%s",
                        xmlFile.getAbsoluteFile(), e.getMessage()), e);
    }	
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to validateWellformed and return = "+ret); 
    return ret;
  }
  
  /**
   * Validate the xml style string whether is wellformed.
   * @param xmlContent
   * @return
   */
  public boolean validateWellformed(String xmlContent) {
      final String loggerMethodName = "validateWellformed(String xmlContent)";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to validateWellformed");
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "xmlContent = " + xmlContent);
    boolean ret = true;
    
    try {
      this.validateWellformed(new InputSource(new StringReader(xmlContent)));
    } catch (ParserConfigurationException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Faild to parse XML String: '%s' when validate it form! \n%s", 
                    xmlContent, e.getMessage()), e);
    } catch (SAXException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to read the XML String: '%s' by SAX when validate it form! \n%s", 
                    xmlContent, e.getMessage()), e);
    } catch (IOException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to load the XML String: '%s' when validate it form! \n%s",
                    xmlContent, e.getMessage()), e);
    }	
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to validateWellformed and return = " + ret);
    return ret;
  }
  
  /**
   * validate whether is wellformed
   * @param inputSource
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  private void validateWellformed(InputSource inputSource) 
                   throws ParserConfigurationException, SAXException, IOException {
      final String loggerMethodName = "validateWellformed(InputSource inputSource)";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to validateWellformed");      
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);

    SAXParser parser = factory.newSAXParser();
    XMLReader reader = parser.getXMLReader();
    reader.setErrorHandler(new ValidateErrorHandler());
    reader.parse(inputSource);
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to validateWellformed");
  }

  /**
   * Validate the xml file whether match the schema
   * @param xmlFile
   * @return
   */
  public boolean validateSchema(File xmlFile) {
      final String loggerMethodName = "validateSchema(File xmlFile)";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to validateSchema");
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "xmlFile = " + xmlFile.getAbsoluteFile());
    boolean ret = true;
    //prepare xml file source to validate
    Source source = new StreamSource(xmlFile);
    //executive validate
    try {
      this.validateSchema(source);
    } catch (SAXException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to read the configuration file: '%s' by SAX when validate it use schema!\n%s",
                    xmlFile.getAbsoluteFile(), e.getMessage()), e);
    } catch (IOException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to load the xml file: '%s' when validate it use schema!\n%s", 
                    xmlFile.getAbsoluteFile(), e.getMessage()), e);
    }
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to validateSchema and return = " + ret);  
    return ret;
  }
  
  /**
   * Validate the xml style string whether match the schema
   * @param xmlContent
   * @return
   */
  public boolean validateSchema(String xmlContent) {
      final String loggerMethodName = "validateSchema(String xmlContent)";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to validateSchema"); 
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "xmlContent = " + xmlContent);
    boolean ret = true;	
    //prepare xml style string source to validate
    Source source = new StreamSource(new StringReader(xmlContent));
    //executive validate
    try {
      this.validateSchema(source);
    } catch (SAXException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to read the XML string: '%s' by SAX when validate it use schema! \n%s",
                    xmlContent, e.getMessage()), e);
    } catch (IOException e) {
      ret = false;
      s_logger.log(Level.WARNING,
            String.format("Can not to load the XML string: '%s' when validate it use schema! \n%s",
                    xmlContent, e.getMessage()), e);
    }
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to validateSchema and return = " + ret); 
    return ret;
  }

  /**
   * validate it whether match the schema
   * @param source
   * @throws SAXException
   * @throws IOException
   */
  private void validateSchema(Source source) throws SAXException, IOException { 
	/**
	 * don't do schema validate!
	 */
    //create a schema factory
    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    Schema schema = null;
    
    try {
      schema = schemaFactory.newSchema(this.getXsdUrl());
    } catch(SAXException e) {
      s_logger.log(Level.WARNING,
            String.format("Can not load the schema file : XMLConfiguration.xsd!\n%s", e.getMessage()), e);
      throw e;
    }
    //create validator by Schema
    Validator validator = schema.newValidator();
    validator.setErrorHandler(new ValidateErrorHandler());
    //executive validate
    validator.validate(source);
  }
  
  /**
   * get the xsd's url
   * @return
   */
  private URL getXsdUrl() {
      final String loggerMethodName = "getXsdUrl";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to getXsdUrl");       
    URL url = XmlConfigurationHandler.class.getClassLoader().getResource("cn/ubaby/configure/persistences/XmlConfiguration.xsd");
    if (url == null) {
      s_logger.log(Level.WARNING,"cn/ubaby/configure/persistences/XmlConfiguration.xsd can not be found in package!");
    }
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to getXsdUrl and return URL"); 
    return url;		
  }
  
  /**
   * reset the Handler.
   */
  public static void Reset(){
      final String loggerMethodName = "Reset";
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to Reset");       
	  instance = null;
      s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to Reset instance = null");   
  }
  
  /**
  * handle the error when validate
  * @author xiaoguang.gao@oracle.com
  *
  */
  class ValidateErrorHandler implements ErrorHandler {    

    public void warning(SAXParseException exception) throws SAXException {
      s_logger.log(Level.WARNING, String.format(exception.getMessage()), exception);
    }

    public void error(SAXParseException exception) throws SAXException {
      s_logger.log(Level.WARNING, String.format(exception.getMessage()), exception);
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      s_logger.log(Level.WARNING, String.format(exception.getMessage()), exception);
    }		
  }
  
  //public static void main(String[] args){
	  /*XmlConfigurationHandler handler = XmlConfigurationHandler.getInstance();
	  List<XmlConfiguration> configList = handler.parseFile(new File("configuration.xml"));
	  handler.writeToFile(new File("D:/new.xml"), configList);*/
  //}
}
