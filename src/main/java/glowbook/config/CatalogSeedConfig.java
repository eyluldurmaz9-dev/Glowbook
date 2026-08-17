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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
                "Cilt Bakımı",
                "Cilt analizi, derin temizlik ve nem bakımı ile GlowBook'un imza bakım deneyimi.",
                "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=900&q=80"
        );
        ServiceOption klasikCiltBakimi = createOption(skinCare, "Klasik Cilt Bakımı", 900.0);
        ServiceOption hydrafacialBakim = createOption(skinCare, "Hydrafacial Bakım", 1500.0);
        createOption(skinCare, "Anti Aging Bakım", 1800.0);
        ServiceOption lekeBakimi = createOption(skinCare, "Leke Bakımı", 1650.0);
        createOption(skinCare, "Akne Bakımı", 1400.0);
        // Covers two related general-renewal treatments — a genuine multi-option package.
        createPackage(skinCare, "Glow Cilt Paketi", "4 seanslık yenileyici cilt bakımı paketi.", 4, 5200.0,
                List.of(klasikCiltBakimi, lekeBakimi));
        // Single-option package: its name names the one treatment it covers, and nothing else
        // under "Cilt Bakımı" (e.g. Akne Bakımı) may ever be booked against it.
        createPackage(skinCare, "Hydrafacial Bakım Paketi", "5 seanslık nem ve parlaklık bakım paketi.", 5, 6500.0,
                List.of(hydrafacialBakim));

        glowbook.entity.Service laser = createService(
                "Lazer Epilasyon",
                "Konforlu randevu akışıyla bölge bazlı lazer epilasyon hizmetleri.",
                "https://images.unsplash.com/photo-1515377905703-c4788e51af15?auto=format&fit=crop&w=900&q=80"
        );
        createOption(laser, "Tek Bölge", 450.0);
        createOption(laser, "3 Bölge", 1100.0);
        ServiceOption besBolge = createOption(laser, "5 Bölge", 1650.0);
        ServiceOption yuzBolgesiLazer = createOption(laser, "Yüz Bölgesi", 650.0);
        ServiceOption tumVucutLazer = createOption(laser, "Tüm Vücut", 2200.0);
        createPackage(laser, "5 Bölge Lazer Paketi", "10 seanslık 5 bölge lazer epilasyon paketi.", 10, 1500.0,
                List.of(besBolge));
        createPackage(laser, "Tüm Vücut Lazer Paketi", "8 seanslık tüm vücut lazer epilasyon programı.", 8, 11500.0,
                List.of(tumVucutLazer));
        createPackage(laser, "Yüz Bölgesi Lazer Paketi", "6 seanslık yüz bölgesi lazer epilasyon paketi.", 6, 3200.0,
                List.of(yuzBolgesiLazer));

        glowbook.entity.Service massage = createService(
                "Masaj ve Spa",
                "Rahatlatan masaj seansları ve spa bakımları.",
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=900&q=80"
        );
        ServiceOption aromaterapi = createOption(massage, "Aromaterapi Masajı", 1200.0);
        ServiceOption medikalMasaj = createOption(massage, "Medikal Masaj", 1450.0);
        createPackage(massage, "Spa Yenilenme Paketi", "3 seanslık masaj ve spa paketi.", 3, 3600.0,
                List.of(aromaterapi, medikalMasaj));

        glowbook.entity.Service brow = createService(
                "Kaş ve Kirpik",
                "Kaş alımı, kaş tasarımı, lifting ve kirpik bakımı.",
                "https://images.unsplash.com/photo-1519415510236-718bdfcd89c8?auto=format&fit=crop&w=900&q=80"
        );
        createOption(brow, "Kaş Alımı", 350.0);
        ServiceOption kasTasarimi = createOption(brow, "Kaş Tasarımı", 650.0);
        createOption(brow, "Kaş Laminasyonu", 900.0);
        ServiceOption kirpikLifting = createOption(brow, "Kirpik Lifting", 950.0);
        createPackage(brow, "Kaş Kirpik Bakım Paketi", "4 seanslık kaş ve kirpik bakım paketi.", 4, 2600.0,
                List.of(kasTasarimi, kirpikLifting));

        glowbook.entity.Service slimming = createService(
                "Bölgesel İncelme",
                "Bölgesel incelme, sıkılaşma ve selülit bakımı seansları.",
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=900&q=80"
        );
        ServiceOption karinBolgesi = createOption(slimming, "Karın Bölgesi", 1450.0);
        ServiceOption bacakBolgesi = createOption(slimming, "Bacak Bölgesi", 1550.0);
        ServiceOption kolBolgesi = createOption(slimming, "Kol Bölgesi", 1250.0);
        ServiceOption selulitBakimi = createOption(slimming, "Selülit Bakımı", 1650.0);
        // Covers three body-region treatments but deliberately not Selülit Bakımı, which has
        // its own dedicated package below.
        createPackage(slimming, "İncelme Programı", "6 seanslık bölgesel incelme programı.", 6, 7800.0,
                List.of(karinBolgesi, bacakBolgesi, kolBolgesi));
        createPackage(slimming, "Sıkılaşma Paketi", "8 seanslık bölgesel sıkılaşma ve selülit bakımı.", 8, 9800.0,
                List.of(selulitBakimi));

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

    private ServiceOption createOption(glowbook.entity.Service service, String name, Double price) {
        var existing = serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(service.getServiceId())
                .stream()
                .filter(option -> name.equalsIgnoreCase(option.getOptionName()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        return serviceOptionRepository.save(ServiceOption.builder()
                .service(service)
                .optionName(name)
                .price(price)
                .active(true)
                .build());
    }

    /** {@code coveredOptions} is authoritative: it is what a customer may actually book
     * this package for (see docs/PACKAGE_SERVICE_COVERAGE.md), independent of how many
     * other sub-services its {@code service} category happens to have. */
    private void createPackage(glowbook.entity.Service service, String name, String description, Integer sessions,
                                Double price, List<ServiceOption> coveredOptions) {
        Set<ServiceOption> covered = new LinkedHashSet<>(coveredOptions);
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
            servicePackage.setCoveredOptions(covered);
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
                .coveredOptions(covered)
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
