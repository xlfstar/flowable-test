package com.example.flowable.service;

import com.example.flowable.dto.LoginRequest;
import com.example.flowable.dto.LoginResponse;
import com.example.flowable.dto.UserInfo;
import com.example.flowable.entity.SysUser;
import com.example.flowable.security.JwtUtil;
import com.example.flowable.security.LoginUser;
import com.example.flowable.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final OrganizationService organizationService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtil.generateToken(loginUser.getUserId(), loginUser.getUsername(), loginUser.getRole());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(toUserInfo(loginUser.getUser()))
                .build();
    }

    public UserInfo currentUser() {
        return toUserInfo(SecurityUtils.getLoginUser().getUser());
    }

    public UserInfo toUserInfo(SysUser user) {
        if (user == null) {
            return null;
        }
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .deptId(user.getDeptId())
                .deptName(organizationService.departmentName(user.getDeptId()))
                .positionId(user.getPositionId())
                .positionName(organizationService.positionName(user.getPositionId()))
                .email(user.getEmail())
                .build();
    }
}
