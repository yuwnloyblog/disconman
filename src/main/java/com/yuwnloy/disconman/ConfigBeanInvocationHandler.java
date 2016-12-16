package com.yuwnloy.disconman;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.InvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yuwnloy.disconman.annotations.BooleanDefaultValue;
import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.IntDefaultValue;
import com.yuwnloy.disconman.annotations.LongDefaultValue;
import com.yuwnloy.disconman.annotations.StringDefaultValue;
import com.yuwnloy.disconman.persistences.AttributeDetail;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class ConfigBeanInvocationHandler implements InvocationHandler {
	private static Logger s_logger = LoggerFactory.getLogger(ConfigBeanInvocationHandler.class);

	private interface MethodInvocationHandler {
		public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException, InvalidAttributeValueException, InstantiationException;
	}

	private interface AttributeHandler {
		public MethodInvocationHandler getGetterHandler();

		public MethodInvocationHandler getSetterHandler();
	}

	private Class<?> m_interface;
	private String keyName = null;

	// The actual cache for ALL mbeans
	public static final ConcurrentHashMap<String, ConfigBeanDetail> m_allBeanList = new ConcurrentHashMap<String, ConfigBeanDetail>();

	private Map<Method, MethodInvocationHandler> m_methodHandlers;
	private final ConfigBeanDetail beanDetail;

	/**
	 * 
	 * @param detail
	 *            : The implementation
	 * @throws NoSuchMethodException
	 */
	public ConfigBeanInvocationHandler(ConfigBeanDetail detail) throws NoSuchMethodException {
		this.beanDetail = detail;
		if (detail == null) {
			s_logger.warn("detail is null. return null.");
			return;
		}
		
		m_interface = detail.getIntf();

		m_methodHandlers = new HashMap<Method, MethodInvocationHandler>(m_interface.getMethods().length);

		String jmxDomainName = detail.getObjName().getDomain();
		String mbeanName = detail.getObjName().getKeyPropertyListString();
		// The keyname is used to persist the information. We use the partition name if supplied:
		keyName = jmxDomainName + ":" + mbeanName;
		m_allBeanList.put(keyName, detail); // init the attlist map
		
		Map<String, AttributeHandler> attributeHandlers = new HashMap<String, AttributeHandler>();
		for (Method method : m_interface.getMethods()) {
			final String methodName = method.getName();

			String attributeName = null;
			boolean isGetter = false;
			if (methodName.startsWith("set")) {
				attributeName = methodName.substring(3);
			} else if (methodName.startsWith("get")) {
				attributeName = methodName.substring(3);
				isGetter = true;
			} else if (methodName.startsWith("is")) {
				attributeName = methodName.substring(2);
				isGetter = true;
			}

			if (attributeName != null) {
				AttributeHandler attributeHandler = null;
				if (!attributeHandlers.containsKey(attributeName)){
					//add attribute detail to map
					AttributeDetail attDetail = this.getAttDetail(method);
					if (m_allBeanList.containsKey(keyName)) {
						ConcurrentHashMap<String, AttributeDetail> attDetailMap = m_allBeanList.get(keyName).getAttDetailMap();
						if (!attDetailMap.containsKey(attributeName)) {
							attDetailMap.put(attributeName, attDetail);
						}
					}
					//build attribute handler
					attributeHandler = buildAttributeHandler(method, attributeName);
					attributeHandlers.put(attributeName, attributeHandler);
				}else{
					attributeHandler = attributeHandlers.get(attributeName);
				}
				if(isGetter){
					m_methodHandlers.put(method, attributeHandler.getGetterHandler());
				}else{
					m_methodHandlers.put(method, attributeHandler.getSetterHandler());
				}
			} else {
				throw new NoSuchMethodException(
						String.format("The following method is missing " + "in the definition of the mbean \"%s\": %s!",
								m_interface.getName(), method.toString()));
			}
			
		}
	}

	/**
	 * build getter and setter handler for interface's attributes
	 * 
	 * @param method
	 * @param attributeName
	 * @return
	 */
	private AttributeHandler buildAttributeHandler(final Method method, final String attName) {
		return new AttributeHandler() {
			public MethodInvocationHandler getGetterHandler() {
				return new MethodInvocationHandler() {
					public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
							InvocationTargetException, InvalidAttributeValueException, InstantiationException {
						Object dbValue = null;
						if (m_allBeanList.containsKey(keyName)) {
							ConcurrentHashMap<String, AttributeDetail> m_attributeList = m_allBeanList.get(keyName).getAttDetailMap();
							if (m_attributeList.containsKey(attName)) {
								AttributeDetail attDetail = m_attributeList.get(attName);
								dbValue = attDetail.getValue();
								if (dbValue==null || (dbValue != null
										&& (!MBeanUtil.classEqual(attDetail.getDataTypeClass(), dbValue.getClass())))) {
									dbValue = m_attributeList.get(attName).getDefaultValue();
								}
							}
						}
						return dbValue;
					}
				};
			}

			public MethodInvocationHandler getSetterHandler() {
				return new MethodInvocationHandler() {
					public Object invoke(Object[] args) throws IllegalArgumentException, IllegalAccessException,
							InvocationTargetException, InvalidAttributeValueException, InstantiationException {
						Object value = args[0];
						if(m_allBeanList.containsKey(keyName)){
							ConcurrentHashMap<String, AttributeDetail> m_attributeList = m_allBeanList.get(keyName).getAttDetailMap();
							if(m_attributeList.containsKey(attName)){
								m_attributeList.get(attName).setValue(value);
							}
						}
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
			Object defaultValue = this.getDefaultValue(method);
			detail.setDefaultValue(defaultValue);

			detail.setDataTypeClass(method.getReturnType());
			// set the description for attribute
			Description aDesc = method.getAnnotation(Description.class);
			if (aDesc != null)
				detail.setDescription(aDesc.value());
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
