package me.kveex.akttapispringed.repository;

import me.kveex.akttapispringed.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> getGroupByName(String name);
}
