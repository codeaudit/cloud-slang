/*******************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package io.cloudslang.lang.cli.services;

import io.cloudslang.lang.entities.ExecutableType;
import io.cloudslang.lang.entities.ScoreLangConstants;
import io.cloudslang.lang.runtime.env.ExecutionPath;
import io.cloudslang.lang.runtime.events.LanguageEventData;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import io.cloudslang.score.events.EventConstants;
import io.cloudslang.score.events.ScoreEvent;
import io.cloudslang.score.events.ScoreEventListener;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Date: 2/26/2015
 *
 * @author lesant
 */
public class SyncTriggerEventListener implements ScoreEventListener{
    public static final String SLANG_STEP_ERROR_MSG = "Slang Error : ";
    public static final String SCORE_ERROR_EVENT_MSG = "Score Error Event :";
    public static final String FLOW_FINISHED_WITH_FAILURE_MSG = "Flow finished with failure";
    public static final String EXEC_START_PATH = "0";
    public static final int OUTPUT_VALUE_LIMIT = 100;
    private final static String TASK_PATH_PREFIX = "- ";
    public static final String FLOW_OUTPUTS = "Flow outputs:";
    public static final String OPERATION_OUTPUTS = "Run outputs:";

    private AtomicBoolean flowFinished = new AtomicBoolean(false);
    private AtomicReference<String> errorMessage = new AtomicReference<>("");
    private boolean isDebugMode = false;

    public void setIsDebugMode(boolean isDebugMode){
        this.isDebugMode = isDebugMode;
    }
    public boolean isFlowFinished() {
        return flowFinished.get();
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    @Override
    public synchronized void onEvent(ScoreEvent scoreEvent) throws InterruptedException {
        @SuppressWarnings("unchecked") Map<String,Serializable> data = (Map<String,Serializable>)scoreEvent.getData();
        switch (scoreEvent.getEventType()){
            case EventConstants.SCORE_FINISHED_EVENT :
                flowFinished.set(true);
                break;
            case EventConstants.SCORE_ERROR_EVENT :
                errorMessage.set(SCORE_ERROR_EVENT_MSG + data.get(EventConstants.SCORE_ERROR_LOG_MSG) + " , " +
                        data.get(EventConstants.SCORE_ERROR_MSG));
                break;
            case EventConstants.SCORE_FAILURE_EVENT :
                printWithColor(Ansi.Color.RED,FLOW_FINISHED_WITH_FAILURE_MSG);
                flowFinished.set(true);
                break;
            case ScoreLangConstants.SLANG_EXECUTION_EXCEPTION:
                errorMessage.set(SLANG_STEP_ERROR_MSG + data.get(LanguageEventData.EXCEPTION));
                break;
            case ScoreLangConstants.EVENT_TASK_START:
                LanguageEventData eventData = (LanguageEventData) data;
                if(eventData.getStepType() == LanguageEventData.StepType.TASK){
                    String taskName = eventData.getStepName();
                    String path = eventData.getPath();
                    int matches = StringUtils.countMatches(path, ExecutionPath.PATH_SEPARATOR);
                    String prefix = StringUtils.repeat(TASK_PATH_PREFIX, matches);
                    printWithColor(Ansi.Color.YELLOW, prefix + taskName);
                }
                break;
            case ScoreLangConstants.EVENT_OUTPUT_END:
                // Task end case
                if((data.get(LanguageEventData.STEP_TYPE)).equals((LanguageEventData.StepType.TASK))) {
                    if (this.isDebugMode) {
                        Map<String, Serializable> taskOutputs = extractNotEmptyOutputs(data);
                        String path = ((LanguageEventData) data).getPath();
                        int matches = StringUtils.countMatches(path, ExecutionPath.PATH_SEPARATOR);
                        String prefix = StringUtils.repeat(TASK_PATH_PREFIX, matches);

                        for (String key : taskOutputs.keySet()) {
                            printWithColor(Ansi.Color.WHITE, prefix + key + " = " + taskOutputs.get(key));
                        }
                    }
                }

                // Flow end case
                else if(data.containsKey(LanguageEventData.OUTPUTS)
                        && data.containsKey(LanguageEventData.PATH)
                        && data.get(LanguageEventData.PATH).equals(EXEC_START_PATH)) {
                    Map<String, Serializable> outputs = extractNotEmptyOutputs(data);
                    if (data.containsKey(ScoreLangConstants.EXECUTABLE_TYPE) &&
                            ExecutableType.OPERATION.equals(data.get(ScoreLangConstants.EXECUTABLE_TYPE))) {
                        printWithColor(Ansi.Color.WHITE, "\n" + OPERATION_OUTPUTS);
                    } else {
                        printWithColor(Ansi.Color.WHITE, "\n" + FLOW_OUTPUTS);
                    }
                    for (String key : outputs.keySet()) {
                        printWithColor(Ansi.Color.WHITE, "- " + key + " = " + outputs.get(key));
                    }
                }
                break;
            case ScoreLangConstants.EVENT_EXECUTION_FINISHED :
                flowFinished.set(true);
                printFinishEvent(data);
                break;
        }
    }

    public static Map<String, Serializable> extractNotEmptyOutputs(Map<String, Serializable> data) {

        Map<String, Serializable> originalOutputs = (Map<String, Serializable>) data.get(LanguageEventData.OUTPUTS);
        Map<String, Serializable> extractedOutputs = new HashMap<>();

        if (MapUtils.isNotEmpty(originalOutputs)) {
            Iterator<Map.Entry<String, Serializable>> iterator = originalOutputs.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Serializable> output = iterator.next();

                if (output.getValue() != null && !(StringUtils.isEmpty(output.getValue().toString()))){
                    extractedOutputs.put(output.getKey(), StringUtils.abbreviate(output.getValue().toString(), 0, OUTPUT_VALUE_LIMIT));
                }
            }
            return extractedOutputs;
        }
        return new HashMap<>();
    }


    private void printFinishEvent(Map<String, Serializable> data) {
        String flowResult = (String)data.get(LanguageEventData.RESULT);
        String flowName = (String)data.get(LanguageEventData.STEP_NAME);
        printWithColor(Ansi.Color.CYAN,"Flow : " + flowName + " finished with result : " + flowResult);
    }

    private void printWithColor(Ansi.Color color, String msg){
        AnsiConsole.out().print(ansi().fg(color).a(msg).newline());
        AnsiConsole.out().print(ansi().fg(Ansi.Color.WHITE));

    }
}

