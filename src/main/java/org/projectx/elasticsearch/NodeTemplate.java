package org.projectx.elasticsearch;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
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
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.projectx.index.IndexSearchEngine;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/** 
* @ClassName: NodeTemplate 
*
* @Description: 一个使用Elasticsearch 节点和客户端的模板，提供对Elasticsearch抽象的常用操作
*
* @author Huatao Jin
* 
* @date 2016年3月18日 上午11:34:04 
*  
*/
public class NodeTemplate implements IndexSearchEngine<SearchHit>, NodeOperations{

	private final Node node;
	
	private final String indexName;
	
	public NodeTemplate(final Node node) {
		Assert.notNull(node, "节点不能为空！");
		
		this.node = node;
		this.indexName = "";
	}
	
	public NodeTemplate(final Node node, final String indexName) {
		Assert.notNull(node, "节点不能为空！");
		Assert.notNull(indexName, "索引名不能为空！");
		
		this.node = node;
		this.indexName = indexName;
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
				return admin.refresh(Requests.refreshRequest(indexName));
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
	public void snapshotIndex(final String indexName) {
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ActionResponse> T executeGet(NodeCallback<T> callback) {
		final Client client = node.client();
		final IndicesAdminClient indicesAdmin = client.admin().indices();
		final ActionFuture<?> action = callback.execute(indicesAdmin);
		final T response = (T) action.actionGet();
		client.close();
		return response;
	}

	@Override
	public <T extends ActionResponse> T executeGet(ClusterCallback<T> callback) {
		final Client client = node.client();
		final ClusterAdminClient clusterAdmin = client.admin().cluster();
		final ActionFuture<?> action = callback.execute(clusterAdmin);
		final T response = (T) action.actionGet();
		client.close();
		return response;
	}

	@Override
	public <T extends ActionResponse> T executeGet(ClientCallback<T> callback) {
		final Client client = node.client();
		final ActionFuture<?> action = callback.execute(client);
		final T response = (T) action.actionGet();
		client.close();
		return response;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public <Q> List<SearchHit> search(final String field, final Q queryString,
			final int maxResults) {
		final QueryBuilder qb = 
				QueryBuilders.queryStringQuery(String.valueOf(queryString)).field(field);
		
		return searchInternal(qb, maxResults);
	}

	@Override
	public List<SearchHit> search(final String queryString, final int maxResults) {
		final QueryStringQueryBuilder query = 
				QueryBuilders.queryStringQuery(queryString);
		return searchInternal(query, maxResults);
	}
	
	private List<SearchHit> searchInternal(final QueryBuilder query, final int maxResults){
		final SearchResponse response = executeGet(new ClientCallback<SearchResponse>() {
			@Override
			public ActionFuture<SearchResponse> execute(final Client client) {
				final SearchRequest request = Requests.searchRequest().searchType(SearchType.DFS_QUERY_THEN_FETCH);
				final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
				sourceBuilder.query(query);
				sourceBuilder.size(maxResults);
				request.source(sourceBuilder);
				return client.search(request);
			}
		});
		
		return Arrays.asList(response.getHits().getHits());
	}

}
