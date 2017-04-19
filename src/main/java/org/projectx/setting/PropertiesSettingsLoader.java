package org.projectx.setting;

import org.apache.lucene.util.IOUtils;
import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.settings.loader.SettingsLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Description:属性文件加载器
 * User： JinHuaTao
 * Date：2017/4/13
 * Time: 18:29
 */

public class PropertiesSettingsLoader implements SettingsLoader {

    @Override
    public Map<String, String> load(String source) throws IOException {
        Properties props = new Properties();
        FastStringReader reader = new FastStringReader(source);
        try {
            props.load(reader);
            Map<String, String> result = new HashMap<String, String>();
            for (Map.Entry entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
            return result;
        } finally {
            IOUtils.closeWhileHandlingException(reader);
        }
    }

    @Override
    public Map<String, String> load(byte[] source) throws IOException {
        return null;
    }
}
