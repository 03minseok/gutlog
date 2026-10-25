package com.gutlog.domain.user.repository;

import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserRepository 슬라이스 테스트.
 * JPA 관련 빈만 띄우고 인메모리 H2에서 실제 쿼리/제약조건을 확인한다. 각 테스트는 끝나면 롤백된다.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    private User newUser(String username, String nickname) {
        return User.create(username, "ENCODED", nickname, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
    }

    @Test
    @DisplayName("저장하면 id와 생성/수정 시각(JPA Auditing)이 채워진다")
    void save() {
        User saved = userRepository.saveAndFlush(newUser("gutlog", "장건강"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByUsername - 존재 여부를 정확히 반환한다")
    void existsByUsername() {
        userRepository.save(newUser("gutlog", "장건강"));

        assertThat(userRepository.existsByUsername("gutlog")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    @DisplayName("findByUsername - 아이디로 조회한다")
    void findByUsername() {
        userRepository.save(newUser("gutlog", "장건강"));

        assertThat(userRepository.findByUsername("gutlog"))
                .get()
                .extracting(User::getNickname)
                .isEqualTo("장건강");
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("username은 unique 제약이 걸려 있다")
    void usernameUnique() {
        userRepository.saveAndFlush(newUser("gutlog", "닉1"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("gutlog", "닉2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("nickname도 unique 제약이 걸려 있다")
    void nicknameUnique() {
        userRepository.saveAndFlush(newUser("user1", "장건강"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("user2", "장건강")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
