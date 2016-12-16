package com.yuwnloy.disconman;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.Domain;
import com.yuwnloy.disconman.annotations.Group;
import com.yuwnloy.disconman.annotations.Name;
import com.yuwnloy.disconman.utils.ThreadPool;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class ConfigManager {
	public final static String defaultDomain = "DefaultDomain";
	public final static String defaultGroup = "DefaultGroup";
	
	
	private final static Logger s_logger = LoggerFactory.getLogger(ConfigManager.class);
	private static ConfigManager s_instance = null;
	
	private Map<Class<?>, Map<ObjectName, Object>> m_mbeanImplementations = new ConcurrentHashMap<Class<?>, Map<ObjectName, Object>>();
	private MBeanServer m_mbeanServer = null;
	
	private ConfigManager(){
		m_mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}
	/**
	 * Desctroy the content related mbean framewrok
	 */
	public static void Destroy() {
		getInstance().unregisterMBeans();
		//
		// configMBean = null;
		s_instance = null;
	}

	/**
	 * Get a MBeanManager instance to use MBean Service
	 * 
	 * @throws RuntimeException
	 *             : if jndi data source validating faile in init(), MBean
	 *             service will not return a instance and throws this exception
	 */
	public static ConfigManager getInstance() {
		if (s_instance == null) {
			init();
		}
		return s_instance;
	}
	
	private static synchronized void init(){
		if(s_instance==null)
			s_instance = new ConfigManager();
	}

	/**
	 * Register an MBean with given interface and implementation with 4
	 * parameters
	 *
	 * Create a dynamic mbean supporting the interface defined by the supplied
	 * intf. The implementation can call the matching method on the supplied
	 * impl object, or from the annotations located on intf.
	 * 
	 * <ul>
	 * <li>Description: specify the description of an attribute, operation or
	 * parameter.
	 * <li>Name: specify the name of a parameter.
	 * <li>Impact: specify the impact of an operation.
	 * <li>Internal: specify if an attribute of operation of for internal use
	 * only.
	 * </ul>
	 * 
	 * @param <T>
	 *            : MBean interface
	 * @return DynamicMBean : Supporting the interface intf.
	 * @throws NotCompliantMBeanException
	 * @throws NoSuchMethodException
	 *             Throw if a method if the implementation of a method of the
	 *             the supplied mbean interface could not be deduced from the
	 *             annotations or the given impl object.
	 * @throws Exception
	 */
	public <T> DynamicMBean createMBean(Class<T> intf)
			throws NotCompliantMBeanException, NoSuchMethodException, Exception {
		MBeanInfo mbInfo = this.getMBeanInfo(intf);
		ConfigBeanDetail detail = new ConfigBeanDetail(intf, mbInfo,this.getObjectName(intf));
		T proxy = generateProxy(detail);
		StandardMBean mbean = new StandardMBeanWithAnnotations(proxy, intf, mbInfo.desc);
		registerMBean(mbean, detail.getObjName(), intf);
		return mbean;
	}
	
	public <T> DynamicMBean createMBean(Class<T> intf, Object impl)
			throws NotCompliantMBeanException, NoSuchMethodException, Exception{
		MBeanInfo mbInfo = this.getMBeanInfo(intf);
		StandardMBean mbean = new StandardMBeanWithAnnotations(impl, intf, mbInfo.desc);
		registerMBean(mbean, this.getObjectName(intf), intf);
		return mbean;
	}

	/**
	 * Return all the mbean implementations of the registered interface
	 * 
	 * @param intf
	 *            : MBean interface name
	 * @return
	 */
	public <T> Collection<T> getMBeans(Class<T> intf) {
		if (m_mbeanImplementations.containsKey(intf)) {
			if (m_mbeanImplementations.get(intf) != null && !m_mbeanImplementations.get(intf).isEmpty()) {
				Collection<Object> coll = m_mbeanImplementations.get(intf).values();
				Collection<T> retColl = new ArrayList<T>();
				for (Object obj : coll) {
					retColl.add(intf.cast(obj));
				}
				return retColl;
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Return the mbean implementation by ObjectName
	 * 
	 * @param intf
	 * @param objName
	 * @return
	 */
	public <T> T getMBean(Class<T> intf, ObjectName objName) {
		if (m_mbeanImplementations.containsKey(intf)) {
			Map<ObjectName, Object> mbeansMap = m_mbeanImplementations.get(intf);

			if (mbeansMap != null && !mbeansMap.isEmpty()) {
				Object obj = mbeansMap.values().iterator().next();
				if (objName != null) {
					obj = mbeansMap.get(objName);
				}
				return intf.cast(obj);
			}
		}
		return null;
	}

	/**
	 * Return the mbean implementation for the current instance
	 * 
	 * @param interfaceClass
	 *            : the MBean interface class
	 * @return the instance of the specific MBean interface
	 * @throws InstanceNotFoundException
	 */
	public <T> T getMBean(Class<T> intf) {
		Map<ObjectName, Object> mbeansMap = null;
		if (m_mbeanImplementations.containsKey(intf)) {
			mbeansMap = m_mbeanImplementations.get(intf);
		}

		ObjectName on = getObjectName(intf);
		if (mbeansMap != null && !mbeansMap.isEmpty()) {
			if (on != null && mbeansMap.containsKey(on)) {
				Object obj = mbeansMap.get(on);
				return intf.cast(obj);
			}
		}
		return null;
	}

	/**
	 * unregister all mbeans from MBean server
	 */
	public void unregisterMBeans() {

		for (Class<?> intf : m_mbeanImplementations.keySet()) {
			Map<ObjectName, Object> map = m_mbeanImplementations.get(intf);

			for (ObjectName on : map.keySet()) {
				try {
					this.m_mbeanServer.unregisterMBean(on);
				} catch (InstanceNotFoundException e) {
					s_logger.warn(e.toString());
				} catch (MBeanRegistrationException e) {
					s_logger.warn(e.toString());
				}
			}
		}
		m_mbeanImplementations.clear();
	}

	/**
	 * Check whether the MBean interface has been registered in MBean Server
	 * 
	 * @param intf
	 *            : MBean interface
	 * @return
	 */
	public boolean isRegistered(Class<?> intf) {
		ObjectName objname = getObjectName(intf);
		return this.isRegistered(objname);
	}

	/**
	 * Check whether the MBean Object Name has been registered in MBean Server
	 * 
	 * @param objName
	 *            : the object name of a MBean
	 * @return
	 */
	public boolean isRegistered(ObjectName objName) {
		boolean result = false;
		result = this.m_mbeanServer.isRegistered(objName);
		return result;
	}

	/**
	 * Unregister MBean interface from MBean Server with 1 parameter interface
	 * name, the object name will be created according to MBean Service policy
	 * 
	 * @param intf
	 *            : MBean interface
	 */
	public void unregisterMBean(Class<?> intf) {
		ObjectName objname = getObjectName(intf);
		unregisterMBean(objname);
	}

	/**
	 * Unregister MBean interface from MBean Server with given object name
	 * 
	 * @param objectName
	 *            : the MBean with the given object name will be unregistered
	 *            from MBean Server
	 */
	public void unregisterMBean(ObjectName objectName) {
		try {
			// ObjectName objectName = new ObjectName(name);
			this.m_mbeanServer.unregisterMBean(objectName);
			// remove it from local cache
			for (Class<?> intf : m_mbeanImplementations.keySet()) {
				Map<ObjectName, Object> map = m_mbeanImplementations.get(intf);
				if (map.containsKey(objectName)) {
					map.remove(objectName);
					break;
				}
			}
		} catch (MBeanRegistrationException e) {
			s_logger.warn(e.toString());
		} catch (InstanceNotFoundException e) {
			s_logger.warn(e.toString());
		}
	}

	/**
	 * Unregister All MBean interface from MBean Server by domain
	 * 
	 * @param domain
	 */
	public void unregisterMBeans(String domain) {
		List<ObjectName> objNameList = new ArrayList<ObjectName>();
		for (Class<?> intf : m_mbeanImplementations.keySet()) {
			Map<ObjectName, Object> map = m_mbeanImplementations.get(intf);
			for (ObjectName on : map.keySet()) {
				if (on.getDomain().equalsIgnoreCase(domain)) {
					objNameList.add(on);
				}
			}
		}
		for (ObjectName objName : objNameList) {
			this.unregisterMBean(objName);
		}
	}

	/**
	 * Generate a MBean proxy for the interface and its implement, visiting
	 * mbean information by accessing this proxy
	 * 
	 * @param detail
	 *            : MBean interface information
	 * @return
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 */
	@SuppressWarnings("unchecked")
	private <T> T generateProxy(ConfigBeanDetail detail)
			throws IllegalArgumentException, NoSuchMethodException {
		Class<?> intf = detail.getIntf();
		return (T) intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), new Class[] { intf },
				new ConfigBeanInvocationHandler(detail)));
	}

	/**
	 * Register mbean on the platform mbean server
	 * 
	 * @param mbean
	 *            : (the proxy of)/created dynamic mbean
	 * @param objName
	 *            : object name of this mbean
	 */
	private void registerMBean(StandardMBean mbean, ObjectName objName, Class<?> intf) throws Exception {
		if (objName == null) {
			s_logger.warn("MBean has no name.");
			return;
		}
		if (isRegistered(objName)) {
			unregisterMBean(objName);
		}
		// ObjectName objName = new ObjectName(name);
		this.m_mbeanServer.registerMBean(mbean, objName);

		if (m_mbeanImplementations.containsKey(intf)) {
			Map<ObjectName, Object> map = m_mbeanImplementations.get(intf);
			map.put(objName, mbean.getImplementation());
		} else {
			Map<ObjectName, Object> map = new ConcurrentHashMap<ObjectName, Object>();
			map.put(objName, mbean.getImplementation());
			m_mbeanImplementations.put(intf, map);
		}
	}

	public void removeMBean(String objectName) {
		if (objectName == null || objectName.equals("")) {
			return;
		}

		ObjectName objName = null;
		try {
			objName = new ObjectName(objectName);
		} catch (MalformedObjectNameException e) {
			s_logger.warn(e.toString());
		}
		if (objName != null) {
			unregisterMBean(objName);
		}
	}
	
	/**
	 * Class which contains interface or implement Name and Description
	 */
	public static class MBeanInfo {
		public String name = null;
		public String group = ConfigManager.defaultGroup;
		public String desc = null;
		public String domain = ConfigManager.defaultDomain;
	}
	
	/**
	 * Get interface or implement Name and Description
	 * 
	 * @param cls
	 *            : MBean interface or implement
	 */
	private MBeanInfo getMBeanInfo(Class<?> cls) {
		MBeanInfo mbinfo = new MBeanInfo();

		if (cls == null) {
			//s_logger.error("Interface or Implement is null, throws NullPointerException!");
			throw new NullPointerException("Interface or Implement is null, throws NullPointerException!");
		}
		try {
			mbinfo.name = ((Name) cls.getAnnotation(Name.class)).value();
		} catch (Exception e) {
			// NOTHING TO DO HERE, logger probably here
		}
		try {
			mbinfo.desc = ((Description) cls.getAnnotation(Description.class)).value();
		} catch (Exception e) {
			// NOTHING TO DO HERE, logger probably here
		}
		try {
			mbinfo.domain = ((Domain) cls.getAnnotation(Domain.class)).value();
		} catch (Exception e) {
			// nothing to do here
		}
		try{
			mbinfo.group = ((Group)cls.getAnnotation(Group.class)).value();
		} catch (Exception e){
			
		}
		if(mbinfo.name==null)
			mbinfo.name = cls.getSimpleName();
		return mbinfo;
	}
	/**
	 * Create an Object Name for the interface and implement
	 * 
	 * @param intf
	 *            : MBean interface
	 * @param impl
	 *            : the implement of MBean interface
	 * @return
	 */
	public ObjectName getObjectName(Class<?> intf) {
		MBeanInfo mbeanInfo = this.getMBeanInfo(intf);
		String objname = mbeanInfo.domain + ":group="+mbeanInfo.group+",name=" + mbeanInfo.name;
		ObjectName objName = null;
		try {
			objName = new ObjectName(objname);
		} catch (MalformedObjectNameException e) {
			//s_logger.warn(e.toString());
		}
		return objName;
	}
}
