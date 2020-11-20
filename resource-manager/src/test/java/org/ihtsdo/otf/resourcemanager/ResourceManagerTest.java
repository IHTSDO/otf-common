package org.ihtsdo.otf.resourcemanager;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ResourceLoader;

public class ResourceManagerTest {

    @Test(expected = NullPointerException.class)
    public void testCreatingResourceManagerInstanceWithNullResourceConfigurationThrowsNPE() {
        new ResourceManager(null, Mockito.mock(ResourceLoader.class));
    }

    @Test(expected = NullPointerException.class)
    public void testCreatingResourceManagerInstanceWithNullResourceLoaderThrowsNPE() {
        final ResourceConfiguration resourceConfiguration = Mockito.mock(ResourceConfiguration.class);
        Mockito.when(resourceConfiguration.isUseCloud()).thenReturn(true);
        new ResourceManager(resourceConfiguration, null);
    }
}