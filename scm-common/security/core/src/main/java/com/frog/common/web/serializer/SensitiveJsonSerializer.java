package com.frog.common.web.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.frog.common.web.annotation.Sensitive;
import com.frog.common.web.enums.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
/**
 * 敏感数据序列化器
 * 在JSON序列化时自动脱敏
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
        // 处理空值和空字符串
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }

        // 如果未启用脱敏，直接返回原值
        if (!enabled) {
            gen.writeString(value);
            return;
        }

        // 空指针检查：如果type为null，返回原值并记录警告
        if (type == null) {
            log.warn("SensitiveType is null, returning original value");
            gen.writeString(value);
            return;
        }

        try {
            // 执行脱敏
            String desensitizedValue = type.desensitize(value);
            gen.writeString(desensitizedValue);

            // 记录脱敏操作（仅在debug级别，避免性能影响）
            if (log.isDebugEnabled()) {
                log.debug("Desensitized field with type: {}", type);
            }
        } catch (Exception e) {
            // 脱敏失败时返回原值并记录错误
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

        // 获取字段上的@Sensitive注解
        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive == null) {
            return prov.findValueSerializer(property.getType(), property);
        }

        // 创建新的序列化器实例
        SensitiveJsonSerializer serializer = new SensitiveJsonSerializer();
        serializer.type = sensitive.type();
        serializer.enabled = sensitive.enabled();

        if (log.isDebugEnabled()) {
            log.debug("Created SensitiveJsonSerializer for field: {}, type: {}", property.getName(), sensitive.type());
        }

        return serializer;
    }
}
