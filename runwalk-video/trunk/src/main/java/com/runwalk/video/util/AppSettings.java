package com.runwalk.video.util;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.beansbinding.ELProperty;

import com.runwalk.video.io.DefaultVideoFolderRetrievalStrategy;
import com.runwalk.video.io.VideoFolderRetrievalStrategy;

@SuppressWarnings("serial")
public class AppSettings implements Serializable {

	//FIXME dit zou terug uit een resourceMap moeten gehaald worden.
	public static Font MAIN_FONT = new Font("Geneva", Font.PLAIN, 11);  //= ApplicationUtil.getResourceMap(ApplicationSettings.class).getFont("Application.mainFont").deriveFont(11f);

	public final static String FILE_ENCODING = "UTF-8";

	private final static String FILE_APPENDER_NAME = "A1";

	private final static String SETTINGS_FILE_NAME = "settings.xml";

	private final static String UNCOMPRESSED_VIDEO_DIRNAME = "uncompressed";

	private static Logger logger;

	// this is the only (?) thread safe way to initialize a singleton
	private final static AppSettings INSTANCE = new AppSettings();

	private File logFile;

	private transient JAXBContext jaxbContext;

	/** This object's fields will be mapped to XML using JaxB */
	private Settings settings;

	/** The parsed directories for both uncompressed and compressed video's are cached here after lazy initialization */
	private File videoDir, uncompressedVideoDir;
	
	/**
	 * This method will do the initial log4j configuration using the properties file embedded in the jar.
	 * You can reconfigure log4j afterwards by adding a log4j.properties file to the {@link #getLocalStorageDir()} directory.
	 * 
	 * This file will be loaded when {@link #loadSettings()} is executed.
	 */
	public static void configureLog4j() {
		URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/log4j.properties");
		PropertyConfigurator.configure(resource);
		logger = Logger.getLogger(AppSettings.class);
		FileAppender appndr = (FileAppender) Logger.getRootLogger().getAppender(FILE_APPENDER_NAME);
		logger.debug("Logging to file with location " + appndr.getFile());
		org.jdesktop.beansbinding.util.logging.Logger.getLogger(ELProperty.class.getName()).setLevel(Level.SEVERE);
	}
	
	private AppSettings() {
		settings = new Settings();
	}

	public static AppSettings getInstance() {
		return INSTANCE;
	}

	public void loadSettings() {
		loadAddtionalLog4JSettings();
		logger.debug("Initializing ApplicationSettings..");
		synchronized(this) {
			try { 
				File settingsFile = createSettingsFileIfAbsent();
				if (jaxbContext == null) {
					logger.debug("Instantiating JAXB context..");
					jaxbContext = JAXBContext.newInstance( Settings.class );
				}
				logger.debug("Loading application settings from file " + settingsFile.getAbsolutePath());
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				settings = (Settings) unmarshaller.unmarshal(settingsFile);
			} catch(JAXBException jaxbExc) {
				logger.error("Exception while instantiating JAXB context", jaxbExc);
			} catch(Exception exc) {
				logger.error("Exception thrown while loading settings file", exc);
				if (exc.getMessage() != null) {
					logger.error("Settings file seems to be corrupt. Attempting to delete..", exc);
					try {
						ApplicationContext appContext = Application.getInstance().getContext();
						appContext.getLocalStorage().deleteFile(SETTINGS_FILE_NAME);
						logger.warn("Settings file deleted. Default settings will be applied");
					} catch (IOException e) {
						logger.error("Removing corrupt settings file failed", e);
					}
				}
			} finally {
				if (settings == null) {
					settings = new Settings();
					saveSettings();
				}
			}
		}
		logger.debug("Found videodir: " + getVideoDir().getAbsolutePath());
		logger.debug("Found uncompressed videodir: " + getUncompressedVideoDir().getAbsolutePath());
	}
	
	/**
	 * Looks for additional log4j files in the user.home directory of the application.
	 * Use this to store specific appenders that require easy configuration. 
	 */
	private void loadAddtionalLog4JSettings() {
		File resource = new File(getLocalStorageDir(), "log4j.properties");
		if (resource.exists()) {
			logger.debug("Loading additional log4J properties from " + resource.getAbsolutePath());
			PropertyConfigurator.configure(resource.getAbsolutePath());
		}
	}

	public void saveSettings() {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, FILE_ENCODING);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			File settingsFile = createSettingsFileIfAbsent();
			logger.debug("Saving application settings to file " + settingsFile.getAbsolutePath());
			marshaller.marshal(settings, settingsFile);
		} catch (Exception exc) {
			logger.error("Exception thrown while saving settings to file " + SETTINGS_FILE_NAME, exc);
		} 
	}
	
	private File createSettingsFileIfAbsent() throws IOException {
		File settingsFile = new File(getLocalStorageDir(), SETTINGS_FILE_NAME);
		File settingsFolder = settingsFile.getParentFile();
		// check if parent folder and settings file exists, create them otherwise
		if (!(settingsFolder.exists() || settingsFolder.mkdirs()) && 
			!(settingsFile.exists() || settingsFile.createNewFile())) {
			throw new FileNotFoundException("Settings file could not be created");
		}
		return settingsFile;
	}
	
	public File getLocalStorageDir() {
		ApplicationContext appContext = Application.getInstance().getContext();
		return appContext.getLocalStorage().getDirectory();
	}

	public Settings getSettings() {
		return settings;
	}

	/**
	 * Set a new directory for storing and reading compressed video files. 
	 * The directory will be lazily reloaded by clearing the cached value.
	 * 
	 * @param dir The new directory
	 */
	public void setVideoDir(File dir) {
		getSettings().videoDir = dir.getAbsolutePath();
		// reset the cached value for this directory so it will be reinitialized during the next request
		videoDir = null;
	}

	public File getVideoDir() {
		synchronized(this) {
			if (videoDir == null) {
				File defaultDir = new File(System.getProperty("user.dir"));
				videoDir = getDirectory(getSettings().videoDir, defaultDir);
			}
		}
		return videoDir;
	}

	public void setCapturerName(String capturerName) {
		getSettings().capturerName = capturerName;	
	}

	/**
	 * Returns the first chosen capturer of the last program instance.
	 * @return The save capturer name
	 */
	public String getCapturerName() {
		return getSettings().capturerName;
	}

	/**
	 * Set a new directory for storing and reading uncompressed video files. 
	 * The directory will be lazily reloaded by clearing the cached value.
	 * 
	 * @param dir The new directory
	 */
	public void setUncompressedVideoDir(File dir) {
		getSettings().uncompressedVideoDir = dir.getAbsolutePath();
		// reset the cached value for this directory so it will be reinitialized during the next request
		uncompressedVideoDir = null;
	}

	public File getUncompressedVideoDir() {
		synchronized(this) {
			if (uncompressedVideoDir == null) {
				File defaultDir = new File(getVideoDir(), UNCOMPRESSED_VIDEO_DIRNAME);
				uncompressedVideoDir = getDirectory(getSettings().uncompressedVideoDir, defaultDir);
			}
		}
		return uncompressedVideoDir;
	}

	public File getDirectory(String path, File defaultDir) {
		File result = null;
		// first check whether the path in the settings is null or not
		if (path == null) {
			// if null, use default layout and create
			result = defaultDir;
		} else {
			// if not null, then try to create directory
			File dir = new File(path);
			result = AppUtil.createDirectories(dir, defaultDir);
		}
		return result;
	}

	public File getLogFile() {
		if (logFile == null || !logFile.exists()) {
			FileAppender appndr = (FileAppender) Logger.getRootLogger().getAppender(FILE_APPENDER_NAME);
			String fileName = appndr.getFile();
			logFile = new File(fileName);
		}
		return logFile;
	}

	public float getSavedVolume() {
		return getSettings().savedVolume;
	}

	public void setSavedVolume(float savedVolume) {
		getSettings().savedVolume = savedVolume;
	}

	public float getPlayRate() {
		return getSettings().playRate;
	}

	public void setPlayRate(float rateIndex) {
		getSettings().playRate = rateIndex;
	}

	public String getTranscoderName() {
		return getSettings().transcoderName;
	}
	
	public String getCaptureEncoderName() {
		return getSettings().captureEncoderName;
	}
	
	public void setCaptureEncoderName(String captureEncoderName) {
		getSettings().captureEncoderName = captureEncoderName;
	}
	
	public String getLogFileUploadUrl() {
		return getSettings().logFileUploadUrl;
	}
	
	public String getDbUrl() {
		return getSettings().dbUrl;
	}
	
	public String getDbPassword() {
		return getSettings().dbPassword;
	}
	
	public String getDbUser() {
		return getSettings().dbUser;
	}
	
	public VideoFolderRetrievalStrategy getVideoFolderRetrievalStrategy() {
		if (getSettings().videoFolderRetrievalStrategy == null) {
			getSettings().videoFolderRetrievalStrategy = new DefaultVideoFolderRetrievalStrategy();
		}
		return getSettings().videoFolderRetrievalStrategy;
	}

	
	public void setVideoFolderRetrievalStrategy(VideoFolderRetrievalStrategy videoFolderRetrievalStrategy) {
		getSettings().videoFolderRetrievalStrategy = videoFolderRetrievalStrategy;
	}
	
	public String getVlcPath() {
		return getSettings().vlcPath;
	}
	
	public void setVlcPath(String vlcPath) {
		getSettings().vlcPath = vlcPath;
	}
	
	public List<String> getVideoCapturerFactories() {
		return Arrays.asList(getSettings().videoCapturerFactories);
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Settings implements Serializable {

		private String videoDir = "D:\\Video's";

		private String uncompressedVideoDir;

		/**
		 * The last selected capturer on startup.
		 */
		private String capturerName;

		private float playRate;

		private float savedVolume;

		private String transcoderName = "XviD MPEG-4 Codec";
		
		private String captureEncoderName = "none";
		
		private String logFileUploadUrl = "http://www.runwalk.be/index.php/logs/upload";

		private String[] videoCapturerFactories = new String[] {"com.runwalk.video.media.dsj.DSJCapturerFactory", "com.runwalk.video.media.ueye.UEyeCapturerFactory"};
		
		/** 
		 * The video folder retrieval strategy is cached here after lazy initialization with its stored format string 
		 */
		@XmlElementRef
		private VideoFolderRetrievalStrategy videoFolderRetrievalStrategy;
		
		private String vlcPath = "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe";
		
		private String dbUrl = "jdbc:mysql://localhost:3306";
		
		private String dbUser = "root";
		
		private String dbPassword = "Letmein3";
 
	}

}
