package glowbook.config;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.Employee;
import glowbook.entity.EmployeeService;
import glowbook.entity.Service;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.entity.WaitingList;
import glowbook.entity.WaitingListStatus;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.ServiceRepository;
import glowbook.repository.WaitingListRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link CatalogSeedConfig}'s duplicate-cleanup mechanics directly (in-package,
 * since {@code seedCatalogData()} is intentionally package-private — it is startup-only
 * machinery, never a public API). Every scenario simulates the exact kind of Turkish-
 * spelling drift the real production database had accumulated: a wrongly-spelled row that
 * pre-dates canonical-name matching, still holding live foreign-key references, sitting
 * alongside (or instead of) the correctly-spelled row this seed is authoritative for.
 */
@SpringBootTest
class CatalogSeedConfigDeduplicationTest {

    @Autowired CatalogSeedConfig catalogSeedConfig;
    @Autowired ServiceRepository serviceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired ServicePackageRepository servicePackageRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired WaitingListRepository waitingListRepository;
    @Autowired CustomerPackageRepository customerPackageRepository;
    @Autowired CustomerRepository customerRepository;

    // A. Duplicate SERVICE (wrong Turkish spelling) with live appointment/assignment/
    // waiting-list references is merged into the correctly-spelled survivor; nothing
    // referencing it is lost.
    @Test
    @Transactional
    void duplicateServiceIsMergedAndEveryReferenceIsRepointed() {
        Service canonicalBrow = serviceRepository.findAll().stream()
                .filter(service -> "Kaş ve Kirpik".equals(service.getServiceName()))
                .findFirst().orElseThrow();

        Service wrongBrow = serviceRepository.save(Service.builder()
                .serviceName("Kas ve Kirpik")
                .description("eski kayit")
                .serviceImage("")
                .active(true)
                .build());
        ServiceOption wrongOption = serviceOptionRepository.save(ServiceOption.builder()
                .service(wrongBrow).optionName("Kas Alimi").price(350.0).active(true).build());

        Employee employee = employeeRepository.findById("GLW001").orElseThrow();
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .employee(employee)
                .service(wrongBrow)
                .serviceOption(wrongOption)
                .customerName("Test").customerSurname("Musteri").phone("05551234567")
                .appointmentDate(LocalDate.now().plusDays(10))
                .appointmentTime(LocalTime.of(11, 0))
                .price(java.math.BigDecimal.valueOf(350))
                .status(AppointmentStatus.PENDING)
                .build());
        EmployeeService assignment = employeeServiceRepository.save(EmployeeService.builder()
                .employee(employee).service(wrongBrow).build());
        WaitingList waiting = waitingListRepository.save(WaitingList.builder()
                .service(wrongBrow).serviceOption(wrongOption)
                .customerName("Bekleyen").customerSurname("Musteri").phone("05559876543")
                .preferredDate(LocalDate.now().plusDays(5))
                .status(WaitingListStatus.ACTIVE)
                .build());

        long appointmentsBefore = appointmentRepository.count();
        long waitingBefore = waitingListRepository.count();

        catalogSeedConfig.seedCatalogData();

        // Exactly one "Kaş ve Kirpik" survives, with the correct spelling.
        List<Service> browRows = serviceRepository.findAll().stream()
                .filter(service -> service.getServiceName() != null
                        && service.getServiceName().replace('ş', 's').replace('Ş', 'S')
                                .equalsIgnoreCase("Kas ve Kirpik"))
                .toList();
        assertThat(browRows).hasSize(1);
        assertThat(browRows.get(0).getServiceName()).isEqualTo("Kaş ve Kirpik");
        assertThat(serviceRepository.findById(wrongBrow.getServiceId())).isEmpty();

        // Nothing that referenced the duplicate was lost — it now points at the survivor.
        assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore);
        Appointment reloadedAppointment = appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow();
        assertThat(reloadedAppointment.getService().getServiceId()).isEqualTo(canonicalBrow.getServiceId());
        assertThat(reloadedAppointment.getServiceOption().getOptionName()).isEqualTo("Kaş Alımı");

        // The assignment itself is preserved as a fact (this employee can perform this
        // service) even though the specific row created in this test was, correctly, the
        // one collapsed by deduplicateEmployeeServiceAssignments() — GLW001 was already
        // assigned to "Kaş ve Kirpik" by the initial automatic seed run, and re-pointing
        // this test's row onto the same survivor made the two rows genuine duplicates of
        // each other, not of the merge under test.
        assertThat(employeeServiceRepository.existsByEmployeeEmployeeIdAndServiceServiceId(
                employee.getEmployeeId(), canonicalBrow.getServiceId())).isTrue();

        assertThat(waitingListRepository.count()).isEqualTo(waitingBefore);
        assertThat(waitingListRepository.findById(waiting.getWaitingListId()).orElseThrow()
                .getService().getServiceId()).isEqualTo(canonicalBrow.getServiceId());
    }

    // B. Duplicate OPTION under an already-canonical service: a package that ends up
    // covering both the duplicate and the survivor keeps only one reference, not two.
    @Test
    @Transactional
    void duplicateOptionIsMergedAndPackageCoverageStaysDeduplicated() {
        Service brow = serviceRepository.findAll().stream()
                .filter(service -> "Kaş ve Kirpik".equals(service.getServiceName()))
                .findFirst().orElseThrow();
        ServiceOption canonicalOption = serviceOptionRepository.findByServiceServiceId(brow.getServiceId()).stream()
                .filter(option -> "Kaş Alımı".equals(option.getOptionName()))
                .findFirst().orElseThrow();

        ServiceOption wrongOption = serviceOptionRepository.save(ServiceOption.builder()
                .service(brow).optionName("Kas Alimi").price(350.0).active(true).build());

        Employee employee = employeeRepository.findById("GLW001").orElseThrow();
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .employee(employee)
                .service(brow)
                .serviceOption(wrongOption)
                .customerName("Test").customerSurname("Musteri").phone("05551234568")
                .appointmentDate(LocalDate.now().plusDays(11))
                .appointmentTime(LocalTime.of(12, 0))
                .price(java.math.BigDecimal.valueOf(350))
                .status(AppointmentStatus.PENDING)
                .build());

        // An admin-created custom package outside the authoritative seed list — the seed
        // never rewrites its coverage, so this specifically exercises the join-table
        // cleanup in mergeServiceOptionInto rather than being masked by createPackage()
        // resetting coveredOptions afterwards (as it would for every package this seed
        // actually manages). Deliberately covers both spellings of the same option at
        // once, as historical drift could have left it.
        ServicePackage customPackage = servicePackageRepository.save(ServicePackage.builder()
                .service(brow)
                .packageName("Ozel Kas Paketi")
                .description("Salonun kendi ekledigi ozel paket.")
                .totalSession(3)
                .price(1000.0)
                .validityDays(180)
                .active(true)
                .coveredOptions(new LinkedHashSet<>(Set.of(canonicalOption, wrongOption)))
                .build());

        catalogSeedConfig.seedCatalogData();

        assertThat(serviceOptionRepository.findById(wrongOption.getOptionId())).isEmpty();
        List<ServiceOption> options = serviceOptionRepository.findByServiceServiceId(brow.getServiceId()).stream()
                .filter(option -> "Kaş Alımı".equals(option.getOptionName()))
                .toList();
        assertThat(options).hasSize(1);

        Appointment reloaded = appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow();
        assertThat(reloaded.getServiceOption().getOptionId()).isEqualTo(canonicalOption.getOptionId());

        // The unrelated custom package was left alone (still exists, still named the same)
        // but its coverage no longer references the now-deleted duplicate option, and does
        // not reference the survivor twice either.
        ServicePackage reloadedPackage = servicePackageRepository.findById(customPackage.getPackageId()).orElseThrow();
        assertThat(reloadedPackage.getPackageName()).isEqualTo("Ozel Kas Paketi");
        long survivorReferences = reloadedPackage.getCoveredOptions().stream()
                .filter(option -> option.getOptionId().equals(canonicalOption.getOptionId()))
                .count();
        assertThat(survivorReferences).isEqualTo(1);
    }

    // C. Duplicate PACKAGE with an owned CustomerPackage: the owned copy survives, now
    // pointing at the correctly-spelled package, never deleted.
    @Test
    @Transactional
    void duplicatePackageIsMergedAndOwnedCopySurvives() {
        Service slimming = serviceRepository.findAll().stream()
                .filter(service -> "Bölgesel İncelme".equals(service.getServiceName()))
                .findFirst().orElseThrow();
        ServicePackage canonicalPackage = servicePackageRepository.findByServiceServiceId(slimming.getServiceId()).stream()
                .filter(item -> "Sıkılaşma Paketi".equals(item.getPackageName()))
                .findFirst().orElseThrow();

        ServicePackage wrongPackage = servicePackageRepository.save(ServicePackage.builder()
                .service(slimming)
                .packageName("Sikilasma Paketi")
                .description("eski kayit")
                .totalSession(8)
                .price(9800.0)
                .validityDays(365)
                .active(true)
                .coveredOptions(new LinkedHashSet<>())
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Paket").lastName("Sahibi").phone("05557778899")
                .password("irrelevant").active(true).build());
        CustomerPackage owned = customerPackageRepository.save(CustomerPackage.builder()
                .customer(customer)
                .servicePackage(wrongPackage)
                .remainingSession(8)
                .purchasePrice(9800.0)
                .purchaseDate(LocalDate.now())
                .active(true)
                .build());

        long customerPackagesBefore = customerPackageRepository.count();

        catalogSeedConfig.seedCatalogData();

        assertThat(servicePackageRepository.findById(wrongPackage.getPackageId())).isEmpty();
        assertThat(customerPackageRepository.count()).isEqualTo(customerPackagesBefore);
        CustomerPackage reloaded = customerPackageRepository.findById(owned.getCustomerPackageId()).orElseThrow();
        assertThat(reloaded.getServicePackage().getPackageId()).isEqualTo(canonicalPackage.getPackageId());
        assertThat(reloaded.getServicePackage().getPackageName()).isEqualTo("Sıkılaşma Paketi");
    }

    // D. No duplicate exists — a single row with drifted spelling is fixed in place; its id
    // (and everything that already referenced it) is untouched.
    @Test
    @Transactional
    void singleMisspelledRowIsFixedInPlaceWithoutChangingItsId() {
        Service slimming = serviceRepository.findAll().stream()
                .filter(service -> "Bölgesel İncelme".equals(service.getServiceName()))
                .findFirst().orElseThrow();
        Integer originalId = slimming.getServiceId();
        slimming.setServiceName("Bolgesel Incelme");
        serviceRepository.save(slimming);

        catalogSeedConfig.seedCatalogData();

        List<Service> matches = serviceRepository.findAll().stream()
                .filter(service -> service.getServiceId().equals(originalId))
                .toList();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getServiceName()).isEqualTo("Bölgesel İncelme");

        long totalWithThatBusinessIdentity = serviceRepository.findAll().stream()
                .filter(service -> normalizedTr(service.getServiceName()).equals("bolgesel incelme"))
                .count();
        assertThat(totalWithThatBusinessIdentity).isEqualTo(1);
    }

    // E. Idempotency — seeding repeatedly never grows the catalog and never throws.
    @Test
    @Transactional
    void reseedingRepeatedlyIsANoOp() {
        long servicesBefore = serviceRepository.count();
        long optionsBefore = serviceOptionRepository.count();
        long packagesBefore = servicePackageRepository.count();
        long assignmentsBefore = employeeServiceRepository.count();

        catalogSeedConfig.seedCatalogData();
        catalogSeedConfig.seedCatalogData();
        catalogSeedConfig.seedCatalogData();

        assertThat(serviceRepository.count()).isEqualTo(servicesBefore);
        assertThat(serviceOptionRepository.count()).isEqualTo(optionsBefore);
        assertThat(servicePackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(employeeServiceRepository.count()).isEqualTo(assignmentsBefore);
    }

    // F. A service with no counterpart in the authoritative seed list — an admin-created,
    // genuinely different offering — is never touched, merged, renamed, or removed.
    @Test
    @Transactional
    void unrelatedCustomServiceIsNeverTouched() {
        Service custom = serviceRepository.save(Service.builder()
                .serviceName("Ozel Danismanlik Hizmeti")
                .description("Salonun kendi ekledigi ozel bir hizmet.")
                .serviceImage("")
                .active(true)
                .build());
        long servicesBefore = serviceRepository.count();

        catalogSeedConfig.seedCatalogData();

        assertThat(serviceRepository.count()).isEqualTo(servicesBefore);
        Service reloaded = serviceRepository.findById(custom.getServiceId()).orElseThrow();
        assertThat(reloaded.getServiceName()).isEqualTo("Ozel Danismanlik Hizmeti");
        assertThat(reloaded.getDescription()).isEqualTo("Salonun kendi ekledigi ozel bir hizmet.");
    }

    // G. Employee display-name drift is fixed in place; the id (and any appointment
    // already referencing it) is completely unaffected — employees are matched by their
    // stable id, never merged.
    @Test
    @Transactional
    void employeeDisplayNameIsFixedInPlaceWithoutTouchingId() {
        Employee employee = employeeRepository.findById("GLW001").orElseThrow();
        employee.setFirstName("Defne");
        employee.setLastName("Yilmaz");
        employeeRepository.save(employee);

        Service skinCare = serviceRepository.findAll().stream()
                .filter(service -> "Cilt Bakımı".equals(service.getServiceName()))
                .findFirst().orElseThrow();
        ServiceOption anyOption = serviceOptionRepository.findByServiceServiceId(skinCare.getServiceId()).get(0);
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .employee(employee)
                .service(skinCare)
                .serviceOption(anyOption)
                .customerName("Test").customerSurname("Musteri").phone("05551234569")
                .appointmentDate(LocalDate.now().plusDays(12))
                .appointmentTime(LocalTime.of(13, 0))
                .price(java.math.BigDecimal.valueOf(900))
                .status(AppointmentStatus.PENDING)
                .build());

        catalogSeedConfig.seedCatalogData();

        Employee reloaded = employeeRepository.findById("GLW001").orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo("Defne");
        assertThat(reloaded.getLastName()).isEqualTo("Yılmaz");

        Appointment reloadedAppointment = appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow();
        assertThat(reloadedAppointment.getEmployee().getEmployeeId()).isEqualTo("GLW001");
    }

    // H. Duplicate employee-service assignment rows (the same employee, service and option
    // assigned twice — a side effect earlier drift could produce) collapse to one row.
    @Test
    @Transactional
    void duplicateEmployeeServiceAssignmentCollapsesToOne() {
        Employee employee = employeeRepository.findById("GLW002").orElseThrow();
        Service laser = serviceRepository.findAll().stream()
                .filter(service -> "Lazer Epilasyon".equals(service.getServiceName()))
                .findFirst().orElseThrow();

        long before = employeeServiceRepository.findAll().stream()
                .filter(a -> a.getEmployee().getEmployeeId().equals("GLW002")
                        && a.getService().getServiceId().equals(laser.getServiceId())
                        && a.getServiceOption() == null)
                .count();
        assertThat(before).isEqualTo(1);

        employeeServiceRepository.save(EmployeeService.builder().employee(employee).service(laser).build());
        long afterDuplicateInsert = employeeServiceRepository.findAll().stream()
                .filter(a -> a.getEmployee().getEmployeeId().equals("GLW002")
                        && a.getService().getServiceId().equals(laser.getServiceId())
                        && a.getServiceOption() == null)
                .count();
        assertThat(afterDuplicateInsert).isEqualTo(2);

        catalogSeedConfig.seedCatalogData();

        long afterCleanup = employeeServiceRepository.findAll().stream()
                .filter(a -> a.getEmployee().getEmployeeId().equals("GLW002")
                        && a.getService().getServiceId().equals(laser.getServiceId())
                        && a.getServiceOption() == null)
                .count();
        assertThat(afterCleanup).isEqualTo(1);
    }

    private String normalizedTr(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ı', 'i').replace('İ', 'I')
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
