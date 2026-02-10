package iuh.fit.se.servicepayment.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.servicepayment.entity.json.PlanFeatures;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanFeaturesConverter implements AttributeConverter<PlanFeatures, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a PlanFeatures instance to its JSON string representation for database storage.
     *
     * @param attribute the PlanFeatures object to serialize; may be {@code null}
     * @return the JSON string representation of {@code attribute}, or {@code null} if {@code attribute} is {@code null}
     * @throws IllegalArgumentException if the PlanFeatures value cannot be serialized to JSON
     */
    @Override
    public String convertToDatabaseColumn(PlanFeatures attribute) {
        if (attribute == null) return null;
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting PlanFeatures to JSON", e);
        }
    }

    /**
     * Converts a JSON string from the database into a PlanFeatures instance.
     *
     * @param dbData the JSON string stored in the database, may be null or empty
     * @return the deserialized PlanFeatures instance, or null if {@code dbData} is null or empty
     * @throws IllegalArgumentException if JSON parsing fails
     */
    @Override
    public PlanFeatures convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        try {
            return objectMapper.readValue(dbData, PlanFeatures.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to PlanFeatures", e);
        }
    }
}