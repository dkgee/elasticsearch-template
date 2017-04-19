package org.projectx.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.loader.SettingsLoader;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.projectx.setting.PropertiesSettingsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** 
* @ClassName: ElasticsearchTransportClientFactoryBean 
*
* @Description: Elasticsearch TransportClient 客户端工厂类
*
* @author Huatao Jin
* 
* @date 2016年3月18日 下午3:03:06 
*  
*/
public class ElasticsearchTransportClientFactoryBean implements FactoryBean<Client>, InitializingBean, DisposableBean {

	private static Logger logger = LoggerFactory.getLogger(ElasticsearchTransportClientFactoryBean.class);
			
	private Client client;
	
	private List<Resource> configLocations;
	
	private Resource configLocation;
	
	private Map<String, String> settings;
	
	private Map<String, Integer> transportAddresses;
	
	public void setConfigLocations(List<Resource> configLocations) {
		this.configLocations = configLocations;
	}

	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public void setTransportAddresses(Map<String, Integer> transportAddresses) {
		this.transportAddresses = transportAddresses;
	}

	@Override
	public void destroy() throws Exception {
		client.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		internalCreateTransportClient();
	}
	
	private void internalCreateTransportClient(){
		Settings.Builder builder = Settings.builder();
		
		if(null != configLocation)
			internalLoadSettings(builder, configLocation);
		
		if(null != configLocations){
			for(final Resource location:configLocations){
				internalLoadSettings(builder, location);
			}
		}
		
		if(null != settings)
			builder.put(settings);
		
		client = new PreBuiltTransportClient(builder.build());
		
		if(transportAddresses != null){
			for(final Entry<String, Integer> address: transportAddresses.entrySet()){
				if(address.getKey() != null && !address.getValue().equals("")){
					logger.info("正在添加 transport IP地址:" + address.getKey() + " 端口:" + address.getValue());
					try {
						InetAddress e1 = InetAddress.getByName(address.getKey());
						((PreBuiltTransportClient)client).addTransportAddress(new InetSocketTransportAddress(e1, address.getValue()));
					} catch (UnknownHostException e) {
						logger.info("解析主机地址异常", e);
					}
				}
			}
		}
	}
	
	private void internalLoadSettings(Settings.Builder builder, Resource configLocation){
		try{
			final String fileName = configLocation.getFilename();
			if(logger.isInfoEnabled()){
				logger.info("正在从" + fileName +"加载配置文件...");
			}
			//builder.loadFromStream(fileName, configLocation.getInputStream());
			if(fileName.endsWith(".properties")){
				SettingsLoader settingsLoader = new PropertiesSettingsLoader();
				Map<String, String> loadedSettings =
						settingsLoader.load(Streams.copyToString(new InputStreamReader(configLocation.getInputStream(), StandardCharsets.UTF_8)));
				if(!loadedSettings.isEmpty()){
					builder.put(loadedSettings);
				}
			}
		}catch(final Exception e){
			throw new IllegalArgumentException("无法加载配置文件:" + configLocation.getDescription() , e);
		}
	}

	@Override
	public Client getObject() throws Exception {
		return client;
	}

	@Override
	public Class<TransportClient> getObjectType() {
		return TransportClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	
}
