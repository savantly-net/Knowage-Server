package it.eng.spagobi.functions.metadata;

import java.util.HashSet;
import java.util.Set;

// Generated 10-mag-2016 14.47.57 by Hibernate Tools 3.4.0.CR1

import it.eng.spagobi.commons.metadata.SbiHibernateModel;

/**
 * SbiCatalogFunction generated by hbm2java
 */
public class SbiCatalogFunction extends SbiHibernateModel {

	private Integer functionId;
	private String name;
	private String description;
	private String benchmarks;
	private String language;
	private String family;
	private String onlineScript;
	private String offlineScriptTrain;
	private String offlineScriptUse;
	private String owner;
	private String keywords;
	private String label;
	private String type;
	private Set sbiFunctionInputVariables = new HashSet(0);
	private Set sbiFunctionOutputColumns = new HashSet(0);
	private Set sbiFunctionInputColumns = new HashSet(0);

	public SbiCatalogFunction() {
	}

	public SbiCatalogFunction(int functionId, String name, String description, String benchmarks, String language, String family, String script, String owner,
			String keywords, String label, String type) {
		this.functionId = functionId;
		this.name = name;
		this.description = description;
		this.benchmarks = benchmarks;
		this.family = family;
		this.language = language;
		this.onlineScript = script;
		this.owner = owner;
		this.keywords = keywords;
		this.label = label;
		this.type = type;
	}

	public Integer getFunctionId() {
		return this.functionId;
	}

	public void setFunctionId(Integer functionId) {
		this.functionId = functionId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getBenchmarks() {
		return benchmarks;
	}

	public void setBenchmarks(String benchmarks) {
		this.benchmarks = benchmarks;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public String getOnlineScript() {
		return onlineScript;
	}

	public void setOnlineScript(String onlineScript) {
		this.onlineScript = onlineScript;
	}

	public String getOfflineScriptTrain() {
		return offlineScriptTrain;
	}

	public void setOfflineScriptTrain(String offlineScriptTrain) {
		this.offlineScriptTrain = offlineScriptTrain;
	}

	public String getOfflineScriptUse() {
		return offlineScriptUse;
	}

	public void setOfflineScriptUse(String offlineScriptUse) {
		this.offlineScriptUse = offlineScriptUse;
	}

	public Set getSbiFunctionInputVariables() {
		return this.sbiFunctionInputVariables;
	}

	public void setSbiFunctionInputVariables(Set sbiFunctionInputVariables) {
		this.sbiFunctionInputVariables = sbiFunctionInputVariables;
	}

	public Set getSbiFunctionOutputColumns() {
		return sbiFunctionOutputColumns;
	}

	public void setSbiFunctionOutputColumns(Set sbiFunctionOutputColumns) {
		this.sbiFunctionOutputColumns = sbiFunctionOutputColumns;
	}

	public Set getSbiFunctionInputColumns() {
		return sbiFunctionInputColumns;
	}

	public void setSbiFunctionInputColumns(Set sbiFunctionInputColumns) {
		this.sbiFunctionInputColumns = sbiFunctionInputColumns;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
