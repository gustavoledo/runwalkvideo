package com.runwalk.video.gui.media;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.jdesktop.application.utils.PlatformType;

import com.google.common.collect.Lists;

import de.humatic.dsj.DSCapture;
import de.humatic.dsj.DSCapture.CaptureDevice;
import de.humatic.dsj.DSFilter.DSPin;
import de.humatic.dsj.DSFilterInfo;
import de.humatic.dsj.DSFilterInfo.DSPinInfo;
import de.humatic.dsj.DSFiltergraph;
import de.humatic.dsj.DSMediaType;

/**
 * This class is a concrete implementation for the DirectShow for Java (dsj) libary, 
 * which enables capturing on the {@link PlatformType#WINDOWS} platform.
 * 
 * @author Jeroen Peelaerts
 */
public class DSJCapturer extends DSJComponent<DSCapture> implements IVideoCapturer {

	public final static DSFilterInfo[] VIDEO_ENCODERS = {
		DSFilterInfo.doNotRender(), 
		DSFilterInfo.filterInfoForProfile("RunwalkVideoApp"),
		DSFilterInfo.filterInfoForName("XviD MPEG-4 Codec")
	};

	private static final int FLAGS = DSFiltergraph.DD7;

	private DSFilterInfo captureEncoder = VIDEO_ENCODERS[0];

	private DSFilterInfo filterInfo;

	private String capturerName;

	DSJCapturer(String capturerName) {
		this.capturerName = capturerName;
		filterInfo = DSFilterInfo.filterInfoForName(capturerName);
	}

	/**
	 * Get {@link DSFilterInfo} depending on the state in which the capturer is. 
	 * If a {@link DSFiltergraph} is already created, then this method will return 
	 * the {@link DSFilterInfo} of the active {@link CaptureDevice}.
	 * 
	 * @return The requested filter info for this capturer
	 */
	private DSFilterInfo getDSFilterInfo() {
		DSFilterInfo result = filterInfo;
		if (isActive()) {
			result = getCaptureDevice().getFilterInfo();
			filterInfo = null;
		}
		return result;
	}

	/** {@inheritDoc} */
	public void startCapturer() {
		setFiltergraph(new DSCapture(FLAGS, filterInfo, false, DSFilterInfo.doNotRender(), null));
		getFiltergraph().lockAspectRatio(true);
	}

	public List<String> getVideoFormats() {
		List<String> result = Lists.newArrayList();
		if (isActive()) {
			DSPin activePin = getActivePin();
			getLogger().debug("Currently active pin : "  + activePin.getName());
			DSPinInfo pinInfo = activePin.getPinInfo();
			for (DSMediaType mediaFormat : pinInfo.getFormats()) {
				String format = mediaFormat.getDisplayString() + " @ " + mediaFormat.getFrameRate();
				result.add(format);
			}
		} else {
			DSPinInfo[] ouputPins = getDSFilterInfo().getDownstreamPins();
			for (DSPinInfo pinInfo : ouputPins) {
				for (DSMediaType mediaFormat : pinInfo.getFormats()) {
					String format = mediaFormat.getDisplayString() + " @ " + mediaFormat.getFrameRate();
					result.add(format);
				}
			}
		}
		return result;
	}

	public void setSelectedVideoFormatIndex(int index) {
		if (isActive()) {
//			getCaptureDevice().setOutputFormat(getActivePin(), index);
			getCaptureDevice().setOutputFormat(index);
		} else {
			int counter = 0;
			for (DSPinInfo pinInfo : getDSFilterInfo().getDownstreamPins()) {
				for(DSMediaType mediaFormat : pinInfo.getFormats()) {
					if (++ counter == index) {
						int formatIndex = Arrays.asList(pinInfo.getFormats()).indexOf(mediaFormat);
						pinInfo.setPreferredFormat(formatIndex);
					}
				}
			}
		}
		getLogger().debug("Pin " + getActivePin().getName() + " fps: " + getActiveDeviceFps());
	}

	/**
	 * Return the {@link CaptureDevice} for the current {@link DSFiltergraph} if there is one.
	 * 
	 * @return The active {@link CaptureDevice}
	 */
	protected CaptureDevice getCaptureDevice() {
		return getFiltergraph() != null ? getFiltergraph().getActiveVideoDevice() : null;
	}

	private DSPin getActivePin() {
		CaptureDevice vDev = getFiltergraph().getActiveVideoDevice();
		DSPin previewOut = vDev.getDeviceOutput(DSCapture.CaptureDevice.PIN_CATEGORY_PREVIEW);
		DSPin captureOut = vDev.getDeviceOutput(DSCapture.CaptureDevice.PIN_CATEGORY_CAPTURE);
		return previewOut != null ? previewOut : captureOut;
	}

	private float getActiveDeviceFps() {
		CaptureDevice vDev = getFiltergraph().getActiveVideoDevice();
		return vDev.getFrameRate(getActivePin());
	}

	private DSFilterInfo getCaptureEncoder() {
		return captureEncoder;
	}

	public void startRecording(File destFile) {
		getFiltergraph().setAviExportOptions(-1, -1, -1, getRejectPauseFilter(), -1);
		getFiltergraph().setCaptureFile(destFile.getAbsolutePath(), getCaptureEncoder(),	DSFilterInfo.doNotRender(),	true);
		getLogger().debug("Video encoder for " + getTitle() + " set to " + getCaptureEncoder().getName());
		getLogger().debug("Pause filter rejection set to " + getRejectPauseFilter());
		getFiltergraph().record();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				while(isRecording()) {
					getLogger().debug("captured: " + getFiltergraph().getFrameDropInfo()[0] + 
							" dropped: "+ getFiltergraph().getFrameDropInfo()[1]);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						getLogger().error(e);
					}
				}
			}
		});
		thread.start();
	}

	public void stopRecording() {
		getFiltergraph().record();
		getFiltergraph().setPreview();
	}

	public void togglePreview() {
		if (getFiltergraph().getState() == DSCapture.PREVIEW) {
			getFiltergraph().stop();
			getLogger().debug("Filtergraph for " + getTitle() + " stopped");
		} else {
			getFiltergraph().setPreview();
			getLogger().debug("Filtergraph for " + getTitle() + " set to preview mode");
		}
	}

	public void showCaptureSettings() {
		getFiltergraph().getActiveVideoDevice().showDialog(DSCapture.CaptureDevice.WDM_DEVICE);
	}

	public void showCameraSettings() {
		getFiltergraph().getActiveVideoDevice().showDialog(DSCapture.CaptureDevice.WDM_CAPTURE);
	}

	public void setSelectedCaptureEncoderIndex(int index) {
		captureEncoder = VIDEO_ENCODERS[index];
	}

	public String getSelectedCaptureEncoderName() {
		return captureEncoder.getName();
	}

	public List<String> getCaptureEncoders() {
		List<String> result = Lists.newArrayList();
		for (DSFilterInfo videoEncoder : VIDEO_ENCODERS) {
			result.add(videoEncoder.getName());
		}
		return result;
	}

	/**
	 * This method should return the name of the capturer, 
	 * which was originally provided by {@link VideoCapturerFactory#initializeCapturer(String)}.
	 */
	public String getTitle() {
		return capturerName;
	}

	private boolean isRecording() {
		return getFiltergraph().getState() == DSCapture.RECORDING;
	}


}
