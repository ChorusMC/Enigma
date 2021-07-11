package cuchaz.enigma.gui.util;

public interface LanguageChangeListener {

	void retranslateUi();

	default boolean isValid() {
		return true;
	}

}
