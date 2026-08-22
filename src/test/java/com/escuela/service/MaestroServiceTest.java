package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.MaestroRequest;
import com.escuela.model.dto.MaestroResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Maestro;
import com.escuela.repository.MaestroRepository;
import com.escuela.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaestroService")
class MaestroServiceTest {

    @Mock
    private MaestroRepository maestroRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MaestroService maestroService;

    private Maestro maestro;
    private MaestroRequest request;

    @BeforeEach
    void setUp() {
        maestro = new Maestro("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
        maestro.setId(1L);
        request = new MaestroRequest("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        private final Pageable primeraPagina = PageRequest.of(0, 2, Sort.by("apellido"));

        @Test
        @DisplayName("devuelve los maestros de la página mapeados a MaestroResponse")
        void devuelveLosMaestrosDeLaPagina() {
            Maestro otro = new Maestro("Jorge", "Salas", "jorge.salas@escuela.com", "Física");
            otro.setId(2L);
            when(maestroRepository.findAll(primeraPagina))
                    .thenReturn(new PageImpl<>(List.of(maestro, otro), primeraPagina, 2));

            PaginaResponse<MaestroResponse> resultado = maestroService.listar(primeraPagina);

            assertThat(resultado.content()).hasSize(2);
            assertThat(resultado.content()).extracting(MaestroResponse::email)
                    .containsExactly("laura.mendoza@escuela.com", "jorge.salas@escuela.com");
            assertThat(resultado.content().get(1).especialidad()).isEqualTo("Física");
        }

        @Test
        @DisplayName("traslada el Pageable recibido al repositorio sin modificarlo")
        void trasladaElPageableAlRepositorio() {
            when(maestroRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(maestro), primeraPagina, 1));

            maestroService.listar(primeraPagina);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(maestroRepository).findAll(captor.capture());
            assertThat(captor.getValue()).isSameAs(primeraPagina);
        }

        @Test
        @DisplayName("expone los metadatos de paginación de la página consultada")
        void exponeLosMetadatosDePaginacion() {
            when(maestroRepository.findAll(primeraPagina))
                    .thenReturn(new PageImpl<>(List.of(maestro, maestro), primeraPagina, 5));

            PaginaResponse<MaestroResponse> resultado = maestroService.listar(primeraPagina);

            assertThat(resultado.page()).isZero();
            assertThat(resultado.size()).isEqualTo(2);
            assertThat(resultado.totalElements()).isEqualTo(5);
            assertThat(resultado.totalPages()).isEqualTo(3);
            assertThat(resultado.first()).isTrue();
            assertThat(resultado.last()).isFalse();
        }

        @Test
        @DisplayName("devuelve una página vacía cuando no hay maestros")
        void devuelvePaginaVacia() {
            when(maestroRepository.findAll(primeraPagina))
                    .thenReturn(new PageImpl<>(List.of(), primeraPagina, 0));

            assertThat(maestroService.listar(primeraPagina).content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("devuelve el maestro cuando existe")
        void devuelveMaestroExistente() {
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));

            MaestroResponse resultado = maestroService.obtenerPorId(1L);

            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.nombre()).isEqualTo("Laura");
            assertThat(resultado.apellido()).isEqualTo("Mendoza");
            assertThat(resultado.especialidad()).isEqualTo("Matemáticas");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el id no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(maestroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maestroService.obtenerPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Maestro")
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("guarda el maestro cuando el email es único")
        void creaMaestro() {
            when(maestroRepository.existsByEmail("laura.mendoza@escuela.com")).thenReturn(false);
            when(maestroRepository.save(any(Maestro.class))).thenReturn(maestro);

            MaestroResponse resultado = maestroService.crear(request);

            ArgumentCaptor<Maestro> captor = ArgumentCaptor.forClass(Maestro.class);
            verify(maestroRepository).save(captor.capture());
            Maestro guardado = captor.getValue();
            assertThat(guardado.getNombre()).isEqualTo("Laura");
            assertThat(guardado.getApellido()).isEqualTo("Mendoza");
            assertThat(guardado.getEmail()).isEqualTo("laura.mendoza@escuela.com");
            assertThat(guardado.getEspecialidad()).isEqualTo("Matemáticas");
            assertThat(resultado.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("rechaza emails duplicados")
        void rechazaEmailDuplicado() {
            when(maestroRepository.existsByEmail("laura.mendoza@escuela.com")).thenReturn(true);

            assertThatThrownBy(() -> maestroService.crear(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("laura.mendoza@escuela.com");

            verify(maestroRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("actualiza todos los campos del maestro")
        void actualizaMaestro() {
            MaestroRequest cambios = new MaestroRequest(
                    "Laura Isabel", "Mendoza Ruiz", "laura.i@escuela.com", "Álgebra");
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(maestroRepository.existsByEmail("laura.i@escuela.com")).thenReturn(false);
            when(maestroRepository.save(any(Maestro.class))).thenAnswer(inv -> inv.getArgument(0));

            MaestroResponse resultado = maestroService.actualizar(1L, cambios);

            assertThat(resultado.nombre()).isEqualTo("Laura Isabel");
            assertThat(resultado.apellido()).isEqualTo("Mendoza Ruiz");
            assertThat(resultado.email()).isEqualTo("laura.i@escuela.com");
            assertThat(resultado.especialidad()).isEqualTo("Álgebra");
        }

        @Test
        @DisplayName("permite conservar el mismo email sin consultar duplicados")
        void permiteConservarSuPropioEmail() {
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(maestroRepository.save(any(Maestro.class))).thenAnswer(inv -> inv.getArgument(0));

            MaestroResponse resultado = maestroService.actualizar(1L, request);

            assertThat(resultado.email()).isEqualTo("laura.mendoza@escuela.com");
            verify(maestroRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("trata el mismo email con distinta capitalización como propio y lo guarda normalizado")
        void ignoraCapitalizacionDelPropioEmail() {
            MaestroRequest cambios = new MaestroRequest(
                    "Laura", "Mendoza", "LAURA.MENDOZA@escuela.com", "Matemáticas");
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(maestroRepository.save(any(Maestro.class))).thenAnswer(inv -> inv.getArgument(0));

            MaestroResponse resultado = maestroService.actualizar(1L, cambios);

            // El DTO ya bajó el email a minúsculas, así que se persiste en su forma canónica
            assertThat(resultado.email()).isEqualTo("laura.mendoza@escuela.com");
            verify(maestroRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("rechaza un email que ya pertenece a otro maestro")
        void rechazaEmailDeOtroMaestro() {
            MaestroRequest cambios = new MaestroRequest(
                    "Laura", "Mendoza", "jorge.salas@escuela.com", "Matemáticas");
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(maestroRepository.existsByEmail("jorge.salas@escuela.com")).thenReturn(true);

            assertThatThrownBy(() -> maestroService.actualizar(1L, cambios))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("jorge.salas@escuela.com");

            verify(maestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el maestro no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(maestroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maestroService.actualizar(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(maestroRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina el maestro cuando existe")
        void eliminaMaestro() {
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));

            maestroService.eliminar(1L);

            verify(maestroRepository).delete(maestro);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el maestro no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(maestroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maestroService.eliminar(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(maestroRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("obtenerActual")
    class ObtenerActual {

        @Test
        @DisplayName("devuelve el maestro vinculado al email del token")
        void devuelveElDelEmailAutenticado() {
            // El token lleva email y rol, no el id: es el único enlace entre el usuario
            // autenticado y su registro de maestro.
            when(currentUserService.getEmail()).thenReturn("laura.mendoza@escuela.com");
            when(maestroRepository.findByEmail("laura.mendoza@escuela.com"))
                    .thenReturn(Optional.of(maestro));

            MaestroResponse respuesta = maestroService.obtenerActual();

            assertThat(respuesta.id()).isEqualTo(1L);
            assertThat(respuesta.email()).isEqualTo("laura.mendoza@escuela.com");
        }

        @Test
        @DisplayName("un usuario sin maestro vinculado no es una avería: es un 404")
        void sinMaestroVinculadoLanzaNotFound() {
            // Le pasa a cualquier ADMIN, que no imparte nada.
            when(currentUserService.getEmail()).thenReturn("admin@escuela.com");
            when(maestroRepository.findByEmail("admin@escuela.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maestroService.obtenerActual())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("admin@escuela.com");
        }
    }
}
