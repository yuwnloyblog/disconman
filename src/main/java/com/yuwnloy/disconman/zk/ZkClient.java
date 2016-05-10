package com.yuwnloy.disconman.zk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.yuwnloy.disconman.ConfigBeanDetail;
import com.yuwnloy.disconman.persistences.AttributeDetail;
import com.yuwnloy.disconman.utils.SafeEncoder;

/**
 * 
 * @author xiaoguang.gao
 *
 * @date May 6, 2016
 */
public class ZkClient {
	public final static String RootName = "DisconmainRoot";
	private static ZkClient instance;

	public static ZkClient getInstance() {
		if (instance == null) {
			instance = new ZkClient();
		}
		return instance;
	}

	private static ExecutorService threadPool = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

	private CuratorFramework client;

	private ZkClient() {
		this.connect();
	}

	public void connect() {
		client = CuratorFrameworkFactory.builder()
					.connectString("127.0.0.1:2182")
					.sessionTimeoutMs(5000)
					.connectionTimeoutMs(3000)
					.retryPolicy(new ExponentialBackoffRetry(1000, 3))
					.build();
		client.start();
	}
	
	public void handleBean(ConfigBeanDetail beanDetail){
		if(beanDetail!=null&&beanDetail.getMbeanInfo()!=null){
			String domain = beanDetail.getMbeanInfo().domain;
			String group = beanDetail.getMbeanInfo().group;
			String name = beanDetail.getMbeanInfo().name;
			for(String attName : beanDetail.getAttDetailMap().keySet()){
				AttributeDetail attDetail = beanDetail.getAttDetailMap().get(attName);
				if(attDetail!=null)
					this.handleAttDetail(domain, group, name, attName, attDetail);
			}
		}
	}
	
	public void setAttValue(String domain, String group,String beanName, String attName, Object attValue){
		String path = "/" + RootName + "/" + domain + "/" + group + "/" + beanName + "/" + attName;
		try {
			client.setData().forPath(path,SafeEncoder.encode(attValue));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param attDetail
	 */
	public void handleAttDetail(String domain, String group, String beanName, String attName,
			final AttributeDetail attDetail) {
		String path = "/" + RootName + "/" + domain + "/" + group + "/" + beanName + "/" + attName;
		boolean flag = true;
		try {
			client.create().creatingParentsIfNeeded().forPath(path, SafeEncoder.encode(attDetail.getDefaultValue()));
		} catch (org.apache.zookeeper.KeeperException.NodeExistsException e) {
			// the node have existed.
			try {
				byte[] nodeData = client.getData().forPath(path);
				String strNodeData = new String(nodeData,"UTF-8");
				attDetail.setValueWithTypeCast(strNodeData);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}catch(org.apache.curator.CuratorConnectionLossException e){
			flag = false;
			e.printStackTrace();
		}catch (Exception e) {
			flag = false;
			e.printStackTrace();
		}
		if (flag) {
			try {
				/**
				 * 监听数据节点的变化情况
				 */
				final NodeCache nodeCache = new NodeCache(client, path, false);

				nodeCache.start(true);

				nodeCache.getListenable().addListener(new NodeCacheListener() {
					@Override
					public void nodeChanged() throws Exception {
						byte[] nodeData = nodeCache.getCurrentData().getData();
						String strNodeData = new String(nodeData,"UTF-8");
						System.out.println(
								"Node data is changed, new data: " + strNodeData);
						attDetail.setValueWithTypeCast(strNodeData);
					}
				}, threadPool);
			} catch (Exception e) {

			}
		}
	}
	
	public void destroy(){
		 threadPool.shutdown();
		 if(instance!=null){
			 instance.client.close();
			 instance = null;
		 }
	}

	public static void main(String[] args) {
	}
}
