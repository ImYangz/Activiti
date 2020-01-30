package org.activiti.spring.resources;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class DeploymentResourceLoaderTest {

    @Test
    public void shouldLoadOnlyResourcesSelectdedByReader() throws IOException {
        //given
        List<String> names = new ArrayList<>();
        names.add("classpath:file-selected.txt");
        names.add("classpath:file-unselected.txt");
        Resource resource = new ClassPathResource("file-selected.txt");

        RepositoryService service = Mockito.mock(RepositoryService.class);
        Mockito.when(service.getDeploymentResourceNames("123456"))
                .thenReturn(names);
        Mockito.when(service.getResourceAsStream("123456", "classpath:file-selected.txt"))
                .thenReturn(resource.getInputStream());

        DeploymentResourceLoader deploymentResourceLoader = new DeploymentResourceLoader<String>();
        deploymentResourceLoader.setRepositoryService(service);

        ResourceReader selectorReader = new ResourceReader<String>() {
            @Override
            public Predicate<String> getResourceNameSelector() {
                return resourceName -> resourceName.endsWith("-selected.txt");

            }

            @Override
            public String read(InputStream inputStream, String processDefinitionKey) throws IOException {
                return new String(IoUtil.readInputStream(inputStream, "the stream"));
            }
        };

        //when
        List<String> loaded = deploymentResourceLoader.loadResourcesForDeployment("123456", selectorReader, "");

        //then
        Assertions.assertThat(loaded)
                .hasSize(1)
                .contains("a selected resource\n");

    }
}
