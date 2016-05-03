package com.yuwnloy.disconman;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InvalidAttributeValueException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yuwnloy.disconman.annotations.BooleanDefaultValue;
import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.IntDefaultValue;
import com.yuwnloy.disconman.annotations.LongDefaultValue;
import com.yuwnloy.disconman.annotations.MethodDefaultValue;
import com.yuwnloy.disconman.annotations.StringDefaultValue;
import com.yuwnloy.disconman.exceptions.PersistenceException;
import com.yuwnloy.disconman.persistences.AttributeDetail;
import com.yuwnloy.disconman.persistences.IPersistence;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class ConfigBeanInvocationHandler<T> implements InvocationHandler {
	private static Logger s_logger = LoggerFactory.getLogger(ConfigBeanInvocationHandler.class);

	private interface MethodInvocationHandler {
		public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException, InvalidAttributeValueException, InstantiationException;
	}

	private interface AttributeHandler {
		public MethodInvocationHandler getGetterHandler();

		public MethodInvocationHandler getSetterHandler();
	}

	private Object m_customImpl;
	private Class<T> m_interface;
	private String keyName = null;

	// Used to ensure only one thread will refresh the cache
	private static AtomicBoolean s_cacheRefreshLocked = new AtomicBoolean(false);
	// Used to detect when the cache need to be refreshed
	private static AtomicLong s_expiracy = new AtomicLong(0);
	// Used to setup a monitor to synchronize access
	// Access needs to be synchronized when the cache is cleared
	// and needs to be reloaded from DB synchronously
	// If not, some accessor will get no entries in the cache as they are not
	// loaded
	private static final Object s_cacheReentrancyMonitor = new Object();
	// The actual cache for ALL mbeans
	public static final ConcurrentHashMap<String, ConfigBeanDetail<?>> m_allAttList = new ConcurrentHashMap<String, ConfigBeanDetail<?>>();

	private Map<Method, MethodInvocationHandler> m_methodHandlers;

	//private MBeanDetail<T> mbeanDetail = null;

	/**
	 * 
	 * @param detail
	 *            : The implementation
	 * @throws NoSuchMethodException
	 */
	public ConfigBeanInvocationHandler(ConfigBeanDetail<T> detail) throws NoSuchMethodException, PersistenceException {

		if (detail == null) {
			s_logger.warn("detail is null. return null.");
			return;
		}
		m_customImpl = detail.getImplement();
		m_interface = detail.getIntf();
		//this.mbeanDetail = detail;

		m_methodHandlers = new HashMap<Method, MethodInvocationHandler>(m_interface.getMethods().length);
		// m_methodHandlerTmpValue = new HashMap<String,
		// Object>(intf.getMethods().length);
		Map<String, Method> missingAttributeHandlers = new HashMap<String, Method>();
		Map<String, AttributeHandler> attributeHandlers = new HashMap<String, AttributeHandler>();

		String jmxDomainName = detail.getObjName().getDomain();
		String mbeanName = detail.getObjName().getKeyPropertyListString();
		// The keyname is used to persist the information. We use the partition
		// name if supplied:
		keyName = jmxDomainName + ":" + mbeanName;
		
		// if(!detail.getIntf().equals(ConfigurationMBean.class)) {
		if (m_allAttList.containsKey(keyName)) {
			detail.setAttMap(m_allAttList.get(keyName).getAttMap());
		}
		m_allAttList.put(keyName, detail); // init the attlist map
		IPersistence persistence = detail.getPersistence();

		try {
			persistence.setMBeanDesc(keyName, detail.getDescription());
		} catch (PersistenceException e) {
			s_logger.warn(String.format("Store the description of mbean: %s in persistence datasource failed", keyName), e);
		} catch (RuntimeException re) {
			s_logger.warn(String.format("Encounter RuntimeException when store the description of mbean: %s.", keyName), re);
		}
		// }
		for (Method method : m_interface.getMethods()) {
			final String methodName = method.getName();

			String attributeName = null;
			boolean isSetter = false;
			boolean isGetter = false;
			if (methodName.startsWith("set")) {
				isSetter = true;
				attributeName = methodName.substring(3);
			} else if (methodName.startsWith("get")) {
				attributeName = methodName.substring(3);
				isGetter = true;
			} else if (methodName.startsWith("is")) {
				attributeName = methodName.substring(2);
				isGetter = true;
			}

			if (attributeName != null) {
				// AttributeHandler attributeHandler =
				// buildAttributeHandler(method, attributeName);
				AttributeHandler attributeHandler = attributeHandlers.get(attributeName);
				if (attributeHandler == null)
					attributeHandler = buildAttributeHandler(method, attributeName);

				// *******************************************************
				if (attributeHandler == null) {// attributeHandler is not null,
												// so do not know the mean of
												// these code.
					attributeHandler = attributeHandlers.get(attributeName);
				} else {
					attributeHandlers.put(attributeName, attributeHandler);
				}

				if (attributeHandler != null) {
					m_methodHandlers.put(method,
							isSetter ? attributeHandler.getSetterHandler() : attributeHandler.getGetterHandler());
					Method sibling = missingAttributeHandlers.get(attributeName);

					if (sibling != null) {
						m_methodHandlers.put(sibling, isSetter ? /* inverted */attributeHandler.getGetterHandler()
								: attributeHandler.getSetterHandler());
					}
				} else {//
					missingAttributeHandlers.put(attributeName, method);
				}
				// ***********************************************************
			} else {
				throw new NoSuchMethodException(
						String.format("The following method is missing " + "in the definition of the mbean \"%s\": %s!",
								m_interface.getName(), method.toString()));
			}
		}
		detail.getPersistence().storeProperties(jmxDomainName, m_allAttList);
	}

	/**
	 * build getter and setter handler for interface's attributes
	 * 
	 * @param method
	 * @param attributeName
	 * @return
	 */
	private AttributeHandler buildAttributeHandler(final Method method, String attributeName) {

		final AttributeDetail attDetail = this.getAttDetail(method);
		final String attName = attributeName;
		// add the description of attribute when first handle it
		if (m_allAttList.containsKey(keyName)) {
			if (!m_allAttList.get(keyName).getAttDetailMap().containsKey(attName)) {
				ConcurrentHashMap<String, AttributeDetail> detailMap = m_allAttList.get(keyName).getAttDetailMap();
				detailMap.put(attName, attDetail);
			}
		}
		final IPersistence m_properties = attDetail.getPersistence();
		return new AttributeHandler() {
			public MethodInvocationHandler getGetterHandler() {
				return new MethodInvocationHandler() {
					@SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
					public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
							InvocationTargetException, InvalidAttributeValueException, InstantiationException {
						//Long now = new Date().getTime();
						Object dbValue = null;
						if (isCacheExpired(attDetail, m_properties)) {
							// access to database and load all the
							// attributes.
							RefreshCacheWorker refreshWorker = new RefreshCacheWorker(m_properties);
							ThreadExecuteQueue.getInstance().execute(refreshWorker);
						}

						ConcurrentHashMap<String, Object> m_attributeList = null;
						if (m_allAttList.containsKey(keyName)) {
							m_attributeList = m_allAttList.get(keyName).getAttMap();
						}
						if (m_attributeList != null) {
							if (m_attributeList.containsKey(attName)) {
								dbValue = m_attributeList.get(attName);
								if (dbValue != null
										&& (!MBeanUtil.classEqual(attDetail.getDataTypeClass(), dbValue.getClass()))) {
									m_attributeList.remove(attName);
								}
							} else {
								ConcurrentHashMap<String, AttributeDetail> attDetailMap = m_allAttList.get(keyName)
										.getAttDetailMap();
								dbValue = attDetail.getDefaultValue();
								// persistent attribute
								StoreAttributeWorker storeWorker = new StoreAttributeWorker(m_properties, keyName,
										attName, dbValue, attDetailMap.get(attName));
								// storeWorker.start();
								ThreadExecuteQueue.getInstance().execute(storeWorker);
								m_attributeList.put(attName, dbValue);
							}

						}else{
							
						}
						return dbValue;
					}
				};
			}

			public MethodInvocationHandler getSetterHandler() {
				return new MethodInvocationHandler() {
					public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
							InvocationTargetException, InvalidAttributeValueException, InstantiationException {
						ConcurrentHashMap<String, AttributeDetail> attDetailMap = m_allAttList.get(keyName)
								.getAttDetailMap();
						//ConcurrentHashMap<String, Object> attMap = m_allAttList.get(keyName).getAttMap();
						Object value = args[0];
						// persistent attribute,update the value
						StoreAttributeWorker storeWorker = new StoreAttributeWorker(m_properties, keyName, attName,
								value, attDetailMap.get(attName));
						ThreadExecuteQueue.getInstance().execute(storeWorker);

						ConcurrentHashMap<String, Object> m_attributeList = m_allAttList.get(keyName)
								.getAttMap();
						m_attributeList.put(attName, value);
						return null;
					}
				};
			}
		};
	}

	/**
	 * get the attribute's default value(come from method's annotation) tanh
	 * modified for default value;
	 * 
	 * @param method
	 * @return
	 */
	private Object getDefaultValue(Method method) {
		Object value = null;
		for (Annotation ann : method.getAnnotations()) {
			if (ann instanceof IntDefaultValue) {
				value = ((IntDefaultValue) ann).value();
			} else if (ann instanceof StringDefaultValue) {
				value = ((StringDefaultValue) ann).value();
			} else if (ann instanceof BooleanDefaultValue) {
				value = ((BooleanDefaultValue) ann).value();
			} else if (ann instanceof LongDefaultValue) {
				value = (((LongDefaultValue) ann).value());
			} else if (ann instanceof MethodDefaultValue) {
				try {
					Method defaultMethod = m_customImpl.getClass().getMethod(((MethodDefaultValue) ann).value());
					// ~ add 'Object[] a = null' to avoid varargs warning ~
					Object[] avoidVarargsWarning = null;
					value = (defaultMethod.invoke(m_customImpl, avoidVarargsWarning));
				} catch (Exception e) {
					s_logger.error("MBEAN:Exception encountered.", e);
				}
			}
		}
		if (value == null) { // not exist annotation, set default value
								// according to data type
			Class<?> retType = method.getReturnType();
			if (retType == int.class) {
				value = 0;
			} else if (retType == boolean.class) {
				value = false;
			} else if (retType == long.class) {
				value = 0;
			}
		}
		return value;
	}

	/**
	 * get the detail of attribute from annotation
	 * 
	 * @param method
	 * @return
	 */
	private AttributeDetail getAttDetail(Method method) {
		AttributeDetail detail = new AttributeDetail();
		if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
			// set the default value
			Object defaultValueAttr = this.getDefaultValue(method);

			// tanh modified for default value
			detail.setDefaultValue(defaultValueAttr);

			detail.setDataTypeClass(method.getReturnType());
			// set the description for attribute
			Description aDesc = method.getAnnotation(Description.class);
			if (aDesc != null)
				detail.setDescription(aDesc.value());
			// set the persistence way for attribute
			//detail.setPersistence(this.getPersistenceProperty(method));
		} else if (method.getName().startsWith("set")) {
			String attName = method.getName().substring(3);
			Object[] avoidVarargsWarning = null;
			Method getter = null;
			try {
				getter = m_interface.getMethod("get" + attName, (Class<?>[]) avoidVarargsWarning);
			} catch (SecurityException e) {
				s_logger.warn(String
						.format("To reflect method '%s' " + "failed when query attribute detail.", "set" + attName), e);
			} catch (NoSuchMethodException e) {

			}
			if (getter != null) {
				return this.getAttDetail(getter);
			} else {
				Method isser = null;
				try {
					isser = m_interface.getMethod("is" + attName, (Class<?>[]) avoidVarargsWarning);
				} catch (SecurityException e) {
					s_logger.warn(String.format(
							"To reflect method '%s' " + "failed when query attribute detail.", "set" + attName), e);
				} catch (NoSuchMethodException e) {

				}
				if (isser != null)
					return this.getAttDetail(isser);
			}

		}
		return detail;
	}

	/**
	 * invoke the proxy object's method which will be found in m_methodHandlers.
	 * 
	 * @param proxy
	 * @param method
	 * @param args
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodInvocationHandler methodHandler = m_methodHandlers.get(method);
		if (methodHandler == null) {
			throw new NoSuchMethodException(String.format("Missing handler method on of the mbean \"%s\": %s",
					m_interface.getName(), method.toString()));
		}
		return methodHandler.invoke(args);
	}

	/**
	 * @param method
	 * @return
	 */
//	private IPersistence getPersistenceProperty(Method method) {
//		IPersistence per = null;
//		if (method.getAnnotation(Invisible.class) != null) {
//			per = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.Memory);
//		} else if (method.getAnnotation(InXml.class) != null) {
//			per = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.XML);
//		} else if (method.getAnnotation(InDB.class) != null) {
//			per = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.DB);
//		} else if (method.getAnnotation(InMemory.class) != null) {
//			per = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.Memory);
//		} else {
//			per = PersistenceFactory.getPersistenceInstance(PersistenceFactory.PersistenceType.XML);
//		}
//		return per;
//	}

	/**
	 * Calculate time interval and decide whether access db to refresh cache
	 * When s_expiracy is 0, we need to reload the cache synchronously
	 * 
	 * @param attDetail
	 * @return
	 */
	private boolean isCacheExpired(AttributeDetail attDetail, IPersistence m_persistence) {
		boolean cacheExpired = false;
		long cacheTimeOut = 30 * 60 * 1000;// this.configMBean.getCacheValidityPeriodInMillis();

		// TODO: Figure out why we check the attribute detail memory setting to
		// decide
		// why to refresh a global cache that will not even change the value
		// read
		// immediately as it is a background thread that will be invoked....
		// TODO: Figure out why the cache refresh thread is not started here
		// instead of in the caller
		if (!attDetail.isMemory()) {
			// If first time we are invoked OR we require the cache to be
			// cleared
			// If not first time we are invoked, we need to verify the time
			if (s_expiracy.get() == 0) {
				// Need to synchronize
				// - no cache exists yet, so no mbean can read any correct
				// values - they all end up here
				// - the cache needs to get cleared - same issue
				synchronized (s_cacheReentrancyMonitor) {
					// still 0 - we are the lucky winner to reload the cache
					if (s_expiracy.get() == 0) {
						// We want the cache cleared, or its already cleared
						//m_allAttList.clear();
						RefreshCacheWorker worker = new RefreshCacheWorker(m_persistence);
						worker.run(); // invoke the cache refresh synchronously
										// to initialize our values
						s_expiracy.set(System.currentTimeMillis() + cacheTimeOut);
					}
				} // synchronized
			} else {
				// If expiracy is passed, we need to refresh the cache
				long now = System.currentTimeMillis();
				if (s_expiracy.get() < now) {
					// We need to refresh the cache in the background.
					// Only one thread needs to do so
					boolean isRefreshMine = s_cacheRefreshLocked.compareAndSet(false, true);
					if (isRefreshMine) {
						try {
							// This thread is now in charge of refreshing the
							// cache via background thread
							// Other threads will access the cache as usual
							s_expiracy.set(now + cacheTimeOut);
							cacheExpired = true;
						} finally {
							s_cacheRefreshLocked.set(false);
						}
					}
				}
			}
		} // memory

		return cacheExpired;

	}

	// Commented out and does not work [race conditions]
	// public void CompellentRefreshCache(IPersistence m_persistence){
	// RefreshCacheWorker worker = new RefreshCacheWorker(m_persistence);
	// worker.run();
	//
	// synchronized(s_cacheReentrancyMonitor) {
	// long now = System.currentTimeMillis();
	// s_expiracy = now + this.configMBean.getCacheValidityPeriodInMillis();
	// }
	//
	// s_logger.log(Level.SEVERE,"cache has been refreshed.");
	// }
	//

	/**
	 * Clear the cache. WARNING- This introduce a race condition as when the
	 * cache gets cleard some other threads may be reading values from it and
	 * not finding any. It is a problem with the current architecture that will
	 * need to be addressed.
	 */
	public static void Clear() {
		s_expiracy.set(0);
	}

	/**
	 * Responsible for the refresh cache
	 */
	class RefreshCacheWorker extends Thread {
		IPersistence m_properties;

		public RefreshCacheWorker(IPersistence m_properties) {
			this.m_properties = m_properties;
		}

		@Override
		public void run() {
			try {
				if (this.m_properties != null) {
					ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> allMap = this.m_properties
							.getProperties();
				
					refreshCache(allMap);
				}
			} catch (PersistenceException e) {
				// DbExceptionException logging is special
				s_logger.warn(String.format("Load all attributes from persistence datasource failed!"), e);
			}
		}

		/**
		 * refresh the cache from persistence datasource.
		 * 
		 * @param map
		 */
		private void refreshCache(ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> map) {
			if (map != null) {
				for (String key : map.keySet()) {
					ConcurrentHashMap<String, Object> attMap = map.get(key);
					if (m_allAttList.containsKey(key)) {
						m_allAttList.get(key).setAttMap(attMap);
					} else {
						// MBeanDetail(Class<T> intf,Object implement,ObjectName
						// objName,String desc)
						ConfigBeanDetail<?> detail = new ConfigBeanDetail(null, null, null, null,null);
						detail.setAttMap(attMap);
						m_allAttList.put(key, detail);
					}
				}
			}
		}
	}

	class StoreAttributeWorker extends Thread {
		private IPersistence m_properties;
		private String kName;
		private String aName;
		private Object value;
		private AttributeDetail attDetail;

		public StoreAttributeWorker(IPersistence properties, String keyName, String attName,
				Object value, AttributeDetail attDetail) {
			this.m_properties = properties;
			this.kName = keyName;
			this.aName = attName;
			this.value = value;
			this.attDetail = attDetail;
		}

		@Override
		public void run() {
			try {
				if (m_properties != null)
					this.m_properties.setProperty(kName, aName, value, attDetail);
			} catch (PersistenceException e) {
				s_logger.warn(String.format("Persistence the mbean(%s)'s attribute(%s) failed.", kName, aName), e);
			} catch (RuntimeException re) {
				s_logger.warn(String.format("Encounter RuntimeException when store the attribute(%s) of mbean(%s).", aName, keyName), re);
			}
		}

		public String getKeyName() {
			return kName;
		}

		public String getAttributeName() {
			return aName;
		}

		public Object getValue() {
			return value;
		}
	}

	static class MBeanUtil {
		public static boolean classEqual(Class<?> mbeanValueClass, Class<?> dbValueClass) {
			if (mbeanValueClass == int.class && dbValueClass == Integer.class)
				return true;
			if (mbeanValueClass == long.class && dbValueClass == Long.class)
				return true;
			if (mbeanValueClass == boolean.class && dbValueClass == Boolean.class)
				return true;
			return mbeanValueClass.equals(dbValueClass);

		}
	}
}
