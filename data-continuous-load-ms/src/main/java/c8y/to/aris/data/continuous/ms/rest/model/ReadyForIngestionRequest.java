package c8y.to.aris.data.continuous.ms.rest.model;

import java.util.List;

public class ReadyForIngestionRequest {
	private List<FullyQualifiedName> dataUploadTargets;

	public List<FullyQualifiedName> getDataUploadTargets() {
		return dataUploadTargets;
	}

	public void setDataUploadTargets(List<FullyQualifiedName> dataUploadTargets) {
		this.dataUploadTargets = dataUploadTargets;
	}
	
}
