package com.scmcloud.common.web.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
/**
 * 鏁忔劅鏁版嵁搴忓垪鍖栧櫒
 * 鍦↗SON搴忓垪鍖栨椂鑷姩鑴辨晱
 *
 * @author Deng
 * createData 2025/10/30 11:24
 * @version 1.0
 */
@Slf4j
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private SensitiveType type;
    private boolean enabled = true;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 澶勭悊绌哄€煎拰绌哄瓧绗︿覆
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }

        // 濡傛灉鏈惎鐢ㄨ劚鏁忥紝鐩存帴杩斿洖鍘燂拷
        if (!enabled) {
            gen.writeString(value);
            return;
        }

        // 绌烘寚閽堟鏌ワ細濡傛灉type涓簄ull锛岃繑鍥炲師鍊煎苟璁板綍璀﹀憡
        if (type == null) {
            log.warn("SensitiveType is null, returning original value");
            gen.writeString(value);
            return;
        }

        try {
            // 鎵ц鑴辨晱
            String desensitizedValue = type.desensitize(value);
            gen.writeString(desensitizedValue);

            // 璁板綍鑴辨晱鎿嶄綔锛堜粎鍦╠ebug绾у埆锛岄伩鍏嶆€ц兘褰卞搷锟?
            if (log.isDebugEnabled()) {
                log.debug("Desensitized field with type: {}", type);
            }
        } catch (Exception e) {
            // 鑴辨晱澶辫触鏃惰繑鍥炲師鍊煎苟璁板綍閿欒
            log.error("Failed to desensitize value with type: {}, error: {}", type, e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }

        // 鑾峰彇瀛楁涓婄殑@Sensitive娉ㄨВ
        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            return prov.findValueSerializer(property.getType(), property);
        }

        // 鍒涘缓鏂扮殑搴忓垪鍖栧櫒瀹炰緥
        SensitiveJsonSerializer serializer = new SensitiveJsonSerializer();
        serializer.type = sensitive.type();
        serializer.enabled = sensitive.enabled();

        if (log.isDebugEnabled()) {
            log.debug("Created SensitiveJsonSerializer for field: {}, type: {}", property.getName(), sensitive.type());
        }

        return serializer;
    }
}
