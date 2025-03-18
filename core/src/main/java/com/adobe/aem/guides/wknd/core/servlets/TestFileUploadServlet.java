package com.adobe.aem.guides.wknd.core.servlets;
import com.adobe.aem.guides.wknd.core.utility.WkndUtility;
import com.day.cq.commons.Externalizer;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jackrabbit.api.JackrabbitValueFactory;
import org.apache.jackrabbit.api.binary.BinaryUpload;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.request.builder.SlingHttpServletRequestBuilder;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import java.io.Closeable;
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
import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Reference;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class }, property = {
    Constants.SERVICE_DESCRIPTION + "=Test service to validate binary upload service",
    ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
    ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/customupload"
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
                int uploadBinaryStatus = 0;
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
                    uploadBinaryStatus = uploadBinaryCall(uri, request, resourceResolver);
                    }

                    json.put("uploadURIs", uris);
                    
                    if (uploadBinaryStatus == 200 || uploadBinaryStatus == 201) {
                        completeUploadProcess(upload.getUploadToken(), path, valueFactory, session);
                    }else{
                        LOGGER.info("Error occurred while uploading the file");
                    }
    
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
            response.getWriter().write("Error while upload process" + e.getMessage());
        }
                    
        }
    
        private int uploadBinaryCall(URI uri, SlingHttpServletRequest request, ResourceResolver resourceResolver) {
            RequestParameter file = request.getRequestParameter("file");
            int statuscode = 0;
            if (Objects.nonNull(file) && StringUtils.isNotBlank(uri.toString())) {
                try (CloseableHttpClient httpClient =  HttpClientBuilder.create().build()) {
                    HttpPut httpPut = new HttpPut(uri.toString());
                    final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.addBinaryBody(file.getName(), file.getInputStream());
                    final HttpEntity multipart = builder.build();
                    httpPut.setEntity(multipart);
                    CloseableHttpResponse httpResponse = httpClient.execute(httpPut);
                    LOGGER.info("Response code: " + httpResponse.getStatusLine().getStatusCode());
                    statuscode = httpResponse.getStatusLine().getStatusCode();
                } catch (IOException e) {
                   LOGGER.error("Error occurred while uploading the file " + e.getMessage());
                }         
            }else{
                LOGGER.info("File not found");
            }
            return statuscode;
        }
                    
       private void completeUploadProcess(String uploadToken, String path, JackrabbitValueFactory valueFactory, Session session) throws IllegalArgumentException, RepositoryException {
            try{
                Binary binary = valueFactory.completeBinaryUpload(uploadToken);

            Node ntFile = JcrUtils.getOrCreateByPath(path, "nt:file", session);
            Node ntResource = ntFile.addNode("jcr:content", "nt:resource");
            ntResource.setProperty("jcr:data", binary);
            ntResource.setProperty("jcr:mimeType", "image/jpeg");
            session.save();
            }catch(Exception e){
                LOGGER.info("Error occurred while completing the last upload process " + e.getMessage());
            }
    }
}