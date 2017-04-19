package org.projectx.elasticsearch;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:elasticsearch/applicationContext-elasticsearch.xml"})
public class ESClientTemplateIntegrationTest extends ESClientTemplateTestBase{
	
	//@Test
	public void addAliases() {
		addAliases("medicine_test","medicine_20160318");
	}
	
	//@Test
	public void refresh(){
		refreshIndex();
	}
	
	@Test
	public void index() throws IOException{
		String path = "src/main/resources/file/othello1-1.txt";
		final Collection<File> files = Collections.singletonList(new File(path));
		
		//int totalDocs = 0;
		for(final File file:files){
			final List<String> lines = FileUtils.readLines(file, "UTF-8");
			
			int lineNumber = 1;
			for(final String line: lines){
				indexLine(file, line, lineNumber);
				lineNumber++;
				//totalDocs++;
			}
		}
		
		refreshIndex();
		
		final List<SearchHit> hits = search("rownum", 1, 10);
		
		Assert.assertEquals("搜索结果数字不正确！", 1, hits.size());
	}
	
	private void indexLine(final File file, final String line, final int rownum) throws IOException{
		final String indexType = "log";
		final XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
		jsonBuilder.startObject();
		jsonBuilder.field("rownum", rownum);
		jsonBuilder.field("path", file.getAbsolutePath());
		jsonBuilder.field("modified", file.lastModified());
		jsonBuilder.field("contents", line);
		jsonBuilder.endObject();
		
		index(indexType, jsonBuilder);
	}
}
