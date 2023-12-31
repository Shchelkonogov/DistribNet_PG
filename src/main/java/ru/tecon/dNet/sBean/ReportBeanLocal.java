package ru.tecon.dNet.sBean;

import ru.tecon.dNet.report.model.CellValue;
import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;

import javax.ejb.Local;
import java.time.LocalDate;
import java.util.List;

/**
 * local интерфейс для получения данных для excel отчета
 */
@Local
public interface ReportBeanLocal {

    /**
     * Получение имени ЦТП
     * @param object id объекта
     * @return имя ЦТП
     */
    String getCTP(int object);

    /**
     * Получение схемы подключения
     * @param object id объекта
     * @return схема подключения
     */
    String getConnectSchema(int object);

    /**
     * Получение имени филиала
     * @param object id объекта
     * @return имя филиала
     */
    String getFilial(int object);

    /**
     * Полечение имени организации
     * @param object id объекта
     * @return имя организации
     */
    String getCompany(int object);

    /**
     * Получение источника
     * @param object id объекта
     * @return источник
     */
    String getSource(int object);

    /**
     * Получение географического адреса объекта
     * @param objectID id объекта
     * @return адрес
     */
    String getAddress(int objectID);

    /**
     * Получение списка подключенных объектов с их id
     * @param object id объекта
     * @return список подключенных объектов
     */
    List<ConsumerModel> getObjectNames(int object);

    /**
     * Получение списка входный параметров
     * @param object id объекта
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return список входных параметров
     */
    List<DataModel> getInParameters(int object, LocalDate startDate, LocalDate endDate);

    /**
     * Получение списка выходных параметров
     * @param object id объекта
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return список выходных параметров
     */
    List<DataModel> getOutParameters(int object, LocalDate startDate, LocalDate endDate);

    /**
     * Получние значений по парамтру
     * @param parentID id цтп
     * @param object id объекта
     * @param id id параметра
     * @param statId id стат агрегата
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return значение параметра
     */
    CellValue getValue(int parentID, int object, int id, int statId, LocalDate startDate, LocalDate endDate);

    List<String> getTotalData(int objectID, LocalDate startDate, LocalDate endDate);
}
