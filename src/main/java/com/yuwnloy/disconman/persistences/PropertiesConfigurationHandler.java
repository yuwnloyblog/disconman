package com.yuwnloy.disconman.persistences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class PropertiesConfigurationHandler implements IConfigurationHandler{
	private final static String CLASS_NAME = PropertiesConfigurationHandler.class.getName();    
	private static Logger s_logger = Logger.getLogger(CLASS_NAME);
	private static PropertiesConfigurationHandler instance = null;
	
	private PropertiesConfigurationHandler(){}
	
	public static PropertiesConfigurationHandler getInstance(){
		if(instance==null)
			init();
		return instance;
	}
	
	private static synchronized void init(){
		if(instance==null)
			instance = new PropertiesConfigurationHandler();
	}
	/**
	 * parse the properties file.
	 * @return
	 */
	 public Collection<XmlConfiguration> parseFile(File prosFile){
		 Collection<XmlConfiguration> configList = new ArrayList<XmlConfiguration>();
		 if(prosFile==null||!prosFile.exists()){
			 s_logger.log(Level.WARNING,
		                String.format("The properties file is not existed!"));
			 return configList;
		 }
		 
		try {
			FileInputStream fis = new FileInputStream(prosFile);
			Properties prop = new Properties();
			prop.load(fis);
			
			//parse properties object
			configList = this.parsePropertiesObj(prop);
		} catch (FileNotFoundException e) {
			s_logger.log(Level.WARNING,
	                String.format("The properties file '%s' is not existed!"));
		} catch (IOException e) {
			s_logger.log(Level.WARNING,
	                String.format("Failed to read the properties file '%s'",prosFile.getAbsoluteFile()));
		}
		
		 return configList;
	 }
	 /**
	  * translate the properties object to config list.
	  * @param props
	  * @return
	  */
	 private Collection<XmlConfiguration> parsePropertiesObj(Properties props){
		 HashMap<String,XmlConfiguration> configurationMap = new HashMap<String,XmlConfiguration>();
		 for(Object key : props.keySet()){
			 String keyStr = key.toString();
			 String[] keyParts = keyStr.split("\\.");
			 if(keyParts.length==2){//bean's description
				 XmlConfiguration configuration = null;
				 if(configurationMap.containsKey(keyStr)){
					 configuration = configurationMap.get(keyStr);
				 }else{
					 configuration = new XmlConfiguration();
					 configurationMap.put(keyStr, configuration);
				 }
				 configuration.setName("type="+keyParts[1]);//set the bean's name
				 configuration.setDomain(keyParts[0]);//set the bean's domain
				 configuration.setDescription(props.getProperty(keyStr));//set the bean's description
			 }else if(keyParts.length==3){//the value of bean's attribute
				 String domainAndName = keyParts[0]+"."+keyParts[1];
				 XmlConfiguration configuration = null;
				 if(configurationMap.containsKey(domainAndName)){
					 configuration = configurationMap.get(domainAndName);
				 }else{
					 configuration = new XmlConfiguration();
					 configurationMap.put(domainAndName, configuration);
				 }
				 //configuration.removeAttribute(keyParts[2]);
				 configuration.addAttribute(keyParts[2], props.get(keyStr));//set the attribute's value of bean
				 
			 }else if(keyParts.length==4){//the detail info of attribute of bean
				 String domainAndName = keyParts[0]+"."+keyParts[1];
				 XmlConfiguration configuration = null;
				 if(configurationMap.containsKey(domainAndName)){
					 configuration = configurationMap.get(domainAndName);
				 }else{
					 configuration = new XmlConfiguration();
					 configurationMap.put(domainAndName, configuration);					
				 }
				 AttributeDetail detail = null;
				 if(configuration.getAttDetailMap()!=null){
					 detail = configuration.getAttDetailMap().get(keyParts[2]);
					 if(detail==null){
						 detail = new AttributeDetail();
						 configuration.addAttDetail(keyParts[2], detail);
					 }
					 
				 }else{
					 detail = new AttributeDetail();
					 configuration.addAttDetail(keyParts[2], detail);
				 }
				 //System.out.println("length4: "+keyStr+","+props.getProperty(keyStr));
				 if("desc".equals(keyParts[3])){
					 detail.setDescription(props.getProperty(keyStr));
				 }else if("type".equals(keyParts[3])){
					 detail.setDataType(props.getProperty(keyStr));
				 }else if("default".equals(keyParts[3])){
					 detail.setDefaultValue(props.get(keyStr));
				 }
			 }
		 }
		 ArrayList<XmlConfiguration> configList = new ArrayList<XmlConfiguration>();
		 for(XmlConfiguration xconfig : configurationMap.values()){
			 if(xconfig.getAttMap()!=null){
				 for(String att : xconfig.getAttMap().keySet()){
					 AttributeDetail detail = xconfig.getAttDetailMap().get(att);
					 if(detail!=null){
						 Object value = xconfig.getAttMap().get(att);
						 detail.setDefaultValue(DataConvert.convert(detail.getDataTypeClass(), detail.getDefaultValue()));
						 detail.setValue(DataConvert.convert(detail.getDataTypeClass(), value));
						 xconfig.getAttMap().put(att, DataConvert.convert(detail.getDataTypeClass(), value));
						 
					 }
				 }
			 }
			 configList.add(xconfig);
		 }
		 
		 return configList;
	 }
	/**
	 * Write config list to properties file.
	 * @param xmlFile
	 * @param configList
	 */
	 public void writeToFile(File propFile, Collection<XmlConfiguration> configList){
		 if(configList!=null){
			 List<String> valueList = new ArrayList<String>();
			 for(XmlConfiguration config : configList){				 
				 String beanName = config.getName();
				 if(beanName!=null)
					 beanName = beanName.replace("type=", "");
				 String domainAndName = config.getDomain()+"."+beanName;
				 valueList.add(domainAndName+"="+config.getDescription());//bean's description
				 for(String attName : config.getAttMap().keySet()){
					 valueList.add(domainAndName+"."+attName+"="+config.getAttMap().get(attName));//att's value
					 //att's detail 
					 if(config.getAttDetailMap()!=null){
						 AttributeDetail detail = config.getAttDetailMap().get(attName);
						 if(detail!=null){
							 valueList.add(domainAndName+"."+attName+".desc="+detail.getDescription());
							 valueList.add(domainAndName+"."+attName+".type="+detail.getDataType());
							 valueList.add(domainAndName+"."+attName+".default="+detail.getDefaultValue());
						 }
					 }
					 valueList.add("");
				 }
			 }
			 ReaderWriter.writeToFileByLine(propFile, valueList);
		 }
		
	 }
	public void backupXmlFile(File sourceFile, File targetFile) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public String writeToString(Collection<XmlConfiguration> configList) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args){
		Collection<XmlConfiguration> list = PropertiesConfigurationHandler.getInstance().parseFile(new File("D:/config/xiao1.properties"));
		for(XmlConfiguration conf : list){
			System.out.println(conf.getAttMap().size());
		}
		
	}
}
