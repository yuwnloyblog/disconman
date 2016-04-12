package com.yuwnloy.disconman;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.ObjectName;

import com.yuwnloy.disconman.persistences.AttributeDetail;
/**
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
public class MBeanDetail<T> {
  private ObjectName objName = null;
  private String description = "";
  private Class<T> intf = null;
  private Object implement = null;
  private ConcurrentHashMap<String,Object> attMap = new ConcurrentHashMap<String,Object>();
  private ConcurrentHashMap<String,AttributeDetail> attDetailMap = new ConcurrentHashMap<String,AttributeDetail>();
	
  public MBeanDetail(Class<T> intf,Object implement,ObjectName objName,String desc) {
    this.objName = objName;
    this.description = desc;
    this.intf = intf;
    this.implement = implement;
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
	
}
