package glowbook.entity;

public class ServicePackage {
package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Integer packageId;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_session", nullable = false)
    private Integer totalSession;

    @Column(nullable = false)
    private Double price;

    @Column(name = "package_image")
    private String packageImage;

    @Column(name = "is_active")
    private Boolean active = true;
}
