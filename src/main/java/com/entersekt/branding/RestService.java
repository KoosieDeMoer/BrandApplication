package com.entersekt.branding;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.io.RuntimeIOException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.branding.entity.Branding;
import com.entersekt.entity.PersistedFile;
import com.entersekt.hub.GuiceBindingsModule;
import com.entersekt.topology.NodeRegisterService;
import com.entersekt.utils.AppUtils;

@Path("/Branding")
@Api(value = "/Branding")
public class RestService {

	private static final int LENGTH_OF_CODE = 9;

	private static final String CODE_PREFIX = "a";

	private static final Logger log = LoggerFactory.getLogger(RestService.class);

	private static final NodeRegisterService nodeRegisterService = GuiceBindingsModule.injector
			.getInstance(NodeRegisterService.class);

	@GET
	@Path("css")
	@ApiOperation(value = "Provides a compatible branding CSS file")
	@Produces("text/css")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Branding CSS file served") })
	public Response getBrandingCSS(
			@ApiParam(value = "Brand to identify CSS to use", required = true) @QueryParam("brand") String brand)
			throws Exception {
		return generateBrandedCss(brand);
	}

	@GET
	@Path("currentCss")
	@ApiOperation(value = "Provides a compatible branding CSS file for the current set brand")
	@Produces("text/css")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Branding CSS file served") })
	public Response getCurrentBrandingCSS() throws Exception {
		return generateBrandedCss(App.brand);
	}

	@POST
	@Path("info")
	@ApiOperation(value = "Stores branding info")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Branding info stored") })
	public Response postBrandingStyle(
			@ApiParam(value = "Brand Identifier", required = true) @QueryParam("brand") String brand,
			@ApiParam(value = "Logo image", required = true) @QueryParam("logo_image") String logo_image_name,
			@ApiParam(value = "Logo background colour", required = true) @QueryParam("logo_background_colour") String logo_background_colour,
			@ApiParam(value = "Logo height", required = true) @QueryParam("logo_height") int logo_height,
			@ApiParam(value = "Logo background size width") @QueryParam("logo_background_size_width") int logo_background_size_width,
			@ApiParam(value = "Logo background size height") @QueryParam("logo_background_size_height") int logo_background_size_height,
			@ApiParam(value = "Body background colour", required = true) @QueryParam("body_background_colour") String body_background_colour,
			@ApiParam(value = "Table Heading background colour", required = true) @QueryParam("table_heading_background_colour") String table_heading_background_colour,
			@ApiParam(value = "Submit Button background colour", required = true) @QueryParam("submit_button_background_colour") String submit_button_background_colour)
			throws Exception {
		Branding branding = new Branding(logo_image_name, logo_background_colour, logo_height,
				logo_background_size_width, logo_background_size_height, body_background_colour,
				table_heading_background_colour, submit_button_background_colour);
		App.persistenceService.blatDoc(brand, App.jsonSerialisationService.serialise(branding));
		AppUtils.emitEventSafely(App.eventSource, "changed_css");
		return Response.status(Response.Status.OK).entity("").build();
	}

	@PUT
	@Path("info")
	@ApiOperation(value = "Stores branding info - consumes JSON")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Branding info stored") })
	public Response putBrandingStyle(
			@ApiParam(value = "Brand Identifier", required = true) @QueryParam("brand") String brand, Branding branding)
			throws Exception {
		App.persistenceService.blatDoc(brand, App.jsonSerialisationService.serialise(branding));
		AppUtils.emitEventSafely(App.eventSource, "changed_css");
		return Response.status(Response.Status.OK).entity("").build();
	}

	@GET
	@Path("info")
	@ApiOperation(value = "Provides UI branding info. Side effect is to mark brand as current for viewing and preload image")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response getBrandingStyle(
			@ApiParam(value = "Brand Identifier", required = true) @QueryParam("brand") String brand) throws Exception {
		App.brand = brand;
		final String readDoc = App.persistenceService.readDoc(brand);
		final Branding branding = App.jsonSerialisationService.deSerialise(readDoc, Branding.class);
		App.currentImage = branding.logo_image_name;
		com.entersekt.hub.App.brandingImageContent = loadImageFile(App.currentImage);
		AppUtils.emitEventSafely(App.eventSource, "changed_css");
		return Response.status(Response.Status.OK).entity(readDoc).build();
	}

	@GET
	@Path("infos")
	@ApiOperation(value = "Retrieves list of branding infos")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response listBrandings() {
		final String listDocs = App.persistenceService.listDocs();
		final String[] brandings = App.jsonSerialisationService.extractRowIds(listDocs);
		Arrays.sort(brandings);
		return Response.status(Response.Status.OK).entity(App.jsonSerialisationService.serialise(brandings)).build();
	}

	@GET
	@Path("images")
	@ApiOperation(value = "Retrieves list of images")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response listImages() {
		final String listDocs = App.imagePersistenceService.listDocs();
		return Response.status(Response.Status.OK)
				.entity(App.jsonSerialisationService.serialise(App.jsonSerialisationService.extractRowIds(listDocs)))
				.build();
	}

	@POST
	@Path("upload")
	@ApiOperation(value = "Uploads and persists an image file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

		final String fileName = fileDetail.getFileName();
		byte[] bytes2 = AppUtils.readBytesFromInputStream(uploadedInputStream);

		App.imagePersistenceService.blatDoc(fileName,
				App.jsonSerialisationService.serialise(new PersistedFile(fileName, bytes2)));

		if (fileName.equals(App.currentImage)) {
			com.entersekt.hub.App.brandingImageContent = loadImageFile(App.currentImage);
			AppUtils.emitEventSafely(App.eventSource, "new_image");
		}
		return Response.status(200).entity("").build();

	}

	@GET
	@Path("download/{file-name}")
	@ApiOperation(value = "Downloads an image file")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response getFile(
			@ApiParam(value = "File Identifier", required = true) @PathParam("file-name") String fileName)
			throws Exception {
		return Response.ok(loadImageFile(fileName)).build();
	}

	private Response generateBrandedCss(final String brand) throws FileNotFoundException, IOException {
		if (brand == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		String template = AppUtils.readStringFromFile("styles/template.css");
		String readDoc;
		try {
			readDoc = App.persistenceService.readDoc(brand);
		} catch (RuntimeIOException e) {
			throw new RuntimeException("Problems reading branding info for '" + brand + "' with: " + e.getMessage());
		}
		final Branding brandingInfo = App.jsonSerialisationService.deSerialise(readDoc, Branding.class);
		return Response.status(Response.Status.OK).entity(brandingInfo.applyToTemplate(template)).build();
	}

	private byte[] loadImageFile(final String imageFileName) {
		final String readDoc = App.imagePersistenceService.readDoc(imageFileName);
		final PersistedFile persistedFile = App.jsonSerialisationService.deSerialise(readDoc, PersistedFile.class);
		final byte[] contents = persistedFile.getContents();
		return contents;
	}

}