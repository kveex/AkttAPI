package me.kveex.akttapispringed.repository;

import me.kveex.akttapispringed.domain.entity.schedule.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
