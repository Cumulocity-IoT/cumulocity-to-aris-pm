package c8y.to.aris.data.continuous.ms.rest.model;

import java.util.List;

public class SourceTableResponse {
	private String key = null;
	
	private String name = null;

	private String namespace = null;
	
	private String fullyQualifiedName = null;
	
	private String persistenceMode = null;
	
	private List<SourceTableColumn> columns = null;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}

	public void setFullyQualifiedName(String fullyQualifiedName) {
		this.fullyQualifiedName = fullyQualifiedName;
	}

	public String getPersistenceMode() {
		return persistenceMode;
	}

	public void setPersistenceMode(String persistenceMode) {
		this.persistenceMode = persistenceMode;
	}

	public List<SourceTableColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<SourceTableColumn> columns) {
		this.columns = columns;
	}
	
	
}
