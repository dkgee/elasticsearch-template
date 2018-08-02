package org.projectx.elasticsearch;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.projectx.index.IndexSearchEngine;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

public class ClientTemplate implements IndexSearchEngine<SearchHit>, NodeOperations{
	
	private Client client;
	
	private final String indexName;
	
	public ClientTemplate(final Client client) {
		Assert.notNull(client, "客户端不允许为空");
		
		this.client = client;
		this.indexName = "";
	}
	
	public ClientTemplate(final Client client, final String indexName) {
		Assert.notNull(client, "客户端不允许为空");
		Assert.notNull(indexName, "节点名不允许为空");
		
		this.client = client;
		this.indexName = indexName;
	}
	
	protected void createIndex(final String indexType, final Settings settings, final XContentBuilder mapping){
		createIndex(indexType, indexType, settings, mapping);
	}
	
	protected void createIndex(final String indexName,final String indexType, final Settings settings, final XContentBuilder mapping){
		executeGet(new NodeCallback<CreateIndexResponse>() {
			@Override
			public ActionFuture<CreateIndexResponse> execute(
					IndicesAdminClient client) {
				CreateIndexRequest request = Requests.createIndexRequest(indexName);
				
				if(settings != null ){
					request.settings(settings);
				}

				if(indexType != null && mapping != null){
					request.mapping(indexType, mapping);
				}

				return client.create(request);
			}
		});
	}

	@Override
	public <Q> List<SearchHit> search(String field, Q queryString,
			int maxResults) {
		final QueryBuilder qb = 
				QueryBuilders.queryStringQuery(String.valueOf(queryString)).field(field);
		
		return searchInternal(qb, maxResults);
	}

	@Override
	public List<SearchHit> search(String queryString, int maxResults) {
		final QueryStringQueryBuilder query = 
				QueryBuilders.queryStringQuery(queryString);
		return searchInternal(query, maxResults);
	}

	@Override
	public boolean indexExists() {
		return indexExists(indexName);
	}

	@Override
	public boolean indexExists(String indexName) {
		executeGet(new ClusterCallback<ClusterHealthResponse>() {
			@Override
			public ActionFuture<ClusterHealthResponse> execute(
					final ClusterAdminClient admin) {
				return admin.health(Requests.clusterHealthRequest().waitForStatus(ClusterHealthStatus.YELLOW));
			}
		});
		
		final IndicesStatsResponse response = executeGet(new NodeCallback<IndicesStatsResponse>(){
			@Override
			public ActionFuture<IndicesStatsResponse> execute(
					final IndicesAdminClient admin) {
				return admin.stats(new IndicesStatsRequest());
			}
		});
		
		return response.getIndices().get(indexName) != null;
	}

	@Override
	public void deleteIndex() {
		deleteIndex(indexName);
	}

	@Override
	public void deleteIndex(final String indexName) {
		executeGet(new NodeCallback<DeleteIndexResponse>() {

			@Override
			public ActionFuture<DeleteIndexResponse> execute(
					final IndicesAdminClient admin) {
				return admin.delete(Requests.deleteIndexRequest(indexName));
			}
		});
	}

	@Override
	public void refreshIndex() {
		refreshIndex(indexName);
	}

	@Override
	public void refreshIndex(final String indexName) {
		executeGet(new NodeCallback<RefreshResponse>() {

			@Override
			public ActionFuture<RefreshResponse> execute(
					IndicesAdminClient admin) {
				return admin.refresh(Requests.refreshRequest(new String[]{ indexName }));
			}
		});
	}

	@Override
	public void closeIndex() {
		closeIndex(indexName);
	}

	@Override
	public void closeIndex(final String indexName) {
		executeGet(new NodeCallback<CloseIndexResponse>() {
			@Override
			public ActionFuture<CloseIndexResponse> execute(
					IndicesAdminClient admin) {
				return admin.close(Requests.closeIndexRequest(indexName));
			}
		});
	}

	@Override
	public void flushIndex() {
		flushIndex(indexName);
	}

	@Override
	public void flushIndex(final String indexName) {
		executeGet(new NodeCallback<FlushResponse>() {
			@Override
			public ActionFuture<FlushResponse> execute(IndicesAdminClient admin) {
				return admin.flush(Requests.flushRequest(indexName));
			}
		});
	}

	@Override
	@Deprecated
	public void snapshotIndex() {
		snapshotIndex(indexName);
	}

	@Override
	@Deprecated
	public void snapshotIndex(String indexName) {
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ActionResponse> T executeGet(NodeCallback<T> callback) {
		IndicesAdminClient indicesAdmin = this.client.admin().indices();
		ActionFuture<T> action = callback.execute(indicesAdmin);
		ActionResponse response = action.actionGet();
		return (T) response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ActionResponse> T executeGet(ClusterCallback<T> callback) {
		ClusterAdminClient clusterAdmin = this.client.admin().cluster();
		ActionFuture<T> action = callback.execute(clusterAdmin);
		ActionResponse response = action.actionGet();
		
		return (T) response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ActionResponse> T executeGet(ClientCallback<T> callback) {
		ActionFuture<T> action = callback.execute(this.client);
		ActionResponse response = action.actionGet();
		
		return (T) response;
	}
	
	private List<SearchHit> searchInternal(final QueryBuilder query, final int maxResults){
		final SearchResponse response = executeGet(new ClientCallback<SearchResponse>() {
			@Override
			public ActionFuture<SearchResponse> execute(final Client client) {
				final SearchRequest request = Requests.searchRequest().searchType(SearchType.DFS_QUERY_THEN_FETCH);
				final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
				sourceBuilder.query(query).size(maxResults);
				request.source(sourceBuilder);
				return client.search(request);
			}
		});
		
		return Arrays.asList(response.getHits().getHits());
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

}
