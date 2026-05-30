package ru.studymarket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.studymarket.domain.UserAccount;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
