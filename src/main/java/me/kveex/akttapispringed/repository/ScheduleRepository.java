package me.kveex.akttapispringed.repository;

import me.kveex.akttapispringed.domain.entity.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    boolean existsByEditTimeStamp(LocalDateTime editTimeStamp);
}
