package com.ib.urireg.opendata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Този обект служи за изпращане на данни към OpenData
 *
 */
public class OpenDataObj {

    /** Ключ на потребител  */
	private String api_key;
    /** Уникален идентификатор на ресурс  */
	private String resource_uri;
    /** Формат на данните за визуализация (csv, xsd, odt…). */
	private String extension_format;

    /**
     * Масив съдържащ данните на ресурса. Данните
     * трябва да бъдат форматирани в json формат, в utf-8
     * кодиране. За форматиране може да се използва
     * методите за конвертиране, които API-то предоставя.
     */
    /* Ако е така то подаваме:
        String tmpData = "{"
                + "\"headers\": [\"Данни\", \"Месец\", \"Брой\"],"
                + "\"row1\": [\"тестови данни\", \"Май\", 4]"
                + "}";
    oData.setData(tmpData);*/
    @JsonProperty
    @JsonRawValue
    @JsonbTypeAdapter(RawJsonAdapter.class)
    private String data;

    /*
    Ако е така:
                oData.getData().put("headers", new Object[] { "Данни", "Месец", "Брой" });
                oData.getData().put("row1", new Object[] { "тестови данни", "Май", 4 });
     */
//    private Map<String, Object[]> data = new LinkedHashMap<String, Object[]>();


	public String getApi_key() {
		return api_key;
	}

	public void setApi_key(String api_key) {
		this.api_key = api_key;
	}

//	public Map<String, Object[]> getData() {
//		return data;
//	}
//
//	public void setData(Map<String, Object[]> data) {
//		this.data = data;
//	}

	public String getResource_uri() {
		return resource_uri;
	}

	public void setResource_uri(String resource_uri) {
		this.resource_uri = resource_uri;
	}

	public String getExtension_format() {
		return extension_format;
	}

	public void setExtension_format(String extension_format) {
		this.extension_format = extension_format;
	}

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OpenDataObj{");
        sb.append("api_key='").append(api_key).append('\'');
        sb.append(", resource_uri='").append(resource_uri).append('\'');
        sb.append(", extension_format='").append(extension_format).append('\'');
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
