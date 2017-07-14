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

package org.activiti.services.audit.producer.app.events;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiSequenceFlowTakenEventImpl;
import org.activiti.services.audit.producer.app.events.converter.EventConverter;
import org.activiti.services.model.events.ProcessEngineEvent;
import org.springframework.stereotype.Component;

import static org.activiti.engine.delegate.event.ActivitiEventType.SEQUENCEFLOW_TAKEN;

@Component
public class SequenceFlowTakenEventConverter implements EventConverter {

    @Override
    public ProcessEngineEvent from(ActivitiEvent event) {
        System.out.println(event.getType() + "---> Sequence Flow Taken??? " + event.getClass().getCanonicalName());
        return new SequenceFlowTakenEventImpl(event.getExecutionId(),
                                                  event.getProcessDefinitionId(),
                                                  event.getProcessInstanceId(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getId(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getSourceActivityId(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getSourceActivityName(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getSourceActivityType(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getTargetActivityId(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getTargetActivityName(),
                                                  ((ActivitiSequenceFlowTakenEventImpl)event).getTargetActivityType());
    }

    @Override
    public ActivitiEventType handledType() {
        return SEQUENCEFLOW_TAKEN;
    }
}
