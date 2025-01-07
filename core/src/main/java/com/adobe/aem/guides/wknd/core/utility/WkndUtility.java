package com.adobe.aem.guides.wknd.core.utility;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import java.util.Map;

public class WkndUtility {

    public static ResourceResolver getServiceResourceResolver(ResourceResolverFactory resourceResolverFactory) throws LoginException {
        Map<String, Object> map = new HashedMap<>();
        map.put(ResourceResolverFactory.SUBSERVICE, "core-workflow-service");
        return resourceResolverFactory.getServiceResourceResolver(map);
    }

}
