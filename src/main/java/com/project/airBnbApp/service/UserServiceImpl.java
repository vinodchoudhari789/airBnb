package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+id));
    }
}
