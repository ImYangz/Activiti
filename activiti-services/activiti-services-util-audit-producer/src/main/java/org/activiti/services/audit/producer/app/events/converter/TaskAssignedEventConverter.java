/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.services.audit.producer.app.events.converter;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.task.Task;
import org.activiti.services.audit.producer.app.events.TaskAssignedEventImpl;
import org.activiti.services.model.converter.TaskConverter;
import org.activiti.services.model.events.ProcessEngineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.activiti.engine.delegate.event.ActivitiEventType.TASK_ASSIGNED;

@Component
public class TaskAssignedEventConverter implements EventConverter {

    private final TaskConverter taskConverter;

    @Autowired
    public TaskAssignedEventConverter(TaskConverter taskConverter) {
        this.taskConverter = taskConverter;
    }

    @Override
    public ProcessEngineEvent from(ActivitiEvent event) {
        System.out.println("Task Assigned : " + ((ActivitiEntityEventImpl) event).getEntity().getClass().getCanonicalName());

        return new TaskAssignedEventImpl(event.getExecutionId(),
                                             event.getProcessDefinitionId(),
                                             event.getProcessInstanceId(),
                                             taskConverter.from((Task) ((ActivitiEntityEventImpl) event).getEntity()));
    }

    @Override
    public ActivitiEventType handledType() {
        return TASK_ASSIGNED;
    }
}
