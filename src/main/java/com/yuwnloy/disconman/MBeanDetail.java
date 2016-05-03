package com.yuwnloy.disconman;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import com.yuwnloy.disconman.persistences.AttributeDetail;
import com.yuwnloy.disconman.persistences.IPersistence;
import com.yuwnloy.disconman.persistences.PersistenceFactory;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class MBeanDetail<T> {
  private ObjectName objName = null;
  private String description = "";
  private Class<T> intf = null;
  private Object implement = null;
  private ConcurrentHashMap<String,Object> attMap = new ConcurrentHashMap<String,Object>();
  private ConcurrentHashMap<String,AttributeDetail> attDetailMap = new ConcurrentHashMap<String,AttributeDetail>();
  private IPersistence persistence;
  public MBeanDetail(Class<T> intf,Object implement,ObjectName objName,String desc, IPersistence persistence) {
    this.objName = objName;
    this.description = desc;
    this.intf = intf;
    this.implement = implement;
    if(persistence!=null)
    	this.persistence = persistence;
    else
    	this.persistence = PersistenceFactory.getPersistenceInstance(ConfigManager.defaultPersistenceType, ConfigManager.defaultFileName);
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

  public Class<T> getIntf() {
    return intf;
  }

  public void setIntf(Class<T> intf) {
    this.intf = intf;
  }
    
  public Object getImplement() {
    return implement;
  }

  public void setImplement(Object implement) {
    this.implement = implement;
  }

  public ConcurrentHashMap<String, Object> getAttMap() {
    return attMap;
  }

  public void setAttMap(ConcurrentHashMap<String, Object> attMap) {
    this.attMap = attMap;
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
	
}
