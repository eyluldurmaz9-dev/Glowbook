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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

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
        createPackage(laser, "Lazer Devam Paketi", "6 seanslik avantajli lazer epilasyon paketi.", 6, 11500.0);

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

        Employee admin = createEmployee("ADMIN", "GlowBook", "Admin", "admin123", "05550000000", "admin@glowbook.com");
        Employee skinExpert = createEmployee("GLW001", "Defne", "Yilmaz", "123456", "05551110001", "defne@glowbook.com");
        Employee laserExpert = createEmployee("GLW002", "Mina", "Kaya", "123456", "05551110002", "mina@glowbook.com");
        Employee spaExpert = createEmployee("GLW003", "Selin", "Aydin", "123456", "05551110003", "selin@glowbook.com");

        assignAll(admin, List.of(skinCare, laser, massage, brow, slimming));
        assignAll(skinExpert, List.of(skinCare, brow, slimming));
        assignAll(laserExpert, List.of(laser, slimming));
        assignAll(spaExpert, List.of(massage, skinCare));

        seedWorkingHours();
    }

    private glowbook.entity.Service createService(String name, String description, String image) {
        return serviceRepository.findAll().stream()
                .filter(service -> name.equalsIgnoreCase(service.getServiceName()))
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
        boolean exists = servicePackageRepository.findByServiceServiceIdAndActiveTrueOrderByPackageNameAsc(service.getServiceId())
                .stream()
                .anyMatch(servicePackage -> name.equalsIgnoreCase(servicePackage.getPackageName()));
        if (exists) {
            return;
        }
        servicePackageRepository.save(ServicePackage.builder()
                .service(service)
                .packageName(name)
                .description(description)
                .totalSession(sessions)
                .price(price)
                .active(true)
                .build());
    }

    private Employee createEmployee(String id, String firstName, String lastName, String password, String phone, String email) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFirstName(firstName);
                    employee.setLastName(lastName);
                    employee.setPhone(phone);
                    employee.setEmail(email);
                    employee.setActive(true);
                    if (employee.getPassword() == null || employee.getPassword().isBlank()) {
                        employee.setPassword(passwordEncoder.encode(password));
                    }
                    return employeeRepository.save(employee);
                })
                .orElseGet(() -> employeeRepository.save(Employee.builder()
                        .employeeId(id)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(passwordEncoder.encode(password))
                        .phone(phone)
                        .email(email)
                        .active(true)
                        .build()));
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
