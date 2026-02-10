package iuh.fit.se.serviceidentity.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter(autoApply = true)
public class JsonToMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert a Map<String, Object> into its JSON string representation for database storage.
     *
     * @param attribute the map to convert; may be null
     * @return the JSON string representation of the map, or `null` if {@code attribute} is null
     * @throws IllegalArgumentException if the map cannot be serialized to JSON
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) return null;
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Lỗi convert Map sang JSON", e);
        }
    }

    /**
     * Convert a JSON string retrieved from the database into a Map.
     *
     * @param dbData the JSON string stored in the database; may be null or empty
     * @return a {@code Map<String, Object>} parsed from {@code dbData}, or an empty map if {@code dbData} is null or empty
     * @throws IllegalArgumentException if the JSON cannot be parsed into a map
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(dbData, new TypeReference<HashMap<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Lỗi convert JSON sang Map", e);
        }
    }
}