package glowbook.service;

import glowbook.entity.EmployeeService;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.EmployeeServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceAssignmentService {

    private final EmployeeServiceRepository employeeServiceRepository;
    private final EmployeeManagementService employeeManagementService;
    private final ServiceCatalogService serviceCatalogService;

    public List<EmployeeService> getEmployeesByService(Integer serviceId) {
        return employeeServiceRepository.findByServiceServiceIdAndEmployeeActiveTrue(serviceId);
    }

    public EmployeeService getById(Integer employeeServiceId) {
        return employeeServiceRepository.findById(employeeServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee service assignment not found: " + employeeServiceId));
    }

    public boolean employeeCanProvideService(String employeeId, Integer serviceId) {
        return employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(employeeId, serviceId);
    }

    @Transactional
    public EmployeeService assign(String employeeId, Integer serviceId) {
        if (employeeCanProvideService(employeeId, serviceId)) {
            throw new BusinessException("Employee already assigned to service");
        }

        EmployeeService employeeService = EmployeeService.builder()
                .employee(employeeManagementService.getActiveById(employeeId))
                .service(serviceCatalogService.getActiveById(serviceId))
                .build();

        return employeeServiceRepository.save(employeeService);
    }

    @Transactional
    public void delete(Integer employeeServiceId) {
        employeeServiceRepository.delete(getById(employeeServiceId));
    }
}
