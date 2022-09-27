package c8y.to.aris.ms.rest.model;

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
