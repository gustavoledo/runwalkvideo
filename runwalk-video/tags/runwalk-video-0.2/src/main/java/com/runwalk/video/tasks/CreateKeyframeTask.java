package com.runwalk.video.tasks;

import java.util.Collection;
import java.util.Date;

import org.jdesktop.application.Task;

import com.runwalk.video.dao.DaoService;
import com.runwalk.video.entities.Keyframe;
import com.runwalk.video.entities.Recording;
import com.runwalk.video.media.VideoPlayer;
import com.runwalk.video.util.AppUtil;

public class CreateKeyframeTask extends AbstractTask<Keyframe, Void> {

	private final VideoPlayer frontMostPlayer;
	private final Collection<VideoPlayer> videoPlayers;
	private final DaoService daoService;

	/**
	 * This {@link Task} will create {@link Keyframe}s for the given {@link VideoPlayer}s.
	 * It will return the created {@link Keyframe} for the given frontMostPlayer.
	 * 
	 * @author Jeroen Peelaerts
	 */
	public CreateKeyframeTask(DaoService daoService, VideoPlayer frontMostPlayer, Collection<VideoPlayer> videoPlayers) {
		super("createKeyframe");
		this.daoService = daoService;
		this.videoPlayers = videoPlayers;
		this.frontMostPlayer = frontMostPlayer;
	}
	
	protected Keyframe doInBackground() throws Exception {
		message("startMessage", videoPlayers.size());
		Keyframe result = null;
		for (VideoPlayer videoPlayer : getVideoPlayers()) {
			videoPlayer.pauseIfPlaying();
			int position = videoPlayer.getKeyframePosition();
			// create a new Keyframe for the player's current recording
			final Recording recording = videoPlayer.getRecording();
			Keyframe keyframe = new Keyframe(recording, position);
			getDaoService().getDao(Keyframe.class).persist(keyframe);
			recording.addKeyframe(keyframe);
			if (getFrontmostPlayer() == videoPlayer) {
				result = keyframe;
			}
		}
		Date date = new Date(result.getPosition());
		String formattedDate = AppUtil.formatDate(date, AppUtil.EXTENDED_DURATION_FORMATTER);
		message("endMessage", formattedDate);
		return result;
	}
	
	public DaoService getDaoService() {
		return daoService;
	}

	public VideoPlayer getFrontmostPlayer() {
		return frontMostPlayer;
	}

	public Iterable<VideoPlayer> getVideoPlayers() {
		return videoPlayers;
	}
	
}
