package com.griddynamics.cd.internal.rest;

import com.griddynamics.cd.internal.model.api.ArtifactMetaInfo;
import com.griddynamics.cd.internal.model.api.RestResponse;
import com.thoughtworks.xstream.XStream;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.maven.ArtifactStoreHelper;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.artifact.AbstractArtifactPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * REST resource force repository update artifact
 */
@Path(ArtifactUpdatePlexusResource.REQUEST_URI)
@Produces({MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_XML})
@Component(role = PlexusResource.class, hint = ArtifactUpdatePlexusResource.ID)
public class ArtifactUpdatePlexusResource extends AbstractArtifactPlexusResource {
    public static final String ID = "artifactUpdatePlexusResource";
    public static final String REQUEST_URI = "/artifact/maven/update";

    private Logger log = LoggerFactory.getLogger(ArtifactUpdatePlexusResource.class);

    public ArtifactUpdatePlexusResource() {
        // Allows POST requests
        setModifiable(true);
    }

    /**
     * The location to attach this resource to.
     */
    @Override
    public String getResourceUri() {
        return REQUEST_URI;
    }

    /**
     * A permission prefix to be applied when securing the resource.
     */
    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:artifact]");
    }

    /**
     * A factory method to create an instance of DTO (POST request body).
     */
    @Override
    public Object getPayloadInstance() {
        return new ArtifactMetaInfo();
    }

    /**
     * A Resource may add some configuration stuff to the XStream, and control the serialization of the payloads it
     * uses.
     */
    @Override
    public void configureXStream(XStream xstream) {
        xstream.processAnnotations(ArtifactMetaInfo.class);
        xstream.processAnnotations(RestResponse.class);
    }

    /**
     * Update content of passed artifact in the proxy repositories of the passed repository
     */
    @POST
    @Override
    @ResourceMethodSignature(input = ArtifactMetaInfo.class, output = RestResponse.class)
    public Object post(Context context, Request request, Response response, Object payload) throws ResourceException {
        ArtifactMetaInfo metaInfo = (ArtifactMetaInfo) payload;
        if (!metaInfo.isValid()) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "At least following request parameters have to be given: nexusUrl, groupId, artifactId, version, repositoryId!");
        }
        log.trace(String.format("Remote URL: %s:%d", request.getClientInfo().getAddress(), request.getClientInfo().getPort()));
        boolean artifactResolved = false;
        for (Repository repository : getRepositoryRegistry().getRepositories()) {
            if (repository instanceof MavenProxyRepository) {
                MavenProxyRepository mavenProxyRepository = (MavenProxyRepository) repository;
                log.trace(String.format("Processing repository: %s. Remote url: %s", mavenProxyRepository.getId(), mavenProxyRepository.getRemoteUrl()));
                if (mavenProxyRepository.getRemoteUrl() != null
                        && mavenProxyRepository.getRemoteUrl().endsWith("/" + metaInfo.getRepositoryId() + "/")
                        && mavenProxyRepository.getRemoteUrl().startsWith(metaInfo.getNexusUrl())) {
                    ArtifactStoreRequest gavRequest = getResourceStoreRequest(request, false, false,
                            mavenProxyRepository.getId(), metaInfo.getGroupId(), metaInfo.getArtifactId(),
                            metaInfo.getVersion(), metaInfo.getPackaging(), metaInfo.getClassifier(), metaInfo.getExtension());
                    try {
                        ArtifactStoreHelper helper = mavenProxyRepository.getArtifactStoreHelper();
                        helper.retrieveArtifact(gavRequest);
                        artifactResolved = true;
                    } catch (ItemNotFoundException | IllegalOperationException | StorageException | AccessDeniedException e) {
                        log.error("Can not resolve artifact", e);
                        return new RestResponse(false, "Can not resolve artifact. " + e.getMessage());
                    }
                }
            }
        }
        if (artifactResolved) {
            return new RestResponse(true, "Artifact is resolved");
        } else {
            return new RestResponse(false, "Artifact has not resolved. Replication nexus server should proxy master repository");
        }
    }
}
