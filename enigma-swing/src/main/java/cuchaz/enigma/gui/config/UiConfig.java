package cuchaz.enigma.gui.config;

import java.awt.Color;
import java.awt.Font;
import java.util.Optional;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.elements.MainWindow;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public final class UiConfig {

	private UiConfig() {
	}

	// General UI configuration such as localization
	private static final ConfigContainer ui = ConfigContainer.getOrCreate("enigma/enigmaui");
	// Swing specific configuration such as theming
	private static final ConfigContainer swing = ConfigContainer.getOrCreate("enigma/enigmaswing");

	// These are used for getting stuff that needs to stay constant for the
	// runtime of the program, e.g. the current theme, because changing of these
	// settings without a restart isn't implemented correctly yet.
	// Don't change the values in this container with the expectation that they
	// get saved, this is purely a backup of the configuration that existed at
	// startup.
	private static ConfigSection runningSwing;

	static {
		if (!swing.existsOnDisk() && !ui.existsOnDisk()) {
			OldConfigImporter.doImport();
		}

		UiConfig.snapshotConfig();
	}

	// Saves the current configuration state so a consistent user interface can
	// be provided for parts of the interface that don't support changing the
	// configuration at runtime. Calling this after any UI elements are
	// displayed can lead to visual glitches!
	public static void snapshotConfig() {
		runningSwing = swing.data().copy();
	}

	public static void save() {
		ui.save();
		swing.save();
	}

	public static String getLanguage() {
		return ui.data().section("General").setIfAbsentString("Language", I18n.DEFAULT_LANGUAGE);
	}

	public static void setLanguage(String language) {
		ui.data().section("General").setString("Language", language);
	}

	public static float getScaleFactor() {
		return (float) swing.data().section("General").setIfAbsentDouble("Scale Factor", 1.0);
	}

	public static float getActiveScaleFactor() {
		return (float) runningSwing.section("General").setIfAbsentDouble("Scale Factor", 1.0);
	}

	public static void setScaleFactor(float scale) {
		swing.data().section("General").setDouble("Scale Factor", scale);
	}

	/**
	 * Gets the dimensions of the different panels of the GUI.
	 * <p>These dimensions are used to determine the location of the separators between these panels.</p>
	 *
	 * <ul>
	 *     <li>[0] - The height of the obfuscated classes panel</li>
	 *     <li>[1] - The width of the classes panel</li>
	 *     <li>[2] - The width of the center panel</li>
	 *     <li>[3] - The height of the tabs panel. Only used if the logs panel should appear</li>
	 * </ul>
	 *
	 * @return an integer array composed of these 4 dimensions
	 */
	public static int[] getLayout() {
		return swing.data().section("Main Window").getIntArray("Layout").orElseGet(() -> new int[] { -1, -1, -1, -1 });
	}

	public static void setLayout(int leftV, int left, int right, int rightH) {
		swing.data().section("Main Window").setIntArray("Layout", new int[] { leftV, left, right, rightH });
	}

	public static LookAndFeel getLookAndFeel() {
		return swing.data().section("Themes").setIfAbsentEnum(LookAndFeel::valueOf, "Current", LookAndFeel.NONE);
	}

	public static LookAndFeel getActiveLookAndFeel() {
		return runningSwing.section("Themes").setIfAbsentEnum(LookAndFeel::valueOf, "Current", LookAndFeel.NONE);
	}

	public static void setLookAndFeel(LookAndFeel laf) {
		swing.data().section("Themes").setEnum("Current", laf);
	}

	public static Decompiler getDecompiler() {
		return ui.data().section("Decompiler").setIfAbsentEnum(Decompiler::valueOf, "Current", Decompiler.CFR);
	}

	public static void setDecompiler(Decompiler d) {
		ui.data().section("Decompiler").setEnum("Current", d);
	}

	private static Color fromComponents(int rgb, double alpha) {
		int rgba = rgb & 0xFFFFFF | (int) (alpha * 255) << 24;
		return new Color(rgba, true);
	}

	private static Color getThemeColorRgba(String colorName) {
		ConfigSection s = runningSwing.section("Themes").section(getActiveLookAndFeel().name()).section("Colors");
		return fromComponents(s.getRgbColor(colorName).orElse(0), s.getDouble(String.format("%s Alpha", colorName)).orElse(0));
	}

	private static Color getThemeColorRgb(String colorName) {
		ConfigSection s = runningSwing.section("Themes").section(getActiveLookAndFeel().name()).section("Colors");
		return new Color(s.getRgbColor(colorName).orElse(0));
	}

	public static Color getObfuscatedColor() {
		return getThemeColorRgba("Obfuscated");
	}

	public static Color getObfuscatedOutlineColor() {
		return getThemeColorRgba("Obfuscated Outline");
	}

	public static Color getProposedColor() {
		return getThemeColorRgba("Proposed");
	}

	public static Color getProposedOutlineColor() {
		return getThemeColorRgba("Proposed Outline");
	}

	public static Color getDeobfuscatedColor() {
		return getThemeColorRgba("Deobfuscated");
	}

	public static Color getDeobfuscatedOutlineColor() {
		return getThemeColorRgba("Deobfuscated Outline");
	}

	public static Color getEditorBackgroundColor() {
		return getThemeColorRgb("Editor Background");
	}

	public static Color getHighlightColor() {
		return getThemeColorRgb("Highlight");
	}

	public static Color getCaretColor() {
		return getThemeColorRgb("Caret");
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb("Selection Highlight");
	}

	public static Color getStringColor() {
		return getThemeColorRgb("String");
	}

	public static Color getNumberColor() {
		return getThemeColorRgb("Number");
	}

	public static Color getOperatorColor() {
		return getThemeColorRgb("Operator");
	}

	public static Color getDelimiterColor() {
		return getThemeColorRgb("Delimiter");
	}

	public static Color getTypeColor() {
		return getThemeColorRgb("Type");
	}

	public static Color getIdentifierColor() {
		return getThemeColorRgb("Identifier");
	}

	public static Color getTextColor() {
		return getThemeColorRgb("Text");
	}

	public static Color getLineNumbersForegroundColor() {
		return getThemeColorRgb("Line Numbers Foreground");
	}

	public static Color getLineNumbersBackgroundColor() {
		return getThemeColorRgb("Line Numbers Background");
	}

	public static Color getLineNumbersSelectedColor() {
		return getThemeColorRgb("Line Numbers Selected");
	}

	public static boolean useCustomFonts() {
		return swing.data().section("Themes").section(getActiveLookAndFeel().name()).section("Fonts").setIfAbsentBool("Use Custom", false);
	}

	public static boolean activeUseCustomFonts() {
		return runningSwing.section("Themes").section(getActiveLookAndFeel().name()).section("Fonts").setIfAbsentBool("Use Custom", false);
	}

	public static void setUseCustomFonts(boolean b) {
		swing.data().section("Themes").section(getActiveLookAndFeel().name()).section("Fonts").setBool("Use Custom", b);
	}

	public static Optional<Font> getFont(String name) {
		Optional<String> spec = swing.data().section("Themes").section(getActiveLookAndFeel().name()).section("Fonts").getString(name);
		return spec.map(Font::decode);
	}

	public static Optional<Font> getActiveFont(String name) {
		Optional<String> spec = runningSwing.section("Themes").section(getActiveLookAndFeel().name()).section("Fonts").getString(name);
		return spec.map(Font::decode);
	}

	public static void setFont(String name, Font font) {
		swing.data().section("Themes").section(getLookAndFeel().name()).section("Fonts").setString(name, encodeFont(font));
	}

	public static Font getDefaultFont() {
		return getActiveFont("Default").orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG).deriveFont(Font.BOLD)));
	}

	public static void setDefaultFont(Font font) {
		setFont("Default", font);
	}

	public static Font getDefault2Font() {
		return getActiveFont("Default 2").orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static void setDefault2Font(Font font) {
		setFont("Default 2", font);
	}

	public static Font getSmallFont() {
		return getActiveFont("Small").orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static void setSmallFont(Font font) {
		setFont("Small", font);
	}

	public static Font getEditorFont() {
		return getActiveFont("Editor").orElseGet(UiConfig::getFallbackEditorFont);
	}

	public static void setEditorFont(Font font) {
		setFont("Editor", font);
	}

	/**
	 * Gets the fallback editor font.
	 * It is used
	 * <ul>
	 * <li>when there is no custom editor font chosen</li>
	 * <li>when custom fonts are disabled</li>
	 * </ul>
	 *
	 * @return the fallback editor font
	 */
	public static Font getFallbackEditorFont() {
		return ScaleUtil.scaleFont(Font.decode(Font.MONOSPACED));
	}

	public static String encodeFont(Font font) {
		int style = font.getStyle();
		String s = style == (Font.BOLD | Font.ITALIC) ? "bolditalic" : style == Font.ITALIC ? "italic" : style == Font.BOLD ? "bold" : "plain";
		return String.format("%s-%s-%s", font.getName(), s, font.getSize());
	}

	public static void saveWindowState(MainWindow win) {
		win.saveState(swing.data().section("Main Window"));
	}

	public static void restoreWindowState(MainWindow win) {
		win.restoreState(swing.data().section("Main Window"));
	}

	public static String getLastSelectedDir() {
		return swing.data().section("File Dialog").getString("Selected").orElse("");
	}

	public static void setLastSelectedDir(String directory) {
		swing.data().section("File Dialog").setString("Selected", directory);
	}

	public static String getLastTopLevelPackage() {
		return swing.data().section("Mapping Stats").getString("Top-Level Package").orElse("");
	}

	public static void setLastTopLevelPackage(String topLevelPackage) {
		swing.data().section("Mapping Stats").setString("Top-Level Package", topLevelPackage);
	}

	public static boolean shouldIncludeSyntheticParameters() {
		return swing.data().section("Mapping Stats").setIfAbsentBool("Synthetic Parameters", false);
	}

	public static void setIncludeSyntheticParameters(boolean b) {
		swing.data().section("Mapping Stats").setBool("Synthetic Parameters", b);
	}

	public static void setLookAndFeelDefaults(LookAndFeel laf, boolean isDark) {
		ConfigSection s = swing.data().section("Themes").section(laf.name()).section("Colors");
		if (!isDark) {
			// Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
			s.setIfAbsentRgbColor("Line Numbers Foreground", 0x333300);
			s.setIfAbsentRgbColor("Line Numbers Background", 0xEEEEFF);
			s.setIfAbsentRgbColor("Line Numbers Selected", 0xCCCCEE);
			s.setIfAbsentRgbColor("Obfuscated", 0xFFDCDC);
			s.setIfAbsentDouble("Obfuscated Alpha", 1.0);
			s.setIfAbsentRgbColor("Obfuscated Outline", 0xA05050);
			s.setIfAbsentDouble("Obfuscated Outline Alpha", 1.0);
			s.setIfAbsentRgbColor("Proposed", 0x000000);
			s.setIfAbsentDouble("Proposed Alpha", 0.15);
			s.setIfAbsentRgbColor("Proposed Outline", 0x000000);
			s.setIfAbsentDouble("Proposed Outline Alpha", 0.75);
			s.setIfAbsentRgbColor("Deobfuscated", 0xDCFFDC);
			s.setIfAbsentDouble("Deobfuscated Alpha", 1.0);
			s.setIfAbsentRgbColor("Deobfuscated Outline", 0x50A050);
			s.setIfAbsentDouble("Deobfuscated Outline Alpha", 1.0);
			s.setIfAbsentRgbColor("Editor Background", 0xFFFFFF);
			s.setIfAbsentRgbColor("Highlight", 0x3333EE);
			s.setIfAbsentRgbColor("Caret", 0x000000);
			s.setIfAbsentRgbColor("Selection Highlight", 0x000000);
			s.setIfAbsentRgbColor("String", 0xCC6600);
			s.setIfAbsentRgbColor("Number", 0x999933);
			s.setIfAbsentRgbColor("Operator", 0x000000);
			s.setIfAbsentRgbColor("Delimiter", 0x000000);
			s.setIfAbsentRgbColor("Type", 0x000000);
			s.setIfAbsentRgbColor("Identifier", 0x000000);
			s.setIfAbsentRgbColor("Text", 0x000000);
		} else {
			// Based off colors found here: https://github.com/dracula/dracula-theme/
			s.setIfAbsentRgbColor("Line Numbers Foreground", 0xA4A4A3);
			s.setIfAbsentRgbColor("Line Numbers Background", 0x313335);
			s.setIfAbsentRgbColor("Line Numbers Selected", 0x606366);
			s.setIfAbsentRgbColor("Obfuscated", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Alpha", 0.3);
			s.setIfAbsentRgbColor("Obfuscated Outline", 0xFF5555);
			s.setIfAbsentDouble("Obfuscated Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Proposed", 0x606366);
			s.setIfAbsentDouble("Proposed Alpha", 0.3);
			s.setIfAbsentRgbColor("Proposed Outline", 0x606366);
			s.setIfAbsentDouble("Proposed Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Deobfuscated", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Alpha", 0.3);
			s.setIfAbsentRgbColor("Deobfuscated Outline", 0x50FA7B);
			s.setIfAbsentDouble("Deobfuscated Outline Alpha", 0.5);
			s.setIfAbsentRgbColor("Editor Background", 0x282A36);
			s.setIfAbsentRgbColor("Highlight", 0xFF79C6);
			s.setIfAbsentRgbColor("Caret", 0xF8F8F2);
			s.setIfAbsentRgbColor("Selection Highlight", 0xF8F8F2);
			s.setIfAbsentRgbColor("String", 0xF1FA8C);
			s.setIfAbsentRgbColor("Number", 0xBD93F9);
			s.setIfAbsentRgbColor("Operator", 0xF8F8F2);
			s.setIfAbsentRgbColor("Delimiter", 0xF8F8F2);
			s.setIfAbsentRgbColor("Type", 0xF8F8F2);
			s.setIfAbsentRgbColor("Identifier", 0xF8F8F2);
			s.setIfAbsentRgbColor("Text", 0xF8F8F2);
		}
	}

}
