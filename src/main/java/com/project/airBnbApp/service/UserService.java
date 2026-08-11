package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.ProfileUpdateRequestDTO;
import com.project.airBnbApp.dto.UserDTO;
import com.project.airBnbApp.entity.User;
import org.jspecify.annotations.Nullable;

public interface UserService{

    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO);

    UserDTO getMyProfile();
}
