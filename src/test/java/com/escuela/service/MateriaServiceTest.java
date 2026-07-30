package com.escuela.service;

import com.escuela.exception.ResourceNotFoundException;
import com.escuela.model.dto.MateriaRequest;
import com.escuela.model.dto.MateriaResponse;
import com.escuela.model.dto.PaginaResponse;
import com.escuela.model.entity.Maestro;
import com.escuela.model.entity.Materia;
import com.escuela.repository.MaestroRepository;
import com.escuela.repository.MateriaRepository;
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
@DisplayName("MateriaService")
class MateriaServiceTest {

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private MaestroRepository maestroRepository;

    @InjectMocks
    private MateriaService materiaService;

    private Maestro maestro;
    private Materia materia;
    private MateriaRequest request;

    @BeforeEach
    void setUp() {
        maestro = new Maestro("Laura", "Mendoza", "laura.mendoza@escuela.com", "Matemáticas");
        maestro.setId(1L);
        materia = new Materia("Álgebra I", 8, maestro);
        materia.setId(10L);
        request = new MateriaRequest("Álgebra I", 8, 1L);
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        private final Pageable primeraPagina = PageRequest.of(0, 2, Sort.by("nombre"));

        @Test
        @DisplayName("devuelve las materias de la página mapeadas a MateriaResponse")
        void devuelveLasMateriasDeLaPagina() {
            Materia otra = new Materia("Cálculo", 10, maestro);
            otra.setId(11L);
            when(materiaRepository.findAll(primeraPagina))
                    .thenReturn(new PageImpl<>(List.of(materia, otra), primeraPagina, 2));

            PaginaResponse<MateriaResponse> resultado = materiaService.listar(primeraPagina);

            assertThat(resultado.content()).hasSize(2);
            assertThat(resultado.content()).extracting(MateriaResponse::nombre)
                    .containsExactly("Álgebra I", "Cálculo");
            assertThat(resultado.content().get(0).maestroNombre()).isEqualTo("Laura Mendoza");
        }

        @Test
        @DisplayName("traslada el Pageable recibido al repositorio sin modificarlo")
        void trasladaElPageableAlRepositorio() {
            when(materiaRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(materia), primeraPagina, 1));

            materiaService.listar(primeraPagina);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(materiaRepository).findAll(captor.capture());
            assertThat(captor.getValue()).isSameAs(primeraPagina);
        }

        @Test
        @DisplayName("expone los metadatos de paginación de la página consultada")
        void exponeLosMetadatosDePaginacion() {
            Pageable ultimaPagina = PageRequest.of(2, 2);
            when(materiaRepository.findAll(ultimaPagina))
                    .thenReturn(new PageImpl<>(List.of(materia), ultimaPagina, 5));

            PaginaResponse<MateriaResponse> resultado = materiaService.listar(ultimaPagina);

            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.totalElements()).isEqualTo(5);
            assertThat(resultado.totalPages()).isEqualTo(3);
            assertThat(resultado.first()).isFalse();
            assertThat(resultado.last()).isTrue();
        }

        @Test
        @DisplayName("devuelve una página vacía cuando no hay materias")
        void devuelvePaginaVacia() {
            when(materiaRepository.findAll(primeraPagina))
                    .thenReturn(new PageImpl<>(List.of(), primeraPagina, 0));

            assertThat(materiaService.listar(primeraPagina).content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("devuelve la materia con los datos del maestro asignado")
        void devuelveMateriaExistente() {
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));

            MateriaResponse resultado = materiaService.obtenerPorId(10L);

            assertThat(resultado.id()).isEqualTo(10L);
            assertThat(resultado.nombre()).isEqualTo("Álgebra I");
            assertThat(resultado.creditos()).isEqualTo(8);
            assertThat(resultado.maestroId()).isEqualTo(1L);
            assertThat(resultado.maestroNombre()).isEqualTo("Laura Mendoza");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el id no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.obtenerPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia")
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("guarda la materia vinculada al maestro indicado")
        void creaMateria() {
            when(maestroRepository.findById(1L)).thenReturn(Optional.of(maestro));
            when(materiaRepository.save(any(Materia.class))).thenReturn(materia);

            MateriaResponse resultado = materiaService.crear(request);

            ArgumentCaptor<Materia> captor = ArgumentCaptor.forClass(Materia.class);
            verify(materiaRepository).save(captor.capture());
            Materia guardada = captor.getValue();
            assertThat(guardada.getNombre()).isEqualTo("Álgebra I");
            assertThat(guardada.getCreditos()).isEqualTo(8);
            assertThat(guardada.getMaestro()).isSameAs(maestro);
            assertThat(resultado.id()).isEqualTo(10L);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el maestroId no existe")
        void rechazaMaestroInexistente() {
            MateriaRequest sinMaestro = new MateriaRequest("Álgebra I", 8, 404L);
            when(maestroRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.crear(sinMaestro))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Maestro")
                    .hasMessageContaining("404");

            verify(materiaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("actualiza nombre, créditos y maestro asignado")
        void actualizaMateria() {
            Maestro otroMaestro = new Maestro("Jorge", "Salas", "jorge.salas@escuela.com", "Física");
            otroMaestro.setId(2L);
            MateriaRequest cambios = new MateriaRequest("Álgebra Lineal", 6, 2L);
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(maestroRepository.findById(2L)).thenReturn(Optional.of(otroMaestro));
            when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> inv.getArgument(0));

            MateriaResponse resultado = materiaService.actualizar(10L, cambios);

            assertThat(resultado.nombre()).isEqualTo("Álgebra Lineal");
            assertThat(resultado.creditos()).isEqualTo(6);
            assertThat(resultado.maestroId()).isEqualTo(2L);
            assertThat(resultado.maestroNombre()).isEqualTo("Jorge Salas");
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la materia no existe")
        void lanzaExcepcionCuandoLaMateriaNoExiste() {
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.actualizar(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Materia");

            verify(materiaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando el nuevo maestroId no existe")
        void lanzaExcepcionCuandoElMaestroNoExiste() {
            MateriaRequest cambios = new MateriaRequest("Álgebra I", 8, 404L);
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));
            when(maestroRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.actualizar(10L, cambios))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Maestro")
                    .hasMessageContaining("404");

            verify(materiaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina la materia cuando existe")
        void eliminaMateria() {
            when(materiaRepository.findById(10L)).thenReturn(Optional.of(materia));

            materiaService.eliminar(10L);

            verify(materiaRepository).delete(materia);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException cuando la materia no existe")
        void lanzaExcepcionCuandoNoExiste() {
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materiaService.eliminar(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(materiaRepository, never()).delete(any());
        }
    }
}
