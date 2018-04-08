package org.myweb.vo;

public class Subject {
	private int id;
	private String name;
	private int teacher;
	private int grade;
	
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getTeacher() {
		return teacher;
	}
	
	public int getGrade() {
		return grade;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setTeacher(int teacher) {
		this.teacher = teacher;
	}
	
	public void setGrade(int grade) {
		this.grade = grade;
	}

}
