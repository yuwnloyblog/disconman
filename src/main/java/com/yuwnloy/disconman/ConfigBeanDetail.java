package com.yuwnloy.disconman;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.yuwnloy.disconman.ConfigManager.MBeanInfo;
import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.Domain;
import com.yuwnloy.disconman.annotations.Group;
import com.yuwnloy.disconman.annotations.Name;
import com.yuwnloy.disconman.persistences.AttributeDetail;
import com.yuwnloy.disconman.persistences.IPersistence;
import com.yuwnloy.disconman.persistences.PersistenceFactory;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class ConfigBeanDetail {
	private MBeanInfo mbeanInfo = null;
	private ObjectName objName = null;
	private String description = "";
	private Class<?> intf = null;
	private ConcurrentHashMap<String, AttributeDetail> attDetailMap = new ConcurrentHashMap<String, AttributeDetail>();
	private IPersistence persistence;

	public ConfigBeanDetail(Class<?> intf, MBeanInfo mbeanInfo, ObjectName objName, IPersistence persistence) {
		this.mbeanInfo = mbeanInfo;
		this.objName = objName;
		this.description = mbeanInfo.desc;
		this.intf = intf;
		if (persistence != null)
			this.persistence = persistence;
		else
			this.persistence = PersistenceFactory.getPersistenceInstance(ConfigManager.defaultPersistenceType,
					ConfigManager.defaultFileName);
	}

	public ObjectName getObjName() {
		return objName;
	}

	public void setObjName(ObjectName objName) {
		this.objName = objName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Class<?> getIntf() {
		return intf;
	}

	public void setIntf(Class<?> intf) {
		this.intf = intf;
	}

	public ConcurrentHashMap<String, AttributeDetail> getAttDetailMap() {
		return attDetailMap;
	}

	public void setAttDetailMap(ConcurrentHashMap<String, AttributeDetail> attDetailMap) {
		this.attDetailMap = attDetailMap;
	}

	public IPersistence getPersistence() {
		return persistence;
	}

	public void setPersistence(IPersistence persistence) {
		this.persistence = persistence;
	}
	
	

	public MBeanInfo getMbeanInfo() {
		return mbeanInfo;
	}
	
}
