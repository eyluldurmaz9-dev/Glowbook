package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer optionId;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(nullable = false)
    private Double price;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;
}
