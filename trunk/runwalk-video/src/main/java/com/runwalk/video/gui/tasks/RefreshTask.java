package com.runwalk.video.gui.tasks;

import java.util.List;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Task;

import ca.odell.glazedlists.CollectionList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.runwalk.video.RunwalkVideoApp;
import com.runwalk.video.VideoFileManager;
import com.runwalk.video.entities.Analysis;
import com.runwalk.video.entities.Article;
import com.runwalk.video.entities.City;
import com.runwalk.video.entities.Client;
import com.runwalk.video.gui.AnalysisConnector;
import com.runwalk.video.gui.panels.AnalysisOverviewTablePanel;
import com.runwalk.video.gui.panels.AnalysisTablePanel;
import com.runwalk.video.gui.panels.ClientTablePanel;

/**
 * This {@link Task} handles all database lookups and injects the results in the appropriate application component.
 * 
 * @author Jeroen Peelaerts
 *
 */
public class RefreshTask extends AbstractTask<Boolean, Void> {

	private EntityManager entityManager;
	
	private VideoFileManager videoFileManager;

	public RefreshTask(VideoFileManager videoFileManager, EntityManager entityManager) {
		super("refresh");
		this.videoFileManager = videoFileManager;
		this.entityManager = entityManager;
	}

	@Override 
	@SuppressWarnings("unchecked")
	protected Boolean doInBackground() {
		boolean success = true;
		try {
			message("startMessage");
			message("fetchMessage");
			setProgress(0, 0, 6);
			// get all clients from the db
			Query query = getEntityManager().createNamedQuery("findAllClients"); // NOI18N
			query.setHint("eclipselink.left-join-fetch", "c.analyses.recordings")
			.setHint("toplink.refresh", "true")
			.setHint("oracle.toplink.essentials.config.CascadePolicy", "CascadePrivateParts");
			final EventList<Client> clientList = GlazedLists.threadSafeList(GlazedLists.eventList(query.getResultList()));
			setProgress(1, 0, 6);
			// get all cities from the db
			query = getEntityManager().createNamedQuery("findAllCities")
			.setHint("toplink.refresh", "true")
			.setHint("oracle.toplink.essentials.config.CascadePolicy", "CascadePrivateParts");
			final EventList<City> cityList = GlazedLists.eventList(query.getResultList());
			setProgress(2, 0, 6);
			// get all articles from the db
			query = getEntityManager().createNamedQuery("findAllArticles")
			.setHint("toplink.refresh", "true")
			.setHint("oracle.toplink.essentials.config.CascadePolicy", "CascadePrivateParts"); // NOI18N
			final EventList<Article> articleList = GlazedLists.eventList(query.getResultList());
			setProgress(3, 0, 6);
			SwingUtilities.invokeAndWait(new Runnable() {

				public void run() {
					RunwalkVideoApp.getApplication().getClientInfoPanel().setItemList(cityList);
					// get client table panel and inject data
					ClientTablePanel clientTablePanel = RunwalkVideoApp.getApplication().getClientTablePanel();
					clientTablePanel.setItemList(clientList, Client.class);
					final EventList<Client> selectedClients = clientTablePanel.getEventSelectionModel().getSelected();
					CollectionList<Client, Analysis> selectedClientAnalyses = new CollectionList<Client, Analysis>(selectedClients, new CollectionList.Model<Client, Analysis>() {

						public List<Analysis> getChildren(Client parent) {
							return parent.getAnalyses();
						}

					});
					// get analysis tablepanel and inject data
					AnalysisTablePanel analysisTablePanel = RunwalkVideoApp.getApplication().getAnalysisTablePanel();
					analysisTablePanel.setArticleList(articleList);
					analysisTablePanel.setItemList(selectedClientAnalyses, new AnalysisConnector());
					final EventList<Client> deselectedClients = clientTablePanel.getEventSelectionModel().getDeselected();
					final CollectionList<Client, Analysis> deselectedClientAnalyses = new CollectionList<Client, Analysis>(deselectedClients, new CollectionList.Model<Client, Analysis>() {

						public List<Analysis> getChildren(Client parent) {
							return parent.getAnalyses();
						}

					});
					// get analysis overview tablepanel and inject data
					final CompositeList<Analysis> analysesOverview = new CompositeList<Analysis>(selectedClientAnalyses.getPublisher(), selectedClientAnalyses.getReadWriteLock());
					analysesOverview.addMemberList(selectedClientAnalyses);
					analysesOverview.addMemberList(deselectedClientAnalyses);
					// create the overview with unfinished analyses
					AnalysisOverviewTablePanel analysisOverviewTablePanel = RunwalkVideoApp.getApplication().getAnalysisOverviewTablePanel();
					analysisOverviewTablePanel.setItemList(analysesOverview, new AnalysisConnector());
				}

			});
			setProgress(4, 0, 6);
			// clear video file cache
			getVideoFileManager().clear();
			// some not so beautiful way to refresh the cache
			for (Client theClient : clientList) {
				for (Analysis analysis : theClient.getAnalyses()) {
					getVideoFileManager().refreshCache(analysis);
				}
			}
			setProgress(5, 0, 6);
			// check whether compressing should be enabled
			RunwalkVideoApp.getApplication().getAnalysisOverviewTablePanel().setCompressionEnabled(true);
			getEntityManager().close();
			setProgress(6, 0, 6);
			message("endMessage");
		} catch(Exception ignore) {
			getLogger().error(Level.SEVERE, ignore);
			success = false;
		}
		return success;
	}
	
	private VideoFileManager getVideoFileManager() {
		return this.videoFileManager;
	}
	
	private EntityManager getEntityManager() {
		return entityManager;
	}

}