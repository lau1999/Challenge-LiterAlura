package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Principal {
    private final Scanner teclado = new Scanner(System.in);
    private final String URL_BASE = "https://gutendex.com/books/";
    private final AutorRepository repository;
    private final ConsumoAPI consumoAPI = new ConsumoAPI();
    private final ConvierteDatos conversor = new ConvierteDatos();

    public Principal(AutorRepository repository) {
        this.repository = repository;
    }

    public void mostrarMenu() {
        var opcion = -1;
        var menu = """
                -----  Bienvenidos a Literalura  -----
                --------------------------------------------
                              MENU PRINCIPAL 
                --------------------------------------------
                1 - Buscar Libros por Título
                2 - Buscar Autor por Nombre
                3 - Listar Libros Registrados
                4 - Listar Autores Registrados
                5 - Listar Autores Vivos
                6 - Listar Libros por Idioma
                7 - Listar Autores por Año
                8 - Top 10 Libros más Buscados
                9 - Generar Estadísticas
                ----------------------------------------------
                0 -   SALIR DEL PROGRAMA 
                ----------------------------------------------
                Elija una opción:
                """;

        while (opcion != 0) {
            System.out.println(menu);
            try {
                opcion = Integer.valueOf(teclado.nextLine());
                switch (opcion) {
                    case 1:
                        buscarLibroPorTitulo();
                        break;
                    case 2:
                        buscarAutorPorNombre();
                        break;
                    case 3:
                        listarLibrosRegistrados();
                        break;
                    case 4:
                        listarAutoresRegistrados();
                        break;
                    case 5:
                        listarAutoresVivos();
                        break;
                    case 6:
                        listarLibrosPorIdioma();
                        break;
                    case 7:
                        listarAutoresPorAnio();
                        break;
                    case 8:
                        top10Libros();
                        break;
                    case 9:
                        generarEstadisticas();
                        break;
                    case 0:
                        System.out.println("Gracias por utilizar Literalura");
                        System.out.println("Cerrando la aplicacion Literalura");
                        break;
                    default:
                        System.out.println("Opción no válida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: ¡Ingresa un número válido! " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void buscarLibroPorTitulo() {
        while (true) {
            System.out.println("""
                    --------------------------------
                       BUSCAR LIBROS POR TÍTULO 
                    --------------------------------
                     """);

            System.out.println("Introduzca el nombre del libro que desea buscar:");
            var nombre = teclado.nextLine();

            // Verificar si el nombre contiene solo letras
            if (!nombre.matches("[a-zA-Z ]+")) {
                System.out.println("Error: Por favor, introduce el nombre del libro con letras.");
                continue; // Vuelve al inicio del bucle para solicitar nuevamente el nombre
            }

            var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());


            if (json != null && (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]"))) {

                try {
                    var datos = conversor.obtenerDatos(json, Datos.class);

                    Optional<DatosLibro> libroBuscado = datos.libros().stream().findFirst();
                    if (libroBuscado.isPresent()) {
                        System.out.println(
                                "\n------------- LIBRO  --------------" +
                                        "\nTítulo: " + libroBuscado.get().titulo() +
                                        "\nAutor: " + libroBuscado.get().autores().stream()
                                        .map(a -> a.nombre()).limit(1).collect(Collectors.joining()) +
                                        "\nIdioma: " + libroBuscado.get().idiomas().stream().collect(Collectors.joining()) +
                                        "\nNúmero de descargas: " + libroBuscado.get().descargas() +
                                        "\n--------------------------------------\n"
                        );

                        try {
                            Optional<Libro> libroOptional = repository.buscarLibroPorNombre(nombre);
                            if (libroOptional.isPresent()) {
                                System.out.println("El libro ya está guardado en la BD.");
                            } else {
                                List<Libro> libroEncontrado = libroBuscado.stream().map(a -> new Libro(a)).collect(Collectors.toList());
                                Autor autorAPI = libroBuscado.stream().
                                        flatMap(l -> l.autores().stream()
                                                .map(a -> new Autor(a)))
                                        .collect(Collectors.toList()).stream().findFirst().get();
                                Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                                        .map(a -> a.nombre())
                                        .collect(Collectors.joining()));

                                Autor autor;
                                if (autorBD.isPresent()) {
                                    autor = autorBD.get();
                                    System.out.println("El autor y el Libro ya está guardado en la BD");
                                } else {
                                    autor = autorAPI;
                                    repository.save(autor);
                                }
                                autor.setLibros(libroEncontrado);
                                repository.save(autor);
                            }
                        } catch (Exception e) {
                            System.out.println("Warning! " + e.getMessage());
                        }

                        // Preguntar al usuario si desea buscar otro libro
                        while (true) {
                            System.out.println("¿Desea buscar otro libro? (S/N):");
                            var continuar = teclado.nextLine();
                            if (continuar.equalsIgnoreCase("N")) {
                                return; // Sal del método si el usuario no desea buscar otro libro
                            } else if (continuar.equalsIgnoreCase("S")) {
                                break; // Sal del bucle si el usuario desea buscar otro libro
                            } else {
                                System.out.println("Error: Por favor, introduce 'S' para buscar otro libro o 'N' para salir.");
                            }
                        }
                    } else {
                        System.out.println("Libro no encontrado!");
                        continue; // Vuelve al inicio del bucle si no se encuentra un libro
                    }
                } catch (Exception e) {
                    System.out.println("Error al procesar los datos del libro: " + e.getMessage());
                    continue; // Vuelve al inicio del bucle en caso de error
                }
            }
        }
    }

    public void buscarAutorPorNombre() {
        boolean realizarOtraBusqueda = true;

        while (realizarOtraBusqueda) {
            System.out.println("""
                    -------------------------------
                         BUSCAR AUTOR POR NOMBRE 
                    -------------------------------
                    """);
            System.out.println("Ingrese el nombre del autor que deseas buscar:");
            String nombre = teclado.nextLine().trim();

            // Verificar si el nombre contiene solo letras
            if (!nombre.matches("[a-zA-Z ]+")) {
                System.out.println("Error: Por favor, introduce el nombre del autor solo con letras.");
                continue; // Vuelve al inicio del bucle para solicitar nuevamente el nombre
            }

            // Verificar si el nombre contiene números
            if (nombre.matches(".*\\d.*")) {
                System.out.println("Error: El nombre del autor no debe contener números.");
                continue; // Vuelve al inicio del bucle para solicitar nuevamente el nombre
            }

            Optional<Autor> autor = repository.buscarAutorPorNombre(nombre);

            if (autor.isPresent()) {
                System.out.println(
                        "\nAutor: " + autor.get().getNombre() +
                                "\nFecha de Nacimiento: " + autor.get().getNacimiento() +
                                "\nFecha de Fallecimiento: " + autor.get().getFallecimiento() +
                                "\nLibros: " + autor.get().getLibros().stream()
                                .map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                );
            } else {
                System.out.println("El autor no existe en la BD");
            }

            System.out.println("¿Desea realizar otra búsqueda? (S/N)");
            String respuesta = teclado.nextLine().trim();
            if (!respuesta.equalsIgnoreCase("s")) {
                realizarOtraBusqueda = false;
            }
        }
    }


    public void listarLibrosRegistrados() throws Exception {
        System.out.println("""
                ----------------------------------
                    LISTAR LIBROS REGISTRADOS
                ----------------------------------
                 """);
        List<Libro> libros = repository.buscarTodosLosLibros();
        libros.forEach(l -> System.out.println(
                "-------------- LIBRO -----------------" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n----------------------------------------\n"
        ));
    }


    public void listarAutoresRegistrados() {
        System.out.println("""
                ----------------------------------
                   LISTAR AUTORES REGISTRADOS
                ----------------------------------
                 """);
        List<Autor> autores = repository.findAll();
        System.out.println();
        autores.forEach(l -> System.out.println(
                "Autor: " + l.getNombre() +
                        "\nFecha de Nacimiento: " + l.getNacimiento() +
                        "\nFecha de Fallecimiento: " + l.getFallecimiento() +
                        "\nLibros: " + l.getLibros().stream()
                        .map(t -> t.getTitulo()).collect(Collectors.toList()) + "\n"
        ));
    }


    public void listarAutoresVivos() {
        System.out.println("""
                -----------------------------
                   LISTAR AUTORES VIVOS 
                -----------------------------
                 """);
        while (true) {
            System.out.println("Introduzca un año para verificar el autor que desea buscar:");
            try {
                var input = teclado.nextLine();
                // Verificar si la entrada es un número
                if (!input.matches("\\d+")) {
                    System.out.println("Error: Por favor, ingrese solo números.");
                    continue; // Vuelve al inicio del bucle para solicitar nuevamente el año
                }
                var fecha = Integer.valueOf(input);
                List<Autor> autores = repository.buscarAutoresVivos(fecha);
                if (!autores.isEmpty()) {
                    System.out.println();
                    autores.forEach(a -> System.out.println(
                            "Autor: " + a.getNombre() +
                                    "\nFecha de Nacimiento: " + a.getNacimiento() +
                                    "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                    "\nLibros: " + a.getLibros().stream()
                                    .map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                    ));
                } else {
                    System.out.println("No hay autores vivos en el año especificado.");
                    System.out.println("¿Desea realizar otra búsqueda? (S/N)");
                    var respuesta = teclado.nextLine().trim();
                    if (respuesta.equalsIgnoreCase("S")) {
                        continue; // Vuelve al inicio del bucle para solicitar nuevamente el año
                    } else {
                        return; // Regresa al menú principal si el usuario no desea realizar otra búsqueda
                    }
                }
                // Si la ejecución llega a este punto, se ha completado con éxito la búsqueda del autor.
                break; // Sal del bucle while
            } catch (NumberFormatException e) {
                System.out.println("Error: Ingresa un año válido.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void listarLibrosPorIdioma() {
        while (true) {
            System.out.println("""
                    --------------------------------
                      LISTAR LIBROS POR IDIOMA 
                    --------------------------------
                    """);
            var menu = """
                    ---------------------------------------------------
                    Seleccione el idioma del libro que desea encontrar:
                    ---------------------------------------------------
                    1 - Español
                    2 - Francés
                    3 - Inglés
                    4 - Portugués
                    ----------------------------------------------------
                    """;
            System.out.println(menu);

            try {
                System.out.println("Ingrese el número correspondiente al idioma (o 'N' para regresar al menú principal):");
                var input = teclado.nextLine().trim();

                if (input.equalsIgnoreCase("N")) {
                    return; // Regresar al menú principal
                }

                var opcion = Integer.parseInt(input);

                switch (opcion) {
                    case 1:
                        buscarLibrosPorIdioma("es");
                        break;
                    case 2:
                        buscarLibrosPorIdioma("fr");
                        break;
                    case 3:
                        buscarLibrosPorIdioma("en");
                        break;
                    case 4:
                        buscarLibrosPorIdioma("pt");
                        break;
                    default:
                        System.out.println("Opción inválida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: Por favor, ingrese un número válido o 'N' para regresar al menú principal.");
            }
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.buscarLibrosPorIdioma(idiomaEnum);
            if (libros.isEmpty()) {
                System.out.println("No hay libros registrados en ese idioma");
            } else {
                System.out.println();
                libros.forEach(l -> System.out.println(
                        "----------- LIBRO   --------------" +
                                "\nTítulo: " + l.getTitulo() +
                                "\nAutor: " + l.getAutor().getNombre() +
                                "\nIdioma: " + l.getIdioma().getIdioma() +
                                "\nNúmero de descargas: " + l.getDescargas() +
                                "\n----------------------------------------\n"
                ));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido en el formato especificado.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void listarAutoresPorAnio() {
        while (true) {
            System.out.println("""
                    ------------------------------
                        LISTAR AUTORES POR AÑO 
                    ------------------------------
                     """);
            var menu = """
                    ------------------------------------------
                    Ingresa una opción para listar los autores
                    -------------------------------------------
                    1 - Listar autor por Año de Nacimiento
                    2 - Listar autor por año de Fallecimiento
                    3 - Volver al menú principal
                    -------------------------------------------
                    """;
            System.out.println(menu);
            try {
                var opcion = teclado.nextLine().trim(); // Lee la opción como String
                switch (opcion) {
                    case "1":
                        listarAutoresPorNacimiento();
                        break;
                    case "2":
                        listarAutoresPorFallecimiento();
                        break;
                    case "3":
                        return; // Regresar al menú principal
                    default:
                        System.out.println("Opción inválida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: " + e.getMessage());
            }
        }
    }

    public void listarAutoresPorNacimiento() {
        while (true) {
            System.out.println("""
                    ---------------------------------------------
                      BUSCAR AUTOR POR SU AÑO DE NACIMIENTO 
                    ---------------------------------------------
                    """);
            System.out.println("Introduzca el año de nacimiento del autor que desea buscar (o 'N' para regresar al menú principal):");
            try {
                var input = teclado.nextLine().trim();
                // Verificar si la entrada es un número o la letra 'N'
                if (!Pattern.matches("\\d+|N", input)) {
                    System.out.println("Opción no válida: Por favor, ingrese un año válido o 'N' para regresar al menú principal.");
                    continue; // Vuelve al inicio del bucle para solicitar nuevamente la entrada
                }
                if (input.equalsIgnoreCase("N")) {
                    return; // Regresar al menú principal
                }
                var nacimiento = Integer.valueOf(input);
                List<Autor> autores = repository.listarAutoresPorNacimiento(nacimiento);
                if (autores.isEmpty()) {
                    System.out.println("No existen autores con año de nacimiento igual a " + nacimiento);
                } else {
                    System.out.println();
                    autores.forEach(a -> System.out.println(
                            "Autor: " + a.getNombre() +
                                    "\nFecha de Nacimiento: " + a.getNacimiento() +
                                    "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                    "\nLibros: " + a.getLibros().stream().map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                    ));
                }
                // Preguntar al usuario si desea realizar otra búsqueda
                System.out.println("¿Desea realizar otra búsqueda? (S/N):");
                var continuar = teclado.nextLine();
                if (!continuar.equalsIgnoreCase("S")) {
                    return; // Regresar al menú principal
                }
            } catch (NumberFormatException e) {
                System.out.println("Año no válido: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void listarAutoresPorFallecimiento() {
        while (true) {
            System.out.println("""
                    ---------------------------------------------------------
                       BUSCAR LIBROS POR AÑO DE FALLECIMIENTO DEL AUTOR 
                    ----------------------------------------------------------
                     """);
            System.out.println("Introduzca el año de fallecimiento del autor que desea buscar (o 'N' para regresar al menú principal):");
            try {
                var input = teclado.nextLine().trim();
                // Verificar si la entrada es un número o la letra 'N'
                if (!Pattern.matches("\\d+|N", input)) {
                    System.out.println("Opción no válida: Por favor, ingrese un año válido o 'N' para regresar al menú principal.");
                    continue; // Vuelve al inicio del bucle para solicitar nuevamente la entrada
                }
                if (input.equalsIgnoreCase("N")) {
                    return; // Regresar al menú principal
                }
                var fallecimiento = Integer.valueOf(input);
                List<Autor> autores = repository.listarAutoresPorFallecimiento(fallecimiento);
                if (autores.isEmpty()) {
                    System.out.println("No existen autores con año de fallecimiento igual a " + fallecimiento);
                } else {
                    System.out.println();
                    autores.forEach(a -> System.out.println(
                            "Autor: " + a.getNombre() +
                                    "\nFecha de Nacimiento: " + a.getNacimiento() +
                                    "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                    "\nLibros: " + a.getLibros().stream().map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                    ));
                }
                // Preguntar al usuario si desea realizar otra búsqueda
                System.out.println("¿Desea realizar otra búsqueda? (S/N):");
                var continuar = teclado.nextLine();
                if (!continuar.equalsIgnoreCase("S")) {
                    return; // Regresar al menú principal
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void top10Libros() throws Exception {
        System.out.println("""
                -------------------------------------
                     TOP 10 LIBROS MÁS BUSCADOS 
                -------------------------------------
                 """);
        List<Libro> libros = repository.top10Libros();
        System.out.println();
        libros.forEach(l -> System.out.println(
                "----------------- LIBRO ----------------" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n-------------------------------------------\n"
        ));
    }

    public void generarEstadisticas() {
        System.out.println("""
                ----------------------------
                   GENERAR ESTADÍSTICAS 
                ----------------------------
                 """);
        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);
        IntSummaryStatistics est = datos.libros().stream()
                .filter(l -> l.descargas() > 0)
                .collect(Collectors.summarizingInt(DatosLibro::descargas));
        Integer media = (int) est.getAverage();
        System.out.println("\n--------- ESTADÍSTICAS  ------------");
        System.out.println(" Media de descargas: " + media);
        System.out.println(" Máxima de descargas: " + est.getMax());
        System.out.println(" Mínima de descargas: " + est.getMin());
        System.out.println(" Total registros para calcular las estadísticas: " + est.getCount());
        System.out.println("---------------------------------------------------\n");
    }
}