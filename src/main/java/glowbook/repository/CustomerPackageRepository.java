package glowbook.repository;

import glowbook.entity.CustomerPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPackageRepository extends JpaRepository<CustomerPackage, Integer> {

    List<CustomerPackage> findByCustomerCustomerIdAndActiveTrueOrderByPurchaseDateDesc(Integer customerId);

    Optional<CustomerPackage> findByCustomerPackageIdAndCustomerCustomerIdAndActiveTrue(
            Integer customerPackageId,
            Integer customerId
    );
}
