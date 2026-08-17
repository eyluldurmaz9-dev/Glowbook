package glowbook.service;

import glowbook.entity.EmployeeLeave;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.EmployeeLeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLeaveService {

    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final EmployeeManagementService employeeManagementService;

    public List<EmployeeLeave> getLeaves(String employeeId, LocalDate startDate, LocalDate endDate) {
        return employeeLeaveRepository.findByEmployeeEmployeeIdAndLeaveDateBetween(employeeId, startDate, endDate);
    }

    public boolean isEmployeeOnLeave(String employeeId, LocalDate leaveDate) {
        return employeeLeaveRepository.existsByEmployeeEmployeeIdAndLeaveDate(employeeId, leaveDate);
    }

    public EmployeeLeave getById(Integer leaveId) {
        return employeeLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("İzin kaydı bulunamadı."));
    }

    @Transactional
    public EmployeeLeave create(String employeeId, EmployeeLeave employeeLeave) {
        if (isEmployeeOnLeave(employeeId, employeeLeave.getLeaveDate())) {
            throw new BusinessException("Bu personelin seçilen tarihte zaten izni var.");
        }

        employeeLeave.setEmployee(employeeManagementService.getActiveById(employeeId));
        return employeeLeaveRepository.save(employeeLeave);
    }

    @Transactional
    public EmployeeLeave update(Integer leaveId, EmployeeLeave request) {
        EmployeeLeave employeeLeave = getById(leaveId);

        employeeLeave.setLeaveDate(request.getLeaveDate());
        employeeLeave.setReason(request.getReason());

        return employeeLeaveRepository.save(employeeLeave);
    }

    @Transactional
    public void delete(Integer leaveId) {
        employeeLeaveRepository.delete(getById(leaveId));
    }
}
