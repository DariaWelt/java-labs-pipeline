package main.java;

import ru.spbstu.SmirnovaD.LogType;
import ru.spbstu.pipeline.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerParser {
    private String[] processNames;
    private String[] processConfigs;
    private FileInputStream input;
    private FileOutputStream output;
    private final Logger LOGGER;
    private RC error;

    ManagerParser(Map<String, String> map, Logger logger) {
        LOGGER = logger;
        error = RC.CODE_SUCCESS;
        try {
            int pipsNum = Integer.parseInt(map.get(
                    ManagerConfigParams.PROCESSES_AMOUNT.toString()));
            if (pipsNum <= 0)
                throw new NumberFormatException();

            input = new FileInputStream(map.get(
                    ManagerConfigParams.INPUT_STREAM.toString()));
            output = new FileOutputStream(map.get(
                    ManagerConfigParams.OUTPUT_STREAM.toString()));
            processNames = new String[pipsNum];
            processConfigs = new String[pipsNum];
            for (int i = 0; i < pipsNum; ++i) {
                processNames[i] = map.get(
                    ManagerConfigParams.PROCESS_NAME.toString().concat(Integer.toString(i)));
                processConfigs[i] = map.get(
                        ManagerConfigParams.CONFIG_PATH.toString().concat(Integer.toString(i)));
            }

        } catch (NullPointerException| NumberFormatException e) {
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    error.toString());

        } catch (FileNotFoundException e) {
            error = RC. CODE_INVALID_OUTPUT_STREAM;
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    error.toString());

        }
    }

    public RC getError() {
        return error;
    }

    public FileInputStream getInput() {
        return input;
    }

    public FileOutputStream getOutput() {
        return output;
    }

    public String getProcessConfigByIndex(int i) {
        if (processConfigs == null || i < 0 || i > processConfigs.length)
            return null;
        return processConfigs[i];
    }

    public String getProcessNameByIndex(int i) {
        if (processNames == null || i < 0 || i > processNames.length)
            return null;
        return processNames[i];
    }

    public int getProcessNum() {
        if (processNames == null)
            return -1;
        return processNames.length;
    }
}
