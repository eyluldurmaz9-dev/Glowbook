package glowbook.service;

import glowbook.entity.WorkingHour;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.WorkingHourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkingHourService {

    private final WorkingHourRepository workingHourRepository;

    public List<WorkingHour> getAllWorkingHours() {
        return workingHourRepository.findAll();
    }

    public WorkingHour getByDay(DayOfWeek dayOfWeek) {
        return workingHourRepository.findByDayOfWeek(dayOfWeek)
                .orElseThrow(() -> new ResourceNotFoundException("Working hour not found for day: " + dayOfWeek));
    }

    public boolean isWorkingTime(DayOfWeek dayOfWeek, LocalTime time) {
        return workingHourRepository.findByDayOfWeek(dayOfWeek)
                .filter(workingHour -> !Boolean.TRUE.equals(workingHour.getClosed()))
                .filter(workingHour -> !time.isBefore(workingHour.getStartTime()))
                .filter(workingHour -> time.isBefore(workingHour.getEndTime()))
                .isPresent();
    }

    @Transactional
    public WorkingHour create(WorkingHour workingHour) {
        workingHourRepository.findByDayOfWeek(workingHour.getDayOfWeek())
                .ifPresent(existing -> {
                    throw new BusinessException("Working hour already exists for day: " + workingHour.getDayOfWeek());
                });
        return workingHourRepository.save(workingHour);
    }

    @Transactional
    public WorkingHour update(Integer workingHourId, WorkingHour request) {
        WorkingHour workingHour = workingHourRepository.findById(workingHourId)
                .orElseThrow(() -> new ResourceNotFoundException("Working hour not found: " + workingHourId));

        workingHour.setDayOfWeek(request.getDayOfWeek());
        workingHour.setStartTime(request.getStartTime());
        workingHour.setEndTime(request.getEndTime());
        workingHour.setClosed(request.getClosed());

        return workingHourRepository.save(workingHour);
    }
}
