package com.eclipseuzmani.migrationtools.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class MigrationToolsModel {
	protected final transient PropertyChangeSupport pcs = new PropertyChangeSupport(
			this);
	private String pre;
	private String post;
	private String detail;

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public String getPre() {
		return pre;
	}

	public void setPre(String pre) {
		pcs.firePropertyChange("pre", this.pre, this.pre = pre);
	}

	public String getPost() {
		return post;
	}

	public void setPost(String post) {
		pcs.firePropertyChange("post", this.post, this.post = post);
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		pcs.firePropertyChange("detail", this.detail, this.detail = detail);
	}
}
