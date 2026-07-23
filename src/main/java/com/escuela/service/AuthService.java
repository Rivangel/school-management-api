package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.model.dto.AuthResponse;
import com.escuela.model.dto.LoginRequest;
import com.escuela.model.dto.RegisterRequest;
import com.escuela.model.entity.Usuario;
import com.escuela.repository.UsuarioRepository;
import com.escuela.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre(),
                usuario.getRol().name());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Ya existe un usuario con el email " + request.email());
        }

        Usuario usuario = new Usuario(
                request.nombre(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.rol());
        usuarioRepository.save(usuario);

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre(),
                usuario.getRol().name());
    }
}
