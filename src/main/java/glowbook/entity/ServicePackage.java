package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

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

    /**
     * The specific sub-service(s) (see {@link ServiceOption}) this package may be booked
     * for — the authoritative source of package/service coverage. A package's {@link
     * #service} only says which broad category it belongs to (e.g. "Cilt Bakımı"); most
     * categories have several sub-services (Hydrafacial, Akne Bakımı, ...), so without this
     * relationship a "Hydrafacial" package could be booked for any of them. Eagerly fetched
     * — this app runs with {@code spring.jpa.open-in-view=false}, so a lazy collection would
     * throw once accessed from a DTO mapper outside the owning transaction, and every
     * package only ever covers a handful of options.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "package_covered_options",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    @OrderBy("optionName ASC")
    @Builder.Default
    private Set<ServiceOption> coveredOptions = new LinkedHashSet<>();

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_session", nullable = false)
    private Integer totalSession;

    @Column(nullable = false)
    private Double price;

    @Column(name = "validity_days")
    @Builder.Default
    private Integer validityDays = 365;

    @Column(name = "package_image")
    private String packageImage;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;
}
