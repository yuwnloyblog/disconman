package com.yuwnloy.disconman.persistences;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
 */
public interface IConfigurationHandler {
	public Collection<XmlConfiguration> parseFile(File xmlFile);

	public void writeToFile(File xmlFile, Collection<XmlConfiguration> configList);

	public String writeToString(Collection<XmlConfiguration> configList);

	public void backupXmlFile(File sourceFile, File targetFile) throws IOException;
}
