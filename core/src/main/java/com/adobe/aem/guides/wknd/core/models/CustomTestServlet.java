package com.adobe.aem.guides.wknd.core.models;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.Servlet;

import com.adobe.aem.guides.wknd.core.utility.WkndUtility;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { Servlet.class },property = {
    Constants.SERVICE_DESCRIPTION + "=Test service to validate mutable objects",
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/sampletestobject",
    ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=txt"
})
public class CustomTestServlet extends SlingSafeMethodsServlet {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException{
        response.setContentType("text/plain");
        String reqPath = request.getParameter("path");
        String name = request.getParameter("name");

        final Map<String, Object> slingFolderProp = new HashMap<>();
        slingFolderProp.put("jcr:primaryType", "sling:Folder");

        try(ResourceResolver resourceResolver = Objects.requireNonNull(WkndUtility.getServiceResourceResolver(resourceResolverFactory))){
            Resource destRes = resourceResolver.getResource(reqPath);
            if (Objects.nonNull(destRes)) {
                if (Objects.nonNull(destRes.getChild(name))) {
                    response.getWriter().write("node is already present");
                }else{
                    Resource newnode = resourceResolver.create(destRes, name, slingFolderProp);
                    request.getResourceResolver().commit();
                    response.getWriter().write("node created" + newnode);
                }
              
            }
         
        }catch(Exception e){
            response.getWriter().write("node not created");
        }

    }


    
}