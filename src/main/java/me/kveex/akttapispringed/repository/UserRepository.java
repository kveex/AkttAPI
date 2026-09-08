package me.kveex.akttapispringed.repository;

import jakarta.validation.constraints.NotBlank;
import me.kveex.akttapispringed.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(@NotBlank String login);
}
