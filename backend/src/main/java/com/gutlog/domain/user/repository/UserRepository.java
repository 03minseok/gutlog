package com.gutlog.domain.user.repository;

import com.gutlog.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public boolean existsByUsername(String username);
    public Optional<User> findByUsername(String username);

    /**
     * 사용자 행을 SELECT ... FOR UPDATE로 잠그고 읽는다. 트랜잭션이 끝날 때까지 같은 행을 잠그려는 요청은 기다린다.
     * 보호자의 코드 입력을 한 명씩 처리해 보호자당 환자 2명 제한을 지킬 때 쓴다. (design.md 5.2)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
