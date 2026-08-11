package glowbook.service;

import glowbook.entity.ServicePackage;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.ServicePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final ServiceCatalogService serviceCatalogService;

    public List<ServicePackage> getActivePackages() {
        return servicePackageRepository.findByActiveTrueOrderByPackageNameAsc();
    }

    public List<ServicePackage> getActivePackagesByService(Integer serviceId) {
        return servicePackageRepository.findByServiceServiceIdAndActiveTrueOrderByPackageNameAsc(serviceId);
    }

    public ServicePackage getById(Integer packageId) {
        return servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found: " + packageId));
    }

    public ServicePackage getActiveById(Integer packageId) {
        return servicePackageRepository.findByPackageIdAndActiveTrue(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Active service package not found: " + packageId));
    }

    @Transactional
    public ServicePackage create(Integer serviceId, ServicePackage servicePackage) {
        servicePackage.setService(serviceCatalogService.getActiveById(serviceId));
        return servicePackageRepository.save(servicePackage);
    }

    @Transactional
    public ServicePackage update(Integer packageId, ServicePackage request) {
        ServicePackage servicePackage = getById(packageId);

        servicePackage.setPackageName(request.getPackageName());
        servicePackage.setDescription(request.getDescription());
        servicePackage.setTotalSession(request.getTotalSession());
        servicePackage.setPrice(request.getPrice());
        servicePackage.setValidityDays(request.getValidityDays());
        servicePackage.setPackageImage(request.getPackageImage());
        servicePackage.setActive(request.getActive());

        return servicePackageRepository.save(servicePackage);
    }

    @Transactional
    public ServicePackage deactivate(Integer packageId) {
        ServicePackage servicePackage = getById(packageId);
        servicePackage.setActive(false);
        return servicePackageRepository.save(servicePackage);
    }
}
