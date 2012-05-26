package com.runwalk.video.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Bean that represents information regarding an exercise performed during a Redcord workout.
 * 
 * @author Jeroen Peelaerts
 */
@SuppressWarnings("serial")
@Entity
@Table(name="redcord_exercises")
public class RedcordExercise extends SerializableEntity<RedcordTableElement> implements RedcordTableElement {
	
	@ManyToOne
	@JoinColumn(name="redcord_session_id", nullable=false )
	private RedcordSession redcordSession;
	
	@Column
	private String name;
	
	@Column(name = "exercise_type")
	@Enumerated(EnumType.STRING)
	private ExerciseType exerciseType;
	
	@Column(name = "exercise_direction")
	@Enumerated(EnumType.STRING)
	private ExerciseDirection exerciseDirection;
	
	@Column(name="start_date")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date startDate;
	
	@Lob
	private String comments;
	
	public RedcordExercise() {	}

	public RedcordExercise(RedcordSession redcordSession, String name) {
		this.redcordSession = redcordSession;
		this.name = name;
		this.startDate = new Date();
	}
	
	public RedcordExercise(RedcordSession redcordSession) {
		this(redcordSession, "Oefening " + (redcordSession.getRedcordExerciseCount() + 1));
	}

	public RedcordSession getRedcordSession() {
		return redcordSession;
	}

	public ExerciseType getExerciseType() {
		return exerciseType;	
	}
	
	public void setExerciseDirection(ExerciseDirection exerciseDirection) {
		this.exerciseDirection = exerciseDirection;
	}
	
	public ExerciseDirection getExerciseDirection() {
		return exerciseDirection;
	}
	
	public void setExerciseType(ExerciseType exerciseType) {
		this.exerciseType = exerciseType;
	}

	public String getComments() {
		return comments;
	}
	
	public void setComments(String comments) {
		firePropertyChange(COMMENTS, this.comments, this.comments = comments);
	}
	
	public String getName() {
		return name;
	}

	public Date getStartDate() {
		return startDate;
	}
	
	public boolean allowsChildren() {
		return false;
	}
	
	public boolean isSynthetic() {
		return false;
	}

	public int compareTo(RedcordTableElement redcordTableElement) {
		int result = 1;
		if (redcordTableElement != null) {
			result = equals(redcordTableElement) ? 0 : getName().compareTo(redcordTableElement.getName());
		}
		return result;
	}

	@Override
	public String toString() {
		return "RedcordExercise [redcordSession=" + redcordSession + ", name="
				+ name + "]";
	}
	
}
