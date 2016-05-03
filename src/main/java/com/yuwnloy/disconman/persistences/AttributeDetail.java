package com.yuwnloy.disconman.persistences;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class AttributeDetail {
	private String description = "";
	private String dataType = "";
	//private DefaultValueAttribute defaultValueAttribute = null;
	private Object value;
	private Object defaultValue;
	
	private IPersistence persistence = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.Memory,null);
	//private Class<? extends CustomPropertyValidator> validatorClass = null;
	
	
	

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
	public IPersistence getPersistence() {
		return persistence;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setPersistence(IPersistence persistence) {
		this.persistence = persistence;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	/*public Class<? extends CustomPropertyValidator> getValidatorClass() {
		return validatorClass;
	}

	public void setValidatorClass(
			Class<? extends CustomPropertyValidator> validatorClass) {
		this.validatorClass = validatorClass;
	}*/
	public boolean isMemory(){
		boolean ret = true;
		if(persistence instanceof XmlPersistence){
			ret = false;
		}
		return ret;
	}
}
