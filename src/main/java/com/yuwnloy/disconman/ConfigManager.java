package com.yuwnloy.disconman;

import java.io.File;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.Domain;
import com.yuwnloy.disconman.annotations.Name;
import com.yuwnloy.disconman.exceptions.PersistenceException;
import com.yuwnloy.disconman.persistences.IPersistence;
import com.yuwnloy.disconman.persistences.PersistenceFactory;
import com.yuwnloy.disconman.persistences.XmlPersistence;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class ConfigManager {
	public final static String defaultDomain = "DefaultDomain";
	public final static String defaultFileName = "config.properties";
	public final static PersistenceFactory.PersistenceType defaultPersistenceType = PersistenceFactory.PersistenceType.Properties;
	
	
	private final static Logger s_logger = LoggerFactory.getLogger(ConfigManager.class);
	private String currentDomain = defaultDomain;
	private static ConfigManager s_instance = null;
	
	private Map<Class<?>, Map<ObjectName, Object>> m_mbeanImplementations = new ConcurrentHashMap<Class<?>, Map<ObjectName, Object>>();
	private MBeanServer m_mbeanServer = null;
	public IPersistence defaultPersistence = null;
//	private PersistenceFactory.PersistenceType persistenceType = PersistenceFactory.PersistenceType.Properties;
//	private String filePath = "config.properties";
	
	public static void create(){
		if(s_instance==null)
			init(null,null);
	}
	public static void create(String domain, PersistenceFactory.PersistenceType persistenceType,String fileName){
		if(PersistenceFactory.PersistenceType.XML.equals(persistenceType)||PersistenceFactory.PersistenceType.Properties.equals(persistenceType)){
			//validate the file.
			File file = new File(fileName);
			if(file.exists()){
				if(file.isDirectory()){
					String ext = "properties";
					if(PersistenceFactory.PersistenceType.XML.equals(persistenceType))
						ext = "xml";
					fileName = fileName+File.separator+"config."+ext;
				}
			}
		}
		if(s_instance==null){
			init(persistenceType, fileName);
		}
	}

	private ConfigManager() {
		this(null,null,null);
	}
	
	private ConfigManager(String mbeanserNamePath){
		this(mbeanserNamePath,null,null);
	}
	
	private ConfigManager(String mbeanserNamePath,PersistenceFactory.PersistenceType persistenceType,String fileName){
		if(mbeanserNamePath!=null){
			try {
				InitialContext ctx = new InitialContext();
				m_mbeanServer = MBeanServer.class.cast(ctx.lookup(mbeanserNamePath));
				s_logger.info("Found WLS runtime MBeanServer");
			} catch (NamingException e) {
				m_mbeanServer = ManagementFactory.getPlatformMBeanServer();
			}
		}else{
			m_mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}
		PersistenceFactory.PersistenceType perType = ConfigManager.defaultPersistenceType;
		if(persistenceType!=null){
			perType = persistenceType;
		}
		String filePath = "config.properties";
		if(fileName!=null){
			filePath = fileName;
		}
		this.initPersistenceFile(perType, filePath);
	}

	/**
	 * Class which contains interface or implement Name and Description
	 */
	static class MBeanInfo {
		String name = "";
		String desc = "";
		String domain = "";
	}
	/**
	 * Init MBeanManager with XML file or directory path contains XML file
	 * 
	 * @param xmlFile
	 */
	private void initPersistenceFile(PersistenceFactory.PersistenceType persistenceType,String filePath) {
		try {
			if(PersistenceFactory.PersistenceType.XML.equals(persistenceType)){//xml
				defaultPersistence = PersistenceFactory.getPersistenceInstance(persistenceType,filePath);
			}else if(PersistenceFactory.PersistenceType.Properties.equals(persistenceType)){//properties file
				defaultPersistence = PersistenceFactory.getPersistenceInstance(persistenceType,filePath);
			}else if(PersistenceFactory.PersistenceType.DB.equals(persistenceType)){//db
				
			}
		} catch (RuntimeException e) {
			s_logger.error("initiate the MBeanManager instance failed with error: [" + e.getMessage() + "]", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Desctroy the content related mbean framewrok
	 */
	public static void Destroy() {
		XmlPersistence.Clear();
		// DbProperties.Clear();
		MBeanInvocationHandler.Clear();
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
			throw new RuntimeException("Please call ConfigManager.create() before get the instance.");
		}
		return s_instance;
	}
	
	private static synchronized void init(PersistenceFactory.PersistenceType persistenceType,String fileName){
		if(s_instance==null)
			s_instance = new ConfigManager(null, persistenceType, fileName);
	}

	

	/**
	 * Register an MBean with given interface with only 1 parameter
	 * 
	 * @param intf
	 *            : MBean interface
	 * @throws NotCompliantMBeanException
	 * @throws NoSuchMethodException
	 * @throws Exception
	 */
	public <T> DynamicMBean createMBean(Class<T> intf)
			throws NotCompliantMBeanException, NoSuchMethodException, Exception {
		return createMBean(intf, null);
	}

	/**
	 * Register an MBean with given interface and implementation with 2
	 * parameters
	 * 
	 * @param intf
	 *            : MBean interface
	 * @param impl
	 *            : MBean instance.
	 * @throws NotCompliantMBeanException
	 * @throws NoSuchMethodException
	 * @throws Exception
	 */
	public <T> DynamicMBean createMBean(Class<T> intf, Object impl)
			throws NotCompliantMBeanException, NoSuchMethodException, Exception {
		String desc = this.getDesc(intf, impl);
		ObjectName objName = getObjectName(intf, impl);
		return createMBean(intf, impl, objName, desc);
	}

	/**
	 * Get the description of class
	 * 
	 * @param intf
	 * @param impl
	 * @return
	 */
	private String getDesc(Class<?> intf, Object impl) {
		String desc = "";
		MBeanInfo mbinfo = new MBeanInfo();

		if (impl != null) {
			mbinfo = getMBeanInfo(impl.getClass());
			desc = mbinfo.desc;
		}

		mbinfo = getMBeanInfo(intf);
		desc = desc.equalsIgnoreCase("") ? mbinfo.desc : desc;
		return desc;
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
	 * @param impl
	 *            : MBean instance(implement)
	 * @param objName
	 *            : Object Name registered in MBean Server
	 * @param description
	 *            : MBean Description
	 * @return DynamicMBean : Supporting the interface intf.
	 * @throws NotCompliantMBeanException
	 * @throws NoSuchMethodException
	 *             Throw if a method if the implementation of a method of the
	 *             the supplied mbean interface could not be deduced from the
	 *             annotations or the given impl object.
	 * @throws Exception
	 */
	public <T> DynamicMBean createMBean(Class<T> intf, Object impl, ObjectName objName, String description)
			throws NotCompliantMBeanException, NoSuchMethodException, Exception {
		// StandardMBean mbean = createMBean(intf, impl, description);
		
		MBeanDetail<T> detail = new MBeanDetail<T>(intf, impl, objName, description, this.defaultPersistence);
		T proxy = generateProxy(detail);
		StandardMBean mbean = new StandardMBeanWithAnnotations(proxy, intf, description);
		registerMBean(mbean, objName, intf);
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
	 * Return the mbean implementation for the current instance
	 * 
	 * @param interfaceClass
	 *            : the MBean interface class
	 * @return the instance of the specific MBean interface
	 * @throws InstanceNotFoundException
	 */
	public <T> T getMBean(Class<T> interfaceClass) {
		return this.getMBean(interfaceClass, null);
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
	 * @param intf
	 *            : the MBean interface class
	 * @param impl
	 *            : the Implement of the MBean interface, it can be null
	 * @return the instance of the specific MBean interface
	 */
	public <T> T getMBean(Class<T> intf, Object impl) {
		Map<ObjectName, Object> mbeansMap = null;
		if (m_mbeanImplementations.containsKey(intf)) {
			mbeansMap = m_mbeanImplementations.get(intf);
		}

		ObjectName on = getObjectName(intf, impl);
		if (mbeansMap != null && !mbeansMap.isEmpty()) {
			if (on != null && mbeansMap.containsKey(on)) {
				Object obj = mbeansMap.get(on);
				return intf.cast(obj);
			}
		}
		return null;
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
	public ObjectName getObjectName(Class<?> intf, Object impl) {
		String name = "";
		String domain = "";
		MBeanInfo mbinfo = new MBeanInfo();

		if (impl != null) {
			mbinfo = getMBeanInfo(impl.getClass());
			domain = mbinfo.domain;
			name = ",name=" + mbinfo.name;
		}
		mbinfo = getMBeanInfo(intf);
		domain = (impl == null || domain.equalsIgnoreCase("")) ? mbinfo.domain : domain;

		if (domain == null || domain.trim().equals("")) {
			domain = currentDomain;
		}
		// add identity into domain
		// domain = configMBean.getGroupIdentity() + domain;
		name = domain + ":type=" + mbinfo.name + name;

		ObjectName objName = null;
		try {
			objName = new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			s_logger.warn(e.toString());
		}
		return objName;
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
		ObjectName objname = getObjectName(intf, null);
		return this.isRegistered(objname);
	}

	/**
	 * Check whether the MBean interface with implement has been registered in
	 * MBean Server
	 * 
	 * @param intf
	 *            : MBean interface
	 * @param impl
	 *            : the implement of MBean interface
	 * @return
	 */
	public boolean isRegistered(Class<?> intf, Object impl) {
		ObjectName objname = getObjectName(intf, impl);
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
		ObjectName objname = getObjectName(intf, null);
		unregisterMBean(objname);
	}

	/**
	 * Unregister MBean interface from MBean Server with 2 parameters interface
	 * name and implement name, the object name will be created according to
	 * MBean Service policy
	 * 
	 * @param intf
	 *            : MBean interface
	 * @param impl
	 *            : the implement of MBean interface
	 */
	public void unregisterMBean(Class<?> intf, Object impl) {
		ObjectName objname = getObjectName(intf, impl);
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
	private <T> T generateProxy(MBeanDetail<T> detail)
			throws IllegalArgumentException, NoSuchMethodException, PersistenceException {
		Class<T> intf = detail.getIntf();
		return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), new Class[] { intf },
				new MBeanInvocationHandler<T>(detail)));
	}

	/**
	 * Get interface or implement Name and Description
	 * 
	 * @param cls
	 *            : MBean interface or implement
	 */
	private MBeanInfo getMBeanInfo(Class<?> cls) {
		String name = "";
		String desc = "";
		;
		String domain = "";
		MBeanInfo mbinfo = new MBeanInfo();

		if (cls == null) {
			s_logger.error("Interface or Implement is null, throws NullPointerException!");
			throw new NullPointerException("Interface or Implement is null, throws NullPointerException!");
		}
		try {
			name = ((Name) cls.getAnnotation(Name.class)).value();
		} catch (NullPointerException e) {
			// NOTHING TO DO HERE, logger probably here
		}
		try {
			desc = ((Description) cls.getAnnotation(Description.class)).value();
		} catch (NullPointerException e) {
			// NOTHING TO DO HERE, logger probably here
		}
		try {
			domain = ((Domain) cls.getAnnotation(Domain.class)).value();
		} catch (NullPointerException e) {
			// nothing to do here
		}
		mbinfo.name = name.equalsIgnoreCase("") ? cls.getSimpleName() : name;
		mbinfo.desc = desc;
		mbinfo.domain = domain;

		return mbinfo;
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
}
