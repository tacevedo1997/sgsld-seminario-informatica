package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.SgsldException;

/**
 * Gestión de jugadores (CU-01, {@code jugadores.fxml}). Rol: Coordinador.
 * <p>
 * Búsqueda por DNI exacto (el servicio usa búsqueda binaria sobre el índice
 * ordenado por DNI) o por parte del apellido (búsqueda secuencial), alta,
 * modificación y baja lógica con confirmación: el jugador dado de baja deja de
 * figurar en los listados y en el estado de disponibilidad, pero conserva su
 * historial. "Incluir dados de baja" los muestra en la tabla.
 */
public class JugadoresController extends ControladorAdministracion<Jugador> {

    /** Encabezados de la tabla (cantidad fija). */
    private static final String[] ENCABEZADOS = {"DNI", "Apellido y nombre", "Edad", "Camiseta", "Posición",
        "Categoría", "Situación"};

    @FXML
    private TextField campoBusqueda;
    @FXML
    private CheckBox checkIncluirBaja;
    @FXML
    private TextField campoDni;
    @FXML
    private TextField campoApellido;
    @FXML
    private TextField campoNombre;
    @FXML
    private DatePicker campoNacimiento;
    @FXML
    private TextField campoCamiseta;
    @FXML
    private TextField campoPosicion;
    @FXML
    private ComboBox<Categoria> comboCategoria;
    @FXML
    private Button botonBaja;

    /** Estilo de las filas de jugadores dados de baja (texto gris). */
    private static final PseudoClass DADO_DE_BAJA = PseudoClass.getPseudoClass("dado-de-baja");

    /** Categorías y jugadores leídos en una misma tarea. */
    private record Datos(List<Categoria> categorias, List<Jugador> jugadores) {
    }

    @Override
    public String getTitulo() {
        return "Jugadores";
    }

    @Override
    protected void configurar() {
        TableView<Jugador> tabla = tabla();
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[0], Jugador::getDni));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[1], Jugador::getNombreCompleto));
        tabla.getColumns().add(Tablas.columnaNumerica(ENCABEZADOS[2],
                jugador -> String.valueOf(jugador.getEdad(hoy()))));
        tabla.getColumns().add(Tablas.columnaNumerica(ENCABEZADOS[3],
                jugador -> jugador.getNumeroCamiseta() == null ? "" : String.valueOf(jugador.getNumeroCamiseta())));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[4], Jugador::getPosicion));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[5], jugador -> jugador.getCategoria().getNombre()));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[6],
                jugador -> jugador.isActivo() ? "Activo" : "Dado de baja"));
        Tablas.distribuirAnchos(tabla, 95, 185, 62, 95, 130, 105, 115);
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Jugador jugador, boolean vacia) {
                super.updateItem(jugador, vacia);
                pseudoClassStateChanged(DADO_DE_BAJA, !vacia && jugador != null && !jugador.isActivo());
            }
        });
        tabla.setPlaceholder(new Label("No hay jugadores para mostrar."));
        Campos.soloDigitos(campoDni, 8);
        Campos.soloDigitos(campoCamiseta, 2);
        Campos.configurarFecha(campoNacimiento, hoy());
        checkIncluirBaja.setOnAction(evento -> buscar());
        campoBusqueda.textProperty().addListener((propiedad, anterior, nuevo) -> {
            if (Campos.leerTexto(campoBusqueda).isEmpty() && anterior != null && !anterior.isBlank()) {
                cargar(null);
            }
        });
    }

    @Override
    protected void cargar(Integer idASeleccionar) {
        boolean incluirBaja = checkIncluirBaja.isSelected();
        ejecutar(() -> new Datos(servicios().categorias().listar(), servicios().jugadores().listar(incluirBaja)),
                datos -> {
                    Categoria elegida = comboCategoria.getValue();
                    comboCategoria.getItems().setAll(datos.categorias());
                    elegirCategoria(elegida);
                    campoBusqueda.clear();
                    tabla().setPlaceholder(new Label("No hay jugadores para mostrar."));
                    mostrarLista(datos.jugadores(), idASeleccionar);
                });
    }

    /**
     * Busca por DNI si el texto son solo dígitos (coincidencia exacta) o por
     * parte del apellido en otro caso. Sin texto, lista todos (también al borrar
     * el texto de búsqueda).
     */
    @FXML
    private void buscar() {
        String texto = Campos.leerTexto(campoBusqueda);
        boolean incluirBaja = checkIncluirBaja.isSelected();
        if (texto.isEmpty()) {
            cargar(null);
        } else if (texto.chars().allMatch(Character::isDigit)) {
            ejecutar(() -> List.of(servicios().jugadores().buscarPorDni(texto)),
                    encontrados -> mostrarBusqueda(encontrados, "No hay jugadores para mostrar."),
                    error -> {
                        if (error instanceof EntidadNoEncontradaException) {
                            mostrarBusqueda(List.of(), error.getMessage());
                        } else {
                            informarError(error);
                        }
                    });
        } else {
            ejecutar(() -> servicios().jugadores().buscarPorApellido(texto), encontrados -> {
                List<Jugador> visibles = new ArrayList<>();
                for (Jugador jugador : encontrados) {
                    if (incluirBaja || jugador.isActivo()) {
                        visibles.add(jugador);
                    }
                }
                mostrarBusqueda(visibles, "No se encontraron jugadores con apellido \"" + texto + "\".");
            });
        }
    }

    private void mostrarBusqueda(List<Jugador> encontrados, String sinResultados) {
        tabla().setPlaceholder(new Label(sinResultados));
        mostrarLista(encontrados, encontrados.size() == 1 ? encontrados.get(0).getId() : null);
    }


    /** Baja lógica con confirmación (CU-01, 3b). */
    @FXML
    private void darDeBaja() {
        Jugador jugador = seleccionado();
        if (jugador == null || !confirmar("¿Dar de baja a " + describir(jugador) + "?\n\n"
                + "Dejará de figurar en los listados y en el estado de disponibilidad; su historial clínico se conserva.")) {
            return;
        }
        int id = jugador.getId();
        ejecutar(() -> {
            servicios().jugadores().darDeBaja(id);
            return id;
        }, baja -> recargar(checkIncluirBaja.isSelected() ? baja : null,
                "Jugador dado de baja: " + jugador.getNombreCompleto() + "."));
    }

    @Override
    protected String tituloAlta() {
        return "NUEVO JUGADOR";
    }

    @Override
    protected String describir(Jugador jugador) {
        return jugador.getNombreCompleto() + " (DNI " + jugador.getDni() + ")";
    }

    @Override
    protected String textoRegistrado() {
        return "Jugador registrado";
    }

    @Override
    protected void alCambiarSeleccion(Jugador jugador) {
        botonBaja.setDisable(jugador == null || !jugador.isActivo());
    }

    @Override
    protected void mostrarEnFormulario(Jugador jugador) {
        campoDni.setText(jugador.getDni());
        campoApellido.setText(jugador.getApellido());
        campoNombre.setText(jugador.getNombre());
        Campos.mostrarFecha(campoNacimiento, jugador.getFechaNacimiento());
        campoCamiseta.setText(jugador.getNumeroCamiseta() == null ? "" : String.valueOf(jugador.getNumeroCamiseta()));
        campoPosicion.setText(jugador.getPosicion());
        elegirCategoria(jugador.getCategoria());
    }

    /** Elige en el combo la categoría con el mismo número (los objetos pueden ser otros tras releer). */
    private void elegirCategoria(Categoria buscada) {
        comboCategoria.setValue(null);
        if (buscada == null) {
            return;
        }
        for (Categoria categoria : comboCategoria.getItems()) {
            if (categoria.getId() == buscada.getId()) {
                comboCategoria.setValue(categoria);
            }
        }
    }

    @Override
    protected void limpiarFormulario() {
        campoDni.clear();
        campoApellido.clear();
        campoNombre.clear();
        Campos.mostrarFecha(campoNacimiento, null);
        campoCamiseta.clear();
        campoPosicion.clear();
        comboCategoria.setValue(null);
    }

    @Override
    protected Jugador armarCandidato(int id, List<String> errores) {
        return new Jugador(id, Campos.leerTexto(campoDni), Campos.leerTexto(campoNombre),
                Campos.leerTexto(campoApellido), Campos.leerFecha(campoNacimiento, "fecha de nacimiento", errores),
                Campos.leerEntero(campoCamiseta), Campos.leerTexto(campoPosicion), true, comboCategoria.getValue());
    }

    @Override
    protected int registrar(Jugador candidato) throws SgsldException {
        return servicios().jugadores().registrar(candidato);
    }

    @Override
    protected void modificar(Jugador candidato) throws SgsldException {
        servicios().jugadores().modificar(candidato);
    }
}

