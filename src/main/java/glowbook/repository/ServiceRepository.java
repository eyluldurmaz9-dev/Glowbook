package glowbook.repository;

import glowbook.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    List<Service> findByActiveTrueOrderByServiceNameAsc();

    Optional<Service> findByServiceIdAndActiveTrue(Integer serviceId);
}
