package glowbook.service;

import glowbook.entity.Holiday;
import glowbook.exception.BusinessException;
import glowbook.exception.ResourceNotFoundException;
import glowbook.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public List<Holiday> getHolidays(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate);
    }

    public boolean isHoliday(LocalDate holidayDate) {
        return holidayRepository.existsByHolidayDate(holidayDate);
    }

    public Holiday getById(Integer holidayId) {
        return holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Tatil günü bulunamadı."));
    }

    @Transactional
    public Holiday create(Holiday holiday) {
        if (isHoliday(holiday.getHolidayDate())) {
            throw new BusinessException("Bu tarih için tatil zaten tanımlı.");
        }
        return holidayRepository.save(holiday);
    }

    @Transactional
    public Holiday update(Integer holidayId, Holiday request) {
        Holiday holiday = getById(holidayId);

        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setHolidayName(request.getHolidayName());
        holiday.setDescription(request.getDescription());

        return holidayRepository.save(holiday);
    }

    @Transactional
    public void delete(Integer holidayId) {
        holidayRepository.delete(getById(holidayId));
    }
}
