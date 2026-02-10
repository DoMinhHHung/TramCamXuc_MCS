package iuh.fit.se.serviceidentity.entity;

import iuh.fit.se.serviceidentity.entity.converter.JsonToMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_features")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeatures {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> features;
}