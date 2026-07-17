package com.flashflow.auth.security;

import com.flashflow.auth.dao.UserInfoMapper;
import com.flashflow.auth.entity.UserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：兼容管理员（sys_user）和 C端用户（user_info）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserInfoMapper userInfoMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getClaimFromToken(token, "role");
                String username = jwtTokenProvider.getUsernameFromToken(token);

                UserDetails userDetails;
                if ("ROLE_ADMIN".equals(role)) {
                    // 管理员：从 sys_user 加载
                    userDetails = customUserDetailsService.loadUserById(userId);
                } else {
                    // C端用户：从 user_info 加载
                    UserInfo user = userInfoMapper.selectById(userId);
                    if (user == null || user.getStatus() != 1) {
                        throw new RuntimeException("用户不存在或已禁用");
                    }
                    userDetails = new LoginUser(
                            user.getId(),
                            user.getEmail() != null ? user.getEmail() : "",
                            user.getPassword(),
                            role != null ? role : "ROLE_USER",
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                            user.getStatus() == 1
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.warn("JWT 认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
