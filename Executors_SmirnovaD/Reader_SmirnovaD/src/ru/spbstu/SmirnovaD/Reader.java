package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Reader implements IReader {
    private final Logger LOGGER;
    private final static TYPE[] supportedOutputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final static int mask = 0x00ff;
    private final static int byteSize = 8;

    private FileInputStream stream;
    private INotifier notifier;
    private int bufferCapacity;
    private byte[] processedData;
    private final Queue<byte[]> collectedData;


    public Reader(Logger logger) {
        LOGGER = logger;
        stream = null;
        notifier = null;
        processedData = new byte[0];
        collectedData = new LinkedList<>();
    }

    @Override
    public void run() {
        if (notifier == null || stream == null){
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return;
        }
        byte[] buffer = new byte[bufferCapacity];
        try {
            int already_read = stream.read(buffer);
            while (already_read >= 0) {
                already_read += already_read % 2;
                processedData = Arrays.copyOfRange(buffer, 0, already_read);
                synchronized (collectedData) {
                    collectedData.add(processedData);
                }
                notifier.notify(0);
                already_read = stream.read(buffer);
            }
            synchronized (collectedData) {
                collectedData.add(null);
            }
            notifier.notify(0);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_TO_READ.toString());
            return;
        }
    }

    private class Mediator implements IMediator {
        private final TYPE returnedType;
        Mediator(TYPE type) {
            returnedType = type;
        }
        @Override
        public Object getData(int idChunk) {
            byte[] data = null;
            synchronized (collectedData) {
                data = collectedData.poll();
            }
            if (data == null)
                return null;
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

    @Override
    public RC setConfig(String config) {
        if (config == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(), "Argument of method is null. ");
            return RC.CODE_INVALID_ARGUMENT;
        }
        Pair<Integer, RC> result = AppParser.semanticParse(config, LOGGER, Mode.MODE_READER);
        if (result.getValue() != RC.CODE_SUCCESS)
            return result.getValue();
        bufferCapacity = result.getKey();
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
    public RC setInputStream(FileInputStream input) {
        if (input == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        stream = input;
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
}
