package glowbook.repository;

import glowbook.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, Integer> {

    Optional<WorkingHour> findByDayOfWeek(DayOfWeek dayOfWeek);
}
