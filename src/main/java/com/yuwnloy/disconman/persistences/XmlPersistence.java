package com.yuwnloy.disconman.persistences;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yuwnloy.disconman.ConfigBeanDetail;
import com.yuwnloy.disconman.exceptions.PersistenceException;


/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class XmlPersistence implements IPersistence, Serializable {
	private static Logger s_logger = LoggerFactory.getLogger(XmlPersistence.class);
	private static final long serialVersionUID = 1L;
	private transient IConfigurationHandler xmlHandler = null;
	private String tmpMBeanDesc = "";
	private String filePath = null;

	public static void Clear() {
		XmlConfigurationHandler.Clear();
	}
	/**
	 * generate the XmlPersistence instance.
	 * @param persistenceType
	 * @param filePath
	 * @return
	 */
	public static XmlPersistence getInstance(PersistenceFactory.PersistenceType persistenceType,String filePath) {
		XmlPersistence instance = new XmlPersistence(persistenceType, filePath);
		return instance;
	}

	private XmlPersistence(PersistenceFactory.PersistenceType persistenceType,String filePath) {
		this.filePath = filePath;
		// init xml handler.
		if(PersistenceFactory.PersistenceType.XML.equals(persistenceType))
			this.xmlHandler = XmlConfigurationHandler.getInstance();
		else if(PersistenceFactory.PersistenceType.Properties.equals(persistenceType)){
			this.xmlHandler = PropertiesConfigurationHandler.getInstance();
		}else{
			this.xmlHandler = PropertiesConfigurationHandler.getInstance();
		}
		/**
		 * init the config file.
		 */
		this.xmlHandler.writeToFile(new File(filePath), null);
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> getProperties()
			throws PersistenceException {

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
		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					if (config.getAttMap() != null) {
						return config.getAttMap().get(propertyName);
					}
				}
			}
		}
		return null;
	}

	public void setProperty(String mbeanName, String propertyName, Object value,
			AttributeDetail attDetail) throws PersistenceException {
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

		File xmltmpFile = new File(this.filePath);
		// backup xml file
		try {
			Date now = new Date();
			// enhance mbean log
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			String backFileName = this.filePath+"_"+sdf.format(now);
			this.xmlHandler.backupXmlFile(xmltmpFile, new File(backFileName));
		} catch (IOException e) {
			s_logger.error("MBean encounter the error when backup xml configuration file.", e);
		}
		this.xmlHandler.writeToFile(xmltmpFile, configList);
	}

	/**
	 * get the config list from xml file
	 * 
	 * @return
	 */
	private Collection<XmlConfiguration> getConfigList() {
		Collection<XmlConfiguration> configList = null;
		configList = this.xmlHandler.parseFile(new File(this.filePath));
		return configList;
	}

	/**
	 * get the snapshot
	 * 
	 * @return
	 */
	public String getSnapshot() {
		Collection<XmlConfiguration> configList = this.xmlHandler.parseFile(new File(this.filePath));
		return this.xmlHandler.writeToString(configList);
	}

	public void setAttDetail(String mbeanName, String propertyName, AttributeDetail detail)
			throws PersistenceException {
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
		this.xmlHandler.writeToFile(new File(this.filePath), configList);
	}

	public AttributeDetail getAttDetail(String mbeanName, String propertyName) throws PersistenceException {
		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					return config.getAttDetailMap().get(propertyName);
				}
			}
		}
		return null;
	}

	public void setMBeanDesc(String mbeanName, String desc) throws PersistenceException {
		tmpMBeanDesc = desc;
		/*
		 * List<XmlConfiguration> configList = this.getConfigList(); if
		 * (configList != null) { for (XmlConfiguration config : configList) {
		 * if (config.getFullName().equalsIgnoreCase(mbeanName)) {
		 * config.setDescription(desc); } } } this.xmlHandler.writeToFile(new
		 * File(xmlFile), configList);
		 */
	}

	public String getMBeanDesc(String mbeanName) throws PersistenceException {
		Collection<XmlConfiguration> configList = this.getConfigList();
		if (configList != null) {
			for (XmlConfiguration config : configList) {
				if (config.getFullName().equalsIgnoreCase(mbeanName)) {
					return config.getDescription();
				}
			}
		}
		return "";
	}

	public void clearMBeanProperties(String ObjectName) throws PersistenceException {

	}
	@Override
	public void storeProperties(String domain, ConcurrentHashMap<String, ConfigBeanDetail<?>> map)
			throws PersistenceException {
		Collection<XmlConfiguration> configList = new ArrayList<XmlConfiguration>();
		for(String key : map.keySet()){
			XmlConfiguration config = new XmlConfiguration();
			config.setName(key);
			config.setDomain(domain);
			config.setAttMap(map.get(key).getAttMap());
			configList.add(config);
			System.out.println("key:"+map.get(key).getIntf().getName());
		}
		this.xmlHandler.writeToFile(new File(this.filePath), configList);
	}

}
