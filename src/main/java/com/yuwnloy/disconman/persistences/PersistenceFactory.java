package com.yuwnloy.disconman.persistences;
import java.util.concurrent.ConcurrentHashMap;

import com.yuwnloy.disconman.ConfigBeanDetail;
import com.yuwnloy.disconman.exceptions.PersistenceException;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class PersistenceFactory {
	public static enum PersistenceType{
		Memory, DB, XML, Properties
	}

	public static IPersistence getPersistenceInstance(PersistenceType type, String filePath) {
		IPersistence persistence = null;
		/*if (type == PersistenceType.DB) { //store in db
			persistance = DbProperties.getDbProperties();
		} else */
		if (type == PersistenceType.XML) {   //store in xml
			persistence = XmlPersistence.getInstance(PersistenceType.XML,filePath);
		} else if(PersistenceType.Properties.equals(type)){
			persistence = XmlPersistence.getInstance(PersistenceType.Properties,filePath);
		}else{   //store in memory
			persistence = new IPersistence() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 3592970423395913385L;

				public ConcurrentHashMap<String,ConcurrentHashMap<String, Object>> getProperties() {
					// TODO Auto-generated method stub
					return null;
				}

				public Object getProperty(String mbeanName, String propertyName) {
					// TODO Auto-generated method stub
					return null;
				}

				public void setProperty(String mbeanName, String propertyName, Object value, AttributeDetail attDetail) {
					// TODO Auto-generated method stub
				}

				public void setAttDetail(String mbeanName, String propertyName,
						AttributeDetail detail) {
					// TODO Auto-generated method stub

				}

				public AttributeDetail getAttDetail(String mbeanName,
						String propertyName) {
					// TODO Auto-generated method stub
					return null;
				}

				public void setMBeanDesc(String mbeanName, String desc) {
					// TODO Auto-generated method stub

				}

				public String getMBeanDesc(String mbeanName) {
					// TODO Auto-generated method stub
					return "";
				}			
				public void clearMBeanProperties(String ObjectName)
				{

				}

				@Override
				public void storeProperties(String domain, ConcurrentHashMap<String, ConfigBeanDetail> map)
						throws PersistenceException {
					// TODO Auto-generated method stub
					
				}     


			};
		}
		return persistence;
	}

}
