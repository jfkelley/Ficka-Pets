package com.game.fickapets;

import java.io.Serializable;

public class Complaint implements Serializable {
	private static final long serialVersionUID = 0;
	
	public double hoursBeforeComplaint;
	public String complaint;
	
	public Integer getComplaintType() {
		return complaint.hashCode();
	}
}
