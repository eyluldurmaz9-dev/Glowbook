package glowbook.repository;

import glowbook.entity.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOptionRepository extends JpaRepository<ServiceOption, Integer> {

    List<ServiceOption> findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(Integer serviceId);

    Optional<ServiceOption> findByOptionIdAndServiceServiceIdAndActiveTrue(Integer optionId, Integer serviceId);
}
