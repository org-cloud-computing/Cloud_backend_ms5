package com.example.implementacion.controller;

import com.example.implementacion.service.servicio_athenea;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ms5/api/analytics")
public class controller_ms5 {

    private final servicio_athenea athenaService;

    @Value("${db.ms1:db_ms1}")
    private String dbMs1;

    @Value("${db.ms2:db_ms2}")
    private String dbMs2;

    @Value("${db.ms3:db_ms3}")
    private String dbMs3;

    @Value("${db.ms4:db_ms4}")
    private String dbMs4;

    public controller_ms5(servicio_athenea athenaService) {
        this.athenaService = athenaService;
    }

    @Operation(summary = "Health check del servicio")
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "service", "ms5-analytics",
            "timestamp", Instant.now().toString()
        );
    }

    @Operation(summary = "1. Productos con menos stock")
    @GetMapping("/productos-menos-stock")
    public List<Map<String, String>> productosMenosStock() {
        String query = String.format("""
            SELECT p.product_id, p.name, i.available_stock
            FROM product p
            JOIN stock i ON p.product_id = i.product_id
            ORDER BY i.available_stock ASC
            LIMIT 10
        """, dbMs1, dbMs1);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "2. Productos por categoría, orden alfabético")
    @GetMapping("/productos-por-categoria")
    public List<Map<String, String>> productosPorCategoria() {
        String query = String.format("""
            SELECT c.name AS category, p.name AS product
            FROM product p
            JOIN category c ON p.category_id = c.category_id
            ORDER BY c.name ASC, p.name ASC
        """, dbMs1, dbMs1);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "3. Productos con mejores reseñas")
    @GetMapping("/productos-mejores-resenas")
    public List<Map<String, String>> productosMejoresResenas() {
        String query = String.format("""
            SELECT product_id, name, stars, reviews
            FROM product
            ORDER BY stars DESC, reviews DESC
            LIMIT 10
        """, dbMs1);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "4. Productos con peores reseñas")
    @GetMapping("/productos-peores-resenas")
    public List<Map<String, String>> productosPeoresResenas() {
        String query = String.format("""
            SELECT product_id, name, stars, reviews
            FROM product
            ORDER BY stars ASC, reviews DESC
            LIMIT 10
        """, dbMs1);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "5. Clasificación de pedidos por fecha")
    @GetMapping("/pedidos-por-fecha")
    public List<Map<String, String>> pedidosPorFecha() {
        String query = String.format("""
            SELECT fecha_pedido, estado, COUNT(*) AS total_pedidos
            FROM pedido
            GROUP BY fecha_pedido, estado
            ORDER BY fecha_pedido DESC
        """, dbMs2);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "6. Email de clientes frecuentes")
    @GetMapping("/clientes-frecuentes")
    public List<Map<String, String>> clientesFrecuentes() {
        String query = String.format("""
            SELECT c.email, COUNT(pe.id) AS total_pedidos
            FROM cliente c
            JOIN pedido pe ON CAST(c.id AS VARCHAR) = CAST(pe.cliente_id AS VARCHAR)
            GROUP BY c.email
            HAVING COUNT(pe.id) > 3
            ORDER BY total_pedidos DESC
        """, dbMs2, dbMs2);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "7. Producto más pedido")
    @GetMapping("/producto-mas-pedido")
    public List<Map<String, String>> productoMasPedido() {
        String query = String.format("""
            SELECT producto_id, producto_nombre, SUM(cantidad) AS total_pedido
            FROM detalle_pedido
            GROUP BY producto_id, producto_nombre
            ORDER BY total_pedido DESC
            LIMIT 1
        """, dbMs2);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "7b. Producto menos pedido")
    @GetMapping("/producto-menos-pedido")
    public List<Map<String, String>> productoMenosPedido() {
        String query = String.format("""
            SELECT p.product_id AS product_id, p.name AS nombre_producto, COALESCE(SUM(dp.cantidad), 0) AS total_pedido
            FROM product p
            LEFT JOIN detalle_pedido dp ON p.product_id = dp.producto_id
            GROUP BY p.product_id, p.name
            ORDER BY total_pedido ASC
            LIMIT 1
        """, dbMs1, dbMs2);
        return athenaService.runQuery(query);
    }

    @Operation(summary = "8. Carritos abiertos (no completados)")
    @GetMapping("/carritos-abiertos")
    public List<Map<String, String>> carritosAbiertos() {
        String query = String.format("""
            SELECT COUNT(*) AS total_carritos_abiertos
            FROM carritos
            WHERE estado != 'COMPLETADO'
        """, dbMs3);
        return athenaService.runQuery(query);
    }
}
