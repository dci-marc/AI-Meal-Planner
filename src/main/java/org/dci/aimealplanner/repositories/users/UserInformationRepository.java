package org.dci.aimealplanner.repositories.users;

import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserInformationRepository extends JpaRepository<UserInformation, Long> {
    Optional<UserInformation> findByUser(User user);
    List<UserInformation> findByUser_IdIn(Collection<Long> userIds);
}
