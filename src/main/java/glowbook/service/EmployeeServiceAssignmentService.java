package glowbook.service;

import glowbook.entity.EmployeeService;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.EmployeeServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceAssignmentService {

    private final EmployeeServiceRepository employeeServiceRepository;
    private final EmployeeManagementService employeeManagementService;
    private final ServiceCatalogService serviceCatalogService;

    private final ServiceOptionService serviceOptionService;

    public List<EmployeeService> getEmployeesByService(Integer serviceId) {
        serviceCatalogService.getActiveById(serviceId);
        return employeeServiceRepository.findByServiceServiceIdAndEmployeeActiveTrue(serviceId);
    }

    public List<EmployeeService> getEmployeesByServiceOption(Integer serviceId, Integer optionId) {
        serviceOptionService.getActiveByService(serviceId, optionId);
        return employeeServiceRepository.findQualifiedActiveEmployees(serviceId, optionId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.getEmployee().getEmployeeId(),
                        item -> item,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values().stream().toList();
    }

    public List<EmployeeService> getAssignments(String employeeId) {
        employeeManagementService.getById(employeeId);
        return employeeServiceRepository.findByEmployeeEmployeeIdOrderByServiceServiceNameAsc(employeeId);
    }

    public EmployeeService getById(Integer employeeServiceId) {
        return employeeServiceRepository.findById(employeeServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Personel görev ataması bulunamadı."));
    }

    public boolean employeeCanProvideServiceOption(String employeeId, Integer serviceId, Integer optionId) {
        return employeeServiceRepository.employeeCanProvideOption(employeeId, serviceId, optionId);
    }

    public boolean employeeCanProvideService(String employeeId, Integer serviceId) {
        return employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(employeeId, serviceId);
    }

    @Transactional
    public EmployeeService assign(String employeeId, Integer serviceId) {
        if (employeeCanProvideService(employeeId, serviceId)) {
            throw new BusinessException("Bu personel bu hizmete zaten atanmış.");
        }

        EmployeeService employeeService = EmployeeService.builder()
                .employee(employeeManagementService.getActiveById(employeeId))
                .service(serviceCatalogService.getActiveById(serviceId))
                .build();

        return employeeServiceRepository.save(employeeService);
    }

    @Transactional
    public List<EmployeeService> replaceOptionAssignments(String employeeId, Collection<Integer> optionIds) {
        var employee = employeeManagementService.getById(employeeId);
        var uniqueOptionIds = new LinkedHashSet<>(optionIds == null ? List.of() : optionIds);
        var options = uniqueOptionIds.stream()
                .map(serviceOptionService::getActiveById)
                .toList();

        employeeServiceRepository.deleteByEmployeeEmployeeId(employeeId);
        employeeServiceRepository.flush();
        return options.stream()
                .map(option -> employeeServiceRepository.save(EmployeeService.builder()
                        .employee(employee)
                        .service(option.getService())
                        .serviceOption(option)
                        .build()))
                .toList();
    }

    @Transactional
    public List<EmployeeService> replaceServiceAssignments(String employeeId, Collection<Integer> serviceIds) {
        var employee = employeeManagementService.getById(employeeId);
        var uniqueServiceIds = new LinkedHashSet<>(serviceIds == null ? List.of() : serviceIds);
        var services = uniqueServiceIds.stream()
                .map(serviceCatalogService::getActiveById)
                .toList();

        employeeServiceRepository.deleteByEmployeeEmployeeId(employeeId);
        employeeServiceRepository.flush();
        return services.stream()
                .map(service -> employeeServiceRepository.save(EmployeeService.builder()
                        .employee(employee)
                        .service(service)
                        .build()))
                .toList();
    }

    @Transactional
    public void delete(Integer employeeServiceId) {
        employeeServiceRepository.delete(getById(employeeServiceId));
    }
}
