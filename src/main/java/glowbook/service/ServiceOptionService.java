package glowbook.service;

import glowbook.entity.ServiceOption;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.ServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceOptionService {

    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceCatalogService serviceCatalogService;

    public List<ServiceOption> getActiveOptionsByService(Integer serviceId) {
        return serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId);
    }

    public ServiceOption getById(Integer optionId) {
        return serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Service option not found: " + optionId));
    }

    public ServiceOption getActiveByService(Integer serviceId, Integer optionId) {
        return serviceOptionRepository.findByOptionIdAndServiceServiceIdAndActiveTrue(optionId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Active option not found: " + optionId));
    }

    public ServiceOption getActiveById(Integer optionId) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Service option not found: " + optionId));
        if (!Boolean.TRUE.equals(option.getActive()) || !Boolean.TRUE.equals(option.getService().getActive())) {
            throw new ResourceNotFoundException("Active service option not found: " + optionId);
        }
        return option;
    }

    @Transactional
    public ServiceOption create(Integer serviceId, ServiceOption option) {
        option.setService(serviceCatalogService.getActiveById(serviceId));
        return serviceOptionRepository.save(option);
    }

    @Transactional
    public ServiceOption update(Integer optionId, ServiceOption request) {
        ServiceOption option = getById(optionId);

        option.setOptionName(request.getOptionName());
        option.setPrice(request.getPrice());
        option.setActive(request.getActive());

        return serviceOptionRepository.save(option);
    }

    @Transactional
    public ServiceOption deactivate(Integer optionId) {
        ServiceOption option = getById(optionId);
        option.setActive(false);
        return serviceOptionRepository.save(option);
    }
}
