package com.yuwnloy.disconman;

import javax.management.NotCompliantMBeanException;

import com.yuwnloy.disconman.annotations.Description;
import com.yuwnloy.disconman.annotations.Domain;
import com.yuwnloy.disconman.annotations.Group;
import com.yuwnloy.disconman.annotations.IntDefaultValue;
import com.yuwnloy.disconman.annotations.StringDefaultValue;

/**
 * Unit test for simple App.
 */
public class AppTest {
    public static void main(String[] args){
    	try {
			ConfigManager.getInstance().createMBean(Xiao.class);
			ConfigManager.getInstance().createMBean(MethodTest.class, new MethodTest(){

				@Override
				public void reforceRefresh(long appId) {
					// TODO Auto-generated method stub
					System.out.println("Test:"+System.currentTimeMillis()+","+appId);
				}});
			Xiao x = ConfigManager.getInstance().getMBean(Xiao.class);
			while(true){
			System.out.println(x.getName()+","+x.getAge());
			Thread.sleep(1000);
			}
		} catch (NotCompliantMBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    @Domain("rcloud")
    @Group("group")
    @Description("test mbean")
    public static interface Xiao{
    	public void setName(String name);
    	@StringDefaultValue("xxxx")
    	@Description("name")
    	public String getName();
    	
    	public void setAge(int age);
    	@IntDefaultValue(10)
    	@Description("age")
    	public int getAge();
    }
    
    @Domain("rcloud")
    @Description("test method")
    public static interface MethodTest{
    	@Description("test method fsfs")
    	public void reforceRefresh(long appId);
    }
}
