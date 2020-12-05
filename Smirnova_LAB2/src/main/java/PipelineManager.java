package main.java;


import ru.spbstu.SmirnovaD.*;
import ru.spbstu.pipeline.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PipelineManager implements Runnable {
    private IExecutable pipelineStart;
    private final Logger LOGGER;
    private RC error;

    public PipelineManager(Logger logger, String config) {
        LOGGER = logger;
        error = RC.CODE_SUCCESS;
        Map<String, String> map = AppParser.syntaxParse(config, LOGGER);
        if (map == null){
            error = RC.CODE_INVALID_ARGUMENT;
            return;
        }
        ManagerParser info = new ManagerParser(map, LOGGER);
        if (info.getError() != RC.CODE_SUCCESS) {
            error = info.getError();
            return;
        }
        BuildPipeline(info);
    }

    private  RC MakePipeLineDependences(IExecutable[] pipline) {
        if (error != RC.CODE_SUCCESS)
            return error;
        for (int i = 0; i < pipline.length - 1; ++i) {
            if (i == 0)
                error = ((IPipelineStep)pipline[i]).setProducer(null);
            else
                error = ((IPipelineStep)pipline[i]).setProducer(pipline[i-1]);
            if (error != RC.CODE_SUCCESS)
                return error;
            error = ((IPipelineStep)pipline[i]).setConsumer(pipline[i+1]);
            if (error != RC.CODE_SUCCESS)
                return error;
        }
        pipelineStart = pipline[0];
        return error;
    }

    private void BuildPipeline(ManagerParser info) {
        int pipsNum = info.getProcessNum();
        if (pipsNum == -1) {
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            return;
        }
        IExecutable[] pipeline = new IExecutable[pipsNum];
        for (int i = 0; i < pipsNum; ++i) {
            pipeline[i] = CreateExecutable(info.getProcessNameByIndex(i), info.getProcessConfigByIndex(i));
            if (pipeline[i] == null) {
                error = RC.CODE_CONFIG_SEMANTIC_ERROR;
                LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                        error.toString()+" pipeline conf");
                return;
            }
        }
        try {
            error = ((IReader) pipeline[0]).setInputStream(info.getInput());
            if (error != RC.CODE_SUCCESS)
                return;
            error = ((IWriter) pipeline[pipsNum - 1]).setOutputStream(info.getOutput());
            if (error != RC.CODE_SUCCESS)
                return;
        } catch (ClassCastException ex) {
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    error.toString());
        }
        error = MakePipeLineDependences(pipeline);
    }

    private IExecutable CreateExecutable(String className, String configPath) {
        IExecutable process = null;
        String absolutePath = new File("").getAbsolutePath();
        try {// Загружаем класс по имени
            Class cl = Class.forName(className);
            // Этот класс - обработчик данных. То есть он реализует интерфейс Executable
            // Зададим параметры конструктора, общего для всех обработчиков и вызовем этот конструктор явно
            Class[] params = {Logger.class};
            process = ((IExecutable) cl.getConstructor(params).newInstance(LOGGER));
            error = ((IConfigurable)process).setConfig(absolutePath+configPath);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException exception) {
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    error.toString());
        }
        if (error != RC.CODE_SUCCESS)
            return null;
        return process;
    }

    @Override
    public void run() {
        error = pipelineStart.execute(null);
    }

    public RC getError() {
        return error;
    }
}
