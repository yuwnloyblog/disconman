package com.yuwnloy.disconman.persistences;

import java.util.concurrent.ConcurrentHashMap;
/**
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
public class XmlConfiguration {
  private String domain = "";
  private String name = "";
  private String description = "";
  private ConcurrentHashMap<String,AttributeDetail> attDetailMap = null;
  private ConcurrentHashMap<String,Object> attMap = null;
	
  public void addAttDetail(String attName, AttributeDetail detail){
	if(detail!=null){
	  if (this.attDetailMap == null)
	    this.attDetailMap = new ConcurrentHashMap<String,AttributeDetail>();
	  this.attDetailMap.put(attName, detail);
	}
  }
  public void removeAttDetail(String attName){
	if (this.attDetailMap != null) {
	  this.attDetailMap.remove(attName);
	  if (this.attDetailMap.size() <= 0) {
		this.attDetailMap = null;
	  }
	}
  }
  public void addAttribute(String attName, Object value/*, String desc*/) {
    if (this.attMap == null) {
      this.attMap = new ConcurrentHashMap<String,Object>();
    }
    
    //tanh modified for defaultvalue
    //this.attMap.put(attName, new Attribute(attName,value));
    
    this.attMap.put(attName,value);
    
    //this.addAttDesc(attName, desc);
  }

  public void removeAttribute(String attName) {
    if (this.attMap != null) {
      this.attMap.remove(attName);
      this.removeAttDetail(attName);
      if (this.attMap.size() <= 0) {
        this.attMap = null;
      }
    }
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public ConcurrentHashMap<String,Object> getAttMap() {
    return attMap;
  }
  
  public void setAttMap(ConcurrentHashMap<String,Object> attMap) {
    this.attMap = attMap;
  }
  
  public String getFullName() {
    return this.domain + ":" + this.name;
  }
  
  public void setFullName(String fullName) {
    int index = fullName.lastIndexOf(":");
    if (index >= 0) {
      this.domain = fullName.substring(0, index);
      this.name = fullName.substring(index + 1);
    }
  }
  public String getDescription() {
	return description;
  }
  public void setDescription(String description) {
	this.description = description;
  }
  public ConcurrentHashMap<String, AttributeDetail> getAttDetailMap() {
	return this.attDetailMap;
  }
  public void setAttDetailMap(ConcurrentHashMap<String, AttributeDetail> attDescMap) {
	this.attDetailMap = attDescMap;
  }  
}
