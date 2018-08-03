package org.projectx.elasticsearch;


import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasAction;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;

public class ESClientTemplateTestBase {
	
	private static Logger logger = LoggerFactory.getLogger(ESClientTemplateTestBase.class);
	
	@Resource
	private ClientTemplate clientTemplate;
	
	
	@Before
	public void before() {
		try{
			if(!clientTemplate.indexExists()){
				logger.warn("索引不存在！");
				clientTemplate.executeGet(new NodeCallback<CreateIndexResponse>() {
					@Override
					public ActionFuture<CreateIndexResponse> execute(
							IndicesAdminClient admin) {
						return admin.create(Requests.createIndexRequest(clientTemplate.getIndexName()));
					}
				});
				logger.warn("索引创建成功！");
			}
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	@After
	public void after(){
//		clientTemplate.deleteIndex();
//		logger.info("删除索引成功！");
	}
	
	protected void refreshIndex(){
		clientTemplate.refreshIndex();
		logger.info("刷新索引成功！");
	}
	
	protected void flushIndex() {
		clientTemplate.flushIndex();
		logger.info("index flush sucessful!");
	}
	
	protected void addAliases(final String indexName, final String indexAlias){
		clientTemplate.executeGet(new NodeCallback<IndicesAliasesResponse>() {
			@Override
			public ActionFuture<IndicesAliasesResponse> execute(
					IndicesAdminClient admin) {
				IndicesAliasesRequest request = Requests.indexAliasesRequest();
//				request.addAlias(indexAlias, indexName);
				//request.addAliasAction();

				//添加索引别名
				IndicesAliasesRequest.AliasActions aliasActions = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD);
				aliasActions.index(indexName);
				aliasActions.alias(indexAlias);
				request.addAliasAction(aliasActions);
				return admin.aliases(request);
			}
		});
		
		logger.info("添加别名成功，索引名:" + indexName + ",别名:" + indexAlias);
	}
	
	protected IndexResponse index(final String indexType, final XContentBuilder content){
		final IndexResponse response = clientTemplate.executeGet(new ClientCallback<IndexResponse>() {

			@Override
			public ActionFuture<IndexResponse> execute(final Client client) {
				final IndexRequest request = Requests.indexRequest(clientTemplate.getIndexName())
						.type(indexType).source(content);
				return client.index(request);
			}
		});
		
		Assert.assertNotNull("响应为空", response);
		
		return response;
	}
	
	protected <Q> List<SearchHit> search(final String field, final Q value, final int maxResults) {
		return clientTemplate.search(field, value, maxResults);
	}
}
