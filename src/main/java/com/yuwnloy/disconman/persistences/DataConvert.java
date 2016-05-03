package com.yuwnloy.disconman.persistences;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public class DataConvert {
	/**
	   * 
	   * @param type
	   * @param value
	   * @return
	   */
	  public static Object convert(Class<?> type,Object value){
		if (type == int.class || type == Integer.class) {
	      value = Integer.parseInt(value.toString());
	    } else if (type == boolean.class || type == Boolean.class) {
	      value = Boolean.parseBoolean(value.toString());
	    } else if (type == long.class || type == Long.class) {
	      value = Long.parseLong(value.toString());
	    }
		return value;
	  }
}
