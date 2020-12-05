package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/** brief
 * Процесс, который производит кодирование циклическим побайтовым сдвигом.
 */
public class CycleShiftEncoder implements IExecutor {

    private final Logger LOGGER;
    private IExecutable producer;
    private IExecutable consumer;
    private Integer shift;
    final static private Integer maxShift = (int)Character.MAX_VALUE;

    public CycleShiftEncoder(Logger logger) {
        LOGGER = logger;
        producer = null;
        consumer = null;
        shift = 0;
    }

    @Override
    public RC execute(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] += shift;
            // сохраняем инвариант при каждом изменении
            while (data[i] < 0)
                data[i] += maxShift;
            data[i] %= maxShift;
        }
        // передаем следующему на конвейере
        RC err = consumer.execute(data);
        // когда все следующие процессы выполнены, проверяем вернувшийся код
        if (err != RC.CODE_SUCCESS)
            return err;
        // Если все хорошо, в лог пишем, что прошло успешно
        LOGGER.log(Level.FINE, LogType.SUCCESS_FINISH_METHOD.toString());
        return err;
    }

    @Override
    public RC setConfig(String config) {
        // если получили нулевой аргумент:
        if (config == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(), "Argument of method is null. ");
            return RC.CODE_INVALID_ARGUMENT;
        }
        // парсим конфиг файл и получаем информацию для процесса
        Pair<Integer, RC> result = AppParser.semanticParse(config, LOGGER, Mode.MODE_ENCODER);
        if (result.getValue() != RC.CODE_SUCCESS)
            return result.getValue();
        // храним число от 0 до maxShift для удобства
        shift = result.getKey() % maxShift;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IExecutable process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        producer = process;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IExecutable process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        consumer = process;
        return RC.CODE_SUCCESS;
    }

}
