package com.alura.literalura.repository;

import com.alura.literalura.model.Autor;
import com.alura.literalura.model.Idioma;
import com.alura.literalura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    @Query("SELECT l FROM Libro l JOIN l.autor a WHERE l.titulo LIKE %:nombre%")
    Optional<Libro> buscarLibroPorNombre(@Param("nombre") String nombre);

    @Query("SELECT a FROM Libro l JOIN l.autor a WHERE lower(a.nombre) LIKE lower(concat('%', :nombre, '%'))")
    Optional<Autor> buscarAutorPorNombre(@Param("nombre") String nombre);


    @Query("SELECT a FROM Autor a WHERE a.fallecimiento > :fecha")
    List<Autor> buscarAutoresVivos(@Param("fecha") Integer fecha) throws Exception;

    ;

    @Query("SELECT l FROM Autor a JOIN a.libros l WHERE l.idioma = :idioma")
    List<Libro> buscarLibrosPorIdioma(@Param("idioma") Idioma idioma) throws Exception;

    ;

    @Query("SELECT l FROM Autor a JOIN a.libros l ORDER BY l.descargas DESC LIMIT 10")
    List<Libro> top10Libros() throws Exception;

    ;

    @Query("SELECT a FROM Autor a WHERE a.nacimiento = :fecha")
    List<Autor> listarAutoresPorNacimiento(@Param("fecha") Integer fecha) throws Exception;

    ;

    @Query("SELECT a FROM Autor a WHERE a.fallecimiento = :fecha")
    List<Autor> listarAutoresPorFallecimiento(@Param("fecha") Integer fecha) throws Exception;

    ;

    @Query("SELECT l FROM Autor a JOIN a.libros l")
    List<Libro> buscarTodosLosLibros() throws Exception;

    ;
}
