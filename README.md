# elasticsearch-template

es5.2 Java API二次封装<br>

进行打包安装本地仓库后，在POM文件中引用elasticsearch-template    
Spring项目使用方式如下  
 * 将其resources目录下elasticsearch 配置拷贝至项目资源目录，修改elasticsearch.properties，elasticsearch-conf.properties中配置参数  
 * 程序中引入客户端模板调用，再开发即可  
     参考如下：
 <code>
    @Autowired
    protected ClientTemplate clientTemplate;

    /**
     * 插入或修改文档
     */
    public IndexResponse insertOrUpdate(final String indices, final IndexType indexType, final String keyId, final Map<String, Object> source) {
        return clientTemplate.executeGet((Client client)->{
            IndexRequest request = Requests.indexRequest(indices).type(indexType.name()).id(keyId).source(source);
            logger.debug("insertOrUpdate-" + request.toString());
            ActionFuture<IndexResponse> res = client.index(request);
            clientTemplate.refreshIndex();
            return res;
        });
    }
    
     /**
     * 根据ID获取文档
     */
    public GetResponse get(final String indices, final IndexType indexType, final String keyId) {
        return clientTemplate.executeGet((Client client)->{
            GetRequest request = Requests.getRequest(indices).type(indexType.name()).id(keyId);
            logger.debug("get-" + request.toString());
            return client.get(request);
        });
    }

    /**
     * 根据ID删除文档
     */
    public DeleteResponse delete(final String indices, final IndexType indexType, final String keyId) {
        return clientTemplate.executeGet((Client client)->{
            DeleteRequest request = Requests.deleteRequest(indices).type(indexType.name()).id(keyId);
            logger.debug("delete-" + request.toString());
            return client.delete(request);
        });
    }

    /**
     * 查询文档
     */
    public SearchResponse search(final String indices, final IndexType indexType,final SearchSourceBuilder ssb, final SearchType searchType) {
        return clientTemplate.executeGet((Client client)->{
            SearchRequest request = Requests.searchRequest(indices).preference("_primary_first").types(indexType.name()).searchType(searchType).source(ssb);
            logger.debug("search-" + request.toString());
            return client.search(request);
        });
    }

    /**
     * 创建索引文档类型及其Mapping
     * */
    public PutMappingResponse putMapping(final String indices,final IndexType indexType, final XContentBuilder mapping){
        return clientTemplate.executeGet((IndicesAdminClient indicesAdminClient)->{
            PutMappingRequest mappingRequest = Requests.putMappingRequest(indices).type(indexType.name()).source(mapping);
            return indicesAdminClient.putMapping(mappingRequest);
        });
    }

    /**
     * 批量插入数据
     * */
    public void batchInsertData(final String indices, final IndexType indexType, final Map<String, Map<String, Object>> sourceMap) {
        try {
            clientTemplate.executeGet((Client client)->{
                BulkRequest br = Requests.bulkRequest();
                for (String id : sourceMap.keySet()) {
                    br.add(Requests.indexRequest(indices).type(indexType.name()).id(id).source(sourceMap.get(id)));
                }
                ActionFuture<BulkResponse> result = client.bulk(br);
                br.requests().clear();
                return result;
            });
            logger.info("Insert into " + indices + ", type: " + indexType.name() + ", total: " + sourceMap.size() + ", success!");
        } catch (Exception e) {
            logger.error("Insert into " + indices + ", type: " + indexType.name() + ", total: " + sourceMap.size() + ", fail!", e);
        }
    }

  </code>


