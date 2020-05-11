package text.formic;

public final class Stringf {
	private Stringf(){}
	public static String format(String format, Object... args){
		return String.format(format, args);
	}
}
