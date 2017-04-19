package org.projectx.elasticsearch;

import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.loader.SettingsLoader;
import org.elasticsearch.node.Node;
import org.projectx.setting.PropertiesSettingsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 
* @ClassName: ElasticsearchNodeFactoryBean 
*
* @Description: Elasticsearch节点工厂
*
* @author Huatao Jin
* 
* @date 2016年3月18日 下午2:33:11 
*  
*/
public class ElasticsearchNodeFactoryBean implements FactoryBean<Node>, InitializingBean, DisposableBean{

	private static Logger logger = LoggerFactory.getLogger(ElasticsearchNodeFactoryBean.class);
	
	private List<Resource> configLocations;
	
	private Resource configLocation;
	
	private Map<String, String> settings;
	
	private Node node;
	
	public void setConfigLocations(final List<Resource> configLocations) {
		this.configLocations = configLocations;
	}

	public void setConfigLocation(final Resource configLocation) {
		this.configLocation = configLocation;
	}

	public void setSettings(final Map<String, String> settings) {
		this.settings = settings;
	}

	@Override
	public void destroy() throws Exception {
		try{
			node.close();
		}catch(final Exception e){
			logger.error("关闭Elasticsearch节点异常！", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		internalCreateNode();
	}
	
	private void internalCreateNode(){
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
		
		node = new Node(builder.build());
	}
	
	private void internalLoadSettings(final Settings.Builder builder, final Resource configLocation){
		try{
			final String fileName = configLocation.getFilename();
			if(logger.isInfoEnabled()){
				logger.info("正在创建节点，从" + fileName +"加载配置文件...");
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
			throw new IllegalArgumentException("创建节点异常，无法加载配置文件:" + configLocation.getDescription() , e);
		}
	}

	@Override
	public Node getObject() throws Exception {
		return node;
	}

	@Override
	public Class<Node> getObjectType() {
		return Node.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
