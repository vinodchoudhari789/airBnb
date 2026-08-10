package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.ProfileUpdateRequestDTO;
import com.project.airBnbApp.entity.User;

public interface UserService{

    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO);
}
