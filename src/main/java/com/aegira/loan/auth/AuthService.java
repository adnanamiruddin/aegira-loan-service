package com.aegira.loan.auth;

import com.aegira.loan.auth.dto.LoginRequest;
import com.aegira.loan.auth.dto.LoginResponse;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.user.entity.User;
import com.aegira.loan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUtil securityUtil;

    public LoginResponse login(LoginRequest request) {

        
        // User user = userRepository.findByEmail(request.getEmail())
        //         .orElseThrow(() -> new BadRequestException("Invalid credentials"));



        // if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        //     throw new BadRequestException("Invalid credentials");
        // }
        // return new LoginResponse(jwtTokenProvider.generate(user), user.getId(), user.getEmail(), user.getRole());
    



        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if(userOpt.isEmpty()){
            log.warn("event_name=auth_login_failed error_code=BAD_REQUEST");
            throw new BadRequestException("invalid credential");
        }


        User user = userOpt.get();
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("event_name=auth_login_failed user_id={} role={} error_code=BAD_REQUEST", user.getId(), user.getRole());
            throw new BadRequestException("Invalid credentials");
        }



        String token = jwtTokenProvider.generate(user);

        LoginResponse response = new LoginResponse(token , user.getId(), user.getEmail(), user.getRole());

        log.info("event_name=auth_login_success user_id={} role={}", user.getId(), user.getRole());

        return response;

        
    

    
    }




    public LoginResponse me() {
        User user = securityUtil.currentUser();
        return new LoginResponse(null, user.getId(), user.getEmail(), user.getRole());
    }
}
