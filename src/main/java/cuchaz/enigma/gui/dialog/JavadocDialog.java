/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.Utils;

import javax.swing.*;
import javax.swing.text.html.HTML;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class JavadocDialog {

	private static JavadocDialog instance = null;

	private JFrame frame;

	private JavadocDialog(JFrame parent, JTextArea text, Callback callback) {
		// init frame
		frame = new JFrame(I18n.translate("javadocs.edit"));
		final Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout());

		// editor panel
		text.setTabSize(2);
		pane.add(new JScrollPane(text), BorderLayout.CENTER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						if (event.isControlDown())
							callback.closeUi(frame, true);
						break;
					case KeyEvent.VK_ESCAPE:
						callback.closeUi(frame, false);
						break;
					default:
						break;
				}
			}
		});

		// buttons panel
		JPanel buttonsPanel = new JPanel();
		FlowLayout buttonsLayout = new FlowLayout();
		buttonsLayout.setAlignment(FlowLayout.RIGHT);
		buttonsPanel.setLayout(buttonsLayout);
		buttonsPanel.add(Utils.unboldLabel(new JLabel(I18n.translate("javadocs.instruction"))));
		JButton cancelButton = new JButton(I18n.translate("javadocs.cancel"));
		cancelButton.addActionListener(event -> {
			// close (hide) the dialog
			callback.closeUi(frame, false);
		});
		buttonsPanel.add(cancelButton);
		JButton saveButton = new JButton(I18n.translate("javadocs.save"));
		saveButton.addActionListener(event -> {
			// exit enigma
			callback.closeUi(frame, true);
		});
		buttonsPanel.add(saveButton);
		pane.add(buttonsPanel, BorderLayout.SOUTH);

		// tags panel
		JMenuBar tagsMenu = new JMenuBar();

		// add javadoc tags
		for (JavadocTag tag : JavadocTag.values()) {
			JButton tagButton = new JButton(tag.getText());
			tagButton.addActionListener(action -> {
				String tagText = tag.isInline() ? "{" + tag.getText() + " }" : tag.getText() + " ";
				text.insert(tagText, text.getCaretPosition());
				if (tag.isInline()) {
					text.setCaretPosition(text.getCaretPosition() - 1);
				}
				text.grabFocus();
			});
			tagsMenu.add(tagButton);
		}

		// add html tags
		JComboBox<String> htmlList = new JComboBox<String>();
		htmlList.setPreferredSize(new Dimension());
		for (HTML.Tag htmlTag : HTML.getAllTags()) {
			htmlList.addItem(htmlTag.toString());
		}
		htmlList.addActionListener(action -> {
			String tagText = "<" + htmlList.getSelectedItem().toString() + ">";
			text.insert(tagText, text.getCaretPosition());
			text.grabFocus();
		});
		tagsMenu.add(htmlList);

		pane.add(tagsMenu, BorderLayout.NORTH);

		// show the frame
		frame.setSize(ScaleUtil.getDimension(600, 400));
		frame.setLocationRelativeTo(parent);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public static void init(JFrame parent, JTextArea area, Callback callback) {
		instance = new JavadocDialog(parent, area, callback);
		instance.frame.doLayout();
		instance.frame.setVisible(true);
	}

	public interface Callback {
		void closeUi(JFrame frame, boolean save);
	}

	private enum JavadocTag {
		CODE("@code", true),
		LINK("@link", true),
		LINKPLAIN("@linkplain", true),
		RETURN("@return", false),
		SEE("@see", false),
		THROWS("@throws", false);

		private String text;
		private boolean inline;

		private JavadocTag(String text, boolean inline) {
			this.text = text;
			this.inline = inline;
		}

		public String getText() {
			return this.text;
		}

		public boolean isInline() {
			return this.inline;
		}
	}
}
