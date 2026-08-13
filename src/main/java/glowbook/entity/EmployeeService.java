package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_service_id")
    private Integer employeeServiceId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    /**
     * Null keeps legacy service-wide assignments valid. New admin-managed
     * competencies are stored at the more precise option/sub-service level.
     */
    @ManyToOne
    @JoinColumn(name = "option_id")
    private ServiceOption serviceOption;

}
