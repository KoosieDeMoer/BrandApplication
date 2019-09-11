package com.entersekt.branding;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.UnknownHostException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.communications.CommunicationsService;
import com.entersekt.configuration.ConfigurationService;
import com.entersekt.entity.PersistedFile;
import com.entersekt.eventsource.EventSource;
import com.entersekt.eventsource.EventSourceServlet;
import com.entersekt.hub.GuiceBindingsModule;
import com.entersekt.hub.common.RestCommonService;
import com.entersekt.json.JsonSerialisationService;
import com.entersekt.persistence.PersistenceService;
import com.entersekt.persistence.PersistenceServiceBase;
import com.entersekt.topology.Node;
import com.entersekt.topology.NodeRegisterService;
import com.entersekt.topology.NodeType;
import com.entersekt.utils.AppUtils;
import com.entersekt.utils.SwaggerUtils;

public class App {

	static final String JSON_EXTENSION = ".*\\.json";
	static final String IMAGE_EXTENSION = ".*\\.(png|gif|jpg|svg)";
	public static String who = com.entersekt.hub.App.BRANDING_WHO;
	static Node myNode;
	static String myHostname;
	static int myPortNo;
	static String hubHostname;
	static int hubPortNo;
	static String persistanceHostname;
	static int persistancePort;
	public static String brand;
	public static String currentImage;
	static PersistenceService persistenceService;
	static PersistenceService imagePersistenceService;

	private static final Logger log = LoggerFactory.getLogger(App.class);

	static final double WAIT_PROPORTION_OF_TTL = 0.7;

	static final CommunicationsService communicationsService = GuiceBindingsModule.injector
			.getInstance(CommunicationsService.class);

	public static NodeRegisterService nodeRegisterService = GuiceBindingsModule.injector
			.getInstance(NodeRegisterService.class);

	static final ConfigurationService configService = GuiceBindingsModule.injector
			.getInstance(ConfigurationService.class);
	static final JsonSerialisationService jsonSerialisationService = GuiceBindingsModule.injector
			.getInstance(JsonSerialisationService.class);

	public static EventSource eventSource = new EventSource();

	public static void main(String[] args) throws Exception {

		usage(args);
		initialiseAndStoreBuiltInBranding();
		new App().start();
	}

	public void start() throws Exception, UnknownHostException, InterruptedException {

		final HandlerList handlers = new HandlerList();

		// URL has form: http://localhost:8080/angularjs-ui/#!
		handlers.addHandler(AppUtils.buildWebUI(App.class, null, "angularjs-ui", "AngularJS based Web UI"));

		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(MultiPartFeature.class);

		resourceConfig.packages(RestService.class.getPackage().getName(), RestCommonService.class.getPackage()
				.getName());

		SwaggerUtils.buildSwaggerBean("Payee Node", "Token Issuer API", RestService.class.getPackage().getName() + ","
				+ RestCommonService.class.getPackage().getName());

		SwaggerUtils.attachSwagger(handlers, App.class, resourceConfig);

		ServletContainer servletContainer = new ServletContainer(resourceConfig);
		ServletHolder jerseyServlet = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(new ServletHolder(new EventSourceServlet(eventSource)), "/event-source/*");
		context.addServlet(jerseyServlet, "/*");

		handlers.addHandler(context);

		Server jettyServer = new Server(myPortNo);

		jettyServer.setHandler(handlers);

		myNode = nodeRegisterService.registerNodeWithHub(NodeType.BRANDING, who, who, myHostname, myPortNo, "",
				hubHostname, hubPortNo);

		try {
			jettyServer.start();
			jettyServer.join();
		} finally {
			nodeRegisterService.deregister(who, nodeRegisterService.getNode(com.entersekt.hub.App.WHO));
			jettyServer.destroy();
		}
	}

	private static void initialiseAndStoreBuiltInBranding() {
		persistenceService = PersistenceServiceBase.initialisePersistenceService(persistenceService,
				PersistenceService.BRANDING_INFO_DB_NAME, App.persistanceHostname, App.persistancePort);
		imagePersistenceService = PersistenceServiceBase.initialisePersistenceService(imagePersistenceService,
				PersistenceService.BRANDING_IMAGES_DB_NAME, App.persistanceHostname, App.persistancePort);

		for (File f : new File("branding_info").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(JSON_EXTENSION);
			}
		})) {
			final String fileName = f.getName();
			try {
				persistenceService.blatDoc(fileName.substring(0, fileName.length() - 5),
						AppUtils.readStringFromFile(f.getPath()));
			} catch (IOException e) {
				log.error("Failed to read and persist banding file: " + fileName);

			}
		}
		for (File f : new File("images").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(IMAGE_EXTENSION);
			}
		})) {
			try {
				imagePersistenceService.blatDoc(f.getName(), jsonSerialisationService.serialise(new PersistedFile(f
						.getName(), AppUtils.readBytesFromFile(f.getPath()))));
			} catch (IOException e) {
				log.error("Failed to read and persist banding file: " + f.getName());

			}
		}

	}

	private static void usage(String[] args) throws Exception {
		final String[] actors = configService.getPropertyList(AppUtils.PUBLIC_KEY_PROPERTY_TYPE);

		if (args.length < 5) {
			log.error("Usage requires command line parameters MY_HOSTNAME, MY_PORT, HUB_HOSTNAME, HUB_PORT, PERSISTANCE_HOSTNAME, PERSISTENCE_PORT  eg 192.168.99.100 8500 192.168.99.100 8080 192.168.99.100 5984");
			System.exit(0);
		} else {
			myHostname = args[0];
			myPortNo = AppUtils.extractPortNumber(args[1]);
			hubHostname = args[2];
			hubPortNo = AppUtils.extractPortNumber(args[3]);
			persistanceHostname = args[4];
			persistancePort = AppUtils.extractPortNumber(args[5]);

		}
		log.info("Starting Branding node with parameters: who='" + who + "', myHostname='" + myHostname
				+ "', myPortNo='" + myPortNo + "', hubHostname='" + hubHostname + "', hubPortNo='" + hubPortNo
				+ "', persistanceHostname='" + persistanceHostname + "', persistancePort='" + persistancePort + "'");
	}

}
