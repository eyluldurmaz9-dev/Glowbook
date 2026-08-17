package glowbook.config;

import glowbook.entity.Employee;
import glowbook.entity.EmployeeService;
import glowbook.entity.Service;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.entity.WorkingHour;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.ServiceRepository;
import glowbook.repository.WaitingListRepository;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.text.Normalizer;
import glowbook.security.UserRole;

/**
 * Seeds (and, on every startup, re-heals) the demo/production catalog.
 *
 * <p>Every {@code createXxx} helper below is idempotent in the strong sense required by
 * docs/CATALOG_DATA_CLEANUP.md: given the same authoritative Turkish name, it always
 * converges the database to exactly one row with that exact spelling, no matter how many
 * historically-drifted duplicates (created back when matching was case-sensitive and
 * diacritic-sensitive) currently exist. It does this by (1) finding every row whose name
 * is the same business entity once Turkish diacritics are normalized away, (2) merging
 * every foreign-key reference (appointments, employee assignments, waiting list entries,
 * package coverage, owned packages) off every duplicate and onto a single surviving row,
 * (3) deleting the now-empty duplicates, and (4) overwriting the survivor's own fields —
 * including its name — to the authoritative value. Step (4) is what earlier versions of
 * this seed were missing: a canonical-name match alone does not fix a misspelled name
 * already stored on the one row that survives, so a row could stay wrong forever even
 * without ever being duplicated.</p>
 *
 * <p>Nothing here ever touches a row whose name does not canonically match one of the
 * authoritative names below — an admin-created service, option, package or employee with
 * no counterpart in this list is left completely untouched, including if its name happens
 * to look similar to something else. Matching is scoped (services by name; options and
 * packages by name <b>within their current service</b>) specifically so two genuinely
 * different real-world offerings are never merged just because they share a word.</p>
 */
@Configuration
@RequiredArgsConstructor
public class CatalogSeedConfig {

    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeServiceRepository employeeServiceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final WaitingListRepository waitingListRepository;
    private final CustomerPackageRepository customerPackageRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(0)
    ApplicationRunner seedCatalog() {
        return args -> seedCatalogData();
    }

    @Transactional
    void seedCatalogData() {
        Service skinCare = createService(
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

        Service laser = createService(
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

        Service massage = createService(
                "Masaj ve Spa",
                "Rahatlatan masaj seansları ve spa bakımları.",
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=900&q=80"
        );
        ServiceOption aromaterapi = createOption(massage, "Aromaterapi Masajı", 1200.0);
        ServiceOption medikalMasaj = createOption(massage, "Medikal Masaj", 1450.0);
        createPackage(massage, "Spa Yenilenme Paketi", "3 seanslık masaj ve spa paketi.", 3, 3600.0,
                List.of(aromaterapi, medikalMasaj));

        Service brow = createService(
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

        Service slimming = createService(
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
        Employee skinExpert = createEmployee("GLW001", "Defne", "Yılmaz", "05551110001", "defne@glowbook.com", UserRole.EMPLOYEE);
        Employee laserExpert = createEmployee("GLW002", "Mina", "Kaya", "05551110002", "mina@glowbook.com", UserRole.EMPLOYEE);
        Employee spaExpert = createEmployee("GLW003", "Selin", "Aydın", "05551110003", "selin@glowbook.com", UserRole.EMPLOYEE);

        assignAll(admin, List.of(skinCare, laser, massage, brow, slimming));
        assignAll(skinExpert, List.of(skinCare, brow, slimming));
        assignAll(laserExpert, List.of(laser, slimming));
        assignAll(spaExpert, List.of(massage, skinCare));

        // Historical Turkish-spelling drift (case- and diacritic-sensitive matching in
        // earlier versions of this seed) could have left an employee assigned twice —
        // once for a service/option row that has since been merged into a survivor above,
        // and once for the survivor itself. Neither assignment is wrong on its own; only
        // the resulting duplicate pairing is.
        deduplicateEmployeeServiceAssignments();

        seedWorkingHours();
    }

    private Service createService(String name, String description, String image) {
        List<Service> matches = serviceRepository.findAll().stream()
                .filter(service -> canonicalName(name).equals(canonicalName(service.getServiceName())))
                .sorted(Comparator.comparing(Service::getServiceId))
                .toList();

        Service survivor;
        if (matches.isEmpty()) {
            survivor = serviceRepository.save(Service.builder()
                    .serviceName(name)
                    .description(description)
                    .serviceImage(image)
                    .active(true)
                    .build());
        } else {
            survivor = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                mergeServiceInto(matches.get(i), survivor);
            }
        }

        // Overwritten unconditionally — a canonical-name match only proves this is the
        // same business entity, not that its stored spelling is already correct.
        survivor.setServiceName(name);
        survivor.setDescription(description);
        survivor.setServiceImage(image);
        survivor.setActive(true);
        return serviceRepository.save(survivor);
    }

    /**
     * Re-points every reference to {@code duplicate} onto {@code survivor} — appointments,
     * employee assignments, waiting-list entries, and the duplicate's own sub-services and
     * packages (which are moved rather than deleted, so they can then be deduplicated in
     * turn against whatever the survivor already had) — then deletes the now-unreferenced
     * duplicate row.
     */
    private void mergeServiceInto(Service duplicate, Service survivor) {
        Integer duplicateId = duplicate.getServiceId();

        appointmentRepository.findByServiceServiceId(duplicateId)
                .forEach(appointment -> {
                    appointment.setService(survivor);
                    appointmentRepository.save(appointment);
                });
        employeeServiceRepository.findByServiceServiceId(duplicateId)
                .forEach(assignment -> {
                    assignment.setService(survivor);
                    employeeServiceRepository.save(assignment);
                });
        waitingListRepository.findByServiceServiceId(duplicateId)
                .forEach(entry -> {
                    entry.setService(survivor);
                    waitingListRepository.save(entry);
                });
        serviceOptionRepository.findByServiceServiceId(duplicateId)
                .forEach(option -> {
                    option.setService(survivor);
                    serviceOptionRepository.save(option);
                });
        servicePackageRepository.findByServiceServiceId(duplicateId)
                .forEach(servicePackage -> {
                    servicePackage.setService(survivor);
                    servicePackageRepository.save(servicePackage);
                });

        serviceRepository.delete(duplicate);
    }

    private ServiceOption createOption(Service service, String name, Double price) {
        List<ServiceOption> matches = serviceOptionRepository.findByServiceServiceId(service.getServiceId()).stream()
                .filter(option -> canonicalName(name).equals(canonicalName(option.getOptionName())))
                .sorted(Comparator.comparing(ServiceOption::getOptionId))
                .toList();

        ServiceOption survivor;
        if (matches.isEmpty()) {
            survivor = serviceOptionRepository.save(ServiceOption.builder()
                    .service(service)
                    .optionName(name)
                    .price(price)
                    .active(true)
                    .build());
        } else {
            survivor = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                mergeServiceOptionInto(matches.get(i), survivor);
            }
        }

        survivor.setService(service);
        survivor.setOptionName(name);
        survivor.setPrice(price);
        survivor.setActive(true);
        return serviceOptionRepository.save(survivor);
    }

    /**
     * Re-points every reference to {@code duplicate} onto {@code survivor} — appointments,
     * employee assignments, waiting-list entries, and every package's coverage — then
     * deletes the duplicate. {@code coveredOptions} is a many-to-many the option side does
     * not own, so it is fixed up by editing each package's own set rather than a join-table
     * query; a package that already covered both the duplicate and the survivor keeps a
     * single reference to the survivor instead of gaining a second one.
     */
    private void mergeServiceOptionInto(ServiceOption duplicate, ServiceOption survivor) {
        Integer duplicateId = duplicate.getOptionId();
        Integer survivorId = survivor.getOptionId();

        appointmentRepository.findByServiceOptionOptionId(duplicateId)
                .forEach(appointment -> {
                    appointment.setServiceOption(survivor);
                    appointmentRepository.save(appointment);
                });
        employeeServiceRepository.findByServiceOptionOptionId(duplicateId)
                .forEach(assignment -> {
                    assignment.setServiceOption(survivor);
                    employeeServiceRepository.save(assignment);
                });
        waitingListRepository.findByServiceOptionOptionId(duplicateId)
                .forEach(entry -> {
                    entry.setServiceOption(survivor);
                    waitingListRepository.save(entry);
                });
        servicePackageRepository.findAll().forEach(servicePackage -> {
            Set<ServiceOption> covered = servicePackage.getCoveredOptions();
            boolean coveredDuplicate = covered.stream().anyMatch(option -> option.getOptionId().equals(duplicateId));
            if (!coveredDuplicate) {
                return;
            }
            boolean alreadyCoveredSurvivor = covered.stream().anyMatch(option -> option.getOptionId().equals(survivorId));
            covered.removeIf(option -> option.getOptionId().equals(duplicateId));
            if (!alreadyCoveredSurvivor) {
                covered.add(survivor);
            }
            servicePackageRepository.save(servicePackage);
        });

        serviceOptionRepository.delete(duplicate);
    }

    /** {@code coveredOptions} is authoritative: it is what a customer may actually book
     * this package for (see docs/PACKAGE_SERVICE_COVERAGE.md), independent of how many
     * other sub-services its {@code service} category happens to have. */
    private void createPackage(Service service, String name, String description, Integer sessions,
                                Double price, List<ServiceOption> coveredOptions) {
        Set<ServiceOption> covered = new LinkedHashSet<>(coveredOptions);
        List<ServicePackage> matches = servicePackageRepository.findByServiceServiceId(service.getServiceId()).stream()
                .filter(servicePackage -> canonicalName(name).equals(canonicalName(servicePackage.getPackageName())))
                .sorted(Comparator.comparing(ServicePackage::getPackageId))
                .toList();

        ServicePackage survivor;
        if (matches.isEmpty()) {
            survivor = ServicePackage.builder()
                    .service(service)
                    .packageName(name)
                    .build();
        } else {
            survivor = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                mergeServicePackageInto(matches.get(i), survivor);
            }
        }

        survivor.setService(service);
        survivor.setPackageName(name);
        survivor.setDescription(description);
        survivor.setTotalSession(sessions);
        survivor.setPrice(price);
        survivor.setValidityDays(365);
        survivor.setActive(true);
        survivor.setCoveredOptions(covered);
        servicePackageRepository.save(survivor);
    }

    /**
     * Re-points every owned copy ({@code CustomerPackage}) onto {@code survivor}, clears the
     * duplicate's own coverage so its join rows are removed before the row itself is deleted
     * (the survivor's coverage is set explicitly by the caller right after this returns), and
     * deletes the duplicate.
     */
    private void mergeServicePackageInto(ServicePackage duplicate, ServicePackage survivor) {
        customerPackageRepository.findByServicePackagePackageId(duplicate.getPackageId())
                .forEach(customerPackage -> {
                    customerPackage.setServicePackage(survivor);
                    customerPackageRepository.save(customerPackage);
                });

        duplicate.setCoveredOptions(new LinkedHashSet<>());
        servicePackageRepository.save(duplicate);
        servicePackageRepository.delete(duplicate);
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

    private void assignAll(Employee employee, List<Service> services) {
        services.forEach(service -> {
            if (!employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(employee.getEmployeeId(), service.getServiceId())) {
                employeeServiceRepository.save(EmployeeService.builder()
                        .employee(employee)
                        .service(service)
                        .build());
            }
        });
    }

    /**
     * Keeps the lowest-id assignment for every distinct (employee, service, option) triple
     * and removes the rest. A duplicate pair here is a side effect of merging duplicate
     * services/options above (an employee could have been separately assigned to both a
     * duplicate and its survivor before they were merged) — it is not itself a
     * service/option duplicate, so it is cleaned up separately, after every merge above has
     * already run.
     */
    private void deduplicateEmployeeServiceAssignments() {
        List<EmployeeService> all = employeeServiceRepository.findAll().stream()
                .sorted(Comparator.comparing(EmployeeService::getEmployeeServiceId))
                .toList();
        Set<String> seenKeys = new HashSet<>();
        for (EmployeeService assignment : all) {
            String key = assignment.getEmployee().getEmployeeId()
                    + "|" + assignment.getService().getServiceId()
                    + "|" + (assignment.getServiceOption() == null ? "" : assignment.getServiceOption().getOptionId());
            if (!seenKeys.add(key)) {
                employeeServiceRepository.delete(assignment);
            }
        }
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
