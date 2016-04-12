package com.yuwnloy.disconman;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;

import com.yuwnloy.disconman.exceptions.PersistenceException;
import com.yuwnloy.disconman.persistences.AttributeDetail;
import com.yuwnloy.disconman.persistences.XmlPersistence;
/**
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
public class PersistenceFactory {
	public static enum PersistenceType{
		Memory, DB, XML, Properties
	}

	public static IPersistence getPersistenceInstance(PersistenceType type) {
		IPersistence persistence = null;
		/*if (type == PersistenceType.DB) { //store in db
			persistance = DbProperties.getDbProperties();
		} else */
		if (type == PersistenceType.XML) {   //store in xml
			persistence = XmlPersistence.getInstance(PersistenceType.XML);
		} else if(PersistenceType.Properties.equals(type)){
			persistence = XmlPersistence.getInstance(PersistenceType.Properties);
		}else{   //store in memory
			persistence = new IPersistence() {

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


			};
		}
		return persistence;
	}

}
