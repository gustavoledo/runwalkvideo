package com.runwalk.video.gui.media;

import java.io.File;
import java.util.Arrays;

import org.jdesktop.application.Action;

import com.runwalk.video.util.AppSettings;

import de.humatic.dsj.DSCapture;
import de.humatic.dsj.DSFilterInfo;
import de.humatic.dsj.DSFiltergraph;
import de.humatic.dsj.DSMediaType;
import de.humatic.dsj.DSCapture.CaptureDevice;
import de.humatic.dsj.DSFilter.DSPin;

public class DSJCapturer extends DSJComponent<DSCapture> implements IVideoCapturer {

	private static final DSFilterInfo[] VIDEO_ENCODERS = AppSettings.VIDEO_ENCODERS;

	private static DSFilterInfo[][] dsi;

	/**
	 * The selected capture device for this recorder
	 */
	private DSFilterInfo selectedDevice = null;

	private DSFilterInfo captureEncoder = VIDEO_ENCODERS[0];
	
	public void initCapturer() {
		//FIXME no more propertychangelisteners here for now!! see what is needed to replace this functionality
		setFiltergraph(new DSCapture(DSFiltergraph.DD7, selectedDevice, false, DSFilterInfo.doNotRender(), null));
		getFiltergraph().lockAspectRatio(true);
	}

	public String[] getVideoFormats() {
		DSPin activePin = getActivePin();
		getLogger().debug("Currently active pin : "  + activePin.getName());
		DSFilterInfo.DSPinInfo usedPinInfo = selectedDevice.getDownstreamPins()[activePin.getIndex()];
		DSMediaType[] mf = usedPinInfo.getFormats();
		String[] formats = new String[mf.length];

		for (int i = 0; i < mf.length; i++) {
			formats[i] = mf[i].getDisplayString() + " @ " + mf[i].getFrameRate();
		}
		return formats;
	}

	public void setSelectedVideoFormatIndex(int index) {
		DSPin activePin = getActivePin();
		getFiltergraph().getActiveVideoDevice().setOutputFormat(activePin, index);
		getFiltergraph().getActiveVideoDevice().setOutputFormat(index);
		getLogger().debug("Pin " + getActivePin().getName() + " fps: " + getActiveDeviceFps());
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
		getLogger().debug("Video encoder = " + getCaptureEncoder().getName() + ".");
		getLogger().debug("Pause filter rejection set to " + getRejectPauseFilter()+ ".");
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

	@Action
	public void togglePreview() {
		if (getFiltergraph().getState() == DSCapture.PREVIEW) {
			getFiltergraph().stop();
		} else {
			getFiltergraph().setPreview();
		}
	}

	public void showCaptureSettings() {
		getFiltergraph().getActiveVideoDevice().showDialog(DSCapture.CaptureDevice.WDM_DEVICE);
	}

	public void showCameraSettings() {
		getFiltergraph().getActiveVideoDevice().showDialog(DSCapture.CaptureDevice.WDM_CAPTURE);
	}
	
	public String[] getCaptureDevices() {
		dsi = DSCapture.queryDevices(1);
		String[] devices = new String[dsi[0].length];
		for (int i = 0; i < dsi[0].length; i++) {
			devices[i] = dsi[0][i].getName();
		}
		return devices;
	}

	public void setSelectedCaptureDeviceIndex(int selectedIndex) {
		this.selectedDevice = dsi[0][selectedIndex];
	}

	public int getSelectedCaptureDeviceIndex() {
		return Arrays.asList(dsi[0]).indexOf(selectedDevice);
	}
	
	public void setSelectedCaptureEncoderIndex(int index) {
		this.captureEncoder = VIDEO_ENCODERS[index];
	}
	
	public String getSelectedCaptureEncoderName() {
		return captureEncoder.getName();
	}

	public String[] getCaptureEncoders() {
		String[] result = new String[VIDEO_ENCODERS.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = VIDEO_ENCODERS[i].getName();
		}
		return result;
	}

	public String getName() {
		return selectedDevice != null ? selectedDevice.getName() : "";
	}
	
	protected boolean isRecording() {
		return getFiltergraph().getState() == DSCapture.RECORDING;
	}


}