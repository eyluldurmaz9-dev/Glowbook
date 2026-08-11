package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.CatalogDtos;
import glowbook.dto.DtoMapper;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.service.ServiceCatalogService;
import glowbook.service.ServiceOptionService;
import glowbook.service.ServicePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final ServiceCatalogService serviceCatalogService;
    private final ServiceOptionService serviceOptionService;
    private final ServicePackageService servicePackageService;

    @GetMapping("/catalog/services")
    public ApiResponse<List<CatalogDtos.ServiceResponse>> getServices() {
        return ApiResponse.success("Services listed", serviceCatalogService.getActiveServices()
                .stream()
                .map(DtoMapper::toServiceResponse)
                .toList());
    }

    @GetMapping("/catalog/services/{serviceId}/options")
    public ApiResponse<List<CatalogDtos.ServiceOptionResponse>> getOptions(@PathVariable Integer serviceId) {
        return ApiResponse.success("Service options listed", serviceOptionService.getActiveOptionsByService(serviceId)
                .stream()
                .map(DtoMapper::toServiceOptionResponse)
                .toList());
    }

    @GetMapping("/catalog/services/{serviceId}/packages")
    public ApiResponse<List<CatalogDtos.ServicePackageResponse>> getPackages(@PathVariable Integer serviceId) {
        return ApiResponse.success("Service packages listed", servicePackageService.getActivePackagesByService(serviceId)
                .stream()
                .map(DtoMapper::toServicePackageResponse)
                .toList());
    }

    @PostMapping("/admin/services")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServiceResponse> createService(@Valid @RequestBody CatalogDtos.ServiceRequest request) {
        glowbook.entity.Service service = glowbook.entity.Service.builder()
                .serviceName(request.serviceName())
                .description(request.description())
                .serviceImage(request.serviceImage())
                .active(request.active() == null || request.active())
                .build();
        return ApiResponse.success("Service created", DtoMapper.toServiceResponse(serviceCatalogService.create(service)));
    }

    @PutMapping("/admin/services/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServiceResponse> updateService(
            @PathVariable Integer serviceId,
            @Valid @RequestBody CatalogDtos.ServiceRequest request
    ) {
        glowbook.entity.Service service = glowbook.entity.Service.builder()
                .serviceName(request.serviceName())
                .description(request.description())
                .serviceImage(request.serviceImage())
                .active(request.active())
                .build();
        return ApiResponse.success("Service updated", DtoMapper.toServiceResponse(serviceCatalogService.update(serviceId, service)));
    }

    @DeleteMapping("/admin/services/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServiceResponse> deactivateService(@PathVariable Integer serviceId) {
        return ApiResponse.success("Service deactivated", DtoMapper.toServiceResponse(serviceCatalogService.deactivate(serviceId)));
    }

    @PostMapping("/admin/services/{serviceId}/options")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServiceOptionResponse> createOption(
            @PathVariable Integer serviceId,
            @Valid @RequestBody CatalogDtos.ServiceOptionRequest request
    ) {
        ServiceOption option = ServiceOption.builder()
                .optionName(request.optionName())
                .price(request.price())
                .active(request.active() == null || request.active())
                .build();
        return ApiResponse.success("Service option created", DtoMapper.toServiceOptionResponse(serviceOptionService.create(serviceId, option)));
    }

    @PutMapping("/admin/options/{optionId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServiceOptionResponse> updateOption(
            @PathVariable Integer optionId,
            @Valid @RequestBody CatalogDtos.ServiceOptionRequest request
    ) {
        ServiceOption option = ServiceOption.builder()
                .optionName(request.optionName())
                .price(request.price())
                .active(request.active())
                .build();
        return ApiResponse.success("Service option updated", DtoMapper.toServiceOptionResponse(serviceOptionService.update(optionId, option)));
    }

    @PostMapping("/admin/services/{serviceId}/packages")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServicePackageResponse> createPackage(
            @PathVariable Integer serviceId,
            @Valid @RequestBody CatalogDtos.ServicePackageRequest request
    ) {
        ServicePackage servicePackage = toServicePackage(request);
        return ApiResponse.success("Service package created", DtoMapper.toServicePackageResponse(servicePackageService.create(serviceId, servicePackage)));
    }

    @PutMapping("/admin/packages/{packageId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ApiResponse<CatalogDtos.ServicePackageResponse> updatePackage(
            @PathVariable Integer packageId,
            @Valid @RequestBody CatalogDtos.ServicePackageRequest request
    ) {
        return ApiResponse.success("Service package updated", DtoMapper.toServicePackageResponse(servicePackageService.update(packageId, toServicePackage(request))));
    }

    private ServicePackage toServicePackage(CatalogDtos.ServicePackageRequest request) {
        return ServicePackage.builder()
                .packageName(request.packageName())
                .description(request.description())
                .totalSession(request.totalSession())
                .price(request.price())
                .validityDays(request.validityDays() == null ? 365 : request.validityDays())
                .packageImage(request.packageImage())
                .active(request.active() == null || request.active())
                .build();
    }
}
