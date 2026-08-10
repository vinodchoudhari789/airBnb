package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.ProfileUpdateRequestDTO;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.project.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+id));
    }

    @Override
    public void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO) {
        User user = getCurrentUser();
        log.info("Updating profile of user : {}",user.getName());

        if(profileUpdateRequestDTO.getName() != null){
            user.setName(profileUpdateRequestDTO.getName());
        }
        if(profileUpdateRequestDTO.getDateOfBirth() != null){
            user.setDateOfBirth(profileUpdateRequestDTO.getDateOfBirth());
        }
        if(profileUpdateRequestDTO.getGender() != null){
            user.setGender(profileUpdateRequestDTO.getGender());
        }

        userRepository.save(user);

        log.info("Successfully updated profile of user : {}",user.getName());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElse(null);
    }
}
