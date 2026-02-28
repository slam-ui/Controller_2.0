package com.example.shop.service;

import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  repository;
    private final PasswordEncoder passwordEncoder;

    // Создать (пароль хешируется — безопасно при прямом вызове через API)
    public User create(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        return repository.save(user);
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<User> update(Long id, User newData) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setUsername(newData.getUsername());
                    existing.setEmail(newData.getEmail());
                    // Обновляем пароль только если он передан и не хэширован
                    if (newData.getPassword() != null && !newData.getPassword().startsWith("$2")) {
                        existing.setPassword(passwordEncoder.encode(newData.getPassword()));
                    }
                    return repository.save(existing);
                });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
