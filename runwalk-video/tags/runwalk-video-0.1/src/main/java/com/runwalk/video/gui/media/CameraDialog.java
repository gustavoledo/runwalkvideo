package com.runwalk.video.gui.media;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;

import com.google.common.collect.Iterables;
import com.runwalk.video.gui.AppDialog;

@SuppressWarnings("serial")
public class CameraDialog extends AppDialog {

	// class properties
	public static final String SELECTED_CAPTURER_NAME = "selectedCapturerName";

	// class actions
	public static final String INITIALIZE_CAPTURER_ACTION = "initializeCapturer";
	public static final String REFRESH_CAPTURER_ACTION = "refreshCapturers";
	private static final String SHOW_CAPTURER_SETTINGS_ACTION = "showCameraSettings";
	private static final String SHOW_CAPTURER_DEVICE_SETINGS_ACTION = "showCapturerSettings";
	private static final String DISMISS_DIALOG_ACTION = "dismissDialog";
	private static final String EXIT_ACTION = "exit";

	private JComboBox capturerComboBox;

	private String selectedCapturerName;
	
	private final String defaultCapturerName;

	private final int capturerId;

	private String selectedMonitorId;

	private JPanel buttonPanel;
	
	private boolean cancelled = false;

	/**
	 * Create a dialog that allows the user to start a capture device. Selection notification will be done by firing {@link PropertyChangeEvent}s 
	 * to registered listeners.
	 * 
	 * @param parent The parent {@link Frame} whose focusing behavior will be inherited
	 * @param actionMap An optional {@link ActionMap} which the {@link Dialog} can use to add extra {@link javax.swing.Action}s
	 * @param capturerId The unique id of the newly opened capturer. This will be used to determine the default monitor to run on
	 * @param defaultCapturer The name of the default selected capturer
	 */
	public CameraDialog(Frame parent, ActionMap actionMap, int capturerId, String defaultCapturerName) {
		super(parent, true);
		this.capturerId = capturerId;
		this.defaultCapturerName = defaultCapturerName;
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		// is the application starting up or already started?
		final boolean isReady = getApplication().isReady();
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (isReady) {
					dismissDialog();
				} else {
					Runtime.getRuntime().exit(0);
				}
			}

		});
		setLayout(new MigLayout("fill, nogrid"));
		setTitle(getResourceMap().getString("captureDeviceDlg.title")); // NOI18N
		setResizable(false);

		JLabel captureDeviceLabel = new JLabel(getResourceMap().getString("captureDeviceLabel.text")); // NOI18N
		add(captureDeviceLabel, "wrap");

		capturerComboBox = new JComboBox();
		add(capturerComboBox, "wrap, grow");

		buttonPanel = new JPanel(new MigLayout("fill, gap 0, insets 0"));
		buttonPanel.setVisible(false);
		add(buttonPanel, "wrap, grow, hidemode 3");

		if (!isReady) {
			javax.swing.Action cancelAction = getApplication().getApplicationActionMap().get(EXIT_ACTION);
			JButton cancelButton = new JButton(cancelAction); // NOI18N
			add(cancelButton, "grow");
		}
		JButton refreshButton = new JButton(getAction(REFRESH_CAPTURER_ACTION)); // NOI18N
		add(refreshButton, "align right");
		// add some extra actions to configure the capture device
		addAction(SHOW_CAPTURER_SETTINGS_ACTION, actionMap, true);
		addAction(SHOW_CAPTURER_DEVICE_SETINGS_ACTION, actionMap);
		JButton okButton = new JButton(getAction(DISMISS_DIALOG_ACTION));
		add(okButton, "align right");
		getRootPane().setDefaultButton(okButton);
		// populate combobox with the capture device list
		capturerComboBox.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				JComboBox source = (JComboBox) e.getSource();
				String capturerName = source.getSelectedItem().toString();
				setSelectedCapturerName(capturerName);
			}

		});
		pack();
		toFront();
	}
	
	private void addAction(String actionName, ActionMap actionMap) {
		addAction(actionName, actionMap, false);
	}

	private void addAction(String actionName, ActionMap actionMap, boolean wrap) {
		if (actionMap != null) {
			final javax.swing.Action action = actionMap.get(actionName);
			if (action != null) {
				JButton chooseCapturerSettings = new JButton(action);
				String wrapButton = wrap ? ", wrap" : "";
				add(chooseCapturerSettings, "align right, grow" + wrapButton);
			}
		}
	}

	public void setSelectedCapturerName(String selectedCapturerName) {
		firePropertyChange(SELECTED_CAPTURER_NAME, this.selectedCapturerName, this.selectedCapturerName = selectedCapturerName);
	}

	public String getSelectedCapturerName() {
		return selectedCapturerName;
	}
	
	public void dismissDialog() {
		dismissDialog(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cancelled"));
	}

	@Action
	public void dismissDialog(ActionEvent actionEvent) {
		setCancelled(actionEvent.getActionCommand().equals("cancelled"));
		setVisible(false);
		// release native screen resources
		dispose();
	}
	
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * This method refreshes the list with connected capture devices 
	 * and displaying devices. The layout of this dialog will be changed accordingly.
	 */
	@Action
	public boolean refreshCapturers() {
		// refresh capture devices by querying the capturer implementation for uninitialized capture devices
		List<String> capturerNames = VideoCapturerFactory.getInstance().getCapturerNames();
		// return if no capturers available
		if (capturerNames.isEmpty()) {
			JOptionPane.showMessageDialog(getParent(), getResourceMap().getString("refreshCapturers.errorMessage"));
			dismissDialog();
			return false;
		}
		// add the capturers to the gui
		addCapturers(capturerNames);
		// add some extra gui elements depending on the number of connected monitors
		addMonitors();
		return true;
	}
	
	/**
	 * Add the available capturers to the {@link Dialog}.
	 * 
	 * @param capturerNames The names of the available capturers
	 */
	private void addCapturers(List<String> capturerNames) {
		String[] captureDevicesArray = Iterables.toArray(capturerNames, String.class);
		capturerComboBox.setModel(new DefaultComboBoxModel(captureDevicesArray));
		// determine the default capturer name as the passed name if available, otherwise use the default combobox model selection
		String defaultCapturerName = capturerComboBox.getSelectedItem().toString();
		// retain the previous selection if there was one. Otherwise use the default selected capturer name
		if (selectedCapturerName == null && this.defaultCapturerName != null && 
				capturerNames.contains(this.defaultCapturerName)) {
			defaultCapturerName = this.defaultCapturerName;
		} else if (selectedCapturerName != null) {
			defaultCapturerName = selectedCapturerName;
		}
		// set the selected item on the combobox
		capturerComboBox.setSelectedItem(defaultCapturerName);
		// make another call to the setter here to make sure the selected capturer will be started
		setSelectedCapturerName(defaultCapturerName);
	}
	
	/**
	 * Add extra monitor selection buttons, only if there are more than 2 connected.  
	 */
	public void addMonitors() {
		// get graphics environment
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
		// let user choose on which screen to show the capturer, only if more than one is connected
		if (graphicsDevices.length > 2) {
			buttonPanel.removeAll();
			JLabel screenLabel = new JLabel("Kies een scherm ");
			buttonPanel.add(screenLabel, "wrap, grow, span");
			// create buttongroup for selecting monitor
			ButtonGroup screenButtonGroup = new ButtonGroup();
			// get the default monitor id for this capturer
			int defaultMonitorId = VideoComponent.getDefaultScreenId(graphicsDevices.length, capturerId);
			for (GraphicsDevice graphicsDevice : graphicsDevices) {
				String monitorIdString  = graphicsDevice.getIDstring();
				monitorIdString = monitorIdString.substring(monitorIdString.length() - 1);
				JButton button = new JButton(monitorIdString);
				button.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						JButton source = (JButton) e.getSource();
						String monitorId = source.getText();
						firePropertyChange(VideoComponent.MONITOR_ID, selectedMonitorId, selectedMonitorId = monitorId);
					}

				});
				button.setBackground(Color.WHITE);
				// set default screen selection
				int monitorId = Integer.parseInt(monitorIdString);
				if (defaultMonitorId == monitorId) {
					button.setSelected(true);
					screenButtonGroup.setSelected(button.getModel(), true);
				} else {
					screenButtonGroup.setSelected(button.getModel(), false);
				}
				screenButtonGroup.add(button);
				buttonPanel.add(button, "height 70!, width 70::, grow");
			}
			screenLabel.setVisible(true);
			buttonPanel.setVisible(true);
		}
	}

}
