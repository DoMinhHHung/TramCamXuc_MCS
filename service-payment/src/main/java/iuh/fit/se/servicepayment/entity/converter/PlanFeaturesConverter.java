package iuh.fit.se.servicepayment.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.servicepayment.entity.json.PlanFeatures;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanFeaturesConverter implements AttributeConverter<PlanFeatures, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PlanFeatures attribute) {
        if (attribute == null) return null;
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting PlanFeatures to JSON", e);
        }
    }

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