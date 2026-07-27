package com.escuela.service;

import com.escuela.exception.BadRequestException;
import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.MaestroRequest;
import com.escuela.model.dto.MaestroResponse;
import com.escuela.model.entity.Maestro;
import com.escuela.repository.MaestroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        @Test
        @DisplayName("devuelve todos los maestros mapeados a MaestroResponse")
        void devuelveTodosLosMaestros() {
            Maestro otro = new Maestro("Jorge", "Salas", "jorge.salas@escuela.com", "Física");
            otro.setId(2L);
            when(maestroRepository.findAll()).thenReturn(List.of(maestro, otro));

            List<MaestroResponse> resultado = maestroService.listar();

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(MaestroResponse::email)
                    .containsExactly("laura.mendoza@escuela.com", "jorge.salas@escuela.com");
            assertThat(resultado.get(1).especialidad()).isEqualTo("Física");
        }

        @Test
        @DisplayName("devuelve una lista vacía cuando no hay maestros")
        void devuelveListaVacia() {
            when(maestroRepository.findAll()).thenReturn(List.of());

            assertThat(maestroService.listar()).isEmpty();
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
        @DisplayName("trata el mismo email con distinta capitalización como propio")
        void ignoraCapitalizacionDelPropioEmail() {
            MaestroRequest cambios = new MaestroRequest(
                    "Laura", "Mendoza", "LAURA.MENDOZA@escuela.com", "Matemáticas");
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(maestroRepository.save(any(Maestro.class))).thenAnswer(inv -> inv.getArgument(0));

            MaestroResponse resultado = maestroService.actualizar(1L, cambios);

            assertThat(resultado.email()).isEqualTo("LAURA.MENDOZA@escuela.com");
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
}
