package com.project.airBnbApp.security;

import com.project.airBnbApp.dto.LoginDTO;
import com.project.airBnbApp.dto.SingUpRequestDTO;
import com.project.airBnbApp.dto.UserDTO;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.Role;
import com.project.airBnbApp.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDTO signUp(SingUpRequestDTO singUpRequestDTO){
        log.info("Inside signUp service");
        Optional<User> user = userRepository.findByEmail(singUpRequestDTO.getEmail());
        if(user.isPresent()){
            throw new RuntimeException("User with email id : "+singUpRequestDTO.getEmail()+ "already exists!!!");
        }
        log.info("User not present!, Creating new user");

        User newUser = modelMapper.map(singUpRequestDTO, User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser = userRepository.save(newUser);
        log.info("Saved User : {}", newUser);
        return modelMapper.map(newUser, UserDTO.class);
    }

    public String[] login(LoginDTO loginDTO){
        log.info("Inside login service");
         Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(),loginDTO.getPassword()
            )
         );
         log.info("User Authenticated");
         User user = (User) authentication.getPrincipal();

         log.info("Starting token generation");
         String tokenArr[] = new String[2];
         tokenArr[0] = jwtService.generateAccessToken(user);
         tokenArr[1] = jwtService.generateRefreshToken(user);
         log.info("Login Completed!!!");
         return tokenArr;
    }
}
