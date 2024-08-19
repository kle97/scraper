package io.playground.scraper.util;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.javaprop.util.Markers;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class JacksonUtil {

    // ObjectMapper should be initialized once. Jackson's functionalities should be used via
    // lightweight ObjectReader and ObjectWriter which are derived from ObjectMapper.
    // The extra configuration is for JSON5 format support. See: https://stackoverflow.com/a/68312228/16422535
    private static final ObjectMapper objectMapper = JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .withConfigOverride(Collection.class, o -> o.setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY)))
            .withConfigOverride(Object[].class, o -> o.setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY)))
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .build();

    public static final Character PROPERTIES_PATH_SEPARATOR_ESCAPE_CHAR = '$';

    private static final JavaPropsMapper javaPropsMapper = JavaPropsMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .withConfigOverride(Collection.class, o -> o.setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY)))
            .withConfigOverride(Object[].class, o -> o.setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY)))
            .addModule(new SimpleModule().addKeySerializer(String.class, new EscapeDotInJsonKeySerializer()))
            .build();
    private static final JavaPropsSchema propsSchema = JavaPropsSchema.emptySchema()
                                                                      .withPathSeparatorEscapeChar(PROPERTIES_PATH_SEPARATOR_ESCAPE_CHAR)
                                                                      .withWriteIndexUsingMarkers(true)
                                                                      .withFirstArrayOffset(0)
                                                                      .withIndexMarker(Markers.create("[", "]"));

    static class EscapeDotInJsonKeySerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            String key = value.replace(".", PROPERTIES_PATH_SEPARATOR_ESCAPE_CHAR + ".");
            gen.writeFieldName(key);
        }
    }

    private JacksonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper.copy();
    }

    public static ObjectReader readerFor(Class<?> type) {
        return objectMapper.readerFor(type);
    }

    public static ObjectReader readerFor(TypeReference<?> typeReference) {
        return objectMapper.readerFor(typeReference);
    }

    public static ObjectReader readerFor(JavaType type) {
        return objectMapper.readerFor(type);
    }

    public static ObjectWriter writerFor(Class<?> type) {
        return objectMapper.writerFor(type);
    }

    public static ObjectWriter writerFor(TypeReference<?> typeReference) {
        return objectMapper.writerFor(typeReference);
    }

    public static ObjectWriter writerFor(JavaType type) {
        return objectMapper.writerFor(type);
    }

    public static ObjectWriter writerWithDefaultPrettyPrinter() {
        return objectMapper.writerWithDefaultPrettyPrinter();
    }

    public static ObjectWriter writer() {
        return objectMapper.writer();
    }

    public static String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] writeValueAsBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String content, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse json file into an object of target class.
     * Example: MyCustomClass object = JacksonUtil.parseJsonFileAsMap(filePath, MyCustomClass.class);
     *
     * @param filePath json file's path
     * @param clazz target class to parse
     * @return object of the target class; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, Class<T> clazz) {
        return parseJsonFileAs(filePath, clazz, false);
    }

    /**
     * Parse json file into an object of target class.
     * Example: MyCustomClass object = JacksonUtil.parseJsonFileAsMap(filePath, MyCustomClass.class, true);
     *
     * @param filePath json file's path
     * @param clazz target class to parse
     * @param overridable whether to override json fields with system properties
     * @return object of the target class; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, Class<T> clazz, boolean overridable) {
        return parseJsonFileAs(filePath, clazz, overridable, false);
    }

    /**
     * Parse json file into an object of target class.
     * Example: MyCustomClass object = JacksonUtil.parseJsonFileAsMap(filePath, MyCustomClass.class, true);
     *
     * @param filePath    json file's path
     * @param clazz       target class to parse
     * @param overridable whether to override json fields with system properties
     * @param extendable  whether to extend json fields with system properties
     * @return object of the target class; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, Class<T> clazz, boolean overridable, boolean extendable) {
        try (InputStream inputStream = getFileAsStream(filePath)) {
            T parsedObject = readerFor(clazz).readValue(inputStream);
            if (overridable) {
                Properties properties = overrideObjectWithSystemProperties(parsedObject, extendable);
                return javaPropsMapper.readPropertiesAs(properties, propsSchema, clazz);
            } else {
                return parsedObject;
            }
        } catch (ValueInstantiationException | IllegalArgumentException | InvalidDefinitionException e) {
            log.error("Json file '{}' could not be deserialized!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.error("Json file is not found for path '{}'!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse json file into an object of target TypeReference.
     * Example: MyCustomClass object = JacksonUtil.parseJsonFileAsMap(filePath, new TypeReference<MyCustomClass>(){});
     *
     * @param filePath json file's path
     * @param typeReference target TypeReference to parse
     * @return object of the target typeReference; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, TypeReference<T> typeReference) {
        try (InputStream inputStream = getFileAsStream(filePath)) {
            return readerFor(typeReference).readValue(inputStream);
        } catch (ValueInstantiationException | IllegalArgumentException | InvalidDefinitionException e) {
            log.error("Json file '{}' could not be deserialized!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.error("Json file is not found for path '{}'!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse json file into an object of target JavaType.
     * Example:
     * MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, MyCustomClass);
     * Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, mapType);
     *
     * @param filePath json file's path
     * @param javaType target JavaType to parse
     * @return object of the target javaType; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, JavaType javaType) {
        return parseJsonFileAs(filePath, javaType, false);
    }

    /**
     * Parse json file into an object of target JavaType.
     * Example:
     * MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, MyCustomClass);
     * Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, mapType, true);
     *
     * @param filePath    json file's path
     * @param javaType    target JavaType to parse
     * @param overridable whether to override json fields with system properties
     * @return object of the target javaType; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, JavaType javaType, boolean overridable) {
        return parseJsonFileAs(filePath, javaType, overridable, false);
    }

    /**
     * Parse json file into an object of target JavaType.
     * Example:
     * MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, MyCustomClass);
     * Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, mapType, true);
     *
     * @param filePath    json file's path
     * @param javaType    target JavaType to parse
     * @param overridable whether to override json fields with system properties
     * @param extendable  whether to extend json fields with system properties
     * @return object of the target javaType; null if file is not found.
     */
    public static <T> T parseJsonFileAs(String filePath, JavaType javaType, boolean overridable, boolean extendable) {
        try (InputStream inputStream = getFileAsStream(filePath)) {
            T parsedObject = readerFor(javaType).readValue(inputStream);
            if (overridable) {
                Properties properties = overrideObjectWithSystemProperties(parsedObject, extendable);
                return javaPropsMapper.readPropertiesAs(properties, propsSchema, javaType);
            } else {
                return parsedObject;
            }
        } catch (ValueInstantiationException | IllegalArgumentException | InvalidDefinitionException e) {
            log.error("Json file '{}' could not be deserialized!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.error("Json file is not found for path '{}'!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse json file into a map Map<KEY_CLASS, VALUE_CLASS>.
     * Example: Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, String.class, MyCustomClass.class);
     *
     * @param filePath   json file's path
     * @param keyClass   class of key
     * @param valueClass class of value
     * @return target map; null if file is not found.
     */
    public static <K, V> Map<K, V> parseJsonFileAsMap(String filePath, Class<K> keyClass, Class<V> valueClass) {
        return parseJsonFileAsMap(filePath, keyClass, valueClass, false);
    }

    /**
     * Parse json file into a map Map<KEY_CLASS, VALUE_CLASS>.
     * Example: Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, String.class, MyCustomClass.class, true);
     *
     * @param filePath    json file's path
     * @param keyClass    class of key
     * @param valueClass  class of value
     * @param overridable whether to override json fields with system properties
     * @return target map; null if file is not found.
     */
    public static <K, V> Map<K, V> parseJsonFileAsMap(String filePath, Class<K> keyClass, Class<V> valueClass, boolean overridable) {
        return parseJsonFileAsMap(filePath, keyClass, valueClass, overridable, false);
    }

    /**
     * Parse json file into a map Map<KEY_CLASS, VALUE_CLASS>.
     * Example: Map<String, MyCustomClass> map = JacksonUtil.parseJsonFileAsMap(filePath, String.class, MyCustomClass.class, true);
     *
     * @param filePath    json file's path
     * @param keyClass    class of key
     * @param valueClass  class of value
     * @param overridable whether to override json fields with system properties
     * @param extendable  whether to extend json fields with system properties
     * @return target map; null if file is not found.
     */
    public static <K, V> Map<K, V> parseJsonFileAsMap(String filePath, Class<K> keyClass, Class<V> valueClass, boolean overridable, boolean extendable) {
        MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, keyClass, valueClass);
        return parseJsonFileAs(filePath, mapType, overridable, extendable);
    }

    /**
     * Parse json file into Java properties object.
     * Example:
     * Properties properties = JacksonUtil.parseJsonFileAsProperties(filePath);
     *
     * @param filePath json file's path
     * @return object of target javaType; null if file is not found.
     */
    public static Properties parseJsonFileAsProperties(String filePath) {
        return parseJsonFileAsProperties(filePath, false);
    }

    /**
     * Parse json file into Java properties object.
     * Example:
     * Properties properties = JacksonUtil.parseJsonFileAsProperties(filePath, true);
     *
     * @param filePath json file's path
     * @param overridable whether to override json fields with system properties
     * @return object of target javaType; null if file is not found.
     */
    public static Properties parseJsonFileAsProperties(String filePath, boolean overridable) {
        return parseJsonFileAsProperties(filePath, overridable, false);
    }

    /**
     * Parse json file into Java properties object.
     * Example:
     * Properties properties = JacksonUtil.parseJsonFileAsProperties(filePath, true);
     *
     * @param filePath    json file's path
     * @param overridable whether to override json fields with system properties
     * @param extendable  whether to extend json fields with system properties
     * @return object of target javaType; null if file is not found.
     */
    public static Properties parseJsonFileAsProperties(String filePath, boolean overridable, boolean extendable) {
        try (InputStream inputStream = getFileAsStream(filePath)) {
            Object parsedObject = readerFor(Object.class).readValue(inputStream);
            if (overridable) {
                return overrideObjectWithSystemProperties(parsedObject, extendable);
            } else {
                return javaPropsMapper.writeValueAsProperties(parsedObject, propsSchema);
            }
        } catch (ValueInstantiationException | IllegalArgumentException | InvalidDefinitionException e) {
            log.debug("Json file '{}' could not be deserialized!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.debug("Json file is not found for path '{}'!", filePath);
            log.trace(e.getMessage(), e);
            return null;
        }
    }


    /**
     * convert Java object to Java Properties and override properties' values with System Properties
     * @param parsedObject parsed object of a json file
     * @return properties from parsed object
     */
    private static Properties overrideObjectWithSystemProperties(Object parsedObject, boolean extendable) {
        try {
            Properties properties = javaPropsMapper.writeValueAsProperties(parsedObject, propsSchema);

            for (String systemPropertiesKey : System.getProperties().stringPropertyNames()) {
                if (properties.containsKey(systemPropertiesKey)) {
                    String systemPropertiesValue = System.getProperty(systemPropertiesKey);
                    log.info("Overriding property with system properties: {}={}", systemPropertiesKey, systemPropertiesValue);
                    properties.setProperty(systemPropertiesKey, systemPropertiesValue);
                } else if (extendable || systemPropertiesKey.contains("desiredCapabilities")) {
                    if (systemPropertiesKey.contains(".")) {
                        // extend child object properties
                        String prefix = systemPropertiesKey.substring(0, systemPropertiesKey.lastIndexOf(".") + 1);
                        if (properties.stringPropertyNames().stream().anyMatch(key -> key.startsWith(prefix))) {
                            String systemPropertiesValue = System.getProperty(systemPropertiesKey);
                            log.info("Setting property with system properties: {}={}", systemPropertiesKey, systemPropertiesValue);
                            properties.setProperty(systemPropertiesKey, systemPropertiesValue);
                        }
                    } else if (systemPropertiesKey.contains("[") && systemPropertiesKey.contains("]")) {
                        // extend array properties
                        String prefix = systemPropertiesKey.substring(0, systemPropertiesKey.lastIndexOf("[") + 1);
                        if (properties.stringPropertyNames().stream().anyMatch(key -> key.startsWith(prefix))) {
                            String systemPropertiesValue = System.getProperty(systemPropertiesKey);
                            log.info("Setting property with system properties: {}={}", systemPropertiesKey, systemPropertiesValue);
                            properties.setProperty(systemPropertiesKey, systemPropertiesValue);
                        }
                    }
                }
            }

            return properties;
        } catch (IOException | IllegalArgumentException e) {
            log.debug(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get an input stream from a target file
     * Looking for file by absolute path first, then by resources classpath
     * @param filePath file's path
     * @return an input stream of opening file
     * @throws IOException if file is not found
     */
    public static InputStream getFileAsStream(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IOException(String.format("Cannot find file %s", filePath));
        }

        File file = new File(filePath);
        if (file.exists()) {
            return new FileInputStream(file);
        } else if (!filePath.isBlank()) {
            return ClassLoader.getSystemResourceAsStream(filePath);
        } else {
            throw new IOException(String.format("Cannot find file %s", filePath));
        }
    }

    @SuppressWarnings("unchecked")
    public static int getAsInteger(Map<String, Object> map, String... keys) {
        try {
            for (int i = 0; i < keys.length; i++) {
                if (i < keys.length - 1) {
                    map = (Map<String, Object>) map.get(keys[i]);
                } else {
                    return (Integer) map.get(keys[i]);
                }
            }
        } catch (Exception ignored) {
        }
        return Integer.MIN_VALUE;
    }

    @SuppressWarnings("unchecked")
    public static String getAsString(Map<String, Object> map, String... keys) {
        try {
            for (int i = 0; i < keys.length; i++) {
                if (i < keys.length - 1) {
                    map = (Map<String, Object>) map.get(keys[i]);
                } else {
                    return (String) map.get(keys[i]);                    
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static double getAsDouble(Map<String, Object> map, String... keys) {
        try {
            for (int i = 0; i < keys.length; i++) {
                if (i < keys.length - 1) {
                    map = (Map<String, Object>) map.get(keys[i]);
                } else {
                    return (Double) map.get(keys[i]);
                }
            }
        } catch (Exception ignored) {
        }
        return Double.MIN_VALUE;
    }
}
