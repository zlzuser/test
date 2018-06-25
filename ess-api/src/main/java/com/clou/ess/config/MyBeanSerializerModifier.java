package com.clou.ess.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Jackson NULL值 处理
 *
 * @author 谭知林
 */
public class MyBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter beanProperty : beanProperties) {
            // 判断字段的类型，如果是array，list，set则注册NullArray
            if (isArrayType(beanProperty)) {
                //注册一个自己的NullArraySerializer
                beanProperty.assignNullSerializer(this.defaultNullArrayJsonSerializer());
            } else if (isMapLikeType(beanProperty)) {
                beanProperty.assignNullSerializer(this.defaultNullMapJsonSerializer());
            } else if (isNumberType(beanProperty)) {
                if (isFloatType(beanProperty) && ObjectUtils.isEmpty(beanProperty.getAnnotation(JsonSerialize.class))) {
                    beanProperty.assignSerializer(this.defaultNumberJsonSerializer());
                }
                beanProperty.assignNullSerializer(this.defaultNullNumberJsonSerializer());
            } else {
                beanProperty.assignNullSerializer(this.defaultNullJsonSerializer());
            }
        }
        return beanProperties;
    }

    /**
     * 判断是否是数组或者集合
     *
     * @param writer
     * @return
     */
    private boolean isArrayType(BeanPropertyWriter writer) {
        JavaType javaType = writer.getType();
        return javaType.isArrayType() || javaType.isCollectionLikeType();
    }

    /**
     * 判断是Map类型
     *
     * @param writer
     * @return
     */
    private boolean isMapLikeType(BeanPropertyWriter writer) {
        JavaType javaType = writer.getType();
        return javaType.isMapLikeType();
    }

    /**
     * 判断是否是Number类型
     *
     * @param writer
     * @return
     */
    private boolean isNumberType(BeanPropertyWriter writer) {
        JavaType javaType = writer.getType();
        return javaType.isTypeOrSubTypeOf(Number.class) || javaType.isTypeOrSubTypeOf(short.class) || javaType.isTypeOrSubTypeOf(int.class) || javaType.isTypeOrSubTypeOf(long.class) || javaType.isTypeOrSubTypeOf(float.class) || javaType.isTypeOrSubTypeOf(double.class);
    }

    /**
     * 判断是否是浮点类型
     *
     * @param writer
     * @return
     */
    private boolean isFloatType(BeanPropertyWriter writer) {
        JavaType javaType = writer.getType();
        return javaType.isTypeOrSubTypeOf(Float.class) || javaType.isTypeOrSubTypeOf(float.class) || javaType.isTypeOrSubTypeOf(Double.class) || javaType.isTypeOrSubTypeOf(double.class) || javaType.isTypeOrSubTypeOf(BigDecimal.class);
    }

    private JsonSerializer<Object> defaultNullArrayJsonSerializer() {
        return new MyNullArrayJsonSerializer();
    }

    private JsonSerializer<Object> defaultNullMapJsonSerializer() {
        return new MyNullMapJsonSerializer();
    }

    private JsonSerializer<Object> defaultNullNumberJsonSerializer() {
        return new MyNullNumberJsonSerializer();
    }

    private JsonSerializer<Object> defaultNumberJsonSerializer() {
        return new MyNumberJsonSerializer();
    }

    private JsonSerializer<Object> defaultNullJsonSerializer() {
        return new MyNullJsonSerializer();
    }

    private class MyNullArrayJsonSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartArray();
            gen.writeEndArray();
        }
    }

    private class MyNullMapJsonSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }

    private class MyNullNumberJsonSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeNumber(0);
        }
    }

    private class MyNullJsonSerializer extends JsonSerializer<Object> {
        /**
         * Method that can be called to ask implementation to serialize
         * values of type this serializer handles.
         *
         * @param value       Value to serialize; can <b>not</b> be null.
         * @param gen         Generator used to output resulting Json content
         * @param serializers Provider that can be used to get serializers for
         */
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("");
        }
    }
}
