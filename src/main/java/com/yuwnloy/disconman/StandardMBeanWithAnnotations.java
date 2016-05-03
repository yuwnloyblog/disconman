package com.yuwnloy.disconman;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.yuwnloy.disconman.annotations.Hidden;
import com.yuwnloy.disconman.annotations.Impact;
import com.yuwnloy.disconman.annotations.Internal;
import com.yuwnloy.disconman.annotations.Invisible;
import com.yuwnloy.disconman.annotations.Name;
import com.yuwnloy.disconman.annotations.Description;

/**
 * Extend the StandardMBean to customise the generated info based on the following annotations:
 *
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class StandardMBeanWithAnnotations extends StandardMBean {
  private static final String CLASS_NAME = StandardMBeanWithAnnotations.class.getName();
  private static final Logger s_logger = Logger.getLogger(CLASS_NAME);    
  private Map<String, String> m_attributeDescriptions;
  private Map<String, String> m_operationDescriptions;
  private Map<String, Integer> m_operationImpacts;
  private Map<String, ArrayList<String>> m_operationParamNames;
  private Map<String, ArrayList<String>> m_operationParamDescriptions;
  private Set<String> m_internalAttributes;
  private Set<String> m_internalOperations;
  private Map<String, Boolean> m_HiddenAttributes;
  private String m_description;
  private Set<String> m_InvisibleAttributes;
  private Set<String> m_InvisibleOperations;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public StandardMBeanWithAnnotations(Object impl, Class intf, String description) 
                                                               throws NotCompliantMBeanException {
    super(impl, intf);
    
    final String loggerMethodName = "StandardMBeanWithAnnotations";
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to StandardMBeanWithAnnotations()");
    
    m_description = description;
    m_attributeDescriptions = new HashMap<String, String>();
    m_operationDescriptions = new HashMap<String, String>();
    m_operationImpacts = new HashMap<String, Integer>();
    m_operationParamNames = new HashMap<String, ArrayList<String>>();
    m_operationParamDescriptions = new HashMap<String, ArrayList<String>>();
    m_internalAttributes = new HashSet<String>();
    m_internalOperations = new HashSet<String>();
    m_HiddenAttributes = new HashMap<String, Boolean>();
    m_InvisibleAttributes = new HashSet<String>();
    m_InvisibleOperations = new HashSet<String>();

    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "For each method in Methods of intreface to deal with method Begin.");
    for (Method method: intf.getMethods()) {
      Description descriptionAnnotation = method.getAnnotation(Description.class);
      boolean isInternal = method.getAnnotation(Internal.class) != null;
      boolean isHidden = method.getAnnotation(Hidden.class) != null;
      boolean isInvisible = method.getAnnotation(Invisible.class)!=null;
      String attributeName = null;
      String methodName = method.getName();
      Boolean isGetter = false;
        
      if (methodName.startsWith("set")) {
        attributeName = methodName.substring(3);
      } else if (methodName.startsWith("get")) {
        attributeName =  methodName.substring(3);
        isGetter = true;
      } else if (methodName.startsWith("is")) {
        attributeName = methodName.substring(2);
        isGetter = true;
      }
      if (attributeName != null) {
        if (descriptionAnnotation != null) {
          m_attributeDescriptions.put(attributeName, descriptionAnnotation.value());
        }
        if (isInternal) {
          m_internalAttributes.add(attributeName);
        }
        if (isGetter) {
            s_logger.logp(Level.FINEST, CLASS_NAME, loggerMethodName, "method is isGetter=ture, setDefaultValue for attributeName: " + attributeName);
            setDefaultValue(impl, method, attributeName);
        }
        if (isHidden) {
          m_HiddenAttributes.put(attributeName, isGetter);
        }
        if(isInvisible){
          this.m_InvisibleAttributes.add(attributeName);
        }
      } else {
        /* Operation */
        String methodSignature = getSignature(method);
        if (descriptionAnnotation != null) {
          m_operationDescriptions.put(methodSignature,  descriptionAnnotation.value());
        }
        if (isInternal) {
          m_internalOperations.add(methodSignature);
        }
        if(isInvisible){
          this.m_InvisibleOperations.add(attributeName);
        }

        Impact impact = method.getAnnotation(Impact.class);
        if (impact != null) {
          m_operationImpacts.put(methodSignature, impact.value());
        }

        int numParams = method.getParameterAnnotations().length;
        ArrayList<String> paramDescriptions = new ArrayList<String>(numParams);
        ArrayList<String> paramNames = new ArrayList<String>(numParams);
        for (int i = 0; i < numParams; i++) {
          for (Annotation ann: method.getParameterAnnotations()[i]) {
            if (ann instanceof Description) {
              paramDescriptions.add(i, ((Description)ann).value());
            } else if (ann instanceof Name) {
              paramNames.add(i, ((Name) ann).value());
            }
          }
        }
        m_operationParamDescriptions.put(methodSignature, paramDescriptions);
        m_operationParamNames.put(methodSignature, paramNames);
      }
    }
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "For each method in Methods of intreface to deal with annotation End.");
    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to StandardMBeanWithAnnotations()");
  }

  public MBeanInfo getMBeanInfo(){
    MBeanInfo               orgMBeanInfo = super.getMBeanInfo();
    //MBeanAttributeInfo[]    newAtts  = null;
    MBeanOperationInfo[]    newOpers = null;
    ArrayList<MBeanAttributeInfo> attInfoList = new ArrayList<MBeanAttributeInfo>();

    boolean   attUpdated = false;
    boolean   operUpdated = false;
    if (m_HiddenAttributes.size() > 0 || this.m_InvisibleAttributes.size() > 0) {
      MBeanAttributeInfo[] attInfo = orgMBeanInfo.getAttributes();    
      for (int i = 0; i < attInfo.length; i++) {
        if(!this.m_InvisibleAttributes.contains(attInfo[i].getName())){
          MBeanAttributeInfo oldAttributeInfo = (MBeanAttributeInfo)attInfo[i].clone();
          if (m_HiddenAttributes.containsKey(attInfo[i].getName())) {
            boolean removeReadable = (attInfo[i].isReadable() &&  m_HiddenAttributes.get(attInfo[i].getName()));
            boolean removeWritable = (attInfo[i].isWritable() && (m_HiddenAttributes.get(attInfo[i].getName()) == false));
                
            if (removeReadable || removeWritable){              
              MBeanAttributeInfo newAtt = null;
              newAtt = new MBeanAttributeInfo(attInfo[i].getName(),
      	   			                          attInfo[i].getType(),
      						                  attInfo[i].getDescription(),
      						                 (removeReadable) ? false : attInfo[i].isReadable(),
      						                 (removeWritable) ? false : attInfo[i].isWritable(),
      						                 (removeReadable) ? false : attInfo[i].isIs());
                  
              oldAttributeInfo = newAtt;
            }
          }
          attInfoList.add(oldAttributeInfo);
          attUpdated = true;
        }        
      }
    }
    if (attUpdated || operUpdated){    
      MBeanInfo newMBeanInfo = new MBeanInfo(orgMBeanInfo.getClassName(),
            orgMBeanInfo.getDescription(),
            (attUpdated) ? attInfoList.toArray(new MBeanAttributeInfo[]{}):orgMBeanInfo.getAttributes(),
            orgMBeanInfo.getConstructors(),
            (operUpdated) ? newOpers : orgMBeanInfo.getOperations(),
            orgMBeanInfo.getNotifications(),
            orgMBeanInfo.getDescriptor()); 
      return newMBeanInfo;
    }else{
      return orgMBeanInfo;
    }
    /*if (attUpdated || operUpdated){
      MBeanInfo newMBeanInfo = new MBeanInfo(orgMBeanInfo.getClassName(),
                                             orgMBeanInfo.getDescription(),
                                             (attUpdated) ? newAtts : orgMBeanInfo.getAttributes(),
                                             orgMBeanInfo.getConstructors(),
                                             (operUpdated) ? newOpers : orgMBeanInfo.getOperations(),
                                             orgMBeanInfo.getNotifications(),
                                             orgMBeanInfo.getDescriptor()); 
      orgMBeanInfo = newMBeanInfo;
    } else {
      return orgMBeanInfo;
    }*/
  }
  
  protected String getDescription(MBeanAttributeInfo info) {
    String description = m_attributeDescriptions.get(info.getName());
    if (description == null) {
      description = super.getDescription(info);
    }
    if (m_internalAttributes.contains(info.getName())) {
      description = String.format("FOR INTERNAL USE ONLY: %s", description);
    }
    return description;
  }

  protected String getDescription(MBeanOperationInfo info) {
    String signature = getSignature(info);
    String description = m_operationDescriptions.get(signature);
    if (description == null) {
      description = super.getDescription(info);
    }
    if (m_internalOperations.contains(getSignature(info))) {
      description = String.format("FOR INTERNAL USE ONLY: %s", description);
    }
    return description;		
  }

  protected int getImpact(MBeanOperationInfo info) {
    Integer impact = m_operationImpacts.get(getSignature(info));
    if (impact == null) {
      impact = super.getImpact(info);
    }
    return impact;
  }

  private String getSignature(MBeanOperationInfo method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    sb.append("(");
    for (MBeanParameterInfo pi: method.getSignature()) {
      sb.append(pi.getType());
      sb.append(",");
    }
    sb.append(")");
    return sb.toString();
  }


  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    sb.append("(");
    for (@SuppressWarnings("rawtypes") Class pt: method.getParameterTypes()) {
      sb.append(pt.getName());
      sb.append(",");
    }
    sb.append(")");
    return sb.toString();
  }

  protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
    ArrayList<String> paramDescriptions = m_operationParamDescriptions.get(getSignature(op));
    String description = null;
    if (paramDescriptions != null && paramDescriptions.size() > sequence) {
      description = paramDescriptions.get(sequence);
    }
    if (description == null) {
      description = super.getDescription(op, param, sequence);
    }
    return description;
  }

  protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
    ArrayList<String> paramNames = m_operationParamNames.get(getSignature(op));
    String name = null;
    if (paramNames != null && paramNames.size() > sequence) {
      name = paramNames.get(sequence);
    }
    if (name == null) {
      name = super.getParameterName(op, param, sequence);
    }
    return name;
  }
  
  private void setDefaultValue(Object impl, Method method, String attributeName){
      final String loggerMethodName = "setDefaultValue";
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to setDefaultValue"); 
      s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "method = " + method.getName()
                                                               + ", attributeName = " + attributeName);
     try {
            // ~ add 'Object[] a = null' to avoid varargs warning ~
            Object[] avoidVarargsWarning = null;
            Object result = null;
            try
            {
              result = method.invoke(impl, avoidVarargsWarning);
            } catch (InvocationTargetException e) 
            {
                String primitiveType = "int,boolean,long";
                String output = "";
                if(primitiveType.contains(method.getGenericReturnType().toString()))
                  output = ", whose return Type is primitive type: " + method.getGenericReturnType().toString();        
              
                s_logger.log(
                    Level.WARNING,
                    String.format(
                        "MBean \"%s\" cannot access the following custom implementation method: %s" + output,
                        impl.getClass().getName(), method.toString()), e);
            }
            
        } catch (IllegalAccessException e) {
        } //catch (InvocationTargetException e) {
        s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to setDefaultValue"); 
  }
  
  protected String getDescription(MBeanInfo info){
      return m_description;
  }
  
  protected String getClassName(MBeanInfo info){
      return this.getClass().getCanonicalName();
  }
}
