package io.github.canjiemo.base.mymvc.privacy.jackson3;

import io.github.canjiemo.base.mymvc.privacy.Privacy;
import io.github.canjiemo.base.mymvc.privacy.PrivacyMaskUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

/** Jackson 3 隐私脱敏模块，由 Spring Boot 4 的默认 JsonMapper 自动注册。 */
public class PrivacyJackson3Module extends SimpleModule {

    public PrivacyJackson3Module() {
        super("PrivacyJackson3Module");
        setSerializerModifier(new PrivacySerializerModifier());
    }

    static class PrivacySerializerModifier extends ValueSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription.Supplier beanDesc,
                                                         List<BeanPropertyWriter> properties) {
            for (int i = 0; i < properties.size(); i++) {
                BeanPropertyWriter writer = properties.get(i);
                Privacy privacy = writer.getAnnotation(Privacy.class);
                if (privacy != null) {
                    properties.set(i, new PrivacyPropertyWriter(writer, privacy));
                }
            }
            return properties;
        }
    }

    static class PrivacyPropertyWriter extends BeanPropertyWriter {
        PrivacyPropertyWriter(BeanPropertyWriter base, Privacy privacy) {
            super(base);
            // 替换值序列化器，保留原属性 writer 的空值、过滤、视图及字段命名规则。
            // 同一个序列化器也用于数组形态及 @JsonUnwrapped，避免这些路径输出原始值。
            _serializer = new PrivacyValueSerializer(privacy, _serializer);
        }

        private PrivacyPropertyWriter(BeanPropertyWriter base, PropertyName name) {
            super(base, name);
        }

        @Override
        protected BeanPropertyWriter _new(PropertyName name) {
            return new PrivacyPropertyWriter(this, name);
        }
    }

    static class PrivacyValueSerializer extends ValueSerializer<Object> {

        private final Privacy privacy;
        private final ValueSerializer<Object> original;

        PrivacyValueSerializer(Privacy privacy, ValueSerializer<Object> original) {
            this.privacy = privacy;
            this.original = original;
        }

        private ValueSerializer<Object> delegate(Object value, SerializationContext context) {
            return original != null ? original : context.findValueSerializer(value.getClass());
        }

        @Override
        public void serialize(Object value, JsonGenerator generator, SerializationContext context) {
            if (value instanceof String text) {
                generator.writeString(PrivacyMaskUtils.mask(text, privacy));
            } else {
                delegate(value, context).serialize(value, generator, context);
            }
        }

        @Override
        public void serializeWithType(Object value, JsonGenerator generator, SerializationContext context,
                                      TypeSerializer typeSerializer) {
            if (value instanceof String text) {
                context.findValueSerializer(String.class).serializeWithType(
                        PrivacyMaskUtils.mask(text, privacy), generator, context, typeSerializer);
            } else {
                delegate(value, context).serializeWithType(value, generator, context, typeSerializer);
            }
        }

        @Override
        public boolean isEmpty(SerializationContext context, Object value) {
            return value == null || delegate(value, context).isEmpty(context, value);
        }
    }
}
