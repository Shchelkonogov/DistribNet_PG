package ru.tecon.dNet.sBean;

import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.ConnectorValue;
import ru.tecon.dNet.model.GraphElement;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Stateless bean для загрузки информации для построения мнемосхемы
 */
@Startup
@Stateless
public class GraphSBean {

    private static Logger log = Logger.getLogger(GraphSBean.class.getName());

    private static final String SQL_ALTER = "alter session set NLS_NUMERIC_CHARACTERS = '.,'";

    private static final String SQL_CONSUMERS = "select obj_id2 as obj_id, " +
            "(select obj_name from obj_object where obj_id = obj_id2) as obj_name " +
            "from obj_rel where obj_rel_type = 321 and obj_id1 = ? order by obj_name";
    private static final String SQL_PRODUCER = "select obj_name from obj_object where obj_id = ?";
    private static final String SQL_INIT_PARAMS = "select n1 as time, n2 as tech_proc, " +
            "n3||'='||n4 as direct_left, n5 as direct_left_color, " +
            "n9||'='||n10 as direct_center, n11 as direct_center_color, " +
            "n15||'='||n16 as direct_right, n17 as direct_right_color, " +
            "n6||'='||n7 as reverse_left, n8 as reverse_left_color, " +
            "n12||'='||n13 as reverse_center, n14 as reverse_center_Color, " +
            "n18||'='||n19 as reverse_right, n20 as reverse_right_color, " +
            "n21||'='||n22||' '||n24||'='||n25||' '||n26 as q, " +
            "n27||'='||n28||' '||n30||'='||n31||' '||n33||'='||n34||' '||n36||'='||n37||' '||n39||'='||n40 as k, " +
            "n29||' '||n32||' '||n35||' '||n38||' '||n41 as k_color from table (mnemo.get_Rnet_CTP_hist_data(?))";
    private static final String SQL_CONNECTIONS = "select n1||' '||n20||'='||n21 as name, " +
            "n2||'='||n3 as direct_left, n4 as direct_left_color, " +
            "n8||'='||n9 as direct_center, n10 as direct_center_color, " +
            "n14||'='||n15 as direct_right, n16 as direct_right_color, " +
            "n5||'='||n6 as reverse_left, n7 as reverse_left_color, " +
            "n11||'='||n12 as reverse_center, n13 as reverse_center_Color, " +
            "n17||'='||n18 as reverse_right, n19 as reverse_right_color," +
            "nvl2(n23, n23||'='||n24, null) as k0, n25 as k0_color, " +
            "nvl2(n26, n26||'='||n27, null) as k1, n28 as k1_color, " +
            "nvl2(n29, n29||'='||n30, null) as k2, n31 as k2_color " +
            "from table (mnemo.get_Rnet_UU_hist_data(?, to_date(?, 'dd-mm-yyyy hh24')))";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Метод загружает начальные данные для мнемосхемы (входные данные в источник)
     * @param objectId id объекта
     * @return данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    public GraphElement loadInitData(int objectId) throws GraphLoadException {
        GraphElement init = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAlter = connect.prepareStatement(SQL_ALTER);
             PreparedStatement stm = connect.prepareStatement(SQL_INIT_PARAMS)) {
            stmAlter.executeQuery();
            stm.setInt(1, objectId);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                init = new GraphElement(0, null, res.getString(1));

                Connector connector = new Connector(res.getString(15));
                connector.getIn()[0] = new ConnectorValue(res.getString(3), res.getString(4));
                connector.getIn()[1] = new ConnectorValue(res.getString(5), res.getString(6));
                connector.getIn()[2] = new ConnectorValue(res.getString(7), res.getString(8));
                connector.getOut()[0] = new ConnectorValue(res.getString(9), res.getString(10));
                connector.getOut()[1] = new ConnectorValue(res.getString(11), res.getString(12));
                connector.getOut()[2] = new ConnectorValue(res.getString(13), res.getString(14));
                connector.getCenter()[0] = new ConnectorValue(res.getString("k"), res.getString("k_color"));
                init.addConnect(connector);

                if (init.getDate() == null) {
                    throw new GraphLoadException("Источник ни разу не выходил на связь!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return init;
    }

    /**
     * Метода загружает основные данные мнемосхемы
     * @param objectId id объекта
     * @param date дата за которую грузить данные
     * @return данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    public GraphElement loadGraph(int objectId, String date) throws GraphLoadException {
        GraphElement producer = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_PRODUCER)) {
            stm.setInt(1, objectId);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                producer = new GraphElement(objectId, res.getString(1), date);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Загрузка данных присоединенных потребителей
        if (producer != null) {
            loadConsumers(producer);
        }
        return producer;
    }

    /**
     * Загрузка данных по потребителям
     * @param producer данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    private void loadConsumers(GraphElement producer) throws GraphLoadException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_CONSUMERS)) {
            stm.setInt(1, producer.getObjectId());
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                producer.addChildren(new GraphElement(res.getInt(1), res.getString(2)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (producer.getChildren() == null) {
            throw new GraphLoadException("У источника нету потребителей!");
        }

        // Загрузка данных по связам для каждого объекта мнемосхемы
        loadConnections(producer);
    }

    /**
     * Загрузка данных по связям для каждого элемента мнемосхемы
     * @param producer данные мнемосхемы
     */
    private void loadConnections(GraphElement producer) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAlter = connect.prepareStatement(SQL_ALTER);
             PreparedStatement stm = connect.prepareStatement(SQL_CONNECTIONS)) {
            stmAlter.executeQuery();
            doConnections(stm, producer, producer.getDate());

            for (GraphElement el: producer.getChildren()) {
                doConnections(stm, el, producer.getDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void doConnections(PreparedStatement stm, GraphElement producer, String date) throws SQLException {
        stm.setInt(1, producer.getObjectId());
        stm.setString(2, date);
        ResultSet res = stm.executeQuery();

        while (res.next()) {
            Connector connector = new Connector(res.getString(1));
            connector.getIn()[0] = new ConnectorValue(res.getString(2), res.getString(3));
            connector.getIn()[1] = new ConnectorValue(res.getString(4), res.getString(5));
            connector.getIn()[2] = new ConnectorValue(res.getString(6), res.getString(7));
            connector.getOut()[0] = new ConnectorValue(res.getString(8), res.getString(9));
            connector.getOut()[1] = new ConnectorValue(res.getString(10), res.getString(11));
            connector.getOut()[2] = new ConnectorValue(res.getString(12), res.getString(13));
            if (res.getString("k0") != null) {
                connector.getCenter()[0] = new ConnectorValue(res.getString("k0"), res.getString("k0_color"));
            }
            if (res.getString("k1") != null) {
                connector.getCenter()[1] = new ConnectorValue(res.getString("k1"), res.getString("k1_color"));
            }
            if (res.getString("k2") != null) {
                connector.getCenter()[2] = new ConnectorValue(res.getString("k2"), res.getString("k2_color"));
            }
            producer.addConnect(connector);
        }
    }
}
