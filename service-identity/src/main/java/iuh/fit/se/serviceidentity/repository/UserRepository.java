package iuh.fit.se.serviceidentity.repository;

import iuh.fit.se.serviceidentity.entity.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
 * Check whether a user with the specified email exists.
 *
 * @param email the email address to look up
 * @return `true` if a user with the specified email exists, `false` otherwise
 */
boolean existsByEmail(String email);
    /**
 * Retrieves a user by their email address.
 *
 * @param email the user's email address to search for
 * @return an {@link Optional} containing the {@link User} with the given email if found, otherwise {@link Optional#empty()}
 */
Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(String keyword, Pageable pageable);
}