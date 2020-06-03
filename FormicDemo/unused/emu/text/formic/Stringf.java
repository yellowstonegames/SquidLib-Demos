package text.formic;
import com.google.gwt.core.client.JsDate;
import elemental2.core.JsArray;
import text.formic.F4JSLibInjector;

import java.util.Date;

public final class Stringf {
	private Stringf(){}
	static {
		F4JSLibInjector.ensureInjected();
	}
	public static String format(String format, Object... args){
		JsArray ja = new JsArray<>();
		for(Object o : args) {
			if(o instanceof Long)
				ja.push(((Long) o).doubleValue());
			else if(o instanceof Date)
				ja.push(JsDate.create(((Date) o).getTime()));
			//// not available on GWT
//			else if(o instanceof Calendar)
//				ja.push(JsDate.create(((Calendar) o).getTimeInMillis()));
			else
				ja.push(o);
		}
		return fmt(format, ja);
	}
	
	private static native String fmt(String form, JsArray<Object> args) /*-{
		return $wnd.f4js.format(form, args);
	}-*/;
}
