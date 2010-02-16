package com.runwalk.video.gui;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.PropertyStateEvent;

import com.runwalk.video.RunwalkVideoApp;

/**
 * Binding listener that marks the currently selected client dirty and enables the save action.
 * 
 * @author Jeroen P.
 *
 */
public class ClientBindingListener extends AbstractBindingListener {

	@Override
	@SuppressWarnings("unchecked")
	public void targetChanged(Binding binding, PropertyStateEvent event) {
		RunwalkVideoApp.getApplication().getSelectedClient().setDirty(true);
		RunwalkVideoApp.getApplication().setSaveNeeded(true);
	}
	
}