package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import sgsld.modelo.Entidad;
import sgsld.util.SgsldException;
import sgsld.util.Texto;

/**
 * Base de las pantallas de administración (Jugadores, Categorías y
 * Kinesiólogos, CU-01 a CU-03): tabla arriba y formulario de alta o
 * modificación abajo.
 * <p>
 * Es un <b>método plantilla</b>: el flujo es común (elegir una fila carga el
 * formulario; "Nuevo" lo limpia; "Guardar" arma un objeto candidato, el
 * servicio lo valida y recién entonces se guarda; luego se recarga la lista y
 * se selecciona lo guardado) y cada subclase define las columnas, los campos y
 * qué servicio llamar. Los errores de validación del servicio se muestran
 * todos juntos.
 *
 * @param <T> entidad que se administra
 */
abstract class ControladorAdministracion<T extends Entidad> extends ControladorPantalla {

    @FXML
    private TableView<T> tabla;
    @FXML
    private Label tituloFormulario;
    @FXML
    private Label etiquetaResultado;
    @FXML
    private Button botonGuardar;

    private T seleccionado;
    private String mensajePendiente;

    @FXML
    private void initialize() {
        Tablas.ajustarAltura(tabla, 1, 8, Tablas.ALTO_FILA_COMPACTA);
        // Volver a elegir el mismo objeto (por ejemplo, al filtrar) no pisa lo que se está escribiendo.
        tabla.getSelectionModel().selectedItemProperty().addListener((propiedad, anterior, elegido) -> {
            if (elegido != null && elegido != seleccionado) {
                editar(elegido);
            }
        });
        botonGuardar.disableProperty().bind(ocupadoProperty());
        etiquetaResultado.managedProperty().bind(etiquetaResultado.visibleProperty());
        configurar();
        nuevo();
    }

    // ---------- Pasos que define cada pantalla ----------

    /**
     * Agrega las columnas a la tabla (y reparte sus anchos con
     * {@link Tablas#distribuirAnchos(TableView, double...)}) y configura los
     * campos del formulario.
     */
    protected abstract void configurar();

    /**
     * Carga la lista desde el servicio (con {@link #ejecutar(Tarea, java.util.function.Consumer)})
     * y la muestra con {@link #mostrarLista(List, Integer)}.
     *
     * @param idASeleccionar elemento a seleccionar al terminar, o {@code null}
     */
    protected abstract void cargar(Integer idASeleccionar);

    /**
     * Título de la sección del formulario en modo alta, en mayúsculas.
     *
     * @return por ejemplo "NUEVO JUGADOR"
     */
    protected abstract String tituloAlta();

    /**
     * Descripción breve del elemento, para títulos y confirmaciones.
     *
     * @param elemento elemento elegido
     * @return por ejemplo "Gómez, Pedro (DNI 42587634)"
     */
    protected abstract String describir(T elemento);

    /**
     * Mensaje de alta exitosa, sin el número.
     *
     * @return por ejemplo "Jugador registrado"
     */
    protected abstract String textoRegistrado();

    /**
     * Muestra los datos del elemento en el formulario.
     *
     * @param elemento elemento elegido
     */
    protected abstract void mostrarEnFormulario(T elemento);

    /** Deja el formulario vacío para un alta. */
    protected abstract void limpiarFormulario();

    /**
     * Arma el objeto candidato con lo escrito en el formulario. Se ejecuta en
     * el hilo de JavaFX, así que debe leer aquí todos los campos.
     *
     * @param id      0 para un alta; el número del elemento para una modificación
     * @param errores lista donde se agregan los errores de formato
     * @return el candidato
     */
    protected abstract T armarCandidato(int id, List<String> errores);

    /**
     * Registra el candidato (fuera del hilo de JavaFX).
     *
     * @param candidato elemento nuevo
     * @return el número asignado
     * @throws SgsldException si el servicio lo rechaza o falla la persistencia
     */
    protected abstract int registrar(T candidato) throws SgsldException;

    /**
     * Guarda los cambios del candidato (fuera del hilo de JavaFX).
     *
     * @param candidato elemento con el número del registrado y los datos nuevos
     * @throws SgsldException si el servicio lo rechaza o falla la persistencia
     */
    protected abstract void modificar(T candidato) throws SgsldException;

    /**
     * Se llama al elegir un elemento o al empezar un alta; por defecto no hace nada.
     *
     * @param elemento elemento elegido, o {@code null} en un alta
     */
    protected void alCambiarSeleccion(T elemento) {
    }

    // ---------- Flujo común ----------

    @Override
    public void alMostrar() {
        cargar(null);
    }

    protected final TableView<T> tabla() {
        return tabla;
    }

    /**
     * Elemento que se está modificando.
     *
     * @return el elemento, o {@code null} si el formulario es de alta
     */
    protected final T seleccionado() {
        return seleccionado;
    }

    /**
     * Muestra la lista y selecciona el elemento indicado (por ejemplo, el que
     * se acaba de guardar) o, si no se indica, el que se estaba modificando.
     * Si el que se modificaba ya no está en la lista (se dio de baja o quedó
     * fuera del filtro), el formulario vuelve al modo alta; un alta en curso
     * no se toca.
     *
     * @param lista          elementos a mostrar
     * @param idASeleccionar número a seleccionar, o {@code null}
     */
    protected final void mostrarLista(List<T> lista, Integer idASeleccionar) {
        tabla.getItems().setAll(lista);
        Integer buscado = idASeleccionar != null ? idASeleccionar
                : seleccionado != null ? Integer.valueOf(seleccionado.getId()) : null;
        boolean encontrado = false;
        if (buscado != null) {
            for (T elemento : lista) {
                if (elemento.getId() == buscado) {
                    tabla.getSelectionModel().select(elemento);
                    tabla.scrollTo(elemento);
                    seleccionado = elemento;
                    encontrado = true;
                    break;
                }
            }
        }
        if (!encontrado && seleccionado != null) {
            nuevo();
        }
        if (mensajePendiente != null) {
            etiquetaResultado.setText(mensajePendiente);
            etiquetaResultado.setVisible(true);
            mensajePendiente = null;
        }
    }

    /**
     * Recarga la lista y, al terminar, muestra un mensaje de resultado.
     *
     * @param idASeleccionar elemento a seleccionar, o {@code null}
     * @param mensaje        mensaje a mostrar debajo del formulario
     */
    protected final void recargar(Integer idASeleccionar, String mensaje) {
        mensajePendiente = mensaje;
        cargar(idASeleccionar);
    }

    /**
     * Selecciona una fila (lo usa el Capturador para mostrar el formulario
     * de modificación).
     *
     * @param indice posición de la fila
     */
    void seleccionarFila(int indice) {
        if (indice < tabla.getItems().size()) {
            tabla.getSelectionModel().select(indice);
        }
    }

    private void editar(T elemento) {
        seleccionado = elemento;
        tituloFormulario.setText(("Modificar · " + describir(elemento)).toUpperCase(Texto.LOCALE));
        etiquetaResultado.setVisible(false);
        mostrarEnFormulario(elemento);
        alCambiarSeleccion(elemento);
    }

    /** Empieza un alta: limpia la selección y el formulario. */
    @FXML
    private void nuevo() {
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();
        tituloFormulario.setText(tituloAlta());
        etiquetaResultado.setVisible(false);
        limpiarFormulario();
        alCambiarSeleccion(null);
    }

    /**
     * Alta o modificación: arma el candidato, lo envía al servicio fuera del
     * hilo de JavaFX y, si se guardó, recarga la lista seleccionándolo.
     */
    @FXML
    private void guardar() {
        List<String> errores = new ArrayList<>();
        boolean alta = seleccionado == null;
        T candidato = armarCandidato(alta ? 0 : seleccionado.getId(), errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        ejecutar(() -> {
            if (alta) {
                return registrar(candidato);
            }
            modificar(candidato);
            return candidato.getId();
        }, id -> recargar(id, alta ? textoRegistrado() + " con el N° " + id + "." : "Cambios guardados."));
    }

    /** Descarta lo escrito: vuelve a los datos guardados o deja el alta vacía. */
    @FXML
    private void cancelar() {
        etiquetaResultado.setVisible(false);
        if (seleccionado == null) {
            limpiarFormulario();
        } else {
            mostrarEnFormulario(seleccionado);
        }
    }
}
