package glowbook.service;

import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public List<glowbook.entity.Service> getActiveServices() {
        return serviceRepository.findByActiveTrueOrderByServiceNameAsc();
    }

    public List<glowbook.entity.Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public glowbook.entity.Service getById(Integer serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hizmet bulunamadı."));
    }

    public glowbook.entity.Service getActiveById(Integer serviceId) {
        return serviceRepository.findByServiceIdAndActiveTrue(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktif hizmet bulunamadı."));
    }

    @Transactional
    public glowbook.entity.Service create(glowbook.entity.Service service) {
        return serviceRepository.save(service);
    }

    @Transactional
    public glowbook.entity.Service update(Integer serviceId, glowbook.entity.Service request) {
        glowbook.entity.Service service = getById(serviceId);

        service.setServiceName(request.getServiceName());
        service.setDescription(request.getDescription());
        service.setServiceImage(request.getServiceImage());
        service.setActive(request.getActive());

        return serviceRepository.save(service);
    }

    @Transactional
    public glowbook.entity.Service deactivate(Integer serviceId) {
        glowbook.entity.Service service = getById(serviceId);
        service.setActive(false);
        return serviceRepository.save(service);
    }
}
