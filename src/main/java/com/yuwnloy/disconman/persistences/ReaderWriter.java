package com.yuwnloy.disconman.persistences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 *
 * @author xiaoguang.gao
 *
 * @date 2016年2月1日 上午10:54:17
 **/
public class ReaderWriter {
	/**
	 * write one line by on line
	 * @param propFile
	 * @param valueList
	 */
	public static void writeToFileByLine(File propFile, List<String> valueList){
		 FileWriter fw = null;
		// BufferedWriter writer = null;
		 OutputStreamWriter writer = null;
		 try{
			 if(valueList!=null&&valueList.size()>0){
				 writer = new OutputStreamWriter(new FileOutputStream(propFile), "utf-8");
				 
				// fw=new FileWriter(propFile);
				// writer = new BufferedWriter(fw);
				 for(String value : valueList){System.out.println(value);
					 writer.write(value+"\r\n");
				 }
				 writer.flush();
			 }
			 
		 }catch(IOException e){
			 e.printStackTrace();
		 }finally{
			 try {
				 if(writer!=null)
					writer.close();					
				 if(fw!=null)
					 fw.close();
			 } catch (IOException e) {
					e.printStackTrace();
			 }
		 }
	 }
}
