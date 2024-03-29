package com.runwalk.video.panels;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;
import org.jdesktop.application.Task.BlockingScope;
import org.jdesktop.application.TaskEvent;
import org.jdesktop.application.TaskListener;
import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.swingx.table.DatePickerCellEditor;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TreeList;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.AutoCompleteSupport.AutoCompleteCellEditor;
import ca.odell.glazedlists.swing.TreeTableSupport;

import com.runwalk.video.core.OnEdt;
import com.runwalk.video.dao.DaoService;
import com.runwalk.video.entities.Client;
import com.runwalk.video.entities.RedcordExercise;
import com.runwalk.video.entities.RedcordSession;
import com.runwalk.video.entities.RedcordTableElement;
import com.runwalk.video.entities.RedcordTableElement.ExerciseDirection;
import com.runwalk.video.entities.RedcordTableElement.ExerciseType;
import com.runwalk.video.settings.SettingsManager;
import com.runwalk.video.tasks.CalendarSlotSyncTask;
import com.runwalk.video.tasks.DeleteTask;
import com.runwalk.video.tasks.PersistTask;
import com.runwalk.video.ui.table.DatePickerTableCellRenderer;
import com.runwalk.video.ui.table.JComboBoxTableCellRenderer;
import com.runwalk.video.ui.table.JSpinnerTableCellEditor;
import com.runwalk.video.ui.table.JSpinnerTableCellRenderer;
import com.runwalk.video.util.AppUtil;

@SuppressWarnings("serial")
public class RedcordTablePanel extends AbstractTablePanel<RedcordTableElement> {

	private static final String REDCORD_SESSION_SELECTED = "redcordSessionSelected";
	private static final String REDCORD_EXERCISE_SELECTED = "redcordExerciseSelected";

	private static final String SYNC_TO_DATABASE_ACTION = "syncToDatabase";
	private static final String ADD_REDCORD_EXCERCISE_ACTION = "addRedcordExercise";
	private static final String DELETE_REDCORD_EXCERCISE_ACTION = "deleteRedcordExercise";
	private static final String ADD_REDCORD_SESSION_ACTION = "addRedcordSession";
	private static final String DELETE_REDCORD_SESSION_ACTION = "deleteRedcordSession";

	private final ClientTablePanel clientTablePanel;

	private final DaoService daoService;

	private JTextArea comments;

	private Boolean clientSelected = false;

	private Boolean redcordSessionSelected = false;

	private Boolean redcordExerciseSelected = false;

	/**
	 * Create the panel.
	 */
	public RedcordTablePanel(ClientTablePanel clientTablePanel, UndoableEditListener undoableEditListener, 
			DaoService daoService) {
		super(new MigLayout("fill, nogrid"));
		this.clientTablePanel = clientTablePanel;
		this.daoService = daoService;

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(getTable());
		add(scrollPane, "wrap, grow, height :130:");

		setFirstButton(new JButton(getAction(ADD_REDCORD_SESSION_ACTION)));
		getFirstButton().setFont(SettingsManager.MAIN_FONT);
		add(getFirstButton());

		setSecondButton(new JButton(getAction(DELETE_REDCORD_SESSION_ACTION)));
		getSecondButton().setFont(SettingsManager.MAIN_FONT);
		add(getSecondButton());

		addButton(ADD_REDCORD_EXCERCISE_ACTION);
		addButton(DELETE_REDCORD_EXCERCISE_ACTION);
		addButton(SYNC_TO_DATABASE_ACTION, "wrap");

		JScrollPane tscrollPane = new JScrollPane();
		tscrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		comments = new JTextArea();
		comments.getDocument().addUndoableEditListener(undoableEditListener);
		comments.setFont(SettingsManager.MAIN_FONT);
		comments.setColumns(20);
		comments.setRows(3);
		tscrollPane.setViewportView(comments);
		add(tscrollPane, "grow, height :60:");

		BindingGroup bindingGroup = new BindingGroup();
		//comments JTextArea binding
		BeanProperty<RedcordTablePanel, String> selectedItemComments = BeanProperty.create("selectedItem.comments");
		BeanProperty<JTextArea, String> jTextAreaValue = BeanProperty.create("text");
		Binding<?, String, JTextArea, String> commentsBinding = 
				Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, this, selectedItemComments, comments, jTextAreaValue);
		bindingGroup.addBinding(commentsBinding);
		/** This listener can be added to each binding group that contains bindings that have a {@link ClientTablePanel} as source. */
		// update session manually if it is a synthetic element
		commentsBinding.addBindingListener(new AbstractBindingListener() {
			
			@Override
			public void targetChanged(@SuppressWarnings("rawtypes") Binding binding, PropertyStateEvent event) {
				// check if a change was made on a synthetic node
				updateSelectionModel();
				getClientTablePanel().getSelectedItem().setDirty(true);
			}

			@OnEdt
			private void updateSelectionModel() {
				// update selected row..
				int firstRow = getEventSelectionModel().getMinSelectionIndex();
				int lastRow = getEventSelectionModel().getMaxSelectionIndex();
				getEventTableModel().fireTableRowsUpdated(firstRow, lastRow);
			}

		});

		BeanProperty<RedcordTablePanel, Boolean> isSelected = BeanProperty.create(ROW_SELECTED);
		BeanProperty<JTextArea, Boolean> jTextAreaEnabled = BeanProperty.create("enabled");
		Binding<?, Boolean, JTextArea, Boolean> enableCommentsBinding = Bindings.createAutoBinding(UpdateStrategy.READ, this, 
				isSelected, comments, jTextAreaEnabled);
		enableCommentsBinding.setSourceUnreadableValue(false);
		bindingGroup.addBinding(enableCommentsBinding);

		BeanProperty<ClientTablePanel, Boolean> isClientSelected = BeanProperty.create(ROW_SELECTED);
		BeanProperty<RedcordTablePanel, Boolean> clientSelected = BeanProperty.create(CLIENT_SELECTED);
		Binding<?, Boolean, RedcordTablePanel, Boolean> clientSelectedBinding = 
				Bindings.createAutoBinding(UpdateStrategy.READ, clientTablePanel, isClientSelected, this, clientSelected);
		clientSelectedBinding.setSourceNullValue(false);
		clientSelectedBinding.setSourceUnreadableValue(false);
		bindingGroup.addBinding(clientSelectedBinding);

		ELProperty<RedcordTablePanel, Boolean> isRedcordSessionSelected = ELProperty.create("${rowSelected && selectedItem.class.simpleName == 'RedcordSession'}");
		BeanProperty<RedcordTablePanel, Boolean> redcordSessionSelected = BeanProperty.create(REDCORD_SESSION_SELECTED);
		Binding<? extends AbstractTablePanel<?>, Boolean, RedcordTablePanel, Boolean> selectedRedcordSessionBinding = 
				Bindings.createAutoBinding(UpdateStrategy.READ, this, isRedcordSessionSelected, this, redcordSessionSelected);
		selectedRedcordSessionBinding.setSourceUnreadableValue(false);
		selectedRedcordSessionBinding.setTargetNullValue(false);
		bindingGroup.addBinding(selectedRedcordSessionBinding);

		ELProperty<RedcordTablePanel, Boolean> isRedcordExerciseSelected = ELProperty.create("${rowSelected && selectedItem.class.simpleName == 'RedcordExercise'}");
		BeanProperty<RedcordTablePanel, Boolean> redcordExerciseSelected = BeanProperty.create(REDCORD_EXERCISE_SELECTED);
		Binding<? extends AbstractTablePanel<?>, Boolean, RedcordTablePanel, Boolean> selectedRedcordExerciseBinding = 
				Bindings.createAutoBinding(UpdateStrategy.READ, this, isRedcordExerciseSelected, this, redcordExerciseSelected);
		selectedRedcordExerciseBinding.setSourceUnreadableValue(false);
		selectedRedcordExerciseBinding.setTargetNullValue(false);
		bindingGroup.addBinding(selectedRedcordExerciseBinding);

		bindingGroup.bind();
	}

	private void addButton(String actionName, String layoutConstraints) {
		JButton button = new JButton(getAction(actionName));
		button.setFont(SettingsManager.MAIN_FONT);
		add(button, layoutConstraints);
	}

	private void addButton(String actionName) {
		addButton(actionName, "");
	}

	@Action(enabledProperty = CLIENT_SELECTED, block = BlockingScope.ACTION)
	public PersistTask<RedcordSession> addRedcordSession() {
		// insert a new session record
		final Client selectedClient = getClientTablePanel().getSelectedItem();
		if (("".equals(selectedClient.getName())  || selectedClient.getName() == null) && 
				("".equals(selectedClient.getOrganization()) || selectedClient.getOrganization() == null)) {
			JOptionPane.showMessageDialog(
					SwingUtilities.windowForComponent(this), 
					getResourceMap().getString("addRedcordSession.errorDialog.text"),
					getResourceMap().getString("addRedcordSession.Action.text"), 
					JOptionPane.ERROR_MESSAGE);
			getLogger().warn("Attempt to insert redcordSession for " + selectedClient + " failed.");
			return null;
		}
		RedcordSession redcordSession = new RedcordSession(selectedClient); 
		PersistTask<RedcordSession> result = new PersistTask<RedcordSession>(getDaoService(), RedcordSession.class, redcordSession);
		result.addTaskListener(new TaskListener.Adapter<RedcordSession, Void>() {

			@Override
			public void succeeded(TaskEvent<RedcordSession> event) {
				addRedcordSession(selectedClient, event.getValue(), true);
			}

		});
		return result;
	}

	private void addRedcordSession(Client client, RedcordSession redcordSession, boolean changeSelection) {
		getItemList().getReadWriteLock().writeLock().lock();
		try {
			client.addRedcordSession(redcordSession);
			if (changeSelection) {
				setSelectedItemRow(redcordSession);
			}
		} finally {
			getItemList().getReadWriteLock().writeLock().unlock();
		}
	}

	@Action(enabledProperty = REDCORD_SESSION_SELECTED, block = BlockingScope.ACTION)
	public DeleteTask<RedcordSession> deleteRedcordSession() {		
		DeleteTask<RedcordSession> result = null;
		int n = JOptionPane.showConfirmDialog(
				SwingUtilities.windowForComponent(this),
				getResourceMap().getString("deleteRedcordSession.confirmDialog.text"),
				getResourceMap().getString("deleteRedcordSession.Action.text"),
				JOptionPane.WARNING_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		if (n == JOptionPane.OK_OPTION) {
			final Client selectedClient = getClientTablePanel().getSelectedItem();
			result = new DeleteTask<RedcordSession>(getDaoService(), RedcordSession.class, (RedcordSession) getSelectedItem());
			result.addTaskListener(new TaskListener.Adapter<RedcordSession, Void>() {

				@Override
				public void succeeded(TaskEvent<RedcordSession> event) {
					deleteRedcordSession(selectedClient, event.getValue(), true);
				}

			});
		}
		return result;
	}

	private void deleteRedcordSession(Client client, RedcordSession redcordSession, boolean changeSelection) {
		getItemList().getReadWriteLock().writeLock().lock();
		try {
			int lastSelectedRowIndex = getEventSelectionModel().getMinSelectionIndex();
			client.removeRedcordSession(redcordSession);
			// set selection on previous item
			if (lastSelectedRowIndex > 0 && changeSelection) {
				setSelectedItemRow(lastSelectedRowIndex - 1);
			}
		} finally {
			getItemList().getReadWriteLock().writeLock().unlock();
		}
	}

	/**
	 * Rename a {@link List} of {@link RedcordSession}s according to their natural order. This method does not 
	 * explicitly require newRedcordSession to be in the client's redcordSession list. If addOrRemoveSession 
	 * is <code>false</code>, then it does not
	 * matter whether the redcordSession's exercise {@link List} contains newRedcordExercise or not.
	 * 
	 * @param client The client whose redcordSessions will be renamed
	 * @param newRedcordSession The redcordSession to be added or removed from the client's redcordSession list
	 * @param addOrRemoveSession <code>true</code> if the second argument needs to be added to the list, 
	 * <code>false</code> if it needs to be removed, <code>null</code> if nothing should happen
	 * @return The list containing the renamed redcordSessions
	 */
	public List<RedcordSession> renameRedcordSessions(Client client, RedcordSession newRedcordSession, Boolean addOrRemoveSession) {
		List<RedcordSession> result = new ArrayList<RedcordSession>(client.getRedcordSessions());
		boolean isDirty = addOrRemoveSession != null;
		if (isDirty) {
			result.add(newRedcordSession);
		} else {
			result.remove(newRedcordSession);
		}
		Collections.sort(result);
		for (int i = 0; i < result.size(); i++) {
			String name = getResourceMap().getString("addRedcordSession.Action.defaultName", i);
			RedcordSession redcordSession = result.get(i);
			if (!name.equals(redcordSession.getName()) && !isDirty) {
				client.setDirty(true);
			}
			redcordSession.setName(name);
		}
		return result;
	}

	public List<RedcordSession> renameRedcordSessions(Client client) {
		return renameRedcordSessions(client, null, null);
	}

	/**
	 * Rename a {@link List} of {@link RedcordExercise}s according to their natural order. If addOrRemoveExercise is <code>true</code>, then
	 * newRedcordExercise should not already be in the redcordSession's exercise {@link List}. If addOrRemoveExercise is <code>false</code>, then it does not
	 * matter whether the redcordSession's exercise {@link List} contains newRedcordExercise or not.
	 * 
	 * @param client The client whose redcordSessions will be renamed
	 * @param addOrRemoveExercise The redcordExercise to be added or removed from the redcordSession's exercise list
	 * @param addOrRemoveExercise <code>true</code> if the second argument needs to be added to the list, 
	 * <code>false</code> if it needs to be removed, <code>null</code> if nothing should happen
	 * @return The list containing the renamed redcordExercises
	 */
	public List<RedcordExercise> renameRedcordExercises(RedcordSession redcordSession, RedcordExercise newRedcordExercise, Boolean addOrRemoveExercise) {
		List<RedcordExercise> result = new ArrayList<RedcordExercise>(redcordSession.getRedcordExercises());
		boolean isDirty = addOrRemoveExercise != null;
		if (addOrRemoveExercise != null) {
			if (addOrRemoveExercise) {
				result.add(newRedcordExercise);
			} else {
				result.remove(newRedcordExercise);
			}
		}
		Collections.sort(result);
		for (int i = 0; i < result.size(); i++) {
			String name = getResourceMap().getString("addRedcordExercise.Action.defaultName", i);
			RedcordExercise redcordExercise = result.get(i);
			if (!name.equals(redcordExercise.getName()) && !isDirty) {
				redcordSession.getClient().setDirty(true);
			}
			redcordExercise.setName(name);
		}
		return result;
	}

	public List<RedcordExercise> renameRedcordExercises(RedcordSession redcordSession) {
		return renameRedcordExercises(redcordSession, null, null);
	}

	@Action(enabledProperty = ROW_SELECTED, block = BlockingScope.ACTION)
	public PersistTask<RedcordExercise> addRedcordExercise() {
		// insert a new exercise record
		RedcordSession selectedRedcordSession = null;
		if (getSelectedItem() instanceof RedcordSession) {
			selectedRedcordSession = (RedcordSession) getSelectedItem();
		} else {
			selectedRedcordSession = ((RedcordExercise) getSelectedItem()).getRedcordSession();
		}
		final RedcordSession finalSelectedRedcordSession = selectedRedcordSession;
		// get exercise count and set name
		int exerciseCount = selectedRedcordSession.getRedcordExerciseCount();
		String exerciseName = getResourceMap().getString("addRedcordExercise.Action.defaultExerciseName", ++exerciseCount);
		RedcordExercise redcordExercise = new RedcordExercise(selectedRedcordSession, exerciseName);
		PersistTask<RedcordExercise> result = new PersistTask<RedcordExercise>(getDaoService(), RedcordExercise.class, redcordExercise);
		result.addTaskListener(new TaskListener.Adapter<RedcordExercise, Void>() {

			@Override
			public void succeeded(TaskEvent<RedcordExercise> event) {
				RedcordExercise redcordExercise = event.getValue();
				getItemList().getReadWriteLock().writeLock().lock();
				try {
					renameRedcordExercises(finalSelectedRedcordSession, redcordExercise, true);
					finalSelectedRedcordSession.addRedcordExercise(redcordExercise);
					setSelectedItemRow(redcordExercise);
				} finally {
					getItemList().getReadWriteLock().writeLock().unlock();
				}
			}

		});
		return result;
	}

	@Action(enabledProperty = REDCORD_EXERCISE_SELECTED, block = BlockingScope.ACTION)
	public DeleteTask<RedcordExercise> deleteRedcordExercise() {		
		DeleteTask<RedcordExercise> result = null;
		int n = JOptionPane.showConfirmDialog(
				SwingUtilities.windowForComponent(this),
				getResourceMap().getString("deleteRedcordExercise.confirmDialog.text"),
				getResourceMap().getString("deleteRedcordExercise.Action.text"),
				JOptionPane.WARNING_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		if (n == JOptionPane.OK_OPTION) {
			RedcordExercise selectedRedcordExercise = (RedcordExercise) getSelectedItem();
			final RedcordSession selectedRedcordSession = selectedRedcordExercise.getRedcordSession();
			result = new DeleteTask<RedcordExercise>(getDaoService(), RedcordExercise.class, selectedRedcordExercise);
			result.addTaskListener(new TaskListener.Adapter<RedcordExercise, Void>() {

				@Override
				public void succeeded(TaskEvent<RedcordExercise> event) {
					RedcordExercise redcordExercise = event.getValue();
					getItemList().getReadWriteLock().writeLock().lock();
					try {
						renameRedcordExercises(selectedRedcordSession, redcordExercise, false);
						int lastSelectedRowIndex = getEventSelectionModel().getMinSelectionIndex();
						selectedRedcordSession.removeRedcordExercise(redcordExercise);
						// set selection on previous item (there will always be one)
						setSelectedItemRow(lastSelectedRowIndex - 1);
					} finally {
						getItemList().getReadWriteLock().writeLock().unlock();
					}
				}

			});
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected TreeList<RedcordTableElement> specializeItemList(EventList<RedcordTableElement> eventList) {
		final Comparator<RedcordTableElement> redcordTableElementComparator = GlazedLists.chainComparators(
				GlazedLists.beanPropertyComparator(RedcordTableElement.class, "name")
				);

		TreeList.Format<RedcordTableElement> listFormat = new TreeList.Format<RedcordTableElement>() {

			public void getPath(List<RedcordTableElement> paramList, RedcordTableElement redcordTableElement) {
				if (!redcordTableElement.allowsChildren()) {
					RedcordExercise redcordExercise = (RedcordExercise) redcordTableElement;
					// always add the session up front
					paramList.add(redcordExercise.getRedcordSession());
					paramList.add(redcordTableElement);
				} else {
					paramList.add(redcordTableElement);
				}
			}

			public boolean allowsChildren(RedcordTableElement redcordTableElement) {
				return redcordTableElement.allowsChildren();
			}

			public Comparator<RedcordTableElement> getComparator(int paramInt) {
				return redcordTableElementComparator;
			}

		};
		return new TreeList<RedcordTableElement>(eventList, listFormat, TreeList.NODES_START_EXPANDED);
	}

	/**
	 * Covariant return here. We can do this cast because the return type of
	 * {@link #specializeItemList(EventList)} is a {@link TreeList}, as well.
	 * 
	 * @return a treeList containing the items for this panel
	 */
	@Override
	public TreeList<RedcordTableElement> getItemList() {
		return (TreeList<RedcordTableElement>) super.getItemList();
	}

	public void initialiseTableColumnModel() {
		getTable().setRowHeight(20);
		JComboBoxTableCellRenderer comboBoxTableCellRenderer = new JComboBoxTableCellRenderer();
		// name of the session / exercise
		getTable().getColumnModel().getColumn(0).setMinWidth(70);
		getTable().getColumnModel().getColumn(0).setResizable(false);
		// date of the session ... workaround for #SWINGX-1086
		DatePickerCellEditor datePickerCellEditor = new DatePickerCellEditor(AppUtil.DATE_FORMATTER);
		// TODO retest date selection...
		datePickerCellEditor.setClickCountToStart(1);
		getTable().getColumnModel().getColumn(1).setCellEditor(datePickerCellEditor);
		getTable().getColumnModel().getColumn(1).setCellRenderer(new DatePickerTableCellRenderer(AppUtil.DATE_FORMATTER));
		getTable().getColumnModel().getColumn(1).setPreferredWidth(70);

		SpinnerDateModel spinnerDateModel = new SpinnerDateModel();
		JSpinner spinner = new JSpinner(spinnerDateModel);
		spinner.setEditor(new JSpinner.DateEditor(spinner, AppUtil.HOUR_MINUTE_FORMATTER.toPattern()));
		getTable().getColumnModel().getColumn(2).setCellRenderer(JSpinnerTableCellRenderer.dateTableCellRenderer(AppUtil.HOUR_MINUTE_FORMATTER));
		getTable().getColumnModel().getColumn(2).setCellEditor(new JSpinnerTableCellEditor(spinner));
		getTable().getColumnModel().getColumn(2).setPreferredWidth(30);

		// create special table cell editor for selecting exercise type
		EventList<ExerciseType> exerciseTypes = GlazedLists.eventListOf(ExerciseType.values());
		AutoCompleteCellEditor<ExerciseType> exerciseTypeTableCellEditor = AutoCompleteSupport.createTableCellEditor(exerciseTypes);
		exerciseTypeTableCellEditor.getAutoCompleteSupport().getComboBox().setEditable(false);
		exerciseTypeTableCellEditor.getAutoCompleteSupport().setStrict(true);
		exerciseTypeTableCellEditor.getAutoCompleteSupport().setFirstItem(null);
		exerciseTypeTableCellEditor.getAutoCompleteSupport().setBeepOnStrictViolation(false);
		exerciseTypeTableCellEditor.setClickCountToStart(1);
		getTable().getColumnModel().getColumn(3).setCellRenderer(comboBoxTableCellRenderer);
		getTable().getColumnModel().getColumn(3).setCellEditor(exerciseTypeTableCellEditor);
		getTable().getColumnModel().getColumn(3).setPreferredWidth(50);

		EventList<ExerciseDirection> exerciseDirections = GlazedLists.eventListOf(ExerciseDirection.values());
		AutoCompleteCellEditor<ExerciseDirection> exerciseDirectionTableCellEditor = AutoCompleteSupport.createTableCellEditor(exerciseDirections);
		exerciseDirectionTableCellEditor.getAutoCompleteSupport().getComboBox().setEditable(false);
		exerciseDirectionTableCellEditor.getAutoCompleteSupport().setStrict(true);
		exerciseDirectionTableCellEditor.getAutoCompleteSupport().setBeepOnStrictViolation(false);
		exerciseDirectionTableCellEditor.getAutoCompleteSupport().setFirstItem(null);
		exerciseDirectionTableCellEditor.setClickCountToStart(1);
		getTable().getColumnModel().getColumn(4).setCellRenderer(comboBoxTableCellRenderer);
		getTable().getColumnModel().getColumn(4).setCellEditor(exerciseDirectionTableCellEditor);
		getTable().getColumnModel().getColumn(4).setResizable(false);
		getTable().getColumnModel().getColumn(4).setPreferredWidth(50);
		// install tree table support on the first column of the table
		TreeTableSupport.install(getTable(), getItemList(), 0);
		// workaround for issue #GLAZEDLISTS-462
		getEventSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				if (isRowSelected() && getSelectedItem() != null &&
						getEventSelectionModel().getSelected().isEmpty()) {
					setSelectedItemRow(getSelectedItem()); 
				}
			}

		});
	}

	@Action(block=BlockingScope.ACTION)
	public Task<?, ?> syncToDatabase() {
		Window parentWindow = SwingUtilities.getWindowAncestor(RedcordTablePanel.this);
		return new CalendarSlotSyncTask<RedcordSession>(parentWindow, getDaoService(), 
				RedcordSession.class, getClientTablePanel().getObservableElementList()) {

			@Override
			public List<RedcordSession> doInBackground() throws Exception {
				List<RedcordSession> result = super.doInBackground();
				// refresh clients with updated session data
				for(RedcordSession redcordSession : result) {
					Client client = redcordSession.getClient();
					// find detached entity and apply modifications
					ObservableElementList<Client> clientList = getClientTablePanel().getObservableElementList();
					clientList.getReadWriteLock().writeLock().lock();
					try {
						client = getClientTablePanel().findItem(client);
						if (client != null) {
							boolean isSelectedClient = getClientTablePanel().getSelectedItem() == client;
							if (redcordSession.isNew()) {
								// add the session to the client and update the ui
								addRedcordSession(client, redcordSession, isSelectedClient);
							} else if (redcordSession.isRemoved()) {
								// remove the session from the client and update the ui
								deleteRedcordSession(client, redcordSession, isSelectedClient);
							} else if (redcordSession.isModified()) {
								//renameRedcordSessions(client);
								client.replaceRedcordSession(redcordSession);
								// refresh manually instead of firing a pce
								clientList.elementChanged(client);
							}
						} else {
							getLogger().warn(redcordSession.toString() + " was not added, client set to null.");
						}
					} finally {
						clientList.getReadWriteLock().writeLock().unlock();
					}
				}
				return result;
			}

		};
	}

	public boolean isClientSelected() {
		return clientSelected;
	}

	public void setClientSelected(boolean clientSelected) {
		firePropertyChange(CLIENT_SELECTED, this.clientSelected, this.clientSelected = clientSelected);
	}

	public Boolean getRedcordSessionSelected() {
		return redcordSessionSelected;
	}

	public void setRedcordSessionSelected(Boolean redcordSessionSelected) {
		firePropertyChange(REDCORD_SESSION_SELECTED, this.redcordSessionSelected, this.redcordSessionSelected = redcordSessionSelected);
	}

	public Boolean getRedcordExerciseSelected() {
		return redcordExerciseSelected;
	}

	public void setRedcordExerciseSelected(Boolean redcordExerciseSelected) {
		firePropertyChange(REDCORD_EXERCISE_SELECTED, this.redcordExerciseSelected, this.redcordExerciseSelected = redcordExerciseSelected);
	}

	public ClientTablePanel getClientTablePanel() {
		return clientTablePanel;
	}

	public DaoService getDaoService() {
		return daoService;
	}


}
