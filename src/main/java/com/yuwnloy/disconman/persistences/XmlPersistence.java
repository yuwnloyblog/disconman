package com.yuwnloy.disconman.persistences;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yuwnloy.disconman.IPersistence;
import com.yuwnloy.disconman.PersistenceFactory;
import com.yuwnloy.disconman.exceptions.PersistenceException;


/**
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
public class XmlPersistence implements IPersistence, Serializable {
	
	
	private final static String CLASS_NAME = XmlPersistence.class.getName();
	private static Logger s_logger = Logger.getLogger(CLASS_NAME);

	private static XmlPersistence instance = null;
	private static String xmlFile = null;

	private static final long serialVersionUID = 1L;
	private transient IConfigurationHandler xmlHandler = null;
	private String tmpMBeanDesc = "";
	private static String xmlFileName = "configure";

	public static void init(String xmlFilePath, String fileName,PersistenceFactory.PersistenceType type) {
		final String loggerMethodName = "init";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to init");
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName,
				"xmlFilePath = " + xmlFilePath + ", fileName = " + fileName);

		xmlFile = xmlFilePath;
		if (fileName != null && !fileName.equals(""))
			xmlFileName = fileName;
		// init xml file
		File xml = new File(xmlFile);
		if (!xml.exists()) {
			if(PersistenceFactory.PersistenceType.XML.equals(type))
				XmlConfigurationHandler.getInstance().writeToFile(xml, null);
			else if(PersistenceFactory.PersistenceType.Properties.equals(type)){
				PropertiesConfigurationHandler.getInstance().writeToFile(xml, null);
			}
		}
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to init");
	}

	public static void Clear() {
		final String loggerMethodName = "Clear";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to Clear");

		instance = null;
		xmlFile = null;
		xmlFileName = "configure";
		XmlConfigurationHandler.Clear();

		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to Clear");
	}

	public static synchronized XmlPersistence getInstance(PersistenceFactory.PersistenceType persistenceType) {
		final String loggerMethodName = "getInstance";
		s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "Begin to getInstance");
		if (instance == null) {
			instance = new XmlPersistence(persistenceType);
		}
		s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "End to getInstance and return instance");
		return instance;
	}

	private XmlPersistence(PersistenceFactory.PersistenceType persistenceType) {
		final String loggerMethodName = "XmlProperties";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to XmlProperties");
		// init xml handler.
		if(PersistenceFactory.PersistenceType.XML.equals(persistenceType))
			this.xmlHandler = XmlConfigurationHandler.getInstance();
		else if(PersistenceFactory.PersistenceType.Properties.equals(persistenceType)){
			this.xmlHandler = PropertiesConfigurationHandler.getInstance();
		}else{
			this.xmlHandler = PropertiesConfigurationHandler.getInstance();
		}
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to XmlProperties");
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> getProperties()
			throws PersistenceException {
		//final String loggerMethodName = "getProperties";
		// s_logger.logp(Level.INFO, CLASS_NAME, loggerMethodName, "isOldDB = "
		// + isOldDB);

		Collection<XmlConfiguration> configList = this.getConfigList();
		ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> map = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getAttMap() != null && config.getAttMap().size() > 0) {
					map.put(config.getFullName(), config.getAttMap());
				}
			}
		}
		return map;
	}

	public Object getProperty(String mbeanName, String propertyName) throws PersistenceException {
		final String loggerMethodName = "getProperty";
		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					if (config.getAttMap() != null) {
						s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName,
								"End to getProperty and return value = "
										+ config.getAttMap().get(propertyName));
						return config.getAttMap().get(propertyName);
					}
				}
			}
		}
		return null;
	}

	public synchronized void setProperty(String mbeanName, String propertyName, Object value,
			AttributeDetail attDetail) throws PersistenceException {
		final String loggerMethodName = "setProperty";
		Collection<XmlConfiguration> configList = this.getConfigList();		
		
		XmlConfiguration realConfig = null;
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					realConfig = config;
				}
			}
		}
		if (realConfig == null) {
			// add a new configuration
			realConfig = new XmlConfiguration();
			realConfig.setFullName(mbeanName);
			realConfig.setDescription(this.tmpMBeanDesc);
			configList.add(realConfig);
		}
		realConfig.addAttribute(propertyName, value);
		realConfig.addAttDetail(propertyName, attDetail);

		File xmltmpFile = new File(xmlFile);
		// backup xml file
		try {
			String backFileName = xmltmpFile.getAbsolutePath();
			backFileName = backFileName.substring(0, backFileName.length() - xmltmpFile.getName().length());
			if (backFileName.endsWith(File.separator))
				backFileName = backFileName.substring(0, backFileName.length() - 1);
			Date now = new Date();
			// enhance mbean log
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			backFileName = backFileName + File.separator + xmlFileName + "_" + System.getProperty("weblogic.Name") + "_"
					+ sdf.format(now) + ".xml";
			this.xmlHandler.backupXmlFile(xmltmpFile, new File(backFileName));
		} catch (IOException e) {
			s_logger.log(Level.WARNING, "MBean encounter the error when backup xml configuration file.", e);
		}
		this.xmlHandler.writeToFile(xmltmpFile, configList);
	}

	/**
	 * get the config list from xml file
	 * 
	 * @return
	 */
	private Collection<XmlConfiguration> getConfigList() {
		final String loggerMethodName = "getConfigList";

		Collection<XmlConfiguration> configList = null;
		configList = this.xmlHandler.parseFile(new File(xmlFile));
		return configList;
	}

	/**
	 * get the snapshot
	 * 
	 * @return
	 */
	public String getSnapshot() {
		final String loggerMethodName = "getSnapshot";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to getSnapshot");

		Collection<XmlConfiguration> configList = this.xmlHandler.parseFile(new File(xmlFile));

		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to getSnapshot before return writeToString()...");
		return this.xmlHandler.writeToString(configList);
	}

	public void setAttDetail(String mbeanName, String propertyName, AttributeDetail detail)
			throws PersistenceException {
		final String loggerMethodName = "setAttDetail";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to setAttDetail");
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName,
				"mbeanName = " + mbeanName + ", propertyName = " + propertyName);

		Collection<XmlConfiguration> configList = this.getConfigList();
		XmlConfiguration realConfig = null;
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					realConfig = config;
				}
			}
		}
		if (realConfig == null) {
			// add a new configuration
			realConfig = new XmlConfiguration();
			realConfig.setFullName(mbeanName);
			configList.add(realConfig);
		}
		realConfig.addAttDetail(propertyName, detail);
		this.xmlHandler.writeToFile(new File(xmlFile), configList);
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to setAttDetail");
	}

	public AttributeDetail getAttDetail(String mbeanName, String propertyName) throws PersistenceException {
		final String loggerMethodName = "getAttDetail";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to getAttDetail");
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName,
				"mbeanName = " + mbeanName + ", propertyName = " + propertyName);

		// TODO Auto-generated method stub
		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					return config.getAttDetailMap().get(propertyName);
				}
			}
		}
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to getAttDetail and return null");
		return null;
	}

	public void setMBeanDesc(String mbeanName, String desc) throws PersistenceException {
		final String loggerMethodName = "setMBeanDesc";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to setMBeanDesc");
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "mbeanName = " + mbeanName + ", desc = " + desc);
		tmpMBeanDesc = desc;
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to setMBeanDesc tmpMBeanDesc = " + tmpMBeanDesc);
		/*
		 * List<XmlConfiguration> configList = this.getConfigList(); if
		 * (configList != null) { for (XmlConfiguration config : configList) {
		 * if (config.getFullName().equalsIgnoreCase(mbeanName)) {
		 * config.setDescription(desc); } } } this.xmlHandler.writeToFile(new
		 * File(xmlFile), configList);
		 */
	}

	public String getMBeanDesc(String mbeanName) throws PersistenceException {
		final String loggerMethodName = "getMBeanDesc";
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to getMBeanDesc");
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "mbeanName = " + mbeanName);

		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName,
							"Begin to getMBeanDesc and return description = " + config.getDescription());
					return config.getDescription();
				}
			}
		}
		s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to getMBeanDesc and return null");
		return "";
	}

	public void clearMBeanProperties(String ObjectName) throws PersistenceException {

	}

}
