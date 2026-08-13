package glowbook.config;

import glowbook.entity.Employee;
import glowbook.entity.EmployeeService;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.entity.WorkingHour;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.ServiceRepository;
import glowbook.repository.WorkingHourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.text.Normalizer;
import glowbook.security.UserRole;

@Configuration
@RequiredArgsConstructor
public class CatalogSeedConfig {

    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(0)
    ApplicationRunner seedCatalog() {
        return args -> seedCatalogData();
    }

    @Transactional
    void seedCatalogData() {
        glowbook.entity.Service skinCare = createService(
                "Cilt Bakimi",
                "Cilt analizi, derin temizlik ve nem bakimi ile GlowBook'un imza bakim deneyimi.",
                "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=900&q=80"
        );
        createOption(skinCare, "Klasik Cilt Bakimi", 900.0);
        createOption(skinCare, "Hydrafacial Bakim", 1500.0);
        createOption(skinCare, "Anti Aging Bakim", 1800.0);
        createOption(skinCare, "Leke Bakimi", 1650.0);
        createOption(skinCare, "Akne Bakimi", 1400.0);
        createPackage(skinCare, "Glow Cilt Paketi", "4 seanslik yenileyici cilt bakimi paketi.", 4, 5200.0);
        createPackage(skinCare, "Hydrafacial Bakim Paketi", "5 seanslik nem ve parlaklik bakim paketi.", 5, 6500.0);

        glowbook.entity.Service laser = createService(
                "Lazer Epilasyon",
                "Konforlu randevu akisiyla bolge bazli lazer epilasyon hizmetleri.",
                "https://images.unsplash.com/photo-1515377905703-c4788e51af15?auto=format&fit=crop&w=900&q=80"
        );
        createOption(laser, "Tek Bolge", 450.0);
        createOption(laser, "3 Bolge", 1100.0);
        createOption(laser, "5 Bolge", 1650.0);
        createOption(laser, "Yuz Bolgesi", 650.0);
        createOption(laser, "Tum Vucut", 2200.0);
        createPackage(laser, "5 Bolge Lazer Paketi", "10 seanslik 5 bolge lazer epilasyon paketi.", 10, 1500.0);
        createPackage(laser, "Tum Vucut Lazer Paketi", "8 seanslik tum vucut lazer epilasyon programi.", 8, 11500.0);
        createPackage(laser, "Yuz Bolgesi Lazer Paketi", "6 seanslik yuz bolgesi lazer epilasyon paketi.", 6, 3200.0);

        glowbook.entity.Service massage = createService(
                "Masaj ve Spa",
                "Rahatlatan masaj seanslari ve spa bakimlari.",
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=900&q=80"
        );
        createOption(massage, "Aromaterapi Masaji", 1200.0);
        createOption(massage, "Medikal Masaj", 1450.0);
        createPackage(massage, "Spa Yenilenme Paketi", "3 seanslik masaj ve spa paketi.", 3, 3600.0);

        glowbook.entity.Service brow = createService(
                "Kas ve Kirpik",
                "Kas alimi, kas tasarimi, lifting ve kirpik bakimi.",
                "https://images.unsplash.com/photo-1519415510236-718bdfcd89c8?auto=format&fit=crop&w=900&q=80"
        );
        createOption(brow, "Kas Alimi", 350.0);
        createOption(brow, "Kas Tasarimi", 650.0);
        createOption(brow, "Kas Laminasyonu", 900.0);
        createOption(brow, "Kirpik Lifting", 950.0);
        createPackage(brow, "Kas Kirpik Bakim Paketi", "4 seanslik kas ve kirpik bakim paketi.", 4, 2600.0);

        glowbook.entity.Service slimming = createService(
                "Bolgesel Incelme",
                "Bolgesel incelme, sikilasma ve selulit bakimi seanslari.",
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=900&q=80"
        );
        createOption(slimming, "Karin Bolgesi", 1450.0);
        createOption(slimming, "Bacak Bolgesi", 1550.0);
        createOption(slimming, "Kol Bolgesi", 1250.0);
        createOption(slimming, "Selulit Bakimi", 1650.0);
        createPackage(slimming, "Incelme Programi", "6 seanslik bolgesel incelme programi.", 6, 7800.0);
        createPackage(slimming, "Sikilasma Paketi", "8 seanslik bolgesel sikilasma ve selulit bakimi.", 8, 9800.0);

        Employee admin = createEmployee("ADMIN", "GlowBook", "Admin", "05550000000", "admin@glowbook.com", UserRole.ADMIN);
        Employee skinExpert = createEmployee("GLW001", "Defne", "Yilmaz", "05551110001", "defne@glowbook.com", UserRole.EMPLOYEE);
        Employee laserExpert = createEmployee("GLW002", "Mina", "Kaya", "05551110002", "mina@glowbook.com", UserRole.EMPLOYEE);
        Employee spaExpert = createEmployee("GLW003", "Selin", "Aydin", "05551110003", "selin@glowbook.com", UserRole.EMPLOYEE);

        assignAll(admin, List.of(skinCare, laser, massage, brow, slimming));
        assignAll(skinExpert, List.of(skinCare, brow, slimming));
        assignAll(laserExpert, List.of(laser, slimming));
        assignAll(spaExpert, List.of(massage, skinCare));

        seedWorkingHours();
    }

    private glowbook.entity.Service createService(String name, String description, String image) {
        return serviceRepository.findAll().stream()
                .filter(service -> canonicalName(name).equals(canonicalName(service.getServiceName())))
                .findFirst()
                .map(service -> {
                    service.setDescription(description);
                    service.setServiceImage(image);
                    service.setActive(true);
                    return serviceRepository.save(service);
                })
                .orElseGet(() -> serviceRepository.save(glowbook.entity.Service.builder()
                .serviceName(name)
                .description(description)
                .serviceImage(image)
                .active(true)
                .build()));
    }

    private void createOption(glowbook.entity.Service service, String name, Double price) {
        boolean exists = serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(service.getServiceId())
                .stream()
                .anyMatch(option -> name.equalsIgnoreCase(option.getOptionName()));
        if (exists) {
            return;
        }
        serviceOptionRepository.save(ServiceOption.builder()
                .service(service)
                .optionName(name)
                .price(price)
                .active(true)
                .build());
    }

    private void createPackage(glowbook.entity.Service service, String name, String description, Integer sessions, Double price) {
        var existing = servicePackageRepository.findByServiceServiceIdAndActiveTrueOrderByPackageNameAsc(service.getServiceId())
                .stream()
                .filter(servicePackage -> name.equalsIgnoreCase(servicePackage.getPackageName()))
                .findFirst();
        if (existing.isPresent()) {
            ServicePackage servicePackage = existing.get();
            servicePackage.setDescription(description);
            servicePackage.setTotalSession(sessions);
            servicePackage.setPrice(price);
            servicePackage.setValidityDays(365);
            servicePackage.setActive(true);
            servicePackageRepository.save(servicePackage);
            return;
        }
        servicePackageRepository.save(ServicePackage.builder()
                .service(service)
                .packageName(name)
                .description(description)
                .totalSession(sessions)
                .price(price)
                .validityDays(365)
                .active(true)
                .build());
    }

    private Employee createEmployee(String id, String firstName, String lastName, String phone, String email, UserRole role) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFirstName(firstName);
                    employee.setLastName(lastName);
                    employee.setPhone(phone);
                    employee.setEmail(email);
                    employee.setActive(true);
                    employee.setRole(role);
                    return employeeRepository.save(employee);
                })
                .orElseGet(() -> employeeRepository.save(Employee.builder()
                        .employeeId(id)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .phone(phone)
                        .email(email)
                        .active(true)
                        .role(role)
                        .build()));
    }

    private String canonicalName(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ı', 'i')
                .replace('İ', 'I')
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private void assignAll(Employee employee, List<glowbook.entity.Service> services) {
        services.forEach(service -> {
            if (!employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(employee.getEmployeeId(), service.getServiceId())) {
                employeeServiceRepository.save(EmployeeService.builder()
                        .employee(employee)
                        .service(service)
                        .build());
            }
        });
    }

    private void seedWorkingHours() {
        Arrays.stream(DayOfWeek.values()).forEach(day -> workingHourRepository.findByDayOfWeek(day)
                .orElseGet(() -> workingHourRepository.save(WorkingHour.builder()
                        .dayOfWeek(day)
                        .startTime(day == DayOfWeek.SUNDAY ? null : LocalTime.of(10, 0))
                        .endTime(day == DayOfWeek.SUNDAY ? null : LocalTime.of(19, 0))
                        .closed(day == DayOfWeek.SUNDAY)
                        .build())));
    }
}
