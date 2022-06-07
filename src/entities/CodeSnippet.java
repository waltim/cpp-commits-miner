package entities;

public class CodeSnippet {

	private String type;
	private String body;
	private String file;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}

	@Override
	public String toString() {
		return "CodeSnippet [type=" + type + ", body=" + body + ", file=" + file + "]";
	}
	
}
