package c8y.to.aris.ms.rest.model;

import java.util.List;

public class IngestionCycleResponse {
	private String key;
	private List<SourceTableResponse> dataUploadTargets = null;
	private boolean dataLoadTriggered;
	private CycleState state;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public List<SourceTableResponse> getDataUploadTargets() {
		return dataUploadTargets;
	}
	public void setDataUploadTargets(List<SourceTableResponse> dataUploadTargets) {
		this.dataUploadTargets = dataUploadTargets;
	}
	public boolean isDataLoadTriggered() {
		return dataLoadTriggered;
	}
	public void setDataLoadTriggered(boolean dataLoadTriggered) {
		this.dataLoadTriggered = dataLoadTriggered;
	}
	public CycleState getState() {
		return state;
	}
	public void setState(CycleState state) {
		this.state = state;
	}
	
	
}
