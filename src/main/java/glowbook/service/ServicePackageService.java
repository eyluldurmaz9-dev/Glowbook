package glowbook.service;

import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.ServicePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final ServiceOptionService serviceOptionService;

    public List<ServicePackage> getActivePackages() {
        return servicePackageRepository.findByActiveTrueOrderByPackageNameAsc();
    }

    public List<ServicePackage> getActivePackagesByService(Integer serviceId) {
        return servicePackageRepository.findByServiceServiceIdAndActiveTrueOrderByPackageNameAsc(serviceId);
    }

    public ServicePackage getById(Integer packageId) {
        return servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Paket bulunamadı."));
    }

    public ServicePackage getActiveById(Integer packageId) {
        return servicePackageRepository.findByPackageIdAndActiveTrue(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktif paket bulunamadı."));
    }

    @Transactional
    public ServicePackage create(Integer serviceId, ServicePackage servicePackage, List<Integer> coveredOptionIds) {
        servicePackage.setService(serviceCatalogService.getActiveById(serviceId));
        servicePackage.setCoveredOptions(resolveCoveredOptions(serviceId, coveredOptionIds));
        return servicePackageRepository.save(servicePackage);
    }

    @Transactional
    public ServicePackage update(Integer packageId, ServicePackage request, List<Integer> coveredOptionIds) {
        ServicePackage servicePackage = getById(packageId);

        servicePackage.setPackageName(request.getPackageName());
        servicePackage.setDescription(request.getDescription());
        servicePackage.setTotalSession(request.getTotalSession());
        servicePackage.setPrice(request.getPrice());
        servicePackage.setValidityDays(request.getValidityDays());
        servicePackage.setPackageImage(request.getPackageImage());
        servicePackage.setActive(request.getActive());
        servicePackage.setCoveredOptions(
                resolveCoveredOptions(servicePackage.getService().getServiceId(), coveredOptionIds));

        return servicePackageRepository.save(servicePackage);
    }

    /**
     * Every covered option id must be an active option belonging to the package's own
     * service — this is what makes package/service coverage authoritative rather than a
     * matter of naming convention. At least one id is required: a package that covers
     * nothing could never be booked.
     */
    private Set<ServiceOption> resolveCoveredOptions(Integer serviceId, List<Integer> coveredOptionIds) {
        if (coveredOptionIds == null || coveredOptionIds.isEmpty()) {
            throw new BusinessException("Paketin kapsadığı en az bir hizmet seçeneği belirtilmelidir.");
        }
        Set<ServiceOption> resolved = new LinkedHashSet<>();
        for (Integer optionId : coveredOptionIds) {
            resolved.add(serviceOptionService.getActiveByService(serviceId, optionId));
        }
        return resolved;
    }

    @Transactional
    public ServicePackage deactivate(Integer packageId) {
        ServicePackage servicePackage = getById(packageId);
        servicePackage.setActive(false);
        return servicePackageRepository.save(servicePackage);
    }
}
