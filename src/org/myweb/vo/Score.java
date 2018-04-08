package org.myweb.vo;

public class Score 
{
	private String subject;
	private double score;
	private String grade;
	private int id = -1;
	
	public int getId() {
		return id;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public double getScore() {
		return score;
	}
	
	public String getGrade() {
		return grade;
	}
	
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public void setScore(double score) {
		this.score = score;
	}
	
	public void setGrade(String grade) {
		this.grade = grade;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
}
