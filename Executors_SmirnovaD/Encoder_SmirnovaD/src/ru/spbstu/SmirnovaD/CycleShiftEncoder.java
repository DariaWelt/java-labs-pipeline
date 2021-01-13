package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.*;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/** brief
 * Процесс, который производит кодирование циклическим побайтовым сдвигом.
 */
public class CycleShiftEncoder implements IExecutor {
    private final Logger LOGGER;
    private final static TYPE[] supportedInputTypes = {TYPE.BYTE, TYPE.SHORT};
    private final static TYPE[] supportedOutputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final static int mask = 0x00ff;
    private final static int byteSize = 8;

    private IMediator produsersMediator;
    private INotifier notifier;
    private Integer shift;
    private byte[] processedData;
    private Queue<byte[]> collectedData;
    private TYPE chosenInputType;
    private volatile int numWaitedChunks;

    @Override
    public void run() {
        while (processedData != null) {
            if (numWaitedChunks > 0) {
                numWaitedChunks--;
                synchronized (collectedData){
                    RC error = execute();
                    if (error != RC.CODE_SUCCESS)
                        return;
                    collectedData.add(processedData);
                    notifier.notify(0);
                }
            }
        }
    }

    private class Mediator implements IMediator {
        private final TYPE returnedType;
        Mediator(TYPE type) {
            returnedType = type;
        }
        @Override
        public Object getData(int idChunk) {
            byte[] data = collectedData.poll();
            if (data == null) {
                return null;
            }
            switch (returnedType) {
                case BYTE: {
                    byte[] newArray = new byte[data.length];
                    System.arraycopy(data, 0, newArray, 0, newArray.length);
                    return newArray;
                }
                case SHORT: {
                    short[] newArray = new short[data.length / 2];
                    for (int i = 0; i < data.length / 2; ++i ){
                        byte hiWord = data[2 * i];
                        byte loWord = data[2 * i + 1];
                        newArray[i] = (short)((hiWord << byteSize) | loWord & mask);
                    }
                    return newArray;
                }
                case CHAR:{
                    String text = new String(data, StandardCharsets.UTF_8);
                    return text.toCharArray();
                }
                default:
                    return null;
            }
        }
    }

    private class Notifier implements INotifier {
        @Override
        public RC notify(int i) {
            numWaitedChunks++;
            return RC.CODE_SUCCESS;
        }
    }

    public CycleShiftEncoder(Logger logger) {
        LOGGER = logger;
        processedData = new byte[0];
        produsersMediator = null;
        notifier = null;
        shift = 0;
        chosenInputType = null;
        numWaitedChunks = 0;
        collectedData = new LinkedList<>();
    }

    public RC execute() {

        switch (chosenInputType) {
            case BYTE:
                processedData = (byte[])produsersMediator.getData(0);
                break;
            case SHORT:
                short[] data = (short[])produsersMediator.getData(0);
                processedData = convertToByte(data);
                break;
        }
        if (processedData != null) {
            for (int i = 0; i < processedData.length; ++i) {
                processedData[i] += shift;
                // сохраняем инвариант при каждом изменении
                //while (processedData[i] < - maxShift || processedData[i] > maxShift)
                //    processedData[i] += maxShift;
                //processedData[i] %= maxShift;
            }
        }
        // в лог пишем, что прошло успешно
        LOGGER.log(Level.FINE, LogType.SUCCESS_FINISH_METHOD.toString());
        return RC.CODE_SUCCESS;
    }

    @Override
    public INotifier getNotifier() {
        return new Notifier();
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
        shift = result.getKey();
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC addNotifier(INotifier iNotifier) {
        if (iNotifier == null){
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        notifier = iNotifier;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        TYPE[] types = process.getOutputTypes();
        chosenInputType = findFirstIntersection(types, supportedInputTypes);
        if (chosenInputType == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString().concat(" can't find suitable exchange type for coder."));
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        produsersMediator = process.getMediator(chosenInputType);
        return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedOutputTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        return new Mediator(type);
    }

    private static byte[] convertToByte(short[] array) {
        byte[] newArray = new byte[array.length * 2];
        for (int i = 0; i < array.length; ++i) {
            newArray[2 * i] = (byte)((array[i] & (mask << byteSize)) >> byteSize);
            newArray[2 * i + 1] = (byte)(array[i] & mask);
        }
        return newArray;
    }

    private TYPE findFirstIntersection(TYPE[] arr1, TYPE[] arr2) {
        for (TYPE type1 : arr1) {
            for (TYPE type2 : arr2) {
                if (type1 == type2)
                    return type1;
            }
        }
        return null;
    }
}
