package glowbook.repository;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    boolean existsByEmployeeEmployeeIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
            String employeeId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            Collection<AppointmentStatus> statuses
    );

    List<Appointment> findByEmployeeEmployeeIdAndAppointmentDateBetweenAndStatusInOrderByAppointmentDateAscAppointmentTimeAsc(
            String employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<AppointmentStatus> statuses
    );

    List<Appointment> findByCustomerCustomerIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
            Integer customerId,
            LocalDate fromDate
    );

    List<Appointment> findByCustomerCustomerIdAndAppointmentDateLessThanOrderByAppointmentDateDescAppointmentTimeDesc(
            Integer customerId,
            LocalDate beforeDate
    );
}
