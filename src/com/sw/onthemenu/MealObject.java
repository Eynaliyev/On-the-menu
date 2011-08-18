package com.sw.onthemenu;

import java.io.Serializable;

public class MealObject implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private String name;
	private String foursquarePicture;
	private String appPicture;

	public String getAppPicture() {
		return appPicture;
	}

	public void setAppPicture(String appPicture) {
		this.appPicture = appPicture;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFoursquarePicture() {
		return foursquarePicture;
	}

	public void setFoursquarePicture(String foursquarePicture) {
		this.foursquarePicture = foursquarePicture;
	}

	public void getAmazonPicture() {

	}
}
