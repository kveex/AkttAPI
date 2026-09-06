package me.kveex.akttapispringed.domain.entity.schedule;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "lesson_time", nullable = false)
    private String lessonTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_time_type", nullable = false)
    private LessonTimeType lessonTimeType;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private String classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subgroup subgroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "lesson_type")
    private LessonType lessonType;
}
