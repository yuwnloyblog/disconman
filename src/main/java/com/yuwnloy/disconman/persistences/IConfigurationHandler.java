package com.yuwnloy.disconman.persistences;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author xiaoguang.gao
 *
 * @date 2016年2月1日 上午11:18:35
 **/
public interface IConfigurationHandler {
	public Collection<XmlConfiguration> parseFile(File xmlFile);

	public void writeToFile(File xmlFile, Collection<XmlConfiguration> configList);

	public String writeToString(Collection<XmlConfiguration> configList);

	public void backupXmlFile(File sourceFile, File targetFile) throws IOException;
}
