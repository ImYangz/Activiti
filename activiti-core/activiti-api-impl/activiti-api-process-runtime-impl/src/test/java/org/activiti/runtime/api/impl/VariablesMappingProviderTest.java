package org.activiti.runtime.api.impl;

import static org.activiti.runtime.api.impl.MappingExecutionContext.buildMappingExecutionContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class VariablesMappingProviderTest {

    @InjectMocks
    private VariablesMappingProvider variablesMappingProvider;

    @Mock
    private ProcessExtensionService processExtensionService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void calculateInputVariablesShouldDoMappingWhenThereIsMappingSet() throws Exception {

        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVarMapping");
        given(execution.getVariable("process_variable_inputmap_1")).willReturn("new-input-value");
        given(execution.getVariable("property-with-no-default-value")).willReturn(null);

        ExpressionResolver expressionResolver = ExpressionResolverHelper.initContext(execution,
                                                                                     extensions);

        ReflectionTestUtils.setField(variablesMappingProvider,
                                     "expressionResolver",
                                     expressionResolver);

        //when
        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        //then
        assertThat(inputVariables.get("task_input_variable_name_1")).isEqualTo("new-input-value");

        //mapped with process variable that is null, so it should not be present
        assertThat(inputVariables).doesNotContainKeys("task_input_variable_mapped_with_null_process_variable");
    }

    private DelegateExecution buildExecution(ProcessExtensionModel extensions, String processDefinitionId) {
        DelegateExecution execution = mock(DelegateExecution.class);
        given(processExtensionService.getExtensionsForId(processDefinitionId.concat(":1:1231230123-123-123"))).willReturn(extensions);
        given(execution.getProcessDefinitionId()).willReturn(processDefinitionId.concat(":1:1231230123-123-123"));
        given(execution.getCurrentActivityId()).willReturn("simpleTask");
        return execution;
    }

    @Test
    public void calculateInputVariablesShouldPassAllVariablesWhenThereIsNoMapping() throws Exception {
        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-no-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVariableNoMapping");
        ExpressionResolver expressionResolver = ExpressionResolverHelper.initContext(execution,
                                                                                     extensions);

        ReflectionTestUtils.setField(variablesMappingProvider,
                                     "expressionResolver",
                                     expressionResolver);

        Map<String, Object> variables = new HashMap<>();
        variables.put("var-one", "one");
        variables.put("var-two", 2);
        given(execution.getVariables()).willReturn(variables);

        //when
        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        //then
        assertThat(inputVariables).isEqualTo(variables);
    }

    @Test
    public void calculateInputVariablesShouldNotPassAnyVariablesWhenTheMappingIsEmpty() throws Exception {

        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-empty-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVariableEmptyMapping");

        //when
        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        //then
        assertThat(inputVariables).isEmpty();
    }

    @Test
    public void calculateInputVariablesShouldPassOnlyConstantsWhenTheMappingIsEmpty() throws Exception {

        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-empty-mapping-with-constants-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVariableEmptyMappingWithContants");

        //when
        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        //then
        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(
                                                           tuple("process_constant_1_2", "constant_2_value"),
                                                           tuple("process_constant_inputmap_2", "constant_value"));

    }

    @Test
    public void calculateOutputVariablesShouldDoMappingWhenThereIsMappingSet() throws Exception {

        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVarMapping");
        ExpressionResolver expressionResolver = ExpressionResolverHelper.initContext(execution,
                                                                                     extensions);

        ReflectionTestUtils.setField(variablesMappingProvider,
                                     "expressionResolver",
                                     expressionResolver);

        Map<String, Object> entityVariables = new HashMap<>();
        entityVariables.put("task_output_variable_name_1", "var-one");

        ExpressionResolverHelper.setExecutionVariables(execution, entityVariables);

        //when
        Map<String, Object> outPutVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                entityVariables);

        //then
        assertThat(outPutVariables.get("process_variable_outputmap_1")).isEqualTo("var-one");

        //mapped with a task variable that is not present, so it should not be present
        assertThat(outPutVariables).doesNotContainKey("property-with-no-default-value");
    }

    @Test
    public void calculateOutputVariablesShouldPassAllVariablesWhenThereIsNoMapping() throws Exception {
        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-no-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVariableNoMapping");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_output_variable_name_1", "var-one");
        taskVariables.put("non-mapped-output_variable_name_2", "var-two");

        //when
        Map<String, Object> outPutVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        //then
        assertThat(outPutVariables).isEqualTo(taskVariables);
    }

    @Test
    public void calculateOutputVariablesShouldNotPassAnyVariablesWhenTheMappingIsEmpty() throws Exception {

        //given
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/task-variable-empty-mapping-extensions.json"),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, "Process_taskVariableEmptyMapping");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_output_variable_name_1", "var-one");
        taskVariables.put("non-mapped-output_variable_name_2", "var-two");

        //when
        Map<String, Object> outputVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        //then
        assertThat(outputVariables).isEmpty();
    }

    private DelegateExecution initExpressionResolverTest(String fileName, String processDefinitionKey) throws JsonParseException,
                                                                          JsonMappingException,
                                                                          IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessExtensionModel extensions = objectMapper.readValue(new File("src/test/resources/expressions/" + fileName),
                                                                  ProcessExtensionModel.class);

        DelegateExecution execution = buildExecution(extensions, processDefinitionKey);
        ExpressionResolver expressionResolver = ExpressionResolverHelper.initContext(execution,
                                                                                     extensions);

        ReflectionTestUtils.setField(variablesMappingProvider,
                                     "expressionResolver",
                                     expressionResolver);

        return execution;
    }

    @Test
    public void should_notSubstituteExpressions_when_thereAreNoExpressions() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("no-expression.json", "Process_NoExpression");

        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(tuple("process_constant_1",
                                                                 "constant_1_value"),
                                                           tuple("process_constant_2",
                                                                 "constant_2_value"),
                                                           tuple("task_input_variable_name_1",
                                                                 "variable_value_1"),
                                                           tuple("task_input_variable_name_2",
                                                                 "static_value_1"));

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_input_variable_name_1",
                          "variable_value_1");
        taskVariables.put("task_input_variable_name_2",
                          "static_value_2");

        Map<String, Object> outputVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        assertThat(outputVariables).isNotEmpty();
        assertThat(outputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                          Map.Entry::getValue)
                                              .containsOnly(tuple("process_variable_3",
                                                                  "variable_value_1"),
                                                            tuple("process_variable_4",
                                                                  "static_value_2"));
    }

    @Test
    public void should_notSubstituteExpressions_when_expressionIsInConstants() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-constants.json",
                                                       "Process_expression-in-constants");

        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(tuple("process_constant_1",
                                                                 "${process_variable_1}"),
                                                           tuple("process_constant_2",
                                                                 "constant_2_value"),
                                                           tuple("task_input_variable_name_1",
                                                                 "variable_value_1"),
                                                           tuple("task_input_variable_name_2",
                                                                 "static_value_1"));

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_input_variable_name_1",
                          "variable_value_1");
        taskVariables.put("task_input_variable_name_2",
                          "static_value_2");

        Map<String, Object> outputVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        assertThat(outputVariables).isNotEmpty();
        assertThat(outputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                          Map.Entry::getValue)
                                              .containsOnly(tuple("process_variable_3",
                                                                  "variable_value_1"),
                                                            tuple("process_variable_4",
                                                                  "static_value_2"));
    }

    @Test
    public void should_substituteExpressions_when_expressionIsInInputMappingValue() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-input-value.json",
                    "Process_expressionMappingInputValue");

        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(tuple("process_constant_1",
                                                                 "constant_1_value"),
                                                           tuple("process_constant_2",
                                                                 "constant_2_value"),
                                                           tuple("task_input_variable_name_1",
                                                                 "variable_value_1"),
                                                           tuple("task_input_variable_name_2",
                                                                 "variable_value_1"));
    }

    @Test
    public void should_notSubstituteExpressions_when_expressionIsInInputMappingVariable() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-input-variable.json",
                                                "Process_expressionMappingInputVariable");

        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);

        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(tuple("process_constant_1",
                                                                 "constant_1_value"),
                                                           tuple("process_constant_2",
                                                                 "constant_2_value"),
                                                           tuple("task_input_variable_name_2",
                                                                 "static_value_1"));
    }

    @Test
    public void should_substituteExpressions_when_expressionIsInOutputMappingValue() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-output-value.json",
            "Process_expressionMappingOutputValue");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_input_variable_name_1",
                          "variable_value_1");
        taskVariables.put("task_input_variable_name_2",
                          "static_value_2");

        Map<String, Object> outputVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        assertThat(outputVariables).isNotEmpty();
        assertThat(outputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                          Map.Entry::getValue)
                                              .containsOnly(tuple("process_variable_3",
                                                                  "variable_value_1"),
                                                            tuple("process_variable_4",
                                                                  "static_value_2"));
    }

    @Test
    public void should_notSubstituteExpressions_when_expressionIsInOutputMappingVariable() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-output-variable.json",
            "Process_expressionMappingOutputVariable");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_input_variable_name_1",
                          "variable_value_1");
        taskVariables.put("task_input_variable_name_2",
                          "static_value_2");

        Map<String, Object> outputVariables = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                                taskVariables);

        assertThat(outputVariables).isNotEmpty();
        assertThat(outputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                          Map.Entry::getValue)
                                              .containsOnly(tuple("process_variable_4",
                                                                  "static_value_2"));
    }

    @Test
    public void should_substituteExpressions_when_expressionIsInProperties() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-properties.json",
            "Process_expressionProperty");

        String[] array = {"1", "this expressionResolved is OK", "2"};
        List<String> list = Arrays.asList(array);

        Map<String, Object> var1 = new HashMap<>();
        var1.put("prop1",
                 "property 1");
        var1.put("prop2",
                 "expressionResolved");
        var1.put("prop3",
                 list);

        Map<String, Object> inputVariables = variablesMappingProvider.calculateInputVariables(execution);
        assertThat(inputVariables).isNotEmpty();
        assertThat(inputVariables.entrySet()).extracting(Map.Entry::getKey,
                                                         Map.Entry::getValue)
                                             .containsOnly(tuple("process_constant_1",
                                                                 "constant_1_value"),
                                                           tuple("process_constant_2",
                                                                 "constant_2_value"),
                                                           tuple("task_input_variable_name_1",
                                                                 var1),
                                                           tuple("task_input_variable_name_2",
                                                                 "static_value_1"));
    }

    @Test(expected = ActivitiIllegalArgumentException.class)
    public void should_throwActivitiIllegalArgumentException_when_expressionIsOutputMapping() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-output-value.json",
            "Process_expressionMappingOutputValue");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("task_input_variable_name_1",
                          "variable_value_1");
        taskVariables.put("task_input_variable_name_2",
                          "${expression}");

        variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                          taskVariables);
    }

    @Test
    public void should_returnEmptyOutputMapping_when_thereIsNoAvaliableVariableInTask() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("expression-in-mapping-output-value.json",
            "Process_expressionMappingOutputValue");

        Map<String, Object> outputMapping = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                              null);

        assertThat(outputMapping).isEmpty();
    }

    @Test
    public void should_returnEmptyOutputMapping_when_thereIsAnEmptyValueInOutputMappingVariable() throws Exception {
        DelegateExecution execution = initExpressionResolverTest("no-value-in-output-mapping-variable.json",
            "Process_noValueOutputMappingVariable");

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("not_matching_variable",
                          "variable_value_1");

        Map<String, Object> outputMapping = variablesMappingProvider.calculateOutPutVariables(buildMappingExecutionContext(execution),
                                                                                              taskVariables);

        assertThat(outputMapping).isEmpty();
    }
}
