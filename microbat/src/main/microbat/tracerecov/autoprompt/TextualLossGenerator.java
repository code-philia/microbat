package microbat.tracerecov.autoprompt;

public class TextualLossGenerator {
	
	public TextualLossGenerator() {}

	public String getLossFromException(String output, Exception exception) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("When parsing the previously generated output: ```\n" + output + "\n```,");
		stringBuilder.append("The following exception was thrown: " + exception.getMessage());

		return stringBuilder.toString();
	}
}
