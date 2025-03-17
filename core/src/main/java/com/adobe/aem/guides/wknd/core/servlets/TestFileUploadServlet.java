package com.adobe.aem.guides.wknd.core.servlets;
import com.adobe.aem.guides.wknd.core.utility.WkndUtility;
import com.day.cq.commons.Externalizer;

import org.apache.jackrabbit.api.JackrabbitValueFactory;
import org.apache.jackrabbit.api.binary.BinaryUpload;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Objects;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Reference;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class }, property = {
    Constants.SERVICE_DESCRIPTION + "=Test service to validate binary upload service",
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
    ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/customupload",
    ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=txt"
})
public class TestFileUploadServlet extends SlingAllMethodsServlet{

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileUploadServlet.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Externalizer externalizer;

    @Override
    public void doPost(SlingHttpServletRequest request,
                final SlingHttpServletResponse response) throws ServletException,
                IOException {

    try(ResourceResolver resourceResolver = Objects.requireNonNull(WkndUtility.getServiceResourceResolver(resourceResolverFactory))){
            Session session = resourceResolver.adaptTo(Session.class);
            if (Objects.nonNull(session)) {
        // allows to limit number of returned URIs in case the response message size is limited
        // use -1 for unlimited
        final int maxURIs = 50;

        final String path = request.getParameter("path");
        final long filesize = Long.parseLong(request.getParameter("filesize"));

        ValueFactory vf = session.getValueFactory();
         if (vf instanceof JackrabbitValueFactory) {
            JackrabbitValueFactory valueFactory = (JackrabbitValueFactory) vf;
             BinaryUpload upload = valueFactory.initiateBinaryUpload(filesize, maxURIs);
             if (upload == null) {
                // feature not available, must pass binary via InputStream through vf.createBinary()
                LOGGER.info("feature not available, must pass binary via InputStream through vf.createBinary()");
            } else {
                JSONObject json = new JSONObject();
                json.put("minPartSize", upload.getMinPartSize());
                json.put("maxPartSize", upload.getMaxPartSize());

                JSONArray uris = new JSONArray();
                Iterable<URI> iter = upload.getUploadURIs();
                for (URI uri : iter)  {
                    uris.put(uri);
                }
                json.put("uploadURIs", uris);
                
                // provide the client with a complete URL to request later, pass through the path
                completeUploadProcess(upload.getUploadToken(), path, valueFactory, session);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(json.toString());
            }



            }else{
                // feature not available, must pass binary via InputStream through vf.createBinary()
                LOGGER.info("feature not available, must pass binary via InputStream through vf.createBinary()");
            }
        }
    }catch(Exception e){
        LOGGER.info("Error occurred while creating the node " + e.getMessage());
        response.getWriter().write("node not created");
    }

 }

    private void completeUploadProcess(String uploadToken, String path, JackrabbitValueFactory valueFactory, Session session) throws IllegalArgumentException, RepositoryException {
        // TODO Auto-generated method stub
        Binary binary = valueFactory.completeBinaryUpload(uploadToken);

            Node ntFile = JcrUtils.getOrCreateByPath(path, "nt:file", session);
            Node ntResource = ntFile.addNode("jcr:content", "nt:resource");
            ntResource.setProperty("jcr:data", binary);
            ntResource.setProperty("jcr:mimeType", "image/jpeg");
            session.save();
        throw new UnsupportedOperationException("Unimplemented method 'completeUploadProcess'");
    }
}