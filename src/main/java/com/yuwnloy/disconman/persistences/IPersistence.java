package com.yuwnloy.disconman.persistences;

import java.io.Serializable;

import java.util.concurrent.ConcurrentHashMap;

import com.yuwnloy.disconman.ConfigBeanDetail;
import com.yuwnloy.disconman.exceptions.PersistenceException;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public interface IPersistence extends Serializable{
  /**
   * Get all the mbeans' properties
   * @param domainName
   *  
   * @param isOldDB,verified if the db is upgraded accordingly.
   * @return
   */
  public ConcurrentHashMap<String,ConcurrentHashMap<String, Object>> getProperties() throws PersistenceException;
  public void storeProperties(String domain, ConcurrentHashMap<String, ConfigBeanDetail> map) throws PersistenceException;
  /**
   * Get one property based mbeanName
   * @param mbeanName
   * @param propertyName
   * @return
   */
  public Object getProperty(String mbeanName, String propertyName) throws PersistenceException;

  /**
   * Set the property's value
   * @param mbeanName
   * @param propertyName
   * @param value
   * 
   * @param isOldDB ,verified if the db is upgraded accordingly.
   */
  public void setProperty(String mbeanName, String propertyName, Object value, AttributeDetail attDetail) throws PersistenceException;
  
  /**
   * set the attribute's detail info
   * @param mbeanName
   * @param propertyName
   * @param detail
   */
  public void setAttDetail(String mbeanName, String propertyName, AttributeDetail detail) throws PersistenceException;
  /**
   * 
   * @param mbeanName
   * @param propertyName
   * @return
   */
  public AttributeDetail getAttDetail(String mbeanName, String propertyName) throws PersistenceException;
  
  /**
   * set the mbean's description.
   * @param mbeanName
   * @param desc
   */
  public void setMBeanDesc(String mbeanName, String desc) throws PersistenceException;
  
  /**
   * 
   * @param mbeanName
   */
  public String getMBeanDesc(String mbeanName) throws PersistenceException;


public void clearMBeanProperties(String ObjectName) throws PersistenceException;

}
