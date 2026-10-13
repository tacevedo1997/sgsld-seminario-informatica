package sgsld.controlador;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Persona;
import sgsld.modelo.Usuario;
import sgsld.servicio.KinesiologoService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Gestión de kinesiólogos (CU-03): listar, alta (con creación opcional de su
 * usuario) y modificación.
 */
public class KinesiologoController {

    private final Consola consola;
    private final KinesiologoService kinesiologos;

    /**
     * Crea el controlador.
     *
     * @param consola      entrada y salida
     * @param kinesiologos servicio de kinesiólogos
     */
    public KinesiologoController(Consola consola, KinesiologoService kinesiologos) {
        this.consola = consola;
        this.kinesiologos = kinesiologos;
    }

    /**
     * Lista los kinesiólogos con su matrícula y usuario.
     *
     * @throws AccesoDenegadoException si el rol no puede consultar kinesiólogos
     */
    public void listar() throws AccesoDenegadoException {
        List<Kinesiologo> lista = kinesiologos.listar();
        consola.titulo("Kinesiólogos");
        TablaConsola tabla = new TablaConsola("N°", "Kinesiólogo", "Matrícula", "Usuario").alinearDerecha(0);
        for (Kinesiologo kinesiologo : lista) {
            tabla.agregarFila(String.valueOf(kinesiologo.getId()), kinesiologo.getNombreCompleto(),
                    kinesiologo.getMatricula(),
                    kinesiologo.tieneUsuario() ? kinesiologo.getUsuario().getNombreUsuario() : "sin usuario");
        }
        tabla.imprimir(consola);
    }

    /**
     * Registra un kinesiólogo y, si se indica, crea su usuario del sistema.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void alta() throws AccesoDenegadoException {
        consola.titulo("Alta de kinesiólogo");
        Kinesiologo base = null;
        while (true) {
            Kinesiologo candidato = pedirDatos(base);
            boolean crearUsuario = consola.leerSiNo("¿Crear el usuario del sistema para el kinesiólogo? (S/N): ");
            String nombreUsuario = null;
            char[] contrasena = null;
            if (crearUsuario) {
                nombreUsuario = consola.leerTexto("Nombre de usuario: ", true, Usuario.MAX_NOMBRE_USUARIO);
                contrasena = leerContrasenaNueva();
            }
            try {
                int id = kinesiologos.registrar(candidato, nombreUsuario, contrasena);
                consola.mostrarOk("Kinesiólogo registrado con el N° " + id + ": " + candidato.getNombreCompleto()
                        + (crearUsuario ? ", con usuario " + nombreUsuario + "." : ", sin usuario."));
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada.");
                    return;
                }
                base = candidato;
            } finally {
                if (contrasena != null) {
                    Arrays.fill(contrasena, '\0');
                }
            }
        }
    }

    /**
     * Modifica nombre, apellido y matrícula; Enter conserva cada valor.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void modificar() throws AccesoDenegadoException {
        Optional<Kinesiologo> elegido = consola.elegirDeLista("Kinesiólogo a modificar:", kinesiologos.listar(),
                kinesiologo -> kinesiologo.getNombreCompleto() + " (" + kinesiologo.getIdentificacion() + ")");
        if (elegido.isEmpty()) {
            return;
        }
        consola.mostrar("Presione Enter para conservar el valor actual de cada dato.");
        Kinesiologo base = elegido.get();
        while (true) {
            Kinesiologo candidato = pedirDatos(base);
            try {
                kinesiologos.modificar(candidato);
                consola.mostrarOk("Datos de " + candidato.getNombreCompleto() + " actualizados.");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada: no se modificó el kinesiólogo.");
                    return;
                }
                base = candidato;
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarError(e.getMessage());
                return;
            }
        }
    }

    private Kinesiologo pedirDatos(Kinesiologo base) {
        boolean hayBase = base != null;
        String nombre = consola.editarTexto("Nombre", hayBase ? base.getNombre() : null, true, Persona.MAX_NOMBRE);
        String apellido = consola.editarTexto("Apellido", hayBase ? base.getApellido() : null, true,
                Persona.MAX_NOMBRE);
        String matricula = consola.editarTexto("Matrícula", hayBase ? base.getMatricula() : null, true,
                Kinesiologo.MAX_MATRICULA);
        return new Kinesiologo(hayBase ? base.getId() : 0, nombre, apellido, matricula,
                hayBase ? base.getUsuario() : null);
    }

    /** Pide la contraseña dos veces hasta que coincidan; limpia la confirmación. */
    private char[] leerContrasenaNueva() {
        while (true) {
            char[] contrasena = consola.leerContrasena("Contraseña (al menos " + KinesiologoService.MIN_CONTRASENA
                    + " caracteres): ");
            char[] confirmacion = consola.leerContrasena("Repita la contraseña: ");
            boolean coinciden = Arrays.equals(contrasena, confirmacion);
            Arrays.fill(confirmacion, '\0');
            if (coinciden) {
                return contrasena;
            }
            Arrays.fill(contrasena, '\0');
            consola.mostrarError("Las contraseñas no coinciden.");
        }
    }
}
