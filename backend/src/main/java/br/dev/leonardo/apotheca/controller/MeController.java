package br.dev.leonardo.apotheca.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.dev.leonardo.apotheca.dto.UserResponse;
import br.dev.leonardo.apotheca.mapper.UserMapper;
import br.dev.leonardo.apotheca.service.UserService;

@RestController
public class MeController {

	private final UserService userService;
	private final UserMapper userMapper;

	public MeController(UserService userService, UserMapper userMapper) {
		this.userService = userService;
		this.userMapper = userMapper;
	}

	@GetMapping("/api/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userMapper.toResponse(userService.getOrCreate(jwt));
	}

}
