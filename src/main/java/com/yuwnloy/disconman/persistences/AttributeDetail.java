package com.yuwnloy.disconman.persistences;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class AttributeDetail {
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = lock.readLock();
	private WriteLock writeLock = lock.writeLock();
	private String description = "";
	private String dataType = "";
	private Object value;
	private Object defaultValue;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public Class<?> getDataTypeClass(){
		if(dataType.equalsIgnoreCase("Integer")){
			return Integer.class;
		} else if(dataType.equalsIgnoreCase("Long")){
			return Long.class;
		}else if(dataType.equalsIgnoreCase("Boolean")){
			return Boolean.class;
		}else if(dataType.equalsIgnoreCase("String")){
			return String.class;
		}else if(dataType.equalsIgnoreCase("int")){
			return int.class;
		}else if(dataType.equalsIgnoreCase("long")){
			return long.class;
		}else if(dataType.equalsIgnoreCase("boolean")){
			return boolean.class;
		}
		return null;
	}
	public void setDataTypeClass(Class<?> type){
		if(type == Integer.class)
			this.dataType = "Integer";
		else if(type == Long.class)
			this.dataType = "Long";
		else if(type == Boolean.class)
			this.dataType = "Boolean";
		else if(type == String.class)
			this.dataType = "String";
		else if(type == int.class)
			this.dataType = "int";
		else if(type == long.class)
			this.dataType = "long";
		else if(type == boolean.class)
			this.dataType = "boolean";
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Object getValue() {
		this.readLock.lock();
		try{
			return value;
		}finally{
			this.readLock.unlock();
		}
	}

	public void setValue(Object value) {
		this.writeLock.lock();
		try{
			this.value = value;
		}finally{
			this.writeLock.unlock();
		}
	}

	public boolean isMemory(){
		boolean ret = true;
		return ret;
	}

	public ReadLock getReadLock() {
		return readLock;
	}

	public WriteLock getWriteLock() {
		return writeLock;
	}
	
	public void setValueWithTypeCast(String value){
		Class<?> typeClass = this.getDataTypeClass();
		if(Integer.class == typeClass||int.class == typeClass){
			this.setValue(Integer.valueOf(value));
		}else if(Long.class == typeClass || long.class == typeClass){
			this.setValue(Long.valueOf(value));
		}else if(Boolean.class == typeClass || boolean.class == typeClass){
			this.setValue(Boolean.valueOf(value));
		}else if(String.class == typeClass){
			this.setValue(value);
		}
	}
}
