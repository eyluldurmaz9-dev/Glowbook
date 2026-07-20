package glowbook.repository;

import glowbook.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {

    List<ServicePackage> findByActiveTrueOrderByPackageNameAsc();

    List<ServicePackage> findByServiceServiceIdAndActiveTrueOrderByPackageNameAsc(Integer serviceId);

    Optional<ServicePackage> findByPackageIdAndActiveTrue(Integer packageId);
}
