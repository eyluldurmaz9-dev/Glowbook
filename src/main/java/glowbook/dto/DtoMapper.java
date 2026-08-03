package glowbook.dto;

import glowbook.entity.*;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static CustomerDtos.CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerDtos.CustomerResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getActive(),
                customer.getCreatedAt()
        );
    }

    public static EmployeeDtos.EmployeeResponse toEmployeeResponse(Employee employee) {
        return new EmployeeDtos.EmployeeResponse(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getActive()
        );
    }

    public static CatalogDtos.ServiceResponse toServiceResponse(glowbook.entity.Service service) {
        return new CatalogDtos.ServiceResponse(
                service.getServiceId(),
                service.getServiceName(),
                service.getDescription(),
                service.getServiceImage(),
                service.getActive()
        );
    }

    public static CatalogDtos.ServiceOptionResponse toServiceOptionResponse(ServiceOption option) {
        return new CatalogDtos.ServiceOptionResponse(
                option.getOptionId(),
                option.getService().getServiceId(),
                option.getOptionName(),
                option.getPrice(),
                option.getActive()
        );
    }

    public static CatalogDtos.ServicePackageResponse toServicePackageResponse(ServicePackage servicePackage) {
        return new CatalogDtos.ServicePackageResponse(
                servicePackage.getPackageId(),
                servicePackage.getService().getServiceId(),
                servicePackage.getService().getServiceName(),
                servicePackage.getPackageName(),
                servicePackage.getDescription(),
                servicePackage.getTotalSession(),
                servicePackage.getPrice(),
                servicePackage.getPackageImage(),
                servicePackage.getActive()
        );
    }

    public static CustomerDtos.CustomerPackageResponse toCustomerPackageResponse(CustomerPackage customerPackage) {
        return new CustomerDtos.CustomerPackageResponse(
                customerPackage.getCustomerPackageId(),
                customerPackage.getCustomer().getCustomerId(),
                customerPackage.getServicePackage().getPackageId(),
                customerPackage.getServicePackage().getPackageName(),
                customerPackage.getRemainingSession(),
                customerPackage.getPurchasePrice(),
                customerPackage.getPurchaseDate(),
                customerPackage.getActive()
        );
    }

    public static EmployeeDtos.EmployeeServiceResponse toEmployeeServiceResponse(EmployeeService employeeService) {
        Employee employee = employeeService.getEmployee();
        glowbook.entity.Service service = employeeService.getService();
        return new EmployeeDtos.EmployeeServiceResponse(
                employeeService.getEmployeeServiceId(),
                employee.getEmployeeId(),
                employee.getFirstName() + " " + employee.getLastName(),
                service.getServiceId(),
                service.getServiceName()
        );
    }

    public static EmployeeDtos.EmployeeLeaveResponse toEmployeeLeaveResponse(EmployeeLeave leave) {
        return new EmployeeDtos.EmployeeLeaveResponse(
                leave.getLeaveId(),
                leave.getEmployee().getEmployeeId(),
                leave.getLeaveDate(),
                leave.getReason()
        );
    }

    public static ScheduleDtos.WorkingHourResponse toWorkingHourResponse(WorkingHour workingHour) {
        return new ScheduleDtos.WorkingHourResponse(
                workingHour.getWorkingHourId(),
                workingHour.getDayOfWeek(),
                workingHour.getStartTime(),
                workingHour.getEndTime(),
                workingHour.getClosed()
        );
    }

    public static ScheduleDtos.HolidayResponse toHolidayResponse(Holiday holiday) {
        return new ScheduleDtos.HolidayResponse(
                holiday.getHolidayId(),
                holiday.getHolidayDate(),
                holiday.getHolidayName(),
                holiday.getDescription()
        );
    }

    public static AppointmentDtos.AppointmentResponse toAppointmentResponse(Appointment appointment) {
        return new AppointmentDtos.AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getCustomer() == null ? null : appointment.getCustomer().getCustomerId(),
                appointment.getCustomerPackage() == null ? null : appointment.getCustomerPackage().getCustomerPackageId(),
                appointment.getEmployee().getEmployeeId(),
                appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName(),
                appointment.getService().getServiceId(),
                appointment.getService().getServiceName(),
                appointment.getServiceOption().getOptionId(),
                appointment.getServiceOption().getOptionName(),
                appointment.getCustomerName(),
                appointment.getCustomerSurname(),
                appointment.getPhone(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getPrice(),
                appointment.getStatus(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    public static NotificationDtos.NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getNotificationId(),
                notification.getCustomer() == null ? null : notification.getCustomer().getCustomerId(),
                notification.getAppointment() == null ? null : notification.getAppointment().getAppointmentId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRead(),
                notification.getSmsSent(),
                notification.getCreatedAt()
        );
    }

    public static WaitingListDtos.WaitingListResponse toWaitingListResponse(WaitingList waitingList) {
        return new WaitingListDtos.WaitingListResponse(
                waitingList.getWaitingListId(),
                waitingList.getCustomer() == null ? null : waitingList.getCustomer().getCustomerId(),
                waitingList.getService().getServiceId(),
                waitingList.getService().getServiceName(),
                waitingList.getServiceOption().getOptionId(),
                waitingList.getServiceOption().getOptionName(),
                waitingList.getCustomerName(),
                waitingList.getCustomerSurname(),
                waitingList.getPhone(),
                waitingList.getPreferredDate(),
                waitingList.getPreferredStartTime(),
                waitingList.getPreferredEndTime(),
                waitingList.getStatus(),
                waitingList.getCreatedAt()
        );
    }
}
