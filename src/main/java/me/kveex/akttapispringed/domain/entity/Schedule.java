package me.kveex.akttapispringed.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Schedule {
    @Id
    private Long id;

    @Column(name = "edit_timestamp", nullable = false)
    private LocalDateTime editTimeStamp;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();
}
