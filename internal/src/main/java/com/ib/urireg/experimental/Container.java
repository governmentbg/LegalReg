package com.ib.urireg.experimental;

import com.ib.system.exceptions.InvalidParameterException;

public class Container {

	private String action;
	private String name;
	private String url;


	public Container(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}
	public void setAction(String action) throws InvalidParameterException {
		if (this.action == null) {
			this.action = action;
		}else {
			throw new InvalidParameterException("Container action is allready filled " + this.action + " --- " + action );
		}
	}
	public String getName() {
		return name;
	}
	public void setName(String name) throws InvalidParameterException {
		if (this.name == null) {
			this.name = name;
		}else {
			this.name += " " + name;
			//throw new InvalidParameterException("Container name is allready filled " + this.name + " --- " + name );
		}
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) throws InvalidParameterException {
		if (this.url == null && name != null) {
			this.url = url;
		}else {
			throw new InvalidParameterException("Container url is allready filled " + this.url + " --- " + url );
		}
	}

	public boolean isReady() {
		if (action != null && name != null && url != null) {
			System.out.println(action + " -> " + name);
			return true;
		}else {
			return false;
		}
	}


}
