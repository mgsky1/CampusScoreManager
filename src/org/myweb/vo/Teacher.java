package org.myweb.vo;
/*
 * 老师实体类
 * Written By 小远
 * 2016-12-07
 */
public class Teacher {
	private String id;
	private String tel;
	private String name;
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setTel(String tel) {
		this.tel = tel;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTel() {
		return tel;
	}

}
