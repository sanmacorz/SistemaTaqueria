package model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductRepository {
    private final DBConnection dbConnection;
    private Boolean pedidoAreaIsNumeric;
    private Set<String> pedidoColumnsCache;
    private Set<String> productoColumnsCache;

    public ProductRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public List<String> getItemNames() {
        String sql = "SELECT nombre FROM producto";
        List<String> names = new ArrayList<>();

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo el resultado", e);
        }

        return names;
    }

    public Map<String, BigDecimal> getUnitPricesByNames(List<String> productNames) {
        Map<String, BigDecimal> unitPrices = new LinkedHashMap<>();
        if (productNames == null || productNames.isEmpty()) {
            return unitPrices;
        }

        String query = "SELECT precio_unitario FROM producto WHERE nombre = ?";
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            for (String name : productNames) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal price = rs.getBigDecimal("precio_unitario");
                        unitPrices.put(name, price == null ? BigDecimal.ZERO : price);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando precios unitarios", e);
        }

        return unitPrices;
    }

    public Map<String, Integer> getStocksByNames(List<String> productNames) {
        Map<String, Integer> stocks = new LinkedHashMap<>();
        if (productNames == null || productNames.isEmpty()) {
            return stocks;
        }

        String query = "SELECT stock FROM producto WHERE nombre = ?";
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            for (String name : productNames) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stocks.put(name, rs.getInt("stock"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando stock por producto", e);
        }

        return stocks;
    }

    public List<InventoryProductRow> getInventoryProducts() {
        String query = "SELECT id_producto, nombre, stock FROM producto ORDER BY id_producto ASC";
        List<InventoryProductRow> rows = new ArrayList<>();

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new InventoryProductRow(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getInt("stock")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo inventario de productos", e);
        }

        return rows;
    }

    public List<String> getDistinctAreaValues() {
        if (!hasPedidoColumn("area")) {
            return Collections.emptyList();
        }

        String query = "SELECT DISTINCT CAST(area AS text) AS area_text FROM pedido ORDER BY area_text";
        List<String> areas = new ArrayList<>();
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String value = rs.getString("area_text");
                if (value != null && !value.isBlank()) {
                    areas.add(value.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo areas de pedido", e);
        }
        return areas;
    }

    public List<StatsSaleRow> getStatsSalesRows(StatsFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filtro de reporte invalido");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id_pedido, p.id_empleado, p.fecha, p.estado, d.cantidad, pr.nombre, pr.precio_unitario");
        sql.append(hasPedidoColumn("area") ? ", CAST(p.area AS text) AS area_value" : ", NULL::text AS area_value");
        sql.append(hasPedidoColumn("mesa") ? ", CAST(p.mesa AS text) AS mesa_value" : ", NULL::text AS mesa_value");
        sql.append(hasPedidoColumn("servicio") ? ", CAST(p.servicio AS text) AS servicio_value"
                : ", NULL::text AS servicio_value");
        sql.append(hasPedidoColumn("serie") ? ", CAST(p.serie AS text) AS serie_value" : ", NULL::text AS serie_value");
        sql.append(hasProductoColumn("clasificacion") ? ", CAST(pr.clasificacion AS text) AS clasificacion_value"
                : ", NULL::text AS clasificacion_value");
        sql.append(" FROM pedido p ");
        sql.append(" JOIN detallepp d ON d.id_pedido = p.id_pedido ");
        sql.append(" JOIN producto pr ON pr.id_producto = d.id_producto ");
        sql.append(" WHERE p.fecha BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(filter.fromDate));
        params.add(Timestamp.valueOf(filter.toDate));

        if (filter.area != null && !filter.area.isBlank() && !"(Todas)".equalsIgnoreCase(filter.area)
                && hasPedidoColumn("area")) {
            sql.append(" AND CAST(p.area AS text) = ? ");
            params.add(normalizeNumericText(filter.area));
        }
        if (filter.serie != null && !filter.serie.isBlank() && !"(Todas)".equalsIgnoreCase(filter.serie)
                && hasPedidoColumn("serie")) {
            sql.append(" AND CAST(p.serie AS text) = ? ");
            params.add(filter.serie);
        }
        if (filter.servicio != null && !filter.servicio.isBlank() && !"(Todos)".equalsIgnoreCase(filter.servicio)
                && hasPedidoColumn("servicio")) {
            sql.append(" AND LOWER(CAST(p.servicio AS text)) = LOWER(?) ");
            params.add(filter.servicio);
        }
        if (filter.mesa != null && !filter.mesa.isBlank() && !"(Todas)".equalsIgnoreCase(filter.mesa)
                && hasPedidoColumn("mesa")) {
            sql.append(" AND CAST(p.mesa AS text) = ? ");
            params.add(normalizeNumericText(filter.mesa));
        }
        if (filter.onlyPaid) {
            sql.append(
                    " AND LOWER(CAST(p.estado AS text)) IN ('pagado', 'hecho', 'completado', 'finalizado', 'entregado', 'listo') ");
        }
        if ("Vendidos".equalsIgnoreCase(filter.productsMode)) {
            sql.append(" AND d.cantidad > 0 ");
        } else if ("Devueltos".equalsIgnoreCase(filter.productsMode)) {
            sql.append(" AND d.cantidad < 0 ");
        }
        if (filter.clasificacion != null && !filter.clasificacion.isBlank()
                && !"(Todas)".equalsIgnoreCase(filter.clasificacion) && hasProductoColumn("clasificacion")) {
            sql.append(" AND CAST(pr.clasificacion AS text) = ? ");
            params.add(filter.clasificacion);
        }

        sql.append(" ORDER BY p.fecha ASC, p.id_pedido ASC ");

        List<StatsSaleRow> rows = new ArrayList<>();
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) value);
                } else {
                    ps.setString(i + 1, String.valueOf(value));
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new StatsSaleRow(
                            rs.getInt("id_pedido"),
                            rs.getInt("id_empleado"),
                            rs.getTimestamp("fecha"),
                            rs.getString("estado"),
                            rs.getInt("cantidad"),
                            rs.getString("nombre"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getString("area_value"),
                            rs.getString("mesa_value"),
                            rs.getString("servicio_value"),
                            rs.getString("serie_value"),
                            rs.getString("clasificacion_value")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo datos de reporte de ventas", e);
        }

        return rows;
    }

    public int adjustProductStock(int productId, int delta) {
        if (productId <= 0) {
            throw new IllegalArgumentException("ID de producto invalido");
        }

        String selectQuery = "SELECT stock FROM producto WHERE id_producto = ? FOR UPDATE";
        String updateQuery = "UPDATE producto SET stock = ? WHERE id_producto = ?";

        try {
            boolean previousAutoCommit = dbConnection.getConnection().getAutoCommit();
            dbConnection.getConnection().setAutoCommit(false);
            try (PreparedStatement selectPs = dbConnection.getConnection().prepareStatement(selectQuery);
                    PreparedStatement updatePs = dbConnection.getConnection().prepareStatement(updateQuery)) {

                selectPs.setInt(1, productId);
                int currentStock;
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Producto no encontrado: " + productId);
                    }
                    currentStock = rs.getInt("stock");
                }

                int nextStock = currentStock + delta;
                if (nextStock < 0) {
                    throw new IllegalArgumentException(
                            "Stock insuficiente para el producto " + productId + ". Stock actual: " + currentStock);
                }

                updatePs.setInt(1, nextStock);
                updatePs.setInt(2, productId);
                updatePs.executeUpdate();

                dbConnection.getConnection().commit();
                return nextStock;
            } catch (SQLException | IllegalArgumentException e) {
                dbConnection.getConnection().rollback();
                throw e;
            } finally {
                dbConnection.getConnection().setAutoCommit(previousAutoCommit);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException("Error ajustando stock del producto " + productId, e);
        }
    }

    public List<KitchenOrderRow> getPendingKitchenRows() {
        String query = """
                SELECT p.id_pedido, p.area, p.id_empleado, p.fecha, d.id_detallepp, d.cantidad, pr.nombre
                FROM pedido p
                JOIN detallepp d ON d.id_pedido = p.id_pedido
                JOIN producto pr ON pr.id_producto = d.id_producto
                WHERE LOWER(CAST(p.estado AS text)) = 'pendiente'
                ORDER BY p.fecha ASC, p.id_pedido ASC, d.id_detallepp ASC
                """;

        List<KitchenOrderRow> rows = new ArrayList<>();
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new KitchenOrderRow(
                        rs.getInt("id_pedido"),
                        rs.getInt("id_detallepp"),
                        rs.getString("area"),
                        rs.getInt("id_empleado"),
                        rs.getTimestamp("fecha"),
                        rs.getInt("cantidad"),
                        rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo ordenes pendientes para cocina", e);
        }

        return rows;
    }

    public boolean markOrderAsDone(int orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("ID de pedido invalido");
        }

        String doneStatus = resolveCompletedOrderStatus();
        String query = """
                UPDATE pedido
                SET estado = ?
                WHERE id_pedido = ?
                  AND LOWER(CAST(estado AS text)) = 'pendiente'
                """;

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, doneStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando estado del pedido " + orderId, e);
        }
    }

    public boolean markSingleDetailItemAsDone(int detailId) {
        if (detailId <= 0) {
            throw new IllegalArgumentException("ID de detalle invalido");
        }

        String selectQuery = "SELECT id_pedido, cantidad FROM detallepp WHERE id_detallepp = ?";
        String updateQuery = "UPDATE detallepp SET cantidad = ? WHERE id_detallepp = ?";
        String deleteQuery = "DELETE FROM detallepp WHERE id_detallepp = ?";
        String countDetailsQuery = "SELECT COUNT(*) FROM detallepp WHERE id_pedido = ?";
        String updateOrderStatusQuery = "UPDATE pedido SET estado = ? WHERE id_pedido = ?";
        String doneStatus = resolveCompletedOrderStatus();

        try {
            boolean previousAutoCommit = dbConnection.getConnection().getAutoCommit();
            dbConnection.getConnection().setAutoCommit(false);
            try (PreparedStatement selectPs = dbConnection.getConnection().prepareStatement(selectQuery);
                    PreparedStatement updatePs = dbConnection.getConnection().prepareStatement(updateQuery);
                    PreparedStatement deletePs = dbConnection.getConnection().prepareStatement(deleteQuery);
                    PreparedStatement countPs = dbConnection.getConnection().prepareStatement(countDetailsQuery);
                    PreparedStatement updateOrderPs = dbConnection.getConnection()
                            .prepareStatement(updateOrderStatusQuery)) {

                selectPs.setInt(1, detailId);
                int orderId;
                int quantity;
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (!rs.next()) {
                        dbConnection.getConnection().rollback();
                        return false;
                    }
                    orderId = rs.getInt("id_pedido");
                    quantity = rs.getInt("cantidad");
                }

                if (quantity > 1) {
                    updatePs.setInt(1, quantity - 1);
                    updatePs.setInt(2, detailId);
                    updatePs.executeUpdate();
                } else {
                    deletePs.setInt(1, detailId);
                    deletePs.executeUpdate();
                }

                countPs.setInt(1, orderId);
                int remainingDetails;
                try (ResultSet rs = countPs.executeQuery()) {
                    rs.next();
                    remainingDetails = rs.getInt(1);
                }

                if (remainingDetails == 0) {
                    updateOrderPs.setString(1, doneStatus);
                    updateOrderPs.setInt(2, orderId);
                    updateOrderPs.executeUpdate();
                }

                dbConnection.getConnection().commit();
                return true;
            } catch (SQLException e) {
                dbConnection.getConnection().rollback();
                throw e;
            } finally {
                dbConnection.getConnection().setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error marcando item de detalle como hecho (detallepp=" + detailId + ")", e);
        }
    }

    public void registerSale(Map<String, Integer> orderItems, int customerId, int employeeId, String areaValue) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("No hay productos para registrar");
        }
        if (areaValue == null || areaValue.isBlank()) {
            throw new IllegalArgumentException("Area invalida para registrar la venta");
        }

        Map<String, ProductInfo> productsByName = loadProductsForOrder(orderItems.keySet());
        BigDecimal total = calculateTotal(orderItems, productsByName);
        String initialStatus = resolveInitialOrderStatus();

        String orderQuery = "INSERT INTO pedido (id_cliente, id_empleado, area, fecha, estado, total) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?)";
        String detailQuery = "INSERT INTO detallepp (id_pedido, id_producto, cantidad) VALUES (?, ?, ?)";
        String modsQuery = "INSERT INTO detallempp (id_detallepp, id_modificacion) VALUES (?, ?)";
        String currentStep = "inicio";

        try {
            boolean previousAutoCommit = dbConnection.getConnection().getAutoCommit();
            dbConnection.getConnection().setAutoCommit(false);

            try (PreparedStatement orderPs = dbConnection.getConnection().prepareStatement(orderQuery,
                    Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement detailPs = dbConnection.getConnection().prepareStatement(detailQuery,
                            Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement modsPs = dbConnection.getConnection().prepareStatement(modsQuery)) {

                currentStep = "insert pedido";
                orderPs.setInt(1, customerId);
                orderPs.setInt(2, employeeId);
                bindOrderAreaValue(orderPs, 3, areaValue);
                orderPs.setString(4, initialStatus);
                orderPs.setBigDecimal(5, total);
                orderPs.executeUpdate();

                currentStep = "obtener id_pedido generado";
                int orderId = extractGeneratedId(orderPs, "pedido");

                for (Map.Entry<String, Integer> item : orderItems.entrySet()) {
                    ProductInfo info = productsByName.get(item.getKey());
                    currentStep = "descontar stock para producto '" + item.getKey() + "'";
                    reduceProductStockInTransaction(info.productId, item.getValue(), item.getKey());

                    currentStep = "insert detallepp para producto '" + item.getKey() + "'";
                    detailPs.setInt(1, orderId);
                    detailPs.setInt(2, info.productId);
                    detailPs.setInt(3, item.getValue());
                    detailPs.executeUpdate();

                    currentStep = "obtener id_detallepp generado para producto '" + item.getKey() + "'";
                    int detailId = extractGeneratedId(detailPs, "detallepp");
                    currentStep = "insert detallempp para producto '" + item.getKey() + "'";
                    registerDetailMods(modsPs, detailId, List.of());
                }

                currentStep = "commit";
                dbConnection.getConnection().commit();
            } catch (SQLException e) {
                try {
                    dbConnection.getConnection().rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw new RuntimeException(
                        "Error registrando la venta en paso '" + currentStep + "'. " + buildSqlDebugMessage(e),
                        e);
            } finally {
                dbConnection.getConnection().setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de transaccion al registrar la venta. " + buildSqlDebugMessage(e), e);
        }
    }

    private void bindOrderAreaValue(PreparedStatement orderPs, int parameterIndex, String areaValue)
            throws SQLException {
        if (isPedidoAreaNumeric()) {
            orderPs.setInt(parameterIndex, Integer.parseInt(areaValue));
            return;
        }
        orderPs.setString(parameterIndex, areaValue);
    }

    private boolean isPedidoAreaNumeric() {
        if (pedidoAreaIsNumeric != null) {
            return pedidoAreaIsNumeric;
        }

        String query = """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_name = 'pedido'
                  AND column_name = 'area'
                LIMIT 1
                """;

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                pedidoAreaIsNumeric = Boolean.FALSE;
                return false;
            }

            String dataType = rs.getString("data_type");
            if (dataType == null) {
                pedidoAreaIsNumeric = Boolean.FALSE;
                return false;
            }

            String normalized = dataType.toLowerCase();
            pedidoAreaIsNumeric = normalized.contains("int")
                    || normalized.equals("numeric")
                    || normalized.equals("decimal");
            return pedidoAreaIsNumeric;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando tipo de columna pedido.area", e);
        }
    }

    private boolean hasPedidoColumn(String columnName) {
        if (pedidoColumnsCache == null) {
            pedidoColumnsCache = loadColumnNames("pedido");
        }
        return pedidoColumnsCache.contains(columnName.toLowerCase());
    }

    private boolean hasProductoColumn(String columnName) {
        if (productoColumnsCache == null) {
            productoColumnsCache = loadColumnNames("producto");
        }
        return productoColumnsCache.contains(columnName.toLowerCase());
    }

    private Set<String> loadColumnNames(String tableName) {
        String query = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = ?
                """;

        Set<String> columns = new HashSet<>();
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String column = rs.getString("column_name");
                    if (column != null) {
                        columns.add(column.toLowerCase());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando columnas para tabla " + tableName, e);
        }
        return columns;
    }

    private String normalizeNumericText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.matches("\\d+")) {
            return trimmed;
        }
        return String.valueOf(Integer.parseInt(trimmed));
    }

    private String resolveInitialOrderStatus() {
        List<String> allowedStatuses = loadAllowedOrderStatuses();
        if (allowedStatuses.isEmpty()) {
            return "Pendiente";
        }

        List<String> preferred = Arrays.asList(
                "Pendiente",
                "pendiente",
                "PENDIENTE",
                "En proceso",
                "en proceso",
                "EN_PROCESO",
                "Activo",
                "activo",
                "ACTIVO");

        for (String candidate : preferred) {
            for (String allowed : allowedStatuses) {
                if (allowed.equalsIgnoreCase(candidate)) {
                    return allowed;
                }
            }
        }

        return allowedStatuses.get(0);
    }

    private String resolveCompletedOrderStatus() {
        List<String> allowedStatuses = loadAllowedOrderStatuses();
        if (allowedStatuses.isEmpty()) {
            return "Hecho";
        }

        List<String> preferred = Arrays.asList(
                "Hecho",
                "hecho",
                "HECHO",
                "Completado",
                "completado",
                "COMPLETADO",
                "Finalizado",
                "finalizado",
                "FINALIZADO",
                "Entregado",
                "entregado",
                "ENTREGADO",
                "Listo",
                "listo",
                "LISTO");

        for (String candidate : preferred) {
            for (String allowed : allowedStatuses) {
                if (allowed.equalsIgnoreCase(candidate)) {
                    return allowed;
                }
            }
        }

        for (String allowed : allowedStatuses) {
            if (!allowed.equalsIgnoreCase("pendiente")) {
                return allowed;
            }
        }

        throw new IllegalStateException(
                "No existe un estado de 'pedido' para marcar como hecho. Revisa pedido_estado_check.");
    }

    private List<String> loadAllowedOrderStatuses() {
        String query = """
                SELECT pg_get_constraintdef(c.oid) AS def
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE t.relname = 'pedido'
                  AND c.conname = 'pedido_estado_check'
                """;

        List<String> statuses = new ArrayList<>();
        Pattern valuePattern = Pattern.compile("'([^']+)'");

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return statuses;
            }

            String definition = rs.getString("def");
            if (definition == null || definition.isBlank()) {
                return statuses;
            }

            Matcher matcher = valuePattern.matcher(definition);
            while (matcher.find()) {
                String value = matcher.group(1);
                if (!statuses.contains(value)) {
                    statuses.add(value);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando estados permitidos para pedido", e);
        }

        return statuses;
    }

    private Map<String, ProductInfo> loadProductsForOrder(Set<String> productNames) {
        String query = "SELECT id_producto, precio_unitario FROM producto WHERE nombre = ?";
        Map<String, ProductInfo> result = new LinkedHashMap<>();

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            for (String name : productNames) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Producto no encontrado en DB: " + name);
                    }
                    int productId = rs.getInt("id_producto");
                    BigDecimal price = rs.getBigDecimal("precio_unitario");
                    if (price == null) {
                        price = BigDecimal.ZERO;
                    }
                    result.put(name, new ProductInfo(productId, price));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando productos para la venta", e);
        }

        return result;
    }

    private BigDecimal calculateTotal(Map<String, Integer> orderItems, Map<String, ProductInfo> productsByName) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> item : orderItems.entrySet()) {
            ProductInfo info = productsByName.get(item.getKey());
            if (info == null) {
                throw new IllegalArgumentException("Producto sin info para total: " + item.getKey());
            }
            BigDecimal lineTotal = info.price.multiply(BigDecimal.valueOf(item.getValue()));
            total = total.add(lineTotal);
        }
        return total;
    }

    private int extractGeneratedId(PreparedStatement ps, String tableName) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("No se pudo obtener el ID generado para " + tableName);
            }
            return keys.getInt(1);
        }
    }

    private void registerDetailMods(PreparedStatement modsPs, int detailId, List<Integer> modificationIds)
            throws SQLException {
        for (Integer modificationId : modificationIds) {
            modsPs.setInt(1, detailId);
            modsPs.setInt(2, modificationId);
            modsPs.executeUpdate();
        }
    }

    private void reduceProductStockInTransaction(int productId, int quantitySold, String productName)
            throws SQLException {
        String updateQuery = """
                UPDATE producto
                SET stock = stock - ?
                WHERE id_producto = ?
                  AND stock >= ?
                """;

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(updateQuery)) {
            ps.setInt(1, quantitySold);
            ps.setInt(2, productId);
            ps.setInt(3, quantitySold);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Stock insuficiente para '" + productName + "' (id=" + productId
                        + ", cantidad solicitada=" + quantitySold + ")");
            }
        }
    }

    private String buildSqlDebugMessage(SQLException error) {
        StringBuilder debug = new StringBuilder();
        SQLException current = error;
        int index = 1;
        while (current != null) {
            if (index > 1) {
                debug.append(" | ");
            }
            debug.append("SQL#").append(index)
                    .append(" state=").append(current.getSQLState())
                    .append(" code=").append(current.getErrorCode())
                    .append(" msg=").append(current.getMessage());
            current = current.getNextException();
            index++;
        }
        return debug.toString();
    }

    private static class ProductInfo {
        private final int productId;
        private final BigDecimal price;

        private ProductInfo(int productId, BigDecimal price) {
            this.productId = productId;
            this.price = price;
        }
    }

    public static class KitchenOrderRow {
        private final int orderId;
        private final int detailId;
        private final String area;
        private final int employeeId;
        private final Timestamp createdAt;
        private final int quantity;
        private final String productName;

        public KitchenOrderRow(int orderId, int detailId, String area, int employeeId, Timestamp createdAt,
                int quantity, String productName) {
            this.orderId = orderId;
            this.detailId = detailId;
            this.area = area;
            this.employeeId = employeeId;
            this.createdAt = createdAt;
            this.quantity = quantity;
            this.productName = productName;
        }

        public int getOrderId() {
            return orderId;
        }

        public int getDetailId() {
            return detailId;
        }

        public String getArea() {
            return area;
        }

        public int getEmployeeId() {
            return employeeId;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getProductName() {
            return productName;
        }
    }

    public static class InventoryProductRow {
        private final int productId;
        private final String productName;
        private final int stock;

        public InventoryProductRow(int productId, String productName, int stock) {
            this.productId = productId;
            this.productName = productName;
            this.stock = stock;
        }

        public int getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public int getStock() {
            return stock;
        }
    }

    public static class StatsFilter {
        private final LocalDateTime fromDate;
        private final LocalDateTime toDate;
        private final String area;
        private final String serie;
        private final String servicio;
        private final String mesa;
        private final boolean onlyPaid;
        private final String productsMode;
        private final String clasificacion;

        public StatsFilter(LocalDateTime fromDate,
                LocalDateTime toDate,
                String area,
                String serie,
                String servicio,
                String mesa,
                boolean onlyPaid,
                String productsMode,
                String clasificacion) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.area = area;
            this.serie = serie;
            this.servicio = servicio;
            this.mesa = mesa;
            this.onlyPaid = onlyPaid;
            this.productsMode = productsMode;
            this.clasificacion = clasificacion;
        }
    }

    public static class StatsSaleRow {
        private final int orderId;
        private final int employeeId;
        private final Timestamp soldAt;
        private final String estado;
        private final int quantity;
        private final String productName;
        private final BigDecimal unitPrice;
        private final String area;
        private final String mesa;
        private final String servicio;
        private final String serie;
        private final String clasificacion;

        public StatsSaleRow(int orderId,
                int employeeId,
                Timestamp soldAt,
                String estado,
                int quantity,
                String productName,
                BigDecimal unitPrice,
                String area,
                String mesa,
                String servicio,
                String serie,
                String clasificacion) {
            this.orderId = orderId;
            this.employeeId = employeeId;
            this.soldAt = soldAt;
            this.estado = estado;
            this.quantity = quantity;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.area = area;
            this.mesa = mesa;
            this.servicio = servicio;
            this.serie = serie;
            this.clasificacion = clasificacion;
        }

        public int getOrderId() {
            return orderId;
        }

        public int getEmployeeId() {
            return employeeId;
        }

        public Timestamp getSoldAt() {
            return soldAt;
        }

        public String getEstado() {
            return estado;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public String getArea() {
            return area;
        }

        public String getMesa() {
            return mesa;
        }

        public String getServicio() {
            return servicio;
        }

        public String getSerie() {
            return serie;
        }

        public String getClasificacion() {
            return clasificacion;
        }
    }
}
