package com.empresa.pos.controller.admin;

import com.empresa.pos.dao.AppContext;
import com.empresa.pos.dao.CategoriaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class CategoriasController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CategoriasController.class);

    @FXML private TextField txtNuevaCategoria;
    @FXML private ListView<String> listCategorias;

    private final ObservableList<String> categorias = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listCategorias.setItems(categorias);
        txtNuevaCategoria.setOnAction(e -> handleAgregar());
        cargarDatos();
    }

    private void cargarDatos() {
        categorias.clear();
        try {
            CategoriaDAO dao = AppContext.getInstance().categoriaDAO();
            for (CategoriaDAO.Categoria cat : dao.findAll()) {
                categorias.add(cat.getNombre() + "  —  " + cat.getCantidadProductos() + " productos");
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar categorias: {}", e.getMessage());
            categorias.addAll(
                "Bebidas  —  3 productos",
                "Snacks  —  5 productos",
                "Alimentos  —  8 productos",
                "Lacteos  —  4 productos",
                "Limpieza  —  2 productos"
            );
        }
    }

    @FXML
    private void handleAgregar() {
        String nombre = txtNuevaCategoria.getText().trim();
        if (nombre.isEmpty()) return;

        try {
            CategoriaDAO dao = AppContext.getInstance().categoriaDAO();
            dao.insert(nombre);
            txtNuevaCategoria.clear();
            txtNuevaCategoria.requestFocus();
            cargarDatos();
        } catch (Exception e) {
            log.warn("No se pudo insertar categoria (DAO no disponible): {}", e.getMessage());
            categorias.add(nombre + "  —  0 productos");
            txtNuevaCategoria.clear();
            txtNuevaCategoria.requestFocus();
        }
    }
}
